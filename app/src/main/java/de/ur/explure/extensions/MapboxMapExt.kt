package de.ur.explure.extensions

import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap

fun MapboxMap.moveCameraToPosition(position: LatLng, zoom: Double? = null) {
    val newCameraPositionBuilder = CameraPosition.Builder()
        .target(position)

    val newCameraPosition = if (zoom != null) {
        newCameraPositionBuilder.zoom(zoom).build()
    } else {
        newCameraPositionBuilder.build()
    }

    easeCamera(CameraUpdateFactory.newCameraPosition(newCameraPosition))
}
