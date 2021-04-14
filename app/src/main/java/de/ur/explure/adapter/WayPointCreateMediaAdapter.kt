package de.ur.explure.adapter

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.widget.ImageView
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.hannesdorfmann.adapterdelegates4.ListDelegationAdapter
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import de.ur.explure.GlideApp
import de.ur.explure.R
import de.ur.explure.databinding.CardviewWaypointAudioBinding
import de.ur.explure.databinding.CardviewWaypointImageBinding
import de.ur.explure.databinding.CardviewWaypointVideoBinding
import de.ur.explure.model.view.WayPointAudioItem
import de.ur.explure.model.view.WayPointImageItem
import de.ur.explure.model.view.WayPointMediaItem
import de.ur.explure.model.view.WayPointVideoItem
import org.koin.core.component.KoinComponent
import timber.log.Timber

@Suppress("TooGenericExceptionCaught", "SwallowedException")
class WayPointCreateMediaAdapter(private val mediaListener: WayPointMediaInterface) :
    ListDelegationAdapter<List<WayPointMediaItem>>(), KoinComponent {

    init {
        delegatesManager.addDelegate(imageDelegate())
        delegatesManager.addDelegate(videoDelegate())
        delegatesManager.addDelegate(audioDelegate())
    }

    private var audioPlayer: MediaPlayer? = null
    private var isPlayingAudio: Boolean = false

    private fun imageDelegate() =
        adapterDelegateViewBinding<
                WayPointImageItem,
                WayPointMediaItem,
                CardviewWaypointImageBinding>({ layoutInflater, root ->
            CardviewWaypointImageBinding.inflate(
                layoutInflater,
                root,
                false
            )
        }) {

            bind {
                GlideApp
                    .with(itemView)
                    .load(item.uri)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(binding.ivImagePreview)

                binding.ivDeleteButton.setOnClickListener {
                    mediaListener.removeMediaItem(item)
                }
            }
        }

    private fun videoDelegate() =
        adapterDelegateViewBinding<
                WayPointVideoItem,
                WayPointMediaItem,
                CardviewWaypointVideoBinding>({ layoutInflater, root ->
            CardviewWaypointVideoBinding.inflate(
                layoutInflater,
                root,
                false
            )
        }) {

            bind {
                try {
                    binding.ivVideoPreview.setVideoURI(item.uri)
                    binding.ivVideoPreview.seekTo(1)
                } catch (e: Exception) {
                    Timber.d("Failed to load video into preview")
                }

                binding.ivDeleteButton.setOnClickListener {
                    mediaListener.removeMediaItem(item)
                }
            }
        }

    private fun audioDelegate() =
        adapterDelegateViewBinding<
                WayPointAudioItem,
                WayPointMediaItem,
                CardviewWaypointAudioBinding>({ layoutInflater, root ->
            CardviewWaypointAudioBinding.inflate(
                layoutInflater,
                root,
                false
            )
        }) {

            bind {
                binding.ivPlayAudioBtn.setOnClickListener {
                    if (!isPlayingAudio) {
                        playAudio(context, item.uri, binding.ivPlayAudioBtn)
                        binding.ivPlayAudioBtn.setImageResource(R.drawable.ic_pause)
                    } else {
                        pauseAudio()
                        binding.ivPlayAudioBtn.setImageResource(R.drawable.ic_play_arrow)
                    }
                }
                binding.ivDeleteButton.setOnClickListener {
                    isPlayingAudio = false
                    mediaListener.removeMediaItem(item)
                }
            }
        }

    private fun pauseAudio() {
        try {
            audioPlayer?.pause()
            isPlayingAudio = false
        } catch (e: Exception) {
            Timber.d("Failed to pause list audio")
        }
    }

    private fun playAudio(context: Context, uri: Uri, button: ImageView) {
        try {
            if (audioPlayer == null) {
                audioPlayer = MediaPlayer()
                audioPlayer?.setOnCompletionListener {
                    it.release()
                    isPlayingAudio = false
                    button.setImageResource(R.drawable.ic_play_arrow)
                    audioPlayer = null
                }
                audioPlayer?.setDataSource(context, uri)
                audioPlayer?.prepare()
            }
            audioPlayer?.start()
            isPlayingAudio = true
        } catch (e: Exception) {
            Timber.d("Failed to play list audio")
        }
    }
}
