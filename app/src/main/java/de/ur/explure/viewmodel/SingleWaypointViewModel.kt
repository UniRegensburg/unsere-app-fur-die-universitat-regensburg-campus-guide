package de.ur.explure.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import de.ur.explure.model.waypoint.WayPoint
import de.ur.explure.repository.route.RouteRepositoryImpl

class SingleWaypointViewModel(private val routeRepo: RouteRepositoryImpl) : ViewModel() {

    val wayPoint : MutableLiveData<WayPoint> = MutableLiveData()

    fun setWayPoint(wayPointData: WayPoint) {
        wayPoint.postValue(wayPointData)
    }

}