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

class FavoriteRoutesViewModel(
    private val userRepo: UserRepositoryImpl,
    private val routeRepo: RouteRepositoryImpl,
    private val appRouter: MainAppRouter
) : ViewModel() {

    var user: MutableLiveData<User> = MutableLiveData()
    var favoriteRoutes: MutableLiveData<List<Route>> = MutableLiveData()

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

    fun getFavoriteRoutes() {
        viewModelScope.launch {
            val userInfo = userRepo.getUserInfo()
            when (userInfo) {
                is FirebaseResult.Success -> {
                    if (userInfo.data.favouriteRoutes.size == 0) {
                        favoriteRoutes.postValue(emptyList())
                    } else {
                        val routeInfo = routeRepo.getRoutes(userInfo.data.favouriteRoutes)
                        when (routeInfo) {
                            is FirebaseResult.Success -> {
                                favoriteRoutes.postValue(routeInfo.data)
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

    fun removeRouteFromFavoriteRoutes(route: Route) {
        viewModelScope.launch {
            userRepo.removeRouteFromFavouriteRoutes(route.id)
            getFavoriteRoutes()
        }
    }
}
