package de.ur.explure.views

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.crazylegend.viewbinding.viewBinding
import com.google.android.material.snackbar.Snackbar
import de.ur.explure.R
import de.ur.explure.adapter.CategoryDiscoverAdapter
import de.ur.explure.adapter.RouteDiscoverAdapter
import de.ur.explure.databinding.FragmentDiscoverBinding
import de.ur.explure.utils.showSnackbar
import de.ur.explure.viewmodel.DiscoverViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

@Suppress("TooManyFunctions", "StringLiteralDuplication", "FunctionMaxLength")
class DiscoverFragment : Fragment(R.layout.fragment_discover) {

    private val binding by viewBinding(FragmentDiscoverBinding::bind)
    private val discoverViewModel: DiscoverViewModel by viewModel()

    private lateinit var categoryAdapter: CategoryDiscoverAdapter

    private lateinit var latestRoutesAdapter: RouteDiscoverAdapter

    private lateinit var popularRoutesAdapter: RouteDiscoverAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // unlock the screen rotation if it was locked during the login process
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR

        initializeAdapter()
        startShimmer()
        initObservers()
        observeRecyclerViewScroll()
        setOnClickListeners()
        setupSearchBar()
        getData()
        setOnSwipeListener()
    }

    private fun setOnSwipeListener() {
        binding.pullToRefresh.setOnRefreshListener {
            discoverViewModel.getPopularRoutes()
            discoverViewModel.getLatestRoutes()
            discoverViewModel.getCategories()
        }
    }

    private fun startShimmer() {
        binding.shimmerCategoryLayout.shimmerCategoryLayout.startShimmer()
        binding.shimmerLatestRouteLayout.shimmerRouteLayout.startShimmer()
        binding.shimmerPopularRouteLayout.shimmerRouteLayout.startShimmer()
    }

    private fun observeRecyclerViewScroll() {
        observePopularListScroll()
        observeLatestListScroll()
    }

    private fun getData() {
        discoverViewModel.getCategories()
        discoverViewModel.getLatestRoutes()
        discoverViewModel.getPopularRoutes()
    }

    private fun initObservers() {
        initCategoryObserver()
        initLatestRouteObserver()
        initPopularRouteObserver()
        initCategoryErrorObserver()
        initRouteErrorObserver()
    }

    private fun initializeAdapter() {
        initCategoryAdapter()
        initLatestRouteAdapter()
        initPopularRouteAdapter()
    }

    private fun initCategoryObserver() {
        discoverViewModel.categories.observe(viewLifecycleOwner, { categories ->
            if (categories != null) {
                categoryAdapter.setList(categories)
                stopShimmerAndVisibilityOfCategories()
            }
            binding.pullToRefresh.isRefreshing = false
        })
    }

    private fun initLatestRouteObserver() {
        discoverViewModel.latestRouteList.observe(viewLifecycleOwner, { latestRoutes ->
            if (latestRoutes != null) {
                latestRoutesAdapter.items = latestRoutes
                latestRoutesAdapter.notifyDataSetChanged()
                stopShimmerAndVisibilityOfLatestRoutes()
            }
            binding.pullToRefresh.isRefreshing = false
        })
    }

    private fun initPopularRouteObserver() {
        discoverViewModel.popularRouteList.observe(viewLifecycleOwner, { popularRoutes ->
            if (popularRoutes != null) {
                popularRoutesAdapter.items = popularRoutes
                popularRoutesAdapter.notifyDataSetChanged()
                stopShimmerAndVisibilityOfPopularRoutes()
            }
            binding.pullToRefresh.isRefreshing = false
        })
    }

    private fun initCategoryErrorObserver() {
        discoverViewModel.showCategoryError.observe(viewLifecycleOwner, { showCategoryError ->
            if (showCategoryError) {
                showCategoryErrorSnackBar()
            }
        })
    }

    private fun initRouteErrorObserver() {
        discoverViewModel.showRouteError.observe(viewLifecycleOwner, { showRouteError ->
            if (showRouteError) {
                showRouteErrorSnackBar()
            }
        })
    }

    private fun setOnClickListeners() {
        binding.tvNewRouteMore.setOnClickListener {
            discoverViewModel.getLatestRoutes()
        }
        binding.tvPopularRouteMore.setOnClickListener {
            discoverViewModel.getPopularRoutes()
        }
    }

    private fun showCategoryErrorSnackBar() {
        showSnackbar(
            requireActivity(),
            R.string.discover_category_error,
            R.id.discover_container,
            Snackbar.LENGTH_SHORT
        ) {
            discoverViewModel.resetCategoryErrorFlag()
        }
    }

    private fun showRouteErrorSnackBar() {
        showSnackbar(
            requireActivity(),
            R.string.discover_route_error,
            R.id.discover_container,
            Snackbar.LENGTH_SHORT
        ) {
            discoverViewModel.resetRouteErrorFlag()
        }
    }

    private fun stopShimmerAndVisibilityOfLatestRoutes() {
        binding.shimmerLatestRouteLayout.shimmerRouteLayout.stopShimmer()
        binding.vsNewRoutes.displayedChild = RECYCLER_VIEW_VIEW_POSITION
    }

    private fun stopShimmerAndVisibilityOfPopularRoutes() {
        binding.shimmerPopularRouteLayout.shimmerRouteLayout.stopShimmer()
        binding.vsPopularRoutes.displayedChild = RECYCLER_VIEW_VIEW_POSITION
    }

    private fun stopShimmerAndVisibilityOfCategories() {
        binding.shimmerCategoryLayout.shimmerCategoryLayout.stopShimmer()
        binding.vsCategories.displayedChild = RECYCLER_VIEW_VIEW_POSITION
    }

    private fun observePopularListScroll() {
        binding.rvPopularRouteList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (!recyclerView.canScrollVertically(1) && newState == RecyclerView.SCROLL_STATE_IDLE) {
                    binding.tvPopularRouteMore.visibility = View.VISIBLE
                }
            }
        })
    }

    private fun observeLatestListScroll() {
        binding.rvNewRouteList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (!recyclerView.canScrollVertically(1) && newState == RecyclerView.SCROLL_STATE_IDLE) {
                    binding.tvNewRouteMore.visibility = View.VISIBLE
                }
            }
        })
    }

    private fun setupSearchBar() {
        binding.svRouteSearch.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                if (query.isNotEmpty()) {
                    discoverViewModel.startTextQuery(query)
                }
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean = false
        })
    }

    private fun initLatestRouteAdapter() {
        latestRoutesAdapter = RouteDiscoverAdapter {
            discoverViewModel.showRouteDetails(it.id)
        }
        binding.rvNewRouteList.adapter = latestRoutesAdapter
        binding.rvNewRouteList.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
    }

    private fun initCategoryAdapter() {
        categoryAdapter = CategoryDiscoverAdapter {
            discoverViewModel.startCategoryQuery(it)
        }
        binding.rvCategoryList.adapter = categoryAdapter
        binding.rvCategoryList.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
    }

    private fun initPopularRouteAdapter() {
        popularRoutesAdapter = RouteDiscoverAdapter {
            discoverViewModel.showRouteDetails(it.id)
        }
        binding.rvPopularRouteList.adapter = popularRoutesAdapter
        binding.rvPopularRouteList.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
    }

    companion object {
        const val RECYCLER_VIEW_VIEW_POSITION = 1
    }
}
