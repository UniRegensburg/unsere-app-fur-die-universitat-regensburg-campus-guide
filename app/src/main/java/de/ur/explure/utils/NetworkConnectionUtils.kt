package de.ur.explure.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.annotation.StringRes
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.ur.explure.R

/**
 * Checks if the device is connected with a network.
 * Taken from https://stackoverflow.com/questions/53532406/activenetworkinfo-type-is-deprecated-in-api-level-28
 */
private fun isNetworkAvailable(context: Context) =
    (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).run {
        getNetworkCapabilities(activeNetwork)?.run {
            hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } ?: false
    }

/**
 * Util-Method that pings a Google Server to test if the internet connection works.
 * Taken from https://stackoverflow.com/questions/1560788/how-to-check-internet-access-on-android-inetaddress-never-times-out/27312494#27312494
 */
/*
private fun hasInternetConnection(): Single<Boolean> {
    return Single.fromCallable {
        try {
            // Connect to Google DNS to check for connection
            val timeoutMs = 1500
            val socket = Socket()
            val socketAddress = InetSocketAddress("8.8.8.8", 53)

            socket.connect(socketAddress, timeoutMs)
            socket.close()

            true
        } catch (e: IOException) {
            false
        }
    }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
}
*/

/**
 * Show alert dialog when the device has no internet connection.
 */
private fun showConnectionAlert(context: Context, noInternetMessage: String) {
    MaterialAlertDialogBuilder(context)
        .setTitle(R.string.no_internet)
        .setMessage(noInternetMessage)
        .setIcon(R.drawable.ic_baseline_warning_24)
        .setPositiveButton(android.R.string.ok) { _, _ -> }
        .setNegativeButton(android.R.string.cancel) { _, _ -> }
        .show()
}

/**
 *  Checks if the device has an internet connection.
 */
fun hasInternetConnection(
    context: Context,
    @StringRes noInternetMessage: Int = R.string.no_internet_default_explanation
): Boolean {
    val message = context.getString(noInternetMessage)

    if (!isNetworkAvailable(context)) {
        showConnectionAlert(context, message)
        return false
    }

    return true
}
