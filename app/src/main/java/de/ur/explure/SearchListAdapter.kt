package de.ur.explure

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import de.ur.explure.model.route.Route
import kotlinx.android.synthetic.main.search_item.view.*

class SearchListAdapter(private val onClick: (Route) -> Unit) :
        ListAdapter<Route, SearchListAdapter.RouteViewHolder>(RouteDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteViewHolder {
        return RouteViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: RouteViewHolder, position: Int) {
        holder.bind(getItem(position), onClick)
    }

    class RouteViewHolder private constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(data: Route, onClick: (Route) -> Unit) {
            itemView.search_route_title.text = data.title
            itemView.search_route_description.text = data.description
            itemView.setOnClickListener {
                onClick(data)
            }
        }

        companion object {
            fun from(parent: ViewGroup): RouteViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val view = layoutInflater.inflate(R.layout.search_item, parent, false)

                return RouteViewHolder(view)
            }
        }
    }
}

object RouteDiffCallback : DiffUtil.ItemCallback<Route>() {
    override fun areItemsTheSame(oldItem: Route, newItem: Route): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: Route, newItem: Route): Boolean {
        return oldItem.id == newItem.id
    }
}
