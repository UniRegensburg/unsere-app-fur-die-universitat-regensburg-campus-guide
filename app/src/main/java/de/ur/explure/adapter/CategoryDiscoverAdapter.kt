package de.ur.explure.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import com.hannesdorfmann.adapterdelegates4.AdapterDelegate
import com.hannesdorfmann.adapterdelegates4.ListDelegationAdapter
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateLayoutContainer
import de.ur.explure.R
import de.ur.explure.model.category.Category
import kotlinx.android.synthetic.main.category_card_item.view.*
import java.lang.Exception

class CategoryDiscoverAdapter(private val exportListener: (Category) -> Unit) :
    ListDelegationAdapter<List<Category>>() {
    init {
        delegatesManager.addDelegate(categoryDelegate())
    }

    fun setList(items: List<Category>) {
        this.items = items
        this.notifyDataSetChanged()
    }

    private fun categoryDelegate(): AdapterDelegate<List<Category>> {
        return adapterDelegateLayoutContainer(R.layout.category_card_item) {

            itemView.cl_category_card.setOnClickListener { exportListener(item) }

            bind {
                val color = try {
                    Color.parseColor(item.color)
                } catch (e: Exception) {
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