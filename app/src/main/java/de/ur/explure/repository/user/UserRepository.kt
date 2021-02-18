package de.ur.explure.repository.user

import de.ur.explure.model.user.User
import de.ur.explure.model.user.UserDTO
import de.ur.explure.utils.FirebaseResult

@Suppress("TooManyFunctions")
interface UserRepository {

    suspend fun createUserInFirestore(user: UserDTO): FirebaseResult<Void>

    suspend fun getUserInfo(): FirebaseResult<User>

    suspend fun updateUserName(name: String): FirebaseResult<Void>

    suspend fun addRouteToFinishedRoutes(routeId: String): FirebaseResult<Void>

    suspend fun addRouteToFavouriteRoutes(routeId: String): FirebaseResult<Void>

    suspend fun addRouteToCreatedRoutes(routeId: String): FirebaseResult<Void>

    suspend fun addRouteToActiveRoutes(routeId: String): FirebaseResult<Void>

    suspend fun removeRouteFromFinishedRoutes(routeId: String): FirebaseResult<Void>

    suspend fun removeRouteFromFavouriteRoutes(routeId: String): FirebaseResult<Void>

    suspend fun removeRouteFromCreatedRoutes(routeId: String): FirebaseResult<Void>

    suspend fun removeRouteFromActiveRoutes(routeId: String): FirebaseResult<Void>
}
