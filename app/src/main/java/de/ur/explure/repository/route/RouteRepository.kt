package de.ur.explure.repository.route

import android.net.Uri
import de.ur.explure.model.comment.CommentDTO
import de.ur.explure.model.route.Route
import de.ur.explure.model.route.RouteDTO
import de.ur.explure.utils.FirebaseResult
import java.util.*

interface RouteRepository {

    suspend fun createRouteInFireStore(
        routeDTO: RouteDTO,
        routeTitle: String,
        routeDescr: String
    ): FirebaseResult<String>

    suspend fun getRoute(routeId: String, getAsPreview: Boolean = false): FirebaseResult<Route>

    suspend fun getRoutes(
        routeIds: List<String>,
        getAsPreview: Boolean = false
    ): FirebaseResult<List<Route>>

    suspend fun deleteRoute(routeId: String): FirebaseResult<Void>

    suspend fun addComment(routeId: String, commentDTO: CommentDTO): FirebaseResult<Void>

    suspend fun deleteComment(commentId: String, routeId: String): FirebaseResult<Void>

    suspend fun addAnswer(
        routeId: String,
        commentId: String,
        commentDTO: CommentDTO
    ): FirebaseResult<Void>

    suspend fun deleteAnswer(
        answerId: String,
        commentId: String,
        routeId: String
    ): FirebaseResult<Void>

    suspend fun getLatestRoutes(
        lastVisibleDate: Date?,
        batchSize: Long
    ): FirebaseResult<List<Route>>

    suspend fun getMostPopularRoutes(
        lastRating: Double?,
        batchSize: Long
    ): FirebaseResult<List<Route>>

    suspend fun uploadRouteThumbnail(routeId: String, uri: Uri): String?
}
