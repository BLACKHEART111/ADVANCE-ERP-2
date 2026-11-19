package com.pocketdev.advance_erp

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pocketdev.advance_erp.core.database.AdvanceErpDatabase
import com.pocketdev.advance_erp.data.entity.*
import com.pocketdev.advance_erp.data.repository.ImportRepositoryImpl
import com.pocketdev.advance_erp.data.repository.AccountingRepositoryImpl
import com.pocketdev.advance_erp.domain.repository.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.math.BigDecimal

/** Real SQLite foreign keys and transactions; never opens the user's database. */
@RunWith(AndroidJUnit4::class)
class ImportPersistenceRegressionTest {
    private lateinit var db: AdvanceErpDatabase
    private lateinit var repository: ImportRepositoryImpl
    @Before fun setup() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, AdvanceErpDatabase::class.java).build()
        repository = ImportRepositoryImpl(db, db.purchaseOrderDao(), db.supplierPaymentDao(), db.importShipmentDao(),
            db.importCostDao(), db.warehouseReceiptDao(), db.inventoryCostLayerDao(), db.minimalImportDao(),
            db.supplierDao(), db.productDao(), db.productVariantDao(), db.stockBalanceDao(), db.stockMovementDao(),
            AccountingRepositoryImpl(db, db.accountingDao()))
        db.supplierDao().insertSupplier(Supplier(id="s", supplierCode="S", companyName="Test", phone="", address=""))
        db.productCategoryDao().insertCategory(ProductCategory(id="c", nameEn="Test", nameBn="পরীক্ষা", code="T"))
        db.productDao().insertProduct(Product(id="p", categoryId="c", nameEn="Test", nameBn="পরীক্ষা", baseSku="P", purchasePriceMinor=4500, sellingPriceMinor=9000))
        db.productVariantDao().insertVariants(listOf("s", "m").map { ProductVariant(id=it, productId="p", sku=it, variantCostMinor=4500, variantPriceMinor=9000) })
    }
    @After fun close() { db.close() }
    private fun items() = listOf(PurchaseOrderItemDraft("p", "s", 20, 4500), PurchaseOrderItemDraft("p", "m", 10, 4500))
    private fun carton(weight: String = "15") = PoMixedCartonDraft("CTN-01", BigDecimal(weight), lines=listOf(
        MixedCartonLineDraft("p", "s", quantity=20, manualGrossWeightKg=BigDecimal("12")),
        MixedCartonLineDraft("p", "m", quantity=10, manualGrossWeightKg=BigDecimal("3"))))
    private suspend fun save(id: String?=null, boxes: List<PoMixedCartonDraft> = listOf(carton()), status: PurchaseOrderStatus=PurchaseOrderStatus.DRAFT): String =
        repository.savePurchaseOrderDraft(id, "s", "CNY", BigDecimal("17"), items(), "test", status, boxes, 1000, mapOf("c" to 12000L), 500, 200, null)

    @Test fun saveReloadAndEditPreserveShipmentLinkManualWeightsAndBdtCosts() = runBlocking {
        val id=save()
        val shipment=repository.getShipmentsForOrderSync(id).single()
        val before=repository.getItemsForOrderSync(id).map { it.id }
        val cartons=repository.getCartonsForOrderSync(id).associateBy { it.variantId }
        assertEquals(0, BigDecimal("12").compareTo(BigDecimal(cartons.getValue("s").grossWeightKg)))
        assertEquals(0, BigDecimal("3").compareTo(BigDecimal(cartons.getValue("m").grossWeightKg)))
        assertEquals(1, repository.getPurchaseOrderById(id)!!.totalCartons)
        val handling=db.importCostDao().getCostItemsForOrderSync(id).single { it.description.contains("Handling") }
        assertEquals("BDT", handling.currency)
        assertEquals(500L, handling.bdtAmountMinor)
        save(id)
        assertEquals(shipment.id, repository.getShipmentsForOrderSync(id).single().id)
        assertEquals(before, repository.getItemsForOrderSync(id).map { it.id })
        assertEquals(2, repository.getCartonsForOrderSync(id).size)
    }
    @Test fun invalidCartonRollsBackNewOrderShipmentCostsAndPayable() = runBlocking {
        try { save(boxes=listOf(carton("10")), status=PurchaseOrderStatus.CONFIRMED); fail("Invalid manual sum must fail") }
        catch (_: IllegalArgumentException) {}
        assertEquals(0, db.purchaseOrderDao().getOrderCount())
        assertTrue(db.importShipmentDao().getAllShipments().first().isEmpty())
        assertEquals(0L, db.supplierDao().getSupplierById("s")!!.outstandingPayableMinor)
    }
    @Test fun editFailureRollsBackExistingOrderAndCartons() = runBlocking {
        val id=save()
        val old=repository.getPurchaseOrderById(id)
        val cartons=repository.getCartonsForOrderSync(id)
        try { save(id, listOf(carton("10"))); fail("Invalid carton must fail") } catch (_: IllegalArgumentException) {}
        assertEquals(old, repository.getPurchaseOrderById(id))
        assertEquals(cartons, repository.getCartonsForOrderSync(id))
    }
    @Test fun draftConfirmationCreatesLiabilityOnceAndCanBeCancelledWithAutoBatch() = runBlocking {
        val id=save()
        save(id, status=PurchaseOrderStatus.CONFIRMED)
        save(id, status=PurchaseOrderStatus.CONFIRMED)
        assertEquals(2295000L, db.supplierDao().getSupplierById("s")!!.outstandingPayableMinor)
        repository.cancelPurchaseOrder(id, "test")
        assertEquals(0L, db.supplierDao().getSupplierById("s")!!.outstandingPayableMinor)
    }
    @Test fun failedJournalRollsBackPaymentAndPaidTotals() = runBlocking {
        val id=save(status=PurchaseOrderStatus.CONFIRMED)
        val payable=db.supplierDao().getSupplierById("s")!!.outstandingPayableMinor
        try { repository.recordSupplierPayment("s", id, 100L, "CNY", BigDecimal("17"), PaymentMethod.BANK_TT, "Test", null, null); fail("Missing accounting accounts must fail") }
        catch (_: Exception) {}
        assertTrue(repository.getPaymentsForOrderSync(id).isEmpty())
        assertEquals(0L, repository.getPurchaseOrderById(id)!!.paidAmountMinor)
        assertEquals(payable, db.supplierDao().getSupplierById("s")!!.outstandingPayableMinor)
    }

    @Test fun receivingMissingDoesNotSubtractGoodTwiceAndCannotPostTwice() = runBlocking {
        val id=save(status=PurchaseOrderStatus.CONFIRMED)
        val po=repository.getPurchaseOrderById(id)!!
        db.purchaseOrderDao().updateOrder(po.copy(isActualCostFinalized=true))
        db.warehouseDao().insertWarehouse(Warehouse("wh","Test","WH","Test"))
        listOf("1210","1220","5050","1050").forEach { code ->
            db.accountingDao().insertAccount(ChartOfAccount("coa-$code",code,"Test","পরীক্ষা",AccountType.ASSET.name,NormalBalance.DEBIT.name))
        }
        val draft=ReceiptItemDraft(productId="p",variantId="s",expectedQuantity=20,receivedQuantity=18,
            goodQuantity=16,damagedQuantity=2,missingQuantity=2,pendingQuantity=0)
        val receipt=repository.createPurchaseReceiving(id,"wh","PHYSICAL_COUNT",listOf(draft),null)
        db.warehouseReceiptDao().getReceiptById(receipt)!!.let {
            db.warehouseReceiptDao().updateReceipt(it.copy(discrepancyApproved=true))
        }
        repository.postPurchaseReceiving(receipt)
        assertEquals(16,db.stockBalanceDao().getVariantBalance("wh","p","s")!!.quantity)
        try { repository.postPurchaseReceiving(receipt); fail("Already posted") } catch (_: IllegalStateException) {}
        assertEquals(16,db.stockBalanceDao().getVariantBalance("wh","p","s")!!.quantity)
        val another=repository.createPurchaseReceiving(id,"wh","PHYSICAL_COUNT",listOf(draft),null)
        db.warehouseReceiptDao().getReceiptById(another)!!.let {
            db.warehouseReceiptDao().updateReceipt(it.copy(discrepancyApproved=true))
        }
        try { repository.postPurchaseReceiving(another); fail("Cannot receive over remaining quantity") } catch (_: IllegalArgumentException) {}
        assertEquals(16,db.stockBalanceDao().getVariantBalance("wh","p","s")!!.quantity)
    }

    @Test fun duplicateCreatesIndependentDraftAndBatchWithoutFinalizedFlags() = runBlocking {
        val id=save(status=PurchaseOrderStatus.CONFIRMED)
        val oldBatch=repository.getShipmentsForOrderSync(id).single()
        db.purchaseOrderDao().updateOrder(repository.getPurchaseOrderById(id)!!.copy(isActualCostFinalized=true))
        val duplicate=repository.duplicatePurchaseOrder(id)
        val order=repository.getPurchaseOrderById(duplicate)!!
        assertEquals(PurchaseOrderStatus.DRAFT.name,order.status)
        assertFalse(order.isActualCostFinalized)
        assertEquals(0L,order.paidAmountMinor)
        assertNotEquals(oldBatch.id,repository.getShipmentsForOrderSync(duplicate).single().id)
        assertEquals(2,repository.getCartonsForOrderSync(duplicate).size)
        assertEquals(2295000L,db.supplierDao().getSupplierById("s")!!.outstandingPayableMinor)
    }
    @Test fun manualVariantAllocationsReachFinalSnapshotsWithoutLostKeys() = runBlocking {
        val id=save(status=PurchaseOrderStatus.CONFIRMED)
        val cost=repository.addPurchaseCostItem(id,CostCategory.OTHER_CHARGES,"Manual test",300L,"BDT",BigDecimal.ONE,
            com.pocketdev.advance_erp.core.engine.AllocationMethod.MANUAL,
            manualAllocationsBdtMinor=mapOf("p:s" to 250L,"p:m" to 50L))
        val shipment=repository.getShipmentsForOrderSync(id).single()
        val snapshots=repository.finalizePurchaseActualCost(id)
        assertEquals(2493000L,snapshots.sumOf { it.totalLandedCostBdtMinor })
        val allocation=db.importCostDao().getAllocationsForShipment(shipment.id).first().filter { it.costItemId==cost }.associate { it.variantId to it.allocatedBdtMinor }
        assertEquals(mapOf("s" to 250L,"m" to 50L),allocation)
        try { repository.deletePurchaseCostItem(cost); fail("Cannot delete capitalized costs") } catch (_: IllegalArgumentException) {}
    }

    @Test fun localPurchaseKeepsBdtDeliveryAndZeroInternationalFreight() = runBlocking {
        db.productCategoryDao().updateCategoryDefaultFreightRate("c",12000)
        val id=repository.savePurchaseOrderDraft(null,"s","BDT",BigDecimal.ONE,items(),null,PurchaseOrderStatus.CONFIRMED,
            listOf(carton()),1000,mapOf("c" to 0L),500,200,null)
        val snapshots=repository.finalizePurchaseActualCost(id)
        assertEquals(136700L,snapshots.sumOf { it.totalLandedCostBdtMinor })
        assertEquals(0L,snapshots.sumOf { it.internationalFreightBdtMinor })
    }

    @Test fun editingPreservesNetWeightDimensionsAndExplicitCbm() = runBlocking {
        val box=carton().copy(netWeightKg=BigDecimal("13.5"),lengthCm=BigDecimal("50"),widthCm=BigDecimal("40"),heightCm=BigDecimal("30"))
        val id=save(boxes=listOf(box))
        save(id,listOf(box))
        val order=repository.getPurchaseOrderById(id)!!
        assertEquals(0,BigDecimal("13.5").compareTo(BigDecimal(order.totalNetWeightKg)))
        assertEquals(0,BigDecimal("0.06").compareTo(BigDecimal(order.totalCbm)))
        assertTrue(repository.getCartonsForOrderSync(id).all { BigDecimal(it.lengthCm).compareTo(BigDecimal("50"))==0 })
        val explicit=box.copy(lengthCm=BigDecimal.ZERO,widthCm=BigDecimal.ZERO,heightCm=BigDecimal.ZERO,cbm=BigDecimal("0.125"))
        save(id,listOf(explicit))
        assertEquals(0,BigDecimal("0.125").compareTo(BigDecimal(repository.getPurchaseOrderById(id)!!.totalCbm)))
    }
    @Test fun packingAfterAdvanceIsAllowedButOverPackingIsAtomic() = runBlocking {
        val id=save(status=PurchaseOrderStatus.CONFIRMED)
        db.purchaseOrderDao().updateOrder(repository.getPurchaseOrderById(id)!!.copy(paidAmountMinor=100))
        repository.deleteCartonByNumber(id,"CTN-01")
        repository.addMixedCarton(id,"CTN-02",BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,
            BigDecimal("15"),BigDecimal("13.5"),carton().lines)
        val before=repository.getCartonsForOrderSync(id)
        assertEquals(2,before.size)
        try {
            repository.addMixedCarton(id,"CTN-03",BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,
                BigDecimal.ONE,BigDecimal.ONE,listOf(MixedCartonLineDraft("p","s",quantity=1)))
            fail("Cannot pack more than ordered")
        } catch (_: IllegalArgumentException) {}
        assertEquals(before,repository.getCartonsForOrderSync(id))
    }

    @Test fun failedExpenseJournalRollsBackExpenseRow() = runBlocking {
        val id=save(status=PurchaseOrderStatus.CONFIRMED)
        val shipment=repository.getShipmentsForOrderSync(id).single()
        val before=db.importCostDao().getCostItemsForShipmentSync(shipment.id)
        try {
            repository.addCostItem(shipment.id,CostCategory.OTHER_CHARGES,"Expense test",100,"BDT",BigDecimal.ONE,
                com.pocketdev.advance_erp.core.engine.AllocationMethod.QUANTITY,"Bank",null)
            fail("Missing journal accounts must roll back the expense")
        } catch (_: IllegalArgumentException) {}
        assertEquals(before,db.importCostDao().getCostItemsForShipmentSync(shipment.id))
    }

    @Test fun finalizedOrderMetadataEditPreservesEveryFinancialFieldAndLinks() = runBlocking {
        val id=save(status=PurchaseOrderStatus.CONFIRMED)
        val locked=repository.getPurchaseOrderById(id)!!.copy(paidAmountMinor=100,isActualCostFinalized=true)
        db.purchaseOrderDao().updateOrder(locked)
        val items=repository.getItemsForOrderSync(id)
        val shipments=repository.getShipmentsForOrderSync(id)
        repository.updatePurchaseOrderMetadata(id,"Updated delivery note","[]")
        val updated=repository.getPurchaseOrderById(id)!!
        assertEquals("Updated delivery note",updated.notes)
        assertEquals(locked,updated.copy(notes=locked.notes,attachments=locked.attachments,updatedAt=locked.updatedAt))
        assertEquals(items,repository.getItemsForOrderSync(id))
        assertEquals(shipments,repository.getShipmentsForOrderSync(id))
    }

    @Test fun completingWithDueIsIdempotentAndPreservesPayable() = runBlocking {
        val id = save(status = PurchaseOrderStatus.CONFIRMED)
        val payable = db.supplierDao().getSupplierById("s")!!.outstandingPayableMinor
        val record = repository.completePurchase(id, "SUPPLIER_CLAIM", "Supplier cannot fulfill balance", null)
        val repeated = repository.completePurchase(id, "SHORTAGE", "Repeated tap", null)
        assertEquals(record.id, repeated.id)
        assertEquals(135000L, record.paymentDueMinor)
        assertEquals(payable, db.supplierDao().getSupplierById("s")!!.outstandingPayableMinor)
        assertEquals("COMPLETED", repository.getPurchaseOrderById(id)!!.status)
        db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM purchase_completion_records").use {
            assertTrue(it.moveToFirst()); assertEquals(1, it.getInt(0))
        }
    }

    @Test fun failedCompletionStatusUpdateRollsBackRecord() = runBlocking {
        val id = save(status = PurchaseOrderStatus.CONFIRMED)
        db.openHelper.writableDatabase.execSQL("""
            CREATE TRIGGER fail_completion BEFORE UPDATE OF status ON purchase_orders
            WHEN NEW.status = 'COMPLETED' BEGIN SELECT RAISE(ABORT, 'Injected failure'); END
        """)
        try { repository.completePurchase(id, "SHORTAGE", "Lost in transit", null); fail("Expected injected failure") }
        catch (_: android.database.SQLException) { }
        assertNull(db.minimalImportDao().getCompletionRecordForOrder(id))
        assertEquals("CONFIRMED", repository.getPurchaseOrderById(id)!!.status)
    }

    @Test fun completionRejectsDraftAndUnexplainedShortage() = runBlocking {
        val id = save()
        try { repository.completePurchase(id, "SHORTAGE", "reason", null); fail("Draft cannot complete") }
        catch (_: IllegalArgumentException) { }
        save(id, status = PurchaseOrderStatus.CONFIRMED)
        for ((disposition, reason) in listOf("SHORTAGE" to "", "unknown" to "reason")) {
            try { repository.completePurchase(id, disposition, reason, null); fail("Invalid disposition") }
            catch (_: IllegalArgumentException) { }
        }
        assertNull(db.minimalImportDao().getCompletionRecordForOrder(id))
    }

    @Test fun concurrentCompletionReturnsOneRecord() = runBlocking {
        val id = save(status = PurchaseOrderStatus.CONFIRMED)
        val results = kotlinx.coroutines.coroutineScope {
            (1..8).map { async(kotlinx.coroutines.Dispatchers.IO) {
                repository.completePurchase(id, "SHORTAGE", "Agreed balance", null).id
            } }.map { it.await() }
        }
        assertEquals(1, results.distinct().size)
    }

    @Test fun historicalCompletionDuplicatesArePreservedAndLatestIsReturned() = runBlocking {
        val id = save(status = PurchaseOrderStatus.CONFIRMED)
        val original = repository.completePurchase(id, "SHORTAGE", "History", null)
        val later = original.copy(id = "historical-later", completedAt = original.completedAt + 1)
        db.minimalImportDao().insertCompletionRecord(later)
        assertEquals(later, repository.completePurchase(id, null, null, null))
        db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM purchase_completion_records").use {
            assertTrue(it.moveToFirst()); assertEquals(2, it.getInt(0))
        }
    }

    @Test fun expenseDocumentFailureRollsBackJournalAndBalances() = runBlocking {
        db.accountingDao().insertAccounts(listOf(
            ChartOfAccount("cash", "1010", "Cash", "নগদ", AccountType.ASSET.name, NormalBalance.DEBIT.name),
            ChartOfAccount("expense", "5190", "Expense", "ব্যয়", AccountType.EXPENSE.name, NormalBalance.DEBIT.name)
        ))
        val finance = com.pocketdev.advance_erp.data.repository.FinancialRepositoryImpl(
            db, db.accountingDao(), db.expenseDao(), db.cashBankDao(), db.auditLogDao(), db.salesDao(),
            db.customerPaymentDao(), db.supplierPaymentDao(), db.purchaseOrderDao(), db.customerDao(), db.supplierDao(),
            AccountingRepositoryImpl(db, db.accountingDao())
        )
        db.openHelper.writableDatabase.execSQL("""
            CREATE TRIGGER fail_expense BEFORE INSERT ON expense_vouchers
            BEGIN SELECT RAISE(ABORT, 'Injected voucher failure'); END
        """)
        try {
            finance.recordExpense("GENERAL_ADMIN", "Test", "পরীক্ষা", 10000L, "BDT", "1", "cash", "expense", 1L, null, null)
            fail("Expected injected voucher failure")
        } catch (_: android.database.SQLException) { }
        for (table in listOf("journal_entries", "journal_lines", "expense_vouchers")) {
            db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM $table").use {
                assertTrue(it.moveToFirst()); assertEquals(0, it.getInt(0))
            }
        }
    }
}
