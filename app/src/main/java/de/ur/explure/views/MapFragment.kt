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
import com.mapbox.api.matching.v5.models.MapMatchingResponse
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.geometry.LatLng
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

    // route creation
    private val waypointsController = WaypointsController()
    private val directionsRoute: DirectionsRoute? = null

    // location tracking
    private lateinit var locationManager: LocationManager

    // permission handling
    private val permissionHelper: PermissionHelper by inject()

    private var backPressedCallback: OnBackPressedCallback? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Timber.d("in MapFragment onViewCreated")

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

        // setup bottomSheet
        childFragmentManager.commit {
            replace<RouteCreationBottomSheet>(R.id.bottomSheetContainer)
            setReorderingAllowed(true)
            addToBackStack(null)
        }
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
        mapViewModel.routeCreationModeActive.observe(viewLifecycleOwner, { active ->
            if (active) {
                enterRouteCreationMode()
            } else {
                leaveRouteCreationMode()
            }
        })
    }

    private fun setupInitialUIState() {
        // enable the buttons now that the map is ready
        binding.ownLocationButton.isEnabled = true
        binding.changeStyleButton.isEnabled = true
        binding.buildRouteButton.isEnabled = true

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

        // if location tracking was enabled before, start it again without forcing the user to
        // press the button again
        if (mapViewModel.isLocationTrackingActivated() == true) {
            mapViewModel.getCurrentMapStyle()?.let {
                startLocationTracking(it)
            }
        }
    }

    private fun showEnterRouteCreationDialog() {
        val activity = activity ?: return

        MaterialAlertDialogBuilder(activity)
            .setTitle("Routen erstellen")
            .setMessage(R.string.route_creation_options)
            .setPositiveButton("Route manuell erstellen") { _, _ ->
                setupRouteCreationMode()
            }
            .setNegativeButton("Route aufzeichnen") { _, _ ->
                Toast.makeText(
                    activity,
                    "Dieses Feature ist leider noch nicht implementiert. Wir arbeiten dran!",
                    Toast.LENGTH_SHORT
                ).show()
                // TODO
                // startLocationTracking(mapViewModel.getCurrentMapStyle() ?: return@setPositiveButton)
            }
            .show()
    }

    private fun setupRouteCreationMode() {
        mapViewModel.setRouteCreationModeStatus(isActive = true)

        // TODO show fragment manually as the injected mapViewModel in the RouteCreationBottomSheet doesn't seem to work
        val fragment: RouteCreationBottomSheet =
            childFragmentManager.findFragmentById(R.id.bottomSheetContainer) as RouteCreationBottomSheet
        fragment.showRouteCreationSheet()

        /*
        showSnackbar(
            "Click on the map to add points and build your route out these. You can see and reorder
            your waypoints at any time in the menu.",
            binding.mapContainer,
            length = Snackbar.LENGTH_LONG
        )*/

        // TODO map listeners erst hier setzen und sobald der Modus vorbei ist wieder entfernen?
        map.addOnMapClickListener {
            waypointsController.add(it)
            return@addOnMapClickListener true // consume the click
        }
    }

    private fun enterRouteCreationMode() {
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
    }

    // TODO this method is unfinished!
    private fun leaveRouteCreationMode() {
        binding.endRouteBuildingButton.visibility = View.INVISIBLE
        binding.endRouteBuildingButton.isEnabled = false
        binding.buildRouteButton.visibility = View.VISIBLE
        binding.buildRouteButton.isEnabled = true
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

    private fun convertPointsToRoute() {
        // check first if the user has an internet connection before requesting a route from mapbox
        if (!hasInternetConnection(requireContext(), R.string.no_internet_map_matching)) {
            return
        }

        val wayPoints = waypointsController.getAllWaypoints()
        if (wayPoints.size < 2) {
            showSnackbar(
                "Du musst mindestens 2 Punkte auf der Karte auswählen, damit eine Route erstellt werden kann!",
                binding.mapContainer,
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
            .coordinates(coordinates)
            .waypointIndices(0, coordinates.size - 1)
            .steps(true)
            .bannerInstructions(true)
            // .voiceInstructions(true)
            .profile(DirectionsCriteria.PROFILE_WALKING)
            // .overview(DirectionsCriteria.OVERVIEW_FULL)
            // .annotations(DirectionsCriteria.ANNOTATION_DURATION, DirectionsCriteria.ANNOTATION_DISTANCE)
            .build()

        mapMatchingRequest.enqueueCall(
            object : Callback<MapMatchingResponse> {
                override fun onFailure(call: Call<MapMatchingResponse>, t: Throwable) {
                    Timber.e("MapMatching request failure %s", t.toString())
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
                            return
                        }

                        val route = allMatchings?.get(0)?.toDirectionRoute()
                        Timber.d("Route: $route")
                        if (route != null) {
                            // TODO
                            /*
                            if (directionsRoute == null) {
                                startNavigation.visibility = View.VISIBLE
                            }
                            directionsRoute = route
                            mapboxNavigation?.setRoutes(listOf(route))
                            navigationMapboxMap?.drawRoute(route)
                            */
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

    /**
     * * Map Listeners
     */

    private fun setupMapListeners() {
        map.addOnCameraIdleListener(this::onCameraMoved)
        map.addOnMapClickListener(this::onMapClicked)
        map.addOnMapLongClickListener(this::onMapLongClicked)
    }

    private fun removeMapListeners() {
        if (this::map.isInitialized) {
            map.removeOnCameraIdleListener(this::onCameraMoved)
            map.removeOnMapClickListener(this::onMapClicked)
            map.removeOnMapLongClickListener(this::onMapLongClicked)
        }
    }

    private fun onCameraMoved() {
        mapViewModel.setCurrentCameraPosition(map.cameraPosition)
    }

    private fun onMapClicked(point: LatLng): Boolean {
        Timber.d("Clicked on map point with coordinates: $point")
        // If true this click is consumed and not passed to other listeners registered afterwards!
        return false
    }

    private fun onMapLongClicked(point: LatLng): Boolean {
        Timber.d("in on Long click on map")

        // add a symbol to the long-clicked point
        val marker = markerManager.addMarker(point)

        if (marker != null) {
            // save the marker in the viewmodel
            mapViewModel.saveMarker(marker)
        }

        binding.routeRetrievalProgressSpinner.visibility = View.VISIBLE
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

    companion object {
        // Note: this layer is not in all map styles available (e.g. the satellite style)!
        // private const val FIRST_SYMBOL_LAYER_ID = "waterway-label"

        private const val ROUTE_MARKER_SOURCE_ID = "ROUTE_MARKER_SOURCE_ID"
    }
}
