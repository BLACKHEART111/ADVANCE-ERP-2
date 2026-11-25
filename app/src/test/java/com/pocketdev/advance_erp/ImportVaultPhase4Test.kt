package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.core.engine.ImportVaultAnalyticsEngine
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.security.UserRole
import com.pocketdev.advance_erp.core.util.ImportVaultAttachmentFileStore
import com.pocketdev.advance_erp.core.util.ImportVaultExcelGenerator
import com.pocketdev.advance_erp.core.util.ImportVaultExportBuilder
import com.pocketdev.advance_erp.core.util.ImportVaultPdfGenerator
import com.pocketdev.advance_erp.core.util.VaultExportRequest
import com.pocketdev.advance_erp.core.util.VaultExportScope
import com.pocketdev.advance_erp.data.entity.AdditionalCostCategory
import com.pocketdev.advance_erp.data.entity.AdditionalProductCost
import com.pocketdev.advance_erp.data.entity.BusinessProfile
import com.pocketdev.advance_erp.data.entity.CustomerPayment
import com.pocketdev.advance_erp.data.entity.ExpenseVoucher
import com.pocketdev.advance_erp.data.entity.ImportCostItem
import com.pocketdev.advance_erp.data.entity.ImportShipment
import com.pocketdev.advance_erp.data.entity.ImportVaultAttachment
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
import com.pocketdev.advance_erp.data.entity.SalesReturn
import com.pocketdev.advance_erp.data.entity.SalesReturnLayerAllocation
import com.pocketdev.advance_erp.data.entity.ShipmentCarton
import com.pocketdev.advance_erp.data.entity.ShipmentStatus
import com.pocketdev.advance_erp.data.entity.Supplier
import com.pocketdev.advance_erp.data.entity.Warehouse
import com.pocketdev.advance_erp.data.entity.WarehouseReceipt
import com.pocketdev.advance_erp.data.entity.WarehouseReceiptItem
import com.pocketdev.advance_erp.data.repository.extractVaultAttachmentsFromZip
import com.pocketdev.advance_erp.data.repository.verifyBackupZipEntriesIntegrity
import com.pocketdev.advance_erp.domain.model.ImportVaultDataset
import com.pocketdev.advance_erp.domain.model.ImportVaultFilter
import com.pocketdev.advance_erp.domain.model.VaultDocumentCategory
import com.pocketdev.advance_erp.domain.model.VaultScopeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.xml.parsers.DocumentBuilderFactory

class ImportVaultPhase4Test {

