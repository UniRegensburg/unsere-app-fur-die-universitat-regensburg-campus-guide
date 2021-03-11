package de.ur.explure.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import de.ur.explure.R
import de.ur.explure.model.waypoint.WayPoint
import kotlinx.android.synthetic.main.waypoint_list_item.view.*

typealias onItemClickCallback = (itemView: View, waypoint: WayPoint) -> Unit

class RouteCreationAdapter(private val callback: onItemClickCallback) :
    ListAdapter<WayPoint, RouteCreationAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

    /**
     * Replace the contents of a view (this is invoked by the layout manager)
     */
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val item = getItem(position)
        viewHolder.bind(item, callback)
    }

    // defined with a private constructor so it can only be instantiated with the from()-Method!
    class ViewHolder private constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(waypoint: WayPoint, callback: onItemClickCallback) {
            // setup the view
            itemView.waypoint_title.text = waypoint.title
            itemView.waypoint_description.text = waypoint.description

            // TODO get the waypoint imageUrl and show it in the recycler item !
            /*
            val iconIdentifier = itemView.context.resources.getIdentifier(
                waypoint.imageURL,
                "drawable",
                itemView.context.packageName
            )*/
            // itemView.marker_image.setImageResource(iconIdentifier)

            // TODO allow drag & drop when clicked on the drag_handler_icon !

            enableItemClicks(waypoint, callback)
        }

        private fun enableItemClicks(waypoint: WayPoint, callback: onItemClickCallback) {
            // set a ripple effect on the background to be shown on click
            itemView.marker_list_item.setBackgroundResource(R.drawable.ripple_effect)

            itemView.setOnClickListener {
                // invoke the given callback on item click
                callback(it, waypoint)
            }
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val view = layoutInflater.inflate(R.layout.waypoint_list_item, parent, false)

                return ViewHolder(view)
            }
        }
    }
}

/**
 * This callback efficiently checks which items need to be updated so only these are redrawn
 * instead of the entire list!
 */
class DiffCallback : DiffUtil.ItemCallback<WayPoint>() {
    override fun areItemsTheSame(oldItem: WayPoint, newItem: WayPoint): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: WayPoint, newItem: WayPoint): Boolean {
        return oldItem == newItem
    }
}
