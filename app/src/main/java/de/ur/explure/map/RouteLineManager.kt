package de.ur.explure.map

import android.annotation.SuppressLint
import android.app.Application
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
import com.mapbox.geojson.utils.PolylineUtils
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
import de.ur.explure.R
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

    private val defaultDrawColor = ContextCompat.getColor(context, R.color.colorRouteDraw)
    private val mapMatchedRouteColor = ContextCompat.getColor(context, R.color.themeColorDark)

    private var onRouteDrawListener: OnRouteDrawListener? = null

    private val lineManager: LineManager =
        LineManager(mapView, map, mapStyle, MAPBOX_FIRST_LABEL_LAYER).apply {
            lineCap = LINE_CAP_ROUND
        }
    private val defaultLineOptions = LineOptions()
        .withLineWidth(DEFAULT_LINE_WIDTH)
        .withLineJoin(LINE_JOIN_ROUND)
        .withLineOpacity(DEFAULT_LINE_OPACITY)

    private var routeDrawFeatureList: MutableList<Feature> = mutableListOf()
    private var currentRoutePoints: MutableList<Point> = mutableListOf()

    @SuppressLint("ClickableViewAccessibility")
    private val mapDrawListener = View.OnTouchListener { _, motionEvent ->
        // get the touch position on the screen
        val latLngTouchCoordinate: LatLng =
            map.projection.fromScreenLocation(PointF(motionEvent.x, motionEvent.y))
        val screenTouchPoint =
            Point.fromLngLat(latLngTouchCoordinate.longitude, latLngTouchCoordinate.latitude)

        currentRoutePoints.add(screenTouchPoint)
        // make a copy as otherwise the references to the old points will be kept and cleared below!
        val points = currentRoutePoints.toMutableList()

        // create a unique id for the currently drawn route and draw it while the user touches the screen
        val routeId = UUID.randomUUID().toString()
        drawRoute(points, routeId)

        // Take certain actions when the drawing is done
        if (motionEvent.action == MotionEvent.ACTION_UP) {
            saveRoute(points, routeId)
        }
        true
    }

    fun setRouteDrawListener(listener: OnRouteDrawListener) {
        onRouteDrawListener = listener
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

    // use map sources and layers for free drawing instead of the lineManager as this increases the
    // performance A LOT!
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
                lineColor(defaultDrawColor)
            ), MAPBOX_FIRST_LABEL_LAYER
        )
    }

    fun getCompleteRoute(): MutableList<Point> {
        val completeRoute = mutableListOf<Point>()
        routeDrawFeatureList.forEach { lineFeature ->
            val coords = lineFeature.toLineString().coordinates()

            // Simplify the linestring with the Ramer-Douglas-Peucker algorithm to decrease the number
            // of route points which improves the performance and makes map matching feasible.
            // A higher tolerance (e.g. 0.1) will take only very few points of the original list
            // which might result in fuzzier or even completely wrong routes, a very low tolerance
            // (e.g. 0.000001) will try to take as many points as possible which results in very
            //  accurate routes but decreases performance and will exceed the map matching api rate limit.
            val simplifiedPoints = PolylineUtils.simplify(coords, DEFAULT_ROUTE_TOLERANCE, true)
            completeRoute.addAll(simplifiedPoints)
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

    fun redrawActiveRoutes(currentRoutes: MutableList<Feature>) {
        routeDrawFeatureList.addAll(currentRoutes)
        redrawRouteOverlay()
    }

    private fun saveRoute(points: MutableList<Point>, featureID: String) {
        val lineFeature = points.toLineString().toFeature()
        // set the same identifier that was used to draw the route so it can be identified later
        lineFeature.addStringProperty(ID_PROPERTY_KEY, featureID)
        routeDrawFeatureList.add(lineFeature)
        onRouteDrawListener?.onNewRouteDrawn(lineFeature)

        // reset the current route points so they won't be drawn twice
        currentRoutePoints.clear()
    }

    fun addLineToMap(linePoints: List<Point>, @ColorRes color: Int? = null): Line? {
        val resolvedColor = color?.let { ContextCompat.getColor(context, it) } ?: mapMatchedRouteColor
        val lineCoords = linePoints.map { it.toLatLng() }
        return createLineFromCoordinates(lineCoords, resolvedColor)
    }

    fun addLineToMap(line: LineString, @ColorRes color: Int? = null): Line? {
        val resolvedColor = color?.let { ContextCompat.getColor(context, it) } ?: mapMatchedRouteColor
        return createLineFromGeometry(line, resolvedColor)
    }

    fun removeDrawnLineStringFromMap(feature: Feature) {
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
        // Reset lists
        routeDrawFeatureList.clear()
        currentRoutePoints.clear()

        // clear linemanager
        lineManager.deleteAll()

        // Add an empty Feature array to the draw source to clear it
        map.style?.getSourceAs<GeoJsonSource>(DRAW_LINE_LAYER_SOURCE_ID)
            ?.setGeoJson(FeatureCollection.fromFeatures(arrayOf<Feature>()))
    }

    fun removeMapMatching() {
        // ! the lineManager is only used to create the map matching so it's safe to simply delete all
        lineManager.deleteAll()
    }

    private fun createLineFromCoordinates(
        lineCoords: List<LatLng>,
        color: Int = defaultDrawColor
    ): Line? {
        return lineManager.create(
            defaultLineOptions
                .withLineColor(ColorUtils.colorToRgbaString(color))
                .withLatLngs(lineCoords)
        )
    }

    private fun createLineFromGeometry(
        lineGeometry: LineString,
        color: Int = defaultDrawColor
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

        onRouteDrawListener = null
    }

    companion object {
        private const val DEFAULT_LINE_WIDTH = 5.0f
        private const val DEFAULT_LINE_OPACITY = 1.0f

        // The tolerance value used in the douglas - peucker simplification algorithm,
        // see https://en.wikipedia.org/wiki/Ramer%E2%80%93Douglas%E2%80%93Peucker_algorithm
        private const val DEFAULT_ROUTE_TOLERANCE = 0.001

        const val MAPBOX_FIRST_LABEL_LAYER = "road-label"
        const val DRAW_LINE_LAYER_SOURCE_ID = "DRAW_LINE_LAYER_SOURCE_ID"
        const val DRAW_LINE_LAYER_ID = "DRAW_LINE_LAYER_ID"

        const val ID_PROPERTY_KEY = "ID_PROPERTY"

        private val customLineLayers = mutableMapOf(
            DRAW_LINE_LAYER_ID to DRAW_LINE_LAYER_SOURCE_ID
        )
    }

    interface OnRouteDrawListener {
        fun onNewRouteDrawn(lineFeature: Feature)
    }
}
