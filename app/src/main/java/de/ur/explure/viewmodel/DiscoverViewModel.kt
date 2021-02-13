package de.ur.explure.viewmodel

import androidx.lifecycle.ViewModel
import de.ur.explure.navigation.AppRouter
import de.ur.explure.views.DiscoverFragmentDirections

class DiscoverViewModel(private val appRouter: AppRouter) : ViewModel() {

    fun showMap() {
        val mapAction = DiscoverFragmentDirections.actionDiscoverFragmentToMapFragment()
        appRouter.getNavController().navigate(mapAction)
    }
}
