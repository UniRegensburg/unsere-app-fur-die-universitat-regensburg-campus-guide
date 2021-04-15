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

    suspend fun registerUser(email: String, password: String): FirebaseResult<FirebaseUser?> {
        try {
            return when (val registerTask = firebaseAuth.createUserWithEmailAndPassword(email, password).await()) {
                is FirebaseResult.Success -> {
                    val user = registerTask.data.user
                    FirebaseResult.Success(user)
                }
                is FirebaseResult.Error -> {
                    FirebaseResult.Error(registerTask.exception)
                }
                is FirebaseResult.Canceled -> {
                    FirebaseResult.Canceled(registerTask.exception)
                }
            }
        } catch (exception: Exception) {
            return FirebaseResult.Error(exception)
        }
    }

    suspend fun signIn(email: String, password: String): FirebaseResult<FirebaseUser?> {
        try {
            return when (val loginTask = firebaseAuth.signInWithEmailAndPassword(email, password).await()) {
                is FirebaseResult.Success -> {
                    val user = loginTask.data.user
                    FirebaseResult.Success(user)
                }
                is FirebaseResult.Error -> {
                    FirebaseResult.Error(loginTask.exception)
                }
                is FirebaseResult.Canceled -> {
                    FirebaseResult.Canceled(loginTask.exception)
                }
            }
        } catch (exception: java.lang.Exception) {
            return FirebaseResult.Error(exception)
        }
    }

    suspend fun resetPassword(email: String): FirebaseResult<Void> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
        } catch (exception: Exception) {
            FirebaseResult.Error(exception)
        }
    }

    suspend fun signInAnonymously(): FirebaseResult<AuthResult> {
        return try {
            firebaseAuth.signInAnonymously().await()
        } catch (exception: Exception) {
            FirebaseResult.Error(exception)
        }
    }

    fun logout() {
        firebaseAuth.signOut()
    }

    fun isAnonymousUser(): Boolean? {
        return currentUser.value?.isAnonymous
    }

    fun getCurrentUserId(): String? = currentUser.value?.uid

    fun getCurrentUserEmail(): String? = currentUser.value?.email
}
