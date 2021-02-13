package de.ur.explure.model.user

import android.os.Parcelable
import de.ur.explure.config.UserDocumentConfig.ACTIVE_ROUTES_KEY
import de.ur.explure.config.UserDocumentConfig.CREATED_ROUTES_KEY
import de.ur.explure.config.UserDocumentConfig.EMAIL_KEY
import de.ur.explure.config.UserDocumentConfig.FAVOURITE_ROUTES_KEY
import de.ur.explure.config.UserDocumentConfig.FINISHED_ROUTES_KEY
import de.ur.explure.config.UserDocumentConfig.NAME_KEY
import de.ur.explure.config.UserDocumentConfig.PROFILE_PICTURE_KEY
import kotlinx.android.parcel.Parcelize

@Parcelize
data class UserDTO(
    var email: String,
    var name: String,
) : Parcelable {
    fun toMap(): Map<String, Any> {
        return mapOf(
            EMAIL_KEY to email,
            NAME_KEY to name,
            CREATED_ROUTES_KEY to emptyList<String>(),
            FAVOURITE_ROUTES_KEY to emptyList<String>(),
            FINISHED_ROUTES_KEY to emptyList<String>(),
            ACTIVE_ROUTES_KEY to emptyList<String>(),
            PROFILE_PICTURE_KEY to ""
        )
    }
}
