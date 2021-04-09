package de.ur.explure.views

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.crazylegend.viewbinding.viewBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.ur.explure.R
import de.ur.explure.adapter.RouteEditAdapter
import de.ur.explure.databinding.RouteWaypointBottomsheetBinding
import de.ur.explure.model.waypoint.WayPointDTO
import de.ur.explure.utils.ItemDragHelper
import de.ur.explure.viewmodel.EditRouteViewModel
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.scope.emptyState

class RouteWaypointBottomSheet : Fragment(R.layout.route_waypoint_bottomsheet),
    ItemDragHelper.OnDragStartListener, RouteEditAdapter.OnDeleteItemListener {

    private val binding by viewBinding(RouteWaypointBottomsheetBinding::bind)

    // see https://stackoverflow.com/questions/59094242/get-sharedviewmodel-in-childfragment-using-koin-and-navargs
    // private val editRouteViewModel: EditRouteViewModel by lazy { requireParentFragment().getViewModel() }
    private val editRouteViewModel: EditRouteViewModel by sharedViewModel(state = emptyState())

    private var routeEditAdapter: RouteEditAdapter? = null
    private var itemTouchHelper: ItemTouchHelper? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeViewModel()
        setupWaypointList()
    }

    private fun observeViewModel() {
        editRouteViewModel.routeWayPoints.observe(viewLifecycleOwner, { waypoints ->
            routeEditAdapter?.waypointList = waypoints
        })
    }

    private fun setupWaypointList() {
        val linearLayoutManager = LinearLayoutManager(activity ?: return)
        linearLayoutManager.stackFromEnd = true // insert items at the bottom instead of top

        routeEditAdapter = RouteEditAdapter(this, this) { wayPoint: WayPointDTO, _ ->
            // a item in the recyclerView was clicked
            editRouteViewModel.selectedMarker.value = wayPoint
        }

        binding.recyclerWaypointList.apply {
            adapter = routeEditAdapter
            layoutManager = linearLayoutManager

            addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                // show a text message if the recyclerView is empty
                if (routeEditAdapter?.itemCount != 0) {
                    binding.emptyRecyclerWaypointText.visibility = View.GONE
                } else {
                    binding.emptyRecyclerWaypointText.visibility = View.VISIBLE
                }
            }
        }

        // TODO: drag and drop MUST also update the viewmodel marker list to change the route!!

        // enable drag & drop on this recyclerview
        val callback: ItemTouchHelper.Callback = ItemDragHelper(routeEditAdapter ?: return)
        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper?.attachToRecyclerView(binding.recyclerWaypointList)
    }

    private fun showDeleteWaypointDialog(marker: WayPointDTO) {
        with(MaterialAlertDialogBuilder(requireActivity())) {
            setMessage(R.string.delete_waypoint_confirmation)
            setPositiveButton(R.string.yes) { _, _ ->
                editRouteViewModel.removeWaypointFromSheet(marker)
            }
            setNegativeButton(R.string.cancel) { _, _ -> }
            show()
        }
    }

    override fun onStartDrag(viewHolder: RouteEditAdapter.ViewHolder) {
        itemTouchHelper?.startDrag(viewHolder)
    }

    override fun onItemDeleted(waypoint: WayPointDTO, layoutPosition: Int) {
        showDeleteWaypointDialog(waypoint)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        itemTouchHelper?.attachToRecyclerView(null)

        // reset adapter when finished
        routeEditAdapter?.waypointList = emptyList()
    }
}
