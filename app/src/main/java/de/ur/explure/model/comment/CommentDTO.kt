package de.ur.explure.model.comment

import android.os.Parcelable
import com.google.firebase.firestore.FieldValue
import de.ur.explure.config.CommentDocumentConfig.AUTHOR_ID_FIELD
import de.ur.explure.config.CommentDocumentConfig.DATE_FIELD
import de.ur.explure.config.CommentDocumentConfig.MESSAGE_FIELD
import de.ur.explure.config.CommentDocumentConfig.USER_NAME_FIELD
import kotlinx.parcelize.Parcelize

@Parcelize
data class CommentDTO(
    var message: String,
    var userName: String
) : Parcelable {

    fun toMap(userId: String): Map<String, Any> {
        return mapOf(
                MESSAGE_FIELD to message,
                USER_NAME_FIELD to userName,
                AUTHOR_ID_FIELD to userId,
                DATE_FIELD to FieldValue.serverTimestamp(),
        )
    }
}
