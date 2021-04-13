package de.ur.explure.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.firebase.storage.FirebaseStorage
import de.ur.explure.GlideApp
import de.ur.explure.R
import de.ur.explure.databinding.RouteElementBinding
import de.ur.explure.model.route.Route
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

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
        RecyclerView.ViewHolder(binding.root), KoinComponent {

        private val fireStorage: FirebaseStorage by inject()

        fun bind(
            currentRouteItem: Route,
            onClickListener: OnItemClickListener,
            onLongClickListener: OnItemLongClickListener
        ) {
            if (currentRouteItem.thumbnailUrl.isNotEmpty()) {
                try {
                    val gsReference = fireStorage.getReferenceFromUrl(currentRouteItem.thumbnailUrl)
                    GlideApp.with(itemView)
                        .load(gsReference)
                        .error(R.drawable.map_background)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(binding.ivRouteThumbnail)
                } catch (_: Exception) {
                }
            }

            binding.tvRouteTitle.text = currentRouteItem.title
            binding.tvRouteDescription.text = currentRouteItem.description
            binding.tvRatingCount.text = itemView.context.resources.getString(
                R.string.route_item_rating, currentRouteItem.rating.size)
            binding.tvDistance.text = itemView.context.resources.getString(
                R.string.route_item_distance, currentRouteItem.distance.toInt())
            binding.tvDuration.text = itemView.context.resources.getString(
                R.string.route_item_duration, currentRouteItem.duration.toInt())
            binding.tvWayPointCount.text = currentRouteItem.wayPointCount.toString()
            binding.tvComment.text = currentRouteItem.commentCount.toString()
            binding.ratingBarRoute.rating = currentRouteItem.currentRating.toFloat()

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
