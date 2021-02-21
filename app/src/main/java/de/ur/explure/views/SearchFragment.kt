package de.ur.explure.views

import android.util.Log
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import de.ur.explure.R
import de.ur.explure.viewmodel.SearchViewModel
import kotlinx.android.synthetic.main.fragment_search.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class SearchFragment : Fragment(R.layout.fragment_search) {

    private val viewModel: SearchViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setOnClickListeners()
    }


    private fun setOnClickListeners(){
        searchButton.setOnClickListener{
            Log.d("ABCD","Vor viewModel.navigateToSearchResult")
            viewModel.navigateToSearchResult()
        }

        chillenButton.setOnClickListener {
            viewModel.navigateToCategoryWork()
        }

        lernenButton.setOnClickListener {
            viewModel.navigateToCategoryWork()
        }

        freizeitButton.setOnClickListener {
            viewModel.navigateToCategoryWork()
        }

        cafeteButton.setOnClickListener {
            viewModel.navigateToCategoryWork()
        }

        organisationButton.setOnClickListener {
            viewModel.navigateToCategoryWork()
        }

        bibliothekenButton.setOnClickListener {
            viewModel.navigateToCategoryWork()
        }
//insert navigateToMap
        searchMapButton.setOnClickListener {
            viewModel.navigateToCategoryWork()
        }
    }
}
