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

/** Explicit dated adjustments for supplier payable ledgers (e.g. opening payable balance). */
object SupplierLedgerAdjustment {
    const val CREDIT = "SUPPLIER_LEDGER_CREDIT_ADJUSTMENT"
    const val DEBIT = "SUPPLIER_LEDGER_DEBIT_ADJUSTMENT"

    /** Positive delta = increases supplier payable; negative delta = decreases supplier payable. */
    fun delta(entry: JournalEntry): Long {
        if (!entry.isPosted || entry.isReversed) return 0L
        require(entry.totalDebitMinor == entry.totalCreditMinor && entry.totalDebitMinor >= 0L)
        return when (entry.referenceType) {
            CREDIT -> entry.totalCreditMinor
            DEBIT -> -entry.totalDebitMinor
            else -> 0L
        }
    }
}
