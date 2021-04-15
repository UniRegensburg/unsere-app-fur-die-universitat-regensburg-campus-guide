package de.ur.explure.views

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.doOnLayout
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.crazylegend.viewbinding.viewBinding
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.material.snackbar.Snackbar
import de.ur.explure.GlideApp
import de.ur.explure.R
import de.ur.explure.databinding.DialogSingleWaypointBinding
import de.ur.explure.extensions.isEllipsized
import de.ur.explure.model.waypoint.WayPoint
import de.ur.explure.utils.showSnackbar
import de.ur.explure.viewmodel.SingleWaypointViewModel
import org.koin.android.ext.android.bind
import org.koin.androidx.viewmodel.ext.android.viewModel

@Suppress("TooManyFunctions", "TooGenericExceptionCaught", "SwallowedException")
class SingleWaypointDialogFragment : DialogFragment(R.layout.dialog_single_waypoint) {

    private val viewModel: SingleWaypointViewModel by viewModel()
    private val navArgs: SingleWaypointDialogFragmentArgs by navArgs()
    private val binding by viewBinding(DialogSingleWaypointBinding::bind)

    private var fullScreenButton: FrameLayout? = null
    private var fullScreenIcon: ImageView? = null

    private var fullscreenDialog: Dialog? = null

    // Exoplayer
    private var playIfReady = false
    private var currentWindow = 0
    private var playbackPosition: Long = 0
    private var isFullscreen = false

