package de.ur.explure.repository.route

import de.ur.explure.model.rating.RatingDTO
import de.ur.explure.model.route.Route
import de.ur.explure.model.route.RouteDTO
import de.ur.explure.utils.FirebaseResult

interface RouteRepository {

    suspend fun addRouteToFireStore(routeDTO: RouteDTO) : FirebaseResult<Void>

    suspend fun getRoute(routeId: String) : FirebaseResult<Route>

    suspend fun getRoutes(routeIds: List<String>) : FirebaseResult<List<Route>>

    suspend fun deleteRoute(routeId: String) : FirebaseResult<Void>

}