    private fun buildRichDataset(baseTime: Long = 1_700_000_000_000L): ImportVaultDataset {
        val prodA = Product(
            id = "prod-a",
            categoryId = "cat-1",
            nameEn = "Thermal Bottle",
            nameBn = "থার্মাল বোতল",
            baseSku = "SKU-BOT",
            unit = "PCS",
            purchasePriceMinor = 200_00L,
            sellingPriceMinor = 450_00L
        )
        val prodB = Product(
            id = "prod-b",
            categoryId = "cat-1",
            nameEn = "Unpriced Gadget",
            nameBn = "মূল্যহীন গ্যাজেট",
            baseSku = "SKU-GAD",
            unit = "PCS",
            purchasePriceMinor = 150_00L,
            sellingPriceMinor = 0L
        )
        val varA1 = ProductVariant(
            id = "var-a1",
            productId = "prod-a",
            sku = "SKU-BOT-BLK",
            color = "Black",
            size = "500ml",
            variantPriceMinor = 500_00L,
            variantCostMinor = 200_00L
        )
        val varB1 = ProductVariant(
            id = "var-b1",
            productId = "prod-b",
            sku = "SKU-GAD-DEF",
            color = "Silver",
            size = "Std",
            variantPriceMinor = 0L,
            variantCostMinor = 150_00L
        )
        val po1 = PurchaseOrder(
            id = "po-1",
            poNumber = "PO-2026-001",
            supplierId = "sup-1",
            status = PurchaseOrderStatus.PARTIALLY_RECEIVED.name,
            currency = "BDT",
            exchangeRateToBdt = "1.0",
            totalAmountMinor = 3_500_00L,
            totalBdtAmountMinor = 3_500_00L,
            orderDate = baseTime - 40L * 86_400_000L,
            isActualCostFinalized = false,
            attachments = "/files/po_attachments/po1__sup_invoice.pdf",
            shipmentId = "shp-1",
            createdAt = baseTime - 40L * 86_400_000L,
            updatedAt = baseTime - 39L * 86_400_000L
        )
        val shp1 = ImportShipment(
            id = "shp-1",
            shipmentNumber = "SHP-2026-001",
            supplierId = "sup-1",
            purchaseOrderId = "po-1",
            shippingDate = baseTime - 35L * 86_400_000L,
            status = ShipmentStatus.RECEIVED.name,
            totalCartons = 2,
            totalGrossWeightKg = "25.000",
            createdAt = baseTime - 35L * 86_400_000L,
            updatedAt = baseTime - 30L * 86_400_000L
        )
        val poItems = listOf(
            PurchaseOrderItem("poi-1", "po-1", "prod-a", "var-a1", 10, 200_00L, 2_000_00L, 2_000_00L, 8),
            PurchaseOrderItem("poi-2", "po-1", "prod-b", "var-b1", 10, 150_00L, 1_500_00L, 1_500_00L, 6)
        )
        val layers = listOf(
            InventoryCostLayer(
                id = "l-a1",
                productId = "prod-a",
                variantId = "var-a1",
                warehouseId = "wh-1",
                shipmentId = "shp-1",
                shipmentNumber = "SHP-2026-001",
                warehouseReceiptId = "wr-1",
                receiptNumber = "WR-001",
                initialQuantity = 8,
                remainingQuantity = 3,
                unitLandedCostBdtMinor = 250_00L,
                originalCurrency = "BDT",
                originalPurchasePriceMinor = 200_00L,
                exchangeRateToBdt = "1.0",
                allocatedExpensePerUnitBdtMinor = 50_00L,
                receiptDate = baseTime - 25L * 86_400_000L,
                originReceiptDate = baseTime - 25L * 86_400_000L,
                purchaseOrderId = "po-1",
                purchaseOrderItemId = "poi-1",
                layerSourceType = "IMPORT_RECEIPT",
                createdAt = baseTime - 25L * 86_400_000L
            ),
            InventoryCostLayer(
                id = "l-b1",
                productId = "prod-b",
                variantId = "var-b1",
                warehouseId = "wh-1",
                shipmentId = "shp-1",
                shipmentNumber = "SHP-2026-001",
                warehouseReceiptId = "wr-1",
                receiptNumber = "WR-001",
                initialQuantity = 6,
                remainingQuantity = 6,
                unitLandedCostBdtMinor = 180_00L,
                originalCurrency = "BDT",
                originalPurchasePriceMinor = 150_00L,
                exchangeRateToBdt = "1.0",
                allocatedExpensePerUnitBdtMinor = 30_00L,
                receiptDate = baseTime - 10L * 86_400_000L,
                originReceiptDate = baseTime - 10L * 86_400_000L,
                purchaseOrderId = "po-1",
                purchaseOrderItemId = "poi-2",
                layerSourceType = "IMPORT_RECEIPT",
                createdAt = baseTime - 10L * 86_400_000L
            )
        )
        val invoices = listOf(
            SalesInvoice(
                id = "inv-1",
                invoiceNumber = "INV-1001",
                customerId = "c-1",
                customerName = "Rahim Traders",
                warehouseId = "wh-1",
                warehouseName = "Dhaka Central",
                status = InvoiceStatus.PARTIALLY_PAID.name,
                subtotalMinor = 2_500_00L,
                taxMinor = 100_00L,
                totalAmountMinor = 2_600_00L,
                paidAmountMinor = 1_500_00L,
                dueAmountMinor = 1_100_00L,
                totalCogsMinor = 1_250_00L,
                invoiceDate = baseTime - 5L * 86_400_000L,
                isPosted = true
            ),
            SalesInvoice(
                id = "inv-unattr",
                invoiceNumber = "INV-UNATTR",
                customerId = "c-1",
                customerName = "Rahim Traders",
                warehouseId = "wh-1",
                warehouseName = "Dhaka Central",
                status = InvoiceStatus.PAID.name,
                subtotalMinor = 400_00L,
                totalAmountMinor = 400_00L,
                paidAmountMinor = 400_00L,
                dueAmountMinor = 0L,
                totalCogsMinor = 0L,
                invoiceDate = baseTime - 4L * 86_400_000L,
                isPosted = true
            )
        )
        val saleAllocs = listOf(
            SaleLayerAllocation(
                id = "sla-1",
                invoiceId = "inv-1",
                invoiceItemId = "ii-1",
                costLayerId = "l-a1",
                productId = "prod-a",
                variantId = "var-a1",
                warehouseId = "wh-1",
                purchaseOrderId = "po-1",
                purchaseOrderItemId = "poi-1",
                shipmentId = "shp-1",
                quantity = 5,
                unitCostBdtMinor = 250_00L,
                totalCogsBdtMinor = 1_250_00L,
                allocatedGrossRevenueMinor = 2_500_00L,
                allocatedLineDiscountMinor = 0L,
                allocatedInvoiceDiscountMinor = 0L,
                allocatedNetRevenueMinor = 2_500_00L,
                createdAt = baseTime - 5L * 86_400_000L
            ),
            SaleLayerAllocation(
                id = "sla-unattr",
                invoiceId = "inv-unattr",
                invoiceItemId = "ii-2",
                costLayerId = null,
                productId = "prod-a",
                variantId = "var-a1",
                warehouseId = "wh-1",
                purchaseOrderId = null,
                shipmentId = null,
                quantity = 1,
                unitCostBdtMinor = 0L,
                totalCogsBdtMinor = 0L,
                allocatedGrossRevenueMinor = 400_00L,
                allocatedLineDiscountMinor = 0L,
                allocatedInvoiceDiscountMinor = 0L,
                allocatedNetRevenueMinor = 400_00L,
                isUnattributedFallback = true,
                createdAt = baseTime - 4L * 86_400_000L
            )
        )
        val returns = listOf(
            SalesReturn(
                id = "ret-1",
                returnNumber = "RET-01",
                invoiceId = "inv-1",
                invoiceNumber = "INV-1001",
                customerId = "c-1",
                customerName = "Rahim Traders",
                warehouseId = "wh-1",
                totalReturnAmountMinor = 500_00L,
                totalCogsReversalMinor = 250_00L,
                refundOption = ReturnRefundOption.CUSTOMER_CREDIT.name,
                returnDate = baseTime - 3L * 86_400_000L,
                isPosted = true
            )
        )
        val returnAllocs = listOf(
            SalesReturnLayerAllocation(
                id = "rla-1",
                returnId = "ret-1",
                returnItemId = "ri-1",
                invoiceId = "inv-1",
                invoiceItemId = "ii-1",
                saleLayerAllocationId = "sla-1",
                costLayerId = "l-a1",
                productId = "prod-a",
                variantId = "var-a1",
                purchaseOrderId = "po-1",
                purchaseOrderItemId = "poi-1",
                shipmentId = "shp-1",
                soundQuantity = 0,
                damagedQuantity = 1,
                reversedGrossRevenueMinor = 500_00L,
                reversedDiscountMinor = 0L,
                reversedNetRevenueMinor = 500_00L,
                reversedCogsMinor = 0L,
                damageLossMinor = 250_00L,
                createdAt = baseTime - 3L * 86_400_000L
            )
        )
        val payments = listOf(
            CustomerPayment(
                id = "pay-1",
                paymentNumber = "PAY-101",
                customerId = "c-1",
                customerName = "Rahim Traders",
                amountMinor = 1_500_00L,
                allocatedAmountMinor = 1_500_00L,
                unallocatedCreditMinor = 0L,
                paymentDate = baseTime - 2L * 86_400_000L,
                isPosted = true
            ),
            CustomerPayment(
                id = "pay-standalone",
                paymentNumber = "PAY-UNLINKED",
                customerId = "c-1",
                customerName = "Rahim Traders",
                amountMinor = 200_00L,
                allocatedAmountMinor = 0L,
                unallocatedCreditMinor = 200_00L,
                paymentDate = baseTime - 1L * 86_400_000L,
                isPosted = true
            )
        )
        val paymentAllocs = listOf(
            PaymentAllocation(
                id = "pa-1",
                paymentId = "pay-1",
                invoiceId = "inv-1",
                invoiceNumber = "INV-1001",
                amountMinor = 1_500_00L,
                allocatedAt = baseTime - 2L * 86_400_000L
            )
        )
        val importCosts = listOf(
            ImportCostItem(
                id = "ic-1",
                shipmentId = "shp-1",
                costCategory = "INTERNATIONAL_FREIGHT",
                description = "Air Freight Bill",
                amountMinor = 400_00L,
                bdtAmountMinor = 400_00L,
                costDate = baseTime - 33L * 86_400_000L,
                documentReference = "AWB-991",
                isEstimated = true,
                attachmentPath = "/files/freight/awb_991.pdf"
            )
        )
        val addCosts = listOf(
            AdditionalProductCost(
                id = "ac-1",
                costNumber = "ADD-01",
                purchaseOrderId = "po-1",
                category = AdditionalCostCategory.HANDLING.name,
                description = "Sample Courier Fee",
                amountMinor = 100_00L,
                bdtAmountMinor = 100_00L,
                isCapitalizable = false,
                expenseDate = baseTime - 24L * 86_400_000L
            )
        )
        val expenses = listOf(
            ExpenseVoucher(
                id = "ev-1",
                voucherNumber = "EXP-901",
                category = "PORT_HANDLING",
                descriptionEn = "Dock Unloading",
                descriptionBn = "ডক আনলোডিং",
                amountMinor = 150_00L,
                bdtAmountMinor = 150_00L,
                paymentAccountId = "coa-1010",
                expenseAccountId = "coa-5020",
                voucherDate = baseTime - 20L * 86_400_000L,
                attachmentPath = "/files/expenses/dock_receipt.jpg",
                shipmentId = "shp-1",
                purchaseOrderId = "po-1",
                isPosted = true
            )
        )
        val attachments = listOf(
            ImportVaultAttachment(
                id = "iva-local-1",
                vaultEntryId = "ive-shp-shp-1",
                scopeType = VaultScopeType.SHIPMENT.name,
                sourceId = "shp-1",
                category = VaultDocumentCategory.PACKING.name,
                title = "Signed Carton Checklist",
                filePath = "/files/vault_attachments/vault_shp-1__checklist.pdf",
                mimeType = "application/pdf",
                fileSizeBytes = 1024L,
                isImmutableSnapshot = false,
                createdAt = baseTime - 15L * 86_400_000L
            ),
            ImportVaultAttachment(
                id = "iva-snap-1",
                vaultEntryId = "ive-shp-shp-1",
                scopeType = VaultScopeType.SHIPMENT.name,
                sourceId = "shp-1",
                category = VaultDocumentCategory.REPORTS.name,
                title = "Month Close Snapshot (PDF)",
                filePath = "/files/vault_attachments/snapshot_shp-1__close.pdf",
                mimeType = "application/pdf",
                fileSizeBytes = 2048L,
                isImmutableSnapshot = true,
                createdAt = baseTime - 2L * 86_400_000L
            )
        )
        return ImportVaultDataset(
            products = listOf(prodA, prodB),
            variants = listOf(varA1, varB1),
            categories = listOf(ProductCategory("cat-1", "General", "সাধারণ", "GEN")),
            warehouses = listOf(Warehouse(id = "wh-1", name = "Dhaka Central", code = "WH-1", location = "Dhaka", isDefault = true)),
            suppliers = listOf(Supplier(id = "sup-1", supplierCode = "SUP-1", companyName = "Guangzhou Co", phone = "01711000000", address = "Guangzhou")),
            purchaseOrders = listOf(po1),
            purchaseOrderItems = poItems,
            supplierPayments = emptyList(),
            shipments = listOf(shp1),
            cartons = listOf(
                ShipmentCarton(
                    id = "c-1",
                    shipmentId = "shp-1",
                    cartonNumber = "CTN-1",
                    productId = "prod-a",
                    variantId = "var-a1",
                    quantity = 8,
                    grossWeightKg = "25.0",
                    cbm = "0.2",
                    purchaseOrderId = "po-1",
                    purchaseOrderItemId = "poi-1"
                )
            ),
            shipmentSuppliers = emptyList(),
            freightRates = emptyList(),
            importCosts = importCosts,
            landedAllocations = emptyList(),
            landedSnapshots = emptyList(),
            warehouseReceipts = listOf(
                WarehouseReceipt(
                    id = "wr-1",
                    receiptNumber = "WR-001",
                    shipmentId = "shp-1",
                    warehouseId = "wh-1",
                    receiptDate = baseTime - 25L * 86_400_000L,
                    purchaseOrderId = "po-1"
                )
            ),
            warehouseReceiptItems = listOf(
                WarehouseReceiptItem(
                    id = "wri-1",
                    receiptId = "wr-1",
                    productId = "prod-a",
                    variantId = "var-a1",
                    purchaseOrderId = "po-1",
                    purchaseOrderItemId = "poi-1",
                    expectedQuantity = 10,
                    receivedQuantity = 8
                )
            ),
            costLayers = layers,
            costAdjustments = emptyList(),
            fifoConsumptions = emptyList(),
            stockTransfers = emptyList(),
            stockTransferCostLayers = emptyList(),
            damageRecords = emptyList(),
            shortageRecords = emptyList(),
            additionalProductCosts = addCosts,
            salesInvoices = invoices,
            salesInvoiceItems = emptyList(),
            saleLayerAllocations = saleAllocs,
            salesReturns = returns,
            salesReturnItems = emptyList(),
            salesReturnLayerAllocations = returnAllocs,
            customerPayments = payments,
            paymentAllocations = paymentAllocs,
            expenseVouchers = expenses,
            expenseAllocationRules = emptyList(),
            expenseAllocations = emptyList(),
            vaultEntries = emptyList(),
            vaultAttachments = attachments,
            customerOpeningDueTotalMinor = 300_00L
        )
    }

