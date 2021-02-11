package de.ur.explure.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.ur.explure.model.rating.RatingDTO
import de.ur.explure.navigation.AppRouter
import de.ur.explure.repository.rating.RatingRepositoryImpl
import de.ur.explure.services.FirebaseAuthService
import kotlinx.coroutines.launch

class TestViewModel(
    private val appRouter: AppRouter,
    private val userRepo: RatingRepositoryImpl,
    private val authService: FirebaseAuthService
) : ViewModel() {

    fun loginUser() {
        viewModelScope.launch {
            authService.logInUserWithEmail("testUser@test.de", "Password1")
        }
    }

    fun testAction() {
        viewModelScope.launch {
            val list = listOf<String>("FBd4rRunN7mNoMl8S5Dn", "7XwhG4B1quV5doC2IxzV")
            val result = userRepo.addRatingToFireStore(RatingDTO(2, "34234"))
        }
    }
}
