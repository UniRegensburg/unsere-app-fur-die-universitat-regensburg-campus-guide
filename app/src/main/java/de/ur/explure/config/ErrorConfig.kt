package de.ur.explure.config

import de.ur.explure.utils.FirebaseResult

/**
 * Object holding constants for error handling
 */

object ErrorConfig {

    // Used when no user id could be retrieved
    private const val NO_USER_MESSAGE = "User is not logged in"
    private val NO_USER_ERROR = Exception(NO_USER_MESSAGE)
    val NO_USER_RESULT = FirebaseResult.Error(NO_USER_ERROR)

    // Used when parsing of returned data failed
    private const val DESERIALIZATION_FAILED_MESSAGE = "Firebase Response could not be deserialized"
    private val DESERIALIZATION_FAILED_ERROR = Exception(DESERIALIZATION_FAILED_MESSAGE)
    val DESERIALIZATION_FAILED_RESULT = FirebaseResult.Error(DESERIALIZATION_FAILED_ERROR)
}
