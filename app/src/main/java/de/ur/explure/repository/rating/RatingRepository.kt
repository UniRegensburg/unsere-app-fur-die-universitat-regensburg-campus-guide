package de.ur.explure.repository.rating

import de.ur.explure.model.rating.Rating
import de.ur.explure.model.rating.RatingDTO
import de.ur.explure.utils.FirebaseResult

interface RatingRepository {

    suspend fun addRatingToFireStore(ratingDTO: RatingDTO): FirebaseResult<Void>

    suspend fun getRating(ratingId: String): FirebaseResult<Rating>

    suspend fun getRatingList(ratingIds: List<String>): FirebaseResult<List<Rating>>

    suspend fun editRating(ratingId: String, ratingValue: Int): FirebaseResult<Void>

    suspend fun deleteRating(ratingId: String): FirebaseResult<Void>
}
