package de.ur.explure.viewmodel

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.ur.explure.navigation.MainAppRouter
import de.ur.explure.repository.user.UserRepositoryImpl
import de.ur.explure.services.FirebaseAuthService
import kotlinx.coroutines.launch

/**
 * Main ViewModel used to handle initializing operations for main app and observe auth state.
 *
 */

class MainViewModel(
    private val mainAppRouter: MainAppRouter,
    private val authService: FirebaseAuthService,
    private val userRepo : UserRepositoryImpl
) : ViewModel() {

    /**
     * Observe auth state of Firebase. Navigates to LoginFragment when no user is logged in.
     * Navigates to main application when user is logged in.
     *
     * @param activity  LifecycleOwner of the current activity
     */

    fun observeAuthState(activity: LifecycleOwner) {
        authService.currentUser.observe(activity, { user ->
            if (user != null) {
                viewModelScope.launch {
                    if (userRepo.isProfileCreated(user.uid)){
                        mainAppRouter.navigateToMainApp()
                    } else {
                        mainAppRouter.navigateToOnboarding()
                    }
                }
            } else {
                mainAppRouter.navigateToLogin()
            }
        })
    }
}
