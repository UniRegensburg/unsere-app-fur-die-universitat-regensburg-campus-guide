package de.ur.explure.viewmodel

import android.location.Location
import androidx.collection.LongSparseArray
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.Symbol
import de.ur.explure.utils.Event

/**
 * Map Viewmodel to handle and preserve map state.
 */
class MapViewModel : ViewModel() {

    private val _mapReady = MutableLiveData<Event<Boolean>>()
    val mapReady: LiveData<Event<Boolean>> = _mapReady

    // current map state
    // TODO reset them to null in mapfragment onDestroy?
    private var currentMapStyle: Style? = null
    var lastKnownUserLocation: Location? = null
    var lastKnownCameraPosition: CameraPosition? = null

    var activeMarkers: LongSparseArray<Symbol>? = LongSparseArray()

    fun setMapReadyStatus(status: Boolean) {
        _mapReady.value = Event(status) // Trigger the event by setting a new Event as a new value
    }

    fun setCurrentMapStyle(style: Style) {
        this.currentMapStyle = style
    }

    fun getCurrentMapStyle(): Style? {
        return this.currentMapStyle
    }

    companion object {
        val All_MAP_STYLES = mapOf(
            "Streets" to Style.MAPBOX_STREETS,
            "Outdoors" to Style.OUTDOORS,
            "Satellite" to Style.SATELLITE_STREETS,
            "Night" to Style.TRAFFIC_NIGHT,
            "Dark" to Style.DARK,
        )
    }
}
