package de.ur.explure.views

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import de.ur.explure.R
import de.ur.explure.repository.user.UserRepositoryImpl
import de.ur.explure.services.FireStoreInstance
import de.ur.explure.services.FirebaseAuthService
import de.ur.explure.viewmodel.FavoriteRoutesFragmentViewModel
import kotlinx.android.synthetic.main.fragment_favorite_routes.*

class FavoriteRoutesFragment : Fragment(R.layout.fragment_favorite_routes) {

    private val viewModel =
        FavoriteRoutesFragmentViewModel(
            UserRepositoryImpl(
                FirebaseAuthService(FirebaseAuth.getInstance()),
                FireStoreInstance(FirebaseFirestore.getInstance())
            )
        )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.setUserName(userNameTextView)
        viewModel.setProfilePicture(profilePicture)
    }
}
