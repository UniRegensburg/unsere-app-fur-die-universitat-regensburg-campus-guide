package de.ur.explure.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.ur.explure.model.category.Category
import de.ur.explure.model.route.RouteDTO
import de.ur.explure.model.waypoint.WayPointDTO
import de.ur.explure.navigation.MainAppRouter
import de.ur.explure.repository.category.CategoryRepositoryImpl
import de.ur.explure.repository.route.RouteRepositoryImpl
import de.ur.explure.utils.FirebaseResult
import de.ur.explure.views.SaveRouteFragmentDirections
import kotlinx.coroutines.launch
import timber.log.Timber

class SaveRouteViewModel(
    private val state: SavedStateHandle,
    private val appRouter: MainAppRouter,
    private val categoryRepo: CategoryRepositoryImpl,
    private val routeRepository: RouteRepositoryImpl
) : ViewModel() {

    val categories: MutableLiveData<List<Category>> = MutableLiveData()

    val wayPointDTOs: MutableLiveData<MutableList<WayPointDTO>> = MutableLiveData()

    val routeDTO = RouteDTO()

    var routeTitle: String? = state[ROUTE_TITLE_KEY]
    var routeDescription: String? = state[ROUTE_DESCRIPTION_KEY]
    var routeCategory: String? = state[ROUTE_CATEGORY_KEY]
    var routeDuration: Double? = state[ROUTE_DURATION_KEY]

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
            SaveRouteFragmentDirections.actionSaveRouteFragmentToCreateWayPointDialog(
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
        state[ROUTE_TITLE_KEY] = title
    }

    fun setDescription(description: String) {
        routeDTO.description = description
        state[ROUTE_DESCRIPTION_KEY] = description
    }

    fun setCategoryId(categoryId: String) {
        routeDTO.category = categoryId
        state[ROUTE_CATEGORY_KEY] = categoryId
    }

    fun updateRouteDuration(duration: Double) {
        routeDTO.duration = duration
        state[ROUTE_DURATION_KEY] = duration
    }

    fun setInitialRouteInformation(distance: Double, duration: Double) {
        routeDTO.distance = distance
        routeDTO.duration = duration
        state[ROUTE_DURATION_KEY] = duration
    }

    // TODO only update the route here! saving it should probably be done in the editRouteFragment ?
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

    companion object {
        private const val ROUTE_TITLE_KEY = "routeTitle"
        private const val ROUTE_DESCRIPTION_KEY = "routeDescription"
        private const val ROUTE_CATEGORY_KEY = "routeCategory"
        private const val ROUTE_DURATION_KEY = "routeDuration"
    }
}
