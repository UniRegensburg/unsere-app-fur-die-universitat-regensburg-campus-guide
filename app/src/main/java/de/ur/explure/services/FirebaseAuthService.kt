package de.ur.explure.services

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import de.ur.explure.extensions.await
import de.ur.explure.utils.FirebaseResult

@Suppress("TooGenericExceptionCaught")
class FirebaseAuthService(private val firebaseAuth: FirebaseAuth) {

    private val user: MutableLiveData<FirebaseUser> = MutableLiveData<FirebaseUser>()
    val currentUser: LiveData<FirebaseUser> = user

    init {
        setAuthStateListener()
    }

    private fun setAuthStateListener() {
        firebaseAuth.addAuthStateListener { auth ->
            user.postValue(auth.currentUser)
        }
    }

    suspend fun registerUser(email: String, password: String): FirebaseResult<AuthResult> {
        return firebaseAuth.createUserWithEmailAndPassword(email, password).await()
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

    fun getCurrentUserId(): String? = currentUser.value?.uid
}
