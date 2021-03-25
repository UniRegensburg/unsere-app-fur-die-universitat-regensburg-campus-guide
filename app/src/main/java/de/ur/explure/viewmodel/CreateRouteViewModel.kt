package de.ur.explure.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.ur.explure.model.category.Category
import de.ur.explure.model.route.RouteDTO
import de.ur.explure.model.waypoint.WayPointDTO
import de.ur.explure.navigation.MainAppRouter
import de.ur.explure.repository.category.CategoryRepositoryImpl
import de.ur.explure.repository.route.RouteRepositoryImpl
import de.ur.explure.utils.FirebaseResult
import de.ur.explure.views.CreateRouteFragmentDirections
import kotlinx.coroutines.launch
import timber.log.Timber

class CreateRouteViewModel(
    private val appRouter: MainAppRouter,
    private val categoryRepo: CategoryRepositoryImpl,
    private val routeRepository: RouteRepositoryImpl
) : ViewModel() {

    val categories: MutableLiveData<List<Category>> = MutableLiveData()

    val wayPointDTOs: MutableLiveData<MutableList<WayPointDTO>> = MutableLiveData()

    val routeDTO = RouteDTO()

    fun getCategories() {
        viewModelScope.launch {
            val categoryCall = categoryRepo.getAllCategories()
            if (categoryCall is FirebaseResult.Success) {
                categories.postValue(categoryCall.data)
            } else {
                Timber.d("Failed to get Categories")
            }
        }
    }

    fun setWayPointDTOs(wayPointDTOList: List<WayPointDTO>) {
        wayPointDTOs.postValue(wayPointDTOList.toMutableList())
    }

    fun openWayPointDialogFragment(wayPointDTO: WayPointDTO) {
        val directions =
            CreateRouteFragmentDirections.actionCreateRouteFragmentToCreateWayPointDialog(
                wayPointDTO
            )
        appRouter.getNavController()?.navigate(
            directions
        )
    }

    fun updateWayPointDTO(editedWayPointDTO: WayPointDTO) {
        val wayPointArray = wayPointDTOs.value
        wayPointArray?.forEachIndexed { index, wayPoint ->
            if (wayPoint.geoPoint == editedWayPointDTO.geoPoint) {
                wayPointArray[index] = editedWayPointDTO
            }
        }
        wayPointDTOs.postValue(wayPointArray)
    }

    fun setTitle(title: String) {
        routeDTO.title = title
    }

    fun setDescription(description: String) {
        routeDTO.description = description
    }

    fun setCategoryId(categoryId: String) {
        routeDTO.category = categoryId
    }

    fun setRouteInformation(distance: Double, duration: Double) {
        routeDTO.distance = distance
        routeDTO.duration = duration
    }

    fun saveRoute() {
        routeDTO.wayPoints = wayPointDTOs.value ?: mutableListOf()
        viewModelScope.launch {
            when (val routeCall = routeRepository.createRouteInFireStore(routeDTO)) {
                is FirebaseResult.Success -> {
                    appRouter.navigateToRouteDetailsAfterCreation(routeCall.data)
                }
                is FirebaseResult.Error -> {
                    // Failed
                }
            }
        }
    }
}
