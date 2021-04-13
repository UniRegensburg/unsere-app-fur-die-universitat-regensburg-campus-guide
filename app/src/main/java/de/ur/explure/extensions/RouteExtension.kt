package de.ur.explure.extensions

import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.DocumentSnapshot
import de.ur.explure.config.RouteDocumentConfig
import de.ur.explure.model.route.Route
import timber.log.Timber

fun MutableLiveData<MutableList<Route>>.appendRoutes(data: List<Route>) {
    val currentList = this.value
    if (currentList.isNullOrEmpty()) {
        this.postValue(data.toMutableList())
    } else {
        currentList.addAll(data)
        this.postValue(currentList)
    }
}

@Suppress("TooGenericExceptionCaught", "ReturnCount")
fun DocumentSnapshot.toRouteObject(): Route? {
    try {
        val route = this.toObject(Route::class.java)
        val lineString = this.getString(RouteDocumentConfig.ROUTE_LINE_FIELD)
        val coordinatesList = lineString?.toLatLngList() ?: return null
        route?.addRouteCoordinates(coordinatesList)
        return route
    } catch (e: Exception) {
        Timber.d("Failed to convert route from $this with $e")
        return null
    }
}
