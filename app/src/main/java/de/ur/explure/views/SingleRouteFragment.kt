package de.ur.explure.views

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.ArrayAdapter
import de.ur.explure.R
import de.ur.explure.viewmodel.SingleRouteViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlinx.android.synthetic.main.fragment_single_route.*

class SingleRouteFragment : Fragment(R.layout.fragment_single_route) {

    private val singleRouteViewModel: SingleRouteViewModel by viewModel()
    val listItems = ArrayList<String>()
    lateinit var adapter: ArrayAdapter<String>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeRouteInformation()
        observeWaypoints()
        setOnClickListener()

        singleRouteViewModel.setRouteData()
        singleRouteViewModel.setWaypoints()
    }

    private fun observeRouteInformation() {
        singleRouteViewModel.route.observe(viewLifecycleOwner, { route ->
            if (route != null) {
                routeName.text = route.title
                routeDescription.text = route.description
                routeDuration.text = route.duration.toString()
                routeDistance.text = route.distance.toString()
            }
        })
    }

    private fun observeWaypoints() {
        singleRouteViewModel.waypointList.observe(viewLifecycleOwner, { waypoint ->
            if (waypoint != null) {
                for (i in 0 until waypoint.size) {
                    val list = waypoint[i]
                    listItems.addAll(listOf(list.title))
                    listItems.addAll(listOf(list.description))
                }
                adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, listItems)
                waypoints.adapter = adapter
                adapter.notifyDataSetChanged()
            }
        })
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
