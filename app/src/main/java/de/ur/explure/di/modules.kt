package de.ur.explure.di

import android.app.Activity
import android.location.Location
import androidx.lifecycle.Lifecycle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import de.ur.explure.map.LocationManager
import de.ur.explure.map.MapMatchingClient
import de.ur.explure.map.MarkerManager
import de.ur.explure.map.PermissionHelper
import de.ur.explure.map.RouteDrawManager
import de.ur.explure.map.RouteLineManager
import de.ur.explure.map.WaypointsController
import de.ur.explure.navigation.MainAppRouter
import de.ur.explure.repository.category.CategoryRepositoryImpl
import de.ur.explure.repository.rating.RatingRepositoryImpl
import de.ur.explure.repository.route.RouteRepositoryImpl
import de.ur.explure.repository.user.UserRepositoryImpl
import de.ur.explure.services.FireStoreInstance
import de.ur.explure.services.FirebaseAuthService
import de.ur.explure.map.InfoWindowGenerator
import de.ur.explure.map.MapHelper
import de.ur.explure.repository.storage.StorageRepositoryImpl
import de.ur.explure.services.AlgoliaService
import de.ur.explure.utils.SharedPreferencesManager
import de.ur.explure.viewmodel.AuthenticationViewModel
import de.ur.explure.viewmodel.CategoryViewModel
import de.ur.explure.viewmodel.CreateWayPointViewModel
import de.ur.explure.viewmodel.CreatedRoutesViewModel
import de.ur.explure.viewmodel.DiscoverViewModel
import de.ur.explure.viewmodel.EditRouteViewModel
import de.ur.explure.viewmodel.FavoriteRoutesViewModel
import de.ur.explure.viewmodel.MainViewModel
import de.ur.explure.viewmodel.MapViewModel
import de.ur.explure.viewmodel.ProfileViewModel
import de.ur.explure.viewmodel.SaveRouteViewModel
import de.ur.explure.viewmodel.SingleRouteViewModel
import de.ur.explure.viewmodel.SingleWaypointViewModel
import de.ur.explure.viewmodel.StatisticsViewModel
import de.ur.explure.viewmodel.WordSearchViewModel
import org.koin.android.ext.koin.androidApplication
import de.ur.explure.viewmodel.UserDataViewModel
import de.ur.explure.viewmodel.RatingViewModel
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
    factory { (mapView: MapView, map: MapboxMap, mapStyle: Style) ->
        RouteLineManager(androidApplication(), mapView, map, mapStyle)
    }
    factory { (mapView: MapView, map: MapboxMap) ->
        RouteDrawManager(mapView, map)
    }
    factory { (callback: (Location) -> Unit) ->
        LocationManager(androidApplication(), callback)
    }
    factory { (mapView: MapView, lifecycle: Lifecycle) ->
        MapHelper(mapView, lifecycle)
    }
    factory { (activityContext: Activity) -> InfoWindowGenerator(activityContext) }
    single { PermissionHelper() }
    single { WaypointsController() }
    single { MapMatchingClient(androidApplication()) }

    // navigation router
    single { MainAppRouter() }

    // firebase
    single { FirebaseAuth.getInstance() }
    factory { FirebaseFirestore.getInstance() }
    factory { FirebaseStorage.getInstance() }
    factory { FireStoreInstance(get()) }
    single { FirebaseAuthService(get()) }

    // algolia
    single { AlgoliaService() }

    // repositories
    single { RatingRepositoryImpl(get(), get()) }
    single { RouteRepositoryImpl(get(), get(), get(), get()) }
    single { UserRepositoryImpl(get(), get(), get()) }
    single { CategoryRepositoryImpl(get(), get()) }
    single { StorageRepositoryImpl(get()) }

    // viewmodels
    viewModel { AuthenticationViewModel(get(), get()) }
    viewModel { WordSearchViewModel(get(), get()) }
    viewModel { CategoryViewModel(get(), get()) }
    viewModel { DiscoverViewModel(get(), get(), get()) }
    viewModel { MainViewModel(get(), get(), get()) }
    viewModel { MapViewModel(get(), get(), get()) }
    viewModel { EditRouteViewModel(get(), get(), get()) }
    viewModel { SaveRouteViewModel(get(), get(), get(), get()) }
    viewModel { CreateWayPointViewModel(get()) }
    viewModel { ProfileViewModel(get(), get(), get()) }
    viewModel { CreatedRoutesViewModel(get(), get(), get()) }
    viewModel { FavoriteRoutesViewModel(get(), get(), get()) }
    viewModel { StatisticsViewModel(get(), get(), get(), get()) }
    viewModel { SingleRouteViewModel(get(), get(), get()) }
    viewModel { UserDataViewModel(get(), get()) }
    viewModel { RatingViewModel(get()) }
    viewModel { SingleWaypointViewModel(get()) }
}
