package de.ur.explure.viewmodel

import android.widget.TextView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.ur.explure.repository.route.RouteRepositoryImpl
import de.ur.explure.utils.FirebaseResult
import kotlinx.coroutines.launch

@Suppress("StringLiteralDuplication")
class SingleRouteViewModel(private val routeRepository: RouteRepositoryImpl) : ViewModel() {
    fun setRouteName(textView: TextView) {
        textView.text = ""
        viewModelScope.launch {
            when (val routeName = routeRepository.getRoute("83bAuunZzXwaPIJ0Xc3a")) {
                is FirebaseResult.Success -> {
                    textView.text = routeName.data.title
                }
            }
        }
    }

    fun setRouteInformation(textView: TextView) {
        textView.text = ""
        viewModelScope.launch {
            when (val routeInformation = routeRepository.getRoute("83bAuunZzXwaPIJ0Xc3a")) {
                is FirebaseResult.Success -> {
                    textView.text = routeInformation.data.description
                }
            }
        }
    }

    fun setWaypoints(textView: TextView) {
        textView.text = ""
        viewModelScope.launch {
            when (val routeWaypoints = routeRepository.getRoute("83bAuunZzXwaPIJ0Xc3a")) {
                is FirebaseResult.Success -> {
                    textView.text = routeWaypoints.data.wayPoints.toString()
                }
            }
        }
    }

    fun setRouteDuration(textView: TextView) {
        textView.text = ""
        viewModelScope.launch {
            when (val routeDuration = routeRepository.getRoute("83bAuunZzXwaPIJ0Xc3a")) {
                is FirebaseResult.Success -> {
                    textView.text = routeDuration.data.duration.toString()
                }
            }
        }
    }

    fun setRouteDistance(textView: TextView) {
        textView.text = ""
        viewModelScope.launch {
            when (val routeDistance = routeRepository.getRoute("83bAuunZzXwaPIJ0Xc3a")) {
                is FirebaseResult.Success -> {
                    textView.text = routeDistance.data.distance.toString()
                }
            }
        }
    }
}
