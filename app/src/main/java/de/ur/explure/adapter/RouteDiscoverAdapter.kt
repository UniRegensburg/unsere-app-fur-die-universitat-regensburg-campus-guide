package de.ur.explure.adapter

import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.firebase.storage.FirebaseStorage
import com.hannesdorfmann.adapterdelegates4.AdapterDelegate
import com.hannesdorfmann.adapterdelegates4.ListDelegationAdapter
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateLayoutContainer
import de.ur.explure.GlideApp
import de.ur.explure.R
import de.ur.explure.model.route.Route
import kotlinx.android.synthetic.main.category_card_item.view.*
import kotlinx.android.synthetic.main.route_card_item.view.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.lang.Exception

class RouteDiscoverAdapter(private val clickListener: (Route) -> Unit) :
    ListDelegationAdapter<List<Route>>(), KoinComponent {
    private val fireStorage: FirebaseStorage by inject()

    init {
        delegatesManager.addDelegate(routeDelegate())
    }

    fun setList(items: List<Route>) {
        this.items = items
        this.notifyDataSetChanged()
    }

    private fun routeDelegate(): AdapterDelegate<List<Route>> {
        return adapterDelegateLayoutContainer(R.layout.route_card_item) {

            itemView.route_cardview.setOnClickListener { clickListener(item) }

            bind {
                if (item.thumbnailUrl.isNotEmpty()) {
                    try {
                        val gsReference = fireStorage.getReferenceFromUrl(item.thumbnailUrl)
                        GlideApp.with(itemView)
                            .load(gsReference)
                            .error(R.drawable.map_background)
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .into(itemView.iv_route_thumbnail)
                    } catch (_: Exception) {
                    }
                }

                itemView.tv_route_title.text = item.title
                itemView.tv_rating_count.text =
                    getString(R.string.route_item_rating, item.rating.size)
                itemView.tv_distance.text =
                    getString(R.string.route_item_distance, item.distance.toInt())
                itemView.tv_duration.text =
                    getString(R.string.route_item_duration, item.duration.toInt())
                itemView.tv_way_point_count.text = item.wayPointCount.toString()
                itemView.tv_comment.text = item.commentCount.toString()
                itemView.rating_bar_route.rating = item.currentRating.toFloat()
            }
        }
    }
}
