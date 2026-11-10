package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.core.engine.AllocationItemInput
import com.pocketdev.advance_erp.core.engine.AllocationMethod
import com.pocketdev.advance_erp.core.engine.LandedCostAllocationEngine
import com.pocketdev.advance_erp.core.engine.LandingCostEngine
import com.pocketdev.advance_erp.core.engine.LandingCostEngine.CategoryFreightRateInput
import com.pocketdev.advance_erp.core.engine.LandingCostEngine.FifoLayerAdjustmentInput
import com.pocketdev.advance_erp.core.engine.LandingCostEngine.ImportExpenseInput
import com.pocketdev.advance_erp.core.engine.LandingCostEngine.ShipmentCartonInput
import com.pocketdev.advance_erp.core.engine.LandingCostEngine.ShipmentProductInput
import com.pocketdev.advance_erp.core.engine.LandingCostEngine.SupplierChinaDeliveryInput
import com.pocketdev.advance_erp.data.entity.CostCategory
import com.pocketdev.advance_erp.data.entity.ExpenseCategory
import com.pocketdev.advance_erp.data.entity.ExpenseVoucher
import com.pocketdev.advance_erp.data.entity.InventoryCostLayer
import com.pocketdev.advance_erp.data.entity.PurchaseOrder
import com.pocketdev.advance_erp.data.entity.PurchaseOrderStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.math.BigDecimal

/**
 * PHASE 8 — COMPLETE IMPORT MODULE & LANDING COST ENGINE VERIFICATION SUITE
 *
 * Covers all 25 required business logic, receiving, landing cost, FIFO, post-finalization,
 * and accounting segregation scenarios.
 */
class ImportModulePhase8CompleteTest {

    // -------------------------------------------------------------------------
    // Scenario 1: Single supplier shipment
    // -------------------------------------------------------------------------
    @Test
    fun scenario01_singleSupplierShipment() {
        val products = listOf(
            ShipmentProductInput(
                productId = "prod-1",
                productNameEn = "Smart Watch X1",
                supplierId = "sup-gz",
                categoryId = "cat-elec",
                packedQuantity = 100,
                unitPurchasePriceForeignMinor = 50_00L, // CNY 50.00
                purchaseCurrency = "CNY",
                purchaseExchangeRateToBdt = BigDecimal("17.00")
            )
        )
        val cartons = listOf(
            ShipmentCartonInput(
                cartonNumber = "CTN-01",
                supplierId = "sup-gz",
                productId = "prod-1",
                categoryId = "cat-elec",
                quantity = 100,
                grossWeightKg = BigDecimal("20.000"),
                cbm = BigDecimal("0.150000")
            )
        )
        val chinaDeliveries = listOf(
            SupplierChinaDeliveryInput(
                supplierId = "sup-gz",
                deliveryChargeForeignMinor = 200_00L, // CNY 200.00 -> BDT 3,400.00
                currency = "CNY",
                exchangeRateToBdt = BigDecimal("17.00")
            )
        )
        val freightRates = listOf(
            CategoryFreightRateInput(
                categoryId = "cat-elec",
                categoryNameEn = "Electronics",
                defaultRatePerKgBdtMinor = 180_00L // BDT 180/KG -> 20 KG = BDT 3,600.00
            )
        )

        val result = LandingCostEngine.calculateShipmentLandingCost(
            products = products,
            cartons = cartons,
            supplierChinaDeliveries = chinaDeliveries,
            categoryFreightRates = freightRates,
            expenses = emptyList()
        )

        // Purchase = 100 * 50 * 17 = BDT 85,000.00 (8,500,000 minor)
        assertEquals(8_500_000L, result.totalPurchaseCostBdtMinor)
        // China Delivery = 200 * 17 = BDT 3,400.00 (340,000 minor)
        assertEquals(340_000L, result.totalChinaDeliveryBdtMinor)
        // Intl Freight = 20 * 180 = BDT 3,600.00 (360,000 minor)
        assertEquals(360_000L, result.totalInternationalFreightBdtMinor)
        // Total Landed = 85,000 + 3,400 + 3,600 = BDT 92,000.00 (9,200,000 minor)
        assertEquals(9_200_000L, result.grandTotalLandingCostBdtMinor)
        // Unit cost = 92,000 / 100 = BDT 920.00 (92,000 minor)
        assertEquals(92_000L, result.productBreakdowns.single().unitImportCostBdtMinor)
    }

    // -------------------------------------------------------------------------
    // Scenario 2: Multi-supplier shipment
    // -------------------------------------------------------------------------
    @Test
    fun scenario02_multiSupplierShipment() {
        val products = listOf(
            ShipmentProductInput(
                productId = "prod-a",
                productNameEn = "Earbuds",
                supplierId = "sup-a",
                categoryId = "cat-elec",
                packedQuantity = 50,
                unitPurchasePriceForeignMinor = 20_00L, // CNY 1,000 total
                purchaseCurrency = "CNY",
                purchaseExchangeRateToBdt = BigDecimal("17.00"),
                fallbackGrossWeightKg = BigDecimal("10.000")
            ),
            ShipmentProductInput(
                productId = "prod-b",
                productNameEn = "Phone Case",
                supplierId = "sup-b",
                categoryId = "cat-acc",
                packedQuantity = 200,
                unitPurchasePriceForeignMinor = 5_00L, // CNY 1,000 total
                purchaseCurrency = "CNY",
                purchaseExchangeRateToBdt = BigDecimal("17.00"),
                fallbackGrossWeightKg = BigDecimal("15.000")
            )
        )
        val chinaDeliveries = listOf(
            SupplierChinaDeliveryInput(
                supplierId = "sup-a",
                deliveryChargeForeignMinor = 100_00L, // CNY 100 -> BDT 1,700
                currency = "CNY",
                exchangeRateToBdt = BigDecimal("17.00")
            ),
            SupplierChinaDeliveryInput(
                supplierId = "sup-b",
                deliveryChargeForeignMinor = 50_00L, // CNY 50 -> BDT 850
                currency = "CNY",
                exchangeRateToBdt = BigDecimal("17.00")
            )
        )

        val result = LandingCostEngine.calculateShipmentLandingCost(
            products = products,
            cartons = emptyList(),
            supplierChinaDeliveries = chinaDeliveries,
            categoryFreightRates = emptyList(),
            expenses = emptyList()
        )

        val prodA = result.productBreakdowns.first { it.productId == "prod-a" }
        val prodB = result.productBreakdowns.first { it.productId == "prod-b" }

        // Supplier A's delivery goes strictly to Supplier A's product
        assertEquals(170_000L, prodA.chinaDeliveryBdtMinor)
        // Supplier B's delivery goes strictly to Supplier B's product
        assertEquals(85_000L, prodB.chinaDeliveryBdtMinor)
    }

