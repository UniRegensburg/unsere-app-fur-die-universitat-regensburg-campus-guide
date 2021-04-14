package de.ur.explure.viewmodel

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.ur.explure.R
import de.ur.explure.model.comment.CommentDTO
import de.ur.explure.model.route.Route
import de.ur.explure.repository.route.RouteRepositoryImpl
import de.ur.explure.repository.user.UserRepositoryImpl
import de.ur.explure.utils.DeepLinkUtils
import de.ur.explure.utils.FirebaseResult
import kotlinx.coroutines.launch
import java.util.*

@Suppress("StringLiteralDuplication")
class SingleRouteViewModel(
    private val routeRepository: RouteRepositoryImpl,
    private val userRepository: UserRepositoryImpl
) : ViewModel() {

    private val mutableRoute: MutableLiveData<Route> = MutableLiveData()
    val route: LiveData<Route> = mutableRoute

    fun getRouteData(routeId: String) {
        viewModelScope.launch {
            when (val routeData = routeRepository.getRoute(routeId, false)) {
                is FirebaseResult.Success -> {
                    mutableRoute.postValue(routeData.data)
                }
            }
        }
    }

    fun addComment(comment: String) {
        viewModelScope.launch {
            val commentDto = CommentDTO(comment)
            val routeId = route.value?.id ?: return@launch
            when (routeRepository.addComment(routeId, commentDto)) {
                is FirebaseResult.Success -> {
                    getRouteData(routeId)
                }
            }
        }
    }

    fun addAnswer(commentId: String, answerText: String) {
        viewModelScope.launch {
            val commentDto = CommentDTO(answerText)
            val routeId = route.value?.id ?: return@launch
            when (routeRepository.addAnswer(routeId, commentId, commentDto)) {
                is FirebaseResult.Success -> {
                    getRouteData(routeId)
                }
            }
        }
    }

    fun deleteComment(commentId: String) {
        viewModelScope.launch {
            val routeId = route.value?.id ?: return@launch
            when (routeRepository.deleteComment(commentId, routeId)) {
                is FirebaseResult.Success -> {
                    getRouteData(routeId)
                }
            }
        }
    }

    fun deleteAnswer(answerId: String, commentId: String) {
        viewModelScope.launch {
            val routeId = route.value?.id ?: return@launch
            when (routeRepository.deleteAnswer(answerId, commentId, routeId)) {
                is FirebaseResult.Success -> {
                    getRouteData(routeId)
                }
            }
        }
    }

    fun shareRoute(context: Context) {
            val route = route.value ?: return
            val shareLink = DeepLinkUtils.getURLforRouteId(route.id)
            val intent = Intent()
            intent.action = Intent.ACTION_SEND
            intent.putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_text,
                route.title,
                route.wayPointCount,
                route.duration.toInt(),
                route.distance.toInt(),
                shareLink))
            intent.type = "text/plain"
            context.startActivity(
                Intent.createChooser(
                    intent,
                    context.getString(R.string.share_option)
                )
            )
    }

    fun favorRoute(routeId: String) {
        viewModelScope.launch {
            when (val userInfo = userRepository.getUserInfo()) {
                is FirebaseResult.Success -> {
                    if (!userInfo.data.favouriteRoutes.contains(routeId)) {
                        userRepository.addRouteToFavouriteRoutes(routeId)
                    }
                }
            }
        }
    }
}
