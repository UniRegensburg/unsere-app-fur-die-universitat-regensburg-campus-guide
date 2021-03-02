package de.ur.explure.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.ur.explure.extensions.appendRoutes
import de.ur.explure.model.category.Category
import de.ur.explure.model.route.Route
import de.ur.explure.navigation.MainAppRouter
import de.ur.explure.repository.category.CategoryRepositoryImpl
import de.ur.explure.repository.route.RouteRepositoryImpl
import de.ur.explure.utils.FirebaseResult
import de.ur.explure.views.DiscoverFragmentDirections
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*

@Suppress("MagicNumber")
class DiscoverViewModel(
    private val mainAppRouter: MainAppRouter,
    private val categoryRepo: CategoryRepositoryImpl,
    private val routeRepo: RouteRepositoryImpl
) : ViewModel() {

    val categories: MutableLiveData<List<Category>> = MutableLiveData()

    val latestRouteList: MutableLiveData<MutableList<Route>> = MutableLiveData()

    val popularRouteList: MutableLiveData<MutableList<Route>> = MutableLiveData()

    fun showMap() {
        val mapAction = DiscoverFragmentDirections.actionDiscoverFragmentToMapFragment()
        mainAppRouter.getNavController()?.navigate(mapAction)
    }

    fun getCategories() {
        viewModelScope.launch {
            when (val categoryCall = categoryRepo.getAllCategories()) {
                is FirebaseResult.Success -> {
                    categories.postValue(categoryCall.data)
                }
            }
        }
    }

    fun getLatestRoutes() {
        viewModelScope.launch {
            val lastDate: Date? = getLastVisibleDate()
            when (val latestRouteCall = routeRepo.getLatestRoutes(lastDate, ROUTE_BATCH_SIZE)) {
                is FirebaseResult.Success -> {
                    latestRouteList.appendRoutes(latestRouteCall.data)
                }
                is FirebaseResult.Error -> {
                    Timber.d("Failed to update latestRoutes")
                }
                is FirebaseResult.Canceled -> {
                    Timber.d("Failed to update latestRoutes")
                }
            }
        }
    }

    fun getPopularRoutes() {
        viewModelScope.launch {
            val lastRating: Double? = getLastVisibleRating()
            when (val latestRouteCall = routeRepo.getMostPopularRoutes(lastRating, ROUTE_BATCH_SIZE)) {
                is FirebaseResult.Success -> {
                    popularRouteList.appendRoutes(latestRouteCall.data)
                }
                is FirebaseResult.Error -> {
                    Timber.d("Failed to update popularRoutes")
                }
                is FirebaseResult.Canceled -> {
                    Timber.d("Failed to update popularRoutes")
                }
            }
        }
    }

    private fun getLastVisibleRating(): Double? {
        var lastRating: Double? = null
        val routeList = popularRouteList.value
        if (!routeList.isNullOrEmpty()) {
            lastRating = routeList[routeList.lastIndex].currentRating
        }
        return lastRating
    }

    private fun getLastVisibleDate(): Date? {
        var lastDate: Date? = null
        val routeList = latestRouteList.value
        if (!routeList.isNullOrEmpty()) {
            lastDate = routeList[routeList.lastIndex].createdAt
        }
        return lastDate
    }

    companion object {
        const val ROUTE_BATCH_SIZE: Long = 10
    }
}
