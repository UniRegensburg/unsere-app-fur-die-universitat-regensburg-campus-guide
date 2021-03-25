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
import kotlinx.coroutines.launch
import java.util.*

@Suppress("MagicNumber", "TooManyFunctions")
class DiscoverViewModel(
    private val mainAppRouter: MainAppRouter,
    private val categoryRepo: CategoryRepositoryImpl,
    private val routeRepo: RouteRepositoryImpl
) : ViewModel() {

    val categories: MutableLiveData<List<Category>> = MutableLiveData()

    val latestRouteList: MutableLiveData<MutableList<Route>> = MutableLiveData()

    val popularRouteList: MutableLiveData<MutableList<Route>> = MutableLiveData()

    val showRouteError: MutableLiveData<Boolean> = MutableLiveData()

    val showCategoryError: MutableLiveData<Boolean> = MutableLiveData()

    fun getCategories() {
        viewModelScope.launch {
            val categoryCall = categoryRepo.getAllCategories()
            if (categoryCall is FirebaseResult.Success) {
                categories.postValue(categoryCall.data)
            } else {
                displayCategoryErrorOnFragment()
            }
        }
    }

    fun getLatestRoutes() {
        viewModelScope.launch {
            val lastDate: Date? = getLastVisibleDate()
            val latestRouteCall = routeRepo.getLatestRoutes(lastDate, ROUTE_BATCH_SIZE)
            if (latestRouteCall is FirebaseResult.Success) {
                latestRouteList.appendRoutes(latestRouteCall.data)
            } else {
                displayRouteErrorOnFragment()
            }
        }
    }

    fun getPopularRoutes() {
        viewModelScope.launch {
            val lastRating: Double? = getLastVisibleRating()
            val popularRouteCall = routeRepo.getMostPopularRoutes(lastRating, ROUTE_BATCH_SIZE)
            if (popularRouteCall is FirebaseResult.Success) {
                popularRouteList.appendRoutes(popularRouteCall.data)
            } else {
                displayRouteErrorOnFragment()
            }
        }
    }

    fun resetCategoryErrorFlag() {
        showCategoryError.postValue(false)
    }

    fun resetRouteErrorFlag() {
        showRouteError.postValue(false)
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

    private fun displayCategoryErrorOnFragment() {
        showCategoryError.postValue(true)
    }

    private fun displayRouteErrorOnFragment() {
        showRouteError.postValue(true)
    }

    fun startTextQuery(textQueryKey: String) {
        mainAppRouter.navigateToTextSearchResult(textQueryKey)
    }

    fun startCategoryQuery(categoryQueryKey: Category) {

        mainAppRouter.navigateToCategoryQuery(categoryQueryKey)
    }

    fun showRouteDetails(routeId: String) {
        mainAppRouter.navigateToRouteDetails(routeId)
    }

    companion object {
        const val ROUTE_BATCH_SIZE: Long = 10
    }
}
