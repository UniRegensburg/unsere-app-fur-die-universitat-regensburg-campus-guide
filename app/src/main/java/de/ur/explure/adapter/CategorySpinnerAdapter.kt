package de.ur.explure.adapter

import android.content.Context
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.firebase.storage.FirebaseStorage
import de.ur.explure.GlideApp
import de.ur.explure.R
import de.ur.explure.databinding.SpinnerCategoryItemBinding
import de.ur.explure.model.category.Category
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class CategorySpinnerAdapter(context: Context, categories: List<Category> = mutableListOf()) :
    ArrayAdapter<Category>(context, 0, categories), KoinComponent {

    private val fireStorage: FirebaseStorage by inject()

    private val layoutInflater: LayoutInflater = LayoutInflater.from(context)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View =
            convertView ?: layoutInflater.inflate(R.layout.spinner_category_item, parent, false)
        getItem(position)?.let { category ->
            setItemForCategory(view, category)
        }
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        if (position == 0) {
            view = layoutInflater.inflate(R.layout.spinner_category_header, parent, false)
            view.setOnClickListener {
                val root = parent.rootView
                root.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK))
                root.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK))
            }
        } else {
            view = layoutInflater.inflate(R.layout.spinner_category_item, parent, false)
            getItem(position)?.let { category ->
                setItemForCategory(view, category)
            }
        }
        return view
    }

    override fun getItem(position: Int): Category? {
        if (position == 0) {
            return null
        }
        return super.getItem(position - 1)
    }

    override fun getCount() = super.getCount() + 1

    private fun setItemForCategory(view: View, category: Category) {
        val binding = SpinnerCategoryItemBinding.bind(view)
        try {
            val gsReference = fireStorage.getReferenceFromUrl(category.iconResource)
            GlideApp.with(binding.root)
                .load(gsReference)
                .fitCenter()
                .error(R.drawable.circular_background)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(binding.ivCategoryIcon)
        } catch (_: Exception) {
        }

        binding.tvCategory.text = category.name
    }
}
