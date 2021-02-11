package de.ur.explure.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import de.ur.explure.navigation.MainAppRouter
import org.koin.core.KoinComponent
import org.koin.core.inject

class BottomNavViewModel : ViewModel(), KoinComponent {

    private val mainAppRouter: MainAppRouter by inject()

    var currentNavController: LiveData<NavController>? = null

    /**
     * Sets the current navigation graph in the appRouter.
     */

    fun initializeNavController(navController: NavController) {
        mainAppRouter.initializeNavController(navController)
    }

    fun resetCurrentNavController() {
        currentNavController = null
    }

    /**
     * Navigate ups in the current navigation controller.
     *
     * @return Returns [Boolean] value = true if backstack is not empty | false if backstack is empty.
     */
    fun navigateUp(): Boolean {
        return mainAppRouter.navigateUp()
    }
}
