package de.ur.explure.views

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import de.ur.explure.R
import de.ur.explure.repository.user.UserRepositoryImpl
import de.ur.explure.services.FireStoreInstance
import de.ur.explure.services.FirebaseAuthService
import de.ur.explure.viewmodel.ProfileFragmentViewModel
import kotlinx.android.synthetic.main.fragment_profile.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private val viewModel =
        ProfileFragmentViewModel(UserRepositoryImpl(FirebaseAuthService(FirebaseAuth.getInstance()),
        FireStoreInstance(FirebaseFirestore.getInstance())))

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

        viewModel.setUserName(userNameTextView)
        viewModel.setProfilePicture(profilePicture)
    }
}
