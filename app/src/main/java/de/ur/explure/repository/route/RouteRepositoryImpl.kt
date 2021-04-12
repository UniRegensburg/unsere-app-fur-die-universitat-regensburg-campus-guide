package de.ur.explure.repository.route

import android.graphics.Bitmap
import android.net.Uri
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.WriteBatch
import com.google.firebase.storage.FirebaseStorage
import de.ur.explure.config.ErrorConfig
import de.ur.explure.config.FirebaseCollections.ANSWER_COLLECTION_NAME
import de.ur.explure.config.FirebaseCollections.COMMENT_COLLECTION_NAME
import de.ur.explure.config.FirebaseCollections.WAYPOINT_COLLECTION_NAME
import de.ur.explure.config.FirestoreStorageDirectories
import de.ur.explure.config.RouteDocumentConfig
import de.ur.explure.extensions.await
import de.ur.explure.model.comment.Comment
import de.ur.explure.model.comment.CommentDTO
import de.ur.explure.model.rating.RatingValues
import de.ur.explure.model.route.Route
import de.ur.explure.model.route.RouteDTO
import de.ur.explure.model.waypoint.WayPoint
import de.ur.explure.model.waypoint.WayPointDTO
import de.ur.explure.services.FireStoreInstance
import de.ur.explure.services.FirebaseAuthService
import de.ur.explure.utils.CachedFileUtils
import de.ur.explure.utils.FirebaseResult
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.util.*

