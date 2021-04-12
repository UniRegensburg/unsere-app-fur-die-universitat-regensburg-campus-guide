package de.ur.explure.map

import android.view.Gravity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import de.ur.explure.map.RouteLineManager.Companion.MAPBOX_FIRST_LABEL_LAYER
import de.ur.explure.utils.SharedPreferencesManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

class MapHelper(
    private var mapView: MapView,
    private val lifecycle: Lifecycle
) : DefaultLifecycleObserver, KoinComponent, OnMapReadyCallback {

    private var mapHelperListener: MapHelperListener? = null

    // map
    lateinit var map: MapboxMap
    lateinit var markerManager: MarkerManager
    var routeLineManager: RouteLineManager? = null
    var buildingPlugin: CustomBuildingPlugin? = null

    // SharedPrefs
    private val preferencesManager: SharedPreferencesManager by inject()

    init {
        mapView.getMapAsync(this)
    }

    fun setMapHelperListener(listener: MapHelperListener) {
        mapHelperListener = listener
    }

    fun isMapInitialized(): Boolean {
        return ::map.isInitialized
    }

    fun isMarkerManagerInitialized(): Boolean {
        return ::markerManager.isInitialized
    }

    /**
     * * Map setup code
     */

    override fun onMapReady(mapboxMap: MapboxMap) {
        map = mapboxMap
        setupMapUI()

        mapHelperListener?.onMapLoaded(map)

        val style = preferencesManager.getCurrentMapStyle()
        setMapStyle(style)
    }

    private fun setupMapUI() {
        // restrict the camera to a given bounding box as the app focuses only on the uni campus
        map.setLatLngBoundsForCameraTarget(latLngBounds)

        // move the compass to the bottom left corner of the mapView so it doesn't overlap with buttons
        map.uiSettings.compassGravity = Gravity.BOTTOM or Gravity.START
        map.uiSettings.setCompassMargins(compassMarginLeft, 0, 0, compassMarginBottom)
    }

    fun setMapStyle(styleUrl: String?) {
        styleUrl ?: return

        map.setStyle(styleUrl) { mapStyle ->
            // Map is set up and the style has loaded.
            // save the current style in the shared preferences
            preferencesManager.setCurrentMapStyle(styleUrl)

            // setup other helper classes
            setupMarkerManager(mapStyle)
            setupRouteLineManager(mapStyle)

            // setup building plugin
            setupBuildingExtrusions(mapStyle)

            mapHelperListener?.onMapStyleLoaded(mapStyle)
        }
    }

    private fun setupMarkerManager(currentMapStyle: Style) {
        markerManager = get { parametersOf(mapView, map, currentMapStyle) }
        // let the marker manager observe the fragment lifecycle so it can clean itself up on destroy
        lifecycle.addObserver(markerManager)
        // mapHelperListener?.onMarkerManagerSetup()
    }

    private fun setupRouteLineManager(currentMapStyle: Style) {
        routeLineManager = get { parametersOf(mapView, map, currentMapStyle) }
        routeLineManager?.let { lifecycle.addObserver(it) }
        // mapHelperListener?.onRouteLineManagerSetup()
    }

    private fun setupBuildingExtrusions(mapStyle: Style) {
        // setup the building plugin below the map labels so map click events are not consumed here!
        buildingPlugin = CustomBuildingPlugin(mapStyle, MAPBOX_FIRST_LABEL_LAYER)

        val visibility = preferencesManager.getBuildingExtrusionShown()
        buildingPlugin?.setVisibility(visibility)
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        mapView.onStart()
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        mapView.onPause()
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        mapView.onResume()
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        mapView.onStop()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        // cleanup
        mapView.onDestroy()
        mapHelperListener = null
    }

    companion object {
        // custom margins of the mapbox compass
        private const val compassMarginLeft = 10
        private const val compassMarginBottom = 100

        // camera bounding box (only the relevant part of Regensburg around the university)
        private const val southWestLatitude = 48.990768
        private const val southWestLongitude = 12.087611
        private const val northEastLatitude = 49.006718
        private const val northEastLongitude = 12.101880
        private val southWestCorner = LatLng(southWestLatitude, southWestLongitude)
        private val northEastCorner = LatLng(northEastLatitude, northEastLongitude)

        private val latLngBounds: LatLngBounds = LatLngBounds.Builder()
            .include(southWestCorner)
            .include(northEastCorner)
            .build()
    }

    interface MapHelperListener {
        fun onMapLoaded(map: MapboxMap)
        fun onMapStyleLoaded(mapStyle: Style)
        // fun onMarkerManagerSetup()
        // fun onRouteLineManagerSetup()
    }
}
