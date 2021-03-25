package de.ur.explure.adapter

import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import com.hannesdorfmann.adapterdelegates4.AdapterDelegate
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import de.ur.explure.R
import de.ur.explure.databinding.WaypointEditCardBinding
import de.ur.explure.model.waypoint.WayPointDTO
import org.koin.core.component.KoinComponent

class WayPointCreateAdapter(private val clickListener: (WayPointDTO) -> Unit) :
    AsyncListDifferDelegationAdapter<WayPointDTO>(WayPointDiffCallback), KoinComponent {

    init {
        delegatesManager.addDelegate(wayPointDelegate())
    }

    private fun wayPointDelegate(): AdapterDelegate<List<WayPointDTO>> =
        adapterDelegateViewBinding(
            { layoutInflater, root -> WaypointEditCardBinding.inflate(layoutInflater, root, false) }
        ) {

            // Save the original elevation of the layout as otherwise the elevation would
            // decrease on every click and not just the first.
            // This is run once in onCreateViewHolder and not everytime onBindViewHolder is called!
            val originalElevation = binding.root.cardElevation

            binding.waypointCardview.setOnClickListener {
                // reduce the cardview's elevation by half to visualize the click better
                binding.root.cardElevation = originalElevation / 2
                clickListener(item)
            }
            bind {
                val colorFilter = ContextCompat.getColor(context, R.color.highlightColor)
                binding.tvWaypointTitle.text = item.title
                if (item.audioURL != null) {
                    binding.ivAudioIcon.setColorFilter(colorFilter)
                }
                if (item.imageURL != null) {
                    binding.ivImageIcon.setColorFilter(colorFilter)
                }
                if (item.videoURL != null) {
                    binding.ivVideoIcon.setColorFilter(colorFilter)
                }
            }
        }
}

// diffUtil for better performance and default animations
object WayPointDiffCallback : DiffUtil.ItemCallback<WayPointDTO>() {
    override fun areItemsTheSame(oldItem: WayPointDTO, newItem: WayPointDTO): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: WayPointDTO, newItem: WayPointDTO): Boolean {
        return oldItem.geoPoint == newItem.geoPoint
    }
}
