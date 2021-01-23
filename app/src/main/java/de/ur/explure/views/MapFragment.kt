package de.ur.explure.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import de.ur.explure.databinding.FragmentMapBinding
import de.ur.explure.utils.viewLifecycle
import de.ur.explure.viewmodel.MapViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class MapFragment : Fragment(), OnMapReadyCallback, MapboxMap.OnMapClickListener {

    private val mapViewModel: MapViewModel by viewModel()

    private var binding: FragmentMapBinding by viewLifecycle()

    private var mapView: MapView? = null
    // private lateinit var map: MapboxMap

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMapBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // disable the button until the map has finished loading
        binding.ownLocationButton.isEnabled = false

        setupViewModelObservers()

        mapView = binding.mapView
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)
    }

    private fun setupViewModelObservers() {
        // TODO use observeOnce maybe?
        mapViewModel.mapReady.observe(viewLifecycleOwner, {
            Toast.makeText(
                requireContext(),
                "Map has finished loading and can be used now!",
                Toast.LENGTH_SHORT
            ).show()

            binding.ownLocationButton.isEnabled = true
        })
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        // this.map = mapboxMap

        mapboxMap.addOnMapClickListener(this)

        // TODO setup a separate mapbox map object/singleton to handle an encapsulate map stuff?
        mapboxMap.setStyle(Style.MAPBOX_STREETS) {
            // Map is set up and the style has loaded.
            mapViewModel.mapReady.value = true

            // print out all layers of current style
            for (singleLayer in it.layers) {
                Timber.d("onMapReady: layer id = %s", singleLayer.id)
            }
        }
    }

    override fun onMapClick(point: LatLng): Boolean {
        Timber.d("Clicked on map point with coordinates: $point")
        // return true if this click should be consumed and not passed to other listeners registered afterwards
        return true
    }

    /**
     * Mapbox Lifecycle Hooks
     */

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // map.removeOnMapClickListener(this)
        mapView?.onDestroy()
    }
}
