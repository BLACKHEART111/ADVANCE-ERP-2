package com.pocketdev.advance_erp.core.util

import com.pocketdev.advance_erp.core.engine.PoPackingEngine
import com.pocketdev.advance_erp.data.entity.BusinessProfile
import com.pocketdev.advance_erp.data.entity.Product
import com.pocketdev.advance_erp.data.entity.ProductVariant
import com.pocketdev.advance_erp.data.entity.PurchaseOrder
import com.pocketdev.advance_erp.data.entity.PurchaseOrderItem
import com.pocketdev.advance_erp.data.entity.ShipmentCarton
import com.pocketdev.advance_erp.data.entity.Supplier
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ExportCartonAllocation(
    val cartonNumber: String,
    val quantity: Int
)

data class ValidatedPoItemRow(
    val index: Int,
    val item: PurchaseOrderItem,
    val productName: String,
    val sku: String,
    val color: String,
    val size: String,
    val imagePath: String?,
    val cartonAllocations: List<ExportCartonAllocation>,
    val cartonDisplay: String,
    val quantity: Int,
    val unitPriceMinor: Long,
    val lineTotalMinor: Long
)

data class ValidatedCartonLine(
    val productId: String,
    val variantId: String?,
    val productName: String,
    val sku: String,
    val color: String,
    val size: String,
    val quantity: Int,
    val allocatedGrossWeightKg: BigDecimal,
    val allocatedNetWeightKg: BigDecimal
)

data class ValidatedCartonRow(
    val sequence: Int,
    val rawCartonNumber: String,
    val cartonNumber: String,
    val isMixed: Boolean,
    val lines: List<ValidatedCartonLine>,
    val totalQty: Int,
    val grossWeightKg: BigDecimal,
    val netWeightKg: BigDecimal,
    val lengthCm: BigDecimal,
    val widthCm: BigDecimal,
    val heightCm: BigDecimal,
    val dimensionsDisplay: String,
    val cbm: BigDecimal,
    val shippingMark: String,
    val isCustomShippingMark: Boolean
)

data class ValidatedSupplierPoExport(
    val companyName: String,
    val companyAddress: String,
    val companyPhone: String,
    val companyBin: String,
    val supplierName: String,
    val supplierContactPerson: String,
    val supplierPhone: String,
    val supplierAddress: String,
    val supplierCountry: String,
    val poNumber: String,
    val orderDateFormatted: String,
    val status: String,
    val currency: String,
    val isLocalOrder: Boolean,
    val paymentTerms: String,
    val deliveryAddress: String,
    val defaultShippingMark: String,
    val userNotes: String,
    val items: List<ValidatedPoItemRow>,
    val allCartons: List<ValidatedCartonRow>,
    val exportedCartons: List<ValidatedCartonRow>,
    val isPartialCartonExport: Boolean,
    val poTotalQuantity: Int,
    val poTotalCartons: Int,
    val poTotalGrossWeightKg: BigDecimal,
    val poTotalNetWeightKg: BigDecimal,
    val poTotalCbm: BigDecimal,
    val poGrandTotalMinor: Long,
    val exportedCartonCount: Int,
    val exportedPackedQuantity: Int,
    val exportedGrossWeightKg: BigDecimal,
    val exportedNetWeightKg: BigDecimal,
    val exportedCbm: BigDecimal,
    val blockingErrors: List<String>,
    val warnings: List<String>
) {
    val isValidForSupplierExport: Boolean get() = blockingErrors.isEmpty()
}

/**
 * Single source of truth used by Supplier PDF, 4-sheet Supplier Excel, CTN Labels, and Share dialog.
 * Guarantees 100% cross-format consistency and validates commercial + packing math prior to generation.
 */
object PoExportDataBuilder {

