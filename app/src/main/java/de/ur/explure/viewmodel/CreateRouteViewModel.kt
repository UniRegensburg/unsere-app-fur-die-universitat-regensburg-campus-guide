package de.ur.explure.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.ur.explure.model.category.Category
import de.ur.explure.model.route.RouteDTO
import de.ur.explure.model.waypoint.WayPointDTO
import de.ur.explure.navigation.MainAppRouter
import de.ur.explure.repository.category.CategoryRepositoryImpl
import de.ur.explure.repository.route.RouteRepositoryImpl
import de.ur.explure.utils.CachedFileUtils
import de.ur.explure.utils.FirebaseResult
import de.ur.explure.views.CreateRouteFragmentDirections
import kotlinx.coroutines.launch

class CreateRouteViewModel(
    private val appRouter: MainAppRouter,
    private val categoryRepo: CategoryRepositoryImpl,
    private val routeRepository: RouteRepositoryImpl
) : ViewModel() {

    val categories: MutableLiveData<List<Category>> = MutableLiveData()

    val wayPointDTOs: MutableLiveData<MutableList<WayPointDTO>> = MutableLiveData()

    val currentImageUri: MutableLiveData<Uri> = MutableLiveData()

    val showRouteCreationError: MutableLiveData<Boolean> = MutableLiveData(false)

    val showCategoryDownloadError: MutableLiveData<Boolean> = MutableLiveData(false)

    var currentTempCameraUri: Uri? = null

    private val routeDTO = RouteDTO()

    fun createNewCameraUri(context: Context): Uri {
        val newUri = CachedFileUtils.getNewImageUri(context)
        currentTempCameraUri = newUri
        return newUri
    }

    fun getCategories() {
        viewModelScope.launch {
            val categoryCall = categoryRepo.getAllCategories()
            if (categoryCall is FirebaseResult.Success) {
                categories.postValue(categoryCall.data)
            } else {
                showCategoryDownloadError.postValue(true)
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

    fun saveRoute() {
        routeDTO.wayPoints = wayPointDTOs.value ?: mutableListOf()
        routeDTO.thumbnailUri = currentTempCameraUri
        viewModelScope.launch {
            when (val routeCall = routeRepository.createRouteInFireStore(routeDTO)) {
                is FirebaseResult.Success -> {
                    appRouter.navigateToRouteDetailsAfterCreation(routeCall.data)
                }
                is FirebaseResult.Error -> {
                    showRouteCreationError.postValue(true)
                }
            }
        }
    }

    fun setRouteInformation(distance: Double, duration: Double) {
        routeDTO.distance = distance
        routeDTO.duration = duration
    }

    fun setImageUri(data: Uri) {
        currentImageUri.postValue(data)
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

    fun deleteCurrentUri() {
        currentImageUri.postValue(null)
    }
}
