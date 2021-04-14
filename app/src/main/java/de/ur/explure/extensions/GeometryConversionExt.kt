package de.ur.explure.extensions

/**
 * Helper extensions to convert different geographic data types like GeoJson points, lineStrings or
 * features, Firebase GeoPoints, Polylines and LatLng-Coordinates into each other.
 */

import android.location.Location
import com.google.firebase.firestore.GeoPoint
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

fun Location.toLatLng(): LatLng =
    if (this.hasAltitude()) {
        LatLng(this.latitude, this.longitude, this.altitude)
    } else {
        LatLng(this.latitude, this.longitude)
    }

fun LatLng.toPoint(): Point = Point.fromLngLat(this.longitude, this.latitude)

fun LatLng.toGeoPoint(): GeoPoint = GeoPoint(latitude, longitude)

fun String.toLatLngList(): List<LatLng> {
    val lineString = LineString.fromPolyline(this, PRECISION_6)
    val coordinates = lineString.coordinates()
    return coordinates.map { it.toLatLng() }
}

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

fun GeoPoint.toLatLng(): LatLng = LatLng(latitude, longitude)

fun GeoPoint.toFeature(): Feature = Feature.fromGeometry(this.toLatLng().toPoint())
