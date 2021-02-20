package de.ur.explure.views

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import de.ur.explure.R
import de.ur.explure.repository.rating.RatingRepositoryImpl
import de.ur.explure.repository.user.UserRepositoryImpl
import de.ur.explure.services.FireStoreInstance
import de.ur.explure.services.FirebaseAuthService
import de.ur.explure.viewmodel.StatisticsFragmentViewModel
import kotlinx.android.synthetic.main.fragment_statistics.*

class StatisticsFragment : Fragment(R.layout.fragment_statistics) {

    private val viewModel =
        StatisticsFragmentViewModel(
            UserRepositoryImpl(FirebaseAuthService(FirebaseAuth.getInstance()),
            FireStoreInstance(FirebaseFirestore.getInstance())),
            RatingRepositoryImpl(FirebaseAuthService(FirebaseAuth.getInstance()),
            FireStoreInstance(FirebaseFirestore.getInstance()))
        )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.setUserName(userNameTextView)
        viewModel.setProfilePicture(profilePicture)

        viewModel.setDistanceStatistics(traveledDistanceTextView,
                                        startedRoutesTextView,
                                        endedRoutesTextView)

        viewModel.setContentStatistics(createdRoutesTextView, createdLandmarksTextView)

        viewModel.setInteractionStatistics(createdCommentsTextView, createdRatingsTextView)
    }
}
