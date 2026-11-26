package com.pocketdev.advance_erp.feature.importvault

import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.model.CurrencyCode
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.domain.model.ProductVaultRow
import com.pocketdev.advance_erp.domain.model.PurchaseOrderVaultSummary
import com.pocketdev.advance_erp.domain.model.QuantityByUnit
import com.pocketdev.advance_erp.domain.model.ShipmentVaultSummary
import com.pocketdev.advance_erp.domain.model.VariantVaultRow
import java.util.Calendar
import java.util.Locale

enum class ContributionChartMode {
    DONUT,
    DIVERGING_BARS,
    LOSS_BASE_BARS,
    ZERO_BASE_NA
}

data class ContributionSlice(
    val id: String,
    val label: String,
    val amountMinor: Long,
    val percent: Double?,
    val marginPercent: Double?
)

enum class VaultShelfMode {
    SHIPMENTS,
    PURCHASE_ORDERS
}

enum class VaultLayoutMode {
    SHELF_GRID,
    COMPACT_LIST
}

enum class VaultArchiveFilter {
    ACTIVE,
    ARCHIVED,
    ALL
}

enum class VaultDatePreset {
    LIFETIME,
    TODAY,
    THIS_MONTH,
    LAST_30_DAYS,
    LAST_90_DAYS,
    CUSTOM_RANGE,
    CUSTOM_AS_OF
}

enum class ProfitSortOption {
    HIGHEST_PROFIT,
    LOWEST_PROFIT,
    LOSS_MAKING_FIRST,
    HIGHEST_MARGIN,
    HIGHEST_ROI,
    HIGHEST_CONTRIBUTION,
    HIGHEST_SELL_THROUGH,
    FASTEST_SELLING,
    SLOWEST_SELLING,
    HIGHEST_LOCKED_CAPITAL
}

data class ShelfMonthGroup<T>(
    val yearMonthKey: String,
    val year: Int,
    val monthZeroBased: Int,
    val items: List<T>,
    val totalInvestmentMinor: Long,
    val totalNetProfitMinor: Long
)

object ImportVaultChartPolicy {

    /**
     * Spec §5 & §14 chart rule:
     * - Zero parent profit -> ZERO_BASE_NA (N/A)
     * - Negative parent profit -> LOSS_BASE_BARS (never a misleading pie chart)
     * - Positive parent profit with any negative contributor or zero positive slices -> DIVERGING_BARS
     * - Positive parent profit with strictly nonnegative components and >=1 positive slice -> DONUT
     */
    fun resolveContributionChartMode(
        slices: List<ContributionSlice>,
        parentProfitMinor: Long
    ): ContributionChartMode {
        if (parentProfitMinor == 0L) return ContributionChartMode.ZERO_BASE_NA
        if (parentProfitMinor < 0L) return ContributionChartMode.LOSS_BASE_BARS
        if (slices.isEmpty() || slices.any { it.amountMinor < 0L } || slices.none { it.amountMinor > 0L }) {
            return ContributionChartMode.DIVERGING_BARS
        }
        return ContributionChartMode.DONUT
    }

    /**
     * Ensures Donut slices reconcile to positive [parentProfitMinor] by adding an explicit
     * unallocated residual slice when component sum is strictly below parent profit.
     */
    fun buildDonutDisplaySlices(
        slices: List<ContributionSlice>,
        parentProfitMinor: Long,
        residualLabel: String = "Unallocated / Residual"
    ): List<ContributionSlice> {
        if (resolveContributionChartMode(slices, parentProfitMinor) != ContributionChartMode.DONUT) {
            return emptyList()
        }
        val cleanPositive = slices.filter { it.amountMinor > 0L }
        val sum = cleanPositive.sumOf { it.amountMinor }
        val residual = parentProfitMinor - sum
        return if (residual > 0L) {
            cleanPositive + ContributionSlice(
                id = "__residual__",
                label = residualLabel,
                amountMinor = residual,
                percent = (residual.toDouble() / parentProfitMinor.toDouble()) * 100.0,
                marginPercent = null
            )
        } else {
            cleanPositive
        }
    }
}

object ImportVaultUiLogic {

    private val BANGLA_MONTHS = arrayOf(
        "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
        "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
    )

