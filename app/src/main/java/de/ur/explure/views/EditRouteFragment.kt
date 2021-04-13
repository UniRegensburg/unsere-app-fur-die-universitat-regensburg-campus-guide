package de.ur.explure.views

import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.Rect
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.crazylegend.viewbinding.viewBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.GeoPoint
import com.mapbox.core.constants.Constants.PRECISION_6
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.expressions.Expression
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import de.ur.explure.R
import de.ur.explure.databinding.FragmentEditRouteBinding
import de.ur.explure.extensions.moveCameraToPosition
import de.ur.explure.extensions.pointToLatLng
import de.ur.explure.extensions.toFeature
import de.ur.explure.extensions.toLatLng
import de.ur.explure.map.InfoWindowGenerator
import de.ur.explure.map.MapHelper
import de.ur.explure.map.MarkerManager
import de.ur.explure.map.MarkerManager.Companion.DESTINATION_ICON
import de.ur.explure.map.MarkerManager.Companion.selectedMarkerZoom
import de.ur.explure.model.waypoint.WayPointDTO
import de.ur.explure.utils.SharedPreferencesManager
import de.ur.explure.utils.hasInternetConnection
import de.ur.explure.utils.measureTimeFor
import de.ur.explure.utils.showSnackbar
import de.ur.explure.viewmodel.EditRouteViewModel
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.scope.emptyState
import org.koin.core.parameter.parametersOf
import timber.log.Timber

