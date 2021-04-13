package de.ur.explure.map

import android.app.Application
import android.graphics.BitmapFactory
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.GeoPoint
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.OnSymbolClickListener
import com.mapbox.mapboxsdk.plugins.annotation.OnSymbolDragListener
import com.mapbox.mapboxsdk.plugins.annotation.Symbol
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.mapbox.mapboxsdk.style.layers.Property
import de.ur.explure.R
import de.ur.explure.extensions.moveCameraToPosition
import de.ur.explure.extensions.toLatLng
import de.ur.explure.model.waypoint.WayPointDTO

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

    private var markerEditListener: MarkerEditListener? = null

    private var symbolClickListenerBehavior: OnSymbolClickListener? = null

    // TODO for testing: this list size should ALWAYS equal the mapMarkers list size in the mapViewmodel!
    private val activeMarkers: MutableList<Symbol> = mutableListOf() // only used in mapFragment
    // TODO for testing: this list size should ALWAYS equal the routeWaypoints list size in the editViewmodel!
    private var activeWaypoints: MutableMap<Symbol, WayPointDTO> = mutableMapOf() // only used in editRouteFragment

    init {
        initMapSymbols()
    }

    private fun initMapSymbols() {
        BitmapFactory.decodeResource(context.resources, R.drawable.mapbox_marker_icon_default)
            ?.let {
                // add a marker icon to the style
                mapStyle.addImage(MARKER_ICON, it)
            }
        ContextCompat.getDrawable(context, R.drawable.ic_marker_destination)?.let {
            mapStyle.addImage(DESTINATION_ICON, it)
        }
    }

    fun setMarkerEditListener(listener: MarkerEditListener) {
        markerEditListener = listener
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
                .withIconSize(1.0f)
                // .withDraggable(true)
        )
        activeWaypoints[waypointSymbol] = waypoint
        return waypointSymbol
    }

    fun deleteWaypoint(waypoint: WayPointDTO) {
        var deletedSymbol: Symbol? = null
        activeWaypoints.entries.forEach { entry ->
            if (entry.value == waypoint) {
                deletedSymbol = entry.key
            }
        }
        symbolManager.delete(deletedSymbol)
        activeWaypoints.remove(deletedSymbol)
    }

    fun setEditingMarkerClickBehavior() {

        // add a long click listener to enable marker dragging without triggering the map listeners!
        symbolManager.addLongClickListener {
            activeWaypoints.entries.forEach { entry ->
                if (entry.key == it) {
                    // only set dragging dynamically to prevent bugs with the map listeners!
                    // this has to be set after getting the waypoint!
                    entry.key.isDraggable = true
                    markerEditListener?.onMarkerLongClicked(entry.value)
                }
            }
            true
        }

        symbolManager.addDragListener(object : OnSymbolDragListener {
            override fun onAnnotationDragStarted(annotation: Symbol?) {
                // not needed
            }

            override fun onAnnotationDrag(annotation: Symbol?) {
                // not needed
            }

            override fun onAnnotationDragFinished(annotation: Symbol?) {
                activeWaypoints.entries.forEach { entry ->
                    if (entry.key == annotation) {
                        entry.key.isDraggable = false

                        val newPosition = annotation.latLng
                        markerEditListener?.onMarkerPositionChanged(newPosition, entry.value)
                        // update the waypoint AFTER the callback because in the featureCollection
                        // we still have the old coordinates!
                        entry.value.geoPoint = GeoPoint(newPosition.latitude, newPosition.longitude)
                    }
                }
            }
        })

        symbolClickListenerBehavior?.let { symbolManager.removeClickListener(it) }
        symbolClickListenerBehavior = OnSymbolClickListener {
            var wayPoint: WayPointDTO? = null
            // has to be filtered manually as map[it] wouldn't work here after dragging for example
            for (entry in activeWaypoints.entries) {
                if (entry.key == it) {
                    wayPoint = entry.value
                    break
                }
            }

            wayPoint?.let { wp -> markerEditListener?.onMarkerClicked(wp) }
            true
        }
        symbolClickListenerBehavior?.let { symbolManager.addClickListener(it) }
    }

    /**
     * Methods for route creation:
     */

    fun addMarker(coordinate: LatLng, icon: String = MARKER_ICON): Symbol? {
        return createMarker(coordinate, icon)
    }

    fun addMarkers(markerCoordinates: List<LatLng>?) {
        markerCoordinates?.forEach { coordinate ->
            createMarker(coordinate)
        }
    }

    private fun createMarker(coordinate: LatLng, icon: String = MARKER_ICON): Symbol? {
        val newMarker = symbolManager.create(
            SymbolOptions()
                .withLatLng(coordinate)
                .withIconImage(icon)
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

        markerEditListener = null
    }

    companion object {
        private const val MARKER_ICON = "marker-icon"
        const val DESTINATION_ICON = "destination-icon"

        // moves the marker icon offset a little bit down so it feels closer to the actual touch point
        private val markerIconOffset = arrayOf(0f, 12f)

        const val selectedMarkerZoom = 17.0
    }

    interface MarkerEditListener {
        fun onMarkerClicked(waypoint: WayPointDTO)

        fun onMarkerLongClicked(waypoint: WayPointDTO)

        fun onMarkerPositionChanged(newPosition: LatLng, waypoint: WayPointDTO)
    }
}
