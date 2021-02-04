package de.ur.explure.model.route

import android.os.Parcelable
import de.ur.explure.model.landmark.LandmarkDTO
import kotlinx.android.parcel.Parcelize
import java.util.Date
import kotlin.collections.ArrayList

@Parcelize
data class RouteDTO(
    var authorId: String,
    var category: String,
    var date: Date,
    var title: String,
    var description: String,
    var distance: Double,
    var duration: Double,
    var thumbnail: String?,
    var landMarks: ArrayList<LandmarkDTO> = ArrayList()
) : Parcelable {

    fun addLandMark(landMark: LandmarkDTO) {
        landMarks.add(landMark)
    }
}
