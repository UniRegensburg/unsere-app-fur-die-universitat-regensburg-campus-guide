package de.ur.campusguide.viewmodel

import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import de.ur.campusguide.navigation.AppRouter

/**
 * Main Viewmodel used to handle initializing operations for main app
 *
 * @property appRouter Navigation router as [AppRouter] used for navigation operations
 */

class MainViewModel(private val appRouter: AppRouter) : ViewModel() {

    fun setNavigationGraph(navController: NavController) {
        appRouter.setGraph(navController)
    }

}