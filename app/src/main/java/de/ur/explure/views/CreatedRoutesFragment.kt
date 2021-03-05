package de.ur.explure.views

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import de.ur.explure.R
import de.ur.explure.adapter.RouteAdapter
import de.ur.explure.model.route.Route
import de.ur.explure.viewmodel.CreatedRoutesViewModel
import kotlinx.android.synthetic.main.fragment_created_routes.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class CreatedRoutesFragment : Fragment(R.layout.fragment_created_routes),
        RouteAdapter.OnItemClickListener, RouteAdapter.OnItemLongClickListener {

    private val viewModel: CreatedRoutesViewModel by viewModel()
    private lateinit var adapter: RouteAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeAdapter()

        observeUserModel()
        observeRouteModel()
        viewModel.getUserInfo()
        viewModel.getCreatedRoutes()
    }

    private fun initializeAdapter() {
        adapter = RouteAdapter(this, this)
        createdRoutesRecyclerView.adapter = adapter
        createdRoutesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        createdRoutesRecyclerView.setHasFixedSize(true)
    }

    private fun observeUserModel() {
        viewModel.user.observe(viewLifecycleOwner, { user ->
            if (user != null) {
                userNameTextView.text = user.name
            }
        })
    }

    private fun observeRouteModel() {
        viewModel.createdRoutes.observe(viewLifecycleOwner, { routes ->
            if (routes != null) {
                adapter.routeList = routes
            }
        })
    }

    override fun onItemClick(position: Int) {
        viewModel.navigateToSinglePage()
    }

    override fun onItemLongClick(position: Int) {
        showDialog(adapter.routeList[position])
    }

    private fun showDialog(route: Route) {
        with(AlertDialog.Builder(requireContext())) {
            setTitle(resources.getString(R.string.delete_route))
            setMessage(resources.getString(R.string.delete_route_message, route.title))
            setPositiveButton(resources.getString(R.string.yes)) { _, _ -> viewModel.deleteRoute(route) }
            setNegativeButton(resources.getString(R.string.no)) { _, _ -> }
            show()
        }
    }
}
