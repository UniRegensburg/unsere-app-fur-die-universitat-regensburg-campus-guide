package de.ur.explure.model.comment

import android.os.Parcelable
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ServerTimestamp
import de.ur.explure.config.CommentDocumentConfig.ANSWERS_FIELD
import de.ur.explure.config.CommentDocumentConfig.AUTHOR_ID_FIELD
import de.ur.explure.config.CommentDocumentConfig.DATE_FIELD
import de.ur.explure.config.CommentDocumentConfig.MESSAGE_FIELD
import java.util.Date
import kotlinx.android.parcel.Parcelize

@Parcelize
data class CommentDTO(
    var message: String,
) : Parcelable {

    fun toMap(userId: String): Map<String, Any>{
        return mapOf(
            MESSAGE_FIELD to message,
            AUTHOR_ID_FIELD to userId,
            DATE_FIELD to FieldValue.serverTimestamp(),
        )
    }
}
