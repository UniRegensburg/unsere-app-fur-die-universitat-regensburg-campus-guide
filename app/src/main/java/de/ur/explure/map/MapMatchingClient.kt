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

    @Suppress("SpreadOperator")
    fun requestMapMatchedRoute(coordinates: List<Point>) {
        Timber.d("MapMatching request with ${coordinates.size} coordinates.")

        // Create a snap radius for every coordinate point; must be a value between 0.0 and 50.0 (meter).
        // This determines how far the map matching api can snap the point to known routes, that means
        // higher values will create more often successful but quite fuzzy routes, while low values
        // (default is 5 meter) will be quite accurate but won't produce results for some routes.
        val snapRadiuses = Array(coordinates.size) { return@Array DEFAULT_SNAP_RADIUS }

        val mapMatchingRequest = MapboxMapMatching.builder()
            .accessToken(getMapboxAccessToken(context))
            .profile(DirectionsCriteria.PROFILE_WALKING)
            .coordinates(coordinates)
            // set indices for correct navigation instructions with waypoints
            // (otherwise every waypoint would be an end point!)
            .waypointIndices(0, coordinates.size - 1)
            // .addWaypointNames(...) // add a name to every waypoint given
            .radiuses(*snapRadiuses) // increase default map matching radius to get more routes
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
                    // for errors see https://docs.mapbox.com/api/navigation/map-matching/#map-matching-api-errors
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

        // Testing routes for manual route creation with markers:
        /*
        // vor Vielberth-Gebäude -> vor Wirtschaft bei Künstlergarderoben -> vor Zentralbib ->
        // vor Edeka -> vor Gewächshaus bei botanischem Garten
        val wayPoints1 = listOf<Point>(
            Point.fromLngLat(12.09557108259304, 49.000041978000496),
            Point.fromLngLat(12.093845692056306, 48.99882152821593),
            Point.fromLngLat(12.09530219054713, 48.998001399781884),
            Point.fromLngLat(12.093340435528745, 48.997536834987386),
            Point.fromLngLat(12.091857047675944, 48.99459799361907)
        )
        // Busbahnhof -> Forum -> unterhalb Uni-See -> in Chemie Cafete
        // schwerer, da letzter Punkt in der Chemiecafete und deshalb nicht direkt matchbar
        val wayPoints2 = listOf<Point>(
            Point.fromLngLat(12.092106289665622, 48.99849122229557),
            Point.fromLngLat(12.094221280884113, 48.99779454841865),
            Point.fromLngLat(12.095034668600732, 48.99695424906457),
            Point.fromLngLat(12.095637216208019, 48.99583671282065)
        )*/

        // Testing routes for map matching and free draw:
        /*
        // Busbahnhof -> Mensa -> vor Zentralbib -> Unisee -> Weg unter Mensa -> Botanischer Garten
        val routePoints = listOf<Point>(
            Point.fromLngLat(12.091897088615497, 48.9986755276432),
            Point.fromLngLat(12.093398779683042, 48.99790078797133),
            Point.fromLngLat(12.095176764949287, 48.99759573694382),
            Point.fromLngLat(12.095045596693524, 48.99696500867813),
            Point.fromLngLat(12.092009249797059, 48.996774307308414),
            Point.fromLngLat(12.091600540864277, 48.99278790637206),
        )

        // Vielberthgebäude -> Unisee -> Botanischer Garten
        val routePointsSimple = listOf<Point>(
            Point.fromLngLat(12.095577, 49.000083),
            Point.fromLngLat(12.095153, 48.998243),
            Point.fromLngLat(12.091600540864277, 48.99278790637206)
        )

        // Vielberthgebäude -> PT-Cafete -> vor Zentralbib -> Mensa -> Weg unter Mensa
        // -> Botanischer Garten -> außen vor H51 -> Chemie-Cafete -> Unisee -> Kugel
        val routePointsComplicated = listOf<Point>(
            Point.fromLngLat(12.095577, 49.000083),
            Point.fromLngLat(12.095873, 48.999174),
            Point.fromLngLat(12.095224, 48.998051),
            Point.fromLngLat(12.093412, 48.997994),
            Point.fromLngLat(12.091575, 48.993578),
            Point.fromLngLat(12.094713, 48.994392),
            Point.fromLngLat(12.095448, 48.995758),
            Point.fromLngLat(12.095244, 48.997110),
            Point.fromLngLat(12.095153, 48.998243)
        )*/
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
