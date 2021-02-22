package de.ur.explure.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.GeoPoint
import de.ur.explure.R
import de.ur.explure.model.route.RouteDTO
import de.ur.explure.model.waypoint.WayPointDTO
import de.ur.explure.navigation.MainAppRouter
import de.ur.explure.repository.route.RouteRepositoryImpl
import de.ur.explure.services.FirebaseAuthService
import kotlinx.coroutines.launch
import timber.log.Timber

@Suppress("MagicNumber")
class TestViewModel(
    private val mainAppRouter: MainAppRouter,
    private val userRepo: RouteRepositoryImpl,
    private val authService: FirebaseAuthService
) : ViewModel() {

    // "kXvvpB6ukGQtiafDTMxq", "QZLgj7nsSAWFHg54dqzG", "83bAuunZzXwaPIJ0Xc3a"

    fun loginUser() {
        viewModelScope.launch {
            authService.signIn("testUser@test.de", "Password1")
        }
    }

    fun showMap() {
        mainAppRouter.getNavController().navigate(R.id.mapFragment)
    }

    fun testAction() {
        viewModelScope.launch {
            authService.logout()
        }
    }
}
