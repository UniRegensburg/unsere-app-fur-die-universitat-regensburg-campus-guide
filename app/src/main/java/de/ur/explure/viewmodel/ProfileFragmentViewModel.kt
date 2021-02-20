package de.ur.explure.viewmodel

import android.graphics.BitmapFactory
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.ur.explure.repository.user.UserRepositoryImpl
import de.ur.explure.utils.FirebaseResult
import kotlinx.coroutines.launch

class ProfileFragmentViewModel(
    private val userRepo: UserRepositoryImpl
) : ViewModel() {

    fun setUserName(textView: TextView) {
        viewModelScope.launch() {
            val userInfo = userRepo.getUserInfo()
            when (userInfo) {
                is FirebaseResult.Success -> {
                    textView.text = userInfo.data.name
                }
            }
        }
    }

    fun setProfilePicture(imageView: ImageView) {
        viewModelScope.launch() {
            val userInfo = userRepo.getUserInfo()
            when (userInfo) {
                is FirebaseResult.Success -> {
                    val inp = java.net.URL(userInfo.data.profilePictureUrl).openStream()
                    val image = BitmapFactory.decodeStream(inp)
                    imageView.setImageBitmap(image)
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
