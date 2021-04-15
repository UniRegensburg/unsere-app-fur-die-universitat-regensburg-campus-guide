package de.ur.explure.extensions

import android.widget.TextView

fun TextView.isEllipsized(): Boolean {
    val textPixelLength = paint.measureText(text.toString())
    val numberOfLines = kotlin.math.ceil((textPixelLength / measuredWidth).toDouble())
    return lineHeight * numberOfLines > measuredHeight
}
