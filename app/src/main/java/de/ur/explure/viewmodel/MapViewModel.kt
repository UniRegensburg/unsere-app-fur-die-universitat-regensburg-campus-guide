package de.ur.explure.viewmodel

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.Symbol
import de.ur.explure.utils.Event

/**
 * Map Viewmodel to handle and preserve map state.
 */
@Suppress("TooManyFunctions")
class MapViewModel(private val state: SavedStateHandle) : ViewModel() {

    private val _mapReady = MutableLiveData<Event<Boolean>>()
    val mapReady: LiveData<Event<Boolean>> = _mapReady

    private var currentMapStyle: Style? = null

    // TODO only saving the latlng coords will probably not be enough later but symbol cannot be parcelized
    // -> probably create an own mapPin parcelize data class ? (e.g. https://github.com/amohnacs15/MeshMap/blob/master/app/src/main/java/com/zhudapps/meshmap/model/MapPin.kt)
    private val markers: MutableList<LatLng> = state[ACTIVE_MARKERS_KEY] ?: mutableListOf()

    fun saveMarker(marker: Symbol) {
        markers.add(marker.latLng)
    }

    fun saveActiveMarkers() {
        state[ACTIVE_MARKERS_KEY] = markers
    }

    fun getAllActiveMarkers(): List<LatLng>? {
        return state[ACTIVE_MARKERS_KEY]
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

    companion object {
        val All_MAP_STYLES = mapOf(
            "Streets" to Style.MAPBOX_STREETS,
            "Outdoors" to Style.OUTDOORS,
            "Satellite" to Style.SATELLITE_STREETS,
            "Night" to Style.TRAFFIC_NIGHT,
            "Dark" to Style.DARK,
        )

        // saved state keys
        private const val USER_LOCATION_KEY = "userLocation"
        private const val CAMERA_POSITION_KEY = "cameraPosition"
        private const val ACTIVE_MARKERS_KEY = "activeMarkers"
        private const val LOCATION_TRACKING_KEY = "locationTracking"
    }
}
