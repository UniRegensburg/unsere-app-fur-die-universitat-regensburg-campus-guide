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
            binding.progressBar.visibility = View.GONE
        })
    }

    private fun observeRouteResult() {
        viewModel.noRoutes.observe(viewLifecycleOwner, {
            if (it == true) {
                binding.progressBar.visibility = View.GONE
                binding.noResults.visibility = View.VISIBLE
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

        if (category == getString(R.string.work_ID)) {
            binding.tvTitelCategory.text = getString(R.string.work_title)
        }
        if (category == getString(R.string.bib_ID)) {
            binding.tvTitelCategory.text = getString(R.string.bib_title)
        }
        if (category == getString(R.string.chill_ID)) {
            binding.tvTitelCategory.text = getString(R.string.chill_title)
        }
        if (category == getString(R.string.orga_ID)) {
            binding.tvTitelCategory.text = getString(R.string.orga_title)
        }
        if (category == getString(R.string.sport_ID)) {
            binding.tvTitelCategory.text = getString(R.string.sport_title)
        }
        if (category == getString(R.string.coffee_ID)) {
            binding.tvTitelCategory.text = getString(R.string.coffee_title)
        }
    }
}