    fun build(
        order: PurchaseOrder,
        items: List<PurchaseOrderItem>,
        productsMap: Map<String, Product>,
        supplier: Supplier?,
        businessProfile: BusinessProfile?,
        variantsMap: Map<String, ProductVariant> = emptyMap(),
        itemImagePaths: Map<String, String> = emptyMap(),
        cartons: List<ShipmentCarton> = emptyList(),
        selectedCartonNumbers: Set<String>? = null
    ): ValidatedSupplierPoExport {
        val isLocalOrder = order.currency.equals("BDT", ignoreCase = true)
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)
        val defaultDeliveryAddr = if (isLocalOrder) {
            businessProfile?.address?.takeIf { it.isNotBlank() } ?: "Company Central Warehouse"
        } else {
            ""
        }
        val parsedNotes = PoNotesMetadataParser.parse(order.notes, defaultForwarder = defaultDeliveryAddr)
        val resolvedDefaultShippingMark = parsedNotes.defaultShippingMark.ifBlank {
            PoNotesMetadataParser.buildShippingMark(businessProfile?.companyName, order.poNumber, isLocalOrder)
        }
        val resolvedDeliveryAddress = parsedNotes.forwarderAddress.trim().ifBlank { "To be advised" }

        val blockingErrors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (supplier == null || supplier.companyName.isBlank()) {
            warnings += "Supplier profile details are incomplete."
        } else if (supplier.address.isBlank() && supplier.phone.isBlank()) {
            warnings += "Supplier address and phone number are missing."
        }
        if (parsedNotes.forwarderAddress.isBlank() || parsedNotes.forwarderAddress.equals("WILL BE NEXT", ignoreCase = true)) {
            warnings += "Warehouse delivery address is not finalized (${resolvedDeliveryAddress})."
        }

        // Normalize physical cartons in natural sequence -> CTN-001, CTN-002 ...
        val groupedRaw = cartons
            .groupBy { it.cartonNumber.trim().ifBlank { it.id } }
            .entries
            .sortedWith(compareBy<Map.Entry<String, List<ShipmentCarton>>> { PoPackingEngine.cartonSortKey(it.key) }.thenBy { it.key })

        val allCartons = groupedRaw.mapIndexed { idx, (rawCtnNo, rows) ->
            val seq = idx + 1
            val normalizedCtnNo = PoPackingEngine.formatCartonNumber(seq)
            val customOverride = parsedNotes.cartonShippingMarks[rawCtnNo]
                ?.takeIf { it.isNotBlank() }
                ?: parsedNotes.cartonShippingMarks[normalizedCtnNo]?.takeIf { it.isNotBlank() }
            val resolvedMark = PoPackingEngine.resolveShippingMark(
                defaultMark = resolvedDefaultShippingMark,
                individualMark = customOverride,
                cartonNumber = normalizedCtnNo
            )

            // Detect duplicate variant inside the same carton
            rows.groupBy { it.productId to it.variantId }.forEach { (key, dupRows) ->
                if (dupRows.size > 1) {
                    val pName = productsMap[key.first]?.nameEn ?: key.first
                    blockingErrors += "$normalizedCtnNo contains duplicate variant entry for $pName."
                }
            }

            val first = rows.first()
            val len = first.lengthCm.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
            val wid = first.widthCm.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
            val hei = first.heightCm.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
            val hasDims = len > BigDecimal.ZERO && wid > BigDecimal.ZERO && hei > BigDecimal.ZERO
            val dimStr = if (hasDims) {
                "${formatTrimmed(len)} × ${formatTrimmed(wid)} × ${formatTrimmed(hei)}"
            } else {
                warnings += "$normalizedCtnNo is missing carton dimensions (L×W×H)."
                "-"
            }
            val computedCbm = if (hasDims) {
                len.multiply(wid).multiply(hei).movePointLeft(6).setScale(6, RoundingMode.HALF_UP)
            } else {
                first.cbm.trim().toBigDecimalOrNull()?.coerceAtLeast(BigDecimal.ZERO) ?: BigDecimal.ZERO
            }

            val lineSumGross = rows.fold(BigDecimal.ZERO) { acc, r ->
                acc + (r.grossWeightKg.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO)
            }
            val cartonGross = first.cartonGrossWeightKg.trim().toBigDecimalOrNull()
                ?.takeIf { it > BigDecimal.ZERO }
                ?: lineSumGross
            val lineSumNet = rows.fold(BigDecimal.ZERO) { acc, r ->
                acc + (r.netWeightKg.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO)
            }
            if (cartonGross <= BigDecimal.ZERO) {
                warnings += "$normalizedCtnNo has 0 KG gross weight."
            }

            val validatedLines = rows.map { r ->
                val prod = productsMap[r.productId]
                val variant = r.variantId?.let { variantsMap[it] }
                val sku = variant?.sku?.takeIf { it.isNotBlank() } ?: prod?.baseSku?.takeIf { it.isNotBlank() } ?: "-"
                val color = variant?.color?.takeIf { it.isNotBlank() } ?: "-"
                val size = variant?.size?.takeIf { it.isNotBlank() } ?: "-"
                val lineGw = r.grossWeightKg.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
                val lineNw = r.netWeightKg.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
                if (r.quantity <= 0) {
                    blockingErrors += "$normalizedCtnNo has non-positive quantity for ${prod?.nameEn ?: r.productId}."
                }
                ValidatedCartonLine(
                    productId = r.productId,
                    variantId = r.variantId,
                    productName = prod?.nameEn ?: r.productId,
                    sku = sku,
                    color = color,
                    size = size,
                    quantity = r.quantity,
                    allocatedGrossWeightKg = lineGw,
                    allocatedNetWeightKg = lineNw
                )
            }

            val distinctVariants = rows.map { it.productId to it.variantId }.distinct().size
            ValidatedCartonRow(
                sequence = seq,
                rawCartonNumber = rawCtnNo,
                cartonNumber = normalizedCtnNo,
                isMixed = distinctVariants > 1 || rows.any { it.isMixedCarton },
                lines = validatedLines,
                totalQty = rows.sumOf { it.quantity },
                grossWeightKg = cartonGross,
                netWeightKg = lineSumNet,
                lengthCm = len,
                widthCm = wid,
                heightCm = hei,
                dimensionsDisplay = dimStr,
                cbm = computedCbm,
                shippingMark = resolvedMark,
                isCustomShippingMark = customOverride != null
            )
        }

