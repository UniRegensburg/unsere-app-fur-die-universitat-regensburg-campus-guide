package de.ur.explure.views

import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.widget.ListPopupWindow
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.crazylegend.viewbinding.viewBinding
import com.google.android.material.snackbar.Snackbar
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import de.ur.explure.R
import de.ur.explure.databinding.FragmentMapBinding
import de.ur.explure.map.LocationManager
import de.ur.explure.map.MarkerManager
import de.ur.explure.utils.EventObserver
import de.ur.explure.utils.Highlight
import de.ur.explure.utils.SharedPreferencesManager
import de.ur.explure.utils.TutorialBuilder
import de.ur.explure.utils.isGPSEnabled
import de.ur.explure.utils.measureContentWidth
import de.ur.explure.utils.showSnackbar
import de.ur.explure.viewmodel.MapViewModel
import de.ur.explure.viewmodel.MapViewModel.Companion.All_MAP_STYLES
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.androidx.viewmodel.scope.emptyState
import org.koin.core.parameter.parametersOf
import timber.log.Timber

@Suppress("TooManyFunctions")
class MapFragment : Fragment(R.layout.fragment_map), OnMapReadyCallback, PermissionsListener {

    private val binding by viewBinding(FragmentMapBinding::bind)

    // Setting the state as emptyState as a workaround for this issue: https://github.com/InsertKoinIO/koin/issues/963
    private val mapViewModel: MapViewModel by viewModel(state = emptyState())

    // SharedPrefs
    private val preferencesManager: SharedPreferencesManager by inject()

    // map
    private var mapView: MapView? = null
    private lateinit var map: MapboxMap
    private lateinit var markerManager: MarkerManager

    // location tracking
    private lateinit var locationManager: LocationManager

    // permission handling
    private var permissionsManager: PermissionsManager = PermissionsManager(this)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Timber.d("in MapFragment onViewCreated")

        // disable the buttons until the map has finished loading
        binding.ownLocationButton.isEnabled = false
        binding.changeStyleButton.isEnabled = false

        // init locationManager and sync with fragment lifecycle
        locationManager = get { parametersOf(this::onNewLocationReceived) }
        viewLifecycleOwner.lifecycle.addObserver(locationManager)

        setupViewModelObservers()

