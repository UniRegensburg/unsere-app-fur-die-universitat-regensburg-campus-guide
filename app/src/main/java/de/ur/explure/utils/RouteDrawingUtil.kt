package de.ur.explure.utils

/**
 * * This Util was taken from:
 * https://github.com/mapbox/mapbox-navigation-android/blob/main/examples/src/main/java/com/mapbox/navigation/examples/utils/RouteDrawingUtil.kt
 */

/**
 * A utility for drawing a line on a map and using map matching to get a route.
 * When enable() is called the utility will listen for long click events on the map and for each point
 * a dot will be placed on the map with a line between representing the collective points received.
 *
 * Calling fetchRoute() will attempt to fetch a map matched route matching the points that
 * have been configured.  When a route is received draw it on the map and be sure to set the route
 * on your [MapboxNavivation] instance.
 *
 * Suggested usage:
 * Instantiate this class after the Map Style is loaded and ready.
 *
 * Call enable() to activate the long press listener which will collect the points used for creating
 * a route.
 *
 * Call disable() to deactivate the long press listener.
 *
 * When finished establishing points for the route you want to create call fetchRoute(). If a route
 * was received successfully it will be passed to the [RoutesRequestCallback] else a toast will appear
 * with some error information, also the error information will be logged.
 *
 * When a route is received call clear() this utility's line.
 *
 * Another useful function is removeLastPoint() which will remove the last point added. This could
 * be useful as a sort of "undo" if you make a mistake in where you press on the map. You may need
 * to temporarily add a button to your layout in order to make use of this function.
 *
 */
