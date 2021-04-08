package de.ur.explure.views

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import com.crazylegend.viewbinding.viewBinding
import de.ur.explure.R
import de.ur.explure.databinding.FragmentUserDataBinding

class UserDataFragment : Fragment(R.layout.fragment_user_data) {

    private val binding by viewBinding(FragmentUserDataBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setImage()
        setUsername()
        setOnClickListener()
    }

    private fun setUsername() {
    }

    private fun setImage() {
    }

    private fun setOnClickListener() {
    }
}
