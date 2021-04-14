@file:Suppress("MagicNumber")
package de.ur.explure.views

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.location.Location
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.appcompat.widget.AppCompatImageButton
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.crazylegend.viewbinding.viewBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.mapboxsdk.location.LocationUpdate
import com.mapbox.mapboxsdk.location.OnCameraTrackingChangedListener
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.Symbol
import com.mapbox.navigation.base.internal.extensions.applyDefaultParams
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.arrival.ArrivalController
import com.mapbox.navigation.core.arrival.ArrivalObserver
import com.mapbox.navigation.core.arrival.ArrivalOptions
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper
import com.mapbox.navigation.core.trip.session.BannerInstructionsObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.ui.camera.DynamicCamera
import com.mapbox.navigation.ui.camera.NavigationCamera
import com.mapbox.navigation.ui.map.NavigationMapboxMap
import com.mapbox.navigation.ui.map.OnWayNameChangedListener
import com.mapbox.navigation.ui.route.NavigationMapRoute
import com.mapbox.navigation.ui.summary.SummaryBottomSheet
import de.ur.explure.R
import de.ur.explure.databinding.FragmentNavigationBinding
import de.ur.explure.extensions.toLatLng
import de.ur.explure.map.LocationManager
import de.ur.explure.map.MapHelper
import de.ur.explure.map.MarkerManager
import de.ur.explure.map.PermissionHelper
import de.ur.explure.model.route.Route
import de.ur.explure.model.waypoint.WayPoint
import de.ur.explure.utils.EventObserver
import de.ur.explure.utils.SharedPreferencesManager
import de.ur.explure.utils.getMapboxAccessToken
import de.ur.explure.utils.getRouteFromBundle
import de.ur.explure.utils.isGPSEnabled
import de.ur.explure.utils.showSnackbar
import de.ur.explure.viewmodel.NavigationViewModel
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.androidx.viewmodel.scope.emptyState
import org.koin.core.parameter.parametersOf
import timber.log.Timber
import java.util.*

/**
 * TODO show waypoint markers only if the user is near them!! otherwise we would probably the navigation completely!
 */

/**
 * TODO allow user to move the camera more freely!
 */

/**
 * Many parts of this class have been taken and slightly adjusted from this Mapbox Github Example:
 * https://github.com/mapbox/mapbox-navigation-android/blob/afdd8587b684cf7b82f44288cc2063444d96cfe5/examples/src/main/java/com/mapbox/navigation/examples/core/BasicNavigationFragment.kt
 */

