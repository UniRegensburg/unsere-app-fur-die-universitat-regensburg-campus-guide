package de.ur.explure.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import androidx.core.widget.ImageViewCompat
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.google.firebase.storage.FirebaseStorage
import com.hannesdorfmann.adapterdelegates4.AdapterDelegate
import com.hannesdorfmann.adapterdelegates4.ListDelegationAdapter
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import de.ur.explure.GlideApp
import de.ur.explure.R
import de.ur.explure.databinding.CategoryCardItemBinding
import de.ur.explure.model.category.Category
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

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

    private fun categoryDelegate(): AdapterDelegate<List<Category>> = adapterDelegateViewBinding(
        { layoutInflater, root -> CategoryCardItemBinding.inflate(layoutInflater, root, false) }
    ) {

        binding.categoryCardview.setOnClickListener { exportListener(item) }

        bind {
            try {
                val gsReference = fireStorage.getReferenceFromUrl(item.iconResource)
                GlideApp.with(itemView)
                    .load(gsReference)
                    .fitCenter()
                    .error(R.drawable.circular_background)
                    .transition(withCrossFade())
                    .into(binding.ivCategoryIcon)
            } catch (_: Exception) {
            }

            val color = try {
                Color.parseColor(item.color)
            } catch (_: Exception) {
                Color.WHITE
            }
            binding.categoryCardview.setCardBackgroundColor(color)

            ImageViewCompat.setImageTintList(
                binding.ivCategoryIcon,
                ColorStateList.valueOf(color)
            )

            binding.tvCategoryName.text = item.name
        }
    }
}
