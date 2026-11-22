package com.pocketdev.advance_erp.domain.repository

import com.pocketdev.advance_erp.data.entity.AccountType
import com.pocketdev.advance_erp.data.entity.ChartOfAccount
import com.pocketdev.advance_erp.data.entity.JournalEntry
import com.pocketdev.advance_erp.data.entity.JournalLine
import com.pocketdev.advance_erp.data.entity.LiquidAccountKind
import com.pocketdev.advance_erp.data.entity.NormalBalance
import com.pocketdev.advance_erp.data.entity.TrialBalanceItem
import kotlinx.coroutines.flow.Flow

data class JournalLineDraft(
    val accountId: String,
    val isDebit: Boolean,
    val amountMinor: Long,
    val description: String? = null
)

interface AccountingRepository {

    fun getAllAccounts(): Flow<List<ChartOfAccount>>
    suspend fun getAccountById(id: String): ChartOfAccount?
    suspend fun createAccount(
        code: String,
        nameEn: String,
        nameBn: String,
        type: AccountType,
        normalBalance: NormalBalance,
        description: String?
    ): String

    /** Cash / bank / mobile-banking accounts (active and inactive). */
    fun getLiquidAccounts(): Flow<List<ChartOfAccount>>

    /**
     * Creates a money account (cash drawer, bank account or MFS wallet) with an auto-generated
     * code. If [openingBalanceMinor] > 0, an opening-balance journal is posted
     * (Dr new account / Cr Owner's Capital).
     */
    suspend fun createLiquidAccount(
        kind: LiquidAccountKind,
        nameEn: String,
        nameBn: String,
        institutionName: String?,
        branchName: String?,
        accountNumber: String?,
        description: String?,
        openingBalanceMinor: Long
    ): String

    /** Updates editable fields (names, description, bank details, active flag). Code & type are immutable. */
    suspend fun updateAccount(account: ChartOfAccount)

    /** Deletes a bank/MFS/secondary cash account if and only if its current balance is zero. */
    suspend fun deleteLiquidAccount(accountId: String)

    /** Returns the next free 4-digit account code for a given account type / money kind. */
    suspend fun suggestNextAccountCode(type: AccountType, kind: LiquidAccountKind? = null): String

    fun getAllJournalEntries(): Flow<List<JournalEntry>>
    fun getLinesForEntry(entryId: String): Flow<List<JournalLine>>
    suspend fun postJournalEntry(
        entryDate: Long,
        descEn: String,
        descBn: String,
        referenceType: String,
        referenceId: String?,
        lines: List<JournalLineDraft>
    ): String

    suspend fun reverseJournalEntry(entryId: String): String
    suspend fun getTrialBalance(): List<TrialBalanceItem>
}