@Suppress("TooGenericExceptionCaught", "UnnecessaryParentheses", "ReturnCount")
class RouteRepositoryImpl(
    private val authService: FirebaseAuthService,
    private val fireStore: FireStoreInstance,
    private val fireStorage: FirebaseStorage
) : RouteRepository {

    /**
     * Creates a Route Document in FireStore
     *
     * @param routeDTO [RouteDTO] object holding the routes information
     * @return On Success: Returns [FirebaseResult.Success] with id of newly created route page\
     * On Failure: Returns [FirebaseResult.Error] with exception\
     * On Cancellation: Returns [FirebaseResult.Canceled] with exception
     */

    override suspend fun createRouteInFireStore(routeDTO: RouteDTO): FirebaseResult<String> {
        return try {
            val userId = authService.getCurrentUserId() ?: return ErrorConfig.NO_USER_RESULT
            val routeDocument = fireStore.routeCollection.document()
            val routeCreationBatch = createRouteWriteBatch(userId, routeDTO, routeDocument)
            when (val routeBatch = routeCreationBatch.commit().await()) {
                is FirebaseResult.Success -> {
                    return FirebaseResult.Success(routeDocument.id)
                }
                is FirebaseResult.Error -> FirebaseResult.Error(routeBatch.exception)
                is FirebaseResult.Canceled -> FirebaseResult.Canceled(routeBatch.exception)
            }
        } catch (exception: Exception) {
            Timber.d(exception)
            FirebaseResult.Error(exception)
        }
    }

    /**
     * Gets a Route document from FireStore by ID and serializes it to a [Route] object.
     *
     * @param routeId [String] of the route's id
     * @param getAsPreview [Boolean] which determines if route should be retrieved
     * as Preview (No Comments/WayPoints/Rating)
     * @return On Success: Returns [FirebaseResult.Success] with [Route] object as data\
     * On Failure: Returns [FirebaseResult.Error] with exception\
     * On Cancellation: Returns [FirebaseResult.Canceled] with exception
     */

    override suspend fun getRoute(routeId: String, getAsPreview: Boolean): FirebaseResult<Route> {
        return try {
            when (val routeCall = fireStore.routeCollection.document(routeId).get().await()) {
                is FirebaseResult.Success -> {
                    if (getAsPreview) {
                        val routeList = routeCall.data.toObject(Route::class.java)
                            ?: return ErrorConfig.DESERIALIZATION_FAILED_RESULT
                        FirebaseResult.Success(routeList)
                    } else {
                        return snapshotToRouteObject(routeCall.data)
                    }
                }
                is FirebaseResult.Error -> FirebaseResult.Error(routeCall.exception)
                is FirebaseResult.Canceled -> FirebaseResult.Canceled(routeCall.exception)
            }
        } catch (exception: Exception) {
            FirebaseResult.Error(exception)
        }
    }

    /**
     * Gets route documents from FireStore by ID and serializes them to [Route] objects.
     *
     * @param routeIds [List] of [String] objects of the routes' ID
     * @param getAsPreview [Boolean] which determines if routes should be retrieved
     * as Preview (No Comments/WayPoints/Rating)
     * @return On Success: Returns [FirebaseResult.Success] with [List] of [Route] object as data\
     * On Failure: Returns [FirebaseResult.Error] with exception\
     * On Cancellation: Returns [FirebaseResult.Canceled] with exception
     */

    override suspend fun getRoutes(
        routeIds: List<String>,
        getAsPreview: Boolean
    ): FirebaseResult<List<Route>> {
        return try {
            when (val routeCall =
                fireStore.routeCollection.whereIn(FieldPath.documentId(), routeIds).get().await()) {
                is FirebaseResult.Success -> {
                    if (getAsPreview) {
                        val routeList = routeCall.data.toObjects(Route::class.java)
                        FirebaseResult.Success(routeList)
                    } else {
                        return snapshotToRouteList(routeCall.data)
                    }
                }
                is FirebaseResult.Error -> FirebaseResult.Error(routeCall.exception)
                is FirebaseResult.Canceled -> FirebaseResult.Canceled(routeCall.exception)
            }
        } catch (exception: Exception) {
            FirebaseResult.Error(exception)
        }
    }

    /**
     * Deletes a route document in FireStore
     *
     * @param routeId [String] of the route's ID
     * @return On Success: Returns [FirebaseResult.Success] with empty return\
     * On Failure: Returns [FirebaseResult.Error] with exception\
     * On Cancellation: Returns [FirebaseResult.Canceled] with exception
     */

    override suspend fun deleteRoute(routeId: String): FirebaseResult<Void> {
        return try {
            return fireStore.routeCollection.document(routeId).delete().await()
        } catch (exception: Exception) {
            FirebaseResult.Error(exception)
        }
    }

    /**
     * Adds a new comment to a route document.
     *
     * @param routeId [String] of the route's ID
     * @param commentDTO [CommentDTO] object with the comment's message
     * @return On Success: Returns [FirebaseResult.Success] with empty return\
     * On Failure: Returns [FirebaseResult.Error] with exception\
     * On Cancellation: Returns [FirebaseResult.Canceled] with exception
     */

    override suspend fun addComment(routeId: String, commentDTO: CommentDTO): FirebaseResult<Void> {
        return try {
            val userId = authService.getCurrentUserId() ?: return ErrorConfig.NO_USER_RESULT
            return fireStore.routeCollection.document(routeId)
                .collection(COMMENT_COLLECTION_NAME)
                .document()
                .set(commentDTO.toMap(userId)).await()
        } catch (exception: Exception) {
            FirebaseResult.Error(exception)
        }
    }

    /**
     * Adds a new answer to a comment.
     *
     * @param routeId [String] of the route's ID
     * @param commentId [String] of the comment's ID
     * @param commentDTO [CommentDTO] object with the answer's message
     * @return On Success: Returns [FirebaseResult.Success] with empty return\
     * On Failure: Returns [FirebaseResult.Error] with exception\
     * On Cancellation: Returns [FirebaseResult.Canceled] with exception
     */

    override suspend fun addAnswer(
        routeId: String,
        commentId: String,
        commentDTO: CommentDTO
    ): FirebaseResult<Void> {
        return try {
            val userId = authService.getCurrentUserId() ?: return ErrorConfig.NO_USER_RESULT
            return fireStore.routeCollection.document(routeId)
                .collection(COMMENT_COLLECTION_NAME)
                .document(commentId)
                .collection(ANSWER_COLLECTION_NAME)
                .document()
                .set(commentDTO.toMap(userId)).await()
        } catch (exception: Exception) {
            FirebaseResult.Error(exception)
        }
    }

    override suspend fun getLatestRoutes(
        lastVisibleDate: Date?,
        batchSize: Long
    ): FirebaseResult<List<Route>> {
        return try {
            when (val routeCall =
                fireStore.routeCollection
                    .orderBy(RouteDocumentConfig.DATE_FIELD)
                    .startAfter(lastVisibleDate)
                    .limit(batchSize)
                    .get()
                    .await()) {
                is FirebaseResult.Success -> {
                    val routeList = routeCall.data.toObjects(Route::class.java)
                    FirebaseResult.Success(routeList)
                }
                is FirebaseResult.Error -> FirebaseResult.Error(routeCall.exception)
                is FirebaseResult.Canceled -> FirebaseResult.Canceled(routeCall.exception)
            }
        } catch (exception: Exception) {
            FirebaseResult.Error(exception)
        }
    }

    override suspend fun getMostPopularRoutes(
        lastRating: Double?,
        batchSize: Long
    ): FirebaseResult<List<Route>> {
        return try {
            when (val routeCall =
                fireStore.routeCollection
                    .orderBy(RouteDocumentConfig.CURRENT_RATING_FIELD, Query.Direction.DESCENDING)
                    .startAfter(lastRating ?: RatingValues.FIVE_STARS)
                    .limit(batchSize)
                    .get()
                    .await()) {
                is FirebaseResult.Success -> {
                    val routeList = routeCall.data.toObjects(Route::class.java)
                    FirebaseResult.Success(routeList)
                }
                is FirebaseResult.Error -> FirebaseResult.Error(routeCall.exception)
                is FirebaseResult.Canceled -> FirebaseResult.Canceled(routeCall.exception)
            }
        } catch (exception: Exception) {
            FirebaseResult.Error(exception)
        }
    }

    private suspend fun snapshotToRouteObject(data: DocumentSnapshot): FirebaseResult<Route> {
        val wayPoints =
            getWayPoints(data.id) ?: return ErrorConfig.DESERIALIZATION_FAILED_RESULT
        val comments =
            getComments(data.id) ?: mutableListOf()
        val routeObject =
            data.toObject(Route::class.java) ?: return ErrorConfig.DESERIALIZATION_FAILED_RESULT
        routeObject.fillComments(comments)
        routeObject.fillWayPoints(wayPoints)
        return FirebaseResult.Success(routeObject)
    }

    private suspend fun getWayPoints(routeId: String): List<WayPoint>? {
        val wayPointCall =
            fireStore.routeCollection.document(routeId).collection(WAYPOINT_COLLECTION_NAME).get()
                .await()
        if (wayPointCall is FirebaseResult.Success) {
            return wayPointCall.data.toObjects(WayPoint::class.java)
        }
        return null
    }

    private suspend fun getComments(routeId: String): List<Comment>? {
        val commentCall =
            fireStore.routeCollection.document(routeId).collection(COMMENT_COLLECTION_NAME).get()
                .await()
        if (commentCall is FirebaseResult.Success) {
            val resultList = mutableListOf<Comment>()
            commentCall.data.forEach {
                val comment = getFullComment(it) ?: return@forEach
                resultList.add(comment)
            }
            return resultList
        }
        return null
    }

    private suspend fun getFullComment(snapshot: QueryDocumentSnapshot): Comment? {
        val answers = getAnswers(snapshot)
        val commentObject = snapshot.toObject(Comment::class.java)
        commentObject.fillAnswers(answers)
        return commentObject
    }

    private suspend fun getAnswers(snapshot: QueryDocumentSnapshot): List<Comment> {
        val resultList = mutableListOf<Comment>()
        val answerCall = snapshot.reference.collection(ANSWER_COLLECTION_NAME).get().await()
        if (answerCall is FirebaseResult.Success && !answerCall.data.isEmpty) {
            answerCall.data.forEach {
                val answer = it.toObject(Comment::class.java)
                resultList.add(answer)
            }
        }
        return resultList
    }

    private suspend fun snapshotToRouteList(data: QuerySnapshot): FirebaseResult<List<Route>> {
        val resultList = mutableListOf<Route>()
        data.documents.forEach {
            val routeObjectResult = snapshotToRouteObject(it)
            if (routeObjectResult is FirebaseResult.Success) {
                resultList.add(routeObjectResult.data)
            }
        }
        return FirebaseResult.Success(resultList)
    }

    private fun createRouteWriteBatch(
        userId: String,
        routeDTO: RouteDTO,
        routeDocument: DocumentReference
    ): WriteBatch {
        val batch = fireStore.getWriteBatch()
        batch.set(routeDocument, routeDTO.toMap(userId))
        val wayPointCollection = routeDocument.collection(WAYPOINT_COLLECTION_NAME)
        routeDTO.wayPoints.forEach { wayPoint ->
            val wayPointDocument = wayPointCollection.document()
            val waypointWithRefs =
                uploadMediaToStorage(wayPoint, wayPointDocument.id, routeDocument.id)
            batch.set(wayPointDocument, waypointWithRefs.toMap())
        }
        return batch
    }

    private fun uploadMediaToStorage(wayPoint: WayPointDTO, waypointId: String, routeId: String):
            WayPointDTO {
        wayPoint.audioUri?.run {
            val secretId = UUID.randomUUID().toString()
            val reference =
                fireStorage.reference.child(FirestoreStorageDirectories.WAYPOINT_DATA_DIRECTORY)
                    .child(
                        "$routeId-$waypointId-$secretId-${CachedFileUtils.AUDIO_FILE_SUFFIX}"
                    )
            wayPoint.audioURL = reference.toString()
            reference.putFile(this)
        }
        wayPoint.videoUri?.run {
            val secretId = UUID.randomUUID().toString()
            val reference =
                fireStorage.reference.child(FirestoreStorageDirectories.WAYPOINT_DATA_DIRECTORY)
                    .child(
                        "$routeId-$waypointId-$secretId-${CachedFileUtils.VIDEO_FILE_SUFFIX}"
                    )
            wayPoint.videoURL = reference.toString()
            reference.putFile(this)
        }
        wayPoint.imageUri?.run {
            val secretId = UUID.randomUUID().toString()
            val reference =
                fireStorage.reference.child(FirestoreStorageDirectories.WAYPOINT_DATA_DIRECTORY)
                    .child(
                        "$routeId-$waypointId-$secretId-${CachedFileUtils.IMAGE_FILE_SUFFIX}"
                    )
            wayPoint.imageURL = reference.toString()
            reference.putFile(this)
        }
        return wayPoint
    }

    suspend fun getCategoryRoutes(category: String): FirebaseResult<List<Route>> {
        return try {
            when (val categoryResult =
                (fireStore.routeCollection.whereEqualTo("category", category).get().await())) {
                is FirebaseResult.Success -> {
                    val routeList = categoryResult.data.toObjects(Route::class.java)
                    Timber.d(routeList.toString())
                    FirebaseResult.Success(routeList)
                }
                is FirebaseResult.Error -> FirebaseResult.Error(categoryResult.exception)
                is FirebaseResult.Canceled -> FirebaseResult.Canceled(categoryResult.exception)
            }
        } catch (exception: Exception) {
            FirebaseResult.Error(exception)
        }
    }

    override suspend fun uploadRouteThumbnail(bitmap: Bitmap): FirebaseResult<Uri> {
        return try {
            val userId = authService.getCurrentUserId() ?: return ErrorConfig.NO_USER_RESULT
            val secretId = UUID.randomUUID().toString()
            val storageRef = fireStorage.reference.child("route_thumbnails/$userId-$secretId")

            // TODO should probably save to file first so we don't need to keep all of this in memory!
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val byteArray = baos.toByteArray()
            // val byteArray = bitmap.convertToByteArray()
            bitmap.recycle()
            val uploadTask = storageRef.putBytes(byteArray)

            when (val uploadResult = uploadTask.await()) {
                is FirebaseResult.Success -> {
                    uploadResult.data.storage.downloadUrl.await()
                }
                is FirebaseResult.Error -> FirebaseResult.Error(uploadResult.exception)
                is FirebaseResult.Canceled -> FirebaseResult.Canceled(uploadResult.exception)
            }
        } catch (exception: Exception) {
            FirebaseResult.Error(exception)
        }
    }
}
