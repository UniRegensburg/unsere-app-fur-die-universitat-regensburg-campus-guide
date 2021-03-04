package de.ur.explure.views

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.firebase.storage.FirebaseStorage
import de.ur.explure.GlideApp
import de.ur.explure.R
import de.ur.explure.adapter.WayPointAdapter
import de.ur.explure.viewmodel.SingleRouteViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlinx.android.synthetic.main.fragment_single_route.*
import org.koin.core.component.KoinComponent
import org.koin.android.ext.android.inject

class SingleRouteFragment : Fragment(R.layout.fragment_single_route), KoinComponent {

    private val singleRouteViewModel: SingleRouteViewModel by viewModel()
    private val fireStorage: FirebaseStorage by inject()

    lateinit var wayPointAdapter: WayPointAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initObservers()
        getData()
        setOnClickListener()
    }

    private fun initObservers() {
        observeRouteInformation()
        observeWaypoints()
        observeRating()
    }

    private fun getData() {
        singleRouteViewModel.setRouteData()
        singleRouteViewModel.setWaypoints()
        singleRouteViewModel.setRating()
    }

    private fun observeRouteInformation() {
        singleRouteViewModel.route.observe(viewLifecycleOwner, { route ->
            if (route != null) {
                routeName.text = route.title
                routeDescription.text = route.description
                routeDuration.text = route.duration.toString()
                routeDistance.text = route.distance.toString()
                setImage()
            }
        })
    }

    private fun observeRating() {
        singleRouteViewModel.rating.observe(viewLifecycleOwner, { rating ->
            if (rating != null) {
                routeRating.rating = rating.ratingValue.toFloat()
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

    private fun setImage() {
        singleRouteViewModel.route.observe(viewLifecycleOwner, { image ->
            if (image.thumbnailUrl.isNotEmpty()) {
                try {
                    val gsReference = fireStorage.getReferenceFromUrl(image.thumbnailUrl)
                    GlideApp.with(requireContext())
                        .load(gsReference)
                        .error(R.drawable.map_background)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(routeImage)
                } catch (_: Exception) {
                }
            }
        })
    }

    private fun setOnClickListener() {
        descriptionButton.setOnClickListener {
            viewFlipper.displayedChild = 0
        }
        mapButton.setOnClickListener {
            viewFlipper.displayedChild = 1
        }
        waypointsButton.setOnClickListener {
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
