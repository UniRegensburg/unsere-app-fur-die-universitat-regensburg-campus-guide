package de.ur.explure.views

import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.appcompat.widget.ListPopupWindow
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.navigation.fragment.findNavController
import com.crazylegend.viewbinding.viewBinding
import com.daimajia.androidanimations.library.Techniques
import com.daimajia.androidanimations.library.YoYo
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.matching.v5.MapboxMapMatching
import com.mapbox.api.matching.v5.models.MapMatchingMatching
import com.mapbox.api.matching.v5.models.MapMatchingResponse
import com.mapbox.core.constants.Constants.PRECISION_6
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import de.ur.explure.R
import de.ur.explure.databinding.FragmentMapBinding
import de.ur.explure.map.LocationManager
import de.ur.explure.map.MarkerManager
import de.ur.explure.map.PermissionHelper
import de.ur.explure.map.RouteLineManager
import de.ur.explure.map.WaypointsController
import de.ur.explure.utils.EventObserver
import de.ur.explure.utils.Highlight
import de.ur.explure.utils.SharedPreferencesManager
import de.ur.explure.utils.TutorialBuilder
import de.ur.explure.utils.getMapboxAccessToken
import de.ur.explure.utils.hasInternetConnection
import de.ur.explure.utils.isGPSEnabled
import de.ur.explure.utils.measureContentWidth
import de.ur.explure.utils.showSnackbar
import de.ur.explure.utils.slideInView
import de.ur.explure.utils.slideOutView
import de.ur.explure.viewmodel.MapViewModel
import de.ur.explure.viewmodel.MapViewModel.Companion.All_MAP_STYLES
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.androidx.viewmodel.scope.emptyState
import org.koin.core.parameter.parametersOf
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
import java.util.*

@Suppress("TooManyFunctions")
class MapFragment : Fragment(R.layout.fragment_map), OnMapReadyCallback {

    private val binding by viewBinding(FragmentMapBinding::bind)

    // Setting the state as emptyState as a workaround for this issue: https://github.com/InsertKoinIO/koin/issues/963
    private val mapViewModel: MapViewModel by viewModel(state = emptyState())

    // SharedPrefs
    private val preferencesManager: SharedPreferencesManager by inject()

    // map
    private var mapView: MapView? = null
    private lateinit var map: MapboxMap
    private lateinit var markerManager: MarkerManager
    private lateinit var routeLineManager: RouteLineManager

    // route creation
    private val waypointsController = WaypointsController()
    private var directionsRoute: DirectionsRoute? = null

    // location tracking
    private lateinit var locationManager: LocationManager

    // permission handling
    private val permissionHelper: PermissionHelper by inject()

    private var backPressedCallback: OnBackPressedCallback? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // disable the buttons until the map has finished loading
        binding.ownLocationButton.isEnabled = false
        binding.changeStyleButton.isEnabled = false
        binding.buildRouteButton.isEnabled = false

        // init locationManager and sync with fragment lifecycle
        locationManager = get { parametersOf(this::onNewLocationReceived) }
        viewLifecycleOwner.lifecycle.addObserver(locationManager)

        setupViewModelObservers()

        setupBackButtonClickObserver()

