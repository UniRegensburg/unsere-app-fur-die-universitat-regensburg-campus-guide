package de.ur.explure.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.ur.explure.model.user.User
import de.ur.explure.navigation.MainAppRouter
import de.ur.explure.repository.user.UserRepositoryImpl
import de.ur.explure.utils.FirebaseResult
import kotlinx.coroutines.launch

class ProfileFragmentViewModel(
    private val userRepo: UserRepositoryImpl,
    private val appRouter: MainAppRouter
) : ViewModel() {

    var user: MutableLiveData<User> = MutableLiveData()

    fun getUserInfo() {
        viewModelScope.launch() {
            val userInfo = userRepo.getUserInfo()
            when (userInfo) {
                is FirebaseResult.Success -> {
                    user.postValue(userInfo.data)
                }
            }
        }
    }

    fun updateUserName(newUserName: String) {
        viewModelScope.launch {
            updateUserName(newUserName)
        }
    }
}
