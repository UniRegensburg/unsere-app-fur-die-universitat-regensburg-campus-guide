package de.ur.explure.viewmodel

import androidx.lifecycle.ViewModel
import de.ur.explure.repository.route.RouteRepositoryImpl

class SingleRouteViewModel(private val routeRepository: RouteRepositoryImpl) : ViewModel()
