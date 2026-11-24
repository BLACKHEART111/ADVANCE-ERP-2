package com.pocketdev.advance_erp.core.util

import com.pocketdev.advance_erp.data.entity.InventoryCostLayer

/** Read-only reference price; never changes FIFO layers, COGS or product master prices. */
fun latestLandedCostMinor(layers: List<InventoryCostLayer>, productId: String, variantId: String? = null): Long? =
    layers.asSequence()
        .filter { it.productId == productId && (variantId == null || it.variantId == variantId) }
        .filter { it.initialQuantity > 0 && it.unitLandedCostBdtMinor >= 0 && (it.isActive || it.remainingQuantity == 0) }
        .maxWithOrNull(compareBy<InventoryCostLayer> { it.receiptDate }.thenBy { it.createdAt }.thenBy { it.id })
        ?.unitLandedCostBdtMinor

/**
 * Resolves the unit cost (minor BDT) for display in sales dialogs.
 * Prioritizes landed cost layers in the selected warehouse, then any warehouse,
 * matching exact variant first or unvarianted base layers before falling back to
 * variant/product master purchase prices.
 */
fun resolveUnitCostMinorForSale(
    layers: List<InventoryCostLayer>,
    productId: String,
    variantId: String?,
    warehouseId: String?,
    variantCostMinor: Long?,
    productPurchasePriceMinor: Long
): Long {
    val whLayers = if (!warehouseId.isNullOrBlank()) layers.filter { it.warehouseId == warehouseId } else emptyList()
    val landedCost = if (variantId != null) {
        latestLandedCostMinor(whLayers, productId, variantId)
            ?: latestLandedCostMinor(layers, productId, variantId)
            ?: latestLandedCostMinor(whLayers.filter { it.variantId == null }, productId, null)
            ?: latestLandedCostMinor(layers.filter { it.variantId == null }, productId, null)
    } else {
        latestLandedCostMinor(whLayers, productId, null)
            ?: latestLandedCostMinor(layers, productId, null)
    }
    if (landedCost != null) return landedCost
    return variantCostMinor?.takeIf { it > 0L } ?: productPurchasePriceMinor.coerceAtLeast(0L)
}
