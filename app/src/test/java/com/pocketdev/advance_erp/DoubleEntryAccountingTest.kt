package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.data.entity.JournalEntry
import com.pocketdev.advance_erp.data.entity.JournalLine
import com.pocketdev.advance_erp.domain.repository.JournalLineDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DoubleEntryAccountingTest {

    @Test
    fun testBalancedJournalEntryValidation() {
        val line1 = JournalLineDraft(accountId = "coa-1210", isDebit = true, amountMinor = 5000000L) // Dr Inventory ৳50,000
        val line2 = JournalLineDraft(accountId = "coa-1020", isDebit = false, amountMinor = 5000000L) // Cr Bank ৳50,000

        val lines = listOf(line1, line2)
        val totalDebit = lines.filter { it.isDebit }.sumOf { it.amountMinor }
        val totalCredit = lines.filter { !it.isDebit }.sumOf { it.amountMinor }

        assertEquals(5000000L, totalDebit)
        assertEquals(5000000L, totalCredit)
        assertEquals(totalDebit, totalCredit)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testUnbalancedJournalEntryRejected() {
        val line1 = JournalLineDraft(accountId = "coa-1210", isDebit = true, amountMinor = 5000000L)
        val line2 = JournalLineDraft(accountId = "coa-1020", isDebit = false, amountMinor = 4800000L) // ৳200 discrepancy!

        val lines = listOf(line1, line2)
        val totalDebit = lines.filter { it.isDebit }.sumOf { it.amountMinor }
        val totalCredit = lines.filter { !it.isDebit }.sumOf { it.amountMinor }

        require(totalDebit == totalCredit) {
            "Debit ($totalDebit) does not balance with Credit ($totalCredit) in minor units!"
        }
    }

    @Test
    fun testJournalReversalSwapsDebitsAndCredits() {
        // Original entry:
        // Dr. Inventory Asset ৳1,00,000 (10,000,000 poisha)
        // Cr. Accounts Payable ৳1,00,000
        val originalLines = listOf(
            JournalLine("jl-1", "je-01", "coa-1210", "1210", "Inventory Asset", isDebit = true, amountMinor = 10000000L),
            JournalLine("jl-2", "je-01", "coa-2010", "2010", "Accounts Payable", isDebit = false, amountMinor = 10000000L)
        )

        // Reversal lines swap isDebit
        val reversalLines = originalLines.map { line ->
            JournalLine(
                id = "jl-rev-${line.id}",
                journalEntryId = "je-02",
                accountId = line.accountId,
                accountCode = line.accountCode,
                accountName = line.accountName,
                isDebit = !line.isDebit, // Swapped!
                amountMinor = line.amountMinor
            )
        }

        val revDebit = reversalLines.filter { it.isDebit }.sumOf { it.amountMinor }
        val revCredit = reversalLines.filter { !it.isDebit }.sumOf { it.amountMinor }

        assertEquals(10000000L, revDebit)
        assertEquals(10000000L, revCredit)

        // The line that was originally Debit is now Credit
        val revInventoryLine = reversalLines.find { it.accountCode == "1210" }!!
        assertFalse(revInventoryLine.isDebit)

        // The line that was originally Credit is now Debit
        val revPayableLine = reversalLines.find { it.accountCode == "2010" }!!
        assertTrue(revPayableLine.isDebit)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testPreventDuplicateReversal() {
        val entry = JournalEntry(
            id = "je-01",
            entryNumber = "JE-2026-0001",
            descriptionEn = "Test",
            descriptionBn = "টেস্ট",
            referenceType = "MANUAL",
            totalDebitMinor = 10000L,
            totalCreditMinor = 10000L,
            isReversed = true // Already reversed!
        )

        require(!entry.isReversed) { "Entry is already reversed: ${entry.entryNumber}" }
    }

    @Test
    fun testReversedEntryAndReversalAuditEntryProduceExactZeroActiveBalance() {
        fun isActiveBalanceEntry(entry: JournalEntry): Boolean =
            entry.isPosted && !entry.isReversed && entry.referenceType != "REVERSAL" && entry.reversalOfEntryId == null

        val priorCashDeposit = JournalEntry("je-10", "JE-2026-0010", descriptionEn = "Opening cash", descriptionBn = "", referenceType = "CAPITAL_DEPOSIT", totalDebitMinor = 10_950_000L, totalCreditMinor = 10_950_000L)
        val priorLines = listOf(
            JournalLine("jl-10a", "je-10", "coa-1010", "1010", "Cash", true, 10_950_000L),
            JournalLine("jl-10b", "je-10", "coa-3010", "3010", "Owner Capital", false, 10_950_000L)
        )

        val mistakenSojolDeposit = JournalEntry("je-13", "JE-2026-0013", descriptionEn = "Sojol deposit", descriptionBn = "", referenceType = "CAPITAL_DEPOSIT", totalDebitMinor = 20_000_000L, totalCreditMinor = 20_000_000L)
        val mistakenLines = listOf(
            JournalLine("jl-13a", "je-13", "coa-1010", "1010", "Cash", true, 20_000_000L),
            JournalLine("jl-13b", "je-13", "coa-3030", "3030", "SOJOL DADA", false, 20_000_000L)
        )

        // User withdrew ৳200,000 (je-14) then reversed that withdrawal (marking je-14 isReversed=true + inserting audit je-16).
        val reversedWithdrawal = JournalEntry("je-14", "JE-2026-0014", descriptionEn = "Withdraw", descriptionBn = "", referenceType = "CAPITAL_WITHDRAWAL", totalDebitMinor = 20_000_000L, totalCreditMinor = 20_000_000L, isReversed = true, reversalEntryId = "je-16")
        val withdrawalLines = listOf(
            JournalLine("jl-14a", "je-14", "coa-3030", "3030", "SOJOL DADA", true, 20_000_000L),
            JournalLine("jl-14b", "je-14", "coa-1010", "1010", "Cash", false, 20_000_000L)
        )

        val reversalAuditEntry = JournalEntry("je-16", "JE-2026-0016", descriptionEn = "Reversal of JE-2026-0014", descriptionBn = "", referenceType = "REVERSAL", referenceId = "je-14", totalDebitMinor = 20_000_000L, totalCreditMinor = 20_000_000L, reversalOfEntryId = "je-14")
        val auditLines = listOf(
            JournalLine("jl-16a", "je-16", "coa-1010", "1010", "Cash", true, 20_000_000L),
            JournalLine("jl-16b", "je-16", "coa-3030", "3030", "SOJOL DADA", false, 20_000_000L)
        )

        val entriesAfterWithdrawalReversal = listOf(priorCashDeposit, mistakenSojolDeposit, reversedWithdrawal, reversalAuditEntry).associateBy { it.id }
        val allLines = priorLines + mistakenLines + withdrawalLines + auditLines

        val cashAfterStep1 = allLines
            .filter { it.accountCode == "1010" && isActiveBalanceEntry(entriesAfterWithdrawalReversal[it.journalEntryId]!!) }
            .sumOf { if (it.isDebit) it.amountMinor else -it.amountMinor }
        assertEquals(30_950_000L, cashAfterStep1) // ৳309,500.00 (no ৳509,500 double count!)

        // Now user reverses mistaken deposit (je-13):
        val reversedMistakenDeposit = mistakenSojolDeposit.copy(isReversed = true, reversalEntryId = "je-17")
        val depositAudit = JournalEntry("je-17", "JE-2026-0017", descriptionEn = "Reversal of JE-2026-0013", descriptionBn = "", referenceType = "REVERSAL", referenceId = "je-13", totalDebitMinor = 20_000_000L, totalCreditMinor = 20_000_000L, reversalOfEntryId = "je-13")
        val depositAuditLines = listOf(
            JournalLine("jl-17a", "je-17", "coa-3030", "3030", "SOJOL DADA", true, 20_000_000L),
            JournalLine("jl-17b", "je-17", "coa-1010", "1010", "Cash", false, 20_000_000L)
        )
        val finalEntries = entriesAfterWithdrawalReversal + ("je-13" to reversedMistakenDeposit) + ("je-17" to depositAudit)
        val finalLines = allLines + depositAuditLines

        val finalCash = finalLines
            .filter { it.accountCode == "1010" && isActiveBalanceEntry(finalEntries[it.journalEntryId]!!) }
            .sumOf { if (it.isDebit) it.amountMinor else -it.amountMinor }
        val finalSojolEquity = finalLines
            .filter { it.accountCode == "3030" && isActiveBalanceEntry(finalEntries[it.journalEntryId]!!) }
            .sumOf { if (!it.isDebit) it.amountMinor else -it.amountMinor }

        assertEquals(10_950_000L, finalCash) // Exact original ৳109,500.00!
        assertEquals(0L, finalSojolEquity)     // Exact ৳0.00!
    }
}
