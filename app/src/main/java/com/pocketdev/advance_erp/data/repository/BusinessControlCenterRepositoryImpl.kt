package com.pocketdev.advance_erp.data.repository

import androidx.room.InvalidationTracker
import androidx.room.withTransaction
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import com.pocketdev.advance_erp.core.engine.LedgerReportEngine
import com.pocketdev.advance_erp.domain.model.LedgerReport
import com.pocketdev.advance_erp.domain.model.LedgerParty
import android.content.Context
import android.content.SharedPreferences
import com.pocketdev.advance_erp.core.database.AdvanceErpDatabase
import com.pocketdev.advance_erp.data.dao.BusinessProfileDao
import com.pocketdev.advance_erp.data.dao.CustomerDao
import com.pocketdev.advance_erp.data.dao.CustomerPaymentDao
import com.pocketdev.advance_erp.data.dao.ExpenseDao
import com.pocketdev.advance_erp.data.dao.ProductCategoryDao
import com.pocketdev.advance_erp.data.dao.ProductDao
import com.pocketdev.advance_erp.data.dao.ProductVariantDao
import com.pocketdev.advance_erp.data.dao.PurchaseOrderDao
import com.pocketdev.advance_erp.data.dao.SalesDao
import com.pocketdev.advance_erp.data.dao.SalesReturnDao
import com.pocketdev.advance_erp.data.dao.StockMovementDao
import com.pocketdev.advance_erp.data.dao.StockTransferDao
import com.pocketdev.advance_erp.data.dao.SupplierDao
import com.pocketdev.advance_erp.data.dao.SupplierPaymentDao
import com.pocketdev.advance_erp.data.dao.WarehouseDao
import com.pocketdev.advance_erp.data.entity.BusinessProfile
import com.pocketdev.advance_erp.data.entity.Customer
import com.pocketdev.advance_erp.data.entity.Product
import com.pocketdev.advance_erp.data.entity.ProductCategory
import com.pocketdev.advance_erp.data.entity.Supplier
import com.pocketdev.advance_erp.data.entity.Warehouse
import com.pocketdev.advance_erp.domain.model.AmountRangeFilter
import com.pocketdev.advance_erp.domain.model.BusinessSummaryDashboardData
import com.pocketdev.advance_erp.domain.model.ControlCenterFilterState
import com.pocketdev.advance_erp.domain.model.DateRangeOption
import com.pocketdev.advance_erp.domain.model.DeliveryReturnFilter
import com.pocketdev.advance_erp.domain.model.FinancialMetricFilter
import com.pocketdev.advance_erp.domain.model.LineItemDetail
import com.pocketdev.advance_erp.domain.model.PartyLedgerStatement
import com.pocketdev.advance_erp.domain.model.PaymentMethodFilter
import com.pocketdev.advance_erp.domain.model.PaymentStatusFilter
import com.pocketdev.advance_erp.domain.model.ReportCategory
import com.pocketdev.advance_erp.domain.model.SaleChannelFilter
import com.pocketdev.advance_erp.domain.model.SavedFilterPreset
import com.pocketdev.advance_erp.domain.model.SortOption
import com.pocketdev.advance_erp.domain.model.TransactionDetailRecord
import com.pocketdev.advance_erp.domain.model.TransactionDirectionFilter
import com.pocketdev.advance_erp.domain.model.UnifiedTransactionItem
import com.pocketdev.advance_erp.domain.repository.BusinessControlCenterRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BusinessControlCenterRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AdvanceErpDatabase,
    private val salesDao: SalesDao,
    private val customerPaymentDao: CustomerPaymentDao,
    private val salesReturnDao: SalesReturnDao,
    private val purchaseOrderDao: PurchaseOrderDao,
    private val supplierPaymentDao: SupplierPaymentDao,
    private val expenseDao: ExpenseDao,
    private val stockMovementDao: StockMovementDao,
    private val stockTransferDao: StockTransferDao,
    private val customerDao: CustomerDao,
    private val supplierDao: SupplierDao,
    private val warehouseDao: WarehouseDao,
    private val productCategoryDao: ProductCategoryDao,
    private val productDao: ProductDao,
    private val productVariantDao: ProductVariantDao,
    private val businessProfileDao: BusinessProfileDao
) : BusinessControlCenterRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences("control_center_presets", Context.MODE_PRIVATE)
    private val presetsFlow = MutableStateFlow<List<SavedFilterPreset>>(loadPresetsFromPrefs())

    override fun getAllCustomers(): Flow<List<Customer>> = customerDao.getLedgerCustomers()

    override fun getAllSuppliers(): Flow<List<Supplier>> = supplierDao.getLedgerSuppliers()

    override fun getAllWarehouses(): Flow<List<Warehouse>> = warehouseDao.getAllWarehouses()

    override fun getAllCategories(): Flow<List<ProductCategory>> = productCategoryDao.getAllCategories()

    override fun getAllProducts(): Flow<List<Product>> = productDao.getAllActiveProducts()

    override fun getBusinessProfile(): Flow<BusinessProfile?> = businessProfileDao.getProfile()

    override fun getSavedPresets(): Flow<List<SavedFilterPreset>> = presetsFlow.asStateFlow()

    override suspend fun savePreset(name: String, filterState: ControlCenterFilterState): String {
        val newPreset = SavedFilterPreset(
            id = UUID.randomUUID().toString(),
            name = name,
            filterState = filterState
        )
        val current = presetsFlow.value.toMutableList()
        current.add(0, newPreset)
        savePresetsToPrefs(current)
        presetsFlow.value = current
        return newPreset.id
    }

    override suspend fun deletePreset(presetId: String) {
        val current = presetsFlow.value.filterNot { it.id == presetId }
        savePresetsToPrefs(current)
        presetsFlow.value = current
    }

    private fun loadPresetsFromPrefs(): List<SavedFilterPreset> {
        val jsonStr = prefs.getString("presets_list", null) ?: return emptyList()
        val list = mutableListOf<SavedFilterPreset>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val filterObj = obj.getJSONObject("filterState")
                val filterState = ControlCenterFilterState(
                    dateRangeOption = DateRangeOption.valueOf(filterObj.optString("dateRangeOption", DateRangeOption.ALL_TIME.name)),
                    customStartDate = if (filterObj.has("customStartDate")) filterObj.getLong("customStartDate") else null,
                    customEndDate = if (filterObj.has("customEndDate")) filterObj.getLong("customEndDate") else null,
                    category = ReportCategory.valueOf(filterObj.optString("category", ReportCategory.ALL_TRANSACTIONS.name)),
                    searchQuery = filterObj.optString("searchQuery", ""),
                    selectedCustomerId = if (filterObj.has("selectedCustomerId")) filterObj.getString("selectedCustomerId") else null,
                    selectedSupplierId = if (filterObj.has("selectedSupplierId")) filterObj.getString("selectedSupplierId") else null,
                    selectedWarehouseId = if (filterObj.has("selectedWarehouseId")) filterObj.getString("selectedWarehouseId") else null,
                    selectedCategoryId = if (filterObj.has("selectedCategoryId")) filterObj.getString("selectedCategoryId") else null,
                    selectedProductId = if (filterObj.has("selectedProductId")) filterObj.getString("selectedProductId") else null,
                    paymentMethodFilter = runCatching { PaymentMethodFilter.valueOf(filterObj.optString("paymentMethodFilter", PaymentMethodFilter.ALL.name)) }.getOrDefault(PaymentMethodFilter.ALL),
                    paymentStatus = PaymentStatusFilter.valueOf(filterObj.optString("paymentStatus", PaymentStatusFilter.ALL.name)),
                    deliveryReturn = DeliveryReturnFilter.valueOf(filterObj.optString("deliveryReturn", DeliveryReturnFilter.ALL.name)),
                    financialMetric = FinancialMetricFilter.valueOf(filterObj.optString("financialMetric", FinancialMetricFilter.ALL.name)),
                    amountFilter = AmountRangeFilter.valueOf(filterObj.optString("amountFilter", AmountRangeFilter.ALL.name)),
                    customMinAmountMinor = if (filterObj.has("customMinAmountMinor")) filterObj.getLong("customMinAmountMinor") else null,
                    customMaxAmountMinor = if (filterObj.has("customMaxAmountMinor")) filterObj.getLong("customMaxAmountMinor") else null,
                    sortBy = runCatching { SortOption.valueOf(filterObj.optString("sortBy", SortOption.NEWEST_FIRST.name)) }.getOrDefault(SortOption.NEWEST_FIRST),
                    transactionDirection = runCatching { TransactionDirectionFilter.valueOf(filterObj.optString("transactionDirection", TransactionDirectionFilter.ALL.name)) }.getOrDefault(TransactionDirectionFilter.ALL),
                    saleChannel = runCatching { SaleChannelFilter.valueOf(filterObj.optString("saleChannel", SaleChannelFilter.ALL.name)) }.getOrDefault(SaleChannelFilter.ALL)
                )
                list.add(
                    SavedFilterPreset(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        filterState = filterState,
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    private fun savePresetsToPrefs(presets: List<SavedFilterPreset>) {
        val arr = JSONArray()
        for (p in presets) {
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("name", p.name)
            obj.put("createdAt", p.createdAt)
            val fObj = JSONObject()
            fObj.put("dateRangeOption", p.filterState.dateRangeOption.name)
            p.filterState.customStartDate?.let { fObj.put("customStartDate", it) }
            p.filterState.customEndDate?.let { fObj.put("customEndDate", it) }
            fObj.put("category", p.filterState.category.name)
            fObj.put("searchQuery", p.filterState.searchQuery)
            p.filterState.selectedCustomerId?.let { fObj.put("selectedCustomerId", it) }
            p.filterState.selectedSupplierId?.let { fObj.put("selectedSupplierId", it) }
            p.filterState.selectedWarehouseId?.let { fObj.put("selectedWarehouseId", it) }
            p.filterState.selectedCategoryId?.let { fObj.put("selectedCategoryId", it) }
            p.filterState.selectedProductId?.let { fObj.put("selectedProductId", it) }
            fObj.put("paymentMethodFilter", p.filterState.paymentMethodFilter.name)
            fObj.put("paymentStatus", p.filterState.paymentStatus.name)
            fObj.put("deliveryReturn", p.filterState.deliveryReturn.name)
            fObj.put("financialMetric", p.filterState.financialMetric.name)
            fObj.put("amountFilter", p.filterState.amountFilter.name)
            p.filterState.customMinAmountMinor?.let { fObj.put("customMinAmountMinor", it) }
            p.filterState.customMaxAmountMinor?.let { fObj.put("customMaxAmountMinor", it) }
            fObj.put("sortBy", p.filterState.sortBy.name)
            fObj.put("transactionDirection", p.filterState.transactionDirection.name)
            fObj.put("saleChannel", p.filterState.saleChannel.name)
            obj.put("filterState", fObj)
            arr.put(obj)
        }
        prefs.edit().putString("presets_list", arr.toString()).apply()
    }

    private suspend fun loadLedgerRows(filterState: ControlCenterFilterState): List<UnifiedTransactionItem> {
        val fromMillis = 0L
        val toMillis = Long.MAX_VALUE

        val suppliers = supplierDao.getAllSuppliersIncludingInactiveSync().associateBy { it.id }
        val productCategoryByProductId = productDao.getAllProductsSync().associate { it.id to it.categoryId }
        val invoiceItemsByInvoiceId = salesDao.getAllInvoiceItemsSync().groupBy { it.invoiceId }
        val poItemsByOrderId = purchaseOrderDao.getAllOrderItemsSync().groupBy { it.purchaseOrderId }
        val returnItemsByReturnId = salesReturnDao.getAllReturnItemsSync().groupBy { it.returnId }
        val allList = mutableListOf<UnifiedTransactionItem>()

        // 1. Sales Invoices
        val invoices = when { filterState.selectedSupplierId != null -> emptyList(); filterState.selectedCustomerId != null -> salesDao.getInvoicesForCustomerSync(filterState.selectedCustomerId); else -> salesDao.getAllInvoicesSync() }
        for (inv in invoices) {
            if (inv.invoiceDate in fromMillis..toMillis) {
                val statusColor = when (inv.status) {
                    "PAID" -> 0xFF10B981L
                    "PARTIALLY_PAID" -> 0xFFF59E0BL
                    "CANCELLED" -> 0xFFEF4444L
                    "DELIVERED" -> 0xFF059669L
                    "PARTIALLY_DELIVERED" -> 0xFF0284C7L
                    else -> 0xFF6366F1L
                }
                val itemRows = invoiceItemsByInvoiceId[inv.id].orEmpty()
                val invProductIds = itemRows.map { it.productId }.toSet()
                val invCategoryIds = itemRows.mapNotNull { productCategoryByProductId[it.productId] }.toSet()
                val retail = inv.customerName.equals("Walk-in Customer", ignoreCase = true) ||
                    inv.notes?.contains("Retail", ignoreCase = true) == true ||
                    inv.notes?.contains("POS", ignoreCase = true) == true ||
                    (inv.paidAmountMinor >= inv.totalAmountMinor && inv.dueDate == null && inv.totalAmountMinor <= 500_000L)
                allList.add(
                    UnifiedTransactionItem(
                        id = inv.id,
                        referenceNumber = inv.invoiceNumber,
                        date = inv.invoiceDate,
                        category = if (inv.status in listOf("DELIVERED", "PARTIALLY_DELIVERED")) ReportCategory.DELIVERY else ReportCategory.SALES,
                        primaryEntityName = inv.customerName,
                        primaryEntityId = inv.customerId,
                        particulars = "Sales Invoice (${inv.status})",
                        amountMinor = inv.totalAmountMinor,
                        secondaryAmountMinor = inv.dueAmountMinor,
                        isInflow = true,
                        paymentMethod = when {
                            inv.paidAmountMinor >= inv.totalAmountMinor -> "PAID"
                            inv.paidAmountMinor > 0 -> "PARTIAL"
                            else -> "UNPAID"
                        },
                        status = inv.status,
                        statusColorHex = statusColor,
                        warehouseName = inv.warehouseName,
                        rawType = "INVOICE",
                        notes = inv.notes,
                        createdAt = inv.createdAt, accountingPosted = inv.isPosted, warehouseId = inv.warehouseId,
                        dueDate = inv.dueDate, cogsMinor = inv.totalCogsMinor, profitMinor = inv.totalAmountMinor - inv.taxMinor - inv.totalCogsMinor, discountMinor = inv.discountMinor, taxMinor = inv.taxMinor,
                        productIds = invProductIds,
                        categoryIds = invCategoryIds,
                        isRetailSale = retail
                    )
                )
            }
        }

        // 2. Customer Payments (Receipts)
        val customerPayments = when { filterState.selectedSupplierId != null -> emptyList(); filterState.selectedCustomerId != null -> customerPaymentDao.getPaymentsForCustomerSync(filterState.selectedCustomerId); else -> customerPaymentDao.getAllPaymentsSync() }
        for (pmt in customerPayments) {
            if (pmt.paymentDate in fromMillis..toMillis) {
                allList.add(
                    UnifiedTransactionItem(
                        id = pmt.id,
                        referenceNumber = pmt.paymentNumber,
                        date = pmt.paymentDate,
                        category = ReportCategory.RECEIPT,
                        primaryEntityName = pmt.customerName,
                        primaryEntityId = pmt.customerId,
                        particulars = pmt.referenceNote?.ifBlank { null } ?: "Customer Payment Received",
                        amountMinor = pmt.amountMinor,
                        secondaryAmountMinor = pmt.unallocatedCreditMinor,
                        isInflow = true,
                        paymentMethod = pmt.paymentMethod,
                        status = if (pmt.isVoided) "VOIDED" else "POSTED",
                        statusColorHex = if (pmt.isVoided) 0xFFEF4444L else 0xFF10B981L,
                        rawType = "PAYMENT",
                        notes = listOfNotNull(pmt.referenceNote, pmt.voidReason).joinToString(" · "),
                        createdAt = pmt.createdAt, accountingPosted = pmt.isPosted
                    )
                )
            }
        }

        // 3. Sales Returns
        val salesReturns = when { filterState.selectedSupplierId != null -> emptyList(); filterState.selectedCustomerId != null -> salesReturnDao.getReturnsForCustomerSync(filterState.selectedCustomerId); else -> salesReturnDao.getAllReturnsSync() }
        for (ret in salesReturns) {
            if (ret.returnDate in fromMillis..toMillis) {
                val retItems = returnItemsByReturnId[ret.id].orEmpty()
                val retProductIds = retItems.map { it.productId }.toSet()
                val retCategoryIds = retItems.mapNotNull { productCategoryByProductId[it.productId] }.toSet()
                allList.add(
                    UnifiedTransactionItem(
                        id = ret.id,
                        referenceNumber = ret.returnNumber,
                        date = ret.returnDate,
                        category = ReportCategory.SALES_RETURN,
                        primaryEntityName = ret.customerName,
                        primaryEntityId = ret.customerId,
                        particulars = ret.reason?.ifBlank { null } ?: "Return against ${ret.invoiceNumber}",
                        amountMinor = ret.totalReturnAmountMinor,
                        secondaryAmountMinor = ret.totalCogsReversalMinor,
                        isInflow = false,
                        paymentMethod = ret.refundOption,
                        status = "RETURNED",
                        statusColorHex = 0xFFDC2626L,
                        rawType = "RETURN",
                        notes = ret.reason,
                        createdAt = ret.createdAt, accountingPosted = ret.isPosted, warehouseId = ret.warehouseId,
                        cogsMinor = ret.totalCogsReversalMinor, profitMinor = -ret.totalReturnAmountMinor + ret.totalCogsReversalMinor,
                        productIds = retProductIds,
                        categoryIds = retCategoryIds
                    )
                )
            }
        }

        // 4. Purchase Orders
        val purchaseOrders = if (filterState.selectedCustomerId != null) emptyList() else filterState.selectedSupplierId?.let { purchaseOrderDao.getLedgerOrdersSync(it) } ?: purchaseOrderDao.getAllOrdersSync()
        for (po in purchaseOrders) {
            if (po.orderDate in fromMillis..toMillis) {
                val supplierName = suppliers[po.supplierId]?.companyName ?: "Supplier"
                val isImport = po.currency != "BDT"
                val statusColor = when (po.status) {
                    "COMPLETED", "RECEIVED" -> 0xFF10B981L
                    "SHIPPED" -> 0xFF0284C7L
                    "CANCELLED" -> 0xFFEF4444L
                    else -> 0xFF8B5CF6L
                }
                val poItems = poItemsByOrderId[po.id].orEmpty()
                val poProductIds = poItems.map { it.productId }.toSet()
                val poCategoryIds = poItems.mapNotNull { productCategoryByProductId[it.productId] }.toSet()
                allList.add(
                    UnifiedTransactionItem(
                        id = po.id,
                        referenceNumber = po.poNumber,
                        date = po.orderDate,
                        category = if (isImport) ReportCategory.IMPORT else ReportCategory.PURCHASE,
                        primaryEntityName = supplierName,
                        primaryEntityId = po.supplierId,
                        particulars = "Purchase Order [${po.currency} ${po.totalAmountMinor / 100.0}]",
                        amountMinor = po.totalBdtAmountMinor,
                        secondaryAmountMinor = po.totalBdtAmountMinor - po.paidBdtAmountMinor,
                        isInflow = false,
                        paymentMethod = po.currency,
                        status = po.status,
                        statusColorHex = statusColor,
                        rawType = "PURCHASE",
                        notes = po.notes,
                        createdAt = po.createdAt, accountingPosted = po.status !in setOf("DRAFT", "CANCELLED"),
                        originalCurrency = po.currency, originalAmountMinor = po.totalAmountMinor, exchangeRate = po.exchangeRateToBdt,
                        productIds = poProductIds,
                        categoryIds = poCategoryIds
                    )
                )
            }
        }

        // 5. Supplier Payments
        val supplierPayments = if (filterState.selectedCustomerId != null) emptyList() else filterState.selectedSupplierId?.let { supplierPaymentDao.getLedgerPaymentsSync(it) } ?: supplierPaymentDao.getAllPaymentsSync()
        for (sp in supplierPayments) {
            if (sp.paymentDate in fromMillis..toMillis) {
                val supplierName = suppliers[sp.supplierId]?.companyName ?: "Supplier"
                allList.add(
                    UnifiedTransactionItem(
                        id = sp.id,
                        referenceNumber = sp.paymentNumber,
                        date = sp.paymentDate,
                        category = ReportCategory.PAYMENT,
                        primaryEntityName = supplierName,
                        primaryEntityId = sp.supplierId,
                        particulars = "Supplier Remittance (${sp.paymentMethod})",
                        amountMinor = sp.bdtAmountMinor,
                        secondaryAmountMinor = sp.amountMinor,
                        isInflow = false,
                        paymentMethod = sp.paymentMethod,
                        status = "PAID",
                        statusColorHex = 0xFF0284C7L,
                        rawType = "SUPPLIER_PAYMENT",
                        notes = sp.notes,
                        createdAt = sp.createdAt, originalCurrency = sp.currency, originalAmountMinor = sp.amountMinor, exchangeRate = sp.exchangeRateToBdt
                    )
                )
            }
        }

        // 6. Expenses
        val expenses = if (filterState.selectedCustomerId != null || filterState.selectedSupplierId != null) emptyList() else expenseDao.getAllExpensesSync()
        for (exp in expenses) {
            if (exp.voucherDate in fromMillis..toMillis) {
                allList.add(
                    UnifiedTransactionItem(
                        id = exp.id,
                        referenceNumber = exp.voucherNumber,
                        date = exp.voucherDate,
                        category = ReportCategory.EXPENSE,
                        primaryEntityName = exp.category,
                        particulars = exp.descriptionEn.ifBlank { exp.descriptionBn },
                        amountMinor = exp.bdtAmountMinor,
                        isInflow = false,
                        paymentMethod = exp.currency,
                        status = if (exp.isReversed) "REVERSED" else "POSTED",
                        statusColorHex = if (exp.isReversed) 0xFFEF4444L else 0xFFF97316L,
                        rawType = "EXPENSE",
                        notes = exp.notes,
                        createdAt = exp.createdAt, accountingPosted = exp.isPosted, warehouseId = exp.warehouseId
                    )
                )
            }
        }

        // 7. Stock Movements
        val movements = if (filterState.selectedCustomerId != null || filterState.selectedSupplierId != null) emptyList() else stockMovementDao.getAllMovementsWithDetailsSync()
        for (mv in movements) {
            if (mv.createdAt in fromMillis..toMillis) {
                val cat = when (mv.movementType) {
                    "PURCHASE_RECEIPT", "OPENING_STOCK" -> ReportCategory.INVENTORY
                    "SALE_DISPATCH" -> ReportCategory.DELIVERY
                    "SALE_RETURN" -> ReportCategory.SALES_RETURN
                    "PURCHASE_RETURN" -> ReportCategory.PURCHASE_RETURN
                    "STOCK_ADJUSTMENT_IN", "STOCK_ADJUSTMENT_OUT" -> ReportCategory.STOCK_ADJUSTMENT
                    "TRANSFER_IN", "TRANSFER_OUT" -> ReportCategory.STOCK_TRANSFER
                    else -> ReportCategory.INVENTORY
                }
                val costImpact = if (mv.totalCostImpactMinor != 0L) mv.totalCostImpactMinor else (mv.quantity.toLong() * mv.unitCostMinor)
                allList.add(
                    UnifiedTransactionItem(
                        id = mv.id,
                        referenceNumber = mv.movementNumber,
                        date = mv.createdAt,
                        category = cat,
                        primaryEntityName = mv.productNameEn,
                        particulars = "${mv.movementType} | Qty: ${mv.quantity} ${mv.baseSku}",
                        amountMinor = kotlin.math.abs(costImpact),
                        isInflow = mv.quantity >= 0,
                        paymentMethod = mv.warehouseName,
                        status = mv.movementType,
                        statusColorHex = if (mv.quantity >= 0) 0xFF10B981L else 0xFFF59E0BL,
                        warehouseName = mv.warehouseName,
                        rawType = "STOCK_MOVEMENT",
                        notes = mv.notes,
                        createdBy = mv.operatorName,
                        createdAt = mv.createdAt, warehouseId = mv.warehouseId,
                        productIds = setOf(mv.productId),
                        categoryIds = setOfNotNull(productCategoryByProductId[mv.productId])
                    )
                )
            }
        }

        // 8. Stock Transfers
        val transfers = if (filterState.selectedCustomerId != null || filterState.selectedSupplierId != null) emptyList() else stockTransferDao.getAllTransfersWithDetailsSync()
        for (trf in transfers) {
            if (trf.transferDate in fromMillis..toMillis) {
                allList.add(
                    UnifiedTransactionItem(
                        id = trf.id,
                        referenceNumber = trf.transferNumber,
                        date = trf.transferDate,
                        category = ReportCategory.STOCK_TRANSFER,
                        primaryEntityName = "${trf.sourceWarehouseName} ➔ ${trf.destinationWarehouseName}",
                        particulars = "${trf.productNameEn} (Qty: ${trf.quantity})",
                        amountMinor = 0L,
                        isInflow = false,
                        paymentMethod = "INTERNAL_TRANSFER",
                        status = trf.status,
                        statusColorHex = 0xFF8B5CF6L,
                        warehouseName = trf.destinationWarehouseName,
                        rawType = "STOCK_TRANSFER",
                        notes = trf.notes,
                        createdAt = trf.createdAt, warehouseId = trf.destinationWarehouseId,
                        productIds = setOf(trf.productId),
                        categoryIds = setOfNotNull(productCategoryByProductId[trf.productId])
                    )
                )
            }
        }

        val originals = allList.associateBy { it.rawType to it.id }
        for (invoice in invoices.filter { it.isPosted && it.status == "CANCELLED" }) {
            originals["INVOICE" to invoice.id]?.let { row ->
                allList.add(row.copy(id = "${row.id}-cancel", sourceId = row.id, rawType = "INVOICE_REVERSAL",
                    date = invoice.cancelledAt ?: invoice.updatedAt, createdAt = invoice.cancelledAt ?: invoice.updatedAt,
                    effectSign = -1, status = "REVERSED", particulars = "Invoice cancellation / বাতিল", notes = invoice.cancellationReason))
            }
        }
        for (payment in customerPayments.filter { it.isPosted && it.isVoided }) {
            originals["PAYMENT" to payment.id]?.let { row ->
                val date = payment.voidedAt ?: (payment.paymentDate + 1)
                allList.add(row.copy(id = "${row.id}-void", sourceId = row.id, rawType = "PAYMENT_REVERSAL",
                    date = date, createdAt = date, effectSign = -1, status = "REVERSED", particulars = "Payment reversal / রিভার্সাল", notes = payment.voidReason))
            }
        }
        for (expense in expenses.filter { it.isPosted && it.isReversed }) {
            val date = expense.reversalJournalEntryId?.let { database.accountingDao().getJournalEntryById(it)?.entryDate }
            originals["EXPENSE" to expense.id]?.let { row ->
                if (date != null) allList.add(row.copy(id = "${row.id}-reverse", sourceId = row.id, rawType = "EXPENSE_REVERSAL",
                    date = date, createdAt = date, effectSign = -1, status = "REVERSED", particulars = "Expense reversal / ব্যয় রিভার্সাল"))
                else { val index = allList.indexOf(row); allList[index] = row.copy(accountingPosted = false) }
            }
        }
        // Preserve imported/legacy opening balances without writing or inventing new transactions.
        if (filterState.selectedSupplierId == null) {
            for (adjustment in database.accountingDao().getCustomerLedgerAdjustments(filterState.selectedCustomerId)) {
                val customer = adjustment.referenceId?.let { customerDao.getCustomerById(it) } ?: continue
                val delta = com.pocketdev.advance_erp.core.engine.CustomerLedgerAdjustment.delta(adjustment)
                allList.add(UnifiedTransactionItem(
                    id = adjustment.id, referenceNumber = adjustment.entryNumber, date = adjustment.entryDate,
                    category = ReportCategory.CUSTOMER_LEDGER, primaryEntityName = customer.name,
                    primaryEntityId = customer.id, particulars = adjustment.descriptionEn,
                    amountMinor = kotlin.math.abs(delta), isInflow = delta > 0L, status = "POSTED",
                    rawType = "CUSTOMER_ADJUSTMENT", effectSign = if (delta >= 0L) 1 else -1,
                    createdAt = adjustment.createdAt
                ))
            }
        }
        if (filterState.selectedCustomerId == null) {
            for (adjustment in database.accountingDao().getSupplierLedgerAdjustments(filterState.selectedSupplierId)) {
                val supplier = adjustment.referenceId?.let { suppliers[it] ?: supplierDao.getSupplierById(it) } ?: continue
                val delta = com.pocketdev.advance_erp.core.engine.SupplierLedgerAdjustment.delta(adjustment)
                allList.add(UnifiedTransactionItem(
                    id = adjustment.id, referenceNumber = adjustment.entryNumber, date = adjustment.entryDate,
                    category = ReportCategory.SUPPLIER_LEDGER, primaryEntityName = supplier.companyName,
                    primaryEntityId = supplier.id, particulars = adjustment.descriptionEn,
                    amountMinor = kotlin.math.abs(delta), isInflow = false, status = "POSTED",
                    rawType = "SUPPLIER_ADJUSTMENT", effectSign = if (delta >= 0L) 1 else -1,
                    createdAt = adjustment.createdAt
                ))
            }
        }
        val customerNet = allList.filter { LedgerReportEngine.isCustomer(it) }.groupBy { it.primaryEntityId }
            .mapValues { (_, rows) -> rows.sumOf { LedgerReportEngine.delta(it) } }
        val supplierNet = allList.filter { LedgerReportEngine.isSupplier(it) }.groupBy { it.primaryEntityId }
            .mapValues { (_, rows) -> rows.sumOf { LedgerReportEngine.delta(it) } }
        if (filterState.selectedSupplierId == null) {
            val customers = filterState.selectedCustomerId?.let { listOfNotNull(customerDao.getCustomerById(it)) } ?: customerDao.getLedgerCustomersSync()
            customers.forEach { customer ->
                val residual = LedgerReportEngine.openingAdjustment(customer.outstandingBalanceMinor, customerNet[customer.id] ?: 0)
                if (residual != 0L) allList.add(UnifiedTransactionItem("opening-${customer.id}", "OPENING", 0,
                    ReportCategory.CUSTOMER_LEDGER, customer.name, customer.id, "Opening / legacy balance (পূর্বের ব্যালেন্স)",
                    kotlin.math.abs(residual), isInflow = residual > 0, status = "OPENING", rawType = "OPENING_CUSTOMER", effectSign = if (residual > 0) 1 else -1))
            }
        }
        if (filterState.selectedCustomerId == null) suppliers.values.filter { filterState.selectedSupplierId == null || it.id == filterState.selectedSupplierId }.forEach { supplier ->
            val residual = LedgerReportEngine.openingAdjustment(supplier.outstandingPayableMinor, supplierNet[supplier.id] ?: 0)
            if (residual != 0L) allList.add(UnifiedTransactionItem("opening-${supplier.id}", "OPENING", 0,
                ReportCategory.SUPPLIER_LEDGER, supplier.companyName, supplier.id, "Opening / legacy balance (পূর্বের ব্যালেন্স)",
                kotlin.math.abs(residual), isInflow = residual > 0, status = "OPENING", rawType = "OPENING_SUPPLIER", effectSign = if (residual > 0) 1 else -1))
        }
        return allList
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun getLedgerReport(filterState: ControlCenterFilterState): Flow<LedgerReport> = callbackFlow {
        val observer = object : InvalidationTracker.Observer("sales_invoices", "customer_payments", "sales_returns", "purchase_orders", "supplier_payments", "expense_vouchers", "stock_movements", "stock_transfers", "customers", "suppliers", "warehouses", "payment_allocations", "journal_entries") {
            override fun onInvalidated(tables: Set<String>) { trySend(Unit) }
        }
        database.invalidationTracker.addObserver(observer)
        trySend(Unit)
        awaitClose { database.invalidationTracker.removeObserver(observer) }
    }.conflate().map {
        database.withTransaction {
            val rows = loadLedgerRows(filterState)
            val party = filterState.selectedCustomerId?.let { customerDao.getCustomerById(it)?.let { p -> LedgerParty(p.id, p.name, p.customerCode, "CUSTOMER", p.phone, p.address) } }
                ?: filterState.selectedSupplierId?.let { supplierDao.getSupplierById(it)?.let { p -> LedgerParty(p.id, p.companyName, p.supplierCode, "SUPPLIER", p.phone, p.address) } }
            LedgerReportEngine.build(rows, filterState, party)
        }
    }.flowOn(Dispatchers.IO)

    override fun getTransactions(filterState: ControlCenterFilterState): Flow<List<UnifiedTransactionItem>> = getLedgerReport(filterState).map { it.transactions }
    override fun getSummaryDashboard(filterState: ControlCenterFilterState): Flow<BusinessSummaryDashboardData> = getLedgerReport(filterState).map { it.summary }

    override suspend fun getTransactionDetails(rawType: String, id: String): TransactionDetailRecord? {
        return when (rawType) {
            "CUSTOMER_ADJUSTMENT" -> {
                val entry = database.accountingDao().getJournalEntryById(id) ?: return null
                if (entry.referenceType !in setOf(
                        com.pocketdev.advance_erp.core.engine.CustomerLedgerAdjustment.DEBIT,
                        com.pocketdev.advance_erp.core.engine.CustomerLedgerAdjustment.CREDIT)) return null
                val customer = entry.referenceId?.let { customerDao.getCustomerById(it) } ?: return null
                val delta = com.pocketdev.advance_erp.core.engine.CustomerLedgerAdjustment.delta(entry)
                TransactionDetailRecord(transaction = UnifiedTransactionItem(
                    id = entry.id, referenceNumber = entry.entryNumber, date = entry.entryDate,
                    category = ReportCategory.CUSTOMER_LEDGER, primaryEntityName = customer.name,
                    primaryEntityId = customer.id, particulars = entry.descriptionEn,
                    amountMinor = kotlin.math.abs(delta), isInflow = delta > 0L,
                    status = if (entry.isReversed) "REVERSED" else if (entry.isPosted) "POSTED" else "DRAFT",
                    rawType = "CUSTOMER_ADJUSTMENT", effectSign = if (delta >= 0L) 1 else -1,
                    accountingPosted = entry.isPosted && !entry.isReversed, createdAt = entry.createdAt
                ), referenceDocument = entry.entryNumber, auditTrailInfo = entry.descriptionEn)
            }
            "SUPPLIER_ADJUSTMENT" -> {
                val entry = database.accountingDao().getJournalEntryById(id) ?: return null
                if (entry.referenceType !in setOf(
                        com.pocketdev.advance_erp.core.engine.SupplierLedgerAdjustment.CREDIT,
                        com.pocketdev.advance_erp.core.engine.SupplierLedgerAdjustment.DEBIT)) return null
                val supplier = entry.referenceId?.let { supplierDao.getSupplierById(it) } ?: return null
                val delta = com.pocketdev.advance_erp.core.engine.SupplierLedgerAdjustment.delta(entry)
                TransactionDetailRecord(transaction = UnifiedTransactionItem(
                    id = entry.id, referenceNumber = entry.entryNumber, date = entry.entryDate,
                    category = ReportCategory.SUPPLIER_LEDGER, primaryEntityName = supplier.companyName,
                    primaryEntityId = supplier.id, particulars = entry.descriptionEn,
                    amountMinor = kotlin.math.abs(delta), isInflow = false,
                    status = if (entry.isReversed) "REVERSED" else if (entry.isPosted) "POSTED" else "DRAFT",
                    rawType = "SUPPLIER_ADJUSTMENT", effectSign = if (delta >= 0L) 1 else -1,
                    accountingPosted = entry.isPosted && !entry.isReversed, createdAt = entry.createdAt
                ), referenceDocument = entry.entryNumber, auditTrailInfo = entry.descriptionEn)
            }
            "INVOICE" -> {
                val inv = salesDao.getInvoiceById(id) ?: return null
                val items = salesDao.getItemsForInvoice(id)
                val lineItems = items.map {
                    LineItemDetail(
                        productName = it.productNameEn,
                        sku = it.sku,
                        variantInfo = null,
                        quantity = it.quantity,
                        unitPriceMinor = it.unitPriceMinor,
                        totalAmountMinor = it.lineTotalMinor
                    )
                }
                val tx = UnifiedTransactionItem(
                    id = inv.id,
                    referenceNumber = inv.invoiceNumber,
                    date = inv.invoiceDate,
                    category = ReportCategory.SALES,
                    primaryEntityName = inv.customerName,
                    primaryEntityId = inv.customerId,
                    particulars = "Sales Invoice (${inv.status})",
                    amountMinor = inv.totalAmountMinor,
                    secondaryAmountMinor = inv.dueAmountMinor,
                    isInflow = true,
                    status = inv.status,
                    warehouseName = inv.warehouseName,
                    rawType = "INVOICE",
                    notes = inv.notes
                )
                TransactionDetailRecord(
                    transaction = tx,
                    items = lineItems,
                    subtotalMinor = inv.subtotalMinor,
                    discountMinor = inv.discountMinor,
                    taxMinor = inv.taxMinor,
                    paidMinor = inv.paidAmountMinor,
                    dueMinor = inv.dueAmountMinor,
                    contactPhone = inv.customerPhone,
                    referenceDocument = inv.invoiceNumber,
                    auditTrailInfo = "Invoice created at ${java.util.Date(inv.createdAt)}. Updated at ${java.util.Date(inv.updatedAt)}."
                )
            }
            "PURCHASE" -> {
                val po = purchaseOrderDao.getOrderById(id) ?: return null
                val items = purchaseOrderDao.getItemsForOrderSync(id)
                val lineItems = items.map {
                    LineItemDetail(
                        productName = "Product Item (${it.productId.take(8)})",
                        sku = it.variantId ?: "SKU-${it.productId.take(6)}",
                        quantity = it.quantity,
                        unitPriceMinor = it.unitPriceMinor,
                        totalAmountMinor = it.totalAmountMinor
                    )
                }
                val supplier = supplierDao.getSupplierById(po.supplierId)
                val tx = UnifiedTransactionItem(
                    id = po.id,
                    referenceNumber = po.poNumber,
                    date = po.orderDate,
                    category = if (po.currency != "BDT") ReportCategory.IMPORT else ReportCategory.PURCHASE,
                    primaryEntityName = supplier?.companyName ?: "Supplier",
                    primaryEntityId = po.supplierId,
                    particulars = "Purchase Order [${po.currency}] - Status: ${po.status}",
                    amountMinor = po.totalBdtAmountMinor,
                    secondaryAmountMinor = po.totalBdtAmountMinor - po.paidBdtAmountMinor,
                    isInflow = false,
                    paymentMethod = po.currency,
                    status = po.status,
                    rawType = "PURCHASE",
                    notes = po.notes
                )
                TransactionDetailRecord(
                    transaction = tx,
                    items = lineItems,
                    subtotalMinor = po.totalBdtAmountMinor,
                    paidMinor = po.paidBdtAmountMinor,
                    dueMinor = (po.totalBdtAmountMinor - po.paidBdtAmountMinor).coerceAtLeast(0L),
                    contactPhone = supplier?.phone,
                    contactAddress = supplier?.address,
                    referenceDocument = po.poNumber,
                    auditTrailInfo = "Purchase Order created: ${java.util.Date(po.createdAt)}. Currency: ${po.currency} at rate ${po.exchangeRateToBdt} BDT."
                )
            }
            "PAYMENT" -> {
                val pmt = customerPaymentDao.getPaymentById(id) ?: return null
                val allocations = customerPaymentDao.getAllocationsForPayment(id)
                val lineItems = allocations.map {
                    LineItemDetail(
                        referenceType = "INVOICE", referenceId = it.invoiceId,
                        productName = "Allocation for Invoice ${it.invoiceNumber}",
                        sku = it.invoiceId.take(8),
                        quantity = 1,
                        unitPriceMinor = it.amountMinor,
                        totalAmountMinor = it.amountMinor
                    )
                }
                val tx = UnifiedTransactionItem(
                    id = pmt.id,
                    referenceNumber = pmt.paymentNumber,
                    date = pmt.paymentDate,
                    category = ReportCategory.RECEIPT,
                    primaryEntityName = pmt.customerName,
                    primaryEntityId = pmt.customerId,
                    particulars = pmt.referenceNote ?: "Customer Receipt",
                    amountMinor = pmt.amountMinor,
                    secondaryAmountMinor = pmt.unallocatedCreditMinor,
                    isInflow = true,
                    paymentMethod = pmt.paymentMethod,
                    status = if (pmt.isVoided) "VOIDED" else "POSTED",
                    rawType = "PAYMENT",
                    notes = pmt.referenceNote
                )
                TransactionDetailRecord(
                    transaction = tx,
                    items = lineItems,
                    subtotalMinor = pmt.amountMinor,
                    paidMinor = pmt.amountMinor - pmt.unallocatedCreditMinor,
                    dueMinor = pmt.unallocatedCreditMinor,
                    referenceDocument = pmt.paymentNumber,
                    auditTrailInfo = "Payment recorded: ${java.util.Date(pmt.createdAt)}. Allocated: ${java.math.BigDecimal.valueOf(pmt.allocatedAmountMinor,2)} BDT; unallocated: ${java.math.BigDecimal.valueOf(pmt.unallocatedCreditMinor,2)} BDT. Void: ${pmt.isVoided}; date: ${pmt.voidedAt?.let { java.util.Date(it) }}; reason: ${pmt.voidReason.orEmpty()}; by: ${pmt.voidedBy.orEmpty()}."
                )
            }
            "SUPPLIER_PAYMENT" -> {
                val payment = supplierPaymentDao.getLedgerPaymentById(id) ?: return null
                val supplier = supplierDao.getSupplierById(payment.supplierId)
                val order = payment.purchaseOrderId?.let { purchaseOrderDao.getOrderById(it) }
                val row = loadLedgerRows(ControlCenterFilterState(selectedSupplierId = payment.supplierId)).first { it.rawType == "SUPPLIER_PAYMENT" && it.id == id }
                TransactionDetailRecord(row,
                    items = if (order == null) emptyList() else listOf(LineItemDetail("PO ${order.poNumber}", order.poNumber, quantity=1,
                        unitPriceMinor=payment.bdtAmountMinor,totalAmountMinor=payment.bdtAmountMinor,referenceType="PURCHASE",referenceId=order.id)),
                    paidMinor=payment.bdtAmountMinor,contactPhone=supplier?.phone, contactAddress=supplier?.address,
                    referenceDocument=payment.referenceNumber,
                    auditTrailInfo="${java.util.Date(payment.createdAt)} • ${payment.currency} ${java.math.BigDecimal.valueOf(payment.amountMinor,2)} @ ${payment.exchangeRateToBdt} • ${payment.paymentAccount} • ${payment.notes.orEmpty()}")
            }
            "RETURN" -> {
                val ret = salesReturnDao.getReturnById(id) ?: return null
                val items = salesReturnDao.getItemsForReturn(id)
                val lineItems = items.map {
                    LineItemDetail(
                        productName = it.productNameEn,
                        sku = it.variantId ?: it.productId.take(8),
                        variantInfo = null,
                        quantity = it.returnQuantity,
                        unitPriceMinor = it.unitPriceMinor,
                        totalAmountMinor = it.returnTotalMinor
                    )
                }
                val tx = UnifiedTransactionItem(
                    id = ret.id,
                    referenceNumber = ret.returnNumber,
                    date = ret.returnDate,
                    category = ReportCategory.SALES_RETURN,
                    primaryEntityName = ret.customerName,
                    primaryEntityId = ret.customerId,
                    particulars = ret.reason ?: "Return against ${ret.invoiceNumber}",
                    amountMinor = ret.totalReturnAmountMinor,
                    secondaryAmountMinor = ret.totalCogsReversalMinor,
                    isInflow = false,
                    paymentMethod = ret.refundOption,
                    status = "RETURNED",
                    rawType = "RETURN",
                    notes = ret.reason
                )
                TransactionDetailRecord(
                    transaction = tx,
                    items = lineItems,
                    subtotalMinor = ret.totalReturnAmountMinor,
                    referenceDocument = ret.invoiceNumber,
                    auditTrailInfo = "Sales Return registered: ${java.util.Date(ret.createdAt)}. Reason: ${ret.reason ?: "N/A"}."
                )
            }
            "EXPENSE" -> {
                val exp = expenseDao.getExpenseById(id) ?: return null
                val tx = UnifiedTransactionItem(
                    id = exp.id,
                    referenceNumber = exp.voucherNumber,
                    date = exp.voucherDate,
                    category = ReportCategory.EXPENSE,
                    primaryEntityName = exp.category,
                    particulars = exp.descriptionEn.ifBlank { exp.descriptionBn },
                    amountMinor = exp.bdtAmountMinor,
                    isInflow = false,
                    paymentMethod = exp.currency,
                    status = if (exp.isReversed) "REVERSED" else "POSTED",
                    rawType = "EXPENSE",
                    notes = exp.notes
                )
                TransactionDetailRecord(
                    transaction = tx,
                    subtotalMinor = exp.bdtAmountMinor,
                    referenceDocument = exp.voucherNumber,
                    auditTrailInfo = "Expense voucher created: ${java.util.Date(exp.createdAt)}. Payment account: ${exp.paymentAccountId}."
                )
            }
            else -> null
        }
    }

    override suspend fun getCustomerLedgerStatement(customerId: String, fromDate: Long, toDate: Long): PartyLedgerStatement? =
        getLedgerReport(ControlCenterFilterState(dateRangeOption = com.pocketdev.advance_erp.domain.model.DateRangeOption.CUSTOM,
            customStartDate = fromDate, customEndDate = toDate, category = ReportCategory.CUSTOMER_LEDGER, selectedCustomerId = customerId)).first().statement

    override suspend fun getSupplierLedgerStatement(supplierId: String, fromDate: Long, toDate: Long): PartyLedgerStatement? =
        getLedgerReport(ControlCenterFilterState(dateRangeOption = com.pocketdev.advance_erp.domain.model.DateRangeOption.CUSTOM,
            customStartDate = fromDate, customEndDate = toDate, category = ReportCategory.SUPPLIER_LEDGER, selectedSupplierId = supplierId)).first().statement
}
