package com.pocketdev.advance_erp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pocketdev.advance_erp.data.entity.SavedSuggestion

@Dao
interface SuggestionDao {
    @Query(
        """
        SELECT * FROM saved_suggestions
        WHERE fieldType = :type AND (:q = '' OR valueNormalized LIKE '%' || :q || '%')
        ORDER BY CASE WHEN valueNormalized LIKE :q || '%' THEN 0 ELSE 1 END, useCount DESC, lastUsedAt DESC
        LIMIT 8
        """
    )
    suspend fun search(type: String, q: String): List<SavedSuggestion>

    @Query("SELECT * FROM saved_suggestions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): SavedSuggestion?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: SavedSuggestion)

    @Query("DELETE FROM saved_suggestions WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT COUNT(*) FROM saved_suggestions")
    suspend fun count(): Int

    // Seeds from data the user already entered
    @Query("SELECT DISTINCT color FROM product_variants WHERE color IS NOT NULL AND TRIM(color) != ''")
    suspend fun existingColors(): List<String>

    @Query("SELECT DISTINCT size FROM product_variants WHERE size IS NOT NULL AND TRIM(size) != ''")
    suspend fun existingSizes(): List<String>

    @Query("SELECT DISTINCT unit FROM products WHERE TRIM(unit) != ''")
    suspend fun existingUnits(): List<String>

    @Query("SELECT DISTINCT name FROM customers WHERE TRIM(name) != ''")
    suspend fun existingCustomerNames(): List<String>

    @Query("SELECT DISTINCT companyName FROM suppliers WHERE TRIM(companyName) != ''")
    suspend fun existingSupplierNames(): List<String>

    @Query("SELECT DISTINCT contactPerson FROM suppliers WHERE contactPerson IS NOT NULL AND TRIM(contactPerson) != ''")
    suspend fun existingContactPersons(): List<String>
}
