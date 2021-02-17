package de.ur.explure.model.comment

import android.os.Parcelable
import com.google.firebase.firestore.DocumentId
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class Comment(
    @DocumentId
    val id: String = "",
    val message: String = "",
    val createdAt: Date = Date(),
    val authorId: String = "",
    val answers: LinkedList<Comment> = LinkedList()
) : Parcelable {

    fun fillAnswers(answerList: List<Comment>) {
        answers.addAll(answerList)
    }
}
