package de.ur.explure.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import de.ur.explure.R
import de.ur.explure.model.waypoint.WayPoint
import kotlinx.android.synthetic.main.image_item.view.*
import java.util.*

class ImageAdapter(private val context: Context, private val dataSource: LinkedList<WayPoint>) : PagerAdapter() {

    override fun getCount(): Int {
        return dataSource.size
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view.equals(`object`)
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val layoutInflater = LayoutInflater.from(context)
        val view = layoutInflater.inflate(R.layout.image_item, container, false)
        //val currentItem = dataSource[position]
        // view.singleImage.setImageResource(currentItem)
        container.addView(view)
        return view
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }
}
