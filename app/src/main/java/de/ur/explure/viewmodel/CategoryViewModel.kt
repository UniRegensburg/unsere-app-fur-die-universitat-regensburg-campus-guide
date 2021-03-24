package de.ur.explure.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.ur.explure.model.route.Route
import de.ur.explure.repository.route.RouteRepositoryImpl
import de.ur.explure.utils.FirebaseResult
import kotlinx.coroutines.launch

class CategoryViewModel(
    private val routeRepo: RouteRepositoryImpl
) : ViewModel() {

    var categoryRoutes: MutableLiveData<List<Route>> = MutableLiveData()
    var noRoutes = MutableLiveData<Boolean>(false)

    fun getCategoryRoutes(category: String) {
        viewModelScope.launch {
            when (val routeLists = routeRepo.getCategoryRoutes(category)){
                is FirebaseResult.Success -> {
                    noRoutes.postValue(routeLists.data.isEmpty())
                    categoryRoutes.postValue(routeLists.data!!)
                }
            }
        }
    }
}
