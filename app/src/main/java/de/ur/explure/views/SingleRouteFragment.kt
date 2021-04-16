package de.ur.explure.views

import android.app.AlertDialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.crazylegend.viewbinding.viewBinding
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.storage.FirebaseStorage
import de.ur.explure.GlideApp
import de.ur.explure.adapter.CommentInterface
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

class SingleRouteFragment : Fragment(R.layout.fragment_single_route), KoinComponent,
    CommentInterface {

    private val binding by viewBinding(FragmentSingleRouteBinding::bind)
    private val args: SingleRouteFragmentArgs by navArgs()
    private val singleRouteViewModel: SingleRouteViewModel by viewModel()
    private val fireStorage: FirebaseStorage by inject()

    private lateinit var wayPointAdapter: WayPointAdapter
    private lateinit var commentAdapter: CommentAdapter

    private var shareButton: MenuItem? = null

    private var routeFavoriteSnackbar: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        singleRouteViewModel.getRouteData(args.routeID)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        observeRouteInformation()
        observeFavoriteRouteStatus()
        setOnClickListener()
        setErrorObserver()
        setViewFlipperObserver()
    }

    /**
     * Menu
     *
     */

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_single_route, menu)
        shareButton = menu.findItem(R.id.shareSingleRoute)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.shareSingleRoute) {
            singleRouteViewModel.shareRoute(requireContext())
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    private fun setViewFlipperObserver() {
        singleRouteViewModel.currentFlipperViewId.observe(viewLifecycleOwner, { childID ->
            binding.descriptionButton.isEnabled = true
            binding.commentsButton.isEnabled = true
            binding.waypointsButton.isEnabled = true
            when (childID) {
                DESCRIPTION_VIEW_ID -> {
                    setDescriptionState()
                }
                COMMENTS_VIEW_ID -> {
                    setCommentState()
                }
                WAYPOINTS_VIEW_ID -> {
                    setWayPointState()
                }
            }
        })
    }

    private fun setWayPointState() {
        binding.waypointsButton.isEnabled = false
        binding.commentsButton.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.textColorGrey
            )
        )
        binding.descriptionButton.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.textColorGrey
            )
        )
        binding.waypointsButton.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.themeColor
            )
        )
        binding.viewFlipper.displayedChild = WAYPOINTS_VIEW_ID
    }

    private fun setDescriptionState() {
        binding.descriptionButton.isEnabled = false
        binding.commentsButton.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.textColorGrey
            )
        )
        binding.descriptionButton.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.themeColor
            )
        )
        binding.waypointsButton.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.textColorGrey
            )
        )
        binding.viewFlipper.displayedChild = DESCRIPTION_VIEW_ID
    }

    private fun setCommentState() {
        binding.commentsButton.isEnabled = false
        binding.commentsButton.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.themeColor
            )
        )
        binding.descriptionButton.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.textColorGrey
            )
        )
        binding.waypointsButton.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.textColorGrey
            )
        )
        binding.viewFlipper.displayedChild = COMMENTS_VIEW_ID
    }

    private fun initAdapters() {
        initCommentAdapter()
        initWayPointAdapter()
    }

    private fun observeRouteInformation() {
        singleRouteViewModel.route.observe(viewLifecycleOwner, { route ->
            if (route != null) {
                binding.routeName.text = route.title
                binding.routeDescriptionText.text = route.description
                binding.routeDuration.text =
                    getString(R.string.route_item_duration, route.duration.toInt())
                binding.routeDistance.text =
                    getString(R.string.route_item_distance, route.distance.toInt())
                binding.routeRating.rating = route.currentRating.toFloat()
                setImage(route.thumbnailUrl)
                // Initialises Adapter here so that new responses are also displayed immediately
                initAdapters()
                setAdapters(route)
                binding.scrollview.visibility = View.VISIBLE
                binding.progressBar.visibility = View.GONE
            }
        })
    }

    private fun observeFavoriteRouteStatus() {
        singleRouteViewModel.routeFavorited.observe(viewLifecycleOwner, { status ->
            setFavoriteRouteIcon(status)
        })
    }

    private fun setFavoriteRouteIcon(active: Boolean) {
        if (active) {
            binding.favorRouteButton.background = ContextCompat.getDrawable(
                requireActivity(),
                R.drawable.ic_baseline_favorite_24
            )
        } else {
            binding.favorRouteButton.background = ContextCompat.getDrawable(
                requireActivity(),
                R.drawable.ic_favorite_routes_icon
            )
        }
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
                    .fitCenter()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(binding.routeImage)
            } catch (_: Exception) {
            }
        }
    }

    private fun initCommentAdapter() {
        commentAdapter = CommentAdapter(this)
        binding.comments.adapter = commentAdapter
        binding.comments.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun initWayPointAdapter() {
        wayPointAdapter = WayPointAdapter {
            singleRouteViewModel.showWayPointDialog(it)
        }
        binding.waypoints.adapter = wayPointAdapter
        binding.waypoints.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setOnClickListener() {
        binding.descriptionButton.setOnClickListener {
            singleRouteViewModel.setFlipperView(DESCRIPTION_VIEW_ID)
        }
        binding.commentsButton.setOnClickListener {
            singleRouteViewModel.setFlipperView(COMMENTS_VIEW_ID)
        }
        binding.waypointsButton.setOnClickListener {
            singleRouteViewModel.setFlipperView(WAYPOINTS_VIEW_ID)
        }
        binding.startRouteButton.setOnClickListener {
            singleRouteViewModel.startNavigation()
        }
        binding.favorRouteButton.setOnClickListener {
            if (singleRouteViewModel.routeFavorited.value == true) {
                // removed from favorites
                routeFavoriteSnackbar = showSnackbar(
                    requireActivity(),
                    R.string.remove_route_from_favorites,
                    R.id.scrollview,
                    colorRes = R.color.themeColor
                )
            } else {
                // added to favorites
                routeFavoriteSnackbar = showSnackbar(
                    requireActivity(),
                    R.string.add_route_to_favorites,
                    R.id.scrollview,
                    colorRes = R.color.themeColor
                )
            }

            singleRouteViewModel.toggleFavoriteRouteStatus(args.routeID)
        }
        binding.addCommentButton.setOnClickListener {
            addComment()
        }
    }

    private fun addComment() {
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

    override fun addAnswer(commentId: String, answerText: String) {
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

    override fun deleteComment(commentId: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_comment)
            .setPositiveButton(R.string.delete_button) { _, _ ->
                singleRouteViewModel.deleteComment(commentId)
                commentDeleted()
            }
            .setNegativeButton(R.string.back_button) { _, _ -> }
            .show()
    }

    override fun deleteAnswer(answerId: String, commentId: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_answer)
            .setPositiveButton(R.string.delete_button) { _, _ ->
                singleRouteViewModel.deleteAnswer(answerId, commentId)
                answerDeleted()
            }
            .setNegativeButton(R.string.back_button) { _, _ -> }
            .show()
    }

    private fun setErrorObserver() {
        singleRouteViewModel.errorMessage.observe(viewLifecycleOwner, { showError ->
            if (showError == true) {
                showSnackbar(
                    requireActivity(),
                    R.string.single_route_error,
                    R.id.single_route_container,
                    Snackbar.LENGTH_LONG,
                    colorRes = R.color.colorError
                ) {
                    this.addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
                        override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                            super.onDismissed(transientBottomBar, event)
                            singleRouteViewModel.popToDiscover()
                        }
                    })
                }
            }
        })
    }

    private fun commentDeleted() {
        singleRouteViewModel.successMessage.observe(viewLifecycleOwner, { successMessage ->
            if (successMessage == true) {
                showSnackbar(
                    requireActivity(),
                    R.string.comment_deleted,
                    R.id.single_route_container,
                    Snackbar.LENGTH_LONG,
                    colorRes = R.color.themeColorDark
                )
            } else {
                showSnackbar(
                    requireActivity(),
                    R.string.delete_comment_failed,
                    R.id.single_route_container,
                    Snackbar.LENGTH_LONG,
                    colorRes = R.color.colorError
                )
            }
        })
    }

    private fun answerDeleted() {
        singleRouteViewModel.successMessage.observe(viewLifecycleOwner, { successMessage ->
            if (successMessage == true) {
                showSnackbar(
                    requireActivity(),
                    R.string.answer_deleted,
                    R.id.single_route_container,
                    Snackbar.LENGTH_LONG,
                    colorRes = R.color.themeColorDark
                )
            } else {
                showSnackbar(
                    requireActivity(),
                    R.string.delete_answer_failed,
                    R.id.single_route_container,
                    Snackbar.LENGTH_LONG,
                    colorRes = R.color.colorError
                )
            }
        })
    }

    companion object {
        private const val DESCRIPTION_VIEW_ID = 0
        private const val COMMENTS_VIEW_ID = 1
        private const val WAYPOINTS_VIEW_ID = 2
    }
}
