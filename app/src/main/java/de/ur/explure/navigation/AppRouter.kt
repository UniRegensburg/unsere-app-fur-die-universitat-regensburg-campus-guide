package de.ur.explure.navigation

import androidx.navigation.NavController

/**
 * Router class used for navigation operations
 *
 */

class AppRouter {

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
}
