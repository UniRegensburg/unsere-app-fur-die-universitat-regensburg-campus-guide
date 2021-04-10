package de.ur.explure.views

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.opengl.Visibility
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.crazylegend.viewbinding.viewBinding
import de.ur.explure.R
import de.ur.explure.WayPointMediaInterface
import de.ur.explure.adapter.WayPointCreateMediaAdapter
import de.ur.explure.databinding.DialogCreateWaypointBinding
import de.ur.explure.model.view.WayPointAudioItem
import de.ur.explure.model.view.WayPointImageItem
import de.ur.explure.model.view.WayPointMediaItem
import de.ur.explure.model.view.WayPointVideoItem
import de.ur.explure.model.waypoint.WayPointDTO
import de.ur.explure.utils.CachedFileUtils
import de.ur.explure.viewmodel.CreateWayPointViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel


class CreateWayPointDialogFragment : DialogFragment(R.layout.dialog_create_waypoint),
    WayPointMediaInterface {

    private val binding by viewBinding(DialogCreateWaypointBinding::bind)
    private val viewModel: CreateWayPointViewModel by viewModel()
    private val args: CreateWayPointDialogFragmentArgs by navArgs()

    private lateinit var imageResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var videoResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var audioResultLauncher: ActivityResultLauncher<Intent>

    private lateinit var mediaAdapter: WayPointCreateMediaAdapter

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
        initMediaAdapter()
        initResultLaunchers()
        analyseNavArgs()
    }

    private fun initMediaAdapter() {
        mediaAdapter = WayPointCreateMediaAdapter(this)
        binding.rvMediaList.adapter = mediaAdapter
        binding.rvMediaList.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
    }

    private fun initResultLaunchers() {
        initImageResultLauncher()
        initVideoResultLauncher()
        initAudioResultLauncher()
    }

    private fun initImageResultLauncher() {
        imageResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val data: Uri = result.data?.data ?: return@registerForActivityResult
                    viewModel.setSelectedImage(data)
                }
            }
    }

    private fun initVideoResultLauncher() {
        videoResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val data: Intent? = result.data
                }
            }
    }

    private fun initAudioResultLauncher() {
        audioResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val data: Uri = result.data?.data ?: return@registerForActivityResult
                    viewModel.setSelectedImage(data)
                }
            }
    }

    private fun analyseNavArgs() {
        val wayPointDTO: WayPointDTO? = args.wayPointDTO
        val latitude: Long = args.latitude
        val longitude: Long = args.longitude
        if (latitude != COORDINATES_DEFAULT_VALUE && longitude != COORDINATES_DEFAULT_VALUE) {
            viewModel.initNewWayPointDTO(longitude.toDouble(), latitude.toDouble())
        } else if (wayPointDTO != null) {
            viewModel.initWayPointDTOEdit(wayPointDTO)
        } else {
            dismiss()
        }
    }

    private fun initObservers() {
        initEditObserver()
        initMediaListObserver()
        initSelectedImageObserver()
    }

    private fun initMediaListObserver() {
        viewModel.mediaList.observe(viewLifecycleOwner, { mediaList ->
            if (mediaList != null) {
                mediaAdapter.items = mediaList
                mediaAdapter.notifyDataSetChanged()
                setMediaButtons(mediaList)
            }
        })
    }

    private fun setMediaButtons(mediaList: List<WayPointMediaItem>) {
        binding.ivAddAudio.isEnabled = !mediaList.any { it is WayPointAudioItem }
        binding.ivAddImage.isEnabled = !mediaList.any { it is WayPointImageItem }
        binding.ivAddVideo.isEnabled = !mediaList.any { it is WayPointVideoItem }

    }

    private fun initSelectedImageObserver() {
        viewModel.selectedImage.observe(viewLifecycleOwner, { image ->
            if (image != null) {
                binding.ivAddImage.setImageURI(image)
            }
        })
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
            viewModel.addMediaItem(WayPointAudioItem(null))
        }
    }

    private fun initVideoMediaButton() {
        binding.ivAddVideo.setOnClickListener {
            viewModel.addMediaItem(WayPointVideoItem(null))
        }
    }

    private fun initImageMediaButton() {
        binding.ivAddImage.setOnClickListener {
            viewModel.addMediaItem(WayPointImageItem(null))
            //startImageIntent()
        }
    }

    private fun startImageIntent() {
        val galleryIntent = Intent(Intent.ACTION_GET_CONTENT, null)
        galleryIntent.type = "image/*"

        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(
            MediaStore.EXTRA_OUTPUT,
            CachedFileUtils.getImageUri(requireContext())
        )
        val chooser = Intent(Intent.ACTION_CHOOSER)
        chooser.putExtra(Intent.EXTRA_INTENT, galleryIntent)
        val intentArray = arrayOf(cameraIntent)
        chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)
        imageResultLauncher.launch(chooser)
    }

    private fun initSaveButton() {
        binding.btnSaveWaypoint.setOnClickListener {
            val title = binding.etWayPointTitle.text.toString()
            viewModel.setTitle(title)
            val wayPointDTO = viewModel.newWayPointDTO.value
            if (wayPointDTO != null) {
                findNavController().previousBackStackEntry?.savedStateHandle?.set(
                    CreateRouteFragment.WAYPOINT_EDIT_KEY, wayPointDTO
                )
            }
            dismiss()
        }
    }

    companion object {
        const val COORDINATES_DEFAULT_VALUE: Long = 0L
    }

    override fun showImageMedia(mediaItem: WayPointImageItem) {
        TODO("Not yet implemented")
    }

    override fun showVideoMedia(mediaItem: WayPointVideoItem) {
        TODO("Not yet implemented")
    }

    override fun playAudioMedia(mediaItem: WayPointAudioItem) {
        TODO("Not yet implemented")
    }

    override fun removeMediaItem(item: WayPointMediaItem) {
        viewModel.deleteMediaItem(item)
    }
}
