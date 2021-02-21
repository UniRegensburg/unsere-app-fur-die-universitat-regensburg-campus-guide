package de.ur.explure.views

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import de.ur.explure.R
import de.ur.explure.viewmodel.FavoriteRoutesFragmentViewModel
import kotlinx.android.synthetic.main.fragment_created_routes.*
import kotlinx.android.synthetic.main.fragment_favorite_routes.*
import kotlinx.android.synthetic.main.fragment_favorite_routes.userNameTextView
import org.koin.androidx.viewmodel.ext.android.viewModel

class FavoriteRoutesFragment : Fragment(R.layout.fragment_favorite_routes) {

    private val viewModel: FavoriteRoutesFragmentViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
}
