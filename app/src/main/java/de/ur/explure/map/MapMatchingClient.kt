package de.ur.explure.map

import android.content.Context
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.matching.v5.MapboxMapMatching
import com.mapbox.api.matching.v5.models.MapMatchingMatching
import com.mapbox.api.matching.v5.models.MapMatchingResponse
import com.mapbox.geojson.Point
import de.ur.explure.utils.getMapboxAccessToken
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber

class MapMatchingClient(private val context: Context) {

    private var mapMatchingListener: MapMatchingListener? = null

    // Todo: auch aus dem MapMatching response obj könnte man die route instructions bekommen!!
    // see https://docs.mapbox.com/help/tutorials/get-started-map-matching-api/#display-the-turn-by-turn-directions

    fun requestMapMatchedRoute(coordinates: List<Point>) {
        Timber.d("MapMatching request with ${coordinates.size} coordinates.")

        if (coordinates.size < 2) {
            // we need at least two points to get a successful match!
            Timber.e("Map Matching not possible! At least two coordinates are necessary!")
            return
        } else if (coordinates.size > 100) {
            // the api also doesn't accept requests with more than 100 coordinates
            Timber.e("Map Matching not possible! There can be no more than 100 coordinates!")
            return
        }

        // Create a snap radius for every coordinate point; must be a value between 0.0 and 50.0 (meter).
        // This determines how far the map matching api can snap the point to known routes, that means
        // higher values will create more often successful but quite fuzzy routes, while low values
        // (default is 5 meter) will be quite accurate but won't produce results for some routes.
        // val snapRadiuses = Array(coordinates.size) { return@Array DEFAULT_SNAP_RADIUS }

        val mapMatchingRequest = MapboxMapMatching.builder()
            .accessToken(getMapboxAccessToken(context))
            .profile(DirectionsCriteria.PROFILE_WALKING)
            .coordinates(coordinates)
            // set indices for correct navigation instructions with waypoints
            // (otherwise every waypoint would be an end point!)
            .waypointIndices(0, coordinates.size - 1)
            // .addWaypointNames(...) // add a name to every waypoint given
            // .radiuses(*snapRadiuses) // increase default map matching radius to get more routes
            // * optional params: *
            .tidy(true)
            .overview(DirectionsCriteria.OVERVIEW_FULL)
            .steps(true)
            .bannerInstructions(true)
            .voiceInstructions(false)
            .annotations(
                DirectionsCriteria.ANNOTATION_DURATION,
                DirectionsCriteria.ANNOTATION_DISTANCE
            )
            .build()

        mapMatchingRequest.enqueueCall(
            object : Callback<MapMatchingResponse> {
                override fun onFailure(call: Call<MapMatchingResponse>, t: Throwable) {
                    Timber.e("MapMatching request failure %s", t.toString())
                    // TODO check for all exceptions and give appropriate user feedback
                    //  see https://docs.mapbox.com/api/navigation/map-matching/#map-matching-api-errors
                    mapMatchingListener?.onRouteMatchingFailed(t.toString())
                }

                override fun onResponse(
                    call: Call<MapMatchingResponse>,
                    response: Response<MapMatchingResponse>
                ) {
                    if (!response.isSuccessful) {
                        Timber.e("MapMatching response unsuccessful with code ${response.code()}")
                        Timber.e("Errormessage: ${response.errorBody()?.string()}")
                        mapMatchingListener?.onRouteMatchingFailed(response.message())
                        return
                    }

                    val allMatchings = response.body()?.matchings()
                    Timber.d("MapMatching request succeeded! Found ${allMatchings?.size} matchings!")

                    if (allMatchings == null || allMatchings.isEmpty()) {
                        Timber.w("Couldn't get any map matchings for the waypoints!")
                        mapMatchingListener?.onNoRouteMatchings()
                        return
                    }

                    // TODO things to do here:
                    //  - print the confidence and ask the user to provide more/ or more closely
                    //    aligned points if below threshold -> probably not: confidence is usually quite
                    //    low but it works not too bad and there are almost never alternatives :(
                    //  - explain how this map matching works and that it is meant for outdoor usage!!

                    val allTracePoints = response.body()?.tracepoints()
                    Timber.d("All ${allTracePoints?.size} Trace Points:\n $allTracePoints")

                    mapMatchingListener?.onRouteMatched(allMatchings)
                }
            }
        )
    }

    fun setMapMatchingListener(listener: MapMatchingListener?) {
        this.mapMatchingListener = listener
    }

    companion object {
        private const val DEFAULT_SNAP_RADIUS = 15.0 // in meter
    }

    interface MapMatchingListener {
        /**
         * Called when the map matching request was successful and at least one map matched route
         * was found.
         */
        fun onRouteMatched(allMatchings: MutableList<MapMatchingMatching>)

        /**
         * Called when the map matching request was successful but no match could be found for the
         * given points.
         */
        fun onNoRouteMatchings()

        /**
         * Called when the map matching request failed for some reason, either due to a network error,
         * a server error or because the api rate limit was exceeded.
         */
        fun onRouteMatchingFailed(message: String)
    }
}
