package de.ur.explure.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.GeoPoint
import de.ur.explure.model.route.RouteDTO
import de.ur.explure.model.waypoint.WayPointDTO
import de.ur.explure.navigation.AppRouter
import de.ur.explure.repository.route.RouteRepositoryImpl
import de.ur.explure.services.FirebaseAuthService
import kotlinx.coroutines.launch

@Suppress("MagicNumber")
class TestViewModel(
    private val appRouter: AppRouter,
    private val userRepo: RouteRepositoryImpl,
    private val authService: FirebaseAuthService
) : ViewModel() {

    // "kXvvpB6ukGQtiafDTMxq", "QZLgj7nsSAWFHg54dqzG", "83bAuunZzXwaPIJ0Xc3a"

    fun loginUser() {
        viewModelScope.launch {
            authService.logInUserWithEmail("testUser@test.de", "Password1")
        }
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
            Log.d(
                "TAG", fullRoute.toString()
            )
            Log.d("TAG", previewRoute.toString())
        }
    }
}
