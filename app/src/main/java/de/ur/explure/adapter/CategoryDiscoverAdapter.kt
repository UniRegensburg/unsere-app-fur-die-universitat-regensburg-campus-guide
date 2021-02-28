package de.ur.explure.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import androidx.core.widget.ImageViewCompat
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.google.firebase.storage.FirebaseStorage
import com.hannesdorfmann.adapterdelegates4.AdapterDelegate
import com.hannesdorfmann.adapterdelegates4.ListDelegationAdapter
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateLayoutContainer
import de.ur.explure.GlideApp
import de.ur.explure.R
import de.ur.explure.model.category.Category
import kotlinx.android.synthetic.main.category_card_item.view.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.lang.Exception

class CategoryDiscoverAdapter(private val exportListener: (Category) -> Unit) :
    ListDelegationAdapter<List<Category>>(), KoinComponent {

    private val fireStorage: FirebaseStorage by inject()

    init {
        delegatesManager.addDelegate(categoryDelegate())
    }

    fun setList(items: List<Category>) {
        this.items = items
        this.notifyDataSetChanged()
    }

    private fun categoryDelegate(): AdapterDelegate<List<Category>> {
        return adapterDelegateLayoutContainer(R.layout.category_card_item) {

            itemView.category_cardview.setOnClickListener { exportListener(item) }

            bind {
                val gsReference = fireStorage.getReferenceFromUrl(item.iconResource)
                GlideApp.with(itemView)
                    .load(gsReference)
                    .fitCenter()
                    .error(R.drawable.circular_background)
                    .transition(withCrossFade())
                    .into(itemView.iv_category_icon)

                val color = try {
                    Color.parseColor(item.color)
                } catch (_: Exception) {
                    Color.WHITE
                }

                itemView.category_cardview.setCardBackgroundColor(color)

                ImageViewCompat.setImageTintList(
                    itemView.iv_category_icon,
                    ColorStateList.valueOf(color)
                )

                itemView.tv_category_name.text = item.name
            }
        }
    }
}
