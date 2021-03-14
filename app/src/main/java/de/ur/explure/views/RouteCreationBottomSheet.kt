package de.ur.explure.views

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.crazylegend.viewbinding.viewBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import de.ur.explure.R
import de.ur.explure.adapter.RouteCreationAdapter
import de.ur.explure.databinding.RouteCreationBottomsheetBinding
import de.ur.explure.extensions.hide
import de.ur.explure.extensions.initHidden
import de.ur.explure.extensions.show
import de.ur.explure.model.waypoint.WayPoint
import de.ur.explure.viewmodel.MapViewModel
import org.koin.androidx.viewmodel.ext.android.getViewModel

class RouteCreationBottomSheet : Fragment(R.layout.route_creation_bottomsheet) {

    private val binding by viewBinding(RouteCreationBottomsheetBinding::bind)

    // see https://stackoverflow.com/questions/59094242/get-sharedviewmodel-in-childfragment-using-koin-and-navargs
    private val mapViewModel: MapViewModel by lazy { requireParentFragment().getViewModel() }

    private var routeCreationAdapter: RouteCreationAdapter? = null

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bottomSheetBehavior = BottomSheetBehavior.from(binding.waypointBottomsheetLayout)
        // init the bottom sheet hidden with zero peekHeight
        bottomSheetBehavior.initHidden()

        observeViewModel()
    }

    private fun observeViewModel() {
        mapViewModel.manualRouteCreationModeActive.observe(viewLifecycleOwner, { active ->
            if (active) {
                showRouteCreationSheet()
            } else {
                hideRouteCreationSheet()
            }
        })
        mapViewModel.customRouteWaypoints.observe(viewLifecycleOwner, { routeWaypoints ->
            routeCreationAdapter?.submitList(routeWaypoints)
        })
    }

    private fun showRouteCreationSheet() {
        bottomSheetBehavior.isHideable = false // TODO does not work! still hideable!
        bottomSheetBehavior.show { state ->
            if (state == BottomSheetBehavior.STATE_COLLAPSED || state == BottomSheetBehavior.STATE_HIDDEN) {
                // TODO reset content and cleanup on when hidden
            }
        }

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

        routeCreationAdapter = RouteCreationAdapter { view: View, wayPoint: WayPoint ->
            // a item in the recyclerView was clicked
            Toast.makeText(requireActivity(), "Clicked on ${wayPoint.title}", Toast.LENGTH_SHORT)
                .show()
        }

        binding.recyclerWaypointList.apply {
            adapter = routeCreationAdapter
            layoutManager = linearLayoutManager
            // TODO for smooth scrolling when in nested scroll view
            isNestedScrollingEnabled = true

            addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                // show a text message if the recyclerView is empty
                if (routeCreationAdapter?.itemCount != 0) {
                    binding.emptyRecyclerWaypointText.visibility = View.GONE
                } else {
                    binding.emptyRecyclerWaypointText.visibility = View.VISIBLE
                }
            }
        }
    }
}
