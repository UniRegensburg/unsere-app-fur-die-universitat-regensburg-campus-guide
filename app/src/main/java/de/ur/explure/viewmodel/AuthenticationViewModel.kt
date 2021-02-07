package de.ur.explure.viewmodel

import android.widget.EditText
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseUser
import de.ur.explure.repository.AuthenticationRepository


class AuthenticationViewModel : ViewModel() {

    private var authenticationRepository: AuthenticationRepository = AuthenticationRepository()
    private var user: MutableLiveData<FirebaseUser>
    private var userLoggedOut: MutableLiveData<Boolean>

    init {
        user = authenticationRepository.getLiveData()
        userLoggedOut = authenticationRepository.getLoggedOutLiveData()

    }

    fun signIn(email: String, password : String) {
        authenticationRepository.signIn(email, password)
    }

    fun register (email: String, password : String) {
        authenticationRepository.registerUser(email, password)
    }

    fun resetPassword(email : EditText) {
        authenticationRepository.resetPassword(email)
    }

    fun signInAnonymously() {
        authenticationRepository.signInAnonymously()
    }

    fun logout() {
        authenticationRepository.logout()
    }

    fun getLiveData() : MutableLiveData<FirebaseUser> {
        return user
    }

}