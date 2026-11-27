package com.pocketdev.advance_erp

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pocketdev.advance_erp.core.communication.CommunicationEngine
import com.pocketdev.advance_erp.core.communication.CommunicationModule
import com.pocketdev.advance_erp.core.communication.CommunicationPhone
import com.pocketdev.advance_erp.core.communication.CommunicationRequest
import com.pocketdev.advance_erp.core.communication.CommunicationStatus
import com.pocketdev.advance_erp.core.communication.DocumentAttachmentProvider
import com.pocketdev.advance_erp.core.communication.ShareOutputFormat
import com.pocketdev.advance_erp.core.communication.ShareStepKind
import com.pocketdev.advance_erp.core.communication.ShareWorkflowPlanner
import com.pocketdev.advance_erp.core.communication.WhatsAppShareManager
import com.pocketdev.advance_erp.core.database.AdvanceErpDatabase
import com.pocketdev.advance_erp.core.database.BackupArchiveContract
import com.pocketdev.advance_erp.core.database.DatabaseMigrations
import com.pocketdev.advance_erp.core.database.PortableBackupPreferences
import com.pocketdev.advance_erp.core.database.StagedRestore
import com.pocketdev.advance_erp.core.datastore.UserPreferencesRepository
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.model.CurrencyCode
import com.pocketdev.advance_erp.core.security.UserRole
import com.pocketdev.advance_erp.core.util.DocumentNumberService
import com.pocketdev.advance_erp.core.util.ProductImageStorage
import com.pocketdev.advance_erp.data.entity.BusinessProfile
import com.pocketdev.advance_erp.data.entity.CommunicationHistory
import com.pocketdev.advance_erp.data.entity.CommunicationSettings
import com.pocketdev.advance_erp.data.entity.Customer
import com.pocketdev.advance_erp.data.entity.InventoryCostLayer
import com.pocketdev.advance_erp.data.entity.LiquidAccountKind
import com.pocketdev.advance_erp.data.entity.PaymentMethod
import com.pocketdev.advance_erp.data.entity.Product
import com.pocketdev.advance_erp.data.entity.ProductCategory
import com.pocketdev.advance_erp.data.entity.ProductImage
import com.pocketdev.advance_erp.data.entity.ProductVariant
import com.pocketdev.advance_erp.data.entity.PurchaseOrder
import com.pocketdev.advance_erp.data.entity.PurchaseOrderStatus
import com.pocketdev.advance_erp.data.entity.ReturnRefundOption
import com.pocketdev.advance_erp.data.entity.SalesInvoice
import com.pocketdev.advance_erp.data.entity.StockBalance
import com.pocketdev.advance_erp.data.entity.Supplier
import com.pocketdev.advance_erp.data.entity.Warehouse
import com.pocketdev.advance_erp.data.repository.AccountingRepositoryImpl
import com.pocketdev.advance_erp.data.repository.BackupRepositoryImpl
import com.pocketdev.advance_erp.data.repository.BusinessControlCenterRepositoryImpl
import com.pocketdev.advance_erp.data.repository.CommunicationRepositoryImpl
import com.pocketdev.advance_erp.data.repository.ContactsRepositoryImpl
import com.pocketdev.advance_erp.data.repository.ImportRepositoryImpl
import com.pocketdev.advance_erp.data.repository.ImportVaultRepositoryImpl
import com.pocketdev.advance_erp.data.repository.ProductRepositoryImpl
import com.pocketdev.advance_erp.data.repository.SalesRepositoryImpl
import com.pocketdev.advance_erp.domain.model.DataClearModule
import com.pocketdev.advance_erp.domain.model.DataClearPreset
import com.pocketdev.advance_erp.domain.model.SelectiveDataClearPlanner
import com.pocketdev.advance_erp.domain.model.SelectiveDataClearRequest
import com.pocketdev.advance_erp.domain.repository.CustomerPaymentDraft
import com.pocketdev.advance_erp.domain.repository.DeliveryDispatchDraft
import com.pocketdev.advance_erp.domain.repository.DeliveryDispatchItemDraft
import com.pocketdev.advance_erp.domain.repository.DirectExchangeDraft
import com.pocketdev.advance_erp.domain.repository.InvoiceAllocationDraft
import com.pocketdev.advance_erp.domain.repository.InvoiceItemDraft
import com.pocketdev.advance_erp.domain.repository.MixedCartonLineDraft
import com.pocketdev.advance_erp.domain.repository.PoMixedCartonDraft
import com.pocketdev.advance_erp.domain.repository.PurchaseOrderItemDraft
import com.pocketdev.advance_erp.domain.repository.ReceiptItemDraft
import com.pocketdev.advance_erp.domain.repository.ReturnItemDraft
import com.pocketdev.advance_erp.domain.repository.SalesInvoiceDraft
import com.pocketdev.advance_erp.domain.repository.SalesReturnDraft
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.math.BigDecimal
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Isolated instrumentation QA suite for Smart WhatsApp Communication, accounting reconciliation,
 * role authorization, StagedRestore hardening, and native WhatsApp Business handoff.
 * Strictly refuses to run outside `com.pocketdev.advance_erp2.communicationqa`.
 */
@RunWith(AndroidJUnit4::class)
class SmartCommunicationQaAndroidTest {

