package de.ur.explure.di

import com.google.firebase.auth.FirebaseAuth
import de.ur.explure.navigation.AppRouter
import de.ur.explure.repository.AuthenticationRepository
import de.ur.explure.viewmodel.AuthenticationViewModel
import de.ur.explure.viewmodel.MainViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Main Koin Module defining the application's components
 *
 */

val mainModule = module {
    single { AppRouter() }
    single { FirebaseAuth.getInstance() }
    single { AuthenticationRepository(get()) }
    viewModel { AuthenticationViewModel(get()) }
    viewModel { MainViewModel(get()) }
}
