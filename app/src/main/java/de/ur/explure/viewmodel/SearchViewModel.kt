package de.ur.explure.viewmodel

import androidx.lifecycle.ViewModel
import de.ur.explure.navigation.StateAppRouter

class SearchViewModel(
    private val stateAppRouter: StateAppRouter
) : ViewModel() {

    fun navigateToCategoryWork() {
        stateAppRouter.navigateToCategoryWork()
    }

    fun navigateToSearchResult(search: String) {
        stateAppRouter.navigateToSearchResult(search)
    }
}
