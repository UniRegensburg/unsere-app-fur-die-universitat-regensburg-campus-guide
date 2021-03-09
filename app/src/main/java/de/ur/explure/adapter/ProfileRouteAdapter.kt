package de.ur.explure.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import de.ur.explure.databinding.RouteElementBinding
import de.ur.explure.model.route.Route

class ProfileRouteAdapter(
    private val onItemClickListener: OnItemClickListener,
    private val onItemLongClickListener: OnItemLongClickListener
) : RecyclerView.Adapter<ProfileRouteAdapter.ProfileRouteViewHolder>() {

    var routeList: List<Route> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileRouteViewHolder {
        return ProfileRouteViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: ProfileRouteViewHolder, position: Int) {
        holder.bind(routeList[position], onItemClickListener, onItemLongClickListener)
    }

    override fun getItemCount() = routeList.size

    class ProfileRouteViewHolder private constructor(private val binding: RouteElementBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(
            currentRouteItem: Route,
            onClickListener: OnItemClickListener,
            onLongClickListener: OnItemLongClickListener
        ) {
            binding.routeName.text = currentRouteItem.title
            binding.routeTimestamp.text = currentRouteItem.createdAt.toString()

            binding.root.setOnClickListener {
                val position: Int = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onClickListener.onItemClick(position)
                }
            }

            binding.root.setOnLongClickListener {
                val position: Int = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onLongClickListener.onItemLongClick(position)
                }
                return@setOnLongClickListener true
            }
        }

        companion object {
            fun from(parent: ViewGroup): ProfileRouteViewHolder {
                val binding =
                    RouteElementBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                return ProfileRouteViewHolder(binding)
            }
        }
    }

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    interface OnItemLongClickListener {
        fun onItemLongClick(position: Int)
    }
}
