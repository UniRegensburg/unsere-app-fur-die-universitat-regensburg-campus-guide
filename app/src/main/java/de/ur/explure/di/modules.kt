package de.ur.explure.di

import de.ur.explure.navigation.AppRouter
import de.ur.explure.viewmodel.MainViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Main Koin Module defining the application's components
 *
 */

val mainModule = module {
    single { AppRouter() }
    viewModel { MainViewModel(get()) }
}
