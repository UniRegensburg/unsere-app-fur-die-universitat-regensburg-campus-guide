package de.ur.explure.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.ur.explure.navigation.AppRouter
import de.ur.explure.repository.user.UserRepositoryImpl
import de.ur.explure.services.FirebaseAuthService
import kotlinx.coroutines.launch

class TestViewModel(
    private val appRouter: AppRouter,
    private val userRepo: UserRepositoryImpl,
    private val authService: FirebaseAuthService
) : ViewModel() {

    fun loginUser() {
        viewModelScope.launch {
            authService.logInUserWithEmail("testUser@test.de", "Password1")
        }
    }

    fun testAction() {
        viewModelScope.launch {
            // Test your action here
        }
    }
}
