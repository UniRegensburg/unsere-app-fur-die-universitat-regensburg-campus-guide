package de.ur.explure.utils

import android.app.Activity
import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import com.google.android.material.snackbar.Snackbar
import de.ur.explure.R

const val SNACKBAR_MAX_LINES = 4

inline fun showSnackbar(
    context: Context,
    message: String,
    anchorView: View,
    length: Int = Snackbar.LENGTH_SHORT,
    @ColorRes colorRes: Int? = null,
    f: Snackbar.() -> Unit = {}
): Snackbar {
    val snackbar = Snackbar.make(anchorView, message, length)
    if (colorRes != null) {
        snackbar.view.setBackgroundColor(context.resources.getColor(colorRes, null))
    }

    // set the snackbar's textview max. lines size up to 4 (instead of 2)
    snackbar.view.findViewById<TextView>(R.id.snackbar_text).maxLines = SNACKBAR_MAX_LINES

    // invoke callback if any
    snackbar.f()
    snackbar.show()

    return snackbar
}

inline fun showSnackbar(
    context: Context,
    @StringRes messageRes: Int,
    anchorView: View,
    length: Int = Snackbar.LENGTH_SHORT,
    @ColorRes colorRes: Int? = null,
    f: Snackbar.() -> Unit = {}
): Snackbar {
    val message = context.resources.getString(messageRes)
    return showSnackbar(context, message, anchorView, length, colorRes, f)
}

inline fun showSnackbar(
    context: Activity,
    message: String,
    anchorViewId: Int = android.R.id.content,
    length: Int = Snackbar.LENGTH_SHORT,
    @ColorRes colorRes: Int? = null,
    f: Snackbar.() -> Unit = {}
): Snackbar? {
    val view = context.findViewById<View>(anchorViewId) ?: return null
    return showSnackbar(context, message, view, length, colorRes, f)
}

inline fun showSnackbar(
    context: Activity,
    @StringRes messageRes: Int,
    anchorViewId: Int = android.R.id.content,
    length: Int = Snackbar.LENGTH_SHORT,
    @ColorRes colorRes: Int? = null,
    f: Snackbar.() -> Unit = {}
): Snackbar? {
    val message = context.getString(messageRes)
    return showSnackbar(context, message, anchorViewId, length, colorRes, f)
}

/**
 * Can be used like this:
 *
 * ```
 * showSnackbar(...).withAction("Ok") {
 *      // do something if Ok button clicked }
 * }
 * ```
 */
fun Snackbar.withAction(action: String, color: Int? = null, listener: ((View) -> Unit)? = null) {
    val actionListener = listener ?: {}
    setAction(action, actionListener)
    color?.let { setActionTextColor(color) }
}
