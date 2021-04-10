package de.ur.explure.adapter

import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.hannesdorfmann.adapterdelegates4.ListDelegationAdapter
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import de.ur.explure.GlideApp
import de.ur.explure.WayPointMediaInterface
import de.ur.explure.databinding.CardviewWaypointAudioBinding
import de.ur.explure.databinding.CardviewWaypointImageBinding
import de.ur.explure.databinding.CardviewWaypointVideoBinding
import de.ur.explure.model.view.WayPointAudioItem
import de.ur.explure.model.view.WayPointImageItem
import de.ur.explure.model.view.WayPointMediaItem
import de.ur.explure.model.view.WayPointVideoItem
import org.koin.core.component.KoinComponent

class WayPointCreateMediaAdapter(private val mediaListener: WayPointMediaInterface) :
    ListDelegationAdapter<List<WayPointMediaItem>>(), KoinComponent {

    init {
        delegatesManager.addDelegate(imageDelegate())
        delegatesManager.addDelegate(videoDelegate())
        delegatesManager.addDelegate(audioDelegate())
    }

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
                binding.ivDeleteButton.setOnClickListener {
                    mediaListener.removeMediaItem(item)
                }
            }
        }
}