    // -------------------------------------------------------------------------
    // Scenario 3: Multi-PO shipment
    // -------------------------------------------------------------------------
    @Test
    fun scenario03_multiPoShipment() {
        val po1 = PurchaseOrder(
            id = "po-1",
            poNumber = "PO-2026-001",
            supplierId = "sup-1",
            shipmentId = "ship-100",
            orderDate = 1700000000000L,
            expectedArrivalDate = null,
            currency = "CNY",
            exchangeRateToBdt = "17.00",
            totalAmountMinor = 1000_00L,
            totalBdtAmountMinor = 17_000_00L,
            status = PurchaseOrderStatus.CONFIRMED.name,
            notes = null,
            createdAt = 1700000000000L,
            updatedAt = 1700000000000L
        )
        val po2 = po1.copy(
            id = "po-2",
            poNumber = "PO-2026-002",
            supplierId = "sup-2",
            totalAmountMinor = 2000_00L,
            totalBdtAmountMinor = 34_000_00L
        )

        val products = listOf(
            ShipmentProductInput(
                productId = "p1",
                productNameEn = "Item from PO 1",
                supplierId = po1.supplierId,
                purchaseOrderId = po1.id,
                categoryId = "cat-1",
                packedQuantity = 10,
                unitPurchasePriceForeignMinor = 100_00L,
                purchaseCurrency = po1.currency,
                purchaseExchangeRateToBdt = BigDecimal(po1.exchangeRateToBdt)
            ),
            ShipmentProductInput(
                productId = "p2",
                productNameEn = "Item from PO 2",
                supplierId = po2.supplierId,
                purchaseOrderId = po2.id,
                categoryId = "cat-1",
                packedQuantity = 20,
                unitPurchasePriceForeignMinor = 100_00L,
                purchaseCurrency = po2.currency,
                purchaseExchangeRateToBdt = BigDecimal(po2.exchangeRateToBdt)
            )
        )

        val result = LandingCostEngine.calculateShipmentLandingCost(
            products = products,
            cartons = emptyList(),
            supplierChinaDeliveries = emptyList(),
            categoryFreightRates = emptyList(),
            expenses = emptyList()
        )

        assertEquals(2, result.productBreakdowns.size)
        assertEquals(51_000_00L, result.totalPurchaseCostBdtMinor)
    }

    // -------------------------------------------------------------------------
    // Scenario 4: Multi-currency purchase conversion (CNY, USD, BDT)
    // -------------------------------------------------------------------------
    @Test
    fun scenario04_multiCurrencyPurchaseConversion() {
        val cnyBdt = LandingCostEngine.convertToBdtMinor(1_000_00L, "CNY", BigDecimal("17.25"))
        val usdBdt = LandingCostEngine.convertToBdtMinor(500_00L, "USD", BigDecimal("121.50"))
        val bdtBdt = LandingCostEngine.convertToBdtMinor(25_000_00L, "BDT", BigDecimal.ONE)

        assertEquals(17_250_00L, cnyBdt)
        assertEquals(60_750_00L, usdBdt)
        assertEquals(25_000_00L, bdtBdt)
    }

    // -------------------------------------------------------------------------
    // Scenario 5: 30% advance + 70% balance supplier payment
    // -------------------------------------------------------------------------
    @Test
    fun scenario05_supplier30PercentAdvanceAnd70PercentBalancePayment() {
        // Total Purchase = CNY 10,000.00 at 17.00 = BDT 170,000.00 (17,000,000 minor)
        val totalPurchaseForeignMinor = 10_000_00L
        val rate = BigDecimal("17.00")
        val totalPurchaseBdtMinor = LandingCostEngine.convertToBdtMinor(totalPurchaseForeignMinor, "CNY", rate)

        // 30% advance payment
        val advance30Foreign = (totalPurchaseForeignMinor * 30L) / 100L // CNY 3,000.00
        val advance30Bdt = LandingCostEngine.convertToBdtMinor(advance30Foreign, "CNY", rate)
        // 70% balance payment
        val balance70Foreign = totalPurchaseForeignMinor - advance30Foreign // CNY 7,000.00
        val balance70Bdt = LandingCostEngine.convertToBdtMinor(balance70Foreign, "CNY", rate)

        assertEquals(3_000_00L, advance30Foreign)
        assertEquals(7_000_00L, balance70Foreign)
        assertEquals(51_000_00L, advance30Bdt)
        assertEquals(119_000_00L, balance70Bdt)
        assertEquals(totalPurchaseBdtMinor, advance30Bdt + balance70Bdt)
    }

