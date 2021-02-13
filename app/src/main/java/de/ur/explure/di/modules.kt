package de.ur.explure.di

import android.location.Location
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import de.ur.explure.map.LocationManager
import de.ur.explure.map.MarkerManager
import de.ur.explure.navigation.AppRouter
import de.ur.explure.repository.rating.RatingRepositoryImpl
import de.ur.explure.repository.user.UserRepositoryImpl
import de.ur.explure.services.FireStoreInstance
import de.ur.explure.services.FirebaseAuthService
import de.ur.explure.viewmodel.DiscoverViewModel
import de.ur.explure.viewmodel.MainViewModel
import de.ur.explure.viewmodel.MapViewModel
import de.ur.explure.viewmodel.TestViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Main Koin Module defining the application's components
 *
 */

val mainModule = module {
    single { AppRouter() }

    // use factory for MarkerManager to always return a new one, in case the mapStyle changes or a config change occurs
    factory { (mapView: MapView, map: MapboxMap, mapStyle: Style) ->
        MarkerManager(androidApplication(), mapView, map, mapStyle)
    }
    factory { (callback: (Location) -> Unit) ->
        LocationManager(androidApplication(), callback)
    }
    // single { (context: Activity) -> PermissionHelper(context) }
    // single { MapController() }

    single { FirebaseAuth.getInstance() }
    factory { FirebaseFirestore.getInstance() }
    factory { FireStoreInstance(get()) }
    single { FirebaseAuthService(get()) }
    single { UserRepositoryImpl(get(), get()) }
    single { RatingRepositoryImpl(get(), get()) }

    viewModel { TestViewModel(get(), get(), get()) }
    viewModel { MainViewModel(get()) }
    viewModel { MapViewModel(get()) }
    viewModel { DiscoverViewModel(get()) }
}
