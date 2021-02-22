package de.ur.explure.model.rating

import android.os.Parcelable
import com.google.firebase.firestore.FieldValue
import de.ur.explure.config.RatingDocumentConfig
import kotlinx.android.parcel.Parcelize

@Parcelize
data class RatingDTO(
    var ratingValue: Int,
    var routeId: String
) : Parcelable {

    fun toMap(userId: String): Map<String, Any> {
        return mapOf(
            RatingDocumentConfig.RATING_FIELD to ratingValue,
            RatingDocumentConfig.ROUTE_FIELD to routeId,
            RatingDocumentConfig.DATE_FIELD to FieldValue.serverTimestamp(),
            RatingDocumentConfig.AUTHOR_FIELD to userId
        )
    }
}
