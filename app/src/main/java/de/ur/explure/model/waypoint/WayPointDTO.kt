package de.ur.explure.model.waypoint

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class WayPointDTO(
    var title: String,
    var coordinates: String,
    var description: String = "",
    var audioURL: String? = null,
    var imageURL: String? = null,
    var videoURL: String? = null
) : Parcelable
