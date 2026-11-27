package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.core.database.AdvanceErpDatabase
import com.pocketdev.advance_erp.core.security.PinSecurityHelper
import com.pocketdev.advance_erp.domain.model.DataClearImpactPreview
import com.pocketdev.advance_erp.domain.model.DataClearModule
import com.pocketdev.advance_erp.domain.model.DataClearPreset
import com.pocketdev.advance_erp.domain.model.SelectiveDataClearPlanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.BufferedReader
import java.io.File

class SelectiveDataClearPlannerTest {

    @Test
    fun presets_mapExactModulesAndDetectBackToPresetOrCustom() {
        val smartFull = SelectiveDataClearPlanner.modulesForPreset(DataClearPreset.SMART_FULL_RESET)
        assertEquals(
            setOf(
                DataClearModule.INVENTORY_STOCK_RESET,
                DataClearModule.PO_RESET_KEEP_STRUCTURE,
                DataClearModule.SALES_FULL_CLEAR,
                DataClearModule.LEDGER_AMOUNTS_RESET,
                DataClearModule.COMMUNICATION_LOGS_CLEAR,
                DataClearModule.AUDIT_AND_DRAFTS_CLEAR
            ),
            smartFull
        )
        assertEquals(DataClearPreset.SMART_FULL_RESET, SelectiveDataClearPlanner.detectMatchingPreset(smartFull))

        val coreOnly = SelectiveDataClearPlanner.modulesForPreset(DataClearPreset.CORE_OPERATIONS_RESET)
        assertEquals(
            setOf(
                DataClearModule.INVENTORY_STOCK_RESET,
                DataClearModule.PO_RESET_KEEP_STRUCTURE,
                DataClearModule.SALES_FULL_CLEAR,
                DataClearModule.LEDGER_AMOUNTS_RESET
            ),
            coreOnly
        )
        assertEquals(DataClearPreset.CORE_OPERATIONS_RESET, SelectiveDataClearPlanner.detectMatchingPreset(coreOnly))

        val singlePo = setOf(DataClearModule.PO_RESET_KEEP_STRUCTURE)
        assertEquals(DataClearPreset.CUSTOM, SelectiveDataClearPlanner.detectMatchingPreset(singlePo))
        assertTrue(SelectiveDataClearPlanner.modulesForPreset(DataClearPreset.CUSTOM).isEmpty())
    }

    @Test
    fun validateConfirmationInput_enforcesPinWhenAppLockEnabledElseRequiresClear() {
        val salt = PinSecurityHelper.generateSalt()
        val hash = PinSecurityHelper.hashPin("2468", salt)

        assertTrue(
            SelectiveDataClearPlanner.validateConfirmationInput(
                isAppLockEnabled = true,
                storedPinHash = hash,
                storedPinSalt = salt,
                rawInput = "2468",
                pinVerifier = PinSecurityHelper::verifyPin
            )
        )
        assertFalse(
            SelectiveDataClearPlanner.validateConfirmationInput(
                isAppLockEnabled = true,
                storedPinHash = hash,
                storedPinSalt = salt,
                rawInput = "CLEAR",
                pinVerifier = PinSecurityHelper::verifyPin
            )
        )
        assertFalse(
            SelectiveDataClearPlanner.validateConfirmationInput(
                isAppLockEnabled = true,
                storedPinHash = hash,
                storedPinSalt = salt,
                rawInput = "9999",
                pinVerifier = PinSecurityHelper::verifyPin
            )
        )

        assertTrue(
            SelectiveDataClearPlanner.validateConfirmationInput(
                isAppLockEnabled = false,
                storedPinHash = null,
                storedPinSalt = null,
                rawInput = "clear",
                pinVerifier = PinSecurityHelper::verifyPin
            )
        )
        assertFalse(
            SelectiveDataClearPlanner.validateConfirmationInput(
                isAppLockEnabled = false,
                storedPinHash = null,
                storedPinSalt = null,
                rawInput = "RESET",
                pinVerifier = PinSecurityHelper::verifyPin
            )
        )
    }

