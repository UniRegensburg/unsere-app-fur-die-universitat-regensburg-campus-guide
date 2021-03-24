package de.ur.explure.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import de.ur.explure.adapter.RouteCreationAdapter.ViewHolder.Companion.from
import de.ur.explure.databinding.WaypointListItemBinding
import de.ur.explure.model.MapMarker
import de.ur.explure.utils.ItemDragHelper
import java.util.*

typealias onItemClickCallback = (waypointMarker: MapMarker, adapterPosition: Int) -> Unit

class RouteCreationAdapter(
    private val onDragListener: ItemDragHelper.OnDragStartListener,
    private val callback: onItemClickCallback
) : RecyclerView.Adapter<RouteCreationAdapter.ViewHolder>(), ItemDragHelper.CustomDragListener {

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
        viewHolder.bind(item, callback, onDragListener)
    }

    // defined with a private constructor so it can only be instantiated with the from()-Method!
    class ViewHolder private constructor(private val binding: WaypointListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("ClickableViewAccessibility")
        fun bind(
            waypointMarker: MapMarker,
            callback: onItemClickCallback,
            dragListener: ItemDragHelper.OnDragStartListener
        ) {
            val waypoint = waypointMarker.wayPoint
            // setup the view
            binding.waypointTitle.text = waypoint.title
            binding.waypointDescription.text = waypoint.description

            // TODO get the waypoint imageUrl and show it in the recycler item ?
            /*
            val iconIdentifier = itemView.context.resources.getIdentifier(
                waypoint.imageURL,
                "drawable",
                itemView.context.packageName
            )
            binding.waypointImage.setImageResource(iconIdentifier)
            */

            // allow drag & drop with the drag handle icon
            binding.waypointDragHandleIcon.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    dragListener.onStartDrag(this)
                }
                true
            }

            // allow removing items by clicking on the delete icon
            binding.waypointDeleteIcon.setOnClickListener {
                // TODO
            }

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

    // update the waypoint list after drag & drop event has finished
    override fun onRowMoved(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(waypointMarkerList, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(waypointMarkerList, i, i - 1)
            }
        }
        notifyItemMoved(fromPosition, toPosition)
    }

    override fun onRowSelected(itemViewHolder: ViewHolder) {
        // not needed
    }

    override fun onRowClear(itemViewHolder: ViewHolder) {
        // not needed
    }
}