        val isPartialCartonExport = selectedCartonNumbers != null &&
            allCartons.isNotEmpty() &&
            selectedCartonNumbers.size < allCartons.size

        val exportedCartons = if (selectedCartonNumbers == null) {
            allCartons
        } else {
            allCartons.filter { it.cartonNumber in selectedCartonNumbers || it.rawCartonNumber in selectedCartonNumbers }
        }

        if (selectedCartonNumbers != null && selectedCartonNumbers.isEmpty() && allCartons.isNotEmpty()) {
            blockingErrors += "Select at least one carton to export."
        }

        // Validate packing per PO variant against allCartons
        val orderedByVariant = items.groupBy { it.productId to it.variantId }.mapValues { (_, list) -> list.sumOf { it.quantity } }
        val packedByVariantAll = mutableMapOf<Pair<String, String?>, Int>()
        allCartons.forEach { ctn ->
            ctn.lines.forEach { line ->
                val key = line.productId to line.variantId
                packedByVariantAll[key] = (packedByVariantAll[key] ?: 0) + line.quantity
            }
        }
        orderedByVariant.forEach { (key, orderedQty) ->
            val packedQty = packedByVariantAll[key] ?: 0
            val pName = productsMap[key.first]?.nameEn ?: key.first
            val vDesc = key.second?.let { variantsMap[it] }?.let { v ->
                listOfNotNull(v.color, v.size).joinToString("/")
            }.orEmpty()
            val label = if (vDesc.isNotBlank()) "$pName ($vDesc)" else pName
            if (packedQty > orderedQty) {
                blockingErrors += "Overpacked variant: $label has $packedQty PCS packed vs $orderedQty PCS ordered."
            } else if (allCartons.isNotEmpty() && packedQty < orderedQty) {
                warnings += "$label has ${orderedQty - packedQty} PCS remaining unallocated to cartons."
            }
        }
        packedByVariantAll.keys.forEach { key ->
            if (key !in orderedByVariant) {
                val pName = productsMap[key.first]?.nameEn ?: key.first
                blockingErrors += "Carton allocation references $pName which is not in the Purchase Order."
            }
        }

