@file:Suppress("MagicNumber")

package de.ur.explure.views

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.crazylegend.viewbinding.viewBinding
import com.google.android.material.snackbar.Snackbar
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.location.LocationUpdate
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
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
import com.mapbox.navigation.core.replay.history.ReplayEventBase
import com.mapbox.navigation.core.replay.history.ReplayEventsObserver
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.ui.camera.DynamicCamera
import com.mapbox.navigation.ui.camera.NavigationCamera
import com.mapbox.navigation.ui.map.NavigationMapboxMap
import com.mapbox.navigation.ui.route.NavigationMapRoute
import de.ur.explure.R
import de.ur.explure.databinding.FragmentNavigationBinding
import de.ur.explure.map.LocationManager
import de.ur.explure.map.MapHelper
import de.ur.explure.map.PermissionHelper
import de.ur.explure.model.route.Route
import de.ur.explure.utils.EventObserver
import de.ur.explure.utils.SharedPreferencesManager
import de.ur.explure.utils.getMapboxAccessToken
import de.ur.explure.utils.isGPSEnabled
import de.ur.explure.utils.showSnackbar
import de.ur.explure.viewmodel.NavigationViewModel
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.androidx.viewmodel.scope.emptyState
import org.koin.core.parameter.parametersOf
import timber.log.Timber

/**
 * TODO um den mapstyle während der navigation zu ändern muss nur das gemacht werden:
 * navigationMapboxMap?.retrieveMap()?.setStyle(...)
 */

/**
 * TODO show waypoint markers only if the user is near them!! otherwise we would probably the navigation completely!
 */

/**
 * TODO the blue flag should only be added here and not during route creation!! this way users see
 *   where the end is but it doesn't overlap with anything!
 */

/**
 * TODO die ganzen observer sollten in onStart registered werden und in onStop schon wieder unregistered!
 */
class NavigationFragment : Fragment(R.layout.fragment_navigation), MapHelper.MapHelperListener {

    private val binding by viewBinding(FragmentNavigationBinding::bind)
    private val navigationViewModel: NavigationViewModel by viewModel(state = emptyState())

    // arguments
    private val args: NavigationFragmentArgs by navArgs()
    private lateinit var route: Route
    private lateinit var routeCoordinates: List<Point>

    // map
    private var mapView: MapView? = null
    private lateinit var mapHelper: MapHelper

    // navigation
    private lateinit var mapboxNavigation: MapboxNavigation
    private var navigationMapboxMap: NavigationMapboxMap? = null
    private var navigationMapRoute: NavigationMapRoute? = null
    private var navIntroSnackbar: Snackbar? = null

    // for testing routes
    private val mapboxReplayer = MapboxReplayer()
    private val replayProgressObserver = ReplayProgressObserver(mapboxReplayer)
    private val replayLocationEngine = ReplayLocationEngine(mapboxReplayer)
    private val replayRouteMapper = ReplayRouteMapper()

    // TODO auch noch replayHistoryMapper ?
    private var shouldSimulateRoute: Boolean = false // TODO for debugging and demo only

    // SharedPrefs
    private val preferencesManager: SharedPreferencesManager by inject()

    // location tracking
    private lateinit var locationManager: LocationManager

    // permission handling
    private val permissionHelper: PermissionHelper by inject()
    private var permissionExplanationSnackbar: Snackbar? = null

    // navigation observers
    private val routeProgressObserver = object : RouteProgressObserver {
        override fun onRouteProgressChanged(routeProgress: RouteProgress) {
            routeProgress.currentState.let { currentState ->
                Toast.makeText(
                    requireActivity(),
                    "onRouteProgressChanged: $currentState",
                    Toast.LENGTH_SHORT
                ).show()
            }

            navigationMapboxMap?.onNewRouteProgress(routeProgress)
        }
    }

    // TODO auch noch routeObserver und offRouteObserver?

