package de.ur.explure.config

import de.ur.explure.utils.FirebaseResult
import java.lang.Exception

object ErrorConfig {

    private const val NO_USER_MESSAGE = "User is not logged in"
    private val NO_USER_ERROR = Exception(NO_USER_MESSAGE)
    val NO_USER_RESULT = FirebaseResult.Error(NO_USER_ERROR)
}
