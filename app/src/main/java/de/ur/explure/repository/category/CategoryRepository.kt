package de.ur.explure.repository.category

import de.ur.explure.model.category.Category
import de.ur.explure.utils.FirebaseResult

interface CategoryRepository {

    suspend fun getAllCategories(): FirebaseResult<List<Category>>

}