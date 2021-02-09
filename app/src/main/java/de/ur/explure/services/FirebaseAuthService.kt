package de.ur.explure.services

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import de.ur.explure.extensions.await
import de.ur.explure.utils.FirebaseResult

// Just for testing

class FirebaseAuthService(private val firebaseAuth: FirebaseAuth) {

    suspend fun logInUserWithEmail(
        email: String,
        password: String
    ): FirebaseResult<FirebaseUser?> {
        try {
            return when (val resultDocumentSnapshot =
                firebaseAuth.signInWithEmailAndPassword(email, password).await()) {
                is FirebaseResult.Success -> {
                    val firebaseUser = resultDocumentSnapshot.data.user
                    FirebaseResult.Success(firebaseUser)
                }
                is FirebaseResult.Error -> {
                    FirebaseResult.Error(resultDocumentSnapshot.exception)
                }
                is FirebaseResult.Canceled -> {
                    FirebaseResult.Canceled(resultDocumentSnapshot.exception)
                }
            }
        } catch (exception: Exception) {
            return FirebaseResult.Error(exception)
        }
    }

    fun getCurrentUserId(): String? = firebaseAuth.currentUser?.uid
}
