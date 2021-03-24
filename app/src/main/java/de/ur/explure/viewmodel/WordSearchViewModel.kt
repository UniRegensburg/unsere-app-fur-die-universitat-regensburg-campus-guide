package de.ur.explure.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.algolia.search.client.ClientSearch
import com.algolia.search.model.APIKey
import com.algolia.search.model.ApplicationID
import com.algolia.search.model.IndexName
import com.algolia.search.model.search.Query
import de.ur.explure.model.route.Route
import de.ur.explure.repository.route.RouteRepositoryImpl
import de.ur.explure.utils.FirebaseResult
import kotlinx.coroutines.launch
import timber.log.Timber

@Suppress("UnnecessaryParentheses")
class WordSearchViewModel(
    private val routeRepo: RouteRepositoryImpl
) : ViewModel() {

    var searchedRoutes: MutableLiveData<List<Route>> = MutableLiveData()
    var noRoutes = MutableLiveData<Boolean?>(false)

    fun getSearchedRoutes(message: String) {
        viewModelScope.launch {

            val applicationID = ApplicationID("CRDAJVEWKR")
            val apiKey = APIKey("7155cf8ed1935275cf3cc60ed4673cd4")
            val client = ClientSearch(applicationID, apiKey)
            val indexName = IndexName("listOfRoutes")
            val index = client.initIndex(indexName)
            val query = Query(message)

            val resultQuery = index.search(query).hits

            val resultIDs = mutableListOf<String>()

            for (i in resultQuery.indices) {
                val resultID = resultQuery.get(i).json.get("objectID").toString()
                val trimResultID = resultID.removePrefix(("\"")).removeSuffix(("\""))
                resultIDs.add(i, trimResultID)
            }

            if (resultIDs.isEmpty()) {
                noRoutes.postValue(true)
            }

            when ( val routeLists = routeRepo.getRoutes(resultIDs, true)) {
                is FirebaseResult.Success -> {
                    searchedRoutes.postValue(routeLists.data)
                    Timber.d(routeLists.toString())
                }
            }
        }
    }

    /*fun setupAlgolia() {
        viewModelScope.launch {
            val applicationID = ApplicationID("CRDAJVEWKR")
            val apiKey = APIKey("19805d168da9d1f8b1f5ffb70283a0c2")

            val client = ClientSearch(applicationID, apiKey)
            val indexName = IndexName("listOfRoutes")

            val index = client.initIndex(indexName)

            addRouteInfo("kXvvpB6ukGQtiafDTMxq", index)
            addRouteInfo("QZLgj7nsSAWFHg54dqzG", index)
            addRouteInfo("83bAuunZzXwaPIJ0Xc3a", index)
            addRouteInfo("vkrSFtip95ya9zRsXqCc", index)

        }
    }

    fun addRouteInfo (routeID: String, index: Index) {
        viewModelScope.launch {
            try {
                when (val newRoute = routeRepo.getRoute(routeID, true)) {
                    is FirebaseResult.Success -> {
                        val newRouteTitle = newRoute.data.title
                        val newRouteDescription = newRoute.data.description
                        val json = listOf(

                            json {
                                "objectID" to routeID
                                "routeID" to routeID
                                "title" to newRouteTitle
                                "description" to newRouteDescription
                            }
                        )
                        index.saveObjects(json)
                    }
                    is FirebaseResult.Error -> FirebaseResult.Error(newRoute.exception)
                    is FirebaseResult.Canceled -> FirebaseResult.Canceled(newRoute.exception)
                }
            } catch (exception: Exception) {
                FirebaseResult.Error(exception)
            }
        }
    }*/
}

/*
private val routeRepo: RouteRepositoryImpl
) : ViewModel() {

    var searchedRoutes: MutableLiveData<List<Route>> = MutableLiveData()

    fun getSearchedRoutes(message: String) {
        viewModelScope.launch {

            val routeLists = routeRepo.getSearchedRoutes(message)
            // val routeList = listOf("kXvvpB6ukGQtiafDTMxq", "QZLgj7nsSAWFHg54dqzG", "83bAuunZzXwaPIJ0Xc3a")
            // val routeLists = routeRepo.getRoutes(routeList, true)
            when (routeLists) {
                is FirebaseResult.Success -> {
                    searchedRoutes.postValue(routeLists.data)
                }
            }
        }
    }*/
