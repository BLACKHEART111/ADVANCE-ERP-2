package com.pocketdev.advance_erp

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pocketdev.advance_erp.core.database.AdvanceErpDatabase
import com.pocketdev.advance_erp.core.util.DocumentNumberService
import com.pocketdev.advance_erp.data.repository.*
import com.pocketdev.advance_erp.domain.model.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Explicit opt-in. Never opens/replaces the production database or runs on the live package. */
@RunWith(AndroidJUnit4::class)
class TuktakMigrationRehearsalTest {
    @Test fun savedHistoryPassesRoomAndBothNativeLedgers() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val input = InstrumentationRegistry.getArguments().getString("migrationDbPath")
        assumeTrue("Requires private rehearsal fixture", input != null)
        val context = instrumentation.targetContext
        check(context.packageName.endsWith(".migrationqa")) { "Refusing migration fixture on live ERP" }
        val source = File(input!!).canonicalFile
        check(source.path.startsWith(context.filesDir.canonicalPath + "/"))
        val dbName = "isolated-tuktak-rehearsal.db"
        context.deleteDatabase(dbName) // QA-package-only disposable database.
        val db = Room.databaseBuilder(context, AdvanceErpDatabase::class.java, dbName)
            .createFromFile(source).build()
        try {
            val accounting = AccountingRepositoryImpl(db, db.accountingDao())
            val sales = SalesRepositoryImpl(db, db.salesDao(), db.customerPaymentDao(), db.salesReturnDao(),
                db.customerDao(), db.warehouseDao(), db.productDao(), db.stockBalanceDao(), db.stockMovementDao(),
                db.inventoryCostLayerDao(), accounting, DocumentNumberService(db))
            val reports = BusinessControlCenterRepositoryImpl(context, db, db.salesDao(), db.customerPaymentDao(),
                db.salesReturnDao(), db.purchaseOrderDao(), db.supplierPaymentDao(), db.expenseDao(),
                db.stockMovementDao(), db.stockTransferDao(), db.customerDao(), db.supplierDao(),
                db.warehouseDao(), db.productCategoryDao(), db.productDao(), db.productVariantDao(), db.businessProfileDao())
            val customers = db.customerDao().getLedgerCustomersSync().filter { it.customerCode.startsWith("TTK-") }
            assertEquals(8, customers.size)
            var eventCount = 0
            for (customer in customers) {
                val ledger = sales.getCustomerLedger(customer.id)
                assertEquals(customer.outstandingBalanceMinor, ledger.lastOrNull()?.runningBalanceMinor ?: 0L)
                assertTrue(ledger.zipWithNext().all { (a,b) -> a.date <= b.date })
                assertFalse(ledger.any { it.transactionType == "OPENING_BALANCE" })
                eventCount += ledger.size
                val report = reports.getLedgerReport(ControlCenterFilterState(
                    category = ReportCategory.CUSTOMER_LEDGER, selectedCustomerId = customer.id,
                    sortBy = SortOption.OLDEST_FIRST)).first()
                assertEquals(customer.outstandingBalanceMinor, report.statement!!.closingBalanceMinor)
                assertEquals(0L, report.legacyOpeningMinor)
                assertEquals(ledger.size, report.transactions.size)
            }
            assertEquals(96, eventCount)
            val payments = db.customerPaymentDao().getAllPaymentsSync().filter { it.paymentNumber.startsWith("TTK-") }
            assertEquals(62, payments.size)
            assertEquals(48080000L, payments.sumOf { it.amountMinor })
            for (payment in payments) {
                assertEquals(0L, payment.allocatedAmountMinor)
                assertTrue(db.customerPaymentDao().getAllocationsForPayment(payment.id).isEmpty())
            }
            db.openHelper.readableDatabase.query("""
                SELECT SUM(b.quantity) FROM stock_balances b JOIN products p ON p.id=b.productId
                WHERE TRIM(p.nameEn)='SHOES INSOLE RETAIL'
            """).use { cursor -> assertTrue(cursor.moveToFirst()); assertEquals(0L, cursor.getLong(0)) }
            db.openHelper.readableDatabase.query("PRAGMA foreign_key_check").use { assertEquals(0, it.count) }
            assertEquals(13, db.openHelper.readableDatabase.version)
        } finally {
            db.close()
            context.deleteDatabase(dbName)
        }
    }
}
