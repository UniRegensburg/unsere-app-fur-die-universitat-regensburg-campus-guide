package de.ur.explure.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.FrameLayout
import android.widget.ListAdapter
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources

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

fun generateBitmap(context: Context, @DrawableRes drawableRes: Int): Bitmap? {
    val drawable: Drawable? = AppCompatResources.getDrawable(context, drawableRes)
    return getBitmapFromDrawable(drawable)
}

private fun getBitmapFromDrawable(drawable: Drawable?): Bitmap? {
    drawable ?: return null

    return if (drawable is BitmapDrawable) {
        drawable.bitmap
    } else {
        // width and height are equal for all assets since they are ovals.
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight())
        drawable.draw(canvas)
        bitmap
    }
}
