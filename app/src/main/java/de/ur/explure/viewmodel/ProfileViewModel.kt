package de.ur.explure.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.ur.explure.model.user.User
import de.ur.explure.navigation.MainAppRouter
import de.ur.explure.repository.user.UserRepositoryImpl
import de.ur.explure.utils.FirebaseResult
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val userRepo: UserRepositoryImpl,
    private val appRouter: MainAppRouter
) : ViewModel() {

    var user: MutableLiveData<User> = MutableLiveData()

    fun getUserInfo() {
        viewModelScope.launch {
            when (val userInfo = userRepo.getUserInfo()) {
                is FirebaseResult.Success -> {
                    user.postValue(userInfo.data)
                }
            }
        }
    }

    fun updateUserName(newUserName: String) {
        viewModelScope.launch {
            userRepo.updateUserName(newUserName)
            getUserInfo()
        }
    }

    fun showCreatedRoutes() {
        appRouter.navigateToCreatedRoutes()
    }

    fun showFavoriteRoutes() {
        appRouter.navigateToFavoriteRoutes()
    }

    fun showStatisticsFragment() {
        appRouter.navigateToStatisticsFragment()
    }
}
