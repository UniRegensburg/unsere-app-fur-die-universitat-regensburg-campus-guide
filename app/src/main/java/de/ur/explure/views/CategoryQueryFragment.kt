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
        initializeAdapter()
        //observeRouteResult()
        observeRouteModel()
    }

    private fun observeRouteModel() {
        viewModel.categoryRoutes.observe(viewLifecycleOwner, { routes ->
            categoryAdapter.submitList(routes)
            //binding.progressBar.setVisibility(View.GONE)
            //binding.titlePageSearchResult.setVisibility(View.VISIBLE)
        })
    }

    /*private fun observeRouteResult() {
        viewModel.noRoutes.observe(viewLifecycleOwner, {
            if (it == true) {
                binding.progressBar.setVisibility(View.GONE)
                binding.noResults.setVisibility(View.VISIBLE)
            }
        })
    }*/

    private fun initializeAdapter() {
        // viewModel.setupAlgolia()
        val category = args.categoryQueryKey
        categoryAdapter = SearchListAdapter { }

        binding.recyclerViewCategoryResults.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = categoryAdapter
        }

        if (category != null) {
            viewModel.getCategoryRoutes(category)
        }
    }
}

