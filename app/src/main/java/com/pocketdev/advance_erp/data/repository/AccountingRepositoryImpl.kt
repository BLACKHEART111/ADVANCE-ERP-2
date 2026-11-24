package com.pocketdev.advance_erp.data.repository

import androidx.room.withTransaction
import com.pocketdev.advance_erp.core.database.AdvanceErpDatabase
import com.pocketdev.advance_erp.core.engine.ErpAccountingGuard
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.data.dao.AccountingDao
import com.pocketdev.advance_erp.data.entity.AccountType
import com.pocketdev.advance_erp.data.entity.ChartOfAccount
import com.pocketdev.advance_erp.data.entity.JournalEntry
import com.pocketdev.advance_erp.data.entity.JournalLine
import com.pocketdev.advance_erp.data.entity.LiquidAccountKind
import com.pocketdev.advance_erp.data.entity.NormalBalance
import com.pocketdev.advance_erp.data.entity.TrialBalanceItem
import com.pocketdev.advance_erp.domain.repository.AccountingRepository
import com.pocketdev.advance_erp.domain.repository.JournalLineDraft
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountingRepositoryImpl @Inject constructor(
    private val database: AdvanceErpDatabase,
    private val accountingDao: AccountingDao
) : AccountingRepository {

    override fun getAllAccounts(): Flow<List<ChartOfAccount>> = accountingDao.getAllAccounts()

    override suspend fun getAccountById(id: String): ChartOfAccount? = accountingDao.getAccountById(id)

    override suspend fun createAccount(
        code: String,
        nameEn: String,
        nameBn: String,
        type: AccountType,
        normalBalance: NormalBalance,
        description: String?
    ): String {
        val cleanCode = code.trim()
        require(cleanCode.isNotBlank()) { "Account code is required" }
        require(cleanCode.all { it.isDigit() }) { "Account code must contain digits only" }
        require(nameEn.isNotBlank()) { "Account name is required" }
        val existing = accountingDao.getAccountByCode(cleanCode)
        require(existing == null) { "Account code already exists: $cleanCode" }

        val id = "coa-$cleanCode"
        require(accountingDao.getAccountById(id) == null) { "Account already exists: $id" }
        val account = ChartOfAccount(
            id = id,
            accountCode = cleanCode,
            nameEn = nameEn.trim(),
            nameBn = nameBn.trim().ifBlank { nameEn.trim() },
            accountType = type.name,
            normalBalance = normalBalance.name,
            description = description?.trim()?.ifBlank { null },
            isActive = true
        )
        accountingDao.insertAccount(account)
        return id
    }

    override fun getLiquidAccounts(): Flow<List<ChartOfAccount>> = accountingDao.getLiquidAccounts()

    override suspend fun createLiquidAccount(
        kind: LiquidAccountKind,
        nameEn: String,
        nameBn: String,
        institutionName: String?,
        branchName: String?,
        accountNumber: String?,
        description: String?,
        openingBalanceMinor: Long
    ): String {
        require(nameEn.isNotBlank()) { "Account name is required" }
        require(openingBalanceMinor >= 0L) { "Opening balance cannot be negative" }
        if (kind != LiquidAccountKind.CASH) {
            require(!institutionName.isNullOrBlank()) {
                if (kind == LiquidAccountKind.BANK) "Please select a bank" else "Please select a mobile banking provider"
            }
        }
        val cleanNumber = accountNumber?.trim()?.ifBlank { null }
        if (cleanNumber != null) {
            val duplicate = accountingDao.getLiquidAccountsSync().firstOrNull {
                it.accountNumber == cleanNumber && it.institutionName.equals(institutionName, ignoreCase = true)
            }
            require(duplicate == null) { "This account number already exists: ${duplicate?.nameEn}" }
        }

        return database.withTransaction {
            val code = suggestNextAccountCode(AccountType.ASSET, kind)
            val id = "coa-$code"
            accountingDao.insertAccount(
                ChartOfAccount(
                    id = id,
                    accountCode = code,
                    nameEn = nameEn.trim(),
                    nameBn = nameBn.trim().ifBlank { nameEn.trim() },
                    accountType = AccountType.ASSET.name,
                    normalBalance = NormalBalance.DEBIT.name,
                    description = description?.trim()?.ifBlank { null },
                    isActive = true,
                    accountSubType = kind.name,
                    institutionName = institutionName?.trim()?.ifBlank { null },
                    branchName = branchName?.trim()?.ifBlank { null },
                    accountNumber = cleanNumber
                )
            )

            if (openingBalanceMinor > 0L) {
                postJournalEntry(
                    entryDate = System.currentTimeMillis(),
                    descEn = "Opening balance - ${nameEn.trim()}",
                    descBn = "প্রারম্ভিক জের - ${nameBn.trim().ifBlank { nameEn.trim() }}",
                    referenceType = "OPENING_BALANCE",
                    referenceId = id,
                    lines = listOf(
                        JournalLineDraft(id, true, openingBalanceMinor, "Opening balance"),
                        JournalLineDraft(OWNER_CAPITAL_ACCOUNT_ID, false, openingBalanceMinor, "Opening balance - ${nameEn.trim()}")
                    )
                )
            }
            id
        }
    }

    override suspend fun updateAccount(account: ChartOfAccount) {
        val existing = accountingDao.getAccountById(account.id)
            ?: throw IllegalArgumentException("Account not found: ${account.id}")
        require(account.nameEn.isNotBlank()) { "Account name is required" }
        if (!account.isActive && existing.isActive) {
            require(account.id !in PROTECTED_ACCOUNT_IDS) {
                "${existing.nameEn} is a system account and cannot be deactivated"
            }
        }
        // Code, type and normal balance are immutable to keep posted journals consistent.
        accountingDao.updateAccount(
            existing.copy(
                nameEn = account.nameEn.trim(),
                nameBn = account.nameBn.trim().ifBlank { account.nameEn.trim() },
                description = account.description?.trim()?.ifBlank { null },
                isActive = account.isActive,
                institutionName = account.institutionName?.trim()?.ifBlank { null },
                branchName = account.branchName?.trim()?.ifBlank { null },
                accountNumber = account.accountNumber?.trim()?.ifBlank { null }
            )
        )
    }

    override suspend fun deleteLiquidAccount(accountId: String) {
        val existing = accountingDao.getAccountById(accountId)
            ?: throw IllegalArgumentException("Account not found: $accountId")
        require(existing.isLiquid) { "Only Cash / Bank / MFS accounts can be deleted here." }
        require(existing.id != MAIN_CASH_ACCOUNT_ID && existing.id !in PROTECTED_ACCOUNT_IDS) {
            "নগদ তহবিল (${existing.nameEn}) মূল হিসাব হওয়ায় ডিলিট করা যাবে না।"
        }
        val netBalanceMinor = accountingDao.getNetDebitBalanceForAccount(existing.accountCode)
        if (netBalanceMinor != 0L) {
            val absFormatted = com.pocketdev.advance_erp.core.util.Formatters.formatMinorUnit(kotlin.math.abs(netBalanceMinor))
            throw IllegalStateException(
                "এই অ্যাকাউন্টে বর্তমানে ৳$absFormatted ব্যালেন্স রয়েছে। অ্যাকাউন্ট ফাঁকা (৳0.00) না হওয়া পর্যন্ত ডিলিট করা যাবে না।"
            )
        }
        val hasJournalHistory = accountingDao.getLineCountForAccount(existing.id) > 0
        if (!hasJournalHistory) {
            val hardDeleted = runCatching { accountingDao.deleteAccountById(existing.id) }.isSuccess
            if (hardDeleted) return
        }
        accountingDao.updateAccount(existing.copy(isActive = false))
    }

    override suspend fun suggestNextAccountCode(type: AccountType, kind: LiquidAccountKind?): String {
        val used = accountingDao.getAllAccountsSync().map { it.accountCode }.toSet()
        val start = when (kind) {
            LiquidAccountKind.CASH -> 1011
            LiquidAccountKind.BANK -> 1021
            LiquidAccountKind.MOBILE_BANKING -> 1062
            null -> when (type) {
                AccountType.ASSET -> 1300
                AccountType.LIABILITY -> 2040
                AccountType.EQUITY -> 3030
                AccountType.REVENUE -> 4040
                AccountType.EXPENSE -> 5200
            }
        }
        val rangeEnd = (start / 1000) * 1000 + 999
        var candidate = start
        while (candidate <= rangeEnd && candidate.toString() in used) candidate++
        require(candidate <= rangeEnd) { "No free account code left in ${type.titleEn} range" }
        return candidate.toString()
    }

    companion object {
        const val OWNER_CAPITAL_ACCOUNT_ID = "coa-3010"
        const val RETAINED_EARNINGS_ACCOUNT_ID = "coa-3020"
        const val MAIN_CASH_ACCOUNT_ID = "coa-1010"

        /** Accounts referenced directly by posting logic – must stay active (coa-1010 main Cash is also protected). */
        val PROTECTED_ACCOUNT_IDS = setOf(
            "coa-1010", "coa-1210", "coa-1220",
            "coa-2010", "coa-2020", "coa-2030", "coa-3010", "coa-3020",
            "coa-4010", "coa-4020", "coa-4030",
            "coa-5010", "coa-5020", "coa-5030", "coa-5040", "coa-5050", "coa-5090",
            "coa-5110", "coa-5120", "coa-5130", "coa-5140", "coa-5150", "coa-5190"
        )
    }

    override fun getAllJournalEntries(): Flow<List<JournalEntry>> = accountingDao.getAllJournalEntries()

    override fun getLinesForEntry(entryId: String): Flow<List<JournalLine>> = accountingDao.getLinesForEntry(entryId)

    override suspend fun postJournalEntry(
        entryDate: Long,
        descEn: String,
        descBn: String,
        referenceType: String,
        referenceId: String?,
        lines: List<JournalLineDraft>
    ): String {
        require(lines.size >= 2) { "Journal entry must contain at least 2 lines" }
        require(lines.all { it.amountMinor > 0L }) { "All journal line amounts must be strictly positive (> 0)" }
        require(lines.none { it.accountId.isBlank() }) { "Journal lines must specify valid account IDs" }

        val totalDebit = lines.filter { it.isDebit }.sumOf { it.amountMinor }
        val totalCredit = lines.filter { !it.isDebit }.sumOf { it.amountMinor }

        require(totalDebit > 0L) { "Journal debit total must be greater than zero: $totalDebit" }
        require(totalDebit == totalCredit) {
            "Debit ($totalDebit) does not balance with Credit ($totalCredit) in minor units!"
        }

        val entryCount = accountingDao.getEntryCount()
        val entryNumber = "JE-2026-%04d".format(entryCount + 1)
        val entryId = "je-${UUID.randomUUID()}"

        val accountsMap = accountingDao.getAllAccountsSync().associateBy { it.id }

        val journalLines = lines.map { draft ->
            val account = accountsMap[draft.accountId]
                ?: throw IllegalArgumentException("Account not found: ${draft.accountId}")
            JournalLine(
                id = "jl-${UUID.randomUUID()}",
                journalEntryId = entryId,
                accountId = draft.accountId,
                accountCode = account.accountCode,
                accountName = account.nameEn,
                isDebit = draft.isDebit,
                amountMinor = draft.amountMinor,
                description = draft.description
            )
        }

        val entry = JournalEntry(
            id = entryId,
            entryNumber = entryNumber,
            entryDate = entryDate,
            descriptionEn = descEn,
            descriptionBn = descBn,
            referenceType = referenceType,
            referenceId = referenceId,
            totalDebitMinor = totalDebit,
            totalCreditMinor = totalCredit,
            isPosted = true,
            isReversed = false,
            createdAt = System.currentTimeMillis()
        )

        accountingDao.postJournalEntryWithLines(entry, journalLines)
        return entryId
    }

    override suspend fun reverseJournalEntry(entryId: String): String {
        val original = accountingDao.getJournalEntryById(entryId)
            ?: throw IllegalArgumentException("Journal entry not found: $entryId")

        require(!original.isReversed) { "Entry is already reversed: ${original.entryNumber}" }

        val originalLines = accountingDao.getLinesForEntrySync(entryId)
        require(originalLines.isNotEmpty()) { "No journal lines found for entry: $entryId" }

        val entryCount = accountingDao.getEntryCount()
        val reversalNumber = "JE-2026-%04d".format(entryCount + 1)
        val reversalId = "je-${UUID.randomUUID()}"

        // Swap debits and credits
        val reversalLines = originalLines.map { line ->
            JournalLine(
                id = "jl-${UUID.randomUUID()}",
                journalEntryId = reversalId,
                accountId = line.accountId,
                accountCode = line.accountCode,
                accountName = line.accountName,
                isDebit = !line.isDebit, // Swapped!
                amountMinor = line.amountMinor,
                description = "Reversal of ${original.entryNumber}: ${line.description ?: ""}"
            )
        }

        val reversalEntry = JournalEntry(
            id = reversalId,
            entryNumber = reversalNumber,
            entryDate = System.currentTimeMillis(),
            descriptionEn = "Reversal of ${original.entryNumber} - ${original.descriptionEn}",
            descriptionBn = "${original.entryNumber} এর বিপরীত দাখিলা - ${original.descriptionBn}",
            referenceType = "REVERSAL",
            referenceId = original.id,
            totalDebitMinor = original.totalCreditMinor,
            totalCreditMinor = original.totalDebitMinor,
            isPosted = true,
            isReversed = false,
            reversalOfEntryId = original.id,
            createdAt = System.currentTimeMillis()
        )

        accountingDao.postJournalEntryWithLines(reversalEntry, reversalLines)
        accountingDao.markEntryReversed(original.id, reversalId)

        return reversalId
    }

    override suspend fun getTrialBalance(): List<TrialBalanceItem> {
        val accounts = accountingDao.getAllAccountsSync()
        val activeLines = accountingDao.getActivePostedLines()

        val linesByAccount = activeLines.groupBy { it.accountId }

        return accounts.map { account ->
            val accountLines = linesByAccount[account.id] ?: emptyList()
            val totalDebit = accountLines.filter { it.isDebit }.sumOf { it.amountMinor }
            val totalCredit = accountLines.filter { !it.isDebit }.sumOf { it.amountMinor }

            val isDebitNormal = account.normalBalance == NormalBalance.DEBIT.name
            val netDebit: Long
            val netCredit: Long

            if (isDebitNormal) {
                val balance = totalDebit - totalCredit
                if (balance >= 0) {
                    netDebit = balance
                    netCredit = 0L
                } else {
                    netDebit = 0L
                    netCredit = -balance
                }
            } else {
                val balance = totalCredit - totalDebit
                if (balance >= 0) {
                    netCredit = balance
                    netDebit = 0L
                } else {
                    netCredit = 0L
                    netDebit = -balance
                }
            }

            TrialBalanceItem(
                accountId = account.id,
                accountCode = account.accountCode,
                accountNameEn = account.nameEn,
                accountNameBn = account.nameBn,
                accountType = account.accountType,
                normalBalance = account.normalBalance,
                totalDebitMinor = totalDebit,
                totalCreditMinor = totalCredit,
                netDebitMinor = netDebit,
                netCreditMinor = netCredit
            )
        }
    }

    override suspend fun requireSufficientLiquidBalance(accountId: String, requiredAmountMinor: Long) {
        val account = accountingDao.getAccountById(accountId) ?: return
        if (account.liquidKind == null) return
        val currentBal = accountingDao.getNetDebitBalanceForAccount(account.accountCode)
        ErpAccountingGuard.validateSufficientLiquidBalance(account, currentBal, requiredAmountMinor)
    }

    override suspend fun depositCapitalToLiquidAccount(
        accountId: String,
        amountMinor: Long,
        entryDate: Long,
        notes: String?,
        equityAccountId: String
    ): String = database.withTransaction {
        require(amountMinor > 0L) { "জমার পরিমাণ শূন্যের বেশি হতে হবে (Amount must be positive)" }
        val acc = accountingDao.getAccountById(accountId)
            ?: throw IllegalArgumentException("Account not found: $accountId")
        require(acc.liquidKind != null || acc.id == MAIN_CASH_ACCOUNT_ID) {
            "শুধুমাত্র নগদ, ব্যাংক বা মোবাইল ব্যাংকিং হিসাবে তহবিল জমা করা যাবে"
        }
        val equityAcc = accountingDao.getAccountById(equityAccountId)
            ?: throw IllegalStateException("Equity account not found: $equityAccountId")
        require(equityAcc.accountType == AccountType.EQUITY.name && equityAcc.accountCode != "3020") {
            "মূলধন জমার জন্য একটি বৈধ মালিক/ইনভেস্টর মূলধন হিসাব নির্বাচন করুন"
        }
        val cleanNote = notes?.trim()?.takeIf { it.isNotBlank() }
        val suffix = cleanNote?.let { " ($it)" } ?: ""
        val equityNameBn = equityAcc.nameBn.ifBlank { equityAcc.nameEn }
        val accNameBn = acc.nameBn.ifBlank { acc.nameEn }
        postJournalEntry(
            entryDate = entryDate,
            descEn = "Capital Deposit (${equityAcc.nameEn}) to ${acc.nameEn}$suffix",
            descBn = "মূলধন জমা ($equityNameBn) -> $accNameBn$suffix",
            referenceType = "CAPITAL_DEPOSIT",
            referenceId = equityAcc.id,
            lines = listOf(
                JournalLineDraft(acc.id, true, amountMinor, "Capital deposit from ${equityAcc.nameEn} to ${acc.nameEn}"),
                JournalLineDraft(equityAcc.id, false, amountMinor, "Capital contribution - ${equityAcc.nameEn}")
            )
        )
    }

    override suspend fun createInvestorEquityAccount(
        investorNameEn: String,
        investorNameBn: String,
        notes: String?
    ): String = database.withTransaction {
        require(investorNameEn.isNotBlank() || investorNameBn.isNotBlank()) { "ইনভেস্টরের নাম লিখুন" }
        val rawEn = investorNameEn.trim().ifBlank { investorNameBn.trim() }
        val rawBn = investorNameBn.trim().ifBlank { rawEn }
        val finalEn = if (rawEn.contains("capital", ignoreCase = true)) rawEn else "$rawEn - Investor Capital"
        val finalBn = if (rawBn.contains("মূলধন")) rawBn else "$rawBn (ইনভেস্টর মূলধন)"
        val code = suggestNextAccountCode(AccountType.EQUITY, null)
        createAccount(
            code = code,
            nameEn = finalEn,
            nameBn = finalBn,
            type = AccountType.EQUITY,
            normalBalance = NormalBalance.CREDIT,
            description = notes?.trim()?.takeIf { it.isNotBlank() } ?: "Investor Capital Account"
        )
    }

    override suspend fun withdrawCapitalFromLiquidAccount(
        liquidAccountId: String,
        equityAccountId: String,
        amountMinor: Long,
        entryDate: Long,
        notes: String?
    ): String = database.withTransaction {
        require(amountMinor > 0L) { "উত্তোলনের পরিমাণ শূন্যের বেশি হতে হবে" }
        val liquidAcc = accountingDao.getAccountById(liquidAccountId)
            ?: throw IllegalArgumentException("Account not found: $liquidAccountId")
        require(liquidAcc.liquidKind != null || liquidAcc.id == MAIN_CASH_ACCOUNT_ID) {
            "শুধুমাত্র নগদ, ব্যাংক বা মোবাইল ব্যাংকিং হিসাব থেকে উত্তোলন করা যাবে"
        }
        val equityAcc = accountingDao.getAccountById(equityAccountId)
            ?: throw IllegalStateException("Equity account not found: $equityAccountId")
        require(equityAcc.accountType == AccountType.EQUITY.name && equityAcc.accountCode != "3020") {
            "উত্তোলনের জন্য একটি বৈধ মালিক/ইনভেস্টর মূলধন হিসাব নির্বাচন করুন"
        }
        requireSufficientLiquidBalance(liquidAccountId, amountMinor)
        if (equityAcc.id != OWNER_CAPITAL_ACCOUNT_ID) {
            val investorNetCreditMinor = (-accountingDao.getNetDebitBalanceForAccount(equityAcc.accountCode)).coerceAtLeast(0L)
            val eqLabel = equityAcc.nameBn.ifBlank { equityAcc.nameEn }
            require(amountMinor <= investorNetCreditMinor) {
                "$eqLabel-এর বর্তমান মূলধন ব্যালেন্স (৳${Formatters.formatMinorUnit(investorNetCreditMinor)}) এর চেয়ে বেশি উত্তোলন করা যাবে না।"
            }
        }
        val cleanNote = notes?.trim()?.takeIf { it.isNotBlank() }
        val suffix = cleanNote?.let { " ($it)" } ?: ""
        val eqNameBn = equityAcc.nameBn.ifBlank { equityAcc.nameEn }
        val liqNameBn = liquidAcc.nameBn.ifBlank { liquidAcc.nameEn }
        postJournalEntry(
            entryDate = entryDate,
            descEn = "Capital Withdrawal (${equityAcc.nameEn}) from ${liquidAcc.nameEn}$suffix",
            descBn = "মূলধন উত্তোলন ($eqNameBn) <- $liqNameBn$suffix",
            referenceType = "CAPITAL_WITHDRAWAL",
            referenceId = equityAcc.id,
            lines = listOf(
                JournalLineDraft(equityAcc.id, true, amountMinor, "Capital withdrawal / drawings - ${equityAcc.nameEn}"),
                JournalLineDraft(liquidAcc.id, false, amountMinor, "Paid withdrawal from ${liquidAcc.nameEn}")
            )
        )
    }

    override suspend fun distributeProfitToInvestor(
        investorEquityAccountId: String,
        amountMinor: Long,
        reinvestToCapital: Boolean,
        paymentAccountId: String?,
        entryDate: Long,
        notes: String?
    ): String = database.withTransaction {
        require(amountMinor > 0L) { "লভ্যাংশের পরিমাণ শূন্যের বেশি হতে হবে" }
        accountingDao.getAccountById(RETAINED_EARNINGS_ACCOUNT_ID)
            ?: throw IllegalStateException("Retained Earnings account (3020) not found")
        val investorAcc = accountingDao.getAccountById(investorEquityAccountId)
            ?: throw IllegalStateException("Investor equity account not found: $investorEquityAccountId")
        require(investorAcc.accountType == AccountType.EQUITY.name && investorAcc.accountCode != "3020") {
            "লভ্যাংশ বণ্টনের জন্য একটি বৈধ মালিক/ইনভেস্টর হিসাব নির্বাচন করুন"
        }

        val allAccountsById = accountingDao.getAllAccountsSync().associateBy { it.id }
        val activeLines = accountingDao.getActivePostedLines()
        val revenueNetCredit = activeLines
            .filter { allAccountsById[it.accountId]?.accountType == AccountType.REVENUE.name }
            .sumOf { if (it.isDebit) -it.amountMinor else it.amountMinor }
        val expenseNetDebit = activeLines
            .filter { allAccountsById[it.accountId]?.accountType == AccountType.EXPENSE.name }
            .sumOf { if (it.isDebit) it.amountMinor else -it.amountMinor }
        val retainedPostedNetCredit = activeLines
            .filter { it.accountId == RETAINED_EARNINGS_ACCOUNT_ID }
            .sumOf { if (it.isDebit) -it.amountMinor else it.amountMinor }
        val availableProfitMinor = (revenueNetCredit - expenseNetDebit) + retainedPostedNetCredit
        require(availableProfitMinor > 0L && amountMinor <= availableProfitMinor) {
            "বণ্টনযোগ্য নিট মুনাফা পর্যাপ্ত নেই (বর্তমান বণ্টনযোগ্য মুনাফা: ৳${Formatters.formatMinorUnit(availableProfitMinor.coerceAtLeast(0L))})"
        }

        val cleanNote = notes?.trim()?.takeIf { it.isNotBlank() }
        val suffix = cleanNote?.let { " ($it)" } ?: ""
        val invNameBn = investorAcc.nameBn.ifBlank { investorAcc.nameEn }

        if (reinvestToCapital) {
            postJournalEntry(
                entryDate = entryDate,
                descEn = "Profit Share Reinvested to Capital (${investorAcc.nameEn})$suffix",
                descBn = "মুনাফার অংশ মূলধনে পুনঃবিনিয়োগ ($invNameBn)$suffix",
                referenceType = "PROFIT_REINVESTMENT",
                referenceId = investorAcc.id,
                lines = listOf(
                    JournalLineDraft(RETAINED_EARNINGS_ACCOUNT_ID, true, amountMinor, "Profit share reinvested to ${investorAcc.nameEn}"),
                    JournalLineDraft(investorAcc.id, false, amountMinor, "Reinvested profit share added to capital")
                )
            )
        } else {
            val cleanPayId = paymentAccountId?.takeIf { it.isNotBlank() }
                ?: throw IllegalArgumentException("পরিশোধের জন্য একটি নগদ/ব্যাংক হিসাব নির্বাচন করুন")
            val payAcc = accountingDao.getAccountById(cleanPayId)
                ?: throw IllegalArgumentException("Payment account not found: $cleanPayId")
            require(payAcc.liquidKind != null || payAcc.id == MAIN_CASH_ACCOUNT_ID) {
                "পরিশোধের জন্য একটি বৈধ নগদ, ব্যাংক বা মোবাইল ব্যাংকিং হিসাব নির্বাচন করুন"
            }
            requireSufficientLiquidBalance(payAcc.id, amountMinor)
            val payNameBn = payAcc.nameBn.ifBlank { payAcc.nameEn }
            postJournalEntry(
                entryDate = entryDate,
                descEn = "Profit Distribution to ${investorAcc.nameEn} via ${payAcc.nameEn}$suffix",
                descBn = "মুনাফার অংশ প্রদান ($invNameBn) - $payNameBn$suffix",
                referenceType = "PROFIT_DISTRIBUTION",
                referenceId = investorAcc.id,
                lines = listOf(
                    JournalLineDraft(RETAINED_EARNINGS_ACCOUNT_ID, true, amountMinor, "Profit share distributed to ${investorAcc.nameEn}"),
                    JournalLineDraft(payAcc.id, false, amountMinor, "Paid profit share to ${investorAcc.nameEn}")
                )
            )
        }
    }

    override suspend fun payAccruedCustomsAndFreight(
        paymentAccountId: String,
        amountMinor: Long,
        entryDate: Long,
        notes: String?
    ): String = database.withTransaction {
        require(amountMinor > 0L) { "পরিশোধের পরিমাণ শূন্যের বেশি হতে হবে (Amount must be positive)" }
        accountingDao.getAccountById("coa-2020")
            ?: throw IllegalStateException("Accrued freight account (2020) not found")
        val accruedDueMinor = (-accountingDao.getNetDebitBalanceForAccount("2020")).coerceAtLeast(0L)
        require(amountMinor <= accruedDueMinor) {
            "বকেয়া ফ্রেইট/শুল্কের (৳${Formatters.formatMinorUnit(accruedDueMinor)}) চেয়ে বেশি পরিশোধ করা যাবে না"
        }
        requireSufficientLiquidBalance(paymentAccountId, amountMinor)
        val suffix = notes?.trim()?.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: ""
        postJournalEntry(
            entryDate = entryDate,
            descEn = "Freight & Customs Payable Settlement$suffix",
            descBn = "বকেয়া শুল্ক ও ফ্রেইট বিল পরিশোধ$suffix",
            referenceType = "FREIGHT_DUTY_PAYMENT",
            referenceId = paymentAccountId,
            lines = listOf(
                JournalLineDraft("coa-2020", true, amountMinor, "Settle accrued customs & freight"),
                JournalLineDraft(paymentAccountId, false, amountMinor, "Paid customs & freight")
            )
        )
    }
}
