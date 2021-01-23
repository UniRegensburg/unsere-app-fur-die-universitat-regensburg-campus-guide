package de.ur.explure.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * Map Viewmodel to handle and preserve map state.
 */

class MapViewModel : ViewModel() {
    val mapReady = MutableLiveData(false)
}
