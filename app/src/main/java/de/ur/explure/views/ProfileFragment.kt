package de.ur.explure.views

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import de.ur.explure.R
import de.ur.explure.viewmodel.ProfileFragmentViewModel
import kotlinx.android.synthetic.main.fragment_profile.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private val viewModel: ProfileFragmentViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setOnClickListeners()

        observeUserModel()
        viewModel.getUserInfo()
    }

    private fun observeUserModel() {
        viewModel.user.observe(viewLifecycleOwner, { user ->
            if (user != null) {
                userNameTextView.text = user.name
            }
        })
    }

    private fun setOnClickListeners() {
        ownRoutesButton.setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.createdRoutesFragment)
        }

        favoriteRoutesButton.setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.favoriteRoutesFragment)
        }

        statisticsButton.setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.statisticsFragment)
        }

        logOutButton.setOnClickListener {
            Toast.makeText(activity, "Still to come!", Toast.LENGTH_SHORT).show()
        }
    }
}
