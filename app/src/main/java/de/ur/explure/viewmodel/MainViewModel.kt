package de.ur.explure.viewmodel

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import de.ur.explure.navigation.MainAppRouter
import de.ur.explure.services.FirebaseAuthService

/**
 * Main ViewModel used to handle initializing operations for main app and observe auth state.
 *
 */

class MainViewModel(
    private val mainAppRouter: MainAppRouter,
    private val authRepo: FirebaseAuthService
) :
    ViewModel() {

    /**
     * Observe auth state of Firebase. Navigates to LoginFragment when no user is logged in.
     * Navigates to main application when user is logged in.
     *
     * @param activity  LifecycleOwner of the current activity
     */

    fun observeAuthState(activity: LifecycleOwner) {
        authRepo.currentUser.observe(activity, { user ->
            if (user != null) {
                mainAppRouter.navigateToMainApp()
            } else {
                mainAppRouter.navigateToLogin()
            }
        })
    }
}
