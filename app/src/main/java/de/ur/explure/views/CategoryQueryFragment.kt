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
        observeRouteResult()
        observeRouteModel()
    }

    private fun observeRouteModel() {
        viewModel.categoryRoutes.observe(viewLifecycleOwner, { routes ->
            categoryAdapter.submitList(routes)
            binding.progressBar.setVisibility(View.GONE)
        })
    }

    private fun observeRouteResult() {
        viewModel.noRoutes.observe(viewLifecycleOwner, {
            if (it == true) {
                binding.progressBar.setVisibility(View.GONE)
                binding.noResults.setVisibility(View.VISIBLE)
            }
        })
    }

    private fun initializeAdapter() {
        // viewModel.setupAlgolia()
        val category = args.categoryQueryKey
        categoryAdapter = SearchListAdapter { }

        binding.recyclerViewCategoryResults.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = categoryAdapter
        }

        viewModel.getCategoryRoutes(category)
    }

    private fun setTitle() {
        val category = args.categoryQueryKey
        val workID = Integer.parseInt(category)

        if (category == R.string.work_ID.toString()) {
            binding.tvTitelCategory.text = R.string.work_title.toString()
        }
        if (category == R.string.bib_ID.toString()) {
            binding.tvTitelCategory.text = R.string.bib_title.toString()
        }
        if (category == R.string.chill_ID.toString()) {
            binding.tvTitelCategory.text = R.string.chill_title.toString()
        }
        if (category == R.string.orga_ID.toString()) {
            binding.tvTitelCategory.text = R.string.orga_title.toString()
        }
        if (category == R.string.sport_ID.toString()) {
            binding.tvTitelCategory.text = R.string.sport_title.toString()
        }
        if (category == R.string.coffee_ID.toString()) {
            binding.tvTitelCategory.text = R.string.coffee_title.toString()
        }
    }
}
