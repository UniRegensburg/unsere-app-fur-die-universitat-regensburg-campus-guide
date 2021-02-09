package de.ur.explure.model.user

import android.os.Parcelable
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import de.ur.explure.config.UserDocumentConfig.ACTIVE_ROUTES_KEY
import de.ur.explure.config.UserDocumentConfig.CREATED_ROUTES_KEY
import de.ur.explure.config.UserDocumentConfig.EMAIL_KEY
import de.ur.explure.config.UserDocumentConfig.FAVOURITE_ROUTES_KEY
import de.ur.explure.config.UserDocumentConfig.FINISHED_ROUTES_KEY
import de.ur.explure.config.UserDocumentConfig.NAME_KEY
import de.ur.explure.config.UserDocumentConfig.PROFILE_PICTURE_KEY
import kotlinx.android.parcel.Parcelize

@Parcelize
data class User(
    @DocumentId
    val id: String = "",
    @get:PropertyName(EMAIL_KEY)
    @set:PropertyName(EMAIL_KEY)
    var email: String = "",
    @get:PropertyName(NAME_KEY)
    @set:PropertyName(NAME_KEY)
    var name: String = "",
    @get:PropertyName(PROFILE_PICTURE_KEY)
    @set:PropertyName(PROFILE_PICTURE_KEY)
    var profilePictureUrl: String = "",
    @get:PropertyName(ACTIVE_ROUTES_KEY)
    @set:PropertyName(ACTIVE_ROUTES_KEY)
    var activeRoutes: List<String> = emptyList(),
    @get:PropertyName(CREATED_ROUTES_KEY)
    @set:PropertyName(CREATED_ROUTES_KEY)
    var createdRotes: List<String> = emptyList(),
    @get:PropertyName(FAVOURITE_ROUTES_KEY)
    @set:PropertyName(FAVOURITE_ROUTES_KEY)
    var favouriteRoutes: List<String> = emptyList(),
    @get:PropertyName(FINISHED_ROUTES_KEY)
    @set:PropertyName(FINISHED_ROUTES_KEY)
    var finishedRoutes: List<String> = emptyList()
) : Parcelable
