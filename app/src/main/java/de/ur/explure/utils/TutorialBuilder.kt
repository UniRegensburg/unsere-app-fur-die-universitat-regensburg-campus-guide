package de.ur.explure.utils

import android.app.Activity
import android.graphics.Rect
import android.util.DisplayMetrics
import android.view.View
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetSequence
import com.getkeepsafe.taptargetview.TapTargetView
import de.ur.explure.R
import de.ur.explure.utils.Highlight.Companion.HIGHLIGHT_RADIUS_LARGE
import de.ur.explure.utils.Highlight.Companion.OUTER_CIRCLE_ALPHA

// TODO listener on the tap target are not yet supported right now
data class Highlight(
    val viewTarget: View,
    val title: String,
    val description: String = "",
    val highlightID: Int = generateId(),
    val iconId: Int? = null,
    val isCancelable: Boolean = true,
    val radius: Int = HIGHLIGHT_RADIUS_SMALL
) {
    companion object {
        // default values for the tutorial highlighting
        const val OUTER_CIRCLE_ALPHA = 0.8f
        const val HIGHLIGHT_RADIUS_SMALL = 60 // in dp
        const val HIGHLIGHT_RADIUS_LARGE = 100

        private var idCounter = 1

        fun generateId(): Int {
            return idCounter++
        }
    }
}

object TutorialBuilder {

    /**
     * Can be used to highlight interesting views on the screen.
     */
    fun showTutorialFor(activityContext: Activity, vararg highlightViews: Highlight) {
        when (highlightViews.size) {
            0 -> {
                // no highlight location was given
                return
            }
            1 -> {
                val highlightTarget = createHighlightView(highlightViews[0])
                TapTargetView.showFor(activityContext, highlightTarget)
            }
            else -> {
                createHighlightViewSequence(activityContext, highlightViews)
            }
        }
    }

    // TODO use the highlight class here as well, probably subclass with bounds instead of view
    /**
     * Create a highlight at the given bounds.
     * If no bounds are given it is shown at the center of the screen.
     *
     * **This can be used to show a custom highlight in a tutorial sequence with other views.**
     */
    fun createCustomHighlightView(
        activityContext: Activity,
        title: String,
        description: String = "",
        @DrawableRes iconId: Int = R.drawable.mapbox_marker_icon_default,
        bounds: Rect? = null
    ): TapTarget? {
        val markerIcon = ContextCompat.getDrawable(activityContext, iconId) ?: return null

        val targetBounds = if (bounds != null) {
            bounds
        } else {
            // setup a custom tap target at the center of the screen with its size based on the given icon
            val displayMetrics = DisplayMetrics()
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                activityContext.display?.getRealMetrics(displayMetrics)
            } else {
                @Suppress("DEPRECATION")
                activityContext.windowManager.defaultDisplay.getRealMetrics(displayMetrics)
            }
            val screenWidth = displayMetrics.widthPixels
            val screenHeight = displayMetrics.heightPixels
            val markerTarget = Rect(0, 0, markerIcon.intrinsicWidth, markerIcon.intrinsicHeight)
            markerTarget.offset(screenWidth / 2, screenHeight / 2) // center the target
            markerTarget
        }

        return TapTarget.forBounds(targetBounds, title, description)
            .cancelable(true)
            .transparentTarget(true)
            .targetRadius(HIGHLIGHT_RADIUS_LARGE)
            .outerCircleAlpha(OUTER_CIRCLE_ALPHA)
            .icon(markerIcon)
    }

    /**
     * Create and show a highlight at the given bounds.
     * If no bounds are given it is shown at the center of the screen.
     */
    fun showCustomHighlightView(
        activityContext: Activity,
        title: String,
        description: String = "",
        @DrawableRes iconId: Int = R.drawable.mapbox_marker_icon_default,
        bounds: Rect? = null
    ) {
        val customTarget = createCustomHighlightView(
            activityContext,
            title,
            description,
            iconId,
            bounds
        )
        TapTargetView.showFor(activityContext, customTarget)
    }

    private fun createHighlightView(highlight: Highlight): TapTarget? {
        return TapTarget.forView(highlight.viewTarget, highlight.title, highlight.description)
            .id(highlight.highlightID)
            .cancelable(highlight.isCancelable)
            .targetRadius(highlight.radius)
            .outerCircleAlpha(OUTER_CIRCLE_ALPHA)
            .transparentTarget(true)
    }

    private fun createHighlightViewSequence(
        activityContext: Activity,
        targetHighlights: Array<out Highlight>
    ) {
        TapTargetSequence(activityContext)
            .targets(
                targetHighlights.map {
                    createHighlightView(it)
                }
            )
            .start()
    }
}
