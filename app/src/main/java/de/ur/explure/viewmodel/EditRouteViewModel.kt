package de.ur.explure.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.GeoPoint
import com.mapbox.geojson.LineString
import com.mapbox.mapboxsdk.plugins.annotation.Symbol
import de.ur.explure.model.MapMarker
import de.ur.explure.model.waypoint.WayPointDTO
import de.ur.explure.repository.route.RouteRepositoryImpl
import de.ur.explure.utils.FirebaseResult
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*

class EditRouteViewModel(private val routeRepository: RouteRepositoryImpl) : ViewModel() {

    var route: LineString? = null
    var routeMarkers: List<MapMarker>? = null

    val routeWaypoints: MutableLiveData<MutableList<WayPointDTO>> by lazy { MutableLiveData() }

    private val _snapshotUploadSuccessful: MutableLiveData<Boolean> by lazy { MutableLiveData() }
    val snapshotUploadSuccessful = _snapshotUploadSuccessful

    var uploadedRouteUri: String? = null

    // TODO größtenteils duplicate code (siehe MapViewmodel)
    fun addNewWaypoint(symbol: Symbol) {
        val coordinates = symbol.latLng
        val waypoint = WayPointDTO(
            title = UUID.randomUUID().toString(),
            geoPoint = GeoPoint(coordinates.latitude, coordinates.longitude)
        )
        routeWaypoints.value?.add(waypoint)
        routeWaypoints.value = routeWaypoints.value
    }

    fun uploadRouteSnapshot(routeBitmap: Bitmap) {
        viewModelScope.launch {
            // TODO show progress ?
            when (val uploadResult = routeRepository.uploadRouteThumbnail(routeBitmap)) {
                is FirebaseResult.Success -> {
                    Timber.d("Uploading snapshot was successful")
                    _snapshotUploadSuccessful.postValue(true)
                    // TODO uploadedRouteUri = routeUri

                    val storagePath = uploadResult.data.metadata?.path
                    val storageReference = uploadResult.data.metadata?.reference
                    val uri = storageReference?.downloadUrl
                    // val uri = storageReference?.downloadUrl?.result
                    Timber.d("url in viewmodel: $uri")

                    // TODO save the uri in the route's entry in firestore so we can access it later !!
                }
                is FirebaseResult.Canceled -> {
                    Timber.d("Uploading snapshot was canceled")
                    _snapshotUploadSuccessful.postValue(false)
                }
                is FirebaseResult.Error -> {
                    Timber.d("Uploading snapshot failed")
                    _snapshotUploadSuccessful.postValue(false)
                }
            }
        }
    }
}
