package de.ur.explure.model.route

import android.os.Parcelable
import com.google.firebase.firestore.DocumentId
import com.mapbox.mapboxsdk.geometry.LatLng
import de.ur.explure.model.comment.Comment
import de.ur.explure.model.waypoint.WayPoint
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
data class Route(
    @DocumentId
    val id: String = "",
    val authorId: String = "",
    val category: String = "",
    val createdAt: Date = Date(),
    val title: String = "",
    val description: String = "",
    val distance: Double = 0.0,
    val duration: Double = 0.0,
    val thumbnailUrl: String = "",
    val wayPoints: LinkedList<WayPoint> = LinkedList(),
    val comments: LinkedList<Comment> = LinkedList(),
    val rating: List<String> = emptyList(),
    val wayPointCount: Int = 0,
    val currentRating: Double = 0.0,
    val commentCount: Int = 0,
    val routeCoordinates: MutableList<LatLng> = mutableListOf()
) : Parcelable {

    fun addRouteCoordinates(latLngList: List<LatLng>) {
        routeCoordinates.addAll(latLngList)
    }

    fun fillWayPoints(wayPointList: List<WayPoint>) {
        wayPoints.addAll(wayPointList)
    }

    fun fillComments(commentList: List<Comment>) {
        comments.addAll(commentList)
    }
}
