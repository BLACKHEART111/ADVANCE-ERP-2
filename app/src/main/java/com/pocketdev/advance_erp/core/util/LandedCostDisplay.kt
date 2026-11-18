package com.pocketdev.advance_erp.core.util

import com.pocketdev.advance_erp.data.entity.InventoryCostLayer

/** Read-only reference price; never changes FIFO layers, COGS or product master prices. */
fun latestLandedCostMinor(layers: List<InventoryCostLayer>, productId: String, variantId: String? = null): Long? =
    layers.asSequence()
        .filter { it.productId == productId && (variantId == null || it.variantId == variantId) }
        .filter { it.initialQuantity > 0 && it.unitLandedCostBdtMinor >= 0 && (it.isActive || it.remainingQuantity == 0) }
        .maxWithOrNull(compareBy<InventoryCostLayer> { it.receiptDate }.thenBy { it.createdAt }.thenBy { it.id })
        ?.unitLandedCostBdtMinor
