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

    /** Fails cleanly if [accountId] is a liquid account with balance below [requiredAmountMinor]. */
    suspend fun requireSufficientLiquidBalance(accountId: String, requiredAmountMinor: Long)

    /** Posts owner or investor capital injection (`Dr Liquid Account / Cr Selected Equity Account`). */
    suspend fun depositCapitalToLiquidAccount(
        accountId: String,
        amountMinor: Long,
        entryDate: Long = System.currentTimeMillis(),
        notes: String? = null,
        equityAccountId: String = "coa-3010"
    ): String

    /** Creates a dedicated 3000-series CREDIT Equity account for an additional investor (`3030`, `3031`, ...). */
    suspend fun createInvestorEquityAccount(
        investorNameEn: String,
        investorNameBn: String = "",
        notes: String? = null
    ): String

    /** Posts owner/investor capital withdrawal or drawings (`Dr Equity Account / Cr Liquid Account`) after liquid balance check. */
    suspend fun withdrawCapitalFromLiquidAccount(
        liquidAccountId: String,
        equityAccountId: String = "coa-3010",
        amountMinor: Long,
        entryDate: Long = System.currentTimeMillis(),
        notes: String? = null
    ): String

    /**
     * Distributes earned net profit from `3020 Retained Earnings` to an owner/investor:
     * - If [reinvestToCapital] == false: `Dr 3020 Retained Earnings / Cr [paymentAccountId]` (checks liquid balance & distributable profit).
     * - If [reinvestToCapital] == true: `Dr 3020 Retained Earnings / Cr [investorEquityAccountId]` (checks distributable profit).
     */
    suspend fun distributeProfitToInvestor(
        investorEquityAccountId: String,
        amountMinor: Long,
        reinvestToCapital: Boolean = false,
        paymentAccountId: String? = null,
        entryDate: Long = System.currentTimeMillis(),
        notes: String? = null
    ): String

    /** Pays down Accrued Customs & Freight (`Dr 2020 / Cr Payment Account`). */
    suspend fun payAccruedCustomsAndFreight(
        paymentAccountId: String,
        amountMinor: Long,
        entryDate: Long = System.currentTimeMillis(),
        notes: String? = null
    ): String
}
