package de.ur.explure.navigation

import android.util.Log
import androidx.navigation.NavController
import de.ur.explure.R

/**
 * Navigation router used for login and register and switching between auth states.
 *
 */

class StateAppRouter {

    private lateinit var navController: NavController

    /**
     * Initializes the navController
     *
     * @param navController Current NavController
     */

    fun initializeNavController(navController: NavController) {
        this.navController = navController
        this.navController.setGraph(R.navigation.nav_graph_auth)
    }

    /**
     * Sets the Navigation graph to auth graph and navigates to LoginFragment while removing
     * stateFragment from Backstack
     */

    fun navigateToLogin() {
        navController.setGraph(R.navigation.nav_graph_auth)
        navController.navigate(R.id.navigateAndPopBackStackToLoginFragment)
    }

    /**
     * Navigates to BottomNavFragment to init main application
     *
     */

    fun navigateToMainApp() {
        navController.setGraph(R.navigation.nav_graph_host)
    }

    fun navigateToRegister() {
        navController.navigate(R.id.navigateToRegister)
    }

    fun navigateUp(): Boolean {
        return if (this::navController.isInitialized) {
            navController.navigateUp()
        } else {
            false
        }
    }

    /**
     * Navigation for the SearchFragment
     */
    fun navigateFromSearch() {
        navController.setGraph(R.navigation.nav_graph_search)
    }

    fun navigateToCategoryWork(){
        navController.setGraph(R.navigation.nav_graph_search)
        navController.navigate(R.id.navigateToCategoryWork)
    }

    fun navigateToSearchResult(){
        navController.setGraph(R.navigation.nav_graph_search)
        navController.navigate(R.id.navigateToSearchResult)
    }
}
