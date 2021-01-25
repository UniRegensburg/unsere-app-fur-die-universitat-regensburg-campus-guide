package de.ur.explure.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mapbox.mapboxsdk.maps.Style
import de.ur.explure.utils.Event

/**
 * Map Viewmodel to handle and preserve map state.
 */

class MapViewModel : ViewModel() {
    private var currentMapStyle: Style? = null

    private val _mapReady = MutableLiveData<Event<Boolean>>()

    val mapReady: LiveData<Event<Boolean>>
        get() = _mapReady

    fun setMapReadyStatus(status: Boolean) {
        _mapReady.value = Event(status) // Trigger the event by setting a new Event as a new value
    }

    fun setMapStyle(style: Style) {
        this.currentMapStyle = style
    }

    fun getMapStyle(): Style? {
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