        // init mapbox map
        mapView = binding.mapView
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)
    }

    private fun setupViewModelObservers() {
        mapViewModel.mapReady.observe(viewLifecycleOwner, EventObserver {
            // this should only get called if the event has never been handled before because of the EventObserver
            Timber.d("Map has finished loading and can be used now!")

            // enable the buttons now that the map is ready
            binding.ownLocationButton.isEnabled = true
            binding.changeStyleButton.isEnabled = true

            binding.changeStyleButton.setOnClickListener {
                showMapStyleOptions(layoutResource = R.layout.popup_list_item)
            }

            binding.ownLocationButton.setOnClickListener {
                mapViewModel.getCurrentMapStyle()?.let { style -> startLocationTracking(style) }
            }

            // if location tracking was enabled before, start it again without forcing the user to
            // press the button again
            if (mapViewModel.getLocationTrackingEnabled() == true) {
                mapViewModel.getCurrentMapStyle()?.let { startLocationTracking(it) }
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

        val styleList = All_MAP_STYLES.keys.toList()
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
            val selectedItem =
                parent.getItemAtPosition(position) as? String ?: return@setOnItemClickListener
            val selectedMapStyle = All_MAP_STYLES[selectedItem]
            setMapStyle(selectedMapStyle)

            listPopup.dismiss()
        }
        listPopup.show()
    }

    /**
     * * Map code
     */

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.map = mapboxMap

        if (preferencesManager.isFirstRun()) {
            // highlight interesting spots the first time the fragment is launched on this device
            TutorialBuilder.showTutorialFor(
                requireActivity(),
                Highlight(
                    binding.changeStyleButton,
                    title = getString(R.string.map_style_button_title),
                    description = getString(R.string.map_style_button_description)
                ), Highlight(
                    binding.ownLocationButton,
                    title = getString(R.string.location_tracking_button_title),
                    description = getString(R.string.location_tracking_button_description)
                )
            )

            // mark first launch as completed so this tutorial won't be shown on further app starts
            // or configuration changes
            preferencesManager.completedFirstRun()
        }

        setupMapListeners()

        // TODO setup a separate mapbox map object/singleton to handle and encapsulate map stuff?
        val style = preferencesManager.getCurrentMapStyle()
        setMapStyle(style)

        // init the camera at the saved cameraPosition if it is not null
        mapViewModel.getLastKnownCameraPosition()?.let {
            map.cameraPosition = it
        }
    }

    private fun setMapStyle(styleUrl: String?) {
        styleUrl ?: return

        map.setStyle(styleUrl) { mapStyle ->
            // Map is set up and the style has loaded.
            mapViewModel.setCurrentMapStyle(mapStyle)

            mapViewModel.setMapReadyStatus(true)

            // save the current style in the shared preferences
            preferencesManager.setCurrentMapStyle(styleUrl)

            setupMarkerManager(mapStyle)
            recreateMarkers()
        }
    }

    private fun setupMarkerManager(mapStyle: Style) {
        markerManager = get { parametersOf(mapView, map, mapStyle) }
        // let the marker manager observe the fragment lifecycle so it can clean itself up on destroy
        viewLifecycleOwner.lifecycle.addObserver(markerManager)
    }

    private fun recreateMarkers() {
        // recreate all markers that were on the map before the config change or process death
        val allActiveMarkers = mapViewModel.getAllActiveMarkers()
        markerManager.addMarkers(allActiveMarkers)
    }

    /**
     * * Map Listeners
     */

    private fun setupMapListeners() {
        map.addOnCameraIdleListener(this::onCameraMoved)
        map.addOnMapClickListener(this::onMapClicked)
        map.addOnMapLongClickListener(this::onMapLongClicked)
    }

    private fun removeMapListeners() {
        if (this::map.isInitialized) {
            map.removeOnCameraIdleListener(this::onCameraMoved)
            map.removeOnMapClickListener(this::onMapClicked)
            map.removeOnMapLongClickListener(this::onMapLongClicked)
        }
    }

    private fun onCameraMoved() {
        mapViewModel.setCurrentCameraPosition(map.cameraPosition)
    }

    private fun onMapClicked(point: LatLng): Boolean {
        Timber.d("Clicked on map point with coordinates: $point")
        // If true this click is consumed and not passed to other listeners registered afterwards!
        // ! This method has to return false as otherwise symbol clicks won't be fired.
        return false
    }

    private fun onMapLongClicked(point: LatLng): Boolean {
        // add a symbol to the long-clicked point
        val marker = markerManager.addMarker(point)

        if (marker != null) {
            // save the marker in the viewmodel
            mapViewModel.saveMarker(marker)
        }
        return false
    }

    /**
     * * Location Tracking Code
     */

    private fun startLocationTracking(loadedMapStyle: Style) {
        val activity = activity ?: return

        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(activity)) {
            // Check if GPS is enabled in the device settings
            if (!isGPSEnabled(activity)) {
                showSnackbar(
                    activity,
                    R.string.gps_not_activated,
                    R.id.mapContainer,
                    Snackbar.LENGTH_LONG,
                    colorRes = R.color.color_warning
                )
                return
            }

            // enable location tracking with custom loaction engine
            if (::locationManager.isInitialized) {
                mapViewModel.setLocationTrackingStatus(isEnabled = true)
                locationManager.activateLocationComponent(
                    map.locationComponent, loadedMapStyle, useDefaultEngine = false
                )
            }
        } else {
            permissionsManager.requestLocationPermissions(activity)
        }
    }

    private fun onNewLocationReceived(location: Location) {
        // Pass the new location to the Maps SDK's LocationComponent
        map.locationComponent.forceLocationUpdate(location)
        // save the new location
        mapViewModel.setCurrentUserPosition(location)
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
        showSnackbar(
            requireActivity(),
            R.string.location_permission_explanation,
            R.id.mapContainer,
            Snackbar.LENGTH_LONG,
            colorRes = R.color.color_info
        )
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            // try to find the device location and enable location tracking
            map.style?.let { startLocationTracking(it) }
        } else {
            showSnackbar(
                requireActivity(),
                R.string.location_permission_not_given,
                R.id.mapContainer,
                Snackbar.LENGTH_LONG,
                colorRes = R.color.color_warning
            )
        }
    }

    /**
     * * Lifecycle Hooks
     *
     * To handle Mapbox state correctly, the corresponding mapView hooks need to be called here.
     */

    override fun onStart() {
        super.onStart()
        Timber.d("in MapFragment onStart")
        mapView?.onStart()
    }

    override fun onResume() {
        super.onResume()
        Timber.d("in MapFragment onResume")
        mapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        Timber.d("in MapFragment onPause")
        mapView?.onPause()
    }

    override fun onStop() {
        super.onStop()
        Timber.d("in MapFragment onStop")
        mapView?.onStop()
        mapViewModel.saveActiveMarkers()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        mapView?.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Timber.d("in MapFragment onDestroyView")

        removeMapListeners()
        mapView?.onDestroy()
    }

    companion object {
        // Note: this layer is not in all map styles available (e.g. the satellite style)!
        // private const val FIRST_SYMBOL_LAYER_ID = "waterway-label"
    }
}
