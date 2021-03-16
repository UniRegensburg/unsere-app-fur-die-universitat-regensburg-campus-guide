package de.ur.explure.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.GeoPoint
import de.ur.explure.model.category.Category
import de.ur.explure.model.waypoint.WayPointDTO
import de.ur.explure.navigation.MainAppRouter
import de.ur.explure.repository.category.CategoryRepositoryImpl
import de.ur.explure.utils.FirebaseResult
import de.ur.explure.views.CreateRouteFragmentDirections
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.function.UnaryOperator

class CreateRouteViewModel(
    private val appRouter: MainAppRouter,
    private val categoryRepo: CategoryRepositoryImpl
) : ViewModel() {

    val categories: MutableLiveData<List<Category>> = MutableLiveData()

    val wayPointDTOs: MutableLiveData<MutableList<WayPointDTO>> = MutableLiveData()

    fun getCategories() {
        viewModelScope.launch {
            when (val categoryCall = categoryRepo.getAllCategories()) {
                is FirebaseResult.Success -> categories.postValue(categoryCall.data)
                else -> Timber.d("Failed to get Categories")
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
            if (wayPoint.coordinates == editedWayPointDTO.coordinates) {
                wayPointArray[index] = editedWayPointDTO
            }
        }
        wayPointDTOs.postValue(wayPointArray)
    }

}