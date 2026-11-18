package com.pocketdev.advance_erp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class AccountType(val titleEn: String, val titleBn: String) {
    ASSET("Asset", "সম্পদ"),
    LIABILITY("Liability", "দায়"),
    EQUITY("Equity", "মালিকানাস্বত্ব"),
    REVENUE("Revenue", "আয়"),
    EXPENSE("Expense", "ব্যয়")
}

enum class NormalBalance {
    DEBIT, CREDIT
}

/**
 * Classifies "money" (liquid) accounts that can receive or pay funds: cash drawers,
 * bank accounts and mobile financial services (bKash, Nagad, Rocket ...).
 * Stored in [ChartOfAccount.accountSubType]; null means a regular ledger account.
 */
enum class LiquidAccountKind(val titleEn: String, val titleBn: String) {
    CASH("Cash", "নগদ"),
    BANK("Bank", "ব্যাংক"),
    MOBILE_BANKING("Mobile Banking", "মোবাইল ব্যাংকিং");

    companion object {
        fun from(value: String?): LiquidAccountKind? = entries.firstOrNull { it.name == value }
    }
}

@Entity(
    tableName = "chart_of_accounts",
    indices = [
        Index(value = ["accountCode"], unique = true)
    ]
)
data class ChartOfAccount(
    @PrimaryKey
    val id: String,
    val accountCode: String,
    val nameEn: String,
    val nameBn: String,
    val accountType: String,
    val normalBalance: String,
    val description: String? = null,
    val isActive: Boolean = true,
    /** CASH / BANK / MOBILE_BANKING for money accounts, null for regular ledger accounts. */
    val accountSubType: String? = null,
    /** Bank or MFS provider name, e.g. "Dutch-Bangla Bank PLC" or "bKash". */
    val institutionName: String? = null,
    val branchName: String? = null,
    /** Bank account number or MFS wallet number. */
    val accountNumber: String? = null
) {
    val liquidKind: LiquidAccountKind? get() = LiquidAccountKind.from(accountSubType)
    val isLiquid: Boolean get() = liquidKind != null
}

@Entity(
    tableName = "journal_entries",
    indices = [
        Index(value = ["entryNumber"], unique = true),
        Index("referenceType"),
        Index("referenceId")
    ]
)
data class JournalEntry(
    @PrimaryKey
    val id: String,
    val entryNumber: String,
    val entryDate: Long = System.currentTimeMillis(),
    val descriptionEn: String,
    val descriptionBn: String,
    val referenceType: String,
    val referenceId: String? = null,
    val totalDebitMinor: Long,
    val totalCreditMinor: Long,
    val isPosted: Boolean = true,
    val isReversed: Boolean = false,
    val reversalOfEntryId: String? = null,
    val reversalEntryId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "journal_lines",
    foreignKeys = [
        ForeignKey(
            entity = JournalEntry::class,
            parentColumns = ["id"],
            childColumns = ["journalEntryId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ChartOfAccount::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("journalEntryId"),
        Index("accountId")
    ]
)
data class JournalLine(
    @PrimaryKey
    val id: String,
    val journalEntryId: String,
    val accountId: String,
    val accountCode: String,
    val accountName: String,
    val isDebit: Boolean,
    val amountMinor: Long,
    val description: String? = null
)

data class TrialBalanceItem(
    val accountId: String,
    val accountCode: String,
    val accountNameEn: String,
    val accountNameBn: String,
    val accountType: String,
    val normalBalance: String,
    val totalDebitMinor: Long,
    val totalCreditMinor: Long,
    val netDebitMinor: Long,
    val netCreditMinor: Long
)
