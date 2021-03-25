package de.ur.explure.navigation

import android.os.Bundle
import androidx.navigation.NavController
import de.ur.explure.R
import de.ur.explure.config.BundleConfig.CATEGORY_QUERY_KEY
import de.ur.explure.config.BundleConfig.TEXT_QUERY_KEY
import de.ur.explure.views.ProfileFragmentDirections
import de.ur.explure.views.DiscoverFragmentDirections

/**
 * Main router class used for navigation operations with the navigation component.
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

    /**
     * Resets the navController's nav graph to the initial host nav graph. This should be called
     * before navigating from one included child graph to another to prevent crashes.
     */

    private fun resetNavGraph() {
        navController.setGraph(R.navigation.nav_graph_host)
    }

    /**
     * Changes the current Navigation graph to the auth graph and navigates to the start fragment in
     * the authentication process while removing the fragments in the current nav graph from the
     * backstack.
     */

    fun navigateToLogin() {
        resetNavGraph()
        navController.navigate(R.id.action_mainFragment_to_auth_graph)
        navController.setGraph(R.navigation.nav_graph_auth)
    }

    /**
     * Changes the current Navigation graph to the main graph and navigates to the start fragment in
     * the main app while removing the fragments in the current nav graph from the backstack.
     */

    fun navigateToMainApp() {
        resetNavGraph()
        navController.navigate(R.id.action_mainFragment_to_main_graph)
        navController.setGraph(R.navigation.nav_graph_main)
    }

    fun navigateToRegister() {
        navController.navigate(R.id.navigateToRegister)
    }

    fun navigateToCreatedRoutes() {
        val ownRoutesAction = ProfileFragmentDirections.actionDiscoverFragmentToCreatedRoutes()
        navController.navigate(ownRoutesAction)
    }

    fun navigateToFavoriteRoutes() {
        val favoriteRoutesAction = ProfileFragmentDirections.actionDiscoverFragmentToFavoritesRoutes()
        navController.navigate(favoriteRoutesAction)
    }

    fun navigateToStatisticsFragment() {
        val statisticsAction = ProfileFragmentDirections.actionDiscoverFragmentToStatisticsFragment()
        navController.navigate(statisticsAction)
    }

    /**
     * Returns the current navigation controller or null if not found.
     */
    fun getNavController(): NavController? {
        return if (this::navController.isInitialized) {
            navController
        } else {
            null
        }
    }

    fun navigateToTextSearchResult(query: String) {
        val bundle = Bundle()
        bundle.putString(TEXT_QUERY_KEY, query)
        navController.navigate(R.id.textQueryFragment, bundle)
    }

    fun navigateToCategoryQuery(categoryId: String) {
        val bundle = Bundle()
        bundle.putString(CATEGORY_QUERY_KEY, categoryId)
        navController.navigate(R.id.categoryQueryFragment, bundle)
    }

    fun navigateToRouteDetails(routeId: String) {
        val action = DiscoverFragmentDirections.actionDiscoverFragmentToRouteDetails(routeId)
        navController.navigate(action)
    }
}
