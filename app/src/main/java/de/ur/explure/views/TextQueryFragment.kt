package de.ur.explure.views

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import de.ur.explure.R
import de.ur.explure.SearchListAdapter
import de.ur.explure.config.BundleConfig
import de.ur.explure.viewmodel.WordSearchViewModel
import kotlinx.android.synthetic.main.fragment_text_query.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class TextQueryFragment : Fragment(R.layout.fragment_text_query) {

    private val viewModel: WordSearchViewModel by viewModel()
    lateinit var searchAdapter: SearchListAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val message = arguments?.getString(BundleConfig.TEXT_QUERY_KEY)
        searchAdapter = SearchListAdapter { }

        recyclerView_searchResults.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = searchAdapter
            // setHasFixedSize(false)
        }

        //viewModel.setupAlgolia()

        observeRouteModel()

        if (message != null) {
            viewModel.getSearchedRoutes(message)
        }
    }

    private fun observeRouteModel() {

        viewModel.searchedRoutes.observe(viewLifecycleOwner, { routes ->
            searchAdapter.submitList(routes)
        })
    }
}
