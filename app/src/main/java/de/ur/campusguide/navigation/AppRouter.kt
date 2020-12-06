package de.ur.campusguide.navigation

import androidx.navigation.NavController
import de.ur.campusguide.R

class AppRouter {

    private lateinit var navController: NavController

    fun setGraph(navController: NavController) {
        this.navController = navController
        navController.setGraph(R.navigation.nav_graph)
    }
}