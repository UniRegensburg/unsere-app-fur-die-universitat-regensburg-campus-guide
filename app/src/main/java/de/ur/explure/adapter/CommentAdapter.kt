package de.ur.explure.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import de.ur.explure.databinding.CommentItemBinding
import de.ur.explure.model.comment.Comment
import java.util.*

class CommentAdapter(private val dataSource: LinkedList<Comment>) :
        RecyclerView.Adapter<CommentAdapter.ViewHolder>() {

    // better solution?
    private var firstLoading: Boolean = true

    inner class ViewHolder(binding: CommentItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val commentAuthor = binding.commentAuthor
        val commentText = binding.commentText
        val commentDate = binding.commentDate
        val showAnswers = binding.showAnswers
        val hideAnswers = binding.hideAnswers
        val answerItem = binding.answerItem
        val answerAuthor = binding.answerAuthor
        val answerText = binding.answerText
        val answerDate = binding.answertDate
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
                CommentItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = dataSource[position]
        holder.commentAuthor.text = currentItem.authorId
        holder.commentText.text = currentItem.message
        holder.commentDate.text = currentItem.createdAt.toString()
        if (currentItem.answers.isNotEmpty() && firstLoading) {
            holder.showAnswers.visibility = View.VISIBLE
            firstLoading = false
        }
        setOnClickListener(holder, position)
    }

    private fun setOnClickListener(holder: ViewHolder, position: Int) {
        val currentItem = dataSource[position]
        holder.showAnswers.setOnClickListener {
            holder.showAnswers.visibility = View.GONE
            holder.hideAnswers.visibility = View.VISIBLE
            holder.answerItem.visibility = View.VISIBLE
            holder.answerAuthor.text = currentItem.answers.toString()
            notifyDataSetChanged()
        }
        holder.hideAnswers.setOnClickListener {
            holder.hideAnswers.visibility = View.GONE
            holder.answerItem.visibility = View.GONE
            holder.showAnswers.visibility = View.VISIBLE
            notifyDataSetChanged()
        }
    }

    override fun getItemCount(): Int {
        return dataSource.size
    }
}
