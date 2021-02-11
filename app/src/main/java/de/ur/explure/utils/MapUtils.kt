package de.ur.explure.utils

import android.app.Activity
import android.content.Context
import android.location.LocationManager
import com.mapbox.mapboxsdk.Mapbox
import de.ur.explure.R
import timber.log.Timber

/**
 * Checks if GPS is enabled in the system settings.
 */

fun isGPSEnabled(context: Activity): Boolean {
    val locManager: LocationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
    return locManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
}

/**
 * Taken from https://github.com/mapbox/mapbox-navigation-android/blob/main/examples/src/main/java/com/mapbox/navigation/examples/utils/Utils.java
 * and slightly adjusted.
 *
 * Returns the Mapbox access token set in the app resources.
 *
 * It will first search for a token in the Mapbox object. If not found it
 * will then attempt to load the access token from the string resources.
 *
 * @param context The [Context] of the [android.app.Activity] or [android.app.Fragment].
 * @return The Mapbox access token or null if not found.
 */

fun getMapboxAccessToken(context: Context): String {
    return try {
        // Read out AndroidManifest
        val token = Mapbox.getAccessToken()
        require(!(token == null || token.isEmpty()))
        token
    } catch (exception: IllegalArgumentException) {
        // Use fallback on string resource
        Timber.w("Getting mapbox token from manifest not possible: $exception\nChecking string ressources instead!")
        context.getString(R.string.access_token)
    }
}
