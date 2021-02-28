package de.ur.explure.views

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.crazylegend.viewbinding.viewBinding
import de.ur.explure.R
import de.ur.explure.adapter.CategoryDiscoverAdapter
import de.ur.explure.databinding.FragmentDiscoverBinding
import de.ur.explure.viewmodel.DiscoverViewModel
import kotlinx.android.synthetic.main.fragment_discover.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class DiscoverFragment : Fragment(R.layout.fragment_discover) {

    private val binding by viewBinding(FragmentDiscoverBinding::bind)
    private val discoverViewModel: DiscoverViewModel by viewModel()

    private lateinit var categoryAdapter: CategoryDiscoverAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeAdapter()
        initObservers()
        getData()

        // /Delete
        binding.showMapButton.setOnClickListener {
            discoverViewModel.showMap()
        }
    }

    private fun getData() {
        discoverViewModel.getCategories()
    }

    private fun initObservers() {
        discoverViewModel.categories.observe(viewLifecycleOwner, { categories ->
            if (categories != null) {
                categoryAdapter.setList(categories)
            }
        })
    }

    private fun initializeAdapter() {
        initCategoryAdapter()
    }

    private fun initCategoryAdapter() {
        categoryAdapter = CategoryDiscoverAdapter {
            Timber.d("%s clicked", it.name)
        }
        rv_category_list.adapter = categoryAdapter
        rv_category_list.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
    }
}
