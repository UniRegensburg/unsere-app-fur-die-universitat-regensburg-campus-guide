package de.ur.explure.extensions

import android.view.View
import com.google.android.material.bottomsheet.BottomSheetBehavior

fun BottomSheetBehavior<View>.initHidden() {
    state = BottomSheetBehavior.STATE_HIDDEN
    peekHeight = 0
}

fun BottomSheetBehavior<View>.hide() {
    isHideable = true
    state = BottomSheetBehavior.STATE_HIDDEN
}

fun BottomSheetBehavior<View>.show(onStateChangedCallback: ((state: Int) -> Unit)? = null) {
    state = BottomSheetBehavior.STATE_EXPANDED

    onStateChangedCallback?.let { callback ->
        addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                callback(newState)
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // not used
            }
        })
    }
}

/**
 * e.g. `bottomSheet.showCompletely(height = bottomSheet.height)`
 */
fun BottomSheetBehavior<View>.showCompletely(height: Int) {
    peekHeight = height
    state = BottomSheetBehavior.STATE_EXPANDED
    isHideable = false
}
