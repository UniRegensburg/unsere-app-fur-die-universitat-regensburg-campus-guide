package de.ur.explure.model.user

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class UserDTO(
    var email: String,
    var password: String,
    var name: String
) : Parcelable
