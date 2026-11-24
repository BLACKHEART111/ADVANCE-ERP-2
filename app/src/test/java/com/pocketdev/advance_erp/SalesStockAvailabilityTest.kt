package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.core.util.firstInStockProduct
import com.pocketdev.advance_erp.core.util.firstInStockVariant
import com.pocketdev.advance_erp.core.util.sellableStockForProduct
import com.pocketdev.advance_erp.core.util.sellableStockForSelection
import com.pocketdev.advance_erp.core.util.sellableStockForVariant
import com.pocketdev.advance_erp.data.entity.Product
import com.pocketdev.advance_erp.data.entity.ProductVariant
import com.pocketdev.advance_erp.data.entity.StockBalance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SalesStockAvailabilityTest {

    private fun product(id: String, sku: String = id) = Product(
        id = id,
        categoryId = "cat-1",
        nameEn = "Product $id",
        nameBn = "পণ্য $id",
        baseSku = sku,
        purchasePriceMinor = 10000L,
        sellingPriceMinor = 15000L
    )

    private fun variant(id: String, productId: String, sku: String = id) = ProductVariant(
        id = id,
        productId = productId,
        sku = sku,
        color = id,
        variantPriceMinor = 16000L,
        variantCostMinor = 11000L
    )

    private fun balance(
        warehouseId: String,
        productId: String,
        variantId: String? = null,
        quantity: Int = 0,
        reservedQuantity: Int = 0
    ) = StockBalance(
        id = "$warehouseId-$productId-${variantId ?: "base"}",
        warehouseId = warehouseId,
        productId = productId,
        variantId = variantId,
        quantity = quantity,
        reservedQuantity = reservedQuantity
    )

    @Test
    fun missingOrZeroVariantDoesNotBorrowAnotherVariantsStock() {
        val red = variant("v-red", "p1")
        val black = variant("v-black", "p1")
        val balances = listOf(balance("wh-1", "p1", "v-black", quantity = 12, reservedQuantity = 2))

        assertEquals(10, sellableStockForVariant(balances, "wh-1", "p1", "v-black"))
        assertEquals(0, sellableStockForVariant(balances, "wh-1", "p1", "v-red"))
        assertEquals(0, sellableStockForSelection(balances, "wh-1", "p1", "v-red", listOf(red, black)))
    }

    @Test
    fun productWithOneOutOfStockAndOneInStockVariantRemainsSelectableAndPicksInStockVariant() {
        val vOut = variant("v-out", "p1")
        val vIn = variant("v-in", "p1")
        val p1 = product("p1")
        val variants = listOf(vOut, vIn)
        val balances = listOf(
            balance("wh-1", "p1", "v-out", quantity = 3, reservedQuantity = 3),
            balance("wh-1", "p1", "v-in", quantity = 8, reservedQuantity = 1)
        )

        assertEquals(0, sellableStockForVariant(balances, "wh-1", "p1", "v-out"))
        assertEquals(7, sellableStockForVariant(balances, "wh-1", "p1", "v-in"))
        assertEquals(7, sellableStockForProduct(balances, "wh-1", "p1", variants))
        assertEquals("v-in", firstInStockVariant(balances, "wh-1", "p1", variants)?.id)
        assertEquals("p1", firstInStockProduct(balances, "wh-1", listOf(p1), mapOf("p1" to variants))?.id)
    }

    @Test
    fun fullyStockOutOrReservedProductIsSkippedByInitialPicker() {
        val pOut = product("p-out")
        val pIn = product("p-in")
        val vOut1 = variant("v1", "p-out")
        val vOut2 = variant("v2", "p-out")
        val variantsByProduct = mapOf("p-out" to listOf(vOut1, vOut2))
        val balances = listOf(
            balance("wh-1", "p-out", "v1", quantity = 0, reservedQuantity = 0),
            balance("wh-1", "p-out", "v2", quantity = 5, reservedQuantity = 5),
            balance("wh-1", "p-in", null, quantity = 4, reservedQuantity = 0)
        )

        assertEquals(0, sellableStockForProduct(balances, "wh-1", "p-out", listOf(vOut1, vOut2)))
        assertEquals("p-in", firstInStockProduct(balances, "wh-1", listOf(pOut, pIn), variantsByProduct)?.id)
        assertNull(firstInStockVariant(balances, "wh-1", "p-out", listOf(vOut1, vOut2)))
    }

    @Test
    fun unvariantedProductUsesSelectedWarehouseOnly() {
        val p1 = product("p1")
        val balances = listOf(
            balance("wh-2", "p1", null, quantity = 25, reservedQuantity = 0),
            balance("wh-1", "p1", null, quantity = 5, reservedQuantity = 2)
        )

        assertEquals(3, sellableStockForProduct(balances, "wh-1", "p1", emptyList()))
        assertEquals(25, sellableStockForProduct(balances, "wh-2", "p1", emptyList()))
        assertEquals(0, sellableStockForProduct(balances, "wh-3", "p1", emptyList()))
        assertNull(firstInStockProduct(balances, "wh-3", listOf(p1), emptyMap()))
    }
}
