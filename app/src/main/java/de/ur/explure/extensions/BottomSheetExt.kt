package de.ur.explure.extensions

import android.view.View
import com.google.android.material.bottomsheet.BottomSheetBehavior

fun <T : View> BottomSheetBehavior<T>.initHidden() {
    state = BottomSheetBehavior.STATE_HIDDEN
    peekHeight = 0
}

fun <T : View> BottomSheetBehavior<T>.hide() {
    isHideable = true
    state = BottomSheetBehavior.STATE_HIDDEN
}

fun <T : View> BottomSheetBehavior<T>.show(onStateChangedCallback: ((state: Int) -> Unit)? = null) {
    state = BottomSheetBehavior.STATE_HALF_EXPANDED

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
