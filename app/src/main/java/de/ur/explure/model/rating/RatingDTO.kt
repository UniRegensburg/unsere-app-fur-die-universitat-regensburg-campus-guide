package de.ur.explure.model.rating

import android.os.Parcelable
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date
import kotlinx.android.parcel.Parcelize

@Parcelize
data class RatingDTO(
    var ratingValue: Int,
    var text: String,
    var routeId: String,
    var authorId: String,
    @ServerTimestamp
    val createdAt: Date? = null
) : Parcelable
