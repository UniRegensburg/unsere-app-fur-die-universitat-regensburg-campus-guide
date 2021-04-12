package de.ur.explure.views

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.crazylegend.viewbinding.viewBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.storage.FirebaseStorage
import de.ur.explure.GlideApp
import de.ur.explure.R
import de.ur.explure.adapter.CommentAdapter
import de.ur.explure.adapter.WayPointAdapter
import de.ur.explure.databinding.FragmentSingleRouteBinding
import de.ur.explure.model.route.Route
import de.ur.explure.utils.showSnackbar
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
        observeRouteInformation()
        val routeId = args.routeID
        singleRouteViewModel.getRouteData(routeId)
        setOnClickListener()
        setErrorObserver()
    }

    private fun initAdapters() {
        initCommentAdapter()
        initWayPointAdapter()
    }

    private fun observeRouteInformation() {
        singleRouteViewModel.route.observe(viewLifecycleOwner, { route ->
            if (route != null) {
                binding.routeName.text = route.title
                binding.routeDescription.text = route.description
                binding.routeDuration.text = getString(R.string.route_item_duration, route.duration.toInt())
                binding.routeDistance.text = getString(R.string.route_item_distance, route.distance.toInt())
                binding.routeRating.rating = route.currentRating.toFloat()
                setImage(route.thumbnailUrl)
                // needs to init Adapters here because otherwise it won't load new comments and answers correctly
                initAdapters()
                setAdapters(route)
                binding.scrollview.visibility = View.VISIBLE
                binding.progressBar.visibility = View.GONE
            }
        })
    }

    private fun setAdapters(route: Route) {
        wayPointAdapter.setItems(route.wayPoints)
        commentAdapter.setItems(route.comments)
    }

    private fun setImage(image: String) {
        if (image.isNotEmpty()) {
            try {
                val gsReference = fireStorage.getReferenceFromUrl(image)
                GlideApp.with(requireContext())
                        .load(gsReference)
                        .error(R.drawable.map_background)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(binding.routeImage)
            } catch (_: Exception) {
            }
        }
    }

    private fun addAnswers(commentId: String, answerText: String) {
        if (answerText.isNotEmpty()) {
            singleRouteViewModel.addAnswer(commentId, answerText)
        } else {
            showSnackbar(
                    requireActivity(),
                    R.string.empty_answer,
                    R.id.single_route_container,
                    Snackbar.LENGTH_LONG,
                    colorRes = R.color.colorError
            )
        }
    }

    private fun initCommentAdapter() {
        commentAdapter = CommentAdapter { commentId, answerText ->
            addAnswers(commentId, answerText)
        }
        binding.comments.adapter = commentAdapter
        binding.comments.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun initWayPointAdapter() {
        wayPointAdapter = WayPointAdapter()
        binding.waypoints.adapter = wayPointAdapter
        binding.waypoints.layoutManager = LinearLayoutManager(requireContext())
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
        binding.addCommentButton.setOnClickListener {
            val commentInput = binding.commentInput.text.toString()
            if (commentInput.isNotEmpty()) {
                singleRouteViewModel.addComment(commentInput)
                binding.commentInput.text.clear()
            } else {
                showSnackbar(
                        requireActivity(),
                        R.string.empty_comment,
                        R.id.single_route_container,
                        Snackbar.LENGTH_LONG,
                        colorRes = R.color.colorError
                )
            }
        }
    }

    private fun setErrorObserver() {
        singleRouteViewModel.showErrorMessage.observe(viewLifecycleOwner, { showError ->
            if (showError != null && showError) {
                showSnackbar(
                        requireActivity(),
                        R.string.single_route_error,
                        R.id.single_route_container,
                        Snackbar.LENGTH_LONG,
                        colorRes = R.color.colorWarning
                )
            }
        })
    }
}
