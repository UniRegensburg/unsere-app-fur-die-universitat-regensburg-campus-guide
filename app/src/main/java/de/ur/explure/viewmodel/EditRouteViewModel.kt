package de.ur.explure.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapbox.geojson.Feature
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.geometry.LatLng
import de.ur.explure.extensions.toGeoPoint
import de.ur.explure.model.waypoint.WayPointDTO
import de.ur.explure.navigation.MainAppRouter
import de.ur.explure.repository.route.RouteRepositoryImpl
import de.ur.explure.utils.FirebaseResult
import de.ur.explure.utils.reorderList
import de.ur.explure.views.EditRouteFragment.Companion.PROPERTY_ID
import de.ur.explure.views.EditRouteFragmentDirections
import kotlinx.coroutines.launch
import timber.log.Timber

class EditRouteViewModel(
    private val state: SavedStateHandle,
    private val appRouter: MainAppRouter,
    private val routeRepository: RouteRepositoryImpl
) : ViewModel() {

    private var route: LineString? = state[ROUTE_KEY]
    var routeSnapshotUri: String? = state[SNAPSHOT_URI_KEY]
    // var routeId: String? = null // TODO save the id of the newly created route here!

    // TODO LinkedList ?
    val routeWayPoints: MutableLiveData<MutableList<WayPointDTO>> by lazy {
        MutableLiveData(state[ROUTE_WayPointS_KEY] ?: mutableListOf())
    }

    val selectedMarker: MutableLiveData<WayPointDTO> by lazy { MutableLiveData<WayPointDTO>() }

    // TODO schönere lösung hierfür finden, wenn Zeit
    // used to update the map marker symbols when a waypoint is deleted from the bottomsheet
    val deletedWaypoint: MutableLiveData<WayPointDTO> by lazy { MutableLiveData<WayPointDTO>() }

    val buildingExtrusionActive by lazy { MutableLiveData(state[BUILDING_KEY] ?: true) }

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

    /**
     * Used when a marker symbol has been removed directly from the map via it's info window.
     * Removes this marker from the waypoints list and from the bottom sheet.
     */
    fun deleteWaypoint(wayPoint: WayPointDTO) {
        routeWayPoints.value?.remove(wayPoint)
        routeWayPoints.value = routeWayPoints.value
    }

    /**
     * Used when a waypoint item has been removed from the bottom sheet.
     * Removes this marker from the waypoints list and sets the deleted marker livedata to delete it
     * from the marker symbols as well.
     */
    fun removeWaypointFromSheet(waypoint: WayPointDTO) {
        deleteWaypoint(waypoint)
        deletedWaypoint.value = waypoint
    }

    fun updateWaypointOrder(originalPos: Int, newPos: Int) {
        routeWayPoints.value?.let { reorderList(it, originalPos, newPos) }
    }

    fun updateWayPoint(updatedWayPoint: WayPointDTO) {
        val wayPointList = routeWayPoints.value
        wayPointList?.forEachIndexed { index, wayPoint ->
            if (wayPoint.geoPoint == updatedWayPoint.geoPoint) {
                wayPointList[index] = updatedWayPoint
            }
        }
        routeWayPoints.value = wayPointList
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

    fun getRouteCoordinates(): MutableList<Point>? {
        return route?.coordinates()
    }

    fun uploadRouteSnapshot(routeBitmap: Bitmap) {
        viewModelScope.launch {
            when (val uploadResult = routeRepository.uploadRouteThumbnail(routeBitmap)) {
                is FirebaseResult.Success -> {
                    Timber.d("Uploading snapshot was successful")
                    routeSnapshotUri = uploadResult.data.toString()
                    _snapshotUploadSuccessful.postValue(true)
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

    fun navigateToWayPointDialogFragment(wayPointDTO: WayPointDTO) {
        val directions =
            EditRouteFragmentDirections.actionEditRouteFragmentToCreateWayPointDialog(wayPointDTO)
        appRouter.getNavController()?.navigate(directions)
    }

    fun resetSnapshotUpload() {
        _snapshotUploadSuccessful.value = null
    }

    fun setBuildingExtrusionStatus(active: Boolean) {
        buildingExtrusionActive.value = active
        state[BUILDING_KEY] = active
    }

    companion object {
        private const val ROUTE_KEY = "recreatedRoute"
        private const val ROUTE_WayPointS_KEY = "routeWayPoints"
        private const val SNAPSHOT_URI_KEY = "snapshotUri"
        private const val BUILDING_KEY = "3dBuildingsActive"
    }
}
