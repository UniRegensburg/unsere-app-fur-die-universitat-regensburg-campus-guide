package de.ur.explure.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.ur.explure.model.user.User
import de.ur.explure.navigation.MainAppRouter
import de.ur.explure.repository.user.UserRepositoryImpl
import de.ur.explure.utils.FirebaseResult
import kotlinx.coroutines.launch

class UserDataViewModel(
        private val userRepository: UserRepositoryImpl,
        private val appRouter: MainAppRouter
) : ViewModel() {

    var user: MutableLiveData<User> = MutableLiveData()

    fun getUserInfo() {
        viewModelScope.launch {
            when (val userInfo = userRepository.getUserInfo()) {
                is FirebaseResult.Success -> {
                    user.postValue(userInfo.data)
                }
            }
        }
    }

    fun updateProfilePicture(bitmap: Bitmap, qualityValue: Int) {
        viewModelScope.launch {
            userRepository.uploadImageAndSaveUri(bitmap, qualityValue)
        }
    }

    fun updateUserName(newUserName: String) {
        viewModelScope.launch {
            userRepository.updateUserName(newUserName)
            getUserInfo()
        }
    }

    fun createProfileAndNavigateToMain(userName: String) {
        // Create User in Firestore with UserRepository
        viewModelScope.launch {
            userRepository.createUserInFirestore(userName)
        }
        appRouter.navigateToMainApp()
    }
}
