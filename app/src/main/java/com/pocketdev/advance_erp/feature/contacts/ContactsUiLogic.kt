package com.pocketdev.advance_erp.feature.contacts

import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.model.CurrencyCode
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.data.entity.Customer
import com.pocketdev.advance_erp.data.entity.Supplier
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

enum class ContactBalanceFilter {
    ALL,
    WITH_DUE,
    SETTLED,
    ADVANCE_CREDIT
}

enum class ContactRegistrationFilter {
    ALL,
    THIS_MONTH,
    LAST_30_DAYS,
    OLDER
}

enum class ContactSortOption {
    NAME_ASC,
    NAME_DESC,
    BALANCE_DESC,
    BALANCE_ASC,
    LOCATION_ASC,
    CREATED_DESC,
    CREATED_ASC
}

data class ContactsFilterState(
    val balanceFilter: ContactBalanceFilter = ContactBalanceFilter.ALL,
    val selectedLocation: String? = null,
    val registrationFilter: ContactRegistrationFilter = ContactRegistrationFilter.ALL
) {
    val activeCount: Int
        get() = (if (balanceFilter != ContactBalanceFilter.ALL) 1 else 0) +
            (if (!selectedLocation.isNullOrBlank()) 1 else 0) +
            (if (registrationFilter != ContactRegistrationFilter.ALL) 1 else 0)
}

data class ContactsSummaryMetrics(
    val totalCount: Int = 0,
    val newThisMonthCount: Int = 0,
    val totalOutstandingMinor: Long = 0L,
    val totalSettledPaymentsMinor: Long = 0L,
    val duePartyCount: Int = 0
)

object ContactsUiLogic {

    private const val THIRTY_DAYS_MILLIS = 30L * 24L * 60L * 60L * 1000L

