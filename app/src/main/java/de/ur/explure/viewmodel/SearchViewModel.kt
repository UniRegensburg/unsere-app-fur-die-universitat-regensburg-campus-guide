package de.ur.explure.viewmodel

import androidx.lifecycle.ViewModel
import de.ur.explure.navigation.MainAppRouter

class SearchViewModel(
    private val mainAppRouter: MainAppRouter
) : ViewModel() {

    fun navigateToCategoryWork() {
        // mainAppRouter.getNavController()?.navigate(R.id.c)
    }

    fun navigateToSearchResult(search: String) {
        // stateAppRouter.navigateToSearchResult(search)
    }
}
