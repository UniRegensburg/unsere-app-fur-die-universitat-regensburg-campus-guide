package de.ur.explure.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import de.ur.explure.navigation.MainAppRouter
import de.ur.explure.navigation.StateAppRouter
import de.ur.explure.repository.rating.RatingRepositoryImpl
import de.ur.explure.repository.route.RouteRepositoryImpl
import de.ur.explure.repository.user.UserRepositoryImpl
import de.ur.explure.services.FireStoreInstance
import de.ur.explure.services.FirebaseAuthService
import de.ur.explure.viewmodel.AuthenticationViewModel
import de.ur.explure.viewmodel.BottomNavViewModel
import de.ur.explure.viewmodel.SearchViewModel
import de.ur.explure.viewmodel.StateViewModel
import de.ur.explure.viewmodel.TestViewModel
import de.ur.explure.viewmodel.ProfileFragmentViewModel
import de.ur.explure.viewmodel.CreatedRoutesFragmentViewModel
import de.ur.explure.viewmodel.FavoriteRoutesFragmentViewModel
import de.ur.explure.viewmodel.StatisticsFragmentViewModel
import de.ur.explure.viewmodel.SingleRouteViewModel
import de.ur.explure.viewmodel.WordSearchViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Main Koin Module defining the application's components
 *
 */

val mainModule = module {
    single { MainAppRouter() }
    single { StateAppRouter() }
    single { FirebaseAuth.getInstance() }
    factory { FirebaseFirestore.getInstance() }
    factory { FireStoreInstance(get()) }
    single { FirebaseAuthService(get()) }
    single { RatingRepositoryImpl(get(), get()) }
    single { RouteRepositoryImpl(get(), get()) }
    single { UserRepositoryImpl(get(), get()) }
    viewModel { AuthenticationViewModel(get(), get()) }
    viewModel { TestViewModel(get(), get(), get()) }
    viewModel { SearchViewModel(get()) }
    viewModel { WordSearchViewModel(get()) }
    viewModel { StateViewModel() }
    viewModel { BottomNavViewModel() }
    viewModel { ProfileFragmentViewModel(get(), get()) }
    viewModel { CreatedRoutesFragmentViewModel(get(), get(), get()) }
    viewModel { FavoriteRoutesFragmentViewModel(get(), get(), get()) }
    viewModel { StatisticsFragmentViewModel(get(), get(), get(), get()) }
    viewModel { SingleRouteViewModel(get()) }
}
