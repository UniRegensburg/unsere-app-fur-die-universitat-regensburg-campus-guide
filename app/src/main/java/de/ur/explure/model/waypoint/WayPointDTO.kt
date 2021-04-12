package de.ur.explure.model.waypoint

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.firestore.GeoPoint
import de.ur.explure.config.WayPointDocumentConfig

data class WayPointDTO(
    var title: String,
    var geoPoint: GeoPoint,
    var description: String = "",
    var audioUri: Uri? = null,
    var imageUri: Uri? = null,
    var videoUri: Uri? = null,
    var audioURL: String = "",
    var videoURL: String = "",
    var imageURL: String = ""
) : Parcelable {

    fun toMap(): Map<String, Any> {
        return mapOf(
            WayPointDocumentConfig.WAYPOINT_TITLE_FIELD to title,
            WayPointDocumentConfig.WAYPOINT_GEOPOINT_FIELD to geoPoint,
            WayPointDocumentConfig.WAYPOINT_DESCRIPTION_FIELD to description,
            WayPointDocumentConfig.WAYPOINT_AUDIO_FIELD to audioURL,
            WayPointDocumentConfig.WAYPOINT_VIDEO_FIELD to videoURL,
            WayPointDocumentConfig.WAYPOINT_IMAGE_FIELD to imageURL
        )
    }

    constructor(parcel: Parcel) : this(
        title = parcel.readString() ?: "",
        description = parcel.readString() ?: "",
        geoPoint = GeoPoint(parcel.readDouble(), parcel.readDouble()),
        audioUri = parcel.readParcelable(ClassLoader.getSystemClassLoader()),
        imageUri = parcel.readParcelable(ClassLoader.getSystemClassLoader()),
        videoUri = parcel.readParcelable(ClassLoader.getSystemClassLoader()),
    )

    override fun writeToParcel(parcel: Parcel, i: Int) {
        parcel.writeDouble(geoPoint.latitude)
        parcel.writeDouble(geoPoint.longitude)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<WayPoint> {
        override fun createFromParcel(parcel: Parcel): WayPoint {
            return WayPoint(parcel)
        }

        override fun newArray(size: Int): Array<WayPoint?> {
            return arrayOfNulls(size)
        }
    }
}
