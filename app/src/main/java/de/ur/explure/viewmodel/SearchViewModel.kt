package de.ur.explure.viewmodel

import androidx.lifecycle.ViewModel
import de.ur.explure.R
import de.ur.explure.navigation.MainAppRouter
import de.ur.explure.navigation.StateAppRouter

class SearchViewModel(
    private val mainAppRouter: MainAppRouter
) : ViewModel() {

    fun navigateToCategoryWork() {
        mainAppRouter.getNavController().navigate(R.id.categoryWorkFragment)
    }

    fun navigateToSearchResult() {
        mainAppRouter.getNavController().navigate(R.id.wordSearchFragment)
    }
}
