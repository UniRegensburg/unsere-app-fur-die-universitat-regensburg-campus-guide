package de.ur.explure.views

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.crazylegend.viewbinding.viewBinding
import de.ur.explure.R
import de.ur.explure.adapter.SearchListAdapter
import de.ur.explure.databinding.FragmentCategoryQueryBinding
import de.ur.explure.viewmodel.CategoryViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class CategoryQueryFragment : Fragment(R.layout.fragment_category_query) {

    private val binding by viewBinding(FragmentCategoryQueryBinding::bind)
    private val args: CategoryQueryFragmentArgs by navArgs()

    private val viewModel: CategoryViewModel by viewModel()
    private lateinit var categoryAdapter: SearchListAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setTitle()
        initializeAdapter()
        getRoutes()
        observeRouteModel()
    }

    private fun observeRouteModel() {
        viewModel.categoryRoutes.observe(viewLifecycleOwner, { routes ->
            if (routes != null) {
                if (routes.isEmpty()) {
                    binding.progressBar.visibility = View.GONE
                    binding.noResults.visibility = View.VISIBLE
                } else {
                    categoryAdapter.submitList(routes)
                    binding.progressBar.visibility = View.GONE
                }
            }
        })
    }

    private fun initializeAdapter() {
        // viewModel.setupAlgolia()
        categoryAdapter = SearchListAdapter {
            viewModel.showRouteDetails(it.id)
        }

        binding.recyclerViewCategoryResults.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = categoryAdapter
        }
    }

    private fun getRoutes() {
        val category = args.categoryQueryKey
        viewModel.getCategoryRoutes(category)
    }

    private fun setTitle() {
        val category = args.categoryQueryKey
        binding.tvTitelCategory.text = category.name
    }
}
