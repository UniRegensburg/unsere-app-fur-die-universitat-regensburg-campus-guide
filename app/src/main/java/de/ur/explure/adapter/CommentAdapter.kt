package de.ur.explure.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import de.ur.explure.databinding.CommentItemBinding
import de.ur.explure.model.comment.Comment
import java.util.*

class CommentAdapter(private val listener: (String, String) -> Unit) :
        RecyclerView.Adapter<CommentAdapter.ViewHolder>() {

    private var firstLoading: Boolean = true
    private var commentList: MutableList<Comment> = mutableListOf()

    fun setItems(comments: List<Comment>) {
        commentList = comments.toMutableList()
        this.notifyDataSetChanged()
    }

    inner class ViewHolder(binding: CommentItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val commentAuthor = binding.commentAuthor
        val commentText = binding.commentText
        val commentDate = binding.commentDate
        val showAnswers = binding.showAnswers
        val hideAnswers = binding.hideAnswers
        val answerItem = binding.answerItem
        val answerAuthor = binding.answerAuthor
        val answerText = binding.answerText
        val answerDate = binding.answerDate
        val answerInput = binding.answerInput
        val answerButton = binding.addAnswerButton
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
                CommentItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = commentList[position]
        holder.commentAuthor.text = currentItem.authorId
        holder.commentText.text = currentItem.message
        holder.commentDate.text = currentItem.createdAt.toString()
        if (currentItem.answers.isNotEmpty() && firstLoading) {
            holder.showAnswers.visibility = View.VISIBLE
            firstLoading = false
        }
        setOnClickListener(holder, position)
        holder.answerButton.setOnClickListener {
            listener(holder.answerInput.toString(), commentList[position].id)
        }
    }

    private fun setOnClickListener(holder: ViewHolder, position: Int) {
        holder.showAnswers.setOnClickListener {
            holder.showAnswers.visibility = View.GONE
            holder.hideAnswers.visibility = View.VISIBLE
            holder.answerItem.visibility = View.VISIBLE
            setAnswers(holder, position)
            notifyDataSetChanged()
        }
        holder.hideAnswers.setOnClickListener {
            holder.hideAnswers.visibility = View.GONE
            holder.answerItem.visibility = View.GONE
            holder.showAnswers.visibility = View.VISIBLE
            notifyDataSetChanged()
        }
    }

    private fun setAnswers(holder: ViewHolder, position: Int) {
        val currentItem = commentList[position]
        for (i in 0 until currentItem.answers.size) {
            holder.answerAuthor.text = currentItem.answers[i].authorId
            holder.answerText.text = currentItem.answers[i].message
            holder.answerDate.text = currentItem.answers[i].createdAt.toString()
       }
    }

    override fun getItemCount(): Int {
        return commentList.size
    }
}
