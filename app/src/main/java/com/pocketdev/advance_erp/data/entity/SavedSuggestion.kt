package com.pocketdev.advance_erp.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** A value the user typed once (e.g. color "Black") that is suggested again when typing its first letters. */
@Entity(
    tableName = "saved_suggestions",
    indices = [Index(value = ["fieldType", "valueNormalized"], unique = true)]
)
data class SavedSuggestion(
    @PrimaryKey val id: String,
    val fieldType: String,
    val value: String,
    val valueNormalized: String,
    val useCount: Int = 1,
    val lastUsedAt: Long = System.currentTimeMillis()
)

object SuggestionType {
    const val COLOR = "COLOR"
    const val SIZE = "SIZE"
    const val UNIT = "UNIT"
    const val CUSTOMER_NAME = "CUSTOMER_NAME"
    const val SUPPLIER_NAME = "SUPPLIER_NAME"
    const val CONTACT_PERSON = "CONTACT_PERSON"
    const val ADDRESS = "ADDRESS"
    const val COUNTRY = "COUNTRY"
}
