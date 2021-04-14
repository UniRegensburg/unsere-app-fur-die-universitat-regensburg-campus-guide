package de.ur.explure.services

import com.algolia.search.client.ClientSearch
import com.algolia.search.model.APIKey
import com.algolia.search.model.ApplicationID
import com.algolia.search.model.IndexName
import kotlinx.serialization.json.json

class AlgoliaService {

    private var applicationID: ApplicationID = ApplicationID(APPLICATION_ID)
    private var apiKey: APIKey = APIKey(API_KEY)
    private var indexName: IndexName = IndexName(INDEX_NAME)
    private var clientSearch: ClientSearch = ClientSearch(applicationID, apiKey)

    /**
     * Adds a route to the algolia data set to include route in search results
     *
     * @param routeID [String] of the route id
     * @param routeTitle [String] of the route's title
     * @param routeDescr [String] of the route's description
     */

    suspend fun addRouteInfoToAlgolia(routeID: String, routeTitle: String, routeDescr: String) {
        val index = clientSearch.initIndex(indexName)
        val json = listOf(
            json {
                OBJECT_ID_FIELD to routeID
                ROUTE_ID_FIELD to routeID
                TITLE_FIELD to routeTitle
                DESCRIPTION_FIELD to routeDescr
            }
        )
        index.saveObjects(json)
    }

    internal companion object {
        private const val APPLICATION_ID = "CRDAJVEWKR"
        private const val API_KEY = "19805d168da9d1f8b1f5ffb70283a0c2"
        private const val INDEX_NAME = "listOfRoutes"

        private const val OBJECT_ID_FIELD = "objectID"
        private const val ROUTE_ID_FIELD = "routeID"
        private const val TITLE_FIELD = "title"
        private const val DESCRIPTION_FIELD = "description"
    }
}
