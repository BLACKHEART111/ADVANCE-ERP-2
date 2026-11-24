package com.pocketdev.advance_erp.core.engine

import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.data.entity.ChartOfAccount

/**
 * Pure double-entry accounting invariants, liquid balance guards, and import cost
 * expense account mapping helpers.
 */
object ErpAccountingGuard {

    fun validateSufficientLiquidBalance(
        account: ChartOfAccount?,
        currentBalanceMinor: Long,
        requiredAmountMinor: Long
    ) {
        if (account == null || account.liquidKind == null || requiredAmountMinor <= 0L) return
        if (currentBalanceMinor < requiredAmountMinor) {
            val accName = account.nameBn.ifBlank { account.nameEn }
            val currentFmt = Formatters.formatMinorUnit(currentBalanceMinor)
            val neededFmt = Formatters.formatMinorUnit(requiredAmountMinor)
            throw IllegalStateException(
                "নির্বাচিত অ্যাকাউন্টে ($accName) পর্যাপ্ত ব্যালেন্স নেই! " +
                    "বর্তমান ব্যালেন্স: ৳$currentFmt, প্রয়োজন: ৳$neededFmt। অনুগ্রহ করে আগে মূলধন/টাকা জমা করুন। " +
                    "(Insufficient balance in ${account.nameEn}: available ৳$currentFmt, required ৳$neededFmt)"
            )
        }
    }

    fun mapImportCostCategoryToExpenseAccountId(
        costCategory: String,
        description: String = ""
    ): String {
        if (description.contains("Sample", ignoreCase = true)) return "coa-5140"
        if (description.contains("Handling", ignoreCase = true)) return "coa-5040"
        return when (costCategory) {
            "CUSTOMS_DUTY", "PORT_CHARGES", "CLEARING_AGENT_FEE" -> "coa-5020"
            "INTERNATIONAL_FREIGHT", "SEA_FREIGHT", "AIR_FREIGHT" -> "coa-5030"
            "CHINA_DOMESTIC_TRANSPORT", "LOCAL_TRANSPORT", "WAREHOUSE_UNLOADING", "PACKAGING" -> "coa-5040"
            else -> "coa-5190"
        }
    }

    /**
     * Distributes [targetTotalCapitalizedDeltaMinor] strictly positively across mapped expense accounts
     * (`5020` Customs, `5030` Shipping/Freight, `5040` Handling/Transport, `5140` Samples, `5190` Other),
     * reconciling any integer division rounding delta so the bucket sum equals [targetTotalCapitalizedDeltaMinor].
     */
    fun allocateLandedExpenseBucketsMinor(
        unpostedCosts: List<Pair<String, Long>>,
        categoryFreightMinor: Long,
        targetTotalCapitalizedDeltaMinor: Long
    ): Map<String, Long> {
        if (targetTotalCapitalizedDeltaMinor <= 0L) return emptyMap()

        val rawBuckets = linkedMapOf<String, Long>()
        for ((accountId, amount) in unpostedCosts) {
            if (amount > 0L && accountId.isNotBlank()) {
                rawBuckets[accountId] = (rawBuckets[accountId] ?: 0L) + amount
            }
        }
        if (categoryFreightMinor > 0L) {
            rawBuckets["coa-5030"] = (rawBuckets["coa-5030"] ?: 0L) + categoryFreightMinor
        }

        if (rawBuckets.isEmpty()) {
            return mapOf("coa-5030" to targetTotalCapitalizedDeltaMinor)
        }

        val rawSum = rawBuckets.values.sum()
        if (rawSum == targetTotalCapitalizedDeltaMinor) {
            return rawBuckets
        }

        val adjusted = rawBuckets.toMutableMap()
        val diff = targetTotalCapitalizedDeltaMinor - rawSum
        val maxEntry = adjusted.maxByOrNull { it.value }
        if (maxEntry != null && maxEntry.value + diff > 0L) {
            adjusted[maxEntry.key] = maxEntry.value + diff
            return adjusted.filterValues { it > 0L }
        }

        // Proportional fallback when negative variance exceeds single largest bucket
        val result = linkedMapOf<String, Long>()
        var remainingTarget = targetTotalCapitalizedDeltaMinor
        val entries = adjusted.entries.toList()
        for (idx in entries.indices) {
            val (key, value) = entries[idx]
            val alloc = if (idx == entries.lastIndex) {
                remainingTarget.coerceAtLeast(0L)
            } else {
                ((value.toDouble() / rawSum.toDouble()) * targetTotalCapitalizedDeltaMinor.toDouble())
                    .toLong()
                    .coerceIn(0L, remainingTarget)
            }
            if (alloc > 0L) {
                result[key] = alloc
                remainingTarget -= alloc
            }
        }
        return if (result.isEmpty()) mapOf("coa-5030" to targetTotalCapitalizedDeltaMinor) else result
    }
}
