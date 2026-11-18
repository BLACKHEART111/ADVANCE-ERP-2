package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.core.util.latestLandedCostMinor
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
}
