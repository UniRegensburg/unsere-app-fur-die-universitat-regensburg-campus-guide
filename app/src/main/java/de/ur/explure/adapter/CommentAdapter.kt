package de.ur.explure.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.ur.explure.R
import de.ur.explure.databinding.CommentItemBinding
import de.ur.explure.model.comment.Comment
import java.text.SimpleDateFormat

@SuppressLint("SimpleDateFormat")
class CommentAdapter(private val listener: CommentInterface) :
        RecyclerView.Adapter<CommentAdapter.ViewHolder>() {

    private var commentList: MutableList<Comment> = mutableListOf()
    private val viewPool = RecyclerView.RecycledViewPool()
    private val dateFormat: SimpleDateFormat = SimpleDateFormat("dd. MMM yyyy  HH:mm")

    fun setItems(comments: List<Comment>) {
        commentList = comments.toMutableList()
        sortComments()
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
        holder.commentAuthor.text = currentItem.userName
        holder.commentText.text = currentItem.message
        val date = dateFormat.format(currentItem.createdAt)
        holder.commentDate.text = date.toString()
        if (currentItem.answers.isNotEmpty()) {
            showAnswers(holder, position)
        }
        deleteComment(holder, position)
        addAnswer(holder, position)
    }

    private fun showAnswers(holder: ViewHolder, position: Int) {
        val answerCount = commentList[position].answers.size
        holder.showAnswers.visibility = View.VISIBLE
        holder.showAnswers.text = holder.itemView.context.getString(R.string.show_answers, answerCount)
        holder.showAnswers.setOnClickListener {
            holder.showAnswers.visibility = View.GONE
            holder.hideAnswers.visibility = View.VISIBLE
            holder.answerItem.visibility = View.VISIBLE
            initAdapter(holder, position)
        }
        holder.hideAnswers.setOnClickListener {
            holder.hideAnswers.visibility = View.GONE
            holder.answerItem.visibility = View.GONE
            holder.showAnswers.visibility = View.VISIBLE
        }
    }

    private fun deleteComment(holder: ViewHolder, position: Int) {
        holder.commentItem.setOnLongClickListener {
            holder.commentItem.isLongClickable = true
            listener.deleteComment(commentList[position].id)
            true
        }
    }

    // initialises AnswerAdapter to show answers to comment and sets listener to delete answers
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

    private fun addAnswer(holder: ViewHolder, position: Int) {
        holder.answerButton.setOnClickListener {
            listener.addAnswer(commentList[position].id, holder.answerInput.text.toString())
            holder.answerInput.text.clear()
        }
    }

    // sorts comments by date and time and puts newest comment on the top
    private fun sortComments() {
        commentList.sortWith { o1, o2 ->
                o2.createdAt.compareTo(o1.createdAt)
        }
    }

    override fun getItemCount(): Int {
        return commentList.size
    }
}
