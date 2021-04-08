package de.ur.explure.repository.user

import android.graphics.Bitmap
import com.google.firebase.firestore.FieldValue.arrayRemove
import com.google.firebase.firestore.FieldValue.arrayUnion
import com.google.firebase.storage.FirebaseStorage
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
import java.io.ByteArrayOutputStream

@Suppress("TooGenericExceptionCaught", "UnnecessaryParentheses")
class UserRepositoryImpl(
    private val firebaseAuth: FirebaseAuthService,
    private val fireStore: FireStoreInstance,
    private val fireStorage: FirebaseStorage
) : UserRepository {

    override suspend fun isProfileCreated(userId: String): Boolean {
        return try {
            when (val userCall = fireStore.userCollection.document(userId).get().await()) {
                is FirebaseResult.Success -> userCall.data.exists()
                else -> false
            }
        } catch (exception: Exception) {
            false
        }
    }

    /**
     * Creates a new user document in FireStore for the currently logged in user.
     *
     * @param user [UserDTO] object containing the user's email and name.
     * @return On Success: Returns [FirebaseResult.Success] with empty return\
     * On Failure: Returns [FirebaseResult.Error] with exception\
     * On Cancellation: Returns [FirebaseResult.Canceled] with exception
     *
     */

    override suspend fun createUserInFirestore(userName: String): FirebaseResult<Void> {
        return try {
            val email = firebaseAuth.getCurrentUserEmail() ?: return NO_USER_RESULT
            val userId = firebaseAuth.getCurrentUserId() ?: return NO_USER_RESULT
            val userDTO = UserDTO(email = email, name = userName)
            fireStore.userCollection.document(userId).set(userDTO.toMap(userId)).await()
        } catch (exception: Exception) {
            FirebaseResult.Error(exception)
        }
    }

    /**
     * Retrieves the user's information from the FireStore database.
     *
     * @return On Success: Returns [FirebaseResult.Success] with an [User] object as data\
     * On Failure: Returns [FirebaseResult.Error] with exception\
     * On Cancellation: Returns [FirebaseResult.Canceled] with exception
     */

    @Suppress("ReturnCount")
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

    /**
     * Updates the user's name in the firestore db.
     *
     * @param name [String] object with the new name of the user.
     * @return On Success: Returns [FirebaseResult.Success] with empty return\
     * On Failure: Returns [FirebaseResult.Error] with exception\
     * On Cancellation: Returns [FirebaseResult.Canceled] with exception
     *
     */

    override suspend fun updateUserName(name: String): FirebaseResult<Void> {
        return try {
            val userId = firebaseAuth.getCurrentUserId() ?: return NO_USER_RESULT
            fireStore.userCollection.document(userId).update(mapOf(NAME_KEY to name)).await()
        } catch (exception: Exception) {
            FirebaseResult.Error(exception)
        }
    }

    /**
     * Adds the given route id to the user's finished route list.
     *
     * @param routeId [String] object containing the route's id which should be added.
     * @return On Success: Returns [FirebaseResult.Success] with empty return\
     * On Failure: Returns [FirebaseResult.Error] with exception\
     * On Cancellation: Returns [FirebaseResult.Canceled] with exception
     *
     */

    override suspend fun addRouteToFinishedRoutes(routeId: String): FirebaseResult<Void> {
        return try {
            val userId = firebaseAuth.getCurrentUserId() ?: return NO_USER_RESULT
            fireStore.userCollection.document(userId)
                .update(FINISHED_ROUTES_KEY, (arrayUnion(routeId))).await()
        } catch (exception: Exception) {
            FirebaseResult.Error(exception)
        }
    }

    /**
     * Adds the given route id to the user's favourite route list.
     *
     * @param routeId [String] object containing the route's id which should be added.
     * @return On Success: Returns [FirebaseResult.Success] with empty return\
     * On Failure: Returns [FirebaseResult.Error] with exception\
     * On Cancellation: Returns [FirebaseResult.Canceled] with exception
     *
     */

    override suspend fun addRouteToFavouriteRoutes(routeId: String): FirebaseResult<Void> {
        return try {
            val userId = firebaseAuth.getCurrentUserId() ?: return NO_USER_RESULT
            fireStore.userCollection.document(userId)
                .update(FAVOURITE_ROUTES_KEY, (arrayUnion(routeId))).await()
        } catch (exception: Exception) {
            FirebaseResult.Error(exception)
        }
    }

    /**
     * Adds the given route id to the user's created route list.
     *
     * @param routeId [String] object containing the route's id which should be added.
     * @return On Success: Returns [FirebaseResult.Success] with empty return\
     * On Failure: Returns [FirebaseResult.Error] with exception\
     * On Cancellation: Returns [FirebaseResult.Canceled] with exception
     *
     */

    override suspend fun addRouteToCreatedRoutes(routeId: String): FirebaseResult<Void> {
        return try {
            val userId = firebaseAuth.getCurrentUserId() ?: return NO_USER_RESULT
            fireStore.userCollection.document(userId)
                .update(CREATED_ROUTES_KEY, (arrayUnion(routeId))).await()
        } catch (exception: Exception) {
            FirebaseResult.Error(exception)
        }
    }

    /**
     * Adds the given route id to the user's active route list.
     *
     * @param routeId [String] object containing the route's id which should be added.
     * @return On Success: Returns [FirebaseResult.Success] with empty return\
     * On Failure: Returns [FirebaseResult.Error] with exception\
     * On Cancellation: Returns [FirebaseResult.Canceled] with exception
     *
     */

    override suspend fun addRouteToActiveRoutes(routeId: String): FirebaseResult<Void> {
        return try {
            val userId = firebaseAuth.getCurrentUserId() ?: return NO_USER_RESULT
            fireStore.userCollection.document(userId)
                .update(ACTIVE_ROUTES_KEY, (arrayUnion(routeId))).await()
        } catch (exception: Exception) {
            FirebaseResult.Error(exception)
        }
    }

    /**
     * Removes the given route id from the user's finished route list.
     *
     * @param routeId [String] object containing the route's id which should be removed.
     * @return On Success: Returns [FirebaseResult.Success] with empty return\
     * On Failure: Returns [FirebaseResult.Error] with exception\
     * On Cancellation: Returns [FirebaseResult.Canceled] with exception
     *
     */

    override suspend fun removeRouteFromFinishedRoutes(routeId: String): FirebaseResult<Void> {
        return try {
            val userId = firebaseAuth.getCurrentUserId() ?: return NO_USER_RESULT
            fireStore.userCollection.document(userId)
                .update(FINISHED_ROUTES_KEY, (arrayRemove(routeId))).await()
        } catch (exception: Exception) {
            FirebaseResult.Error(exception)
        }
    }

    /**
     * Removes the given route id from the user's favourite route list.
     *
     * @param routeId [String] object containing the route's id which should be removed.
     * @return On Success: Returns [FirebaseResult.Success] with empty return\
     * On Failure: Returns [FirebaseResult.Error] with exception\
     * On Cancellation: Returns [FirebaseResult.Canceled] with exception
     *
     */

    override suspend fun removeRouteFromFavouriteRoutes(routeId: String): FirebaseResult<Void> {
        return try {
            val userId = firebaseAuth.getCurrentUserId() ?: return NO_USER_RESULT
            fireStore.userCollection.document(userId)
                .update(FAVOURITE_ROUTES_KEY, (arrayRemove(routeId))).await()
        } catch (exception: Exception) {
            FirebaseResult.Error(exception)
        }
    }

    /**
     * Removes the given route id from the user's created route list.
     *
     * @param routeId [String] object containing the route's id which should be removed.
     * @return On Success: Returns [FirebaseResult.Success] with empty return\
     * On Failure: Returns [FirebaseResult.Error] with exception\
     * On Cancellation: Returns [FirebaseResult.Canceled] with exception
     *
     */

    override suspend fun removeRouteFromCreatedRoutes(routeId: String): FirebaseResult<Void> {
        return try {
            val userId = firebaseAuth.getCurrentUserId() ?: return NO_USER_RESULT
            fireStore.userCollection.document(userId)
                .update(CREATED_ROUTES_KEY, (arrayRemove(routeId))).await()
        } catch (exception: Exception) {
            FirebaseResult.Error(exception)
        }
    }

    /**
     * Removes the given route id from the user's active route list.
     *
     * @param routeId [String] object containing the route's id which should be removed.
     * @return On Success: Returns [FirebaseResult.Success] with empty return\
     * On Failure: Returns [FirebaseResult.Error] with exception\
     * On Cancellation: Returns [FirebaseResult.Canceled] with exception
     *
     */

    override suspend fun removeRouteFromActiveRoutes(routeId: String): FirebaseResult<Void> {
        return try {
            val userId = firebaseAuth.getCurrentUserId() ?: return NO_USER_RESULT
            fireStore.userCollection.document(userId)
                .update(ACTIVE_ROUTES_KEY, (arrayRemove(routeId))).await()
        } catch (exception: Exception) {
            FirebaseResult.Error(exception)
        }
    }

    override suspend fun uploadImageAndSaveUri(bitmap: Bitmap, qualityValue: Int) {
        val baos = ByteArrayOutputStream()
        val storageRef = fireStorage.reference.child("profile_pictures/${firebaseAuth.getCurrentUserId()}")
        bitmap.compress(Bitmap.CompressFormat.JPEG, qualityValue, baos)
        val image = baos.toByteArray()
        storageRef.putBytes(image)
    }
}
