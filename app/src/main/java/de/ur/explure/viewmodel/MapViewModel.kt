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
    private var styleIndex: Int = 0
    private var currentMapStyle: Style? = null

    private val _mapReady = MutableLiveData<Event<Boolean>>()

    val mapReady: LiveData<Event<Boolean>>
        get() = _mapReady

    fun setMapReadyStatus(status: Boolean) {
        _mapReady.value = Event(status) // Trigger the event by setting a new Event as a new value
    }

    fun getNextMapStyleId(): String {
        styleIndex++
        if (styleIndex == All_MAP_STYLES.size) {
            // reset to the first style
            styleIndex = 0
        }

        return All_MAP_STYLES[styleIndex]
    }

    fun getActiveMapStyleId(): String {
        return All_MAP_STYLES[styleIndex]
    }

    fun setMapStyle(style: Style) {
        this.currentMapStyle = style
    }

    fun getMapStyle(): Style? {
        return this.currentMapStyle
    }

    companion object {
        private val All_MAP_STYLES = arrayOf(
            Style.MAPBOX_STREETS,
            Style.OUTDOORS,
            Style.LIGHT,
            Style.DARK,
            Style.SATELLITE,
            Style.SATELLITE_STREETS,
            Style.TRAFFIC_DAY,
            Style.TRAFFIC_NIGHT
        )
    }
}
