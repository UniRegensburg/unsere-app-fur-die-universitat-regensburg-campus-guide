package de.ur.explure.views

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.crazylegend.viewbinding.viewBinding
import de.ur.explure.R
import de.ur.explure.adapter.WaypointsAdapter
import de.ur.explure.databinding.RouteCreationBottomsheetBinding
import de.ur.explure.model.waypoint.WayPoint

class RouteCreationBottomSheet : Fragment(R.layout.route_creation_bottomsheet) {

    private val binding by viewBinding(RouteCreationBottomsheetBinding::bind)

    private lateinit var waypointsAdapter: WaypointsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
