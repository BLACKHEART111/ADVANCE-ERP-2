package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.core.model.BangladeshBanks
import com.pocketdev.advance_erp.core.model.BankCategory
import com.pocketdev.advance_erp.data.entity.AccountType
import com.pocketdev.advance_erp.data.entity.ChartOfAccount
import com.pocketdev.advance_erp.data.entity.LiquidAccountKind
import com.pocketdev.advance_erp.data.entity.NormalBalance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartOfAccountsAndPaymentAccountTest {

    @Test
    fun testBangladeshBanksDirectoryCompleteAndUnique() {
        val banks = BangladeshBanks.allBanks
        assertTrue("Expected all scheduled BD banks (>= 60), found ${banks.size}", banks.size >= 60)

        val codes = banks.map { it.code }.toSet()
        assertEquals("Bank codes must be unique", banks.size, codes.size)

        // Ensure all major categories are represented
        assertTrue(banks.any { it.category == BankCategory.STATE_OWNED && it.nameEn.contains("Sonali") })
        assertTrue(banks.any { it.category == BankCategory.SPECIALIZED && it.nameEn.contains("Krishi") })
        assertTrue(banks.any { it.category == BankCategory.PRIVATE_CONVENTIONAL && it.nameEn.contains("Dutch-Bangla") })
        assertTrue(banks.any { it.category == BankCategory.PRIVATE_CONVENTIONAL && it.nameEn.contains("BRAC") })
        assertTrue(banks.any { it.category == BankCategory.ISLAMI && it.nameEn.contains("Islami Bank Bangladesh") })
        assertTrue(banks.any { it.category == BankCategory.FOREIGN && it.nameEn.contains("Standard Chartered") })

        banks.forEach { bank ->
            assertTrue("Missing English name for ${bank.code}", bank.nameEn.isNotBlank())
            assertTrue("Missing Bangla name for ${bank.code}", bank.nameBn.isNotBlank())
            assertTrue("Missing short name for ${bank.code}", bank.shortName.isNotBlank())
        }
    }

    @Test
    fun testMfsProvidersDirectoryIncludesBkashNagadRocket() {
        val codes = BangladeshBanks.mfsProviders.map { it.paymentMethodCode }.toSet()
        assertTrue("BKASH" in codes)
        assertTrue("NAGAD" in codes)
        assertTrue("ROCKET" in codes)
        assertTrue("UPAY" in codes)
    }

    @Test
    fun testInferPaymentMethodFromLiquidAccount() {
        val cashAcc = ChartOfAccount(
            id = "coa-1010",
            accountCode = "1010",
            nameEn = "Cash in Hand",
            nameBn = "নগদ তহবিল",
            accountType = AccountType.ASSET.name,
            normalBalance = NormalBalance.DEBIT.name,
            accountSubType = LiquidAccountKind.CASH.name
        )
        val dbblAcc = ChartOfAccount(
            id = "coa-1021",
            accountCode = "1021",
            nameEn = "Dutch-Bangla Bank PLC (Gulshan)",
            nameBn = "ডাচ-বাংলা ব্যাংক পিএলসি",
            accountType = AccountType.ASSET.name,
            normalBalance = NormalBalance.DEBIT.name,
            accountSubType = LiquidAccountKind.BANK.name,
            institutionName = "Dutch-Bangla Bank PLC",
            accountNumber = "101110001234"
        )
        val bkashAcc = ChartOfAccount(
            id = "coa-1060",
            accountCode = "1060",
            nameEn = "bKash Account",
            nameBn = "বিকাশ অ্যাকাউন্ট",
            accountType = AccountType.ASSET.name,
            normalBalance = NormalBalance.DEBIT.name,
            accountSubType = LiquidAccountKind.MOBILE_BANKING.name,
            institutionName = "bKash"
        )
        val nagadAcc = ChartOfAccount(
            id = "coa-1061",
            accountCode = "1061",
            nameEn = "Nagad Account",
            nameBn = "নগদ অ্যাকাউন্ট",
            accountType = AccountType.ASSET.name,
            normalBalance = NormalBalance.DEBIT.name,
            accountSubType = LiquidAccountKind.MOBILE_BANKING.name,
            institutionName = "Nagad"
        )
        val rocketAcc = ChartOfAccount(
            id = "coa-1062",
            accountCode = "1062",
            nameEn = "Rocket Merchant",
            nameBn = "রকেট মার্চেন্ট",
            accountType = AccountType.ASSET.name,
            normalBalance = NormalBalance.DEBIT.name,
            accountSubType = LiquidAccountKind.MOBILE_BANKING.name,
            institutionName = "Rocket (DBBL)"
        )
        val expenseAcc = ChartOfAccount(
            id = "coa-5110",
            accountCode = "5110",
            nameEn = "Office Rent",
            nameBn = "অফিস ভাড়া",
            accountType = AccountType.EXPENSE.name,
            normalBalance = NormalBalance.DEBIT.name
        )

        assertTrue(cashAcc.isLiquid)
        assertTrue(dbblAcc.isLiquid)
        assertTrue(bkashAcc.isLiquid)
        assertFalse(expenseAcc.isLiquid)
        assertNotNull(dbblAcc.liquidKind)

        assertEquals("CASH", BangladeshBanks.inferPaymentMethod(cashAcc))
        assertEquals("BANK", BangladeshBanks.inferPaymentMethod(dbblAcc))
        assertEquals("BKASH", BangladeshBanks.inferPaymentMethod(bkashAcc))
        assertEquals("NAGAD", BangladeshBanks.inferPaymentMethod(nagadAcc))
        assertEquals("ROCKET", BangladeshBanks.inferPaymentMethod(rocketAcc))
    }
}
