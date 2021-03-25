package de.ur.explure.views

import android.graphics.RectF
import android.location.Location
import android.os.Bundle
import android.view.Gravity
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
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.matching.v5.models.MapMatchingMatching
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
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import de.ur.explure.R
import de.ur.explure.databinding.FragmentMapBinding
import de.ur.explure.extensions.moveCameraToPosition
import de.ur.explure.extensions.toLatLng
import de.ur.explure.extensions.toPoint
import de.ur.explure.map.LocationManager
import de.ur.explure.map.ManualRouteCreationModes
import de.ur.explure.map.MapMatchingClient
import de.ur.explure.map.MarkerManager
import de.ur.explure.map.PermissionHelper
import de.ur.explure.map.RouteCreationMode
import de.ur.explure.map.RouteDrawModes
import de.ur.explure.map.RouteLineManager
import de.ur.explure.map.RouteLineManager.Companion.DRAW_LINE_LAYER_ID
import de.ur.explure.map.WaypointsController
import de.ur.explure.utils.EventObserver
import de.ur.explure.utils.Highlight
import de.ur.explure.utils.SharedPreferencesManager
import de.ur.explure.utils.TutorialBuilder
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
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.scope.emptyState
import org.koin.core.parameter.parametersOf
import timber.log.Timber
import java.util.*

