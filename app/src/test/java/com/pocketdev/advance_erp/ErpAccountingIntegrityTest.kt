package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.core.engine.ErpAccountingGuard
import com.pocketdev.advance_erp.data.entity.AccountType
import com.pocketdev.advance_erp.data.entity.ChartOfAccount
import com.pocketdev.advance_erp.data.entity.LiquidAccountKind
import com.pocketdev.advance_erp.data.entity.NormalBalance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ErpAccountingIntegrityTest {

    private fun cashAccount(id: String = "coa-1010") = ChartOfAccount(
        id = id,
        accountCode = "1010",
        nameEn = "Cash in Hand",
        nameBn = "নগদ তহবিল",
        accountType = AccountType.ASSET.name,
        normalBalance = NormalBalance.DEBIT.name,
        accountSubType = LiquidAccountKind.CASH.name
    )

    private fun dummyNonLiquidAccount() = ChartOfAccount(
        id = "cash",
        accountCode = "1010",
        nameEn = "Cash",
        nameBn = "নগদ",
        accountType = AccountType.ASSET.name,
        normalBalance = NormalBalance.DEBIT.name,
        accountSubType = null
    )

    @Test(expected = IllegalStateException::class)
    fun blocksLiquidPaymentWhenCurrentBalanceIsZeroOrLessThanRequired() {
        ErpAccountingGuard.validateSufficientLiquidBalance(
            account = cashAccount(),
            currentBalanceMinor = 0L,
            requiredAmountMinor = 30760000L
        )
    }

    @Test
    fun allowsLiquidPaymentWhenSufficientCapitalDepositedAndSkipsNonLiquidTestAccounts() {
        ErpAccountingGuard.validateSufficientLiquidBalance(
            account = cashAccount(),
            currentBalanceMinor = 30760000L,
            requiredAmountMinor = 30760000L
        )
        ErpAccountingGuard.validateSufficientLiquidBalance(
            account = dummyNonLiquidAccount(),
            currentBalanceMinor = 0L,
            requiredAmountMinor = 10000L
        )
    }

    @Test
    fun mapsSeaFreightCustomsAndHandlingToStandardExpenseAccounts() {
        assertEquals("coa-5030", ErpAccountingGuard.mapImportCostCategoryToExpenseAccountId("SEA_FREIGHT"))
        assertEquals("coa-5020", ErpAccountingGuard.mapImportCostCategoryToExpenseAccountId("CUSTOMS_DUTY"))
        assertEquals("coa-5040", ErpAccountingGuard.mapImportCostCategoryToExpenseAccountId("CHINA_DOMESTIC_TRANSPORT"))
        assertEquals("coa-5140", ErpAccountingGuard.mapImportCostCategoryToExpenseAccountId("OTHER_EXPENSES", "Sample Cost"))
        assertEquals("coa-5040", ErpAccountingGuard.mapImportCostCategoryToExpenseAccountId("OTHER_EXPENSES", "Handling Charge"))
    }

    @Test
    fun allocatesFreightRoundingDeltaExactlyAcrossExpenseBuckets() {
        // Matches live PO-2026-002 scenario: 496,937.60 landed - 307,600.00 PO = 189,337.60 delta
        val targetDelta = 49693760L - 30760000L // 18,933,760
        val buckets = ErpAccountingGuard.allocateLandedExpenseBucketsMinor(
            unpostedCosts = emptyList(),
            categoryFreightMinor = 18934400L, // exact KG * rate before per-unit rounding
            targetTotalCapitalizedDeltaMinor = targetDelta
        )

        assertEquals(mapOf("coa-5030" to 18933760L), buckets)
        assertEquals(targetDelta, buckets.values.sum())
        assertTrue(buckets.values.all { it > 0L })
    }
}
