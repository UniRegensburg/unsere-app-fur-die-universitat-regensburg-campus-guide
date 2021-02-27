package de.ur.explure.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.GeoPoint
import de.ur.explure.model.category.Category
import de.ur.explure.model.route.RouteDTO
import de.ur.explure.model.waypoint.WayPointDTO
import de.ur.explure.navigation.MainAppRouter
import de.ur.explure.repository.route.RouteRepositoryImpl
import de.ur.explure.services.FirebaseAuthService
import de.ur.explure.views.DiscoverFragmentDirections
import kotlinx.coroutines.launch
import timber.log.Timber

@Suppress("MagicNumber")
class DiscoverViewModel(
    private val mainAppRouter: MainAppRouter,
    private val userRepo: RouteRepositoryImpl,
    private val authService: FirebaseAuthService
) : ViewModel() {

    val categories : MutableLiveData<List<Category>> = MutableLiveData()

    fun loginUser() {
        viewModelScope.launch {
            authService.signIn("testUser@test.de", "Password1")
        }
    }

    fun showMap() {
        val mapAction = DiscoverFragmentDirections.actionDiscoverFragmentToMapFragment()
        mainAppRouter.getNavController().navigate(mapAction)
    }

    fun testAction() {
        viewModelScope.launch {
            val waypoint = WayPointDTO("title", GeoPoint(45.1, 32.0), "Descrp")
            val waypoint2 = WayPointDTO("title", GeoPoint(45.1, 32.0), "Descrp")
            val route = RouteDTO(
                "Category",
                "title2",
                "Descr",
                50.0,
                15.3,
                mutableListOf(waypoint, waypoint2)
            )
            val fullRoute = userRepo.getRoute("83bAuunZzXwaPIJ0Xc3a")
            val previewRoute = userRepo.getRoute("83bAuunZzXwaPIJ0Xc3a", true)
            Timber.d(fullRoute.toString())
            Timber.d(previewRoute.toString())
        }
    }

    fun getCategories() {
        val c1 = Category("9384", "UR", "#cbd4c2", "")
        val c2 = Category("93841", "UR", "#dbebc0", "")
        val c3 = Category("93842", "UR", "#c3b299", "")
        val c4 = Category("93843", "UR", "#815355", "")
        val c5 = Category("93844", "UR", "#523249", "")
        val c6 = Category("93854", "UR", "#788585", "")

        val list = listOf<Category>(c1,c2,c3,c4,c5,c6)
        categories.postValue(list)
    }
}
