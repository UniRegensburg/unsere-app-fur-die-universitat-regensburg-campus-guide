package de.ur.explure.views

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.crazylegend.viewbinding.viewBinding
import de.ur.explure.GlideApp
import de.ur.explure.R
import de.ur.explure.adapter.WayPointAdapter
import de.ur.explure.databinding.FragmentSingleRouteBinding
import de.ur.explure.viewmodel.SingleRouteViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.KoinComponent

class SingleRouteFragment : Fragment(R.layout.fragment_single_route), KoinComponent {

    private val binding by viewBinding(FragmentSingleRouteBinding::bind)
    private val args: SingleRouteFragmentArgs by navArgs()
    private val singleRouteViewModel: SingleRouteViewModel by viewModel()

    // TODO: access to firestorage should probably not happen in the fragment! move to repository or viewmodel instead
    //  -> probably not even necessary: the route thumbnail has probably already been downloaded before
    //  so it can simply be passed as a safe arg param or accessed via a sharedViewModel

    private lateinit var wayPointAdapter: WayPointAdapter

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
        singleRouteViewModel.setWaypoints()
        singleRouteViewModel.setRating()
    }

    private fun observeRouteInformation() {
        binding.routeName.text = args.routeTitle
        binding.routeDescription.text = args.routeDescription
        binding.routeDuration.text = args.routeDuration.toString()
        binding.routeDistance.text = args.routeDistance.toString()
        setImage()
    }

    private fun observeRating() {
        singleRouteViewModel.rating.observe(viewLifecycleOwner, { rating ->
            if (rating != null) {
                binding.routeRating.rating = rating.ratingValue.toFloat()
            }
        })
    }

    private fun observeWaypoints() {
        singleRouteViewModel.waypointList.observe(viewLifecycleOwner, { waypoint ->
            wayPointAdapter = WayPointAdapter(waypoint)
            binding.waypoints.adapter = wayPointAdapter
            binding.waypoints.layoutManager = LinearLayoutManager(requireContext())
            wayPointAdapter.notifyDataSetChanged()
        })
    }

    private fun setImage() {
            if (args.routeImage.isNotEmpty()) {
                try {
                    // TODO not here!
                    GlideApp.with(requireContext())
                        .load(args.routeImage)
                        .error(R.drawable.map_background)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(binding.routeImage)
                } catch (_: Exception) {
                }
            }
    }

    private fun setOnClickListener() {
        binding.descriptionButton.setOnClickListener {
            binding.viewFlipper.displayedChild = 0
        }
        binding.mapButton.setOnClickListener {
            binding.viewFlipper.displayedChild = 1
        }
        binding.waypointsButton.setOnClickListener {
            binding.viewFlipper.displayedChild = 2
        }
        binding.startRouteButton.setOnClickListener {
            // start route
        }
        binding.shareRouteButton.setOnClickListener {
            // share Route
        }
    }
}
