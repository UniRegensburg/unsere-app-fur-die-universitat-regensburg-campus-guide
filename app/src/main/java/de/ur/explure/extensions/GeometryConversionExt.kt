package de.ur.explure.extensions

import android.location.Location
import com.mapbox.core.constants.Constants.PRECISION_6
import com.mapbox.geojson.Feature
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.geometry.LatLng

fun Point.toLocation(): Location {
    val location = Location("")
    location.latitude = this.latitude()
    location.longitude = this.longitude()
    return location
}

fun Location.toPoint(): Point = Point.fromLngLat(this.longitude, this.latitude)

fun LatLng.toPoint(): Point = Point.fromLngLat(this.longitude, this.latitude)

fun Point.toLatLng(): LatLng =
    if (this.hasAltitude()) {
        LatLng(this.latitude(), this.longitude(), this.altitude())
    } else {
        LatLng(this.latitude(), this.longitude())
    }

fun MutableList<Point>.toLineString(): LineString = LineString.fromLngLats(this)

fun Feature.pointToLatLng(): LatLng {
    val pointFeature = geometry() as Point
    return LatLng(pointFeature.latitude(), pointFeature.longitude())
}

fun Feature.lineToPoints(): MutableList<Point> = (geometry() as LineString).coordinates()

fun Feature.lineToPolyline(): String = (geometry() as LineString).toPolyline(PRECISION_6)

fun Feature.toLineString(): LineString = geometry() as LineString

fun LineString.toFeature(): Feature = Feature.fromGeometry(this)