    @Test
    fun test1_exportReconcilesUiTotalsAcrossAllFiveScopesAndSurfacesWarnings() {
        val now = 1_700_000_000_000L
        val dataset = buildRichDataset(now)
        val filter = ImportVaultFilter(nowMillis = now)
        val profile = BusinessProfile(
            companyName = "Advance Trading BD",
            phone = "+8801700000000",
            address = "Dhaka"
        )

        val overview = ImportVaultAnalyticsEngine.computeOverview(dataset, filter)
        val shpDetail = ImportVaultAnalyticsEngine.computeShipmentDetail(dataset, "shp-1", filter)!!
        val poDetail = ImportVaultAnalyticsEngine.computePurchaseOrderDetail(dataset, "po-1", filter)!!
        val prodA = shpDetail.products.first { it.productId == "prod-a" }
        val varA1 = prodA.variants.first { it.variantId == "var-a1" }

        // 1. Portfolio Scope
        val portPkg = ImportVaultExportBuilder.buildPackage(
            dataset,
            VaultExportRequest(VaultExportScope.PORTFOLIO, filter = filter, language = AppLanguage.BANGLA, businessProfile = profile)
        )
        assertEquals(overview.totalInvestedCapitalMinor, portPkg.totals.totalInvestedCapitalMinor)
        assertEquals(overview.totalAttributedNetRevenueMinor, portPkg.totals.netSalesRevenueMinor)
        assertEquals(overview.totalAttributedCogsMinor, portPkg.totals.cogsMinor)
        assertEquals(overview.totalAttributedNetProfitMinor, portPkg.totals.attributedNetProfitMinor)
        assertEquals(overview.cashRecovery.attributableCashCollectedMinor, portPkg.totals.attributableCashCollectedMinor)
        assertEquals(overview.cashRecovery.attributableReceivableMinor, portPkg.totals.attributableReceivableMinor)
        assertEquals("BDT", portPkg.currencyCode)
        assertTrue(portPkg.assumptions.isNotEmpty())
        assertTrue(portPkg.warnings.any { it.contains("Provisional", ignoreCase = true) })
        assertTrue(portPkg.warnings.any { it.contains("Pending Receiving", ignoreCase = true) })
        assertTrue(portPkg.warnings.any { it.contains("Missing", ignoreCase = true) || it.contains("SKU") })
        assertTrue(portPkg.warnings.any { it.contains("Unattributed", ignoreCase = true) || it.contains("অচিহ্নিত") })
        assertTrue(portPkg.warnings.any { it.contains("Tax", ignoreCase = true) || it.contains("নন-মার্চেন্ডাইজ") })

        // 2. Shipment Scope
        val shpPkg = ImportVaultExportBuilder.buildPackage(
            dataset,
            VaultExportRequest(VaultExportScope.SHIPMENT, scopeId = "shp-1", filter = filter, language = AppLanguage.ENGLISH)
        )
        assertEquals(shpDetail.summary.totalInvestedCapitalMinor, shpPkg.totals.totalInvestedCapitalMinor)
        assertEquals(shpDetail.summary.netSalesRevenueMinor, shpPkg.totals.netSalesRevenueMinor)
        assertEquals(shpDetail.summary.attributedNetProfitMinor, shpPkg.totals.attributedNetProfitMinor)
        assertEquals(shpDetail.summary.lockedInventoryCapitalMinor, shpPkg.totals.lockedInventoryCapitalMinor)

        // 3. Purchase Order Scope
        val poPkg = ImportVaultExportBuilder.buildPackage(
            dataset,
            VaultExportRequest(VaultExportScope.PURCHASE_ORDER, scopeId = "po-1", filter = filter, language = AppLanguage.ENGLISH)
        )
        assertEquals(poDetail.summary.totalInvestedCapitalMinor, poPkg.totals.totalInvestedCapitalMinor)
        assertEquals(poDetail.summary.netSalesRevenueMinor, poPkg.totals.netSalesRevenueMinor)
        assertEquals(poDetail.summary.attributedNetProfitMinor, poPkg.totals.attributedNetProfitMinor)

        // 4. Product Scope (parent Shipment)
        val prodPkg = ImportVaultExportBuilder.buildPackage(
            dataset,
            VaultExportRequest(
                scope = VaultExportScope.PRODUCT,
                scopeId = "prod-a",
                parentScopeType = VaultScopeType.SHIPMENT,
                parentScopeId = "shp-1",
                filter = filter,
                language = AppLanguage.ENGLISH
            )
        )
        assertEquals(prodA.netSalesRevenueMinor, prodPkg.totals.netSalesRevenueMinor)
        assertEquals(prodA.cogsMinor, prodPkg.totals.cogsMinor)
        assertEquals(prodA.attributedNetProfitMinor, prodPkg.totals.attributedNetProfitMinor)

        // 5. Variant Scope
        val varPkg = ImportVaultExportBuilder.buildPackage(
            dataset,
            VaultExportRequest(
                scope = VaultExportScope.VARIANT,
                scopeId = "prod-a",
                variantId = "var-a1",
                parentScopeType = VaultScopeType.SHIPMENT,
                parentScopeId = "shp-1",
                filter = filter,
                language = AppLanguage.ENGLISH
            )
        )
        assertEquals(varA1.netSalesRevenueMinor, varPkg.totals.netSalesRevenueMinor)
        assertEquals(varA1.cogsMinor, varPkg.totals.cogsMinor)
        assertEquals(varA1.attributedNetProfitMinor, varPkg.totals.attributedNetProfitMinor)
        assertEquals(1, varPkg.variants.size)
    }

