package de.ur.explure.di

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import de.ur.explure.MapController
import de.ur.explure.MarkerManager
import de.ur.explure.navigation.AppRouter
import de.ur.explure.repository.user.UserRepositoryImpl
import de.ur.explure.services.FireStoreInstance
import de.ur.explure.services.FirebaseAuthService
import de.ur.explure.viewmodel.DiscoverViewModel
import de.ur.explure.viewmodel.MainViewModel
import de.ur.explure.viewmodel.MapViewModel
import de.ur.explure.viewmodel.TestViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Main Koin Module defining the application's components
 *
 */

val mainModule = module {
    single { AppRouter() }
    single { MapController() }
    // use factory for MarkerManager to always return a new one, in case the mapStyle changes or a config change occurs
    factory { (context: Context, mapView: MapView, map: MapboxMap, mapStyle: Style) ->
        MarkerManager(context, mapView, map, mapStyle)
    }
    single { FirebaseAuth.getInstance() }
    factory { FirebaseFirestore.getInstance() }
    factory { FireStoreInstance(get()) }
    single { FirebaseAuthService(get()) }
    single { UserRepositoryImpl(get(), get()) }
    viewModel { TestViewModel(get(), get(), get()) }
    viewModel { MainViewModel(get()) }
    viewModel { MapViewModel(get()) }
    viewModel { DiscoverViewModel(get()) }
}
