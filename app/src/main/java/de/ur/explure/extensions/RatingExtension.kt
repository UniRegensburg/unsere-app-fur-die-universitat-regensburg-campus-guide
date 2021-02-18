package de.ur.explure.extensions

import de.ur.explure.model.rating.Rating
import kotlin.math.roundToInt

fun List<Rating>.getAverageRating(): Int {
    val ratingList = mutableListOf<Int>()
    this.forEach {
        ratingList.add(it.ratingValue)
    }
    return ratingList.average().roundToInt()
}
