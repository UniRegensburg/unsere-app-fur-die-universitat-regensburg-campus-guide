package de.ur.explure.viewmodel

import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import de.ur.explure.navigation.AppRouter

/**
 * Main Viewmodel used to handle initializing operations for main app
 *
 * @property appRouter Navigation router as [AppRouter] used for navigation operations
 */

class MainViewModel(private val appRouter: AppRouter) : ViewModel() {

    /**
     * Sets navigation graph in AppRouter
     *
     */

    fun initializeNavController(navController: NavController) {
        appRouter.initializeNavController(navController)
    }

    /**
     * Navigate ups in the current navigation controller.
     *
     * @return Returns [Boolean] value = true if backstack is not empty | false if backstack is empty.
     */

    fun navigateUp(): Boolean {
        return appRouter.navigateUp()
    }
}
