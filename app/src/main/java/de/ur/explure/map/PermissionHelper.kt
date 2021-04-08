package de.ur.explure.map

import android.app.Activity
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager

/**
 * **Usage with Koin:**
 *
 * ```
 * private val permissionHelper: PermissionHelper by inject()
 * ...
 * permissionHelper.requestLocationPermissions(requireActivity(), callback1, callback2)
 * ```
 *
 * The main activity also has to override ```onRequestPermissionResult``` and needs to call
 * ```
 * permissionHelper.onRequestLocationPermissionsResult(requestCode, permissions, grantResults)
 * ```
 * inside.
 */
class PermissionHelper : PermissionsListener {

    // MapBox permission manager
    private val permissionsManager: PermissionsManager by lazy {
        PermissionsManager(this)
    }

    private var onPermissionsResultCallback: ((Boolean) -> Unit)? = null
    private var onPermissionsExplanationNeededCallback: (() -> Unit)? = null

    /**
     * Call this to request location permissions.
     *
     * @param activityContext An activity context
     * @param onPermissionsResultCallback If given, it will return whether permissions were granted or not
     * @param onPermissionsExplanationNeededCallback Can be used to explain why the permissions are necessary
     */
    fun requestLocationPermissions(
        activityContext: Activity,
        onPermissionsResultCallback: ((Boolean) -> Unit)? = null,
        onPermissionsExplanationNeededCallback: (() -> Unit)? = null
    ) {
        this.onPermissionsResultCallback = onPermissionsResultCallback
        this.onPermissionsExplanationNeededCallback = onPermissionsExplanationNeededCallback

        // Needs to be given an activity context here as this calls
        // ActivityCompat.requestPermissions(activity, permissions, requestCode) under the hood!
        // see https://github.com/mapbox/mapbox-events-android/issues/395
        permissionsManager.requestLocationPermissions(activityContext)
    }

    /**
     * Call this in the activities ```onRequestPermissionsResult()``` method after the super call.
     */
    fun onRequestLocationPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onPermissionResult(granted: Boolean) {
        onPermissionsResultCallback?.invoke(granted)
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        onPermissionsExplanationNeededCallback?.invoke()
    }
}
