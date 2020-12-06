package de.ur.campusguide.di

import de.ur.campusguide.navigation.AppRouter
import de.ur.campusguide.viewmodel.MainViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val mainModule = module {
    single { AppRouter() }
    viewModel { MainViewModel(get()) }
}