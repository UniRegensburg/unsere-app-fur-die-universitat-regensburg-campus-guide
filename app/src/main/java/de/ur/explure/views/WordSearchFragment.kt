package de.ur.explure.views

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import de.ur.explure.R
import de.ur.explure.SearchListAdapter
import de.ur.explure.model.route.Route
import de.ur.explure.viewmodel.WordSearchViewModel
import kotlinx.android.synthetic.main.fragment_word_search.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber


class WordSearchFragment : Fragment(R.layout.fragment_word_search) {

    private val viewModel: WordSearchViewModel by viewModel()
    lateinit var searchAdapter: SearchListAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        searchAdapter = SearchListAdapter {  }

        recyclerView_searchResults.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = searchAdapter
            //setHasFixedSize(false)
        }

        observeRouteModel()

        viewModel.getSearchedRoutes()

    }

    private fun observeRouteModel() {

        viewModel.searchedRoutes.observe(viewLifecycleOwner, { routes ->
            searchAdapter.routeList = routes
            Log.d("Koller2",routes.toString())
            Log.d("Koller3",searchAdapter.routeList.toString())

            })
        }

    }