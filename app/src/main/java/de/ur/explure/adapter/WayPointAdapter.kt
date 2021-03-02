package de.ur.explure.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.ur.explure.R
import de.ur.explure.model.waypoint.WayPoint
import kotlinx.android.synthetic.main.waypoint_item.view.*
import java.util.*

class WayPointAdapter(private val dataSource: LinkedList<WayPoint>) : RecyclerView.Adapter<WayPointAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val waypointTitle: TextView = itemView.waypointTitle
        val waypointDescription: TextView = itemView.waypointDescription
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)

        val rowView = layoutInflater.inflate(R.layout.waypoint_item, parent, false)
        return ViewHolder(rowView)
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
