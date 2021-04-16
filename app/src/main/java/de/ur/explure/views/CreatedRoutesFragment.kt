package de.ur.explure.views

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.crazylegend.viewbinding.viewBinding
import com.google.firebase.storage.FirebaseStorage
import de.ur.explure.GlideApp
import de.ur.explure.R
import de.ur.explure.adapter.ProfileRouteAdapter
import de.ur.explure.databinding.FragmentCreatedRoutesBinding
import de.ur.explure.model.route.Route
import de.ur.explure.viewmodel.CreatedRoutesViewModel
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class CreatedRoutesFragment : Fragment(R.layout.fragment_created_routes),
    ProfileRouteAdapter.OnItemClickListener, ProfileRouteAdapter.OnItemLongClickListener {

    private val binding by viewBinding(FragmentCreatedRoutesBinding::bind)

    private val viewModel: CreatedRoutesViewModel by viewModel()
    private lateinit var adapter: ProfileRouteAdapter

    private var userId: String = ""

    private val fireStorage: FirebaseStorage by inject()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeAdapter()

        observeUserModel()
        observeRouteModel()
        viewModel.getUserInfo()
        viewModel.getCreatedRoutes()
    }

    private fun initializeAdapter() {
        adapter = ProfileRouteAdapter(this, this)
        binding.createdRoutesRecyclerView.adapter = adapter
        binding.createdRoutesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.createdRoutesRecyclerView.setHasFixedSize(true)
    }

    private fun observeUserModel() {
        viewModel.user.observe(viewLifecycleOwner, { user ->
            if (user != null) {
                binding.userNameTextView.text = user.name
                userId = user.id
                if (user.profilePictureUrl.isNotEmpty()) {
                    try {
                        val gsReference =
                                fireStorage.getReferenceFromUrl(user.profilePictureUrl)
                        GlideApp.with(requireContext())
                                .load(gsReference)
                                .skipMemoryCache(true)
                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                .error(R.drawable.user_profile_picture)
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .into(binding.profilePicture)
                    } catch (_: Exception) {
                    }
                }
            }
        })
    }

    private fun observeRouteModel() {
        viewModel.createdRoutes.observe(viewLifecycleOwner, { routes ->
            if (routes != null) {
                adapter.routeList = routes
                binding.loadingCircle.visibility = View.GONE
                binding.page.visibility = View.VISIBLE
            }
        })
    }

    override fun onItemClick(position: Int) {
        viewModel.navigateToSinglePage(adapter.routeList[position].id)
    }

    override fun onItemLongClick(position: Int) {
        showDialog(adapter.routeList[position])
    }

    private fun showDialog(route: Route) {
        with(AlertDialog.Builder(requireContext())) {
            setTitle(resources.getString(R.string.delete_route))
            setMessage(resources.getString(R.string.delete_route_message, route.title))
            setPositiveButton(resources.getString(R.string.yes)) { _, _ ->
                viewModel.deleteRoute(
                    route
                )
            }
            setNegativeButton(resources.getString(R.string.no)) { _, _ -> }
            show()
        }
    }
}
