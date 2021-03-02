package de.ur.explure.viewmodel

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import de.ur.explure.navigation.StateAppRouter
import de.ur.explure.services.FirebaseAuthService
import org.koin.core.KoinComponent
import org.koin.core.inject

/**
 * State Viewmodel used to handle initializing operations for main app and observe auth state
 *
 */

class StateViewModel : ViewModel(), KoinComponent {

    private val stateAppRouter: StateAppRouter by inject()
    private val authRepo: FirebaseAuthService by inject()

    /**
     * Observe auth state of Firebase. Navigates to LoginFragment when no user is logged in.
     * Navigates to main application when user is logged in.
     *
     * @param activity  LifecycleOwner of the current activity
     */

    fun observeAuthState(activity: LifecycleOwner) {
        authRepo.currentUser.observe(activity, Observer { user ->
            if (user != null) {
                stateAppRouter.navigateToMainApp()
            } else {
                stateAppRouter.navigateToLogin()
            }
        })
    }

    /**
     * Sets the current navigation graph in the appRouter.
     */

    fun initializeStateNavController(navController: NavController) {
        stateAppRouter.initializeNavController(navController)
    }

    /**
     * Navigate ups in the current navigation controller.
     *
     * @return Returns [Boolean] value = true if backstack is not empty | false if backstack is empty.
     */
    fun navigateUp(): Boolean {
        return stateAppRouter.navigateUp()
    }
}
