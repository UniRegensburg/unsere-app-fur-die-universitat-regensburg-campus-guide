package de.ur.explure.adapter

import androidx.recyclerview.widget.DiffUtil
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.firebase.storage.FirebaseStorage
import com.hannesdorfmann.adapterdelegates4.AdapterDelegate
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import de.ur.explure.GlideApp
import de.ur.explure.R
import de.ur.explure.databinding.RouteCardItemBinding
import de.ur.explure.model.route.Route
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class RouteDiscoverAdapter(private val clickListener: (Route) -> Unit) :
    AsyncListDifferDelegationAdapter<Route>(RouteDiffCallback), KoinComponent {

    private val fireStorage: FirebaseStorage by inject()

    init {
        delegatesManager.addDelegate(routeDelegate())
    }

    private fun routeDelegate(): AdapterDelegate<List<Route>> =
        adapterDelegateViewBinding(
            { layoutInflater, root -> RouteCardItemBinding.inflate(layoutInflater, root, false) }
        ) {

            // Save the original elevation of the layout as otherwise the elevation would
            // decrease on every click and not just the first.
            // This is run once in onCreateViewHolder and not everytime onBindViewHolder is called!
            val originalElevation = binding.root.cardElevation

            binding.routeCardview.setOnClickListener {
                // reduce the cardview's elevation by half to visualize the click better
                binding.root.cardElevation = originalElevation / 2
                clickListener(item)
            }

            bind {
                if (item.thumbnailUrl.isNotEmpty()) {
                    try {
                        val gsReference = fireStorage.getReferenceFromUrl(item.thumbnailUrl)
                        GlideApp.with(itemView)
                            .load(gsReference)
                            .error(R.drawable.map_background)
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .into(binding.ivRouteThumbnail)
                    } catch (_: Exception) {
                    }
                }

                binding.tvRouteTitle.text = item.title
                binding.tvRatingCount.text = getString(R.string.route_item_rating, item.rating.size)
                binding.tvDistance.text =
                    getString(R.string.route_item_distance, item.distance.toInt())
                binding.tvDuration.text =
                    getString(R.string.route_item_duration, item.duration.toInt())
                binding.tvWayPointCount.text = item.wayPointCount.toString()
                binding.tvComment.text = item.commentCount.toString()
                binding.ratingBarRoute.rating = item.currentRating.toFloat()
            }
        }
}

// diffUtil for better performance and default animations
object RouteDiffCallback : DiffUtil.ItemCallback<Route>() {
    override fun areItemsTheSame(oldItem: Route, newItem: Route): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: Route, newItem: Route): Boolean {
        return oldItem.id == newItem.id
    }
}
