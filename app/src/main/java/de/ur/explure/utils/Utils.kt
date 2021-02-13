package de.ur.explure.utils

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import android.widget.ListAdapter

/**
 * Returns the width of the longest item in a list.
 * * Taken from https://medium.com/bugless/stylised-listpopupwindow-in-android-9cb453d42b
 */
fun measureContentWidth(context: Context, adapter: ListAdapter): Int {
    val measureParentViewGroup = FrameLayout(context)
    var itemView: View? = null

    var maxWidth = 0
    var itemType = 0

    val widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
    val heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)

    for (index in 0 until adapter.count) {
        val positionType = adapter.getItemViewType(index)
        if (positionType != itemType) {
            itemType = positionType
            itemView = null
        }
        itemView = adapter.getView(index, itemView, measureParentViewGroup)
        itemView.measure(widthMeasureSpec, heightMeasureSpec)
        val itemWidth = itemView.measuredWidth
        if (itemWidth > maxWidth) {
            maxWidth = itemWidth
        }
    }
    return maxWidth
}
