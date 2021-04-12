package de.ur.explure.views

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.crazylegend.viewbinding.viewBinding
import de.ur.explure.R
import de.ur.explure.adapter.CategorySpinnerAdapter
import de.ur.explure.adapter.WayPointCreateAdapter
import de.ur.explure.databinding.FragmentCreateRouteBinding
import de.ur.explure.model.category.Category
import de.ur.explure.model.waypoint.WayPointDTO
import de.ur.explure.viewmodel.CreateRouteViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class CreateRouteFragment : Fragment(R.layout.fragment_create_route) {

    private val binding by viewBinding(FragmentCreateRouteBinding::bind)
    private val viewModel: CreateRouteViewModel by viewModel()
    private val args: CreateRouteFragmentArgs by navArgs()

    private lateinit var categoryAdapter: CategorySpinnerAdapter
    private lateinit var wayPointAdapter: WayPointCreateAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initAdapters()
        initObservers()
        initClickListeners()
        initWayPointEditObserver()
        setRouteData()
        viewModel.getCategories()
    }

    private fun setRouteData() {
        viewModel.setRouteInformation(args.distance.toDouble(), args.duration.toDouble())
        viewModel.setWayPointDTOs(args.waypoints.toList())
    }

    private fun initClickListeners() {
        binding.btnSaveRoute.setOnClickListener {
            if (setRouteTitle() && setDescription() && setCategoryId()) {
                viewModel.saveRoute()
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
