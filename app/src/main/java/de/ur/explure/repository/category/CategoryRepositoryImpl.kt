package de.ur.explure.repository.category

import de.ur.explure.extensions.await
import de.ur.explure.model.category.Category
import de.ur.explure.services.FireStoreInstance
import de.ur.explure.services.FirebaseAuthService
import de.ur.explure.utils.FirebaseResult

@Suppress("TooGenericExceptionCaught")
class CategoryRepositoryImpl(
    private val authService: FirebaseAuthService,
    private val fireStore: FireStoreInstance
) : CategoryRepository {

    override suspend fun getAllCategories(): FirebaseResult<List<Category>> {
        return try {
            when (val categoryCall = fireStore.categoryCollection.get().await()) {
                is FirebaseResult.Success -> {
                    val categories = categoryCall.data.toObjects(Category::class.java)
                    FirebaseResult.Success(categories)
                }
                is FirebaseResult.Error -> categoryCall
                is FirebaseResult.Canceled -> categoryCall
            }
        } catch (e: Exception) {
            FirebaseResult.Error(e)
        }
    }
}
