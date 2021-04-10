package de.ur.explure.viewmodel

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.GeoPoint
import de.ur.explure.model.view.WayPointMediaItem
import de.ur.explure.model.waypoint.WayPointDTO
import timber.log.Timber

class CreateWayPointViewModel : ViewModel() {

    val newWayPointDTO: MutableLiveData<WayPointDTO> = MutableLiveData()

    val oldWayPointDTO: MutableLiveData<WayPointDTO> = MutableLiveData()

    val selectedImage: MutableLiveData<Uri> = MutableLiveData()

    val mediaList : MutableLiveData<MutableList<WayPointMediaItem>> = MutableLiveData(mutableListOf())

    fun initWayPointDTOEdit(wayPointDTO: WayPointDTO) {
        newWayPointDTO.postValue(wayPointDTO)
        oldWayPointDTO.postValue(wayPointDTO)
        Timber.d("Editing Waypoint: %s", wayPointDTO.toString())
    }

    fun initNewWayPointDTO(longitude: Double, latitude: Double) {
        val wayPointDTO = WayPointDTO("", GeoPoint(latitude, longitude))
        Timber.d("Creating new Waypoint with: %s", wayPointDTO.toString())
        newWayPointDTO.postValue(wayPointDTO)
    }

    fun setTitle(title: String) {
        newWayPointDTO.value?.title = title
    }

    fun setDescription(description: String) {
        newWayPointDTO.value?.title = description
    }

    fun setSelectedImage(uri: Uri){
        selectedImage.postValue(uri)
    }

    fun addMediaItem(item: WayPointMediaItem){
        val list = mediaList.value ?: mutableListOf()
        list.add(item)
        mediaList.postValue(list)
    }

    fun replaceMediaItem(item: WayPointMediaItem, type: Class<*>){
        val list = mediaList.value ?: mutableListOf()
        list.forEach {

        }
        mediaList.postValue(list)
    }

    fun deleteMediaItem(item: WayPointMediaItem){
        val list = mediaList.value ?: mutableListOf()
        list.remove(item)
        mediaList.postValue(list)
    }

}
