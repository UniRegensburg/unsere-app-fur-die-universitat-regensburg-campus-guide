package de.ur.explure.views

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.crazylegend.viewbinding.viewBinding
import com.google.firebase.firestore.GeoPoint
import de.ur.explure.R
import de.ur.explure.adapter.CategorySpinnerAdapter
import de.ur.explure.adapter.WayPointCreateAdapter
import de.ur.explure.databinding.FragmentCreateRouteBinding
import de.ur.explure.model.waypoint.WayPointDTO
import de.ur.explure.viewmodel.CreateRouteViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class CreateRouteFragment : Fragment(R.layout.fragment_create_route) {

    companion object {
        const val WAYPOINT_EDIT_KEY = "WayPointEdit"
    }

    private val binding by viewBinding(FragmentCreateRouteBinding::bind)
    private val viewModel: CreateRouteViewModel by viewModel()


    private lateinit var categoryAdapter: CategorySpinnerAdapter
    private lateinit var wayPointAdapter: WayPointCreateAdapter


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initAdapters()
        setObservers()
        initWayPointEditObserver()
        val waypoint1 =
            WayPointDTO(
                "Mensa",
                GeoPoint(15.56, 33.2),
                "Hier gibts the Good Stuff",
                "nicht null",
                null,
                null
            )
        val waypoint2 =
            WayPointDTO(
                "Kugel",
                GeoPoint(25.56, 33.2),
                "Das hier ist ein ziemlich guter Wegpunkt",
                "nicht null",
                "nicht null",
                null
            )
        val waypoint3 =
            WayPointDTO(
                "PT Cafete",
                GeoPoint(15.56, 53.2),
                "Das hier ist ein ziemlich guter Wegpunkt",
                null,
                null,
                null
            )
        val list = listOf(waypoint1, waypoint2, waypoint3)
        viewModel.setWayPointDTOs(list)
        viewModel.getCategories()
    }

    private fun initWayPointEditObserver() {
        val navBackStackEntry = findNavController().getBackStackEntry(R.id.createRouteFragment)

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME
                && navBackStackEntry.savedStateHandle.contains(WAYPOINT_EDIT_KEY)
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
    }

    private fun setObservers() {
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


}