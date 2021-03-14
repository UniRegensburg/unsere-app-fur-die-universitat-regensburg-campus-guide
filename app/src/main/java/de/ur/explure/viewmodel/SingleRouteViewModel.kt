package de.ur.explure.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.ur.explure.model.comment.Comment
import de.ur.explure.model.comment.CommentDTO
import de.ur.explure.model.route.Route
import de.ur.explure.model.waypoint.WayPoint
import de.ur.explure.repository.route.RouteRepositoryImpl
import de.ur.explure.utils.FirebaseResult
import kotlinx.coroutines.launch
import java.util.*

@Suppress("StringLiteralDuplication")
class SingleRouteViewModel(private val routeRepository: RouteRepositoryImpl) : ViewModel() {

    private val mutableRoute: MutableLiveData<Route> = MutableLiveData()
    val route: LiveData<Route> = mutableRoute
    private val mutableWayPointList: MutableLiveData<LinkedList<WayPoint>> = MutableLiveData(LinkedList())
    val wayPointList: LiveData<LinkedList<WayPoint>> = mutableWayPointList
    val mutableCommentList: MutableLiveData<LinkedList<Comment>> = MutableLiveData(LinkedList())
    val commentList: LiveData<LinkedList<Comment>> = mutableCommentList
    val mutableAddComment: MutableLiveData<Void> = MutableLiveData()
    val addComment: LiveData<Void> = mutableAddComment

    fun getRouteData(routeId: String) {
        viewModelScope.launch {
            when (val routeData = routeRepository.getRoute(routeId, false)) {
                is FirebaseResult.Success -> {
                    mutableRoute.postValue(routeData.data)
                    mutableWayPointList.postValue(routeData.data.wayPoints)
                    mutableCommentList.postValue(routeData.data.comments)
                }
            }
        }
    }

    fun addComment(comment: String) {
        viewModelScope.launch {
            val commentDto = CommentDTO(comment)
            val routeId = route.value?.id ?: return@launch
            when (val addComment = routeRepository.addComment(routeId, commentDto)) {
                is FirebaseResult.Success -> {
                    getRouteData(routeId)
                }
            }
        }
    }
}
