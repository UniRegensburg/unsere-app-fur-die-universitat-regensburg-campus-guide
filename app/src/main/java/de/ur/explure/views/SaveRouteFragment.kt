package de.ur.explure.views

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.crazylegend.viewbinding.viewBinding
import com.google.firebase.storage.FirebaseStorage
import de.ur.explure.GlideApp
import de.ur.explure.R
import de.ur.explure.adapter.CategorySpinnerAdapter
import de.ur.explure.adapter.WayPointCreateAdapter
import de.ur.explure.databinding.FragmentSaveRouteBinding
import de.ur.explure.model.category.Category
import de.ur.explure.model.waypoint.WayPointDTO
import de.ur.explure.utils.showSnackbar
import de.ur.explure.viewmodel.SaveRouteViewModel
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.androidx.viewmodel.scope.emptyState
import java.lang.Double.parseDouble
import kotlin.math.roundToInt

// TODO allow user to go back to previous step here ??
class SaveRouteFragment : Fragment(R.layout.fragment_save_route) {

    private val fireStorage: FirebaseStorage by inject()

    private val binding by viewBinding(FragmentSaveRouteBinding::bind)
    private val viewModel: SaveRouteViewModel by viewModel(state = emptyState())
    private val args: SaveRouteFragmentArgs by navArgs()

    private lateinit var categoryAdapter: CategorySpinnerAdapter
    private lateinit var wayPointAdapter: WayPointCreateAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initAdapters()
        initObservers()
        initClickListeners()
        initWayPointEditObserver()
        setRouteData()
        setupTextListeners()
        viewModel.getCategories()

        // set the initial selection (e.g. after a rotation)
        viewModel.routeCategory?.let { categoryId ->
            val category = viewModel.categories.value?.find { it.id == categoryId }
            if (category != null) {
                val spinnerPosition = categoryAdapter.getPosition(category)
                binding.spinnerCategories.setSelection(spinnerPosition)
            }
        }
    }

    private fun setRouteData() {
        // TODO save routeLine.coordinates and routeThumbnail in firestore when creating the route!
        //  -> update the RouteDTO for this?
        val routeLine = args.route
        val routeThumbnailUri = args.routeThumbnail
        val routeWaypoints = args.waypoints.toList()
        val routeDuration = args.duration.toDouble()
        val routeDistance = args.distance.toDouble()

        viewModel.setInitialRouteInformation(routeDistance, routeDuration)
        viewModel.setWayPointDTOs(routeWaypoints)

        binding.etRouteDuration.setText(
            viewModel.routeDuration?.toString() ?: routeDuration.roundToInt().toString()
        )
        viewModel.routeTitle?.let { binding.etRouteTitle.setText(it) }
        viewModel.routeDescription?.let { binding.etRouteDescription.setText(it) }

        try {
            // val gsReference = fireStorage.getReferenceFromUrl(routeThumbnailUri)
            GlideApp.with(this)
                .load(routeThumbnailUri)
                .error(R.drawable.map_background)
                .transition(DrawableTransitionOptions.withCrossFade())
                .centerInside()
                .into(binding.routeThumbnail)
        } catch (_: Exception) {}
    }

    private fun setupTextListeners() {
        binding.etRouteTitle.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // not needed
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // not needed
            }

            override fun afterTextChanged(s: Editable) {
                viewModel.setTitle(s.toString())
            }
        })

        binding.etRouteDescription.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // not needed
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // not needed
            }

            override fun afterTextChanged(s: Editable) {
                viewModel.setDescription(s.toString())
            }
        })

        binding.etRouteDuration.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // not needed
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // not needed
            }

            override fun afterTextChanged(s: Editable?) {
                val newDuration = s?.toString()
                if (newDuration != null && newDuration != "") {
                    viewModel.updateRouteDuration(parseDouble(newDuration))
                }
            }
        })
    }

    private fun initClickListeners() {
        binding.btnSaveRoute.setOnClickListener {
            if (setRouteTitle() && setDescription() && setCategoryId()) {
                viewModel.saveRoute()
            } else {
                // TODO noch genauere fehlerinformationen mit fallunterscheidung?
                showSnackbar(
                    requireActivity(),
                    "Speichern nicht möglich, es fehlen noch Informationen! " +
                            "Die Route benötigt einen Titel, eine Beschreibung und eine Kategorie!",
                    colorRes = R.color.colorError
                )
            }
        }
    }

    private fun setCategoryId(): Boolean {
        // TODO: Validate
        val category = binding.spinnerCategories.selectedItem as Category? ?: return false
        viewModel.setCategoryId(category.id)
        return true
    }

    private fun setDescription(): Boolean {
        // TODO: Validate
        viewModel.setDescription(binding.etRouteDescription.text.toString())
        return true
    }

    private fun setRouteTitle(): Boolean {
        // TODO: Validate
        viewModel.setTitle(binding.etRouteTitle.text.toString())
        return true
    }

    private fun initWayPointEditObserver() {
        val navBackStackEntry = findNavController().getBackStackEntry(R.id.saveRouteFragment)

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME &&
                navBackStackEntry.savedStateHandle.contains(WAYPOINT_EDIT_KEY)
            ) {
                val editedWayPointDTO =
                    navBackStackEntry.savedStateHandle.get<WayPointDTO>(WAYPOINT_EDIT_KEY)
                if (editedWayPointDTO != null) {
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

    private fun initAdapters() {
        initCategoryAdapter()
        initWayPointAdapter()
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

        binding.spinnerCategories.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCategory = parent?.getItemAtPosition(position) as? Category ?: return
                viewModel.setCategoryId(selectedCategory.id)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // not needed
            }
        }
    }

    private fun initObservers() {
        viewModel.categories.observe(viewLifecycleOwner, { categories ->
            if (categories != null) {
                categoryAdapter.addAll(categories)
                categoryAdapter.notifyDataSetChanged()
            }
        })
        viewModel.wayPointDTOs.observe(viewLifecycleOwner, { waypointsDTO ->
            if (waypointsDTO != null) {
                wayPointAdapter.items = waypointsDTO
                wayPointAdapter.notifyDataSetChanged()
            }
        })
    }

    companion object {
        const val WAYPOINT_EDIT_KEY = "WayPointEdit"
    }
}
