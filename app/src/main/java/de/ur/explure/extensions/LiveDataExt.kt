package de.ur.explure.extensions

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData

/**
 * // Taken from https://stackoverflow.com/questions/50599830/how-to-combine-two-live-data-one-after-the-other/52306675#52306675
 *
 * Can be used with two input liveData variables like this:
 * ```
 * val title = profile.combineWith(user) { profile, user ->
 *   "${profile.job} ${user.name}"
 * }
 * ```
 * The title-LiveData will then update its value when either the profile or user or both are updated!
 */
fun <T, K, R> LiveData<T>.combineWith(
    liveData: LiveData<K>,
    block: (T?, K?) -> R
): LiveData<R> {
    val result = MediatorLiveData<R>()
    result.addSource(this) {
        result.value = block(this.value, liveData.value)
    }
    result.addSource(liveData) {
        result.value = block(this.value, liveData.value)
    }
    return result
}
