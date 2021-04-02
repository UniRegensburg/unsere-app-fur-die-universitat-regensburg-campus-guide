package de.ur.explure.views

import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.crazylegend.viewbinding.viewBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mapbox.core.constants.Constants.PRECISION_6
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.snapshotter.MapSnapshotter
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import de.ur.explure.R
import de.ur.explure.databinding.FragmentEditRouteBinding
import de.ur.explure.map.MarkerManager
import de.ur.explure.map.RouteLineManager
import de.ur.explure.utils.SharedPreferencesManager
import de.ur.explure.utils.hasInternetConnection
import de.ur.explure.utils.measureTimeFor
import de.ur.explure.viewmodel.EditRouteViewModel
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber

class EditRouteFragment : Fragment(R.layout.fragment_edit_route), OnMapReadyCallback {

    private val binding by viewBinding(FragmentEditRouteBinding::bind)
    private val editRouteViewModel: EditRouteViewModel by viewModel()

    private val args: EditRouteFragmentArgs by navArgs()

    // SharedPrefs
    private val preferencesManager: SharedPreferencesManager by inject()

    // map
    private var mapView: MapView? = null
    private lateinit var map: MapboxMap
    private lateinit var markerManager: MarkerManager
    private var routeLineManager: RouteLineManager? = null

    private var mapSnapshotter: MapSnapshotter? = null

    private var mapClickListener: MapboxMap.OnMapClickListener? = null
    private var backPressedCallback: OnBackPressedCallback? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        editRouteViewModel.route = LineString.fromPolyline(args.routePolyline, PRECISION_6)
        Timber.d("Route: ${editRouteViewModel.route}")
        editRouteViewModel.routeMarkers = args.routeMarkers?.toList()

        setupBackButtonClickObserver()
        setupViewModelObservers()
        // setupUI()

        // TODO sliding panel hier statt in mapFragment?
        // setup the sliding panel BEFORE the map!
        // setupSlidingPanel()