    private val ENGLISH_MONTHS = arrayOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    fun groupShipmentsByYearMonth(
        items: List<ShipmentVaultSummary>,
        archiveFilter: VaultArchiveFilter
    ): Pair<List<ShipmentVaultSummary>, List<ShelfMonthGroup<ShipmentVaultSummary>>> {
        val filtered = items.filter {
            when (archiveFilter) {
                VaultArchiveFilter.ACTIVE -> it.archiveStatus != "ARCHIVED"
                VaultArchiveFilter.ARCHIVED -> it.archiveStatus == "ARCHIVED"
                VaultArchiveFilter.ALL -> true
            }
        }
        val pinned = filtered.filter { it.isPinned }.sortedByDescending { it.shippingDate }
        val cal = Calendar.getInstance()
        val grouped = filtered.sortedByDescending { it.shippingDate }
            .groupBy { item ->
                cal.timeInMillis = item.shippingDate
                val y = cal.get(Calendar.YEAR)
                val m = cal.get(Calendar.MONTH)
                "%04d-%02d".format(Locale.US, y, m + 1)
            }
            .entries
            .sortedByDescending { it.key }
            .map { (key, list) ->
                cal.timeInMillis = list.first().shippingDate
                ShelfMonthGroup(
                    yearMonthKey = key,
                    year = cal.get(Calendar.YEAR),
                    monthZeroBased = cal.get(Calendar.MONTH),
                    items = list,
                    totalInvestmentMinor = list.sumOf { it.totalInvestedCapitalMinor },
                    totalNetProfitMinor = list.sumOf { it.attributedNetProfitMinor }
                )
            }
        return pinned to grouped
    }

    fun groupPurchaseOrdersByYearMonth(
        items: List<PurchaseOrderVaultSummary>,
        archiveFilter: VaultArchiveFilter
    ): Pair<List<PurchaseOrderVaultSummary>, List<ShelfMonthGroup<PurchaseOrderVaultSummary>>> {
        val filtered = items.filter {
            when (archiveFilter) {
                VaultArchiveFilter.ACTIVE -> it.archiveStatus != "ARCHIVED"
                VaultArchiveFilter.ARCHIVED -> it.archiveStatus == "ARCHIVED"
                VaultArchiveFilter.ALL -> true
            }
        }
        val pinned = filtered.filter { it.isPinned }.sortedByDescending { it.orderDate }
        val cal = Calendar.getInstance()
        val grouped = filtered.sortedByDescending { it.orderDate }
            .groupBy { item ->
                cal.timeInMillis = item.orderDate
                val y = cal.get(Calendar.YEAR)
                val m = cal.get(Calendar.MONTH)
                "%04d-%02d".format(Locale.US, y, m + 1)
            }
            .entries
            .sortedByDescending { it.key }
            .map { (key, list) ->
                cal.timeInMillis = list.first().orderDate
                ShelfMonthGroup(
                    yearMonthKey = key,
                    year = cal.get(Calendar.YEAR),
                    monthZeroBased = cal.get(Calendar.MONTH),
                    items = list,
                    totalInvestmentMinor = list.sumOf { it.totalInvestedCapitalMinor },
                    totalNetProfitMinor = list.sumOf { it.attributedNetProfitMinor }
                )
            }
        return pinned to grouped
    }

    fun sortProducts(rows: List<ProductVaultRow>, option: ProfitSortOption): List<ProductVaultRow> {
        return when (option) {
            ProfitSortOption.HIGHEST_PROFIT -> rows.sortedByDescending { it.attributedNetProfitMinor }
            ProfitSortOption.LOWEST_PROFIT -> rows.sortedBy { it.attributedNetProfitMinor }
            ProfitSortOption.LOSS_MAKING_FIRST -> rows.sortedWith(
                compareBy<ProductVaultRow> { if (it.attributedNetProfitMinor < 0L) 0 else 1 }
                    .thenBy { it.attributedNetProfitMinor }
            )
            ProfitSortOption.HIGHEST_MARGIN -> rows.sortedWith(
                compareByDescending<ProductVaultRow> { it.netMarginPercent ?: Double.NEGATIVE_INFINITY }
                    .thenByDescending { it.attributedNetProfitMinor }
            )
            ProfitSortOption.HIGHEST_ROI -> rows.sortedWith(
                compareByDescending<ProductVaultRow> { it.roiPercent ?: Double.NEGATIVE_INFINITY }
                    .thenByDescending { it.attributedNetProfitMinor }
            )
            ProfitSortOption.HIGHEST_CONTRIBUTION -> rows.sortedWith(
                compareByDescending<ProductVaultRow> {
                    it.profitContributionToParentScope?.contributionPercent ?: Double.NEGATIVE_INFINITY
                }.thenByDescending { it.attributedNetProfitMinor }
            )
            ProfitSortOption.HIGHEST_SELL_THROUGH -> rows.sortedWith(
                compareByDescending<ProductVaultRow> { it.sellThroughPercent ?: Double.NEGATIVE_INFINITY }
                    .thenByDescending { it.attributedNetProfitMinor }
            )
            ProfitSortOption.FASTEST_SELLING -> rows.sortedWith(
                compareByDescending<ProductVaultRow> { it.sellingVelocityPerDay ?: Double.NEGATIVE_INFINITY }
                    .thenByDescending { it.netSoldQty }
            )
            ProfitSortOption.SLOWEST_SELLING -> rows.sortedWith(
                compareBy<ProductVaultRow> { it.sellingVelocityPerDay ?: Double.POSITIVE_INFINITY }
                    .thenByDescending { it.lockedInventoryCostMinor }
            )
            ProfitSortOption.HIGHEST_LOCKED_CAPITAL -> rows.sortedByDescending { it.lockedInventoryCostMinor }
        }
    }

