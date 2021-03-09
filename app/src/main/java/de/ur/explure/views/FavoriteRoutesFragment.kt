package de.ur.explure.views

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.crazylegend.viewbinding.viewBinding
import de.ur.explure.R
import de.ur.explure.adapter.ProfileRouteAdapter
import de.ur.explure.databinding.FragmentFavoriteRoutesBinding
import de.ur.explure.model.route.Route
import de.ur.explure.viewmodel.FavoriteRoutesViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class FavoriteRoutesFragment : Fragment(R.layout.fragment_favorite_routes),
    ProfileRouteAdapter.OnItemClickListener, ProfileRouteAdapter.OnItemLongClickListener {

    private val binding by viewBinding(FragmentFavoriteRoutesBinding::bind)

    private val viewModel: FavoriteRoutesViewModel by viewModel()
    private lateinit var adapter: ProfileRouteAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeAdapter()

        observeUserModel()
        observeRouteModel()
        viewModel.getUserInfo()
        viewModel.getFavoriteRoutes()
    }

    private fun initializeAdapter() {
        adapter = ProfileRouteAdapter(this, this)
        binding.favoriteRoutesRecyclerView.adapter = adapter
        binding.favoriteRoutesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.favoriteRoutesRecyclerView.setHasFixedSize(true)
    }

    private fun observeUserModel() {
        viewModel.user.observe(viewLifecycleOwner, { user ->
            if (user != null) {
                binding.userNameTextView.text = user.name
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
        with(AlertDialog.Builder(requireContext())) {
            setTitle(resources.getString(R.string.remove_favorite))
            setMessage(resources.getString(R.string.remove_favorite_message, route.title))
            setPositiveButton(resources.getString(R.string.yes)) { _, _ ->
                viewModel.removeRouteFromFavoriteRoutes(route)
            }
            setNegativeButton(resources.getString(R.string.no)) { _, _ -> }
            show()
        }
    }
}
