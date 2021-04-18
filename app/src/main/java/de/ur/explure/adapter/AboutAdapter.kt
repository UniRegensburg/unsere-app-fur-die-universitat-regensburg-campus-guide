package de.ur.explure.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import de.ur.explure.databinding.AboutListItemBinding
import de.ur.explure.model.AttributionItem

class AboutAdapter : RecyclerView.Adapter<AboutAdapter.ViewHolder>() {

    var attributionList = listOf<AttributionItem>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

    override fun getItemCount() = attributionList.size

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        viewHolder.bind(attributionList[position])
    }

    class ViewHolder private constructor(private val binding: AboutListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(data: AttributionItem) {
            binding.aboutImage.setImageResource(data.imageResource)
            binding.aboutCaption.text = data.attribution
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val binding =
                    AboutListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                return ViewHolder(binding)
            }
        }
    }
}
