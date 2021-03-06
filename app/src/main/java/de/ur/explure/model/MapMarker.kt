package de.ur.explure.model

import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.firestore.GeoPoint
import com.mapbox.mapboxsdk.geometry.LatLng
import de.ur.explure.model.waypoint.WayPointDTO

data class MapMarker(
    val id: String = "",
    val wayPoint: WayPointDTO = WayPointDTO("", GeoPoint(0.0, 0.0)),
    // val markerSymbol: Symbol, // not parcelizable
    val markerPosition: LatLng = LatLng(0.0, 0.0)
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readParcelable(WayPointDTO::class.java.classLoader)
            ?: WayPointDTO("", GeoPoint(0.0, 0.0)),
        LatLng(parcel.readDouble(), parcel.readDouble())
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeParcelable(wayPoint, flags)
        parcel.writeDouble(markerPosition.latitude)
        parcel.writeDouble(markerPosition.longitude)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MapMarker> {
        override fun createFromParcel(parcel: Parcel): MapMarker {
            return MapMarker(parcel)
        }

        override fun newArray(size: Int): Array<MapMarker?> {
            return arrayOfNulls(size)
        }
    }
}
