package de.ur.explure.utils

object DeepLinkUtils {
    private const val SCHEME = "http:"
    private const val HOST = "explure.de"
    private const val ROUTE_PREFIX = "route"
    const val ID_PARAMETER_KEY = "id"

    fun getURLforRouteId(routeId: String): String {
        return "$SCHEME//$HOST/$ROUTE_PREFIX/?$ID_PARAMETER_KEY=$routeId"
    }
}
