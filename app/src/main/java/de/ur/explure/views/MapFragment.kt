package de.ur.explure.views

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
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
import de.ur.explure.databinding.FragmentMapBinding
import de.ur.explure.utils.EventObserver
import de.ur.explure.utils.viewLifecycle
import de.ur.explure.viewmodel.MapViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class MapFragment : Fragment(), OnMapReadyCallback, MapboxMap.OnMapClickListener,
    PermissionsListener {

    private var binding: FragmentMapBinding by viewLifecycle()

    private val mapViewModel: MapViewModel by viewModel()
    private var mapView: MapView? = null
    private lateinit var map: MapboxMap

    private var permissionsManager: PermissionsManager = PermissionsManager(this)

    private var styleIndex = 0
    private var currentMapStyle: Style? = null

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

        // disable the buttons until the map has finished loading
        binding.ownLocationButton.isEnabled = false
        binding.changeStyleButton.isEnabled = false

        setupViewModelObservers()

        // init mapbox map
        mapView = binding.mapView
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)
    }

    private fun setupViewModelObservers() {
        mapViewModel.mapReady.observe(viewLifecycleOwner, EventObserver {
            // this only gets called if the event has never been handled thanks to the EventObserver
            Toast.makeText(
                requireContext(),
                "Map has finished loading and can be used now!",
                Toast.LENGTH_SHORT
            ).show()

            binding.ownLocationButton.isEnabled = true
            binding.changeStyleButton.isEnabled = true

            binding.changeStyleButton.setOnClickListener {
                styleIndex++
                if (styleIndex == All_STYLES.size) {
                    // reset to the first style
                    styleIndex = 0
                }
                map.setStyle(All_STYLES[styleIndex]) { mapStyle ->
                    currentMapStyle = mapStyle
                }
            }

            binding.ownLocationButton.setOnClickListener {
                currentMapStyle?.let { style -> enableLocationComponent(style) }
            }
        })
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.map = mapboxMap

        mapboxMap.addOnMapClickListener(this)

        // TODO setup a separate mapbox map object/singleton to handle and encapsulate map stuff?
        mapboxMap.setStyle(Style.MAPBOX_STREETS) {
            // Map is set up and the style has loaded.
            currentMapStyle = it
            mapViewModel.setMapReadyStatus(true)

            // print out all layers of current style
            // for (singleLayer in mapStyle.layers) {
            //     Timber.d("onMapReady: layer id = %s", singleLayer.id)
            // }
        }
    }

    override fun onMapClick(point: LatLng): Boolean {
        Timber.d("Clicked on map point with coordinates: $point")
        // return true if this click should be consumed and not passed to other listeners registered afterwards
        return true
    }

    /**
     * Permission Code
     * TODO extract to other file!
     */

    @SuppressLint("MissingPermission")
    private fun enableLocationComponent(loadedMapStyle: Style) {
        val activity = activity ?: return

        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(activity)) {

            // Create and customize the LocationComponent's options
            val customLocationComponentOptions = LocationComponentOptions.builder(activity)
                .layerBelow(FIRST_SYMBOL_LAYER_ID)
                .trackingGesturesManagement(true)
                .pulseEnabled(true)
                .pulseFadeEnabled(true)
                // .accuracyColor(ContextCompat.getColor(activity, R.color.colorAccent))
                .build()

            val locationComponentActivationOptions =
                LocationComponentActivationOptions.builder(activity, loadedMapStyle)
                    .locationComponentOptions(customLocationComponentOptions)
                    .build()

            // Get an instance of the LocationComponent and then adjust its settings
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
        } else {
            permissionsManager = PermissionsManager(this)
            permissionsManager.requestLocationPermissions(activity)
        }
    }

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
     * Mapbox Lifecycle Hooks
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
        mapView?.onDestroy()
    }

    companion object {
        private val All_STYLES = arrayOf(
            Style.MAPBOX_STREETS,
            Style.OUTDOORS,
            Style.LIGHT,
            Style.DARK,
            Style.SATELLITE,
            Style.SATELLITE_STREETS,
            Style.TRAFFIC_DAY,
            Style.TRAFFIC_NIGHT
        )

        private const val FIRST_SYMBOL_LAYER_ID = "waterway-label"
    }
}