    fun sortVariants(rows: List<VariantVaultRow>, option: ProfitSortOption): List<VariantVaultRow> {
        return when (option) {
            ProfitSortOption.HIGHEST_PROFIT -> rows.sortedByDescending { it.attributedNetProfitMinor }
            ProfitSortOption.LOWEST_PROFIT -> rows.sortedBy { it.attributedNetProfitMinor }
            ProfitSortOption.LOSS_MAKING_FIRST -> rows.sortedWith(
                compareBy<VariantVaultRow> { if (it.attributedNetProfitMinor < 0L) 0 else 1 }
                    .thenBy { it.attributedNetProfitMinor }
            )
            ProfitSortOption.HIGHEST_MARGIN -> rows.sortedWith(
                compareByDescending<VariantVaultRow> { it.netMarginPercent ?: Double.NEGATIVE_INFINITY }
                    .thenByDescending { it.attributedNetProfitMinor }
            )
            ProfitSortOption.HIGHEST_ROI -> rows.sortedWith(
                compareByDescending<VariantVaultRow> { it.roiPercent ?: Double.NEGATIVE_INFINITY }
                    .thenByDescending { it.attributedNetProfitMinor }
            )
            ProfitSortOption.HIGHEST_CONTRIBUTION -> rows.sortedWith(
                compareByDescending<VariantVaultRow> {
                    it.profitContributionToParentScope?.contributionPercent
                        ?: it.profitContributionToProduct?.contributionPercent
                        ?: Double.NEGATIVE_INFINITY
                }.thenByDescending { it.attributedNetProfitMinor }
            )
            ProfitSortOption.HIGHEST_SELL_THROUGH -> rows.sortedWith(
                compareByDescending<VariantVaultRow> { it.sellThroughPercent ?: Double.NEGATIVE_INFINITY }
                    .thenByDescending { it.attributedNetProfitMinor }
            )
            ProfitSortOption.FASTEST_SELLING -> rows.sortedWith(
                compareByDescending<VariantVaultRow> { it.sellingVelocityPerDay ?: Double.NEGATIVE_INFINITY }
                    .thenByDescending { it.netSoldQty }
            )
            ProfitSortOption.SLOWEST_SELLING -> rows.sortedWith(
                compareBy<VariantVaultRow> { it.sellingVelocityPerDay ?: Double.POSITIVE_INFINITY }
                    .thenByDescending { it.lockedInventoryCostMinor }
            )
            ProfitSortOption.HIGHEST_LOCKED_CAPITAL -> rows.sortedByDescending { it.lockedInventoryCostMinor }
        }
    }

    fun formatQuantityByUnit(qty: QuantityByUnit, language: AppLanguage): String {
        if (qty.byUom.isEmpty()) {
            return if (language == AppLanguage.BANGLA) "০" else "0"
        }
        val active = qty.byUom.filterValues { it != 0 }.ifEmpty { qty.byUom }
        return active.entries.joinToString(" + ") { (uom, count) ->
            "${Formatters.formatNumber(count.toLong(), language)} $uom"
        }
    }

    fun formatPercent(percent: Double?, language: AppLanguage): String {
        if (percent == null || percent.isNaN() || percent.isInfinite()) {
            return if (language == AppLanguage.BANGLA) "প্রযোজ্য নয়" else "N/A"
        }
        val raw = String.format(Locale.US, "%.2f%%", percent)
        return if (language == AppLanguage.BANGLA) Formatters.toBanglaDigits(raw) else raw
    }

    fun formatPercentCompact(percent: Double?, language: AppLanguage): String {
        if (percent == null || percent.isNaN() || percent.isInfinite()) return "—"
        if (kotlin.math.abs(percent) < 0.005) {
            return if (language == AppLanguage.BANGLA) "০%" else "0%"
        }
        val raw = String.format(Locale.US, "%.1f%%", percent)
        return if (language == AppLanguage.BANGLA) Formatters.toBanglaDigits(raw) else raw
    }

