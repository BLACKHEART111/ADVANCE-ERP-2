package com.pocketdev.advance_erp.core.util

import android.content.Context
import com.pocketdev.advance_erp.core.model.CurrencyCode
import com.pocketdev.advance_erp.core.pdf.CentralizedPdfEngine
import com.pocketdev.advance_erp.core.pdf.CentralizedPdfReport
import com.pocketdev.advance_erp.core.pdf.PdfKpiBadge
import com.pocketdev.advance_erp.core.pdf.PdfReportColumn
import com.pocketdev.advance_erp.core.pdf.PdfReportRow
import com.pocketdev.advance_erp.core.pdf.PdfReportSection
import com.pocketdev.advance_erp.core.security.ImportVaultAccessGuard
import com.pocketdev.advance_erp.core.security.UserRole
import com.pocketdev.advance_erp.domain.model.StockAgingBucket
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generates Bangla-capable, UI-reconciled PDF reports for Import Vault scopes via CentralizedPdfEngine.
 * buildCentralizedPdfReport() is pure Kotlin so JVM unit tests verify all metrics, assumptions, and warnings.
 */
object ImportVaultPdfGenerator {

    fun buildCentralizedPdfReport(
        pkg: VaultExportPackage,
        role: UserRole = UserRole.OWNER_ADMIN
    ): CentralizedPdfReport {
        ImportVaultAccessGuard.requireVaultAccess(role)
        val lang = pkg.language
        val isBn = pkg.isBangla
        val dateOnlyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        fun money(minor: Long): String = Formatters.formatMoney(minor, CurrencyCode.BDT, lang)

        val t = pkg.totals
        val scopeMetaTitleEn = pkg.titleEn.take(28)
        val scopeMetaTitleBn = pkg.titleBn.take(28)

        val kpiBadges = listOf(
            PdfKpiBadge("Total Invested", "মোট বিনিয়োগ", money(t.totalInvestedCapitalMinor)),
            PdfKpiBadge("Net Revenue", "নিট বিক্রয় আয়", money(t.netSalesRevenueMinor)),
            PdfKpiBadge(t.profitLabel, t.profitLabel, money(t.attributedNetProfitMinor)),
            PdfKpiBadge("Locked Stock", "বাকি স্টক মূল্য", money(t.lockedInventoryCapitalMinor))
        )

        val sections = mutableListOf<PdfReportSection>()

        // Section 1: Explicit Scope, Filters, As-Of, Currency, Assumptions & Warnings (visibly rendered on Page 1)
        val metaRows = mutableListOf(
            PdfReportRow(listOf(if (isBn) "রিপোর্ট শিরোনাম" else "Report Title", if (isBn) pkg.titleBn else pkg.titleEn), isEmphasized = true),
            PdfReportRow(listOf(if (isBn) "স্কোপ ও রেফারেন্স" else "Scope & Ref", "${pkg.scope.name} · ${pkg.scopeReference}")),
            PdfReportRow(listOf(if (isBn) "অ্যাস-অফ তারিখ ও মুদ্রা" else "As-Of Date & Currency", "${dateOnlyFmt.format(Date(pkg.asOfMillis))} · ${pkg.currencyCode}")),
            PdfReportRow(listOf(if (isBn) "সক্রিয় ফিল্টার" else "Applied Filters", pkg.filterSummary)),
            PdfReportRow(listOf(if (isBn) "খরচ চূড়ান্তকরণ" else "Cost Finalization", t.costFinalizationStatusLabel))
        )
        pkg.assumptions.forEachIndexed { idx, a ->
            metaRows += PdfReportRow(listOf(if (isBn) "অনুমান ${idx + 1}" else "Assumption ${idx + 1}", a))
        }
        pkg.warnings.forEachIndexed { idx, w ->
            metaRows += PdfReportRow(listOf(if (isBn) "সতর্কতা ${idx + 1}" else "Warning ${idx + 1}", w), isEmphasized = true)
        }
        sections += PdfReportSection(
            titleEn = "Scope, Filters, Assumptions & Warnings",
            titleBn = "স্কোপ, ফিল্টার, অনুমান ও সতর্কতা",
            columns = listOf(
                PdfReportColumn("Attribute", "বিবরণ", widthWeight = 1.1f),
                PdfReportColumn("Value / Policy", "মান / নীতিমালা", widthWeight = 2.9f)
            ),
            rows = metaRows
        )

        // Section 2: Financial & Capital Reconciliation + Stock Aging
        val finRows = mutableListOf(
            PdfReportRow(listOf(if (isBn) "মোট বিনিয়োগকৃত মূলধন" else "Total Invested Capital", money(t.totalInvestedCapitalMinor))),
            PdfReportRow(listOf(if (isBn) "বাকি স্টকে আটকা মূলধন" else "Locked Stock Capital", money(t.lockedInventoryCapitalMinor))),
            PdfReportRow(listOf(if (isBn) "অ্যাট্রিবউটেড নিট আয়" else "Attributed Net Revenue", money(t.netSalesRevenueMinor))),
            PdfReportRow(listOf(if (isBn) "অ্যাট্রিবউটেড COGS" else "Attributed COGS", money(t.cogsMinor))),
            PdfReportRow(listOf(if (isBn) "গ্রস মুনাফা" else "Attributed Gross Profit", money(t.grossProfitMinor)), isEmphasized = true),
            PdfReportRow(listOf(if (isBn) "সরাসরি অপারেটিং খরচ" else "Direct Attributed Expenses", money(t.directAttributedExpensesMinor))),
            PdfReportRow(listOf(if (isBn) "বন্টিত শেয়ার্ড ওভারহেড" else "Allocated Shared Overhead", money(t.allocatedSharedOverheadMinor))),
            PdfReportRow(listOf(if (isBn) "ইনভেন্টরি ক্ষতি / ঘাটতি" else "Inventory Losses / Damage", money(t.inventoryLossesMinor))),
            PdfReportRow(listOf(if (isBn) "অ্যাট্রিবউটেড ক্যাশ কালেকশন" else "Attributable Cash Collected", money(t.attributableCashCollectedMinor))),
            PdfReportRow(listOf(if (isBn) "বাকি পাওনা (Receivable)" else "Attributable Receivable", money(t.attributableReceivableMinor))),
            PdfReportRow(listOf(if (isBn) "অপুনরুদ্ধার মূলধন" else "Unrecovered Capital", money(t.unrecoveredCapitalMinor))),
            PdfReportRow(listOf(if (isBn) "অবন্টিত শেয়ার্ড ওভারহেড" else "Unallocated Shared Overhead", money(t.unallocatedSharedOverheadMinor))),
            PdfReportRow(listOf(if (isBn) "সম্ভাব্য অবশিষ্ট গ্রস লাভ" else "Expected Remaining Gross Profit", "${money(t.expectedRemainingGrossProfitMinor)} (${t.priceBasisLabel})"))
        )
        StockAgingBucket.entries.forEach { bucket ->
            val q = pkg.agingSummary.bucketQuantities[bucket] ?: 0
            val v = pkg.agingSummary.bucketValuesMinor[bucket] ?: 0L
            finRows += PdfReportRow(
                listOf(
                    if (isBn) "স্টক এজিং (${bucket.labelBn})" else "Stock Aging (${bucket.labelEn})",
                    "$q qty · ${money(v)}"
                )
            )
        }
        sections += PdfReportSection(
            titleEn = "Financial, Capital & Aging Reconciliation",
            titleBn = "আর্থিক, মূলধন ও এজিং সমন্বয়",
            columns = listOf(
                PdfReportColumn("Financial Line Item", "হিসাবের খাত", widthWeight = 2.4f),
                PdfReportColumn("Amount / Summary", "টাকা / তথ্য", widthWeight = 1.6f, isRightAligned = true)
            ),
            rows = finRows,
            summaryRows = listOf(
                PdfReportRow(listOf(t.profitLabel, money(t.attributedNetProfitMinor)), isSummary = true, isEmphasized = true)
            )
        )

        // Section 3: Products & Variants Breakdown
        if (pkg.variants.isNotEmpty() || pkg.products.isNotEmpty()) {
            val itemRows = mutableListOf<PdfReportRow>()
            if (pkg.variants.isNotEmpty()) {
                pkg.variants.take(35).forEach { (prod, v) ->
                    val name = if (isBn) "${prod.productNameBn} (${v.variantLabel})" else "${prod.productNameEn} (${v.variantLabel})"
                    itemRows += PdfReportRow(
                        listOf(
                            name,
                            "${v.netSoldQty} / ${v.remainingGoodQty}",
                            money(v.netSalesRevenueMinor),
                            money(v.cogsMinor),
                            money(v.attributedNetProfitMinor)
                        )
                    )
                }
            } else {
                pkg.products.take(35).forEach { prod ->
                    val name = if (isBn) prod.productNameBn else prod.productNameEn
                    itemRows += PdfReportRow(
                        listOf(
                            name,
                            "${prod.netSoldQty} / ${prod.remainingGoodQty}",
                            money(prod.netSalesRevenueMinor),
                            money(prod.cogsMinor),
                            money(prod.attributedNetProfitMinor)
                        )
                    )
                }
            }
            sections += PdfReportSection(
                titleEn = "Products & Variants Performance",
                titleBn = "প্রোডাক্ট ও ভ্যারিয়েন্ট ফলাফল",
                columns = listOf(
                    PdfReportColumn("Product / Variant", "পণ্য / ভ্যারিয়েন্ট", widthWeight = 2.0f),
                    PdfReportColumn("Sold/Rem", "বিক্রয়/বাকি", widthWeight = 0.8f, isRightAligned = true),
                    PdfReportColumn("Revenue", "আয়", widthWeight = 1.1f, isRightAligned = true),
                    PdfReportColumn("COGS", "খরচ", widthWeight = 1.1f, isRightAligned = true),
                    PdfReportColumn("Net Profit", "নিট লাভ", widthWeight = 1.1f, isRightAligned = true)
                ),
                rows = itemRows
            )
        }

        // Section 4: Expenses, Receivables & Documents Register
        val registerRows = mutableListOf<PdfReportRow>()
        pkg.expenses.take(20).forEach { exp ->
            registerRows += PdfReportRow(
                listOf("EXP · ${exp.expenseType}", "${exp.referenceNumber} · ${exp.description}", money(exp.amountBdtMinor))
            )
        }
        pkg.receivables.take(15).forEach { rec ->
            registerRows += PdfReportRow(
                listOf("DUE · ${rec.invoiceNumber}", "${rec.customerName} (${rec.ageDays}d)", money(rec.receivableMinor))
            )
        }
        pkg.documentsIndex.take(20).forEach { doc ->
            val tag = when {
                doc.isLiveRegeneratedReport -> "LIVE"
                doc.isImmutableSnapshot -> "SNAPSHOT"
                doc.attachmentId != null -> "FILE"
                else -> doc.category.name
            }
            registerRows += PdfReportRow(
                listOf(
                    "DOC · $tag",
                    if (isBn) doc.titleBn else doc.titleEn,
                    doc.amountMinor?.let { money(it) } ?: doc.referenceNumber
                )
            )
        }
        if (registerRows.isNotEmpty()) {
            sections += PdfReportSection(
                titleEn = "Expenses, Receivables & Documents Index",
                titleBn = "খরচ, পাওনা ও ডকুমেন্ট তালিকা",
                columns = listOf(
                    PdfReportColumn("Type", "ধরন", widthWeight = 1.2f),
                    PdfReportColumn("Reference & Description", "রেফারেন্স ও বিবরণ", widthWeight = 2.6f),
                    PdfReportColumn("Value / Ref", "মান / রেফ", widthWeight = 1.2f, isRightAligned = true)
                ),
                rows = registerRows
            )
        }

        val slug = ControlCenterExcelGenerator.sanitizeExportFileSlug(
            pkg.scopeReference,
            pkg.scope.name.lowercase(Locale.US)
        )
        return CentralizedPdfReport(
            reportId = "vault_${pkg.scope.name.lowercase(Locale.US)}_$slug",
            category = "Import Vault",
            titleEn = scopeMetaTitleEn,
            titleBn = scopeMetaTitleBn,
            dateRangeText = "As-Of: ${dateOnlyFmt.format(Date(pkg.asOfMillis))}",
            filterText = pkg.scopeReference,
            kpiBadges = kpiBadges,
            sections = sections,
            additionalNotes = (pkg.assumptions + pkg.warnings).joinToString(" | "),
            isBangla = isBn
        )
    }

    fun generatePdfFile(
        context: Context,
        pkg: VaultExportPackage,
        role: UserRole = UserRole.OWNER_ADMIN
    ): File {
        ImportVaultAccessGuard.requireVaultAccess(role)
        val report = buildCentralizedPdfReport(pkg, role)
        val rawEnginePdf = CentralizedPdfEngine.generatePdf(context, report, pkg.businessProfile)
        val exportDir = ImportVaultAttachmentFileStore.resolveExportCacheDir(context.cacheDir)
        val finalFile = File(exportDir, rawEnginePdf.name)
        if (rawEnginePdf.canonicalFile != finalFile.canonicalFile) {
            FileInputStream(rawEnginePdf).use { input ->
                FileOutputStream(finalFile).use { output ->
                    input.copyTo(output)
                }
            }
            rawEnginePdf.delete()
        }
        return finalFile
    }
}
