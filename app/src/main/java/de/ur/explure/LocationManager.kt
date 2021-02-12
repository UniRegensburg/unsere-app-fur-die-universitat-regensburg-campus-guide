package de.ur.explure

import android.annotation.SuppressLint
import android.app.Application
import android.location.Location
import android.os.Looper
import android.widget.Toast
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.Style

// TODO doc not up to date
/**
 * Usage:
 *
 * Setup a locationManager - Instance:
 * ```
 * private lateinit var locationManagerInstance: LocationManager
 * ...
 * locationManagerInstance = LocationManager(application, lifecycle) { location ->
 *      // do something with the location
 * }
 * ```
 *
 * and enable location updates with
 *
 * ```locationManagerInstance.enable()```
 */

internal class LocationManager(
    private val context: Application,
    private var newLocationCallback: ((Location) -> Unit)? = null
) : DefaultLifecycleObserver {

    @get:VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    var enabled = false

    private var locationEngine: LocationEngine? = null
    private var locationUpdatesCallback: LocationEngineCallback<LocationEngineResult>? = null

    // private var callbackWeakReference = WeakReference(newLocationCallback)

    init {
        locationUpdatesCallback = object : LocationEngineCallback<LocationEngineResult> {
            override fun onSuccess(result: LocationEngineResult?) {
                val location = result?.lastLocation ?: return
                // invoke callback with new location
                newLocationCallback?.invoke(location)
                /*
                val callback = callbackWeakReference.get()
                if (callback != null) {
                    callback(location)
                }*/
            }

            override fun onFailure(exception: Exception) {
                Toast.makeText(
                    context.applicationContext,
                    exception.localizedMessage,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /*
    fun enable(
        locationComponent: LocationComponent,
        mapStyle: Style,
        useDefaultEngine: Boolean = true
    ) {
        // connect  to location updates  only if we are at least in the onStarted Lifecycle State
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            // check if already enabled
            if (!enabled) {
                activateLocationComponent(locationComponent, mapStyle, useDefaultEngine)
            } else {
                Toast.makeText(
                    context.applicationContext,
                    "The LocationManager is already enabled!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }*/

    @SuppressLint("MissingPermission")
    fun activateLocationComponent(
        locationComponent: LocationComponent,
        mapStyle: Style,
        useDefaultEngine: Boolean = true
    ) {
        val customLocationComponentOptions =
            LocationComponentOptions.builder(context.applicationContext)
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

        val options =
            LocationComponentActivationOptions.builder(context.applicationContext, mapStyle)
                .locationComponentOptions(customLocationComponentOptions)
                // whether to use our custom location engine instead of the built-in location engine
                // to track user location updates
                .useDefaultLocationEngine(useDefaultEngine)
                .build()

        locationComponent.apply {
            // Activate the LocationComponent with options
            activateLocationComponent(options)
            // Enable to make the LocationComponent visible
            isLocationComponentEnabled = true
            // Set the LocationComponent's camera mode
            cameraMode = CameraMode.TRACKING
            // Set the LocationComponent's render mode
            renderMode = RenderMode.COMPASS
        }

        if (!useDefaultEngine) {
            initCustomLocationEngine()
        }
    }

    @SuppressLint("MissingPermission")
    private fun initCustomLocationEngine() {
        locationEngine = LocationEngineProvider.getBestLocationEngine(context.applicationContext)

        val request = LocationEngineRequest.Builder(DEFAULT_INTERVAL_IN_MILLISECONDS)
            .setPriority(LocationEngineRequest.PRIORITY_BALANCED_POWER_ACCURACY)
            .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME)
            .build()

        val callback = locationUpdatesCallback ?: return
        locationEngine?.requestLocationUpdates(request, callback, Looper.getMainLooper())
        locationEngine?.getLastLocation(callback)
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        if (enabled) {
            enabled = false

            // remove location updates to prevent leaks
            locationUpdatesCallback?.let { locationEngine?.removeLocationUpdates(it) }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        locationEngine = null
        locationUpdatesCallback = null
        newLocationCallback = null
    }

    companion object {
        private const val DEFAULT_INTERVAL_IN_MILLISECONDS = 1000L
        private const val DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 5
    }

    /*
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
    */
}
