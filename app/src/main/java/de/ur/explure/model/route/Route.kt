package de.ur.explure.model.route

import android.os.Parcelable
import de.ur.explure.model.comment.Comment
import de.ur.explure.model.waypoint.WayPoint
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class Route(
    val id: String,
    val authorId: String,
    val category: String,
    val createdAt: Date,
    val title: String,
    val description: String,
    val distance: Double,
    val duration: Double,
    val thumbnailUrl: String? = null,
    val landMarks: LinkedList<WayPoint>,
    val comments: List<Comment> = emptyList(),
    val rating: List<String> = emptyList()
) : Parcelable
