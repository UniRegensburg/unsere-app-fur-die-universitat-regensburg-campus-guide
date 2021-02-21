package de.ur.explure.viewmodel


import android.util.Log
import androidx.lifecycle.ViewModel
import de.ur.explure.navigation.StateAppRouter
import kotlinx.android.synthetic.main.fragment_search.*


class SearchViewModel(
        private val stateAppRouter: StateAppRouter
) :    ViewModel(){

        fun navigateToCategoryWork() {
                stateAppRouter.navigateToCategoryWork()
        }

        fun navigateToSearchResult() {
                stateAppRouter.navigateToSearchResult()
        }
}