package de.ur.explure.map

import android.app.Application
import android.graphics.Color
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.Line
import com.mapbox.mapboxsdk.plugins.annotation.LineManager
import com.mapbox.mapboxsdk.plugins.annotation.LineOptions
import com.mapbox.mapboxsdk.utils.ColorUtils
import de.ur.explure.extensions.toLatLng

class RouteLineManager(
    private val context: Application,
    mapView: MapView,
    map: MapboxMap,
    mapStyle: Style
) : DefaultLifecycleObserver {

    private val defaultLineColor = Color.BLUE
    // private val currentLines = mutableListOf<Line>()

    private val lineManager: LineManager = LineManager(mapView, map, mapStyle)

    fun addLineToMap(lineCoords: List<LatLng>, @ColorRes color: Int? = null): Line? {
        val resolvedColor = color?.let { ContextCompat.getColor(context, it) } ?: defaultLineColor
        return createLine(lineCoords, resolvedColor)
    }

    @JvmName("addLineToMapPoints")
    fun addLineToMap(linePoints: List<Point>, @ColorRes color: Int? = null): Line? {
        val resolvedColor = color?.let { ContextCompat.getColor(context, it) } ?: defaultLineColor
        val lineCoords = linePoints.map { it.toLatLng() }
        return createLine(lineCoords, resolvedColor)
    }

    fun removeLineFromMap(line: Line) {
        lineManager.delete(line)
    }

    fun clearAllLines() {
        lineManager.deleteAll()
    }

    fun addLines(lineList: List<Line>) {
        lineList.forEach { line ->
            createLine(line.latLngs)
        }
    }

    @Suppress("MagicNumber")
    private fun createLine(lineCoords: List<LatLng>, color: Int = defaultLineColor): Line? {
        return lineManager.create(
            LineOptions()
                .withLatLngs(lineCoords)
                .withLineColor(ColorUtils.colorToRgbaString(color))
                .withLineWidth(5.0f)
                .withLineOpacity(1.0f)
        )
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        lineManager.onDestroy() // cleanup to prevent leaks
    }
}
