package de.ur.explure.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import de.ur.explure.R
import de.ur.explure.databinding.AnswerItemBinding
import de.ur.explure.model.comment.Comment
import de.ur.explure.services.FirebaseAuthService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.text.SimpleDateFormat

@SuppressLint("SimpleDateFormat")
class AnswerAdapter(private val listener: (String) -> Unit) :
    RecyclerView.Adapter<AnswerAdapter.ViewHolder>(), KoinComponent {

    private val authService: FirebaseAuthService by inject()

    private var answerList: MutableList<Comment> = mutableListOf()
    private val dateFormat: SimpleDateFormat = SimpleDateFormat("dd. MMM yyyy  HH:mm")

    fun setItems(answers: List<Comment>) {
        answerList = answers.toMutableList()
        sortAnswers()
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
        holder.answerAuthor.text = currentItem.userName
        holder.answerText.text = currentItem.message
        val date = dateFormat.format(currentItem.createdAt)
        holder.answerDate.text = date.toString()
        deleteAnswer(holder, position)
    }

    private fun deleteAnswer(holder: ViewHolder, position: Int) {
        holder.answerItem.setOnLongClickListener {
            if (authService.getCurrentUserId() != answerList[position].authorId) {
                // only the creator of this answer is allowed to delete it !!
                Toast.makeText(holder.itemView.context, R.string.not_answer_creator, Toast.LENGTH_SHORT).show()
                return@setOnLongClickListener true
            }

            listener(answerList[position].id)
            return@setOnLongClickListener true
        }
    }

    // sorts answers by date and time and puts newest answer on the top
    private fun sortAnswers() {
        answerList.sortWith { o1, o2 ->
            o2.createdAt.compareTo(o1.createdAt)
        }
    }

    override fun getItemCount(): Int {
        return answerList.size
    }
}
