package de.ur.explure.views

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import de.ur.explure.R
import de.ur.explure.SearchListAdapter
import de.ur.explure.viewmodel.WordSearchViewModel
import kotlinx.android.synthetic.main.fragment_word_search.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber


class WordSearchFragment : Fragment(R.layout.fragment_word_search) {

    private val viewModel: WordSearchViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView_searchResults.apply {
            layoutManager = LinearLayoutManager(activity ?: return)
            adapter = SearchListAdapter {
                Timber.d("Fragment")
            }
            setHasFixedSize(true)
        }
    }


}