package de.ur.explure.views

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.crazylegend.viewbinding.viewBinding
import com.google.firebase.storage.FirebaseStorage
import de.ur.explure.GlideApp
import de.ur.explure.R
import de.ur.explure.databinding.FragmentStatisticsBinding
import de.ur.explure.viewmodel.StatisticsViewModel
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class StatisticsFragment : Fragment(R.layout.fragment_statistics) {

    private val binding by viewBinding(FragmentStatisticsBinding::bind)

    private val viewModel: StatisticsViewModel by viewModel()

    private var userId: String = ""

    private val fireStorage: FirebaseStorage by inject()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeUserModel()
        observeRouteModel()
        viewModel.getUserInfo()
        viewModel.getTraveledDistance()
        viewModel.getCreatedWaypoints()
    }

    private fun observeUserModel() {
        viewModel.user.observe(viewLifecycleOwner, { user ->
            if (user != null) {
                binding.userNameTextView.text = user.name
                userId = user.id
                if (user.profilePictureUrl.isNotEmpty()) {
                    try {
                        val gsReference =
                                fireStorage.getReferenceFromUrl(user.profilePictureUrl)
                        GlideApp.with(requireContext())
                                .load(gsReference)
                                .skipMemoryCache(true)
                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                .error(R.drawable.user_profile_picture)
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .into(binding.profilePicture)
                    } catch (_: Exception) {
                    }
                }
                binding.startedRoutesTextView.text = user.startedRoutes.toString()
                binding.endedRoutesTextView.text = user.endedRoutes.toString()
                binding.createdRoutesTextView.text = user.createdRoutes.size.toString()
                binding.createdCommentsTextView.text = user.commentCount.toString()
                binding.createdRatingsTextView.text = user.ratingCount.toString()
            }
        })
    }

    private fun observeRouteModel() {
        viewModel.traveledDistance.observe(viewLifecycleOwner, { distance ->
            if (distance != null) {
                binding.traveledDistanceTextView.text = distance
            }
        })
        viewModel.createdWaypoints.observe(viewLifecycleOwner, { waypoints ->
            if (waypoints != null) {
                binding.createdLandmarksTextView.text = waypoints
            }
        })
    }
}
