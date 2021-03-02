package de.ur.explure.views

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.crazylegend.viewbinding.viewBinding
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.snackbar.Snackbar
import de.ur.explure.R
import de.ur.explure.adapter.CategoryDiscoverAdapter
import de.ur.explure.adapter.RouteDiscoverAdapter
import de.ur.explure.databinding.FragmentDiscoverBinding
import de.ur.explure.utils.showSnackbar
import de.ur.explure.viewmodel.DiscoverViewModel
import kotlinx.android.synthetic.main.fragment_discover.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

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
        getData()

        // Delete
        binding.showMapButton.setOnClickListener {
            discoverViewModel.showMap()
        }
    }

    override fun onDestroyView() {
        detachAdapterOnViewDetach(rv_popular_route_list)
        detachAdapterOnViewDetach(rv_new_route_list)
        super.onDestroyView()
    }

    private fun startShimmer() {
        (shimmer_popular_route_layout as ShimmerFrameLayout).startShimmer()
        (shimmer_latest_route_layout as ShimmerFrameLayout).startShimmer()
        (shimmer_category_layout as ShimmerFrameLayout).startShimmer()
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
        })
    }

    private fun initLatestRouteObserver() {
        discoverViewModel.latestRouteList.observe(viewLifecycleOwner, { latestRoutes ->
            if (latestRoutes != null) {
                latestRoutesAdapter.setList(latestRoutes)
                stopShimmerAndVisibilityOfLatestRoutes()
            }
        })
    }

    private fun initPopularRouteObserver() {
        discoverViewModel.popularRouteList.observe(viewLifecycleOwner, { popularRoutes ->
            if (popularRoutes != null) {
                popularRoutesAdapter.setList(popularRoutes)
                stopShimmerAndVisibilityOfPopularRoutes()
            }
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
        tv_new_route_more.setOnClickListener {
            discoverViewModel.getLatestRoutes()
        }
        tv_popular_route_more.setOnClickListener {
            discoverViewModel.getPopularRoutes()
        }
    }

    private fun showCategoryErrorSnackBar() {
        showSnackbar(
            requireActivity(),
            R.string.discover_category_error,
            R.id.discover_container,
            Snackbar.LENGTH_SHORT, null
        ) {
            discoverViewModel.resetCategoryErrorFlag()
        }
    }

    private fun showRouteErrorSnackBar() {
        showSnackbar(
            requireActivity(),
            R.string.discover_route_error,
            R.id.discover_container,
            Snackbar.LENGTH_SHORT, null
        ) {
            discoverViewModel.resetRouteErrorFlag()
        }
    }

    private fun stopShimmerAndVisibilityOfLatestRoutes() {
        (shimmer_latest_route_layout as ShimmerFrameLayout).stopShimmer()
        shimmer_latest_route_layout.visibility = View.GONE
    }

    private fun stopShimmerAndVisibilityOfPopularRoutes() {
        (shimmer_popular_route_layout as ShimmerFrameLayout).stopShimmer()
        shimmer_popular_route_layout.visibility = View.GONE
    }

    private fun stopShimmerAndVisibilityOfCategories() {
        (shimmer_category_layout as ShimmerFrameLayout).stopShimmer()
        shimmer_category_layout.visibility = View.GONE
    }

    private fun observePopularListScroll() {
        rv_popular_route_list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (!recyclerView.canScrollVertically(1) && newState == RecyclerView.SCROLL_STATE_IDLE) {
                    tv_popular_route_more.visibility = View.VISIBLE
                }
            }
        })
    }

    private fun observeLatestListScroll() {
        rv_new_route_list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (!recyclerView.canScrollVertically(1) && newState == RecyclerView.SCROLL_STATE_IDLE) {
                    tv_new_route_more.visibility = View.VISIBLE
                }
            }
        })
    }

    private fun initLatestRouteAdapter() {
        latestRoutesAdapter = RouteDiscoverAdapter {
            Timber.d("%s clicked", it.title)
        }
        rv_new_route_list.adapter = latestRoutesAdapter
        rv_new_route_list.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
    }

    private fun initCategoryAdapter() {
        categoryAdapter = CategoryDiscoverAdapter {
            Timber.d("%s clicked", it.name)
        }
        rv_category_list.adapter = categoryAdapter
        rv_category_list.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
    }

    private fun initPopularRouteAdapter() {
        popularRoutesAdapter = RouteDiscoverAdapter {
            Timber.d("%s clicked", it.title)
        }
        rv_popular_route_list.adapter = popularRoutesAdapter
        rv_popular_route_list.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
    }

    private fun detachAdapterOnViewDetach(recyclerView: RecyclerView) {
        recyclerView.addOnAttachStateChangeListener(object :
            View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) = run { }

            override fun onViewDetachedFromWindow(v: View) {
                recyclerView.adapter = null
            }
        })
    }
}
