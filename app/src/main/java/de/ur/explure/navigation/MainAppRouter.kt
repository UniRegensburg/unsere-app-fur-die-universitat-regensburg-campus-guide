package de.ur.explure.navigation

import androidx.navigation.NavController
import com.mapbox.core.constants.Constants.PRECISION_6
import com.mapbox.geojson.LineString
import de.ur.explure.R
import de.ur.explure.model.MapMarker
import de.ur.explure.model.category.Category
import de.ur.explure.views.CategoryQueryFragmentDirections
import de.ur.explure.views.CreatedRoutesFragmentDirections
import de.ur.explure.views.DiscoverFragmentDirections
import de.ur.explure.views.FavoriteRoutesFragmentDirections
import de.ur.explure.views.MapFragmentDirections
import de.ur.explure.views.ProfileFragmentDirections
import de.ur.explure.views.SaveRouteFragmentDirections
import de.ur.explure.views.TextQueryFragmentDirections

/**
 * Main router class used for navigation operations with the navigation component.
 */

@Suppress("TooManyFunctions")
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

    fun navigateToTextSearchResult(textQueryKey: String) {
        val action = DiscoverFragmentDirections.actionDiscoverFragmentToTextQueryFragment(textQueryKey)
        navController.navigate(action)
    }

    fun navigateToCategoryQuery(categoryQueryKey: Category) {
        val action = DiscoverFragmentDirections.actionDiscoverFragmentToCategoryQueryFragment(categoryQueryKey)
        navController.navigate(action)
    }

    fun navigateToRouteDetails(routeId: String) {
        val action = DiscoverFragmentDirections.actionDiscoverFragmentToRouteDetails(routeId)
        navController.navigate(action)
    }

    fun navigateToRouteDetailsAfterCreation(routeId: String) {
        val action = SaveRouteFragmentDirections.actionSaveRouteFragmentToSingleRouteFragment(routeId)
        navController.navigate(action)
    }

    fun navigateToRouteDetailsFromCategory(routeId: String) {
        val action = CategoryQueryFragmentDirections.actionCategoryQueryFragmentToSingleRouteFragment(routeId)
        navController.navigate(action)
    }

    fun navigateToRouteDetailsFromQuery(routeId: String) {
        val action = TextQueryFragmentDirections.actionTextQueryFragmentToSingleRouteFragment(routeId)
        navController.navigate(action)
    }

    fun navigateToCreatedRouteDetails(routeId: String) {
        val action = CreatedRoutesFragmentDirections.actionCreatedRoutesFragmentToRouteDetails(routeId)
        navController.navigate(action)
    }

    fun navigateToFavoriteRouteDetails(routeId: String) {
        val action = FavoriteRoutesFragmentDirections.actionFavoriteRoutesFragmentToRouteDetails(routeId)
        navController.navigate(action)
    }

    fun navigateToRouteEditFragment(route: LineString, markers: List<MapMarker>?) {
        val encodedRoute = route.toPolyline(PRECISION_6)
        val markerArray = markers?.toTypedArray()
        val action = MapFragmentDirections.actionMapFragmentToEditRouteFragment(
            routePolyline = encodedRoute,
            routeMarkers = markerArray
        )
        navController.navigate(action)
    }
}