    @Test
    fun test2_excelGeneratorProducesValidEightSheetOpenXmlWithWideColumnsAndBanglaText() {
        val now = 1_700_000_000_000L
        val dataset = buildRichDataset(now)
        val pkg = ImportVaultExportBuilder.buildPackage(
            dataset,
            VaultExportRequest(
                scope = VaultExportScope.PORTFOLIO,
                filter = ImportVaultFilter(nowMillis = now),
                language = AppLanguage.BANGLA,
                businessProfile = BusinessProfile(companyName = "অ্যাডভান্স ট্রেডিং", phone = "017", address = "ঢাকা")
            )
        )

        // Verify wide column naming beyond Z
        assertEquals("A", ImportVaultExcelGenerator.excelColName(0))
        assertEquals("Z", ImportVaultExcelGenerator.excelColName(25))
        assertEquals("AA", ImportVaultExcelGenerator.excelColName(26))
        assertEquals("AD", ImportVaultExcelGenerator.excelColName(29))

        val tempDir = Files.createTempDirectory("vault_xlsx_test_").toFile()
        try {
            val outFile = File(tempDir, "portfolio_export.xlsx")
            ImportVaultExcelGenerator.exportPackageToXlsxFile(pkg, outFile, UserRole.OWNER_ADMIN)
            assertTrue(outFile.exists() && outFile.length() > 1000L)

            ZipFile(outFile).use { zip ->
                val workbookXml = zip.getInputStream(zip.getEntry("xl/workbook.xml")).bufferedReader(StandardCharsets.UTF_8).readText()
                val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(ByteArrayInputStream(workbookXml.toByteArray(StandardCharsets.UTF_8)))
                val sheetNodes = doc.getElementsByTagName("sheet")
                assertEquals(8, sheetNodes.length)
                val sheetNames = (0 until sheetNodes.length).map { (sheetNodes.item(it) as Element).getAttribute("name") }
                assertEquals(
                    listOf("Summary", "Products", "Variants", "Sales Allocations", "Expenses", "Stock", "Receivables", "Documents Index"),
                    sheetNames
                )

                val allXmlContents = (1..8).map { idx ->
                    val entry = zip.getEntry("xl/worksheets/sheet$idx.xml")
                    assertNotNull("Missing sheet$idx.xml", entry)
                    val xml = zip.getInputStream(entry).bufferedReader(StandardCharsets.UTF_8).readText()
                    // Verify well-formed XML
                    DocumentBuilderFactory.newInstance().newDocumentBuilder()
                        .parse(ByteArrayInputStream(xml.toByteArray(StandardCharsets.UTF_8)))
                    xml
                }

                // Summary sheet preserves Bangla text & reconciled profit amount
                assertTrue(allXmlContents[0].contains("অ্যাডভান্স ট্রেডিং"))
                assertTrue(allXmlContents[0].contains(ImportVaultExcelGenerator.minorToDecimalString(pkg.totals.attributedNetProfitMinor)))

                // Products sheet exercises wide columns AA..AD and Bangla name
                assertTrue(allXmlContents[1].contains("থার্মাল বোতল"))
                assertTrue(allXmlContents[1].contains("r=\"AD1\""))
                assertTrue(allXmlContents[2].contains("r=\"AB1\""))
            }
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun test3_pdfReportIncludesVisibleAssumptionsWarningsAndReconciledTotals() {
        val now = 1_700_000_000_000L
        val dataset = buildRichDataset(now)
        val pkg = ImportVaultExportBuilder.buildPackage(
            dataset,
            VaultExportRequest(VaultExportScope.SHIPMENT, scopeId = "shp-1", filter = ImportVaultFilter(nowMillis = now), language = AppLanguage.BANGLA)
        )
        val report = ImportVaultPdfGenerator.buildCentralizedPdfReport(pkg, UserRole.OWNER_ADMIN)
        assertEquals(4, report.kpiBadges.size)
        assertTrue(report.sections.size >= 3)
        val metaSectionRows = report.sections.first().rows.flattenCellsText()
        assertTrue(metaSectionRows.contains("BDT"))
        assertTrue(metaSectionRows.contains("SHP-2026-001"))
        assertTrue(metaSectionRows.contains("Provisional") || metaSectionRows.contains("সতর্কতা"))
        val finSection = report.sections[1]
        assertTrue(finSection.summaryRows.first().cells.first().contains(pkg.totals.profitLabel))
    }

    @Test
    fun test4_attachmentFileStoreDeletesOnlyManagedVaultFilesAndEngineAutoLinksDocuments() {
        val tmpRoot = Files.createTempDirectory("vault_attach_test_").toFile()
        try {
            val poDir = File(tmpRoot, "po_attachments").apply { mkdirs() }
            val imgDir = File(tmpRoot, "product_images").apply { mkdirs() }
            val poFile = File(poDir, "po_keep_me.pdf").apply { writeText("ERP_PO_INVOICE") }
            val imgFile = File(imgDir, "img_keep_me.jpg").apply { writeText("ERP_PRODUCT_IMG") }

            val stored = ImportVaultAttachmentFileStore.saveStreamAttachment(
                filesDir = tmpRoot,
                inputStream = ByteArrayInputStream("CUSTOM_BL_PDF".toByteArray()),
                preferredName = "Bill of Lading.pdf",
                scopePrefix = "shp-1",
                mimeType = "application/pdf",
                role = UserRole.OWNER_ADMIN
            )
            assertTrue(File(stored.absolutePath).exists())
            assertTrue(ImportVaultAttachmentFileStore.isSafeManagedVaultAttachmentPath(tmpRoot, stored.absolutePath))

            val tempExport = File(tmpRoot, "report.xlsx").apply { writeText("FAKE_XLSX") }
            val snapshot = ImportVaultAttachmentFileStore.saveImmutableSnapshotFile(
                filesDir = tmpRoot,
                sourceFile = tempExport,
                preferredName = "Q1_Close.xlsx",
                scopePrefix = "shp-1",
                role = UserRole.OWNER_ADMIN
            )
            assertTrue(File(snapshot.absolutePath).exists())

            // Deleting PO or product image paths through Vault delete helper MUST fail safely and keep files intact
            assertFalse(ImportVaultAttachmentFileStore.deleteManagedAttachmentFile(tmpRoot, poFile.absolutePath, UserRole.OWNER_ADMIN))
            assertFalse(ImportVaultAttachmentFileStore.deleteManagedAttachmentFile(tmpRoot, imgFile.absolutePath, UserRole.OWNER_ADMIN))
            assertTrue(poFile.exists())
            assertTrue(imgFile.exists())

            // Deleting managed vault attachment removes only vault file
            assertTrue(ImportVaultAttachmentFileStore.deleteManagedAttachmentFile(tmpRoot, stored.absolutePath, UserRole.OWNER_ADMIN))
            assertFalse(File(stored.absolutePath).exists())
            assertTrue(File(snapshot.absolutePath).exists())

            // Verify Engine auto-linking + Live vs Snapshot separation
            val detail = ImportVaultAnalyticsEngine.computeShipmentDetail(buildRichDataset(), "shp-1")!!
            val docs = detail.documentRefs
            assertTrue(docs.any { it.isLiveRegeneratedReport && !it.isImmutableSnapshot })
            assertTrue(docs.any { it.isImmutableSnapshot && !it.isLiveRegeneratedReport && it.attachmentId == "iva-snap-1" })
            assertTrue(docs.any { it.attachmentId == "iva-local-1" && !it.isImmutableSnapshot })
            assertTrue(docs.any { it.localFilePath == "/files/po_attachments/po1__sup_invoice.pdf" })
            assertTrue(docs.any { it.localFilePath == "/files/freight/awb_991.pdf" })
            assertTrue(docs.any { it.localFilePath == "/files/expenses/dock_receipt.jpg" })
            assertTrue(docs.any { it.id.startsWith("doc-ret-") })
            assertTrue(docs.any { it.id.startsWith("doc-acost-") })
        } finally {
            tmpRoot.deleteRecursively()
        }
    }

    @Test
    fun test5_backupZipIntegrityAndVaultAttachmentsRestoreRoundTrip() {
        val tmpRoot = Files.createTempDirectory("vault_backup_test_").toFile()
        try {
            val validZip = File(tmpRoot, "valid_backup.zip")
            val payloadBytes = "VAULT_SNAPSHOT_PAYLOAD_12345".toByteArray(StandardCharsets.UTF_8)
            ZipOutputStream(FileOutputStream(validZip)).use { zos ->
                zos.putNextEntry(ZipEntry("database/advance_erp.db"))
                zos.write("SQLite format 3\u0000".toByteArray(StandardCharsets.US_ASCII))
                zos.closeEntry()
                zos.putNextEntry(ZipEntry("metadata.json"))
                zos.write("{}".toByteArray())
                zos.closeEntry()
                zos.putNextEntry(ZipEntry("vault_attachments/snapshot_shp1__close.pdf"))
                zos.write(payloadBytes)
                zos.closeEntry()
            }

            assertTrue(verifyBackupZipEntriesIntegrity(validZip))

            val restoreDir = File(tmpRoot, "restored_vault")
            val extractedCount = ZipFile(validZip).use { zip ->
                extractVaultAttachmentsFromZip(zip, restoreDir)
            }
            assertEquals(1, extractedCount)
            val restoredFile = File(restoreDir, "snapshot_shp1__close.pdf")
            assertTrue(restoredFile.exists())
            assertTrue(payloadBytes.contentEquals(restoredFile.readBytes()))

            // Zip slip traversal attempt rejected
            val badZip = File(tmpRoot, "traversal_backup.zip")
            ZipOutputStream(FileOutputStream(badZip)).use { zos ->
                zos.putNextEntry(ZipEntry("database/advance_erp.db"))
                zos.write("SQLite format 3\u0000".toByteArray(StandardCharsets.US_ASCII))
                zos.closeEntry()
                zos.putNextEntry(ZipEntry("vault_attachments/../../evil.db"))
                zos.write("hack".toByteArray())
                zos.closeEntry()
            }
            assertFalse(verifyBackupZipEntriesIntegrity(badZip))
        } finally {
            tmpRoot.deleteRecursively()
        }
    }

    @Test
    fun test6_nonOwnerRolesRejectedAtExportAndAttachmentSecurityBoundaries() {
        val dataset = buildRichDataset()
        val ownerPkg = ImportVaultExportBuilder.buildPackage(
            dataset,
            VaultExportRequest(VaultExportScope.PORTFOLIO, role = UserRole.OWNER_ADMIN)
        )
        val tmpRoot = Files.createTempDirectory("vault_sec_test_").toFile()
        try {
            val nonOwners = listOf(
                UserRole.MANAGER,
                UserRole.ACCOUNTANT,
                UserRole.SALES_OPERATOR,
                UserRole.PURCHASE_OPERATOR,
                UserRole.INVENTORY_OPERATOR
            )
            val sampleOut = File(tmpRoot, "forbidden.xlsx")
            val sampleSrc = File(tmpRoot, "src.pdf").apply { writeText("PDF") }

            for (role in nonOwners) {
                assertThrows(SecurityException::class.java) {
                    ImportVaultExportBuilder.buildPackage(
                        dataset,
                        VaultExportRequest(VaultExportScope.PORTFOLIO, role = role)
                    )
                }
                assertThrows(SecurityException::class.java) {
                    ImportVaultExcelGenerator.exportPackageToXlsxFile(ownerPkg, sampleOut, role = role)
                }
                assertThrows(SecurityException::class.java) {
                    ImportVaultPdfGenerator.buildCentralizedPdfReport(ownerPkg, role = role)
                }
                assertThrows(SecurityException::class.java) {
                    ImportVaultAttachmentFileStore.saveStreamAttachment(
                        filesDir = tmpRoot,
                        inputStream = ByteArrayInputStream("X".toByteArray()),
                        preferredName = "doc.pdf",
                        scopePrefix = "shp-1",
                        role = role
                    )
                }
                assertThrows(SecurityException::class.java) {
                    ImportVaultAttachmentFileStore.saveImmutableSnapshotFile(
                        filesDir = tmpRoot,
                        sourceFile = sampleSrc,
                        preferredName = "snap.pdf",
                        scopePrefix = "shp-1",
                        role = role
                    )
                }
                assertThrows(SecurityException::class.java) {
                    ImportVaultAttachmentFileStore.deleteManagedAttachmentFile(
                        filesDir = tmpRoot,
                        filePath = sampleSrc.absolutePath,
                        role = role
                    )
                }
            }
        } finally {
            tmpRoot.deleteRecursively()
        }
    }

    private fun List<com.pocketdev.advance_erp.core.pdf.PdfReportRow>.flattenCellsText(): String =
        joinToString("\n") { it.cells.joinToString(" | ") }
}
