package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.core.engine.ImportVaultAnalyticsEngine
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.security.BiometricStatus
import com.pocketdev.advance_erp.core.security.ImportVaultAccessGuard
import com.pocketdev.advance_erp.core.security.UserRole
import com.pocketdev.advance_erp.core.security.VaultUnlockPolicy
import com.pocketdev.advance_erp.data.entity.ImportShipment
import com.pocketdev.advance_erp.data.entity.ImportVaultEntry
import com.pocketdev.advance_erp.data.entity.InventoryCostLayer
import com.pocketdev.advance_erp.data.entity.InvoiceStatus
import com.pocketdev.advance_erp.data.entity.Product
import com.pocketdev.advance_erp.data.entity.ProductCategory
import com.pocketdev.advance_erp.data.entity.ProductImage
import com.pocketdev.advance_erp.data.entity.ProductVariant
import com.pocketdev.advance_erp.data.entity.PurchaseOrder
import com.pocketdev.advance_erp.data.entity.PurchaseOrderItem
import com.pocketdev.advance_erp.data.entity.SaleLayerAllocation
import com.pocketdev.advance_erp.data.entity.SalesInvoice
import com.pocketdev.advance_erp.data.entity.ShipmentCarton
import com.pocketdev.advance_erp.data.entity.Supplier
import com.pocketdev.advance_erp.data.entity.Warehouse
import com.pocketdev.advance_erp.domain.model.AgingSummary
import com.pocketdev.advance_erp.domain.model.CashRecoverySummary
import com.pocketdev.advance_erp.domain.model.CommercialStatus
import com.pocketdev.advance_erp.domain.model.CostFinalizationStatus
import com.pocketdev.advance_erp.domain.model.ExpectedRemainingProfitEstimate
import com.pocketdev.advance_erp.domain.model.ImportVaultDataset
import com.pocketdev.advance_erp.domain.model.ImportVaultFilter
import com.pocketdev.advance_erp.domain.model.OperationalStatus
import com.pocketdev.advance_erp.domain.model.ProductVaultRow
import com.pocketdev.advance_erp.domain.model.ProfitContributionBaseStatus
import com.pocketdev.advance_erp.domain.model.ProfitContributionMetric
import com.pocketdev.advance_erp.domain.model.QuantityByUnit
import com.pocketdev.advance_erp.domain.model.ShipmentVaultSummary
import com.pocketdev.advance_erp.domain.model.StockAgingBucket
import com.pocketdev.advance_erp.domain.model.VariantVaultRow
import com.pocketdev.advance_erp.domain.model.VaultScopeType
import com.pocketdev.advance_erp.feature.importvault.ContributionChartMode
import com.pocketdev.advance_erp.feature.importvault.ContributionSlice
import com.pocketdev.advance_erp.feature.importvault.ImportVaultChartPolicy
import com.pocketdev.advance_erp.feature.importvault.ImportVaultUiLogic
import com.pocketdev.advance_erp.feature.importvault.ProfitSortOption
import com.pocketdev.advance_erp.feature.importvault.VaultArchiveFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ImportVaultPhase3UiLogicTest {

    private fun emptyDataset(
        products: List<Product> = emptyList(),
        variants: List<ProductVariant> = emptyList(),
        categories: List<ProductCategory> = listOf(ProductCategory("cat-1", "Insole", "ইনসোল", "INS", null, 18000L)),
        warehouses: List<Warehouse> = listOf(Warehouse("wh-1", "Main", "WH-MAIN", "Dhaka", true, true)),
        suppliers: List<Supplier> = listOf(Supplier("sup-1", "SUP-01", "Shenzhen Supplier", "Wei", "", "", "", "China", "CNY", 0L, true, 1L)),
        purchaseOrders: List<PurchaseOrder> = emptyList(),
        purchaseOrderItems: List<PurchaseOrderItem> = emptyList(),
        shipments: List<ImportShipment> = emptyList(),
        cartons: List<ShipmentCarton> = emptyList(),
        costLayers: List<InventoryCostLayer> = emptyList(),
        salesInvoices: List<SalesInvoice> = emptyList(),
        saleLayerAllocations: List<SaleLayerAllocation> = emptyList(),
        vaultEntries: List<ImportVaultEntry> = emptyList(),
        productImages: List<ProductImage> = emptyList()
    ) = ImportVaultDataset(
        products = products,
        variants = variants,
        categories = categories,
        warehouses = warehouses,
        suppliers = suppliers,
        purchaseOrders = purchaseOrders,
        purchaseOrderItems = purchaseOrderItems,
        supplierPayments = emptyList(),
        shipments = shipments,
        cartons = cartons,
        shipmentSuppliers = emptyList(),
        freightRates = emptyList(),
        importCosts = emptyList(),
        landedAllocations = emptyList(),
        landedSnapshots = emptyList(),
        warehouseReceipts = emptyList(),
        warehouseReceiptItems = emptyList(),
        costLayers = costLayers,
        costAdjustments = emptyList(),
        fifoConsumptions = emptyList(),
        stockTransfers = emptyList(),
        stockTransferCostLayers = emptyList(),
        damageRecords = emptyList(),
        shortageRecords = emptyList(),
        additionalProductCosts = emptyList(),
        salesInvoices = salesInvoices,
        salesInvoiceItems = emptyList(),
        saleLayerAllocations = saleLayerAllocations,
        salesReturns = emptyList(),
        salesReturnItems = emptyList(),
        salesReturnLayerAllocations = emptyList(),
        customerPayments = emptyList(),
        paymentAllocations = emptyList(),
        expenseVouchers = emptyList(),
        expenseAllocationRules = emptyList(),
        expenseAllocations = emptyList(),
        vaultEntries = vaultEntries,
        vaultAttachments = emptyList(),
        customerOpeningDueTotalMinor = 0L,
        productImages = productImages
    )

    @Test
    fun chartPolicy_usesDonutForNonnegativeAndDivergingForLossesOrZeroNa() {
        val positiveSlices = listOf(
            ContributionSlice("p1", "4D Cloud", 55_000_00L, 55.0, 36.67),
            ContributionSlice("p2", "PU Height", 30_000_00L, 30.0, 30.0)
        )
        assertEquals(
            ContributionChartMode.DONUT,
            ImportVaultChartPolicy.resolveContributionChartMode(positiveSlices, 100_000_00L)
        )
        val donutDisplay = ImportVaultChartPolicy.buildDonutDisplaySlices(positiveSlices, 100_000_00L, "Residual")
        assertEquals(3, donutDisplay.size)
        assertEquals(15_000_00L, donutDisplay.last().amountMinor)
        assertEquals(15.0, donutDisplay.last().percent!!, 0.001)

        val mixedSlices = listOf(
            ContributionSlice("p1", "Profitable", 12_000L, 120.0, 40.0),
            ContributionSlice("p2", "Loss Maker", -2_000L, -20.0, -10.0)
        )
        assertEquals(
            ContributionChartMode.DIVERGING_BARS,
            ImportVaultChartPolicy.resolveContributionChartMode(mixedSlices, 10_000L)
        )
        assertTrue(ImportVaultChartPolicy.buildDonutDisplaySlices(mixedSlices, 10_000L).isEmpty())

        assertEquals(
            ContributionChartMode.LOSS_BASE_BARS,
            ImportVaultChartPolicy.resolveContributionChartMode(mixedSlices, -5_000L)
        )
        assertEquals(
            ContributionChartMode.ZERO_BASE_NA,
            ImportVaultChartPolicy.resolveContributionChartMode(positiveSlices, 0L)
        )
    }

    @Test
    fun shelfGrouping_handlesYearMonthDescendingPinPriorityAndArchiveFilter() {
        fun sampleShipment(id: String, num: String, year: Int, monthZero: Int, pinned: Boolean, status: String, profit: Long): ShipmentVaultSummary {
            val cal = Calendar.getInstance().apply {
                set(year, monthZero, 15, 10, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return ShipmentVaultSummary(
                shipmentId = id,
                shipmentNumber = num,
                shippingMethod = "SEA",
                supplierNames = listOf("Shenzhen"),
                purchaseOrderIds = emptyList(),
                purchaseOrderNumbers = emptyList(),
                shippingDate = cal.timeInMillis,
                arrivalDate = null,
                operationalStatus = OperationalStatus.RECEIVED,
                commercialStatus = CommercialStatus.PROFITABLE,
                costFinalizationStatus = CostFinalizationStatus.FINALIZED,
                hasPendingReceiving = false,
                archiveStatus = status,
                isPinned = pinned,
                verifiedBillableWeightKg = "10.0",
                supplierDeclaredWeightKg = null,
                receivedGoodQtyByUnit = QuantityByUnit.fromItems(listOf("PCS" to 10)),
                netSoldQtyByUnit = QuantityByUnit.fromItems(listOf("PCS" to 5)),
                remainingStockQtyByUnit = QuantityByUnit.fromItems(listOf("PCS" to 5)),
                totalInvestedCapitalMinor = 100_00L,
                purchaseCostBdtMinor = 80_00L,
                chinaDeliveryBdtMinor = 5_00L,
                internationalFreightBdtMinor = 10_00L,
                bdTransportAndOtherImportMinor = 5_00L,
                capitalizedAdditionalCostsMinor = 0L,
                lockedInventoryCapitalMinor = 50_00L,
                netSalesRevenueMinor = 70_00L,
                cogsMinor = 50_00L,
                grossProfitMinor = 20_00L,
                grossMarginPercent = 28.57,
                directAttributedExpensesMinor = 0L,
                allocatedSharedOverheadMinor = 0L,
                inventoryLossesMinor = 0L,
                attributedNetProfitMinor = profit,
                profitLabel = "Attributed Net Profit",
                netMarginPercent = 28.57,
                roiPercent = 20.0,
                sellThroughPercent = 50.0,
                cashRecovery = CashRecoverySummary(100_00L, 50_00L, 20_00L, 50_00L, 50.0, false),
                agingSummary = AgingSummary(emptyMap(), emptyMap(), 10),
                expectedRemainingProfit = ExpectedRemainingProfitEstimate(5, 50_00L, 100_00L, 50_00L, "Current Selling Price", cal.timeInMillis, 0)
            )
        }

        val items = listOf(
            sampleShipment("s1", "SHP-OCT", 2026, Calendar.OCTOBER, pinned = true, status = "ACTIVE", profit = 20_00L),
            sampleShipment("s2", "SHP-NOV-1", 2026, Calendar.NOVEMBER, pinned = false, status = "ACTIVE", profit = 30_00L),
            sampleShipment("s3", "SHP-NOV-ARC", 2026, Calendar.NOVEMBER, pinned = true, status = "ARCHIVED", profit = 40_00L)
        )

        val (activePinned, activeGroups) = ImportVaultUiLogic.groupShipmentsByYearMonth(items, VaultArchiveFilter.ACTIVE)
        assertEquals(listOf("s1"), activePinned.map { it.shipmentId })
        assertEquals(listOf("2026-11", "2026-10"), activeGroups.map { it.yearMonthKey })
        assertEquals(30_00L, activeGroups[0].totalNetProfitMinor)
        assertEquals(20_00L, activeGroups[1].totalNetProfitMinor)

        val (archPinned, archGroups) = ImportVaultUiLogic.groupShipmentsByYearMonth(items, VaultArchiveFilter.ARCHIVED)
        assertEquals(listOf("s3"), archPinned.map { it.shipmentId })
        assertEquals(1, archGroups.size)

        val (_, allGroups) = ImportVaultUiLogic.groupShipmentsByYearMonth(items, VaultArchiveFilter.ALL)
        assertEquals(3, allGroups.sumOf { it.items.size })
    }

    @Test
    fun searchFilter_matchesCustomTitleTagsNotesBanglaNameSkuBarcodeAndCarton() {
        val now = 1_750_000_000_000L
        val prod = Product(
            id = "prod-1",
            categoryId = "cat-1",
            nameEn = "4D Cloud Insole",
            nameBn = "ফোরডি ক্লাউড ইনসোল",
            baseSku = "4D-INS",
            barcode = "8901234567890",
            qrCode = "QR-INSOLE-01",
            purchasePriceMinor = 100_00L,
            sellingPriceMinor = 300_00L
        )
        val po = PurchaseOrder(
            id = "po-1",
            poNumber = "PO-2026-009",
            supplierId = "sup-1",
            totalAmountMinor = 1_000_00L,
            totalBdtAmountMinor = 17_000_00L,
            orderDate = now - 5_000L,
            shipmentId = "shp-1",
            isActualCostFinalized = true
        )
        val shp = ImportShipment(
            id = "shp-1",
            shipmentNumber = "SHP-2026-009",
            supplierId = "sup-1",
            purchaseOrderId = "po-1",
            shippingDate = now - 4_000L,
            trackingReference = "TRK-AIR-991"
        )
        val carton = ShipmentCarton(
            id = "ctn-1",
            shipmentId = "shp-1",
            cartonNumber = "CTN-WINTER-08",
            productId = "prod-1",
            quantity = 50,
            purchaseOrderId = "po-1"
        )
        val entry = ImportVaultEntry(
            id = "ive-shp-1",
            scopeType = VaultScopeType.SHIPMENT.name,
            sourceId = "shp-1",
            displayTitle = "ডিসেম্বর সুপার ইনসোল ফোল্ডার",
            archiveStatus = "ACTIVE",
            isPinned = true,
            tagsJson = "[\"VIP\",\"Winter\"]",
            notes = "Urgent air cargo batch"
        )
        val dataset = emptyDataset(
            products = listOf(prod),
            purchaseOrders = listOf(po),
            shipments = listOf(shp),
            cartons = listOf(carton),
            vaultEntries = listOf(entry)
        )

        listOf(
            "ডিসেম্বর সুপার",
            "Winter",
            "Urgent air cargo",
            "ফোরডি ক্লাউড",
            "8901234567890",
            "QR-INSOLE-01",
            "CTN-WINTER-08"
        ).forEach { query ->
            val ov = ImportVaultAnalyticsEngine.computeOverview(
                dataset,
                ImportVaultFilter(searchQuery = query, nowMillis = now)
            )
            assertEquals("Query '$query' should match shipment", 1, ov.shipmentCount)
            assertEquals("ডিসেম্বর সুপার ইনসোল ফোল্ডার", ov.shipments.first().displayTitle)
            assertEquals(listOf("VIP", "Winter"), ov.shipments.first().tags)
        }

        val miss = ImportVaultAnalyticsEngine.computeOverview(
            dataset,
            ImportVaultFilter(searchQuery = "NON-EXISTENT-XYZ", nowMillis = now)
        )
        assertEquals(0, miss.shipmentCount)
    }

    @Test
    fun splitPoAcrossTwoShipmentsAndProductImages_reconcilesWithoutDuplication() {
        val now = 1_750_000_000_000L
        val prod = Product(
            id = "prod-a",
            categoryId = "cat-1",
            nameEn = "Air Cushion Insole",
            nameBn = "এয়ার কুশন ইনসোল",
            baseSku = "AC-100",
            purchasePriceMinor = 200_00L,
            sellingPriceMinor = 400_00L
        )
        val varBlack = ProductVariant("var-blk", "prod-a", "AC-100-BLK", color = "Black", size = "37-38", variantPriceMinor = 400_00L, variantCostMinor = 200_00L)
        val varGray = ProductVariant("var-gry", "prod-a", "AC-100-GRY", color = "Gray", size = "35-36", variantPriceMinor = 400_00L, variantCostMinor = 200_00L)

        val po = PurchaseOrder(
            id = "po-split",
            poNumber = "PO-SPLIT-1",
            supplierId = "sup-1",
            totalAmountMinor = 100_00L,
            totalBdtAmountMinor = 40_000_00L,
            orderDate = now - 20_000L,
            isActualCostFinalized = true
        )
        val shp1 = ImportShipment("shp-1", "SHP-ONE", "sup-1", "po-split", shippingDate = now - 15_000L)
        val shp2 = ImportShipment("shp-2", "SHP-TWO", "sup-1", "po-split", shippingDate = now - 10_000L)

        val l1 = InventoryCostLayer(
            id = "l1", productId = "prod-a", variantId = "var-blk", warehouseId = "wh-1",
            shipmentId = "shp-1", shipmentNumber = "SHP-ONE", warehouseReceiptId = "wr-1", receiptNumber = "WR-1",
            initialQuantity = 100, remainingQuantity = 40, unitLandedCostBdtMinor = 200_00L,
            originalPurchasePriceMinor = 200_00L, exchangeRateToBdt = "1.0", allocatedExpensePerUnitBdtMinor = 0L,
            receiptDate = now - 14_000L, purchaseOrderId = "po-split", originReceiptDate = now - 14_000L
        )
        val l2 = InventoryCostLayer(
            id = "l2", productId = "prod-a", variantId = "var-gry", warehouseId = "wh-1",
            shipmentId = "shp-2", shipmentNumber = "SHP-TWO", warehouseReceiptId = "wr-2", receiptNumber = "WR-2",
            initialQuantity = 100, remainingQuantity = 70, unitLandedCostBdtMinor = 200_00L,
            originalPurchasePriceMinor = 200_00L, exchangeRateToBdt = "1.0", allocatedExpensePerUnitBdtMinor = 0L,
            receiptDate = now - 9_000L, purchaseOrderId = "po-split", originReceiptDate = now - 9_000L
        )
        val inv = SalesInvoice(
            id = "inv-1", invoiceNumber = "INV-1", customerId = "cust-1", customerName = "Rahim",
            warehouseId = "wh-1", warehouseName = "Main", status = InvoiceStatus.POSTED.name,
            subtotalMinor = 36_000_00L, totalAmountMinor = 36_000_00L, dueAmountMinor = 36_000_00L,
            totalCogsMinor = 18_000_00L, invoiceDate = now - 2_000L, isPosted = true
        )
        val s1 = SaleLayerAllocation(
            id = "sa-1", invoiceId = "inv-1", invoiceItemId = "ii-1", costLayerId = "l1",
            productId = "prod-a", variantId = "var-blk", warehouseId = "wh-1",
            purchaseOrderId = "po-split", shipmentId = "shp-1", quantity = 60,
            unitCostBdtMinor = 200_00L, totalCogsBdtMinor = 12_000_00L,
            allocatedGrossRevenueMinor = 24_000_00L, allocatedLineDiscountMinor = 0L,
            allocatedInvoiceDiscountMinor = 0L, allocatedNetRevenueMinor = 24_000_00L,
            createdAt = now - 2_000L
        )
        val s2 = SaleLayerAllocation(
            id = "sa-2", invoiceId = "inv-1", invoiceItemId = "ii-2", costLayerId = "l2",
            productId = "prod-a", variantId = "var-gry", warehouseId = "wh-1",
            purchaseOrderId = "po-split", shipmentId = "shp-2", quantity = 30,
            unitCostBdtMinor = 200_00L, totalCogsBdtMinor = 6_000_00L,
            allocatedGrossRevenueMinor = 12_000_00L, allocatedLineDiscountMinor = 0L,
            allocatedInvoiceDiscountMinor = 0L, allocatedNetRevenueMinor = 12_000_00L,
            createdAt = now - 2_000L
        )
        val images = listOf(
            ProductImage(id = "img-prod", productId = "prod-a", variantId = null, imageUri = "/tmp/prod_a.jpg", isPrimary = true),
            ProductImage(id = "img-blk", productId = "prod-a", variantId = "var-blk", imageUri = "/tmp/var_blk.jpg", isPrimary = true)
        )

        val dataset = emptyDataset(
            products = listOf(prod),
            variants = listOf(varBlack, varGray),
            purchaseOrders = listOf(po),
            purchaseOrderItems = listOf(
                PurchaseOrderItem("poi-1", "po-split", "prod-a", "var-blk", 100, 100L, 10000L, 20_000_00L),
                PurchaseOrderItem("poi-2", "po-split", "prod-a", "var-gry", 100, 100L, 10000L, 20_000_00L)
            ),
            shipments = listOf(shp1, shp2),
            costLayers = listOf(l1, l2),
            salesInvoices = listOf(inv),
            saleLayerAllocations = listOf(s1, s2),
            productImages = images
        )

        val poDetail = ImportVaultAnalyticsEngine.computePurchaseOrderDetail(
            dataset,
            "po-split",
            ImportVaultFilter(nowMillis = now)
        )!!
        assertEquals(2, poDetail.shipmentAllocations.size)
        assertEquals(40_000_00L, poDetail.shipmentAllocations.sumOf { it.allocatedInvestmentMinor })
        assertEquals(36_000_00L, poDetail.shipmentAllocations.sumOf { it.netSalesRevenueMinor })
        assertEquals(18_000_00L, poDetail.shipmentAllocations.sumOf { it.attributedNetProfitMinor })
        assertEquals(100.0, poDetail.shipmentAllocations.sumOf { it.shareOfPoPercent }, 0.01)

        // Screen D: Product/Variant thumbnails & separate shares (prod spans 2 shipments -> shipment share N/A at product level, populated at single-shipment variant level!)
        val prodRow = poDetail.products.first()
        assertEquals("/tmp/prod_a.jpg", prodRow.primaryImageUri)
        assertEquals(100.0, prodRow.profitContributionToPo?.contributionPercent!!, 0.01)
        assertNull(prodRow.profitContributionToShipment)

        val blkVariant = prodRow.variants.first { it.variantId == "var-blk" }
        val gryVariant = prodRow.variants.first { it.variantId == "var-gry" }
        assertEquals("/tmp/var_blk.jpg", blkVariant.primaryImageUri)
        assertEquals("/tmp/prod_a.jpg", gryVariant.primaryImageUri) // fallback to product image
        assertEquals(66.6667, blkVariant.profitContributionToPo?.contributionPercent!!, 0.01)
        assertEquals(100.0, blkVariant.profitContributionToShipment?.contributionPercent!!, 0.01)

        assertTrue(poDetail.timelineEvents.isNotEmpty())
        assertTrue(poDetail.documentRefs.isNotEmpty())
    }

    @Test
    fun sortingFormattingAndAccessGuard_behaveDeterministically() {
        val mixed = QuantityByUnit.fromItems(listOf("PCS" to 80, "PAIR" to 40))
        assertEquals("৮০ PCS + ৪০ PAIR", ImportVaultUiLogic.formatQuantityByUnit(mixed, AppLanguage.BANGLA))
        assertEquals("80 PCS + 40 PAIR", ImportVaultUiLogic.formatQuantityByUnit(mixed, AppLanguage.ENGLISH))

        assertEquals("প্রযোজ্য নয়", ImportVaultUiLogic.formatPercent(null, AppLanguage.BANGLA))
        assertEquals("N/A", ImportVaultUiLogic.formatPercent(null, AppLanguage.ENGLISH))
        assertEquals("৩৬.৬৭%", ImportVaultUiLogic.formatPercent(36.6667, AppLanguage.BANGLA))

        val dummyAging = AgingSummary(emptyMap(), emptyMap(), 5)
        val dummyExp = ExpectedRemainingProfitEstimate(0, 0L, 0L, 0L, "Current Selling Price", 1L, 0)
        fun vRow(sku: String, profit: Long, margin: Double?, locked: Long) = VariantVaultRow(
            productId = "p", variantId = sku, sku = sku, variantLabel = sku, uom = "PCS",
            purchaseOrderId = "po", shipmentId = "shp", receivedGoodQty = 10, netSoldQty = 5,
            returnedGoodQty = 0, returnedDamagedQty = 0, writeOffLossQty = 0, remainingGoodQty = 5,
            remainingInTransitTransferQty = 0, unitLandedCostMinor = 100L, netSalesRevenueMinor = 200L,
            cogsMinor = 100L, grossProfitMinor = profit, grossMarginPercent = margin,
            inventoryLossMinor = 0L, directExpensesMinor = 0L, allocatedSharedOverheadMinor = 0L,
            attributedNetProfitMinor = profit, netMarginPercent = margin,
            lockedInventoryCostMinor = locked, sellThroughPercent = 50.0,
            profitContributionToProduct = ProfitContributionMetric(profit, 100L, 10.0, ProfitContributionBaseStatus.POSITIVE_BASE),
            profitContributionToParentScope = null,
            agingSummary = dummyAging, expectedRemainingProfit = dummyExp
        )

        val variants = listOf(
            vRow("A", profit = 50L, margin = 20.0, locked = 900L),
            vRow("B", profit = 30L, margin = 60.0, locked = 100L),
            vRow("C", profit = -10L, margin = -5.0, locked = 500L)
        )
        assertEquals(listOf("A", "B", "C"), ImportVaultUiLogic.sortVariants(variants, ProfitSortOption.HIGHEST_PROFIT).map { it.sku })
        assertEquals(listOf("C", "B", "A"), ImportVaultUiLogic.sortVariants(variants, ProfitSortOption.LOWEST_PROFIT).map { it.sku })
        assertEquals(listOf("B", "A", "C"), ImportVaultUiLogic.sortVariants(variants, ProfitSortOption.HIGHEST_MARGIN).map { it.sku })
        assertEquals(listOf("A", "C", "B"), ImportVaultUiLogic.sortVariants(variants, ProfitSortOption.HIGHEST_LOCKED_CAPITAL).map { it.sku })

        val dummyProd = listOf(
            ProductVaultRow("p1", "One", "এক", "SKU1", "PCS", emptyList(), 10, 5, 0, 0, 0, 5, 0, 10L, 100L, 50L, 50L, 50.0, 0L, 0L, 0L, 50L, 50.0, 200L, 50.0, null, dummyAging, dummyExp),
            ProductVaultRow("p2", "Two", "দুই", "SKU2", "PCS", emptyList(), 10, 8, 0, 0, 0, 2, 0, 10L, 100L, 20L, 80L, 80.0, 0L, 0L, 0L, 80L, 80.0, 50L, 80.0, null, dummyAging, dummyExp)
        )
        assertEquals(listOf("p2", "p1"), ImportVaultUiLogic.sortProducts(dummyProd, ProfitSortOption.HIGHEST_SELL_THROUGH).map { it.productId })

        assertEquals(VaultUnlockPolicy.DENIED_ROLE, ImportVaultAccessGuard.evaluateAccess(UserRole.MANAGER, BiometricStatus.AVAILABLE))
        assertEquals(VaultUnlockPolicy.REQUIRE_BIOMETRIC, ImportVaultAccessGuard.evaluateAccess(UserRole.OWNER_ADMIN, BiometricStatus.AVAILABLE))
        assertEquals(VaultUnlockPolicy.SAFE_FALLBACK_NO_BIOMETRIC, ImportVaultAccessGuard.evaluateAccess(UserRole.OWNER_ADMIN, BiometricStatus.NOT_ENROLLED))
        assertFalse(ImportVaultAccessGuard.canRevealVaultWithBiometricFallback(UserRole.SALES_OPERATOR, BiometricStatus.NOT_SUPPORTED))
    }
}
