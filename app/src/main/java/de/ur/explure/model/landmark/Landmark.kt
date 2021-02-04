package de.ur.explure.model.landmark

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Landmark(
    val id: String,
    val title: String,
    val description: String,
    val coordinates: String,
    val audioURL: String?,
    val imageURL: String?,
    val videoURL: String?
) : Parcelable
