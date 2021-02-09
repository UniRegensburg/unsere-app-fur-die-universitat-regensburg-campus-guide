package de.ur.explure.model.comment

import android.os.Parcelable
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date
import kotlinx.android.parcel.Parcelize

@Parcelize
data class CommentDTO(
    var message: String,
    var authorId: String,
    @ServerTimestamp
    val createdAt: Date? = null,
) : Parcelable
