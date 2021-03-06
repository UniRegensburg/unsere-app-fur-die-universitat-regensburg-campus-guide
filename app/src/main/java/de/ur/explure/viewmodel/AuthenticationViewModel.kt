package de.ur.explure.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.ur.explure.navigation.MainAppRouter
import de.ur.explure.services.FirebaseAuthService
import de.ur.explure.utils.FirebaseResult
import kotlinx.coroutines.launch

class AuthenticationViewModel(
    private val authService: FirebaseAuthService,
    private val mainAppRouter: MainAppRouter
) : ViewModel() {

    private val mutableUserInfo = MutableLiveData<String?>()
    val userInfo: LiveData<String?> = mutableUserInfo
    private val mutableResetSuccessful: MutableLiveData<Boolean> = MutableLiveData()
    val resetSuccessful: LiveData<Boolean> = mutableResetSuccessful

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            when (val loginTask = authService.signIn(email, password)) {
                is FirebaseResult.Success -> {
                    // doesn't need to do anything
                }
                is FirebaseResult.Error -> {
                    mutableUserInfo.value = loginTask.exception.message
                }
                is FirebaseResult.Canceled -> {
                    mutableUserInfo.value = loginTask.exception?.message
                }
            }
        }
    }

    fun register(email: String, password: String) {
        viewModelScope.launch {
            when (val registerTask = authService.registerUser(email, password)) {
                is FirebaseResult.Success -> {
                    // doesn't need to do anything
                }
                is FirebaseResult.Error -> {
                    mutableUserInfo.value = registerTask.exception.message
                }
                is FirebaseResult.Canceled -> {
                    mutableUserInfo.value = registerTask.exception?.message
                }
            }
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            when (val resetTask = authService.resetPassword(email)) {
                is FirebaseResult.Success -> {
                    mutableResetSuccessful.postValue(true)
                }
                is FirebaseResult.Error -> {
                    mutableUserInfo.value = resetTask.exception.message
                }
                is FirebaseResult.Canceled -> {
                    mutableUserInfo.value = resetTask.exception?.message
                }
            }
        }
    }

    fun signInAnonymously() {
        viewModelScope.launch {
            when (val anonymouslyTask = authService.signInAnonymously()) {
                is FirebaseResult.Success -> {
                    // doesn't need to do anything
                }
                is FirebaseResult.Error -> {
                    mutableUserInfo.value = anonymouslyTask.exception.message
                }
                is FirebaseResult.Canceled -> {
                    mutableUserInfo.value = anonymouslyTask.exception?.message
                }
            }
        }
    }

    fun goBackToLogin() {
        mainAppRouter.navigateUp()
    }

    fun navigateToRegister() {
        mainAppRouter.navigateToRegister()
    }
}
