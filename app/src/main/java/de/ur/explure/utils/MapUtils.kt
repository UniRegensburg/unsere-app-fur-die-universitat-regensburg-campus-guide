package de.ur.explure.utils

import android.app.Activity
import android.content.Context
import android.location.LocationManager
import de.ur.explure.R

/**
 * Checks if GPS is enabled in the system settings.
 */
fun isGPSEnabled(context: Activity): Boolean {
    val locManager: LocationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
    return locManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
}

/**
 * Returns the mapbox access token for this application.
 */
fun getMapboxAccessToken(context: Context): String {
    return context.applicationContext.getString(R.string.access_token)
}
