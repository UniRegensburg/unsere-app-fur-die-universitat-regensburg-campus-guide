package de.ur.explure.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.ur.explure.R
import de.ur.explure.navigation.StateAppRouter
import de.ur.explure.services.FirebaseAuthService
import de.ur.explure.utils.FirebaseResult
import kotlinx.coroutines.launch

class AuthenticationViewModel(
    private val authService: FirebaseAuthService,
    private val stateAppRouter: StateAppRouter
) : ViewModel() {

    private val _toast = MutableLiveData<String?>()
    val toast: LiveData<String?>
        get() = _toast

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            when (val loginTask = authService.signIn(email, password)) {
                is FirebaseResult.Success -> {
                  // do task
                }
                is FirebaseResult.Error -> {
                    _toast.value = loginTask.exception.message
                }
                is FirebaseResult.Canceled -> {
                    _toast.value = loginTask.exception!!.message
                }
            }
        }
    }

    fun register(email: String, password: String) {
        viewModelScope.launch {
            when (val registerTask = authService.registerUser(email, password)) {
                is FirebaseResult.Success -> {
                    // do task
                }
                is FirebaseResult.Error -> {
                    _toast.value = registerTask.exception.message
                }
                is FirebaseResult.Canceled -> {
                    _toast.value = registerTask.exception!!.message
                }
            }
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            when (val resetTask = authService.resetPassword(email)) {
                is FirebaseResult.Success -> {
                    // Error: returns String email_sent and then int
                    _toast.value = R.string.email_sent.toString()
                }
                is FirebaseResult.Error -> {
                    _toast.value = resetTask.exception.message
                }
                is FirebaseResult.Canceled -> {
                    _toast.value = resetTask.exception!!.message
                }
            }
        }
    }

    fun signInAnonymously() {
        viewModelScope.launch {
            when (val anonymouslyTask = authService.signInAnonymously()) {
                is FirebaseResult.Success -> {
                    // do task
                }
                is FirebaseResult.Error -> {
                    _toast.value = anonymouslyTask.exception.message
                }
                is FirebaseResult.Canceled -> {
                    _toast.value = anonymouslyTask.exception!!.message
                }
            }
        }
    }

    fun goBackToLogin() {
        stateAppRouter.navigateUp()
    }

    fun navigateToRegister() {
        stateAppRouter.navigateToRegister()
    }
}