    @Test
    fun buildWarnings_detectsPartialModuleDependenciesCleanly() {
        val preview = DataClearImpactPreview(
            moduleCounts = emptyMap(),
            preservedProductCount = 12,
            preservedVariantCount = 30,
            preservedPoCount = 4,
            preservedAccountCount = 10,
            preservedCustomerCount = 5,
            preservedSupplierCount = 3,
            poReceiptLayerCount = 6
        )

        val poOnlyWarnings = SelectiveDataClearPlanner.buildWarnings(
            setOf(DataClearModule.PO_RESET_KEEP_STRUCTURE),
            preview
        )
        assertEquals(1, poOnlyWarnings.size)
        assertTrue(poOnlyWarnings.first().first.contains("6 PO-received"))

        val salesOnlyWarnings = SelectiveDataClearPlanner.buildWarnings(
            setOf(DataClearModule.SALES_FULL_CLEAR),
            preview
        )
        assertEquals(1, salesOnlyWarnings.size)
        assertTrue(salesOnlyWarnings.first().first.contains("will NOT automatically re-add sold stock"))

        val ledgerOnlyWarnings = SelectiveDataClearPlanner.buildWarnings(
            setOf(DataClearModule.LEDGER_AMOUNTS_RESET),
            preview
        )
        assertEquals(1, ledgerOnlyWarnings.size)
        assertTrue(ledgerOnlyWarnings.first().first.contains("unpaid invoices/confirmed POs"))

        val fullResetWarnings = SelectiveDataClearPlanner.buildWarnings(
            SelectiveDataClearPlanner.modulesForPreset(DataClearPreset.SMART_FULL_RESET),
            preview
        )
        assertEquals(1, fullResetWarnings.size)
        assertTrue(fullResetWarnings.first().first.contains("preserved CONFIRMED PO bills (4)"))

        val emptyPoFullResetWarnings = SelectiveDataClearPlanner.buildWarnings(
            SelectiveDataClearPlanner.modulesForPreset(DataClearPreset.SMART_FULL_RESET),
            preview.copy(preservedPoCount = 0)
        )
        assertTrue(emptyPoFullResetWarnings.isEmpty())
    }

