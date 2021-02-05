package de.ur.explure.model.rating

import android.os.Parcelable
import java.util.Date
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Rating(
    val id: String,
    val ratingValue: Int,
    val createdAt: Date,
    val text: String,
    val routeId: String,
    val authorId: String,
) : Parcelable
