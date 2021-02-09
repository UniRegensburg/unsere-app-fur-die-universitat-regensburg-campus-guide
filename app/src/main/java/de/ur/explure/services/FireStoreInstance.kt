package de.ur.explure.services

import com.google.firebase.firestore.FirebaseFirestore
import de.ur.explure.config.FirebaseCollections

class FireStoreInstance(fireStore: FirebaseFirestore) {
    val userCollection = fireStore.collection(FirebaseCollections.USER_COLLECTION_NAME)
}
