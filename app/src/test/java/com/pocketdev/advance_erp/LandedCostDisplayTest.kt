package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.core.util.latestLandedCostMinor
import com.pocketdev.advance_erp.core.util.resolveUnitCostMinorForSale
import com.pocketdev.advance_erp.data.entity.InventoryCostLayer
import org.junit.Assert.*
import org.junit.Test

class LandedCostDisplayTest {
    private fun layer(id: String, cost: Long, date: Long, variant: String? = "black") = InventoryCostLayer(
        id=id,productId="p",variantId=variant,warehouseId="w",shipmentId="s",shipmentNumber="S",
        warehouseReceiptId="r",receiptNumber="R",initialQuantity=10,remainingQuantity=10,
        unitLandedCostBdtMinor=cost,originalPurchasePriceMinor=50,exchangeRateToBdt="1",
        allocatedExpensePerUnitBdtMinor=0,receiptDate=date,createdAt=date
    )
    @Test fun newestReceivedLandedCostOverridesOldReferencePrice() {
        assertEquals(200L,latestLandedCostMinor(listOf(layer("new",200,20),layer("old",100,10)),"p","black"))
    }
    @Test fun doesNotBorrowOtherVariantOrOtherProductCost() {
        val layers=listOf(layer("a",100,10),layer("b",200,20,"blue"),layer("c",999,30).copy(productId="other"))
        assertEquals(100L,latestLandedCostMinor(layers,"p","black"))
        assertNull(latestLandedCostMinor(layers,"p","red"))
    }
    @Test fun baseProductUsesLatestReceivedVariantCost() {
        assertEquals(200L,latestLandedCostMinor(listOf(layer("a",100,10),layer("b",200,20,"blue")),"p"))
    }
    @Test fun soldOutReceiptStillProvidesReferenceCostButDisabledStockDoesNot() {
        val old=layer("a",100,10).copy(isActive=false,remainingQuantity=0)
        val disabled=layer("b",999,20).copy(isActive=false)
        assertEquals(100L,latestLandedCostMinor(listOf(old,disabled),"p","black"))
    }
    @Test fun zeroCostIsValidAndMissingHistoryReturnsNull() {
        assertEquals(0L,latestLandedCostMinor(listOf(layer("a",0,10)),"p","black"))
        assertNull(latestLandedCostMinor(emptyList(),"p","black"))
    }
    @Test fun ignoresInvalidRowsAndUsesStableReceiptTieBreak() {
        val a=layer("a",100,10)
        val b=layer("b",200,10).copy(createdAt=11)
        assertEquals(200L,latestLandedCostMinor(listOf(b,a,layer("bad",-1,50),layer("empty",500,60).copy(initialQuantity=0)),"p","black"))
    }
    @Test fun resolveUnitCostPrefersWarehouseLandedCostEvenWhenMasterPurchasePriceIsZero() {
        val wh1 = layer("w1", 55000, 10, "black").copy(warehouseId = "wh-1")
        val wh2 = layer("w2", 52000, 20, "black").copy(warehouseId = "wh-2")
        assertEquals(55000L, resolveUnitCostMinorForSale(listOf(wh1, wh2), "p", "black", "wh-1", 0L, 0L))
        assertEquals(52000L, resolveUnitCostMinorForSale(listOf(wh1, wh2), "p", "black", "wh-other", 0L, 0L))
    }
    @Test fun resolveUnitCostVariantDoesNotBorrowDifferentVariantCostAndFallsBackCorrectly() {
        val black = layer("b", 55000, 10, "black").copy(warehouseId = "wh-1")
        val base = layer("base", 48000, 5, null).copy(warehouseId = "wh-1")
        assertEquals(30000L, resolveUnitCostMinorForSale(listOf(black), "p", "red", "wh-1", 30000L, 25000L))
        assertEquals(48000L, resolveUnitCostMinorForSale(listOf(black, base), "p", "red", "wh-1", 0L, 0L))
        assertEquals(25000L, resolveUnitCostMinorForSale(emptyList(), "p", "red", "wh-1", 0L, 25000L))
    }
}