@Suppress("TooManyFunctions")
class NavigationFragment : Fragment(R.layout.fragment_navigation), MapHelper.MapHelperListener,
    OnWayNameChangedListener {

    private val binding by viewBinding(FragmentNavigationBinding::bind)
    private val navigationViewModel: NavigationViewModel by viewModel(state = emptyState())

    private var backPressedCallback: OnBackPressedCallback? = null

    // arguments
    private val args: NavigationFragmentArgs by navArgs()
    private lateinit var route: Route
    private lateinit var routeCoordinates: List<Point>
    private lateinit var routeWayPoints: LinkedList<WayPoint>

    // map
    private var mapView: MapView? = null
    private lateinit var mapHelper: MapHelper
    private var routeDestinationMarker: Symbol? = null

    // navigation
    private lateinit var mapboxNavigation: MapboxNavigation
    private var navigationMapboxMap: NavigationMapboxMap? = null
    private var navigationMapRoute: NavigationMapRoute? = null
    private var navIntroSnackbar: Snackbar? = null
    private lateinit var summaryBehavior: BottomSheetBehavior<SummaryBottomSheet>
    private lateinit var cancelBtn: AppCompatImageButton
    private lateinit var routeOverviewButton: ImageButton

    // for testing routes
    private val mapboxReplayer = MapboxReplayer()
    private val replayProgressObserver = ReplayProgressObserver(mapboxReplayer)
    private val replayLocationEngine = ReplayLocationEngine(mapboxReplayer)
    private val replayRouteMapper = ReplayRouteMapper()
    private var shouldSimulateRoute: Boolean = true // TODO for debugging and demo only

    // SharedPrefs
    private val preferencesManager: SharedPreferencesManager by inject()

    // location tracking
    private lateinit var locationManager: LocationManager
    private var gpsWarning: Snackbar? = null

    // permission handling
    private val permissionHelper: PermissionHelper by inject()
    private var permissionExplanationSnackbar: Snackbar? = null

    // navigation observers
    private val routeProgressObserver = object : RouteProgressObserver {
        override fun onRouteProgressChanged(routeProgress: RouteProgress) {
            binding.instructionView.updateDistanceWith(routeProgress)
            binding.summaryBottomSheet.update(routeProgress)

            // TODO
            /*
            val routeOptions = routeProgress.route.routeOptions()
            val waypoints = routeOptions?.coordinates()?.size ?: 0
            val silentWaypoints = waypoints - (routeOptions?.waypointIndicesList()?.size ?: 0)
            val remainingWaypoints = routeProgress.remainingWaypoints
            tvDebugInfo.text = "Waypoints: $waypoints\nSilent waypoints: $silentWaypoints\n" +
                    "RemainingWaypoints: $remainingWaypoints"
            */
        }
    }

    private val bannerInstructionObserver = object : BannerInstructionsObserver {
        override fun onNewBannerInstructions(bannerInstructions: BannerInstructions) {
            binding.instructionView.updateBannerInstructionsWith(bannerInstructions)
        }
    }

    private val tripSessionStateObserver = object : TripSessionStateObserver {
        override fun onSessionStateChanged(tripSessionState: TripSessionState) {
            when (tripSessionState) {
                TripSessionState.STARTED -> {
                    updateNavigationViews(TripSessionState.STARTED)

                    navigationMapboxMap?.addOnWayNameChangedListener(this@NavigationFragment)
                    navigationMapboxMap?.updateWaynameQueryMap(true)
                }

                TripSessionState.STOPPED -> {
                    updateNavigationViews(TripSessionState.STOPPED)

                    if (mapboxNavigation.getRoutes().isNotEmpty()) {
                        navigationMapboxMap?.hideRoute()
                    }

                    navigationMapboxMap?.removeOnWayNameChangedListener(this@NavigationFragment)
                    navigationMapboxMap?.updateWaynameQueryMap(false)

                    updateCameraOnNavigationStateChange(false)
                }
            }
        }
    }

    private val cameraTrackingChangedListener = object : OnCameraTrackingChangedListener {
        override fun onCameraTrackingChanged(currentMode: Int) {
            // not needed
        }

        override fun onCameraTrackingDismissed() {
            if (mapboxNavigation.getTripSessionState() == TripSessionState.STARTED) {
                summaryBehavior.isHideable = true
                summaryBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                hideWayNameView()
            }
        }
    }

    /**
     * Show and create experiences when arriving at waypoints and destinations.
     */
    private val arrivalController = object : ArrivalController {
        val arrivalOptions = ArrivalOptions.Builder()
            // .arrivalInSeconds(10.0)
            .arrivalInMeters(5.0)
            .build()

        override fun arrivalOptions(): ArrivalOptions = arrivalOptions

        override fun navigateNextRouteLeg(routeLegProgress: RouteLegProgress): Boolean {
            // Move to the next step when the distance is small
            Toast.makeText(
                requireActivity(),
                "navigateNextRouteLeg: ${routeLegProgress.upcomingStep?.name()}",
                Toast.LENGTH_SHORT
            ).show()
            return routeLegProgress.distanceRemaining < 3.0
        }
    }

    private val arrivalObserver = object : ArrivalObserver {
        /**
         * Called once the driver has arrived at a stop and has started navigating the next leg.
         * NavigateNextRouteLeg will also be true when this happens
         */
        override fun onNextRouteLegStart(routeLegProgress: RouteLegProgress) {
            Toast.makeText(
                requireActivity(),
                "onNextRouteLegStart: ${routeLegProgress.distanceRemaining}",
                Toast.LENGTH_SHORT
            ).show()
        }

        /**
         * Called once the driver has reached the final destination at the end of the route.
         * RouteProgress.currentState() will equal RouteProgressState.ROUTE_ARRIVED
         */
        override fun onFinalDestinationArrival(routeProgress: RouteProgress) {
            showSnackbar(
                requireActivity(),
                "Gut gemacht! Du hast das Ziel der Route erreicht!",
                colorRes = R.color.themeColor
            )

            // TODO show dialog fragment and give user the option to rate this route!
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // lock the screen rotation, sry :(
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_NOSENSOR

        setHasOptionsMenu(true)

        // TODO for testing only:
        routeCoordinates = listOf<Point>(
            Point.fromLngLat(12.091897088615497, 48.9986755276432),
            Point.fromLngLat(12.093398779683042, 48.99790078797133),
            Point.fromLngLat(12.095176764949287, 48.99759573694382),
            Point.fromLngLat(12.095045596693524, 48.99696500867813),
            Point.fromLngLat(12.092009249797059, 48.996774307308414),
            Point.fromLngLat(12.091600540864277, 48.99278790637206),
        )

        route = args.route
        // val routeCoordinates = route. // TODO
        routeWayPoints = route.wayPoints

        setupViewModelObservers()
        setupBackButtonClickObserver()

        // init map
        mapView = binding.mapView
        mapView?.onCreate(savedInstanceState)
        mapHelper = get { parametersOf(mapView, viewLifecycleOwner.lifecycle) }
        viewLifecycleOwner.lifecycle.addObserver(mapHelper)
        mapHelper.setMapHelperListener(this)

        // init locationManager and sync with fragment lifecycle
        locationManager = get { parametersOf(this::onNewLocationReceived) }
        viewLifecycleOwner.lifecycle.addObserver(locationManager)

        // init navigation components
        val recreated = savedInstanceState != null
        setupMapboxNavigation(recreated)
        setupInitialUI()

        // create a navigable route
        generateRoute() // TODO generate route erst in startNavigation onClickListener?
    }

    private fun setupBackButtonClickObserver() {
        // This callback will show an alert dialog when the back button is pressed
        backPressedCallback = activity?.onBackPressedDispatcher?.addCallback(
            viewLifecycleOwner,
            enabled = false // not enabled at the start!
        ) {
            showCancelNavigationWarning()
        }
    }

    private fun setupViewModelObservers() {
        navigationViewModel.inNavigationMode.observe(viewLifecycleOwner, EventObserver { active ->
            if (active) {
                backPressedCallback?.isEnabled = true

                if (shouldSimulateRoute) {
                    setupNavUI()
                    startSimulation()
                } else {
                    prepareNavigation()
                }
            } else {
                backPressedCallback?.isEnabled = false
                setupInitialUI()
            }
        })
        navigationViewModel.locationPermissionGranted.observe(viewLifecycleOwner) { granted ->
            if (granted && navigationViewModel.inNavigationMode()) {
                setupNavUI()
                startNavigation()
            }
        }
        navigationViewModel.buildingExtrusionActive.observe(viewLifecycleOwner) { extrusionsActive ->
            mapHelper.buildingPlugin?.setVisibility(extrusionsActive)
        }
    }

    private fun setupMapboxNavigation(isRecreated: Boolean) {
        mapboxNavigation = if (isRecreated) {
            // fragment was recreated so retrieve the already existing mapboxNavigation instance
            MapboxNavigationProvider.retrieve()
        } else {
            val navigationOptions = MapboxNavigation.defaultNavigationOptionsBuilder(
                requireContext(), getMapboxAccessToken(requireContext())
            )
                .locationEngine(getLocationEngine())
                // .isRouteRefreshEnabled(false)
                .build()

            MapboxNavigationProvider.create(navigationOptions)
        }

        mapboxNavigation.apply {
            registerTripSessionStateObserver(tripSessionStateObserver)
            registerRouteProgressObserver(routeProgressObserver)
            registerBannerInstructionsObserver(bannerInstructionObserver)
            setArrivalController(arrivalController)
            registerArrivalObserver(arrivalObserver)
        }
        // disable off-route detection for now because we don't want new routes when leaving the route
        // see https://docs.mapbox.com/android/navigation/guides/off-route/
        // ! Could later be used to inform the user that he is no longer on the route
        mapboxNavigation.setRerouteController(null)
    }

    private fun setupInitialUI() {
        binding.startNavigationButton.visibility = View.VISIBLE
        binding.navigationProgressBar.visibility = View.VISIBLE

        navIntroSnackbar = showSnackbar(
            requireActivity(),
            R.string.navigation_intro,
            colorRes = R.color.colorInfo,
            length = Snackbar.LENGTH_INDEFINITE
        )

        binding.startNavigationButton.setOnClickListener {
            // check if we have a route first!
            if (navigationViewModel.directionsRoute != null) {
                navigationViewModel.enterNavigationMode()
            } else {
                // toast so it looks better in combination with the navigation intro snackbar
                Toast.makeText(
                    requireContext(),
                    R.string.route_still_loading,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setupNavUI() {
        navIntroSnackbar?.dismiss()
        binding.startNavigationButton.visibility = View.GONE

        binding.summaryBottomSheet.visibility = View.GONE
        summaryBehavior = BottomSheetBehavior.from(binding.summaryBottomSheet).apply {
            isHideable = false
            addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {

                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (summaryBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
                        binding.recenterBtn.show()
                    }
                }
                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    // not needed
                }
            })
        }

        routeOverviewButton = requireView().findViewById(R.id.routeOverviewBtn)
        routeOverviewButton.setOnClickListener {
            navigationMapboxMap?.showRouteGeometryOverview(buildRouteOverviewPadding())
            binding.recenterBtn.show()
        }

        cancelBtn = requireView().findViewById(R.id.cancelBtn)
        cancelBtn.setOnClickListener {
            showCancelNavigationWarning()
        }

        binding.recenterBtn.apply {
            hide()
            addOnClickListener {
                binding.recenterBtn.hide()
                summaryBehavior.isHideable = false
                summaryBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                showWayNameView()
                navigationMapboxMap?.resetPadding()
                navigationMapboxMap?.resetCameraPositionWith(NavigationCamera.NAVIGATION_TRACKING_MODE_GPS)
            }
        }

        binding.wayNameView.apply {
            visibility = View.GONE
        }

        // remove the initial route overlay after starting the route
        mapHelper.routeLineManager?.removeMapMatching()
        // routeDestinationMarker?.let { mapHelper.markerManager.deleteMarker(it) }

        // setup the waypoints
        // TODO jetzt schon anzeigen oder nur wenn in der Nähe?
        mapHelper.markerManager.setupNavigationWaypoints(routeWayPoints) { clickedPoint ->
            Toast.makeText(
                requireActivity(),
                "Hier befindet sich der Wegpunkt: ${clickedPoint.title}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun buildRouteOverviewPadding(): IntArray {
        val leftRightPadding =
            resources.getDimension(com.mapbox.navigation.ui.R.dimen.mapbox_route_overview_left_right_padding)
                .toInt()
        val paddingBuffer =
            resources.getDimension(com.mapbox.navigation.ui.R.dimen.mapbox_route_overview_buffer_padding)
                .toInt()
        val instructionHeight = (resources
            .getDimension(com.mapbox.navigation.ui.R.dimen.mapbox_instruction_content_height) + paddingBuffer).toInt()
        val summaryHeight = resources
            .getDimension(com.mapbox.navigation.ui.R.dimen.mapbox_summary_bottom_sheet_height)
            .toInt()
        return intArrayOf(leftRightPadding, instructionHeight, leftRightPadding, summaryHeight)
    }

    private fun showCancelNavigationWarning() {
        mapboxReplayer.stop() // stop replaying while showing this dialog
        with(MaterialAlertDialogBuilder(requireActivity())) {
            setMessage(R.string.leave_navigation_warning)
            setCancelable(false)
            setPositiveButton(R.string.yes) { _, _ ->
                navigationViewModel.leaveNavigationMode()
                findNavController().navigateUp()
            }
            setNegativeButton(R.string.no) { _, _ ->
                mapboxReplayer.play() // start simulation again
            }
            show()
        }
    }

    override fun onMapLoaded(map: MapboxMap) {
        // adjust the compass position
        map.uiSettings.compassGravity = Gravity.BOTTOM or Gravity.START
        map.uiSettings.setCompassMargins(compassMarginLeft, 0, 0, compassMarginBottom)
    }

    override fun onMapStyleLoaded(mapStyle: Style) {
        // ! check if already initialized, otherwise it will crash because it adds sources twice
        if (navigationMapboxMap == null) {
            val mapview = mapView ?: return
            // setup navigation ui sdk (some parts of it are needed)
            navigationMapboxMap =
                NavigationMapboxMap.Builder(mapview, mapHelper.map, viewLifecycleOwner)
                    .withSourceTolerance(0.001f) // TODO play with different values !
                    .useSpecializedLocationLayer(true)
                    .vanishRouteLineEnabled(true)
                    .build().apply {
                        addProgressChangeListener(mapboxNavigation)
                        addOnCameraTrackingChangedListener(cameraTrackingChangedListener)
                        setCamera(DynamicCamera(mapHelper.map))
                        showAlternativeRoutes(false)
                    }
        }

        /*// ! check if already initialized, otherwise it will crash because it adds sources twice
        if (navigationMapRoute != null) {
            navigationMapRoute =
                NavigationMapRoute.Builder(mapview, mapHelper.map, viewLifecycleOwner)
                    .withVanishRouteLineEnabled(true)
                    .withStyle(styleRes)
                    // .withMapboxNavigation(mapboxNavigation)
                    .build()  // FIXME: Crash: Source mapbox-navigation-waypoint-source already exists

            // navigationMapRoute.addRoutes(TODO())
        }*/

        // add a destination marker to the end
        val lastRoutePoint = routeCoordinates.last()
        routeDestinationMarker = mapHelper.markerManager.addMarker(
            lastRoutePoint.toLatLng(),
            MarkerManager.DESTINATION_ICON
        )

        if (!navigationViewModel.inNavigationMode()) {
            // if not in navigation mode show the route on the map
            mapHelper.routeLineManager?.addLineToMap(routeCoordinates)
        }
    }

    private fun prepareNavigation() {
        // start location tracking and ask for permissions!
        mapHelper.map.style?.let { startLocationTracking(it) }

        // update the used location engine option
        // mapboxNavigation.navigationOptions.toBuilder().locationEngine(getLocationEngine())
    }

    @SuppressLint("MissingPermission")
    private fun startNavigation() {
        val route = navigationViewModel.directionsRoute ?: return
        // val directionRoute = mapboxNavigation.getRoutes().firstOrNull() ?: return

        updateCameraOnNavigationStateChange(true)
        navigationMapboxMap?.startCamera(route)
        mapboxNavigation.startTripSession()
    }

    // TODO das hier in onMapStyle, falls die directionsRoute schon existiert, aufrufen ?
    @SuppressLint("MissingPermission")
    private fun restoreNavigation() {
        navigationViewModel.directionsRoute?.let {
            mapboxNavigation.setRoutes(listOf(it))
            navigationMapboxMap?.addProgressChangeListener(mapboxNavigation)
            navigationMapboxMap?.startCamera(it)
            updateCameraOnNavigationStateChange(true)
            mapboxNavigation.startTripSession()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startSimulation() {
        val route = navigationViewModel.directionsRoute ?: return
        val startPoint = route.routeOptions()?.coordinates()?.first()

        mapboxReplayer.clearEvents()

        /*
        mapboxNavigation.registerRouteProgressObserver(ReplayProgressObserver(mapboxReplayer))
        mapboxReplayer.pushRealLocation(requireContext(), 0.0) // TODO startPoint stattdessen?
        */
        updateCameraOnNavigationStateChange(true)
        navigationMapboxMap?.startCamera(route)
        mapboxNavigation.startTripSession()

        mapboxReplayer.apply {
            val replayEvents = ReplayRouteMapper().mapDirectionsRouteGeometry(route)
            pushEvents(replayEvents)
            seekTo(replayEvents.first())
        }
        mapboxReplayer.playbackSpeed(2.5) // TODO schneller für demo setzen?
        mapboxReplayer.play()
    }

    private fun updateCameraOnNavigationStateChange(navigationStarted: Boolean) {
        navigationMapboxMap?.apply {
            if (navigationStarted) {
                // track user position during navigation
                updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_GPS)
                updateLocationLayerRenderMode(RenderMode.GPS)
            } else {
                // stop tracking user position if not in navigation mode
                updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_NONE)
                updateLocationLayerRenderMode(RenderMode.COMPASS) // TODO sometimes crashes here when leaving!
            }
        }
    }

    private fun updateNavigationViews(tripSessionState: TripSessionState) {
        when (tripSessionState) {
            TripSessionState.STARTED -> {
                binding.summaryBottomSheet.visibility = View.VISIBLE
                binding.recenterBtn.hide()
                binding.instructionView.visibility = View.VISIBLE
                // instructionSoundButton.show()
            }
            TripSessionState.STOPPED -> {
                binding.summaryBottomSheet.visibility = View.GONE
                binding.recenterBtn.hide()
                hideWayNameView()
                binding.instructionView.visibility = View.GONE
                // instructionSoundButton.hide()
            }
        }
    }

    override fun onWayNameChanged(wayName: String) {
        binding.wayNameView.updateWayNameText(wayName)
        if (summaryBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
            hideWayNameView()
        } else {
            showWayNameView()
        }
    }

    private fun showWayNameView() {
        binding.wayNameView.updateVisibility(binding.wayNameView.retrieveWayNameText().isNotEmpty())
    }

    private fun hideWayNameView() {
        binding.wayNameView.updateVisibility(false)
    }

    private fun generateRoute() {
        val coordinateList = cleanCoordinates()

        // sanity check to make sure the algorithm above does work as intended!
        if (coordinateList.size > 25) {
            // ! This SHOULD never happen in production!
            Timber.e("The direction route builder limit of 25 coordinates was exceeded!")
            showSnackbar(
                requireActivity(),
                R.string.direction_route_fetch_error,
                colorRes = R.color.colorError
            )
            return
        }

        // TODO vermutlich wäre das mapMatching ergebnis hier wichtig, um waypoints richtig zu erkennen ...
        val token = getMapboxAccessToken(requireActivity().applicationContext)
        val routeOptions = RouteOptions.builder()
            .applyDefaultParams()
            .accessToken(token)
            .coordinates(coordinateList)
            // .waypointTargetsList(...)
            .alternatives(false) // we don't need any alternative routes
            .steps(true)
            .bannerInstructions(true)
            .voiceInstructions(false)
            .profile(DirectionsCriteria.PROFILE_WALKING)
            .overview(DirectionsCriteria.OVERVIEW_FULL)
            .annotationsList(
                listOf(
                    DirectionsCriteria.ANNOTATION_DURATION,
                    DirectionsCriteria.ANNOTATION_DISTANCE,
                )
            )
            .build()

        /**
         * ! Calling requestRoutes() affects the TripSessionState. If you call startTripSession()
         * ! without calling requestRoutes(), you'll be in "Free Drive" state. If you call requestRoutes()
         * ! after startTripSession(), you'll be in "Active Guidance" state
         */
        mapboxNavigation.requestRoutes(
            routeOptions,
            object : RoutesRequestCallback {
                override fun onRoutesReady(routes: List<DirectionsRoute>) {
                    binding.navigationProgressBar.visibility = View.GONE
                    navigationViewModel.directionsRoute = routes[0]

                    navigationMapboxMap?.drawRoutes(routes) // TODO only for debugging! don't show this to user!
                    mapboxNavigation.setRoutes(routes)

                    // TODO navigationMapRoute?.addRoutes(routes)

                    // TODO hier schon starten?
                    /*
                    val mapper = ReplayRouteMapper()
                    val firstRouteGeometry = routes.firstOrNull()?.geometry() ?: return

                    mapboxReplayer.apply {
                        clearEvents()
                        pushEvents(mapper.mapGeometry(firstRouteGeometry))
                        playbackSpeed(10.0)
                        play()
                    }*/
                }

                override fun onRoutesRequestFailure(throwable: Throwable, routeOptions: RouteOptions) {
                    Timber.e("route request failure %s", throwable.toString())
                }

                override fun onRoutesRequestCanceled(routeOptions: RouteOptions) {
                    Timber.d("route request canceled")
                }
            }
        )
    }

    private fun cleanCoordinates(): MutableList<Point> {
        val startLocation = routeCoordinates.first()
        val destinationLocation = routeCoordinates.last()

        // ! the directionsRoute has a limit of 25 coordinates, so we have to make sure every passed
        // ! route doesn't exceed this limit!
        val coordinateList = mutableListOf<Point>()
        when {
            routeCoordinates.size <= 25 -> {
                // if we have 25 or less coordinates, everything's fine :)
                coordinateList.addAll(routeCoordinates)
            }
            // TODO in dem fall vllt nur random dazwischen genau die nötige anzahl darüber wegschmeißen?
            routeCoordinates.size in 26..50 -> {
                // if we have between 25 and 50 coords we can simply take the first and last point and
                // from the list between every second so we simply take half of the coordinates
                coordinateList.add(startLocation)
                routeCoordinates.subList(1, routeCoordinates.size - 2)
                    .forEachIndexed { index, point ->
                        if (index % 2 == 0) {
                            coordinateList.add(point)
                        }
                    }
                coordinateList.add(destinationLocation)
            }
            else -> {
                // if we have more than 50 coordinates we use the same algorithm as above but take every
                // 4th element instead because the maximum is 100 (because of the mapMatching - API - Limit
                // during route creation (so we take only a quarter of all coordinates)
                coordinateList.add(startLocation)
                /*val tooManyCount = routeCoordinates.size - 25
                for (i in 2..46 step 2) {
                    coordinateList.add(routeCoordinates[i])
                }*/
                for (i in 4..routeCoordinates.size - 2 step 4) {
                    coordinateList.add(routeCoordinates[i])
                }
                coordinateList.add(destinationLocation)
            }
        }

        var simplifiedCoords: MutableList<Point>? = null
        var simplifiedCoords2: MutableList<Point>? = null
        if (routeCoordinates.size > 25) {
            simplifiedCoords = PolylineUtils.simplify(routeCoordinates, 0.001, true)
            simplifiedCoords2 = PolylineUtils.simplify(routeCoordinates, 0.01, true)
        }

        return coordinateList
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
                    Snackbar.LENGTH_LONG,
                    colorRes = R.color.colorWarning
                )
                return
            }

            if (::locationManager.isInitialized) {
                navigationViewModel.setLocationTrackingStatus(isEnabled = true)
                navigationViewModel.setLocationPermissionStatus(permissionGiven = true)

                // enable location tracking with custom location engine
                locationManager.activateLocationComponent(
                    mapHelper.map.locationComponent, loadedMapStyle, useDefaultEngine = false
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
            permissionExplanationSnackbar?.dismiss()
            // permission is granted, so try to find the device location and enable location tracking
            mapHelper.map.style?.let { startLocationTracking(it) }
            navigationViewModel.setLocationPermissionStatus(permissionGiven = true)
        } else {
            navigationViewModel.setLocationPermissionStatus(permissionGiven = false)
            showSnackbar(
                requireActivity(),
                R.string.location_permission_not_given,
                Snackbar.LENGTH_LONG,
                colorRes = R.color.colorWarning
            )
        }
    }

    private fun onLocationPermissionExplanation() {
        permissionExplanationSnackbar = showSnackbar(
            requireActivity(),
            R.string.location_permission_explanation,
            Snackbar.LENGTH_INDEFINITE,
            colorRes = R.color.themeColor
        )
    }

    private fun onNewLocationReceived(location: Location) {
        // check if the user position is in the latLngBounds for the campus!
        if (!mapHelper.boundsContainPosition(location.toLatLng())) {
            if (gpsWarning == null || gpsWarning?.isShown == false) {
                gpsWarning = showSnackbar(
                    requireActivity(),
                    R.string.not_on_campus_error,
                    colorRes = R.color.colorWarning,
                    length = Snackbar.LENGTH_INDEFINITE
                )
            }
            return
        }
        gpsWarning?.dismiss() // we got a location in the correct bounds, dismiss warning if shown

        // Pass the new location to the Maps SDK's LocationComponent
        val locationUpdate = LocationUpdate.Builder().location(location).build()
        mapHelper.map.locationComponent.forceLocationUpdate(locationUpdate)

        // save the new location
        navigationViewModel.setCurrentUserPosition(location)
    }

    // If shouldSimulateRoute is true a ReplayRouteLocationEngine will be used which is intended
    // for testing else a real location engine is used.
    private fun getLocationEngine(): LocationEngine {
        return if (shouldSimulateRoute) {
            ReplayLocationEngine(mapboxReplayer)
        } else {
            locationManager.getCustomLocationEngine()
        }
    }

    /**
     * * Menu
     */

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_navigation, menu)
        menu.findItem(R.id.show3dBuildings).isChecked = preferencesManager.getBuildingExtrusionShown()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        @Suppress("UseIfInsteadOfWhen") // for further updates when is better!
        return when (item.itemId) {
            R.id.show3dBuildings -> {
                item.isChecked = !item.isChecked
                navigationViewModel.setBuildingExtrusionStatus(item.isChecked)
                preferencesManager.setBuildingExtrusionShown(item.isChecked)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Lifecycle
     */

    override fun onStop() {
        super.onStop()
        // TODO save current navigation state and progress!
        // -> bottom sheet and instructionView as well as waypointName! And user position!!
        //  -> is saving the camera position necessary too?
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
        navigationViewModel.directionsRoute?.let {
            outState.putString(ROUTE_BUNDLE_KEY, it.toJson())
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        if (savedInstanceState != null) {
            navigationViewModel.directionsRoute = getRouteFromBundle(savedInstanceState)
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // cleanup
        navigationMapboxMap?.removeProgressChangeListener()
        mapboxReplayer.finish()

        mapboxNavigation.run {
            unregisterArrivalObserver(arrivalObserver)
            unregisterTripSessionStateObserver(tripSessionStateObserver)
            unregisterRouteProgressObserver(routeProgressObserver)
            unregisterBannerInstructionsObserver(bannerInstructionObserver)
            stopTripSession() // TODO causes crash in updateCameraOnNavigationStateChange sometimes?
            onDestroy()
        }

        backPressedCallback?.remove()
        gpsWarning?.dismiss()
        navIntroSnackbar?.dismiss()
    }

    companion object {
        const val ROUTE_BUNDLE_KEY = "routeBundleKey"
        // custom margins of the mapbox compass
        private const val compassMarginLeft = 10
        private const val compassMarginBottom = 200
    }
}
