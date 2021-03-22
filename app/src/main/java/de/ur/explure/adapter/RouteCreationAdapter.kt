package de.ur.explure.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import de.ur.explure.R
import de.ur.explure.adapter.RouteCreationAdapter.ViewHolder.Companion.from
import de.ur.explure.databinding.WaypointListItemBinding
import de.ur.explure.model.MapMarker

typealias onItemClickCallback = (waypointMarker: MapMarker, adapterPosition: Int) -> Unit

class RouteCreationAdapter(private val callback: onItemClickCallback) :
    RecyclerView.Adapter<RouteCreationAdapter.ViewHolder>() {

    var waypointMarkerList = emptyList<MapMarker>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItemCount(): Int = waypointMarkerList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return from(parent)
    }

    /**
     * Replace the contents of a view (this is invoked by the layout manager)
     */
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val item = waypointMarkerList[position]
        viewHolder.bind(item, callback)
    }

    // defined with a private constructor so it can only be instantiated with the from()-Method!
    class ViewHolder private constructor(private val binding: WaypointListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(waypointMarker: MapMarker, callback: onItemClickCallback) {
            val waypoint = waypointMarker.wayPoint
            // setup the view
            binding.waypointTitle.text = waypoint.title
            binding.waypointDescription.text = waypoint.description

            // TODO get the waypoint imageUrl and show it in the recycler item !
            /*
            val iconIdentifier = itemView.context.resources.getIdentifier(
                waypoint.imageURL,
                "drawable",
                itemView.context.packageName
            )*/
            // binding.waypointImage.setImageResource(iconIdentifier)

            // TODO allow drag & drop when clicked on the drag_handler_icon !

            enableItemClicks(waypointMarker, callback)
        }

        private fun enableItemClicks(waypointMarker: MapMarker, callback: onItemClickCallback) {
            // set a ripple effect on the background to be shown on click
            binding.markerListItem.setBackgroundResource(R.drawable.ripple_effect)

            itemView.setOnClickListener {
                // invoke the given callback on item click
                callback(waypointMarker, layoutPosition)
            }
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val binding = WaypointListItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return ViewHolder(binding)
            }
        }
    }
}
