package de.ur.explure.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.ur.explure.navigation.MainAppRouter
import de.ur.explure.repository.user.UserRepositoryImpl
import de.ur.explure.services.FirebaseAuthService
import kotlinx.coroutines.launch

class TestViewModel(
    private val mainAppRouter: MainAppRouter,
    private val userRepo: UserRepositoryImpl,
    private val authService: FirebaseAuthService
) : ViewModel() {

    fun testAction() {
        viewModelScope.launch {
            authService.logout()
        }
    }
}
