package de.ur.explure.views

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.crazylegend.viewbinding.viewBinding
import com.google.android.material.snackbar.Snackbar
import de.ur.explure.GlideApp
import de.ur.explure.R
import de.ur.explure.adapter.CategorySpinnerAdapter
import de.ur.explure.adapter.WayPointCreateAdapter
import de.ur.explure.databinding.FragmentCreateRouteBinding
import de.ur.explure.model.category.Category
import de.ur.explure.model.waypoint.WayPointDTO
import de.ur.explure.utils.hasCameraPermission
import de.ur.explure.utils.hasExternalReadPermission
import de.ur.explure.utils.showSnackbar
import de.ur.explure.viewmodel.CreateRouteViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

@Suppress("TooManyFunctions")
class CreateRouteFragment : Fragment(R.layout.fragment_create_route) {

    private val binding by viewBinding(FragmentCreateRouteBinding::bind)
    private val viewModel: CreateRouteViewModel by viewModel()
    private val args: CreateRouteFragmentArgs by navArgs()

    private lateinit var imageResultLauncher: ActivityResultLauncher<Intent>

    private lateinit var multiplePermissionLauncher: ActivityResultLauncher<Array<String>>

    private lateinit var categoryAdapter: CategorySpinnerAdapter
    private lateinit var wayPointAdapter: WayPointCreateAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initAdapters()
        initObservers()
        initClickListeners()
        initResultLaunchers()
        initWayPointEditObserver()
        setRouteData()
        viewModel.getCategories()
    }

    private fun initResultLaunchers() {
        initImageResultLauncher()
        initMultiplePermissionLauncher()
    }

    private fun initClickListeners() {
        initSaveButtonListener()
        initRouteImageListener()
        initDeleteButtonListener()
    }

    private fun initAdapters() {
        initCategoryAdapter()
        initWayPointAdapter()
    }

    private fun initObservers() {
        initWayPointListObserver()
        initCategoriesObserver()
        initCurrentImageObserver()
        initRouteErrorObserver()
        initCategoryErrorObserver()
    }

    private fun initImageResultLauncher() {
        imageResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val data: Uri = result.data?.data ?: viewModel.currentTempCameraUri
                    ?: return@registerForActivityResult
                    viewModel.setImageUri(data)
                } else {
                    showSnackbar(
                        requireActivity(),
                        R.string.route_image_error,
                        R.id.btn_save_route,
                        Snackbar.LENGTH_LONG,
                        colorRes = R.color.colorWarning
                    )
                }
            }
    }

    private fun initMultiplePermissionLauncher() {
        multiplePermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { resultsMap ->
                resultsMap.forEach {
                    if (!it.value) {
                        val message = when (it.key) {
                            Manifest.permission.CAMERA ->
                                R.string.camera_permission_not_granted
                            Manifest.permission.READ_EXTERNAL_STORAGE ->
                                R.string.external_storage_permission_not_granted
                            else ->
                                R.string.universal_permission_not_granted
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

    private fun initSaveButtonListener() {
        binding.btnSaveRoute.setOnClickListener {
            if (validateInputs()) {
                viewModel.saveRoute()
            }
        }
    }

    private fun initRouteImageListener() {
        binding.ivRouteImage.setOnClickListener {
            if (hasCameraPermission(requireContext()) &&
                hasExternalReadPermission(requireContext())
            ) {
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

    private fun initDeleteButtonListener() {
        binding.ivDeleteButton.setOnClickListener {
            viewModel.deleteCurrentUri()
        }
    }

    private fun initWayPointEditObserver() {
        val navBackStackEntry = findNavController().getBackStackEntry(R.id.createRouteFragment)

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME &&
                navBackStackEntry.savedStateHandle.contains(WAYPOINT_EDIT_KEY)
            ) {
                val editedWayPointDTO =
                    navBackStackEntry.savedStateHandle.get<WayPointDTO>(WAYPOINT_EDIT_KEY)
                if (editedWayPointDTO != null) {
                    Timber.d(editedWayPointDTO.toString())
                    viewModel.updateWayPointDTO(editedWayPointDTO)
                }
            }
        }

        navBackStackEntry.lifecycle.addObserver(observer)

        viewLifecycleOwner.lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                navBackStackEntry.lifecycle.removeObserver(observer)
            }
        })
    }

    private fun initWayPointAdapter() {
        wayPointAdapter = WayPointCreateAdapter { wayPointDTO ->
            viewModel.openWayPointDialogFragment(wayPointDTO)
        }
        binding.rvWaypointList.adapter = wayPointAdapter
        binding.rvWaypointList.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
    }

    private fun initCategoryAdapter() {
        categoryAdapter = CategorySpinnerAdapter(requireContext())
        binding.spinnerCategories.adapter = categoryAdapter
    }

    private fun initCurrentImageObserver() {
        viewModel.currentImageUri.observe(viewLifecycleOwner, { imageUri ->
            if (imageUri != null) {
                GlideApp.with(this)
                    .load(imageUri)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(binding.ivRouteImage)
                binding.tvRouteImageHint.visibility = View.GONE
                binding.ivDeleteButton.visibility = View.VISIBLE
                binding.ivDeleteButton.isEnabled = true
            } else {
                binding.tvRouteImageHint.visibility = View.VISIBLE
                binding.ivRouteImage.setImageResource(R.drawable.highlight_square_outline)
                binding.ivDeleteButton.visibility = View.INVISIBLE
                binding.ivDeleteButton.isEnabled = false
            }
        })
    }

    private fun initCategoriesObserver() {
        viewModel.categories.observe(viewLifecycleOwner, { categories ->
            if (categories != null) {
                categoryAdapter.addAll(categories)
                categoryAdapter.notifyDataSetChanged()
            }
        })
    }

    private fun initWayPointListObserver() {
        viewModel.wayPointDTOs.observe(viewLifecycleOwner, { waypointsDTO ->
            if (waypointsDTO != null) {
                wayPointAdapter.items = waypointsDTO
                wayPointAdapter.notifyDataSetChanged()
            }
        })
    }

    private fun initRouteErrorObserver() {
        viewModel.showRouteCreationError.observe(viewLifecycleOwner, { showError ->
            if (showError) {
                showSnackbar(
                    requireActivity(),
                    R.string.route_creation_error,
                    R.id.btn_save_route,
                    Snackbar.LENGTH_LONG,
                    colorRes = R.color.colorWarning
                )
                viewModel.showRouteCreationError.postValue(false)
            }
        })
    }

    private fun initCategoryErrorObserver() {
        viewModel.showCategoryDownloadError.observe(viewLifecycleOwner, { showError ->
            if (showError) {
                showSnackbar(
                    requireActivity(),
                    R.string.category_download_error,
                    R.id.btn_save_route,
                    Snackbar.LENGTH_LONG,
                    colorRes = R.color.colorWarning
                )
                viewModel.showRouteCreationError.postValue(false)
            }
        })
    }

    private fun setRouteData() {
        viewModel.setRouteInformation(args.distance.toDouble(), args.duration.toDouble())
        viewModel.setWayPointDTOs(args.waypoints.toList())
    }

    private fun validateInputs(): Boolean {
        var inputsAreValid = true
        inputsAreValid = validateAndSaveTitle()
        inputsAreValid = validateAndSaveDescription()
        inputsAreValid = validateAndSaveCategory()
        return inputsAreValid
    }

    private fun validateAndSaveCategory(): Boolean {
        val category = binding.spinnerCategories.selectedItem as Category?
        return if (category == null) {
            showSnackbar(
                requireActivity(),
                R.string.no_category_selected_error,
                R.id.btn_save_route,
                Snackbar.LENGTH_LONG,
                colorRes = R.color.colorWarning
            )
            false
        } else {
            viewModel.setCategoryId(category.id)
            true
        }
    }

    private fun validateAndSaveDescription(): Boolean {
        val description = binding.etRouteDescription.text.toString()
        return if (description.isEmpty()) {
            binding.etRouteDescription.error = getString(R.string.waypoint_description_error)
            false
        } else {
            viewModel.setDescription(description)
            true
        }
    }

    private fun validateAndSaveTitle(): Boolean {
        val title = binding.etRouteTitle.text.toString()
        return if (title.isEmpty()) {
            binding.etRouteTitle.error = getString(R.string.waypoint_title_error)
            false
        } else {
            viewModel.setTitle(title)
            true
        }
    }

    private fun startImageIntent() {
        val galleryIntent = Intent(Intent.ACTION_GET_CONTENT, null)
        galleryIntent.type = "image/*"

        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        cameraIntent.putExtra(
            MediaStore.EXTRA_OUTPUT,
            viewModel.createNewCameraUri(requireContext())
        )

        val chooser = Intent(Intent.ACTION_CHOOSER)
        chooser.putExtra(Intent.EXTRA_INTENT, galleryIntent)
        val intentArray = arrayOf(cameraIntent)
        chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)
        imageResultLauncher.launch(chooser)
    }

    companion object {
        const val WAYPOINT_EDIT_KEY = "WayPointEdit"
    }
}
