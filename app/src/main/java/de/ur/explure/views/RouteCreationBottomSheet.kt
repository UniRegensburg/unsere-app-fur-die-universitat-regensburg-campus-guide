package de.ur.explure.views

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.crazylegend.viewbinding.viewBinding
import com.mapbox.mapboxsdk.geometry.LatLng
import de.ur.explure.R
import de.ur.explure.adapter.RouteCreationAdapter
import de.ur.explure.databinding.RouteCreationBottomsheetBinding
import de.ur.explure.model.waypoint.WayPoint
import de.ur.explure.viewmodel.MapViewModel
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.scope.emptyState

class RouteCreationBottomSheet : Fragment(R.layout.route_creation_bottomsheet) {

    private val binding by viewBinding(RouteCreationBottomsheetBinding::bind)

    // see https://stackoverflow.com/questions/59094242/get-sharedviewmodel-in-childfragment-using-koin-and-navargs
    // private val mapViewModel: MapViewModel by lazy { requireParentFragment().getViewModel() }
    private val mapViewModel: MapViewModel by sharedViewModel(state = emptyState())

    private var routeCreationAdapter: RouteCreationAdapter? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeViewModel()
    }

    private fun observeViewModel() {
        mapViewModel.manualRouteCreationModeActive.observe(viewLifecycleOwner, { active ->
            if (active) {
                showRouteCreationSheet()
            } else {
                // reset adapter when finished
                routeCreationAdapter?.waypointList = emptyList()
            }
        })
        mapViewModel.customRouteWaypoints.observe(viewLifecycleOwner, { routeWaypoints ->
            routeCreationAdapter?.waypointList = routeWaypoints
        })
    }

    private fun showRouteCreationSheet() {
        setupWaypointList()
    }

    private fun setupWaypointList() {
        val linearLayoutManager = LinearLayoutManager(activity ?: return)
        linearLayoutManager.stackFromEnd = true // insert items at the bottom instead of top

        routeCreationAdapter = RouteCreationAdapter { wayPoint: WayPoint, adapterPosition: Int ->
            // a item in the recyclerView was clicked, get the marker symbol for this waypoint
            mapViewModel.getActiveMarkerSymbols().forEach {
                val geoPoint = wayPoint.geoPoint
                val wayPointLocation = LatLng(geoPoint.latitude, geoPoint.longitude)
                // TODO not the best way probably, but should be safe, as there can only be one marker
                //  at a specific location
                if (it.latLng == wayPointLocation) {
                    mapViewModel.selectedMarker.value = it
                }
            }
        }

        binding.recyclerWaypointList.apply {
            adapter = routeCreationAdapter
            layoutManager = linearLayoutManager

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
