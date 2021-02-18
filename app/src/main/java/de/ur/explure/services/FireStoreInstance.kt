package de.ur.explure.services

import com.google.firebase.firestore.FirebaseFirestore
import de.ur.explure.config.FirebaseCollections

/**
 * Provider holding Firestore instances and the corresponding collections
 *
 * @param fireStore Singleton Firestore instance
 */

class FireStoreInstance(fireStore: FirebaseFirestore) {
    val userCollection = fireStore.collection(FirebaseCollections.USER_COLLECTION_NAME)
    val ratingCollection = fireStore.collection(FirebaseCollections.RATING_COLLECTION_NAME)
}
