package de.ur.explure.views

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.crazylegend.viewbinding.viewBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import de.ur.explure.R
import de.ur.explure.adapter.WaypointsAdapter
import de.ur.explure.databinding.RouteCreationBottomsheetBinding
import de.ur.explure.extensions.hide
import de.ur.explure.extensions.initHidden
import de.ur.explure.extensions.show
import de.ur.explure.model.waypoint.WayPoint
import de.ur.explure.viewmodel.MapViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.androidx.viewmodel.scope.emptyState

class RouteCreationBottomSheet : Fragment(R.layout.route_creation_bottomsheet) {

    private val binding by viewBinding(RouteCreationBottomsheetBinding::bind)

    // TODO does this work (or does it have to be scoped to the activity?)
    //  does state = emptyState() reset the savedStateHandle?
    private val mapViewModel: MapViewModel by viewModel(state = emptyState())

    private lateinit var waypointsAdapter: WaypointsAdapter

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bottomSheetBehavior = BottomSheetBehavior.from(binding.waypointBottomsheetLayout)
        // init the bottom sheet hidden with zero peekHeight
        bottomSheetBehavior.initHidden()

        observeViewModel()
    }

    private fun observeViewModel() {
        mapViewModel.routeCreationModeActive.observe(viewLifecycleOwner, { active ->
            // TODO this is never called :(
            if (active) {
                showRouteCreationSheet()
            } else {
                hideRouteCreationSheet()
            }
        })
    }

    fun showRouteCreationSheet() {
        bottomSheetBehavior.show { state ->
            if (state == BottomSheetBehavior.STATE_COLLAPSED || state == BottomSheetBehavior.STATE_HIDDEN) {
                // TODO reset content and cleanup on when hidden
            }
        }

        // bottomSheetBehavior.peekHeight = 120
        setupUI()
    }

    private fun hideRouteCreationSheet() {
        bottomSheetBehavior.hide()
    }

    private fun setupUI() {
        setupWaypointList()
    }

    private fun setupWaypointList() {
        val linearLayoutManager = LinearLayoutManager(activity ?: return)
        linearLayoutManager.stackFromEnd = true // insert items at the bottom instead of top

        waypointsAdapter = WaypointsAdapter { view: View, wayPoint: WayPoint ->
            // a item in the recyclerView was clicked
            Toast.makeText(requireActivity(), "Clicked on ${wayPoint.title}", Toast.LENGTH_SHORT)
                .show()
        }

        binding.recyclerWaypointList.apply {
            setHasFixedSize(true) // TODO sinnvoll?
            adapter = waypointsAdapter
            layoutManager = linearLayoutManager
        }

        // TODO can this listener cause a leak?
        binding.recyclerWaypointList.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            // show a text message if the recyclerView is empty
            if (waypointsAdapter.itemCount == 0) {
                binding.emptyRecyclerWaypointText.visibility = View.VISIBLE
            } else {
                binding.emptyRecyclerWaypointText.visibility = View.GONE
            }
        }
    }
}