    // Todo: mapboxNavigation.startTripSession() and .stopTripSession() need to be called otherwise useless (?)
    private val tripSessionStateObserver = object : TripSessionStateObserver {
        override fun onSessionStateChanged(tripSessionState: TripSessionState) {
            Toast.makeText(
                requireActivity(),
                "onTripSessionStateChanged: $tripSessionState",
                Toast.LENGTH_SHORT
            ).show()

            when (tripSessionState) {

                TripSessionState.STARTED -> {
                    // Todo stop or start location updates?
                }

                TripSessionState.STOPPED -> {
                    // waypointsController.clear()
                    navigationMapboxMap?.hideRoute()
                    updateCameraOnNavigationStateChange(false)
                }
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

        /**
         * ! Useful: this method can also be called from outside with
         * mapboxNavigation?.navigateNextRouteLeg()
         */

        override fun navigateNextRouteLeg(routeLegProgress: RouteLegProgress): Boolean {
            // This example shows you can use both time and distance.
            // Move to the next step when the distance is small
            // findViewById<Button>(R.id.navigateNextRouteLeg).visibility = View.VISIBLE
            // return false
            Toast.makeText(
                requireActivity(),
                "navigateNextRouteLeg: ${routeLegProgress.upcomingStep?.name()}",
                Toast.LENGTH_SHORT
            ).show()
            return routeLegProgress.distanceRemaining < 2.0
        }
    }

    private val arrivalObserver = object : ArrivalObserver {
        /**
         * Called once the driver has arrived at a stop and has started navigating the next leg.
         * NavigateNextRouteLeg will also be true when this happens
         */
        override fun onNextRouteLegStart(routeLegProgress: RouteLegProgress) {
            // findViewById<Button>(R.id.navigateNextRouteLeg).visibility = View.GONE
            Toast.makeText(
                requireActivity(),
                "onNextRouteLegStart: ${routeLegProgress.currentStepProgress}; ${routeLegProgress.distanceRemaining}",
                Toast.LENGTH_SHORT
            ).show()
        }

        /**
         * Called once the driver has reached the final destination at the end of the route.
         * RouteProgress.currentState() will equal RouteProgressState.ROUTE_ARRIVED
         */
        override fun onFinalDestinationArrival(routeProgress: RouteProgress) {
            // findViewById<Button>(R.id.navigateNextRouteLeg).visibility = View.GONE
            showSnackbar(requireActivity(), "Ziel erreicht!", colorRes = R.color.themeColor)
        }
    }

    // todo locationObserver ?
    private val locationObserver: LocationObserver = object : LocationObserver {
        override fun onRawLocationChanged(rawLocation: Location) {
            Timber.d("raw location %s", rawLocation.toString())
        }

        override fun onEnhancedLocationChanged(
            enhancedLocation: Location,
            keyPoints: List<Location>
        ) {
            if (keyPoints.isEmpty()) {
                // updateLocation(enhancedLocation)
                // TODO should probably not initCustomLocationEngine when using this
                // https://github.com/mapbox/mapbox-navigation-android/wiki/1.0-Navigation-SDK-Migration-Guide#location
                mapHelper.map.locationComponent.forceLocationUpdate(listOf(enhancedLocation), false)
            } else {
                // updateLocation(keyPoints)
                mapHelper.map.locationComponent.forceLocationUpdate(keyPoints, false)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
        val routeWayPoints = route.wayPoints
        val routeDuration = route.duration
        val routeDistance = route.distance

        setupViewModelObservers()

        // init map
        mapView = binding.mapView
        mapView?.onCreate(savedInstanceState)
        mapHelper = get { parametersOf(mapView, viewLifecycleOwner.lifecycle) }
        viewLifecycleOwner.lifecycle.addObserver(mapHelper)
        mapHelper.setMapHelperListener(this)

        // TODO locationManager could also be extracted to the mapHelper to save some duplicate code!
        // init locationManager and sync with fragment lifecycle
        locationManager = get { parametersOf(this::onNewLocationReceived) }
        viewLifecycleOwner.lifecycle.addObserver(locationManager)

        // init navigation components
        val recreated = savedInstanceState != null
        setupMapboxNavigation(recreated)
        setupNavUI()

        // create a navigable route
        generateRoute()
    }

    private fun setupViewModelObservers() {
        navigationViewModel.inNavigationMode.observe(viewLifecycleOwner, EventObserver { navigationActive ->
                if (navigationActive) {
                    navIntroSnackbar?.dismiss()

                    if (shouldSimulateRoute) {
                        startSimulation()
                    } else {
                        startNavigation()
                    }
                }
            })
        navigationViewModel.buildingExtrusionActive.observe(viewLifecycleOwner) { extrusionsActive ->
            mapHelper.buildingPlugin?.setVisibility(extrusionsActive)
        }
    }

    private fun setupMapboxNavigation(isRecreated: Boolean) {
        mapboxNavigation = if (isRecreated) {
            // fragment was recreated so retrieve the already existing mapboxNavigation instance
            MapboxNavigationProvider.retrieve()
        } else {
            val navigationOptions = MapboxNavigation
                .defaultNavigationOptionsBuilder(requireContext(), getMapboxAccessToken(requireContext()))
                .locationEngine(getLocationEngine())
                // .locationEngine(LocationEngineProvider.getBestLocationEngine(requireActivity()))
                // .isRouteRefreshEnabled(false)
                .build()

            MapboxNavigationProvider.create(navigationOptions)
        }

        mapboxNavigation.apply {
            registerTripSessionStateObserver(tripSessionStateObserver)
            registerRouteProgressObserver(routeProgressObserver)
        }
    }

    private fun setupNavUI() {
        binding.navigationProgressBar.visibility = View.VISIBLE

        navIntroSnackbar = showSnackbar(
            requireActivity(),
            "Bitte begib dich an den markierten Startpunkt der Route, um mit der Navigation" +
                    " zu beginnen. Wenn du da bist, klicke oben auf \"Navigation starten\".",
            colorRes = R.color.colorInfo,
            length = Snackbar.LENGTH_INDEFINITE
        )

        binding.startNavigationButton.setOnClickListener {
            // check if we have a route first!
            if (navigationViewModel.directionsRoute != null) {
                navigationViewModel.enterNavigationMode()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Die Route wird noch geladen. Einen Moment!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onMapLoaded(map: MapboxMap) {
        // TODO
    }

    override fun onMapStyleLoaded(mapStyle: Style) {
        // ! check if already initialized, otherwise it will crash because it adds sources twice
        if (navigationMapboxMap == null) {
            val mapview = mapView ?: return
            // setup navigation ui sdk (some parts of it are needed)
            navigationMapboxMap =
                NavigationMapboxMap.Builder(mapview, mapHelper.map, viewLifecycleOwner)
                    .useSpecializedLocationLayer(true)
                    .vanishRouteLineEnabled(true)
                    .build()
            navigationMapboxMap?.setCamera(DynamicCamera(mapHelper.map))
            navigationMapboxMap?.addProgressChangeListener(mapboxNavigation)

            // use default navigation puck drawables provided by mapbox
            // navigationMapboxMap?.setPuckDrawableSupplier(DefaultMapboxPuckDrawableSupplier())
        }

        // TODO
        /*
        setupNavigationLayers(mapStyle)

        // ! check if already initialized, otherwise it will crash because it adds sources twice
        if (navigationMapRoute != null) {
            navigationMapRoute =
                NavigationMapRoute.Builder(mapview, mapHelper.map, viewLifecycleOwner)
                    .withVanishRouteLineEnabled(true)
                    // .withMapboxNavigation(mapboxNavigation)
                    .build()  // FIXME: Crash: Source mapbox-navigation-waypoint-source already exists

            // navigationMapRoute.addRoutes(TODO())
        }*/
    }

    /**
     * Navigation code
     */

    @SuppressLint("MissingPermission")
    private fun startNavigation() {
        val route = navigationViewModel.directionsRoute ?: return

        mapboxNavigation.setArrivalController(arrivalController)
        mapboxNavigation.registerArrivalObserver(arrivalObserver)

        // TODO navigation mapbox map needs to be setup before!

        updateCameraOnNavigationStateChange(true)
        navigationMapboxMap?.addProgressChangeListener(mapboxNavigation)

        /**
         * TOOD a check if the user position is in the latLngBound for the camera is essential!!!
         * otherwise don't allow navigation!
         */

        // TODO start location tracking and ask for permissions!
        // TODO ist das der gleiche style?
        val style = preferencesManager.getCurrentMapStyle()
        mapHelper.map.style?.let { startLocationTracking(it) }

        /*
        mapViewModel.getCurrentMapStyle()?.let {
            startLocationTracking(it)
        }*/

        // mapboxNavigation.registerVoiceInstructionsObserver(this)

        // TODO wie starte man jetzt navigation? getRoutes wird wohl erst gehen wenn schon route requested?
        val directionRoutes = mapboxNavigation.getRoutes()
        if (directionRoutes.isNotEmpty()) {
            navigationMapboxMap?.startCamera(directionRoutes[0])
        }

        navigationMapboxMap?.showAlternativeRoutes(false)
        mapboxNavigation.startTripSession()

        /*
        startNavigation.visibility = View.GONE
        mapboxReplayer.play()
        */
    }

    private fun startSimulation() {
        val route = navigationViewModel.directionsRoute ?: return

        // TODO
        /*
        mapboxNavigation.registerRouteProgressObserver(ReplayProgressObserver(mapboxReplayer))
        mapboxReplayer.pushRealLocation(requireActivity(), 0.0)
        */

        mapboxReplayer.stop()
        mapboxReplayer.clearEvents()
        val replayData = replayRouteMapper.mapDirectionsRouteGeometry(route)
        mapboxReplayer.pushEvents(replayData)
        mapboxReplayer.seekTo(replayData[0])
        // mapboxReplayer.playbackSpeed(1.5)
        mapboxReplayer.registerObserver(object : ReplayEventsObserver {
            override fun replayEvents(events: List<ReplayEventBase>) {
                // TODO?
            }
        })
        mapboxReplayer.play()
    }

    @SuppressLint("MissingPermission")
    private fun restoreNavigation() {
        navigationViewModel.directionsRoute?.let {
            mapboxNavigation.setRoutes(listOf(it))
            navigationMapboxMap?.addProgressChangeListener(mapboxNavigation)
            navigationMapboxMap?.startCamera(mapboxNavigation.getRoutes()[0])
            updateCameraOnNavigationStateChange(true)
            mapboxNavigation.startTripSession()
        }
    }

    // TODO: this is the old approach using sources and layers with data-driven styling!
    /*
    private fun setupNavigationLayers(mapStyle: Style) {
        // Add the destination marker image
        ContextCompat.getDrawable(requireContext(), R.drawable.ic_marker_destination)?.let {
            mapStyle.addImage(GOAL_ICON_ID, it)
        }

        // Add the LineLayer below the LocationComponent's bottom layer, which is the
        // circular accuracy layer. The LineLayer will display the directions route.
        mapStyle.run {
            addSource(
                GeoJsonSource(
                    "ROUTE_LINE_SOURCE_ID",
                    GeoJsonOptions().withLineMetrics(true)
                )
            )

            addLayerBelow(
                LineLayer("ROUTE_LINE_LAYER_ID", "ROUTE_LINE_SOURCE_ID")
                    .withProperties(
                        lineCap(LINE_CAP_ROUND),
                        lineJoin(LINE_JOIN_ROUND),
                        lineWidth(5f),
                        lineGradient(
                            interpolate(
                                linear(),
                                lineProgress(),
                                stop(0f, color(parseColor(ORIGIN_COLOR))),
                                stop(1f, color(parseColor(DESTINATION_COLOR)))
                            )
                        )
                    ),
                // show below the layer on which the user location puck is shown
                "mapbox-location-shadow-layer"
            )

            // Add the SymbolLayer to show the destination marker
            addSource(GeoJsonSource("ROUTE_MARKER_SOURCE_ID"))
            addLayerAbove(
                SymbolLayer("ROUTE_MARKER_LAYER_ID", "ROUTE_MARKER_SOURCE_ID")
                    .withProperties(
                        iconImage(GOAL_ICON_ID)
                    ),
                "ROUTE_LINE_LAYER_ID"
            )
        }
    }
     */

    /*
    private fun showFinishNavigationDialog() {
        with(MaterialAlertDialogBuilder(requireActivity())) {
            setMessage("Möchtest du die Navigation wirklich beenden?")
            setPositiveButton(R.string.yes) { _, _ ->
                // TODO reset layout
            }
            setNegativeButton(R.string.no) { _, _ -> }
            show()
        }
    }*/

    private fun updateCameraOnNavigationStateChange(navigationStarted: Boolean) {
        navigationMapboxMap?.apply {
            if (navigationStarted) {
                // track user position during navigation
                updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_GPS)
                updateLocationLayerRenderMode(RenderMode.GPS)
            } else {
                // stop tracking user position if not in navigation mode
                updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_NONE)
                updateLocationLayerRenderMode(RenderMode.COMPASS)
            }
        }
    }

    /**
     * TODO die route sollte der vergleichbarkeit halber wieder mit dem grünen map matching route
     * line manager stuff gezeichnet werden!
     */

    @SuppressLint("MissingPermission")
    private fun generateRoute() {
        val startLocation = routeCoordinates.first()
        val destinationLocation = routeCoordinates.last()

        // TODO stattdessen versuchen mit dem PolylineUtils - Algorithmus nochmal ein paar entfernen!

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
                routeCoordinates.subList(1, routeCoordinates.size - 2).forEachIndexed { index, point ->
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

        // sanity check to make sure the algorithm above does work as intended!
        if (coordinateList.size > 25) {
            // ! This SHOULD never happen in production!
            Timber.e("Die Route übersteigt das Limit von 25 Koordinaten!")
            showSnackbar(requireActivity(), "Ein Fehler ist aufgetreten! Für diese Route ist" +
                    " im Moment leider keine Navigation verfügbar!", colorRes = R.color.colorError)
            return
        }

        val token = getMapboxAccessToken(requireActivity().applicationContext)
        val routeOptions = RouteOptions.builder()
            .applyDefaultParams() // TODO überschreibt das das walking profil unten ?? wenn ja, so machen:
            /*
            .baseUrl(RouteUrl.BASE_URL)
            .user(RouteUrl.PROFILE_DEFAULT_USER)
            .geometries(RouteUrl.GEOMETRY_POLYLINE6)
            */
            .accessToken(token)
            .coordinates(coordinateList)
            // .waypointTargetsList(...)
            .alternatives(false) // we don't need any alternative routes
            .steps(true)
            .bannerInstructions(true)
            .voiceInstructions(false)
            .profile(DirectionsCriteria.PROFILE_WALKING)
            .overview(DirectionsCriteria.OVERVIEW_FULL)
            // TODO test this
            .annotationsList(
                listOf(
                    DirectionsCriteria.ANNOTATION_DURATION,
                    DirectionsCriteria.ANNOTATION_DISTANCE,
                    DirectionsCriteria.ANNOTATION_CONGESTION
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
                    // onNewRouteAvailable(routes)

                    binding.navigationProgressBar.visibility = View.GONE
                    navigationViewModel.directionsRoute = routes[0]
                    navigationMapboxMap?.drawRoutes(routes)

                    /*
                    // TODO with navigationMapRoute
                    activeRoute = routes.get(0);
                  navigationMapRoute.addRoutes(routes);
                  routeLoading.setVisibility(View.INVISIBLE);
                  startNavigationButton.setVisibility(View.VISIBLE);
                     */
                }

                override fun onRoutesRequestFailure(
                    throwable: Throwable,
                    routeOptions: RouteOptions
                ) {
                    Timber.e("route request failure %s", throwable.toString())
                    showSnackbar(
                        requireActivity(),
                        R.string.route_request_failed,
                        colorRes = R.color.colorError
                    )
                }

                override fun onRoutesRequestCanceled(routeOptions: RouteOptions) {
                    Timber.d("route request canceled")
                }
            }
        )
    }

    // TODO hier erst navigation starten?
    private fun onNewRouteAvailable(routes: List<DirectionsRoute>) {
        if (routes.isNotEmpty()) {
            // enable navigate button if routes are available
            // TODO binding.startNavigationButton.isEnabled = true

            showSnackbar(
                String.format(
                    getString(R.string.steps_in_route),
                    routes[0].legs()?.get(0)?.steps()?.size
                ),
                binding.navigationContainer,
                colorRes = R.color.themeColor
            )

            // Update a gradient route LineLayer's source with the Maps SDK. This will
            // visually add/update the line on the map. All of this is being done
            // directly with Maps SDK code and NOT the Navigation UI SDK.
            mapHelper.map.getStyle {
                val routeLineSource = it.getSourceAs<GeoJsonSource>("ROUTE_LINE_SOURCE_ID")
                val routeLineString = routes[0].geometry()?.let { geometry ->
                    LineString.fromPolyline(geometry, Constants.PRECISION_6)
                }
                routeLineSource?.setGeoJson(routeLineString)

                // TODO: save the current route (routes[0]) and restore it on config changes
            }

            // TODO
            /*
            navigationMapboxMap?.drawRoutes(routes)

            val replayEvents = replayRouteMapper.mapDirectionsRouteLegAnnotation(routes[0])
            mapboxReplayer.pushEvents(replayEvents)
            mapboxReplayer.seekTo(replayEvents.first())
             */
        } else {
            // TODO binding.startNavigationButton.isEnabled = false
            showSnackbar(requireActivity(), R.string.no_routes, binding.navigationContainer)
        }
    }

    // TODO fullscreen?
    /*
    private fun hideNavigationFullscreen() {
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
        )
    }*/

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

                // TODO oder:
                /*
                if (!shouldSimulateRoute) {
                    val requestLocationUpdateRequest =
                        LocationEngineRequest.Builder(DEFAULT_INTERVAL_IN_MILLISECONDS)
                            .setPriority(LocationEngineRequest.PRIORITY_NO_POWER)
                            .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME)
                            .build()

                    mapboxNavigation.navigationOptions?.locationEngine?.requestLocationUpdates(
                        requestLocationUpdateRequest,
                        locationListenerCallback,
                        mainLooper
                    )
                }*/

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
            // dismiss the permission explanation
            permissionExplanationSnackbar?.dismiss()
            // try to find the device location and enable location tracking
            mapHelper.map.style?.let { startLocationTracking(it) }
        } else {
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
        // Pass the new location to the Maps SDK's LocationComponent
        val locationUpdate = LocationUpdate.Builder().location(location).build()
        mapHelper.map.locationComponent.forceLocationUpdate(locationUpdate)
        // and the navigation map
        navigationMapboxMap?.updateLocation(location)

        // save the new location
        navigationViewModel.setCurrentUserPosition(location)
    }

    // TODO
    // If shouldSimulateRoute is true a ReplayRouteLocationEngine will be used which is intended
    // for testing else a real location engine is used.
    private fun getLocationEngine(): LocationEngine {
        return if (shouldSimulateRoute) {
            ReplayLocationEngine(mapboxReplayer)
        } else {
            LocationEngineProvider.getBestLocationEngine(requireActivity())
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
        return when (item.itemId) {
            R.id.show3dBuildings -> {
                item.isChecked = !item.isChecked
                navigationViewModel.setBuildingExtrusionStatus(item.isChecked)
                preferencesManager.setBuildingExtrusionShown(item.isChecked)
                true
            }
            R.id.simulateRouteProgress -> {
                item.isChecked = !item.isChecked
                shouldSimulateRoute = item.isChecked
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onStart() {
        super.onStart()

        // TODO: combine mapboxnavigation and own location manager?
        // mapboxNavigation?.registerLocationObserver(locationObserver)
        // ! then we could use something like this:
        // mapboxNavigation.getNavigationOptions().getLocationEngine().getLastLocation(locationEngineCallback)
    }

    override fun onStop() {
        super.onStop()

        // * unregistering is automatically done for you if you run mapboxNavigation.onDestroy()
        // mapboxNavigation?.unregisterTripSessionStateObserver(tripSessionStateObserver)
        // mapboxNavigation?.unregisterRouteProgressObserver(routeProgressObserver)

        // mapboxNavigation?.unregisterLocationObserver(locationObserver)

        // mapboxNavigation?.stopTripSession()

        // TODO save current navigation state and progress!
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

        // cleanup
        navIntroSnackbar?.dismiss()
        mapboxNavigation.stopTripSession()

        // todo
        mapboxReplayer.finish()
        navigationMapboxMap?.removeProgressChangeListener()
        mapboxNavigation.unregisterArrivalObserver(arrivalObserver)
        mapboxNavigation.unregisterTripSessionStateObserver(tripSessionStateObserver)
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.onDestroy()
    }

    companion object {
        private const val ORIGIN_COLOR = "#32a852" // Green
        private const val DESTINATION_COLOR = "#F84D4D" // Red
        private const val GOAL_ICON_ID = "goal-icon-id"
    }
}
