package de.ur.explure.views

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.crazylegend.viewbinding.viewBinding
import com.google.firebase.storage.FirebaseStorage
import de.ur.explure.GlideApp
import de.ur.explure.R
import de.ur.explure.adapter.CommentAdapter
import de.ur.explure.adapter.WayPointAdapter
import de.ur.explure.databinding.FragmentSingleRouteBinding
import de.ur.explure.viewmodel.SingleRouteViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SingleRouteFragment : Fragment(R.layout.fragment_single_route), KoinComponent {

    private val binding by viewBinding(FragmentSingleRouteBinding::bind)
    private val args: SingleRouteFragmentArgs by navArgs()
    private val singleRouteViewModel: SingleRouteViewModel by viewModel()
    private val fireStorage: FirebaseStorage by inject()

    private lateinit var wayPointAdapter: WayPointAdapter
    private lateinit var commentAdapter: CommentAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val routeId = args.routeID
        singleRouteViewModel.getRouteData(routeId)
        singleRouteViewModel.getWayPointData(routeId)
        singleRouteViewModel.getCommentData(routeId)
        initObservers()
        setOnClickListener()
    }

    private fun initObservers() {
        observeRouteInformation()
        observeWayPoints()
        observeComments()
        setImage()
    }

    private fun observeRouteInformation() {
        singleRouteViewModel.route.observe(viewLifecycleOwner, { route ->
            binding.routeName.text = route.title
            binding.routeDescription.text = route.description
            binding.routeDuration.text = getString(R.string.route_item_duration, route.duration.toInt())
            binding.routeDistance.text = getString(R.string.route_item_distance, route.distance.toInt())
            binding.routeRating.rating = route.currentRating.toFloat()
        })
    }

    private fun setImage() {
        singleRouteViewModel.route.observe(viewLifecycleOwner, { route ->
            if (route.thumbnailUrl.isNotEmpty()) {
                try {
                    val gsReference = fireStorage.getReferenceFromUrl(route.thumbnailUrl)
                    GlideApp.with(requireContext())
                            .load(gsReference)
                            .error(R.drawable.map_background)
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .into(binding.routeImage)
                } catch (_: Exception) {
                }
            }
        })
    }

    private fun observeWayPoints() {
        singleRouteViewModel.wayPointList.observe(viewLifecycleOwner, { wayPoint ->
            wayPointAdapter = WayPointAdapter(wayPoint)
            binding.waypoints.adapter = wayPointAdapter
            binding.waypoints.layoutManager = LinearLayoutManager(requireContext())
            wayPointAdapter.notifyDataSetChanged()
        })
    }

    private fun observeComments() {
        singleRouteViewModel.commentList.observe(viewLifecycleOwner, { comment ->
            commentAdapter = CommentAdapter(comment)
            binding.comments.adapter = commentAdapter
            binding.comments.layoutManager = LinearLayoutManager(requireContext())
            commentAdapter.notifyDataSetChanged()
        })
    }

    private fun setOnClickListener() {
        binding.descriptionButton.setOnClickListener {
            binding.viewFlipper.displayedChild = 0
        }
        binding.commentsButton.setOnClickListener {
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
