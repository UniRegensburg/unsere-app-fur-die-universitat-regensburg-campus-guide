package de.ur.explure.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.ur.explure.model.category.Category
import de.ur.explure.model.route.Route
import de.ur.explure.repository.route.RouteRepositoryImpl
import de.ur.explure.utils.FirebaseResult
import kotlinx.coroutines.launch

class CategoryViewModel(
    private val routeRepo: RouteRepositoryImpl
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
}
