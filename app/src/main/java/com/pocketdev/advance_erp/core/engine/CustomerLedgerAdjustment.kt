package com.pocketdev.advance_erp.core.engine

import com.pocketdev.advance_erp.data.entity.JournalEntry

/** Explicit dated adjustments; never substitute a receipt or an inferred opening balance. */
object CustomerLedgerAdjustment {
    const val DEBIT = "CUSTOMER_LEDGER_DEBIT_ADJUSTMENT"
    const val CREDIT = "CUSTOMER_LEDGER_CREDIT_ADJUSTMENT"

    fun delta(entry: JournalEntry): Long {
        if (!entry.isPosted || entry.isReversed) return 0L
        require(entry.totalDebitMinor == entry.totalCreditMinor && entry.totalDebitMinor >= 0L)
        return when (entry.referenceType) {
            DEBIT -> entry.totalDebitMinor
            CREDIT -> -entry.totalDebitMinor
            else -> 0L
        }
    }
}
