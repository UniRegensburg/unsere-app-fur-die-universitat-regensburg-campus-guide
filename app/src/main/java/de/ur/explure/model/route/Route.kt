package de.ur.explure.model.route

import android.os.Parcelable
import de.ur.explure.model.comment.Comment
import de.ur.explure.model.landmark.Landmark
import kotlinx.android.parcel.Parcelize
import java.util.Date
import kotlin.collections.ArrayList

@Parcelize
data class Route(
    val id: String,
    val authorId: String,
    val category: String,
    val date: Date,
    val title: String,
    val description: String,
    val distance: Double,
    val duration: Double,
    val thumbnail: String?,
    val landMarks: ArrayList<Landmark>,
    val comments: List<Comment> = emptyList(),
    val rating: List<String> = emptyList()
) : Parcelable
