package de.ur.explure.model.comment

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.Date

@Parcelize
data class CommentDTO(
    var message: String,
    var date: Date,
    var authorId: String
) : Parcelable
