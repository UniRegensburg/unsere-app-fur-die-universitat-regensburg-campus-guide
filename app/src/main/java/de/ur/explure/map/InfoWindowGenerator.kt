package de.ur.explure.map

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.mapbox.geojson.FeatureCollection
import de.ur.explure.databinding.WaypointInfoWindowBinding
import de.ur.explure.views.EditRouteFragment.Companion.PROPERTY_ID
import de.ur.explure.views.EditRouteFragment.Companion.PROPERTY_TITLE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.collections.HashMap

/**
 * Utility - Class used to generate the info windows for the markers on the map as the new annotation
 * plugin from the Mapbox Maps SDK is not yet able to show info windows itself:
 * * Track issue at https://github.com/mapbox/mapbox-plugins-android/issues/649
 *
 * Parts of the code in this class have been taken and adjusted from
 * https://github.com/mapbox/mapbox-android-demo/blob/286f33d848c9fea48de908b144682081961b986b/MapboxAndroidDemo/src/main/java/com/mapbox/mapboxandroiddemo/labs/SymbolLayerMapillaryActivity.java
 */
class InfoWindowGenerator(context: Activity) : DefaultLifecycleObserver {

    private var _binding: WaypointInfoWindowBinding? = null
    private val binding get() = _binding!!

    private var viewMap: HashMap<String, View> = HashMap()

    private var infoWindowListener: InfoWindowListener? = null

    /**
     * This is the job for all coroutines started by this class.
     * Cancelling this job will cancel all coroutines started by this class.
     */
    private var completableJob = Job()

    /**
     * A [CoroutineScope] keeps track of all coroutines started by this class.
     *
     * Because we pass it [completableJob], any coroutine started in this [uiScope] can be cancelled
     * by calling `completableJob.cancel()`
     *
     * By default, all coroutines started in this scope will launch in [Dispatchers.Main] which is
     * the main thread on Android because we will update the UI after performing the background work!
     */
    private val uiScope = CoroutineScope(completableJob + Dispatchers.Main)

    init {
        _binding = WaypointInfoWindowBinding.inflate(LayoutInflater.from(context))
    }

    /**
     * This is an asyncTask re-written with kotlin coroutines as AsyncTasks are deprecated now!
     */
    fun generateCallouts(featureCollection: FeatureCollection) = uiScope.launch {
        // onPreExecute()
        val result = doInBackground(featureCollection)
        onPostExecute(result)
    }

    /**
     * Uses a background thread to generate Bitmaps from Views to be used as iconImage in a SymbolLayer
     * since we are not going to be adding them to the view hierarchy.
     */
    private suspend fun doInBackground(featureCollection: FeatureCollection): HashMap<String, Bitmap>? =
        withContext(Dispatchers.Default) {
            val imagesMap: HashMap<String, Bitmap> = HashMap()

            val featureList = featureCollection.features() ?: return@withContext null
            for (feature in featureList) {
                val bubbleLayout = binding.root

                // get the id and the title from the feature's json properties
                val id = feature.getStringProperty(PROPERTY_ID)
                val name = feature.getStringProperty(PROPERTY_TITLE)
                binding.waypointTitleIW.text = name

                val measureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                bubbleLayout.measure(measureSpec, measureSpec)
                val measuredWidth = bubbleLayout.measuredWidth.toFloat()
                bubbleLayout.arrowPosition = measuredWidth / 2 // position the arrow at the bottom center

                val bitmap = SymbolGenerator.generate(bubbleLayout)
                imagesMap[id] = bitmap
                viewMap[id] = bubbleLayout // save the inflated view so it can be accessed later!
            }
            imagesMap
        }

    /**
     * This runs on the Main Thread again!
     */
    private fun onPostExecute(bitmapHashMap: HashMap<String, Bitmap>?) {
        if (bitmapHashMap != null) {
            infoWindowListener?.onViewsGenerated(bitmapHashMap, viewMap)
        }
    }

    fun setInfoWindowListener(listener: InfoWindowListener) {
        infoWindowListener = listener
    }

    override fun onDestroy(owner: LifecycleOwner) {
        // At this point, we want to cancel all coroutines; otherwise we end up with processes that
        // have nowhere to return to using memory and resources.
        completableJob.cancel()

        _binding = null // cleanup viewBinding
        viewMap.clear()
        infoWindowListener = null
        super.onDestroy(owner)
    }

    /**
     * Utility class to generate Bitmaps for Symbol.
     *
     * Taken from https://github.com/mapbox/mapbox-android-demo/blob/286f33d848c9fea48de908b144682081961b986b/MapboxAndroidDemo/src/main/java/com/mapbox/mapboxandroiddemo/labs/SymbolLayerMapillaryActivity.java
     */
    private object SymbolGenerator {
        /**
         * Generate a Bitmap from an Android SDK View.
         *
         * @param view the View to be drawn to a Bitmap
         * @return the generated bitmap
         */
        fun generate(view: View): Bitmap {
            val measureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            view.measure(measureSpec, measureSpec)
            val measuredWidth = view.measuredWidth
            val measuredHeight = view.measuredHeight
            view.layout(0, 0, measuredWidth, measuredHeight)
            val bitmap = Bitmap.createBitmap(measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.TRANSPARENT)
            val canvas = Canvas(bitmap)
            view.draw(canvas)
            return bitmap
        }
    }

    interface InfoWindowListener {
        fun onViewsGenerated(
            bitmapHashMap: HashMap<String, Bitmap>,
            viewMap: HashMap<String, View>
        )
    }
}
