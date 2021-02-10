package de.ur.explure.repository

import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import org.koin.core.KoinComponent

class AuthenticationRepository(val firebaseAuth: FirebaseAuth) : KoinComponent {

    private val user: MutableLiveData<FirebaseUser> = MutableLiveData<FirebaseUser>()
    val currentUser: MutableLiveData<FirebaseUser> = user

    init {

        // checks if user is already logged in
       /* if(firebaseAuth.currentUser != null) {
            user.postValue(firebaseAuth.currentUser)
            userLoggedOut.postValue(false)
        }*/
    }

    fun registerUser(email: String, password: String) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                            user.postValue(firebaseAuth.currentUser)
                    }
                }
    }

    fun signIn(email: String, password: String) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        user.postValue(firebaseAuth.currentUser)
                    }
                }
    }

    fun resetPassword(email: String) {
        firebaseAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener {
                    task ->
                    if (task.isSuccessful) {
                    // Toast.makeText(this, "Email gesendet", Toast.LENGTH_SHORT).show()
                    }
                }
        }

    fun signInAnonymously() {
        firebaseAuth.signInAnonymously()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        user.postValue(firebaseAuth.currentUser)
                    }
                }
    }

    fun logout() {
        firebaseAuth.signOut()
    }
}
