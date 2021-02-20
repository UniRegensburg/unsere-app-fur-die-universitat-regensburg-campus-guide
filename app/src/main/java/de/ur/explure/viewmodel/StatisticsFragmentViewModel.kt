package de.ur.explure.viewmodel

import android.graphics.BitmapFactory
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.ur.explure.repository.rating.RatingRepositoryImpl
import de.ur.explure.repository.user.UserRepositoryImpl
import de.ur.explure.utils.FirebaseResult
import kotlinx.coroutines.launch

class StatisticsFragmentViewModel(
    private val userRepo: UserRepositoryImpl,
    private val ratingRepo: RatingRepositoryImpl
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
                    try {
                        val inp = java.net.URL(userInfo.data.profilePictureUrl).openStream()
                        val image = BitmapFactory.decodeStream(inp)
                        imageView.setImageBitmap(image)
                    } catch (exception: AccessDeniedException) {
                        Log.e("TAG", "" + exception)
                    }
                }
            }
        }
    }

    fun setDistanceStatistics(distance: TextView, startedRoutes: TextView, endedRoutes: TextView) {
        viewModelScope.launch {
            val userInfo = userRepo.getUserInfo()
            when (userInfo) {
                is FirebaseResult.Success -> {
                    endedRoutes.text = userInfo.data.finishedRoutes.size.toString()
                }
            }
        }
    }

    fun setContentStatistics(createdRoutes: TextView, createdLandmarks: TextView) {
        viewModelScope.launch {
            val userInfo = userRepo.getUserInfo()
            when (userInfo) {
                is FirebaseResult.Success -> {
                    createdRoutes.text = userInfo.data.createdRoutes.size.toString()
                }
            }
        }
    }

    fun setInteractionStatistics(comments: TextView, ratings: TextView) {
        viewModelScope.launch {
            val userInfo = userRepo.getUserInfo()
            when (userInfo) {
                is FirebaseResult.Success -> {
                    // set statistics
                }
            }
        }
    }
}
