package de.ur.explure.views

import android.graphics.BitmapFactory
import android.graphics.Color.parseColor
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.widget.ListPopupWindow
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.crazylegend.viewbinding.viewBinding
import com.google.android.material.snackbar.Snackbar
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.expressions.Expression.color
import com.mapbox.mapboxsdk.style.expressions.Expression.interpolate
import com.mapbox.mapboxsdk.style.expressions.Expression.lineProgress
import com.mapbox.mapboxsdk.style.expressions.Expression.linear
import com.mapbox.mapboxsdk.style.expressions.Expression.stop
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.Property.LINE_CAP_ROUND
import com.mapbox.mapboxsdk.style.layers.Property.LINE_JOIN_ROUND
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineCap
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineGradient
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineJoin
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineWidth
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonOptions
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.navigation.base.internal.extensions.applyDefaultParams
import com.mapbox.navigation.base.internal.extensions.coordinates
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import de.ur.explure.R
import de.ur.explure.databinding.FragmentMapBinding
import de.ur.explure.extensions.toPoint
import de.ur.explure.map.LocationManager
import de.ur.explure.map.MarkerManager
import de.ur.explure.utils.EventObserver
import de.ur.explure.utils.Highlight
import de.ur.explure.utils.SharedPreferencesManager
import de.ur.explure.utils.TutorialBuilder
import de.ur.explure.utils.getMapboxAccessToken
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
import timber.log.Timber

@Suppress("TooManyFunctions")
class MapFragment : Fragment(R.layout.fragment_map), OnMapReadyCallback, PermissionsListener {

    private val binding by viewBinding(FragmentMapBinding::bind)

    // Setting the state as emptyState as a workaround for this issue: https://github.com/InsertKoinIO/koin/issues/963
    private val mapViewModel: MapViewModel by viewModel(state = emptyState())

    // SharedPrefs
    private val preferencesManager: SharedPreferencesManager by inject()

    // map
    private var mapView: MapView? = null
    private lateinit var map: MapboxMap
    private lateinit var markerManager: MarkerManager

    // location tracking
    private lateinit var locationManager: LocationManager

    // permission handling
    private var permissionsManager: PermissionsManager = PermissionsManager(this)

    // navigation
    private var mapboxNavigation: MapboxNavigation? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Timber.d("in MapFragment onViewCreated")

        // disable the buttons until the map has finished loading
        binding.ownLocationButton.isEnabled = false
        binding.changeStyleButton.isEnabled = false

        // init locationManager and sync with fragment lifecycle
        locationManager = get { parametersOf(this::onNewLocationReceived) }
        viewLifecycleOwner.lifecycle.addObserver(locationManager)

        setupViewModelObservers()

