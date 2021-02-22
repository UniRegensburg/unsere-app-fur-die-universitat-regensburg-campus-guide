package de.ur.explure.views

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import de.ur.explure.R
import de.ur.explure.RouteAdapter
import de.ur.explure.RouteItem
import de.ur.explure.viewmodel.CreatedRoutesFragmentViewModel
import kotlinx.android.synthetic.main.fragment_created_routes.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class CreatedRoutesFragment : Fragment(R.layout.fragment_created_routes) {

    private val viewModel: CreatedRoutesFragmentViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeUserModel()
        observeRouteModel()
        viewModel.getUserInfo()
        viewModel.getCreatedRoutes()
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
                val list = ArrayList<RouteItem>()
                for (route in routes) {
                    val item = RouteItem(R.drawable.ic_home, route.description, route.createdAt.toString())
                    list += item
                }
                createdRoutesRecyclerView.adapter = RouteAdapter(list)
                createdRoutesRecyclerView.layoutManager = LinearLayoutManager(this.context)
                createdRoutesRecyclerView.setHasFixedSize(true)
            }
        })
    }
}
