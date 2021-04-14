package de.ur.explure.repository.storage

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import de.ur.explure.extensions.await
import de.ur.explure.utils.FirebaseResult
import timber.log.Timber
import java.lang.Exception

@Suppress("TooGenericExceptionCaught")
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

    suspend fun getDownloadURL(storageURL: String): FirebaseResult<Uri> {
        return try {
            return firebaseStorage.getReferenceFromUrl(storageURL).downloadUrl.await()
        } catch (e: Exception) {
            FirebaseResult.Error(e)
        }
    }
}
