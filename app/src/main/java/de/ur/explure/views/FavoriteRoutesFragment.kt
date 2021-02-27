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
import de.ur.explure.viewmodel.FavoriteRoutesViewModel
import kotlinx.android.synthetic.main.fragment_created_routes.*
import kotlinx.android.synthetic.main.fragment_favorite_routes.*
import kotlinx.android.synthetic.main.fragment_favorite_routes.userNameTextView
import org.koin.androidx.viewmodel.ext.android.viewModel

class FavoriteRoutesFragment : Fragment(R.layout.fragment_favorite_routes),
        RouteAdapter.OnItemClickListener, RouteAdapter.OnItemLongClickListener {

    private val viewModel: FavoriteRoutesViewModel by viewModel()
    private lateinit var adapter: RouteAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeAdapter()

        observeUserModel()
        observeRouteModel()
        viewModel.getUserInfo()
        viewModel.getFavoriteRoutes()
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
        viewModel.favoriteRoutes.observe(viewLifecycleOwner, { routes ->
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

        builder.setTitle(String.format(getResources().getString(R.string.remove_favorite)))
        builder.setMessage(String.format(getResources().getString(R.string.remove_favorite_message_one)) +
                route.title + String.format(getResources().getString(R.string.remove_favorite_message_two)))

        val dialogClickListener = DialogInterface.OnClickListener { _, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    viewModel.removeRouteFromFavoriteRoutes(route)
                }
                DialogInterface.BUTTON_NEGATIVE -> {
                    // do nothing
                }
            }
        }

        builder.setPositiveButton(String.format(getResources().getString(R.string.yes)), dialogClickListener)
        builder.setNegativeButton(String.format(getResources().getString(R.string.no)), dialogClickListener)

        dialog = builder.create()
        dialog.show()
    }
}
