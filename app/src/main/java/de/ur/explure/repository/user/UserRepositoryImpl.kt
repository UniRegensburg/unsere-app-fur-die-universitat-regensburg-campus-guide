package de.ur.explure.repository.user

import com.google.firebase.firestore.FieldValue
import de.ur.explure.config.ErrorConfig.DESERIALIZATION_FAILED_RESULT
import de.ur.explure.config.ErrorConfig.NO_USER_RESULT
import de.ur.explure.config.UserDocumentConfig.ACTIVE_ROUTES_KEY
import de.ur.explure.config.UserDocumentConfig.CREATED_ROUTES_KEY
import de.ur.explure.config.UserDocumentConfig.FAVOURITE_ROUTES_KEY
import de.ur.explure.config.UserDocumentConfig.FINISHED_ROUTES_KEY
import de.ur.explure.config.UserDocumentConfig.NAME_KEY
import de.ur.explure.extensions.await
import de.ur.explure.model.user.User
import de.ur.explure.model.user.UserDTO
import de.ur.explure.services.FireStoreInstance
import de.ur.explure.services.FirebaseAuthService
import de.ur.explure.utils.FirebaseResult

class UserRepositoryImpl(
    private val firebaseAuth: FirebaseAuthService,
    private val fireStore: FireStoreInstance
) : UserRepository {

    override suspend fun createUserInFirestore(user: UserDTO): FirebaseResult<Void> {
        return try {
            val userId = firebaseAuth.getCurrentUserId() ?: return NO_USER_RESULT
            fireStore.userCollection.document(userId).set(user.toMap()).await()
        } catch (exception: Exception) {
            FirebaseResult.Error(exception)
        }
    }

    override suspend fun getUserInfo(): FirebaseResult<User> {
        return try {
            val userId = firebaseAuth.getCurrentUserId() ?: return NO_USER_RESULT
            when (val resultDocumentSnapshot =
                fireStore.userCollection.document(userId).get().await()) {
                is FirebaseResult.Success -> {
                    val user = resultDocumentSnapshot.data.toObject(User::class.java)
                        ?: return DESERIALIZATION_FAILED_RESULT
                    FirebaseResult.Success(user)
                }
                is FirebaseResult.Error -> FirebaseResult.Error(resultDocumentSnapshot.exception)
                is FirebaseResult.Canceled -> FirebaseResult.Canceled(resultDocumentSnapshot.exception)
            }
        } catch (exception: Exception) {
            FirebaseResult.Error(exception)
        }
    }

    override suspend fun updateUserName(name: String): FirebaseResult<Void> {
        return try {
            val userId = firebaseAuth.getCurrentUserId() ?: return NO_USER_RESULT
            fireStore.userCollection.document(userId).update(mapOf(NAME_KEY to name)).await()
        } catch (exception: Exception) {
            FirebaseResult.Error(exception)
        }
    }

    override suspend fun addRouteToFinishedRoutes(routeId: String): FirebaseResult<Void> {
        return try {
            val userId = firebaseAuth.getCurrentUserId() ?: return NO_USER_RESULT
            fireStore.userCollection.document(userId)
                .update(FINISHED_ROUTES_KEY, (FieldValue.arrayUnion(routeId))).await()
        } catch (exception: Exception) {
            FirebaseResult.Error(exception)
        }
    }

    override suspend fun addRouteToFavouriteRoutes(routeId: String): FirebaseResult<Void> {
        return try {
            val userId = firebaseAuth.getCurrentUserId() ?: return NO_USER_RESULT
            fireStore.userCollection.document(userId)
                .update(FAVOURITE_ROUTES_KEY, (FieldValue.arrayUnion(routeId))).await()
        } catch (exception: Exception) {
            FirebaseResult.Error(exception)
        }
    }

    override suspend fun addRouteToCreatedRoutes(routeId: String): FirebaseResult<Void> {
        return try {
            val userId = firebaseAuth.getCurrentUserId() ?: return NO_USER_RESULT
            fireStore.userCollection.document(userId)
                .update(CREATED_ROUTES_KEY, (FieldValue.arrayUnion(routeId))).await()
        } catch (exception: Exception) {
            FirebaseResult.Error(exception)
        }
    }

    override suspend fun addRouteToActiveRoutes(routeId: String): FirebaseResult<Void> {
        return try {
            val userId = firebaseAuth.getCurrentUserId() ?: return NO_USER_RESULT
            fireStore.userCollection.document(userId)
                .update(ACTIVE_ROUTES_KEY, (FieldValue.arrayUnion(routeId))).await()
        } catch (exception: Exception) {
            FirebaseResult.Error(exception)
        }
    }

    override suspend fun removeRouteFromFinishedRoutes(routeId: String): FirebaseResult<Void> {
        return try {
            val userId = firebaseAuth.getCurrentUserId() ?: return NO_USER_RESULT
            fireStore.userCollection.document(userId)
                .update(ACTIVE_ROUTES_KEY, (FieldValue.arrayRemove(routeId))).await()
        } catch (exception: Exception) {
            FirebaseResult.Error(exception)
        }
    }

    override suspend fun removeRouteFromFavouriteRoutes(routeId: String): FirebaseResult<Void> {
        return try {
            val userId = firebaseAuth.getCurrentUserId() ?: return NO_USER_RESULT
            fireStore.userCollection.document(userId)
                .update(ACTIVE_ROUTES_KEY, (FieldValue.arrayRemove(routeId))).await()
        } catch (exception: Exception) {
            FirebaseResult.Error(exception)
        }
    }

    override suspend fun removeRouteFromCreatedRoutes(routeId: String): FirebaseResult<Void> {
        return try {
            val userId = firebaseAuth.getCurrentUserId() ?: return NO_USER_RESULT
            fireStore.userCollection.document(userId)
                .update(ACTIVE_ROUTES_KEY, (FieldValue.arrayRemove(routeId))).await()
        } catch (exception: Exception) {
            FirebaseResult.Error(exception)
        }
    }

    override suspend fun removeRouteFromActiveRoutes(routeId: String): FirebaseResult<Void> {
        return try {
            val userId = firebaseAuth.getCurrentUserId() ?: return NO_USER_RESULT
            fireStore.userCollection.document(userId)
                .update(ACTIVE_ROUTES_KEY, (FieldValue.arrayRemove(routeId))).await()
        } catch (exception: Exception) {
            FirebaseResult.Error(exception)
        }
    }
}
