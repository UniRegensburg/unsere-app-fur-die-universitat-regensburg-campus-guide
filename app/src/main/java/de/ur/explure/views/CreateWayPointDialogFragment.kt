package de.ur.explure.views

import android.os.Bundle
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.crazylegend.viewbinding.viewBinding
import de.ur.explure.R
import de.ur.explure.databinding.DialogCreateWaypointBinding
import de.ur.explure.model.waypoint.WayPointDTO
import de.ur.explure.viewmodel.CreateWayPointViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class CreateWayPointDialogFragment : DialogFragment(R.layout.dialog_create_waypoint) {

    private val binding by viewBinding(DialogCreateWaypointBinding::bind)
    private val viewModel: CreateWayPointViewModel by viewModel()
    private val args: CreateWayPointDialogFragmentArgs by navArgs()

    override fun onResume() {
        super.onResume()
        dialog?.window?.setLayout(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initClickListeners()
        initObservers()
        analyseNavArgs()
    }

    private fun analyseNavArgs() {
        val wayPointDTO: WayPointDTO? = args.wayPointDTO
        val latitude: Long = args.latitude
        val longitude: Long = args.longitude
        if (latitude != COORDINATES_DEFAULT_VALUE && longitude != COORDINATES_DEFAULT_VALUE) {
            viewModel.initNewWayPointDTO(
                longitude.toDouble(),
                latitude.toDouble(),
                getString(R.string.default_waypoint_title)
            )
        } else if (wayPointDTO != null) {
            viewModel.initWayPointDTOEdit(wayPointDTO)
        } else {
            dismiss()
        }
    }

    private fun initObservers() {
        initEditObserver()
    }

    private fun initClickListeners() {
        initSaveButton()
        initImageMediaButton()
        initVideoMediaButton()
        initAudioMediaButton()
    }

    private fun initEditObserver() {
        viewModel.oldWayPointDTO.observe(viewLifecycleOwner, { wayPoint ->
            if (wayPoint != null) {
                fillTitleText(wayPoint.title)
                fillDescriptionText(wayPoint.description)
            }
        })
    }

    private fun fillDescriptionText(description: String) {
        binding.etWayPointDescription.setText(description)
    }

    private fun fillTitleText(title: String) {
        binding.etWayPointTitle.setText(title)
    }

    private fun initAudioMediaButton() {
        binding.ivAddAudio.setOnClickListener {
        }
    }

    private fun initVideoMediaButton() {
        binding.ivAddVideo.setOnClickListener {
        }
    }

    private fun initImageMediaButton() {
        binding.ivAddImage.setOnClickListener {
        }
    }

    private fun initSaveButton() {
        binding.btnSaveWaypoint.setOnClickListener {
            val title = binding.etWayPointTitle.text.toString()
            viewModel.setTitle(title)
            val wayPointDTO = viewModel.newWayPointDTO.value
            if (wayPointDTO != null) {
                findNavController().previousBackStackEntry?.savedStateHandle?.set(
                    SaveRouteFragment.WAYPOINT_EDIT_KEY, wayPointDTO
                )
            }
            dismiss()
        }
    }

    companion object {
        const val COORDINATES_DEFAULT_VALUE: Long = 0L
    }
}
