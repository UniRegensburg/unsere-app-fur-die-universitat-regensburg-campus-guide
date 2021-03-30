package de.ur.explure.`interface`

interface CommentInterface {

    fun addAnswer(commentId: String, answerText: String)

    fun deleteComment(commentId: String)

    fun deleteAnswer(answerId: String, commentId: String)
}
