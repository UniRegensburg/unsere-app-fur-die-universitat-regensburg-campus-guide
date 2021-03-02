package de.ur.explure.views

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import de.ur.explure.R
import de.ur.explure.adapter.ImageAdapter
import de.ur.explure.adapter.WayPointAdapter
import de.ur.explure.viewmodel.SingleRouteViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlinx.android.synthetic.main.fragment_single_route.*

class SingleRouteFragment : Fragment(R.layout.fragment_single_route) {

    private val singleRouteViewModel: SingleRouteViewModel by viewModel()

    lateinit var wayPointAdapter: WayPointAdapter
    lateinit var imageAdapter: ImageAdapter
    var totalRating: Float = 0.0F
    var count: Float = 0.0F

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeRouteInformation()
        observeWaypoints()
        observeImages()
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
               for (element in route.rating) {
                   totalRating += element.toFloat()
                   count += 1
                   val averageRating = totalRating / count
                   routeRating.rating = averageRating
                }
            }
        })
    }

    private fun observeWaypoints() {
        singleRouteViewModel.waypointList.observe(viewLifecycleOwner, { waypoint ->
            wayPointAdapter = WayPointAdapter(waypoint)
            waypoints.adapter = wayPointAdapter
            waypoints.layoutManager = LinearLayoutManager(requireContext())
            wayPointAdapter.notifyDataSetChanged()
        })
    }

    private fun observeImages() {
        singleRouteViewModel.waypointList.observe(viewLifecycleOwner, { image ->
            imageAdapter = ImageAdapter(requireContext(), image)
            images.adapter = imageAdapter
            imageAdapter.notifyDataSetChanged()
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
