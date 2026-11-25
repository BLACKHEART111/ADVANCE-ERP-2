package com.pocketdev.advance_erp.core.engine

import com.pocketdev.advance_erp.domain.model.*
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Read-only accounting projection. Balances are computed BEFORE presentation filters. */
object LedgerReportEngine {
    // Master due is often clamped to zero by legacy code; that must not erase real advances.
    fun openingAdjustment(masterBalance: Long, transactionNet: Long): Long =
        if (masterBalance == 0L) 0L else Math.subtractExact(masterBalance, transactionNet)

    fun type(row: UnifiedTransactionItem) = row.rawType.removeSuffix("_REVERSAL")
    fun isCustomer(row: UnifiedTransactionItem) = type(row) in setOf("INVOICE", "PAYMENT", "RETURN", "OPENING_CUSTOMER", "CUSTOMER_ADJUSTMENT")
    fun isSupplier(row: UnifiedTransactionItem) = type(row) in setOf("PURCHASE", "SUPPLIER_PAYMENT", "OPENING_SUPPLIER", "SUPPLIER_ADJUSTMENT")
    fun delta(row: UnifiedTransactionItem): Long {
        if (!row.accountingPosted) return 0L
        val sign = when (type(row)) {
            "INVOICE", "PURCHASE", "OPENING_CUSTOMER", "OPENING_SUPPLIER", "CUSTOMER_ADJUSTMENT", "SUPPLIER_ADJUSTMENT" -> 1
            "PAYMENT", "SUPPLIER_PAYMENT" -> -1
            "RETURN" -> if (row.paymentMethod == "CUSTOMER_CREDIT") -1 else 0
            else -> 0
        }
        return Math.multiplyExact(row.amountMinor, (sign * row.effectSign).toLong())
    }
    private fun List<UnifiedTransactionItem>.sumMoney(value: (UnifiedTransactionItem) -> Long) =
        fold(0L) { total, row -> Math.addExact(total, value(row)) }
    private val chronology = compareBy<UnifiedTransactionItem> { it.date }.thenBy { it.createdAt }
        .thenBy { if (it.effectSign < 0) 1 else 0 }.thenBy { it.rawType }.thenBy { it.id }

