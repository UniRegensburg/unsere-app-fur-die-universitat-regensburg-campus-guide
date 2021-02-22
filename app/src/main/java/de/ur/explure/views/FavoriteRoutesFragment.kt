package de.ur.explure.views

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import de.ur.explure.R
import de.ur.explure.RouteAdapter
import de.ur.explure.viewmodel.FavoriteRoutesFragmentViewModel
import kotlinx.android.synthetic.main.fragment_created_routes.*
import kotlinx.android.synthetic.main.fragment_favorite_routes.*
import kotlinx.android.synthetic.main.fragment_favorite_routes.userNameTextView
import org.koin.androidx.viewmodel.ext.android.viewModel

class FavoriteRoutesFragment : Fragment(R.layout.fragment_favorite_routes), RouteAdapter.OnItemClickListener {

    private val viewModel: FavoriteRoutesFragmentViewModel by viewModel()
    private lateinit var adapter : RouteAdapter


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = RouteAdapter( this)
        favoriteRoutesRecyclerView.adapter = adapter
        favoriteRoutesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        favoriteRoutesRecyclerView.setHasFixedSize(true)

        observeUserModel()
        observeRouteModel()
        viewModel.getUserInfo()
        viewModel.getFavoriteRoutes()
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

    private fun showDialog(routeName: String) {
        val dialog: AlertDialog
        val builder = AlertDialog.Builder(this.context)

        builder.setTitle(routeName)
        builder.setMessage("Sind Sie sicher, dass Sie die Route starten möchten?")

        val dialogClickListener = DialogInterface.OnClickListener { _, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    // start navigation
                }
                DialogInterface.BUTTON_NEGATIVE -> {
                    // disable dialog
                }
            }
        }

        builder.setPositiveButton("JA", dialogClickListener)
        builder.setNegativeButton("NEIN", dialogClickListener)

        dialog = builder.create()
        dialog.show()
    }
}
