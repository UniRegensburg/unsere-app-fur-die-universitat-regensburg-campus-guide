package de.ur.explure.repository.storage

import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import timber.log.Timber
import java.lang.Exception

class StorageRepositoryImpl(
    private val firebaseStorage: FirebaseStorage
) {

    fun getStorageRefForURL(storageURL: String): StorageReference? {
        return try {
            firebaseStorage.getReferenceFromUrl(storageURL)
        } catch (e: Exception) {
            Timber.d("Failed to create storage ref with $e")
            null
        }
    }

}