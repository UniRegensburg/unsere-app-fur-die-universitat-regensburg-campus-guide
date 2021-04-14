package de.ur.explure.views

import android.os.Bundle
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.doOnLayout
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.navArgs
import com.crazylegend.viewbinding.viewBinding
import de.ur.explure.R
import de.ur.explure.databinding.DialogSingleWaypointBinding
import de.ur.explure.extensions.isEllipsized
import de.ur.explure.model.waypoint.WayPoint
import de.ur.explure.viewmodel.SingleWaypointViewModel
import org.koin.android.ext.android.bind
import org.koin.androidx.viewmodel.ext.android.viewModel

class SingleWaypointDialogFragment : DialogFragment(R.layout.dialog_single_waypoint) {

    private val viewModel: SingleWaypointViewModel by viewModel()
    private val navArgs: SingleWaypointDialogFragmentArgs by navArgs()
    private val binding by viewBinding(DialogSingleWaypointBinding::bind)

    override fun onResume() {
        super.onResume()
        dialog?.window?.setLayout(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.setWayPoint(navArgs.waypoint)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initObservers()
        initClickListeners()
    }

    private fun initObservers() {
        initWayPointObserver()
    }

    private fun initClickListeners() {
        initShowMoreClickListener()
        initDimissClickListener()
    }

    private fun initDimissClickListener() {
        binding.tvBackBtn.setOnClickListener {
            dismiss()
        }
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

    private fun showImageView(){
        binding.tvImageTitle.visibility = View.VISIBLE
        binding.verticalImageView.visibility = View.VISIBLE
        binding.ivImagePreview.visibility = View.VISIBLE
    }

    private fun showVideoView(){
        binding.tvVideoTitle.visibility = View.VISIBLE
        binding.verticalVideoView.visibility = View.VISIBLE
        binding.ivVideoPreview.visibility = View.VISIBLE
    }

    private fun showAudioView(){
        binding.tvAudioTitle.visibility = View.VISIBLE
        binding.verticalAudioView.visibility = View.VISIBLE
        binding.ivAudioPreview.visibility = View.VISIBLE
    }

    private fun initShowMoreClickListener() {
        binding.btnShowMore.setOnClickListener {
            toggleDescription()
        }
    }

    private fun toggleDescription() {
        if (binding.tvWaypointDescription.maxLines != Integer.MAX_VALUE) {
            binding.tvWaypointDescription.maxLines = Integer.MAX_VALUE
            binding.btnShowMore.text = getString(R.string.waypoint_show_less)
        } else {
            binding.tvWaypointDescription.maxLines = DESCRIPTION_MAX_LINES
            binding.btnShowMore.text = getString(R.string.waypoint_show_more)
        }
    }

    private fun initShowMoreButton() {
        binding.tvWaypointDescription.doOnLayout {
            if (binding.tvWaypointDescription.isEllipsized()) {
                binding.btnShowMore.visibility = View.VISIBLE
                if (binding.tvWaypointDescription.maxLines != Integer.MAX_VALUE) {
                    binding.btnShowMore.text = getString(R.string.waypoint_show_more)
                } else {
                    binding.btnShowMore.text = getString(R.string.waypoint_show_less)
                }
            } else {
                binding.btnShowMore.visibility = View.GONE
            }
        }
    }

    private fun setDescription(description: String) {
        if (description.isNotEmpty()) {
            binding.tvWaypointDescription.text = description
        }
    }

    private fun setTitle(title: String) {
        binding.tvWaypointTitle.text = title
    }

    companion object {
        private const val DESCRIPTION_MAX_LINES = 7
    }
}
