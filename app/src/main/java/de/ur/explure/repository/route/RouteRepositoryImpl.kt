package de.ur.explure.repository.route

import de.ur.explure.model.route.Route
import de.ur.explure.model.route.RouteDTO
import de.ur.explure.services.FireStoreInstance
import de.ur.explure.services.FirebaseAuthService
import de.ur.explure.utils.FirebaseResult

class RouteRepositoryImpl(
    private val authService: FirebaseAuthService,
    private val fireStore: FireStoreInstance
) : RouteRepository {
    override suspend fun addRouteToFireStore(routeDTO: RouteDTO): FirebaseResult<Void> {
        TODO("Not yet implemented")
    }

    override suspend fun getRoute(routeId: String): FirebaseResult<Route> {
        TODO("Not yet implemented")
    }

    override suspend fun getRoutes(routeIds: List<String>): FirebaseResult<List<Route>> {
        TODO("Not yet implemented")
    }

    override suspend fun deleteRoute(routeId: String): FirebaseResult<Void> {
        TODO("Not yet implemented")
    }
}