    private var exoPlayer: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            currentWindow = savedInstanceState.getInt(STATE_RESUME_WINDOW)
            playbackPosition = savedInstanceState.getLong(STATE_RESUME_POSITION)
            isFullscreen = savedInstanceState.getBoolean(STATE_PLAYER_FULLSCREEN)
        }
        viewModel.setWayPoint(navArgs.waypoint)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initObservers()
        initFullScreenViews()
        initClickListeners()
        initFullScreenDialog()
        setContinueNavigationButton(navArgs.fromNavigation)
    }

    private fun initObservers() {
        initWayPointObserver()
        initImageRefObserver()
        initImageErrorObserver()
        initVideoUriObserver()
        initVideoErrorObserver()
        initAudioUriObserver()
        initAudioErrorObserver()
    }

    private fun initAudioErrorObserver() {
        viewModel.showAudioError.observe(viewLifecycleOwner, { showError ->
            if (showError) {
                showErrorSnackBar(R.string.waypoint_audio_error)
                viewModel.showAudioError.postValue(false)
            }
        })
    }

    private fun initAudioUriObserver() {
        viewModel.audioUri.observe(viewLifecycleOwner, { audioUri ->
            if (audioUri != null) {
                binding.cardAudioView.ivAudioPreview.setDataSource(audioUri)
            }
        })
    }

    private fun initClickListeners() {
        initShowMoreClickListener()
        initDismissClickListener()
        initFullScreenButton()
    }

    override fun onStart() {
        super.onStart()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            setupMediaController()
        }
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N || exoPlayer == null) {
            setupMediaController()
        }
        dialog?.window?.setLayout(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onPause() {
        super.onPause()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            releasePlayer()
        }
    }

    override fun onStop() {
        super.onStop()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            releasePlayer()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        exoPlayer?.run {
            outState.putInt(STATE_RESUME_WINDOW, this.currentWindowIndex)
            outState.putLong(STATE_RESUME_POSITION, this.currentPosition)
            outState.putBoolean(STATE_PLAYER_FULLSCREEN, isFullscreen)
        }
        super.onSaveInstanceState(outState)
    }

    private fun initDismissClickListener() {
        binding.tvBackBtn.setOnClickListener {
            dismiss()
        }
    }

    /**
     * Fullscreen methods
     *
     */

    private fun initFullScreenDialog() {
        fullscreenDialog =
            object : Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen) {
                override fun onBackPressed() {
                    if (isFullscreen) closeFullscreenDialog()
                    super.onBackPressed()
                }
            }
    }

    private fun initFullScreenButton() {
        fullScreenButton?.setOnClickListener {
            if (!isFullscreen) {
                openFullscreenDialog()
            } else {
                closeFullscreenDialog()
            }
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private fun openFullscreenDialog() {
        try {
            fullScreenIcon?.setImageResource(R.drawable.ic_fullscreen_shrink)
            (binding.cardVideoView.ivVideoPreview.parent as ViewGroup).removeView(binding.cardVideoView.ivVideoPreview)
            fullscreenDialog?.addContentView(
                binding.cardVideoView.ivVideoPreview,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
            isFullscreen = true
            fullscreenDialog?.show()
        } catch (e: Exception) {
            showErrorAndDismiss()
        }
    }

    private fun closeFullscreenDialog() {
        try {
            (binding.cardVideoView.ivVideoPreview.parent as ViewGroup).removeView(binding.cardVideoView.ivVideoPreview)
            binding.cardVideoView.mainMediaFrame.addView(
                binding.cardVideoView.ivVideoPreview, ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    VIDEO_PLAYER_RESTORE_HEIGHT
                )
            )

            fullScreenIcon?.setImageResource(R.drawable.ic_fullscreen_expand)
            isFullscreen = false
            fullscreenDialog?.dismiss()
        } catch (e: Exception) {
            showErrorAndDismiss()
        }
    }

    private fun initFullScreenViews() {
        fullScreenButton = view?.findViewById(R.id.exo_fullscreen_button)
        fullScreenIcon = view?.findViewById(R.id.exo_fullscreen_icon)
    }

    /**
     * Expo Player
     *
     */

    private fun releasePlayer() {
        exoPlayer?.run {
            playIfReady = this.playWhenReady
            playbackPosition = this.currentPosition
            currentWindow = this.currentWindowIndex
            this.release()
            exoPlayer = null
        }
    }

    private fun setupMediaController() {
        exoPlayer = SimpleExoPlayer.Builder(requireContext()).build()
        binding.cardVideoView.ivVideoPreview.player = exoPlayer
        if (isFullscreen) {
            openFullscreenDialog()
        }
    }

    /**
     * Observer
     *
     */

    private fun initVideoUriObserver() {
        viewModel.videoUri.observe(viewLifecycleOwner, { videoUri ->
            if (videoUri != null) {
                try {
                    val mediaItem = MediaItem.fromUri(videoUri)
                    exoPlayer?.setMediaItem(mediaItem)
                } catch (e: Exception) {
                    showErrorSnackBar(R.string.waypoint_video_error)
                }
            }
        })
    }

    private fun initImageErrorObserver() {
        viewModel.showImageError.observe(viewLifecycleOwner, { showError ->
            if (showError) {
                showErrorSnackBar(R.string.waypoint_image_error)
                viewModel.showImageError.postValue(false)
            }
        })
    }

    private fun initVideoErrorObserver() {
        viewModel.showVideoError.observe(viewLifecycleOwner, { showError ->
            if (showError) {
                showErrorSnackBar(R.string.waypoint_video_error)
                viewModel.showVideoError.postValue(false)
            }
        })
    }

    private fun initImageRefObserver() {
        viewModel.imageReference.observe(viewLifecycleOwner, { imageRef ->
            try {
                GlideApp
                    .with(this)
                    .load(imageRef)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(binding.cardImageView.ivImagePreview)
            } catch (e: Exception) {
                showErrorSnackBar(R.string.waypoint_image_error)
            }
        })
    }

    private fun initWayPointObserver() {
        viewModel.wayPoint.observe(viewLifecycleOwner, { wayPoint ->
            wayPoint?.run {
                setTitle(wayPoint.title)
                setDescription(wayPoint.description)
                setMediaViews(wayPoint)
                initShowMoreButton()
            }
        })
    }

    private fun showErrorSnackBar(resID: Int) {
        view?.run {
            showSnackbar(
                requireContext(),
                resID,
                this,
                Snackbar.LENGTH_LONG,
                colorRes = R.color.colorError
            )
        }
    }

    private fun showErrorAndDismiss() {
        view?.run {
            showSnackbar(
                requireContext(),
                R.string.single_route_error,
                this,
                Snackbar.LENGTH_SHORT,
                R.color.colorError
            ) {
            }
        }
    }

    /**
     * Views
     *
     */

    private fun setMediaViews(waypoint: WayPoint) {
        if (!waypoint.audioURL.isNullOrEmpty()) {
            showAudioView()
        }
        if (!waypoint.videoURL.isNullOrEmpty()) {
            showVideoView()
        }
        if (!waypoint.imageURL.isNullOrEmpty()) {
            showImageView()
        }
    }

    private fun showImageView() {
        binding.cardImageView.imageContainer.visibility = View.VISIBLE
    }

    private fun showVideoView() {
        binding.cardVideoView.videoContainer.visibility = View.VISIBLE
    }

    private fun showAudioView() {
        binding.cardAudioView.audioContainer.visibility = View.VISIBLE
    }

    private fun setContinueNavigationButton(fromNavigation: Boolean) {
        if (!fromNavigation) {
            binding.continueNavBtn.visibility = View.VISIBLE
            binding.continueNavBtn.setOnClickListener {
                dismiss()
            }
        } else {
            binding.continueNavBtn.visibility = View.GONE
        }
    }

    private fun initShowMoreClickListener() {
        binding.cardDescriptionView.btnShowMore.setOnClickListener {
            toggleDescription()
        }
    }

    private fun toggleDescription() {
        if (binding.cardDescriptionView.tvWaypointDescription.maxLines != Integer.MAX_VALUE) {
            binding.cardDescriptionView.tvWaypointDescription.maxLines = Integer.MAX_VALUE
            binding.cardDescriptionView.btnShowMore.text = getString(R.string.waypoint_show_less)
        } else {
            binding.cardDescriptionView.tvWaypointDescription.maxLines = DESCRIPTION_MAX_LINES
            binding.cardDescriptionView.btnShowMore.text = getString(R.string.waypoint_show_more)
        }
    }

    private fun initShowMoreButton() {
        binding.cardDescriptionView.tvWaypointDescription.doOnLayout {
            if (binding.cardDescriptionView.tvWaypointDescription.isEllipsized()) {
                binding.cardDescriptionView.btnShowMore.visibility = View.VISIBLE
                if (binding.cardDescriptionView.tvWaypointDescription.maxLines != Integer.MAX_VALUE) {
                    binding.cardDescriptionView.btnShowMore.text = getString(R.string.waypoint_show_more)
                } else {
                    binding.cardDescriptionView.btnShowMore.text = getString(R.string.waypoint_show_less)
                }
            } else {
                binding.cardDescriptionView.btnShowMore.visibility = View.GONE
            }
        }
    }

    private fun setDescription(description: String) {
        if (description.isNotEmpty()) {
            binding.cardDescriptionView.tvWaypointDescription.text = description
        }
    }

    private fun setTitle(title: String) {
        binding.tvWaypointTitle.text = title
    }

    companion object {
        private const val DESCRIPTION_MAX_LINES = 7
        private const val VIDEO_PLAYER_RESTORE_HEIGHT = 500
        private const val STATE_RESUME_WINDOW = "resumeWindow"
        private const val STATE_RESUME_POSITION = "resumePosition"
        private const val STATE_PLAYER_FULLSCREEN = "playerFullscreen"
    }
}
