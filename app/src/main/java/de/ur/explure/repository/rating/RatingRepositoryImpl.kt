package de.ur.explure.repository.rating

import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import de.ur.explure.config.ErrorConfig
import de.ur.explure.config.RatingDocumentConfig.DATE_FIELD
import de.ur.explure.config.RatingDocumentConfig.RATING_FIELD
import de.ur.explure.config.RouteDocumentConfig.CURRENT_RATING_FIELD
import de.ur.explure.extensions.await
import de.ur.explure.model.rating.Rating
import de.ur.explure.model.rating.RatingDTO
import de.ur.explure.repository.route.RouteRepositoryImpl
import de.ur.explure.services.FireStoreInstance
import de.ur.explure.services.FirebaseAuthService
import de.ur.explure.utils.FirebaseResult

@Suppress("TooGenericExceptionCaught")
class RatingRepositoryImpl(
    private val firebaseAuth: FirebaseAuthService,
    private val fireStore: FireStoreInstance,
    private val routeRepository: RouteRepositoryImpl
) : RatingRepository {

    /**
     * Creates a new rating document in FireStore with the given information.
     *
     * @param ratingDTO [RatingDTO] object containing the rating value and the route id.
     * @return On Success: Returns [FirebaseResult.Success] with empty return\
     * On Failure: Returns [FirebaseResult.Error] with exception\
     * On Cancellation: Returns [FirebaseResult.Canceled] with exception
     *
     */

    override suspend fun addRatingToFireStore(ratingDTO: RatingDTO): FirebaseResult<Void> {
        return try {
            val userId = firebaseAuth.getCurrentUserId() ?: return ErrorConfig.NO_USER_RESULT
            val document = fireStore.ratingCollection.document()
            when (val ratingCall =
                    document.set(ratingDTO.toMap(userId)).await()) {
                is FirebaseResult.Success -> {
                    addRatingToRoute(document.id, ratingDTO.routeId)
                    return ratingCall
                }
                is FirebaseResult.Error -> FirebaseResult.Error(ratingCall.exception)
                is FirebaseResult.Canceled -> FirebaseResult.Canceled(ratingCall.exception)
            }
        } catch (exception: Exception) {
            FirebaseResult.Error(exception)
        }
    }

    /**
     * Retrieves a Rating object from the FireStore database.
     *
     * @param ratingId [String] object with the id of the rating.
     * @return On Success: Returns [FirebaseResult.Success] with a [Rating] object as data.\
     * On Failure: Returns [FirebaseResult.Error] with exception.\
     * On Cancellation: Returns [FirebaseResult.Canceled] with exception.
     *
     */

    override suspend fun getRating(ratingId: String): FirebaseResult<Rating> {
        return try {
            when (val resultDocumentSnapshot =
                fireStore.ratingCollection.document(ratingId).get().await()) {
                is FirebaseResult.Success -> {
                    val ratingObject = resultDocumentSnapshot.data.toObject(Rating::class.java)
                        ?: return ErrorConfig.DESERIALIZATION_FAILED_RESULT
                    FirebaseResult.Success(ratingObject)
                }
                is FirebaseResult.Error -> FirebaseResult.Error(resultDocumentSnapshot.exception)
                is FirebaseResult.Canceled -> FirebaseResult.Canceled(resultDocumentSnapshot.exception)
            }
        } catch (exception: Exception) {
            FirebaseResult.Error(exception)
        }
    }

    /**
     * Retrieves a List of Rating objects from the FireStore database.
     *
     * @param ratingIds [List] of [String] with the ids which should be retrieved.
     * @return On Success: Returns [FirebaseResult.Success] with a [List] of [Rating] as data.\
     * On Failure: Returns [FirebaseResult.Error] with exception.\
     * On Cancellation: Returns [FirebaseResult.Canceled] with exception.
     *
     */

    override suspend fun getRatingList(ratingIds: List<String>): FirebaseResult<List<Rating>> {
        return try {
            when (val resultDocumentSnapshot =
                fireStore.ratingCollection.whereIn(FieldPath.documentId(), ratingIds).get()
                    .await()) {
                is FirebaseResult.Success -> {
                    val ratingObject = resultDocumentSnapshot.data.toObjects(Rating::class.java)
                    FirebaseResult.Success(ratingObject)
                }
                is FirebaseResult.Error -> FirebaseResult.Error(resultDocumentSnapshot.exception)
                is FirebaseResult.Canceled -> FirebaseResult.Canceled(resultDocumentSnapshot.exception)
            }
        } catch (exception: Exception) {
            FirebaseResult.Error(exception)
        }
    }

    /**
     * Edits a rating's value and updates its timestamp
     *
     * @param ratingId [String] object with the id of the rating.
     * @param ratingValue [Int] object with the rating value.
     * @return On Success: Returns [FirebaseResult.Success] with an empty response.\
     * On Failure: Returns [FirebaseResult.Error] with exception.\
     * On Cancellation: Returns [FirebaseResult.Canceled] with exception.
     *
     */

    override suspend fun editRating(ratingId: String, ratingValue: Int): FirebaseResult<Void> {
        return try {
            fireStore.ratingCollection.document(ratingId).update(
                mapOf(
                    RATING_FIELD to ratingValue,
                    DATE_FIELD to FieldValue.serverTimestamp()
                )
            )
                .await()
        } catch (exception: Exception) {
            FirebaseResult.Error(exception)
        }
    }

    /**
     * Deletes a rating from the FireStore database.
     *
     * @param ratingId [String] object with the id of the rating.
     * @return On Success: Returns [FirebaseResult.Success] with an empty response.\
     * On Failure: Returns [FirebaseResult.Error] with exception.\
     * On Cancellation: Returns [FirebaseResult.Canceled] with exception.
     *
     */

    override suspend fun deleteRating(ratingId: String): FirebaseResult<Void> {
        return try {
            fireStore.ratingCollection.document(ratingId).delete()
                .await()
        } catch (exception: Exception) {
            FirebaseResult.Error(exception)
        }
    }

    suspend fun addRatingToRoute(ratingId: String, routeId: String) {
        try {
            fireStore.routeCollection.document(routeId).update(CURRENT_RATING_FIELD, (FieldValue.arrayUnion(ratingId))).await()
        } catch (exception: Exception) {
            FirebaseResult.Error(exception)
        }
    }
}
