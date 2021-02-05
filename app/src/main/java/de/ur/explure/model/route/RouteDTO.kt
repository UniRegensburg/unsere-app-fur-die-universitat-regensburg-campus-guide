package de.ur.explure.model.route

import android.os.Parcelable
import com.google.firebase.firestore.ServerTimestamp
import de.ur.explure.model.landmark.LandmarkDTO
import kotlinx.android.parcel.Parcelize
import java.util.Date

@Parcelize
data class RouteDTO(
    var authorId: String,
    var category: String,
    var title: String,
    var description: String,
    var distance: Double,
    var duration: Double,
    var thumbnail: String?,
    var landMarks: MutableList<LandmarkDTO> = mutableListOf(),
    @ServerTimestamp
    var createdAt: Date? = null,
) : Parcelable {

    fun addLandMark(landMark: LandmarkDTO) {
        landMarks.add(landMark)
    }
}
