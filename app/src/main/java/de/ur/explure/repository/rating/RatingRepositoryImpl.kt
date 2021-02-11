package de.ur.explure.repository.rating

import com.google.firebase.firestore.FieldPath
import de.ur.explure.config.ErrorConfig
import de.ur.explure.config.RatingDocumentConfig.RATING_FIELD
import de.ur.explure.extensions.await
import de.ur.explure.model.rating.Rating
import de.ur.explure.model.rating.RatingDTO
import de.ur.explure.services.FireStoreInstance
import de.ur.explure.services.FirebaseAuthService
import de.ur.explure.utils.FirebaseResult

@Suppress("TooGenericExceptionCaught")
class RatingRepositoryImpl(
    private val firebaseAuth: FirebaseAuthService,
    private val fireStore: FireStoreInstance
) : RatingRepository {

    override suspend fun addRatingToFireStore(ratingDTO: RatingDTO): FirebaseResult<Void> {
        return try {
            val userId = firebaseAuth.getCurrentUserId() ?: return ErrorConfig.NO_USER_RESULT
            fireStore.ratingCollection.document().set(ratingDTO.toMap(userId)).await()
        } catch (exception: Exception) {
            FirebaseResult.Error(exception)
        }
    }

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

    override suspend fun editRating(ratingId: String, ratingValue: Int): FirebaseResult<Void> {
        return try {
            fireStore.ratingCollection.document(ratingId).update(mapOf(RATING_FIELD to ratingValue))
                .await()
        } catch (exception: Exception) {
            FirebaseResult.Error(exception)
        }
    }

    override suspend fun deleteRating(ratingId: String): FirebaseResult<Void> {
        return try {
            fireStore.ratingCollection.document(ratingId).delete()
                .await()
        } catch (exception: Exception) {
            FirebaseResult.Error(exception)
        }
    }
}
