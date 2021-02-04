package de.ur.explure.model.rating

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Rating(
    val id : String,
    val ratingValue: Int,
    val text: String,
    val routeId: String,
    val authorId: String,
) : Parcelable
