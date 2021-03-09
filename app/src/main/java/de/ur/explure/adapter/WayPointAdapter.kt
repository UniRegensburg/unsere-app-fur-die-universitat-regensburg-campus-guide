package de.ur.explure.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import de.ur.explure.databinding.WaypointItemBinding
import de.ur.explure.model.waypoint.WayPoint
import java.util.*

class WayPointAdapter(private val dataSource: LinkedList<WayPoint>) :
    RecyclerView.Adapter<WayPointAdapter.ViewHolder>() {

    inner class ViewHolder(binding: WaypointItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val waypointTitle = binding.waypointTitle
        val waypointDescription = binding.waypointDescription
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            WaypointItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = dataSource[position]
        holder.waypointTitle.text = currentItem.title
        holder.waypointDescription.text = currentItem.description
    }

    override fun getItemCount(): Int {
        return dataSource.size
    }
}
