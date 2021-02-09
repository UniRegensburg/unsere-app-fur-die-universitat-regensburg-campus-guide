package de.ur.explure.utils

/**
 * Sealed class used as error handling util for Firebase and Firestore functions.
 *
 * @param R Return type
 */

sealed class FirebaseResult<out R> {

    /**
     * Represents a successful Firebase operation and its result.
     *
     * @param T Type [T] of the object which should be retrieved from the response's data
     * @property data Object of type [T] which represents the operations result.
     */

    data class Success<out T>(val data: T) : FirebaseResult<T>()

    /**
     * Represents an unsuccessful Firebase operation and its exception
     *
     * @property exception [Exception] which occurred during the task
     */

    data class Error(val exception: Exception) : FirebaseResult<Nothing>()

    /**
     * Represents a canceled Firebase operation and its exception
     *
     * @property exception [Exception] which occurred during the task
     */

    data class Canceled(val exception: Exception?) : FirebaseResult<Nothing>()

    override fun toString(): String {
        return when (this) {
            is Success<*> -> "Success[data=$data]"
            is Error -> "Error[exception=$exception]"
            is Canceled -> "Canceled[exception=$exception]"
        }
    }
}
