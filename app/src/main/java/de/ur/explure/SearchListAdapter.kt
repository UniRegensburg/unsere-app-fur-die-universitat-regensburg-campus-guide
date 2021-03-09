package de.ur.explure

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import de.ur.explure.databinding.SearchItemBinding
import de.ur.explure.model.route.Route

class SearchListAdapter(private val onClick: (Route) -> Unit) :
    ListAdapter<Route, SearchListAdapter.RouteViewHolder>(RouteDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteViewHolder {
        return RouteViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: RouteViewHolder, position: Int) {
        holder.bind(getItem(position), onClick)
    }

    class RouteViewHolder private constructor(private val binding: SearchItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(data: Route, onClick: (Route) -> Unit) {
            binding.searchRouteTitle.text = data.title
            binding.searchRouteDescription.text = data.description
            binding.root.setOnClickListener {
                onClick(data)
            }
        }

        companion object {
            fun from(parent: ViewGroup): RouteViewHolder {
                val binding =
                    SearchItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                return RouteViewHolder(binding)
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
