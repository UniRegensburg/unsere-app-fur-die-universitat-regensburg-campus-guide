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
import com.google.android.material.snackbar.Snackbar
import com.mapbox.core.constants.Constants.PRECISION_6
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import de.ur.explure.R
import de.ur.explure.databinding.FragmentEditRouteBinding
import de.ur.explure.map.MarkerManager
import de.ur.explure.map.RouteLineManager
import de.ur.explure.utils.SharedPreferencesManager
import de.ur.explure.utils.hasInternetConnection
import de.ur.explure.utils.measureTimeFor
import de.ur.explure.utils.showSnackbar
import de.ur.explure.viewmodel.EditRouteViewModel
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.androidx.viewmodel.scope.emptyState
import org.koin.core.parameter.parametersOf

class EditRouteFragment : Fragment(R.layout.fragment_edit_route), OnMapReadyCallback {

    private val binding by viewBinding(FragmentEditRouteBinding::bind)
    private val editRouteViewModel: EditRouteViewModel by viewModel(state = emptyState())

    private val args: EditRouteFragmentArgs by navArgs()

    // SharedPrefs
    private val preferencesManager: SharedPreferencesManager by inject()

    // map
    private var mapView: MapView? = null
    private lateinit var map: MapboxMap
    private lateinit var markerManager: MarkerManager
    private var routeLineManager: RouteLineManager? = null

    private var mapClickListener: MapboxMap.OnMapClickListener? = null
    private var backPressedCallback: OnBackPressedCallback? = null

    private var uploadSnackbar: Snackbar? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        // get nav arguments and save them in the viewModel
        val routeLine = LineString.fromPolyline(args.routePolyline, PRECISION_6)
        editRouteViewModel.saveRoute(routeLine)
        val existingWaypoints = args.routeMarkers?.toList()?.map { it.wayPoint }
        editRouteViewModel.setInitialWayPoints(existingWaypoints)

        setupBackButtonClickObserver()
        setupViewModelObservers()

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
            showCancelRouteEditingDialog()
        }
    }

    private fun showCancelRouteEditingDialog() {
        with(MaterialAlertDialogBuilder(requireActivity())) {
            setTitle(R.string.attention)
            setMessage(
                "Wenn du jetzt abbrichst, wird diese Route nicht gespeichert und dein bisheriger " +
                        "Fortschritt geht verloren! Möchtest du wirklich zurückgehen?"
            )
            setPositiveButton(R.string.yes) { _, _ -> findNavController().navigateUp() }
            setNegativeButton(R.string.continue_edit) { _, _ -> }
            show()
        }
    }

    private fun setupViewModelObservers() {
        editRouteViewModel.snapshotUploadSuccessful.observe(viewLifecycleOwner) { successful ->
            uploadSnackbar?.dismiss()

            if (successful) {
                val routeWaypoints = editRouteViewModel.getWayPoints()
                val routeWaypointArray = routeWaypoints?.toTypedArray() // ?: return@observe
                if (routeWaypointArray == null) {
                    // TODO for debugging only
                    showSnackbar(
                        requireActivity(),
                        "Fehler: Keine Waypoints im Viewmodel gefunden!",
                        colorRes = R.color.colorError
                    )
                    return@observe
                }

                val route: LineString? = editRouteViewModel.getRoute()
                if (route == null) {
                    // TODO for debugging only
                    showSnackbar(
                        requireActivity(),
                        "Fehler: Keine Route im Viewmodel gefunden!",
                        colorRes = R.color.colorError
                    )
                    return@observe
                }

                val routeCoordinates: MutableList<Point> = route.coordinates()
                val routeLength = TurfMeasurement.length(routeCoordinates, TurfConstants.UNIT_METERS)
                val routeDuration = routeLength * WALKING_SPEED / 60

                val action = EditRouteFragmentDirections.actionEditRouteFragmentToSaveRouteFragment(
                    // TODO stattdessen lieber die id der erstellten Route übergeben?
                    route = route.toPolyline(PRECISION_6),
                    waypoints = routeWaypointArray,
                    distance = routeLength.toFloat(),
                    duration = routeDuration.toFloat()
                )
                findNavController().navigate(action)
            } else {
                // TODO show warning!
                showSnackbar(
                    requireActivity(),
                    "Die Route konnte nicht erfolgreich hochgeladen und gespeichert werden!",
                    colorRes = R.color.colorError
                )
            }
        }
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.map = mapboxMap
        setupMapUI()

        val style = preferencesManager.getCurrentMapStyle()
        map.setStyle(style) { mapStyle ->
            setupMarkerManager(mapStyle)
            setupRouteLineManager(mapStyle)

            // recreate all markers if any
            val routeWaypoints = editRouteViewModel.getWayPoints()
            markerManager.addWaypoints(routeWaypoints)

            // recreate all route lines
            val routeLine = editRouteViewModel.getRoute()
            if (routeLine != null) {
                routeLineManager?.addLineToMap(routeLine)
            }
        }

        // TODO use an onLongClicklistener here instead and update instruction?
        // -> should this be setup before or after the markerManager DragListener ??

        // allow users to add markers to the map on click
        mapClickListener = MapboxMap.OnMapClickListener {
            val waypointTitle = editRouteViewModel.addNewWayPoint(it)
            markerManager.addWaypoint(it, waypointTitle)
            true
        }
        mapClickListener?.let { map.addOnMapClickListener(it) }

        map.setOnInfoWindowClickListener {
            // TODO implement info windows ?
            false
        }
    }

    private fun setupMapUI() {
        // restrict the camera to a given bounding box as the app focuses only on the uni campus
        map.setLatLngBoundsForCameraTarget(latLngBounds)

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

        map.snapshot { mapSnapshot ->
            uploadSnackbar = showSnackbar(
                requireActivity(),
                "Die Route wird gespeichert. Bitte warten ...",
                colorRes = R.color.colorInfo,
                length = Snackbar.LENGTH_INDEFINITE
            )
            measureTimeFor("uploading route snapshot") {
                editRouteViewModel.uploadRouteSnapshot(mapSnapshot)
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
                showCancelRouteEditingDialog()
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
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
        editRouteViewModel.saveWayPoints()
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

        uploadSnackbar?.dismiss()
        super.onDestroyView()
    }

    companion object {
        // in m/s, see https://en.wikipedia.org/wiki/Preferred_walking_speed
        private const val WALKING_SPEED = 1.4
        // private const val WALKING_SPEED = 0.83 // Spaziergang

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
