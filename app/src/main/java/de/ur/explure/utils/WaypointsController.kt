package de.ur.explure.utils

import android.location.Location
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.geometry.LatLng
import de.ur.explure.extensions.toPoint

/**
 * Utility - Class to temporarily store a list of waypoints.
 *
 * Taken from https://github.com/mapbox/mapbox-navigation-android/blob/main/examples/src/main/java/com/mapbox/navigation/examples/core/utils/WaypointsController.kt
 */

class WaypointsController {
    private val waypoints = mutableListOf<Point>()

    /**
     * Add a waypoint with the given LatLng-Coordinates.
     */
    fun add(latLng: LatLng) {
        waypoints.add(latLng.toPoint())
    }

    /**
     * Clear all waypoints.
     */
    fun clear() {
        waypoints.clear()
    }

    /**
     * Get all waypoints as a list starting with the given starting point.
     */
    fun coordinates(originLocation: Location): List<Point> {
        val coordinates = mutableListOf<Point>()
        coordinates.add(originLocation.toPoint())
        coordinates.addAll(waypoints)

        // ? clear()
        return coordinates
    }
}