/*
class RouteDrawingUtil(private val mapView: MapView) {

    private val touchPoints = mutableListOf<Point>()

    init {
        mapView.getMapAsync { map ->
            map.getStyle { style ->
                val drawingLayerSource = style.getSourceAs<GeoJsonSource>(
                    LINE_LAYER_SOURCE_ID
                )
                if (drawingLayerSource == null) {
                    style.addSource(GeoJsonSource(LINE_LAYER_SOURCE_ID))
                }

                val lineEndSource = style.getSourceAs<GeoJsonSource>(
                    LINE_END_SOURCE_ID
                )
                if (lineEndSource == null) {
                    style.addSource(GeoJsonSource(LINE_END_SOURCE_ID))
                }

                val drawingLayer = style.getLayerAs<LineLayer>(LINE_LAYER_ID)
                if (drawingLayer == null) {
                    style.addLayer(
                        LineLayer(
                            LINE_LAYER_ID,
                            LINE_LAYER_SOURCE_ID
                        ).withProperties(
                            lineWidth(LINE_WIDTH),
                            lineJoin(LINE_JOIN_ROUND),
                            lineOpacity(LINE_OPACITY),
                            lineColor(Color.parseColor(LINE_COLOR))
                        )
                    )
                }

                val lineEndLayer = style.getLayerAs<CircleLayer>(LINE_END_LAYER_ID)
                if (lineEndLayer == null) {
                    style.addLayer(
                        CircleLayer(
                            LINE_END_LAYER_ID,
                            LINE_END_SOURCE_ID
                        ).withProperties(
                            circleRadius(CIRCLE_RADIUS),
                            circleOpacity(1f),
                            circleColor(Color.BLACK)
                        )
                    )
                }
            }
        }
    }

    private val mapLongClickListener = MapboxMap.OnMapLongClickListener { latLng ->
        touchPoints.add(Point.fromLngLat(latLng.longitude, latLng.latitude))
        mapView.getMapAsync { map ->
            map.getStyle { style ->
                when (touchPoints.size) {
                    0 -> {
                    }
                    1 -> {
                        style.getSourceAs<GeoJsonSource>(LINE_END_SOURCE_ID)
                            ?.setGeoJson(touchPoints.first())
                    }
                    else -> {
                        style.getSourceAs<GeoJsonSource>(LINE_LAYER_SOURCE_ID)
                            ?.setGeoJson(LineString.fromLngLats(touchPoints))
                        style.getSourceAs<GeoJsonSource>(LINE_END_SOURCE_ID)
                            ?.setGeoJson(getFeatureCollection(touchPoints))
                    }
                }
            }
        }

        true
    }

    fun clear() {
        touchPoints.clear()
        mapView.getMapAsync { map ->
            map.getStyle { style ->
                style.getSourceAs<GeoJsonSource>(LINE_LAYER_SOURCE_ID)
                    ?.setGeoJson(LineString.fromLngLats(listOf()))
                style.getSourceAs<GeoJsonSource>(LINE_END_SOURCE_ID)
                    ?.setGeoJson(FeatureCollection.fromFeatures(listOf()))
            }
        }
    }

    fun disable() {
        mapView.getMapAsync { map ->
            map.removeOnMapLongClickListener(mapLongClickListener)
        }
    }

    fun enable() {
        mapView.getMapAsync { map ->
            map.addOnMapLongClickListener(mapLongClickListener)
        }
    }

    private fun getFeatureCollection(points: List<Point>): FeatureCollection {
        return points.map {
            Feature.fromGeometry(it)
        }.run {
            FeatureCollection.fromFeatures(this)
        }
    }

    fun removeLastPoint() {
        if (touchPoints.isNotEmpty()) {
            touchPoints.removeLast()
            mapView.getMapAsync { map ->
                map.getStyle { style ->
                    style.getSourceAs<GeoJsonSource>(LINE_LAYER_SOURCE_ID)
                        ?.setGeoJson(LineString.fromLngLats(touchPoints))
                    style.getSourceAs<GeoJsonSource>(LINE_END_SOURCE_ID)
                        ?.setGeoJson(getFeatureCollection(touchPoints))
                }
            }
        }
    }

    fun fetchRoute(routeReadyCallback: RoutesRequestCallback) {
        if (touchPoints.size < 2) {
            return
        }

        val mapMatching = MapboxMapMatching.builder()
            .accessToken(getMapboxAccessToken(mapView.context))
            .coordinates(touchPoints)
            .waypointIndices(0, touchPoints.size - 1)
            .steps(true)
            .bannerInstructions(false)
            .voiceInstructions(false)
            .profile(DirectionsCriteria.PROFILE_DRIVING)
            .build()

        mapMatching.enqueueCall(
            object : Callback<MapMatchingResponse> {
                override fun onFailure(call: Call<MapMatchingResponse>, t: Throwable) {
                    Timber.e("MapMatching request failure %s", t.toString())
                }

                override fun onResponse(
                    call: Call<MapMatchingResponse>,
                    response: Response<MapMatchingResponse>
                ) {
                    val route = response.body()?.matchings()?.get(0)?.toDirectionRoute()
                    if (route == null) {
                        Timber.e("Failed to get a route with message ${response.code()} ${response.message()}")
                        Toast.makeText(
                            mapView.context,
                            "Failed to get a route with message ${response.code()} ${response.message()}",
                            Toast.LENGTH_SHORT
                        ).show()
                        clear()
                        enable()
                    } else {
                        routeReadyCallback.onRoutesReady(listOf(route))
                    }
                }
            }
        )
    }

    companion object {
        const val LINE_LAYER_SOURCE_ID = "DRAW_UTIL_LINE_LAYER_SOURCE_ID"
        const val LINE_LAYER_ID = "DRAW_UTIL_LINE_LAYER_ID"
        const val LINE_END_LAYER_ID = "DRAW_UTIL_LINE_END_LAYER_ID"
        const val LINE_END_SOURCE_ID = "DRAW_UTIL_LINE_END_SOURCE_ID"
        private const val LINE_COLOR = "#ffcc00"
        private const val LINE_WIDTH = 5f
        private const val LINE_OPACITY = 1f
        private const val CIRCLE_RADIUS = 5f
    }
}
*/