        // init mapbox map
        mapView = binding.editMapView
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)
    }

    private fun setupBackButtonClickObserver() {
        // This callback will show an alert dialog when the back button is pressed
        backPressedCallback = activity?.onBackPressedDispatcher?.addCallback(
            viewLifecycleOwner,
            enabled = true
        ) {
            with(MaterialAlertDialogBuilder(requireActivity())) {
                setTitle(R.string.attention)
                setMessage("Wenn du jetzt abbrichst, wird diese Route nicht gespeichert und dein bisheriger " +
                        "Fortschritt geht verloren! Möchtest du wirklich zurückgehen?")
                setPositiveButton(R.string.yes) { _, _ -> findNavController().navigateUp() }
                setNegativeButton(R.string.continue_edit) { _, _ -> }
                show()
            }
        }
    }

    private fun setupViewModelObservers() {
        editRouteViewModel.snapshotUploadSuccessful.observe(viewLifecycleOwner) { successful ->
            if (successful) {
                // TODO cleanup and navigate to save route fragment

                val routeWaypoints = editRouteViewModel.routeWaypoints.value

                val route: LineString? = editRouteViewModel.route
                val routeCoordinates: MutableList<Point>? = route?.coordinates()
                val routeLength = TurfMeasurement.length(routeCoordinates, TurfConstants.UNIT_METERS)
                /*
                val action = EditRouteFragmentDirections.actionEditRouteFragmentToSaveRouteFragment(
                    waypoints = routeCoordinates.toTypedArray(),
                    distance = routeLength,
                    duration = 0L // TODO
                )
                */
            } else {
                // TODO show warning!
            }
        }
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.map = mapboxMap

        setupMapUI()
        val style = preferencesManager.getCurrentMapStyle()
        setMapStyle(style)

        mapClickListener = MapboxMap.OnMapClickListener {
            val symbol = markerManager.addMarker(it)
            if (symbol != null) {
                editRouteViewModel.addNewWaypoint(symbol)
            }
            true
        }
        mapClickListener?.let { map.addOnMapClickListener(it) }
    }

    private fun setupMapUI() {
        // restrict the camera to a given bounding box as the app focuses only on the uni campus
        map.setLatLngBoundsForCameraTarget(latLngBounds)

        // setup the map snapshotter with the map bounds and disable the mapbox logo for snapshots
        // val mapViewWidth = mapView?.measuredWidth ?: return
        // val mapViewHeight = mapView?.measuredHeight ?: return
        val snapShotOptions = MapSnapshotter.Options(SNAPSHOT_IMAGE_SIZE, SNAPSHOT_IMAGE_SIZE)
        snapShotOptions.withRegion(latLngBounds).withLogo(false)
        mapSnapshotter = MapSnapshotter(requireContext(), snapShotOptions)

        // move the compass to the bottom left corner of the mapView so it doesn't overlap with buttons
        map.uiSettings.compassGravity = Gravity.BOTTOM or Gravity.START
        map.uiSettings.setCompassMargins(
            compassMarginLeft, 0, 0,
            compassMarginBottom
        )

        // init the camera at the saved cameraPosition if it is not null
        /*
        editRouteViewModel.getLastKnownCameraPosition()?.let {
            map.cameraPosition = it
        }*/
    }

    private fun setMapStyle(styleUrl: String?) {
        styleUrl ?: return
        map.setStyle(styleUrl) { mapStyle ->
            // editRouteViewModel.setCurrentMapStyle(mapStyle)
            setupMarkerManager(mapStyle)
            setupRouteLineManager(mapStyle)

            // recreate all markers if any
            val allActiveMarkers = editRouteViewModel.routeMarkers
            val markerCoords = allActiveMarkers?.map { it.markerPosition }
            markerManager.addMarkers(markerCoords)

            // recreate all route lines
            val routeLine = editRouteViewModel.route
            if (routeLine != null) {
                routeLineManager?.addLineToMap(routeLine)
            }

            // editRouteViewModel.setMapReadyStatus(true)
        }
    }

    private fun setupMarkerManager(mapStyle: Style) {
        markerManager = get { parametersOf(mapView, map, mapStyle) }
        viewLifecycleOwner.lifecycle.addObserver(markerManager)
    }

    private fun setupRouteLineManager(mapStyle: Style) {
        routeLineManager = get { parametersOf(mapView, map, mapStyle) }
        routeLineManager?.let { viewLifecycleOwner.lifecycle.addObserver(it) }
    }

    private fun finishRouteEditing() {
        if (!hasInternetConnection(requireContext(), R.string.no_internet_save_route)) {
            return
        }

        // TODO ganz am Ende?
        // - make a snapshot of the created route with the Mapbox Snapshotter and save it to firebase storage
        // - set the snapshot as route thumbnail and save the new route for this user to firebase via viewmodel

        /*
        val mapStyle = editRouteViewModel.getCurrentMapStyle()
        // set the current map style // TODO or use always default style?
        mapSnapshotter?.setStyleUrl(mapStyle?.uri)
        mapSnapshotter?.start { snapshot ->
            val routeBitmap = snapshot.bitmap
            measureTimeFor("uploading route snapshot") {
                editRouteViewModel.uploadRouteSnapshot(routeBitmap)
            }
        }*/

        // TODO option2: (diesmal auch mit annotations)
        measureTimeFor("uploading route snapshot alternative") {
            map.snapshot {
                editRouteViewModel.uploadRouteSnapshot(it)
            }
        }

        // TODO oder static image api am besten statt einfachem snapshot ??
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear() // clear the old menu
        inflater.inflate(R.menu.menu_edit_route, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.cancelRouteEditButton -> {
                // TODO
                true
            }
            R.id.saveRouteButton -> {
                with(MaterialAlertDialogBuilder(requireActivity())) {
                    setTitle(R.string.save_created_route_confirmation)
                    setPositiveButton(R.string.yes) { _, _ ->
                        finishRouteEditing()
                    }
                    setNegativeButton(R.string.continue_edit) { _, _ -> }
                    show()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

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
        // Make sure to stop the snapshotter on pause if it exists
        mapSnapshotter?.cancel()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
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
        backPressedCallback?.remove()
        mapClickListener?.let { map.removeOnMapClickListener(it) }
        mapView?.onDestroy()
        super.onDestroyView()
    }

    companion object {
        private const val SNAPSHOT_IMAGE_SIZE = 400 // in px

        // TODO duplicate:

        // custom margins of the mapbox compass
        private const val compassMarginLeft = 10
        private const val compassMarginBottom = 100

        // camera bounding box
        private val southWestCorner = LatLng(48.990768, 12.087611)
        private val northEastCorner = LatLng(49.006718, 12.101880)
        private val latLngBounds = LatLngBounds.Builder()
            .include(southWestCorner)
            .include(northEastCorner)
            .build()
    }
}