        // init mapbox map
        mapView = binding.mapView
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)
    }

    private fun setupBackButtonClickObserver() {
        // This callback will show an alert dialog when the back button is pressed
        backPressedCallback = activity?.onBackPressedDispatcher?.addCallback(
            viewLifecycleOwner,
            enabled = true
        ) {
            // Handle the back button event
            with(MaterialAlertDialogBuilder(requireActivity())) {
                setTitle("Karte verlassen?")
                setMessage(
                    "Dein aktueller Stand geht verloren, wenn du diese Ansicht jetzt verlässt!" +
                            " Trotzdem zurückgehen?"
                )
                setPositiveButton("Ja") { _, _ -> findNavController().navigateUp() }
                setNegativeButton("Nein") { _, _ -> }
                show()
            }
        }
    }

    private fun setupViewModelObservers() {
        mapViewModel.mapReady.observe(viewLifecycleOwner, EventObserver {
            // this should only get called if the event has never been handled before because of the EventObserver
            Timber.d("Map has finished loading and can be used now!")
            setupInitialUIState()
        })
        mapViewModel.manualRouteCreationModeActive.observe(viewLifecycleOwner, { active ->
            if (active) {
                enterManualRouteCreationMode()
            } else {
                leaveManualRouteCreationMode()
            }
        })
    }

    private fun setupInitialUIState() {
        // enable the buttons now that the map is ready
        binding.ownLocationButton.isEnabled = true
        binding.changeStyleButton.isEnabled = true
        binding.buildRouteButton.isEnabled = true

        // setup bottomSheet for route creation mode
        childFragmentManager.commit {
            replace<RouteCreationBottomSheet>(R.id.bottomSheetContainer)
            setReorderingAllowed(true)
            addToBackStack(null)
        }

        binding.changeStyleButton.setOnClickListener {
            showMapStyleOptions(layoutResource = R.layout.popup_list_item)
        }

        binding.ownLocationButton.setOnClickListener {
            mapViewModel.getCurrentMapStyle()?.let { style -> startLocationTracking(style) }
        }

        binding.buildRouteButton.setOnClickListener {
            showEnterRouteCreationDialog()
        }

        binding.endRouteBuildingButton.setOnClickListener {
            saveRoute()
        }

        binding.startNavigationButton.setOnClickListener {
            // convert directionsRoute to json so it can be passed as a string via safe args
            val routeJson = directionsRoute?.toJson() ?: return@setOnClickListener
            val action = MapFragmentDirections.actionMapFragmentToNavigationFragment(
                route = routeJson
            )
            findNavController().navigate(action)
        }

        // if location tracking was enabled before, start it again to prevent forcing the user to
        // press the button again manually
        if (mapViewModel.isLocationTrackingActivated() == true) {
            mapViewModel.getCurrentMapStyle()?.let {
                startLocationTracking(it)
            }
        }
    }

    private fun showEnterRouteCreationDialog() {
        val activity = activity ?: return

        // TODO should probably have a Cancel - Option as well:
        MaterialAlertDialogBuilder(activity)
            .setTitle("Route erstellen")
            .setMessage(R.string.route_creation_options)
            .setPositiveButton("Route manuell erstellen") { _, _ ->
                setupManualRouteCreationMode()
            }
            .setNeutralButton("Route aufzeichnen") { _, _ ->
                Toast.makeText(
                    activity,
                    "Dieses Feature ist leider noch nicht implementiert. Wir arbeiten dran!",
                    Toast.LENGTH_SHORT
                ).show()
                // TODO
                // startLocationTracking(mapViewModel.getCurrentMapStyle() ?: return@setPositiveButton)
                // enterRouteRecordingMode()
            }
            .setNegativeButton("Route einzeichnen") { _, _ ->
                Toast.makeText(
                    activity,
                    "Dieses Feature ist leider auch noch nicht implementiert.",
                    Toast.LENGTH_SHORT
                ).show()
                // enterRouteDrawMode()
            }
            .show()
    }

    private fun setupManualRouteCreationMode() {
        mapViewModel.setManualRouteCreationModeStatus(isActive = true)
        /*
        showSnackbar(
            "Click on the map to add points and build your route out these. You can see and reorder
            your waypoints at any time in the menu.",
            binding.mapContainer,
            length = Snackbar.LENGTH_LONG
        )*/

        // set default map click listener behaviour in routeCreation-Mode and highlight default mode
        setAddMarkerClickListenerBehavior()
        highlightCurrentMode(RouteCreationModes.MODE_ADD)

        binding.routeCreationOptionsLayout.addMarkerButton.setOnClickListener {
            highlightCurrentMode(RouteCreationModes.MODE_ADD)
            setAddMarkerClickListenerBehavior()
        }
        binding.routeCreationOptionsLayout.editMarkerButton.setOnClickListener {
            highlightCurrentMode(RouteCreationModes.MODE_EDIT)
            // TODO allow user to edit the markers and their position (e.g. via infowindow ?)
        }
        binding.routeCreationOptionsLayout.deleteMarkerButton.setOnClickListener {
            highlightCurrentMode(RouteCreationModes.MODE_DELETE)
            // TODO delete markers on click
            // markerManager.setOnMarkerClickListenerBehavior(delete)
        }
        binding.routeCreationOptionsLayout.resetButton.setOnClickListener {
            with(MaterialAlertDialogBuilder(requireActivity())) {
                setTitle("Achtung!")
                setMessage("Möchtest du wirklich alles seit Beginn der Routenerstellung rückgängig machen?")
                setPositiveButton("Ja") { _, _ -> resetMapOverlays() }
                setNegativeButton(R.string.cancel) { _, _ -> }
                show()
            }
        }
        // TODO add a separate button for dragging markers too ? Would probably word if the other listeners are reset
    }

    private fun highlightCurrentMode(mode: RouteCreationModes) {
        val activity = activity ?: return

        // reset current highlight
        binding.routeCreationOptionsLayout.root.children.forEach {
            it.background = ContextCompat.getDrawable(activity, R.drawable.icon_button_border)
        }

        // get button for current mode and highlight it
        val button = when (mode) {
            RouteCreationModes.MODE_ADD -> binding.routeCreationOptionsLayout.addMarkerButton
            RouteCreationModes.MODE_EDIT -> binding.routeCreationOptionsLayout.editMarkerButton
            RouteCreationModes.MODE_DELETE -> binding.routeCreationOptionsLayout.deleteMarkerButton
        }
        button.background = ContextCompat.getDrawable(
            activity,
            R.drawable.icon_button_border_selected
        )
    }

    private fun resetMapOverlays() {
        routeLineManager.clearAllLines()

        mapViewModel.getActiveMarkerSymbols().forEach {
            markerManager.deleteMarker(it)
        }
        waypointsController.clear()
    }

    private fun setAddMarkerClickListenerBehavior() {
        map.addOnMapClickListener {

            val symbol = markerManager.addMarker(it)
            if (symbol != null) {
                mapViewModel.saveMarker(symbol)
            }
            mapViewModel.addCustomWaypoint(it)
            waypointsController.add(it)
            return@addOnMapClickListener true // consume the click
        }
    }

    // TODO im routeCreation Mode wärs schön wenn zusätzlich noch ein button auftauchen würde mit
    //  dem man den Tilt einstellen kann für angenehmeres bearbeiten
    private fun enterManualRouteCreationMode() {
        // toggle the start/end buttons
        binding.buildRouteButton.isEnabled = false
        binding.buildRouteButton.visibility = View.GONE
        binding.endRouteBuildingButton.visibility = View.VISIBLE
        binding.endRouteBuildingButton.isEnabled = true
        // and play an animation
        @Suppress("MagicNumber")
        YoYo.with(Techniques.FlipInX)
            .duration(500)
            .playOn(binding.endRouteBuildingButton)

        // slide in the options panel
        slideInView(binding.routeCreationOptionsLayout.root)
    }

    // TODO this method is unfinished!
    private fun leaveManualRouteCreationMode() {
        binding.endRouteBuildingButton.visibility = View.INVISIBLE
        binding.endRouteBuildingButton.isEnabled = false
        binding.buildRouteButton.visibility = View.VISIBLE
        binding.buildRouteButton.isEnabled = true

        @Suppress("MagicNumber")
        YoYo.with(Techniques.FlipInX)
            .duration(500)
            .playOn(binding.buildRouteButton)

        slideOutView(binding.routeCreationOptionsLayout.root)
    }

    private fun saveRoute() {
        with(MaterialAlertDialogBuilder(requireActivity())) {
            setTitle("Erstellte Route speichern?")
            setPositiveButton("Ja") { _, _ ->
                convertPointsToRoute()
            }
            setNegativeButton("Weiter bearbeiten") { _, _ -> }
            show()
        }
    }

    // TODO this should be a separate option where a user can click to show the current route and
    //  not be hidden behind the save route - Button!
    private fun convertPointsToRoute() {
        // check first if the user has an internet connection before requesting a route from mapbox
        if (!hasInternetConnection(requireContext(), R.string.no_internet_map_matching)) {
            return
        }

        val wayPoints = waypointsController.getAllWaypoints()
        if (wayPoints.size < 2) {
            showSnackbar(
                "Du musst mindestens 2 Punkte auf der Karte auswählen, damit eine Route erstellt werden kann!",
                binding.mapButtonContainer,
                colorRes = R.color.color_error
            )
            return
        }

        fetchRoute(wayPoints)

        // TODO save route

        // reset the controller
        waypointsController.clear()
    }

    private fun fetchRoute(coordinates: List<Point>) {
        Timber.d("MapMatching request with ${coordinates.size} coordinates.")

        val mapMatchingRequest = MapboxMapMatching.builder()
            .accessToken(getMapboxAccessToken(requireActivity()))
            .profile(DirectionsCriteria.PROFILE_WALKING)
            .coordinates(coordinates)
            // * optional params: *
            .waypointIndices(0, coordinates.size - 1)
            // .addWaypointNames()
            .steps(true)
            .bannerInstructions(true)
            // .voiceInstructions(true)
            .tidy(true)
            .overview(DirectionsCriteria.OVERVIEW_FULL)
            .geometries("polyline6") // maximal precision
            .annotations(
                DirectionsCriteria.ANNOTATION_DURATION,
                DirectionsCriteria.ANNOTATION_DISTANCE
            )
            // TODO timestamps pro Koordinate in ca. 5 Sekunden Abstand für bessere Ergebnisse ??
            // .timestamps()
            .build()

        mapMatchingRequest.enqueueCall(
            object : Callback<MapMatchingResponse> {
                override fun onFailure(call: Call<MapMatchingResponse>, t: Throwable) {
                    Timber.e("MapMatching request failure %s", t.toString())
                    // TODO check for all exceptions and give appropriate user feedback
                    //  see https://docs.mapbox.com/api/navigation/map-matching/#map-matching-api-errors
                }

                override fun onResponse(
                    call: Call<MapMatchingResponse>,
                    response: Response<MapMatchingResponse>
                ) {
                    Timber.d("MapMatching request succeeded")

                    if (response.isSuccessful) {
                        val allMatchings = response.body()?.matchings()
                        if (allMatchings?.isEmpty() == true) {
                            Timber.w("Couldn't get any map matchings for the waypoints!")
                            showSnackbar(
                                requireActivity(),
                                "Couldn't get any map matchings for the waypoints!",
                                colorRes = R.color.color_error
                            )
                            return
                        }

                        allMatchings?.let {
                            showSnackbar(
                                requireActivity(),
                                "Successfully mapmatched the given waypoints! Found " +
                                        "${allMatchings.size} possible route options.",
                                colorRes = R.color.colorAccent
                            )
                            showMapMatchedRoute(it)
                        }

                        // TODO things to do here:
                        //  - print the confidence and ask the user to provide more/ or more closely
                        //    aligned points if below threshold
                        //  - explain how this map matching works and that it is meant for outdoor usage!!

                        val allTracePoints = response.body()?.tracepoints()
                        // Timber.d("All Trace Points:\n ${allTracePoints.toString()}")

                        val bestMatching = allMatchings?.get(0)
                        Timber.d("Confidence: ${bestMatching?.confidence()?.times(100)} %")
                        bestMatching?.legs()?.get(0)

                        val route = bestMatching?.toDirectionRoute()
                        if (route != null) {
                            directionsRoute = route

                            // mapboxNavigation?.setRoutes(listOf(route))
                            // navigationMapboxMap?.drawRoute(route)

                            binding.startNavigationButton.visibility = View.VISIBLE
                        } else {
                            binding.startNavigationButton.visibility = View.GONE
                        }
                    } else {
                        Timber.e("Response unsuccessful: ${response.errorBody()}")
                    }
                }
            }
        )
    }

    private fun showMapMatchedRoute(matchings: List<MapMatchingMatching>) {
        if (matchings.isNotEmpty()) {
            val routeGeometry = matchings[0].geometry() ?: return
            val lineString = LineString.fromPolyline(routeGeometry, PRECISION_6)
            // val lineFeature = Feature.fromGeometry(lineString)
            routeLineManager.addLineToMap(lineString.coordinates())
        }
    }

    /*
    private fun drawMapMatched(matchings: List<MapMatchingMatching>, color: String = "#3bb2d0") {
        val style = map.style
        if (style != null && matchings.isNotEmpty()) {
            val routeGeometry = matchings[0].geometry() ?: return
            style.addSource(
                GeoJsonSource(
                    "source_map_matched", Feature.fromGeometry(
                        LineString.fromPolyline(routeGeometry, PRECISION_6)
                    )
                )
            )
            style.addLayer(
                LineLayer("layer_map_matched", "source_map_matched")
                    .withProperties(
                        lineColor(ColorUtils.colorToRgbaString(Color.parseColor(color))),
                        @Suppress("MagicNumber")
                        lineWidth(6f),
                        lineOpacity(0.8f)
                    )
            )
        }
    }*/

    /**
     * Show a popup window to let the user choose a map style.
     */
    private fun showMapStyleOptions(
        layoutResource: Int = android.R.layout.simple_list_item_1,
        horizontalOffsetValue: Int = 0,
        verticalOffsetValue: Int = 0
    ) {
        val context = activity ?: return

        val styleList = All_MAP_STYLES.keys.toList()
        val styleAdapter: ArrayAdapter<String> = ArrayAdapter<String>(
            context,
            layoutResource,
            styleList
        )
        val listPopup = ListPopupWindow(context)

        listPopup.apply {
            setAdapter(styleAdapter)
            width = measureContentWidth(context, styleAdapter)
            height = ListPopupWindow.WRAP_CONTENT
            isModal = true
            anchorView = binding.changeStyleButton
            horizontalOffset = horizontalOffsetValue
            verticalOffset = verticalOffsetValue
        }

        val drawable = ContextCompat.getDrawable(context, R.drawable.popup_list_background)
        listPopup.setBackgroundDrawable(drawable)

        listPopup.setOnItemClickListener { parent, _, position, _ ->
            val selectedItem =
                parent.getItemAtPosition(position) as? String ?: return@setOnItemClickListener
            val selectedMapStyle = All_MAP_STYLES[selectedItem]
            setMapStyle(selectedMapStyle)

            listPopup.dismiss()
        }
        listPopup.show()
    }

    /**
     * * Map code
     */

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.map = mapboxMap

        // restrict the camera to a given bounding box as the app focuses only on the campus
        map.setLatLngBoundsForCameraTarget(latLngBounds)

        // don't allow the user to tilt the map because it may confuse some users and
        // doesn't provide any value here
        map.uiSettings.isTiltGesturesEnabled = false

        if (preferencesManager.isFirstRun()) {
            showFirstRunTutorial()
            // mark first launch as completed so this tutorial won't be shown on further app starts
            // or configuration changes
            preferencesManager.completedFirstRun()
        }

        val style = preferencesManager.getCurrentMapStyle()
        setMapStyle(style)

        // init the camera at the saved cameraPosition if it is not null
        mapViewModel.getLastKnownCameraPosition()?.let {
            map.cameraPosition = it
        }
    }

    private fun showFirstRunTutorial() {
        // highlight interesting spots the first time the fragment is launched on this device
        TutorialBuilder.showTutorialFor(
            requireActivity(),
            Highlight(
                binding.changeStyleButton,
                title = getString(R.string.map_style_button_title),
                description = getString(R.string.map_style_button_description)
            ), Highlight(
                binding.ownLocationButton,
                title = getString(R.string.location_tracking_button_title),
                description = getString(R.string.location_tracking_button_description)
            )
        )
    }

    private fun setMapStyle(styleUrl: String?) {
        styleUrl ?: return

        map.setStyle(styleUrl) { mapStyle ->
            // Map is set up and the style has loaded.
            // save the current style in the shared preferences and the viewmodel
            mapViewModel.setCurrentMapStyle(mapStyle)

            // save the current style in the shared preferences
            preferencesManager.setCurrentMapStyle(styleUrl)

            setupMarkerManager(mapStyle)
            recreateMarkers()

            setupRouteLineManager(mapStyle)

            // ! This needs to be called AFTER the MarkerManager is set up because this way clicks
            // ! on the markers will be handled before clicks on the map itself!
            setupMapListeners()

            mapViewModel.setMapReadyStatus(true)
        }
    }

    private fun setupMarkerManager(mapStyle: Style) {
        markerManager = get { parametersOf(mapView, map, mapStyle) }
        // let the marker manager observe the fragment lifecycle so it can clean itself up on destroy
        viewLifecycleOwner.lifecycle.addObserver(markerManager)
    }

    private fun recreateMarkers() {
        // recreate all markers that were on the map before the config change or process death
        val allActiveMarkers = mapViewModel.getAllActiveMarkers()
        markerManager.addMarkers(allActiveMarkers)
    }

    private fun setupRouteLineManager(mapStyle: Style) {
        routeLineManager = get { parametersOf(mapView, map, mapStyle) }
        viewLifecycleOwner.lifecycle.addObserver(routeLineManager)
    }

    /**
     * * Map Listeners
     */

    private fun setupMapListeners() {
        map.addOnCameraIdleListener(this::onCameraMoved)
        map.addOnMapLongClickListener(this::onMapLongClicked)

        map.setOnInfoWindowClickListener {
            // TODO
            false
        }
    }

    private fun removeMapListeners() {
        if (this::map.isInitialized) {
            map.removeOnCameraIdleListener(this::onCameraMoved)
            map.removeOnMapLongClickListener(this::onMapLongClicked)
            // TODO save the currrent click listener behaviour so it can be removed here
            // map.removeOnMapClickListener()
        }
    }

    private fun onCameraMoved() {
        mapViewModel.setCurrentCameraPosition(map.cameraPosition)
    }

    private fun onMapLongClicked(point: LatLng): Boolean {
        Timber.d("in on Long click on map")

        // add a symbol to the long-clicked point
        val marker = markerManager.addMarker(point)

        if (marker != null) {
            // save the marker in the viewmodel
            mapViewModel.saveMarker(marker)
        }

        // Place the destination marker at the map long click location
        map.getStyle {
            val clickPointSource = it.getSourceAs<GeoJsonSource>(ROUTE_MARKER_SOURCE_ID)
            clickPointSource?.setGeoJson(
                Point.fromLngLat(
                    point.longitude,
                    point.latitude
                )
            )
        }

        // If true this click is consumed and not passed to other listeners registered afterwards!
        return false
    }

    /**
     * * Location Tracking Code
     */

    private fun startLocationTracking(loadedMapStyle: Style) {
        val activity = activity ?: return

        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(activity)) {
            // Check if GPS is enabled in the device settings
            if (!isGPSEnabled(activity)) {
                showSnackbar(
                    activity,
                    R.string.gps_not_activated,
                    binding.mapButtonContainer, // ! needs to be a coordinatorLayout to work correctly
                    Snackbar.LENGTH_LONG,
                    colorRes = R.color.color_warning
                )
                return
            }

            if (::locationManager.isInitialized) {
                mapViewModel.setLocationTrackingStatus(isEnabled = true)

                // enable location tracking with custom location engine
                locationManager.activateLocationComponent(
                    map.locationComponent, loadedMapStyle, useDefaultEngine = false
                )
            }
        } else {
            permissionHelper.requestLocationPermissions(
                activity,
                onPermissionsResultCallback = this::onLocationPermissionsResult,
                onPermissionsExplanationNeededCallback = this::onLocationPermissionExplanation
            )
        }
    }

    private fun onLocationPermissionsResult(granted: Boolean) {
        if (granted) {
            // try to find the device location and enable location tracking
            map.style?.let { startLocationTracking(it) }
        } else {
            showSnackbar(
                requireActivity(),
                R.string.location_permission_not_given,
                binding.mapButtonContainer,
                Snackbar.LENGTH_LONG,
                colorRes = R.color.color_warning
            )
        }
    }

    private fun onLocationPermissionExplanation() {
        showSnackbar(
            requireActivity(),
            R.string.location_permission_explanation,
            binding.mapButtonContainer,
            Snackbar.LENGTH_LONG,
            colorRes = R.color.color_info
        )
    }

    private fun onNewLocationReceived(location: Location) {
        // Pass the new location to the Maps SDK's LocationComponent
        map.locationComponent.forceLocationUpdate(location)
        // save the new location
        mapViewModel.setCurrentUserPosition(location)
    }

    /**
     * * Lifecycle Hooks
     *
     * To handle Mapbox state correctly, the corresponding mapView hooks need to be called here.
     */

    override fun onStart() {
        super.onStart()
        Timber.d("in MapFragment onStart")
        mapView?.onStart()
    }

    override fun onResume() {
        super.onResume()
        Timber.d("in MapFragment onResume")
        mapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        Timber.d("in MapFragment onPause")
        mapView?.onPause()
    }

    override fun onStop() {
        super.onStop()
        Timber.d("in MapFragment onStop")
        mapView?.onStop()
        mapViewModel.saveActiveMarkers()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        mapView?.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Timber.d("in MapFragment onDestroyView")

        removeMapListeners()
        backPressedCallback?.remove()

        mapView?.onDestroy()
    }

    enum class RouteCreationModes {
        MODE_ADD,
        MODE_EDIT,
        MODE_DELETE
    }

    companion object {
        // Note: this layer is not in all map styles available (e.g. the satellite style)!
        // private const val FIRST_SYMBOL_LAYER_ID = "waterway-label"

        private const val ROUTE_MARKER_SOURCE_ID = "ROUTE_MARKER_SOURCE_ID"

        // camera bounding box
        private val southWestCorner = LatLng(48.990768, 12.087611)
        private val northEastCorner = LatLng(49.006718, 12.101880)
        private val latLngBounds = LatLngBounds.Builder()
            .include(southWestCorner)
            .include(northEastCorner)
            .build()

        // Testing routes for map matching etc.:

        // Busbahnhof -> Mensa -> vor Zentralbib -> Unisee -> Weg unter Mensa -> Botanischer Garten
        val routePoints = listOf<Point>(
            Point.fromLngLat(12.091897088615497, 48.9986755276432),
            Point.fromLngLat(12.093398779683042, 48.99790078797133),
            Point.fromLngLat(12.095176764949287, 48.99759573694382),
            Point.fromLngLat(12.095045596693524, 48.99696500867813),
            Point.fromLngLat(12.092009249797059, 48.996774307308414),
            Point.fromLngLat(12.091600540864277, 48.99278790637206),
        )

        // Vielberthgebäude -> Unisee -> Botanischer Garten
        val routePointsSimple = listOf<Point>(
            Point.fromLngLat(12.095577, 49.000083),
            Point.fromLngLat(12.095153, 48.998243),
            Point.fromLngLat(12.091600540864277, 48.99278790637206)
        )

        // Vielberthgebäude -> PT-Cafete -> vor Zentralbib -> Mensa -> Weg unter Mensa
        // -> Botanischer Garten -> außen vor H51 -> Chemie-Cafete -> Unisee -> Kugel
        val routePointsComplicated = listOf<Point>(
            Point.fromLngLat(12.095577, 49.000083),
            Point.fromLngLat(12.095873, 48.999174),
            Point.fromLngLat(12.095224, 48.998051),
            Point.fromLngLat(12.093412, 48.997994),
            Point.fromLngLat(12.091575, 48.993578),
            Point.fromLngLat(12.094713, 48.994392),
            Point.fromLngLat(12.095448, 48.995758),
            Point.fromLngLat(12.095244, 48.997110),
            Point.fromLngLat(12.095153, 48.998243)
        )
    }
}
