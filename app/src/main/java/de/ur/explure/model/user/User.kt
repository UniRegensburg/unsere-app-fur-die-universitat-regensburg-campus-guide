package de.ur.explure.model.user

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class User(
    val id: String,
    val email: String,
    val name: String,
    val profilePictureUrl: String?,
    val activeRoutes: List<String> = emptyList(),
    val createdRotes: List<String> = emptyList(),
    val favouriteRoutes: List<String> = emptyList(),
    val finishedRoutes: List<String> = emptyList()
) : Parcelable
