package de.ur.explure.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.ur.explure.navigation.MainAppRouter
import de.ur.explure.repository.user.UserRepositoryImpl
import de.ur.explure.utils.FirebaseResult
import kotlinx.coroutines.launch

@SuppressWarnings("UseIfInsteadOfWhen")
class UserDataViewModel(
    private val userRepository: UserRepositoryImpl,
    private val appRouter: MainAppRouter
) : ViewModel() {

    val showErrorMessage: MutableLiveData<Boolean> = MutableLiveData()

    fun updateProfilePicture(bitmap: Bitmap, qualityValue: Int) {
        viewModelScope.launch {
            userRepository.uploadImageAndSaveUri(bitmap, qualityValue)
        }
    }

    fun createProfileAndNavigateToMain(userName: String) {
        viewModelScope.launch {
            when (userRepository.createUserInFirestore(userName)) {
                is FirebaseResult.Success -> appRouter.navigateToMainApp()
                else -> showErrorMessage.postValue(true)
            }
        }
    }
}
