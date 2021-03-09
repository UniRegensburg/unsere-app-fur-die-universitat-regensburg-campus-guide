package de.ur.explure.views

import androidx.fragment.app.Fragment
import de.ur.explure.R
import de.ur.explure.viewmodel.SearchViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class SearchFragment : Fragment(R.layout.fragment_search) {

    private val viewModel: SearchViewModel by viewModel()
}
