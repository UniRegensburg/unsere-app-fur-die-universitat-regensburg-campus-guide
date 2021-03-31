package de.ur.explure.Interface

interface CommentInterface {

    fun addAnswer(commentId: String, answerText: String)

    fun deleteComment(commentId: String)

    fun deleteAnswer(answerId: String, commentId: String)
}
