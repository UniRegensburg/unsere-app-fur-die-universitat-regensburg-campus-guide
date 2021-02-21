package de.ur.explure.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.GeoPoint
import de.ur.explure.SearchListAdapter
import de.ur.explure.SearchListItem
import de.ur.explure.model.route.RouteDTO
import de.ur.explure.model.waypoint.WayPointDTO
import de.ur.explure.navigation.StateAppRouter
import de.ur.explure.repository.route.RouteRepositoryImpl
import de.ur.explure.services.FireStoreInstance
import de.ur.explure.services.FirebaseAuthService
import de.ur.explure.views.WordSearchFragment
import kotlinx.coroutines.launch

class WordSearchViewModel(
        private val routeRepo: RouteRepositoryImpl
) : ViewModel() {

    var routelist: ArrayList<SearchListItem?> = ArrayList(mutableListOf())
    //val adapter: SearchListAdapter = SearchListAdapter(WordSearchFragment, routelist)


}