    fun build(all: List<UnifiedTransactionItem>, filter: ControlCenterFilterState, party: LedgerParty? = null,
              now: Long = System.currentTimeMillis()): LedgerReport {
        val (from, to) = filter.dateRangeOption.getBoundsMillis(filter.customStartDate, filter.customEndDate)
        val partyRows = all.filter { row -> party != null && row.primaryEntityId == party.id &&
            (if (party.type == "CUSTOMER") isCustomer(row) else isSupplier(row)) }
        val legacy = partyRows.filter { type(it).startsWith("OPENING_") }.sumMoney(::delta)
        val opening = Math.addExact(legacy, partyRows.filter { it.date < from && !type(it).startsWith("OPENING_") }.sumMoney(::delta))
        val period = partyRows.filter { it.date in from..to && !type(it).startsWith("OPENING_") }.sortedWith(chronology)
        var running = opening
        val balanced = period.map { row ->
            val change = delta(row)
            running = Math.addExact(running, change)
            val debit = if (party?.type == "SUPPLIER") (-change).coerceAtLeast(0) else change.coerceAtLeast(0)
            val credit = if (party?.type == "SUPPLIER") change.coerceAtLeast(0) else (-change).coerceAtLeast(0)
            row.copy(runningBalanceMinor = running, ledgerDebitMinor = debit, ledgerCreditMinor = credit)
        }
        val scope = if (party != null) balanced else all.filter { it.date in from..to && !type(it).startsWith("OPENING_") }
        val visible = scope.filter { matches(it, filter, now) }.let { rows ->
            when (filter.sortBy) {
                SortOption.NEWEST_FIRST -> rows.sortedWith(chronology.reversed())
                SortOption.OLDEST_FIRST -> rows.sortedWith(chronology)
                SortOption.HIGHEST_AMOUNT -> rows.sortedWith(compareByDescending<UnifiedTransactionItem> { it.amountMinor }.then(chronology))
                SortOption.LOWEST_AMOUNT -> rows.sortedWith(compareBy<UnifiedTransactionItem> { it.amountMinor }.then(chronology))
                SortOption.TRANSACTION_TYPE -> rows.sortedWith(compareBy<UnifiedTransactionItem> { it.category.ordinal }.then(chronology.reversed()))
                SortOption.NAME_ASC -> rows.sortedWith(compareBy<UnifiedTransactionItem> { it.primaryEntityName.lowercase() }.then(chronology))
                SortOption.NAME_DESC -> rows.sortedWith(compareByDescending<UnifiedTransactionItem> { it.primaryEntityName.lowercase() }.then(chronology))
            }
        }
        val posted = visible.filter { it.accountingPosted }
        fun sum(kind: String) = posted.filter { type(it) == kind }.sumMoney { it.amountMinor * it.effectSign }
        val sales = sum("INVOICE"); val returns = sum("RETURN"); val expenses = sum("EXPENSE")
        val gross = posted.filter { type(it) in setOf("INVOICE", "RETURN") }.sumMoney { (it.profitMinor ?: 0) * it.effectSign }
        val statement = party?.let { PartyLedgerStatement(it.id, it.name, it.code, it.type, it.phone, it.address,
            opening, balanced.reversed(), balanced.sumMoney { it.ledgerDebitMinor ?: 0 },
            balanced.sumMoney { it.ledgerCreditMinor ?: 0 }, running) }
        val cashOpening = all.filter { it.date < from && it.accountingPosted }.sumMoney(::cashDelta)
        val totalReceivable = when {
            party != null && party.type == "CUSTOMER" -> statement!!.closingBalanceMinor.coerceAtLeast(0L)
            else -> posted.filter { type(it) == "INVOICE" && it.effectSign > 0 && it.status != "CANCELLED" }.sumMoney { it.secondaryAmountMinor ?: 0 }
        }
        val totalPayable = when {
            party != null && party.type == "SUPPLIER" -> statement!!.closingBalanceMinor.coerceAtLeast(0L)
            else -> posted.filter { type(it) == "PURCHASE" && it.effectSign > 0 }.sumMoney { (it.secondaryAmountMinor ?: 0).coerceAtLeast(0) }
        }
        val summary = BusinessSummaryDashboardData(
            totalSalesMinor = sales, totalPurchaseMinor = sum("PURCHASE"), totalReceivedMinor = sum("PAYMENT"),
            totalPaidMinor = sum("SUPPLIER_PAYMENT"), totalExpensesMinor = expenses, totalSalesReturnMinor = returns,
            totalOutstandingReceivableMinor = totalReceivable,
            totalOutstandingPayableMinor = totalPayable,
            grossProfitMinor = gross, netProfitMinor = Math.subtractExact(gross, expenses),
            totalTransactionsCount = visible.size,
            totalDeliveryCount = posted.count { it.status in setOf("DELIVERED", "PARTIALLY_DELIVERED") },
            openingBalanceMinor = statement?.openingBalanceMinor ?: cashOpening,
            closingBalanceMinor = statement?.closingBalanceMinor ?: Math.addExact(cashOpening, posted.sumMoney(::cashDelta))
        )
        // Aging is explicitly CURRENT open-document aging, separate from the dated statement.
        val current = (if (party == null) all else partyRows).filter { it.accountingPosted && it.effectSign > 0 &&
            it.status !in setOf("CANCELLED", "VOIDED") && type(it) in setOf("INVOICE", "PURCHASE") && (it.secondaryAmountMinor ?: 0) > 0 }
        val aging = current.groupBy { row ->
            val due = row.dueDate
            if (due == null) "NO_DUE_DATE" else {
                val zone = ZoneId.systemDefault()
                val days = ChronoUnit.DAYS.between(Instant.ofEpochMilli(due).atZone(zone).toLocalDate(), Instant.ofEpochMilli(now).atZone(zone).toLocalDate())
                when { days <= 0 -> "NOT_DUE"; days <= 30 -> "1_30"; days <= 60 -> "31_60"; days <= 90 -> "61_90"; else -> "90_PLUS" }
            }
        }.map { (key, rows) -> LedgerAgingBucket(key, rows.sumMoney { it.secondaryAmountMinor ?: 0 }, rows.size) }
        val unallocated = (if (party == null) all else partyRows).filter { type(it) == "PAYMENT" && it.effectSign > 0 && it.accountingPosted && it.status != "VOIDED" }
            .sumMoney { it.secondaryAmountMinor ?: 0 }
        return LedgerReport(filter, visible, summary, statement, aging, unallocated, period.size, legacy)
    }

