package de.ur.explure.views

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.crazylegend.viewbinding.viewBinding
import de.ur.explure.R
import de.ur.explure.adapter.SearchListAdapter
import de.ur.explure.databinding.FragmentTextQueryBinding
import de.ur.explure.viewmodel.WordSearchViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class TextQueryFragment : Fragment(R.layout.fragment_text_query) {

    private val binding by viewBinding(FragmentTextQueryBinding::bind)
    private val args: TextQueryFragmentArgs by navArgs()

    private val viewModel: WordSearchViewModel by viewModel()
    private lateinit var searchAdapter: SearchListAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeAdapter()
        observeRouteModel()
        getRoutes()
    }

    private fun observeRouteModel() {
        viewModel.searchedRoutes.observe(viewLifecycleOwner, { routes ->
            if (routes != null){
                if (routes.isEmpty()){
                    binding.progressBar.visibility = View.GONE
                    binding.noResults.visibility = View.VISIBLE
                } else {
                    searchAdapter.submitList(routes)
                    binding.progressBar.visibility = View.GONE
                    binding.titlePageSearchResult.visibility = View.VISIBLE
                }
            }
        })
    }


    private fun initializeAdapter() {
        // viewModel.setupAlgolia()
        searchAdapter = SearchListAdapter { }

        binding.recyclerViewSearchResults.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = searchAdapter
        }
    }

    private fun getRoutes() {
        val message = args.textQueryKey
        viewModel.getSearchedRoutes(message)
    }
}
