package de.ur.explure.extensions

import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap

fun MapboxMap.moveCameraToPosition(position: LatLng, zoom: Double? = null) {
    if (zoom != null) {
        easeCamera(CameraUpdateFactory.newLatLngZoom(position, zoom))
    } else {
        easeCamera(CameraUpdateFactory.newLatLng(position))
    }
}
