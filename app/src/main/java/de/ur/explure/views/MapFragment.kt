package de.ur.explure.views

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.graphics.Color.parseColor
import android.graphics.Rect
import android.os.Bundle
import android.os.Looper
import android.util.DisplayMetrics
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.widget.ListPopupWindow
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.crazylegend.viewbinding.viewBinding
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetSequence
import com.google.android.material.snackbar.Snackbar
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.mapbox.mapboxsdk.style.expressions.Expression.color
import com.mapbox.mapboxsdk.style.expressions.Expression.interpolate
import com.mapbox.mapboxsdk.style.expressions.Expression.lineProgress
import com.mapbox.mapboxsdk.style.expressions.Expression.linear
import com.mapbox.mapboxsdk.style.expressions.Expression.stop
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.Property.ICON_ROTATION_ALIGNMENT_VIEWPORT
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
import de.ur.explure.utils.EventObserver
import de.ur.explure.utils.SharedPreferencesManager
import de.ur.explure.utils.getMapboxAccessToken
import de.ur.explure.utils.isGPSEnabled
import de.ur.explure.utils.measureContentWidth
import de.ur.explure.extensions.toPoint
import de.ur.explure.viewmodel.MapViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.androidx.viewmodel.scope.emptyState
import timber.log.Timber
import java.lang.ref.WeakReference

@Suppress("TooManyFunctions")
class MapFragment : Fragment(R.layout.fragment_map), OnMapReadyCallback, PermissionsListener {

    private val binding by viewBinding(FragmentMapBinding::bind)

    private lateinit var preferencesManager: SharedPreferencesManager

    // Setting the state as emptyState as a workaround for this issue: https://github.com/InsertKoinIO/koin/issues/963
    private val mapViewModel: MapViewModel by viewModel(state = emptyState())

    private var mapView: MapView? = null
    private lateinit var map: MapboxMap

    private var mapboxNavigation: MapboxNavigation? = null

    // location tracking
    private var locationEngine: LocationEngine? = null
    private var callback: LocationListeningCallback? = null

    // map symbols
    private var symbolManager: SymbolManager? = null

    // permission handling
    private var permissionsManager: PermissionsManager = PermissionsManager(this)

    private val routesReqCallback = object : RoutesRequestCallback {
        override fun onRoutesReady(routes: List<DirectionsRoute>) {
            if (routes.isNotEmpty()) {
                Snackbar.make(
                    binding.mapContainer,
                    String.format(
                        getString(R.string.steps_in_route),
                        routes[0].legs()?.get(0)?.steps()?.size
                    ),
                    Snackbar.LENGTH_SHORT
                ).show()

                // Update a gradient route LineLayer's source with the Maps SDK. This will
                // visually add/update the line on the map. All of this is being done
                // directly with Maps SDK code and NOT the Navigation UI SDK.
                map.getStyle {
                    val routeLineSource = it.getSourceAs<GeoJsonSource>(ROUTE_LINE_SOURCE_ID)
                    val routeLineString = routes[0].geometry()?.let { geometry ->
                        LineString.fromPolyline(geometry, ROUTE_LINE_PRECISION)
                    }
                    routeLineSource?.setGeoJson(routeLineString)
                }
                binding.routeRetrievalProgressSpinner.visibility = View.INVISIBLE
            } else {
                Snackbar.make(binding.mapContainer, R.string.no_routes, Snackbar.LENGTH_SHORT)
                    .show()
            }
        }

        override fun onRoutesRequestFailure(throwable: Throwable, routeOptions: RouteOptions) {
            Timber.e("route request failure %s", throwable.toString())
            Snackbar.make(
                binding.mapContainer,
                R.string.route_request_failed,
                Snackbar.LENGTH_SHORT
            ).show()
        }

        override fun onRoutesRequestCanceled(routeOptions: RouteOptions) {
            Timber.d("route request canceled")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Timber.d("in MapFragment onViewCreated")

        preferencesManager = SharedPreferencesManager(requireActivity())

        // disable the buttons until the map has finished loading
        binding.ownLocationButton.isEnabled = false
        binding.changeStyleButton.isEnabled = false

        setupViewModelObservers()

        // init mapbox map
        mapView = binding.mapView
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)

        setupMapboxNavigation()
    }

