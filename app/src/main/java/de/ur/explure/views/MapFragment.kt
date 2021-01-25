package de.ur.explure.views

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.widget.ListPopupWindow
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import de.ur.explure.R
import de.ur.explure.databinding.FragmentMapBinding
import de.ur.explure.utils.EventObserver
import de.ur.explure.utils.SharedPreferencesManager
import de.ur.explure.utils.isGPSEnabled
import de.ur.explure.utils.measureContentWidth
import de.ur.explure.utils.viewLifecycle
import de.ur.explure.viewmodel.MapViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import java.lang.ref.WeakReference

class MapFragment : Fragment(), OnMapReadyCallback, MapboxMap.OnMapClickListener,
    PermissionsListener {

    private var binding: FragmentMapBinding by viewLifecycle()

    private lateinit var preferencesManager: SharedPreferencesManager

    private val mapViewModel: MapViewModel by viewModel()
    private var mapView: MapView? = null
    private lateinit var map: MapboxMap

    // for location tracking
    private var locationEngine: LocationEngine? = null
    private var callback: LocationListeningCallback? = null

    // permission handling
    private var permissionsManager: PermissionsManager = PermissionsManager(this)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMapBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        preferencesManager = SharedPreferencesManager(requireActivity())

        // disable the buttons until the map has finished loading
        binding.ownLocationButton.isEnabled = false
        binding.changeStyleButton.isEnabled = false

        setupViewModelObservers()

        // TODO to prevent recreation of the mapView on ui changes (like rotating the device) the
        // manifest was modified to allow the activity to handle config changes itself
        //  -> This should instead be fixed by saving the necessary state in onSavedInstanceState!
        if (savedInstanceState === null) {
            // init mapbox map
            mapView = binding.mapView
            mapView?.onCreate(savedInstanceState)
            mapView?.getMapAsync(this)
        }
    }

    private fun setupViewModelObservers() {
        mapViewModel.mapReady.observe(viewLifecycleOwner, EventObserver {
            // this only gets called if the event has never been handled thanks to the EventObserver
            Timber.d("Map has finished loading and can be used now!")

            binding.ownLocationButton.isEnabled = true
            binding.changeStyleButton.isEnabled = true

            binding.changeStyleButton.setOnClickListener {
                showMapStyleOptions(layoutResource = R.layout.popup_list_item)
            }

            binding.ownLocationButton.setOnClickListener {
                mapViewModel.getMapStyle()?.let { style -> enableLocationComponent(style) }
            }
        })
    }

    /**
     * Show a popup window to let the user choose a map style.
     */
    private fun showMapStyleOptions(
        layoutResource: Int = android.R.layout.simple_list_item_1,
        horizontalOffsetValue: Int = 0,
        verticalOffsetValue: Int = 0
    ) {
        val context = activity ?: return

        val styleList = MapViewModel.All_MAP_STYLES.keys.toList()
        val styleAdapter: ArrayAdapter<String> = ArrayAdapter<String>(
            context,
            layoutResource,
            styleList
        )
        val listPopup = ListPopupWindow(context)

        listPopup.apply {
            setAdapter(styleAdapter)
            width = measureContentWidth(context, styleAdapter)
            height = ListPopupWindow.WRAP_CONTENT
            isModal = true
            anchorView = binding.changeStyleButton
            horizontalOffset = horizontalOffsetValue
            verticalOffset = verticalOffsetValue
        }

        val drawable = ContextCompat.getDrawable(context, R.drawable.popup_list_background)
        listPopup.setBackgroundDrawable(drawable)

        listPopup.setOnItemClickListener { parent, _, position, _ ->
            val selectedItem = parent.getItemAtPosition(position)
            val selectedMapStyle = MapViewModel.All_MAP_STYLES[selectedItem] ?: return@setOnItemClickListener
            map.setStyle(selectedMapStyle) { mapStyle ->
                mapViewModel.setMapStyle(mapStyle)
                preferencesManager.setCurrentMapStyle(selectedMapStyle)
            }

            listPopup.dismiss()
        }
        listPopup.show()
    }

    /**
     * * Map code
     */

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.map = mapboxMap

        mapboxMap.addOnMapClickListener(this)

        // TODO setup a separate mapbox map object/singleton to handle and encapsulate map stuff?
        val style = preferencesManager.getCurrentMapStyle()
        mapboxMap.setStyle(style) {
            // Map is set up and the style has loaded.
            mapViewModel.setMapStyle(it)
            mapViewModel.setMapReadyStatus(true)

            /*
            // print out all layers of current style
            for (singleLayer in mapStyle.layers) {
                Timber.d("onMapReady: layer id = %s", singleLayer.id)
            }*/
        }
    }

    override fun onMapClick(point: LatLng): Boolean {
        Timber.d("Clicked on map point with coordinates: $point")
        // return true if this click should be consumed and not passed to other listeners registered afterwards
        return true
    }

    /**
     * * Location Tracking Code
     */

    @SuppressLint("MissingPermission")
    private fun enableLocationComponent(loadedMapStyle: Style) {
        val activity = activity ?: return

        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(activity)) {
            // Check if GPS is enabled in the device settings
            if (!isGPSEnabled(activity)) {
                Toast.makeText(
                    activity,
                    "GPS doesn't seem to be enabled! Please enable it in the device settings to continue!",
                    Toast.LENGTH_LONG
                ).show()
                return
            }

            val customLocationComponentOptions = LocationComponentOptions.builder(activity)
                // overwrite custom gestures detection to adjust the camera's focal point and increase
                // thresholds without breaking tracking
                .trackingGesturesManagement(true)
                // show a pulsing circle around the user position
                .pulseEnabled(true)
                .pulseFadeEnabled(true)
                // disable animations to decrease battery and cpu usage
                .compassAnimationEnabled(false)
                .accuracyAnimationEnabled(false)
                .build()

            val locationComponentActivationOptions =
                LocationComponentActivationOptions.builder(activity, loadedMapStyle)
                    .locationComponentOptions(customLocationComponentOptions)
                    // use our custom location engine below instead of the built-in location engine
                    // to track user location updates
                    .useDefaultLocationEngine(false)
                    .build()

            map.locationComponent.apply {
                // Activate the LocationComponent with options
                activateLocationComponent(locationComponentActivationOptions)
                // Enable to make the LocationComponent visible
                isLocationComponentEnabled = true
                // Set the LocationComponent's camera mode
                cameraMode = CameraMode.TRACKING
                // Set the LocationComponent's render mode
                renderMode = RenderMode.COMPASS
            }

            // init custom location engine
            initLocationEngine()
        } else {
            permissionsManager.requestLocationPermissions(activity)
        }
    }

    @SuppressLint("MissingPermission")
    private fun initLocationEngine() {
        val activityCtx = activity ?: return

        locationEngine = LocationEngineProvider.getBestLocationEngine(activityCtx)
        callback = LocationListeningCallback(this)

        val request = LocationEngineRequest.Builder(DEFAULT_INTERVAL_IN_MILLISECONDS)
            .setPriority(LocationEngineRequest.PRIORITY_BALANCED_POWER_ACCURACY)
            .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME)
            .build()

        val locationCallback = callback ?: return
        locationEngine?.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        locationEngine?.getLastLocation(locationCallback)
    }

    /**
     * * Permission Code
     */

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        Toast.makeText(
            requireActivity(),
            "Der Standortzugriff wird benötigt, um deine aktuelle Position auf der Karte anzuzeigen!",
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            // try to find the device location and enable location tracking
            map.style?.let { enableLocationComponent(it) }
        } else {
            Toast.makeText(
                requireActivity(),
                "Ohne die Berechtigung kann dein aktueller Standort nicht angezeigt werden!",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * * Lifecycle Hooks
     * To handle Mapbox state correctly, the corresponding mapView hooks need to be called here.
     */

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onStop() {
        super.onStop()
        // remove location updates to prevent leaks
        callback?.let { locationEngine?.removeLocationUpdates(it) }
        mapView?.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // TODO oder erst hier statt schon in onStop?
        // callback?.let { locationEngine?.removeLocationUpdates(it) }
        callback = null
        locationEngine = null
        mapView?.onDestroy()
    }

    companion object {
        // Note: this layer is not in all map styles available (e.g. the satellite style)!
        private const val FIRST_SYMBOL_LAYER_ID = "waterway-label"

        private const val DEFAULT_INTERVAL_IN_MILLISECONDS = 1000L
        private const val DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 5
    }

    /**
     * This class serves as a "callback" and is needed because a LocationEngine memory leak is
     * possible if the activity/fragment directly implements the LocationEngineCallback<LocationEngineResult>.
     * The WeakReference setup avoids the leak. See https://docs.mapbox.com/android/core/guides/
     */
    private class LocationListeningCallback constructor(fragment: MapFragment) :
        LocationEngineCallback<LocationEngineResult> {

        private val fragmentWeakReference: WeakReference<MapFragment> = WeakReference(fragment)

        /**
         * The LocationEngineCallback interface's method which fires when the device's location has changed.
         */
        override fun onSuccess(result: LocationEngineResult) {
            val fragment: MapFragment? = fragmentWeakReference.get()
            if (fragment != null) {
                val location = result.lastLocation ?: return

                // TODO use a separate map object class to access here to avoid the need of a WeakReference!
                // Pass the new location to the Maps SDK's LocationComponent
                fragment.map.locationComponent.forceLocationUpdate(location)
            }
        }

        /**
         * The LocationEngineCallback interface's method which fires when the device's location can not be captured
         *
         * @param exception the exception message
         */
        override fun onFailure(exception: Exception) {
            val fragment: MapFragment? = fragmentWeakReference.get()
            if (fragment != null) {
                Toast.makeText(
                    fragment.activity,
                    exception.localizedMessage,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
