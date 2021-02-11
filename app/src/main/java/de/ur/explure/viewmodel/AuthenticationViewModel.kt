package de.ur.explure.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.ur.explure.navigation.StateAppRouter
import de.ur.explure.services.FirebaseAuthService
import de.ur.explure.utils.FirebaseResult
import kotlinx.coroutines.launch

class AuthenticationViewModel(
    private val authService: FirebaseAuthService,
    private val stateAppRouter: StateAppRouter
) : ViewModel() {

    fun signIn(email: String, password: String) {
        authService.signIn(email, password)
    }

    fun register(email: String, password: String) {
        viewModelScope.launch {
            when (val registerTask = authService.registerUser(email, password)) {
                is FirebaseResult.Success -> {
                } // Do task
                is FirebaseResult.Error -> {
                } // Do task
                is FirebaseResult.Canceled -> {
                } // Do task
            }
        }
    }

    fun resetPassword(email: String) {
        authService.resetPassword(email)
    }

    fun signInAnonymously() {
        authService.signInAnonymously()
    }

    fun goBackToLogin() {
        stateAppRouter.navigateUp()
    }

    fun navigateToRegister() {
        stateAppRouter.navigateToRegister()
    }
}
