package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.core.engine.ImportVaultAnalyticsEngine
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.data.entity.AdditionalCostCategory
import com.pocketdev.advance_erp.data.entity.AdditionalProductCost
import com.pocketdev.advance_erp.data.entity.DamageRecord
import com.pocketdev.advance_erp.data.entity.ExpenseAllocationRule
import com.pocketdev.advance_erp.data.entity.ExpenseVoucher
import com.pocketdev.advance_erp.data.entity.ImportCostItem
import com.pocketdev.advance_erp.data.entity.ImportShipment
import com.pocketdev.advance_erp.data.entity.ImportVaultAttachment
import com.pocketdev.advance_erp.data.entity.InventoryCostLayer
import com.pocketdev.advance_erp.data.entity.InvoiceStatus
import com.pocketdev.advance_erp.data.entity.LandedCostSnapshot
import com.pocketdev.advance_erp.data.entity.Product
import com.pocketdev.advance_erp.data.entity.ProductCategory
import com.pocketdev.advance_erp.data.entity.ProductVariant
import com.pocketdev.advance_erp.data.entity.PurchaseOrder
import com.pocketdev.advance_erp.data.entity.PurchaseOrderItem
import com.pocketdev.advance_erp.data.entity.PurchaseOrderStatus
import com.pocketdev.advance_erp.data.entity.SaleLayerAllocation
import com.pocketdev.advance_erp.data.entity.SalesInvoice
import com.pocketdev.advance_erp.data.entity.ShipmentStatus
import com.pocketdev.advance_erp.data.entity.ShortageRecord
import com.pocketdev.advance_erp.data.entity.Supplier
import com.pocketdev.advance_erp.data.entity.SupplierPayment
import com.pocketdev.advance_erp.data.entity.Warehouse
import com.pocketdev.advance_erp.data.entity.WarehouseReceipt
import com.pocketdev.advance_erp.data.entity.WarehouseReceiptItem
import com.pocketdev.advance_erp.domain.model.ImportVaultDataset
import com.pocketdev.advance_erp.domain.model.ImportVaultFilter
import com.pocketdev.advance_erp.domain.model.VaultDateFilterTarget
import com.pocketdev.advance_erp.domain.model.VaultDocumentCategory
import com.pocketdev.advance_erp.domain.model.VaultScopeType
import com.pocketdev.advance_erp.feature.importvault.ImportVaultUiLogic
import com.pocketdev.advance_erp.feature.importvault.ProfitSortOption
import com.pocketdev.advance_erp.feature.importvault.VaultDatePreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ImportVaultAuditFixesTest {

    private val dayMs = 24L * 3600L * 1000L

    private fun mkProduct(
        id: String,
        nameEn: String,
        nameBn: String,
        sku: String,
        uom: String = "PCS",
        costMinor: Long = 100_00L,
        sellMinor: Long = 250_00L
    ) = Product(
        id = id,
        categoryId = "cat-1",
        nameEn = nameEn,
        nameBn = nameBn,
        baseSku = sku,
        unit = uom,
        purchasePriceMinor = costMinor,
        sellingPriceMinor = sellMinor
    )

    private fun mkLayer(
        id: String,
        productId: String,
        variantId: String? = null,
        shipmentId: String = "shp-1",
        shipmentNumber: String = "SHP-1",
        poId: String = "po-1",
        poItemId: String? = null,
        initialQty: Int,
        remainingQty: Int,
        unitLandedCostMinor: Long,
        receiptDate: Long
    ) = InventoryCostLayer(
        id = id,
        productId = productId,
        variantId = variantId,
        warehouseId = "wh-1",
        shipmentId = shipmentId,
        shipmentNumber = shipmentNumber,
        warehouseReceiptId = "wr-1",
        receiptNumber = "WR-1",
        initialQuantity = initialQty,
        remainingQuantity = remainingQty,
        unitLandedCostBdtMinor = unitLandedCostMinor,
        originalCurrency = "BDT",
        originalPurchasePriceMinor = unitLandedCostMinor,
        exchangeRateToBdt = "1.0",
        allocatedExpensePerUnitBdtMinor = 0L,
        receiptDate = receiptDate,
        originReceiptDate = receiptDate,
        purchaseOrderId = poId,
        purchaseOrderItemId = poItemId,
        layerSourceType = "IMPORT_RECEIPT",
        createdAt = receiptDate
    )

    private fun mkSaleAlloc(
        id: String,
        invoiceId: String,
        invoiceItemId: String,
        costLayerId: String,
        productId: String,
        variantId: String? = null,
        poId: String = "po-1",
        shipmentId: String = "shp-1",
        qty: Int,
        unitCostMinor: Long,
        netRevMinor: Long,
        createdAt: Long
    ) = SaleLayerAllocation(
        id = id,
        invoiceId = invoiceId,
        invoiceItemId = invoiceItemId,
        costLayerId = costLayerId,
        productId = productId,
        variantId = variantId,
        warehouseId = "wh-1",
        purchaseOrderId = poId,
        shipmentId = shipmentId,
        quantity = qty,
        unitCostBdtMinor = unitCostMinor,
        totalCogsBdtMinor = qty.toLong() * unitCostMinor,
        allocatedGrossRevenueMinor = netRevMinor,
        allocatedLineDiscountMinor = 0L,
        allocatedInvoiceDiscountMinor = 0L,
        allocatedNetRevenueMinor = netRevMinor,
        createdAt = createdAt
    )

    private fun mkInvoice(
        id: String,
        number: String,
        revMinor: Long,
        cogsMinor: Long,
        invoiceDate: Long
    ) = SalesInvoice(
        id = id,
        invoiceNumber = number,
        customerId = "c-1",
        customerName = "Cust",
        warehouseId = "wh-1",
        warehouseName = "Main",
        status = InvoiceStatus.PAID.name,
        subtotalMinor = revMinor,
        totalAmountMinor = revMinor,
        paidAmountMinor = revMinor,
        totalCogsMinor = cogsMinor,
        invoiceDate = invoiceDate,
        isPosted = true
    )

    private fun baseEmptyDataset(
        products: List<Product> = emptyList(),
        variants: List<ProductVariant> = emptyList(),
        purchaseOrders: List<PurchaseOrder> = emptyList(),
        purchaseOrderItems: List<PurchaseOrderItem> = emptyList(),
        supplierPayments: List<SupplierPayment> = emptyList(),
        shipments: List<ImportShipment> = emptyList(),
        importCosts: List<ImportCostItem> = emptyList(),
        landedSnapshots: List<LandedCostSnapshot> = emptyList(),
        warehouseReceipts: List<WarehouseReceipt> = emptyList(),
        warehouseReceiptItems: List<WarehouseReceiptItem> = emptyList(),
        costLayers: List<InventoryCostLayer> = emptyList(),
        damageRecords: List<DamageRecord> = emptyList(),
        shortageRecords: List<ShortageRecord> = emptyList(),
        additionalCosts: List<AdditionalProductCost> = emptyList(),
        salesInvoices: List<SalesInvoice> = emptyList(),
        saleLayerAllocations: List<SaleLayerAllocation> = emptyList(),
        expenseVouchers: List<ExpenseVoucher> = emptyList(),
        vaultAttachments: List<ImportVaultAttachment> = emptyList()
    ) = ImportVaultDataset(
        products = products,
        variants = variants,
        categories = listOf(ProductCategory("cat-1", "General", "সাধারণ", "GEN")),
        warehouses = listOf(Warehouse(id = "wh-1", name = "Main", code = "WH-1", location = "Dhaka", isDefault = true)),
        suppliers = listOf(Supplier(id = "sup-1", supplierCode = "SUP-1", companyName = "Shenzhen Trade", phone = "017", address = "SZ")),
        purchaseOrders = purchaseOrders,
        purchaseOrderItems = purchaseOrderItems,
        supplierPayments = supplierPayments,
        shipments = shipments,
        cartons = emptyList(),
        shipmentSuppliers = emptyList(),
        freightRates = emptyList(),
        importCosts = importCosts,
        landedAllocations = emptyList(),
        landedSnapshots = landedSnapshots,
        warehouseReceipts = warehouseReceipts,
        warehouseReceiptItems = warehouseReceiptItems,
        costLayers = costLayers,
        costAdjustments = emptyList(),
        fifoConsumptions = emptyList(),
        stockTransfers = emptyList(),
        stockTransferCostLayers = emptyList(),
        damageRecords = damageRecords,
        shortageRecords = shortageRecords,
        additionalProductCosts = additionalCosts,
        salesInvoices = salesInvoices,
        salesInvoiceItems = emptyList(),
        saleLayerAllocations = saleLayerAllocations,
        salesReturns = emptyList(),
        salesReturnItems = emptyList(),
        salesReturnLayerAllocations = emptyList(),
        customerPayments = emptyList(),
        paymentAllocations = emptyList(),
        expenseVouchers = expenseVouchers,
        expenseAllocationRules = emptyList(),
        expenseAllocations = emptyList(),
        vaultEntries = emptyList(),
        vaultAttachments = vaultAttachments,
        customerOpeningDueTotalMinor = 0L
    )

    @Test
    fun activityVsCohortDateFilter_separatesPeriodActivityFromLifetimeRoiAndComputesOpeningClosingStock() {
        val now = 1_700_000_000_000L
        val prod = mkProduct("p-1", "Cloud Insole", "ক্লাউড ইনসোল", "SKU-1", "PCS", 200_00L, 450_00L)
        val v1 = ProductVariant("v-1", "p-1", "SKU-1-L", "Black", "L", 450_00L, 200_00L)
        val po1 = PurchaseOrder(
            id = "po-1", poNumber = "PO-1", supplierId = "sup-1", status = PurchaseOrderStatus.RECEIVED.name,
            totalAmountMinor = 2_000_00L, totalBdtAmountMinor = 2_000_00L, paidBdtAmountMinor = 1_200_00L,
            orderDate = now - 40L * dayMs, shipmentId = "shp-1", isActualCostFinalized = true
        )
        val shp1 = ImportShipment(
            id = "shp-1", shipmentNumber = "SHP-1", supplierId = "sup-1", purchaseOrderId = "po-1",
            shippingDate = now - 35L * dayMs, status = ShipmentStatus.RECEIVED.name, totalGrossWeightKg = "10.0"
        )
        val layer1 = mkLayer(
            id = "l-1", productId = "p-1", variantId = "v-1", poItemId = "poi-1",
            initialQty = 10, remainingQty = 3, unitLandedCostMinor = 200_00L, receiptDate = now - 30L * dayMs
        )
        val invOld = mkInvoice("inv-old", "INV-OLD", 1_600_00L, 800_00L, now - 20L * dayMs)
        val invRecent = mkInvoice("inv-recent", "INV-NEW", 1_350_00L, 600_00L, now - 2L * dayMs)
        val allocs = listOf(
            mkSaleAlloc("sa-1", "inv-old", "ii-1", "l-1", "p-1", "v-1", qty = 4, unitCostMinor = 200_00L, netRevMinor = 1_600_00L, createdAt = now - 20L * dayMs),
            mkSaleAlloc("sa-2", "inv-recent", "ii-2", "l-1", "p-1", "v-1", qty = 3, unitCostMinor = 200_00L, netRevMinor = 1_350_00L, createdAt = now - 2L * dayMs)
        )
        val ds = baseEmptyDataset(
            products = listOf(prod),
            variants = listOf(v1),
            purchaseOrders = listOf(po1),
            purchaseOrderItems = listOf(PurchaseOrderItem("poi-1", "po-1", "p-1", "v-1", 10, 200_00L, 2_000_00L, 2_000_00L, 10)),
            shipments = listOf(shp1),
            costLayers = listOf(layer1),
            salesInvoices = listOf(invOld, invRecent),
            saleLayerAllocations = allocs
        )

        val activityFilter = ImportVaultFilter(
            dateFromMillis = now - 10L * dayMs,
            dateToMillis = now,
            asOfMillis = now,
            nowMillis = now,
            dateFilterTarget = VaultDateFilterTarget.ACTIVITY_DATES
        )
        val actOverview = ImportVaultAnalyticsEngine.computeOverview(ds, activityFilter)
        assertTrue(actOverview.isPeriodActivityFiltered)
        assertEquals(1, actOverview.shipmentCount)
        assertEquals(1, actOverview.purchaseOrderCount)
        assertEquals(2_000_00L, actOverview.totalInvestedCapitalMinor)
        assertEquals(1_350_00L, actOverview.totalAttributedNetRevenueMinor)
        assertEquals(600_00L, actOverview.totalAttributedCogsMinor)
        assertEquals(750_00L, actOverview.totalAttributedNetProfitMinor)
        assertEquals(1_550_00L, actOverview.lifetimeNetProfitMinor)
        assertEquals(77.5, actOverview.lifetimePortfolioRoiPercent!!, 0.01)
        assertEquals(1_200_00L, actOverview.openingStockCostMinor)
        assertEquals(600_00L, actOverview.closingStockCostMinor)

        val shpSummary = actOverview.shipments.single()
        assertEquals(1_350_00L, shpSummary.netSalesRevenueMinor)
        assertEquals(750_00L, shpSummary.attributedNetProfitMinor)
        assertEquals(1_550_00L, shpSummary.lifetimeNetProfitMinor)
        assertEquals(77.5, shpSummary.lifetimeRoiPercent!!, 0.01)

        val recentCohortOverview = ImportVaultAnalyticsEngine.computeOverview(
            ds,
            activityFilter.copy(dateFilterTarget = VaultDateFilterTarget.IMPORT_COHORT_DATES)
        )
        assertFalse(recentCohortOverview.isPeriodActivityFiltered)
        assertEquals(0, recentCohortOverview.shipmentCount)
        assertEquals(0, recentCohortOverview.purchaseOrderCount)
        assertEquals(0L, recentCohortOverview.totalInvestedCapitalMinor)
        assertEquals(0L, recentCohortOverview.totalAttributedNetRevenueMinor)

        val matchedCohortOverview = ImportVaultAnalyticsEngine.computeOverview(
            ds,
            ImportVaultFilter(
                dateFromMillis = now - 45L * dayMs,
                dateToMillis = now - 33L * dayMs,
                asOfMillis = now,
                nowMillis = now,
                dateFilterTarget = VaultDateFilterTarget.IMPORT_COHORT_DATES
            )
        )
        assertEquals(1, matchedCohortOverview.purchaseOrderCount)
        assertEquals(2_950_00L, matchedCohortOverview.totalAttributedNetRevenueMinor)
        assertEquals(1_550_00L, matchedCohortOverview.totalAttributedNetProfitMinor)
    }

    @Test
    fun sharedOverheadQuantityBasis_rejectsMixedUnitsAndAllocatesUniformUnitsDeterministically() {
        val now = 1_700_000_000_000L
        val prodPcs = mkProduct("p1", "Brush", "ব্রাশ", "SKU-1", "PCS", 100_00L, 200_00L)
        val prodPair = mkProduct("p2", "Insole Pair", "জুতার ইনসোল", "SKU-2", "PAIR", 100_00L, 200_00L)
        val prodPcs2 = mkProduct("p3", "Comb", "চিরুনি", "SKU-3", "PCS", 100_00L, 200_00L)
        val po1 = PurchaseOrder("po-1", "PO-1", "sup-1", PurchaseOrderStatus.RECEIVED.name, totalAmountMinor = 300_00L, totalBdtAmountMinor = 300_00L, orderDate = now - 10L * dayMs)
        val po2 = PurchaseOrder("po-2", "PO-2", "sup-1", PurchaseOrderStatus.RECEIVED.name, totalAmountMinor = 100_00L, totalBdtAmountMinor = 100_00L, orderDate = now - 9L * dayMs)
        val voucher = ExpenseVoucher(
            id = "ev-oh", voucherNumber = "EXP-OH", category = "RENT_UTILITIES",
            descriptionEn = "Warehouse Rent", descriptionBn = "ভাড়া",
            amountMinor = 100_00L, bdtAmountMinor = 100_00L, paymentAccountId = "coa-1010", expenseAccountId = "coa-5110",
            voucherDate = now - 2L * dayMs, isPosted = true
        )
        val ruleQtyPo = ExpenseAllocationRule(
            id = "rule-qty", ruleName = "Qty Split", allocationBasis = "QUANTITY",
            periodStartMillis = now - 30L * dayMs, periodEndMillis = now,
            targetScopeType = VaultScopeType.PURCHASE_ORDER.name, isActive = true
        )

        val mixedDs = baseEmptyDataset(
            products = listOf(prodPcs, prodPair),
            purchaseOrders = listOf(po1, po2),
            purchaseOrderItems = listOf(
                PurchaseOrderItem("i1", "po-1", "p1", null, 3, 100_00L, 300_00L, 300_00L, 3),
                PurchaseOrderItem("i2", "po-2", "p2", null, 1, 100_00L, 100_00L, 100_00L, 1)
            ),
            costLayers = listOf(
                mkLayer("l1", "p1", shipmentId = "shp-1", shipmentNumber = "S1", poId = "po-1", initialQty = 3, remainingQty = 3, unitLandedCostMinor = 100_00L, receiptDate = now - 5L * dayMs),
                mkLayer("l2", "p2", shipmentId = "shp-2", shipmentNumber = "S2", poId = "po-2", initialQty = 1, remainingQty = 1, unitLandedCostMinor = 100_00L, receiptDate = now - 5L * dayMs)
            ),
            expenseVouchers = listOf(voucher)
        )
        assertThrows(IllegalArgumentException::class.java) {
            ImportVaultAnalyticsEngine.previewExpenseRuleAllocations(mixedDs, ruleQtyPo, now)
        }

        val uniformDs = mixedDs.copy(
            products = listOf(prodPcs, prodPcs2),
            costLayers = listOf(
                mkLayer("l1", "p1", shipmentId = "shp-1", shipmentNumber = "S1", poId = "po-1", initialQty = 3, remainingQty = 3, unitLandedCostMinor = 100_00L, receiptDate = now - 5L * dayMs),
                mkLayer("l2", "p3", shipmentId = "shp-2", shipmentNumber = "S2", poId = "po-2", initialQty = 1, remainingQty = 1, unitLandedCostMinor = 100_00L, receiptDate = now - 5L * dayMs)
            )
        )
        val allocs = ImportVaultAnalyticsEngine.previewExpenseRuleAllocations(uniformDs, ruleQtyPo, now)
        assertEquals(2, allocs.size)
        assertEquals(75_00L, allocs.first { it.targetId == "po-1" }.allocatedAmountBdtMinor)
        assertEquals(25_00L, allocs.first { it.targetId == "po-2" }.allocatedAmountBdtMinor)
    }

    @Test
    fun productAndVariantMetrics_computeRoiVelocityAndSupportAllTenSortOptions() {
        val now = 1_700_000_000_000L
        val prodA = mkProduct("p1", "Fast Winner", "দ্রুত লাভজনক", "SKU-F", "PCS", 100_00L, 300_00L)
        val prodB = mkProduct("p2", "Slow Loss", "ধীর লোকসান", "SKU-S", "PCS", 200_00L, 180_00L)
        val vFast = ProductVariant("vf", "p1", "SKU-F-1", "Blue", "M", 300_00L, 100_00L)
        val vSlow = ProductVariant("vs", "p2", "SKU-S-1", "Red", "L", 180_00L, 200_00L)
        val po1 = PurchaseOrder("po-1", "PO-1", "sup-1", PurchaseOrderStatus.RECEIVED.name, totalAmountMinor = 3_000_00L, totalBdtAmountMinor = 3_000_00L, orderDate = now - 25L * dayMs)
        val ds = baseEmptyDataset(
            products = listOf(prodA, prodB),
            variants = listOf(vFast, vSlow),
            purchaseOrders = listOf(po1),
            costLayers = listOf(
                mkLayer("l1", "p1", "vf", initialQty = 10, remainingQty = 2, unitLandedCostMinor = 100_00L, receiptDate = now - 4L * dayMs),
                mkLayer("l2", "p2", "vs", initialQty = 10, remainingQty = 8, unitLandedCostMinor = 200_00L, receiptDate = now - 20L * dayMs)
            ),
            salesInvoices = listOf(
                mkInvoice("inv-1", "INV-1", 2_400_00L, 800_00L, now - 1L * dayMs),
                mkInvoice("inv-2", "INV-2", 300_00L, 400_00L, now - 2L * dayMs)
            ),
            saleLayerAllocations = listOf(
                mkSaleAlloc("sa-1", "inv-1", "ii-1", "l1", "p1", "vf", qty = 8, unitCostMinor = 100_00L, netRevMinor = 2_400_00L, createdAt = now - 1L * dayMs),
                mkSaleAlloc("sa-2", "inv-2", "ii-2", "l2", "p2", "vs", qty = 2, unitCostMinor = 200_00L, netRevMinor = 300_00L, createdAt = now - 2L * dayMs)
            )
        )

        val rows = ImportVaultAnalyticsEngine.computeProductProfitability(ds, ImportVaultFilter(nowMillis = now))
        val pFast = rows.first { it.productId == "p1" }
        val pSlow = rows.first { it.productId == "p2" }

        assertEquals(4, pFast.activeAgeDays)
        assertEquals(2.0, pFast.sellingVelocityPerDay!!, 0.001)
        assertEquals(160.0, pFast.roiPercent!!, 0.01)

        assertEquals(20, pSlow.activeAgeDays)
        assertEquals(0.1, pSlow.sellingVelocityPerDay!!, 0.001)
        assertEquals(-5.0, pSlow.roiPercent!!, 0.01)

        assertEquals(listOf("p1", "p2"), ImportVaultUiLogic.sortProducts(rows, ProfitSortOption.HIGHEST_ROI).map { it.productId })
        assertEquals(listOf("p1", "p2"), ImportVaultUiLogic.sortProducts(rows, ProfitSortOption.FASTEST_SELLING).map { it.productId })
        assertEquals(listOf("p2", "p1"), ImportVaultUiLogic.sortProducts(rows, ProfitSortOption.SLOWEST_SELLING).map { it.productId })
        assertEquals(listOf("p2", "p1"), ImportVaultUiLogic.sortProducts(rows, ProfitSortOption.LOSS_MAKING_FIRST).map { it.productId })

        val variants = rows.flatMap { it.variants }
        assertEquals(listOf("vf", "vs"), ImportVaultUiLogic.sortVariants(variants, ProfitSortOption.HIGHEST_ROI).map { it.variantId })
        assertEquals(listOf("vf", "vs"), ImportVaultUiLogic.sortVariants(variants, ProfitSortOption.FASTEST_SELLING).map { it.variantId })
        assertEquals(listOf("vs", "vf"), ImportVaultUiLogic.sortVariants(variants, ProfitSortOption.SLOWEST_SELLING).map { it.variantId })
        assertEquals(listOf("vs", "vf"), ImportVaultUiLogic.sortVariants(variants, ProfitSortOption.LOSS_MAKING_FIRST).map { it.variantId })
        assertTrue(ImportVaultUiLogic.formatVelocity(2.0, "PCS", AppLanguage.ENGLISH).contains("2.00 PCS/day"))
        assertEquals("প্রযোজ্য নয়", ImportVaultUiLogic.formatVelocity(null, "PCS", AppLanguage.BANGLA))
    }

    @Test
    fun salesTrendAndDetailCrossLinks_populateTimeBucketsCostLayersSaleSourcesAndLinkedRecords() {
        val now = 1_700_000_000_000L
        val prod = mkProduct("p1", "Bottle", "বোতল", "SKU-B", "PCS", 100_00L, 250_00L)
        val po1 = PurchaseOrder("po-1", "PO-1", "sup-1", PurchaseOrderStatus.RECEIVED.name, totalAmountMinor = 1_000_00L, totalBdtAmountMinor = 1_000_00L, paidBdtAmountMinor = 600_00L, orderDate = now - 60L * dayMs, shipmentId = "shp-1")
        val shp1 = ImportShipment("shp-1", "SHP-1", "sup-1", "po-1", shippingDate = now - 55L * dayMs, status = ShipmentStatus.RECEIVED.name, totalGrossWeightKg = "5.0")
        val ds = baseEmptyDataset(
            products = listOf(prod),
            purchaseOrders = listOf(po1),
            supplierPayments = listOf(SupplierPayment("sp-1", "SP-1", "sup-1", "po-1", 600_00L, "BDT", "1.0", 600_00L, paymentDate = now - 50L * dayMs)),
            shipments = listOf(shp1),
            importCosts = listOf(ImportCostItem("ic-1", "shp-1", "INTERNATIONAL_FREIGHT", "Air Freight", 200_00L, bdtAmountMinor = 200_00L, costDate = now - 50L * dayMs, purchaseOrderId = "po-1")),
            landedSnapshots = listOf(LandedCostSnapshot("ls-1", "shp-1", "p1", purchaseOrderId = "po-1", productNameEn = "Bottle", productNameBn = "বোতল", sku = "SKU-B", totalReceivedQuantity = 10, totalPurchaseCostBdtMinor = 1_000_00L, totalAllocatedCostsBdtMinor = 200_00L, totalLandedCostBdtMinor = 1_200_00L, unitLandedCostBdtMinor = 120_00L, snapshotDate = now - 45L * dayMs)),
            warehouseReceipts = listOf(WarehouseReceipt("wr-1", "WR-1", "shp-1", "wh-1", now - 45L * dayMs, "po-1")),
            costLayers = listOf(mkLayer("l1", "p1", initialQty = 10, remainingQty = 6, unitLandedCostMinor = 120_00L, receiptDate = now - 45L * dayMs)),
            damageRecords = listOf(DamageRecord("d-1", "DMG-1", "po-1", "wr-1", "wh-1", "p1", damagedQuantity = 1, unitCostMinor = 120_00L, totalLossMinor = 120_00L, createdAt = now - 10L * dayMs)),
            shortageRecords = listOf(ShortageRecord("sh-1", "SHT-1", "po-1", "wr-1", "wh-1", "p1", missingQuantity = 1, unitCostMinor = 120_00L, totalAmountMinor = 120_00L, disposition = "SUPPLIER_CLAIM", createdAt = now - 10L * dayMs)),
            additionalCosts = listOf(
                AdditionalProductCost(
                    id = "ac-1",
                    costNumber = "ADD-1",
                    purchaseOrderId = "po-1",
                    category = AdditionalCostCategory.HANDLING.name,
                    description = "Local van",
                    amountMinor = 50_00L,
                    bdtAmountMinor = 50_00L,
                    isCapitalizable = false,
                    expenseDate = now - 8L * dayMs
                )
            ),
            salesInvoices = listOf(
                mkInvoice("i1", "INV-1", 500_00L, 240_00L, now - 5L * dayMs),
                mkInvoice("i2", "INV-2", 250_00L, 120_00L, now - 1L * dayMs)
            ),
            saleLayerAllocations = listOf(
                mkSaleAlloc("sa1", "i1", "ii1", "l1", "p1", qty = 2, unitCostMinor = 120_00L, netRevMinor = 500_00L, createdAt = now - 5L * dayMs),
                mkSaleAlloc("sa2", "i2", "ii2", "l1", "p1", qty = 1, unitCostMinor = 120_00L, netRevMinor = 250_00L, createdAt = now - 1L * dayMs)
            )
        )

        val overview = ImportVaultAnalyticsEngine.computeOverview(ds, ImportVaultFilter(nowMillis = now))
        assertTrue(overview.portfolioCostLayers.isNotEmpty())
        assertEquals(2, overview.portfolioSaleSources.size)
        assertEquals(2, overview.salesTrend.size)
        assertEquals(750_00L, overview.salesTrend.sumOf { it.netRevenueMinor })

        val shpDetail = ImportVaultAnalyticsEngine.computeShipmentDetail(ds, "shp-1", ImportVaultFilter(nowMillis = now))!!
        assertEquals(600_00L, shpDetail.supplierPaidMinor)
        assertEquals(400_00L, shpDetail.supplierPayableMinor)
        assertEquals(1, shpDetail.additionalCosts.size)
        assertEquals(1, shpDetail.damageRecords.size)
        assertEquals(1, shpDetail.shortageRecords.size)
        assertEquals(2, shpDetail.salesTrend.size)

        val poDetail = ImportVaultAnalyticsEngine.computePurchaseOrderDetail(ds, "po-1", ImportVaultFilter(nowMillis = now))!!
        assertEquals(1, poDetail.importCosts.size)
        assertEquals(1, poDetail.landedCostSnapshots.size)
        assertEquals(2, poDetail.salesTrend.size)
    }

    @Test
    fun datePresets_andPortfolioAttachments_behaveDeterministically() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 25, 14, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val now = cal.timeInMillis
        val (todayFrom, todayTo, todayAsOf) = ImportVaultUiLogic.computeDateRangeForPreset(VaultDatePreset.TODAY, nowMillis = now)
        assertEquals(ImportVaultUiLogic.startOfDayMillis(now), todayFrom)
        assertEquals(now, todayTo)
        assertEquals(now, todayAsOf)

        val customA = now - 10L * dayMs
        val customB = now - 2L * dayMs
        val (cFrom, cTo, cAsOf) = ImportVaultUiLogic.computeDateRangeForPreset(
            preset = VaultDatePreset.CUSTOM_RANGE,
            customFromMillis = customB,
            customToMillis = customA,
            nowMillis = now
        )
        assertEquals(ImportVaultUiLogic.startOfDayMillis(customA), cFrom)
        assertEquals(minOf(now, ImportVaultUiLogic.endOfDayMillis(customB)), cTo)
        assertEquals(cTo, cAsOf)

        val ds = baseEmptyDataset(
            vaultAttachments = listOf(
                ImportVaultAttachment(
                    id = "att-port-1",
                    scopeType = "PORTFOLIO",
                    sourceId = "portfolio",
                    category = VaultDocumentCategory.REPORTS.name,
                    title = "Q4 Board Snapshot (PDF)",
                    filePath = "/files/vault_attachments/snap_portfolio.pdf",
                    mimeType = "application/pdf",
                    fileSizeBytes = 4096L,
                    isImmutableSnapshot = true,
                    createdAt = now - dayMs
                )
            )
        )
        val overview = ImportVaultAnalyticsEngine.computeOverview(ds, ImportVaultFilter(nowMillis = now))
        assertEquals(1, overview.portfolioAttachments.size)
        assertTrue(overview.portfolioAttachments.single().isImmutableSnapshot)
        assertNull(overview.openingStockCostMinor)
        assertNotNull(overview.closingStockCostMinor)
    }
}
