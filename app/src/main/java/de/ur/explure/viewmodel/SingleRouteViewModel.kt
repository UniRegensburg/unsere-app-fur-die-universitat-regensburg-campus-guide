package de.ur.explure.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.ur.explure.R
import de.ur.explure.model.comment.CommentDTO
import de.ur.explure.model.route.Route
import de.ur.explure.navigation.MainAppRouter
import de.ur.explure.repository.route.RouteRepositoryImpl
import de.ur.explure.repository.user.UserRepositoryImpl
import de.ur.explure.utils.DeepLinkUtils
import de.ur.explure.utils.FirebaseResult
import de.ur.explure.views.SingleRouteFragmentDirections
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*

class SingleRouteViewModel(
    private val routeRepository: RouteRepositoryImpl,
    private val userRepository: UserRepositoryImpl,
    private val appRouter: MainAppRouter
) : ViewModel() {

    private val mutableRoute: MutableLiveData<Route> = MutableLiveData()
    val route: LiveData<Route> = mutableRoute
    private val mutableErrorMessage: MutableLiveData<Boolean> = MutableLiveData()
    val errorMessage: LiveData<Boolean> = mutableErrorMessage
    private val mutableSuccessMessage: MutableLiveData<Boolean> = MutableLiveData()
    val successMessage: LiveData<Boolean> = mutableSuccessMessage
    private var userName: String = DEFAULT_USERNAME

    fun getRouteData(routeId: String) {
        viewModelScope.launch {
            when (val routeData = routeRepository.getRoute(routeId, false)) {
                is FirebaseResult.Success -> {
                    mutableRoute.postValue(routeData.data)
                }
                is FirebaseResult.Canceled -> {
                    mutableErrorMessage.postValue(true)
                }
                is FirebaseResult.Error -> {
                    mutableErrorMessage.postValue(true)
                }
            }
        }
    }

    fun getUserName() {
        viewModelScope.launch {
            val userData = userRepository.getUserInfo()
            userName = if (userData is FirebaseResult.Success) {
                userData.data.name
            } else {
                DEFAULT_USERNAME
            }
        }
    }

    fun addComment(comment: String) {
        viewModelScope.launch {
            val commentDto = CommentDTO(comment, userName)
            val routeId = route.value?.id ?: return@launch
            if (routeRepository.addComment(routeId, commentDto) is FirebaseResult.Success) {
                getRouteData(routeId)
            } else {
                mutableErrorMessage.postValue(true)
            }
        }
    }

    fun addAnswer(commentId: String, answerText: String) {
        viewModelScope.launch {
            val commentDto = CommentDTO(answerText, userName)
            val routeId = route.value?.id ?: return@launch
            if (routeRepository.addAnswer(
                    routeId,
                    commentId,
                    commentDto
                ) is FirebaseResult.Success
            ) {
                getRouteData(routeId)
            } else {
                mutableErrorMessage.postValue(true)
            }
        }
    }

    fun deleteComment(commentId: String) {
        viewModelScope.launch {
            val routeId = route.value?.id ?: return@launch
            if (routeRepository.deleteComment(commentId, routeId) is FirebaseResult.Success) {
                getRouteData(routeId)
                mutableSuccessMessage.postValue(true)
            } else {
                mutableSuccessMessage.postValue(false)
            }
        }
    }

    fun deleteAnswer(answerId: String, commentId: String) {
        viewModelScope.launch {
            val routeId = route.value?.id ?: return@launch
            if (routeRepository.deleteAnswer(
                    answerId,
                    commentId,
                    routeId
                ) is FirebaseResult.Success
            ) {
                getRouteData(routeId)
                mutableSuccessMessage.postValue(true)
            } else {
                mutableSuccessMessage.postValue(false)
            }
        }
    }

    fun popToDiscover() {
        appRouter.popUpToDiscover()
    }

    fun shareRoute(context: Context) {
        val route = route.value ?: return
        val shareLink = DeepLinkUtils.getURLforRouteId(route.id)
        val intent = Intent()
        intent.action = Intent.ACTION_SEND
        intent.putExtra(
            Intent.EXTRA_TEXT, context.getString(
                R.string.share_text,
                route.title,
                route.wayPointCount,
                route.duration.toInt(),
                route.distance.toInt(),
                shareLink
            )
        )
        intent.type = "text/plain"
        context.startActivity(
            Intent.createChooser(
                intent,
                context.getString(R.string.share_option)
            )
        )
    }

    fun startNavigation() {
        val route = mutableRoute.value
        if (route == null) {
            Timber.e("Navigation start failed because route object was null!")
            return
        }
        val action = SingleRouteFragmentDirections.actionSingleRouteFragmentToNavigationFragment(
            route = route,
            routeTitle = route.title
        )
        appRouter.getNavController()?.navigate(action)
    }

    companion object {
        private const val DEFAULT_USERNAME = "Anonymous"
    }
}
