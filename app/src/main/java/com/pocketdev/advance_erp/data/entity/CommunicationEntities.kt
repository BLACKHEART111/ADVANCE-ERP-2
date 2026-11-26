package com.pocketdev.advance_erp.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "communication_settings")
data class CommunicationSettings(@PrimaryKey val id: Int = 1,
    val autoGenerate: Boolean = true, val includePdf: Boolean = true,
    val includePreviousDue: Boolean = true, val includeInvoiceAmount: Boolean = true,
    val includePaymentDetails: Boolean = true, val includeInvoiceDue: Boolean = true,
    val includeOutstanding: Boolean = true, val includeMethodReference: Boolean = true,
    val includeInstructions: Boolean = false, val includeReminder: Boolean = false,
    val includeSignature: Boolean = true, val previewBeforeSharing: Boolean = true,
    val defaultLanguage: String = "bn", val preferredApp: String = "ASK",
    val paymentInstructions: String = "", val bankDetails: String = "")
@Entity(tableName = "communication_templates", primaryKeys = ["module", "status", "language"])
data class CommunicationTemplate(val module: String, val status: String, val language: String, val body: String)
@Entity(tableName = "communication_party_preferences")
data class CommunicationPartyPreference(@PrimaryKey val partyKey: String, val language: String)
@Entity(tableName = "communication_snapshots", primaryKeys = ["module", "documentId"])
data class CommunicationSnapshot(val module: String, val documentId: String, val number: String,
    val partyId: String, val createdAt: Long, val originalAmountMinor: Long,
    val previousBalanceMinor: Long, val allocatedPaymentMinor: Long, val invoiceDueMinor: Long,
    val closingBalanceMinor: Long, val payload: String = "")
@Entity(
    tableName = "communication_history",
    indices = [
        Index(value = ["createdAt"]),
        Index(value = ["module"])
    ]
)
data class CommunicationHistory(@PrimaryKey val id: String, val module: String, val documentId: String,
    val documentNumber: String, val partyId: String, val partyName: String, val phone: String,
    val createdAt: Long, val message: String, val attachmentReference: String?,
    val language: String, val application: String, val event: String,
    val rangeFrom: Long?, val rangeTo: Long?)
