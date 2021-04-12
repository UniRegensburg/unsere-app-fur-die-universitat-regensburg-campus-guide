package de.ur.explure.model.waypoint

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.firestore.GeoPoint

data class WayPointDTO(
    var title: String,
    var geoPoint: GeoPoint,
    var description: String = "",
    var audioUri: Uri? = null,
    var imageUri: Uri? = null,
    var videoUri: Uri? = null
) : Parcelable {

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
