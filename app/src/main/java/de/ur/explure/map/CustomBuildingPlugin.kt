package de.ur.explure.map

import android.graphics.Color
import androidx.annotation.FloatRange
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.expressions.Expression
import com.mapbox.mapboxsdk.style.layers.FillExtrusionLayer
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.PropertyFactory

/**
 * Taken and adjusted from https://github.com/mapbox/mapbox-plugins-android/blob/main/plugin-building/src/main/java/com/mapbox/mapboxsdk/plugins/building/BuildingPlugin.java
 *
 * The original plugin doesn't allow style changes so a custom implementation had to be created.
 */
@Suppress("MagicNumber")
class CustomBuildingPlugin(private val style: Style, belowLayer: String? = null) {

    private var fillExtrusionLayer: FillExtrusionLayer? = null
    private var color = Color.LTGRAY
    private var opacity = 0.9f
    private var minZoomLevel = 15.0f // extrusions are not shown below this zoom level

    private var isVisible = false

    init {
        initLayer(belowLayer)
    }

    /**
     * Initialises and adds the fill extrusion layer used by this plugin.
     *
     * @param belowLayer optionally place the buildings layer below a provided layer id
     */
    private fun initLayer(belowLayer: String?) {
        // check if the layer id exists and remove it if it does
        if (style.getLayer(BUILDING_EXTRUSION_LAYER_ID) != null) {
            style.removeLayer(BUILDING_EXTRUSION_LAYER_ID)
        }

        fillExtrusionLayer = FillExtrusionLayer(BUILDING_EXTRUSION_LAYER_ID, EXTRUSION_SOURCE_ID)
        fillExtrusionLayer?.sourceLayer = EXTRUSION_SOURCE
        fillExtrusionLayer?.minZoom = minZoomLevel

        fillExtrusionLayer?.setProperties(
            PropertyFactory.visibility(if (isVisible) Property.VISIBLE else Property.NONE),
            PropertyFactory.fillExtrusionColor(color),
            PropertyFactory.fillExtrusionBase(Expression.get("min_height")),
            PropertyFactory.fillExtrusionHeight(
                // extrude based on the current zoom level (i.e. extrude more if we zoom in)
                Expression.interpolate(
                    Expression.exponential(1f), Expression.zoom(),
                    Expression.stop(15, Expression.literal(0)),
                    Expression.stop(17, Expression.get("height"))
                )
            ),
            // Use a runtime styling property to make the opacity
            // dependent on the camera zoom value
            PropertyFactory.fillExtrusionOpacity(
                Expression.interpolate(
                    Expression.linear(), Expression.zoom(),
                    Expression.stop(15, .1),
                    Expression.stop(17, opacity)
                )
            )
        )
        fillExtrusionLayer?.let { addLayer(it, belowLayer) }
    }

    /**
     * Add the extrusion layer to the map
     */
    private fun addLayer(fillExtrusionLayer: FillExtrusionLayer, belowLayer: String?) {
        if (belowLayer != null && belowLayer.isNotEmpty()) {
            style.addLayerBelow(fillExtrusionLayer, belowLayer)
        } else {
            style.addLayer(fillExtrusionLayer)
        }
    }

    /**
     * Toggles the visibility of the building layer.
     *
     * @param visible true for visible, false for none
     */
    fun setVisibility(visible: Boolean) {
        isVisible = visible
        if (!style.isFullyLoaded) {
            // We are in progress of loading a new style
            return
        }
        fillExtrusionLayer?.setProperties(PropertyFactory.visibility(if (visible) Property.VISIBLE else Property.NONE))
    }

    /**
     * Change the building opacity. Calls into changing the fill extrusion fill opacity.
     *
     * @param opacity `float` value between 0 (invisible) and 1 (solid)
     */
    fun setOpacity(@FloatRange(from = 0.0, to = 1.0) opacity: Float) {
        this.opacity = opacity
        if (!style.isFullyLoaded) {
            return
        }
        fillExtrusionLayer?.setProperties(PropertyFactory.fillExtrusionOpacity(opacity))
    }

    companion object {
        private const val BUILDING_EXTRUSION_LAYER_ID = "3d-buildings-extrusion-layer"
        private const val EXTRUSION_SOURCE_ID = "composite" // the tileset source
        private const val EXTRUSION_SOURCE = "building" // the tileset layer we want to extrude
    }
}
