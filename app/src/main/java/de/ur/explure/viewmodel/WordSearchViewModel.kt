package de.ur.explure.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.ur.explure.model.route.Route
import de.ur.explure.repository.route.RouteRepositoryImpl
import de.ur.explure.utils.FirebaseResult
import kotlinx.coroutines.launch

class WordSearchViewModel(
        private val routeRepo: RouteRepositoryImpl
) : ViewModel() {

    var searchedRoutes: MutableLiveData<List<Route>> = MutableLiveData()

    fun getSearchedRoutes() {
        viewModelScope.launch {

            val routeList = listOf("kXvvpB6ukGQtiafDTMxq", "QZLgj7nsSAWFHg54dqzG", "83bAuunZzXwaPIJ0Xc3a")
            val routeLists = routeRepo.getRoutes(routeList, true)
            when (routeLists) {
                is FirebaseResult.Success -> {
                    searchedRoutes.postValue(routeLists.data)
                }
            }
            Log.d("Koller1", searchedRoutes.toString())
        }

    }


}
