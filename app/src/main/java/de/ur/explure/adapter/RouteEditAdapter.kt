package de.ur.explure.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import de.ur.explure.adapter.RouteEditAdapter.ViewHolder.Companion.from
import de.ur.explure.databinding.WaypointListItemBinding
import de.ur.explure.model.waypoint.WayPointDTO
import de.ur.explure.utils.ItemDragHelper
import de.ur.explure.utils.reorderList

typealias onItemClickCallback = (waypoint: WayPointDTO, adapterPosition: Int) -> Unit

class RouteEditAdapter(
    private val onDragListener: ItemDragHelper.OnDragStartListener,
    private val onDeleteListener: OnDeleteItemListener,
    private val callback: onItemClickCallback
) : RecyclerView.Adapter<RouteEditAdapter.ViewHolder>(), ItemDragHelper.CustomDragListener {

    var waypointList = emptyList<WayPointDTO>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItemCount(): Int = waypointList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return from(parent)
    }

    /**
     * Replace the contents of a view (this is invoked by the layout manager)
     */
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val item = waypointList[position]
        viewHolder.bind(item, callback, onDragListener, onDeleteListener)
    }

    // defined with a private constructor so it can only be instantiated with the from()-Method!
    class ViewHolder private constructor(private val binding: WaypointListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("ClickableViewAccessibility")
        fun bind(
            waypoint: WayPointDTO,
            callback: onItemClickCallback,
            dragListener: ItemDragHelper.OnDragStartListener,
            deleteListener: OnDeleteItemListener,
        ) {
            // setup the view
            binding.waypointTitle.text = waypoint.title
            binding.waypointDescription.text = waypoint.description

            itemView.setOnClickListener {
                // invoke the given callback on item click
                callback(waypoint, layoutPosition)
            }

            // allow drag & drop with the drag handle icon
            binding.waypointDragHandleIcon.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    dragListener.onStartDrag(this)
                }
                true
            }

            // allow removing items by clicking on the delete icon
            binding.waypointDeleteIcon.setOnClickListener {
                deleteListener.onItemDeleted(waypoint, layoutPosition)
            }
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val binding = WaypointListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                return ViewHolder(binding)
            }
        }
    }

    // update the waypoint list after drag & drop event has finished
    override fun onRowMoved(fromPosition: Int, toPosition: Int) {
        reorderList(waypointList, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
    }

    override fun onRowSelected(itemViewHolder: ViewHolder) {
        // not needed
    }

    override fun onRowClear(itemViewHolder: ViewHolder) {
        // not needed
    }

    interface OnDeleteItemListener {
        fun onItemDeleted(waypoint: WayPointDTO, layoutPosition: Int)
    }
}