    private fun setupViewModelObservers() {
        mapViewModel.mapReady.observe(viewLifecycleOwner, EventObserver {
            // this only gets called if the event has never been handled before because of the EventObserver
            Timber.d("Map has finished loading and can be used now!")

            // enable the buttons now that the map is ready
            binding.ownLocationButton.isEnabled = true
            binding.changeStyleButton.isEnabled = true

            binding.changeStyleButton.setOnClickListener {
                showMapStyleOptions(layoutResource = R.layout.popup_list_item)
            }

            binding.ownLocationButton.setOnClickListener {
                mapViewModel.getCurrentMapStyle()?.let { style ->
                    mapViewModel.setLocationTrackingStatus(true)
                    enableLocationComponent(style)
                }
            }

            if (mapViewModel.isLocationTrackingActivated()) {
                // enable the locationcomponent again if it was activated before a config change
                mapViewModel.getCurrentMapStyle()?.let { style -> enableLocationComponent(style) }
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

        val styleList = MapViewModel.All_MAP_STYLES.keys.toList()
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
            val selectedMapStyle = MapViewModel.All_MAP_STYLES[selectedItem]
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

        Timber.d("in onMapReady")

        if (preferencesManager.isFirstRun()) {
            showTutorial()
        }

        setupMapListeners()

        // TODO setup a separate mapbox map object/singleton to handle and encapsulate map stuff?
        // -> use android jetpack lifecycle to access lifecycle hooks in that component
        val style = preferencesManager.getCurrentMapStyle()
        setMapStyle(style)

        // init the camera at the saved cameraPosition if it is not null
        mapViewModel.getLastKnownCameraPosition()?.let {
            map.cameraPosition = it
        }
    }

    /**
     * Show interesting spots on the screen the first time the fragment is launched on this device.
     */
    private fun showTutorial() {
        val activity = activity ?: return

        val targetOne = TapTarget.forView(
            binding.changeStyleButton,
            "Verschiedene Kartenstile sind verfügbar!",
            "Hier kannst du den aktuellen Kartenstil anpassen."
        )
            .id(1)
            .cancelable(true)
            .transparentTarget(true)
            .targetRadius(TARGET_ONE_RADIUS) // in dp
            .outerCircleAlpha(OUTER_CIRCLE_ALPHA)

        // setup a custom tap target at the university
        val displayMetrics = DisplayMetrics()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            activity.display?.getRealMetrics(displayMetrics)
        } else {
            @Suppress("DEPRECATION")
            activity.windowManager.defaultDisplay.getRealMetrics(displayMetrics)
        }
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val markerIcon =
            ContextCompat.getDrawable(activity, R.drawable.mapbox_marker_icon_default) ?: return
        val markerTarget = Rect(0, 0, markerIcon.intrinsicWidth, markerIcon.intrinsicHeight)
        markerTarget.offset(screenWidth / 2, screenHeight / 2) // center the target

        val targetTwo = TapTarget.forBounds(markerTarget, "Hier ist die Universität :)")
            .id(2)
            .cancelable(true)
            .transparentTarget(true)
            .targetRadius(TARGET_TWO_RADIUS)
            .outerCircleAlpha(OUTER_CIRCLE_ALPHA)
            .icon(markerIcon)

        TapTargetSequence(activity)
            .targets(targetOne, targetTwo)
            .start()

        // mark first launch as completed so this tutorial won't be shown on further app starts
        preferencesManager.completedFirstRun()
    }

    private fun setMapStyle(styleUrl: String?) {
        styleUrl ?: return

        map.setStyle(styleUrl) { mapStyle ->
            // Map is set up and the style has loaded.
            // save the current style in the shared preferences and the viewmodel
            mapViewModel.setCurrentMapStyle(mapStyle)
            preferencesManager.setCurrentMapStyle(styleUrl)

            setupMarkerManager(mapStyle)

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
        val mView = mapView ?: return

        // add a marker icon to the style
        BitmapFactory.decodeResource(resources, R.drawable.mapbox_marker_icon_default)?.let {
            mapStyle.addImage(MARKER_ICON_ID, it)
        }

        symbolManager = SymbolManager(mView, map, mapStyle)
        symbolManager?.iconAllowOverlap = true
        symbolManager?.iconIgnorePlacement = true
        symbolManager?.textAllowOverlap = false
        symbolManager?.textIgnorePlacement = false
        symbolManager?.iconRotationAlignment = ICON_ROTATION_ALIGNMENT_VIEWPORT

        val allMarker = mapViewModel.getAllActiveMarkers()
        Timber.d("AllMarkers: $allMarker")

        // recreate all markers that were on the map before the config change or process death
        mapViewModel.getAllActiveMarkers()?.forEach { coordinate ->
            symbolManager?.create(
                SymbolOptions()
                    .withLatLng(coordinate)
                    .withIconImage(MARKER_ICON_ID)
                // .withIconAnchor(ICON_ANCHOR_BOTTOM)
            )
        }

        // symbolManager?.addDragListener(this::onSymbolDragged)

        symbolManager?.addClickListener { symbol ->
            Toast.makeText(
                requireActivity(),
                "Clicked on marker ${symbol.id}",
                Toast.LENGTH_SHORT
            ).show()
            return@addClickListener false
        }

        // TODO this also calls the onMapLongClick and therefore spawns a new marker as well
        // -> deleting a marker should happen via its info window (e.g. a small 'delete this marker'-
        // button at the bottom)
        symbolManager?.addLongClickListener { symbol ->
            // remove a marker on long click
            symbolManager?.delete(symbol)
            return@addLongClickListener false
        }
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
        // ! This has method to return false as otherwise symbol clicks won't be fired.
        // If true this click is consumed and not passed to other listeners registered afterwards!
        return false
    }

    private fun onMapLongClicked(clickedPoint: LatLng): Boolean {
        symbolManager ?: return false

        // add a symbol to the long-clicked point
        val symbolOptions = SymbolOptions()
            .withLatLng(clickedPoint)
            // .withIconImage("cafe-15") // use maki icon set
            .withIconImage(MARKER_ICON_ID)
            // .withIconAnchor(ICON_ANCHOR_BOTTOM)
            .withIconSize(1.0f)
            // TODO right now draggable=true spawns a new marker; this seems to be an open issue
            .withDraggable(false)

        val marker = symbolManager?.create(symbolOptions)

        if (marker != null) {
            // save the marker in the viewmodel
            // mapViewModel.activeMarkers?.put(marker.id, marker)
            mapViewModel.saveMarker(marker)
        }

        binding.routeRetrievalProgressSpinner.visibility = View.VISIBLE
        // Place the destination marker at the map long click location
        map.getStyle {
            val clickPointSource = it.getSourceAs<GeoJsonSource>("CLICK_SOURCE")
            clickPointSource?.setGeoJson(
                Point.fromLngLat(
                    clickedPoint.longitude,
                    clickedPoint.latitude
                )
            )
        }

        generateRoute(clickedPoint)

        return false
    }

    private fun generateRoute(clickedPoint: LatLng) {
        if (!map.locationComponent.isLocationComponentActivated) {
            mapViewModel.getCurrentMapStyle()?.let {
                enableLocationComponent(it)
            }
        } else {
            map.locationComponent.lastKnownLocation?.let { originLocation ->
                val token = getMapboxAccessToken(requireActivity().applicationContext)
                mapboxNavigation?.requestRoutes(
                    RouteOptions.builder().applyDefaultParams()
                        .accessToken(token)
                        .coordinates(originLocation.toPoint(), null, clickedPoint.toPoint())
                        .alternatives(true)
                        .steps(true)
                        .bannerInstructions(true)
                        .voiceInstructions(false)
                        .profile(DirectionsCriteria.PROFILE_WALKING)
                        .build(),
                    routesReqCallback
                )
            }
        }
    }

    /**
     * * Location Tracking Code
     */

    @SuppressLint("MissingPermission")
    private fun enableLocationComponent(loadedMapStyle: Style) {
        val activity = activity ?: return

        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(activity)) {
            // Check if GPS is enabled in the device settings
            if (!isGPSEnabled(activity)) {
                Toast.makeText(
                    activity,
                    "GPS doesn't seem to be enabled! Please enable it in the device settings to continue!",
                    Toast.LENGTH_LONG
                ).show()
                return
            }

            val customLocationComponentOptions = LocationComponentOptions.builder(activity)
                // overwrite custom gestures detection to adjust the camera's focal point and increase
                // thresholds without breaking tracking
                .trackingGesturesManagement(true)
                // show a pulsing circle around the user position
                .pulseEnabled(true)
                .pulseFadeEnabled(true)
                // disable animations to decrease battery and cpu usage
                .compassAnimationEnabled(false)
                .accuracyAnimationEnabled(false)
                .build()

            val locationComponentActivationOptions =
                LocationComponentActivationOptions.builder(activity, loadedMapStyle)
                    .locationComponentOptions(customLocationComponentOptions)
                    // use our custom location engine below instead of the built-in location engine
                    // to track user location updates
                    .useDefaultLocationEngine(false)
                    .build()

            map.locationComponent.apply {
                // Activate the LocationComponent with options
                activateLocationComponent(locationComponentActivationOptions)
                // Enable to make the LocationComponent visible
                isLocationComponentEnabled = true
                // Set the LocationComponent's camera mode
                cameraMode = CameraMode.TRACKING
                // Set the LocationComponent's render mode
                renderMode = RenderMode.COMPASS
            }

            // init custom location engine
            initLocationEngine()
        } else {
            permissionsManager.requestLocationPermissions(activity)
        }
    }

    @SuppressLint("MissingPermission")
    private fun initLocationEngine() {
        val activityCtx = activity ?: return

        locationEngine = LocationEngineProvider.getBestLocationEngine(activityCtx)
        callback = LocationListeningCallback(this)

        val request = LocationEngineRequest.Builder(DEFAULT_INTERVAL_IN_MILLISECONDS)
            .setPriority(LocationEngineRequest.PRIORITY_BALANCED_POWER_ACCURACY)
            .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME)
            .build()

        val locationCallback = callback ?: return
        locationEngine?.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        locationEngine?.getLastLocation(locationCallback)
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
        Toast.makeText(
            requireActivity(),
            "Der Standortzugriff wird benötigt, um deine aktuelle Position auf der Karte anzuzeigen!",
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            // try to find the device location and enable location tracking
            map.style?.let { enableLocationComponent(it) }
        } else {
            Toast.makeText(
                requireActivity(),
                "Ohne die Berechtigung kann dein aktueller Standort nicht angezeigt werden!",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * * Lifecycle Hooks
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
        // remove location updates to prevent leaks
        callback?.let {
            locationEngine?.removeLocationUpdates(it)
            // TODO
            // mapViewModel.setLocationTrackingStatus(isActivated = false)
        }
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
        callback = null
        locationEngine = null

        removeMapListeners()
        symbolManager?.onDestroy()

        mapboxNavigation?.onDestroy()
        mapView?.onDestroy()
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("in MapFragment onDestroy")

        // mapViewModel.resetActiveMarkers()
    }

    companion object {
        // Note: this layer is not in all map styles available (e.g. the satellite style)!
        // private const val FIRST_SYMBOL_LAYER_ID = "waterway-label"
        // private val ICON_OFFSET = arrayOf(0f, -9f)

        private const val DEFAULT_INTERVAL_IN_MILLISECONDS = 1000L
        private const val DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 5

        private const val OUTER_CIRCLE_ALPHA = 0.8f
        private const val TARGET_ONE_RADIUS = 60
        private const val TARGET_TWO_RADIUS = 100

        private const val ROUTE_LINE_WIDTH = 6f
        private const val ROUTE_LINE_PRECISION = 6

        private const val ROUTE_LINE_SOURCE_ID = "ROUTE_LINE_SOURCE_ID"
        private const val ROUTE_LINE_LAYER_ID = "ROUTE_LINE_LAYER_ID"
        private const val ROUTE_MARKER_SOURCE_ID = "ROUTE_MARKER_SOURCE_ID"
        private const val ROUTE_MARKER_LAYER_ID = "ROUTE_MARKER_LAYER_ID"

        private const val ORIGIN_COLOR = "#32a852" // Green
        private const val DESTINATION_COLOR = "#F84D4D" // Red

        private const val MARKER_ICON_ID = "marker-icon-id"
        private const val GOAL_ICON_ID = "goal-icon-id"
    }

    /**
     * This class serves as a "callback" and is needed because a LocationEngine memory leak is
     * possible if the activity/fragment directly implements the LocationEngineCallback<LocationEngineResult>.
     * The WeakReference setup avoids the leak. See https://docs.mapbox.com/android/core/guides/
     */
    private class LocationListeningCallback constructor(fragment: MapFragment) :
        LocationEngineCallback<LocationEngineResult> {

        private val fragmentWeakReference: WeakReference<MapFragment> = WeakReference(fragment)

        /**
         * The LocationEngineCallback interface's method which fires when the device's location has changed.
         */
        override fun onSuccess(result: LocationEngineResult) {
            val fragment: MapFragment? = fragmentWeakReference.get()
            if (fragment != null) {
                val location = result.lastLocation ?: return

                // TODO use a separate map object class to access here to avoid the need of a WeakReference!
                // Pass the new location to the Maps SDK's LocationComponent
                fragment.map.locationComponent.forceLocationUpdate(location)
                // save the new location
                fragment.mapViewModel.setCurrentUserPosition(location)
            }
        }

        /**
         * The LocationEngineCallback interface's method which fires when the device's location can not be captured
         *
         * @param exception the exception message
         */
        override fun onFailure(exception: Exception) {
            val fragment: MapFragment? = fragmentWeakReference.get()
            if (fragment != null) {
                Toast.makeText(
                    fragment.activity,
                    exception.localizedMessage,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
