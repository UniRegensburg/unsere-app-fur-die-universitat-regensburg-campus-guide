package de.ur.explure.utils

import android.app.Activity
import android.widget.Toast
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import de.ur.explure.R

interface LocationPermissionListener {

    fun onLocationPermissionGranted()

    // fun onLocationPermissionNotGranted()
}

/**
 * **Usage:**
 *
 * Init:
 * ```
 * private lateinit var permissionHelper: PermissionHelper
 * ...
 * permissionHelper = get { parametersOf(requireActivity())
 * // set listener
 * permissionHelper.setLocationListener(this)
 * ```
 *
 * Cleanup in onDestroy or onDestroyView:
 * ```
 * permissionHelper.clearLocationListener()
 * ```
 */
// TODO context leaks!
class PermissionHelper(private val context: Activity) : PermissionsListener {

    // permission handling
    private var permissionsManager: PermissionsManager = PermissionsManager(this)

    private var locationPermissionListener: LocationPermissionListener? = null

    fun setLocationListener(listener: LocationPermissionListener) {
        locationPermissionListener = listener
    }

    /**
     * Call this in the onDestroy(View)-Lifecycle-Hook to prevent memory leaks.
     */
    fun clearLocationListener() {
        locationPermissionListener = null
    }

    /*
    fun checkLocationPermissionAndGPS(): Boolean {
        if (PermissionsManager.areLocationPermissionsGranted(context.applicationContext)) {
            // Check if GPS is enabled in the device settings
            if (!isGPSEnabled(context.applicationContext)) {
                Toast.makeText(
                    context.applicationContext,
                    context.applicationContext.getString(R.string.gps_not_activated),
                    Toast.LENGTH_LONG
                ).show()
                return true
            }
        }
        return false
    }
    */

    fun requestLocationPermissions() {
        // ! Activity context needed here!
        permissionsManager.requestLocationPermissions(context)
    }

    fun onRequestLocationPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        Toast.makeText(
            context.applicationContext,
            context.applicationContext.getString(R.string.location_permission_explanation),
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            // inform the listener that he has location permissions
            locationPermissionListener?.onLocationPermissionGranted()
        } else {
            Toast.makeText(
                context.applicationContext,
                context.applicationContext.getString(R.string.location_permission_not_given),
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