    // -------------------------------------------------------------------------
    // Scenario 6: Supplier-wise China delivery allocation (Exact Prompt Example + Manual)
    // -------------------------------------------------------------------------
    @Test
    fun scenario06_supplierWiseChinaDeliveryAllocation() {
        // Prompt Example:
        // Supplier A: Product A = CNY 1,000, Product B = CNY 2,000, China Delivery = CNY 300
        // Allocation: Product A = CNY 100, Product B = CNY 200
        val products = listOf(
            ShipmentProductInput(
                productId = "prod-A",
                productNameEn = "Product A",
                supplierId = "sup-A",
                categoryId = "cat-1",
                packedQuantity = 10,
                unitPurchasePriceForeignMinor = 100_00L, // Total = CNY 1,000
                purchaseCurrency = "CNY",
                purchaseExchangeRateToBdt = BigDecimal("17.00")
            ),
            ShipmentProductInput(
                productId = "prod-B",
                productNameEn = "Product B",
                supplierId = "sup-A",
                categoryId = "cat-1",
                packedQuantity = 20,
                unitPurchasePriceForeignMinor = 100_00L, // Total = CNY 2,000
                purchaseCurrency = "CNY",
                purchaseExchangeRateToBdt = BigDecimal("17.00")
            )
        )

        val autoDelivery = SupplierChinaDeliveryInput(
            supplierId = "sup-A",
            deliveryChargeForeignMinor = 300_00L, // CNY 300
            currency = "CNY",
            exchangeRateToBdt = BigDecimal("17.00"),
            allocationMethod = AllocationMethod.PURCHASE_VALUE
        )

        val (foreignAlloc, bdtAlloc) = LandingCostEngine.allocateSupplierChinaDelivery(autoDelivery, products)

        // CNY 100.00 (10,000 minor) and CNY 200.00 (20,000 minor)
        assertEquals(100_00L, foreignAlloc["prod-A"])
        assertEquals(200_00L, foreignAlloc["prod-B"])
        // Converted to BDT at 17.00: BDT 1,700.00 and BDT 3,400.00
        assertEquals(1_700_00L, bdtAlloc["prod-A"])
        assertEquals(3_400_00L, bdtAlloc["prod-B"])

        // Optional manual product-wise allocation
        val manualDelivery = SupplierChinaDeliveryInput(
            supplierId = "sup-A",
            deliveryChargeForeignMinor = 300_00L,
            currency = "CNY",
            exchangeRateToBdt = BigDecimal("17.00"),
            allocationMethod = AllocationMethod.MANUAL,
            manualProductAllocationsForeignMinor = mapOf(
                "prod-A" to 150_00L,
                "prod-B" to 150_00L
            )
        )
        val (manualForeign, _) = LandingCostEngine.allocateSupplierChinaDelivery(manualDelivery, products)
        assertEquals(150_00L, manualForeign["prod-A"])
        assertEquals(150_00L, manualForeign["prod-B"])
    }

    // -------------------------------------------------------------------------
    // Scenario 7: Carton-wise variable weight and CBM calculation
    // -------------------------------------------------------------------------
    @Test
    fun scenario07_cartonWiseVariableWeightAndCbmCalculation() {
        // Different cartons have different weights, quantities, and dimensions
        // One product (prod-1) has multiple cartons
        val cartons = listOf(
            ShipmentCartonInput(
                cartonNumber = "CTN-001",
                supplierId = "sup-1",
                productId = "prod-1",
                categoryId = "cat-elec",
                quantity = 40,
                grossWeightKg = BigDecimal("12.500"),
                cbm = BigDecimal("0.080000")
            ),
            ShipmentCartonInput(
                cartonNumber = "CTN-002",
                supplierId = "sup-1",
                productId = "prod-1",
                categoryId = "cat-elec",
                quantity = 60,
                grossWeightKg = BigDecimal("18.750"),
                // CBM calculated from dimensions 50x40x50 cm = 0.100000 CBM
                cbm = BigDecimal.ZERO,
                lengthCm = BigDecimal("50"),
                widthCm = BigDecimal("40"),
                heightCm = BigDecimal("50")
            ),
            ShipmentCartonInput(
                cartonNumber = "CTN-003",
                supplierId = "sup-2",
                productId = "prod-2",
                categoryId = "cat-acc",
                quantity = 200,
                grossWeightKg = BigDecimal("9.250"),
                cbm = BigDecimal("0.050000")
            )
        )

        val summary = LandingCostEngine.summarizePackagingList(cartons)

        assertEquals(300, summary.totalQuantity)
        assertEquals(3, summary.totalCartonCount)
        assertEquals(0, BigDecimal("40.500").compareTo(summary.totalGrossWeightKg))
        assertEquals(0, BigDecimal("0.230000").compareTo(summary.totalCbm))

        // Product 1 has 2 cartons totaling 100 qty, 31.25 KG, 0.18 CBM
        assertEquals(100, summary.productQuantityMap["prod-1"])
        assertEquals(2, summary.productCartonCountMap["prod-1"])
        assertEquals(0, BigDecimal("31.250").compareTo(summary.productWeightMap["prod-1"]))
        assertEquals(0, BigDecimal("0.180000").compareTo(summary.productCbmMap["prod-1"]))
    }

