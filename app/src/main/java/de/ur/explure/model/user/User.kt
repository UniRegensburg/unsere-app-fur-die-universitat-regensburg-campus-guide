package de.ur.explure.model.user

import android.os.Parcelable
import com.google.firebase.firestore.DocumentId
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    @DocumentId
    val id: String = "",
    var email: String = "",
    var name: String = "",
    var profilePictureUrl: String = "",
    var createdRoutes: List<String> = emptyList(),
    var favouriteRoutes: List<String> = emptyList(),
    var finishedRoutes: List<String> = emptyList()
) : Parcelable
