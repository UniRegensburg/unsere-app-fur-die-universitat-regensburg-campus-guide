package de.ur.explure.viewmodel

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.GeoPoint
import de.ur.explure.model.view.WayPointAudioItem
import de.ur.explure.model.view.WayPointImageItem
import de.ur.explure.model.view.WayPointMediaItem
import de.ur.explure.model.view.WayPointVideoItem
import de.ur.explure.model.waypoint.WayPointDTO
import de.ur.explure.utils.CachedFileUtils
import timber.log.Timber
import java.io.File

@Suppress("TooGenericExceptionCaught", "SwallowedException")
class CreateWayPointViewModel(private val state: SavedStateHandle) : ViewModel() {

    private var newWayPointDTO: WayPointDTO? = state[WAYPOINT_KEY]

    val oldWayPointDTO: MutableLiveData<WayPointDTO> = MutableLiveData()

    val mediaList: MutableLiveData<MutableList<WayPointMediaItem>> =
        MutableLiveData(mutableListOf())

    val isRecording: MutableLiveData<Boolean> = MutableLiveData()

    val showAudioError: MutableLiveData<Boolean> = MutableLiveData(false)

    var currentTempUri: Uri? = null

    var currentAudioOutputFile: File? = null

    private var audioRecorder: MediaRecorder? = null

    private var audioPlayer: MediaPlayer? = null

    fun initWayPointDTOEdit(wayPointDTO: WayPointDTO) {
        if (newWayPointDTO == null) {
            newWayPointDTO = wayPointDTO
            oldWayPointDTO.postValue(wayPointDTO)
            wayPointDTO.audioUri?.run {
                val mediaItem = WayPointAudioItem(this)
                addMediaItem(mediaItem)
            }
            wayPointDTO.videoUri?.run {
                setVideoMedia(this)
            }
            wayPointDTO.imageUri?.run {
                setImageMedia(this)
            }
            Timber.d("Editing Waypoint: %s", wayPointDTO.toString())
        }
    }

    fun initNewWayPointDTO(longitude: Double, latitude: Double, defaultTitle: String) {
        val wayPointDTO = WayPointDTO(defaultTitle, GeoPoint(latitude, longitude))
        Timber.d("Creating new Waypoint with: %s", wayPointDTO.toString())
        newWayPointDTO = wayPointDTO
    }

    fun getEditedWayPointDTO(): WayPointDTO? {
        mediaList.value?.run {
            this.forEach { mediaItem ->
                when (mediaItem) {
                    is WayPointAudioItem -> {
                        newWayPointDTO?.audioUri = mediaItem.uri
                    }
                    is WayPointVideoItem -> {
                        newWayPointDTO?.videoUri = mediaItem.uri
                    }
                    is WayPointImageItem -> {
                        newWayPointDTO?.imageUri = mediaItem.uri
                    }
                }
            }
        }
        return newWayPointDTO
    }

    fun setTitle(title: String) {
        newWayPointDTO?.title = title
        state[WAYPOINT_KEY] = newWayPointDTO
    }

    fun setDescription(description: String) {
        newWayPointDTO?.description = description
        state[WAYPOINT_KEY] = newWayPointDTO
    }

    fun setImageMedia(uri: Uri) {
        val mediaItem = WayPointImageItem(uri)
        addMediaItem(mediaItem)
    }

    fun setVideoMedia(data: Uri) {
        val mediaImageItem = WayPointVideoItem(data)
        addMediaItem(mediaImageItem)
    }

    private fun addMediaItem(item: WayPointMediaItem) {
        val list = mediaList.value ?: mutableListOf()
        list.add(item)
        mediaList.postValue(list)
    }

    fun deleteMediaItem(item: WayPointMediaItem) {
        val list = mediaList.value ?: mutableListOf()
        list.remove(item)
        mediaList.postValue(list)
        when (item) {
            is WayPointAudioItem -> {
                newWayPointDTO?.audioUri = null
            }
            is WayPointVideoItem -> {
                newWayPointDTO?.videoUri = null
            }
            is WayPointImageItem -> {
                newWayPointDTO?.imageUri = null
            }
        }
    }

    fun createNewImageUri(context: Context): Uri {
        val newUri = CachedFileUtils.getNewImageUri(context)
        currentTempUri = newUri
        return newUri
    }

    fun createNewVideoUri(context: Context): Uri {
        val newUri = CachedFileUtils.getNewVideoUri(context)
        currentTempUri = newUri
        return newUri
    }

    private fun initAudioRecorder(context: Context) {
        currentAudioOutputFile?.delete()
        val outputFile = CachedFileUtils.getNewAudioFile(context)
        audioRecorder = MediaRecorder()
        audioRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
        audioRecorder?.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
        audioRecorder?.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB)
        audioRecorder?.setOutputFile(outputFile.path)
        audioRecorder?.setMaxDuration(MAX_AUDIO_DURATION)

        currentAudioOutputFile = outputFile
    }

    fun startRecording(context: Context) {
        try {
            initAudioRecorder(context)
            audioRecorder?.prepare()
            audioRecorder?.start()
            audioRecorder?.setOnInfoListener { _, what, _ ->
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                    stopRecording()
                }
            }
            isRecording.postValue(true)
        } catch (e: Exception) {
            resetMediaPlayerAndRecorder()
            showAudioError.postValue(true)
        }
    }

    fun stopRecording() {
        try {
            isRecording.postValue(false)
            audioRecorder?.stop()
            audioRecorder?.release()
            audioRecorder = null
        } catch (e: Exception) {
            resetMediaPlayerAndRecorder()
            showAudioError.postValue(true)
        }
    }

    fun playRecording(context: Context) {
        audioPlayer = MediaPlayer()
        val audioFile = currentAudioOutputFile
        if (audioFile != null) {
            try {
                audioPlayer?.setOnCompletionListener {
                    it.release()
                    audioPlayer = null
                }
                audioPlayer?.setDataSource(
                    context,
                    CachedFileUtils.getUriForFile(context, audioFile)
                )
                audioPlayer?.prepare()
                audioPlayer?.start()
            } catch (e: Exception) {
                resetMediaPlayerAndRecorder()
                showAudioError.postValue(true)
            }
        }
    }

    fun saveAudioRecording(context: Context) {
        val audioFile = currentAudioOutputFile
        if (audioFile != null) {
            val uri = CachedFileUtils.getUriForFile(context, audioFile)
            val mediaAudioItem = WayPointAudioItem(uri)
            addMediaItem(mediaAudioItem)
        } else {
            showAudioError.postValue(true)
        }
        resetMediaPlayerAndRecorder()
    }

    fun resetMediaPlayerAndRecorder() {
        try {
            isRecording.postValue(false)
            audioPlayer?.release()
            audioRecorder?.release()
        } catch (e: Exception) {
            Timber.d("Failed to release Audio Player or Recorder")
        } finally {
            audioPlayer = null
            audioRecorder = null
        }
    }

    companion object {
        const val MAX_AUDIO_DURATION = 300000
        private const val WAYPOINT_KEY = "waypointTitle"
    }
}
