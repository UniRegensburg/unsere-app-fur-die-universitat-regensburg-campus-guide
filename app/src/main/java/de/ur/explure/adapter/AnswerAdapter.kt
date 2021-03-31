package de.ur.explure.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import de.ur.explure.databinding.AnswerItemBinding
import de.ur.explure.model.comment.Comment
import java.text.SimpleDateFormat

class AnswerAdapter(private val listener: (String) -> Unit) : RecyclerView.Adapter<AnswerAdapter.ViewHolder>() {

    private var answerList: MutableList<Comment> = mutableListOf()
    private val dateFormat: SimpleDateFormat = SimpleDateFormat("dd. MMM yyyy  HH:mm")

    fun setItems(answers: List<Comment>) {
        answerList = answers.toMutableList()
        this.notifyDataSetChanged()
    }

    inner class ViewHolder(binding: AnswerItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val answerAuthor = binding.answerAuthor
        val answerText = binding.answerText
        val answerDate = binding.answerDate
        val answerItem = binding.answer
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
                AnswerItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = answerList[position]
        holder.answerAuthor.text = currentItem.authorId
        holder.answerText.text = currentItem.message
        val date = dateFormat.format(currentItem.createdAt)
        holder.answerDate.text = date.toString()
        holder.answerItem.setOnLongClickListener {
            listener(answerList[position].id)
            return@setOnLongClickListener true
        }
    }

    override fun getItemCount(): Int {
        return answerList.size
    }
}
