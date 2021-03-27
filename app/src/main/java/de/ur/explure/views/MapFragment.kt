package de.ur.explure.views

import android.graphics.RectF
import android.location.Location
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
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
import com.crazylegend.viewbinding.viewBinding
import com.daimajia.androidanimations.library.Techniques
import com.daimajia.androidanimations.library.YoYo
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.matching.v5.models.MapMatchingMatching
import com.mapbox.core.constants.Constants.PRECISION_6
import com.mapbox.geojson.Feature
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.location.LocationUpdate
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
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
    MapMatchingClient.MapMatchingListener, RouteLineManager.OnRouteDrawListener {

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
    private var permissionExplanationSnackbar: Snackbar? = null

    private var backPressedCallback: OnBackPressedCallback? = null

    private lateinit var slidingBottomPanel: SlidingUpPanelLayout
    private lateinit var slidingPanelListener: SlidingUpPanelLayout.PanelSlideListener

    // action menu items
    private var cancelRouteCreationButton: MenuItem? = null
    private var showMapMatchingButton: MenuItem? = null
    private var saveRouteButton: MenuItem? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

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
            enabled = false // set the observer but don't enable it yet!
        ) {
            // show an alert dialog on the back button event
            showLeaveRouteCreationDialog()
        }
    }

    private fun setupViewModelObservers() {
        mapViewModel.mapReady.observe(viewLifecycleOwner, EventObserver {
            // this should only get called if the event has never been handled before because of the EventObserver
            Timber.d("Map has finished loading and can be used now!")
            setupInitialUIState()
        })
        mapViewModel.inRouteCreationMode.observe(viewLifecycleOwner) { inRouteCreationMode ->
            if (inRouteCreationMode) {
                // enable the observer on the back button and show some menu actions
                backPressedCallback?.isEnabled = true
                setMenuItemsVisibility(true)

                // also show a tutorial for the menu actions the first time the user creates a route
                if (preferencesManager.isFirstTimeRouteCreation()) {
                    TutorialBuilder.highlightMapActionMenu(
                        requireActivity(),
                        requireActivity().findViewById(R.id.toolbar),
                        Highlight(
                            requireActivity().findViewById(R.id.saveRouteButton),
                            title = getString(R.string.action_menu_tutorial_title),
                            description = getString(R.string.action_menu_tutorial_description),
                            radius = Highlight.HIGHLIGHT_RADIUS_LARGE
                        )
                    )
                    preferencesManager.completedRouteCreationTutorial()
                }
            } else {
                // disable the observer on the back button and hide the menu actions
                backPressedCallback?.isEnabled = false
                setMenuItemsVisibility(false)
            }
        }
        mapViewModel.manualRouteCreationModeActive.observe(viewLifecycleOwner) { active ->
            if (active) {
                enterManualRouteCreationMode()
            } else {
                exitManualRouteCreationMode()
            }
        }
        mapViewModel.routeDrawModeActive.observe(viewLifecycleOwner) { active ->
            if (active) {
                enterRouteDrawMode()
            } else {
                exitRouteDrawMode()
            }
        }
        mapViewModel.selectedMarker.observe(viewLifecycleOwner) { marker ->
            // move the camera to the selected marker
            if (::map.isInitialized) {
                map.moveCameraToPosition(marker.markerPosition, selectedMarkerZoom)
            }
        }
        mapViewModel.deletedWaypoint.observe(viewLifecycleOwner) { mapMarker ->
            // check to prevent crashes on config change as marker manager is not setup initially
            if (::markerManager.isInitialized) {
                // delete the corresponding marker symbol and remove from waypointscontroller
                waypointsController.remove(mapMarker.markerPosition.toPoint())
                markerManager.removeWaypointMarker(mapMarker)
            }
        }
        /*
        mapViewModel.mapMarkers.observe(viewLifecycleOwner) { markers ->
            if (markers.size > 0) {
                slidingBottomPanel.panelState = SlidingUpPanelLayout.PanelState.COLLAPSED
            }
        }*/
    }

    private fun setMenuItemsVisibility(visible: Boolean) {
        // show the menu items if in route creation mode and hide them if not
        cancelRouteCreationButton?.isVisible = visible
        showMapMatchingButton?.isVisible = visible
        saveRouteButton?.isVisible = visible
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
                    // workaround for visual bug that occurs when setting to collapsed while hiding
                    // the bottom navigation which would move this panel higher than it should!
                    slidingBottomPanel.panelState = SlidingUpPanelLayout.PanelState.COLLAPSED
                }
            }
        }
        slidingBottomPanel.addPanelSlideListener(slidingPanelListener)
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

        binding.routeCreationOptionsLayout.addMarkerButton.setOnClickListener {
            highlightCurrentRouteCreationMode(ManualRouteCreationModes.MODE_ADD)
            mapViewModel.setActiveManualRouteCreationMode(ManualRouteCreationModes.MODE_ADD)

            // (re-)set marker manager click listener behavior
            markerManager.setDefaultMarkerClickListenerBehavior()
            // add markers on click
            setAddMarkerClickListenerBehavior()
        }
        binding.routeCreationOptionsLayout.editMarkerButton.setOnClickListener {
            highlightCurrentRouteCreationMode(ManualRouteCreationModes.MODE_EDIT)
            mapViewModel.setActiveManualRouteCreationMode(ManualRouteCreationModes.MODE_EDIT)

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
            mapViewModel.setActiveManualRouteCreationMode(ManualRouteCreationModes.MODE_DELETE)

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
    }

    /**
     * Set the state in the viewModel to trigger certain corresponding actions and setup the options
     * panel and it's buttons for the route draw mode.
     */
    private fun setupRouteDrawMode() {
        mapViewModel.setRouteDrawModeStatus(isActive = true)

        binding.routeDrawOptionsLayout.drawRouteButton.setOnClickListener {
            highlightCurrentRouteCreationMode(RouteDrawModes.MODE_DRAW)
            mapViewModel.setActiveRouteDrawMode(RouteDrawModes.MODE_DRAW)

            // reset click listener behavior if it was set and enable drawing
            routeCreationMapClickListenerBehavior?.let { map.removeOnMapClickListener(it) }
            routeLineManager?.enableMapDrawing()
        }
        binding.routeDrawOptionsLayout.moveMapButton.setOnClickListener {
            highlightCurrentRouteCreationMode(RouteDrawModes.MODE_MOVE)
            mapViewModel.setActiveRouteDrawMode(RouteDrawModes.MODE_MOVE)

            // reset click listener behavior if it was set and enable map movement
            routeCreationMapClickListenerBehavior?.let { map.removeOnMapClickListener(it) }
            routeLineManager?.enableMapMovement()
        }
        binding.routeDrawOptionsLayout.deleteRouteButton.setOnClickListener {
            highlightCurrentRouteCreationMode(RouteDrawModes.MODE_DELETE)
            mapViewModel.setActiveRouteDrawMode(RouteDrawModes.MODE_DELETE)

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
                mapViewModel.exitCurrentRouteCreationMode()
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
        // hide the start route building button
        binding.buildRouteButton.isEnabled = false
        binding.buildRouteButton.visibility = View.GONE
        YoYo.with(Techniques.FadeOut)
            .duration(ANIMATION_DURATION)
            .playOn(binding.buildRouteButton)
    }

    private fun performSharedCreationExitActions() {
        // show the start route building button with an animation
        binding.buildRouteButton.visibility = View.VISIBLE
        binding.buildRouteButton.isEnabled = true
        YoYo.with(Techniques.FadeIn)
            .duration(ANIMATION_DURATION)
            .playOn(binding.buildRouteButton)

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

        val currentRouteCreationMode =
            mapViewModel.getActiveManualRouteCreationMode() ?: ManualRouteCreationModes.MODE_ADD
        highlightCurrentRouteCreationMode(currentRouteCreationMode)

        when (currentRouteCreationMode) {
            // TODO call the exact same methods as in the onClickListeners! (extract them before!)
            ManualRouteCreationModes.MODE_ADD -> {
                setAddMarkerClickListenerBehavior()
            }
            ManualRouteCreationModes.MODE_EDIT -> {
                Toast.makeText(requireContext(), "Noch nicht implementiert", Toast.LENGTH_SHORT)
                    .show()
            }
            ManualRouteCreationModes.MODE_DELETE -> {
                if (!::markerManager.isInitialized) return

                markerManager.setDeleteMarkerClickListenerBehavior(onMarkerDeleted = {
                    // remove this marker from waypoints controller and from viewModel
                    waypointsController.remove(it.geometry)
                    mapViewModel.removeMarker(it)
                })
            }
        }
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

        val currentRouteDrawMode = mapViewModel.getActiveRouteDrawMode() ?: RouteDrawModes.MODE_DRAW
        highlightCurrentRouteCreationMode(currentRouteDrawMode)
        when (currentRouteDrawMode) {
            RouteDrawModes.MODE_DRAW -> {
                routeLineManager?.enableMapDrawing()
            }
            RouteDrawModes.MODE_MOVE -> {
                routeLineManager?.enableMapMovement()
            }
            RouteDrawModes.MODE_DELETE -> {
                setRemoveRouteClickListenerBehavior()
            }
        }

        // redraw route lines that were saved in the viewModel's saved state if any
        // (needs to be done after the free draw mode has be (re-)inited!
        recreateRouteLines()
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
                // update viewModel list
                mapViewModel.removeDrawnLine(clickedFeature)
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
        if (routeLineManager != null) {
            routeLineManager?.clearAllLines()
            mapViewModel.resetActiveDrawnLines()
            mapViewModel.removeActiveMapMatching()
        }

        if (::markerManager.isInitialized) {
            // clear markers on the map and in the viewmodel
            markerManager.deleteAllMarkers()
            mapViewModel.removeActiveMarkers()
        }

        // reset the waypointsController
        waypointsController.clear()
    }

    private fun mapMatchDrawnRoute() {
        val allRoutePoints = routeLineManager?.getCompleteRoute()

        // TODO: these points should probably be added as waypoints ??
        //  (even though they are not really the waypoints needed later! These have to be added separately later)
        //  -> this would also allow us to delete them with the deleted route part !!!
        //  -> in this case they would need to be saved in a map with the route part as the key to delete them
        if (allRoutePoints != null) {
            // add markers to the waypoints of the route
            allRoutePoints.forEach {
                // TODO should these be show at all to the user ?
                markerManager.addMarker(it.toLatLng())
            }
            makeMatchingRequest(allRoutePoints)

            // TODO if the request failed ask user if he wants to try again and edit the route
            //  if we get a map matched route ask the user which one he wants to save (map matched or his own)
        }
    }

    private fun makeMatchingRequest(points: List<Point>) {
        // some sanity checks before trying to request a map matched route from the mapbox api
        if (points.size < 2) {
            // we need at least two points to get a successful match!
            Timber.e("Map Matching not possible! At least two coordinates are necessary!")
            showSnackbar(
                getString(R.string.too_few_waypoints),
                binding.mapButtonContainer,
                colorRes = R.color.colorError,
                length = Snackbar.LENGTH_LONG
            )
            return
        } else if (points.size > 100) {
            // the api also doesn't accept requests with more than 100 coordinates
            Timber.e("Map Matching not possible! There can be no more than 100 coordinates!")
            showSnackbar(
                getString(R.string.too_many_waypoints),
                binding.mapButtonContainer,
                colorRes = R.color.colorError,
                length = Snackbar.LENGTH_LONG
            )
            return
        }

        // check if the user has an internet connection
        if (hasInternetConnection(requireContext(), R.string.no_internet_map_matching)) {
            // show a small progressbar at the top to provide some visual feedback for the user!
            binding.progressBar.visibility = View.VISIBLE

            // make a request to the Mapbox Map Matching API to get a route from the waypoints
            mapMatchingClient.requestMapMatchedRoute(points)
        }
    }

    private fun confirmSavingRoute() {
        with(MaterialAlertDialogBuilder(requireActivity())) {
            setTitle(R.string.save_created_route_confirmation)
            setPositiveButton(R.string.yes) { _, _ ->
                mapViewModel.exitCurrentRouteCreationMode()
                saveCreatedRoute()
            }
            setNegativeButton(R.string.continue_edit) { _, _ -> }
            show()
        }
    }

    private fun saveCreatedRoute() {
        Toast.makeText(requireContext(), "Speichern ist noch nicht möglich :(", Toast.LENGTH_SHORT)
            .show()
        // TODO:
        // - make a snapshot of the created route with the Mapbox Snapshotter and save it to firebase storage
        // - set the snapshot as route thumbnail and save the new route for this user to firebase via viewmodel

        // TODO get length and duration of route either via mapMatching or via turf or own calculations!
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
                binding.ownLocationButton,
                title = getString(R.string.location_tracking_button_tutorial_title),
                description = getString(R.string.location_tracking_button_tutorial_description)
            ), Highlight(
                binding.changeStyleButton,
                title = getString(R.string.map_style_button_tutorial_title),
                description = getString(R.string.map_style_button_tutorial_description)
            ), Highlight(
                binding.buildRouteButton,
                title = getString(R.string.build_route_tutorial_title),
                description = getString(R.string.build_route_tutorial_description),
                radius = Highlight.HIGHLIGHT_RADIUS_LARGE
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
            recreateMapMatching()

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
        waypointsController.addAll(markerCoords)
    }

    private fun setupRouteLineManager(mapStyle: Style) {
        routeLineManager = get { parametersOf(mapView, map, mapStyle) }
        routeLineManager?.let { viewLifecycleOwner.lifecycle.addObserver(it) }
        routeLineManager?.setRouteDrawListener(this)
    }

    override fun onNewRouteDrawn(lineFeature: Feature) {
        mapViewModel.addActiveLine(lineFeature)
    }

    private fun recreateRouteLines() {
        // recreate all lines that were drawn on the map before the config change or process death
        val allActiveLines = mapViewModel.getActiveDrawnLines()
        // val allActiveRoutePoints = mapViewModel.getActiveRoutePoints()
        if (allActiveLines != null) {
            routeLineManager?.redrawActiveRoutes(allActiveLines)
        }
    }

    private fun recreateMapMatching() {
        // recreate the last map matching if there is one
        val activeMapMatching = mapViewModel.getActiveMapMatching()
        activeMapMatching?.let {
            routeLineManager?.addLineToMap(it)
        }
    }

    /**
     * * Map Matching Callbacks
     */

    /**
     * Called when the map matching api successfully matched a route on the campus.
     */
    override fun onRouteMatched(allMatchings: MutableList<MapMatchingMatching>) {
        binding.progressBar.visibility = View.GONE
        showSnackbar(
            requireActivity(),
            R.string.map_matching_succeeded,
            binding.mapButtonContainer
        )
        val bestMatching = allMatchings[0]

        Timber.d("Confidence of best match: ${bestMatching.confidence().times(100)} %")
        // Timber.d("First route part: ${bestMatching.legs()?.get(0)}")

        // draw the best route match on the map
        showMapMatchedRoute(bestMatching)

        // convert map matching to a route that can be processed by the mapbox navigation api
        directionsRoute = bestMatching.toDirectionRoute()
    }

    private fun showMapMatchedRoute(matchedRoute: MapMatchingMatching) {
        val routeGeometry = matchedRoute.geometry() ?: return
        val lineString = LineString.fromPolyline(routeGeometry, PRECISION_6)
        routeLineManager?.addLineToMap(lineString)
        // save in viewmodel
        mapViewModel.setActiveMapMatching(lineString)
    }

    /**
     * Called when the map matching api couldn't get a suitable route on the campus.
     */
    override fun onNoRouteMatchings() {
        binding.progressBar.visibility = View.GONE
        showSnackbar(
            requireActivity(),
            R.string.map_matching_failed,
            binding.mapButtonContainer,
            colorRes = R.color.colorError,
            length = Snackbar.LENGTH_LONG
        )
    }

    /**
     * Called when the map matching request failed due to some error.
     */
    override fun onRouteMatchingFailed(message: String) {
        binding.progressBar.visibility = View.GONE
        Timber.e("Route map matching failed because: $message")
        showSnackbar(
            requireActivity(),
            R.string.map_matching_request_error,
            binding.mapButtonContainer,
            colorRes = R.color.colorError,
            length = Snackbar.LENGTH_LONG
        )
    }

    /**
     * * Map Listeners
     */

    private fun setupMapListeners() {
        map.addOnCameraIdleListener(this::onCameraMoved)
        map.setOnInfoWindowClickListener {
            // TODO implement info windows ?
            false
        }
    }

    private fun removeMapListeners() {
        if (this::map.isInitialized) {
            map.removeOnCameraIdleListener(this::onCameraMoved)
            routeCreationMapClickListenerBehavior?.let { map.removeOnMapClickListener(it) }
        }
    }

    private fun onCameraMoved() {
        // keep track of the current camera position in case of a configuration change or similar
        mapViewModel.setCurrentCameraPosition(map.cameraPosition)
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
            // dismiss the permission explanation
            permissionExplanationSnackbar?.dismiss()
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
        permissionExplanationSnackbar = showSnackbar(
            requireActivity(),
            R.string.location_permission_explanation,
            binding.mapButtonContainer,
            Snackbar.LENGTH_INDEFINITE,
            colorRes = R.color.themeColor
        )
    }

    private fun onNewLocationReceived(location: Location) {
        // Pass the new location to the Maps SDK's LocationComponent
        val locationUpdate = LocationUpdate.Builder().location(location).build()
        map.locationComponent.forceLocationUpdate(locationUpdate)
        // save the new location
        mapViewModel.setCurrentUserPosition(location)
    }

    private fun prepareMapMatching() {
        if (mapViewModel.manualRouteCreationModeActive.value == true) {
            val wayPoints = waypointsController.getAllWaypoints()
            // TODO the polylineUtils could be used on these points as well!
            //  -> maybe this would improve map matching here ??
            makeMatchingRequest(wayPoints)
        } else if (mapViewModel.routeDrawModeActive.value == true) {
            mapMatchDrawnRoute()
        }
    }

    private fun explainMapMatchingFunctionality() {
        // TODO showing an info button somewhere to re-trigger this explanation would probably improve
        //  the user experience!
        with(MaterialAlertDialogBuilder(requireActivity())) {
            setMessage(R.string.map_matching_explanation_confirmation)
            setPositiveButton(R.string.yes) { _, _ ->
                // change the dialog to show an explanation for the map matching functionality
                setTitle(R.string.map_matching_explanation_title)
                setMessage(R.string.map_matching_explanation_message)
                setPositiveButton(R.string.got_it) { _, _ ->
                    prepareMapMatching()
                }
                setNegativeButton(null, null)
                show()
            }
            setNegativeButton(R.string.no) { _, _ ->
                prepareMapMatching()
            }
            show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_menu, menu)

        cancelRouteCreationButton = menu.findItem(R.id.cancelRouteCreationButton)
        showMapMatchingButton = menu.findItem(R.id.showMapMatchedButton)
        saveRouteButton = menu.findItem(R.id.saveRouteButton)
        // startNavigationButton = menu.findItem(R.id.startNavigationButton)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.cancelRouteCreationButton -> {
                showLeaveRouteCreationDialog()
                true
            }
            R.id.showMapMatchedButton -> {
                if (preferencesManager.isFirstTimeMapMatching()) {
                    explainMapMatchingFunctionality()
                    preferencesManager.finishedMapMatchingExplanation()
                } else {
                    prepareMapMatching()
                }
                true
            }
            R.id.saveRouteButton -> {
                confirmSavingRoute()
                true
            }
            /*
            R.id.startNavigationButton -> {
                // convert directionsRoute to json so it can be passed as a string via safe args
                val routeJson = directionsRoute?.toJson() ?: return false
                val action = MapFragmentDirections.actionMapFragmentToNavigationFragment(
                    route = routeJson
                )
                findNavController().navigate(action)
                true
            }*/
            else -> super.onOptionsItemSelected(item)
        }
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
        mapViewModel.saveActiveDrawnLines()
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
        const val selectedMarkerZoom = 17.0

        private const val ANIMATION_DURATION = 500L // in ms

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
        /*
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
        )*/
    }
}