    fun startOfCurrentMonthMillis(nowMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun normalizeSearchDigits(raw: String): String =
        Formatters.toEnglishDigits(raw).trim().lowercase(Locale.US)

    private fun compactPhoneToken(raw: String): String =
        normalizeSearchDigits(raw).replace(" ", "").replace("-", "").replace("(", "").replace(")", "")

    fun extractLocationLabel(address: String, fallback: String = ""): String {
        val clean = address.trim()
        if (clean.isEmpty()) return fallback.trim()
        if (clean.length <= 18) return clean
        val commaParts = clean.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        return if (commaParts.size >= 2) commaParts.last() else clean
    }

    fun extractCustomerLocations(customers: List<Customer>): List<String> =
        customers.mapNotNull { cust ->
            extractLocationLabel(cust.address).takeIf { it.isNotBlank() }
        }.distinctBy { it.lowercase(Locale.US) }.sortedBy { it.lowercase(Locale.US) }

    fun extractSupplierLocations(suppliers: List<Supplier>): List<String> =
        suppliers.mapNotNull { sup ->
            sup.country.trim().ifBlank { extractLocationLabel(sup.address) }.takeIf { it.isNotBlank() }
        }.distinctBy { it.lowercase(Locale.US) }.sortedBy { it.lowercase(Locale.US) }

    fun computeCustomerSummaryMetrics(
        customers: List<Customer>,
        totalReceivedMinor: Long,
        nowMillis: Long = System.currentTimeMillis()
    ): ContactsSummaryMetrics {
        val monthStart = startOfCurrentMonthMillis(nowMillis)
        return ContactsSummaryMetrics(
            totalCount = customers.size,
            newThisMonthCount = customers.count { it.createdAt >= monthStart },
            totalOutstandingMinor = customers.filter { it.outstandingBalanceMinor > 0L }
                .sumOf { it.outstandingBalanceMinor },
            totalSettledPaymentsMinor = totalReceivedMinor.coerceAtLeast(0L),
            duePartyCount = customers.count { it.outstandingBalanceMinor > 0L }
        )
    }

    fun computeSupplierSummaryMetrics(
        suppliers: List<Supplier>,
        totalPaidBdtMinor: Long,
        nowMillis: Long = System.currentTimeMillis()
    ): ContactsSummaryMetrics {
        val monthStart = startOfCurrentMonthMillis(nowMillis)
        return ContactsSummaryMetrics(
            totalCount = suppliers.size,
            newThisMonthCount = suppliers.count { it.createdAt >= monthStart },
            totalOutstandingMinor = suppliers.filter { it.outstandingPayableMinor > 0L }
                .sumOf { it.outstandingPayableMinor },
            totalSettledPaymentsMinor = totalPaidBdtMinor.coerceAtLeast(0L),
            duePartyCount = suppliers.count { it.outstandingPayableMinor > 0L }
        )
    }

    fun filterAndSortCustomers(
        customers: List<Customer>,
        query: String,
        filterState: ContactsFilterState,
        sortOption: ContactSortOption,
        nowMillis: Long = System.currentTimeMillis()
    ): List<Customer> {
        val monthStart = startOfCurrentMonthMillis(nowMillis)
        val thirtyDaysAgo = nowMillis - THIRTY_DAYS_MILLIS
        val normQuery = normalizeSearchDigits(query)
        val phoneQuery = compactPhoneToken(query)
        val locationNeedle = filterState.selectedLocation?.trim()?.takeIf { it.isNotEmpty() }

        val filtered = customers.filter { c ->
            val matchesQuery = normQuery.isEmpty() ||
                normalizeSearchDigits(c.name).contains(normQuery) ||
                normalizeSearchDigits(c.customerCode).contains(normQuery) ||
                (phoneQuery.isNotEmpty() && compactPhoneToken(c.phone).contains(phoneQuery)) ||
                normalizeSearchDigits(c.address).contains(normQuery) ||
                normalizeSearchDigits(c.email.orEmpty()).contains(normQuery)

            val matchesBal = matchesBalance(c.outstandingBalanceMinor, filterState.balanceFilter)
            val matchesLoc = locationNeedle == null ||
                c.address.contains(locationNeedle, ignoreCase = true)
            val matchesReg = matchesRegistration(c.createdAt, filterState.registrationFilter, monthStart, thirtyDaysAgo)

            matchesQuery && matchesBal && matchesLoc && matchesReg
        }

        val comparator = when (sortOption) {
            ContactSortOption.NAME_ASC -> compareBy<Customer> { it.name.lowercase(Locale.US) }
                .thenBy { it.customerCode.lowercase(Locale.US) }
            ContactSortOption.NAME_DESC -> compareByDescending<Customer> { it.name.lowercase(Locale.US) }
                .thenBy { it.customerCode.lowercase(Locale.US) }
            ContactSortOption.BALANCE_DESC -> compareByDescending<Customer> { it.outstandingBalanceMinor }
                .thenBy { it.name.lowercase(Locale.US) }
            ContactSortOption.BALANCE_ASC -> compareBy<Customer> { it.outstandingBalanceMinor }
                .thenBy { it.name.lowercase(Locale.US) }
            ContactSortOption.LOCATION_ASC -> compareBy<Customer> { extractLocationLabel(it.address, "zzz").lowercase(Locale.US) }
                .thenBy { it.name.lowercase(Locale.US) }
            ContactSortOption.CREATED_DESC -> compareByDescending<Customer> { it.createdAt }
                .thenBy { it.name.lowercase(Locale.US) }
            ContactSortOption.CREATED_ASC -> compareBy<Customer> { it.createdAt }
                .thenBy { it.name.lowercase(Locale.US) }
        }
        return filtered.sortedWith(comparator)
    }

    fun filterAndSortSuppliers(
        suppliers: List<Supplier>,
        query: String,
        filterState: ContactsFilterState,
        sortOption: ContactSortOption,
        nowMillis: Long = System.currentTimeMillis()
    ): List<Supplier> {
        val monthStart = startOfCurrentMonthMillis(nowMillis)
        val thirtyDaysAgo = nowMillis - THIRTY_DAYS_MILLIS
        val normQuery = normalizeSearchDigits(query)
        val phoneQuery = compactPhoneToken(query)
        val locationNeedle = filterState.selectedLocation?.trim()?.takeIf { it.isNotEmpty() }

        val filtered = suppliers.filter { s ->
            val matchesQuery = normQuery.isEmpty() ||
                normalizeSearchDigits(s.companyName).contains(normQuery) ||
                normalizeSearchDigits(s.supplierCode).contains(normQuery) ||
                normalizeSearchDigits(s.contactPerson.orEmpty()).contains(normQuery) ||
                (phoneQuery.isNotEmpty() && compactPhoneToken(s.phone).contains(phoneQuery)) ||
                normalizeSearchDigits(s.address).contains(normQuery) ||
                normalizeSearchDigits(s.country).contains(normQuery) ||
                normalizeSearchDigits(s.currency).contains(normQuery) ||
                normalizeSearchDigits(s.email.orEmpty()).contains(normQuery)

            val matchesBal = matchesBalance(s.outstandingPayableMinor, filterState.balanceFilter)
            val matchesLoc = locationNeedle == null ||
                s.country.contains(locationNeedle, ignoreCase = true) ||
                s.address.contains(locationNeedle, ignoreCase = true)
            val matchesReg = matchesRegistration(s.createdAt, filterState.registrationFilter, monthStart, thirtyDaysAgo)

            matchesQuery && matchesBal && matchesLoc && matchesReg
        }

        val comparator = when (sortOption) {
            ContactSortOption.NAME_ASC -> compareBy<Supplier> { it.companyName.lowercase(Locale.US) }
                .thenBy { it.supplierCode.lowercase(Locale.US) }
            ContactSortOption.NAME_DESC -> compareByDescending<Supplier> { it.companyName.lowercase(Locale.US) }
                .thenBy { it.supplierCode.lowercase(Locale.US) }
            ContactSortOption.BALANCE_DESC -> compareByDescending<Supplier> { it.outstandingPayableMinor }
                .thenBy { it.companyName.lowercase(Locale.US) }
            ContactSortOption.BALANCE_ASC -> compareBy<Supplier> { it.outstandingPayableMinor }
                .thenBy { it.companyName.lowercase(Locale.US) }
            ContactSortOption.LOCATION_ASC -> compareBy<Supplier> {
                it.country.ifBlank { extractLocationLabel(it.address, "zzz") }.lowercase(Locale.US)
            }.thenBy { it.companyName.lowercase(Locale.US) }
            ContactSortOption.CREATED_DESC -> compareByDescending<Supplier> { it.createdAt }
                .thenBy { it.companyName.lowercase(Locale.US) }
            ContactSortOption.CREATED_ASC -> compareBy<Supplier> { it.createdAt }
                .thenBy { it.companyName.lowercase(Locale.US) }
        }
        return filtered.sortedWith(comparator)
    }

    private fun matchesBalance(balanceMinor: Long, filter: ContactBalanceFilter): Boolean = when (filter) {
        ContactBalanceFilter.ALL -> true
        ContactBalanceFilter.WITH_DUE -> balanceMinor > 0L
        ContactBalanceFilter.SETTLED -> balanceMinor == 0L
        ContactBalanceFilter.ADVANCE_CREDIT -> balanceMinor < 0L
    }

    private fun matchesRegistration(
        createdAt: Long,
        filter: ContactRegistrationFilter,
        monthStart: Long,
        thirtyDaysAgo: Long
    ): Boolean = when (filter) {
        ContactRegistrationFilter.ALL -> true
        ContactRegistrationFilter.THIS_MONTH -> createdAt >= monthStart
        ContactRegistrationFilter.LAST_30_DAYS -> createdAt >= thirtyDaysAgo
        ContactRegistrationFilter.OLDER -> createdAt < thirtyDaysAgo
    }

    fun formatCompactSummaryMoney(
        amountMinor: Long,
        currency: CurrencyCode,
        language: AppLanguage
    ): String {
        val isNegative = amountMinor < 0L
        val absMinor = abs(amountMinor)
        val major = absMinor / 100L
        val minor = absMinor % 100L
        val groupedMajor = formatIndianGrouping(major)
        val numericText = if (minor == 0L) {
            groupedMajor
        } else {
            String.format(Locale.US, "%s.%02d", groupedMajor, minor)
        }
        val sign = if (isNegative) "-" else ""
        val localized = if (language == AppLanguage.BANGLA) {
            Formatters.toBanglaDigits(numericText)
        } else {
            numericText
        }
        return "$sign${currency.symbol} $localized"
    }

    fun formatCount(count: Int, language: AppLanguage): String {
        val grouped = formatIndianGrouping(count.toLong().coerceAtLeast(0L))
        return if (language == AppLanguage.BANGLA) Formatters.toBanglaDigits(grouped) else grouped
    }

    private fun formatIndianGrouping(value: Long): String {
        val digits = abs(value).toString()
        if (digits.length <= 3) return digits
        val lastThree = digits.takeLast(3)
        val remaining = digits.dropLast(3)
        val pairs = StringBuilder()
        var idx = remaining.length
        while (idx > 0) {
            val start = (idx - 2).coerceAtLeast(0)
            if (pairs.isNotEmpty()) pairs.insert(0, ',')
            pairs.insert(0, remaining.substring(start, idx))
            idx = start
        }
        return "$pairs,$lastThree"
    }
}
