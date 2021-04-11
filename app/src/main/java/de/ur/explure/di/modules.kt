package de.ur.explure.di

import android.location.Location
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import de.ur.explure.map.LocationManager
import de.ur.explure.map.MarkerManager
import de.ur.explure.navigation.MainAppRouter
import de.ur.explure.repository.category.CategoryRepositoryImpl
import de.ur.explure.repository.rating.RatingRepositoryImpl
import de.ur.explure.repository.route.RouteRepositoryImpl
import de.ur.explure.repository.user.UserRepositoryImpl
import de.ur.explure.services.FireStoreInstance
import de.ur.explure.services.FirebaseAuthService
import de.ur.explure.utils.SharedPreferencesManager
import de.ur.explure.viewmodel.*
import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Main Koin Module defining the application's components
 *
 */

val mainModule = module {
    single { SharedPreferencesManager(androidApplication()) }
    // use factory for MarkerManager to always return a new one, in case the mapStyle changes or a config change occurs
    factory { (mapView: MapView, map: MapboxMap, mapStyle: Style) ->
        MarkerManager(androidApplication(), mapView, map, mapStyle)
    }
    factory { (callback: (Location) -> Unit) ->
        LocationManager(androidApplication(), callback)
    }

    // navigation router
    single { MainAppRouter() }

    // firebase
    single { FirebaseAuth.getInstance() }
    factory { FirebaseFirestore.getInstance() }
    factory { FirebaseStorage.getInstance() }
    factory { FireStoreInstance(get()) }
    single { FirebaseAuthService(get()) }

    // repositories
    single { RatingRepositoryImpl(get(), get()) }
    single { RouteRepositoryImpl(get(), get()) }
    single { UserRepositoryImpl(get(), get(), get()) }
	single { CategoryRepositoryImpl(get(), get()) }

    // viewmodels
    viewModel { AuthenticationViewModel(get(), get()) }
    viewModel { WordSearchViewModel(get(), get()) }
    viewModel { CategoryViewModel(get(), get()) }
    viewModel { DiscoverViewModel(get(), get(), get()) }
    viewModel { MainViewModel(get(), get()) }
    viewModel { MapViewModel(get()) }
    viewModel { CreateRouteViewModel(get(), get(), get()) }
    viewModel { CreateWayPointViewModel() }
	viewModel { ProfileViewModel(get(), get(), get()) }
    viewModel { CreatedRoutesViewModel(get(), get(), get()) }
    viewModel { FavoriteRoutesViewModel(get(), get(), get()) }
    viewModel { StatisticsViewModel(get(), get(), get(), get()) }
    viewModel { SingleRouteViewModel(get()) }
}
