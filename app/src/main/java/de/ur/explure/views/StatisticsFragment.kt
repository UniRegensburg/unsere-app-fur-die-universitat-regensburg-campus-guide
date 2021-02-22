package de.ur.explure.views

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import de.ur.explure.R
import de.ur.explure.viewmodel.StatisticsFragmentViewModel
import kotlinx.android.synthetic.main.fragment_favorite_routes.*
import kotlinx.android.synthetic.main.fragment_statistics.*
import kotlinx.android.synthetic.main.fragment_statistics.userNameTextView
import org.koin.androidx.viewmodel.ext.android.viewModel

class StatisticsFragment : Fragment(R.layout.fragment_statistics) {

    private val viewModel: StatisticsFragmentViewModel by viewModel()

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
                userNameTextView.text = user.name
                endedRoutesTextView.text = user.finishedRoutes.size.toString()
                createdRoutesTextView.text = user.createdRoutes.size.toString()
            }
        })
    }

    private fun observeRouteModel() {
        viewModel.traveledDistance.observe(viewLifecycleOwner, { distance ->
            if (distance != null) {
                traveledDistanceTextView.text = distance
            }
        })
        viewModel.createdWaypoints.observe(viewLifecycleOwner, { waypoints ->
            if (waypoints != null) {
                createdLandmarksTextView.text = waypoints
            }
        })
    }
}
