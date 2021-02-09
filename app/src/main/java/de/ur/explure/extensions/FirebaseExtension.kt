package de.ur.explure.extensions

import com.google.android.gms.tasks.Task
import de.ur.explure.utils.FirebaseResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun <T> Task<T>.await(): FirebaseResult<T> {
    if (isComplete) {
        val e = exception
        return if (e == null) {
            if (isCanceled) {
                FirebaseResult.Canceled(CancellationException("Task $this was cancelled normally."))
            } else {
                @Suppress("UNCHECKED_CAST")
                FirebaseResult.Success(result as T)
            }
        } else {
            FirebaseResult.Error(e)
        }
    }

    return suspendCancellableCoroutine { cont ->
        addOnCompleteListener {
            val e = exception
            if (e == null) {
                @Suppress("UNCHECKED_CAST")
                if (isCanceled) {
                    cont.cancel()
                } else {
                    cont.resume(FirebaseResult.Success(result as T))
                }
            } else {
                cont.resumeWithException(e)
            }
        }
    }
}