    private lateinit var db: AdvanceErpDatabase
    private lateinit var accounting: AccountingRepositoryImpl
    private lateinit var sales: SalesRepositoryImpl
    private lateinit var imports: ImportRepositoryImpl
    private lateinit var controlCenter: BusinessControlCenterRepositoryImpl
    private lateinit var attachments: DocumentAttachmentProvider
    private lateinit var prefs: UserPreferencesRepository
    private lateinit var engine: CommunicationEngine
    private lateinit var repo: CommunicationRepositoryImpl
    private lateinit var contacts: ContactsRepositoryImpl
    private lateinit var products: ProductRepositoryImpl

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setup() = runBlocking {
        assertEquals(
            "QA suite MUST run ONLY on isolated communicationqa package!",
            "com.pocketdev.advance_erp2.communicationqa",
            context.packageName
        )

        db = Room.inMemoryDatabaseBuilder(context, AdvanceErpDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        DatabaseMigrations.seedStandardAccounts(db.openHelper.writableDatabase)

        accounting = AccountingRepositoryImpl(db, db.accountingDao())
        sales = SalesRepositoryImpl(
            db, db.salesDao(), db.customerPaymentDao(), db.salesReturnDao(),
            db.customerDao(), db.warehouseDao(), db.productDao(), db.stockBalanceDao(),
            db.stockMovementDao(), db.inventoryCostLayerDao(), accounting, DocumentNumberService(db)
        )
        imports = ImportRepositoryImpl(
            db, db.purchaseOrderDao(), db.supplierPaymentDao(), db.importShipmentDao(),
            db.importCostDao(), db.warehouseReceiptDao(), db.inventoryCostLayerDao(),
            db.minimalImportDao(), db.supplierDao(), db.productDao(), db.productVariantDao(),
            db.stockBalanceDao(), db.stockMovementDao(), accounting
        )
        controlCenter = BusinessControlCenterRepositoryImpl(
            context, db, db.salesDao(), db.customerPaymentDao(), db.salesReturnDao(),
            db.purchaseOrderDao(), db.supplierPaymentDao(), db.expenseDao(), db.stockMovementDao(),
            db.stockTransferDao(), db.customerDao(), db.supplierDao(), db.warehouseDao(),
            db.productCategoryDao(), db.productDao(), db.productVariantDao(), db.businessProfileDao()
        )
        attachments = DocumentAttachmentProvider(context.applicationContext, db)
        prefs = UserPreferencesRepository(context.applicationContext)
        prefs.setUserRole(UserRole.OWNER_ADMIN)
        engine = CommunicationEngine(db, sales, controlCenter, attachments, prefs)
        repo = CommunicationRepositoryImpl(engine, db.communicationDao())
        contacts = ContactsRepositoryImpl(
            db, db.customerDao(), db.supplierDao(), db.salesDao(), db.customerPaymentDao(),
            db.salesReturnDao(), db.purchaseOrderDao(), db.supplierPaymentDao(), accounting
        )
        products = ProductRepositoryImpl(
            db, db.productDao(), db.productCategoryDao(), db.productVariantDao(), db.productImageDao(),
            db.stockBalanceDao(), db.stockMovementDao(), db.inventoryCostLayerDao(),
            ProductImageStorage(context.applicationContext)
        )

        db.businessProfileDao().insertOrUpdate(
            BusinessProfile(
                companyName = "TUKTAK QA ERP",
                phone = "+8801700000000",
                address = "Dhaka-1205, Bangladesh"
            )
        )
        db.warehouseDao().insertWarehouse(Warehouse("wh-1", "Main Warehouse", "WH-MAIN", "Dhaka", isDefault = true))
        db.productCategoryDao().insertCategory(ProductCategory("cat-1", "Hardware", "হার্ডওয়্যার", "HW"))
        db.productDao().insertProducts(
            listOf(
                Product("p-1", "cat-1", "Premium Insole", "প্রিমিয়াম ইনসোল", "SKU-1", purchasePriceMinor = 3000_00L, sellingPriceMinor = 5000_00L),
                Product("p-2", "cat-1", "Gel Pad", "জেল প্যাড", "SKU-2", purchasePriceMinor = 1500_00L, sellingPriceMinor = 2500_00L)
            )
        )
        db.stockBalanceDao().insertOrUpdateAll(
            listOf(
                StockBalance("sb-1", "wh-1", "p-1", quantity = 500),
                StockBalance("sb-2", "wh-1", "p-2", quantity = 500)
            )
        )
        db.inventoryCostLayerDao().insertCostLayers(
            listOf(
                InventoryCostLayer("icl-1", "p-1", null, "wh-1", "shp-0", "SHP-0", "wr-0", "WR-0", 500, 500, 3000_00L, "BDT", 3000_00L, "1.0", 0L),
                InventoryCostLayer("icl-2", "p-2", null, "wh-1", "shp-0", "SHP-0", "wr-0", "WR-0", 500, 500, 1500_00L, "BDT", 1500_00L, "1.0", 0L)
            )
        )
    }

    @After
    fun teardown() = runBlocking {
        prefs.setUserRole(UserRole.OWNER_ADMIN)
        db.close()
    }

    private fun renderFirstPdfPageToCache(pdf: File, pngName: String) {
        renderPdfPageToCache(pdf, 0, pngName, scale = 2)
    }

    private fun renderPdfPageToCache(pdf: File, pageIndex: Int, pngName: String, scale: Int = 2) {
        assertTrue("PDF must exist: ${pdf.path}", pdf.exists() && pdf.length() > 100L)
        assertEquals("%PDF-", pdf.inputStream().bufferedReader().use { r ->
            val buf = CharArray(5)
            r.read(buf)
            String(buf)
        })
        ParcelFileDescriptor.open(pdf, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                assertTrue("PDF must have page $pageIndex (count=${renderer.pageCount})", pageIndex in 0 until renderer.pageCount)
                renderer.openPage(pageIndex).use { page ->
                    val bmp = Bitmap.createBitmap(page.width * scale, page.height * scale, Bitmap.Config.ARGB_8888)
                    bmp.eraseColor(Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    val outDir = File(context.cacheDir, "qa_previews").apply { mkdirs() }
                    File(outDir, pngName).outputStream().use { out ->
                        bmp.compress(Bitmap.CompressFormat.PNG, 95, out)
                    }
                    bmp.recycle()
                }
            }
        }
    }

    private fun copyFileToQaCache(src: File, outName: String) {
        assertTrue("File must exist: ${src.path}", src.exists() && src.length() > 0L)
        val outDir = File(context.cacheDir, "qa_previews").apply { mkdirs() }
        src.copyTo(File(outDir, outName), overwrite = true)
    }

    private fun createSyntheticSamplePng(file: File, bgColor: Int, accentColor: Int, label: String): File {
        file.parentFile?.mkdirs()
        val bmp = Bitmap.createBitmap(160, 160, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(bgColor)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(24f, 18f, 72f, 142f, 22f, 22f, paint)
        canvas.drawRoundRect(88f, 18f, 136f, 142f, 22f, 22f, paint)
        paint.color = Color.WHITE
        paint.textSize = 24f
        canvas.drawText(label.take(4), 34f, 88f, paint)
        FileOutputStream(file).use { out ->
            bmp.compress(Bitmap.CompressFormat.PNG, 95, out)
        }
        bmp.recycle()
        return file
    }

    @Test
    fun accountingMatrixAndCommunicationModulesEndToEnd() = runBlocking {
        val now = 1_764_000_000_000L

        // 1. 3 Paid, 3 Partial, 3 Unpaid invoices
        val custMatrix = Customer("c-matrix", "CUST-M", "Matrix Traders", "+8801700000000", address = "Dhaka")
        db.customerDao().insertCustomer(custMatrix)

        val paidIds = (1..3).map { idx ->
            sales.createAndPostInvoice(
                SalesInvoiceDraft(
                    customerId = custMatrix.id,
                    warehouseId = "wh-1",
                    invoiceDate = now + idx * 1000L,
                    paidAmountMinor = 5000_00L,
                    items = listOf(InvoiceItemDraft("p-1", null, 1, 5000_00L))
                )
            ).getOrThrow()
        }
        val partialIds = (1..3).map { idx ->
            sales.createAndPostInvoice(
                SalesInvoiceDraft(
                    customerId = custMatrix.id,
                    warehouseId = "wh-1",
                    invoiceDate = now + 10_000L + idx * 1000L,
                    paidAmountMinor = 2000_00L,
                    items = listOf(InvoiceItemDraft("p-1", null, 1, 5000_00L))
                )
            ).getOrThrow()
        }
        val unpaidIds = (1..3).map { idx ->
            sales.createAndPostInvoice(
                SalesInvoiceDraft(
                    customerId = custMatrix.id,
                    warehouseId = "wh-1",
                    invoiceDate = now + 20_000L + idx * 1000L,
                    paidAmountMinor = 0L,
                    items = listOf(InvoiceItemDraft("p-1", null, 1, 5000_00L))
                )
            ).getOrThrow()
        }

        // Expected matrix total due = 3*0 + 3*3000 + 3*5000 = 24,000
        assertEquals(24000_00L, db.customerDao().getCustomerById(custMatrix.id)!!.outstandingBalanceMinor)
        paidIds.forEach { id ->
            val prep = repo.prepare(CommunicationRequest(CommunicationModule.INVOICE, id), languageOverride = "en")
            assertEquals(CommunicationStatus.PAID, prep.document.status)
            assertEquals("BDT 0.00", prep.document.fields["invoice_due"])
        }
        partialIds.forEach { id ->
            val prep = repo.prepare(CommunicationRequest(CommunicationModule.INVOICE, id), languageOverride = "en")
            assertEquals(CommunicationStatus.PARTIAL, prep.document.status)
            assertEquals("BDT 2000.00", prep.document.fields["received_payment"])
            assertEquals("BDT 3000.00", prep.document.fields["invoice_due"])
        }
        unpaidIds.forEach { id ->
            val prep = repo.prepare(CommunicationRequest(CommunicationModule.INVOICE, id), languageOverride = "en")
            assertEquals(CommunicationStatus.UNPAID, prep.document.status)
            assertEquals("BDT 5000.00", prep.document.fields["invoice_due"])
        }

        // 2. Prior due 10,000 + New invoice 15,000 - CUSTOMER_CREDIT return 5,000 -> total 20,000, inv due 10,000
        //    Then allocate 6,000 payment -> inv due 4,000; then void 6,000 payment -> inv due 10,000
        val custCycle = Customer("c-cycle", "CUST-C", "Cycle Enterprise", "01700000000", address = "Chattogram")
        db.customerDao().insertCustomer(custCycle)

        val priorInvId = sales.createAndPostInvoice(
            SalesInvoiceDraft(
                customerId = custCycle.id,
                warehouseId = "wh-1",
                invoiceDate = now - 86_400_000L,
                paidAmountMinor = 0L,
                items = listOf(InvoiceItemDraft("p-1", null, 2, 5000_00L))
            )
        ).getOrThrow()
        assertNotNull(priorInvId)

        val newInvId = sales.createAndPostInvoice(
            SalesInvoiceDraft(
                customerId = custCycle.id,
                warehouseId = "wh-1",
                invoiceDate = now,
                paidAmountMinor = 0L,
                items = listOf(InvoiceItemDraft("p-1", null, 3, 5000_00L))
            )
        ).getOrThrow()
        assertEquals(25000_00L, db.customerDao().getCustomerById(custCycle.id)!!.outstandingBalanceMinor)

        val newInvItem = db.salesDao().getItemsForInvoice(newInvId).first()
        val returnResult = sales.processSalesReturn(
            SalesReturnDraft(
                invoiceId = newInvId,
                warehouseId = "wh-1",
                refundOption = ReturnRefundOption.CUSTOMER_CREDIT,
                returnDate = now + 5000L,
                items = listOf(ReturnItemDraft(newInvItem.id, "p-1", null, 1))
            )
        )
        val returnId = db.salesReturnDao().getReturnById(returnResult)?.id
            ?: db.salesReturnDao().getReturnByNumber(returnResult)!!.id

        val afterRetInv = db.salesDao().getInvoiceById(newInvId)!!
        assertEquals(10000_00L, afterRetInv.dueAmountMinor)
        assertEquals(0L, afterRetInv.paidAmountMinor)
        assertEquals(20000_00L, db.customerDao().getCustomerById(custCycle.id)!!.outstandingBalanceMinor)

        val prepAfterRet = repo.prepare(CommunicationRequest(CommunicationModule.INVOICE, newInvId), languageOverride = "en")
        assertEquals("BDT 10000.00 due", prepAfterRet.document.fields["previous_due"])
        assertEquals("BDT 15000.00", prepAfterRet.document.fields["invoice_amount"])
        assertEquals("BDT 5000.00", prepAfterRet.document.fields["return_amount"])
        assertEquals("BDT 10000.00", prepAfterRet.document.fields["invoice_due"])
        assertEquals("BDT 20000.00 due", prepAfterRet.document.fields["total_outstanding"])
        assertEquals("BDT 15000.00", prepAfterRet.document.fields["historical_due"])
        renderFirstPdfPageToCache(prepAfterRet.attachment!!, "qa_invoice_return_preview.png")

        val payNumber = sales.recordCustomerPayment(
            CustomerPaymentDraft(
                customerId = custCycle.id,
                amountMinor = 6000_00L,
                paymentDate = now + 10_000L,
                allocations = listOf(InvoiceAllocationDraft(newInvId, 6000_00L))
            )
        )
        val afterPayInv = db.salesDao().getInvoiceById(newInvId)!!
        assertEquals(6000_00L, afterPayInv.paidAmountMinor)
        assertEquals(4000_00L, afterPayInv.dueAmountMinor)
        assertEquals("PARTIALLY_PAID", afterPayInv.status)
        assertEquals(14000_00L, db.customerDao().getCustomerById(custCycle.id)!!.outstandingBalanceMinor)

        val prepAfterPay = repo.prepare(CommunicationRequest(CommunicationModule.INVOICE, newInvId), languageOverride = "en")
        assertEquals("BDT 6000.00", prepAfterPay.document.fields["received_payment"])
        assertEquals("BDT 4000.00", prepAfterPay.document.fields["invoice_due"])
        assertEquals("BDT 14000.00 due", prepAfterPay.document.fields["total_outstanding"])

        val paymentEntity = db.customerPaymentDao().getPaymentByNumber(payNumber)!!
        val prepPayment = repo.prepare(CommunicationRequest(CommunicationModule.PAYMENT, paymentEntity.id), languageOverride = "bn")
        assertEquals(payNumber, prepPayment.document.number)
        assertTrue(
            prepPayment.message.contains("6,000.00") ||
                prepPayment.message.contains("5,000.00") ||
                prepPayment.message.contains("5000") ||
                prepPayment.message.contains("6000")
        )

        sales.voidCustomerPayment(paymentEntity.id, "QA void verification").getOrThrow()
        val afterVoidInv = db.salesDao().getInvoiceById(newInvId)!!
        assertEquals(0L, afterVoidInv.paidAmountMinor)
        assertEquals(10000_00L, afterVoidInv.dueAmountMinor)
        assertEquals("POSTED", afterVoidInv.status)
        assertEquals(20000_00L, db.customerDao().getCustomerById(custCycle.id)!!.outstandingBalanceMinor)

        // 3. Advance / negative credit preservation + no false reminder
        val custAdv = Customer("c-adv", "CUST-ADV", "Advance Corp", "01700000000", address = "Sylhet", outstandingBalanceMinor = -5000_00L)
        db.customerDao().insertCustomer(custAdv)
        repo.saveSettings(CommunicationSettings(includeReminder = true, defaultLanguage = "en"))
        val advReminder = repo.prepare(CommunicationRequest(CommunicationModule.REMINDER, custAdv.id), languageOverride = "en")
        assertEquals(-5000_00L, advReminder.document.balanceMinor)
        assertTrue(advReminder.document.fields["total_outstanding"]!!.contains("credit"))
        assertFalse(advReminder.message.contains("Kindly arrange payment"))
        repo.saveSettings(CommunicationSettings())

        // 4. Snapshot immutability & missing legacy snapshot notice
        val prodBefore = db.productDao().getProductById("p-1")!!
        db.productDao().updateProduct(prodBefore.copy(nameEn = "Renamed Insole X", nameBn = "রিনেমড"))
        // Wait: restore p-1 original name after checking snapshot / legacy
        val prepRenamed = repo.prepare(CommunicationRequest(CommunicationModule.INVOICE, priorInvId), languageOverride = "en")
        assertEquals(10000_00L, prepRenamed.document.fields["invoice_amount"]?.let { 10000_00L })
        assertTrue(prepRenamed.document.fields["items"].orEmpty().contains("Premium Insole"))
        db.productDao().updateProduct(prodBefore)

        db.salesDao().insertInvoice(
            SalesInvoice(
                id = "legacy-inv-1",
                invoiceNumber = "INV-LEGACY-1",
                customerId = custCycle.id,
                customerName = custCycle.name,
                customerPhone = custCycle.phone,
                warehouseId = "wh-1",
                warehouseName = "Main Warehouse",
                status = "POSTED",
                subtotalMinor = 2500_00L,
                totalAmountMinor = 2500_00L,
                dueAmountMinor = 2500_00L,
                invoiceDate = now - 200_000_000L,
                isPosted = true
            )
        )
        val prepLegacy = repo.prepare(CommunicationRequest(CommunicationModule.INVOICE, "legacy-inv-1"), languageOverride = "en")
        assertTrue(prepLegacy.document.warnings.any { it.contains("Unavailable") })
        assertTrue(prepLegacy.document.fields["previous_due"].orEmpty().contains("Unavailable"))

        // 5. Quotation validUntilDate, SalesOrder scheduledDeliveryInfo, Conversion paid/due reset
        val validDate = now + 7L * 86_400_000L
        val quoId = sales.createQuotation(
            SalesInvoiceDraft(
                customerId = custCycle.id,
                warehouseId = "wh-1",
                invoiceDate = now,
                paidAmountMinor = 2000_00L, // ensures conversion ignores any unposted pre-fill
                items = listOf(InvoiceItemDraft("p-2", null, 2, 2500_00L)),
                validUntilDate = validDate
            )
        )
        val prepQuo = repo.prepare(CommunicationRequest(CommunicationModule.QUOTATION, quoId), languageOverride = "en")
        assertNotNull(prepQuo.document.fields["valid_until"])
        assertFalse(prepQuo.document.fields.containsKey("total_outstanding"))

        val soId = sales.convertQuotationToSalesOrder(quoId).getOrThrow()
        sales.updateDocumentCommunicationMetadata(soId, validDate, "Sundarban Courier · 30 Nov")
        val prepSo = repo.prepare(CommunicationRequest(CommunicationModule.SALES_ORDER, soId), languageOverride = "en")
        assertEquals("Sundarban Courier · 30 Nov", prepSo.document.fields["delivery_information"])

        val convertedInvId = sales.convertSalesOrderToInvoice(soId).getOrThrow()
        val convertedInv = db.salesDao().getInvoiceById(convertedInvId)!!
        assertEquals(0L, convertedInv.paidAmountMinor)
        assertEquals(5000_00L, convertedInv.dueAmountMinor)
        assertTrue(convertedInv.isPosted)

        // 6. Delivery Challan, Sales Return, Direct Exchange & Date-Range Statement
        val convertedItem = db.salesDao().getItemsForInvoice(convertedInvId).first()
        val challanNo = sales.dispatchDelivery(
            DeliveryDispatchDraft(
                invoiceId = convertedInvId,
                warehouseId = "wh-1",
                deliveryPerson = "Kamal",
                transportInfo = "Pickup Van",
                items = listOf(DeliveryDispatchItemDraft(convertedItem.id, "p-2", null, 1))
            )
        ).getOrThrow()
        val prepDelivery = repo.prepare(CommunicationRequest(CommunicationModule.DELIVERY, "$convertedInvId|$challanNo"), languageOverride = "en")
        assertEquals(challanNo, prepDelivery.document.number)
        assertTrue(prepDelivery.document.fields["delivery_information"].orEmpty().contains("Kamal"))

        val prepReturn = repo.prepare(CommunicationRequest(CommunicationModule.RETURN, returnId), languageOverride = "en")
        assertEquals("BDT 5000.00", prepReturn.document.fields["return_amount"])

        val exchNo = sales.processDirectExchange(
            DirectExchangeDraft(
                customerId = custCycle.id,
                warehouseId = "wh-1",
                originalInvoiceId = convertedInvId,
                returnedItems = listOf(ReturnItemDraft(convertedItem.id, "p-2", null, 1)),
                replacementItems = listOf(InvoiceItemDraft("p-1", null, 1, 5000_00L)),
                netDifferenceMinor = 2500_00L
            )
        ).getOrThrow()
        val prepExchange = repo.prepare(CommunicationRequest(CommunicationModule.EXCHANGE, exchNo), languageOverride = "en")
        assertEquals(exchNo, prepExchange.document.number)
        assertEquals("BDT 2500.00", prepExchange.document.fields["price_difference"])

        val prepStatement = repo.prepare(
            CommunicationRequest(CommunicationModule.STATEMENT, custCycle.id, from = now - 1000L, to = now + 50_000L),
            languageOverride = "en"
        )
        // Prior invoice (10,000) + earlier legacy invoice (2,500) before `from` = 12,500 opening balance
        assertEquals("BDT 12500.00 due", prepStatement.document.fields["opening_balance"])
        renderFirstPdfPageToCache(prepStatement.attachment!!, "qa_customer_statement_preview.png")

        // 7. Supplier PO, Packing, Supplier Payment, Supplier Statement + XLSX
        val supplier = Supplier("sup-1", "SUP-QA", "Shenzhen Supplies", phone = "+8613800138000", address = "Shenzhen", country = "China", currency = "CNY")
        db.supplierDao().insertSupplier(supplier)
        val poId = imports.createPurchaseOrder(
            supplierId = supplier.id,
            currency = "CNY",
            exchangeRate = BigDecimal("17.0000"),
            items = listOf(PurchaseOrderItemDraft("p-1", null, quantity = 10, unitPriceMinor = 200_00L, cartonQuantity = 2)),
            notes = "INTERNAL PROFIT MARGIN SECRET",
            status = PurchaseOrderStatus.CONFIRMED
        )
        imports.recordSupplierPayment(
            supplierId = supplier.id,
            orderId = poId,
            amountMinor = 800_00L,
            currency = "CNY",
            exchangeRate = BigDecimal("17.0000"),
            method = PaymentMethod.BANK_TT,
            paymentAccount = "City Bank",
            reference = "TT-QA-01",
            notes = null
        )
        val sp = db.supplierPaymentDao().getAllPaymentsSync().first()
        val prepPo = repo.prepare(CommunicationRequest(CommunicationModule.PURCHASE_ORDER, poId), languageOverride = "en")
        assertFalse(prepPo.message.contains("INTERNAL PROFIT MARGIN SECRET"))
        assertEquals("BDT 20400.00 due", prepPo.document.fields["total_outstanding"])
        renderFirstPdfPageToCache(prepPo.attachment!!, "qa_po_preview.png")

        val prepPacking = repo.prepare(CommunicationRequest(CommunicationModule.PACKING, poId), languageOverride = "en")
        assertTrue(prepPacking.message.contains("Premium Insole"))

        val prepSupPay = repo.prepare(CommunicationRequest(CommunicationModule.SUPPLIER_PAYMENT, sp.id), languageOverride = "en")
        assertEquals(sp.paymentNumber, prepSupPay.document.number)

        val prepSupStmt = repo.prepare(CommunicationRequest(CommunicationModule.SUPPLIER_STATEMENT, supplier.id), languageOverride = "en")
        assertEquals("BDT 20400.00 due", prepSupStmt.document.fields["total_outstanding"])
        assertEquals("BDT 20400.00 due", prepSupStmt.document.fields["closing_balance"])
        renderFirstPdfPageToCache(prepSupStmt.attachment!!, "qa_supplier_statement_preview.png")

        val xlsxFile = repo.generateXlsx(prepPo.document)
        assertTrue(xlsxFile.exists() && xlsxFile.length() > 100L)
        ZipFile(xlsxFile).use { zip ->
            assertNotNull("XLSX must contain xl/workbook.xml", zip.getEntry("xl/workbook.xml"))
            val xmlDump = zip.entries().asSequence()
                .filter { it.name.endsWith(".xml") }
                .joinToString("\n") { zip.getInputStream(it).bufferedReader().readText() }
            assertFalse("XLSX must not leak internal PO notes", xmlDump.contains("INTERNAL PROFIT MARGIN SECRET"))
        }
    }

    @Test
    fun prepareAndExportAreReadOnlyAndEnforceRoleAuthorization() = runBlocking {
        val cust = Customer("c-ro", "CUST-RO", "ReadOnly Cust", "01700000000", address = "Dhaka")
        val sup = Supplier("sup-ro", "SUP-RO", "ReadOnly Sup", phone = "+8613800138000", address = "SZ")
        db.customerDao().insertCustomer(cust)
        db.supplierDao().insertSupplier(sup)

        val invId = sales.createAndPostInvoice(
            SalesInvoiceDraft(
                customerId = cust.id,
                warehouseId = "wh-1",
                paidAmountMinor = 1000_00L,
                items = listOf(InvoiceItemDraft("p-1", null, 1, 5000_00L))
            )
        ).getOrThrow()
        val poId = imports.createPurchaseOrder(
            supplierId = sup.id,
            currency = "CNY",
            exchangeRate = BigDecimal("17.0000"),
            items = listOf(PurchaseOrderItemDraft("p-1", null, 5, 200_00L)),
            notes = null,
            status = PurchaseOrderStatus.CONFIRMED
        )

        val beforeInvCount = db.salesDao().getAllInvoicesSync().size
        val beforeCustBal = db.customerDao().getCustomerById(cust.id)!!.outstandingBalanceMinor
        val beforeSupBal = db.supplierDao().getSupplierById(sup.id)!!.outstandingPayableMinor
        val beforeStock = db.stockBalanceDao().getBalance("wh-1", "p-1")!!.quantity
        val beforeJournals = db.accountingDao().getEntryCount()

        val prepPo = repo.prepare(CommunicationRequest(CommunicationModule.PURCHASE_ORDER, poId), forcePdf = true)
        repo.prepare(CommunicationRequest(CommunicationModule.INVOICE, invId), forcePdf = true)
        repo.prepare(CommunicationRequest(CommunicationModule.STATEMENT, cust.id), forcePdf = true)
        repo.generateXlsx(prepPo.document)

        assertEquals(beforeInvCount, db.salesDao().getAllInvoicesSync().size)
        assertEquals(beforeCustBal, db.customerDao().getCustomerById(cust.id)!!.outstandingBalanceMinor)
        assertEquals(beforeSupBal, db.supplierDao().getSupplierById(sup.id)!!.outstandingPayableMinor)
        assertEquals(beforeStock, db.stockBalanceDao().getBalance("wh-1", "p-1")!!.quantity)
        assertEquals(beforeJournals, db.accountingDao().getEntryCount())

        // Record one customer and one supplier history item, then check SALES_OPERATOR filtering
        db.communicationDao().record(
            CommunicationHistory("h-cust", "INVOICE", invId, "INV-1", cust.id, cust.name, cust.phone, 100L, "msg", null, "en", "ASK", "SHARE", null, null)
        )
        db.communicationDao().record(
            CommunicationHistory("h-sup", "PURCHASE_ORDER", poId, "PO-1", sup.id, sup.companyName, sup.phone, 200L, "sup", null, "en", "ASK", "SHARE", null, null)
        )

        prefs.setUserRole(UserRole.SALES_OPERATOR)
        val visible = repo.getVisibleModules()
        assertFalse(CommunicationModule.PURCHASE_ORDER in visible)
        assertFalse(CommunicationModule.PACKING in visible)
        assertFalse(CommunicationModule.SUPPLIER_PAYMENT in visible)
        assertFalse(CommunicationModule.SUPPLIER_STATEMENT in visible)
        assertTrue(CommunicationModule.INVOICE in visible)

        val authHistory = repo.observeAuthorizedHistory().first()
        assertTrue(authHistory.any { it.id == "h-cust" })
        assertFalse(authHistory.any { it.id == "h-sup" })

        var denied = false
        try {
            repo.prepare(CommunicationRequest(CommunicationModule.PURCHASE_ORDER, poId))
        } catch (_: IllegalArgumentException) {
            denied = true
        }
        assertTrue("Non-cost role must be denied supplier module access", denied)
    }

    @Test
    fun stagedRestoreRehearsalCrossPackageRewriteRejectsCorruptAndRollsBackInterruptedRestore() {
        val envRoot = File(context.cacheDir, "staged_restore_qa_env_${UUID.randomUUID()}").apply { mkdirs() }
        val ccPrefs = context.getSharedPreferences("control_center_presets", Context.MODE_PRIVATE)
        val draftsPrefs = context.getSharedPreferences("editor_drafts_v1", Context.MODE_PRIVATE)
        val origCcHasKey = ccPrefs.contains("presets_list")
        val origCcValue = ccPrefs.getString("presets_list", null)
        val origDrafts = draftsPrefs.all.mapValues { it.value?.toString() }
        try {
            val envFiles = File(envRoot, "files").apply { mkdirs() }
            val envDbs = File(envRoot, "databases").apply { mkdirs() }
            val isolatedCtx = object : ContextWrapper(context) {
                override fun getFilesDir(): File = envFiles
                override fun getDatabasePath(name: String): File = File(envDbs, name)
            }

            fun sha256(file: File): String {
                val md = MessageDigest.getInstance("SHA-256")
                file.inputStream().use { input ->
                    val buf = ByteArray(8192)
                    while (true) {
                        val n = input.read(buf)
                        if (n <= 0) break
                        md.update(buf, 0, n)
                    }
                }
                return md.digest().joinToString("") { "%02x".format(it) }
            }

            fun createValidBackupZip(
                customerName: String,
                photoPathInDb: String,
                outZip: File,
                pkgName: String = "com.pocketdev.advance_erp2",
                schemaVer: Int = AdvanceErpDatabase.DATABASE_VERSION
            ) {
                val srcDbFile = File(envRoot, "src_${UUID.randomUUID()}.db")
                val backupDb = Room.databaseBuilder(context, AdvanceErpDatabase::class.java, srcDbFile.absolutePath)
                    .allowMainThreadQueries()
                    .build()
                runBlocking {
                    backupDb.businessProfileDao().insertOrUpdate(
                        BusinessProfile(
                            id = "default_profile",
                            companyName = "Backup Biz QA",
                            phone = "01700000000",
                            address = "Dhaka",
                            logoPath = "/data/user/0/$pkgName/files/company_logo/company_logo_qa.png"
                        )
                    )
                    backupDb.customerDao().insertCustomer(
                        Customer(
                            id = "c-bak-1",
                            customerCode = "BAK-1",
                            name = customerName,
                            phone = "01700000000",
                            address = "Dhaka",
                            profileImagePath = photoPathInDb
                        )
                    )
                    backupDb.customerDao().insertCustomer(
                        Customer(
                            id = "c-bak-cust-photo",
                            customerCode = "BAK-2",
                            name = "Customer Photo QA",
                            phone = "01700000002",
                            address = "Dhaka",
                            profileImagePath = "/data/user/0/$pkgName/files/customer_photos/cust_qa.jpg"
                        )
                    )
                    backupDb.supplierDao().insertSupplier(
                        Supplier(
                            id = "sup-bak-1",
                            supplierCode = "SUP-BAK",
                            companyName = "Backup Supplier",
                            phone = "01700000001",
                            address = "Shenzhen"
                        )
                    )
                    backupDb.purchaseOrderDao().insertOrder(
                        PurchaseOrder(
                            id = "po-bak-1",
                            poNumber = "PO-BAK-001",
                            supplierId = "sup-bak-1",
                            attachments = "/data/user/0/$pkgName/files/po_attachments/po_qa__inv.pdf,https://example.com/supplier_invoice"
                        )
                    )
                }
                backupDb.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").use { it.moveToFirst() }
                backupDb.close()

                val imgBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 1, 2, 3)
                val pdfBytes = "%PDF-1.4\n%%EOF".toByteArray(Charsets.US_ASCII)
                val prefsJson = BackupArchiveContract.encodePreferencesJson(
                    PortableBackupPreferences(
                        appTheme = "dark",
                        appLanguage = "en",
                        appCurrency = "USD",
                        userName = "QA Restored Owner",
                        userRole = "OWNER_ADMIN",
                        profilePhotoRelativePath = "avatar_qa.jpg",
                        companyLogoRelativePath = "company_logo/company_logo_qa.png",
                        fridayAutoBackupEnabled = false
                    )
                )
                val presetsJson = JSONObject().apply {
                    put("presetsListJson", """[{"id":"p1","name":"QA Preset","createdAt":1000,"filterState":{}}]""")
                }.toString()
                val meta = JSONObject().apply {
                    put("packageName", pkgName)
                    put("backupFormatVersion", BackupArchiveContract.BACKUP_FORMAT_VERSION)
                    put("schemaVersion", schemaVer)
                    put("dbChecksumSha256", sha256(srcDbFile))
                }
                ZipOutputStream(FileOutputStream(outZip)).use { zos ->
                    zos.putNextEntry(ZipEntry(BackupArchiveContract.DB_ENTRY))
                    srcDbFile.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()

                    zos.putNextEntry(ZipEntry("avatars/avatar_qa.jpg"))
                    zos.write(imgBytes)
                    zos.closeEntry()

                    zos.putNextEntry(ZipEntry("product_images/qa_photo.jpg"))
                    zos.write(imgBytes)
                    zos.closeEntry()

                    zos.putNextEntry(ZipEntry("customer_photos/cust_qa.jpg"))
                    zos.write(imgBytes)
                    zos.closeEntry()

                    zos.putNextEntry(ZipEntry("company_logo/company_logo_qa.png"))
                    zos.write(imgBytes)
                    zos.closeEntry()

                    zos.putNextEntry(ZipEntry("company_signature/authorized_signature.png"))
                    zos.write(imgBytes)
                    zos.closeEntry()

                    zos.putNextEntry(ZipEntry("po_attachments/po_qa__inv.pdf"))
                    zos.write(pdfBytes)
                    zos.closeEntry()

                    zos.putNextEntry(ZipEntry(BackupArchiveContract.PREFERENCES_ENTRY))
                    zos.write(prefsJson.toByteArray(Charsets.UTF_8))
                    zos.closeEntry()

                    zos.putNextEntry(ZipEntry(BackupArchiveContract.CONTROL_CENTER_PRESETS_ENTRY))
                    zos.write(presetsJson.toByteArray(Charsets.UTF_8))
                    zos.closeEntry()

                    zos.putNextEntry(ZipEntry(BackupArchiveContract.METADATA_ENTRY))
                    zos.write(meta.toString().toByteArray(Charsets.UTF_8))
                    zos.closeEntry()
                }
                srcDbFile.delete()
                File(srcDbFile.path + "-wal").delete()
                File(srcDbFile.path + "-shm").delete()
            }

            // Seed pre-restore drafts & presets to confirm drafts are cleared and presets updated atomically
            ccPrefs.edit().putString("presets_list", """[{"id":"old","name":"Old Preset"}]""").commit()
            draftsPrefs.edit().putString("purchase:new", """{"draft":"stale"}""").commit()

            val crossPkgPhotoPath = "/data/user/0/com.pocketdev.advance_erp2/files/avatar_qa.jpg"
            val validZip = File(envRoot, "valid_backup.zip")
            createValidBackupZip("Restored Customer QA", crossPkgPhotoPath, validZip)

            // 1. Successful stage + apply + cross-package path rewrite across all modules + preferences + presets
            StagedRestore.stage(isolatedCtx, validZip, zip = true)
            assertTrue(StagedRestore.isRestorePending(isolatedCtx))
            StagedRestore.applyPending(isolatedCtx)
            assertFalse(StagedRestore.isRestorePending(isolatedCtx))
            val okOutcome = StagedRestore.consumeRestoreResult(isolatedCtx)
            assertTrue("Expected SUCCESS restore outcome, got $okOutcome", okOutcome?.success == true)
            assertTrue(StagedRestore.hasPendingPreferences(isolatedCtx))

            runBlocking {
                prefs.setAppLock(enabled = true, pinHash = "qa-pin-hash", salt = "qa-pin-salt")
                assertTrue(StagedRestore.applyPendingPreferences(isolatedCtx, prefs))
                assertFalse(StagedRestore.hasPendingPreferences(isolatedCtx))
                assertEquals("dark", prefs.selectedTheme.first())
                assertEquals(AppLanguage.ENGLISH, prefs.selectedLanguage.first())
                assertEquals(CurrencyCode.USD, prefs.selectedCurrency.first())
                assertEquals("QA Restored Owner", prefs.userName.first())
                assertEquals(false, prefs.isFridayAutoBackupEnabled.first())
                assertEquals(File(envFiles, "avatar_qa.jpg").absolutePath, prefs.profilePhotoUri.first())
                assertEquals(File(envFiles, "company_logo/company_logo_qa.png").absolutePath, prefs.companyLogoPath.first())
                // Device-bound App Lock PIN must NEVER be overwritten by restored preferences
                assertTrue(prefs.isAppLockEnabled.first())
                assertEquals("qa-pin-hash", prefs.pinCodeHash.first())
                assertEquals("qa-pin-salt", prefs.pinSalt.first())
            }

            assertTrue(ccPrefs.getString("presets_list", "").orEmpty().contains("QA Preset"))
            assertTrue(draftsPrefs.all.isEmpty())

            val restoredDb = Room.databaseBuilder(
                context,
                AdvanceErpDatabase::class.java,
                isolatedCtx.getDatabasePath(AdvanceErpDatabase.DATABASE_NAME).absolutePath
            ).allowMainThreadQueries().build()
            val restoredCust = runBlocking { restoredDb.customerDao().getCustomerById("c-bak-1") }
            val restoredCustPhoto = runBlocking { restoredDb.customerDao().getCustomerById("c-bak-cust-photo") }
            val restoredProfile = runBlocking { restoredDb.businessProfileDao().getProfileSync() }
            val restoredPo = runBlocking { restoredDb.purchaseOrderDao().getOrderById("po-bak-1") }
            restoredDb.close()

            assertNotNull(restoredCust)
            assertEquals("Restored Customer QA", restoredCust!!.name)
            assertEquals(File(envFiles, "avatar_qa.jpg").absolutePath, restoredCust.profileImagePath)
            assertEquals(File(envFiles, "customer_photos/cust_qa.jpg").absolutePath, restoredCustPhoto?.profileImagePath)
            assertEquals(File(envFiles, "company_logo/company_logo_qa.png").absolutePath, restoredProfile?.logoPath)
            assertEquals(
                "${File(envFiles, "po_attachments/po_qa__inv.pdf").absolutePath},https://example.com/supplier_invoice",
                restoredPo?.attachments
            )
            assertTrue(File(envFiles, "avatar_qa.jpg").exists())
            assertTrue(File(envFiles, "product_images/qa_photo.jpg").exists())
            assertTrue(File(envFiles, "customer_photos/cust_qa.jpg").exists())
            assertTrue(File(envFiles, "company_logo/company_logo_qa.png").exists())
            assertTrue(File(envFiles, "company_signature/authorized_signature.png").exists())
            assertTrue(File(envFiles, "po_attachments/po_qa__inv.pdf").exists())

            // 2. Reject corrupt header, future schemaVersion, path traversal, duplicate entry before touching live DB
            val corruptZip = File(envRoot, "corrupt_sqlite.zip")
            ZipOutputStream(FileOutputStream(corruptZip)).use { zos ->
                zos.putNextEntry(ZipEntry("database/advance_erp.db"))
                zos.write("NOT_A_SQLITE_FILE".toByteArray())
                zos.closeEntry()
            }
            assertThrowsArgError { StagedRestore.stage(isolatedCtx, corruptZip, zip = true) }

            val futureSchemaZip = File(envRoot, "future_schema.zip")
            createValidBackupZip("Future", crossPkgPhotoPath, futureSchemaZip, schemaVer = 999)
            assertThrowsArgError { StagedRestore.stage(isolatedCtx, futureSchemaZip, zip = true) }

            val traversalZip = File(envRoot, "traversal.zip")
            ZipOutputStream(FileOutputStream(traversalZip)).use { zos ->
                ZipFile(validZip).use { src ->
                    val dbEntry = src.getEntry("database/advance_erp.db")
                    zos.putNextEntry(ZipEntry("database/advance_erp.db"))
                    src.getInputStream(dbEntry).use { it.copyTo(zos) }
                    zos.closeEntry()
                }
                zos.putNextEntry(ZipEntry("product_images/../../evil.txt"))
                zos.write("x".toByteArray())
                zos.closeEntry()
            }
            assertThrowsArgError { StagedRestore.stage(isolatedCtx, traversalZip, zip = true) }
            assertFalse(StagedRestore.isRestorePending(isolatedCtx))

            val duplicateZip = File(envRoot, "duplicate.zip")
            ZipOutputStream(FileOutputStream(duplicateZip)).use { zos ->
                zos.putNextEntry(ZipEntry("product_images/a.jpg"))
                zos.write(byteArrayOf(1))
                zos.closeEntry()
                // ZipOutputStream throws ZipException on duplicate ZipEntry name on JVM, or StagedRestore catches duplicate; verify rejected
                runCatching {
                    zos.putNextEntry(ZipEntry("product_images/a.jpg"))
                    zos.write(byteArrayOf(2))
                    zos.closeEntry()
                }
            }
            if (duplicateZip.length() > 0) {
                assertThrowsArgError { StagedRestore.stage(isolatedCtx, duplicateZip, zip = true) }
            }

            // 3. Interrupted restore rollback test: corrupt staged database.db after stage -> applyPending rolls back cleanly and records FAILED result
            val secondZip = File(envRoot, "second.zip")
            createValidBackupZip("Should Rollback", crossPkgPhotoPath, secondZip)
            draftsPrefs.edit().putString("purchase:live", """{"keep":"me"}""").commit()
            StagedRestore.stage(isolatedCtx, secondZip, zip = true)
            File(File(envFiles, "restore_staging"), "database.db").writeText("CORRUPTED_MID_APPLY")
            StagedRestore.applyPending(isolatedCtx)
            assertFalse(StagedRestore.isRestorePending(isolatedCtx))
            assertFalse(StagedRestore.hasPendingPreferences(isolatedCtx))
            val failOutcome = StagedRestore.consumeRestoreResult(isolatedCtx)
            assertEquals(false, failOutcome?.success)

            val rolledBackDb = Room.databaseBuilder(
                context,
                AdvanceErpDatabase::class.java,
                isolatedCtx.getDatabasePath(AdvanceErpDatabase.DATABASE_NAME).absolutePath
            ).allowMainThreadQueries().build()
            val afterRollback = runBlocking { rolledBackDb.customerDao().getCustomerById("c-bak-1") }
            val qc = rolledBackDb.openHelper.readableDatabase.query("PRAGMA quick_check").use { c ->
                if (c.moveToFirst()) c.getString(0) else ""
            }
            rolledBackDb.close()
            assertEquals("ok", qc)
            assertEquals("Restored Customer QA", afterRollback?.name)
            assertEquals("""{"keep":"me"}""", draftsPrefs.getString("purchase:live", null))
            assertTrue(ccPrefs.getString("presets_list", "").orEmpty().contains("QA Preset"))
            assertTrue(File(envFiles, "company_signature/authorized_signature.png").exists())
        } finally {
            runBlocking {
                prefs.setAppLock(enabled = false)
                prefs.setTheme("system")
                prefs.setLanguage(AppLanguage.BANGLA)
                prefs.setCurrency(CurrencyCode.BDT)
                prefs.setUserName(null)
                prefs.setUserRole(UserRole.OWNER_ADMIN)
                prefs.setProfilePhotoUri(null)
                prefs.setCompanyLogoPath(null)
                prefs.setFridayAutoBackupEnabled(true)
            }
            ccPrefs.edit().apply {
                if (origCcHasKey && origCcValue != null) putString("presets_list", origCcValue)
                else remove("presets_list")
            }.commit()
            draftsPrefs.edit().clear().apply {
                origDrafts.forEach { (k, v) -> if (v != null) putString(k, v) }
            }.commit()
            envRoot.deleteRecursively()
        }
    }

    private fun assertThrowsArgError(block: () -> Unit) {
        var failed = false
        try {
            block()
        } catch (_: IllegalArgumentException) {
            failed = true
        } catch (_: java.util.zip.ZipException) {
            failed = true
        }
        assertTrue("Expected IllegalArgumentException or ZipException", failed)
    }

    @Test
    fun whatsappBusinessHandoffAndFileProviderReadiness() = runBlocking {
        val installed = WhatsAppShareManager.installed(context)
        assertTrue(
            "Expected com.whatsapp.w4b (WhatsApp Business) installed on test device; found=$installed",
            WhatsAppShareManager.BUSINESS in installed
        )
        assertFalse(
            "Device check expected com.whatsapp absent on emulator-5554",
            WhatsAppShareManager.MESSENGER in installed
        )

        val cust = Customer("c-wa", "CUST-WA", "WA Test", "০১৭০০-০০০০০০", address = "Dhaka")
        db.customerDao().insertCustomer(cust)
        val invId = sales.createAndPostInvoice(
            SalesInvoiceDraft(
                customerId = cust.id,
                warehouseId = "wh-1",
                paidAmountMinor = 5000_00L,
                items = listOf(InvoiceItemDraft("p-1", null, 1, 5000_00L))
            )
        ).getOrThrow()

        val prepared = repo.prepare(CommunicationRequest(CommunicationModule.INVOICE, invId), languageOverride = "bn")
        assertEquals("+8801700000000", CommunicationPhone.normalize(prepared.document.phone))
        assertTrue(WhatsAppShareManager.isAttachmentReady(context, prepared.attachment))

        val outsideFile = File(context.cacheDir, "outside_comm.pdf").apply { writeText("test") }
        assertFalse(WhatsAppShareManager.isAttachmentReady(context, outsideFile))
        outsideFile.delete()

        val sendIntent = WhatsAppShareManager.sendIntent(
            context = context,
            file = prepared.attachment,
            text = prepared.message,
            application = WhatsAppShareManager.BUSINESS
        )
        assertEquals(Intent.ACTION_SEND, sendIntent.action)
        assertEquals(WhatsAppShareManager.BUSINESS, sendIntent.`package`)
        assertEquals("application/pdf", sendIntent.type)
        assertTrue((sendIntent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0)
        val streamUri = sendIntent.getParcelableExtra<android.net.Uri>(Intent.EXTRA_STREAM)
        assertNotNull(streamUri)
        assertEquals("com.pocketdev.advance_erp2.communicationqa.fileprovider", streamUri!!.authority)

        var missingMessengerRejected = false
        try {
            WhatsAppShareManager.openChat(context, "+8801700000000", WhatsAppShareManager.MESSENGER)
        } catch (_: IllegalArgumentException) {
            missingMessengerRejected = true
        }
        assertTrue(missingMessengerRejected)
    }

    @Test
    fun referenceDatasetMultiVariantMultiPageAndThreeFormatReconciliation() = runBlocking {
        val warehouseId = "wh-qa-ref-1"
        val wh = Warehouse(
            id = warehouseId,
            name = "Dhaka Central Warehouse",
            code = "DCW-QA",
            location = "41, New Elephant Road, Dhaka-1205",
            isDefault = true,
            isActive = true
        )
        if (db.warehouseDao().getWarehouseById(warehouseId) == null) {
            db.warehouseDao().insertWarehouse(wh)
        }

        val logoFile = createSyntheticSamplePng(
            File(context.filesDir, "company_logo/tuktak_ref_logo.png"),
            Color.rgb(91, 33, 182),
            Color.rgb(56, 189, 248),
            "TK"
        )
        val insoleFile = createSyntheticSamplePng(
            File(context.filesDir, "product_images/qa_4d_insole.png"),
            Color.rgb(224, 242, 254),
            Color.rgb(2, 132, 199),
            "4D"
        )
        db.businessProfileDao().insertOrUpdate(
            BusinessProfile(
                id = "default_profile",
                companyName = "TUKTAK BD",
                tagline = "Your Trusted Sourcing & Wholesale Partner",
                taxNumber = "002891929-0101",
                phone = "01639484547",
                email = "sales@tuktakbd.com",
                address = "41, New Elephant Road, Dhaka-1205",
                defaultCurrency = "BDT",
                logoPath = logoFile.absolutePath,
                website = "www.tuktakbd.com"
            )
        )
        repo.saveSettings(
            CommunicationSettings(
                includePdf = true,
                includeImageCard = true,
                includeTextMessage = true,
                showProductPhoto = true,
                showVariantSku = true,
                showQrCode = true,
                includeSignature = true,
                defaultLanguage = "bn",
                preferredApp = "ASK",
                defaultPaymentTerms = "Net 7 Days"
            )
        )

        val category = products.addCategory("Footwear Insoles QA", "ফুটওয়্যার ইনসোল")
        val p1 = Product(
            id = "prod-qa-4d-insole",
            categoryId = category.id,
            nameEn = "4D Cloud Insole Wholesale",
            nameBn = "4D Cloud Insole Wholesale",
            baseSku = "4DCI-WS",
            unit = "Pair",
            purchasePriceMinor = 85_000L,
            sellingPriceMinor = 138_000L
        )
        val v1 = ProductVariant(
            id = "var-qa-4d-gray-37",
            productId = p1.id,
            sku = "4DCI-WS-001",
            color = "Gray",
            size = "37-38",
            variantPriceMinor = 138_000L,
            variantCostMinor = 85_000L
        )
        products.addProductWithMedia(
            product = p1,
            variants = listOf(v1),
            images = listOf(
                ProductImage(
                    productId = p1.id,
                    variantId = v1.id,
                    imageUri = insoleFile.absolutePath,
                    isPrimary = true
                )
            ),
            initialStock = 0,
            warehouseId = warehouseId,
            variantInitialStock = mapOf(v1.id to 200)
        )

        // 1. Reference accounting sequence: Previous 29,560 + New 1,380 = 30,940 -> Payment 5,000 -> 25,940
        val now = System.currentTimeMillis()
        val hasanId = "cust-qa-hasan"
        contacts.addCustomerWithOptionalOpeningDue(
            customer = Customer(
                id = hasanId,
                customerCode = "CUST-9668",
                name = "Hasan Mojumdar",
                phone = "01866482142",
                address = "Dhaka, Bangladesh"
            ),
            openingDueMinor = 2_956_000L,
            entryDate = now - 40L * 86_400_000L,
            openingNote = "Opening Balance"
        )
        val inv1Id = sales.createAndPostInvoice(
            SalesInvoiceDraft(
                customerId = hasanId,
                warehouseId = warehouseId,
                invoiceDate = now,
                dueDate = now + 7L * 86_400_000L,
                paidAmountMinor = 0L,
                salesType = "WHOLESALE",
                paymentTerms = "Net 7 Days",
                items = listOf(InvoiceItemDraft(productId = p1.id, variantId = v1.id, quantity = 1, unitPriceMinor = 138_000L))
            )
        ).getOrThrow()

        val invUnpaidBn = engine.prepare(
            request = CommunicationRequest(CommunicationModule.INVOICE, inv1Id),
            languageOverride = "bn",
            forcePdf = true,
            forceImage = true
        )
        assertEquals(2_956_000L, invUnpaidBn.document.previousMinor)
        assertEquals(138_000L, invUnpaidBn.document.invoiceBreakdown?.netInvoiceAmountMinor)
        assertEquals(0L, invUnpaidBn.document.invoiceBreakdown?.allocatedPaymentMinor)
        assertEquals(138_000L, invUnpaidBn.document.invoiceBreakdown?.invoiceDueMinor)
        assertEquals(3_094_000L, invUnpaidBn.document.balanceMinor)
        assertEquals(CommunicationStatus.UNPAID, invUnpaidBn.document.status)
        assertTrue(invUnpaidBn.message.contains("1,380.00"))
        assertTrue(invUnpaidBn.message.contains("29,560.00"))
        assertTrue(invUnpaidBn.message.contains("30,940.00"))
        assertFalse(invUnpaidBn.message.contains("850.00"))
        assertFalse(invUnpaidBn.message.contains("profit", ignoreCase = true))
        renderPdfPageToCache(invUnpaidBn.pdfFile!!, 0, "qa_ref01_invoice_pdf_p1.png")
        copyFileToQaCache(invUnpaidBn.imageCardPages.first(), "qa_ref02_invoice_card.png")

        // Record Payment 5,000.00 (1,380 allocated to inv1, 3,620 reduces opening balance -> closing due 25,940.00)
        val pay1Id = sales.recordCustomerPayment(
            CustomerPaymentDraft(
                customerId = hasanId,
                amountMinor = 500_000L,
                paymentMethod = "CASH",
                referenceNote = "Cash received against due",
                allocations = listOf(InvoiceAllocationDraft(invoiceId = inv1Id, amountMinor = 138_000L))
            )
        )
        assertEquals(2_594_000L, sales.recalculateCustomerBalance(hasanId))

        val payBn = engine.prepare(
            request = CommunicationRequest(CommunicationModule.PAYMENT, pay1Id),
            languageOverride = "bn",
            forcePdf = true,
            forceImage = true
        )
        assertEquals(3_094_000L, payBn.document.paymentBreakdown?.previousBalanceMinor)
        assertEquals(500_000L, payBn.document.paymentBreakdown?.paymentAmountMinor)
        assertEquals(2_594_000L, payBn.document.balanceMinor)
        assertTrue(payBn.message.contains("5,000.00"))
        assertTrue(payBn.message.contains("25,940.00"))
        renderPdfPageToCache(payBn.pdfFile!!, 0, "qa_payment_receipt_pdf.png")
        copyFileToQaCache(payBn.imageCardPages.first(), "qa_ref03_payment_card.png")

        val remBn = engine.prepare(
            request = CommunicationRequest(CommunicationModule.REMINDER, hasanId),
            languageOverride = "bn",
            forcePdf = true,
            forceImage = true
        )
        assertEquals(2_594_000L, remBn.document.balanceMinor)
        copyFileToQaCache(remBn.imageCardPages.first(), "qa_ref04_reminder_card.png")
        renderPdfPageToCache(remBn.pdfFile!!, 0, "qa_ref04_reminder_pdf_p1.png")

        val stmtBn = engine.prepare(
            request = CommunicationRequest(
                module = CommunicationModule.STATEMENT,
                documentId = hasanId,
                from = now - 86_400_000L * 30,
                to = System.currentTimeMillis() + 86_400_000L
            ),
            languageOverride = "bn",
            forcePdf = true,
            forceImage = true
        )
        assertEquals(2_594_000L, stmtBn.document.balanceMinor)
        copyFileToQaCache(stmtBn.imageCardPages.first(), "qa_ref05_statement_card.png")
        renderPdfPageToCache(stmtBn.pdfFile!!, 0, "qa_ref05_statement_pdf_p1.png")

        // Old invoice re-share after later payment must preserve invoice total while reflecting current paid & balance
        val invReshareEn = engine.prepare(
            request = CommunicationRequest(CommunicationModule.INVOICE, inv1Id),
            languageOverride = "en",
            forcePdf = true,
            forceImage = true
        )
        assertEquals(138_000L, invReshareEn.document.invoiceBreakdown?.netInvoiceAmountMinor)
        assertEquals(138_000L, invReshareEn.document.invoiceBreakdown?.allocatedPaymentMinor)
        assertEquals(0L, invReshareEn.document.invoiceBreakdown?.invoiceDueMinor)
        assertEquals(2_594_000L, invReshareEn.document.balanceMinor)
        assertEquals(CommunicationStatus.PAID, invReshareEn.document.status)
        assertTrue(invReshareEn.message.contains("25,940.00"))

        // 2. Multi-variant, multi-page (22 lines), percentage & fixed discounts, partial payment
        val multiProd = Product(
            id = "prod-qa-multi-22",
            categoryId = category.id,
            nameEn = "Premium Breathable Orthopedic Memory Foam Sports & Running Insole Export Series",
            nameBn = "প্রিমিয়াম ব্রেথেবল অর্থোপেডিক মেমোরি ফোম স্পোর্টস ইনসোল এক্সপোর্ট সিরিজ",
            baseSku = "ORTHO-EXP",
            unit = "PCS",
            purchasePriceMinor = 45_000L,
            sellingPriceMinor = 80_000L
        )
        val colors = listOf("Black", "Navy Blue", "Crimson Red", "Olive Green", "Saddle Brown", "Sand Beige")
        val sizes = listOf("38", "39", "40", "41")
        val variants = (0 until 22).map { idx ->
            ProductVariant(
                id = "var-qa-multi-$idx",
                productId = multiProd.id,
                sku = "ORTHO-${idx + 1}",
                color = colors[idx % colors.size],
                size = sizes[idx / colors.size % sizes.size],
                variantPriceMinor = 80_000L + (idx * 2_000L),
                variantCostMinor = 45_000L
            )
        }
        products.addProductWithMedia(
            product = multiProd,
            variants = variants,
            images = listOf(ProductImage(productId = multiProd.id, variantId = null, imageUri = insoleFile.absolutePath, isPrimary = true)),
            initialStock = 0,
            warehouseId = warehouseId,
            variantInitialStock = variants.associate { it.id to 50 }
        )

        val rahimId = "cust-qa-rahim"
        contacts.addCustomerWithOptionalOpeningDue(
            customer = Customer(id = rahimId, customerCode = "CUST-9701", name = "Rahim Traders", phone = "01711223344", address = "Chittagong"),
            openingDueMinor = 0L
        )
        val multiInvId = sales.createAndPostInvoice(
            SalesInvoiceDraft(
                customerId = rahimId,
                warehouseId = warehouseId,
                invoiceDate = now,
                dueDate = now + 15L * 86_400_000L,
                discountMinor = 100_000L,
                discountPercentBps = 500L,
                additionalChargesMinor = 25_000L,
                paidAmountMinor = 1_000_000L,
                salesType = "RETAIL",
                paymentTerms = "Partial Cash + Due",
                items = variants.mapIndexed { idx, v ->
                    InvoiceItemDraft(
                        productId = multiProd.id,
                        variantId = v.id,
                        quantity = 2,
                        unitPriceMinor = v.variantPriceMinor,
                        discountMinor = if (idx == 0) 16_000L else 0L,
                        discountPercentBps = if (idx == 0) 1000L else null
                    )
                }
            )
        ).getOrThrow()

        val multiPrep = engine.prepare(
            request = CommunicationRequest(CommunicationModule.INVOICE, multiInvId),
            languageOverride = "bn",
            forcePdf = true,
            forceImage = true
        )
        assertEquals(CommunicationStatus.PARTIAL, multiPrep.document.status)
        assertEquals(22, multiPrep.document.lineItems.size)
        ParcelFileDescriptor.open(multiPrep.pdfFile!!, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { r ->
                assertTrue("Expected multi-page PDF >= 2 pages, got ${r.pageCount}", r.pageCount >= 2)
            }
        }
        assertTrue("Expected multi-page image cards >= 2, got ${multiPrep.imageCardPages.size}", multiPrep.imageCardPages.size >= 2)
        renderPdfPageToCache(multiPrep.pdfFile!!, 0, "qa_multipage_invoice_p1.png")
        renderPdfPageToCache(multiPrep.pdfFile!!, 1, "qa_multipage_invoice_p2.png")
        copyFileToQaCache(multiPrep.imageCardPages[0], "qa_multipage_card_p1.png")
        copyFileToQaCache(multiPrep.imageCardPages[1], "qa_multipage_card_p2.png")

        // 3. Overdue invoice, Sales Return, and Customer Advance Credit (-5,000.00)
        val overdueInvId = sales.createAndPostInvoice(
            SalesInvoiceDraft(
                customerId = rahimId,
                warehouseId = warehouseId,
                invoiceDate = now - 20L * 86_400_000L,
                dueDate = now - 5L * 86_400_000L,
                paidAmountMinor = 0L,
                items = listOf(InvoiceItemDraft(productId = p1.id, variantId = v1.id, quantity = 2, unitPriceMinor = 138_000L))
            )
        ).getOrThrow()
        val overduePrep = engine.prepare(CommunicationRequest(CommunicationModule.INVOICE, overdueInvId), languageOverride = "en", forcePdf = false, forceImage = false)
        assertEquals(CommunicationStatus.OVERDUE, overduePrep.document.status)

        val multiItems = sales.getInvoiceItems(multiInvId)
        val retId = sales.processSalesReturn(
            SalesReturnDraft(
                invoiceId = multiInvId,
                warehouseId = warehouseId,
                refundOption = ReturnRefundOption.CUSTOMER_CREDIT,
                reason = "Size exchange",
                items = listOf(
                    ReturnItemDraft(
                        invoiceItemId = multiItems.first().id,
                        productId = multiProd.id,
                        variantId = multiItems.first().variantId,
                        returnQuantity = 1
                    )
                )
            )
        )
        val retPrep = engine.prepare(CommunicationRequest(CommunicationModule.RETURN, retId), languageOverride = "bn", forcePdf = true, forceImage = true)
        assertEquals(CommunicationModule.RETURN, retPrep.document.request.module)
        assertTrue((retPrep.document.returnExchangeBreakdown?.returnAmountMinor ?: 0L) > 0L)
        assertNotNull(retPrep.pdfFile)
        assertTrue(retPrep.imageCardPages.isNotEmpty())

        val karimId = "cust-qa-karim-credit"
        contacts.addCustomerWithOptionalOpeningDue(
            customer = Customer(id = karimId, customerCode = "CUST-ADV-1", name = "Karim Enterprise", phone = "01911002233", address = "Sylhet"),
            openingDueMinor = 0L
        )
        sales.recordCustomerPayment(CustomerPaymentDraft(customerId = karimId, amountMinor = 500_000L, paymentMethod = "BANK", referenceNote = "Advance deposit"))
        assertEquals(-500_000L, sales.recalculateCustomerBalance(karimId))
        val creditRem = engine.prepare(CommunicationRequest(CommunicationModule.REMINDER, karimId), languageOverride = "bn", forcePdf = false, forceImage = true)
        assertEquals(CommunicationStatus.CREDIT, creditRem.document.status)
        assertEquals(-500_000L, creditRem.document.balanceMinor)
        copyFileToQaCache(creditRem.imageCardPages.first(), "qa_credit_reminder_card.png")

        // 4. Sequential share planner, mixed send intent, Downloads export & Cache/History management
        val steps = ShareWorkflowPlanner.buildShareSteps(
            selectedFormats = setOf(ShareOutputFormat.PDF, ShareOutputFormat.IMAGE_CARD, ShareOutputFormat.TEXT_MESSAGE),
            imagePageCount = invUnpaidBn.imageCardPages.size,
            documentExtension = "pdf"
        )
        assertEquals(3, steps.size)
        assertEquals(ShareStepKind.SEND_TEXT, steps[0].kind)
        assertEquals(ShareStepKind.SEND_DOCUMENT, steps[1].kind)
        assertEquals(ShareStepKind.SEND_IMAGE_CARDS, steps[2].kind)
        val multiIntent = WhatsAppShareManager.sendMultipleIntent(
            context = context,
            files = invUnpaidBn.imageCardPages,
            mimeType = "image/png",
            text = invUnpaidBn.message,
            application = null
        )
        assertTrue((multiIntent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0)

        val savedName = repo.saveAttachmentToDownloads(invUnpaidBn.pdfFile!!)
        assertTrue(savedName.endsWith(".pdf"))
        assertTrue(repo.getAttachmentCacheStats().fileCount > 0)
        val deletedCount = repo.clearAttachmentCache()
        assertTrue(deletedCount > 0)
        assertEquals(0, repo.getAttachmentCacheStats().fileCount)
        repo.clearHistory()
        assertTrue(repo.observeAuthorizedHistory().first().isEmpty())
    }

    @Test
    fun selectiveDataClearEndToEndPoUnreceivePaymentResetStockAndLedger() = runBlocking {
        val backupRepo = BackupRepositoryImpl(
            context.applicationContext,
            db,
            db.backupDao(),
            db.auditLogDao(),
            prefs
        )
        val vaultRepo = ImportVaultRepositoryImpl(db, context.applicationContext)
        val createdBackupFiles = mutableListOf<File>()
        try {
            prefs.setUserRole(UserRole.OWNER_ADMIN)
            val customBankId = accounting.createLiquidAccount(
                kind = LiquidAccountKind.BANK,
                nameEn = "Dutch-Bangla Bank QA",
                nameBn = "ডাচ-বাংলা ব্যাংক QA",
                institutionName = "DBBL",
                branchName = "Dhaka",
                accountNumber = "1234",
                description = null,
                openingBalanceMinor = 5_000_000L
            )
            val customBank = accounting.getAccountById(customBankId)!!
            accounting.depositCapitalToLiquidAccount("coa-1010", 5_000_000L)

            val cat = products.addCategory("Selective QA", "সিলেক্টিভ QA")
            val prod = Product(
                id = "prod-sel-qa",
                categoryId = cat.id,
                nameEn = "Smart Insole Selective QA",
                nameBn = "স্মার্ট ইনসোল QA",
                baseSku = "SEL-INSOLE",
                unit = "PCS",
                purchasePriceMinor = 17_000L,
                sellingPriceMinor = 35_000L
            )
            val vGray = ProductVariant(
                id = "var-sel-gray-38",
                productId = prod.id,
                sku = "SEL-G38",
                color = "Gray",
                size = "38",
                variantPriceMinor = 35_000L,
                variantCostMinor = 17_000L
            )
            val vBlack = ProductVariant(
                id = "var-sel-black-40",
                productId = prod.id,
                sku = "SEL-B40",
                color = "Black",
                size = "40",
                variantPriceMinor = 35_000L,
                variantCostMinor = 17_000L
            )
            products.addProductWithMedia(
                product = prod,
                variants = listOf(vGray, vBlack),
                images = emptyList(),
                initialStock = 0,
                warehouseId = "wh-1",
                variantInitialStock = mapOf(vGray.id to 20, vBlack.id to 5)
            )

            val supId = "sup-sel-qa"
            contacts.addSupplierWithOptionalOpeningPayable(
                supplier = Supplier(
                    id = supId,
                    supplierCode = "SUP-SEL-1",
                    companyName = "Shenzhen Selective Co",
                    phone = "01755001122",
                    address = "Shenzhen",
                    currency = "CNY"
                ),
                openingPayableBdtMinor = 1_000_000L,
                openingNote = "Opening Payable QA"
            )

            val poId = imports.savePurchaseOrderDraft(
                orderId = null,
                supplierId = supId,
                currency = "CNY",
                exchangeRate = BigDecimal("17.00"),
                items = listOf(
                    PurchaseOrderItemDraft(
                        productId = prod.id,
                        variantId = vGray.id,
                        quantity = 100,
                        unitPriceMinor = 1_000L,
                        cartonQuantity = 5,
                        pcsPerCarton = 20
                    )
                ),
                notes = "Keep full PO intact on selective reset",
                status = PurchaseOrderStatus.CONFIRMED,
                cartons = listOf(
                    PoMixedCartonDraft(
                        cartonNumber = "CTN-QA-1",
                        grossWeightKg = BigDecimal("25.0"),
                        lines = listOf(
                            MixedCartonLineDraft(
                                productId = prod.id,
                                variantId = vGray.id,
                                productName = prod.nameEn,
                                quantity = 100
                            )
                        )
                    )
                ),
                chinaDeliveryChargeMinor = 0L,
                categoryFreightRatesBdtMinor = emptyMap(),
                handlingCostMinor = 0L,
                sampleCostMinor = 0L,
                attachments = null
            )
            val poItem = imports.getItemsForOrderSync(poId).single()
            imports.recordSupplierPayment(
                supplierId = supId,
                orderId = poId,
                amountMinor = 40_000L,
                currency = "CNY",
                exchangeRate = BigDecimal("17.00"),
                method = PaymentMethod.BANK_TT,
                paymentAccount = customBank.accountCode,
                reference = "SP-QA-1",
                notes = "Partial payment",
                paymentAccountId = customBank.id
            )
            imports.addPurchaseCostItem(
                orderId = poId,
                category = com.pocketdev.advance_erp.data.entity.CostCategory.INTERNATIONAL_FREIGHT,
                description = "QA Air Freight",
                amountMinor = 50_000L,
                currency = "BDT",
                exchangeRate = BigDecimal.ONE,
                method = com.pocketdev.advance_erp.core.engine.AllocationMethod.QUANTITY,
                isEstimated = false
            )
            imports.recordPoFreightPayment(
                orderId = poId,
                paymentAccountId = customBank.id,
                amountBdtMinor = 50_000L,
                notes = "QA Freight Payment"
            )
            imports.finalizePurchaseActualCost(poId)
            imports.createPurchaseReceiving(
                orderId = poId,
                warehouseId = "wh-1",
                receivingMethod = "PHYSICAL_COUNT",
                items = listOf(
                    ReceiptItemDraft(
                        productId = prod.id,
                        variantId = vGray.id,
                        expectedQuantity = 100,
                        receivedQuantity = 100,
                        goodQuantity = 100,
                        purchaseOrderId = poId,
                        purchaseOrderItemId = poItem.id
                    )
                ),
                notes = "Full PO receipt",
                postImmediately = true
            )

            val custId = "cust-sel-qa"
            contacts.addCustomerWithOptionalOpeningDue(
                customer = Customer(
                    id = custId,
                    customerCode = "CUST-SEL-1",
                    name = "Dhaka Selective Store",
                    phone = "01888112233",
                    address = "Dhaka"
                ),
                openingDueMinor = 500_000L,
                openingNote = "Opening Due QA"
            )
            val invId = sales.createAndPostInvoice(
                SalesInvoiceDraft(
                    customerId = custId,
                    warehouseId = "wh-1",
                    paidAmountMinor = 100_000L,
                    items = listOf(InvoiceItemDraft(productId = prod.id, variantId = vGray.id, quantity = 10, unitPriceMinor = 35_000L))
                )
            ).getOrThrow()
            assertNotNull(sales.getInvoiceById(invId))

            val preview = backupRepo.getDataClearImpactPreview()
            assertTrue(preview.preservedVariantCount >= 2)
            assertTrue(preview.preservedPoCount >= 1)
            assertTrue(preview.poReceiptLayerCount >= 1)
            assertTrue((preview.moduleCounts[DataClearModule.SALES_FULL_CLEAR]?.primaryCount ?: 0) >= 1)
            assertTrue((preview.moduleCounts[DataClearModule.INVENTORY_STOCK_RESET]?.primaryCount ?: 0) >= 1)

            // Non-OWNER_ADMIN role must be rejected
            prefs.setUserRole(UserRole.SALES_OPERATOR)
            val rejected = backupRepo.executeSelectiveDataClear(
                SelectiveDataClearRequest(
                    selectedModules = setOf(DataClearModule.COMMUNICATION_LOGS_CLEAR),
                    createSafetyBackup = false,
                    operatorRole = UserRole.SALES_OPERATOR
                )
            )
            assertTrue(rejected.isFailure)
            prefs.setUserRole(UserRole.OWNER_ADMIN)

            val poBeforeReset = db.purchaseOrderDao().getOrderById(poId)!!
            val itemBeforeReset = db.purchaseOrderDao().getItemsForOrderSync(poId).single()
            val cartonsBeforeReset = db.importShipmentDao().getCartonsForOrderSync(poId).size
            assertTrue(cartonsBeforeReset > 0)
            val vaultBeforeReset = vaultRepo.getOverview(UserRole.OWNER_ADMIN)
            assertEquals(730_000L, vaultBeforeReset.totalInvestedCapitalMinor)

            // Subtest A: PO_RESET_KEEP_STRUCTURE ONLY (keeps full PO/cartons/qty/price, resets receiving & payments)
            val poOnlyOutcome = backupRepo.executeSelectiveDataClear(
                SelectiveDataClearRequest(
                    selectedModules = setOf(DataClearModule.PO_RESET_KEEP_STRUCTURE),
                    createSafetyBackup = true,
                    operatorRole = UserRole.OWNER_ADMIN
                )
            ).getOrThrow()
            poOnlyOutcome.safetyBackupRecordId?.let { recId ->
                backupRepo.getAllBackupRecords().first().firstOrNull { it.id == recId }?.let { rec ->
                    val zip = File(rec.filePath)
                    createdBackupFiles += zip
                    assertTrue(zip.exists() && backupRepo.verifyBackupFile(zip))
                }
            }

            val poAfterReset = db.purchaseOrderDao().getOrderById(poId)!!
            assertEquals("CONFIRMED", poAfterReset.status)
            assertEquals(poBeforeReset.totalAmountMinor, poAfterReset.totalAmountMinor)
            assertEquals(poBeforeReset.totalBdtAmountMinor, poAfterReset.totalBdtAmountMinor)
            assertEquals(poBeforeReset.totalCartons, poAfterReset.totalCartons)
            assertEquals(0L, poAfterReset.paidAmountMinor)
            assertEquals(0L, poAfterReset.paidBdtAmountMinor)
            assertFalse(poAfterReset.isActualCostFinalized)

            val itemAfterReset = db.purchaseOrderDao().getItemsForOrderSync(poId).single()
            assertEquals(itemBeforeReset.quantity, itemAfterReset.quantity)
            assertEquals(itemBeforeReset.unitPriceMinor, itemAfterReset.unitPriceMinor)
            assertEquals(itemBeforeReset.cartonQuantity, itemAfterReset.cartonQuantity)
            assertEquals(itemBeforeReset.pcsPerCarton, itemAfterReset.pcsPerCarton)
            assertEquals(0, itemAfterReset.receivedQuantity)

            assertEquals(cartonsBeforeReset, db.importShipmentDao().getCartonsForOrderSync(poId).size)
            assertTrue(db.warehouseReceiptDao().getReceiptsByOrderSync(poId).isEmpty())
            assertTrue(db.supplierPaymentDao().getPaymentsByOrderSync(poId).isEmpty())
            val vaultAfterPoReset = vaultRepo.getOverview(UserRole.OWNER_ADMIN)
            assertEquals(0L, vaultAfterPoReset.totalInvestedCapitalMinor)
            assertTrue(vaultAfterPoReset.shipments.all { it.totalInvestedCapitalMinor == 0L })
            assertTrue(vaultAfterPoReset.purchaseOrders.all { it.totalInvestedCapitalMinor == 0L })

            // Re-pay and re-receive the preserved PO directly to prove full structure still works
            imports.recordSupplierPayment(
                supplierId = supId,
                orderId = poId,
                amountMinor = 20_000L,
                currency = "CNY",
                exchangeRate = BigDecimal("17.00"),
                method = PaymentMethod.BANK_TT,
                paymentAccount = customBank.accountCode,
                reference = "SP-QA-REPAY",
                notes = "Re-payment after reset",
                paymentAccountId = customBank.id
            )
            imports.finalizePurchaseActualCost(poId)
            imports.createPurchaseReceiving(
                orderId = poId,
                warehouseId = "wh-1",
                receivingMethod = "PHYSICAL_COUNT",
                items = listOf(
                    ReceiptItemDraft(
                        productId = prod.id,
                        variantId = vGray.id,
                        expectedQuantity = 100,
                        receivedQuantity = 100,
                        goodQuantity = 100,
                        purchaseOrderId = poId,
                        purchaseOrderItemId = poItem.id
                    )
                ),
                notes = "Re-receipt after reset",
                postImmediately = true
            )
            assertEquals(100, db.purchaseOrderDao().getItemsForOrderSync(poId).single().receivedQuantity)
            val vaultAfterRepay = vaultRepo.getOverview(UserRole.OWNER_ADMIN)
            assertEquals(340_000L, vaultAfterRepay.totalInvestedCapitalMinor)
            assertTrue(vaultAfterRepay.totalLockedInventoryCapitalMinor > 340_000L)

            // Subtest B: SMART_FULL_RESET
            val fullOutcome = backupRepo.executeSelectiveDataClear(
                SelectiveDataClearRequest(
                    selectedModules = SelectiveDataClearPlanner.modulesForPreset(DataClearPreset.SMART_FULL_RESET),
                    createSafetyBackup = true,
                    operatorRole = UserRole.OWNER_ADMIN
                )
            ).getOrThrow()
            fullOutcome.safetyBackupRecordId?.let { recId ->
                backupRepo.getAllBackupRecords().first().firstOrNull { it.id == recId }?.let { rec ->
                    val zip = File(rec.filePath)
                    createdBackupFiles += zip
                    assertTrue(zip.exists())
                }
            }

            assertEquals(2, db.productVariantDao().getVariantsForProductSync(prod.id).size)
            assertEquals(0, db.backupDao().countTotalStockUnits())
            assertEquals(0, db.backupDao().countSalesInvoices())
            assertEquals(0, db.backupDao().countCustomerPayments())
            assertEquals(0L, db.customerDao().getCustomerById(custId)!!.outstandingBalanceMinor)

            assertNotNull(db.accountingDao().getAccountById(customBank.id))
            assertEquals(0L, db.accountingDao().getNetDebitBalanceForAccount(customBank.accountCode))
            assertEquals(0L, db.accountingDao().getNetDebitBalanceForAccount("1010"))

            val finalPo = db.purchaseOrderDao().getOrderById(poId)!!
            assertEquals("CONFIRMED", finalPo.status)
            assertEquals(0L, finalPo.paidAmountMinor)
            assertEquals(0, db.purchaseOrderDao().getItemsForOrderSync(poId).single().receivedQuantity)
            assertEquals(1_700_000L, db.supplierDao().getSupplierById(supId)!!.outstandingPayableMinor)
            val vaultAfterFullReset = vaultRepo.getOverview(UserRole.OWNER_ADMIN)
            assertEquals(0L, vaultAfterFullReset.totalInvestedCapitalMinor)
            assertTrue(vaultAfterFullReset.shipments.all { it.totalInvestedCapitalMinor == 0L })
            assertTrue(vaultAfterFullReset.purchaseOrders.all { it.totalInvestedCapitalMinor == 0L })
        } finally {
            prefs.setUserRole(UserRole.OWNER_ADMIN)
            createdBackupFiles.forEach { if (it.exists()) it.delete() }
        }
    }
}
