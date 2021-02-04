package de.ur.explure.model.comment

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.Date

@Parcelize
data class Comment(
    val id: String,
    val message: String,
    val date: Date,
    val authorId: String,
    val answers: List<String>
) : Parcelable
