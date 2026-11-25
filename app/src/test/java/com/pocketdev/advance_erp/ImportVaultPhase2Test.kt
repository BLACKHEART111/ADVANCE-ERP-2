package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.core.engine.DeterministicAllocation
import com.pocketdev.advance_erp.core.engine.ImportVaultAnalyticsEngine
import com.pocketdev.advance_erp.core.engine.LandingCostEngine
import com.pocketdev.advance_erp.core.security.BiometricStatus
import com.pocketdev.advance_erp.core.security.ImportVaultAccessGuard
import com.pocketdev.advance_erp.core.security.UserRole
import com.pocketdev.advance_erp.core.security.VaultUnlockPolicy
import com.pocketdev.advance_erp.data.entity.AdditionalProductCost
import com.pocketdev.advance_erp.data.entity.CustomerPayment
import com.pocketdev.advance_erp.data.entity.DamageRecord
import com.pocketdev.advance_erp.data.entity.ExpenseAllocationRule
import com.pocketdev.advance_erp.data.entity.ExpenseVoucher
import com.pocketdev.advance_erp.data.entity.FifoConsumption
import com.pocketdev.advance_erp.data.entity.ImportCostItem
import com.pocketdev.advance_erp.data.entity.ImportShipment
import com.pocketdev.advance_erp.data.entity.InventoryCostLayer
import com.pocketdev.advance_erp.data.entity.InvoiceStatus
import com.pocketdev.advance_erp.data.entity.PaymentAllocation
import com.pocketdev.advance_erp.data.entity.Product
import com.pocketdev.advance_erp.data.entity.ProductCategory
import com.pocketdev.advance_erp.data.entity.ProductVariant
import com.pocketdev.advance_erp.data.entity.PurchaseOrder
import com.pocketdev.advance_erp.data.entity.PurchaseOrderItem
import com.pocketdev.advance_erp.data.entity.PurchaseOrderStatus
import com.pocketdev.advance_erp.data.entity.ReturnRefundOption
import com.pocketdev.advance_erp.data.entity.SaleLayerAllocation
import com.pocketdev.advance_erp.data.entity.SalesInvoice
import com.pocketdev.advance_erp.data.entity.SalesInvoiceItem
import com.pocketdev.advance_erp.data.entity.SalesReturn
import com.pocketdev.advance_erp.data.entity.SalesReturnItem
import com.pocketdev.advance_erp.data.entity.SalesReturnLayerAllocation
import com.pocketdev.advance_erp.data.entity.ShipmentCarton
import com.pocketdev.advance_erp.data.entity.ShipmentStatus
import com.pocketdev.advance_erp.data.entity.StockTransfer
import com.pocketdev.advance_erp.data.entity.StockTransferCostLayer
import com.pocketdev.advance_erp.data.entity.StockTransferStatus
import com.pocketdev.advance_erp.data.entity.Supplier
import com.pocketdev.advance_erp.data.entity.Warehouse
import com.pocketdev.advance_erp.data.entity.WarehouseReceipt
import com.pocketdev.advance_erp.data.entity.WarehouseReceiptItem
import com.pocketdev.advance_erp.domain.model.CostFinalizationStatus
import com.pocketdev.advance_erp.domain.model.ExpenseAllocationMode
import com.pocketdev.advance_erp.domain.model.ImportVaultDataset
import com.pocketdev.advance_erp.domain.model.ImportVaultFilter
import com.pocketdev.advance_erp.domain.model.ProfitContributionBaseStatus
import com.pocketdev.advance_erp.domain.model.StockAgingBucket
import com.pocketdev.advance_erp.domain.model.VaultScopeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class ImportVaultPhase2Test {

    private fun baseDataset(
        products: List<Product> = listOf(
            Product(id = "prod-a", categoryId = "cat-1", nameEn = "Product A", nameBn = "পণ্য এ", baseSku = "SKU-A", unit = "PCS", purchasePriceMinor = 100_00L, sellingPriceMinor = 180_00L),
            Product(id = "prod-b", categoryId = "cat-1", nameEn = "Product B", nameBn = "পণ্য বি", baseSku = "SKU-B", unit = "PCS", purchasePriceMinor = 120_00L, sellingPriceMinor = 200_00L)
        ),
        variants: List<ProductVariant> = emptyList(),
        warehouses: List<Warehouse> = listOf(
            Warehouse(id = "wh-1", name = "Main", code = "WH-1", location = "Dhaka", isDefault = true),
            Warehouse(id = "wh-2", name = "Branch", code = "WH-2", location = "Chattogram", isDefault = false)
        ),
        suppliers: List<Supplier> = listOf(
            Supplier(id = "sup-1", supplierCode = "SUP-1", companyName = "Guangzhou Factory", phone = "01700000001", address = "Guangzhou"),
            Supplier(id = "sup-2", supplierCode = "SUP-2", companyName = "Shenzhen Tech", phone = "01700000002", address = "Shenzhen")
        ),
        purchaseOrders: List<PurchaseOrder> = emptyList(),
        purchaseOrderItems: List<PurchaseOrderItem> = emptyList(),
        shipments: List<ImportShipment> = emptyList(),
        cartons: List<ShipmentCarton> = emptyList(),
        importCosts: List<ImportCostItem> = emptyList(),
        warehouseReceipts: List<WarehouseReceipt> = emptyList(),
        warehouseReceiptItems: List<WarehouseReceiptItem> = emptyList(),
        costLayers: List<InventoryCostLayer> = emptyList(),
        fifoConsumptions: List<FifoConsumption> = emptyList(),
        stockTransfers: List<StockTransfer> = emptyList(),
        stockTransferCostLayers: List<StockTransferCostLayer> = emptyList(),
        damageRecords: List<DamageRecord> = emptyList(),
        additionalProductCosts: List<AdditionalProductCost> = emptyList(),
        salesInvoices: List<SalesInvoice> = emptyList(),
        salesInvoiceItems: List<SalesInvoiceItem> = emptyList(),
        saleLayerAllocations: List<SaleLayerAllocation> = emptyList(),
        salesReturns: List<SalesReturn> = emptyList(),
        salesReturnItems: List<SalesReturnItem> = emptyList(),
        salesReturnLayerAllocations: List<SalesReturnLayerAllocation> = emptyList(),
        customerPayments: List<CustomerPayment> = emptyList(),
        paymentAllocations: List<PaymentAllocation> = emptyList(),
        expenseVouchers: List<ExpenseVoucher> = emptyList(),
        expenseAllocationRules: List<ExpenseAllocationRule> = emptyList(),
        customerOpeningDueTotalMinor: Long = 0L
    ) = ImportVaultDataset(
        products = products,
        variants = variants,
        categories = listOf(ProductCategory(id = "cat-1", nameEn = "General", nameBn = "সাধারণ", code = "GEN")),
        warehouses = warehouses,
        suppliers = suppliers,
        purchaseOrders = purchaseOrders,
        purchaseOrderItems = purchaseOrderItems,
        supplierPayments = emptyList(),
        shipments = shipments,
        cartons = cartons,
        shipmentSuppliers = emptyList(),
        freightRates = emptyList(),
        importCosts = importCosts,
        landedAllocations = emptyList(),
        landedSnapshots = emptyList(),
        warehouseReceipts = warehouseReceipts,
        warehouseReceiptItems = warehouseReceiptItems,
        costLayers = costLayers,
        costAdjustments = emptyList(),
        fifoConsumptions = fifoConsumptions,
        stockTransfers = stockTransfers,
        stockTransferCostLayers = stockTransferCostLayers,
        damageRecords = damageRecords,
        shortageRecords = emptyList(),
        additionalProductCosts = additionalProductCosts,
        salesInvoices = salesInvoices,
        salesInvoiceItems = salesInvoiceItems,
        saleLayerAllocations = saleLayerAllocations,
        salesReturns = salesReturns,
        salesReturnItems = salesReturnItems,
        salesReturnLayerAllocations = salesReturnLayerAllocations,
        customerPayments = customerPayments,
        paymentAllocations = paymentAllocations,
        expenseVouchers = expenseVouchers,
        expenseAllocationRules = expenseAllocationRules,
        expenseAllocations = emptyList(),
        vaultEntries = emptyList(),
        vaultAttachments = emptyList(),
        customerOpeningDueTotalMinor = customerOpeningDueTotalMinor
    )

    private fun po(id: String, num: String, supId: String = "sup-1", totalBdt: Long, date: Long = 1000L, finalized: Boolean = true, status: String = PurchaseOrderStatus.RECEIVED.name) =
        PurchaseOrder(
            id = id, poNumber = num, supplierId = supId, status = status,
            currency = "BDT", exchangeRateToBdt = "1.0", totalAmountMinor = totalBdt,
            totalBdtAmountMinor = totalBdt, orderDate = date, isActualCostFinalized = finalized,
            createdAt = date, updatedAt = date
        )

    private fun shp(id: String, num: String, poId: String?, supId: String = "sup-1", date: Long = 1000L, status: String = ShipmentStatus.RECEIVED.name) =
        ImportShipment(
            id = id, shipmentNumber = num, supplierId = supId, purchaseOrderId = poId,
            shippingDate = date, status = status, createdAt = date, updatedAt = date
        )

    private fun layer(id: String, prodId: String, poId: String?, shpId: String?, initQty: Int, remQty: Int, unitCost: Long, date: Long = 1000L, sourceType: String = "IMPORT_RECEIPT", whId: String = "wh-1", varId: String? = null, origDate: Long = date) =
        InventoryCostLayer(
            id = id, productId = prodId, variantId = varId, warehouseId = whId,
            shipmentId = shpId ?: "RET", shipmentNumber = shpId ?: "RET",
            warehouseReceiptId = "wr-1", receiptNumber = "WR-1",
            initialQuantity = initQty, remainingQuantity = remQty,
            unitLandedCostBdtMinor = unitCost, originalCurrency = "BDT",
            originalPurchasePriceMinor = unitCost, exchangeRateToBdt = "1.0",
            allocatedExpensePerUnitBdtMinor = 0L, receiptDate = date, isActive = remQty > 0,
            purchaseOrderId = poId, originReceiptDate = origDate, layerSourceType = sourceType, createdAt = date
        )

    @Test
    fun scenario01_fifoCrossPoSalesAndPartialDepletion() {
        val ds = baseDataset(
            purchaseOrders = listOf(po("po-1", "PO-1", totalBdt = 1000_00L, date = 1000L), po("po-2", "PO-2", totalBdt = 1200_00L, date = 2000L)),
            purchaseOrderItems = listOf(
                PurchaseOrderItem("poi-1", "po-1", "prod-a", null, 10, 100_00L, 1000_00L, 1000_00L, 10),
                PurchaseOrderItem("poi-2", "po-2", "prod-a", null, 10, 120_00L, 1200_00L, 1200_00L, 10)
            ),
            shipments = listOf(shp("shp-1", "SHP-1", "po-1", date = 1000L), shp("shp-2", "SHP-2", "po-2", date = 2000L)),
            costLayers = listOf(
                layer("l-1", "prod-a", "po-1", "shp-1", 10, 0, 100_00L, date = 1000L),
                layer("l-2", "prod-a", "po-2", "shp-2", 10, 5, 120_00L, date = 2000L)
            ),
            salesInvoices = listOf(
                SalesInvoice(
                    id = "inv-1", invoiceNumber = "INV-1", customerId = "c1", customerName = "Cust",
                    warehouseId = "wh-1", warehouseName = "Main", status = InvoiceStatus.PAID.name,
                    subtotalMinor = 2700_00L, totalAmountMinor = 2700_00L, paidAmountMinor = 2700_00L, dueAmountMinor = 0L,
                    totalCogsMinor = 1600_00L, invoiceDate = 3000L, isPosted = true
                )
            ),
            saleLayerAllocations = listOf(
                SaleLayerAllocation("sla-1", "inv-1", "ii-1", "l-1", "prod-a", null, "wh-1", "po-1", "poi-1", "shp-1", 10, 100_00L, 1000_00L, 1800_00L, 0L, 0L, 1800_00L, createdAt = 3000L),
                SaleLayerAllocation("sla-2", "inv-1", "ii-1", "l-2", "prod-a", null, "wh-1", "po-2", "poi-2", "shp-2", 5, 120_00L, 600_00L, 900_00L, 0L, 0L, 900_00L, createdAt = 3000L)
            ),
            customerPayments = listOf(CustomerPayment("cp-1", "PAY-1", "c1", "Cust", 2700_00L, 2700_00L, 0L, paymentDate = 3000L, isPosted = true)),
            paymentAllocations = listOf(PaymentAllocation("pa-1", "cp-1", "inv-1", "INV-1", 2700_00L, 3000L))
        )

        val overview = ImportVaultAnalyticsEngine.computeOverview(ds, ImportVaultFilter(nowMillis = 5000L))
        assertEquals(2700_00L, overview.totalAttributedNetRevenueMinor)
        assertEquals(1600_00L, overview.totalAttributedCogsMinor)
        assertEquals(1100_00L, overview.totalAttributedGrossProfitMinor)
        assertEquals(1100_00L, overview.totalAttributedNetProfitMinor)
        assertEquals(600_00L, overview.totalLockedInventoryCapitalMinor)

        val po1 = overview.purchaseOrders.first { it.purchaseOrderId == "po-1" }
        assertEquals(10, po1.netSoldQtyByUnit.singleUnitTotal)
        assertEquals(1800_00L, po1.netSalesRevenueMinor)
        assertEquals(1000_00L, po1.cogsMinor)
        assertEquals(800_00L, po1.grossProfitMinor)
        assertEquals(0, po1.remainingStockQtyByUnit.singleUnitTotal)

        val po2 = overview.purchaseOrders.first { it.purchaseOrderId == "po-2" }
        assertEquals(5, po2.netSoldQtyByUnit.singleUnitTotal)
        assertEquals(900_00L, po2.netSalesRevenueMinor)
        assertEquals(600_00L, po2.cogsMinor)
        assertEquals(300_00L, po2.grossProfitMinor)
        assertEquals(5, po2.remainingStockQtyByUnit.singleUnitTotal)
        assertEquals(600_00L, po2.lockedInventoryCapitalMinor)
    }

    @Test
    fun scenario02_invoiceWideDiscountsAdditionalChargesAndRoundingResidual() {
        val items = listOf(
            SalesInvoiceItem("ii-a", "inv-1", "prod-a", null, "A", "A", "A", 1, 0, 1000_00L, 0L, 1000_00L),
            SalesInvoiceItem("ii-b", "inv-1", "prod-b", null, "B", "B", "B", 1, 0, 500_00L, 0L, 500_00L),
            SalesInvoiceItem("ii-c", "inv-1", "prod-c", null, "C", "C", "C", 1, 0, 500_00L, 0L, 500_00L)
        )
        val discMap = DeterministicAllocation.allocateInvoiceDiscountAcrossItems(200_00L, items)
        assertEquals(100_00L, discMap["ii-a"])
        assertEquals(50_00L, discMap["ii-b"])
        assertEquals(50_00L, discMap["ii-c"])
        assertEquals(200_00L, discMap.values.sum())

        // Odd-paisa rounding check: 100 paisa across 3 equal weights = 34 + 33 + 33
        val oddWeights = listOf("1" to BigDecimal.ONE, "2" to BigDecimal.ONE, "3" to BigDecimal.ONE)
        val oddAlloc = DeterministicAllocation.allocateProportionally(100L, oddWeights)
        assertEquals(100L, oddAlloc.values.sum())
        assertEquals(listOf(34L, 33L, 33L), oddAlloc.values.sortedDescending())

        // Verify taxMinor excluded from GP and tracked separately
        val ds = baseDataset(
            products = listOf(
                Product("prod-a", "cat-1", "A", "A", "A", unit = "PCS", purchasePriceMinor = 500_00L, sellingPriceMinor = 1000_00L),
                Product("prod-b", "cat-1", "B", "B", "B", unit = "PCS", purchasePriceMinor = 250_00L, sellingPriceMinor = 500_00L),
                Product("prod-c", "cat-1", "C", "C", "C", unit = "PCS", purchasePriceMinor = 250_00L, sellingPriceMinor = 500_00L)
            ),
            purchaseOrders = listOf(po("po-1", "PO-1", totalBdt = 1000_00L)),
            shipments = listOf(shp("shp-1", "SHP-1", "po-1")),
            costLayers = listOf(
                layer("l-a", "prod-a", "po-1", "shp-1", 1, 0, 500_00L),
                layer("l-b", "prod-b", "po-1", "shp-1", 1, 0, 250_00L),
                layer("l-c", "prod-c", "po-1", "shp-1", 1, 0, 250_00L)
            ),
            salesInvoices = listOf(
                SalesInvoice(
                    id = "inv-1", invoiceNumber = "INV-1", customerId = "c1", customerName = "Cust",
                    warehouseId = "wh-1", warehouseName = "Main", status = InvoiceStatus.PAID.name,
                    subtotalMinor = 2000_00L, discountMinor = 200_00L, taxMinor = 150_00L, totalAmountMinor = 1950_00L,
                    paidAmountMinor = 1950_00L, dueAmountMinor = 0L, totalCogsMinor = 1000_00L, invoiceDate = 2000L, isPosted = true
                )
            ),
            saleLayerAllocations = listOf(
                SaleLayerAllocation("s-a", "inv-1", "ii-a", "l-a", "prod-a", null, "wh-1", "po-1", null, "shp-1", 1, 500_00L, 500_00L, 1000_00L, 0L, discMap["ii-a"]!!, 900_00L),
                SaleLayerAllocation("s-b", "inv-1", "ii-b", "l-b", "prod-b", null, "wh-1", "po-1", null, "shp-1", 1, 250_00L, 250_00L, 500_00L, 0L, discMap["ii-b"]!!, 450_00L),
                SaleLayerAllocation("s-c", "inv-1", "ii-c", "l-c", "prod-c", null, "wh-1", "po-1", null, "shp-1", 1, 250_00L, 250_00L, 500_00L, 0L, discMap["ii-c"]!!, 450_00L)
            )
        )
        val ov = ImportVaultAnalyticsEngine.computeOverview(ds, ImportVaultFilter(nowMillis = 5000L))
        assertEquals(1800_00L, ov.totalAttributedNetRevenueMinor) // excludes taxMinor 150_00L
        assertEquals(1000_00L, ov.totalAttributedCogsMinor)
        assertEquals(800_00L, ov.totalAttributedGrossProfitMinor)
        assertEquals(150_00L, ov.unknownAttribution.nonMerchandiseAdditionalChargesMinor)
    }

    @Test
    fun scenario03_partialLinkedGoodAndDamagedReturnsNoDoubleDeduction() {
        val ds = baseDataset(
            purchaseOrders = listOf(po("po-1", "PO-1", totalBdt = 500_00L), po("po-2", "PO-2", totalBdt = 600_00L)),
            shipments = listOf(shp("shp-1", "SHP-1", "po-1"), shp("shp-2", "SHP-2", "po-2")),
            costLayers = listOf(
                layer("l-1", "prod-a", "po-1", "shp-1", 5, 0, 100_00L, date = 1000L),
                layer("l-2", "prod-a", "po-2", "shp-2", 5, 0, 120_00L, date = 1500L),
                // Restored layer for 2 sound returned units linked to po-2
                layer("l-ret", "prod-a", "po-2", "shp-2", 2, 2, 120_00L, date = 1500L, sourceType = "SALES_RETURN_RESTORE")
            ),
            salesInvoices = listOf(
                SalesInvoice("inv-1", "INV-1", "c1", "Cust", warehouseId = "wh-1", warehouseName = "Main", status = InvoiceStatus.PAID.name, subtotalMinor = 2000_00L, totalAmountMinor = 2000_00L, paidAmountMinor = 2000_00L, dueAmountMinor = 0L, totalCogsMinor = 1100_00L, invoiceDate = 2000L, isPosted = true)
            ),
            saleLayerAllocations = listOf(
                SaleLayerAllocation("sla-1", "inv-1", "ii-1", "l-1", "prod-a", null, "wh-1", "po-1", null, "shp-1", 5, 100_00L, 500_00L, 1000_00L, 0L, 0L, 1000_00L),
                SaleLayerAllocation("sla-2", "inv-1", "ii-1", "l-2", "prod-a", null, "wh-1", "po-2", null, "shp-2", 5, 120_00L, 600_00L, 1000_00L, 0L, 0L, 1000_00L)
            ),
            salesReturns = listOf(
                SalesReturn("ret-1", "RET-1", "inv-1", "INV-1", "c1", "Cust", "wh-1", 600_00L, 340_00L, ReturnRefundOption.CUSTOMER_CREDIT.name, returnDate = 3000L, isPosted = true)
            ),
            salesReturnLayerAllocations = listOf(
                // 2 good units linked to PO-2
                SalesReturnLayerAllocation("rla-1", "ret-1", "ri-1", "inv-1", "ii-1", "sla-2", "l-2", "l-ret", "prod-a", null, "po-2", null, "shp-2", 2, 0, 400_00L, 0L, 400_00L, 240_00L, 0L, createdAt = 3000L),
                // 1 damaged unit linked to PO-1
                SalesReturnLayerAllocation("rla-2", "ret-1", "ri-1", "inv-1", "ii-1", "sla-1", "l-1", null, "prod-a", null, "po-1", null, "shp-1", 0, 1, 200_00L, 0L, 200_00L, 0L, 100_00L, createdAt = 3000L)
            )
        )

        val ov = ImportVaultAnalyticsEngine.computeOverview(ds, ImportVaultFilter(nowMillis = 5000L))
        val po2 = ov.purchaseOrders.first { it.purchaseOrderId == "po-2" }
        assertEquals(3, po2.netSoldQtyByUnit.singleUnitTotal)
        assertEquals(600_00L, po2.netSalesRevenueMinor)
        assertEquals(360_00L, po2.cogsMinor)
        assertEquals(240_00L, po2.grossProfitMinor)
        assertEquals(0L, po2.inventoryLossesMinor)
        assertEquals(240_00L, po2.attributedNetProfitMinor)
        assertEquals(2, po2.remainingStockQtyByUnit.singleUnitTotal) // restored to po-2!

        val po1 = ov.purchaseOrders.first { it.purchaseOrderId == "po-1" }
        assertEquals(4, po1.netSoldQtyByUnit.singleUnitTotal)
        assertEquals(800_00L, po1.netSalesRevenueMinor)
        assertEquals(400_00L, po1.cogsMinor)
        assertEquals(400_00L, po1.grossProfitMinor)
        assertEquals(100_00L, po1.inventoryLossesMinor)
        assertEquals(300_00L, po1.attributedNetProfitMinor) // 400 - 100, never double-counted
        assertEquals(0, po1.remainingStockQtyByUnit.singleUnitTotal)
    }

    @Test
    fun scenario04_contributionPercentagesAndZeroNegativeEdgeCases() {
        val pos = ImportVaultAnalyticsEngine.calculateContribution(1000_00L, 800_00L)
        val neg = ImportVaultAnalyticsEngine.calculateContribution(-200_00L, 800_00L)
        assertEquals(ProfitContributionBaseStatus.POSITIVE_BASE, pos.baseStatus)
        assertEquals(125.0, pos.contributionPercent!!, 0.001)
        assertEquals(-25.0, neg.contributionPercent!!, 0.001)
        assertEquals(100.0, pos.contributionPercent!! + neg.contributionPercent!!, 0.001)

        val zeroBase = ImportVaultAnalyticsEngine.calculateContribution(100_00L, 0L)
        assertEquals(ProfitContributionBaseStatus.ZERO_BASE, zeroBase.baseStatus)
        assertNull(zeroBase.contributionPercent)

        val lossBase = ImportVaultAnalyticsEngine.calculateContribution(-200_00L, -500_00L)
        assertEquals(ProfitContributionBaseStatus.LOSS_BASE, lossBase.baseStatus)
        assertNull(lossBase.contributionPercent)

        // Zero net revenue -> null margin; parent margin = sum(GP)/sum(Rev), never average of child margins
        val ds = baseDataset(
            products = listOf(
                Product("prod-a", "cat-1", "High Margin", "A", "A", unit = "PCS", purchasePriceMinor = 50_00L, sellingPriceMinor = 100_00L),
                Product("prod-b", "cat-1", "Zero Rev", "B", "B", unit = "PCS", purchasePriceMinor = 50_00L, sellingPriceMinor = 100_00L),
                Product("prod-c", "cat-1", "Loss SKU", "C", "C", unit = "PCS", purchasePriceMinor = 50_00L, sellingPriceMinor = 30_00L)
            ),
            purchaseOrders = listOf(po("po-1", "PO-1", totalBdt = 1000_00L)),
            shipments = listOf(shp("shp-1", "SHP-1", "po-1")),
            costLayers = listOf(
                layer("l-a", "prod-a", "po-1", "shp-1", 10, 0, 50_00L),
                layer("l-b", "prod-b", "po-1", "shp-1", 10, 10, 50_00L),
                layer("l-c", "prod-c", "po-1", "shp-1", 10, 0, 50_00L)
            ),
            salesInvoices = listOf(
                SalesInvoice("inv-1", "INV-1", "c1", "Cust", warehouseId = "wh-1", warehouseName = "Main", status = InvoiceStatus.PAID.name, subtotalMinor = 1300_00L, totalAmountMinor = 1300_00L, totalCogsMinor = 1000_00L, invoiceDate = 2000L, isPosted = true)
            ),
            saleLayerAllocations = listOf(
                SaleLayerAllocation("s-a", "inv-1", "ii-a", "l-a", "prod-a", null, "wh-1", "po-1", null, "shp-1", 10, 50_00L, 500_00L, 1000_00L, 0L, 0L, 1000_00L), // 50% margin
                SaleLayerAllocation("s-c", "inv-1", "ii-c", "l-c", "prod-c", null, "wh-1", "po-1", null, "shp-1", 10, 50_00L, 500_00L, 300_00L, 0L, 0L, 300_00L) // -66.67% margin
            )
        )
        val detail = ImportVaultAnalyticsEngine.computeShipmentDetail(ds, "shp-1", ImportVaultFilter(nowMillis = 5000L))!!
        // Combined GP = 1300 - 1000 = 300; Combined margin = 300 / 1300 = 23.0769% (NOT average (50 - 66.67)/2)
        assertEquals(23.0769, detail.summary.grossMarginPercent!!, 0.01)
        val zeroProd = detail.products.first { it.productId == "prod-b" }
        assertNull(zeroProd.grossMarginPercent)
    }

    @Test
    fun scenario05_multiPoShipmentAndSplitPoAcrossShipmentsZeroCollision() {
        // Test LandingCostEngine collision fix when PO-A and PO-B share prod-a in Shipment-1
        val engineRes = LandingCostEngine.calculateShipmentLandingCost(
            products = listOf(
                LandingCostEngine.ShipmentProductInput("prod-a", null, "Item A", supplierId = "sup-1", purchaseOrderId = "po-a", categoryId = "cat-1", packedQuantity = 10, unitPurchasePriceForeignMinor = 100_00L, purchaseCurrency = "BDT", purchaseExchangeRateToBdt = BigDecimal.ONE, fallbackGrossWeightKg = BigDecimal("10")),
                LandingCostEngine.ShipmentProductInput("prod-a", null, "Item A", supplierId = "sup-2", purchaseOrderId = "po-b", categoryId = "cat-1", packedQuantity = 20, unitPurchasePriceForeignMinor = 200_00L, purchaseCurrency = "BDT", purchaseExchangeRateToBdt = BigDecimal.ONE, fallbackGrossWeightKg = BigDecimal("20"))
            ),
            cartons = listOf(
                LandingCostEngine.ShipmentCartonInput("C-1", "sup-1", "prod-a", null, "cat-1", 10, BigDecimal("10"), purchaseOrderId = "po-a"),
                LandingCostEngine.ShipmentCartonInput("C-2", "sup-2", "prod-a", null, "cat-1", 20, BigDecimal("20"), purchaseOrderId = "po-b")
            ),
            supplierChinaDeliveries = emptyList(),
            categoryFreightRates = listOf(LandingCostEngine.CategoryFreightRateInput("cat-1", "General", 10_00L)),
            expenses = emptyList()
        )
        assertEquals(2, engineRes.productBreakdowns.size)
        val bA = engineRes.productBreakdowns.first { it.purchaseOrderId == "po-a" }
        val bB = engineRes.productBreakdowns.first { it.purchaseOrderId == "po-b" }
        assertEquals(110_00L, bA.unitImportCostBdtMinor) // 100 + 10/kg
        assertEquals(210_00L, bB.unitImportCostBdtMinor) // 200 + 10/kg

        // Test Vault aggregation: Shipment-1 has PO-A + PO-B; PO-C split across Shipment-2 & Shipment-3
        val ds = baseDataset(
            purchaseOrders = listOf(
                po("po-a", "PO-A", "sup-1", 1000_00L),
                po("po-b", "PO-B", "sup-2", 4000_00L),
                po("po-c", "PO-C", "sup-1", 2000_00L)
            ),
            purchaseOrderItems = listOf(
                PurchaseOrderItem("poi-a", "po-a", "prod-a", null, 10, 100_00L, 1000_00L, 1000_00L, 10),
                PurchaseOrderItem("poi-b", "po-b", "prod-a", null, 20, 200_00L, 4000_00L, 4000_00L, 20),
                PurchaseOrderItem("poi-c", "po-c", "prod-b", null, 20, 100_00L, 2000_00L, 2000_00L, 20)
            ),
            shipments = listOf(
                shp("shp-1", "SHP-1", null, "sup-1"),
                shp("shp-2", "SHP-2", "po-c", "sup-1"),
                shp("shp-3", "SHP-3", "po-c", "sup-1")
            ),
            costLayers = listOf(
                layer("la", "prod-a", "po-a", "shp-1", 10, 0, 100_00L),
                layer("lb", "prod-a", "po-b", "shp-1", 20, 20, 200_00L),
                layer("lc2", "prod-b", "po-c", "shp-2", 10, 0, 100_00L),
                layer("lc3", "prod-b", "po-c", "shp-3", 10, 10, 100_00L)
            ),
            salesInvoices = listOf(
                SalesInvoice("inv-1", "INV-1", "c1", "Cust", warehouseId = "wh-1", warehouseName = "Main", status = InvoiceStatus.PAID.name, subtotalMinor = 3000_00L, totalAmountMinor = 3000_00L, totalCogsMinor = 2000_00L, invoiceDate = 2000L, isPosted = true)
            ),
            saleLayerAllocations = listOf(
                SaleLayerAllocation("sa", "inv-1", "i1", "la", "prod-a", null, "wh-1", "po-a", "poi-a", "shp-1", 10, 100_00L, 1000_00L, 1500_00L, 0L, 0L, 1500_00L),
                SaleLayerAllocation("sc", "inv-1", "i2", "lc2", "prod-b", null, "wh-1", "po-c", "poi-c", "shp-2", 10, 100_00L, 1000_00L, 1500_00L, 0L, 0L, 1500_00L)
            )
        )
        val ov = ImportVaultAnalyticsEngine.computeOverview(ds, ImportVaultFilter(nowMillis = 5000L))
        // No double counting across shipments & POs
        assertEquals(7000_00L, ov.totalInvestedCapitalMinor)
        assertEquals(5000_00L, ov.totalLockedInventoryCapitalMinor)
        assertEquals(3000_00L, ov.totalAttributedNetRevenueMinor)
        assertEquals(2000_00L, ov.totalAttributedCogsMinor)
        assertEquals(1000_00L, ov.totalAttributedGrossProfitMinor)
        val s1 = ov.shipments.first { it.shipmentId == "shp-1" }
        assertEquals(setOf("po-a", "po-b"), s1.purchaseOrderIds.toSet())
        val poC = ov.purchaseOrders.first { it.purchaseOrderId == "po-c" }
        assertEquals(setOf("shp-2", "shp-3"), poC.shipmentIds.toSet())
        assertEquals(1500_00L, poC.netSalesRevenueMinor)
        assertEquals(1000_00L, poC.lockedInventoryCapitalMinor)
    }

    @Test
    fun scenario06_partialReceivingAndProvisionalCostStatus() {
        val ds = baseDataset(
            purchaseOrders = listOf(po("po-1", "PO-1", totalBdt = 1000_00L, finalized = false, status = PurchaseOrderStatus.PARTIALLY_RECEIVED.name)),
            purchaseOrderItems = listOf(PurchaseOrderItem("poi-1", "po-1", "prod-a", null, 100, 10_00L, 1000_00L, 1000_00L, 60)),
            shipments = listOf(shp("shp-1", "SHP-1", "po-1", status = ShipmentStatus.RECEIVED.name)),
            importCosts = listOf(ImportCostItem("ic-1", "shp-1", "CUSTOMS_DUTY", "Est Duty", 200_00L, bdtAmountMinor = 200_00L, allocationMethod = "QUANTITY", purchaseOrderId = "po-1", isEstimated = true)),
            warehouseReceipts = listOf(WarehouseReceipt("wr-1", "WR-1", "shp-1", "wh-1", purchaseOrderId = "po-1", status = "POSTED")),
            warehouseReceiptItems = listOf(WarehouseReceiptItem("wri-1", "wr-1", null, "prod-a", null, "po-1", "poi-1", 100, 60, 0, 0, 0, 12_00L, goodQuantity = 60, pendingQuantity = 40)),
            costLayers = listOf(layer("l-1", "prod-a", "po-1", "shp-1", 60, 60, 12_00L))
        )
        val ov = ImportVaultAnalyticsEngine.computeOverview(ds, ImportVaultFilter(nowMillis = 5000L))
        val po1 = ov.purchaseOrders.single()
        assertEquals(CostFinalizationStatus.PROVISIONAL, po1.costFinalizationStatus)
        assertTrue(po1.hasPendingReceiving)
        assertEquals(100, po1.orderedQtyByUnit.singleUnitTotal)
        assertEquals(60, po1.receivedGoodQtyByUnit.singleUnitTotal)
        assertEquals(40, po1.pendingReceivingQtyByUnit.singleUnitTotal)
        assertEquals(60, po1.remainingStockQtyByUnit.singleUnitTotal)
        assertEquals(720_00L, po1.lockedInventoryCapitalMinor)
    }

    @Test
    fun scenario07_sharedOverheadDefaultUnallocatedOptInRuleAndCapitalizableAdditionalCost() {
        // 100 units @ 100 = 10,000; 40 sold -> COGS 4,000; Capitalizable AdditionalCost +1,000 split 600 inv / 400 cogs; non-capitalizable +300 direct expense; shared rent +2,000
        val ds = baseDataset(
            purchaseOrders = listOf(po("po-1", "PO-1", totalBdt = 10000_00L, date = 1000L)),
            shipments = listOf(shp("shp-1", "SHP-1", "po-1", date = 1000L)),
            costLayers = listOf(layer("l-1", "prod-a", "po-1", "shp-1", 100, 60, 110_00L, date = 1000L)), // already updated to 110 after cap cost
            additionalProductCosts = listOf(
                AdditionalProductCost("ac-cap", "AC-1", "po-1", "wh-1", "HANDLING", "Port Repack", 1000_00L, bdtAmountMinor = 1000_00L, isCapitalizable = true, allocatedToInventoryMinor = 600_00L, allocatedToCogsMinor = 400_00L, expenseDate = 2500L),
                AdditionalProductCost("ac-op", "AC-2", "po-1", "wh-1", "WAREHOUSE_RENT", "Direct PO Storage", 300_00L, bdtAmountMinor = 300_00L, isCapitalizable = false, expenseDate = 2500L)
            ),
            salesInvoices = listOf(
                SalesInvoice("inv-1", "INV-1", "c1", "Cust", warehouseId = "wh-1", warehouseName = "Main", status = InvoiceStatus.PAID.name, subtotalMinor = 8000_00L, totalAmountMinor = 8000_00L, totalCogsMinor = 4000_00L, invoiceDate = 2000L, isPosted = true)
            ),
            saleLayerAllocations = listOf(
                SaleLayerAllocation("sla-1", "inv-1", "ii-1", "l-1", "prod-a", null, "wh-1", "po-1", null, "shp-1", 40, 100_00L, 4000_00L, 8000_00L, 0L, 0L, 8000_00L)
            ),
            expenseVouchers = listOf(
                ExpenseVoucher("ev-rent", "RENT-1", "WAREHOUSE_RENT", "Rent", "ভাড়া", 2000_00L, bdtAmountMinor = 2000_00L, paymentAccountId = "coa-1010", expenseAccountId = "coa-5110", voucherDate = 2800L)
            )
        )

        // Default mode: shared overhead unallocated
        val ovDefault = ImportVaultAnalyticsEngine.computeOverview(ds, ImportVaultFilter(nowMillis = 5000L))
        assertEquals(ExpenseAllocationMode.UNALLOCATED, ovDefault.expenseAllocationMode)
        assertEquals("Attributed Net Profit", ovDefault.profitLabel)
        assertEquals(2000_00L, ovDefault.unallocatedSharedOverheadMinor)
        assertEquals(0L, ovDefault.totalAllocatedSharedOverheadMinor)
        assertEquals(4400_00L, ovDefault.totalAttributedCogsMinor) // 4000 + 400 sold adjustment = 40 * 110
        assertEquals(6600_00L, ovDefault.totalLockedInventoryCapitalMinor) // 60 * 110
        assertEquals(11000_00L, ovDefault.totalInvestedCapitalMinor) // 4400 + 6600 exact
        assertEquals(3600_00L, ovDefault.totalAttributedGrossProfitMinor) // 8000 - 4400
        assertEquals(300_00L, ovDefault.totalDirectAttributedExpensesMinor)
        assertEquals(3300_00L, ovDefault.totalAttributedNetProfitMinor) // 3600 - 300

        // Opt-in rule: allocate shared rent by NET_REVENUE
        val rule = ExpenseAllocationRule("rule-1", "Rent By Rev", 1, "NET_REVENUE", 0L, 5000L, VaultScopeType.PURCHASE_ORDER.name, isActive = true)
        val dsWithRule = ds.copy(expenseAllocationRules = listOf(rule))
        val ovAlloc = ImportVaultAnalyticsEngine.computeOverview(dsWithRule, ImportVaultFilter(allocationRuleId = "rule-1", nowMillis = 5000L))
        assertEquals(ExpenseAllocationMode.ALLOCATED, ovAlloc.expenseAllocationMode)
        assertEquals("Net Profit (Allocated)", ovAlloc.profitLabel)
        assertEquals(2000_00L, ovAlloc.totalAllocatedSharedOverheadMinor)
        assertEquals(1300_00L, ovAlloc.totalAttributedNetProfitMinor) // 3300 - 2000
    }

    @Test
    fun scenario08_warehouseTransferPreservesOriginAgingAndNoDoubleCounting() {
        val t0 = 0L
        val dayMs = 24L * 3600L * 1000L
        val t45 = t0 + 45L * dayMs // 45 days old -> 31-60 bucket!
        val ds = baseDataset(
            purchaseOrders = listOf(po("po-1", "PO-1", totalBdt = 1000_00L, date = t0)),
            shipments = listOf(shp("shp-1", "SHP-1", "po-1", date = t0)),
            costLayers = listOf(
                // Original wh-1 layer: initially 10, 5 transferred to wh-2 (3 in transit, 2 received at wh-2)
                layer("l-orig", "prod-a", "po-1", "shp-1", 10, 5, 100_00L, date = t0, sourceType = "IMPORT_RECEIPT", whId = "wh-1", origDate = t0),
                layer("l-repl", "prod-a", "po-1", "shp-1", 2, 2, 100_00L, date = t45 - dayMs, sourceType = "TRANSFER_REPLICA", whId = "wh-2", origDate = t0)
            ),
            stockTransfers = listOf(
                StockTransfer("tr-1", "TR-1", "wh-1", "wh-2", "prod-a", null, 3, t45 - 2 * dayMs, status = StockTransferStatus.IN_TRANSIT.name, dispatchedAt = t45 - 2 * dayMs),
                StockTransfer("tr-2", "TR-2", "wh-1", "wh-2", "prod-a", null, 2, t45 - 5 * dayMs, status = StockTransferStatus.RECEIVED.name, dispatchedAt = t45 - 5 * dayMs, receivedAt = t45 - dayMs)
            ),
            stockTransferCostLayers = listOf(
                StockTransferCostLayer("tl-1", "tr-1", "l-orig", 3, 100_00L),
                StockTransferCostLayer("tl-2", "tr-2", "l-orig", 2, 100_00L)
            )
        )
        val poSummary = ImportVaultAnalyticsEngine.computePurchaseOrderDetail(ds, "po-1", ImportVaultFilter(nowMillis = t45))!!.summary
        assertEquals(10, poSummary.receivedGoodQtyByUnit.singleUnitTotal) // never 10 + 2 = 12
        assertEquals(10, poSummary.remainingStockQtyByUnit.singleUnitTotal) // 5 wh-1 + 2 wh-2 + 3 in-transit
        assertEquals(1000_00L, poSummary.lockedInventoryCapitalMinor)
        // Aging must use originReceiptDate = t0 (45 days old -> DAYS_31_60), NOT recent transfer date!
        assertEquals(10, poSummary.agingSummary.bucketQuantities[StockAgingBucket.DAYS_31_60])
        assertEquals(0, poSummary.agingSummary.bucketQuantities[StockAgingBucket.DAYS_0_30])
        assertEquals(45, poSummary.agingSummary.oldestAgeDays)
    }

    @Test
    fun scenario09_creditSalesPartialPaymentsRefundsUnallocatedAndOpeningDue() {
        val ds = baseDataset(
            purchaseOrders = listOf(po("po-1", "PO-1", totalBdt = 500_00L)),
            shipments = listOf(shp("shp-1", "SHP-1", "po-1")),
            costLayers = listOf(
                layer("l-1", "prod-a", "po-1", "shp-1", 10, 0, 50_00L, date = 1000L),
                layer("l-ret", "prod-a", "po-1", "shp-1", 1, 1, 50_00L, date = 1000L, sourceType = "SALES_RETURN_RESTORE")
            ),
            salesInvoices = listOf(
                SalesInvoice("inv-1", "INV-1", "c1", "Cust", warehouseId = "wh-1", warehouseName = "Main", status = InvoiceStatus.PARTIALLY_PAID.name, subtotalMinor = 1000_00L, totalAmountMinor = 1000_00L, paidAmountMinor = 400_00L, dueAmountMinor = 600_00L, totalCogsMinor = 500_00L, invoiceDate = 2000L, isPosted = true)
            ),
            saleLayerAllocations = listOf(
                SaleLayerAllocation("s-1", "inv-1", "i-1", "l-1", "prod-a", null, "wh-1", "po-1", null, "shp-1", 10, 50_00L, 500_00L, 1000_00L, 0L, 0L, 1000_00L)
            ),
            salesReturns = listOf(
                SalesReturn("ret-1", "RET-1", "inv-1", "INV-1", "c1", "Cust", "wh-1", 100_00L, 50_00L, ReturnRefundOption.CASH_REFUND.name, returnDate = 3000L, isPosted = true)
            ),
            salesReturnLayerAllocations = listOf(
                SalesReturnLayerAllocation("r-1", "ret-1", "ri-1", "inv-1", "i-1", "s-1", "l-1", "l-ret", "prod-a", null, "po-1", null, "shp-1", 1, 0, 100_00L, 0L, 100_00L, 50_00L, 0L, createdAt = 3000L)
            ),
            customerPayments = listOf(
                CustomerPayment("cp-1", "PAY-1", "c1", "Cust", 400_00L, 400_00L, 0L, paymentDate = 2500L, isPosted = true),
                CustomerPayment("cp-adv", "PAY-2", "c1", "Cust", 500_00L, 0L, 500_00L, paymentDate = 2600L, isAdvance = true, isPosted = true)
            ),
            paymentAllocations = listOf(PaymentAllocation("pa-1", "cp-1", "inv-1", "INV-1", 400_00L, 2500L)),
            customerOpeningDueTotalMinor = 2000_00L
        )

        val ov = ImportVaultAnalyticsEngine.computeOverview(ds, ImportVaultFilter(nowMillis = 5000L))
        // 400 collected - 100 cash refund = 300 attributable collected (500 standalone & 2000 opening due stay strictly separated)
        assertEquals(300_00L, ov.cashRecovery.attributableCashCollectedMinor)
        assertEquals(600_00L, ov.cashRecovery.attributableReceivableMinor) // 900 net rev - 300 net cash = 600
        assertEquals(500_00L, ov.unknownAttribution.unattributedCollectionsMinor)
        assertEquals(2000_00L, ov.unknownAttribution.openingDueReceivableMinor)
        assertEquals(60.0, ov.cashRecovery.cashRecoveryRatioPercent!!, 0.001)
    }

    @Test
    fun scenario10_cancelledDraftExclusionsUnitIsolationAndExpectedRemainingProfit() {
        val ds = baseDataset(
            products = listOf(
                Product("prod-pcs", "cat-1", "Mouse", "Mouse", "M", unit = "PCS", purchasePriceMinor = 100_00L, sellingPriceMinor = 150_00L),
                Product("prod-pair", "cat-1", "Shoes", "Shoes", "S", unit = "PAIR", purchasePriceMinor = 200_00L, sellingPriceMinor = 300_00L)
            ),
            purchaseOrders = listOf(po("po-1", "PO-1", totalBdt = 1500_00L)),
            purchaseOrderItems = listOf(
                PurchaseOrderItem("p1", "po-1", "prod-pcs", null, 5, 100_00L, 500_00L, 500_00L, 5),
                PurchaseOrderItem("p2", "po-1", "prod-pair", null, 5, 200_00L, 1000_00L, 1000_00L, 5)
            ),
            shipments = listOf(shp("shp-1", "SHP-1", "po-1")),
            costLayers = listOf(
                layer("l-pcs", "prod-pcs", "po-1", "shp-1", 5, 5, 100_00L),
                layer("l-pair", "prod-pair", "po-1", "shp-1", 5, 5, 200_00L)
            ),
            salesInvoices = listOf(
                SalesInvoice("inv-can", "INV-CAN", "c1", "Cust", warehouseId = "wh-1", warehouseName = "Main", status = InvoiceStatus.CANCELLED.name, totalAmountMinor = 999_00L, totalCogsMinor = 500_00L, invoiceDate = 2000L, isPosted = true),
                SalesInvoice("inv-draft", "INV-DRF", "c1", "Cust", warehouseId = "wh-1", warehouseName = "Main", status = InvoiceStatus.DRAFT.name, totalAmountMinor = 888_00L, totalCogsMinor = 400_00L, invoiceDate = 2000L, isPosted = false)
            ),
            saleLayerAllocations = listOf(
                SaleLayerAllocation("s-can", "inv-can", "i1", "l-pcs", "prod-pcs", null, "wh-1", "po-1", null, "shp-1", 2, 100_00L, 200_00L, 999_00L, 0L, 0L, 999_00L, isReversed = true)
            )
        )
        val ov = ImportVaultAnalyticsEngine.computeOverview(ds, ImportVaultFilter(nowMillis = 5000L))
        assertEquals(0L, ov.totalAttributedNetRevenueMinor)
        assertEquals(0L, ov.totalAttributedCogsMinor)

        val po1 = ov.purchaseOrders.single()
        assertTrue(po1.orderedQtyByUnit.hasMixedUnits)
        assertNull(po1.orderedQtyByUnit.singleUnitTotal) // never sums PCS + PAIR
        assertEquals(5, po1.orderedQtyByUnit.byUom["PCS"])
        assertEquals(5, po1.orderedQtyByUnit.byUom["PAIR"])

        // Expected remaining gross profit: (5*150 + 5*300) - (5*100 + 5*200) = 2250 - 1500 = 750
        assertEquals(1500_00L, ov.expectedRemainingProfit.lockedGoodInventoryCostMinor)
        assertEquals(2250_00L, ov.expectedRemainingProfit.expectedSellingRevenueMinor)
        assertEquals(750_00L, ov.expectedRemainingProfit.expectedGrossProfitMinor)
        assertEquals("Current Selling Price", ov.expectedRemainingProfit.priceBasisLabel)
    }

    @Test
    fun scenario11_asOfReconstructionLegacyUnknownBucketCoverageAndAccessControl() {
        val ds = baseDataset(
            purchaseOrders = listOf(po("po-1", "PO-1", totalBdt = 1000_00L, date = 1000L)),
            shipments = listOf(shp("shp-1", "SHP-1", "po-1", date = 1000L)),
            costLayers = listOf(
                layer("l-1", "prod-a", "po-1", "shp-1", 10, 4, 100_00L, date = 1000L),
                layer("l-legacy", "prod-b", null, null, 3, 3, 50_00L, date = 500L, sourceType = "MANUAL_ADJUSTMENT")
            ),
            salesInvoices = listOf(
                SalesInvoice("inv-t2", "INV-2", "c1", "Cust", warehouseId = "wh-1", warehouseName = "Main", status = InvoiceStatus.POSTED.name, subtotalMinor = 1200_00L, totalAmountMinor = 1200_00L, totalCogsMinor = 600_00L, invoiceDate = 4000L, isPosted = true),
                SalesInvoice("inv-unk", "INV-UNK", "c1", "Cust", warehouseId = "wh-1", warehouseName = "Main", status = InvoiceStatus.PAID.name, subtotalMinor = 400_00L, totalAmountMinor = 400_00L, totalCogsMinor = 150_00L, invoiceDate = 2000L, isPosted = true)
            ),
            saleLayerAllocations = listOf(
                SaleLayerAllocation("s-1", "inv-t2", "i-1", "l-1", "prod-a", null, "wh-1", "po-1", null, "shp-1", 6, 100_00L, 600_00L, 1200_00L, 0L, 0L, 1200_00L, createdAt = 4000L),
                SaleLayerAllocation("s-unk", "inv-unk", "i-2", null, "prod-b", null, "wh-1", null, null, null, 2, 75_00L, 150_00L, 400_00L, 0L, 0L, 400_00L, isUnattributedFallback = true, createdAt = 2000L)
            )
        )

        // As of t=3000 (before inv-t2 at t=4000): all 10 units remain in PO-1 layer
        val asOfBefore = ImportVaultAnalyticsEngine.computeOverview(ds, ImportVaultFilter(asOfMillis = 3000L, nowMillis = 6000L))
        assertEquals(0L, asOfBefore.totalAttributedNetRevenueMinor)
        assertEquals(1000_00L, asOfBefore.totalLockedInventoryCapitalMinor)
        assertEquals(400_00L, asOfBefore.unknownAttribution.unattributedRevenueMinor)
        assertEquals(0.0, asOfBefore.unknownAttribution.revenueAttributionCoveragePercent!!, 0.001)

        // Current (t=6000): 1200 attributed, 400 unattributed -> 75.0% coverage; legacy adj layer in unattributed inventory
        val current = ImportVaultAnalyticsEngine.computeOverview(ds, ImportVaultFilter(nowMillis = 6000L))
        assertEquals(1200_00L, current.totalAttributedNetRevenueMinor)
        assertEquals(400_00L, current.unknownAttribution.unattributedRevenueMinor)
        assertEquals(150_00L, current.unknownAttribution.unattributedInventoryCostMinor)
        assertEquals(75.0, current.unknownAttribution.revenueAttributionCoveragePercent!!, 0.001)

        // Access control guard checks (Policy Gate #7)
        assertEquals(VaultUnlockPolicy.REQUIRE_BIOMETRIC, ImportVaultAccessGuard.evaluateAccess(UserRole.OWNER_ADMIN, BiometricStatus.AVAILABLE))
        assertEquals(VaultUnlockPolicy.SAFE_FALLBACK_NO_BIOMETRIC, ImportVaultAccessGuard.evaluateAccess(UserRole.OWNER_ADMIN, BiometricStatus.NOT_ENROLLED))
        assertTrue(ImportVaultAccessGuard.canRevealVaultWithBiometricFallback(UserRole.OWNER_ADMIN, BiometricStatus.NOT_SUPPORTED))
        for (nonOwner in listOf(UserRole.MANAGER, UserRole.ACCOUNTANT, UserRole.SALES_OPERATOR, UserRole.PURCHASE_OPERATOR, UserRole.INVENTORY_OPERATOR)) {
            assertEquals(VaultUnlockPolicy.DENIED_ROLE, ImportVaultAccessGuard.evaluateAccess(nonOwner, BiometricStatus.AVAILABLE))
            assertFalse(ImportVaultAccessGuard.canRevealVaultWithBiometricFallback(nonOwner, BiometricStatus.NOT_ENROLLED))
            var threw = false
            try {
                ImportVaultAccessGuard.requireVaultAccess(nonOwner)
            } catch (_: SecurityException) {
                threw = true
            }
            assertTrue("Expected SecurityException for role $nonOwner", threw)
        }
    }
}
