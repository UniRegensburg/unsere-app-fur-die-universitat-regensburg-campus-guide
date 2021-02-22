package de.ur.explure.viewmodel

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import de.ur.explure.navigation.StateAppRouter
import de.ur.explure.services.FirebaseAuthService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

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
}
