package de.ur.explure.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapbox.geojson.LineString
import com.mapbox.mapboxsdk.geometry.LatLng
import de.ur.explure.extensions.toGeoPoint
import de.ur.explure.model.MapMarker
import de.ur.explure.model.waypoint.WayPointDTO
import de.ur.explure.navigation.MainAppRouter
import de.ur.explure.repository.route.RouteRepositoryImpl
import de.ur.explure.utils.FirebaseResult
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*

class EditRouteViewModel(
    private val state: SavedStateHandle,
    private val appRouter: MainAppRouter,
    private val routeRepository: RouteRepositoryImpl
) : ViewModel() {

    var route: LineString? = state[ROUTE_KEY]
    var routeMarkers: List<MapMarker>? = state[ROUTE_MARKERS_KEY]

    var uploadedRouteUri: String? = state[SNAPSHOT_URI_KEY]
    var routeId: String? = null // TODO save the id of the newly created route here!

    private val routeWaypoints: MutableLiveData<MutableList<WayPointDTO>> by lazy {
        MutableLiveData(state[ROUTE_WAYPOINTS_KEY] ?: mutableListOf())
    }

    private val _snapshotUploadSuccessful: MutableLiveData<Boolean> by lazy { MutableLiveData() }
    val snapshotUploadSuccessful = _snapshotUploadSuccessful

    // TODO größtenteils duplicate code (siehe MapViewmodel)
    fun addNewWaypoint(coordinates: LatLng): String {
        val waypoint = WayPointDTO(
            title = UUID.randomUUID().toString(),
            geoPoint = coordinates.toGeoPoint()
        )
        routeWaypoints.value?.add(waypoint)
        routeWaypoints.value = routeWaypoints.value
        return waypoint.title
    }

    fun saveWaypoints() {
        state[ROUTE_WAYPOINTS_KEY] = routeWaypoints.value
    }

    fun getWaypoints(): MutableList<WayPointDTO>? {
        return routeWaypoints.value
    }

    fun saveRoute(routeLine: LineString) {
        route = routeLine
        state[ROUTE_KEY] = routeLine
    }

    fun saveMapMarkers(mapMarkers: List<MapMarker>?) {
        routeMarkers = mapMarkers
        state[ROUTE_MARKERS_KEY] = mapMarkers
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

    companion object {
        private const val ROUTE_KEY = "recreatedRoute"
        private const val ROUTE_MARKERS_KEY = "recreatedRouteMarkers"
        private const val ROUTE_WAYPOINTS_KEY = "routeWaypoints"
        private const val SNAPSHOT_URI_KEY = "snapshotUri"
    }
}
