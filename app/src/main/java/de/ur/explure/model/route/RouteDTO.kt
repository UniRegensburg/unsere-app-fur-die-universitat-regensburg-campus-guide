package de.ur.explure.model.route

import android.os.Parcelable
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ServerTimestamp
import de.ur.explure.model.waypoint.WayPointDTO
import kotlinx.parcelize.Parcelize
import java.util.Date
import de.ur.explure.config.RouteDocumentConfig.AUTHOR_ID_FIELD
import de.ur.explure.config.RouteDocumentConfig.CATEGORY_FIELD
import de.ur.explure.config.RouteDocumentConfig.CURRENT_RATING_FIELD
import de.ur.explure.config.RouteDocumentConfig.DATE_FIELD
import de.ur.explure.config.RouteDocumentConfig.DESCR_FIELD
import de.ur.explure.config.RouteDocumentConfig.DISTANCE_FIELD
import de.ur.explure.config.RouteDocumentConfig.DURATION_FIELD
import de.ur.explure.config.RouteDocumentConfig.RATING_LIST_FIELD
import de.ur.explure.config.RouteDocumentConfig.THUMBNAIL_URL_FIELD
import de.ur.explure.config.RouteDocumentConfig.TITLE_FIELD

@Parcelize
data class RouteDTO(
    var category: String = "",
    var title: String = "",
    var description: String = "",
    var distance: Double = 0.0,
    var duration: Double = 0.0,
    var wayPoints: MutableList<WayPointDTO> = mutableListOf(),
    @ServerTimestamp
    var createdAt: Date? = null
) : Parcelable {

    fun addWayPoint(wayPoint: WayPointDTO) {
        wayPoints.add(wayPoint)
    }

    fun toMap(userId: String, thumbnailUrl: String = ""): Map<String, Any> {
        return mapOf(
            AUTHOR_ID_FIELD to userId,
            TITLE_FIELD to title,
            DESCR_FIELD to description,
            DISTANCE_FIELD to distance,
            DURATION_FIELD to duration,
            CATEGORY_FIELD to category,
            THUMBNAIL_URL_FIELD to thumbnailUrl,
            DATE_FIELD to FieldValue.serverTimestamp(),
            RATING_LIST_FIELD to emptyList<String>(),
            CURRENT_RATING_FIELD to 0
        )
    }
}
