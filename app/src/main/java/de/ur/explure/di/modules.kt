package de.ur.explure.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import de.ur.explure.SearchListAdapter
import de.ur.explure.navigation.MainAppRouter
import de.ur.explure.navigation.StateAppRouter
import de.ur.explure.repository.rating.RatingRepositoryImpl
import de.ur.explure.repository.route.RouteRepositoryImpl
import de.ur.explure.repository.user.UserRepositoryImpl
import de.ur.explure.services.FireStoreInstance
import de.ur.explure.services.FirebaseAuthService
import de.ur.explure.viewmodel.*
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
    viewModel { SearchViewModel(get())}
    viewModel { WordSearchViewModel(get()) }
    viewModel { StateViewModel() }
    viewModel { BottomNavViewModel() }
}
