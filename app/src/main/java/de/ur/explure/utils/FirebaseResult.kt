package de.ur.explure.utils

sealed class FirebaseResult<out R> {

    data class Success<out T>(val data: T) : FirebaseResult<T>()
    data class Error(val exception: Exception) : FirebaseResult<Nothing>()
    data class Canceled(val exception: Exception?) : FirebaseResult<Nothing>()

    override fun toString(): String {
        return when (this) {
            is Success<*> -> "Success[data=$data]"
            is Error -> "Error[exception=$exception]"
            is Canceled -> "Canceled[exception=$exception]"
        }
    }
}
