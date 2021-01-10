package de.ur.explure.navigation

import androidx.navigation.NavController
import de.ur.explure.R

/**
 * Router class used for navigation operations
 *
 */

class AppRouter {

    private lateinit var navController: NavController

    /**
     * Initializes navigation controller and sets navigation graph
     *
     * @param navController Navigation Controller as [NavController] used as main fragment-container
     */

    fun setGraph(navController: NavController) {
        this.navController = navController
        navController.setGraph(R.navigation.nav_graph)
    }
}