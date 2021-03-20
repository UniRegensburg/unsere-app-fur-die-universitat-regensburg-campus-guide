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

class MapMatchingClient(context: Context) {

    private var mapMatchingListener: MapMatchingListener? = null

    private val mapMatchingConfig = MapboxMapMatching.builder()
        .accessToken(getMapboxAccessToken(context))
        .profile(DirectionsCriteria.PROFILE_WALKING)
        // * optional params: *
        .tidy(true)
        .overview(DirectionsCriteria.OVERVIEW_FULL)
        .geometries("polyline6") // maximal precision
        .steps(true)
        .bannerInstructions(false)
        .voiceInstructions(false)
        .annotations(
            DirectionsCriteria.ANNOTATION_DURATION,
            DirectionsCriteria.ANNOTATION_DISTANCE
        )

    // Todo: auch aus dem MapMatching response obj könnte man die route instructions bekommen!!
    // see https://docs.mapbox.com/help/tutorials/get-started-map-matching-api/#display-the-turn-by-turn-directions

    fun requestMapMatchedRoute(coordinates: List<Point>) {

        Timber.d("MapMatching request with ${coordinates.size} coordinates.")
        if (coordinates.size < 2) {
            // we need at least two points to get a successful match!
            return
        }

        val mapMatchingRequest = mapMatchingConfig
            .coordinates(coordinates)
            .waypointIndices(0, coordinates.size - 1)
            // .addWaypointNames(...) // add a name to every waypoint given
            .build()

        mapMatchingRequest.enqueueCall(
            object : Callback<MapMatchingResponse> {
                override fun onFailure(call: Call<MapMatchingResponse>, t: Throwable) {
                    Timber.e("MapMatching request failure %s", t.toString())
                    // TODO check for all exceptions and give appropriate user feedback
                    //  see https://docs.mapbox.com/api/navigation/map-matching/#map-matching-api-errors
                    mapMatchingListener?.onRouteMatchingFailed(t.toString())
                }

                override fun onResponse(call: Call<MapMatchingResponse>, response: Response<MapMatchingResponse>) {
                    if (!response.isSuccessful) {
                        Timber.e("MapMatching response unsuccessful: ${response.errorBody()}")
                        mapMatchingListener?.onRouteMatchingFailed(response.message())
                        return
                    }
                    Timber.d("MapMatching request succeeded")

                    val allMatchings = response.body()?.matchings()
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
                    Timber.d("All Trace Points:\n $allTracePoints")

                    mapMatchingListener?.onRouteMatched(allMatchings)
                }
            }
        )
    }

    fun setMapMatchingListener(listener: MapMatchingListener) {
        this.mapMatchingListener = listener
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