        // Map (productId, variantId) -> carton allocations from exportedCartons
        val exportedAllocationsByVariant = LinkedHashMap<Pair<String, String?>, MutableList<ExportCartonAllocation>>()
        exportedCartons.forEach { ctn ->
            ctn.lines.forEach { line ->
                val key = line.productId to line.variantId
                exportedAllocationsByVariant.getOrPut(key) { mutableListOf() }
                    .add(ExportCartonAllocation(ctn.cartonNumber, line.quantity))
            }
        }

        var computedItemsTotalMinor = 0L
        val validatedItems = items.mapIndexed { idx, item ->
            val prod = productsMap[item.productId]
            val variant = item.variantId?.let { variantsMap[it] }
            val pName = prod?.nameEn ?: item.productId
            val sku = variant?.sku?.takeIf { it.isNotBlank() } ?: prod?.baseSku?.takeIf { it.isNotBlank() } ?: "-"
            val color = variant?.color?.takeIf { it.isNotBlank() } ?: "-"
            val size = variant?.size?.takeIf { it.isNotBlank() } ?: "-"
            val key = item.productId to item.variantId

            if (item.quantity <= 0) blockingErrors += "Item #${idx + 1} ($pName) has invalid quantity ${item.quantity}."
            if (item.unitPriceMinor <= 0L) blockingErrors += "Item #${idx + 1} ($pName) has non-positive unit price."

            val expectedLineTotal = item.quantity.toLong() * item.unitPriceMinor
            val lineTotalMinor = if (item.totalAmountMinor > 0L) item.totalAmountMinor else expectedLineTotal
            if (item.totalAmountMinor > 0L && item.totalAmountMinor != expectedLineTotal) {
                blockingErrors += "Item #${idx + 1} ($pName) line total mismatch: expected $expectedLineTotal, stored ${item.totalAmountMinor}."
            }
            computedItemsTotalMinor += lineTotalMinor

            val allocs = exportedAllocationsByVariant[key].orEmpty()
            val ctnDisplay = when {
                allocs.size == 1 && allCartons.isNotEmpty() -> {
                    val only = allocs.first()
                    val parentCtn = exportedCartons.find { it.cartonNumber == only.cartonNumber }
                    // Check if variant total across PO is split; if partially exported or multiple cartons, show qty too
                    if ((packedByVariantAll[key] ?: 0) == item.quantity && !isPartialCartonExport) {
                        only.cartonNumber
                    } else {
                        "${only.cartonNumber} (${only.quantity})"
                    }
                }
                allocs.size > 1 -> allocs.joinToString(", ") { "${it.cartonNumber} (${it.quantity})" }
                allCartons.isNotEmpty() && isPartialCartonExport -> "-"
                item.cartonQuantity > 0 -> {
                    if (item.pcsPerCarton > 0) "${item.cartonQuantity} CTN (${item.pcsPerCarton}/ctn)"
                    else "${item.cartonQuantity} CTN"
                }
                else -> "-"
            }

            val resolvedImg = itemImagePaths[item.id]
                ?: itemImagePaths["${item.productId}:${item.variantId.orEmpty()}"]
                ?: itemImagePaths[item.productId]

            ValidatedPoItemRow(
                index = idx + 1,
                item = item,
                productName = pName,
                sku = sku,
                color = color,
                size = size,
                imagePath = resolvedImg,
                cartonAllocations = allocs,
                cartonDisplay = ctnDisplay,
                quantity = item.quantity,
                unitPriceMinor = item.unitPriceMinor,
                lineTotalMinor = lineTotalMinor
            )
        }

        val resolvedGrandTotalMinor = if (order.totalAmountMinor > 0L) order.totalAmountMinor else computedItemsTotalMinor
        if (items.isNotEmpty() && order.totalAmountMinor > 0L && order.totalAmountMinor != computedItemsTotalMinor) {
            blockingErrors += "PO Grand Total mismatch: sum of items is $computedItemsTotalMinor but PO stores ${order.totalAmountMinor}."
        }