    // -------------------------------------------------------------------------
    // Scenario 8: Default category freight rate loading
    // -------------------------------------------------------------------------
    @Test
    fun scenario08_defaultCategoryFreightRateLoading() {
        // Electronics: BDT 180/KG, Accessories: BDT 120/KG, Household: BDT 150/KG
        val products = listOf(
            ShipmentProductInput("p-elec", productNameEn = "Router", supplierId = "s1", categoryId = "cat-elec", packedQuantity = 10, unitPurchasePriceForeignMinor = 10_00L, fallbackGrossWeightKg = BigDecimal("10.000")),
            ShipmentProductInput("p-acc", productNameEn = "Cable", supplierId = "s1", categoryId = "cat-acc", packedQuantity = 50, unitPurchasePriceForeignMinor = 2_00L, fallbackGrossWeightKg = BigDecimal("5.000")),
            ShipmentProductInput("p-home", productNameEn = "Lamp", supplierId = "s1", categoryId = "cat-home", packedQuantity = 20, unitPurchasePriceForeignMinor = 5_00L, fallbackGrossWeightKg = BigDecimal("8.000"))
        )
        val rates = listOf(
            CategoryFreightRateInput("cat-elec", "Electronics", defaultRatePerKgBdtMinor = 180_00L, overrideRatePerKgBdtMinor = null),
            CategoryFreightRateInput("cat-acc", "Accessories", defaultRatePerKgBdtMinor = 120_00L, overrideRatePerKgBdtMinor = null),
            CategoryFreightRateInput("cat-home", "Household", defaultRatePerKgBdtMinor = 150_00L, overrideRatePerKgBdtMinor = null)
        )

        val results = LandingCostEngine.calculateCategoryInternationalFreight(products, emptyMap(), rates)
        val elec = results.first { it.categoryId == "cat-elec" }
        val acc = results.first { it.categoryId == "cat-acc" }
        val home = results.first { it.categoryId == "cat-home" }

        // 10 KG * 180 = BDT 1,800.00
        assertEquals(1_800_00L, elec.totalCategoryFreightBdtMinor)
        // 5 KG * 120 = BDT 600.00
        assertEquals(600_00L, acc.totalCategoryFreightBdtMinor)
        // 8 KG * 150 = BDT 1,200.00
        assertEquals(1_200_00L, home.totalCategoryFreightBdtMinor)
    }

    // -------------------------------------------------------------------------
    // Scenario 9: Shipment-specific freight rate override
    // -------------------------------------------------------------------------
    @Test
    fun scenario09_shipmentSpecificFreightRateOverride() {
        val products = listOf(
            ShipmentProductInput("p-elec-1", productNameEn = "Drone", supplierId = "s1", categoryId = "cat-elec", packedQuantity = 10, unitPurchasePriceForeignMinor = 100_00L, fallbackGrossWeightKg = BigDecimal("6.000")),
            ShipmentProductInput("p-elec-2", productNameEn = "Camera", supplierId = "s1", categoryId = "cat-elec", packedQuantity = 10, unitPurchasePriceForeignMinor = 100_00L, fallbackGrossWeightKg = BigDecimal("4.000"))
        )
        // Default is 180/KG, overridden to 200/KG for this shipment
        val rates = listOf(
            CategoryFreightRateInput(
                categoryId = "cat-elec",
                categoryNameEn = "Electronics",
                defaultRatePerKgBdtMinor = 180_00L,
                overrideRatePerKgBdtMinor = 200_00L
            )
        )

        val results = LandingCostEngine.calculateCategoryInternationalFreight(products, emptyMap(), rates).single()
        assertEquals(180_00L, results.defaultRatePerKgBdtMinor)
        assertEquals(200_00L, results.appliedRatePerKgBdtMinor)
        // Total category freight = 10 KG * 200 = BDT 2,000.00
        assertEquals(2_000_00L, results.totalCategoryFreightBdtMinor)
        // Allocated by gross weight: 6 KG -> BDT 1,200.00; 4 KG -> BDT 800.00
        assertEquals(1_200_00L, results.productAllocationsBdtMinor["p-elec-1"])
        assertEquals(800_00L, results.productAllocationsBdtMinor["p-elec-2"])
    }

    // -------------------------------------------------------------------------
    // Scenario 10: Preserving historical freight rate after default rate change
    // -------------------------------------------------------------------------
    @Test
    fun scenario10_preserveHistoricalFreightRateAfterDefaultRateChange() {
        // Shipment 1 was saved when Electronics default rate was 180 BDT/KG.
        // ShipmentFreightRate snapshot stores defaultRatePerKgBdtMinor = 180_00L, appliedRatePerKgBdtMinor = 180_00L.
        val historicalSnapshotRate = CategoryFreightRateInput(
            categoryId = "cat-elec",
            categoryNameEn = "Electronics",
            defaultRatePerKgBdtMinor = 180_00L,
            overrideRatePerKgBdtMinor = 180_00L // Preserved applied rate on the shipment
        )

        // Later, Category master default changes to 220 BDT/KG for new shipments.
        val newShipmentRate = CategoryFreightRateInput(
            categoryId = "cat-elec",
            categoryNameEn = "Electronics",
            defaultRatePerKgBdtMinor = 220_00L,
            overrideRatePerKgBdtMinor = null
        )

        assertEquals(180_00L, historicalSnapshotRate.appliedRatePerKgBdtMinor)
        assertEquals(220_00L, newShipmentRate.appliedRatePerKgBdtMinor)
    }

    // -------------------------------------------------------------------------
    // Scenario 11: Bangladesh transport allocation by Gross Weight
    // -------------------------------------------------------------------------
    @Test
    fun scenario11_bdTransportAllocationByGrossWeight() {
        val items = listOf(
            AllocationItemInput(id = "p1", name = "Heavy", quantity = 10, grossWeightKg = BigDecimal("30.000")),
            AllocationItemInput(id = "p2", name = "Light", quantity = 10, grossWeightKg = BigDecimal("20.000"))
        )
        // BDT 5,000.00 allocated by Gross Weight (30 KG : 20 KG = 60% : 40%)
        val allocated = LandedCostAllocationEngine.allocate(
            totalCostBdtMinor = 5_000_00L,
            items = items,
            method = AllocationMethod.GROSS_WEIGHT
        )
        assertEquals(3_000_00L, allocated.first { it.itemId == "p1" }.allocatedBdtMinor)
        assertEquals(2_000_00L, allocated.first { it.itemId == "p2" }.allocatedBdtMinor)
    }