    fun formatVelocity(velocityPerDay: Double?, uom: String, language: AppLanguage): String {
        if (velocityPerDay == null || velocityPerDay.isNaN() || velocityPerDay.isInfinite()) {
            return if (language == AppLanguage.BANGLA) "প্রযোজ্য নয়" else "N/A"
        }
        val raw = String.format(Locale.US, "%.2f", velocityPerDay)
        val digits = if (language == AppLanguage.BANGLA) Formatters.toBanglaDigits(raw) else raw
        val unitLabel = if (language == AppLanguage.BANGLA) "$uom/দিন" else "$uom/day"
        return "$digits $unitLabel"
    }

    fun formatYearMonthHeader(year: Int, monthZeroBased: Int, language: AppLanguage): String {
        val mIdx = monthZeroBased.coerceIn(0, 11)
        return if (language == AppLanguage.BANGLA) {
            "${BANGLA_MONTHS[mIdx]} ${Formatters.toBanglaDigits(year.toString())}"
        } else {
            "${ENGLISH_MONTHS[mIdx]} $year"
        }
    }

    fun formatBdt(minor: Long, language: AppLanguage): String =
        Formatters.formatMoney(minor, CurrencyCode.BDT, language)

    fun formatCurrency(minor: Long, currencyCode: String, language: AppLanguage): String =
        Formatters.formatMoney(minor, CurrencyCode.fromCode(currencyCode), language)

    fun parseTags(tagsJson: String?): List<String> {
        val raw = tagsJson?.trim().orEmpty()
        if (raw.isEmpty()) return emptyList()
        val inner = if (raw.startsWith("[") && raw.endsWith("]")) raw.substring(1, raw.length - 1) else raw
        return inner.split(",")
            .map { it.trim().removePrefix("\"").removeSuffix("\"").trim() }
            .filter { it.isNotEmpty() }
            .distinct()
    }

    fun encodeTags(tags: List<String>): String {
        val clean = tags.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (clean.isEmpty()) return ""
        return clean.joinToString(prefix = "[", postfix = "]", separator = ",") { tag ->
            val escaped = tag.replace("\\", "\\\\").replace("\"", "\\\"")
            "\"$escaped\""
        }
    }

    fun startOfDayMillis(timeMillis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun endOfDayMillis(timeMillis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timeMillis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }

    /** Returns Triple(dateFromMillis, dateToMillis, asOfMillis). */
    fun computeDateRangeForPreset(
        preset: VaultDatePreset,
        customFromMillis: Long? = null,
        customToMillis: Long? = null,
        customAsOfMillis: Long? = null,
        nowMillis: Long = System.currentTimeMillis()
    ): Triple<Long?, Long?, Long?> {
        return when (preset) {
            VaultDatePreset.LIFETIME -> Triple(null, null, null)
            VaultDatePreset.TODAY -> Triple(startOfDayMillis(nowMillis), nowMillis, nowMillis)
            VaultDatePreset.THIS_MONTH -> {
                val cal = Calendar.getInstance().apply {
                    timeInMillis = nowMillis
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                Triple(cal.timeInMillis, nowMillis, nowMillis)
            }
            VaultDatePreset.LAST_30_DAYS -> Triple(nowMillis - 30L * 24L * 3600L * 1000L, nowMillis, nowMillis)
            VaultDatePreset.LAST_90_DAYS -> Triple(nowMillis - 90L * 24L * 3600L * 1000L, nowMillis, nowMillis)
            VaultDatePreset.CUSTOM_RANGE -> {
                val rawFrom = customFromMillis ?: (nowMillis - 7L * 24L * 3600L * 1000L)
                val rawTo = customToMillis ?: nowMillis
                val from = startOfDayMillis(minOf(rawFrom, rawTo))
                val to = minOf(nowMillis, endOfDayMillis(maxOf(rawFrom, rawTo)))
                Triple(from, to, to)
            }
            VaultDatePreset.CUSTOM_AS_OF -> Triple(null, null, customAsOfMillis ?: nowMillis)
        }
    }

    fun computeDateBoundsForPreset(
        preset: VaultDatePreset,
        customAsOfMillis: Long? = null,
        nowMillis: Long = System.currentTimeMillis()
    ): Pair<Long?, Long?> {
        val (from, _, asOf) = computeDateRangeForPreset(
            preset = preset,
            customAsOfMillis = customAsOfMillis,
            nowMillis = nowMillis
        )
        return from to asOf
    }
}
