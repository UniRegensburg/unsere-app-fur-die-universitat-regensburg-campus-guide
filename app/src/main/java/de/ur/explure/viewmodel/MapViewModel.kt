package de.ur.explure.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import de.ur.explure.utils.Event

/**
 * Map Viewmodel to handle and preserve map state.
 */

class MapViewModel : ViewModel() {
    private val _mapReady = MutableLiveData<Event<Boolean>>()

    val mapReady: LiveData<Event<Boolean>>
        get() = _mapReady

    fun setMapReadyStatus(status: Boolean) {
        _mapReady.value = Event(status) // Trigger the event by setting a new Event as a new value
    }
}