    @Test
    fun selectiveDataClear_sqliteForeignKeyAndPoStructurePreservationVerification() {
        val schemaFile = File("schemas/com.pocketdev.advance_erp.core.database.AdvanceErpDatabase/16.json")
        assertTrue(schemaFile.exists())
        assertEquals(16, AdvanceErpDatabase.DATABASE_VERSION)

        val script = """
            import json, sqlite3
            s = json.load(open(r"${schemaFile.absolutePath}"))['database']
            conn = sqlite3.connect(':memory:')
            cur = conn.cursor()
            cur.execute('PRAGMA foreign_keys = ON')
            for e in s['entities']:
                cur.execute(e['createSql'].replace('${'$'}{TABLE_NAME}', e['tableName']))

            # Seed masters + chart_of_accounts + 1 received/paid PO + carton + warehouse receipt + sales invoice + journal
            cur.execute("INSERT INTO product_categories (id, nameEn, nameBn, code, defaultFreightRatePerKgBdtMinor) VALUES ('cat-1','Insole','ইনসোল','INS',0)")
            cur.execute("INSERT INTO products (id, categoryId, nameEn, nameBn, baseSku, unit, purchasePriceMinor, sellingPriceMinor, minStockAlert, isActive, createdAt, updatedAt) VALUES ('p-1','cat-1','4D Insole','4D ইনসোল','4DCI','Pair',85000,138000,10,1,1,1)")
            cur.execute("INSERT INTO product_variants (id, productId, sku, color, size, variantPriceMinor, variantCostMinor, isActive) VALUES ('v-1','p-1','4DCI-G-38','Gray','38',138000,85000,1)")
            cur.execute("INSERT INTO warehouses (id, name, code, location, isDefault, isActive) VALUES ('wh-1','Main','WH1','Dhaka',1,1)")
            cur.execute("INSERT INTO customers (id, customerCode, name, phone, address, creditLimitMinor, outstandingBalanceMinor, isActive, createdAt) VALUES ('c-1','C1','Hasan','018','Dhaka',0,138000,1,1)")
            cur.execute("INSERT INTO suppliers (id, supplierCode, companyName, contactPerson, phone, address, country, currency, outstandingPayableMinor, isActive, createdAt) VALUES ('s-1','S1','Shenzhen Co','Li','86','SZ','China','CNY',50000,1,1)")
            cur.execute("INSERT INTO chart_of_accounts (id, accountCode, nameEn, nameBn, accountType, normalBalance, isActive) VALUES ('coa-1010','1010','Cash','নগদ','ASSET','DEBIT',1)")
            cur.execute("INSERT INTO purchase_orders (id, poNumber, supplierId, status, currency, exchangeRateToBdt, totalAmountMinor, totalBdtAmountMinor, paidAmountMinor, paidBdtAmountMinor, orderDate, isActualCostFinalized, shippingRatePerKgBdt, totalGrossWeightKg, totalNetWeightKg, totalCbm, totalCartons, createdAt, updatedAt, shipmentId) VALUES ('po-1','PO-1','s-1','COMPLETED','CNY','17',10000,170000,5000,85000,1,1,'10','25.0','22.0','0.5',5,1,1,'shp-1')")
            cur.execute("INSERT INTO purchase_order_items (id, purchaseOrderId, productId, variantId, quantity, unitPriceMinor, totalAmountMinor, totalBdtAmountMinor, receivedQuantity, cartonQuantity, pcsPerCarton, grossWeightKg, netWeightKg, cbm) VALUES ('poi-1','po-1','p-1','v-1',100,100,10000,170000,100,5,20,'25.0','22.0','0.5')")
            cur.execute("INSERT INTO import_shipments (id, shipmentNumber, supplierId, purchaseOrderId, shippingMethod, origin, destination, shippingDate, estimatedArrivalDate, actualArrivalDate, status, totalCartons, totalQuantity, totalGrossWeightKg, totalNetWeightKg, totalCbm, createdAt, updatedAt) VALUES ('shp-1','BATCH-PO-1','s-1','po-1','SEA','SZ','CTG',1,2,3,'COMPLETED',5,100,'25.0','22.0','0.5',1,1)")
            cur.execute("INSERT INTO shipment_cartons (id, shipmentId, cartonNumber, productId, variantId, quantity, lengthCm, widthCm, heightCm, grossWeightKg, netWeightKg, cbm, purchaseOrderId, isMixedCarton, totalCartonQuantity, cartonGrossWeightKg, supplierId) VALUES ('ctn-1','shp-1','C1','p-1','v-1',100,'50','40','30','25.0','22.0','0.5','po-1',0,100,'25.0','s-1')")
            cur.execute("INSERT INTO warehouse_receipts (id, receiptNumber, shipmentId, warehouseId, receiptDate, status, receivedBy, hasDiscrepancies, discrepancyApproved, purchaseOrderId, receivingMethod, createdAt) VALUES ('wr-1','WR-1','shp-1','wh-1',1,'POSTED','Admin',0,0,'po-1','CARTON',1)")
            cur.execute("INSERT INTO supplier_payments (id, paymentNumber, supplierId, purchaseOrderId, amountMinor, currency, exchangeRateToBdt, bdtAmountMinor, paymentMethod, paymentAccount, paymentDate, createdAt) VALUES ('sp-1','SP-1','s-1','po-1',5000,'CNY','17',85000,'BANK','coa-1010',1,1)")
            cur.execute("INSERT INTO inventory_cost_layers (id, productId, variantId, warehouseId, shipmentId, shipmentNumber, warehouseReceiptId, receiptNumber, initialQuantity, remainingQuantity, unitLandedCostBdtMinor, originalCurrency, originalPurchasePriceMinor, exchangeRateToBdt, allocatedExpensePerUnitBdtMinor, receiptDate, isActive, purchaseOrderId, originReceiptDate, layerSourceType, createdAt) VALUES ('icl-po','p-1','v-1','wh-1','shp-1','BATCH-PO-1','wr-1','WR-1',100,90,1000,'CNY',50,'17',150,1,1,'po-1',1,'PURCHASE_RECEIPT',1)")
            cur.execute("INSERT INTO inventory_cost_layers (id, productId, variantId, warehouseId, shipmentId, shipmentNumber, warehouseReceiptId, receiptNumber, initialQuantity, remainingQuantity, unitLandedCostBdtMinor, originalCurrency, originalPurchasePriceMinor, exchangeRateToBdt, allocatedExpensePerUnitBdtMinor, receiptDate, isActive, originReceiptDate, layerSourceType, createdAt) VALUES ('icl-init','p-1','v-1','wh-1','INIT','OPEN','INIT','OPEN',10,10,800,'BDT',800,'1',0,1,1,1,'OPENING_STOCK',1)")
            cur.execute("INSERT INTO stock_balances (id, warehouseId, productId, variantId, quantity, reservedQuantity, updatedAt) VALUES ('sb-1','wh-1','p-1','v-1',100,0,1)")
            cur.execute("INSERT INTO sales_invoices (id, invoiceNumber, customerId, customerName, customerPhone, warehouseId, warehouseName, status, subtotalMinor, discountMinor, taxMinor, totalAmountMinor, paidAmountMinor, dueAmountMinor, totalCogsMinor, invoiceDate, isPosted, salesType, createdAt, updatedAt) VALUES ('inv-1','INV-1','c-1','Hasan','018','wh-1','Main','UNPAID',138000,0,0,138000,0,138000,0,1,1,'WHOLESALE',1,1)")

            # Execute partial PO reset ONLY (without clearing inventory_stock or sales) to verify full PO kept intact & unreceived/unpaid
            cur.execute("DELETE FROM inventory_cost_layers WHERE (purchaseOrderId IS NOT NULL AND purchaseOrderId != '') OR warehouseReceiptId IN (SELECT id FROM warehouse_receipts) OR shipmentId IN (SELECT id FROM import_shipments)")
            cur.execute("UPDATE stock_balances SET quantity = COALESCE((SELECT SUM(icl.remainingQuantity) FROM inventory_cost_layers icl WHERE icl.warehouseId = stock_balances.warehouseId AND icl.productId = stock_balances.productId AND ((icl.variantId IS NULL AND stock_balances.variantId IS NULL) OR icl.variantId = stock_balances.variantId) AND icl.isActive = 1), 0)")
            cur.execute("DELETE FROM warehouse_receipts")
            cur.execute("DELETE FROM supplier_payments")
            cur.execute("UPDATE import_cost_items SET isCapitalized = 0, costAdjustmentReference = NULL")
            cur.execute("UPDATE purchase_order_items SET receivedQuantity = 0")
            cur.execute("UPDATE purchase_orders SET status = CASE WHEN status IN ('DRAFT', 'CANCELLED') THEN status ELSE 'CONFIRMED' END, paidAmountMinor = 0, paidBdtAmountMinor = 0, isActualCostFinalized = 0, actualCostFinalizedAt = NULL")
            cur.execute("UPDATE import_shipments SET status = 'DRAFT', actualArrivalDate = NULL")
            cur.execute("UPDATE suppliers SET outstandingPayableMinor = MAX(0, COALESCE((SELECT SUM(totalBdtAmountMinor) FROM purchase_orders po WHERE po.supplierId = suppliers.id AND po.status NOT IN ('DRAFT', 'CANCELLED')), 0) - COALESCE((SELECT SUM(bdtAmountMinor) FROM supplier_payments sp WHERE sp.supplierId = suppliers.id), 0))")

            cur.execute('PRAGMA foreign_key_check')
            assert cur.fetchall() == []
            assert cur.execute("SELECT quantity FROM stock_balances WHERE id='sb-1'").fetchone()[0] == 10
            assert cur.execute("SELECT status, totalAmountMinor, totalBdtAmountMinor, totalCartons, paidAmountMinor, isActualCostFinalized, shipmentId FROM purchase_orders WHERE id='po-1'").fetchone() == ('CONFIRMED', 10000, 170000, 5, 0, 0, 'shp-1')
            assert cur.execute("SELECT quantity, receivedQuantity, cartonQuantity, pcsPerCarton, unitPriceMinor FROM purchase_order_items WHERE id='poi-1'").fetchone() == (100, 0, 5, 20, 100)
            assert cur.execute("SELECT COUNT(*) FROM shipment_cartons").fetchone()[0] == 1
            assert cur.execute("SELECT COUNT(*) FROM warehouse_receipts").fetchone()[0] == 0
            assert cur.execute("SELECT COUNT(*) FROM supplier_payments").fetchone()[0] == 0
            assert cur.execute("SELECT status, totalCartons, totalQuantity FROM import_shipments WHERE id='shp-1'").fetchone() == ('DRAFT', 5, 100)
            assert cur.execute("SELECT outstandingPayableMinor FROM suppliers WHERE id='s-1'").fetchone()[0] == 170000
            assert cur.execute("SELECT COUNT(*) FROM product_variants").fetchone()[0] == 1
            print("SELECTIVE_SQL_OK")
        """.trimIndent()

        val process = ProcessBuilder("python3", "-c", script).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().use(BufferedReader::readText)
        val exitCode = process.waitFor()
        assertEquals("SQLite selective reset failed:\n$output", 0, exitCode)
        assertEquals("SELECTIVE_SQL_OK", output.trim())
    }
}
