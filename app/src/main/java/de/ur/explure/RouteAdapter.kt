package de.ur.explure

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.ur.explure.model.route.Route
import kotlinx.android.synthetic.main.route_element.view.*

class RouteAdapter(
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<RouteAdapter.RouteViewHolder>() {

    var routeList: List<Route> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.route_element, parent, false)
        return RouteViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: RouteViewHolder, position: Int) {
        val currentItem = routeList[position]
        holder.routeName.text = currentItem.description
        holder.routeTimestamp.text = currentItem.createdAt.toString()
    }

    override fun getItemCount() = routeList.size

    inner class RouteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        val routeName: TextView = itemView.routeName
        val routeTimestamp: TextView = itemView.routeTimestamp

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            val position: Int = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                listener.onItemClick(position)
            }
        }
    }

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }
}
