package de.ur.explure.model.rating

import android.os.Parcelable
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date
import kotlinx.parcelize.Parcelize

@Parcelize
data class Rating(
    @DocumentId
    val id: String = "",
    val ratingValue: Int = 0,
    @ServerTimestamp
    val createdAt: Date = Date(),
    val routeId: String = "",
    val authorId: String = "",
) : Parcelable