        val poTotalQty = validatedItems.sumOf { it.quantity }
        val poTotalCartons = if (allCartons.isNotEmpty()) allCartons.size else order.totalCartons
        val poTotalGross = if (allCartons.isNotEmpty()) {
            allCartons.fold(BigDecimal.ZERO) { acc, c -> acc + c.grossWeightKg }
        } else {
            order.totalGrossWeightKg.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
        }
        val poTotalNet = if (allCartons.isNotEmpty()) {
            allCartons.fold(BigDecimal.ZERO) { acc, c -> acc + c.netWeightKg }
        } else {
            order.totalNetWeightKg.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
        }
        val poTotalCbm = if (allCartons.isNotEmpty()) {
            allCartons.fold(BigDecimal.ZERO) { acc, c -> acc + c.cbm }
        } else {
            order.totalCbm.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
        }

        val expCartonCount = exportedCartons.size
        val expPackedQty = exportedCartons.sumOf { it.totalQty }
        val expGross = exportedCartons.fold(BigDecimal.ZERO) { acc, c -> acc + c.grossWeightKg }
        val expNet = exportedCartons.fold(BigDecimal.ZERO) { acc, c -> acc + c.netWeightKg }
        val expCbm = exportedCartons.fold(BigDecimal.ZERO) { acc, c -> acc + c.cbm }

        return ValidatedSupplierPoExport(
            companyName = businessProfile?.companyName?.takeIf { it.isNotBlank() } ?: "ADVANCE IMPORT & EXPORT ERP",
            companyAddress = businessProfile?.address?.takeIf { it.isNotBlank() } ?: "",
            companyPhone = businessProfile?.phone?.takeIf { it.isNotBlank() } ?: "",
            companyBin = businessProfile?.taxNumber?.takeIf { it.isNotBlank() } ?: "",
            supplierName = supplier?.companyName?.takeIf { it.isNotBlank() }
                ?: if (isLocalOrder) "Local Supplier" else "Supplier / Overseas Factory",
            supplierContactPerson = supplier?.contactPerson?.takeIf { it.isNotBlank() } ?: "-",
            supplierPhone = supplier?.phone?.takeIf { it.isNotBlank() } ?: "-",
            supplierAddress = listOfNotNull(
                supplier?.address?.takeIf { it.isNotBlank() },
                supplier?.country?.takeIf { it.isNotBlank() }
            ).joinToString(", ").ifBlank { "-" },
            supplierCountry = supplier?.country?.takeIf { it.isNotBlank() } ?: "",
            poNumber = order.poNumber,
            orderDateFormatted = dateFormat.format(Date(order.orderDate)),
            status = order.status,
            currency = order.currency,
            isLocalOrder = isLocalOrder,
            paymentTerms = parsedNotes.rawTermsText.ifBlank {
                if (isLocalOrder) "Cash / Bank Transfer as agreed" else "30% Advance + 70% Balance"
            },
            deliveryAddress = resolvedDeliveryAddress,
            defaultShippingMark = resolvedDefaultShippingMark,
            userNotes = parsedNotes.userNotes,
            items = validatedItems,
            allCartons = allCartons,
            exportedCartons = exportedCartons,
            isPartialCartonExport = isPartialCartonExport,
            poTotalQuantity = poTotalQty,
            poTotalCartons = poTotalCartons,
            poTotalGrossWeightKg = poTotalGross,
            poTotalNetWeightKg = poTotalNet,
            poTotalCbm = poTotalCbm,
            poGrandTotalMinor = resolvedGrandTotalMinor,
            exportedCartonCount = expCartonCount,
            exportedPackedQuantity = expPackedQty,
            exportedGrossWeightKg = expGross,
            exportedNetWeightKg = expNet,
            exportedCbm = expCbm,
            blockingErrors = blockingErrors,
            warnings = warnings
        )
    }

    private fun formatTrimmed(bd: BigDecimal): String =
        bd.stripTrailingZeros().let { if (it.scale() < 0) it.setScale(0) else it }.toPlainString()
}
