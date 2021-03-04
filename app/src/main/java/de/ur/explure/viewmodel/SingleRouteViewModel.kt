package de.ur.explure.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.ur.explure.model.rating.Rating
import de.ur.explure.model.route.Route
import de.ur.explure.model.waypoint.WayPoint
import de.ur.explure.repository.rating.RatingRepositoryImpl
import de.ur.explure.repository.route.RouteRepositoryImpl
import de.ur.explure.utils.FirebaseResult
import kotlinx.coroutines.launch
import java.util.*

@Suppress("StringLiteralDuplication")
class SingleRouteViewModel(
    private val routeRepository: RouteRepositoryImpl,
    private val ratingRepository: RatingRepositoryImpl
) : ViewModel() {

    private val _route: MutableLiveData<Route> = MutableLiveData()
    val route: LiveData<Route> = _route
    private val _waypointList: MutableLiveData<LinkedList<WayPoint>> = MutableLiveData(LinkedList())
    val waypointList: LiveData<LinkedList<WayPoint>> = _waypointList
    private val _rating: MutableLiveData<Rating> = MutableLiveData()
    val rating: LiveData<Rating> = _rating

    fun setRouteData() {
        /*viewModelScope.launch {
            val routeID : String = getRouteID()
            when (val routeData = routeRepository.getRoute(routeID, true)) {
                is FirebaseResult.Success -> {
                    _route.postValue(routeData.data)
                }
            }
        }*/
        viewModelScope.launch {
            when (val routeData = routeRepository.getRoute("83bAuunZzXwaPIJ0Xc3a")) {
                is FirebaseResult.Success -> {
                    _route.postValue(routeData.data)
                }
            }
        }
    }

    fun setWaypoints() {
        /*viewModelScope.launch {
            val routeID : String = getRouteID()
            when (val route = routeRepository.getRoute(routeID, true)) {
                is FirebaseResult.Success -> {
                        _waypointList.value = route.data.wayPoints
                }
            }
        }*/
        viewModelScope.launch {
            when (val route = routeRepository.getRoute("83bAuunZzXwaPIJ0Xc3a")) {
                is FirebaseResult.Success -> {
                    _waypointList.value = route.data.wayPoints
                }
            }
        }
    }

    fun setRating() {
        /*viewModelScope.launch {
            val ratingID = getRatingID()
            when (val rating = ratingRepository.getRating(ratingID)) {
                is FirebaseResult.Success -> {
                    _rating.postValue(rating.data)
                }
            }
        }*/
        viewModelScope.launch {
            when (val rating = ratingRepository.getRating("83bAuunZzXwaPIJ0Xc3a")) {
                is FirebaseResult.Success -> {
                    _rating.postValue(rating.data)
                }
            }
        }
    }

    private fun getRouteID(): String {
        var routeID = ""
        val routeList = _route.value
        if (routeList != null) {
            routeID = routeList.id
            }
        return routeID
    }

    private fun getRatingID(): String {
        var ratingID = ""
        val ratingList = _rating.value
        if (ratingList != null) {
            ratingID = ratingList.id
        }
        return ratingID
    }
}
