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

    lateinit var currentNavController: LiveData<NavController>

    /**
     * Sets new observable navigation controller
     *
     * @param [LiveData]<[NavController]> object with the new navigation controller which should be observed
     */

    fun setNavigationController(navController: LiveData<NavController>) {
        currentNavController = navController
        setNavigationGraphInAppRouter()
    }

    /**
     * Sets navigation graph in AppRouter
     *
     */

    private fun setNavigationGraphInAppRouter() {
        val navController = currentNavController.value ?: return
        appRouter.setGraph(navController)
    }

    /**
     * Navigate ups in the current navigation controller.
     *
     * @return Returns [Boolean] value = true if backstack is not empty | false if backstack is empty.
     */

    fun navigateUp(): Boolean {
        return if (this::currentNavController.isInitialized) {
            currentNavController.value?.navigateUp() ?: false
        } else {
            false
        }
    }
}