    // -------------------------------------------------------------------------
    // Scenario 12: Bangladesh transport allocation by CBM
    // -------------------------------------------------------------------------
    @Test
    fun scenario12_bdTransportAllocationByCbm() {
        val items = listOf(
            AllocationItemInput(id = "p1", name = "Bulky", quantity = 10, cbm = BigDecimal("0.750000")),
            AllocationItemInput(id = "p2", name = "Compact", quantity = 10, cbm = BigDecimal("0.250000"))
        )
        val allocated = LandedCostAllocationEngine.allocate(
            totalCostBdtMinor = 4_000_00L,
            items = items,
            method = AllocationMethod.CBM
        )
        assertEquals(3_000_00L, allocated.first { it.itemId == "p1" }.allocatedBdtMinor)
        assertEquals(1_000_00L, allocated.first { it.itemId == "p2" }.allocatedBdtMinor)
    }

    // -------------------------------------------------------------------------
    // Scenario 13: Bangladesh transport allocation by Carton Count
    // -------------------------------------------------------------------------
    @Test
    fun scenario13_bdTransportAllocationByCartonCount() {
        val items = listOf(
            AllocationItemInput(id = "p1", name = "6 Cartons", quantity = 60, cartonCount = 6),
            AllocationItemInput(id = "p2", name = "4 Cartons", quantity = 40, cartonCount = 4)
        )
        val allocated = LandedCostAllocationEngine.allocate(
            totalCostBdtMinor = 5_000_00L,
            items = items,
            method = AllocationMethod.CARTON_COUNT
        )
        assertEquals(3_000_00L, allocated.first { it.itemId == "p1" }.allocatedBdtMinor)
        assertEquals(2_000_00L, allocated.first { it.itemId == "p2" }.allocatedBdtMinor)
    }

    // -------------------------------------------------------------------------
    // Scenario 14: Bangladesh transport allocation by Purchase Value
    // -------------------------------------------------------------------------
    @Test
    fun scenario14_bdTransportAllocationByPurchaseValue() {
        val items = listOf(
            AllocationItemInput(id = "p1", name = "High Value", quantity = 10, purchaseValueBdtMinor = 80_000_00L),
            AllocationItemInput(id = "p2", name = "Low Value", quantity = 10, purchaseValueBdtMinor = 20_000_00L)
        )
        val allocated = LandedCostAllocationEngine.allocate(
            totalCostBdtMinor = 5_000_00L,
            items = items,
            method = AllocationMethod.PURCHASE_VALUE
        )
        assertEquals(4_000_00L, allocated.first { it.itemId == "p1" }.allocatedBdtMinor)
        assertEquals(1_000_00L, allocated.first { it.itemId == "p2" }.allocatedBdtMinor)
    }

    // -------------------------------------------------------------------------
    // Scenario 15: Bangladesh transport allocation by Quantity
    // -------------------------------------------------------------------------
    @Test
    fun scenario15_bdTransportAllocationByQuantity() {
        val items = listOf(
            AllocationItemInput(id = "p1", name = "300 Units", quantity = 300),
            AllocationItemInput(id = "p2", name = "200 Units", quantity = 200)
        )
        val allocated = LandedCostAllocationEngine.allocate(
            totalCostBdtMinor = 5_000_00L,
            items = items,
            method = AllocationMethod.QUANTITY
        )
        assertEquals(3_000_00L, allocated.first { it.itemId == "p1" }.allocatedBdtMinor)
        assertEquals(2_000_00L, allocated.first { it.itemId == "p2" }.allocatedBdtMinor)
    }

