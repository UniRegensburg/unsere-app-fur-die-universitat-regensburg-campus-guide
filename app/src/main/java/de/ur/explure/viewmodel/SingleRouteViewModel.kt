package de.ur.explure.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.ur.explure.model.comment.CommentDTO
import de.ur.explure.model.route.Route
import de.ur.explure.navigation.MainAppRouter
import de.ur.explure.repository.route.RouteRepositoryImpl
import de.ur.explure.utils.FirebaseResult
import de.ur.explure.views.SingleRouteFragmentDirections
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*

@Suppress("StringLiteralDuplication")
class SingleRouteViewModel(
    private val routeRepository: RouteRepositoryImpl,
    private var appRouter: MainAppRouter
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
}
