package de.ur.explure.utils

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

/**
 * * Taken from https://stackoverflow.com/questions/47854598/livedata-remove-observer-after-first-callback
 *
 * LiveData Extension that emits only once.
 * !Considered an anti-pattern as LiveData shouldn't be used as an event. Use an event wrapper or
 * SingleLiveEvent instead! See https://medium.com/androiddevelopers/livedata-with-snackbar-navigation-and-other-events-the-singleliveevent-case-ac2622673150
 */
fun <T> LiveData<T>.observeOnce(lifecycleOwner: LifecycleOwner, observer: Observer<T>) {
    observe(lifecycleOwner, object : Observer<T> {
        override fun onChanged(t: T?) {
            removeObserver(this)
            observer.onChanged(t)
        }
    })
}
