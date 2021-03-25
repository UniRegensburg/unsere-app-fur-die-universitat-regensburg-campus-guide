package de.ur.explure.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.ur.explure.model.category.Category
import de.ur.explure.model.route.Route
import de.ur.explure.navigation.MainAppRouter
import de.ur.explure.repository.route.RouteRepositoryImpl
import de.ur.explure.utils.FirebaseResult
import kotlinx.coroutines.launch

class CategoryViewModel(
    private val routeRepo: RouteRepositoryImpl,
    private val mainAppRouter: MainAppRouter
) : ViewModel() {

    var categoryRoutes: MutableLiveData<List<Route>> = MutableLiveData()

    fun getCategoryRoutes(category: Category) {
        viewModelScope.launch {
            when (val routeLists = routeRepo.getCategoryRoutes(category.id)) {
                is FirebaseResult.Success -> {
                    categoryRoutes.postValue(routeLists.data)
                }
            }
        }
    }

    fun showRouteDetails(routeId: String) {
        mainAppRouter.navigateToRouteDetailsFromCategory(routeId)
    }
}
