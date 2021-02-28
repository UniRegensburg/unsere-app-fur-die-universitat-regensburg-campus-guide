@file:Suppress("MatchingDeclarationName")

package de.ur.explure.utils

import android.app.Activity
import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import com.google.android.material.snackbar.Snackbar
import de.ur.explure.R
import de.ur.explure.utils.SnackBarConstants.SNACKBAR_MAX_LINES
import de.ur.explure.utils.SnackBarConstants.defaultAnchorViewID
import de.ur.explure.utils.SnackBarConstants.defaultBackgroundColor

object SnackBarConstants {
    /**
     * ViewGroupId of the entire content area of an Activity, see
     * https://stackoverflow.com/questions/4486034/get-root-view-from-current-activity
     */
    const val defaultAnchorViewID = android.R.id.content
    const val defaultBackgroundColor = R.color.colorPrimary
    const val SNACKBAR_MAX_LINES = 4
}

inline fun showSnackbar(
    message: String,
    anchorView: View,
    length: Int = Snackbar.LENGTH_SHORT,
    @ColorRes colorRes: Int? = defaultBackgroundColor,
    f: Snackbar.() -> Unit = {}
): Snackbar {
    val snackbar = Snackbar.make(anchorView, message, length)
    snackbar.setBackgroundColor(colorRes)
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
    @ColorRes colorRes: Int? = defaultBackgroundColor,
    f: Snackbar.() -> Unit = {}
): Snackbar {
    val message = context.resources.getString(messageRes)
    return showSnackbar(message, anchorView, length, colorRes, f)
}

inline fun showSnackbar(
    context: Activity,
    message: String,
    anchorViewId: Int = defaultAnchorViewID,
    length: Int = Snackbar.LENGTH_SHORT,
    @ColorRes colorRes: Int? = defaultBackgroundColor,
    f: Snackbar.() -> Unit = {}
): Snackbar? {
    val view = context.findViewById<View>(anchorViewId) ?: return null
    return showSnackbar(message, view, length, colorRes, f)
}

inline fun showSnackbar(
    context: Activity,
    @StringRes messageRes: Int,
    anchorViewId: Int = defaultAnchorViewID,
    length: Int = Snackbar.LENGTH_SHORT,
    @ColorRes colorRes: Int? = defaultBackgroundColor,
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

fun Snackbar.withAction(
    @StringRes action: Int,
    color: Int? = null,
    listener: ((View) -> Unit)? = null
) {
    val actionListener = listener ?: {}
    setAction(action, actionListener)
    color?.let { setActionTextColor(color) }
}

fun Snackbar.setBackgroundColor(@ColorRes colorRes: Int? = defaultBackgroundColor) {
    if (colorRes != null) {
        view.setBackgroundColor(context.resources.getColor(colorRes, null))
    }
}

fun Snackbar.updateSnackbar(newText: String, @ColorRes colorRes: Int? = null) {
    setText(newText)
    colorRes?.let { setBackgroundColor(it) }
}

fun Snackbar.updateSnackbar(@StringRes newText: Int, @ColorRes colorRes: Int? = null) {
    setText(newText)
    colorRes?.let { setBackgroundColor(it) }
}
