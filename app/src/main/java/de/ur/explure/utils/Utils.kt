package de.ur.explure.utils

import android.content.Context
import android.graphics.Bitmap
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ListAdapter
import androidx.fragment.app.Fragment
import de.ur.explure.R
import timber.log.Timber
import java.nio.ByteBuffer

/**
 * Returns the width of the longest item in a list.
 * Useful for dynamically adjusting the min width of a dropdown list, for example.
 *
 * Taken from https://medium.com/bugless/stylised-listpopupwindow-in-android-9cb453d42b
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

/**
 * This slides a given view out to the right.
 */
fun Fragment.slideOutView(v: View) {
    val slideOutAnim: Animation =
        AnimationUtils.loadAnimation(requireActivity(), R.anim.slide_out_to_right)
    v.startAnimation(slideOutAnim)

    // https://stackoverflow.com/questions/4728908/android-view-with-view-gone-still-receives-ontouch-and-onclick
    v.visibility = View.GONE
    v.clearAnimation()
}

/**
 * This slides a given view in from the right.
 */
fun Fragment.slideInView(v: View) {
    val slideInAnim: Animation =
        AnimationUtils.loadAnimation(requireActivity(), R.anim.slide_in_from_right)
    v.startAnimation(slideInAnim)
    v.visibility = View.VISIBLE
    v.clearAnimation()
}

/**
 * Convert bitmap to byte array using ByteBuffer.
 */
fun Bitmap.convertToByteArray(): ByteArray {
    // minimum number of bytes that can be used to store this bitmap's pixels
    val size = this.byteCount

    // allocate new instances which will hold bitmap
    val buffer = ByteBuffer.allocate(size)
    val bytes = ByteArray(size)

    // copy the bitmap's pixels into the specified buffer
    this.copyPixelsToBuffer(buffer)

    // rewinds buffer (buffer position is set to zero and the mark is discarded)
    buffer.rewind()

    // transfer bytes from buffer into the given destination array
    buffer.get(bytes)

    // return bitmap's pixels
    return bytes
}

/**
 * Util-Function-Wrapper to measure execution time of a function in milliseconds.
 *
 * Can be called like this:
 * ```
 * val result = measureTimeFor("uploading route snapshot") {
 *      mapViewModel.uploadRouteSnapshot(routeBitmap)
 * }
 * ```
 * or with a proper function:
 * ```
 * val result = measureTimeFor("uploading route snapshot", function = uploadSnapshot())
 * ```
 */
inline fun <T> measureTimeFor(tag: String = "Execution of function", function: () -> T): T {
    val startTime = System.currentTimeMillis()
    val result: T = function.invoke()
    Timber.d("$tag took ${System.currentTimeMillis() - startTime} ms.")
    return result
}