@Suppress("TooManyFunctions")
class MapFragment : Fragment(R.layout.fragment_map), OnMapReadyCallback,
    MapMatchingClient.MapMatchingListener {

    private val binding by viewBinding(FragmentMapBinding::bind)

    // Setting the state as emptyState as a workaround for this issue: https://github.com/InsertKoinIO/koin/issues/963
    // private val mapViewModel: MapViewModel by viewModel(state = emptyState())
    private val mapViewModel: MapViewModel by sharedViewModel(state = emptyState())

    // SharedPrefs
    private val preferencesManager: SharedPreferencesManager by inject()

    // map
    private var mapView: MapView? = null
    private lateinit var map: MapboxMap
    private lateinit var markerManager: MarkerManager
    private var routeLineManager: RouteLineManager? = null

    private var routeCreationMapClickListenerBehavior: MapboxMap.OnMapClickListener? = null

    // route creation
    private val waypointsController: WaypointsController by inject()
    private var directionsRoute: DirectionsRoute? = null
    private val mapMatchingClient: MapMatchingClient by inject()

    // location tracking
    private lateinit var locationManager: LocationManager

    // permission handling
    private val permissionHelper: PermissionHelper by inject()

    private var backPressedCallback: OnBackPressedCallback? = null

    private lateinit var slidingBottomPanel: SlidingUpPanelLayout
    private lateinit var slidingPanelListener: SlidingUpPanelLayout.PanelSlideListener

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // disable the buttons until the map has finished loading
        binding.ownLocationButton.isEnabled = false
        binding.changeStyleButton.isEnabled = false
        binding.buildRouteButton.isEnabled = false

        // init locationManager and sync with fragment lifecycle
        locationManager = get { parametersOf(this::onNewLocationReceived) }
        viewLifecycleOwner.lifecycle.addObserver(locationManager)

        setupBackButtonClickObserver()
        setupViewModelObservers()
        // setup the sliding panel BEFORE the map!
        setupSlidingPanel()

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
                setTitle(R.string.leave_map_title)
                setMessage(R.string.leave_map_warning)
                setPositiveButton(R.string.yes) { _, _ -> findNavController().navigateUp() }
                setNegativeButton(R.string.no) { _, _ -> }
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
                exitManualRouteCreationMode()
            }
        })
        mapViewModel.routeDrawModeActive.observe(viewLifecycleOwner, { active ->
            if (active) {
                enterRouteDrawMode()
            } else {
                exitRouteDrawMode()
            }
        })
        mapViewModel.selectedMarker.observe(viewLifecycleOwner, { marker ->
            // move the camera to the selected marker
            if (::map.isInitialized) {
                map.moveCameraToPosition(marker.markerPosition, selectedMarkerZoom)
            }
        })
        mapViewModel.deletedWaypoint.observe(viewLifecycleOwner, { mapMarker ->
            // check to prevent crashes on config change as marker manager is not setup initially
            if (::markerManager.isInitialized) {
                // delete the corresponding marker symbol and remove from waypointscontroller
                waypointsController.remove(mapMarker.markerPosition.toPoint())
                markerManager.removeWaypointMarker(mapMarker)
            }
        })
    }

    /**
     * * UI Setup
     */

    private fun setupSlidingPanel() {
        slidingBottomPanel = binding.slidingRootLayout
        binding.dragView.visibility = View.INVISIBLE

        slidingPanelListener = object : SlidingUpPanelLayout.PanelSlideListener {
            override fun onPanelSlide(panel: View?, slideOffset: Float) {
                Timber.i("onPanelSlide, offset $slideOffset")
            }

            override fun onPanelStateChanged(
                panel: View?,
                previousState: SlidingUpPanelLayout.PanelState?,
                newState: SlidingUpPanelLayout.PanelState?
            ) {
                if (previousState == SlidingUpPanelLayout.PanelState.HIDDEN) {
                    binding.dragView.visibility = View.VISIBLE
                }
            }
        }
        slidingBottomPanel.addPanelSlideListener(slidingPanelListener)
        /*
        slidingBottomPanel.setFadeOnClickListener {
            slidingBottomPanel.panelState = SlidingUpPanelLayout.PanelState.COLLAPSED
        }*/
    }

    private fun setupInitialUIState() {
        // enable the buttons now that the map is ready
        binding.ownLocationButton.isEnabled = true
        binding.changeStyleButton.isEnabled = true
        binding.buildRouteButton.isEnabled = true

        // setup bottomSheet for route creation mode
        childFragmentManager.commit {
            replace<RouteCreationBottomSheet>(R.id.dragViewFragmentContainer)
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
            confirmManualRouteCreationFinish()
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

        // enable the route creation mode again if it was active when the fragment was destroyed
        if (mapViewModel.manualRouteCreationModeActive.value == true) {
            setupManualRouteCreationMode()
        } else if (mapViewModel.routeDrawModeActive.value == true) {
            setupRouteDrawMode()
        }
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

    private fun showEnterRouteCreationDialog() {
        val activity = activity ?: return

        MaterialAlertDialogBuilder(activity, R.style.RouteCreationMaterialDialogTheme)
            .setTitle(R.string.create_route_title)
            .setMessage(R.string.route_creation_options)
            .setCancelable(true)
            .setPositiveButton(R.string.manual_route_creation_option) { _, _ ->
                // TODO show tutorial with TutorialBuilder and explain mapMatching-Limitations here!
                setupManualRouteCreationMode()
            }
            .setNeutralButton(R.string.route_track_option) { _, _ ->
                Toast.makeText(
                    activity,
                    "Dieses Feature ist leider noch nicht implementiert. Wir arbeiten dran!",
                    Toast.LENGTH_SHORT
                ).show()
                // TODO
                // startLocationTracking(mapViewModel.getCurrentMapStyle() ?: return@setPositiveButton)
                // setupRouteRecordingMode()
            }
            .setNegativeButton(R.string.route_draw_option) { _, _ ->
                setupRouteDrawMode()
            }
            .show()
    }

    /**
     * * Route Creation Code
     */

    /**
     * Set the state in the viewModel to trigger certain corresponding actions and setup the options
     * panel and it's buttons for the manual route creation mode.
     */
    private fun setupManualRouteCreationMode() {
        mapViewModel.setManualRouteCreationModeStatus(isActive = true)

        // highlight default mode
        highlightCurrentRouteCreationMode(ManualRouteCreationModes.MODE_ADD)

        binding.cancelRouteCreationButton.setOnClickListener {
            showLeaveRouteCreationDialog()
        }
        binding.showMapMatchedButton.setOnClickListener {
            convertPointsToRoute()
        }

        binding.routeCreationOptionsLayout.addMarkerButton.setOnClickListener {
            highlightCurrentRouteCreationMode(ManualRouteCreationModes.MODE_ADD)
            // (re-)set marker manager click listener behavior
            markerManager.setDefaultMarkerClickListenerBehavior()
            // add markers on click
            setAddMarkerClickListenerBehavior()
        }
        binding.routeCreationOptionsLayout.editMarkerButton.setOnClickListener {
            highlightCurrentRouteCreationMode(ManualRouteCreationModes.MODE_EDIT)
            routeCreationMapClickListenerBehavior?.let { map.removeOnMapClickListener(it) }
            Toast.makeText(
                requireContext(),
                "Diese Funktionalität ist leider noch nicht implementiert!",
                Toast.LENGTH_SHORT
            ).show()
            // TODO allow user to edit the markers and their position (e.g. via infowindow ?)
        }
        binding.routeCreationOptionsLayout.deleteMarkerButton.setOnClickListener {
            highlightCurrentRouteCreationMode(ManualRouteCreationModes.MODE_DELETE)
            // delete markers on click
            routeCreationMapClickListenerBehavior?.let { map.removeOnMapClickListener(it) }
            markerManager.setDeleteMarkerClickListenerBehavior(onMarkerDeleted = {
                // remove this marker from waypoints controller and from viewModel
                waypointsController.remove(it.geometry)
                mapViewModel.removeMarker(it)
            })
        }
        binding.routeCreationOptionsLayout.resetButton.setOnClickListener {
            showResetMapDialog(getString(R.string.reset_manual_route_creation))
        }

        // TODO add a separate button for dragging markers too ?
        //  Would probably work if the other listeners are reset
    }

    /**
     * Set the state in the viewModel to trigger certain corresponding actions and setup the options
     * panel and it's buttons for the route draw mode.
     */
    private fun setupRouteDrawMode() {
        mapViewModel.setRouteDrawModeStatus(isActive = true)

        highlightCurrentRouteCreationMode(RouteDrawModes.MODE_DRAW)

        binding.cancelRouteCreationButton.setOnClickListener {
            showLeaveRouteCreationDialog()
        }
        binding.showMapMatchedButton.setOnClickListener {
            mapMatchDrawnRoute()
        }

        binding.routeDrawOptionsLayout.drawRouteButton.setOnClickListener {
            highlightCurrentRouteCreationMode(RouteDrawModes.MODE_DRAW)
            // reset click listener behavior if it was set and enable drawing
            routeCreationMapClickListenerBehavior?.let { map.removeOnMapClickListener(it) }
            routeLineManager?.enableMapDrawing()
        }
        binding.routeDrawOptionsLayout.moveMapButton.setOnClickListener {
            highlightCurrentRouteCreationMode(RouteDrawModes.MODE_MOVE)
            // reset click listener behavior if it was set and enable map movement
            routeCreationMapClickListenerBehavior?.let { map.removeOnMapClickListener(it) }
            routeLineManager?.enableMapMovement()
        }
        binding.routeDrawOptionsLayout.deleteRouteButton.setOnClickListener {
            highlightCurrentRouteCreationMode(RouteDrawModes.MODE_DELETE)
            routeLineManager?.enableMapMovement() // reset the touch listener first
            setRemoveRouteClickListenerBehavior()
        }
        binding.routeDrawOptionsLayout.resetButton.setOnClickListener {
            showResetMapDialog(getString(R.string.reset_map_draw))
        }
    }

    private fun highlightCurrentRouteCreationMode(mode: RouteCreationMode) {
        // get the correct options panel layout based on the given mode
        val optionsPanelLayout = when {
            ManualRouteCreationModes.values().contains(mode) -> {
                binding.routeCreationOptionsLayout.root
            }
            RouteDrawModes.values().contains(mode) -> {
                binding.routeDrawOptionsLayout.root
            }
            else -> return
        }

        // reset current highlight
        optionsPanelLayout.children.forEach {
            it.background = ContextCompat.getDrawable(requireActivity(), R.drawable.background_icon_button)
        }

        // get button for current mode and highlight it
        val button = when (mode) {
            ManualRouteCreationModes.MODE_ADD -> binding.routeCreationOptionsLayout.addMarkerButton
            ManualRouteCreationModes.MODE_EDIT -> binding.routeCreationOptionsLayout.editMarkerButton
            ManualRouteCreationModes.MODE_DELETE -> binding.routeCreationOptionsLayout.deleteMarkerButton

            RouteDrawModes.MODE_DRAW -> binding.routeDrawOptionsLayout.drawRouteButton
            RouteDrawModes.MODE_MOVE -> binding.routeDrawOptionsLayout.moveMapButton
            RouteDrawModes.MODE_DELETE -> binding.routeDrawOptionsLayout.deleteRouteButton

            else -> return
        }
        button.background = ContextCompat.getDrawable(
            requireActivity(),
            R.drawable.background_icon_button_selected
        )
    }

    private fun showLeaveRouteCreationDialog() {
        with(MaterialAlertDialogBuilder(requireActivity())) {
            setMessage(R.string.leave_route_creation_warning)
            setPositiveButton(R.string.yes) { _, _ ->
                // reset layout and route creation progress
                if (mapViewModel.manualRouteCreationModeActive.value == true) {
                    mapViewModel.setManualRouteCreationModeStatus(isActive = false)
                } else if (mapViewModel.routeDrawModeActive.value == true) {
                    mapViewModel.setRouteDrawModeStatus(isActive = false)
                }
            }
            setNegativeButton(R.string.no) { _, _ -> }
            show()
        }
    }

    private fun showResetMapDialog(message: String) {
        with(MaterialAlertDialogBuilder(requireActivity())) {
            setTitle(R.string.attention)
            setMessage(message)
            setPositiveButton(R.string.yes) { _, _ ->
                resetMapOverlays()
            }
            setNegativeButton(R.string.cancel) { _, _ -> }
            show()
        }
    }

    private fun performSharedCreationEnterActions() {
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

        binding.cancelRouteCreationButton.visibility = View.VISIBLE
        binding.showMapMatchedButton.visibility = View.VISIBLE
    }

    private fun performSharedCreationExitActions() {
        binding.endRouteBuildingButton.visibility = View.GONE
        binding.endRouteBuildingButton.isEnabled = false
        binding.buildRouteButton.visibility = View.VISIBLE
        binding.buildRouteButton.isEnabled = true

        @Suppress("MagicNumber")
        YoYo.with(Techniques.FlipInX)
            .duration(500)
            .playOn(binding.buildRouteButton)

        binding.cancelRouteCreationButton.visibility = View.GONE
        binding.showMapMatchedButton.visibility = View.GONE
        binding.startNavigationButton.visibility = View.GONE

        // reset the map
        resetMapOverlays()
    }

    // TODO im routeCreation Mode wärs schön wenn zusätzlich noch ein button auftauchen würde mit
    //  dem man den Tilt einstellen kann für angenehmeres bearbeiten
    private fun enterManualRouteCreationMode() {
        performSharedCreationEnterActions()

        // slide in the options panel
        slideInView(binding.routeCreationOptionsLayout.root)

        // show bottom sheet panel
        slidingBottomPanel.panelState = SlidingUpPanelLayout.PanelState.COLLAPSED

        // set default map click listener behaviour in manualRouteCreation-Mode
        setAddMarkerClickListenerBehavior()
    }

    private fun exitManualRouteCreationMode() {
        performSharedCreationExitActions()

        // slide out the options panel
        slideOutView(binding.routeCreationOptionsLayout.root)

        // Hide bottom sheet panel by setting it to collapsed and its view to invisible.
        // This is a workaround as setting it to State.Hidden or its View to Gone would cause the
        // bottom navigation bar that is shown again to overlap the button at the bottom!
        slidingBottomPanel.panelState = SlidingUpPanelLayout.PanelState.COLLAPSED
        binding.dragView.visibility = View.INVISIBLE

        // remove the click listener behavior for manual route creation mode
        routeCreationMapClickListenerBehavior?.let {
            // necessary check as the viewmodel observer fires the first time as well which might be
            // before the map has been setup
            if (::map.isInitialized) {
                map.removeOnMapClickListener(it)
            }
        }
    }

    private fun enterRouteDrawMode() {
        performSharedCreationEnterActions()

        // slide in the options panel
        slideInView(binding.routeDrawOptionsLayout.root)

        routeLineManager?.initFreeDrawMode()
        routeLineManager?.enableMapDrawing()
        // Toast.makeText(requireActivity(), "Move your finger on the map to draw a route", Toast.LENGTH_SHORT).show()
    }

    private fun exitRouteDrawMode() {
        performSharedCreationExitActions()

        // slide out the options panel
        slideOutView(binding.routeDrawOptionsLayout.root)

        // remove the custom touch behavior
        routeLineManager?.enableMapMovement()

        // reset the click listener behavior if it was changed
        routeCreationMapClickListenerBehavior?.let {
            if (::map.isInitialized) {
                map.removeOnMapClickListener(it)
            }
        }
    }

    private fun setAddMarkerClickListenerBehavior() {
        routeCreationMapClickListenerBehavior = MapboxMap.OnMapClickListener {
            val symbol = markerManager.addMarker(it)
            if (symbol != null) {
                mapViewModel.addNewMapMarker(symbol)
            }
            waypointsController.add(it)

            true // consume the click
        }

        routeCreationMapClickListenerBehavior?.let {
            if (::map.isInitialized) {
                map.addOnMapClickListener(it)
            }
        }
    }

    private fun setRemoveRouteClickListenerBehavior() {
        routeCreationMapClickListenerBehavior = MapboxMap.OnMapClickListener {
            // Detect whether a linestring of the draw layer was clicked on
            val screenPoint = map.projection.toScreenLocation(it)

            @Suppress("MagicNumber")
            val touchAreaBuffer = 15
            // make the touch area a little bit bigger to provide a better ux
            val screenArea = RectF(
                screenPoint.x - touchAreaBuffer,
                screenPoint.y - touchAreaBuffer,
                screenPoint.x + touchAreaBuffer,
                screenPoint.y + touchAreaBuffer
            )

            val featureList = map.queryRenderedFeatures(screenArea, DRAW_LINE_LAYER_ID)
            if (featureList.isNotEmpty()) {
                // delete the clicked line
                val clickedFeature = featureList[0]
                routeLineManager?.removeLineStringFromMap(clickedFeature)
            }
            true // consume the click
        }

        routeCreationMapClickListenerBehavior?.let {
            if (::map.isInitialized) {
                map.addOnMapClickListener(it)
            }
        }
    }

    private fun resetMapOverlays() {
        // clear lines on the map
        routeLineManager?.clearAllLines()

        if (::markerManager.isInitialized) {
            // clear markers on the map and in the viewmodel
            markerManager.deleteAllMarkers()
            mapViewModel.removeActiveMarkers()
        }

        // reset the waypointsController
        waypointsController.clear()
    }

    private fun convertPointsToRoute() {
        // check first if the user has an internet connection before requesting a route from mapbox
        if (!hasInternetConnection(requireContext(), R.string.no_internet_map_matching)) {
            return
        }

        val wayPoints = waypointsController.getAllWaypoints()
        if (wayPoints.size < 2) {
            showSnackbar(
                getString(R.string.too_few_waypoints),
                binding.mapButtonContainer,
                colorRes = R.color.colorError
            )
            return
        }

        // make a request to the Mapbox Map Matching API to get a route from the waypoints
        mapMatchingClient.requestMapMatchedRoute(wayPoints)
    }

    private fun mapMatchDrawnRoute() {
        val allRoutePoints = routeLineManager?.getCompleteRoute()

        if (allRoutePoints != null) {
            // add markers to the waypoints of the route
            allRoutePoints.forEach {
                markerManager.addMarker(it.toLatLng())
            }
            mapMatchingClient.requestMapMatchedRoute(allRoutePoints)

            // TODO if the request failed ask user if he wants to try again and edit the route
            //  if we get a map matched route ask the user which one he wants to save (map matched or his own)
        }
    }

    private fun confirmManualRouteCreationFinish() {
        with(MaterialAlertDialogBuilder(requireActivity())) {
            setTitle(R.string.save_created_route_confirmation)
            setPositiveButton(R.string.yes) { _, _ ->
                saveCreatedRoute()
                mapViewModel.setManualRouteCreationModeStatus(isActive = false)
            }
            setNegativeButton(R.string.continue_edit) { _, _ -> }
            show()
        }
    }

    private fun saveCreatedRoute() {
        // TODO:
        // - make a snapshot of the created route with the Mapbox Snapshotter and save it to firebase storage
        // - set the snapshot as route thumbnail and save the new route for this user to firebase via viewmodel

        // TODO (optional) give user the option to show the navigation ui with simulated route progress ??
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

        setupMapUI()

        val style = preferencesManager.getCurrentMapStyle()
        setMapStyle(style)
    }

    private fun setupMapUI() {
        // restrict the camera to a given bounding box as the app focuses only on the uni campus
        map.setLatLngBoundsForCameraTarget(latLngBounds)

        // don't allow the user to tilt the map because it may confuse some users and
        // doesn't provide any value here
        map.uiSettings.isTiltGesturesEnabled = false

        // move the compass to the bottom left corner of the mapView so it doesn't overlap with buttons
        map.uiSettings.compassGravity = Gravity.BOTTOM or Gravity.START
        map.uiSettings.setCompassMargins(compassMarginLeft, 0, 0, compassMarginBottom)

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

            mapMatchingClient.setMapMatchingListener(this)

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
        val markerCoords = allActiveMarkers?.map { it.markerPosition }
        markerManager.addMarkers(markerCoords)
    }

    private fun setupRouteLineManager(mapStyle: Style) {
        routeLineManager = get { parametersOf(mapView, map, mapStyle) }
        routeLineManager?.let { viewLifecycleOwner.lifecycle.addObserver(it) }
    }

    /**
     * * Map Matching Callbacks
     */

    override fun onRouteMatched(allMatchings: MutableList<MapMatchingMatching>) {
        showSnackbar(
            requireActivity(),
            R.string.map_matching_succeeded
        )
        val bestMatching = allMatchings[0]

        Timber.d("Confidence of best match: ${bestMatching.confidence().times(100)} %")
        // Timber.d("First route part: ${bestMatching.legs()?.get(0)}")

        // draw the best route match on the map
        showMapMatchedRoute(bestMatching)

        // convert map matching to a route that can be processed by the mapbox navigation api
        val route = bestMatching.toDirectionRoute()
        if (route != null) {
            directionsRoute = route
            // mapboxNavigation?.setRoutes(listOf(route))

            binding.startNavigationButton.visibility = View.VISIBLE
        } else {
            binding.startNavigationButton.visibility = View.GONE
        }
    }

    // TODO this drawn route should be saved and redrawn on config change as well to prevent another
    //  map matching request!
    private fun showMapMatchedRoute(matchedRoute: MapMatchingMatching) {
        val routeGeometry = matchedRoute.geometry() ?: return
        val lineString = LineString.fromPolyline(routeGeometry, PRECISION_6)
        routeLineManager?.addLineToMap(lineString)
    }

    override fun onNoRouteMatchings() {
        showSnackbar(
            requireActivity(),
            R.string.map_matching_failed,
            colorRes = R.color.colorError
        )
    }

    override fun onRouteMatchingFailed(message: String) {
        Timber.e("Route map matching failed because: $message")
    }

    /**
     * * Map Listeners
     */

    private fun setupMapListeners() {
        map.addOnCameraIdleListener(this::onCameraMoved)
        // map.addOnMapLongClickListener(this::onMapLongClicked)
        map.setOnInfoWindowClickListener {
            // TODO implement info windows ?
            false
        }
    }

    private fun removeMapListeners() {
        if (this::map.isInitialized) {
            map.removeOnCameraIdleListener(this::onCameraMoved)
            // map.removeOnMapLongClickListener(this::onMapLongClicked)
            routeCreationMapClickListenerBehavior?.let { map.removeOnMapClickListener(it) }
        }
    }

    private fun onCameraMoved() {
        // keep track of the current camera position in case of a configuration change or similar
        mapViewModel.setCurrentCameraPosition(map.cameraPosition)
    }

    // TODO this is not really needed anymore:
    private fun onMapLongClicked(point: LatLng): Boolean {
        Timber.d("in on Long click on map")

        // add a symbol to the long-clicked point
        val marker = markerManager.addMarker(point)

        if (marker != null) {
            // save the marker in the viewmodel
            mapViewModel.addNewMapMarker(marker)
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
                    colorRes = R.color.colorWarning
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
                colorRes = R.color.colorWarning
            )
        }
    }

    private fun onLocationPermissionExplanation() {
        showSnackbar(
            requireActivity(),
            R.string.location_permission_explanation,
            binding.mapButtonContainer,
            Snackbar.LENGTH_LONG,
            colorRes = R.color.themeColor
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
        Timber.d("in MapFragment onDestroyView")

        slidingBottomPanel.removePanelSlideListener(slidingPanelListener)
        mapMatchingClient.setMapMatchingListener(null)
        removeMapListeners()
        backPressedCallback?.remove()

        mapView?.onDestroy()
        super.onDestroyView()
    }

    companion object {
        private const val ROUTE_MARKER_SOURCE_ID = "ROUTE_MARKER_SOURCE_ID"
        const val selectedMarkerZoom = 17.0

        // custom margins of the mapbox compass
        private const val compassMarginLeft = 10
        private const val compassMarginBottom = 100

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
