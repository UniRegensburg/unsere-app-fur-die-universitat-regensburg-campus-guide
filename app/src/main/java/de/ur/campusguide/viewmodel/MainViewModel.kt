package de.ur.campusguide.viewmodel

import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import de.ur.campusguide.navigation.AppRouter

class MainViewModel(private val appRouter: AppRouter) : ViewModel() {

    fun setNavigationGraph(navController: NavController) {
        appRouter.setGraph(navController)
    }

}