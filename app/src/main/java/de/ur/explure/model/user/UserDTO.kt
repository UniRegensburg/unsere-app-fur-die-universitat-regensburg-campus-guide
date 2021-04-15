package de.ur.explure.model.user

import android.os.Parcelable
import de.ur.explure.config.UserDocumentConfig.COMMENT_COUNT_KEY
import de.ur.explure.config.UserDocumentConfig.CREATED_ROUTES_KEY
import de.ur.explure.config.UserDocumentConfig.EMAIL_KEY
import de.ur.explure.config.UserDocumentConfig.ENDED_ROUTES_KEY
import de.ur.explure.config.UserDocumentConfig.FAVOURITE_ROUTES_KEY
import de.ur.explure.config.UserDocumentConfig.FINISHED_ROUTES_KEY
import de.ur.explure.config.UserDocumentConfig.NAME_KEY
import de.ur.explure.config.UserDocumentConfig.PROFILE_PICTURE_KEY
import de.ur.explure.config.UserDocumentConfig.RATING_COUNT_KEY
import de.ur.explure.config.UserDocumentConfig.STARTED_ROUTES_KEY
import kotlinx.parcelize.Parcelize

@Parcelize
data class UserDTO(
    var email: String,
    var name: String,
) : Parcelable {
    fun toMap(userId: String): Map<String, Any> {
        return mapOf(
            EMAIL_KEY to email,
            NAME_KEY to name,
            CREATED_ROUTES_KEY to emptyList<String>(),
            FAVOURITE_ROUTES_KEY to emptyList<String>(),
            FINISHED_ROUTES_KEY to emptyList<String>(),
            PROFILE_PICTURE_KEY to "gs://explure-2d2f1.appspot.com/profile_pictures/$userId",
            STARTED_ROUTES_KEY to 0,
            ENDED_ROUTES_KEY to 0,
            COMMENT_COUNT_KEY to 0,
            RATING_COUNT_KEY to 0
        )
    }
}
