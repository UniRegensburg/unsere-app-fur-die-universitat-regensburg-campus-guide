package de.ur.explure.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import de.ur.explure.databinding.AnswerItemBinding
import de.ur.explure.model.comment.Comment

class AnswerAdapter : RecyclerView.Adapter<AnswerAdapter.ViewHolder>() {
    private var answerList: MutableList<Comment> = mutableListOf()

    fun setItems(answers: List<Comment>) {
        answerList = answers.toMutableList()
        this.notifyDataSetChanged()
    }

    inner class ViewHolder(binding: AnswerItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val answerAuthor = binding.answerAuthor
        val answerText = binding.answerText
        val answerDate = binding.answerDate
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
                AnswerItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = answerList[position]
        for (i in 0 until answerList.size) {
            holder.answerAuthor.text = currentItem.authorId
            holder.answerText.text = currentItem.message
            holder.answerDate.text = currentItem.createdAt.toString()
        }
    }

    override fun getItemCount(): Int {
        return answerList.size
    }
}