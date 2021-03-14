package de.ur.explure.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.ur.explure.model.category.Category
import de.ur.explure.navigation.MainAppRouter
import de.ur.explure.repository.category.CategoryRepositoryImpl
import de.ur.explure.utils.FirebaseResult
import kotlinx.coroutines.launch
import timber.log.Timber

class CreateRouteViewModel(
    private val appRouter: MainAppRouter,
    private val categoryRepo: CategoryRepositoryImpl
) : ViewModel() {

    val categories: MutableLiveData<List<Category>> = MutableLiveData()

    fun getCategories() {
        viewModelScope.launch {
            when (val categoryCall = categoryRepo.getAllCategories()) {
                is FirebaseResult.Success -> categories.postValue(categoryCall.data)
                else -> Timber.d("Failed to get Categories")
            }
        }
    }

}