    private fun cashDelta(row: UnifiedTransactionItem): Long = when (type(row)) {
        "PAYMENT" -> row.amountMinor * row.effectSign
        "SUPPLIER_PAYMENT", "EXPENSE" -> -row.amountMinor * row.effectSign
        "RETURN" -> if (row.paymentMethod in setOf("CASH_REFUND", "BANK_REFUND")) -row.amountMinor else 0
        else -> 0
    }

    fun matches(row: UnifiedTransactionItem, f: ControlCenterFilterState, now: Long): Boolean {
        val kind = type(row)
        val category = when (f.category) {
            ReportCategory.ALL_TRANSACTIONS -> true
            ReportCategory.CUSTOMER_LEDGER -> isCustomer(row)
            ReportCategory.SUPPLIER_LEDGER -> isSupplier(row)
            ReportCategory.SALES -> kind == "INVOICE"
            ReportCategory.PURCHASE -> kind == "PURCHASE" && row.category == ReportCategory.PURCHASE
            ReportCategory.IMPORT -> row.category == ReportCategory.IMPORT
            ReportCategory.PAYMENT -> kind in setOf("SUPPLIER_PAYMENT", "EXPENSE")
            ReportCategory.RECEIPT -> kind == "PAYMENT"
            ReportCategory.INCOME -> kind in setOf("INVOICE", "RETURN")
            ReportCategory.EXPENSE -> kind == "EXPENSE"
            ReportCategory.INVENTORY -> kind in setOf("STOCK_MOVEMENT", "STOCK_TRANSFER")
            else -> row.category == f.category
        }
        if (!category) return false
        if (f.selectedCustomerId != null && (!isCustomer(row) || row.primaryEntityId != f.selectedCustomerId)) return false
        if (f.selectedSupplierId != null && (!isSupplier(row) || row.primaryEntityId != f.selectedSupplierId)) return false
        if (f.selectedWarehouseId != null && row.warehouseId != f.selectedWarehouseId) return false
        if (f.selectedCategoryId != null && f.selectedCategoryId !in row.categoryIds) return false
        if (f.selectedProductId != null && f.selectedProductId !in row.productIds) return false
        val method = row.paymentMethod.orEmpty().uppercase()
        val paymentMethodMatch = when (f.paymentMethodFilter) {
            PaymentMethodFilter.ALL -> true
            PaymentMethodFilter.CASH -> method.contains("CASH")
            PaymentMethodFilter.BANK -> method.contains("BANK") || method in setOf("CHEQUE", "TT", "BANK_TT", "LETTER_OF_CREDIT")
            PaymentMethodFilter.MOBILE_BANKING -> method in setOf("BKASH", "NAGAD", "ROCKET", "UPAY", "MOBILE_BANKING", "MFS")
            PaymentMethodFilter.LC -> method.contains("LC") || method.contains("LETTER_OF_CREDIT")
            PaymentMethodFilter.TT -> method.contains("TT") || method.contains("TELEGRAPHIC")
        }
        if (!paymentMethodMatch) return false
        val directionMatch = when (f.transactionDirection) {
            TransactionDirectionFilter.ALL -> true
            TransactionDirectionFilter.INFLOW -> row.isInflow
            TransactionDirectionFilter.OUTFLOW -> !row.isInflow
        }
        if (!directionMatch) return false
        val channelMatch = when (f.saleChannel) {
            SaleChannelFilter.ALL -> true
            SaleChannelFilter.WHOLESALE -> kind == "INVOICE" && !row.isRetailSale
            SaleChannelFilter.RETAIL -> kind == "INVOICE" && row.isRetailSale
        }
        if (!channelMatch) return false
        val due = row.secondaryAmountMinor ?: 0
        val payment = when (f.paymentStatus) {
            PaymentStatusFilter.ALL -> true
            PaymentStatusFilter.PAID -> kind in setOf("PAYMENT", "SUPPLIER_PAYMENT") || (kind in setOf("INVOICE", "PURCHASE") && due <= 0)
            PaymentStatusFilter.UNPAID -> kind in setOf("INVOICE", "PURCHASE") && due >= row.amountMinor && due > 0
            PaymentStatusFilter.PARTIAL_PAID -> kind in setOf("INVOICE", "PURCHASE") && due in 1 until row.amountMinor
            PaymentStatusFilter.OVERDUE -> kind in setOf("INVOICE", "PURCHASE") && due > 0 && row.dueDate?.let { it < now } == true
            PaymentStatusFilter.ADVANCE -> kind == "PAYMENT" && due > 0 || kind == "SUPPLIER_PAYMENT" && row.notes.orEmpty().contains("advance", true)
            PaymentStatusFilter.CASH -> method.contains("CASH")
            PaymentStatusFilter.BANK -> method.contains("BANK") || method in setOf("TT", "LETTER_OF_CREDIT")
            PaymentStatusFilter.MOBILE_BANKING -> method in setOf("BKASH", "NAGAD", "ROCKET", "MOBILE_BANKING")
            PaymentStatusFilter.LC -> method.contains("LC") || method.contains("LETTER_OF_CREDIT")
            PaymentStatusFilter.TT -> method.contains("TT") || method.contains("TELEGRAPHIC")
        }
        if (!payment) return false
        val financial = when (f.financialMetric) {
            FinancialMetricFilter.ALL, FinancialMetricFilter.OPENING_BALANCE, FinancialMetricFilter.CLOSING_BALANCE -> true
            FinancialMetricFilter.INCOME -> kind in setOf("INVOICE", "RETURN")
            FinancialMetricFilter.EXPENSE -> kind == "EXPENSE"
            FinancialMetricFilter.PROFIT -> kind == "INVOICE" && row.profitMinor?.let { it > 0 } == true
            FinancialMetricFilter.LOSS -> kind == "INVOICE" && row.profitMinor?.let { it < 0 } == true
            FinancialMetricFilter.CUSTOMER_DUE -> kind == "INVOICE" && due > 0
            FinancialMetricFilter.SUPPLIER_DUE -> kind == "PURCHASE" && due > 0
            FinancialMetricFilter.DISCOUNT -> row.discountMinor > 0
            FinancialMetricFilter.TAX -> row.taxMinor > 0
            FinancialMetricFilter.REFUND -> kind == "RETURN" && method != "CUSTOMER_CREDIT"
        }
        if (!financial) return false
        val delivery = when (f.deliveryReturn) {
            DeliveryReturnFilter.ALL -> true
            DeliveryReturnFilter.PENDING -> row.status.contains("PENDING")
            DeliveryReturnFilter.PROCESSING -> row.status.contains("PROCESSING")
            DeliveryReturnFilter.DISPATCHED -> row.status.contains("DISPATCH")
            DeliveryReturnFilter.DELIVERED -> row.status == "DELIVERED"
            DeliveryReturnFilter.PARTIAL_DELIVERED -> row.status == "PARTIALLY_DELIVERED"
            DeliveryReturnFilter.CANCELLED -> row.status == "CANCELLED" || row.rawType.endsWith("_REVERSAL")
            DeliveryReturnFilter.ALL_RETURNS -> kind == "RETURN"
            DeliveryReturnFilter.APPROVED_RETURN -> kind == "RETURN" && row.accountingPosted
            DeliveryReturnFilter.PENDING_RETURN -> kind == "RETURN" && !row.accountingPosted
            DeliveryReturnFilter.REJECTED_RETURN -> row.status.contains("REJECT")
            DeliveryReturnFilter.DAMAGED_PRODUCT -> row.particulars.contains("DAMAGE", true)
            DeliveryReturnFilter.GOOD_CONDITION -> kind == "RETURN" && !row.particulars.contains("DAMAGE", true)
        }
        if (!delivery) return false
        val amount = when (f.amountFilter) {
            AmountRangeFilter.ALL -> true
            AmountRangeFilter.RANGE_0_1K -> row.amountMinor in 0..100000L
            AmountRangeFilter.RANGE_1K_10K -> row.amountMinor in 100001..1000000L
            AmountRangeFilter.RANGE_10K_50K -> row.amountMinor in 1000001..5000000L
            AmountRangeFilter.ABOVE_50K -> row.amountMinor > 5000000L
            AmountRangeFilter.CUSTOM -> row.amountMinor in (f.customMinAmountMinor ?: 0)..(f.customMaxAmountMinor ?: Long.MAX_VALUE)
        }
        if (!amount) return false
        val q = f.searchQuery.trim()
        return q.isEmpty() || listOf(
            row.referenceNumber,
            row.primaryEntityName,
            row.particulars,
            row.notes.orEmpty(),
            row.warehouseName.orEmpty(),
            row.paymentMethod.orEmpty(),
            row.status
        ).any { it.contains(q, true) }
    }
}
