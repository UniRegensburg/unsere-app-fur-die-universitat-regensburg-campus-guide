package de.ur.explure.map

import android.app.Application
import android.graphics.BitmapFactory
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.OnSymbolClickListener
import com.mapbox.mapboxsdk.plugins.annotation.Symbol
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.mapbox.mapboxsdk.style.layers.Property
import de.ur.explure.R
import de.ur.explure.extensions.moveCameraToPosition
import de.ur.explure.extensions.toLatLng
import de.ur.explure.model.MapMarker
import de.ur.explure.model.waypoint.WayPointDTO
import de.ur.explure.views.MapFragment.Companion.selectedMarkerZoom

// use the application context instead of the activity context to make sure it doesn't leak memory,
// see https://proandroiddev.com/everything-you-need-to-know-about-memory-leaks-in-android-d7a59faaf46a
class MarkerManager(
    private val context: Application,
    private val mapView: MapView,
    private val map: MapboxMap,
    private val mapStyle: Style
) : DefaultLifecycleObserver {

    private val symbolManager: SymbolManager = SymbolManager(mapView, map, mapStyle).apply {
        iconAllowOverlap = true
        iconIgnorePlacement = true
        textAllowOverlap = false
        textIgnorePlacement = false
        iconRotationAlignment = Property.ICON_ROTATION_ALIGNMENT_VIEWPORT
    }

    private var symbolClickListenerBehavior: OnSymbolClickListener? = null

    // TODO for testing: this list size should ALWAYS equal the mapMarkers list size in the mapViewmodel!
    private val activeMarkers: MutableList<Symbol> = mutableListOf()
    // TODO for testing: this list size should ALWAYS equal the routeWaypoints list size in the editViewmodel!
    private val activeWaypoints: MutableMap<Symbol, WayPointDTO> = mutableMapOf()

    init {
        initMapSymbols()
    }

    private fun initMapSymbols() {
        BitmapFactory.decodeResource(context.resources, R.drawable.mapbox_marker_icon_default)
            ?.let {
                // add a marker icon to the style
                mapStyle.addImage(MARKER_ICON, it)
            }
        // TODO own symbols for waypoints ??
        /*
        BitmapFactory.decodeResource(context.resources, R.drawable.mapbox_ic_map_marker_light)
            ?.let {
                // add a marker icon to the style
                mapStyle.addImage(WAYPOINT_ICON, it)
            }*/
    }

    /**
     * Methods for route editing:
     */

    fun addWaypoint(coordinate: LatLng, waypoint: WayPointDTO): Symbol? {
        return createWaypoint(coordinate, waypoint)
    }

    fun addWaypoints(waypoints: List<WayPointDTO>?) {
        waypoints?.forEach { waypoint ->
            val coordinates = waypoint.geoPoint.toLatLng()
            createWaypoint(coordinates, waypoint)
        }
    }

    private fun createWaypoint(coordinate: LatLng, waypoint: WayPointDTO): Symbol? {
        val waypointSymbol = symbolManager.create(
            SymbolOptions()
                .withLatLng(coordinate)
                .withIconImage(MARKER_ICON)
                .withIconAnchor(Property.ICON_ANCHOR_BOTTOM)
                .withIconOffset(markerIconOffset)
                // .withTextField(waypointName)
                .withIconSize(1.0f)
                // .withDraggable(true) // TODO
        )
        activeWaypoints[waypointSymbol] = waypoint
        return waypointSymbol
    }

    fun deleteWaypoint(waypointSymbol: Symbol) {
        symbolManager.delete(waypointSymbol)
        activeWaypoints.remove(waypointSymbol)
    }

    fun setEditingMarkerClickBehavior(onMarkerClicked: (waypoint: WayPointDTO) -> Unit) {
        symbolClickListenerBehavior?.let { symbolManager.removeClickListener(it) }

        symbolClickListenerBehavior = OnSymbolClickListener {
            val wayPoint = activeWaypoints[it]
            if (wayPoint != null) {
                onMarkerClicked(wayPoint)
            }
            true
        }
        symbolClickListenerBehavior?.let { symbolManager.addClickListener(it) }
    }

    /**
     * Methods for route creation:
     */

    fun addMarker(coordinate: LatLng): Symbol? {
        return createMarker(coordinate)
    }

    fun addMarkers(markerCoordinates: List<LatLng>?) {
        markerCoordinates?.forEach { coordinate ->
            createMarker(coordinate)
        }
    }

    private fun createMarker(coordinate: LatLng): Symbol? {
        val newMarker = symbolManager.create(
            SymbolOptions()
                .withLatLng(coordinate)
                .withIconImage(MARKER_ICON)
                .withIconAnchor(Property.ICON_ANCHOR_BOTTOM)
                .withIconSize(1.0f)
                .withDraggable(false)
        )
        activeMarkers.add(newMarker)
        return newMarker
    }

    fun deleteMarker(marker: Symbol) {
        symbolManager.delete(marker)
        activeMarkers.remove(marker)
    }

    fun removeWaypointMarker(waypointMarker: MapMarker) {
        val markerSymbol = activeMarkers.find {
            it.latLng == waypointMarker.markerPosition
        }

        if (markerSymbol != null) {
            activeMarkers.remove(markerSymbol)
            deleteMarker(markerSymbol)
        }
    }

    fun deleteAllMarkers() {
        symbolManager.deleteAll()
        activeMarkers.clear()
        activeWaypoints.clear()
    }

    fun setDefaultMarkerClickListenerBehavior() {
        // remove the active click listener behavior first before setting a new one;
        // otherwise only the click listener behavior that was first set would be used!
        symbolClickListenerBehavior?.let { symbolManager.removeClickListener(it) }

        symbolClickListenerBehavior = OnSymbolClickListener {
            map.moveCameraToPosition(it.latLng, selectedMarkerZoom)
            // true to consume the click so the map onClick - Listener won't be called!
            true
        }
        symbolClickListenerBehavior?.let { symbolManager.addClickListener(it) }
    }

    fun setDeleteMarkerClickListenerBehavior(onMarkerDeleted: (marker: Symbol) -> Unit) {
        symbolClickListenerBehavior?.let { symbolManager.removeClickListener(it) }

        symbolClickListenerBehavior = OnSymbolClickListener {
            // Hier kann der applicationcontext nicht verwendet werden, da der materialAlertDialogBuilder
            // sonst ein AppCompatTheme als BaseTheme erwartet und crashen würde!
            with(MaterialAlertDialogBuilder(mapView.context)) {
                setMessage(R.string.delete_marker_confirmation)
                setPositiveButton(R.string.yes) { _, _ ->
                    deleteMarker(it)
                    onMarkerDeleted(it)
                }
                setNegativeButton(R.string.cancel) { _, _ -> }
                show()
            }
            true
        }
        symbolClickListenerBehavior?.let { symbolManager.addClickListener(it) }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        // cleanup to prevent leaks (removes click listeners and the annotation manager)
        symbolManager.onDestroy()
    }

    companion object {
        private const val MARKER_ICON = "marker-icon"

        // moves the marker icon offset a little bit down so it feels closer to the actual touch point
        private val markerIconOffset = arrayOf(0f, 12f)
    }
}
