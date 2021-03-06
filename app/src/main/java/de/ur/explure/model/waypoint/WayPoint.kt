package de.ur.explure.model.waypoint

import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint

data class WayPoint(
    @DocumentId
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val geoPoint: GeoPoint = GeoPoint(0.0, 0.0),
    val audioURL: String? = null,
    val imageURL: String? = null,
    val videoURL: String? = null
) : Parcelable {

    constructor(parcel: Parcel) : this(
        id = parcel.readString() ?: "",
        title = parcel.readString() ?: "",
        description = parcel.readString() ?: "",
        geoPoint = GeoPoint(parcel.readDouble(), parcel.readDouble()),
        audioURL = parcel.readString(),
        imageURL = parcel.readString(),
        videoURL = parcel.readString(),
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