        // init mapbox map
        mapView = binding.mapView
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)

        setupMapboxNavigation()
    }

    private fun setupViewModelObservers() {
        mapViewModel.mapReady.observe(viewLifecycleOwner, EventObserver {
            // this should only get called if the event has never been handled before because of the EventObserver
            Timber.d("Map has finished loading and can be used now!")

            // enable the buttons now that the map is ready
            binding.ownLocationButton.isEnabled = true
            binding.changeStyleButton.isEnabled = true

            binding.changeStyleButton.setOnClickListener {
                showMapStyleOptions(layoutResource = R.layout.popup_list_item)
            }

            binding.ownLocationButton.setOnClickListener {
                mapViewModel.getCurrentMapStyle()?.let { style -> startLocationTracking(style) }
            }

            // if location tracking was enabled before, start it again without forcing the user to
            // press the button again
            if (mapViewModel.isLocationTrackingActivated() == true) {
                mapViewModel.getCurrentMapStyle()?.let {
                    startLocationTracking(it)
                }
            }
        })
    }

    private fun setupMapboxNavigation() {
        val context = context ?: return

        val navigationOptions = MapboxNavigation
            .defaultNavigationOptionsBuilder(
                context,
                getMapboxAccessToken(context.applicationContext)
            )
            // .locationEngine(locationEngine)
            .build()
        mapboxNavigation = MapboxNavigationProvider.create(navigationOptions)
        // mapboxNavigation = MapboxNavigationProvider.retrieve()
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

        setupMapListeners()

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

            setupNavigationLayers(mapStyle)

            mapViewModel.setMapReadyStatus(true)
        }
    }

    // TODO: this is the old approach using sources and layers with data-driven styling!
    private fun setupNavigationLayers(mapStyle: Style) {
        // Add the destination marker image
        BitmapFactory.decodeResource(resources, R.drawable.ic_sharp_flag_24)?.let {
            mapStyle.addImage(GOAL_ICON_ID, it)
        }

        // Add the LineLayer below the LocationComponent's bottom layer, which is the
        // circular accuracy layer. The LineLayer will display the directions route.
        mapStyle.addSource(
            GeoJsonSource(
                ROUTE_LINE_SOURCE_ID,
                GeoJsonOptions().withLineMetrics(true)
            )
        )

        mapStyle.addLayerBelow(
            LineLayer(ROUTE_LINE_LAYER_ID, ROUTE_LINE_SOURCE_ID)
                .withProperties(
                    lineCap(LINE_CAP_ROUND),
                    lineJoin(LINE_JOIN_ROUND),
                    lineWidth(ROUTE_LINE_WIDTH),
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
        mapStyle.addSource(GeoJsonSource(ROUTE_MARKER_SOURCE_ID))
        mapStyle.addLayerAbove(
            SymbolLayer(ROUTE_MARKER_LAYER_ID, ROUTE_MARKER_SOURCE_ID)
                .withProperties(
                    iconImage(GOAL_ICON_ID)
                ),
            ROUTE_LINE_LAYER_ID
        )
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
        // ! This method has to return false as otherwise symbol clicks won't be fired.
        return false
    }

    private fun onMapLongClicked(point: LatLng): Boolean {
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

        generateRoute(point)

        return false
    }

    private fun generateRoute(clickedPoint: LatLng) {
        if (!map.locationComponent.isLocationComponentActivated) {
            mapViewModel.getCurrentMapStyle()?.let {
                startLocationTracking(it)
            }
        } else {
            map.locationComponent.lastKnownLocation?.let { originLocation ->
                val token = getMapboxAccessToken(requireActivity().applicationContext)
                val routeOptions = RouteOptions.builder().applyDefaultParams()
                    .accessToken(token)
                    .coordinates(originLocation.toPoint(), null, clickedPoint.toPoint())
                    .alternatives(true)
                    .steps(true)
                    .bannerInstructions(true)
                    .voiceInstructions(false)
                    .profile(DirectionsCriteria.PROFILE_WALKING)
                    .overview(DirectionsCriteria.OVERVIEW_FULL)
                    .build()

                mapboxNavigation?.requestRoutes(
                    routeOptions,
                    object : RoutesRequestCallback {
                        override fun onRoutesReady(routes: List<DirectionsRoute>) {
                            onNewRouteAvailable(routes)
                        }

                        override fun onRoutesRequestFailure(
                            throwable: Throwable,
                            routeOptions: RouteOptions
                        ) {
                            Timber.e("route request failure %s", throwable.toString())
                            showSnackbar(
                                requireActivity(),
                                R.string.route_request_failed,
                                binding.mapContainer,
                                colorRes = R.color.color_error
                            )
                        }

                        override fun onRoutesRequestCanceled(routeOptions: RouteOptions) {
                            Timber.d("route request canceled")
                        }
                    }
                )
            }
        }
    }

    private fun onNewRouteAvailable(routes: List<DirectionsRoute>) {
        if (routes.isNotEmpty()) {
            showSnackbar(
                requireActivity(),
                String.format(
                    getString(R.string.steps_in_route),
                    routes[0].legs()?.get(0)?.steps()?.size
                ),
                binding.mapContainer,
                colorRes = R.color.colorPrimary
            )

            // Update a gradient route LineLayer's source with the Maps SDK. This will
            // visually add/update the line on the map. All of this is being done
            // directly with Maps SDK code and NOT the Navigation UI SDK.
            map.getStyle {
                val routeLineSource = it.getSourceAs<GeoJsonSource>(ROUTE_LINE_SOURCE_ID)
                val routeLineString = routes[0].geometry()?.let { geometry ->
                    LineString.fromPolyline(geometry, Constants.PRECISION_6)
                }
                routeLineSource?.setGeoJson(routeLineString)
            }
            binding.routeRetrievalProgressSpinner.visibility = View.INVISIBLE
        } else {
            showSnackbar(requireActivity(), R.string.no_routes, binding.mapContainer)
        }
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
                    binding.mapContainer,
                    Snackbar.LENGTH_LONG,
                    colorRes = R.color.color_warning
                )
                return
            }

            // enable location tracking with custom loaction engine
            if (::locationManager.isInitialized) {
                mapViewModel.setLocationTrackingStatus(isEnabled = true)
                locationManager.activateLocationComponent(
                    map.locationComponent, loadedMapStyle, useDefaultEngine = false
                )
            }
        } else {
            permissionsManager.requestLocationPermissions(activity)
        }
    }

    private fun onNewLocationReceived(location: Location) {
        // Pass the new location to the Maps SDK's LocationComponent
        map.locationComponent.forceLocationUpdate(location)
        // save the new location
        mapViewModel.setCurrentUserPosition(location)
    }

    /**
     * * Permission Code
     */

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        showSnackbar(
            requireActivity(),
            R.string.location_permission_explanation,
            binding.mapContainer,
            Snackbar.LENGTH_LONG,
            colorRes = R.color.color_info
        )
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            // try to find the device location and enable location tracking
            map.style?.let { startLocationTracking(it) }
        } else {
            showSnackbar(
                requireActivity(),
                R.string.location_permission_not_given,
                binding.mapContainer,
                Snackbar.LENGTH_LONG,
                colorRes = R.color.color_warning
            )
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

        mapboxNavigation?.onDestroy()
        mapView?.onDestroy()
    }

    companion object {
        // Note: this layer is not in all map styles available (e.g. the satellite style)!
        // private const val FIRST_SYMBOL_LAYER_ID = "waterway-label"
        private const val ROUTE_LINE_WIDTH = 6f

        private const val ROUTE_LINE_SOURCE_ID = "ROUTE_LINE_SOURCE_ID"
        private const val ROUTE_LINE_LAYER_ID = "ROUTE_LINE_LAYER_ID"
        private const val ROUTE_MARKER_SOURCE_ID = "ROUTE_MARKER_SOURCE_ID"
        private const val ROUTE_MARKER_LAYER_ID = "ROUTE_MARKER_LAYER_ID"

        private const val ORIGIN_COLOR = "#32a852" // Green
        private const val DESTINATION_COLOR = "#F84D4D" // Red

        // private const val MARKER_ICON_ID = "marker-icon-id"
        private const val GOAL_ICON_ID = "goal-icon-id"
    }
}
