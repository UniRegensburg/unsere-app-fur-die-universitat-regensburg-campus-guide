package de.ur.explure.map

import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.trip.session.RouteProgressObserver

// TODO
class CustomRouteProgressObserver : RouteProgressObserver {
    fun start() {
        if (MapboxNavigationProvider.isCreated()) {
            MapboxNavigationProvider.retrieve().registerRouteProgressObserver(this)
        }
    }

    fun stop() {
        if (MapboxNavigationProvider.isCreated()) {
            MapboxNavigationProvider.retrieve().unregisterRouteProgressObserver(this)
        }
    }

    override fun onRouteProgressChanged(routeProgress: RouteProgress) {
        // My route progress logic
    }
}
