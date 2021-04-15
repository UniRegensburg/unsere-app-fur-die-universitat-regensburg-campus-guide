package de.ur.explure.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.ur.explure.model.rating.RatingDTO
import de.ur.explure.repository.rating.RatingRepositoryImpl
import de.ur.explure.utils.FirebaseResult
import kotlinx.coroutines.launch

class RatingViewModel(private val ratingRepository: RatingRepositoryImpl) : ViewModel() {

    private val mutableErrorMessage: MutableLiveData<Boolean> = MutableLiveData()
    val errorMessage: LiveData<Boolean> = mutableErrorMessage

    fun setRating(rating: Int, routeId: String, callback: () -> Unit) {
        val ratingDTO = RatingDTO(rating, routeId)
        viewModelScope.launch {
            if (ratingRepository.addRatingToFireStore(ratingDTO) is FirebaseResult.Success) {
                callback()
            } else {
                mutableErrorMessage.postValue(true)
            }
        }
    }
}
