package de.ur.explure.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.ur.explure.Interface.CommentInterface
import de.ur.explure.databinding.CommentItemBinding
import de.ur.explure.model.comment.Comment
import java.text.SimpleDateFormat

class CommentAdapter(private val listener: CommentInterface) :
        RecyclerView.Adapter<CommentAdapter.ViewHolder>() {

    private var commentList: MutableList<Comment> = mutableListOf()
    private var firstTimeLoading: Boolean = true
    private var firstLoading: Int = 0
    private val viewPool = RecyclerView.RecycledViewPool()
    private val dateFormat: SimpleDateFormat = SimpleDateFormat("dd. MMM yyyy  HH:mm")

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
        val answerInput = binding.answerInput
        val answerButton = binding.addAnswerButton
        val commentItem = binding.commentItem
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
        val date = dateFormat.format(currentItem.createdAt)
        holder.commentDate.text = date.toString()
        // checks if current comment has answer and if that's true enables show answers button
        if (currentItem.answers.isNotEmpty() && firstTimeLoading) {
            holder.showAnswers.visibility = View.VISIBLE
            firstLoading++
            if (firstLoading >= commentList.size && firstTimeLoading) {
                firstTimeLoading = false
            }
        }
        setOnClickListener(holder, position)
        setOnLongClickListener(holder, position)
        holder.answerButton.setOnClickListener {
            listener.addAnswer(commentList[position].id, holder.answerInput.text.toString())
            holder.answerInput.text.clear()
        }
    }

    private fun setOnClickListener(holder: ViewHolder, position: Int) {
        holder.showAnswers.setOnClickListener {
            holder.showAnswers.visibility = View.GONE
            holder.hideAnswers.visibility = View.VISIBLE
            holder.answerItem.visibility = View.VISIBLE
            initAdapter(holder, position)
            notifyDataSetChanged()
        }
        holder.hideAnswers.setOnClickListener {
            holder.hideAnswers.visibility = View.GONE
            holder.answerItem.visibility = View.GONE
            holder.showAnswers.visibility = View.VISIBLE
            notifyDataSetChanged()
        }
    }

    private fun setOnLongClickListener(holder: ViewHolder, position: Int) {
        holder.commentItem.setOnLongClickListener {
            holder.commentItem.isLongClickable = true
            listener.deleteComment(commentList[position].id)
            true
        }
    }

    private fun initAdapter(holder: ViewHolder, position: Int) {
        val layoutManager = LinearLayoutManager(holder.answerItem.context, RecyclerView.VERTICAL, false)
        layoutManager.initialPrefetchItemCount = commentList[position].answers.size
        val answerAdapter = AnswerAdapter { answerId ->
            listener.deleteAnswer(answerId, commentList[position].id)
        }
        holder.answerItem.layoutManager = layoutManager
        holder.answerItem.adapter = answerAdapter
        holder.answerItem.setRecycledViewPool(viewPool)
        answerAdapter.setItems(commentList[position].answers)
    }

    override fun getItemCount(): Int {
        return commentList.size
    }
}
