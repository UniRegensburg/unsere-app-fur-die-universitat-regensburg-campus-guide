package de.ur.explure.model.rating

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class RatingDTO(
    var ratingValue: Int,
    var text: String,
    var routeId: String,
    var authorId: String,
) : Parcelable
