package de.ur.explure.viewmodel

import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import de.ur.explure.navigation.MainAppRouter
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class BottomNavViewModel : ViewModel(), KoinComponent {

    private val mainAppRouter: MainAppRouter by inject()

    fun initNavController(navController: NavController) {
        mainAppRouter.initializeNavController(navController)
    }

    fun getCurrentNavController(): NavController? =
        mainAppRouter.getNullableNavController()
}
