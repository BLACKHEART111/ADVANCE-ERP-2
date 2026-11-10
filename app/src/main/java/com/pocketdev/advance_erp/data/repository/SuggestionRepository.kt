package com.pocketdev.advance_erp.data.repository

import com.pocketdev.advance_erp.data.dao.SuggestionDao
import com.pocketdev.advance_erp.data.entity.SavedSuggestion
import com.pocketdev.advance_erp.data.entity.SuggestionType
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SuggestionRepository @Inject constructor(private val dao: SuggestionDao) {

    private fun norm(v: String) = v.trim().lowercase(Locale.ROOT)

    suspend fun suggest(type: String, query: String): List<SavedSuggestion> =
        dao.search(type, norm(query))

    suspend fun remember(type: String, value: String) {
        val clean = value.trim()
        if (clean.length < 2) return
        val n = norm(clean)
        val id = "$type|$n"
        val existing = dao.getById(id)
        dao.upsert(
            SavedSuggestion(
                id = id, fieldType = type, value = clean, valueNormalized = n,
                useCount = (existing?.useCount ?: 0) + 1, lastUsedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun forget(id: String) = dao.delete(id)

    /** One-time seeding from data already in the app so suggestions work immediately. */
    suspend fun seedIfEmpty() {
        if (dao.count() > 0) return
        suspend fun seed(type: String, values: List<String>) = values.forEach { remember(type, it) }
        seed(SuggestionType.COLOR, dao.existingColors())
        seed(SuggestionType.SIZE, dao.existingSizes())
        seed(SuggestionType.UNIT, dao.existingUnits())
        seed(SuggestionType.CUSTOMER_NAME, dao.existingCustomerNames())
        seed(SuggestionType.SUPPLIER_NAME, dao.existingSupplierNames())
        seed(SuggestionType.CONTACT_PERSON, dao.existingContactPersons())
    }
}
