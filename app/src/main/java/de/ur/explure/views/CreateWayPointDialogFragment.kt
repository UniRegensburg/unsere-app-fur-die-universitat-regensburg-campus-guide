package de.ur.explure.views

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.crazylegend.viewbinding.viewBinding
import com.google.android.material.snackbar.Snackbar
import de.ur.explure.R
import de.ur.explure.WayPointMediaInterface
import de.ur.explure.adapter.WayPointCreateMediaAdapter
import de.ur.explure.databinding.DialogCreateWaypointBinding
import de.ur.explure.model.view.WayPointAudioItem
import de.ur.explure.model.view.WayPointImageItem
import de.ur.explure.model.view.WayPointMediaItem
import de.ur.explure.model.view.WayPointVideoItem
import de.ur.explure.model.waypoint.WayPointDTO
import de.ur.explure.utils.getRealSize
import de.ur.explure.utils.hasAudioPermission
import de.ur.explure.utils.hasCameraPermission
import de.ur.explure.utils.hasExternalReadPermission
import de.ur.explure.utils.showSnackbar
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

    private lateinit var multiplePermissionLauncher: ActivityResultLauncher<Array<String>>

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
        viewModel.initAudioRecorder(requireContext())
    }


    private fun initResultLaunchers() {
        initImageResultLauncher()
        initVideoResultLauncher()
        initAudioResultLauncher()
        initMultiplePermissionLauncher()
    }

    private fun initObservers() {
        initEditObserver()
        initMediaListObserver()
    }

    private fun initClickListeners() {
        initSaveButton()
        initImageMediaButton()
        initVideoMediaButton()
        initAudioMediaButton()
        initHideAudioUIButton()
        initAudioUI()
    }

    private fun initMediaAdapter() {
        mediaAdapter = WayPointCreateMediaAdapter(this)
        binding.rvMediaList.adapter = mediaAdapter
        binding.rvMediaList.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
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

    private fun initMultiplePermissionLauncher() {
        multiplePermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { resultsMap ->
                resultsMap.forEach {
                    if (!it.value) {
                        val message = when (it.key) {
                            Manifest.permission.RECORD_AUDIO -> R.string.audio_permission_not_granted
                            Manifest.permission.CAMERA -> R.string.camera_permission_not_granted
                            Manifest.permission.READ_EXTERNAL_STORAGE -> R.string.external_storage_permission_not_granted
                            else -> R.string.universal_permission_not_granted
                        }
                        showSnackbar(
                            requireActivity(),
                            message,
                            R.id.btn_save_route,
                            Snackbar.LENGTH_LONG,
                            colorRes = R.color.colorWarning
                        )
                    }
                }
            }
    }

    private fun initImageResultLauncher() {
        imageResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val data: Uri = result.data?.data
                        ?: viewModel.currentTempUri
                        ?: return@registerForActivityResult
                    viewModel.setImageMedia(data)
                    viewModel.currentTempUri = null
                }
            }
    }

    private fun initVideoResultLauncher() {
        videoResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val data: Uri = result.data?.data
                        ?: viewModel.currentTempUri
                        ?: return@registerForActivityResult
                    val fileSize = data.getRealSize(requireContext()) ?: 0L
                    if (fileSize != 0L && fileSize <= MAX_VIDEO_SIZE) {
                        viewModel.setVideoMedia(data)
                    } else {
                        showSnackbar(
                            requireActivity(),
                            resources.getString(R.string.video_size_error, MAX_VIDEO_SIZE_MB),
                            R.id.btn_save_route,
                            Snackbar.LENGTH_LONG,
                            colorRes = R.color.colorWarning
                        )
                    }
                    viewModel.currentTempUri = null
                }
            }
    }

    private fun initAudioResultLauncher() {
        audioResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val data: Uri = result.data?.data ?: return@registerForActivityResult
                }
            }
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

    private fun initEditObserver() {
        viewModel.oldWayPointDTO.observe(viewLifecycleOwner, { wayPoint ->
            if (wayPoint != null) {
                binding.etWayPointTitle.setText(wayPoint.title)
                binding.etWayPointDescription.setText(wayPoint.description)
            }
        })
    }

    private fun initAudioMediaButton() {
        binding.ivAddAudio.setOnClickListener {
            if (binding.llRecordAudioView.visibility == View.GONE) {
                setInitialAudioButtons()
                binding.llRecordAudioView.visibility = View.VISIBLE
                binding.ivExitAudioBtn.visibility = View.VISIBLE
            } else if (binding.llRecordAudioView.visibility == View.VISIBLE) {
                binding.llRecordAudioView.visibility = View.GONE
                binding.ivExitAudioBtn.visibility = View.GONE
            }
        }
    }

    private fun initVideoMediaButton() {
        binding.ivAddVideo.setOnClickListener {
            if (hasCameraPermission(requireContext()) && hasAudioPermission(requireContext())) {
                startVideoIntent()
            } else {
                multiplePermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                )
            }
        }
    }

    private fun initImageMediaButton() {
        binding.ivAddImage.setOnClickListener {
            if (hasCameraPermission(requireContext()) && hasExternalReadPermission(requireContext())) {
                startImageIntent()
            } else {
                multiplePermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                )
            }
        }
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

    private fun initHideAudioUIButton() {
        binding.ivExitAudioBtn.setOnClickListener {
            binding.llRecordAudioView.visibility = View.GONE
            binding.ivExitAudioBtn.visibility = View.GONE
        }
    }

    private fun initAudioUI() {
        binding.ivPlayAudioBtn.setOnClickListener {
            //Play Audio
        }
        binding.ivDeleteAudioBtn.setOnClickListener {
            //Delete
            setInitialAudioButtons()
        }
        binding.ivRecordAudioBtn.setOnClickListener {
            //Record
            disableAudioButton(binding.ivPlayAudioBtn)
            disableAudioButton(binding.ivSaveAudioBtn)
            disableAudioButton(binding.ivRecordAudioBtn)
            enableAudioButton(binding.ivStopAudioBtn)
            enableAudioButton(binding.ivDeleteAudioBtn)
        }
        binding.ivStopAudioBtn.setOnClickListener {
            //Stop Record
            enableAudioButton(binding.ivPlayAudioBtn)
            enableAudioButton(binding.ivSaveAudioBtn)
            enableAudioButton(binding.ivDeleteAudioBtn)
            enableAudioButton(binding.ivRecordAudioBtn)
            disableAudioButton(binding.ivStopAudioBtn)
        }
        binding.ivSaveAudioBtn.setOnClickListener {
            //Save Audio
            binding.llRecordAudioView.visibility = View.GONE
            binding.ivExitAudioBtn.visibility = View.GONE
        }
    }

    private fun disableAudioButton(imageView: ImageView) {
        imageView.isEnabled = false
        imageView.backgroundTintList =
            ContextCompat.getColorStateList(requireContext(), R.color.colorLightGrey)
        imageView.setColorFilter(ContextCompat.getColor(requireContext(), R.color.grey))
    }

    private fun enableAudioButton(imageView: ImageView) {
        imageView.isEnabled = true
        imageView.backgroundTintList =
            ContextCompat.getColorStateList(requireContext(), R.color.white)
        imageView.setColorFilter(ContextCompat.getColor(requireContext(), R.color.highlightColor))
    }

    private fun setInitialAudioButtons() {
        disableAudioButton(binding.ivPlayAudioBtn)
        disableAudioButton(binding.ivSaveAudioBtn)
        disableAudioButton(binding.ivDeleteAudioBtn)
        enableAudioButton(binding.ivRecordAudioBtn)
        disableAudioButton(binding.ivStopAudioBtn)
    }

    private fun startVideoIntent() {
        val galleryIntent = Intent(Intent.ACTION_GET_CONTENT, null)
        galleryIntent.type = "video/*"
        galleryIntent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, MAX_VIDEO_SIZE)
        galleryIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, MAX_VIDEO_LENGTH)
        val cameraIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)

        val uriForFile = viewModel.createNewVideoUri(requireContext())
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, uriForFile)
        cameraIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0)
        cameraIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, MAX_VIDEO_LENGTH)
        cameraIntent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, MAX_VIDEO_SIZE)

        val chooser = Intent(Intent.ACTION_CHOOSER)
        chooser.putExtra(Intent.EXTRA_INTENT, galleryIntent)
        val intentArray = arrayOf(cameraIntent)
        chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)
        videoResultLauncher.launch(chooser)
    }


    private fun startImageIntent() {
        val galleryIntent = Intent(Intent.ACTION_GET_CONTENT, null)
        galleryIntent.type = "image/*"

        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        val uriForFile = viewModel.createNewImageUri(requireContext())
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, uriForFile)

        val chooser = Intent(Intent.ACTION_CHOOSER)
        chooser.putExtra(Intent.EXTRA_INTENT, galleryIntent)
        val intentArray = arrayOf(cameraIntent)
        chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)
        imageResultLauncher.launch(chooser)
    }

    companion object {
        const val COORDINATES_DEFAULT_VALUE: Long = 0L
        const val MAX_VIDEO_SIZE_MB = 50
        const val MAX_VIDEO_SIZE = (MAX_VIDEO_SIZE_MB * 1024 * 1024).toLong()
        const val MAX_VIDEO_LENGTH = 90 //Seconds
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
