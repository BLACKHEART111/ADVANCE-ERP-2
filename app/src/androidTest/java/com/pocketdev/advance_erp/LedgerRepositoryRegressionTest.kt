package com.pocketdev.advance_erp

import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pocketdev.advance_erp.core.database.AdvanceErpDatabase
import com.pocketdev.advance_erp.core.util.DocumentNumberService
import com.pocketdev.advance_erp.data.entity.*
import com.pocketdev.advance_erp.data.repository.*
import com.pocketdev.advance_erp.domain.model.*
import com.pocketdev.advance_erp.domain.repository.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

/** Real SQLite/invalidation tests. Only an isolated in-memory database is used. */
@RunWith(AndroidJUnit4::class)
class LedgerRepositoryRegressionTest {
    private lateinit var db: AdvanceErpDatabase
    private lateinit var repo: BusinessControlCenterRepositoryImpl
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    @Before fun setup() = runBlocking {
        db=Room.inMemoryDatabaseBuilder(context,AdvanceErpDatabase::class.java).build()
        repo=BusinessControlCenterRepositoryImpl(context,db,db.salesDao(),db.customerPaymentDao(),db.salesReturnDao(),db.purchaseOrderDao(),db.supplierPaymentDao(),db.expenseDao(),db.stockMovementDao(),db.stockTransferDao(),db.customerDao(),db.supplierDao(),db.warehouseDao(),db.productCategoryDao(),db.productDao(),db.productVariantDao(),db.businessProfileDao())
        db.customerDao().insertCustomer(Customer(id="c",customerCode="C",name="Customer",phone="",address="",outstandingBalanceMinor=100000))
        db.warehouseDao().insertWarehouse(Warehouse("w","Warehouse","WH",""))
        db.salesDao().insertInvoice(SalesInvoice(id="i",invoiceNumber="INV",customerId="c",customerName="Customer",warehouseId="w",warehouseName="WH",isPosted=true,status="UNPAID",totalAmountMinor=100000,dueAmountMinor=100000,invoiceDate=1000,createdAt=1000))
    }
    @After fun close() {db.close()}
    private fun filter()=ControlCenterFilterState(category=ReportCategory.CUSTOMER_LEDGER,selectedCustomerId="c")
    @Test fun filteredRowsRetainFullAccountBalance() = runBlocking {
        db.withTransaction {
            db.customerPaymentDao().insertPayment(CustomerPayment("p","CP","c","Customer",40000,paymentDate=2000))
            db.customerDao().updateOutstandingBalance("c",60000)
        }
        val report=repo.getLedgerReport(filter().copy(searchQuery="CP")).first()
        assertEquals(1,report.transactions.size)
        assertEquals(60000L,report.statement!!.closingBalanceMinor)
        assertEquals(60000L,report.transactions.single().runningBalanceMinor)
    }
    @Test fun databaseWritesRefreshTheObservedLedgerWithoutChangingFilters() = runBlocking {
        val changes=Channel<LedgerReport>(Channel.UNLIMITED)
        val job=launch(Dispatchers.IO) { repo.getLedgerReport(filter()).collect {changes.send(it)} }
        try {
            assertEquals(100000L,withTimeout(10000){changes.receive()}.statement!!.closingBalanceMinor)
            db.withTransaction {
                db.customerPaymentDao().insertPayment(CustomerPayment("p","CP","c","Customer",40000,paymentDate=2000))
                db.customerDao().updateOutstandingBalance("c",60000)
            }
            val updated=withTimeout(10000){
                var candidate=changes.receive()
                while(candidate.statement?.closingBalanceMinor!=60000L) candidate=changes.receive()
                candidate
            }
            assertEquals(60000L,updated.statement!!.closingBalanceMinor)
            assertEquals(2,updated.transactions.size)
        } finally {job.cancelAndJoin();changes.close()}
    }
    @Test fun zeroClampedMasterBalanceDoesNotEraseAnAdvance() = runBlocking {
        db.withTransaction {
            db.customerPaymentDao().insertPayment(CustomerPayment("p","CP","c","Customer",150000,unallocatedCreditMinor=150000,paymentDate=2000))
            db.customerDao().updateOutstandingBalance("c",0)
        }
        val report=repo.getLedgerReport(filter()).first()
        assertEquals(-50000L,report.statement!!.closingBalanceMinor)
        assertEquals(0L,report.legacyOpeningMinor)
    }
    @Test fun collectionUpdatesMasterDueAndStatementTogether() = runBlocking {
        listOf("1010","1040","2030").forEach { code -> db.accountingDao().insertAccount(ChartOfAccount("coa-$code",code,code,code,if(code=="2030") "LIABILITY" else "ASSET",if(code=="2030") "CREDIT" else "DEBIT")) }
        val accounting=AccountingRepositoryImpl(db,db.accountingDao())
        val sales=SalesRepositoryImpl(db,db.salesDao(),db.customerPaymentDao(),db.salesReturnDao(),db.customerDao(),db.warehouseDao(),db.productDao(),db.stockBalanceDao(),db.stockMovementDao(),db.inventoryCostLayerDao(),accounting,DocumentNumberService(db))
        sales.recordCustomerPayment(CustomerPaymentDraft("c",40000,allocations=listOf(InvoiceAllocationDraft("i",40000))))
        assertEquals(60000L,db.customerDao().getCustomerById("c")!!.outstandingBalanceMinor)
        assertEquals(60000L,repo.getLedgerReport(filter()).first().statement!!.closingBalanceMinor)
        assertEquals(60000L,db.salesDao().getInvoiceById("i")!!.dueAmountMinor)
    }
    @Test fun journalFailureRollsBackPaymentInvoiceAndAllocations() = runBlocking {
        val real=AccountingRepositoryImpl(db,db.accountingDao())
        val failing=object: AccountingRepository by real {
            override suspend fun postJournalEntry(entryDate:Long,descEn:String,descBn:String,referenceType:String,referenceId:String?,lines:List<JournalLineDraft>):String = error("forced journal failure")
        }
        val sales=SalesRepositoryImpl(db,db.salesDao(),db.customerPaymentDao(),db.salesReturnDao(),db.customerDao(),db.warehouseDao(),db.productDao(),db.stockBalanceDao(),db.stockMovementDao(),db.inventoryCostLayerDao(),failing,DocumentNumberService(db))
        try { sales.recordCustomerPayment(CustomerPaymentDraft("c",40000,allocations=listOf(InvoiceAllocationDraft("i",40000))));fail("Expected rollback") }
        catch(error:IllegalStateException) {assertEquals("forced journal failure",error.message)}
        assertTrue(db.customerPaymentDao().getAllPaymentsSync().isEmpty())
        assertEquals(100000L,db.salesDao().getInvoiceById("i")!!.dueAmountMinor)
        assertTrue(db.customerPaymentDao().getAllocationsForInvoiceSync("i").isEmpty())
    }
}
