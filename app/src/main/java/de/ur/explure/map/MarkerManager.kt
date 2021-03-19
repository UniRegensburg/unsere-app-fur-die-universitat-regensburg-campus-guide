package de.ur.explure.map

import android.app.Application
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.Symbol
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.mapbox.mapboxsdk.style.layers.Property
import de.ur.explure.R

// use the application context instead of the activity context to make sure it doesn't leak memory,
// see https://proandroiddev.com/everything-you-need-to-know-about-memory-leaks-in-android-d7a59faaf46a
class MarkerManager(
    private val context: Application,
    mapView: MapView,
    map: MapboxMap,
    private var mapStyle: Style
) : DefaultLifecycleObserver {

    private val symbolManager: SymbolManager = SymbolManager(mapView, map, mapStyle).apply {
        iconAllowOverlap = true
        iconIgnorePlacement = true
        textAllowOverlap = false
        textIgnorePlacement = false
        iconRotationAlignment = Property.ICON_ROTATION_ALIGNMENT_VIEWPORT
    }

    init {
        initMapSymbols()
        initListeners()
    }

    private fun initMapSymbols() {
        BitmapFactory.decodeResource(context.resources, R.drawable.mapbox_marker_icon_default)
            ?.let {
                // add a marker icon to the style
                mapStyle.addImage(ID_ICON, it)
            }
    }

    private fun initListeners() {
        symbolManager.addClickListener(this::onMarkerClickListener)
        symbolManager.addLongClickListener(this::onMarkerLongClickListener)
        // symbolManager.addDragListener(this::onSymbolDragged)
    }

    fun addMarker(coordinate: LatLng): Symbol? {
        return createMarker(coordinate)
    }

    fun deleteMarker(marker: Symbol) {
        symbolManager.delete(marker)
    }

    fun deleteAllMarkers() {
        symbolManager.deleteAll()
    }

    fun addMarkers(markerCoordinates: List<LatLng>?) {
        markerCoordinates?.forEach { coordinate ->
            createMarker(coordinate)
        }
    }

    private fun createMarker(coordinate: LatLng): Symbol? {
        return symbolManager.create(
            SymbolOptions()
                .withLatLng(coordinate)
                .withIconImage(ID_ICON)
                .withIconAnchor(Property.ICON_ANCHOR_BOTTOM)
                .withIconSize(1.0f)
                // TODO right now draggable=true spawns a new marker; this seems to be an open issue
                .withDraggable(false)
        )
    }

    private fun onMarkerClickListener(marker: Symbol): Boolean {
        Toast.makeText(
            context,
            "Clicked on marker ${marker.id}",
            Toast.LENGTH_SHORT
        ).show()
        return true
    }

    /**
     * Remove a marker on long click.
     * TODO deleting a marker should probably happen via its info window (e.g. a small
     * 'delete this marker'- button at the bottom)
     */
    private fun onMarkerLongClickListener(marker: Symbol): Boolean {
        deleteMarker(marker)
        // true to consume the click so the map onLongClick - Listener won't be called!
        return true
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        symbolManager.onDestroy() // cleanup to prevent leaks
    }

    companion object {
        private const val ID_ICON = "id-icon"
    }
}
