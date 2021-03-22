package de.ur.explure.model

import android.os.Parcelable
import com.mapbox.mapboxsdk.geometry.LatLng
import de.ur.explure.model.waypoint.WayPoint
import kotlinx.parcelize.Parcelize

@Parcelize
data class MapMarker(
    val id: String = "",
    val wayPoint: WayPoint = WayPoint(),
    // val markerSymbol: Symbol, // not parcelizable
    val markerPosition: LatLng = LatLng(0.0, 0.0)
) : Parcelable
