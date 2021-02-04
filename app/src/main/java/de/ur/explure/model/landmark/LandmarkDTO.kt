package de.ur.explure.model.landmark

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class LandmarkDTO(
    var title: String,
    var coordinates: String,
    var description: String = "",
    var audioURL: String? = null,
    var imageURL: String? = null,
    var videoURL: String? = null
) : Parcelable
