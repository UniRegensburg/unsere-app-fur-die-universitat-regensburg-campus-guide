package de.ur.explure.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import de.ur.explure.navigation.AppRouter

/**
 * Main Viewmodel used to handle initializing operations for main app
 *
 * @property appRouter Navigation router as [AppRouter] used for navigation operations
 */
class MainViewModel(private val appRouter: AppRouter) : ViewModel() {

    private var currentNavController: LiveData<NavController>? = null

    fun getCurrentNavController(): LiveData<NavController>? {
        return currentNavController
    }

    /**
     * Sets the current nav controller in the viewModel.
     */
    fun setCurrentNavController(controller: LiveData<NavController>) {
        currentNavController = controller
    }

    /**
     * Sets the current navigation graph in the appRouter.
     */
    fun initializeNavController(navController: NavController) {
        appRouter.initializeNavController(navController)
    }

    fun resetCurrentNavController() {
        currentNavController = null
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
