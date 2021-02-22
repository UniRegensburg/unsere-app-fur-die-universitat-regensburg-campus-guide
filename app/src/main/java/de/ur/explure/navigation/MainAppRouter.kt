package de.ur.explure.navigation

import androidx.navigation.NavController

/**
 * Router class used for navigation operations in the main app
 *
 */

class MainAppRouter {

    private lateinit var navController: NavController

    /**
     * Initializes navigation controller
     *
     * @param navController Navigation Controller as [NavController] used as main fragment-container
     */

    fun initializeNavController(navController: NavController) {
        this.navController = navController
    }

    fun navigateUp(): Boolean {
        return if (this::navController.isInitialized) {
            navController.navigateUp()
        } else {
            false
        }
    }

    fun getNullableNavController() : NavController? {
        return if (this::navController.isInitialized) {
            navController
        } else {
            null
        }
    }

    /**
     * Returns the current navigation controller or throws an Exception if none is found.
     * Used in the child fragments to access the current Navigation Graph.
     */
    fun getNavController(): NavController {
        if (this::navController.isInitialized) {
            return navController
        } else {
            throw UninitializedPropertyAccessException("No Navigation Controller is initialized in the AppRouter!")
        }
    }
}
