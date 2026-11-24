package com.pocketdev.advance_erp.core.util

import com.pocketdev.advance_erp.data.entity.Product
import com.pocketdev.advance_erp.data.entity.ProductVariant
import com.pocketdev.advance_erp.data.entity.StockBalance

/**
 * Computes sellable stock (`quantity - reservedQuantity`, coerced >= 0) for a specific
 * product variant in a given warehouse without borrowing another variant's balance.
 */
fun sellableStockForVariant(
    stockBalances: List<StockBalance>,
    warehouseId: String,
    productId: String,
    variantId: String
): Int {
    if (warehouseId.isBlank() || productId.isBlank() || variantId.isBlank()) return 0
    val exact = stockBalances.find {
        it.warehouseId == warehouseId && it.productId == productId && it.variantId == variantId
    }
    if (exact != null) {
        return maxOf(0, exact.quantity - exact.reservedQuantity)
    }
    val hasAnyVariantRow = stockBalances.any {
        it.warehouseId == warehouseId && it.productId == productId && it.variantId != null
    }
    if (hasAnyVariantRow) return 0
    val baseRow = stockBalances.find {
        it.warehouseId == warehouseId && it.productId == productId && it.variantId == null
    }
    return maxOf(0, (baseRow?.quantity ?: 0) - (baseRow?.reservedQuantity ?: 0))
}

/**
 * Computes total sellable stock for a product in a given warehouse across its variants
 * (or base rows when no variants exist).
 */
fun sellableStockForProduct(
    stockBalances: List<StockBalance>,
    warehouseId: String,
    productId: String,
    productVariants: List<ProductVariant>
): Int {
    if (warehouseId.isBlank() || productId.isBlank()) return 0
    if (productVariants.isEmpty()) {
        return stockBalances
            .filter { it.warehouseId == warehouseId && it.productId == productId }
            .sumOf { maxOf(0, it.quantity - it.reservedQuantity) }
    }
    val hasAnyVariantRow = stockBalances.any {
        it.warehouseId == warehouseId && it.productId == productId && it.variantId != null
    }
    return if (hasAnyVariantRow) {
        productVariants.sumOf { variant ->
            sellableStockForVariant(stockBalances, warehouseId, productId, variant.id)
        }
    } else {
        stockBalances
            .filter { it.warehouseId == warehouseId && it.productId == productId && it.variantId == null }
            .sumOf { maxOf(0, it.quantity - it.reservedQuantity) }
    }
}

/**
 * Computes sellable stock for the currently selected `(productId, variantId)` pair.
 */
fun sellableStockForSelection(
    stockBalances: List<StockBalance>,
    warehouseId: String,
    productId: String,
    variantId: String?,
    productVariants: List<ProductVariant>
): Int {
    if (warehouseId.isBlank() || productId.isBlank()) return 0
    return if (variantId != null) {
        sellableStockForVariant(stockBalances, warehouseId, productId, variantId)
    } else {
        sellableStockForProduct(stockBalances, warehouseId, productId, productVariants)
    }
}

fun firstInStockVariant(
    stockBalances: List<StockBalance>,
    warehouseId: String,
    productId: String,
    productVariants: List<ProductVariant>
): ProductVariant? = productVariants.firstOrNull { variant ->
    sellableStockForVariant(stockBalances, warehouseId, productId, variant.id) > 0
}

fun firstInStockProduct(
    stockBalances: List<StockBalance>,
    warehouseId: String,
    products: List<Product>,
    variantsByProduct: Map<String, List<ProductVariant>>
): Product? = products.firstOrNull { product ->
    sellableStockForProduct(
        stockBalances = stockBalances,
        warehouseId = warehouseId,
        productId = product.id,
        productVariants = variantsByProduct[product.id].orEmpty()
    ) > 0
}
