package de.ur.explure.model.category

import com.google.firebase.firestore.DocumentId

/**
 * Data class representing a route category
 *
 * @property id [String] of the Document ID in FireStore
 * @property name [String] with the category's name
 * @property color [String] with the color code of the category (Hexa: #FFFFFF)
 * @property iconResource [String] with the storage location of the category's icon
 */

data class Category(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val color: String = "",
    val iconResource: String = ""
)
