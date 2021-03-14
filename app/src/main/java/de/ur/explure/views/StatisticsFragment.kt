package de.ur.explure.views

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.crazylegend.viewbinding.viewBinding
import de.ur.explure.R
import de.ur.explure.databinding.FragmentStatisticsBinding
import de.ur.explure.viewmodel.StatisticsViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class StatisticsFragment : Fragment(R.layout.fragment_statistics) {

    private val binding by viewBinding(FragmentStatisticsBinding::bind)

    private val viewModel: StatisticsViewModel by viewModel()

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
                binding.endedRoutesTextView.text = user.finishedRoutes.size.toString()
                binding.createdRoutesTextView.text = user.createdRoutes.size.toString()
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
