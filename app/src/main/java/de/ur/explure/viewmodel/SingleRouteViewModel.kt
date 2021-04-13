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

@Suppress("StringLiteralDuplication")
class SingleRouteViewModel(private val routeRepository: RouteRepositoryImpl) : ViewModel() {

    private val mutableRoute: MutableLiveData<Route> = MutableLiveData()
    val route: LiveData<Route> = mutableRoute
    private val mutableErrorMessage: MutableLiveData<Boolean> = MutableLiveData()
    val errorMessage: LiveData<Boolean> = mutableErrorMessage
    private val mutableSuccessMessage: MutableLiveData<Boolean> = MutableLiveData()
    val successMessage: LiveData<Boolean> = mutableSuccessMessage

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

    fun addComment(comment: String) {
        viewModelScope.launch {
            val commentDto = CommentDTO(comment)
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
            val commentDto = CommentDTO(answerText)
            val routeId = route.value?.id ?: return@launch
            if (routeRepository.addAnswer(routeId, commentId, commentDto) is FirebaseResult.Success) {
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
            if (routeRepository.deleteAnswer(answerId, commentId, routeId) is FirebaseResult.Success) {
                getRouteData(routeId)
                mutableSuccessMessage.postValue(true)
            } else {
                mutableSuccessMessage.postValue(false)
            }
        }
    }
}
