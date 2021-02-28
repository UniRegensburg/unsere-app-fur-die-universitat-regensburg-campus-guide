package de.ur.explure.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.GeoPoint
import de.ur.explure.model.category.Category
import de.ur.explure.model.route.RouteDTO
import de.ur.explure.model.waypoint.WayPointDTO
import de.ur.explure.navigation.MainAppRouter
import de.ur.explure.repository.category.CategoryRepositoryImpl
import de.ur.explure.repository.route.RouteRepositoryImpl
import de.ur.explure.services.FirebaseAuthService
import de.ur.explure.utils.FirebaseResult
import de.ur.explure.views.DiscoverFragmentDirections
import kotlinx.coroutines.launch
import timber.log.Timber

@Suppress("MagicNumber")
class DiscoverViewModel(
    private val mainAppRouter: MainAppRouter,
    private val categoryRepo: CategoryRepositoryImpl
) : ViewModel() {

    val categories: MutableLiveData<List<Category>> = MutableLiveData()

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
}
