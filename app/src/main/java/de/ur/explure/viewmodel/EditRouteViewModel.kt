package de.ur.explure.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.Feature
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import de.ur.explure.extensions.toGeoPoint
import de.ur.explure.model.waypoint.WayPointDTO
import de.ur.explure.navigation.MainAppRouter
import de.ur.explure.utils.CachedFileUtils
import de.ur.explure.utils.reorderList
import de.ur.explure.views.EditRouteFragment.Companion.PROPERTY_ID
import de.ur.explure.views.EditRouteFragmentDirections

@Suppress("ReturnCount")
class EditRouteViewModel(
    private val state: SavedStateHandle,
    private val appRouter: MainAppRouter
) : ViewModel() {

    private var route: LineString? = state[ROUTE_KEY]

    val routeWayPoints: MutableLiveData<MutableList<WayPointDTO>> by lazy {
        MutableLiveData(state[ROUTE_WayPointS_KEY] ?: mutableListOf())
    }

    val selectedMarker: MutableLiveData<WayPointDTO> by lazy { MutableLiveData<WayPointDTO>() }

    // used to update the map marker symbols when a waypoint is deleted from the bottomsheet
    val deletedWaypoint: MutableLiveData<WayPointDTO> by lazy { MutableLiveData<WayPointDTO>() }

    val buildingExtrusionActive by lazy { MutableLiveData(state[BUILDING_KEY] ?: true) }

    private var mapSnapshot: Uri? = null

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

    fun navigateToWayPointDialogFragment(wayPointDTO: WayPointDTO) {
        val directions =
            EditRouteFragmentDirections.actionEditRouteFragmentToCreateWayPointDialog(wayPointDTO)
        appRouter.getNavController()?.navigate(directions)
    }

    fun setBuildingExtrusionStatus(active: Boolean) {
        buildingExtrusionActive.value = active
        state[BUILDING_KEY] = active
    }

    fun uploadRoute(callback: () -> Unit) {
        val route = route ?: return
        val waypoints = getWayPoints()?.toTypedArray() ?: return
        val snapShot = mapSnapshot

        val routeCoordinates: MutableList<Point> = route.coordinates()
        val routeLength = TurfMeasurement.length(routeCoordinates, TurfConstants.UNIT_METERS)
        val routeDuration = routeLength * WALKING_SPEED / 60

        callback()

        val action = EditRouteFragmentDirections.actionEditRouteFragmentToSaveRouteFragment(
            route = route.toPolyline(Constants.PRECISION_6),
            routeThumbnail = snapShot,
            waypoints = waypoints,
            distance = routeLength.toFloat(),
            duration = routeDuration.toFloat()
        )
        appRouter.navigateToSaveRouteFragment(action)
    }

    fun setRouteSnapshot(context: Context, snapshot: Bitmap) {
        mapSnapshot = CachedFileUtils.getUriFromBitmap(context, snapshot)
    }

    companion object {
        private const val ROUTE_KEY = "recreatedRoute"
        private const val ROUTE_WayPointS_KEY = "routeWayPoints"
        private const val BUILDING_KEY = "3dBuildingsActive"

        // in m/s, see https://en.wikipedia.org/wiki/Preferred_walking_speed
        private const val WALKING_SPEED = 1.4
        // private const val WALKING_SPEED = 0.83 // Spaziergang
    }
}
