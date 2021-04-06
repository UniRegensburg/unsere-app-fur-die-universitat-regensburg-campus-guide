package de.ur.explure.map

import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds

object MapConstants {

    // custom margins of the mapbox compass
    const val compassMarginLeft = 10
    const val compassMarginBottom = 100

    // camera bounding box (only the relevant part of Regensburg around the university)
    private const val southWestLatitude = 48.990768
    private const val southWestLongitude = 12.087611
    private const val northEastLatitude = 49.006718
    private const val northEastLongitude = 12.101880
    private val southWestCorner = LatLng(southWestLatitude, southWestLongitude)
    private val northEastCorner = LatLng(northEastLatitude, northEastLongitude)

    val latLngBounds: LatLngBounds = LatLngBounds.Builder()
        .include(southWestCorner)
        .include(northEastCorner)
        .build()
}
