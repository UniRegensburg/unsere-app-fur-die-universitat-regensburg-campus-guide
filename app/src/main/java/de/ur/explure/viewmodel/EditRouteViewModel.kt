package de.ur.explure.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapbox.geojson.Feature
import com.mapbox.geojson.LineString
import com.mapbox.mapboxsdk.geometry.LatLng
import de.ur.explure.extensions.toGeoPoint
import de.ur.explure.model.waypoint.WayPointDTO
import de.ur.explure.navigation.MainAppRouter
import de.ur.explure.repository.route.RouteRepositoryImpl
import de.ur.explure.utils.FirebaseResult
import de.ur.explure.views.EditRouteFragment.Companion.PROPERTY_ID
import kotlinx.coroutines.launch
import timber.log.Timber

class EditRouteViewModel(
    private val state: SavedStateHandle,
    private val appRouter: MainAppRouter,
    private val routeRepository: RouteRepositoryImpl
) : ViewModel() {

    private var route: LineString? = state[ROUTE_KEY]

    var uploadedRouteUri: String? = state[SNAPSHOT_URI_KEY]
    // var routeId: String? = null // TODO save the id of the newly created route here!

    private val routeWayPoints: MutableLiveData<MutableList<WayPointDTO>> by lazy {
        MutableLiveData(state[ROUTE_WayPointS_KEY] ?: mutableListOf())
    }

    private val _snapshotUploadSuccessful: MutableLiveData<Boolean?> by lazy { MutableLiveData() }
    val snapshotUploadSuccessful = _snapshotUploadSuccessful

    fun setInitialWayPoints(wayPoints: List<WayPointDTO>?) {
        if (wayPoints != null) {
            routeWayPoints.value = wayPoints.toMutableList()
        }
    }

    fun addNewWayPoint(coordinates: LatLng, defaultTitle: String): WayPointDTO {
        val wayPoint = WayPointDTO(
            title = defaultTitle,
            geoPoint = coordinates.toGeoPoint()
        )
        routeWayPoints.value?.add(wayPoint)
        routeWayPoints.value = routeWayPoints.value

        return wayPoint
    }

    fun saveWayPoints() {
        state[ROUTE_WayPointS_KEY] = routeWayPoints.value
    }

    fun getWayPoints(): MutableList<WayPointDTO>? {
        return routeWayPoints.value
    }

    fun getWaypointForFeature(feature: Feature): WayPointDTO? {
        return routeWayPoints.value?.find {
            it.geoPoint.toString() == feature.getStringProperty(PROPERTY_ID)
        }
    }

    fun deleteWaypoint(wayPoint: WayPointDTO) {
        routeWayPoints.value?.remove(wayPoint)
        routeWayPoints.value = routeWayPoints.value
    }

    fun clearAllWaypoints() {
        routeWayPoints.value?.clear()
        routeWayPoints.value = routeWayPoints.value
    }

    fun saveRoute(routeLine: LineString) {
        route = routeLine
        state[ROUTE_KEY] = routeLine
    }

    fun getRoute(): LineString? {
        return route
    }

    fun uploadRouteSnapshot(routeBitmap: Bitmap) {
        viewModelScope.launch {
            // TODO show progress ?
            when (val uploadResult = routeRepository.uploadRouteThumbnail(routeBitmap)) {
                is FirebaseResult.Success -> {
                    Timber.d("Uploading snapshot was successful")
                    _snapshotUploadSuccessful.postValue(true)
                    // TODO uploadedRouteUri = routeUri

                    val storagePath = uploadResult.data.metadata?.path
                    val storageReference = uploadResult.data.metadata?.reference
                    val uri = storageReference?.downloadUrl
                    // val uri = storageReference?.downloadUrl?.result
                    Timber.d("url in viewmodel: $uri")

                    // TODO save the uri in the route's entry in firestore so we can access it later !!
                }
                is FirebaseResult.Canceled -> {
                    Timber.d("Uploading snapshot was canceled")
                    _snapshotUploadSuccessful.postValue(false)
                }
                is FirebaseResult.Error -> {
                    Timber.d("Uploading snapshot failed")
                    _snapshotUploadSuccessful.postValue(false)
                }
            }
        }
    }

    fun resetSnapshotUpload() {
        _snapshotUploadSuccessful.value = null
    }

    companion object {
        private const val ROUTE_KEY = "recreatedRoute"
        private const val ROUTE_WayPointS_KEY = "routeWayPoints"
        private const val SNAPSHOT_URI_KEY = "snapshotUri"
    }
}
