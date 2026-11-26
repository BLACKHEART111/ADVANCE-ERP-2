package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.model.CurrencyCode
import com.pocketdev.advance_erp.data.entity.Customer
import com.pocketdev.advance_erp.data.entity.Supplier
import com.pocketdev.advance_erp.feature.contacts.ContactBalanceFilter
import com.pocketdev.advance_erp.feature.contacts.ContactRegistrationFilter
import com.pocketdev.advance_erp.feature.contacts.ContactSortOption
import com.pocketdev.advance_erp.feature.contacts.ContactsFilterState
import com.pocketdev.advance_erp.feature.contacts.ContactsUiLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ContactsUiLogicTest {

    private fun fixedNow(): Long = Calendar.getInstance().apply {
        set(2026, Calendar.NOVEMBER, 26, 12, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun sampleCustomers(now: Long): List<Customer> {
        val monthStart = ContactsUiLogic.startOfCurrentMonthMillis(now)
        return listOf(
            Customer(
                id = "c1",
                customerCode = "CUST-9668",
                name = "Hasan Mojumdar",
                phone = "01866482142",
                address = "Narsingdi",
                outstandingBalanceMinor = 2_956_000L,
                createdAt = monthStart + 86_400_000L
            ),
            Customer(
                id = "c2",
                customerCode = "CUST-1060",
                name = "Jahangir mama",
                phone = "+8801758967461",
                address = "Dhaka",
                outstandingBalanceMinor = 1_010_000L,
                createdAt = monthStart - 10L * 86_400_000L
            ),
            Customer(
                id = "c3",
                customerCode = "CUST-6433",
                name = "Jibon Vai Elephant Road",
                phone = "+8801859547741",
                address = "Dhaka",
                outstandingBalanceMinor = 3_783_000L,
                createdAt = now - 45L * 86_400_000L
            ),
            Customer(
                id = "c4",
                customerCode = "CUST-3205",
                name = "Md Salauddin Mojumdar",
                phone = "01935812761",
                address = "Netrakona",
                outstandingBalanceMinor = 0L,
                createdAt = monthStart + 2L * 86_400_000L
            ),
            Customer(
                id = "c5",
                customerCode = "CUST-4001",
                name = "Advance Credit Client",
                phone = "01711000999",
                address = "Chattogram",
                outstandingBalanceMinor = -50_000L,
                createdAt = now - 60L * 86_400_000L
            )
        )
    }

    private fun sampleSuppliers(now: Long): List<Supplier> {
        val monthStart = ContactsUiLogic.startOfCurrentMonthMillis(now)
        return listOf(
            Supplier(
                id = "s1",
                supplierCode = "SUP-12350",
                companyName = "T GUARD",
                contactPerson = "Chen",
                phone = "01700001111",
                address = "Guangzhou, China",
                country = "China",
                currency = "CNY",
                outstandingPayableMinor = 1_500_000L,
                createdAt = monthStart + 1000L
            ),
            Supplier(
                id = "s2",
                supplierCode = "SUP-1948",
                companyName = "Wangdu Jiezu Xian Deng",
                contactPerson = "Li Yanan",
                phone = "15233736047",
                address = "Wangdu County, Baoding City, Hebei Province",
                country = "China",
                currency = "CNY",
                outstandingPayableMinor = 0L,
                createdAt = now - 40L * 86_400_000L
            ),
            Supplier(
                id = "s3",
                supplierCode = "SUP-7701",
                companyName = "Bengal Logistics",
                contactPerson = "Karim",
                phone = "01812345678",
                address = "Motijheel, Dhaka",
                country = "Bangladesh",
                currency = "BDT",
                outstandingPayableMinor = 420_000L,
                createdAt = now - 5L * 86_400_000L
            )
        )
    }

    @Test
    fun summaryMetrics_computeLiveCustomerAndSupplierAggregatesAccurately() {
        val now = fixedNow()
        val customers = sampleCustomers(now)
        val custMetrics = ContactsUiLogic.computeCustomerSummaryMetrics(
            customers = customers,
            totalReceivedMinor = 93_725_000L,
            nowMillis = now
        )
        assertEquals(5, custMetrics.totalCount)
        assertEquals(2, custMetrics.newThisMonthCount)
        // Positive dues only: 2,956,000 + 1,010,000 + 3,783,000 = 7,749,000
        assertEquals(7_749_000L, custMetrics.totalOutstandingMinor)
        assertEquals(93_725_000L, custMetrics.totalSettledPaymentsMinor)
        assertEquals(3, custMetrics.duePartyCount)

        val suppliers = sampleSuppliers(now)
        val suppMetrics = ContactsUiLogic.computeSupplierSummaryMetrics(
            suppliers = suppliers,
            totalPaidBdtMinor = 5_000_000L,
            nowMillis = now
        )
        assertEquals(3, suppMetrics.totalCount)
        assertEquals(2, suppMetrics.newThisMonthCount)
        assertEquals(1_920_000L, suppMetrics.totalOutstandingMinor)
        assertEquals(5_000_000L, suppMetrics.totalSettledPaymentsMinor)
        assertEquals(2, suppMetrics.duePartyCount)
    }

    @Test
    fun searchFilterAndSortCustomers_handleNameCodeBanglaDigitsLocationAndDates() {
        val now = fixedNow()
        val customers = sampleCustomers(now)

        // Search by code
        assertEquals(
            listOf("c1"),
            ContactsUiLogic.filterAndSortCustomers(customers, "9668", ContactsFilterState(), ContactSortOption.NAME_ASC, now)
                .map { it.id }
        )

        // Search by Bangla phone digits
        assertEquals(
            listOf("c4"),
            ContactsUiLogic.filterAndSortCustomers(customers, "০১৯৩৫", ContactsFilterState(), ContactSortOption.NAME_ASC, now)
                .map { it.id }
        )

        // Filter WITH_DUE + location Dhaka + Balance DESC
        val dhakaDues = ContactsUiLogic.filterAndSortCustomers(
            customers = customers,
            query = "",
            filterState = ContactsFilterState(
                balanceFilter = ContactBalanceFilter.WITH_DUE,
                selectedLocation = "Dhaka"
            ),
            sortOption = ContactSortOption.BALANCE_DESC,
            nowMillis = now
        )
        assertEquals(listOf("c3", "c2"), dhakaDues.map { it.id })

        // Filter THIS_MONTH
        val thisMonth = ContactsUiLogic.filterAndSortCustomers(
            customers = customers,
            query = "",
            filterState = ContactsFilterState(registrationFilter = ContactRegistrationFilter.THIS_MONTH),
            sortOption = ContactSortOption.CREATED_DESC,
            nowMillis = now
        )
        assertEquals(listOf("c4", "c1"), thisMonth.map { it.id })

        // Filter ADVANCE_CREDIT
        val credit = ContactsUiLogic.filterAndSortCustomers(
            customers = customers,
            query = "",
            filterState = ContactsFilterState(balanceFilter = ContactBalanceFilter.ADVANCE_CREDIT),
            sortOption = ContactSortOption.NAME_ASC,
            nowMillis = now
        )
        assertEquals(listOf("c5"), credit.map { it.id })
    }

    @Test
    fun searchFilterAndSortSuppliers_handleCompanyContactCountryAndBalance() {
        val now = fixedNow()
        val suppliers = sampleSuppliers(now)

        assertEquals(
            listOf("s2"),
            ContactsUiLogic.filterAndSortSuppliers(suppliers, "Li Yanan", ContactsFilterState(), ContactSortOption.NAME_ASC, now)
                .map { it.id }
        )

        val chinaSuppliers = ContactsUiLogic.filterAndSortSuppliers(
            suppliers = suppliers,
            query = "",
            filterState = ContactsFilterState(selectedLocation = "China"),
            sortOption = ContactSortOption.BALANCE_DESC,
            nowMillis = now
        )
        assertEquals(listOf("s1", "s2"), chinaSuppliers.map { it.id })

        val olderSuppliers = ContactsUiLogic.filterAndSortSuppliers(
            suppliers = suppliers,
            query = "",
            filterState = ContactsFilterState(registrationFilter = ContactRegistrationFilter.OLDER),
            sortOption = ContactSortOption.NAME_ASC,
            nowMillis = now
        )
        assertEquals(listOf("s2"), olderSuppliers.map { it.id })
    }

    @Test
    fun compactSummaryFormatting_usesBangladeshiGroupingAndLocalizedDigits() {
        assertEquals("৳ 12,48,500", ContactsUiLogic.formatCompactSummaryMoney(124_850_000L, CurrencyCode.BDT, AppLanguage.ENGLISH))
        assertEquals("৳ ১২,৪৮,৫০০", ContactsUiLogic.formatCompactSummaryMoney(124_850_000L, CurrencyCode.BDT, AppLanguage.BANGLA))
        assertEquals("৳ ৯,৩৭,২৫০", ContactsUiLogic.formatCompactSummaryMoney(93_725_000L, CurrencyCode.BDT, AppLanguage.BANGLA))
        assertEquals("৳ 29,560.50", ContactsUiLogic.formatCompactSummaryMoney(2_956_050L, CurrencyCode.BDT, AppLanguage.ENGLISH))
        assertEquals("১,২৪৮", ContactsUiLogic.formatCount(1248, AppLanguage.BANGLA))
        assertTrue(ContactsUiLogic.extractLocationLabel("Wangdu County, Baoding City, Hebei Province").contains("Hebei"))
    }
}
