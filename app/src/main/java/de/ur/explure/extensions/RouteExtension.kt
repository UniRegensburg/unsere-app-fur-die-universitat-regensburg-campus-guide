package de.ur.explure.extensions

import androidx.lifecycle.MutableLiveData
import de.ur.explure.model.route.Route

fun MutableLiveData<MutableList<Route>>.appendRoutes(data: List<Route>) {
    val currentList = this.value
    if (currentList.isNullOrEmpty()) {
        this.postValue(data.toMutableList())
    } else {
        currentList.addAll(data)
        this.postValue(currentList)
    }
}