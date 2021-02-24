package de.ur.explure.views

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import de.ur.explure.R
import de.ur.explure.adapter.RouteAdapter
import de.ur.explure.model.route.Route
import de.ur.explure.viewmodel.CreatedRoutesFragmentViewModel
import kotlinx.android.synthetic.main.fragment_created_routes.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class CreatedRoutesFragment : Fragment(R.layout.fragment_created_routes),
        RouteAdapter.OnItemClickListener, RouteAdapter.OnItemLongClickListener {

    private val viewModel: CreatedRoutesFragmentViewModel by viewModel()
    private lateinit var adapter: RouteAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = RouteAdapter(this, this)
        createdRoutesRecyclerView.adapter = adapter
        createdRoutesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        createdRoutesRecyclerView.setHasFixedSize(true)

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
        val dialog: AlertDialog
        val builder = AlertDialog.Builder(this.context)

        builder.setTitle("ROUTE LÖSCHEN")
        builder.setMessage("Sind Sie sicher, dass Sie die Route '${route.title}' entfernen möchten?")

        val dialogClickListener = DialogInterface.OnClickListener { _, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    viewModel.deleteRoute(route)
                }
                DialogInterface.BUTTON_NEGATIVE -> {
                    // do nothing
                }
            }
        }

        builder.setPositiveButton("JA", dialogClickListener)
        builder.setNegativeButton("NEIN", dialogClickListener)

        dialog = builder.create()
        dialog.show()
    }
}
