package de.ur.explure.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.ur.explure.model.user.User
import de.ur.explure.navigation.MainAppRouter
import de.ur.explure.repository.rating.RatingRepositoryImpl
import de.ur.explure.repository.route.RouteRepositoryImpl
import de.ur.explure.repository.user.UserRepositoryImpl
import de.ur.explure.utils.FirebaseResult
import kotlinx.coroutines.launch

class StatisticsViewModel(
    private val userRepo: UserRepositoryImpl,
    private val ratingRepo: RatingRepositoryImpl,
    private val routeRepo: RouteRepositoryImpl,
    private val appRouter: MainAppRouter
) : ViewModel() {

    var user: MutableLiveData<User> = MutableLiveData()
    var traveledDistance: MutableLiveData<String> = MutableLiveData()
    var createdWaypoints: MutableLiveData<String> = MutableLiveData()

    fun getUserInfo() {
        viewModelScope.launch() {
            val userInfo = userRepo.getUserInfo()
            when (userInfo) {
                is FirebaseResult.Success -> {
                    user.postValue(userInfo.data)
                }
            }
        }
    }

    fun getTraveledDistance() {
        viewModelScope.launch {
            val userInfo = userRepo.getUserInfo()
            when (userInfo) {
                is FirebaseResult.Success -> {
                    val routeInfo = routeRepo.getRoutes(userInfo.data.finishedRoutes)
                    when (routeInfo) {
                        is FirebaseResult.Success -> {
                            var dist = 0.0
                            for (route in routeInfo.data) {
                                dist += route.distance
                            }
                            traveledDistance.postValue(dist.toString() + "m")
                        }
                    }
                }
            }
        }
    }

    fun getCreatedWaypoints() {
        viewModelScope.launch {
            val userInfo = userRepo.getUserInfo()
            when (userInfo) {
                is FirebaseResult.Success -> {
                    val routeInfo = routeRepo.getRoutes(userInfo.data.createdRoutes)
                    when (routeInfo) {
                        is FirebaseResult.Success -> {
                            var waypoints = 0
                            for (route in routeInfo.data) {
                                waypoints += route.wayPoints.size
                            }
                            createdWaypoints.postValue(waypoints.toString())
                        }
                    }
                }
            }
        }
    }
}
