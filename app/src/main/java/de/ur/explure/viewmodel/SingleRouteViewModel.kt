package de.ur.explure.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.ur.explure.model.comment.CommentDTO
import de.ur.explure.model.route.Route
import de.ur.explure.repository.route.RouteRepositoryImpl
import de.ur.explure.utils.FirebaseResult
import kotlinx.coroutines.launch
import java.util.*

@Suppress("UseIfInsteadOfWhen")
class SingleRouteViewModel(private val routeRepository: RouteRepositoryImpl) : ViewModel() {

    val showErrorMessage: MutableLiveData<Boolean> = MutableLiveData()
    private val mutableRoute: MutableLiveData<Route> = MutableLiveData()
    val route: LiveData<Route> = mutableRoute

    fun getRouteData(routeId: String) {
        viewModelScope.launch {
            when (val routeData = routeRepository.getRoute(routeId, false)) {
                is FirebaseResult.Success -> mutableRoute.postValue(routeData.data)
                    else -> showErrorMessage.postValue(true)
            }
        }
    }

    fun addComment(comment: String) {
        viewModelScope.launch {
            val commentDto = CommentDTO(comment)
            val routeId = route.value?.id ?: return@launch
            when (routeRepository.addComment(routeId, commentDto)) {
                is FirebaseResult.Success -> getRouteData(routeId)
                    else -> showErrorMessage.postValue(true)
            }
        }
    }

    fun addAnswer(commentId: String, answerText: String) {
        viewModelScope.launch {
            val commentDto = CommentDTO(answerText)
            val routeId = route.value?.id ?: return@launch
            when (routeRepository.addAnswer(routeId, commentId, commentDto)) {
                is FirebaseResult.Success -> getRouteData(routeId)
                    else -> showErrorMessage.postValue(true)
            }
        }
    }
}
