package de.ur.explure.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.algolia.search.client.ClientSearch
import com.algolia.search.model.APIKey
import com.algolia.search.model.ApplicationID
import com.algolia.search.model.IndexName
import com.algolia.search.model.ObjectID
import de.ur.explure.model.route.Route
import de.ur.explure.repository.route.RouteRepositoryImpl
import de.ur.explure.utils.FirebaseResult
import kotlinx.coroutines.launch
import kotlinx.serialization.json.json

class WordSearchViewModel(
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
    }

    /*fun setupAlgolia() {
        viewModelScope.launch {
            val applicationID = ApplicationID("CRDAJVEWKR")
            val apiKey = APIKey("19805d168da9d1f8b1f5ffb70283a0c2")
            val engine = Netty.create(){}

            val client = ClientSearch(applicationID, apiKey)
            val indexName = IndexName("listOfRoutes")
            val index = client.initIndex(indexName)

            //val newRoute = routeRepo.getRoute("kXvvpB6ukGQtiafDTMxq", true)
            //val newRouteID = newRoute.getValue


            val json = listOf(

                json {
                    "objectID" to ObjectID("kXvvpB6ukGQtiafDTMxq")
                    "title" to "Rundgang Fakultät Sport"
                    "description" to "Rundgang Fakultät Sport"
                },
                json {
                    "objectID" to ObjectID("QZLgj7nsSAWFHg54dqzG")
                    "title" to "Persönlicher Campus Rundgang"
                    "description" to "Persönlicher Campus Rundgang"
                }
            )

            index.saveObjects(json)
        }
    }*/
}
