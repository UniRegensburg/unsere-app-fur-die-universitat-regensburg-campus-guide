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
import com.mapbox.mapboxsdk.geometry.LatLng
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
import de.ur.explure.R
import de.ur.explure.databinding.FragmentEditRouteBinding
import de.ur.explure.extensions.pointToLatLng
import de.ur.explure.extensions.toFeature
import de.ur.explure.map.InfoWindowGenerator
import de.ur.explure.map.MapHelper
import de.ur.explure.map.MarkerManager
import de.ur.explure.model.waypoint.WayPointDTO
import de.ur.explure.utils.hasInternetConnection
import de.ur.explure.utils.measureTimeFor
import de.ur.explure.utils.showSnackbar
import de.ur.explure.viewmodel.EditRouteViewModel
import org.koin.android.ext.android.get
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.androidx.viewmodel.scope.emptyState
import org.koin.core.parameter.parametersOf
import timber.log.Timber

@Suppress("TooManyFunctions")
class EditRouteFragment : Fragment(R.layout.fragment_edit_route),
    InfoWindowGenerator.InfoWindowListener,
    MapHelper.MapHelperListener, MarkerManager.MarkerEditListener {

    private val binding by viewBinding(FragmentEditRouteBinding::bind)
    private val editRouteViewModel: EditRouteViewModel by viewModel(state = emptyState())

    private val args: EditRouteFragmentArgs by navArgs()

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

        // TODO sliding panel hier statt in mapFragment?
        // setup the sliding panel BEFORE the map!
        // setupSlidingPanel()

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
    }

    override fun onMapLoaded(map: MapboxMap) {
        // not needed
    }

    override fun onMapStyleLoaded(mapStyle: Style) {
        setupMapData(mapStyle)
        setupListeners()
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
        // This should always be unique as no markers can be at the exact same position!
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

        // remove old feature view from info window map // TODO test
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

    private fun editWaypoint(feature: Feature) {
        val waypoint = editRouteViewModel.getWaypointForFeature(feature)
        // TODO
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
        }

        // TODO oder static image api am besten statt einfachem snapshot ??
    }

    private fun onSuccessfulSnapshot() {
        val routeWaypoints = editRouteViewModel.getWayPoints()
        val routeWaypointArray = routeWaypoints?.toTypedArray() // ?: return@observe
        if (routeWaypointArray == null) {
            // TODO for debugging only
            showSnackbar(
                requireActivity(),
                "Fehler: Keine Waypoints im Viewmodel gefunden!",
                colorRes = R.color.colorError
            )
            return
        }

        val route: LineString? = editRouteViewModel.getRoute()
        if (route == null) {
            // TODO for debugging only
            showSnackbar(
                requireActivity(),
                "Fehler: Keine Route im Viewmodel gefunden!",
                colorRes = R.color.colorError
            )
            return
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
    }

    /**
     * * Menu
     */

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

        if (mapHelper.isMapInitialized()) {
            mapHelper.map.removeOnMapClickListener(this::onMapClick)
            mapHelper.map.removeOnMapLongClickListener(this::onMapLongClick)
        }

        infoWindowMap.clear()
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
