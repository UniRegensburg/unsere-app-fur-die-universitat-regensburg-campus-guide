package de.ur.explure.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.ur.explure.model.route.Route
import de.ur.explure.model.user.User
import de.ur.explure.navigation.MainAppRouter
import de.ur.explure.repository.route.RouteRepositoryImpl
import de.ur.explure.repository.user.UserRepositoryImpl
import de.ur.explure.utils.FirebaseResult
import kotlinx.coroutines.launch

class CreatedRoutesViewModel(
    private val userRepo: UserRepositoryImpl,
    private val routeRepo: RouteRepositoryImpl,
    private val appRouter: MainAppRouter
) : ViewModel() {

    var user: MutableLiveData<User> = MutableLiveData()
    var createdRoutes: MutableLiveData<List<Route>> = MutableLiveData()

    fun getUserInfo() {
        viewModelScope.launch {
            when (val userInfo = userRepo.getUserInfo()) {
                is FirebaseResult.Success -> {
                    user.postValue(userInfo.data)
                }
            }
        }
    }

    fun getCreatedRoutes() {
        viewModelScope.launch {
            when (val userInfo = userRepo.getUserInfo()) {
                is FirebaseResult.Success -> {
                    if (userInfo.data.createdRoutes.isEmpty()) {
                        createdRoutes.postValue(emptyList())
                    } else {
                        when (val routeInfo = routeRepo.getRoutes(userInfo.data.createdRoutes)) {
                            is FirebaseResult.Success -> {
                                createdRoutes.postValue(routeInfo.data)
                            }
                        }
                    }
                }
            }
        }
    }

    fun navigateToSinglePage() {
        // appRouter.getNavController().navigate(R.id.singleRouteFragment)
    }

    fun deleteRoute(route: Route) {
        viewModelScope.launch {
            routeRepo.deleteRoute(route.id)
            userRepo.removeRouteFromCreatedRoutes(route.id)
            getCreatedRoutes()
        }
    }
}
