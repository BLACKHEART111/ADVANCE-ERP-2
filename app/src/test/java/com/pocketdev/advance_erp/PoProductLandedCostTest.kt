package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.core.engine.AllocationMethod
import com.pocketdev.advance_erp.data.entity.AdditionalCostCategory
import com.pocketdev.advance_erp.data.entity.AdditionalProductCost
import com.pocketdev.advance_erp.data.entity.CostCategory
import com.pocketdev.advance_erp.data.entity.ImportCostItem
import com.pocketdev.advance_erp.data.entity.LandedCostSnapshot
import com.pocketdev.advance_erp.data.entity.Product
import com.pocketdev.advance_erp.data.entity.ProductCategory
import com.pocketdev.advance_erp.data.entity.ProductVariant
import com.pocketdev.advance_erp.data.entity.PurchaseOrder
import com.pocketdev.advance_erp.data.entity.PurchaseOrderItem
import com.pocketdev.advance_erp.data.entity.PurchaseOrderStatus
import com.pocketdev.advance_erp.data.entity.ShipmentCarton
import com.pocketdev.advance_erp.data.entity.ShipmentFreightRate
import com.pocketdev.advance_erp.feature.imports.PoDraftCostCalcItem
import com.pocketdev.advance_erp.feature.imports.calculateDraftPoProductLandedBreakdowns
import com.pocketdev.advance_erp.feature.imports.calculatePoFreightPayableSummary
import com.pocketdev.advance_erp.feature.imports.calculatePoLandedCostSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class PoProductLandedCostTest {

    private val catInsole = ProductCategory(
        id = "cat-insole",
        nameEn = "Shoe Insoles",
        nameBn = "জুতার ইনসোল",
        code = "INS",
        defaultFreightRatePerKgBdtMinor = 32_000L // ৳320/KG
    )

    private val prodTsg = Product(
        id = "prod-tsg",
        categoryId = "cat-insole",
        nameEn = "T SHOES GUARD",
        nameBn = "T SHOES GUARD",
        baseSku = "SG7-TSG-005",
        purchasePriceMinor = 900L,
        sellingPriceMinor = 2500L
    )

    private val prodMsg = Product(
        id = "prod-msg",
        categoryId = "cat-insole",
        nameEn = "MOON SHOES GURD",
        nameBn = "MOON SHOES GURD",
        baseSku = "SG7-MSG-004",
        purchasePriceMinor = 500L,
        sellingPriceMinor = 1500L
    )

    private val varTsg1 = ProductVariant(
        id = "var-tsg-1",
        productId = "prod-tsg",
        sku = "SG7-TSG-005-BLK-40",
        color = "Black",
        size = "40",
        variantPriceMinor = 2500L,
        variantCostMinor = 900L
    )

    private val varTsg2 = ProductVariant(
        id = "var-tsg-2",
        productId = "prod-tsg",
        sku = "SG7-TSG-005-WHT-41",
        color = "White",
        size = "41",
        variantPriceMinor = 2500L,
        variantCostMinor = 900L
    )

    private val varMsg1 = ProductVariant(
        id = "var-msg-1",
        productId = "prod-msg",
        sku = "SG7-MSG-004-BLK",
        color = "Black",
        size = "Free",
        variantPriceMinor = 1500L,
        variantCostMinor = 500L
    )

    private val varMsg2 = ProductVariant(
        id = "var-msg-2",
        productId = "prod-msg",
        sku = "SG7-MSG-004-CLR",
        color = "Clear",
        size = "Free",
        variantPriceMinor = 1500L,
        variantCostMinor = 500L
    )

    @Test
    fun sameCategoryMultiProductPoComputesDistinctProductAndVariantLandedCosts() {
        val order = PurchaseOrder(
            id = "po-3",
            poNumber = "PO-2026-003",
            supplierId = "sup-1",
            status = PurchaseOrderStatus.CONFIRMED.name,
            currency = "CNY",
            exchangeRateToBdt = "20.0000",
            totalAmountMinor = 252_500L, // CNY 2,525.00
            totalBdtAmountMinor = 5_050_000L, // ৳50,500.00
            shippingRatePerKgBdt = "320.00",
            totalGrossWeightKg = "46.400",
            totalCartons = 3
        )
        val items = listOf(
            PurchaseOrderItem("poi-1", "po-3", "prod-tsg", "var-tsg-1", 1000, 45L, 45_000L, 900_000L, cartonQuantity = 1, grossWeightKg = "8.700"),
            PurchaseOrderItem("poi-2", "po-3", "prod-tsg", "var-tsg-2", 1000, 45L, 45_000L, 900_000L, cartonQuantity = 1, grossWeightKg = "8.700"),
            PurchaseOrderItem("poi-3", "po-3", "prod-msg", "var-msg-1", 3250, 25L, 81_250L, 1_625_000L, cartonQuantity = 1, grossWeightKg = "14.400"),
            PurchaseOrderItem("poi-4", "po-3", "prod-msg", "var-msg-2", 3250, 25L, 81_250L, 1_625_000L, cartonQuantity = 1, grossWeightKg = "14.600")
        )
        val cartons = listOf(
            ShipmentCarton("c-1", "shp-3", "CTN-001", "prod-tsg", "var-tsg-1", 1000, "50", "40", "30", "8.700", "8.000", "0.060000", "po-3"),
            ShipmentCarton("c-2", "shp-3", "CTN-002", "prod-tsg", "var-tsg-2", 1000, "50", "40", "30", "8.700", "8.000", "0.060000", "po-3"),
            ShipmentCarton("c-3a", "shp-3", "CTN-003", "prod-msg", "var-msg-1", 3250, "60", "40", "40", "14.400", "13.500", "0.096000", "po-3", isMixedCarton = true, totalCartonQuantity = 6500, cartonGrossWeightKg = "29.000"),
            ShipmentCarton("c-3b", "shp-3", "CTN-003", "prod-msg", "var-msg-2", 3250, "60", "40", "40", "14.600", "13.500", "0.096000", "po-3", isMixedCarton = true, totalCartonQuantity = 6500, cartonGrossWeightKg = "29.000")
        )
        val rates = listOf(
            ShipmentFreightRate(
                shipmentId = "shp-3",
                categoryId = "cat-insole",
                categoryNameEn = "Shoe Insoles",
                categoryNameBn = "জুতার ইনসোল",
                defaultRatePerKgBdtMinor = 32_000L,
                appliedRatePerKgBdtMinor = 32_000L,
                totalCategoryGrossWeightKg = "46.400",
                totalCategoryFreightBdtMinor = 1_484_800L
            )
        )

        val summary = calculatePoLandedCostSummary(
            order = order,
            items = items,
            cartons = cartons,
            costItems = emptyList(),
            additionalCosts = emptyList(),
            freightRates = rates,
            categories = listOf(catInsole),
            products = listOf(prodTsg, prodMsg),
            variants = listOf(varTsg1, varTsg2, varMsg1, varMsg2),
            isBangla = true
        )

        assertEquals(5_050_000L, summary.totalPurchaseBdtMinor)
        assertEquals(1_484_800L, summary.totalFreightBdtMinor)
        assertEquals(6_534_800L, summary.totalLandedBdtMinor)
        assertEquals(2, summary.productBreakdowns.size)

        val tsg = summary.productBreakdowns[0]
        assertEquals("prod-tsg", tsg.productId)
        assertEquals(2000, tsg.totalPcs)
        assertEquals(2_356_800L, tsg.totalLandedBdtMinor)
        assertEquals(BigDecimal("11.78"), tsg.unitLandedBdt)

        val msg = summary.productBreakdowns[1]
        assertEquals("prod-msg", msg.productId)
        assertEquals(6500, msg.totalPcs)
        assertEquals(4_178_000L, msg.totalLandedBdtMinor)
        assertEquals(BigDecimal("6.43"), msg.unitLandedBdt)
        assertEquals(BigDecimal("6.42"), msg.itemBreakdowns[0].unitLandedBdt)
        assertEquals(BigDecimal("6.44"), msg.itemBreakdowns[1].unitLandedBdt)
    }

    @Test
    fun multiOverheadAllocationReconcilesItemsProductsAndPoGrandTotalExactly() {
        val order = PurchaseOrder(
            id = "po-oh",
            poNumber = "PO-2026-010",
            supplierId = "sup-1",
            status = PurchaseOrderStatus.CONFIRMED.name,
            currency = "CNY",
            exchangeRateToBdt = "20.0000",
            totalAmountMinor = 120_000L,
            totalBdtAmountMinor = 2_400_000L
        )
        val items = listOf(
            PurchaseOrderItem("i-1", "po-oh", "prod-tsg", "var-tsg-1", 300, 200L, 60_000L, 1_200_000L, grossWeightKg = "10.000"),
            PurchaseOrderItem("i-2", "po-oh", "prod-msg", "var-msg-1", 600, 100L, 60_000L, 1_200_000L, grossWeightKg = "20.000")
        )
        val costs = listOf(
            ImportCostItem(
                id = "c-freight",
                shipmentId = "shp-oh",
                purchaseOrderId = "po-oh",
                costCategory = CostCategory.SEA_FREIGHT.name,
                description = "Sea Freight",
                amountMinor = 960_000L,
                bdtAmountMinor = 960_000L,
                allocationMethod = AllocationMethod.GROSS_WEIGHT.name
            ),
            ImportCostItem(
                id = "c-handling",
                shipmentId = "shp-oh",
                purchaseOrderId = "po-oh",
                costCategory = CostCategory.WAREHOUSE_CHARGES.name,
                description = "Handling",
                amountMinor = 90_003L,
                bdtAmountMinor = 90_003L,
                allocationMethod = AllocationMethod.QUANTITY.name
            ),
            ImportCostItem(
                id = "c-manual",
                shipmentId = "shp-oh",
                purchaseOrderId = "po-oh",
                costCategory = CostCategory.CUSTOMS_DUTY.name,
                description = "Customs",
                amountMinor = 150_000L,
                bdtAmountMinor = 150_000L,
                allocationMethod = AllocationMethod.MANUAL.name,
                manualAllocationsJson = "prod-tsg:var-tsg-1:100000;prod-msg:var-msg-1:50000"
            )
        )

        val summary = calculatePoLandedCostSummary(
            order = order,
            items = items,
            cartons = emptyList(),
            costItems = costs,
            additionalCosts = emptyList(),
            freightRates = emptyList(),
            categories = listOf(catInsole),
            products = listOf(prodTsg, prodMsg),
            variants = listOf(varTsg1, varMsg1),
            isBangla = false
        )

        val expectedGrand = 2_400_000L + 960_000L + 90_003L + 150_000L
        assertEquals(expectedGrand, summary.totalLandedBdtMinor)
        assertEquals(expectedGrand, summary.productBreakdowns.sumOf { it.totalLandedBdtMinor })
        assertEquals(expectedGrand, summary.itemBreakdownsById.values.sumOf { it.totalLandedBdtMinor })

        // TSG: purchase 1,200,000 + freight (1/3 * 960,000 = 320,000) + handling (300/900 * 90,003 = 30,001) + manual 100,000 = 1,650,001
        val tsg = summary.productBreakdowns.first { it.productId == "prod-tsg" }
        assertEquals(1_650_001L, tsg.totalLandedBdtMinor)
        assertEquals(BigDecimal("55.00"), tsg.unitLandedBdt)
    }

    @Test
    fun finalizedSnapshotPlusAdditionalCapitalizableCostAndDraftCalculatorWorkAccurately() {
        val order = PurchaseOrder(
            id = "po-fin",
            poNumber = "PO-2026-099",
            supplierId = "sup-1",
            status = PurchaseOrderStatus.ACTUAL_COST_FINALIZED.name,
            currency = "CNY",
            exchangeRateToBdt = "20.0000",
            totalAmountMinor = 100_000L,
            totalBdtAmountMinor = 2_000_000L,
            isActualCostFinalized = true
        )
        val items = listOf(
            PurchaseOrderItem("p1", "po-fin", "prod-tsg", null, 500, 120L, 60_000L, 1_200_000L, grossWeightKg = "10.000"),
            PurchaseOrderItem("p2", "po-fin", "prod-msg", null, 500, 80L, 40_000L, 800_000L, grossWeightKg = "10.000")
        )
        val snapshots = listOf(
            LandedCostSnapshot(
                id = "s1", shipmentId = "shp-fin", productId = "prod-tsg", variantId = null,
                productNameEn = "T SHOES GUARD", productNameBn = "", sku = "SG7-TSG-005",
                totalReceivedQuantity = 500, totalPurchaseCostBdtMinor = 1_200_000L,
                totalAllocatedCostsBdtMinor = 320_000L, totalLandedCostBdtMinor = 1_520_000L,
                unitLandedCostBdtMinor = 3040L, internationalFreightBdtMinor = 320_000L
            ),
            LandedCostSnapshot(
                id = "s2", shipmentId = "shp-fin", productId = "prod-msg", variantId = null,
                productNameEn = "MOON SHOES GURD", productNameBn = "", sku = "SG7-MSG-004",
                totalReceivedQuantity = 500, totalPurchaseCostBdtMinor = 800_000L,
                totalAllocatedCostsBdtMinor = 320_000L, totalLandedCostBdtMinor = 1_120_000L,
                unitLandedCostBdtMinor = 2240L, internationalFreightBdtMinor = 320_000L
            )
        )
        val extraCosts = listOf(
            AdditionalProductCost(
                id = "ac-1", costNumber = "AC-2026-0001", purchaseOrderId = "po-fin",
                category = AdditionalCostCategory.HANDLING.name, description = "Unloading",
                amountMinor = 50_000L, bdtAmountMinor = 50_000L,
                isCapitalizable = true, allocationMethod = AllocationMethod.QUANTITY.name
            ),
            AdditionalProductCost(
                id = "ac-2", costNumber = "AC-2026-0002", purchaseOrderId = "po-fin",
                category = AdditionalCostCategory.WAREHOUSE_RENT.name, description = "Rent OPEX",
                amountMinor = 100_000L, bdtAmountMinor = 100_000L,
                isCapitalizable = false, allocationMethod = AllocationMethod.QUANTITY.name
            )
        )

        val summary = calculatePoLandedCostSummary(
            order = order,
            items = items,
            cartons = emptyList(),
            costItems = emptyList(),
            additionalCosts = extraCosts,
            freightRates = emptyList(),
            categories = listOf(catInsole),
            products = listOf(prodTsg, prodMsg),
            variants = emptyList(),
            snapshots = snapshots,
            isCostFinalized = true
        )

        assertTrue(summary.isFinalizedSnapshotBased)
        // Only capitalizable AC (50,000) is added (25,000 each); Rent OPEX (100,000) is excluded!
        assertEquals(2_690_000L, summary.totalLandedBdtMinor)
        assertEquals(1_545_000L, summary.productBreakdowns[0].totalLandedBdtMinor)
        assertEquals(BigDecimal("30.90"), summary.productBreakdowns[0].unitLandedBdt)
        assertEquals(1_145_000L, summary.productBreakdowns[1].totalLandedBdtMinor)
        assertEquals(BigDecimal("22.90"), summary.productBreakdowns[1].unitLandedBdt)

        // Also check CreatePurchaseOrder draft calculator
        val draftProducts = calculateDraftPoProductLandedBreakdowns(
            draftItems = listOf(
                PoDraftCostCalcItem("d1", "prod-tsg", "var-tsg-1", 500, 60_000L, 1_200_000L, BigDecimal("10.0"), 1),
                PoDraftCostCalcItem("d2", "prod-msg", "var-msg-1", 500, 40_000L, 800_000L, BigDecimal("10.0"), 1)
            ),
            products = listOf(prodTsg, prodMsg),
            variants = listOf(varTsg1, varMsg1),
            categories = listOf(catInsole),
            isLocalPurchase = false,
            categoryFreightRatesMinor = mapOf("cat-insole" to 32_000L),
            sharedOverheadBdt = BigDecimal("500.00"),
            expectedTotalProductBdt = BigDecimal("20000.00"),
            expectedTotalFreightBdt = BigDecimal("6400.00"),
            isBangla = false
        )
        assertEquals(2_690_000L, draftProducts.sumOf { it.totalLandedBdtMinor })
        assertEquals(BigDecimal("30.90"), draftProducts[0].unitLandedBdt)
        assertEquals(BigDecimal("22.90"), draftProducts[1].unitLandedBdt)
    }

    @Test
    fun freightPayableSummaryTracksTotalBillPaidAndRemainingDueAcrossCosts() {
        val order = PurchaseOrder(
            id = "po-fr-pay",
            poNumber = "PO-2026-003",
            supplierId = "sup-1",
            status = PurchaseOrderStatus.CONFIRMED.name,
            currency = "CNY",
            exchangeRateToBdt = "20.0000",
            totalAmountMinor = 252_500L,
            totalBdtAmountMinor = 5_050_000L,
            shippingRatePerKgBdt = "320.00",
            totalGrossWeightKg = "46.400"
        )
        val items = listOf(
            PurchaseOrderItem("i-1", "po-fr-pay", "prod-tsg", null, 2000, 45L, 90_000L, 1_800_000L, grossWeightKg = "17.400"),
            PurchaseOrderItem("i-2", "po-fr-pay", "prod-msg", null, 6500, 25L, 162_500L, 3_250_000L, grossWeightKg = "29.000")
        )
        val landedSummary = calculatePoLandedCostSummary(
            order = order,
            items = items,
            cartons = emptyList(),
            costItems = emptyList(),
            additionalCosts = emptyList(),
            freightRates = emptyList(),
            categories = listOf(catInsole),
            products = listOf(prodTsg, prodMsg),
            variants = emptyList()
        )
        assertEquals(1_484_800L, landedSummary.totalFreightBdtMinor)

        val partial = calculatePoFreightPayableSummary(
            landedSummary = landedSummary,
            costItems = emptyList(),
            additionalCosts = emptyList(),
            paidFreightBillMinor = 500_000L
        )
        assertEquals(1_484_800L, partial.totalFreightBillMinor)
        assertEquals(500_000L, partial.paidFreightBillMinor)
        assertEquals(984_800L, partial.remainingFreightBillMinor)
        assertTrue(partial.progress > 0.33f && partial.progress < 0.34f)
    }
}
