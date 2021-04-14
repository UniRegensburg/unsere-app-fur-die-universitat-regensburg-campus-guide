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
import de.ur.explure.navigation.MainAppRouter
import de.ur.explure.repository.route.RouteRepositoryImpl
import de.ur.explure.utils.FirebaseResult
import kotlinx.coroutines.launch
import timber.log.Timber

@Suppress("UnnecessaryParentheses")
class WordSearchViewModel(
    private val routeRepo: RouteRepositoryImpl,
    private val mainAppRouter: MainAppRouter
) : ViewModel() {

    var searchedRoutes: MutableLiveData<List<Route>> = MutableLiveData()

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
                searchedRoutes.postValue(emptyList())
            } else {
                when (val routeLists = routeRepo.getRoutes(resultIDs, true)) {
                    is FirebaseResult.Success -> {
                        searchedRoutes.postValue(routeLists.data)
                        Timber.d(routeLists.toString())
                    }
                }
            }
        }
    }

    fun showRouteDetails(routeId: String) {
        mainAppRouter.navigateToRouteDetailsFromQuery(routeId)
    }
}
