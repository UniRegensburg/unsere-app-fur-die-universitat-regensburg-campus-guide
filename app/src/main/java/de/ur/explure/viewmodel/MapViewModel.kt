package de.ur.explure.viewmodel

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.GeoPoint
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.Symbol
import de.ur.explure.model.MapMarker
import de.ur.explure.model.waypoint.WayPoint
import de.ur.explure.utils.Event
import java.util.*

/**
 * Map Viewmodel to handle and preserve map state.
 */
@Suppress("TooManyFunctions")
class MapViewModel(private val state: SavedStateHandle) : ViewModel() {

    private val _mapReady = MutableLiveData<Event<Boolean>>()
    val mapReady: LiveData<Event<Boolean>> = _mapReady

    private val _manualRouteCreationModeActive by lazy {
        MutableLiveData(state[MANUAL_ROUTE_CREATION_KEY] ?: false)
    }
    val manualRouteCreationModeActive: LiveData<Boolean> = _manualRouteCreationModeActive

    private val _routeDrawModeActive by lazy { MutableLiveData(state[ROUTE_DRAW_KEY] ?: false) }
    val routeDrawModeActive: LiveData<Boolean> = _routeDrawModeActive

    private var currentMapStyle: Style? = null

    val mapMarkers: MutableLiveData<MutableList<MapMarker>> by lazy {
        MutableLiveData(state[ACTIVE_MARKERS_KEY] ?: mutableListOf())
    }
    val selectedMarker: MutableLiveData<MapMarker> by lazy { MutableLiveData<MapMarker>() }

    // TODO schönere lösung hierfür finden, wenn Zeit
    // used to update the map marker symbols when a waypoint is deleted from the bottomsheet
    val deletedWaypoint: MutableLiveData<MapMarker> by lazy { MutableLiveData<MapMarker>() }

    fun addNewMapMarker(symbol: Symbol) {
        val coordinates = symbol.latLng

        // TODO bessere default-Werte!
        val waypoint = WayPoint(
            UUID.randomUUID().toString(),
            "Marker ${mapMarkers.value?.size}",
            "Keine Beschreibung (Position: ${coordinates.latitude}, ${coordinates.longitude})",
            GeoPoint(coordinates.latitude, coordinates.longitude)
        )

        val mapMarker = MapMarker(
            id = UUID.randomUUID().toString(),
            wayPoint = waypoint,
            markerPosition = coordinates
        )
        mapMarkers.value?.add(mapMarker)
        // assigning to itself is necessary to trigger the observer!
        mapMarkers.value = mapMarkers.value
    }

    /**
     * Used when a marker symbol has been removed directly from the map.
     * Removes this marker from the mapMarker list and from the bottom sheet.
     */
    fun removeMarker(marker: Symbol) {
        val removedWaypoint = mapMarkers.value?.find {
            it.markerPosition == marker.latLng
        }
        mapMarkers.value?.remove(removedWaypoint)
        mapMarkers.value = mapMarkers.value
    }

    /**
     * Used when a waypoint item has been removed from the bottom sheet.
     * Removes this marker from the mapMarker list and sets the deleted marker livedata to delete it
     * from the marker symbols as well.
     */
    fun removeWaypoint(waypointMarker: MapMarker) {
        mapMarkers.value?.remove(waypointMarker)
        mapMarkers.value = mapMarkers.value
        deletedWaypoint.value = waypointMarker
    }

    fun getAllActiveMarkers(): List<MapMarker>? {
        return state[ACTIVE_MARKERS_KEY]
    }

    fun saveActiveMarkers() {
        state[ACTIVE_MARKERS_KEY] = mapMarkers.value
    }

    fun removeActiveMarkers() {
        mapMarkers.value?.clear()
        mapMarkers.value = mapMarkers.value
    }

    fun setMapReadyStatus(status: Boolean) {
        _mapReady.value = Event(status) // Trigger the event by setting a new Event as a new value
    }

    fun setCurrentMapStyle(style: Style) {
        this.currentMapStyle = style
    }

    fun getCurrentMapStyle(): Style? {
        return this.currentMapStyle
    }

    fun setCurrentUserPosition(userPosition: Location) {
        state[USER_LOCATION_KEY] = userPosition
    }

    fun getLastKnownUserPosition(): Location? {
        return state[USER_LOCATION_KEY]
    }

    fun setCurrentCameraPosition(cameraPosition: CameraPosition) {
        state[CAMERA_POSITION_KEY] = cameraPosition
    }

    fun getLastKnownCameraPosition(): CameraPosition? {
        return state[CAMERA_POSITION_KEY]
    }

    fun isLocationTrackingActivated(): Boolean? {
        return state[LOCATION_TRACKING_KEY]
    }

    fun setLocationTrackingStatus(isEnabled: Boolean) {
        state[LOCATION_TRACKING_KEY] = isEnabled
    }

    fun setManualRouteCreationModeStatus(isActive: Boolean) {
        _manualRouteCreationModeActive.value = isActive
        state[MANUAL_ROUTE_CREATION_KEY] = isActive
    }

    fun setRouteDrawModeStatus(isActive: Boolean) {
        _routeDrawModeActive.value = isActive
        state[ROUTE_DRAW_KEY] = isActive
    }

    companion object {
        val All_MAP_STYLES = mapOf(
            "Standard" to Style.MAPBOX_STREETS,
            "Outdoors" to Style.OUTDOORS,
            "Satellite" to Style.SATELLITE_STREETS,
            "Night" to Style.TRAFFIC_NIGHT,
            "Dark" to Style.DARK,
        )

        // saved state keys
        private const val USER_LOCATION_KEY = "userLocation"
        private const val CAMERA_POSITION_KEY = "cameraPosition"
        private const val ACTIVE_MARKERS_KEY = "activeMarkers"
        private const val LOCATION_TRACKING_KEY = "locationTrackingActive"
        private const val MANUAL_ROUTE_CREATION_KEY = "manualRouteCreationActive"
        private const val ROUTE_DRAW_KEY = "routeDrawModeActive"
    }
}
