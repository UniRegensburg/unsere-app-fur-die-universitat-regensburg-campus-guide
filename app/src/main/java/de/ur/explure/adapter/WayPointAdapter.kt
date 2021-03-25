package de.ur.explure.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import de.ur.explure.databinding.WaypointItemBinding
import de.ur.explure.model.waypoint.WayPoint
import java.util.*

class WayPointAdapter : RecyclerView.Adapter<WayPointAdapter.ViewHolder>() {

    private var wayPointList: MutableList<WayPoint> = mutableListOf()

    fun setItems(wayPoints: List<WayPoint>) {
        wayPointList = wayPoints.toMutableList()
        this.notifyDataSetChanged()
    }

    inner class ViewHolder(binding: WaypointItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val wayPointTitle = binding.wayPointTitle
        val wayPointDescription = binding.wayPointDescription
        val hasImage = binding.wayPointHasImage
        val hasVideo = binding.wayPointHasVideo
        val hasAudio = binding.wayPointHasAudio
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            WaypointItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = wayPointList[position]
        holder.wayPointTitle.text = currentItem.title
        holder.wayPointDescription.text = currentItem.description
        setExtras(holder, position)
    }

    private fun setExtras(holder: ViewHolder, position: Int) {
        val currentItem = wayPointList[position]
            if (!currentItem.imageURL.isNullOrEmpty()) {
                holder.hasImage.visibility = View.VISIBLE
            }
            if (!currentItem.videoURL.isNullOrEmpty()) {
                holder.hasVideo.visibility = View.VISIBLE
            }
            if (!currentItem.audioURL.isNullOrEmpty()) {
                holder.hasAudio.visibility = View.VISIBLE
            }
    }

    override fun getItemCount(): Int {
        return wayPointList.size
    }
}
