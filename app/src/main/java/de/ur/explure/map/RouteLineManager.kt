package de.ur.explure.map

import android.annotation.SuppressLint
import android.app.Application
import android.graphics.Color
import android.graphics.PointF
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.Line
import com.mapbox.mapboxsdk.plugins.annotation.LineManager
import com.mapbox.mapboxsdk.plugins.annotation.LineOptions
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.Property.LINE_CAP_ROUND
import com.mapbox.mapboxsdk.style.layers.Property.LINE_JOIN_ROUND
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineCap
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineColor
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineJoin
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineOpacity
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineWidth
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.mapboxsdk.utils.ColorUtils
import de.ur.explure.extensions.toFeature
import de.ur.explure.extensions.toLatLng
import de.ur.explure.extensions.toLineString
import timber.log.Timber
import java.util.*

class RouteLineManager(
    private val context: Application,
    private val mapView: MapView,
    private val map: MapboxMap,
    mapStyle: Style
) : DefaultLifecycleObserver {

    private val lineManager: LineManager = LineManager(mapView, map, mapStyle).apply {
        lineCap = LINE_CAP_ROUND
    }
    private val defaultLineOptions = LineOptions()
        .withLineWidth(DEFAULT_LINE_WIDTH)
        .withLineJoin(LINE_JOIN_ROUND)
        .withLineOpacity(DEFAULT_LINE_OPACITY)

    // TODO these should probably be saved in the viewModel instead
    //  -> a config or map style change resets all of these right now!
    private var routeDrawFeatureList: MutableList<Feature> = mutableListOf()
    // private var routeDrawLineList: MutableList<LineString> = mutableListOf()
    private var currentRoutePoints: MutableList<Point> = mutableListOf()
    private var eraseList = mutableListOf<Point>()

    @SuppressLint("ClickableViewAccessibility")
    private val mapDrawListener = View.OnTouchListener { _, motionEvent ->
        // get the touch position on the screen
        val latLngTouchCoordinate: LatLng = map.projection.fromScreenLocation(PointF(motionEvent.x, motionEvent.y))
        val screenTouchPoint = Point.fromLngLat(latLngTouchCoordinate.longitude, latLngTouchCoordinate.latitude)

        currentRoutePoints.add(screenTouchPoint)
        // make a copy as otherwise the references to the old points will be kept and cleared below!
        val points = currentRoutePoints.toMutableList()

        // create a unique id for the currently drawn route and draw it while the user touches the screen
        val routeId = UUID.randomUUID().toString()
        drawRoute(points, routeId)

        // Take certain actions when the drawing is done
        if (motionEvent.action == MotionEvent.ACTION_UP) {
            // TODO add a marker to the start and end of the route and save in waypointsContoller
            //  so we can mapmatch the route later

            // TODO listener for markerManager calls to prevent tightly coupling of the managers!
            // currentRoute.firstOrNull()?.toLatLng()?.let { markerManager.addMarker(it) }
            // currentRoute.lastOrNull()?.toLatLng()?.let { markerManager.addMarker(it) }

            saveRoute(points, routeId)
        }
        true
    }

    @SuppressLint("ClickableViewAccessibility")
    private val eraseDrawListener = View.OnTouchListener { _, motionEvent ->
        val latLngTouchCoordinate: LatLng = map.projection.fromScreenLocation(PointF(motionEvent.x, motionEvent.y))
        val screenTouchPoint = Point.fromLngLat(latLngTouchCoordinate.longitude, latLngTouchCoordinate.latitude)

        eraseList.add(screenTouchPoint)

        // TODO
        /*
        val completeRoute = mutableListOf<Point>()
        routeDrawLineList.forEach { lineString ->
            completeRoute.addAll(lineString.coordinates())
        }

        completeRoute.removeAll(eraseList)

        routeDrawLineList.forEach { lineString ->
            if (lineString.coordinates().contains(screenTouchPoint)) {
                lineString.coordinates().remove(screenTouchPoint)
            }
        }*/

        // drawRoute(currentRoutePoints)
        true
    }

    /**
     * Enable moving the map
     */
    @SuppressLint("ClickableViewAccessibility")
    fun enableMapMovement() {
        mapView.setOnTouchListener(null)
    }

    /**
     * Enable drawing on the map by setting the custom touch listener on the [MapView]
     */
    @SuppressLint("ClickableViewAccessibility")
    fun enableMapDrawing() {
        mapView.setOnTouchListener(mapDrawListener)
    }

    @SuppressLint("ClickableViewAccessibility")
    fun enableErasingRoute() {
        mapView.setOnTouchListener(eraseDrawListener)
    }

    // use map sources and layers for free drawing as this increases the performance A LOT
    fun initFreeDrawMode() {
        val mapStyle = map.style

        val layerID = DRAW_LINE_LAYER_ID
        if (checkIfLayerExists(layerID)) {
            if (!removeCustomLineLayerFromMap(layerID)) {
                // if we couldn't remove the layer and it's source, return to prevent crashes
                Timber.e("Couldn't remove the layer: $layerID!")
                return
            }
        }

        mapStyle?.addSource(GeoJsonSource(DRAW_LINE_LAYER_SOURCE_ID))

        // Add freehand draw LineLayer to the map below the symbol icons on the map
        mapStyle?.addLayerBelow(
            LineLayer(
                DRAW_LINE_LAYER_ID,
                DRAW_LINE_LAYER_SOURCE_ID
            ).withProperties(
                lineCap(LINE_CAP_ROUND),
                lineWidth(DEFAULT_LINE_WIDTH),
                lineJoin(LINE_JOIN_ROUND),
                lineOpacity(DEFAULT_LINE_OPACITY),
                lineColor(DEFAULT_LINE_COLOR)
            ), MAPBOX_ICON_LAYER
        )
    }

    fun getCompleteRoute(): MutableList<Point> {
        val completeRoute = mutableListOf<Point>()
        routeDrawFeatureList.forEach { lineFeature ->
            completeRoute.addAll(lineFeature.toLineString().coordinates())
        }
        return completeRoute
    }

    private fun drawRoute(points: MutableList<Point>, featureID: String) {
        val drawLineSource = map.style?.getSourceAs<GeoJsonSource>(DRAW_LINE_LAYER_SOURCE_ID)

        // draw the current route line and all route features that were already created
        val activeLines = routeDrawFeatureList.toMutableList()
        val lineFeature = points.toLineString().toFeature()
        // set a unique id for the currently drawn route so it keeps it's identifier after it is drawn
        lineFeature.addStringProperty(ID_PROPERTY_KEY, featureID)
        activeLines.add(lineFeature)
        val lineCollection = FeatureCollection.fromFeatures(activeLines)

        drawLineSource?.setGeoJson(lineCollection)
    }

    private fun redrawRouteOverlay() {
        val drawLineSource = map.style?.getSourceAs<GeoJsonSource>(DRAW_LINE_LAYER_SOURCE_ID)
        val lineCollection = FeatureCollection.fromFeatures(routeDrawFeatureList)
        drawLineSource?.setGeoJson(lineCollection)
    }

    private fun saveRoute(points: MutableList<Point>, featureID: String) {
        val lineFeature = points.toLineString().toFeature()
        // set the same identifier that was used to draw the route so it can be identified later
        lineFeature.addStringProperty(ID_PROPERTY_KEY, featureID)
        routeDrawFeatureList.add(lineFeature)

        // reset the current route points so they won't be drawn twice
        currentRoutePoints.clear()
    }

    fun addLineToMap(lineCoords: List<LatLng>, @ColorRes color: Int? = null): Line? {
        val resolvedColor = color?.let { ContextCompat.getColor(context, it) } ?: DEFAULT_LINE_COLOR
        return createLineFromCoordinates(lineCoords, resolvedColor)
    }

    @JvmName("addLineToMapPoints")
    fun addLineToMap(linePoints: List<Point>, @ColorRes color: Int? = null): Line? {
        val resolvedColor = color?.let { ContextCompat.getColor(context, it) } ?: DEFAULT_LINE_COLOR
        val lineCoords = linePoints.map { it.toLatLng() }
        return createLineFromCoordinates(lineCoords, resolvedColor)
    }

    fun addLineToMap(line: LineString, @ColorRes color: Int? = null): Line? {
        val resolvedColor = color?.let { ContextCompat.getColor(context, it) } ?: DEFAULT_LINE_COLOR
        return createLineFromGeometry(line, resolvedColor)
    }

    fun removeLineFromMap(line: Line) {
        lineManager.delete(line)
    }

    fun removeLineStringFromMap(feature: Feature) {
        routeDrawFeatureList.forEach {
            if (it.getStringProperty(ID_PROPERTY_KEY) == feature.getStringProperty(ID_PROPERTY_KEY)) {
                routeDrawFeatureList.remove(it)
                redrawRouteOverlay() // draw again so the deleted route disappears immediately
                return // return immediately to prevent a concurrentModificationException!
            }
        }
    }

    fun checkIfLayerExists(layerID: String): Boolean {
        return map.style?.getLayer(layerID) != null
    }

    fun removeCustomLineLayerFromMap(layerID: String): Boolean {
        // remove layer if it exists
        map.style?.removeLayer(layerID)
        // remove the corresponding source
        return customLineLayers[layerID]?.let { map.style?.removeSource(it) } ?: false
    }

    fun clearAllLines() {
        lineManager.deleteAll()
    }

    fun clearDrawLayer() {
        // Reset lists
        routeDrawFeatureList.clear()
        // routeDrawLineList.clear()
        currentRoutePoints.clear()

        // Add empty Feature array to the sources to clear the source
        map.style?.getSourceAs<GeoJsonSource>(DRAW_LINE_LAYER_SOURCE_ID)
            ?.setGeoJson(FeatureCollection.fromFeatures(arrayOf<Feature>()))
    }

    private fun createLineFromCoordinates(
        lineCoords: List<LatLng>,
        color: Int = DEFAULT_LINE_COLOR
    ): Line? {
        return lineManager.create(
            defaultLineOptions
                .withLineColor(ColorUtils.colorToRgbaString(color))
                .withLatLngs(lineCoords)
        )
    }

    private fun createLineFromGeometry(
        lineGeometry: LineString,
        color: Int = DEFAULT_LINE_COLOR
    ): Line? {
        return lineManager.create(
            defaultLineOptions
                .withLineColor(ColorUtils.colorToRgbaString(color))
                .withGeometry(lineGeometry)
        )
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        enableMapMovement()
        lineManager.onDestroy() // cleanup to prevent leaks
    }

    companion object {

        private const val DEFAULT_LINE_WIDTH = 5.0f
        private const val DEFAULT_LINE_OPACITY = 1.0f
        private val DEFAULT_LINE_COLOR = Color.parseColor("#00acff")

        private const val MAPBOX_ICON_LAYER = "waterway-label"

        private const val ID_PROPERTY_KEY = "ID_PROPERTY"

        const val DRAW_LINE_LAYER_SOURCE_ID = "DRAW_LINE_LAYER_SOURCE_ID"
        const val DRAW_LINE_LAYER_ID = "DRAW_LINE_LAYER_ID"

        private val customLineLayers = mutableMapOf(
            DRAW_LINE_LAYER_ID to DRAW_LINE_LAYER_SOURCE_ID
        )
    }
}
