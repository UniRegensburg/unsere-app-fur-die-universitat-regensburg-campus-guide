package de.ur.explure.views

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.crazylegend.viewbinding.viewBinding
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.ui.NavigationView
import com.mapbox.navigation.ui.NavigationViewOptions
import com.mapbox.navigation.ui.OnNavigationReadyCallback
import com.mapbox.navigation.ui.listeners.NavigationListener
import com.mapbox.navigation.ui.map.NavigationMapboxMap
import de.ur.explure.R
import de.ur.explure.databinding.FragmentNavigationUiBinding

// TODO leaving this mode completely destroys our app theme and colors for some reason
class NavigationUIFragment : Fragment(R.layout.fragment_navigation_ui), OnNavigationReadyCallback,
    NavigationListener {

    private val binding by viewBinding(FragmentNavigationUiBinding::bind)

    private val args: NavigationUIFragmentArgs by navArgs()

    private lateinit var navigationView: NavigationView

    // variables for calculating and drawing a route
    private var currentRoute: DirectionsRoute? = null
    // private var navigationMapRoute: NavigationMapRoute? = null

    private lateinit var navigationMapboxMap: NavigationMapboxMap
    private var mapboxNavigation: MapboxNavigation? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentRoute = DirectionsRoute.fromJson(args.route)

        navigationView = binding.navigationView
        binding.navigationView.onCreate(savedInstanceState)
        binding.navigationView.initialize(this)
    }

    override fun onNavigationReady(isRunning: Boolean) {
        val route = currentRoute ?: return

        if (!isRunning && !::navigationMapboxMap.isInitialized) {
            this.navigationMapboxMap = navigationView.retrieveNavigationMapboxMap() ?: return
            navigationView.retrieveMapboxNavigation()?.let {
                this.mapboxNavigation = it
            }

            // Disable off-route detection as this would cause Mapbox to generate a new directionsroute
            // if the user leaves the route but we want to show only the custom route and not the shortest.
            // see https://docs.mapbox.com/android/navigation/guides/off-route/
            mapboxNavigation?.setRerouteController(null)

            val navOptions = NavigationViewOptions.builder(requireActivity())
                .navigationListener(this)
                .directionsRoute(route)
                .shouldSimulateRoute(true)
                .enableVanishingRouteLine(true)
                // .locationObserver(this)
                // .routeProgressObserver(this)
                .build()
            navigationView.startNavigation(navOptions)

            // Reset navigationView Camera Position
            // navigationView.resetCameraPosition()
        }
    }

    override fun onNavigationRunning() {
        // Empty because not needed
    }

    override fun onNavigationFinished() {
        findNavController().popBackStack(R.id.mapFragment, false)
    }

    override fun onCancelNavigation() {
        navigationView.stopNavigation()
        findNavController().popBackStack(R.id.mapFragment, false)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        navigationView.onLowMemory()
    }

    override fun onStart() {
        super.onStart()
        navigationView.onStart()
    }

    override fun onPause() {
        super.onPause()
        navigationView.onPause()
    }

    override fun onResume() {
        super.onResume()
        navigationView.onResume()
    }

    override fun onStop() {
        super.onStop()
        navigationView.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        navigationView.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        navigationView.onDestroy()
        super.onDestroyView()
    }
}
