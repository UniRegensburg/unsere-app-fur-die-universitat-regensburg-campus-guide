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

    fun showMap() {
        val mapAction = DiscoverFragmentDirections.actionDiscoverFragmentToMapFragment()
        mainAppRouter.getNavController().navigate(mapAction)
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
            val latestDate: Date? = getLastVisibleDate()
            when (val latestRouteCall = routeRepo.getLatestRoutes(latestDate, ROUTE_BATCH_SIZE)) {
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

    private fun getLastVisibleDate(): Date? {
        var latestDate: Date? = null
        val routeList = latestRouteList.value
        if (!routeList.isNullOrEmpty()) {
            latestDate = routeList[routeList.lastIndex].createdAt
        }
        return latestDate
    }

    companion object {
        const val ROUTE_BATCH_SIZE: Long = 10
    }
}