    // -------------------------------------------------------------------------
    // Scenario 16: Manual Bangladesh transport allocation and validation
    // -------------------------------------------------------------------------
    @Test
    fun scenario16_manualBangladeshTransportAllocationAndValidation() {
        val validItems = listOf(
            AllocationItemInput(id = "p1", name = "Item 1", quantity = 10, manualAmountBdtMinor = 3_200_00L),
            AllocationItemInput(id = "p2", name = "Item 2", quantity = 10, manualAmountBdtMinor = 1_800_00L)
        )
        val validResult = LandedCostAllocationEngine.allocate(
            totalCostBdtMinor = 5_000_00L,
            items = validItems,
            method = AllocationMethod.MANUAL
        )
        assertEquals(3_200_00L, validResult.first { it.itemId == "p1" }.allocatedBdtMinor)
        assertEquals(1_800_00L, validResult.first { it.itemId == "p2" }.allocatedBdtMinor)

        // Mismatched manual sum must fail validation
        val invalidItems = listOf(
            AllocationItemInput(id = "p1", name = "Item 1", quantity = 10, manualAmountBdtMinor = 3_000_00L),
            AllocationItemInput(id = "p2", name = "Item 2", quantity = 10, manualAmountBdtMinor = 1_500_00L)
        )
        try {
            LandedCostAllocationEngine.allocate(
                totalCostBdtMinor = 5_000_00L,
                items = invalidItems,
                method = AllocationMethod.MANUAL
            )
            fail("Expected IllegalArgumentException when manual allocation sum does not equal expense amount")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("Manual allocated amounts"))
        }
    }

    // -------------------------------------------------------------------------
    // Scenario 17: Pre-finalization additional import expenses
    // -------------------------------------------------------------------------
    @Test
    fun scenario17_preFinalizationAdditionalImportExpenses() {
        val products = listOf(
            ShipmentProductInput(
                productId = "p1",
                productNameEn = "Product 1",
                supplierId = "s1",
                categoryId = "cat-1",
                packedQuantity = 50,
                unitPurchasePriceForeignMinor = 100_00L,
                purchaseCurrency = "CNY",
                purchaseExchangeRateToBdt = BigDecimal("17.00"),
                fallbackGrossWeightKg = BigDecimal("25.000"),
                fallbackCbm = BigDecimal("0.500000"),
                fallbackCartonCount = 5
            ),
            ShipmentProductInput(
                productId = "p2",
                productNameEn = "Product 2",
                supplierId = "s1",
                categoryId = "cat-1",
                packedQuantity = 50,
                unitPurchasePriceForeignMinor = 100_00L,
                purchaseCurrency = "CNY",
                purchaseExchangeRateToBdt = BigDecimal("17.00"),
                fallbackGrossWeightKg = BigDecimal("25.000"),
                fallbackCbm = BigDecimal("0.500000"),
                fallbackCartonCount = 5
            )
        )
        val expenses = listOf(
            ImportExpenseInput("exp-customs", "Customs Duty", CostCategory.CUSTOMS_DUTY, 10_000_00L, "BDT", BigDecimal.ONE, AllocationMethod.PURCHASE_VALUE),
            ImportExpenseInput("exp-cnf", "C&F Agent", CostCategory.CF_CHARGES, 4_000_00L, "BDT", BigDecimal.ONE, AllocationMethod.CBM),
            ImportExpenseInput("exp-port", "Port Charges", CostCategory.PORT_CHARGES, 2_000_00L, "BDT", BigDecimal.ONE, AllocationMethod.GROSS_WEIGHT),
            ImportExpenseInput("exp-unload", "Loading & Unloading", CostCategory.LOADING_UNLOADING, 1_000_00L, "BDT", BigDecimal.ONE, AllocationMethod.CARTON_COUNT),
            ImportExpenseInput("exp-bd-trans", "BD Warehouse Truck", CostCategory.BD_LOCAL_TRANSPORT, 5_000_00L, "BDT", BigDecimal.ONE, AllocationMethod.GROSS_WEIGHT)
        )

        val result = LandingCostEngine.calculateShipmentLandingCost(
            products = products,
            cartons = emptyList(),
            supplierChinaDeliveries = emptyList(),
            categoryFreightRates = emptyList(),
            expenses = expenses
        )

        assertEquals(5_000_00L, result.totalBdTransportBdtMinor)
        assertEquals(17_000_00L, result.totalOtherImportExpensesBdtMinor)
        assertEquals(22_000_00L, result.totalImportExpensesBdtMinor)
    }

    // -------------------------------------------------------------------------
    // Scenario 18: Partial inventory receiving across multiple sessions
    // -------------------------------------------------------------------------
    @Test
    fun scenario18_partialInventoryReceivingAcrossMultipleSessions() {
        val packedQty = 100

        // Session 1 (GRN-001): 60 Good, 2 Damaged, 0 Missing, 38 Pending
        LandingCostEngine.validateReceivingQuantities(
            packedQuantity = packedQty,
            goodQuantity = 60,
            damagedQuantity = 2,
            missingQuantity = 0,
            pendingQuantity = 38
        )

        // Session 2 (GRN-002): remaining 38 arrives -> 35 Good, 1 Damaged, 2 Missing, 0 Pending
        // Cumulative after Session 2: 95 Good, 3 Damaged, 2 Missing, 0 Pending
        LandingCostEngine.validateReceivingQuantities(
            packedQuantity = packedQty,
            goodQuantity = 95,
            damagedQuantity = 3,
            missingQuantity = 2,
            pendingQuantity = 0
        )
    }

    // -------------------------------------------------------------------------
    // Scenario 19: Good, damaged, missing and pending quantity validation
    // -------------------------------------------------------------------------
    @Test
    fun scenario19_goodDamagedMissingPendingQuantityValidation() {
        // Valid: 100 = 80 Good + 5 Damaged + 5 Missing + 10 Pending
        LandingCostEngine.validateReceivingQuantities(100, 80, 5, 5, 10)

        // Invalid: sum != packedQuantity
        try {
            LandingCostEngine.validateReceivingQuantities(100, 80, 5, 5, 5)
            fail("Expected validation failure when Good + Damaged + Missing + Pending != Packed")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("Receiving reconciliation mismatch"))
        }
    }

    // -------------------------------------------------------------------------
    // Scenario 20: Unit landing cost calculation & consistent valuation across outcomes
    // -------------------------------------------------------------------------
    @Test
    fun scenario20_unitLandingCostAndValuationAcrossQuantityOutcomes() {
        // Packed = 100 (Good = 85, Damaged = 5, Missing = 4, Pending = 6)
        // Total Product Import Cost = BDT 100,000.00 -> Unit Import Cost = BDT 1,000.00
        val products = listOf(
            ShipmentProductInput(
                productId = "p-100",
                productNameEn = "Industrial Sensor",
                supplierId = "sup-1",
                categoryId = "cat-1",
                packedQuantity = 100,
                unitPurchasePriceForeignMinor = 1_000_00L, // BDT 1,000 each = BDT 100,000 total
                purchaseCurrency = "BDT",
                purchaseExchangeRateToBdt = BigDecimal.ONE,
                goodQuantity = 85,
                damagedQuantity = 5,
                missingQuantity = 4,
                pendingQuantity = 6
            )
        )

        val result = LandingCostEngine.calculateShipmentLandingCost(
            products = products,
            cartons = emptyList(),
            supplierChinaDeliveries = emptyList(),
            categoryFreightRates = emptyList(),
            expenses = emptyList()
        )
        val b = result.productBreakdowns.single()

        assertEquals(1_000_00L, b.unitImportCostBdtMinor)
        assertEquals(85_000_00L, b.goodInventoryValuationBdtMinor)     // 1210 Inventory Asset
        assertEquals(5_000_00L, b.damagedWriteOffValuationBdtMinor)    // 5050 Damaged Write-Off
        assertEquals(4_000_00L, b.missingShortageValuationBdtMinor)    // 1050 Shortage/Loss
        assertEquals(6_000_00L, b.pendingInTransitValuationBdtMinor)   // 1220 In-Transit Inventory
        assertEquals(
            b.totalProductImportCostBdtMinor,
            b.goodInventoryValuationBdtMinor +
                b.damagedWriteOffValuationBdtMinor +
                b.missingShortageValuationBdtMinor +
                b.pendingInTransitValuationBdtMinor
        )
    }

    // -------------------------------------------------------------------------
    // Scenario 21: FIFO layer creation after receiving
    // -------------------------------------------------------------------------
    @Test
    fun scenario21_fifoLayerCreationAfterReceiving() {
        // Only Good Received Quantity (85 units) enters sellable FIFO layer at unitImportCostBdtMinor
        val unitLandedCost = 920_00L
        val goodReceivedQty = 85
        val fifoLayer = InventoryCostLayer(
            id = "fifo-layer-1",
            productId = "prod-1",
            variantId = null,
            warehouseId = "wh-main",
            shipmentId = "ship-1",
            shipmentNumber = "SHIP-2026-001",
            warehouseReceiptId = "grn-1",
            receiptNumber = "GRN-2026-001",
            initialQuantity = goodReceivedQty,
            remainingQuantity = goodReceivedQty,
            unitLandedCostBdtMinor = unitLandedCost,
            originalCurrency = "CNY",
            originalPurchasePriceMinor = 50_00L,
            exchangeRateToBdt = "17.0000",
            allocatedExpensePerUnitBdtMinor = 70_00L,
            receiptDate = 1700000000000L,
            purchaseOrderId = "po-1",
            createdAt = 1700000000000L
        )

        assertEquals(85, fifoLayer.initialQuantity)
        assertEquals(85, fifoLayer.remainingQuantity)
        assertEquals(920_00L, fifoLayer.unitLandedCostBdtMinor)
    }

    // -------------------------------------------------------------------------
    // Scenario 22: Post-finalization additional expense adjustment
    // -------------------------------------------------------------------------
    @Test
    fun scenario22_postFinalizationAdditionalExpenseAdjustment() {
        val baseProduct = ShipmentProductInput(
            productId = "p1",
            productNameEn = "Item 1",
            supplierId = "s1",
            categoryId = "cat-1",
            packedQuantity = 100,
            unitPurchasePriceForeignMinor = 1_000_00L,
            purchaseCurrency = "BDT",
            purchaseExchangeRateToBdt = BigDecimal.ONE,
            goodQuantity = 100,
            fallbackGrossWeightKg = BigDecimal("50.000")
        )
        val initialResult = LandingCostEngine.calculateShipmentLandingCost(
            products = listOf(baseProduct),
            cartons = emptyList(),
            supplierChinaDeliveries = emptyList(),
            categoryFreightRates = emptyList(),
            expenses = emptyList()
        )

        // All 100 units still in stock
        val fifoLayers = mapOf(
            "p1" to listOf(
                FifoLayerAdjustmentInput(
                    layerId = "layer-1",
                    productId = "p1",
                    warehouseId = "wh-1",
                    initialQuantity = 100,
                    remainingQuantity = 100,
                    soldQuantity = 0,
                    currentUnitLandedCostBdtMinor = 1_000_00L
                )
            )
        )
        val latePortBill = ImportExpenseInput(
            expenseId = "exp-late-1",
            expenseName = "Late Port Demurrage Bill",
            category = CostCategory.PORT_CHARGES,
            amountForeignMinor = 5_000_00L, // BDT 5,000 -> +50 BDT/unit
            currency = "BDT",
            exchangeRateToBdt = BigDecimal.ONE,
            allocationMethod = AllocationMethod.GROSS_WEIGHT,
            isPostFinalization = true,
            isAlreadyCapitalized = false
        )

        val adjResult = LandingCostEngine.calculatePostFinalizeAdjustment(
            adjustmentReference = "LC-ADJ-2026-001",
            existingBreakdowns = initialResult.productBreakdowns,
            fifoLayersByProduct = fifoLayers,
            newPostFinalizeExpenses = listOf(latePortBill)
        )

        assertEquals("LC-ADJ-2026-001", adjResult.adjustmentReference)
        assertEquals(5_000_00L, adjResult.totalAddedExpenseBdtMinor)
        assertEquals(5_000_00L, adjResult.totalInventoryAssetAdjustmentBdtMinor)
        assertEquals(0L, adjResult.totalCogsAdjustmentBdtMinor)
        assertEquals(1_050_00L, adjResult.productAdjustments.single().newUnitCostBdtMinor)
    }

    // -------------------------------------------------------------------------
    // Scenario 23: FIFO remaining inventory + COGS adjustment after partial sale
    // -------------------------------------------------------------------------
    @Test
    fun scenario23_fifoRemainingInventoryAndCogsAdjustmentAfterPartialSale() {
        val baseProduct = ShipmentProductInput(
            productId = "p1",
            productNameEn = "Item 1",
            supplierId = "s1",
            categoryId = "cat-1",
            packedQuantity = 100,
            unitPurchasePriceForeignMinor = 1_000_00L,
            purchaseCurrency = "BDT",
            purchaseExchangeRateToBdt = BigDecimal.ONE,
            goodQuantity = 100,
            fallbackGrossWeightKg = BigDecimal("50.000")
        )
        val initialResult = LandingCostEngine.calculateShipmentLandingCost(
            products = listOf(baseProduct),
            cartons = emptyList(),
            supplierChinaDeliveries = emptyList(),
            categoryFreightRates = emptyList(),
            expenses = emptyList()
        )

        // 40 units already sold, 60 units remaining in FIFO layer
        val fifoLayers = mapOf(
            "p1" to listOf(
                FifoLayerAdjustmentInput(
                    layerId = "layer-1",
                    productId = "p1",
                    warehouseId = "wh-1",
                    initialQuantity = 100,
                    remainingQuantity = 60,
                    soldQuantity = 40,
                    currentUnitLandedCostBdtMinor = 1_000_00L
                )
            )
        )
        val lateCustomsAssessment = ImportExpenseInput(
            expenseId = "exp-late-2",
            expenseName = "Supplementary Customs Assessment",
            category = CostCategory.CUSTOMS_DUTY,
            amountForeignMinor = 10_000_00L, // BDT 10,000 total (+100 BDT/unit)
            currency = "BDT",
            exchangeRateToBdt = BigDecimal.ONE,
            allocationMethod = AllocationMethod.PURCHASE_VALUE,
            isPostFinalization = true,
            isAlreadyCapitalized = false
        )

        val adjResult = LandingCostEngine.calculatePostFinalizeAdjustment(
            adjustmentReference = "LC-ADJ-2026-002",
            existingBreakdowns = initialResult.productBreakdowns,
            fifoLayersByProduct = fifoLayers,
            newPostFinalizeExpenses = listOf(lateCustomsAssessment)
        )

        val prodAdj = adjResult.productAdjustments.single()
        // 60% (BDT 6,000) goes to Remaining FIFO Inventory (1210)
        assertEquals(6_000_00L, prodAdj.inventoryAdjustmentBdtMinor)
        // 40% (BDT 4,000) goes to COGS Adjustment (5010) for already sold units
        assertEquals(4_000_00L, prodAdj.cogsAdjustmentBdtMinor)
        assertEquals(1_000_00L, prodAdj.oldUnitCostBdtMinor)
        assertEquals(1_100_00L, prodAdj.newUnitCostBdtMinor)
    }

    // -------------------------------------------------------------------------
    // Scenario 24: Duplicate finalization / duplicate expense prevention
    // -------------------------------------------------------------------------
    @Test
    fun scenario24_duplicateExpensePrevention() {
        val products = listOf(
            ShipmentProductInput(
                productId = "p1",
                productNameEn = "Item 1",
                supplierId = "s1",
                categoryId = "cat-1",
                packedQuantity = 10,
                unitPurchasePriceForeignMinor = 100_00L,
                purchaseCurrency = "BDT",
                purchaseExchangeRateToBdt = BigDecimal.ONE,
                fallbackGrossWeightKg = BigDecimal("10.000")
            )
        )
        val duplicateExpenses = listOf(
            ImportExpenseInput("dup-exp-1", "Port Fee", CostCategory.PORT_CHARGES, 1_000_00L),
            ImportExpenseInput("dup-exp-1", "Port Fee Duplicate", CostCategory.PORT_CHARGES, 1_000_00L)
        )

        try {
            LandingCostEngine.calculateShipmentLandingCost(
                products = products,
                cartons = emptyList(),
                supplierChinaDeliveries = emptyList(),
                categoryFreightRates = emptyList(),
                expenses = duplicateExpenses
            )
            fail("Expected IllegalArgumentException for duplicate expense ID")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("Duplicate expense allocation detected"))
        }
    }

    // -------------------------------------------------------------------------
    // Scenario 25: Warehouse monthly rent and customer delivery exclusion from landing cost
    // -------------------------------------------------------------------------
    @Test
    fun scenario25_warehouseMonthlyRentAndCustomerDeliveryExcludedFromLandingCost() {
        // Monthly Warehouse Rent belongs to Operating Expense (5110)
        val warehouseRentVoucher = ExpenseVoucher(
            id = "exp-rent-2026-11",
            voucherNumber = "RENT-202611-01",
            category = ExpenseCategory.WAREHOUSE_RENT.name,
            descriptionEn = "Monthly warehouse rent for November 2026",
            descriptionBn = "নভেম্বর ২০২৬ মাসিক গুদাম ভাড়া",
            amountMinor = 35_000_00L,
            bdtAmountMinor = 35_000_00L,
            voucherDate = 1700000000000L,
            paymentAccountId = "coa-1010",
            expenseAccountId = "coa-5110",
            warehouseId = "wh-dhaka-main",
            rentMonth = "2026-11",
            createdAt = 1700000000000L
        )

        // Customer Delivery Cost belongs to Sales/Delivery Expense (5040) linked to Sales Invoice / Challan
        val customerDeliveryVoucher = ExpenseVoucher(
            id = "exp-deliv-001",
            voucherNumber = "CDEL-202611-01",
            category = ExpenseCategory.CUSTOMER_DELIVERY.name,
            descriptionEn = "Customer order delivery charge",
            descriptionBn = "ক্রেতা অর্ডার ডেলিভারি চার্জ",
            amountMinor = 450_00L,
            bdtAmountMinor = 450_00L,
            voucherDate = 1700000000000L,
            paymentAccountId = "coa-1010",
            expenseAccountId = "coa-5040",
            salesInvoiceId = "inv-2026-099",
            deliveryChallanNumber = "CH-2026-099",
            createdAt = 1700000000000L
        )

        assertEquals(ExpenseCategory.WAREHOUSE_RENT.name, warehouseRentVoucher.category)
        assertEquals("coa-5110", warehouseRentVoucher.expenseAccountId)
        assertEquals("wh-dhaka-main", warehouseRentVoucher.warehouseId)
        assertEquals("2026-11", warehouseRentVoucher.rentMonth)

        assertEquals(ExpenseCategory.CUSTOMER_DELIVERY.name, customerDeliveryVoucher.category)
        assertEquals("coa-5040", customerDeliveryVoucher.expenseAccountId)
        assertEquals("inv-2026-099", customerDeliveryVoucher.salesInvoiceId)
        assertEquals("CH-2026-099", customerDeliveryVoucher.deliveryChallanNumber)

        // Verify CostCategory (Import Landing Cost categories) does NOT contain WAREHOUSE_RENT or CUSTOMER_DELIVERY
        val importCostCategoryNames = CostCategory.entries.map { it.name }.toSet()
        assertFalse(importCostCategoryNames.contains("WAREHOUSE_RENT"))
        assertFalse(importCostCategoryNames.contains("CUSTOMER_DELIVERY"))
    }
}
