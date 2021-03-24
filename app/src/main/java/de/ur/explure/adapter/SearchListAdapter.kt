package de.ur.explure.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.firebase.storage.FirebaseStorage
import de.ur.explure.GlideApp
import de.ur.explure.R
import de.ur.explure.databinding.SearchItemBinding
import de.ur.explure.model.route.Route
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SearchListAdapter(private val onClick: (Route) -> Unit) :
        ListAdapter<Route, SearchListAdapter.RouteViewHolder>(SearchListDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteViewHolder {
        return RouteViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: RouteViewHolder, position: Int) {
        holder.bind(getItem(position), onClick)
    }

    class RouteViewHolder private constructor(private val binding: SearchItemBinding) :
            RecyclerView.ViewHolder(binding.root), KoinComponent {

        private val fireStorage: FirebaseStorage by inject()

        fun bind(data: Route, onClick: (Route) -> Unit) {
            if (data.thumbnailUrl.isNotEmpty()) {
                try {
                    val gsReference = fireStorage.getReferenceFromUrl(data.thumbnailUrl)
                    GlideApp.with(itemView)
                            .load(gsReference)
                            .error(R.drawable.map_background)
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .into(binding.ivRouteThumbnail)
                } catch (_: Exception) {
                }
            }

            binding.tvRouteTitle.text = data.title
            binding.tvRouteDescription.text = data.description
            binding.tvRatingCount.text =
                    itemView.context.resources.getString(R.string.route_item_rating, data.rating.size)
            binding.tvDistance.text =
                    itemView.context.resources.getString(R.string.route_item_distance, data.distance.toFloat())
            binding.tvDuration.text =
                    itemView.context.resources.getString(R.string.route_item_duration, data.duration.toFloat())
            binding.tvWayPointCount.text = data.wayPointCount.toString()
            binding.tvComment.text = data.commentCount.toString()
            binding.ratingBarRoute.rating = data.currentRating.toFloat()
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

object SearchListDiffCallback : DiffUtil.ItemCallback<Route>() {
    override fun areItemsTheSame(oldItem: Route, newItem: Route): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: Route, newItem: Route): Boolean {
        return oldItem.id == newItem.id
    }
}
