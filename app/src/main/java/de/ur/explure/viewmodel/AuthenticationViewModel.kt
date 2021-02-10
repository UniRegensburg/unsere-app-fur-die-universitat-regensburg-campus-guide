package de.ur.explure.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseUser
import de.ur.explure.repository.AuthenticationRepository

class AuthenticationViewModel(val authenticationRepository: AuthenticationRepository) : ViewModel() {

    val user: MutableLiveData<FirebaseUser> = authenticationRepository.currentUser

    fun signIn(email: String, password: String) {
        authenticationRepository.signIn(email, password)
    }

    fun register(email: String, password: String) {
        authenticationRepository.registerUser(email, password)
    }

    fun resetPassword(email: String) {
        authenticationRepository.resetPassword(email)
    }

    fun signInAnonymously() {
        authenticationRepository.signInAnonymously()
    }

    fun logout() {
        authenticationRepository.logout()
    }
}
