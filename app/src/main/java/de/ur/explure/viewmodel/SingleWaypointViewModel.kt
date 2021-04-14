package de.ur.explure.viewmodel

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.storage.StorageReference
import de.ur.explure.model.waypoint.WayPoint
import de.ur.explure.repository.storage.StorageRepositoryImpl
import de.ur.explure.utils.FirebaseResult
import kotlinx.coroutines.launch

class SingleWaypointViewModel(private val storageRepo: StorageRepositoryImpl) : ViewModel() {

    val wayPoint: MutableLiveData<WayPoint> = MutableLiveData()

    val videoUri: MutableLiveData<Uri> = MutableLiveData()
    val imageReference: MutableLiveData<StorageReference> = MutableLiveData()
    val audioUri: MutableLiveData<Uri> = MutableLiveData()

    val showImageError: MutableLiveData<Boolean> = MutableLiveData(false)
    val showVideoError: MutableLiveData<Boolean> = MutableLiveData(false)

    fun setWayPoint(wayPointData: WayPoint) {
        wayPoint.postValue(wayPointData)
        wayPointData.imageURL?.run { getImageData(this) }
        wayPointData.videoURL?.run { getVideoData(this) }
    }

    private fun getVideoData(videoURL: String) {
        viewModelScope.launch {
            val downloadTask = storageRepo.getDownloadURL(videoURL)
            if (downloadTask is FirebaseResult.Success) {
                videoUri.postValue(downloadTask.data)
            } else {
                showVideoError.postValue(true)
            }
        }
    }

    private fun getImageData(imageURL: String) {
        val storageRef = storageRepo.getStorageRefForURL(imageURL)
        if (storageRef != null) {
            imageReference.postValue(storageRef)
        } else {
            showImageError.postValue(true)
        }
    }
}