// TODO: this map should also allow map style changes!
@Suppress("TooManyFunctions")
class EditRouteFragment : Fragment(R.layout.fragment_edit_route),
    InfoWindowGenerator.InfoWindowListener,
    MapHelper.MapHelperListener, MarkerManager.MarkerEditListener {

    private val binding by viewBinding(FragmentEditRouteBinding::bind)
    private val editRouteViewModel: EditRouteViewModel by sharedViewModel(state = emptyState())

    private val args: EditRouteFragmentArgs by navArgs()

    // SharedPrefs
    private val preferencesManager: SharedPreferencesManager by inject()

    // bottom sheet sliding panel
    private lateinit var slidingBottomPanel: SlidingUpPanelLayout
    private lateinit var slidingPanelListener: SlidingUpPanelLayout.PanelSlideListener

    // map
    private var mapView: MapView? = null
    private lateinit var mapHelper: MapHelper
    private var infoWindowGenerator: InfoWindowGenerator? = null

    private var backPressedCallback: OnBackPressedCallback? = null

    private var uploadSnackbar: Snackbar? = null

    // for info windows
    private var infoWindowMap = HashMap<String, View>()
    private var featureCollection: FeatureCollection? = null
    private var source: GeoJsonSource? = null

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

        initWayPointEditObserver()

        // setup the sliding panel before the map!
        setupSlidingPanel()

        // init mapbox map
        mapView = binding.editMapView
        mapView?.onCreate(savedInstanceState)
        // and setup the mapHelper
        mapHelper = get { parametersOf(mapView, viewLifecycleOwner.lifecycle) }
        viewLifecycleOwner.lifecycle.addObserver(mapHelper)
        mapHelper.setMapHelperListener(this)
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
            setMessage(R.string.leave_route_editing_warning)
            setPositiveButton(R.string.yes) { _, _ -> findNavController().navigateUp() }
            setNegativeButton(R.string.continue_edit) { _, _ -> }
            show()
        }
    }

    private fun setupViewModelObservers() {
        editRouteViewModel.snapshotUploadSuccessful.observe(viewLifecycleOwner) { successful ->
            uploadSnackbar?.dismiss()
            if (successful == null) return@observe // prevent this livedata from firing everytime after a reset

            if (successful) {
                onSuccessfulSnapshot()
                editRouteViewModel.resetSnapshotUpload()
            } else {
                // inform user that something went horribly wrong...
                showSnackbar(
                    requireActivity(),
                    getString(R.string.error_saving_route_snapshot),
                    colorRes = R.color.colorError
                )
            }
        }
        editRouteViewModel.selectedMarker.observe(viewLifecycleOwner) { marker ->
            // move the camera to the selected marker
            if (mapHelper.isMapInitialized()) {
                mapHelper.map.moveCameraToPosition(marker.geoPoint.toLatLng(), selectedMarkerZoom)
                val feature = getFeatureForWaypoint(marker)
                if (feature != null) {
                    // and show it's info window
                    setFeatureSelectState(feature, true)
                }
            }
        }
        editRouteViewModel.deletedWaypoint.observe(viewLifecycleOwner) { waypoint ->
            // check to prevent crashes on config change as marker manager is not setup initially
            if (mapHelper.isMapInitialized()) {
                deleteWaypoint(waypoint)
            }
        }
        editRouteViewModel.buildingExtrusionActive.observe(viewLifecycleOwner) { extrusionsActive ->
            mapHelper.buildingPlugin?.setVisibility(extrusionsActive)
        }
    }

    private fun setupSlidingPanel() {
        // setup bottomSheet
        childFragmentManager.commit {
            replace<RouteWaypointBottomSheet>(R.id.dragViewFragmentContainer)
            setReorderingAllowed(true)
            addToBackStack(null)
        }

        slidingBottomPanel = binding.slidingRootLayout
        binding.dragView.visibility = View.INVISIBLE

        slidingPanelListener = object : SlidingUpPanelLayout.PanelSlideListener {
            override fun onPanelSlide(panel: View?, slideOffset: Float) {
                Timber.i("onPanelSlide, offset $slideOffset")
            }

            override fun onPanelStateChanged(
                panel: View?,
                previousState: SlidingUpPanelLayout.PanelState?,
                newState: SlidingUpPanelLayout.PanelState?
            ) {
                if (previousState == SlidingUpPanelLayout.PanelState.HIDDEN) {
                    // workaround for visual bug that occurs when setting to collapsed while hiding
                    // the bottom navigation which would move this panel higher than it should!
                    slidingBottomPanel.panelState = SlidingUpPanelLayout.PanelState.COLLAPSED
                }
            }
        }
        slidingBottomPanel.addPanelSlideListener(slidingPanelListener)
    }

    override fun onMapLoaded(map: MapboxMap) {
        // not needed
    }

    override fun onMapStyleLoaded(mapStyle: Style) {
        setupMapData(mapStyle)
        setupListeners()

        // add a marker to the route destination
        // TODO overlaps with marker if route was created manually!
        val routeCoordinates = editRouteViewModel.getRouteCoordinates()
        val lastRoutePoint = routeCoordinates?.last() ?: return
        mapHelper.markerManager.addMarker(lastRoutePoint.toLatLng(), DESTINATION_ICON)

        // show bottom sheet panel
        slidingBottomPanel.panelState = SlidingUpPanelLayout.PanelState.COLLAPSED
    }

    private fun setupMapData(mapStyle: Style) {
        // recreate all markers if any
        val routeWaypoints = editRouteViewModel.getWayPoints()
        mapHelper.markerManager.addWaypoints(routeWaypoints)

        // recreate all route lines
        val routeLine = editRouteViewModel.getRoute()
        if (routeLine != null) {
            mapHelper.routeLineManager?.addLineToMap(routeLine)
        }

        // setup the info window layer
        setupCalloutSource(mapStyle)
        setupCalloutLayer(mapStyle)

        setupInfoWindowGenerator()
        // must be called after the infoWindowGenerator has been setup!
        initFeatureCollection(editRouteViewModel.getWayPoints())
    }

    private fun setupListeners() {
        mapHelper.markerManager.setMarkerEditListener(this)
        mapHelper.markerManager.setEditingMarkerClickBehavior()

        // allow users to add markers to the map on click
        mapHelper.map.addOnMapClickListener(this::onMapClick)
    }

    private fun setupInfoWindowGenerator() {
        // needs to be given an activity context to be able to inflate the info window layout!
        infoWindowGenerator = get { parametersOf(requireActivity()) }
        infoWindowGenerator?.let { viewLifecycleOwner.lifecycle.addObserver(it) }
        infoWindowGenerator?.setInfoWindowListener(this)
    }

    private fun initFeatureCollection(waypoints: List<WayPointDTO>?) {
        val initialFeatures = mutableListOf<Feature>()
        waypoints?.forEach {
            val feature = generateNewFeature(it)
            initialFeatures.add(feature)
        }
        featureCollection = FeatureCollection.fromFeatures(initialFeatures)

        // generate callout views on a background thread
        featureCollection?.let { infoWindowGenerator?.generateCallouts(it) }
    }

    private fun generateNewFeature(wayPoint: WayPointDTO): Feature {
        val feature = wayPoint.geoPoint.toFeature()
        // each GeoJSON Feature must have a "selected" property with a boolean value:
        feature.addBooleanProperty(PROPERTY_SELECTED, false)
        // We don't have an id field so we just use the latLng - position as an id.
        // This should always be unique as no other marker can be at the exact same position!
        feature.addStringProperty(PROPERTY_ID, wayPoint.geoPoint.toString())
        feature.addStringProperty(PROPERTY_TITLE, wayPoint.title)
        return feature
    }

    /**
     * This callback is invoked when the [infoWindowGenerator] has finished generating callout bitmaps
     * that can be shown as a map layer.
     */
    override fun onViewsGenerated(
        bitmapHashMap: HashMap<String, Bitmap>,
        viewMap: HashMap<String, View>
    ) {
        mapHelper.map.getStyle { style ->
            // calling addImages is faster as separate addImage calls for each bitmap.
            style.addImages(bitmapHashMap)
            infoWindowMap.putAll(viewMap)
            refreshSource()
        }
    }

    private fun getFeatureForWaypoint(waypoint: WayPointDTO): Feature? {
        val featureList = featureCollection?.features()
        featureList?.let {
            it.forEachIndexed { i, _ ->
                // get the feature that was clicked and toggle it's selected state
                if (featureList[i].getStringProperty(PROPERTY_ID) == waypoint.geoPoint.toString()) {
                    return featureList[i]
                }
            }
        }
        return null
    }

    override fun onMarkerClicked(waypoint: WayPointDTO) {
        // get the feature that was clicked and toggle it's selected state
        val feature = getFeatureForWaypoint(waypoint)
        if (feature != null) {
            if (featureSelectedStatus(feature)) {
                setFeatureSelectState(feature, false)
            } else {
                setSelected(feature)
            }
        }

        // and hide other info windows when a new one is shown
        val indexPos = featureCollection?.features()?.indexOf(feature)
        featureCollection?.features()?.forEachIndexed { index, ft ->
            if (index != indexPos) {
                setFeatureSelectState(ft, false)
            }
        }
    }

    override fun onMarkerLongClicked(waypoint: WayPointDTO) {
        // hide the info window when the user starts dragging a marker
        // (as the callout window is still fixed to the old position!)
        val feature = getFeatureForWaypoint(waypoint)
        feature?.let { setFeatureSelectState(it, false) }
    }

    override fun onMarkerPositionChanged(newPosition: LatLng, waypoint: WayPointDTO) {
        val feature = getFeatureForWaypoint(waypoint) ?: return
        val newGeoPoint = GeoPoint(newPosition.latitude, newPosition.longitude)

        // update the waypointLists in the viewmodel
        val routeWaypoint = editRouteViewModel.getWaypointForFeature(feature)
        routeWaypoint?.geoPoint = newGeoPoint

        // remove old feature view from info window map
        infoWindowGenerator?.removeCalloutView(feature)

        // update the feature in the featureCollection
        val newID = newGeoPoint.toString()
        feature.properties()?.addProperty(PROPERTY_ID, newID)
        val coords = (feature.geometry() as? Point)?.coordinates()
        coords?.set(0, newPosition.longitude)
        coords?.set(1, newPosition.latitude)

        infoWindowGenerator?.generateCallouts(FeatureCollection.fromFeature(feature))

        refreshSource()
    }

    /**
     * Set a feature selected state and update the data on the map.
     *
     * @param feature the feature to be selected.
     */
    private fun setSelected(feature: Feature?) {
        if (feature != null) {
            setFeatureSelectState(feature, true)
            refreshSource()
        }
    }

    /**
     * Selects the state of a feature and updates the data on the map.
     *
     * @param feature the feature to be selected.
     */
    private fun setFeatureSelectState(feature: Feature, selectedState: Boolean) {
        if (feature.properties() != null) {
            feature.properties()?.addProperty(PROPERTY_SELECTED, selectedState)
            refreshSource()
        }
    }

    /**
     * Updates the display of data on the map after the  [featureCollection] has been modified.
     */
    private fun refreshSource() {
        if (featureCollection != null) {
            source?.setGeoJson(featureCollection)
        }
    }

    /**
     * Checks whether a Feature's boolean "selected" property is true or false.
     *
     * @param feature the specific feature to check
     * @return true if "selected" is true. False if the boolean property is false.
     */
    private fun featureSelectedStatus(feature: Feature?): Boolean {
        return if (featureCollection == null) {
            false
        } else {
            feature?.getBooleanProperty(PROPERTY_SELECTED) == true
        }
    }

    private fun onMapClick(position: LatLng): Boolean {
        val screenPoint = mapHelper.map.projection.toScreenLocation(position)
        val features = mapHelper.map.queryRenderedFeatures(screenPoint, CALLOUT_LAYER_ID)
        if (features.isNotEmpty()) {
            // we received a click event on the callout - layer (i.e. the info window)
            val feature = features[0]
            val symbolScreenPoint = mapHelper.map.projection.toScreenLocation(feature.pointToLatLng())
            handleClickCallout(feature, screenPoint, symbolScreenPoint)
        } else {
            // clicked somewhere else on the map, place a new waypoint there
            val createdWaypoint = editRouteViewModel.addNewWayPoint(
                position,
                getString(R.string.default_waypoint_title)
            )
            mapHelper.markerManager.addWaypoint(position, createdWaypoint)

            // convert the waypoint to a feature and add it to the featureCollection
            val newFeature = generateNewFeature(createdWaypoint)
            featureCollection?.features()?.add(newFeature)
            // also generate an info window for this new feature
            infoWindowGenerator?.generateCallouts(FeatureCollection.fromFeature(newFeature))
        }
        return true
    }

    private fun setupCalloutSource(loadedMapStyle: Style) {
        source = GeoJsonSource(CALLOUT_SOURCE_ID, featureCollection)
        source?.let { loadedMapStyle.addSource(it) }
    }

    private fun setupCalloutLayer(loadedMapStyle: Style) {
        loadedMapStyle.addLayer(
            SymbolLayer(CALLOUT_LAYER_ID, CALLOUT_SOURCE_ID)
                .withProperties(
                    // show image with id title based on the value of the title feature property
                    PropertyFactory.iconImage("{$PROPERTY_ID}"),
                    PropertyFactory.iconAllowOverlap(false),
                    PropertyFactory.iconIgnorePlacement(false),
                    PropertyFactory.iconAnchor(Property.ICON_ANCHOR_BOTTOM),
                    // offset icon slightly to match bubble layout
                    PropertyFactory.iconOffset(INFO_WINDOW_OFFSET)
                )
                // add a filter to show only when selected feature property is true
                .withFilter(
                    Expression.eq(
                        Expression.get(PROPERTY_SELECTED),
                        Expression.literal(true)
                    )
                )
        )
    }

    /**
     * This method handles click events for callout symbols.
     * <p>
     * It creates a hit rectangle based on the the textView, offsets that rectangle to the location
     * of the symbol on screen and hit tests that with the screen point.
     * </p>
     *
     * @param feature           the clicked feature
     * @param screenPoint       the point on screen clicked
     * @param symbolScreenPoint the point of the symbol on screen
     */
    private fun handleClickCallout(
        feature: Feature,
        screenPoint: PointF,
        symbolScreenPoint: PointF
    ) {
        val clickedFeatureID = feature.getStringProperty(PROPERTY_ID)
        val featureList = featureCollection?.features()
        // get the correct feature from the featureCollection (the given feature parameter has other coordinates!)
        val waypointFeature = featureList?.find {
            clickedFeatureID == it.getStringProperty(PROPERTY_ID)
        } ?: return

        val calloutView = infoWindowMap[clickedFeatureID] ?: return
        val deleteButton = calloutView.findViewById<ImageButton>(R.id.deleteWaypointButtonIW)
        val editButton = calloutView.findViewById<ImageButton>(R.id.editWaypointButtonIW)

        // create hitboxes for the info window buttons
        val hitRectDeleteButton = Rect()
        deleteButton.getHitRect(hitRectDeleteButton)
        val hitRectEditButton = Rect()
        editButton.getHitRect(hitRectEditButton)

        for (hitRect in arrayOf(hitRectEditButton, hitRectDeleteButton)) {
            // move hitbox to location of symbol
            hitRect.offset(symbolScreenPoint.x.toInt(), symbolScreenPoint.y.toInt())
            // offset vertically to match anchor behaviour and horizontally to match custom offset for icons
            // TODO these offsets are determined empirically, i.e. they can change when a different marker
            //  icon is used or some other offset are changed!
            //  -> Find a better way to get the correct heights from the generated bitmaps! (listener won't work here!!)
            hitRect.offset(HITBOX_OFFSET_X, -calloutView.measuredHeight + HITBOX_OFFSET_Y)
        }

        // hit test if clicked point is in textview hitbox
        when {
            hitRectDeleteButton.contains(screenPoint.x.toInt(), screenPoint.y.toInt()) -> {
                // user clicked on delete button
                with(MaterialAlertDialogBuilder(requireActivity())) {
                    setMessage(R.string.delete_waypoint_confirmation)
                    setPositiveButton(R.string.yes) { _, _ ->
                        deleteWaypoint(waypointFeature)
                    }
                    setNegativeButton(R.string.no) { _, _ -> }
                    show()
                }
            }
            hitRectEditButton.contains(screenPoint.x.toInt(), screenPoint.y.toInt()) -> {
                // user clicked on edit button
                editWaypoint(waypointFeature)
            }
            else -> {
                // user clicked somewhere else on the callout
                Timber.d("Clicked on callout text")
            }
        }
    }

    private fun deleteWaypoint(feature: Feature) {
        val deletedWaypoint = editRouteViewModel.getWaypointForFeature(feature)
        // hide the info window and remove it from the collection
        setFeatureSelectState(feature, false)
        featureCollection?.features()?.remove(feature)
        // also remove the corresponding marker symbol and the route waypoint itself
        if (deletedWaypoint != null) {
            mapHelper.markerManager.deleteWaypoint(deletedWaypoint)
            editRouteViewModel.deleteWaypoint(deletedWaypoint)
        }
    }

    private fun deleteWaypoint(waypoint: WayPointDTO) {
        val feature = getFeatureForWaypoint(waypoint)
        if (feature != null) {
            // hide the info window and remove it from the collection
            setFeatureSelectState(feature, false)
            featureCollection?.features()?.remove(feature)
        }
        // also remove the corresponding marker symbol
        mapHelper.markerManager.deleteWaypoint(waypoint)
    }

    private fun editWaypoint(feature: Feature) {
        // hide info window first because we can't edit it directly and must recreate it later
        setFeatureSelectState(feature, false)

        val waypoint = editRouteViewModel.getWaypointForFeature(feature)
        if (waypoint != null) {
            editRouteViewModel.navigateToWayPointDialogFragment(waypoint)
        }
    }

    // TODO almost identical to the code in the saveRouteFragment
    private fun initWayPointEditObserver() {
        val navBackStackEntry = findNavController().getBackStackEntry(R.id.editRouteFragment)

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME &&
                navBackStackEntry.savedStateHandle.contains(SaveRouteFragment.WAYPOINT_EDIT_KEY)
            ) {
                // get the edited waypoint from the saved state handle
                val editedWayPoint =
                    navBackStackEntry.savedStateHandle.get<WayPointDTO>(SaveRouteFragment.WAYPOINT_EDIT_KEY)
                if (editedWayPoint != null) {
                    // update the waypoint information
                    editRouteViewModel.updateWayPoint(editedWayPoint)
                    // generate a new info window with a new title
                    val waypointFeature = getFeatureForWaypoint(editedWayPoint)
                    if (waypointFeature != null) {
                        waypointFeature.addStringProperty(PROPERTY_TITLE, editedWayPoint.title)
                        infoWindowGenerator?.generateCallouts(FeatureCollection.fromFeature(waypointFeature))
                    }
                }
            }
        }

        navBackStackEntry.lifecycle.addObserver(observer)

        viewLifecycleOwner.lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                navBackStackEntry.lifecycle.removeObserver(observer)
            }
        })
    }

    private fun onMapLongClick(position: LatLng): Boolean {
        val createdWaypoint = editRouteViewModel.addNewWayPoint(position, getString(R.string.default_waypoint_title))
        mapHelper.markerManager.addWaypoint(position, createdWaypoint)

        // convert the waypoint to a feature and add it to the featureCollection
        val newFeature = generateNewFeature(createdWaypoint)
        featureCollection?.features()?.add(newFeature)
        // also generate an info window for this new feature
        infoWindowGenerator?.generateCallouts(FeatureCollection.fromFeature(newFeature))

        return true // consume the click
    }

    private fun finishRouteEditing() {
        if (!hasInternetConnection(requireContext(), R.string.no_internet_save_route)) {
            return
        }

        // hide all info windows before making a snapshot
        featureCollection?.features()?.forEach {
            setFeatureSelectState(it, false)
        }

        // register a camera listener so we can take a snapshot after the camera has been moved
        // to the correct position
        mapHelper.map.addOnCameraIdleListener(this::takeSnapshot)

        // center camera on the route before making a snapshot
        val routeCoordinates = editRouteViewModel.getRouteCoordinates()?.map { it.toLatLng() }
        if (routeCoordinates != null) {
            val latLngBounds = LatLngBounds.Builder()
                // .includes(routeCoordinates)
                .include(routeCoordinates.first())
                .include(routeCoordinates.last())
                .build()
            mapHelper.map.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 0))
        }

        // hide the compass if it shown
        mapHelper.map.uiSettings.isCompassEnabled = false

        // TODO also reset tilt and bearing of map ? or better to give user control over this?
    }

    private fun takeSnapshot() {
        // alternatively, the Static Image API could be used for more flexibility!
        // (see. https://docs.mapbox.com/android/java/guides/static-image/)
        mapHelper.map.snapshot { mapSnapshot ->
            uploadSnackbar = showSnackbar(
                requireActivity(),
                getString(R.string.saving_route),
                colorRes = R.color.colorInfo,
                length = Snackbar.LENGTH_INDEFINITE
            )
            measureTimeFor("uploading route snapshot") {
                editRouteViewModel.uploadRouteSnapshot(mapSnapshot)
            }

            // enable compass again after snapshot
            mapHelper.map.uiSettings.isCompassEnabled = true
        }
    }

    @Suppress("ReturnCount")
    private fun onSuccessfulSnapshot() {
        val routeWaypoints = editRouteViewModel.getWayPoints()
        val routeWaypointArray = routeWaypoints?.toTypedArray()

        // some checks for debugging
        if (routeWaypointArray == null) {
            Timber.e("Fehler: Keine Waypoints im Viewmodel gefunden!")
            return
        }

        val route: LineString? = editRouteViewModel.getRoute()
        if (route == null) {
            Timber.e("Fehler: Keine Route im Viewmodel gefunden!")
            return
        }

        val routeSnapshot = editRouteViewModel.routeSnapshotUri
        if (routeSnapshot == null) {
            Timber.e("Fehler: Keine RoutenSnapshot im Viewmodel gefunden!")
            return
        }

        resetEditMap()

        // get coordinates and calculate length and duration of the route
        val routeCoordinates: MutableList<Point> = route.coordinates()
        val routeLength = TurfMeasurement.length(routeCoordinates, TurfConstants.UNIT_METERS)
        val routeDuration = routeLength * WALKING_SPEED / 60

        val action = EditRouteFragmentDirections.actionEditRouteFragmentToSaveRouteFragment(
            // TODO später stattdessen die id der hier schon erstellten Route übergeben?
            route = route.toPolyline(PRECISION_6),
            routeThumbnail = routeSnapshot,
            waypoints = routeWaypointArray,
            distance = routeLength.toFloat(),
            duration = routeDuration.toFloat()
        )
        findNavController().navigate(action)
    }

    private fun resetEditMap() {
        if (mapHelper.routeLineManager != null) {
            // clear route line on the map
            mapHelper.routeLineManager?.removeMapMatching()
        }

        if (mapHelper.isMarkerManagerInitialized()) {
            // clear waypoints and markers on the map and in the viewmodel
            mapHelper.markerManager.deleteAllMarkers()
            editRouteViewModel.clearAllWaypoints()
        }

        // clear the featureCollection and the info windows
        featureCollection = null
        source?.setGeoJson(FeatureCollection.fromFeatures(arrayOf<Feature>()))
        infoWindowMap.clear()

        // Hide bottom sheet panel
        slidingBottomPanel.panelState = SlidingUpPanelLayout.PanelState.HIDDEN
        binding.dragView.visibility = View.GONE
    }

    /**
     * * Menu
     */

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear() // clear the old menu
        inflater.inflate(R.menu.menu_edit_route, menu)

        menu.findItem(R.id.showBuildings).isChecked = preferencesManager.getBuildingExtrusionShown()
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
            R.id.showBuildings -> {
                item.isChecked = !item.isChecked
                editRouteViewModel.setBuildingExtrusionStatus(item.isChecked)
                preferencesManager.setBuildingExtrusionShown(item.isChecked)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * * Lifecycle
     */

    override fun onStop() {
        super.onStop()
        // save created waypoints so they are not lost on config change or process death!
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

        slidingBottomPanel.removePanelSlideListener(slidingPanelListener)

        if (mapHelper.isMapInitialized()) {
            mapHelper.map.removeOnMapClickListener(this::onMapClick)
            mapHelper.map.removeOnMapLongClickListener(this::onMapLongClick)
            mapHelper.map.removeOnCameraIdleListener(this::takeSnapshot)
        }

        uploadSnackbar?.dismiss()
        super.onDestroyView()
    }

    companion object {
        // in m/s, see https://en.wikipedia.org/wiki/Preferred_walking_speed
        private const val WALKING_SPEED = 1.4
        // private const val WALKING_SPEED = 0.83 // Spaziergang

        private const val CALLOUT_SOURCE_ID = "mapbox.poi.callout_source"
        private const val CALLOUT_LAYER_ID = "mapbox.poi.callout_layer"
        const val PROPERTY_SELECTED = "selectedStatus"
        const val PROPERTY_ID = "waypointID"
        const val PROPERTY_TITLE = "waypointTitle"

        private const val HITBOX_OFFSET_X = -180
        private const val HITBOX_OFFSET_Y = 15
        private val INFO_WINDOW_OFFSET = arrayOf(-5.0f, -45.0f)
    }
}
