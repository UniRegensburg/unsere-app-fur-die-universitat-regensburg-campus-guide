package de.ur.explure.views

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import de.ur.explure.R
import de.ur.explure.viewmodel.SingleRouteViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlinx.android.synthetic.main.fragment_single_route.*

class SingleRouteFragment : Fragment(R.layout.fragment_single_route) {

    private val singleRouteViewModel: SingleRouteViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setRouteInformation()
        setOnClickListener()
    }

    private fun setRouteInformation() {
        singleRouteViewModel.setRouteName(routeName)
        singleRouteViewModel.setRouteInformation(routeInformation)
        singleRouteViewModel.setWaypoints(waypoints)
        singleRouteViewModel.setRouteDuration(routeDuration)
        singleRouteViewModel.setRouteDistance(routeDistance)
    }

    private fun setOnClickListener() {
        mapButton.setOnClickListener {
            viewFlipper.displayedChild = 0
        }
        waypointsButton.setOnClickListener {
            viewFlipper.displayedChild = 1
        }
        pictureButton.setOnClickListener {
            viewFlipper.displayedChild = 2
        }
        startRouteButton.setOnClickListener {
            // start route
        }
        shareRouteButton.setOnClickListener {
            // share Route
        }
    }
}
