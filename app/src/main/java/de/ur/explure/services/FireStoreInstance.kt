package de.ur.explure.services

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.WriteBatch
import de.ur.explure.config.FirebaseCollections

/**
 * Provider holding Firestore instances and the corresponding collections
 *
 * @param fireStore Singleton Firestore instance
 */

class FireStoreInstance(private val fireStore: FirebaseFirestore) {
    val userCollection = fireStore.collection(FirebaseCollections.USER_COLLECTION_NAME)
    val routeCollection = fireStore.collection(FirebaseCollections.ROUTE_COLLECTION_NAME)
    val ratingCollection = fireStore.collection(FirebaseCollections.RATING_COLLECTION_NAME)
    /**
     * Returns a write batch for firestore. Used to execute multiple write operation at the same time.
     *
     * @return [WriteBatch] object
     */

    fun getWriteBatch(): WriteBatch {
        return fireStore.batch()
    }
}
