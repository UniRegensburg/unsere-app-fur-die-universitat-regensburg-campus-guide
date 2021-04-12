package de.ur.explure.viewmodel

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.mapbox.api.directions.v5.models.DirectionsRoute
import de.ur.explure.utils.Event

class NavigationViewModel(private val savedState: SavedStateHandle) : ViewModel() {

    val buildingExtrusionActive by lazy {
        MutableLiveData(
            savedState[BUILDING_EXTRUSION_KEY] ?: true
        )
    }

    var directionsRoute: DirectionsRoute? = null

    private val _inNavigationMode: MutableLiveData<Event<Boolean>> by lazy {
        MutableLiveData(Event(savedState[NAVIGATION_ACTIVE] ?: false))
    }
    val inNavigationMode: LiveData<Event<Boolean>> = _inNavigationMode

    fun setCurrentUserPosition(userPosition: Location) {
        savedState[USER_LOCATION_KEY] = userPosition
    }

    fun getLastKnownUserPosition(): Location? {
        return savedState[USER_LOCATION_KEY]
    }

    fun isLocationTrackingActivated(): Boolean? {
        return savedState[LOCATION_TRACKING_KEY]
    }

    fun setLocationTrackingStatus(isEnabled: Boolean) {
        savedState[LOCATION_TRACKING_KEY] = isEnabled
    }

    fun setBuildingExtrusionStatus(active: Boolean) {
        buildingExtrusionActive.value = active
        savedState[BUILDING_EXTRUSION_KEY] = active
    }

    fun enterNavigationMode() {
        _inNavigationMode.value = Event(true)
        savedState[NAVIGATION_ACTIVE] = true
    }

    fun leaveNavigationMode() {
        _inNavigationMode.value = Event(false)
        savedState[NAVIGATION_ACTIVE] = false
    }

    companion object {
        private const val USER_LOCATION_KEY = "currentLocation"
        private const val LOCATION_TRACKING_KEY = "trackingActive"
        private const val BUILDING_EXTRUSION_KEY = "3D_buildingExtrusionActive"

        private const val NAVIGATION_ACTIVE = "navigationActive"
    }
}
