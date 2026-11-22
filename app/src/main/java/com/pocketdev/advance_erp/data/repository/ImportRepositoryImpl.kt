package com.pocketdev.advance_erp.data.repository

import androidx.room.withTransaction
import com.pocketdev.advance_erp.core.database.AdvanceErpDatabase
import com.pocketdev.advance_erp.core.util.MovementNumberGenerator
import com.pocketdev.advance_erp.core.engine.AllocationItemInput
import com.pocketdev.advance_erp.core.engine.AllocationMethod
import com.pocketdev.advance_erp.core.engine.CbmCalculator
import com.pocketdev.advance_erp.core.engine.LandedCostAllocationEngine
import com.pocketdev.advance_erp.core.engine.LandingCostEngine
import com.pocketdev.advance_erp.core.engine.CartonWeightAllocationEngine
import com.pocketdev.advance_erp.core.engine.MixedCartonLineInput
import com.pocketdev.advance_erp.core.engine.PoPackingEngine
import com.pocketdev.advance_erp.core.util.PoNotesMetadataParser
import com.pocketdev.advance_erp.domain.repository.PoMixedCartonDraft
import com.pocketdev.advance_erp.data.dao.CustomerDao
import com.pocketdev.advance_erp.data.dao.ImportCostDao
import com.pocketdev.advance_erp.data.dao.ImportShipmentDao
import com.pocketdev.advance_erp.data.dao.InventoryCostLayerDao
import com.pocketdev.advance_erp.data.dao.MinimalImportDao
import com.pocketdev.advance_erp.data.dao.ProductDao
import com.pocketdev.advance_erp.data.dao.ProductVariantDao
import com.pocketdev.advance_erp.data.dao.PurchaseOrderDao
import com.pocketdev.advance_erp.data.dao.StockBalanceDao
import com.pocketdev.advance_erp.data.dao.StockMovementDao
import com.pocketdev.advance_erp.data.dao.SupplierDao
import com.pocketdev.advance_erp.data.dao.SupplierPaymentDao
import com.pocketdev.advance_erp.data.dao.WarehouseReceiptDao
import com.pocketdev.advance_erp.data.entity.AdditionalCostCategory
import com.pocketdev.advance_erp.data.entity.AdditionalProductCost
import com.pocketdev.advance_erp.data.entity.AuditLog
import com.pocketdev.advance_erp.data.entity.CostCategory
import com.pocketdev.advance_erp.data.entity.DamageRecord
import com.pocketdev.advance_erp.data.entity.ExpenseCategory
import com.pocketdev.advance_erp.data.entity.ExpenseVoucher
import com.pocketdev.advance_erp.data.entity.ImportCostItem
import com.pocketdev.advance_erp.data.entity.ImportShipment
import com.pocketdev.advance_erp.data.entity.InventoryCostAdjustment
import com.pocketdev.advance_erp.data.entity.InventoryCostLayer
import com.pocketdev.advance_erp.data.entity.LandedCostAllocation
import com.pocketdev.advance_erp.data.entity.LandedCostSnapshot
import com.pocketdev.advance_erp.data.entity.PaymentMethod
import com.pocketdev.advance_erp.data.entity.ProductCategory
import com.pocketdev.advance_erp.data.entity.PurchaseCompletionRecord
import com.pocketdev.advance_erp.data.entity.PurchaseOrder
import com.pocketdev.advance_erp.data.entity.PurchaseOrderItem
import com.pocketdev.advance_erp.data.entity.PurchaseOrderStatus
import com.pocketdev.advance_erp.data.entity.ReceiptStatus
import com.pocketdev.advance_erp.data.entity.ShipmentCarton
import com.pocketdev.advance_erp.data.entity.ShipmentFreightRate
import com.pocketdev.advance_erp.data.entity.ShipmentStatus
import com.pocketdev.advance_erp.data.entity.ShipmentSupplier
import com.pocketdev.advance_erp.data.entity.ShippingMethod
import com.pocketdev.advance_erp.data.entity.ShortageRecord
import com.pocketdev.advance_erp.data.entity.StockBalance
import com.pocketdev.advance_erp.data.entity.StockMovement
import com.pocketdev.advance_erp.data.entity.StockMovementType
import com.pocketdev.advance_erp.data.entity.SupplierPayment
import com.pocketdev.advance_erp.data.entity.WarehouseReceipt
import com.pocketdev.advance_erp.data.entity.WarehouseReceiptItem
import com.pocketdev.advance_erp.domain.repository.AccountingRepository
import com.pocketdev.advance_erp.domain.repository.CostAdjustmentResult
import com.pocketdev.advance_erp.domain.repository.ImportRepository
import com.pocketdev.advance_erp.domain.repository.JournalLineDraft
import com.pocketdev.advance_erp.domain.repository.MixedCartonLineDraft
import com.pocketdev.advance_erp.domain.repository.PoRemainingItem
import com.pocketdev.advance_erp.domain.repository.PurchaseOrderItemDraft
import com.pocketdev.advance_erp.domain.repository.ReceiptItemDraft
import com.pocketdev.advance_erp.domain.repository.ShipmentCartonDraft
import com.pocketdev.advance_erp.domain.repository.ShipmentSupplierDraft
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImportRepositoryImpl @Inject constructor(
    private val database: AdvanceErpDatabase,
    private val purchaseOrderDao: PurchaseOrderDao,
    private val supplierPaymentDao: SupplierPaymentDao,
    private val importShipmentDao: ImportShipmentDao,
    private val importCostDao: ImportCostDao,
    private val warehouseReceiptDao: WarehouseReceiptDao,
    private val inventoryCostLayerDao: InventoryCostLayerDao,
    private val minimalImportDao: MinimalImportDao,
    private val supplierDao: SupplierDao,
    private val productDao: ProductDao,
    private val productVariantDao: ProductVariantDao,
    private val stockBalanceDao: StockBalanceDao,
    private val stockMovementDao: StockMovementDao,
    private val accountingRepository: AccountingRepository
) : ImportRepository {

    // ==========================================
    // 1. Purchase Orders
    // ==========================================

    override fun getAllPurchaseOrders(): Flow<List<PurchaseOrder>> = purchaseOrderDao.getAllOrders()

    override suspend fun getPurchaseOrderById(id: String): PurchaseOrder? = purchaseOrderDao.getOrderById(id)
    override fun getPurchaseOrderByIdFlow(id: String): Flow<PurchaseOrder?> = purchaseOrderDao.getOrderByIdFlow(id)

    override fun getItemsForOrder(orderId: String): Flow<List<PurchaseOrderItem>> = purchaseOrderDao.getItemsForOrder(orderId)

    override suspend fun getItemsForOrderSync(orderId: String): List<PurchaseOrderItem> =
        purchaseOrderDao.getItemsForOrderSync(orderId)

    override suspend fun getPoRemainingItems(orderId: String): List<PoRemainingItem> {
        val items = purchaseOrderDao.getItemsForOrderSync(orderId)
        val existingShipments = importShipmentDao.getShipmentsByOrderIdSync(orderId)
            .filter {
                it.status != ShipmentStatus.CANCELLED.name &&
                    !(it.shipmentNumber.startsWith("BATCH-") && it.status == ShipmentStatus.DRAFT.name)
            }
        val existingCartons = existingShipments.flatMap { importShipmentDao.getCartonsForShipmentSync(it.id) }
        val shippedByProduct = existingCartons.groupBy { it.productId to it.variantId }
            .mapValues { (_, cList) -> cList.sumOf { it.quantity } }

        return items.map { item ->
            val shipped = shippedByProduct[item.productId to item.variantId] ?: 0
            val remaining = (item.quantity - shipped).coerceAtLeast(0)
            val product = productDao.getProductById(item.productId)
            PoRemainingItem(
                poItem = item,
                product = product,
                orderedQty = item.quantity,
                shippedQty = shipped,
                remainingQty = remaining
            )
        }
    }

    override suspend fun updatePurchaseOrderMetadata(orderId: String, notes: String?, attachments: String?): Unit = database.withTransaction {
        val order = purchaseOrderDao.getOrderById(orderId) ?: error("Purchase order not found")
        require(order.status != PurchaseOrderStatus.CANCELLED.name) { "Cancelled orders cannot be edited" }
        purchaseOrderDao.updateOrder(order.copy(notes = notes, attachments = attachments?.takeIf { it.isNotBlank() }, updatedAt = System.currentTimeMillis()))
    }

    override suspend fun savePurchaseOrderDraft(
        orderId: String?, supplierId: String, currency: String, exchangeRate: BigDecimal,
        items: List<PurchaseOrderItemDraft>, notes: String?, status: PurchaseOrderStatus,
        cartons: List<PoMixedCartonDraft>, chinaDeliveryChargeMinor: Long,
        categoryFreightRatesBdtMinor: Map<String, Long>, handlingCostMinor: Long,
        sampleCostMinor: Long, attachments: String?, orderDateMillis: Long,
        expectedArrivalDateMillis: Long?, shippingMethod: String
    ): String = database.withTransaction {
        val id = orderId ?: createPurchaseOrder(
            supplierId, currency, exchangeRate, items, notes, status, attachments,
            orderDateMillis, expectedArrivalDateMillis, shippingMethod
        )
        updatePurchaseOrder(id, supplierId, currency, exchangeRate, items, notes, status, cartons,
            chinaDeliveryChargeMinor, categoryFreightRatesBdtMinor, handlingCostMinor, sampleCostMinor, attachments,
            orderDateMillis, expectedArrivalDateMillis, updateSchedule = true, shippingMethod = shippingMethod).getOrThrow()
        id
    }

    override suspend fun createPurchaseOrder(
        supplierId: String,
        currency: String,
        exchangeRate: BigDecimal,
        items: List<PurchaseOrderItemDraft>,
        notes: String?,
        status: PurchaseOrderStatus,
        attachments: String?,
        orderDateMillis: Long,
        expectedArrivalDateMillis: Long?,
        shippingMethod: String
    ): String {
        require(items.isNotEmpty()) { "Purchase order must contain at least 1 item" }
        require(exchangeRate > BigDecimal.ZERO) { "Exchange rate must be positive: $exchangeRate" }

        val orderCount = purchaseOrderDao.getOrderCount()
        val year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        var nextNumber = orderCount + 1
        var poNumber: String
        do {
            poNumber = String.format(java.util.Locale.ROOT, "PO-%d-%03d", year, nextNumber++)
        } while (purchaseOrderDao.getOrderByNumber(poNumber) != null)
        val orderId = "po-${UUID.randomUUID()}"

        var totalOriginalMinor = 0L
        var totalBdtMinor = 0L

        val poItems = items.map { draft ->
            require(draft.quantity > 0) { "Quantity must be greater than 0" }
            require(draft.unitPriceMinor > 0) { "Unit price must be greater than 0" }

            val lineOriginalMinor = Math.multiplyExact(draft.quantity.toLong(), draft.unitPriceMinor)
            val lineBdtMinor = BigDecimal(lineOriginalMinor)
                .multiply(exchangeRate)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact()

            totalOriginalMinor = Math.addExact(totalOriginalMinor, lineOriginalMinor)
            totalBdtMinor = Math.addExact(totalBdtMinor, lineBdtMinor)

            PurchaseOrderItem(
                id = "poi-${UUID.randomUUID()}",
                purchaseOrderId = orderId,
                productId = draft.productId,
                variantId = draft.variantId,
                quantity = draft.quantity,
                unitPriceMinor = draft.unitPriceMinor,
                totalAmountMinor = lineOriginalMinor,
                totalBdtAmountMinor = lineBdtMinor,
                receivedQuantity = 0,
                cartonQuantity = draft.cartonQuantity,
                pcsPerCarton = draft.pcsPerCarton,
                grossWeightKg = draft.grossWeightKg.toPlainString(),
                netWeightKg = draft.netWeightKg.toPlainString(),
                cbm = draft.cbm.toPlainString()
            )
        }

        val totalCartonsSum = poItems.sumOf { it.cartonQuantity }
        val totalGrossWeightSum = poItems.fold(BigDecimal.ZERO) { acc, it -> acc.add(BigDecimal(it.grossWeightKg.ifBlank { "0" })) }
        val totalNetWeightSum = poItems.fold(BigDecimal.ZERO) { acc, it -> acc.add(BigDecimal(it.netWeightKg.ifBlank { "0" })) }
        val totalCbmSum = poItems.fold(BigDecimal.ZERO) { acc, it -> acc.add(BigDecimal(it.cbm.ifBlank { "0" })) }

        val order = PurchaseOrder(
            id = orderId,
            poNumber = poNumber,
            supplierId = supplierId,
            status = status.name,
            currency = currency,
            exchangeRateToBdt = exchangeRate.toPlainString(),
            totalAmountMinor = totalOriginalMinor,
            totalBdtAmountMinor = totalBdtMinor,
            paidAmountMinor = 0L,
            paidBdtAmountMinor = 0L,
            orderDate = orderDateMillis,
            expectedArrivalDate = expectedArrivalDateMillis,
            notes = notes,
            attachments = attachments?.takeIf { it.isNotBlank() },
            isActualCostFinalized = false,
            totalCartons = totalCartonsSum,
            totalGrossWeightKg = totalGrossWeightSum.toPlainString(),
            totalNetWeightKg = totalNetWeightSum.toPlainString(),
            totalCbm = totalCbmSum.toPlainString(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        // IMPORTANT: Creating a PO does NOT increase inventory stock! Stock increases only on warehouse receiving.
        purchaseOrderDao.saveOrderWithItems(order, poItems)

        // One Purchase = One Batch: Automatically create linked batch shipment
        val supplier = supplierDao.getSupplierById(supplierId)
        val isLocalOrder = currency.equals("BDT", ignoreCase = true)
        val batchNumber = "BATCH-$poNumber"
        val batchShipment = ImportShipment(
            id = "shp-${UUID.randomUUID()}",
            shipmentNumber = batchNumber,
            supplierId = supplierId,
            purchaseOrderId = orderId,
            shippingMethod = if (isLocalOrder) "LOCAL" else shippingMethod,
            origin = if (isLocalOrder) {
                supplier?.address?.takeIf { it.isNotBlank() } ?: "Local Supplier, Bangladesh"
            } else {
                "Guangzhou, China"
            },
            destination = if (isLocalOrder) "Main Warehouse, Bangladesh" else "Chattogram Port, Bangladesh",
            status = ShipmentStatus.DRAFT.name,
            totalCartons = totalCartonsSum,
            totalQuantity = poItems.sumOf { it.quantity },
            totalGrossWeightKg = totalGrossWeightSum.toPlainString(),
            totalNetWeightKg = totalNetWeightSum.toPlainString(),
            totalCbm = totalCbmSum.toPlainString(),
            notes = "Auto-generated batch for $poNumber",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        importShipmentDao.insertShipment(batchShipment)

        // Ensure supplier entry and category freight rates exist on the auto-created batch shipment
        importShipmentDao.insertShipmentSupplier(
            ShipmentSupplier(
                id = "ss-${batchShipment.id}-$supplierId",
                shipmentId = batchShipment.id,
                supplierId = supplierId,
                purchaseOrderId = orderId,
                currency = currency,
                exchangeRateToBdt = exchangeRate.toPlainString(),
                chinaDeliveryCurrency = currency,
                chinaDeliveryExchangeRateToBdt = exchangeRate.toPlainString()
            )
        )
        syncShipmentCategoryFreightRates(batchShipment.id)

        // Update supplier outstanding payable if PO is confirmed
        if (status == PurchaseOrderStatus.CONFIRMED) {
            val supplier = supplierDao.getSupplierById(supplierId)
            if (supplier != null) {
                supplierDao.updateOutstandingPayable(supplierId, supplier.outstandingPayableMinor + totalBdtMinor)
            }
        }

        return orderId
    }

    override suspend fun updatePurchaseOrderStatus(orderId: String, status: PurchaseOrderStatus) {
        val currentOrder = purchaseOrderDao.getOrderById(orderId)
        val oldStatus = currentOrder?.status
        purchaseOrderDao.updateOrderStatus(orderId, status.name)

        // When a DRAFT order is confirmed, add its BDT amount to the supplier outstanding payable
        if (status == PurchaseOrderStatus.CONFIRMED && oldStatus == PurchaseOrderStatus.DRAFT.name && currentOrder != null) {
            val supplier = supplierDao.getSupplierById(currentOrder.supplierId)
            if (supplier != null) {
                supplierDao.updateOutstandingPayable(currentOrder.supplierId, supplier.outstandingPayableMinor + currentOrder.totalBdtAmountMinor)
            }
        }
    }

    override suspend fun duplicatePurchaseOrder(orderId: String): String = database.withTransaction {
        val original = purchaseOrderDao.getOrderById(orderId) ?: error("Purchase order not found")
        val items = purchaseOrderDao.getItemsForOrderSync(orderId).map {
            PurchaseOrderItemDraft(it.productId, it.variantId, it.quantity, it.unitPriceMinor,
                it.cartonQuantity, it.pcsPerCarton, BigDecimal(it.grossWeightKg), BigDecimal(it.netWeightKg), BigDecimal(it.cbm))
        }
        val cartons = importShipmentDao.getCartonsForOrderSync(orderId).groupBy { it.cartonNumber }.map { (number, lines) ->
            val first = lines.first()
            com.pocketdev.advance_erp.domain.repository.PoMixedCartonDraft(number,
                lines.fold(BigDecimal.ZERO) { sum, line -> sum + BigDecimal(line.grossWeightKg) },
                lines.fold(BigDecimal.ZERO) { sum, line -> sum + BigDecimal(line.netWeightKg) },
                BigDecimal(first.lengthCm), BigDecimal(first.widthCm), BigDecimal(first.heightCm),
                lines.map { MixedCartonLineDraft(it.productId, it.variantId, quantity = it.quantity, manualGrossWeightKg = BigDecimal(it.grossWeightKg)) }, cbm = BigDecimal(first.cbm))
        }
        val costs = importCostDao.getCostItemsForOrderSync(orderId)
        savePurchaseOrderDraft(null, original.supplierId, original.currency, BigDecimal(original.exchangeRateToBdt),
            items, original.notes, PurchaseOrderStatus.DRAFT, cartons,
            costs.firstOrNull { it.description.startsWith("China Local Delivery") }?.amountMinor ?: 0L,
            emptyMap(), costs.firstOrNull { it.description.contains("Handling") }?.bdtAmountMinor ?: 0L,
            costs.firstOrNull { it.description.contains("Sample") }?.bdtAmountMinor ?: 0L, original.attachments)
    }

    override suspend fun cancelPurchaseOrder(orderId: String, reason: String?): Unit = database.withTransaction {
        val order = purchaseOrderDao.getOrderById(orderId)
            ?: throw IllegalArgumentException("Purchase Order not found: $orderId")
        check(order.status != PurchaseOrderStatus.CANCELLED.name) {
            "Purchase Order ${order.poNumber} is already cancelled"
        }

        val existingPayments = supplierPaymentDao.getPaymentsByOrderSync(orderId)
        check(existingPayments.isEmpty()) {
            "Cannot cancel Purchase Order: linked supplier payments exist (${existingPayments.size} payment(s))"
        }

        val existingShipments = importShipmentDao.getShipmentsByOrderIdSync(orderId)
        check(existingShipments.all { it.shipmentNumber.startsWith("BATCH-") && it.status == ShipmentStatus.DRAFT.name }) {
            "Cannot cancel Purchase Order: linked shipments exist (${existingShipments.size} shipment(s))"
        }

        check(warehouseReceiptDao.getReceiptsByOrderSync(orderId).isEmpty() && !order.isActualCostFinalized) {
            "Cannot cancel an order with receiving or finalized costs"
        }
        // Revert supplier outstanding payable if order was confirmed
        if (order.status == PurchaseOrderStatus.CONFIRMED.name) {
            val supplier = supplierDao.getSupplierById(order.supplierId)
            if (supplier != null) {
                val newPayable = (supplier.outstandingPayableMinor - order.totalBdtAmountMinor).coerceAtLeast(0L)
                supplierDao.updateOutstandingPayable(order.supplierId, newPayable)
            }
        }

        val updatedNotes = if (!reason.isNullOrBlank()) {
            "${order.notes ?: ""}\n[Cancellation Reason: $reason]".trim()
        } else order.notes

        database.withTransaction {
            purchaseOrderDao.updateOrder(
                order.copy(
                    status = PurchaseOrderStatus.CANCELLED.name,
                    notes = updatedNotes,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    // ==========================================
    // 2. Supplier Payments
    // ==========================================

    override fun getAllSupplierPayments(): Flow<List<SupplierPayment>> = supplierPaymentDao.getAllPayments()

    override fun getPaymentsForOrder(orderId: String): Flow<List<SupplierPayment>> = supplierPaymentDao.getPaymentsByOrder(orderId)

    override suspend fun getPaymentsForOrderSync(orderId: String): List<SupplierPayment> =
        supplierPaymentDao.getPaymentsByOrderSync(orderId)

    override suspend fun recordSupplierPayment(
        supplierId: String,
        orderId: String?,
        amountMinor: Long,
        currency: String,
        exchangeRate: BigDecimal,
        method: PaymentMethod,
        paymentAccount: String,
        reference: String?,
        notes: String?,
        paymentDate: Long,
        paymentAccountId: String?
    ): String = database.withTransaction {
        require(amountMinor > 0) { "Payment amount must be positive" }
        require(exchangeRate > BigDecimal.ZERO) { "Exchange rate must be positive" }

        var targetOrder: PurchaseOrder? = null
        if (orderId != null) {
            targetOrder = purchaseOrderDao.getOrderById(orderId)
                ?: throw IllegalArgumentException("Purchase Order not found: $orderId")
            require(targetOrder.supplierId == supplierId && targetOrder.currency == currency) { "Supplier and currency must match the selected PO" }
            require(targetOrder.status !in listOf(PurchaseOrderStatus.DRAFT.name, PurchaseOrderStatus.CANCELLED.name)) { "Confirm the PO before recording payment" }
            val remainingBalance = targetOrder.totalAmountMinor - targetOrder.paidAmountMinor
            require(amountMinor <= remainingBalance) {
                "Payment amount ($amountMinor) exceeds remaining PO balance ($remainingBalance)"
            }
        }

        val existingNumbers = supplierPaymentDao.getAllPaymentsSync().map { it.paymentNumber }.toSet()
        val yearStr = java.text.SimpleDateFormat("yyyy", java.util.Locale.US).format(java.util.Date(paymentDate))
        var seq = supplierPaymentDao.getPaymentCount() + 1
        var paymentNumber = "SP-$yearStr-%04d".format(seq)
        while (paymentNumber in existingNumbers) {
            seq++
            paymentNumber = "SP-$yearStr-%04d".format(seq)
        }
        val paymentId = "sp-${UUID.randomUUID()}"

        val bdtAmountMinor = BigDecimal(amountMinor)
            .multiply(exchangeRate)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()

        val payment = SupplierPayment(
            id = paymentId,
            paymentNumber = paymentNumber,
            supplierId = supplierId,
            purchaseOrderId = orderId,
            amountMinor = amountMinor,
            currency = currency,
            exchangeRateToBdt = exchangeRate.toPlainString(),
            bdtAmountMinor = bdtAmountMinor,
            paymentMethod = method.name,
            paymentAccount = paymentAccount,
            referenceNumber = reference,
            notes = notes,
            paymentDate = paymentDate,
            createdAt = System.currentTimeMillis()
        )

        supplierPaymentDao.insertPayment(payment)

        // Deduct from supplier outstanding payable
        val supplier = supplierDao.getSupplierById(supplierId)
        if (supplier != null) {
            val newPayable = (supplier.outstandingPayableMinor - bdtAmountMinor).coerceAtLeast(0L)
            supplierDao.updateOutstandingPayable(supplierId, newPayable)
        }

        // If linked to a PO, update PO paid amount and status
        if (orderId != null && targetOrder != null) {
            val newPaidAmount = targetOrder.paidAmountMinor + amountMinor
            val newPaidBdt = targetOrder.paidBdtAmountMinor + bdtAmountMinor
            val newStatus = if (targetOrder.status !in listOf(PurchaseOrderStatus.CONFIRMED.name, PurchaseOrderStatus.PARTIALLY_PAID.name, PurchaseOrderStatus.PAID.name)) targetOrder.status else if (newPaidAmount >= targetOrder.totalAmountMinor) {
                PurchaseOrderStatus.PAID.name
            } else {
                PurchaseOrderStatus.PARTIALLY_PAID.name
            }
            purchaseOrderDao.recordPayment(orderId, amountMinor, bdtAmountMinor, newStatus)
        }

        // Automatic Double-Entry Accounting:
        // Dr. Accounts Payable (2010)
        // Cr. Bank (1020)
        accountingRepository.postJournalEntry(
                entryDate = paymentDate,
                descEn = "Supplier Payment $paymentNumber to ${supplier?.companyName ?: supplierId}",
                descBn = "সরবরাহকারী পেমেন্ট $paymentNumber (${supplier?.companyName ?: ""})",
                referenceType = "SUPPLIER_PAYMENT",
                referenceId = paymentNumber,
                lines = listOf(
                    JournalLineDraft(
                        accountId = "coa-2010", // Accounts Payable
                        isDebit = true,
                        amountMinor = bdtAmountMinor,
                        description = "Debit Accounts Payable for remittance $paymentNumber"
                    ),
                    JournalLineDraft(
                        accountId = paymentAccountId ?: if (method == PaymentMethod.CASH) "coa-1010" else "coa-1020",
                        isDebit = false,
                        amountMinor = bdtAmountMinor,
                        description = "Credit Bank Account for remittance $paymentNumber"
                    )
                )
            )
        paymentId
    }

    // ==========================================
    // 3. Import Shipments & Cartons
    // ==========================================

    override fun getAllShipments(): Flow<List<ImportShipment>> = importShipmentDao.getAllShipments()

    override suspend fun getShipmentById(id: String): ImportShipment? = importShipmentDao.getShipmentById(id)

    override fun getShipmentsForOrder(orderId: String): Flow<List<ImportShipment>> = importShipmentDao.getShipmentsForOrder(orderId)

    override suspend fun getShipmentsForOrderSync(orderId: String): List<ImportShipment> = importShipmentDao.getShipmentsByOrderIdSync(orderId)

    override fun getCartonsForShipment(shipmentId: String): Flow<List<ShipmentCarton>> = importShipmentDao.getCartonsForShipment(shipmentId)

    override suspend fun createShipment(
        supplierId: String,
        orderId: String?,
        method: ShippingMethod,
        origin: String,
        destination: String,
        trackingRef: String?,
        shippingLine: String?,
        cartons: List<ShipmentCartonDraft>,
        notes: String?
    ): String {
        require(cartons.isNotEmpty()) { "Shipment must contain at least 1 carton" }

        // Multi-shipment quantity validation against PO if linked
        if (orderId != null) {
            val order = purchaseOrderDao.getOrderById(orderId)
                ?: throw IllegalArgumentException("Purchase Order not found: $orderId")
            val orderItems = purchaseOrderDao.getItemsForOrderSync(orderId)
            val existingShipments = importShipmentDao.getShipmentsByOrderIdSync(orderId)
                .filter {
                    it.status != ShipmentStatus.CANCELLED.name &&
                        !(it.shipmentNumber.startsWith("BATCH-") && it.status == ShipmentStatus.DRAFT.name)
                }
            val existingCartons = existingShipments.flatMap { importShipmentDao.getCartonsForShipmentSync(it.id) }

            val alreadyShippedByProduct = existingCartons.groupBy { it.productId to it.variantId }
                .mapValues { (_, cList) -> cList.sumOf { it.quantity } }
            val newShippedByProduct = cartons.groupBy { it.productId to it.variantId }
                .mapValues { (_, cList) -> cList.sumOf { it.quantity } }

            newShippedByProduct.forEach { (key, newQty) ->
                val matchingOrderItem = orderItems.find { it.productId == key.first && it.variantId == key.second }
                val orderedQty = matchingOrderItem?.quantity ?: 0
                require(orderedQty > 0) {
                    val prodName = productDao.getProductById(key.first)?.nameEn ?: key.first
                    "Product $prodName is not part of Purchase Order ${order.poNumber}"
                }
                val prevShipped = alreadyShippedByProduct[key] ?: 0
                val remainingUnshipped = (orderedQty - prevShipped).coerceAtLeast(0)
                require(newQty <= remainingUnshipped) {
                    val prodName = productDao.getProductById(key.first)?.nameEn ?: key.first
                    "Shipment quantity ($newQty) exceeds remaining unshipped quantity ($remainingUnshipped) for product $prodName"
                }
            }
        }

        val shipmentCount = importShipmentDao.getShipmentCount()
        val shipmentNumber = String.format(java.util.Locale.ROOT, "SHP-2026-%04d", shipmentCount + 1)
        val shipmentId = "shp-${UUID.randomUUID()}"

        var totalQty = 0
        var totalGrossWeight = BigDecimal.ZERO
        var totalNetWeight = BigDecimal.ZERO

        val cartonEntities = cartons.map { draft ->
            require(draft.quantity > 0) { "Carton quantity must be positive" }
            require(draft.lengthCm >= BigDecimal.ZERO && draft.widthCm >= BigDecimal.ZERO && draft.heightCm >= BigDecimal.ZERO) {
                "Carton dimensions cannot be negative"
            }
            require(draft.grossWeightKg >= BigDecimal.ZERO && draft.netWeightKg >= BigDecimal.ZERO) {
                "Weights cannot be negative"
            }

            val hasDims = draft.lengthCm > BigDecimal.ZERO && draft.widthCm > BigDecimal.ZERO && draft.heightCm > BigDecimal.ZERO
            val cbm = if (hasDims) {
                CbmCalculator.calculateCbm(draft.lengthCm, draft.widthCm, draft.heightCm, 1)
            } else {
                draft.cbm.setScale(6, RoundingMode.HALF_UP)
            }

            val product = productDao.getProductById(draft.productId)
            val resolvedCatId = draft.categoryId ?: product?.categoryId
            val resolvedCatName = draft.categoryName ?: resolvedCatId?.let { database.productCategoryDao().getCategoryById(it)?.nameEn }

            totalQty += draft.quantity
            totalGrossWeight = totalGrossWeight.add(draft.grossWeightKg)
            totalNetWeight = totalNetWeight.add(draft.netWeightKg)

            ShipmentCarton(
                id = "ctn-${UUID.randomUUID()}",
                shipmentId = shipmentId,
                cartonNumber = draft.cartonNumber,
                productId = draft.productId,
                variantId = draft.variantId,
                quantity = draft.quantity,
                lengthCm = draft.lengthCm.toPlainString(),
                widthCm = draft.widthCm.toPlainString(),
                heightCm = draft.heightCm.toPlainString(),
                grossWeightKg = draft.grossWeightKg.toPlainString(),
                netWeightKg = draft.netWeightKg.toPlainString(),
                cbm = cbm.toPlainString(),
                purchaseOrderId = draft.purchaseOrderId ?: orderId,
                supplierId = draft.supplierId ?: supplierId,
                categoryId = resolvedCatId,
                categoryName = resolvedCatName
            )
        }

        // Count physical cartons by distinct cartonNumber so mixed cartons (multi-category/product) are counted once
        val distinctCartonsCount = cartonEntities.map { it.cartonNumber.trim().ifBlank { it.id } }.distinct().size
        val totalCbm = cartonEntities.groupBy { it.cartonNumber.trim().ifBlank { it.id } }.values
            .fold(BigDecimal.ZERO) { acc, rows ->
                acc.add(BigDecimal(rows.first().cbm.ifBlank { "0" }))
            }

        val shipment = ImportShipment(
            id = shipmentId,
            shipmentNumber = shipmentNumber,
            supplierId = supplierId,
            purchaseOrderId = orderId,
            shippingMethod = method.name,
            origin = origin,
            destination = destination,
            trackingReference = trackingRef,
            shippingLine = shippingLine,
            shippingDate = System.currentTimeMillis(),
            estimatedArrivalDate = System.currentTimeMillis() + (if (method == ShippingMethod.AIR) 5L else 20L) * 24 * 3600 * 1000,
            status = ShipmentStatus.IN_TRANSIT.name,
            totalCartons = distinctCartonsCount,
            totalQuantity = totalQty,
            totalGrossWeightKg = totalGrossWeight.toPlainString(),
            totalNetWeightKg = totalNetWeight.toPlainString(),
            totalCbm = totalCbm.toPlainString(),
            notes = notes,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        importShipmentDao.saveShipmentWithCartons(shipment, cartonEntities)

        // Ensure primary supplier entry exists in shipment_suppliers (copying any China delivery entered on the PO's batch shipment)
        val linkedPo = if (orderId != null) purchaseOrderDao.getOrderById(orderId) else null
        val batchShipment = if (orderId != null) importShipmentDao.getBatchShipmentForOrder(orderId) else null
        val batchSupplier = if (batchShipment != null) {
            importShipmentDao.getSuppliersForShipmentSync(batchShipment.id).firstOrNull { it.supplierId == supplierId }
        } else null

        importShipmentDao.insertShipmentSupplier(
            ShipmentSupplier(
                id = "ss-$shipmentId-$supplierId",
                shipmentId = shipmentId,
                supplierId = supplierId,
                purchaseOrderId = orderId,
                currency = linkedPo?.currency ?: batchSupplier?.currency ?: "CNY",
                exchangeRateToBdt = linkedPo?.exchangeRateToBdt ?: batchSupplier?.exchangeRateToBdt ?: "17.0000",
                chinaDeliveryChargeMinor = batchSupplier?.chinaDeliveryChargeMinor ?: 0L,
                chinaDeliveryCurrency = batchSupplier?.chinaDeliveryCurrency ?: linkedPo?.currency ?: "CNY",
                chinaDeliveryExchangeRateToBdt = batchSupplier?.chinaDeliveryExchangeRateToBdt ?: linkedPo?.exchangeRateToBdt ?: "17.0000",
                chinaDeliveryBdtMinor = batchSupplier?.chinaDeliveryBdtMinor ?: 0L,
                chinaDeliveryAllocationMethod = batchSupplier?.chinaDeliveryAllocationMethod ?: AllocationMethod.QUANTITY.name,
                manualAllocationsJson = batchSupplier?.manualAllocationsJson
            )
        )

        if (orderId != null) {
            purchaseOrderDao.updateOrderShipmentId(orderId, shipmentId)
            purchaseOrderDao.updateOrderStatus(orderId, PurchaseOrderStatus.SHIPPED.name)
        }

        // Auto-load default category freight rates for categories in this shipment (and inherit any PO overrides)
        syncShipmentCategoryFreightRates(shipmentId)
        if (batchShipment != null) {
            val batchRates = importShipmentDao.getFreightRatesForShipmentSync(batchShipment.id)
            batchRates.forEach { br ->
                if (br.overrideRatePerKgBdtMinor != null) {
                    overrideShipmentCategoryFreightRate(shipmentId, br.categoryId, br.overrideRatePerKgBdtMinor)
                }
            }
        }

        return shipmentId
    }

    override suspend fun addCartonToShipment(shipmentId: String, carton: ShipmentCartonDraft): String {
        val shipment = importShipmentDao.getShipmentById(shipmentId)
            ?: throw IllegalArgumentException("Shipment not found: $shipmentId")

        require(carton.quantity > 0) { "Carton quantity must be positive" }
        require(carton.lengthCm >= BigDecimal.ZERO && carton.widthCm >= BigDecimal.ZERO && carton.heightCm >= BigDecimal.ZERO) {
            "Carton dimensions cannot be negative"
        }
        require(carton.grossWeightKg >= BigDecimal.ZERO && carton.netWeightKg >= BigDecimal.ZERO) {
            "Weights cannot be negative"
        }

        val hasDims = carton.lengthCm > BigDecimal.ZERO && carton.widthCm > BigDecimal.ZERO && carton.heightCm > BigDecimal.ZERO
        val cbm = if (hasDims) {
            CbmCalculator.calculateCbm(carton.lengthCm, carton.widthCm, carton.heightCm, 1)
        } else {
            carton.cbm.setScale(6, RoundingMode.HALF_UP)
        }
        val product = productDao.getProductById(carton.productId)
        val resolvedCatId = carton.categoryId ?: product?.categoryId
        val resolvedCatName = carton.categoryName ?: resolvedCatId?.let { database.productCategoryDao().getCategoryById(it)?.nameEn }

        val cartonId = "ctn-${UUID.randomUUID()}"
        val cartonEntity = ShipmentCarton(
            id = cartonId,
            shipmentId = shipmentId,
            cartonNumber = carton.cartonNumber,
            productId = carton.productId,
            variantId = carton.variantId,
            quantity = carton.quantity,
            lengthCm = carton.lengthCm.toPlainString(),
            widthCm = carton.widthCm.toPlainString(),
            heightCm = carton.heightCm.toPlainString(),
            grossWeightKg = carton.grossWeightKg.toPlainString(),
            netWeightKg = carton.netWeightKg.toPlainString(),
            cbm = cbm.toPlainString(),
            purchaseOrderId = carton.purchaseOrderId ?: shipment.purchaseOrderId,
            supplierId = carton.supplierId ?: shipment.supplierId,
            categoryId = resolvedCatId,
            categoryName = resolvedCatName
        )

        importShipmentDao.insertCarton(cartonEntity)

        // Recalculate shipment totals
        val allCartons = importShipmentDao.getCartonsForShipmentSync(shipmentId)
        val totalQty = allCartons.sumOf { it.quantity }
        val totalGross = allCartons.fold(BigDecimal.ZERO) { acc, c -> acc.add(BigDecimal(c.grossWeightKg)) }
        val totalNet = allCartons.fold(BigDecimal.ZERO) { acc, c -> acc.add(BigDecimal(c.netWeightKg)) }
        val totalCbm = allCartons.fold(BigDecimal.ZERO) { acc, c -> acc.add(BigDecimal(c.cbm)) }

        val updatedShipment = shipment.copy(
            totalCartons = allCartons.map { it.cartonNumber }.distinct().size.coerceAtLeast(allCartons.size),
            totalQuantity = totalQty,
            totalGrossWeightKg = totalGross.toPlainString(),
            totalNetWeightKg = totalNet.toPlainString(),
            totalCbm = totalCbm.toPlainString(),
            updatedAt = System.currentTimeMillis()
        )
        importShipmentDao.updateShipment(updatedShipment)
        syncShipmentCategoryFreightRates(shipmentId)

        return cartonId
    }

    override suspend fun updateShipmentStatus(shipmentId: String, status: ShipmentStatus) {
        val arrivalDate = if (status == ShipmentStatus.ARRIVED || status == ShipmentStatus.RECEIVED) {
            System.currentTimeMillis()
        } else null
        importShipmentDao.updateShipmentStatus(shipmentId, status.name, arrivalDate)

        if (status == ShipmentStatus.CANCELLED) {
            val shipment = importShipmentDao.getShipmentById(shipmentId)
            val orderId = shipment?.purchaseOrderId
            if (orderId != null) {
                val activeShipments = importShipmentDao.getShipmentsByOrderIdSync(orderId)
                    .filter { it.id != shipmentId && it.status != ShipmentStatus.CANCELLED.name }
                if (activeShipments.isEmpty()) {
                    purchaseOrderDao.updateOrderStatus(orderId, PurchaseOrderStatus.CONFIRMED.name)
                }
            }
        }
    }

    // ==========================================
    // 4. Cost Management & Landed Cost Engine
    // ==========================================

    override fun getCostItemsForShipment(shipmentId: String): Flow<List<ImportCostItem>> = importCostDao.getCostItemsForShipment(shipmentId)

    override suspend fun addCostItem(
        shipmentId: String,
        category: CostCategory,
        description: String,
        amountMinor: Long,
        currency: String,
        exchangeRate: BigDecimal,
        method: AllocationMethod,
        paymentAccount: String,
        docRef: String?,
        manualAllocationsBdtMinor: Map<String, Long>
    ): String = database.withTransaction {
        require(amountMinor > 0) { "Cost amount must be greater than zero" }
        require(exchangeRate > BigDecimal.ZERO) { "Exchange rate must be positive" }

        if (method == AllocationMethod.MANUAL) {
            val expected = BigDecimal(amountMinor).multiply(exchangeRate).setScale(0, RoundingMode.HALF_UP).longValueExact()
            require(manualAllocationsBdtMinor.isNotEmpty() && manualAllocationsBdtMinor.values.all { it >= 0 } &&
                manualAllocationsBdtMinor.values.fold(0L, Math::addExact) == expected) { "Manual BDT allocations must equal the expense total" }
        }
        val costId = "cost-${UUID.randomUUID()}"
        val bdtAmountMinor = BigDecimal(amountMinor)
            .multiply(exchangeRate)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()

        val costItem = ImportCostItem(
            id = costId,
            shipmentId = shipmentId,
            costCategory = category.name,
            description = description,
            amountMinor = amountMinor,
            currency = currency,
            exchangeRateToBdt = exchangeRate.toPlainString(),
            bdtAmountMinor = bdtAmountMinor,
            costDate = System.currentTimeMillis(),
            paymentAccount = paymentAccount,
            documentReference = docRef,
            allocationMethod = method.name,
            manualAllocationsJson = LandedCostAllocationEngine.formatManualAllocations(manualAllocationsBdtMinor),
            isPosted = true,
            createdAt = System.currentTimeMillis()
        )

        importCostDao.insertCostItem(costItem)

        // Automatic Accounting:
        // Dr. In-Transit Inventory (1220)
        // Cr. Bank (1020)
        try {
            accountingRepository.postJournalEntry(
                entryDate = System.currentTimeMillis(),
                descEn = "Import Expense: $description ($currency $amountMinor)",
                descBn = "আমদানি ব্যয়: $description",
                referenceType = "IMPORT_EXPENSE",
                referenceId = costId,
                lines = listOf(
                    JournalLineDraft(
                        accountId = "coa-1220", // In-Transit Inventory
                        isDebit = true,
                        amountMinor = bdtAmountMinor,
                        description = "Capitalize import expense $description"
                    ),
                    JournalLineDraft(
                        accountId = "coa-1020", // City Bank Corporate
                        isDebit = false,
                        amountMinor = bdtAmountMinor,
                        description = "Payment for import expense $description"
                    )
                )
            )
        } catch (e: Exception) {
            throw e
        }

        return@withTransaction costId
    }

    override suspend fun deleteCostItem(costItemId: String) = database.withTransaction {
        deletePurchaseCostItem(costItemId)
    }

    override suspend fun calculateAndSaveLandedCost(shipmentId: String): List<LandedCostSnapshot> {
        return finalizeShipmentLandingCost(
            shipmentId = shipmentId,
            operatorRole = "ADMIN",
            operatorName = "SYSTEM_ADMIN"
        )
    }

    override suspend fun getCostItemsForShipmentSync(shipmentId: String): List<ImportCostItem> =
        importCostDao.getCostItemsForShipmentSync(shipmentId)

    override suspend fun adjustLandedCost(
        shipmentId: String,
        newCostCategory: CostCategory,
        newAmountMinor: Long,
        exchangeRate: BigDecimal,
        reason: String
    ): CostAdjustmentResult = database.withTransaction {
        val shipment = importShipmentDao.getShipmentById(shipmentId)
            ?: throw IllegalArgumentException("Shipment not found: $shipmentId")

        val existingCosts = importCostDao.getCostItemsForShipmentSync(shipmentId)
        val oldTotalExpenseBdt = existingCosts.sumOf { it.bdtAmountMinor }

        val bdtAmountMinor = BigDecimal(newAmountMinor)
            .multiply(exchangeRate)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()

        val costId = "cost-${UUID.randomUUID()}"
        val costItem = ImportCostItem(
            id = costId,
            shipmentId = shipmentId,
            costCategory = newCostCategory.name,
            description = "Cost Adjustment: $reason",
            amountMinor = newAmountMinor,
            currency = "BDT",
            exchangeRateToBdt = exchangeRate.toPlainString(),
            bdtAmountMinor = bdtAmountMinor,
            costDate = System.currentTimeMillis(),
            paymentAccount = "Accounts Payable (Landed Cost Clearing)",
            documentReference = MovementNumberGenerator.generate("ADJ"),
            allocationMethod = newCostCategory.defaultAllocationMethod.name,
            isPosted = true
        )
        importCostDao.insertCostItem(costItem)

        val newTotalExpenseBdt = oldTotalExpenseBdt + bdtAmountMinor
        val varianceBdtMinor = bdtAmountMinor

        val layers = inventoryCostLayerDao.getLayersForShipmentSync(shipmentId)
        val totalInitialQty = layers.sumOf { it.initialQuantity }

        var unitVariance = 0L
        var remainingInventoryAdjustment = 0L
        var cogsAdjustment = 0L
        val journalEntryNumber = MovementNumberGenerator.generate("JV-ADJ")

        if (totalInitialQty > 0 && varianceBdtMinor != 0L) {
            unitVariance = BigDecimal(varianceBdtMinor)
                .divide(BigDecimal(totalInitialQty), 0, RoundingMode.HALF_UP)
                .longValueExact()

            layers.forEach { layer ->
                val soldQty = layer.initialQuantity - layer.remainingQuantity
                val remQty = layer.remainingQuantity

                val layerRemAdj = remQty.toLong() * unitVariance
                val layerSoldAdj = soldQty.toLong() * unitVariance

                remainingInventoryAdjustment += layerRemAdj
                cogsAdjustment += layerSoldAdj

                inventoryCostLayerDao.updateCostLayerUnitCost(
                    layerId = layer.id,
                    unitCostMinor = layer.unitLandedCostBdtMinor + unitVariance,
                    allocatedExpenseMinor = layer.allocatedExpensePerUnitBdtMinor + unitVariance
                )
            }

            val unallocated = varianceBdtMinor - (remainingInventoryAdjustment + cogsAdjustment)
            cogsAdjustment += unallocated

            try {
                val lines = mutableListOf<JournalLineDraft>()
                if (remainingInventoryAdjustment > 0L) {
                    lines.add(
                        JournalLineDraft(
                            accountId = "coa-1210",
                            isDebit = true,
                            amountMinor = remainingInventoryAdjustment,
                            description = "Landed cost adjustment for remaining inventory ($shipmentId)"
                        )
                    )
                }
                if (cogsAdjustment > 0L) {
                    lines.add(
                        JournalLineDraft(
                            accountId = "coa-5010",
                            isDebit = true,
                            amountMinor = cogsAdjustment,
                            description = "Landed cost adjustment for sold inventory ($shipmentId)"
                        )
                    )
                }
                lines.add(
                    JournalLineDraft(
                        accountId = "coa-2010",
                        isDebit = false,
                        amountMinor = varianceBdtMinor,
                        description = "Landed cost adjustment payable/clearing ($shipmentId)"
                    )
                )

                if (lines.isNotEmpty()) {
                    accountingRepository.postJournalEntry(
                        entryDate = System.currentTimeMillis(),
                        descEn = "Actual Landed Cost Adjustment for ${shipment.shipmentNumber}",
                        descBn = "প্রকৃত আমদানি ব্যয় সমন্বয় (${shipment.shipmentNumber})",
                        referenceType = "LANDED_COST_ADJUSTMENT",
                        referenceId = shipment.shipmentNumber,
                        lines = lines
                    )
                }
            } catch (e: Exception) {
                throw e
            }
        }

        return@withTransaction CostAdjustmentResult(
            shipmentId = shipmentId,
            previousTotalExpenseBdtMinor = oldTotalExpenseBdt,
            actualTotalExpenseBdtMinor = newTotalExpenseBdt,
            costVarianceBdtMinor = varianceBdtMinor,
            unitVarianceBdtMinor = unitVariance,
            remainingInventoryAdjustmentBdtMinor = remainingInventoryAdjustment,
            cogsAdjustmentBdtMinor = cogsAdjustment,
            unallocatedVarianceBdtMinor = 0L,
            journalEntryNumber = journalEntryNumber
        )
    }

    override fun getLandedCostSnapshots(shipmentId: String): Flow<List<LandedCostSnapshot>> = importCostDao.getSnapshotsForShipment(shipmentId)

    override fun getAllocationsForShipment(shipmentId: String): Flow<List<LandedCostAllocation>> = importCostDao.getAllocationsForShipment(shipmentId)

    // ==========================================
    // 5. Warehouse Receiving & Cost Layers
    // ==========================================

    override fun getAllWarehouseReceipts(): Flow<List<WarehouseReceipt>> = warehouseReceiptDao.getAllReceipts()

    override fun getReceiptsForShipment(shipmentId: String): Flow<List<WarehouseReceipt>> = warehouseReceiptDao.getReceiptsByShipment(shipmentId)

    override fun getReceiptItems(receiptId: String): Flow<List<WarehouseReceiptItem>> = warehouseReceiptDao.getItemsForReceipt(receiptId)

    override suspend fun getReceiptItemsSync(receiptId: String): List<WarehouseReceiptItem> = warehouseReceiptDao.getItemsForReceiptSync(receiptId)

    override suspend fun createWarehouseReceipt(
        shipmentId: String,
        warehouseId: String,
        items: List<ReceiptItemDraft>,
        notes: String?,
        postImmediately: Boolean,
        allowDiscrepancy: Boolean
    ): String = database.withTransaction {
        require(items.isNotEmpty()) { "Receipt must contain at least 1 item" }

        val receiptCount = warehouseReceiptDao.getReceiptCount()
        val receiptNumber = "WR-2026-%04d".format(receiptCount + 1)
        val receiptId = "wr-${UUID.randomUUID()}"

        // Lookup snapshots to set estimated landed cost
        val snapshots = importCostDao.getSnapshotsForShipmentSync(shipmentId).associateBy {
            if (it.variantId != null) "${it.productId}:${it.variantId}" else it.productId
        }

        var hasDiscrepancy = false

        val itemEntities = items.map { draft ->
            val key = if (draft.variantId != null) "${draft.productId}:${draft.variantId}" else draft.productId
            val snapshot = snapshots[key] ?: snapshots[draft.productId]
            val unitLandedCost = snapshot?.unitLandedCostBdtMinor ?: 0L

            val resolvedGood = if (draft.goodQuantity >= 0) {
                draft.goodQuantity
            } else if (draft.receivedQuantity + draft.damagedQuantity + draft.missingQuantity == draft.expectedQuantity) {
                draft.receivedQuantity
            } else {
                (draft.receivedQuantity - draft.damagedQuantity).coerceAtLeast(0)
            }
            val resolvedPending = if (draft.pendingQuantity >= 0) {
                draft.pendingQuantity
            } else {
                (draft.expectedQuantity - resolvedGood - draft.damagedQuantity - draft.missingQuantity).coerceAtLeast(0)
            }

            // Phase 3 Validation: Packed Quantity = Good + Damaged + Missing + Pending
            if (draft.excessQuantity == 0) {
                LandingCostEngine.validateReceivingQuantities(
                    packedQuantity = draft.expectedQuantity,
                    goodQuantity = resolvedGood,
                    damagedQuantity = draft.damagedQuantity,
                    missingQuantity = draft.missingQuantity,
                    pendingQuantity = resolvedPending
                )
            }

            if (resolvedGood != draft.expectedQuantity || draft.damagedQuantity > 0 || draft.missingQuantity > 0 || resolvedPending > 0) {
                hasDiscrepancy = true
            }

            WarehouseReceiptItem(
                id = "wri-${UUID.randomUUID()}",
                receiptId = receiptId,
                cartonId = draft.cartonId,
                productId = draft.productId,
                variantId = draft.variantId,
                expectedQuantity = draft.expectedQuantity,
                receivedQuantity = draft.receivedQuantity,
                damagedQuantity = draft.damagedQuantity,
                missingQuantity = draft.missingQuantity,
                excessQuantity = draft.excessQuantity,
                unitLandedCostMinor = unitLandedCost,
                conditionNotes = draft.conditionNotes,
                goodQuantity = resolvedGood,
                pendingQuantity = resolvedPending
            )
        }

        val receipt = WarehouseReceipt(
            id = receiptId,
            receiptNumber = receiptNumber,
            shipmentId = shipmentId,
            warehouseId = warehouseId,
            receiptDate = System.currentTimeMillis(),
            status = ReceiptStatus.DRAFT.name,
            receivedBy = "Central Warehouse In-Charge",
            hasDiscrepancies = hasDiscrepancy,
            discrepancyApproved = false,
            notes = notes,
            createdAt = System.currentTimeMillis()
        )

        warehouseReceiptDao.saveReceiptWithItems(receipt, itemEntities)
        if (postImmediately) postWarehouseReceipt(receiptId, allowDiscrepancy)
        receiptId
    }

    override suspend fun postWarehouseReceipt(receiptId: String, allowDiscrepancy: Boolean): Unit = database.withTransaction {
        val receipt = warehouseReceiptDao.getReceiptById(receiptId)
            ?: throw IllegalArgumentException("Warehouse receipt not found: $receiptId")

        // CRITICAL CHECK: Prevent duplicate posting
        check(receipt.status != ReceiptStatus.POSTED.name) {
            "Duplicate posting prevented: Warehouse receipt ${receipt.receiptNumber} is already posted!"
        }

        val items = warehouseReceiptDao.getItemsForReceiptSync(receiptId)
        require(items.isNotEmpty()) { "Cannot post empty warehouse receipt" }

        // Check for excess discrepancies without approval
        val excessItem = items.find { it.receivedQuantity > it.expectedQuantity }
        if (excessItem != null && !allowDiscrepancy && !receipt.discrepancyApproved) {
            throw IllegalStateException(
                "Received quantity (${excessItem.receivedQuantity}) exceeds expected (${excessItem.expectedQuantity}). Explicit approval required!"
            )
        }

        val shipment = importShipmentDao.getShipmentById(receipt.shipmentId)
        check(shipment?.status != ShipmentStatus.COMPLETED.name && shipment?.status != ShipmentStatus.CANCELLED.name) {
            "Cannot post a receipt for a closed shipment"
        }
        val linkedPoId = receipt.purchaseOrderId ?: shipment?.purchaseOrderId
        val linkedPo = if (linkedPoId != null) {
            purchaseOrderDao.getOrderById(linkedPoId)?.also { po ->
                check(po.status != PurchaseOrderStatus.COMPLETED.name && po.status != PurchaseOrderStatus.CANCELLED.name) {
                    "Cannot post a receipt for a closed purchase order"
                }
                if (!po.isActualCostFinalized) {
                    throw IllegalStateException("Cannot post stock receiving: Actual Landing Cost must be finalized before posting stock into inventory!")
                }
            }
        } else null

        val snapshotsByKey = importCostDao.getSnapshotsForShipmentSync(receipt.shipmentId).associateBy {
            if (it.variantId != null) "${it.productId}:${it.variantId}" else it.productId
        }
        val poItemsByKey = if (linkedPoId != null) {
            purchaseOrderDao.getItemsForOrderSync(linkedPoId).associateBy {
                if (it.variantId != null) "${it.productId}:${it.variantId}" else it.productId
            }
        } else {
            emptyMap()
        }

        // ATOMIC DATABASE TRANSACTION
        database.withTransaction {
            var totalCapitalizedValueBdt = 0L
            var totalDamagedValueBdt = 0L
            var totalMissingValueBdt = 0L
            var hasRemainingPending = false

            items.forEach { item ->
                val itemKey = if (item.variantId != null) "${item.productId}:${item.variantId}" else item.productId
                val snapshot = snapshotsByKey[itemKey] ?: snapshotsByKey[item.productId]
                val poItem = poItemsByKey[itemKey] ?: poItemsByKey[item.productId]
                val product = productDao.getProductById(item.productId)
                val variant = if (item.variantId != null) productVariantDao.getVariantById(item.variantId) else null

                val unitPurchaseBdt = when {
                    poItem != null && poItem.quantity > 0 ->
                        BigDecimal(poItem.totalBdtAmountMinor)
                            .divide(BigDecimal(poItem.quantity), 0, RoundingMode.HALF_UP)
                            .longValueExact()
                    variant != null && variant.variantCostMinor > 0L -> variant.variantCostMinor
                    else -> product?.purchasePriceMinor ?: 0L
                }
                val unitCost = snapshot?.unitLandedCostBdtMinor?.takeIf { it > 0L }
                    ?: item.unitLandedCostMinor.takeIf { it > 0L }
                    ?: unitPurchaseBdt

                // Effective sound sellable stock:
                val goodQuantity = if (item.goodQuantity > 0 || (item.damagedQuantity + item.missingQuantity + item.pendingQuantity == item.expectedQuantity)) {
                    item.goodQuantity
                } else if (item.receivedQuantity + item.damagedQuantity + item.missingQuantity == item.expectedQuantity) {
                    item.receivedQuantity
                } else {
                    (item.receivedQuantity - item.damagedQuantity).coerceAtLeast(0)
                }
                if (item.pendingQuantity > 0) {
                    hasRemainingPending = true
                }

                var balanceAfterReceipt = (if (item.variantId != null) {
                    stockBalanceDao.getVariantBalance(receipt.warehouseId, item.productId, item.variantId)
                } else {
                    stockBalanceDao.getBalance(receipt.warehouseId, item.productId)
                })?.quantity ?: 0

                if (goodQuantity > 0) {
                    // 1. Update StockBalance with sound sellable quantity
                    val existingBalance = if (item.variantId != null) {
                        stockBalanceDao.getVariantBalance(receipt.warehouseId, item.productId, item.variantId)
                    } else {
                        stockBalanceDao.getBalance(receipt.warehouseId, item.productId)
                    }

                    val newBalance = if (existingBalance != null) {
                        existingBalance.copy(
                            quantity = existingBalance.quantity + goodQuantity,
                            updatedAt = System.currentTimeMillis()
                        )
                    } else {
                        StockBalance(
                            id = "bal-${UUID.randomUUID()}",
                            warehouseId = receipt.warehouseId,
                            productId = item.productId,
                            variantId = item.variantId,
                            quantity = goodQuantity,
                            reservedQuantity = 0,
                            updatedAt = System.currentTimeMillis()
                        )
                    }

                    stockBalanceDao.insertOrUpdate(newBalance)
                    balanceAfterReceipt = newBalance.quantity

                    // 2. Audit Trail: Create StockMovement for sound stock
                    val movementNumber = MovementNumberGenerator.generate("SM-2026")
                    stockMovementDao.insertMovement(
                        StockMovement(
                            id = MovementNumberGenerator.generateId("mov"),
                            movementNumber = movementNumber,
                            warehouseId = receipt.warehouseId,
                            productId = item.productId,
                            variantId = item.variantId,
                            movementType = StockMovementType.PURCHASE_RECEIPT.code,
                            quantity = goodQuantity,
                            unitCostMinor = unitCost,
                            balanceAfterMovement = newBalance.quantity,
                            totalCostImpactMinor = goodQuantity.toLong() * unitCost,
                            referenceType = "WAREHOUSE_RECEIPT",
                            referenceId = receipt.receiptNumber,
                            purchaseOrderId = linkedPo?.poNumber ?: shipment?.shipmentNumber ?: receipt.shipmentId,
                            purchaseBatchId = receipt.receiptNumber,
                            notes = "Import receiving check-in from ${shipment?.shipmentNumber ?: receipt.shipmentId}",
                            createdAt = System.currentTimeMillis()
                        )
                    )

                    // 3. Batch Cost Layer for sound stock (distinct FIFO batch preserving exact buy & landed cost)
                    totalCapitalizedValueBdt += (goodQuantity.toLong() * unitCost)

                    inventoryCostLayerDao.insertCostLayer(
                        InventoryCostLayer(
                            id = "icl-${UUID.randomUUID()}",
                            productId = item.productId,
                            variantId = item.variantId,
                            warehouseId = receipt.warehouseId,
                            shipmentId = receipt.shipmentId,
                            shipmentNumber = shipment?.shipmentNumber ?: "SHP-UNKNOWN",
                            warehouseReceiptId = receipt.id,
                            receiptNumber = receipt.receiptNumber,
                            purchaseOrderId = linkedPoId,
                            purchaseBatchId = receipt.receiptNumber,
                            initialQuantity = goodQuantity,
                            remainingQuantity = goodQuantity,
                            unitLandedCostBdtMinor = unitCost,
                            originalCurrency = linkedPo?.currency ?: "BDT",
                            originalPurchasePriceMinor = poItem?.unitPriceMinor ?: variant?.variantCostMinor ?: product?.purchasePriceMinor ?: 0L,
                            exchangeRateToBdt = linkedPo?.exchangeRateToBdt ?: "1.0",
                            allocatedExpensePerUnitBdtMinor = (unitCost - unitPurchaseBdt).coerceAtLeast(0L),
                            receiptDate = receipt.receiptDate,
                            isActive = true,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }

                // 4. Record Damaged Stock (Excluded from sellable stock -> 5050 Write-Off)
                if (item.damagedQuantity > 0) {
                    val dmgLoss = item.damagedQuantity.toLong() * unitCost
                    totalDamagedValueBdt += dmgLoss
                    val dmgMovementNumber = MovementNumberGenerator.generate("DMG-2026")
                    stockMovementDao.insertMovement(
                        StockMovement(
                            id = MovementNumberGenerator.generateId("mov-dmg"),
                            movementNumber = dmgMovementNumber,
                            warehouseId = receipt.warehouseId,
                            productId = item.productId,
                            variantId = item.variantId,
                            movementType = StockMovementType.DAMAGE_WRITE_OFF.code,
                            quantity = item.damagedQuantity,
                            unitCostMinor = unitCost,
                            balanceAfterMovement = balanceAfterReceipt,
                            totalCostImpactMinor = dmgLoss,
                            referenceType = "WAREHOUSE_RECEIPT",
                            referenceId = receipt.receiptNumber,
                            purchaseOrderId = shipment?.shipmentNumber ?: receipt.shipmentId,
                            purchaseBatchId = receipt.receiptNumber,
                            notes = "Damaged during transit. Excluded from sellable stock. Notes: ${item.conditionNotes ?: "Damaged"}",
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }

                // 5. Record Missing Stock (Excluded from stock -> 1050 Shortage/Loss)
                if (item.missingQuantity > 0) {
                    val misLoss = item.missingQuantity.toLong() * unitCost
                    totalMissingValueBdt += misLoss
                    val misMovementNumber = MovementNumberGenerator.generate("MIS-2026")
                    stockMovementDao.insertMovement(
                        StockMovement(
                            id = MovementNumberGenerator.generateId("mov-mis"),
                            movementNumber = misMovementNumber,
                            warehouseId = receipt.warehouseId,
                            productId = item.productId,
                            variantId = item.variantId,
                            movementType = StockMovementType.SHORTAGE.code,
                            quantity = item.missingQuantity,
                            unitCostMinor = unitCost,
                            balanceAfterMovement = balanceAfterReceipt,
                            totalCostImpactMinor = misLoss,
                            referenceType = "WAREHOUSE_RECEIPT",
                            referenceId = receipt.receiptNumber,
                            purchaseOrderId = shipment?.shipmentNumber ?: receipt.shipmentId,
                            purchaseBatchId = receipt.receiptNumber,
                            notes = "Missing / Shortage in transit. Excluded from stock.",
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }
            }

            // 4. Mark receipt as POSTED
            warehouseReceiptDao.markReceiptPosted(
                receiptId = receiptId,
                status = ReceiptStatus.POSTED.name,
                postedAt = System.currentTimeMillis()
            )

            // 5. Update shipment status (RECEIVED if pending items remain for partial receiving; COMPLETED if all received)
            val newShipmentStatus = if (hasRemainingPending) {
                ShipmentStatus.RECEIVED.name
            } else {
                ShipmentStatus.COMPLETED.name
            }
            importShipmentDao.updateShipmentStatus(
                shipmentId = receipt.shipmentId,
                status = newShipmentStatus,
                arrivalDate = receipt.receiptDate,
                updatedAt = System.currentTimeMillis()
            )

            // 6. Double-Entry Accounting Event:
            // Good -> Dr. Inventory Asset (1210)
            // Damaged -> Dr. Damaged Merchandise Write-Off (5050)
            // Missing -> Dr. Supplier Shortage & Claims Receivable (1050)
            // Pending -> Remains in In-Transit Inventory (1220)
            // Credit -> Cr. In-Transit Inventory (1220)
            val totalClearedFromInTransit = totalCapitalizedValueBdt + totalDamagedValueBdt + totalMissingValueBdt
            if (totalClearedFromInTransit > 0L) {
                try {
                    val lines = mutableListOf<JournalLineDraft>()
                    if (totalCapitalizedValueBdt > 0L) {
                        lines.add(
                            JournalLineDraft(
                                accountId = "coa-1210",
                                isDebit = true,
                                amountMinor = totalCapitalizedValueBdt,
                                description = "Capitalize good received stock into warehouse inventory"
                            )
                        )
                    }
                    if (totalDamagedValueBdt > 0L) {
                        lines.add(
                            JournalLineDraft(
                                accountId = "coa-5050",
                                isDebit = true,
                                amountMinor = totalDamagedValueBdt,
                                description = "Write-off damaged goods on ${receipt.receiptNumber}"
                            )
                        )
                    }
                    if (totalMissingValueBdt > 0L) {
                        lines.add(
                            JournalLineDraft(
                                accountId = "coa-1050",
                                isDebit = true,
                                amountMinor = totalMissingValueBdt,
                                description = "Record missing/shortage claim on ${receipt.receiptNumber}"
                            )
                        )
                    }
                    lines.add(
                        JournalLineDraft(
                            accountId = "coa-1220",
                            isDebit = false,
                            amountMinor = totalClearedFromInTransit,
                            description = "Clear received/reconciled goods from in-transit inventory (Pending remains in 1220)"
                        )
                    )
                    accountingRepository.postJournalEntry(
                        entryDate = receipt.receiptDate,
                        descEn = "Inventory Receiving & Reconciliation for ${receipt.receiptNumber}",
                        descBn = "মজুদ পণ্য গ্রহণ ও সমন্বয় (${receipt.receiptNumber})",
                        referenceType = "WAREHOUSE_RECEIPT",
                        referenceId = receipt.receiptNumber,
                        lines = lines
                    )
                } catch (e: Exception) {
                    throw e
                }
            }
        }
        Unit
    }

    override fun getAllCostLayers(): Flow<List<InventoryCostLayer>> = inventoryCostLayerDao.getAllCostLayers()

    override fun getCostLayersForProduct(productId: String): Flow<List<InventoryCostLayer>> = inventoryCostLayerDao.getActiveLayersForProduct(productId)

    override suspend fun getCostLayersForShipmentSync(shipmentId: String): List<InventoryCostLayer> =
        inventoryCostLayerDao.getLayersForShipmentSync(shipmentId)

    // ==========================================
    // 5. Minimal Import Management System Enhancements
    // ==========================================

    override suspend fun addMixedCarton(
        orderId: String,
        cartonNumber: String,
        lengthCm: BigDecimal,
        widthCm: BigDecimal,
        heightCm: BigDecimal,
        grossWeightKg: BigDecimal,
        netWeightKg: BigDecimal,
        lines: List<MixedCartonLineDraft>
    ): List<String> = database.withTransaction {
        require(lines.isNotEmpty()) { "Mixed carton must have at least one product line" }
        val po = purchaseOrderDao.getOrderById(orderId)
            ?: throw IllegalArgumentException("Purchase order not found: $orderId")
        val batchShipment = importShipmentDao.getBatchShipmentForOrder(orderId)
            ?: throw IllegalStateException("Batch shipment not found for order: $orderId")

        requirePackingEditableOrder(orderId)
        val currentPacking = importShipmentDao.getCartonsForOrderSync(orderId)
        require(cartonNumber.isNotBlank() && currentPacking.none { it.cartonNumber == cartonNumber.trim() }) {
            "Each carton needs a unique non-empty number"
        }
        val ordered = purchaseOrderDao.getItemsForOrderSync(orderId).associateBy { it.productId to it.variantId }
        val existingQty = currentPacking.groupBy { it.productId to it.variantId }.mapValues { (_, rows) -> rows.sumOf { it.quantity.toLong() } }
        lines.groupBy { it.productId to it.variantId }.forEach { (key, rows) ->
            val item = requireNotNull(ordered[key]) { "Packed variant is not in the purchase order" }
            require((existingQty[key] ?: 0L) + rows.sumOf { it.quantity.toLong() } <= item.quantity.toLong()) {
                "Packed quantity exceeds the ordered quantity"
            }
        }
        // Run allocation engine
        val lineInputs = lines.map {
            MixedCartonLineInput(
                productId = it.productId,
                variantId = it.variantId,
                productName = it.productName,
                quantity = it.quantity,
                manualGrossWeightKg = it.manualGrossWeightKg
            )
        }
        val allocated = CartonWeightAllocationEngine.allocateWeights(
            cartonGrossWeightKg = grossWeightKg,
            cartonNetWeightKg = netWeightKg,
            lines = lineInputs
        )

        val dimensions = listOf(lengthCm, widthCm, heightCm)
        require(dimensions.all { it.signum() == 0 } || dimensions.all { it > BigDecimal.ZERO }) {
            "Enter all three carton dimensions or leave all blank"
        }
        val cbm = if (dimensions.all { it.signum() == 0 }) BigDecimal.ZERO
            else CbmCalculator.calculateCbm(lengthCm, widthCm, heightCm, 1)
        val totalQty = lines.sumOf { it.quantity }

        val insertedIds = mutableListOf<String>()
        allocated.forEach { alloc ->
            val cartonId = "ctn-${UUID.randomUUID()}"
            val product = productDao.getProductById(alloc.productId)
            val resolvedCatId = product?.categoryId
            val resolvedCatName = resolvedCatId?.let { database.productCategoryDao().getCategoryById(it)?.nameEn }
            val cartonEntity = ShipmentCarton(
                id = cartonId,
                shipmentId = batchShipment.id,
                cartonNumber = cartonNumber.trim(),
                productId = alloc.productId,
                variantId = alloc.variantId,
                quantity = alloc.quantity,
                lengthCm = lengthCm.toPlainString(),
                widthCm = widthCm.toPlainString(),
                heightCm = heightCm.toPlainString(),
                grossWeightKg = alloc.allocatedGrossWeightKg.toPlainString(),
                netWeightKg = alloc.allocatedNetWeightKg.toPlainString(),
                cbm = cbm.toPlainString(),
                purchaseOrderId = orderId,
                isMixedCarton = lines.size > 1,
                totalCartonQuantity = totalQty,
                cartonGrossWeightKg = grossWeightKg.toPlainString(),
                supplierId = po.supplierId,
                categoryId = resolvedCatId,
                categoryName = resolvedCatName
            )
            importShipmentDao.insertCarton(cartonEntity)
            insertedIds.add(cartonId)
        }

        // Refresh totals on purchase order and batch shipment
        val allCartons = importShipmentDao.getCartonsForOrderSync(orderId)
        val distinctCartonsCount = allCartons.map { it.cartonNumber }.distinct().size
        val totalGross = allCartons.fold(BigDecimal.ZERO) { acc, c -> acc.add(BigDecimal(c.grossWeightKg.ifBlank { "0" })) }
        val totalNet = allCartons.fold(BigDecimal.ZERO) { acc, c -> acc.add(BigDecimal(c.netWeightKg.ifBlank { "0" })) }
        val totalCbm = allCartons.groupBy { it.cartonNumber }.values.fold(BigDecimal.ZERO) { acc, cList ->
            acc.add(BigDecimal(cList.first().cbm.ifBlank { "0" }))
        }

        purchaseOrderDao.updatePackingSummary(
            orderId = orderId,
            grossWeight = totalGross.toPlainString(),
            netWeight = totalNet.toPlainString(),
            cbm = totalCbm.toPlainString(),
            cartons = distinctCartonsCount
        )

        importShipmentDao.updateShipment(
            batchShipment.copy(
                totalCartons = distinctCartonsCount,
                totalQuantity = allCartons.sumOf { it.quantity },
                totalGrossWeightKg = totalGross.toPlainString(),
                totalNetWeightKg = totalNet.toPlainString(),
                totalCbm = totalCbm.toPlainString(),
                updatedAt = System.currentTimeMillis()
            )
        )
        syncShipmentCategoryFreightRates(batchShipment.id)

        insertedIds
    }

    override fun getCartonsForOrder(orderId: String): Flow<List<ShipmentCarton>> =
        importShipmentDao.getCartonsForOrder(orderId)

    override suspend fun getCartonsForOrderSync(orderId: String): List<ShipmentCarton> =
        importShipmentDao.getCartonsForOrderSync(orderId)

    override suspend fun addPurchaseCostItem(
        orderId: String,
        category: CostCategory,
        description: String,
        amountMinor: Long,
        currency: String,
        exchangeRate: BigDecimal,
        method: AllocationMethod,
        isEstimated: Boolean,
        paymentAccount: String,
        docRef: String?,
        notes: String?,
        manualAllocationsBdtMinor: Map<String, Long>
    ): String {
        val po = purchaseOrderDao.getOrderById(orderId)
            ?: throw IllegalArgumentException("Purchase order not found: $orderId")
        val batchShipment = importShipmentDao.getBatchShipmentForOrder(orderId)
            ?: throw IllegalStateException("Batch shipment not found for order: $orderId")

        val bdtAmountMinor = BigDecimal(amountMinor)
            .multiply(exchangeRate)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()

        if (method == AllocationMethod.MANUAL) {
            val expected = BigDecimal(amountMinor).multiply(exchangeRate).setScale(0, RoundingMode.HALF_UP).longValueExact()
            require(manualAllocationsBdtMinor.isNotEmpty() && manualAllocationsBdtMinor.values.all { it >= 0 } &&
                manualAllocationsBdtMinor.values.fold(0L, Math::addExact) == expected) { "Manual BDT allocations must equal the expense total" }
        }
        val costId = "cost-${UUID.randomUUID()}"
        val costItem = ImportCostItem(
            id = costId,
            shipmentId = batchShipment.id,
            purchaseOrderId = orderId,
            costCategory = category.name,
            description = description,
            amountMinor = amountMinor,
            currency = currency,
            exchangeRateToBdt = exchangeRate.toPlainString(),
            bdtAmountMinor = bdtAmountMinor,
            costDate = System.currentTimeMillis(),
            paymentAccount = paymentAccount,
            documentReference = docRef,
            allocationMethod = method.name,
            manualAllocationsJson = LandedCostAllocationEngine.formatManualAllocations(manualAllocationsBdtMinor),
            isPosted = false,
            isEstimated = isEstimated,
            isPaid = false,
            notes = notes,
            createdAt = System.currentTimeMillis()
        )

        importCostDao.insertCostItem(costItem)
        return costId
    }

    override fun getCostItemsForOrder(orderId: String): Flow<List<ImportCostItem>> =
        importCostDao.getCostItemsForOrder(orderId)

    override suspend fun getCostItemsForOrderSync(orderId: String): List<ImportCostItem> =
        importCostDao.getCostItemsForOrderSync(orderId)

    override suspend fun finalizePurchaseActualCost(orderId: String): List<LandedCostSnapshot> {
        val po = purchaseOrderDao.getOrderById(orderId)
            ?: throw IllegalArgumentException("Purchase order not found: $orderId")

        check(!po.isActualCostFinalized) {
            "Duplicate finalization prevented: Purchase order ${po.poNumber} actual costs are already finalized!"
        }

        val batchShipment = importShipmentDao.getBatchShipmentForOrder(orderId)
            ?: throw IllegalStateException("Batch shipment not found for order: $orderId")

        val orderActualCosts = importCostDao.getActualCostItemsForOrderSync(orderId)
        val shipmentActualCosts = importCostDao.getCostItemsForShipmentSync(batchShipment.id).filter { !it.isEstimated }
        val actualCosts = (orderActualCosts + shipmentActualCosts)
            .distinctBy { it.id }
            .filter { !it.costCategory.equals(CostCategory.PRODUCT_PURCHASE.name, ignoreCase = true) }

        // Get product items for allocation inputs
        val poItems = purchaseOrderDao.getItemsForOrderSync(orderId)
        require(poItems.isNotEmpty()) { "Purchase order has no items to allocate costs" }

        val cartons = importShipmentDao.getCartonsForOrderSync(orderId)

        val allocationInputs = poItems.map { item ->
            val product = productDao.getProductById(item.productId)
            val variant = if (item.variantId != null) productVariantDao.getVariantById(item.variantId) else null

            // Weight: from cartons if entered, else from poItem summary
            val itemCartons = cartons.filter { it.productId == item.productId && (item.variantId == null || it.variantId == item.variantId) }
            val grossWeight = if (itemCartons.isNotEmpty()) {
                itemCartons.fold(BigDecimal.ZERO) { acc, c -> acc.add(BigDecimal(c.grossWeightKg.ifBlank { "0" })) }
            } else {
                BigDecimal(item.grossWeightKg.ifBlank { "0" })
            }

            val cbm = if (itemCartons.isNotEmpty()) {
                itemCartons.fold(BigDecimal.ZERO) { acc, c -> acc.add(BigDecimal(c.cbm.ifBlank { "0" })) }
            } else {
                BigDecimal(item.cbm.ifBlank { "0" })
            }

            val name = "${product?.nameEn ?: item.productId}${if (variant != null) " - ${variant.color ?: ""}" else ""}"

            AllocationItemInput(
                id = if (item.variantId != null) "${item.productId}:${item.variantId}" else item.productId,
                name = name,
                quantity = item.quantity,
                grossWeightKg = grossWeight,
                cbm = cbm,
                purchaseValueBdtMinor = item.totalBdtAmountMinor
            )
        }

        val allAllocations = mutableListOf<LandedCostAllocation>()
        val totalAllocatedPerItem = mutableMapOf<String, Long>()
        val totalActualCostBdt = actualCosts.sumOf { it.bdtAmountMinor }
        var totalAllocatedExpense = 0L

        actualCosts.forEach { costItem ->
            val method = try {
                AllocationMethod.valueOf(costItem.allocationMethod)
            } catch (_: Exception) {
                AllocationMethod.PURCHASE_VALUE
            }
            val manual = LandedCostAllocationEngine.parseManualAllocations(costItem.manualAllocationsJson)
            val inputs = if (method == AllocationMethod.MANUAL) allocationInputs.map {
                it.copy(manualAmountBdtMinor = manual[it.id] ?: 0L)
            } else allocationInputs
            val results = LandedCostAllocationEngine.allocate(costItem.bdtAmountMinor, inputs, method)

            results.forEach { res ->
                val parts = res.itemId.split(":")
                val productId = parts[0]
                val variantId = if (parts.size > 1) parts[1] else null

                allAllocations.add(
                    LandedCostAllocation(
                        id = "lca-${UUID.randomUUID()}",
                        shipmentId = batchShipment.id,
                        costItemId = costItem.id,
                        productId = productId,
                        variantId = variantId,
                        allocationBasis = costItem.allocationMethod,
                        basisValue = res.basisValue.toPlainString(),
                        basisPercentage = res.basisPercentage.toPlainString(),
                        allocatedBdtMinor = res.allocatedBdtMinor,
                        createdAt = System.currentTimeMillis()
                    )
                )

                totalAllocatedPerItem[res.itemId] = (totalAllocatedPerItem[res.itemId] ?: 0L) + res.allocatedBdtMinor
                totalAllocatedExpense += res.allocatedBdtMinor
            }
        }

        // Verify reconciliation: total allocated must match total actual cost exactly
        check(totalAllocatedExpense == totalActualCostBdt) {
            "Cost allocation mismatch: Total allocated ($totalAllocatedExpense) != Total actual ($totalActualCostBdt)"
        }

        // If no explicit freight cost item was recorded, check category-wise freight rate overrides (or import shipment defaults)
        val hasExplicitFreight = actualCosts.any {
            it.costCategory in setOf(
                CostCategory.INTERNATIONAL_FREIGHT.name,
                CostCategory.SEA_FREIGHT.name,
                CostCategory.AIR_FREIGHT.name
            )
        }
        val freightPerKey = mutableMapOf<String, Long>()
        if (!hasExplicitFreight) {
            val syncedRates = syncShipmentCategoryFreightRates(batchShipment.id)
            val applicableRates = syncedRates.filter { rate ->
                rate.overrideRatePerKgBdtMinor != null ||
                    (!po.currency.equals("BDT", ignoreCase = true) && rate.defaultRatePerKgBdtMinor > 0L)
            }
            if (applicableRates.isNotEmpty()) {
                val poProducts = poItems.map { item ->
                    val prod = productDao.getProductById(item.productId)
                    val variant = if (item.variantId != null) productVariantDao.getVariantById(item.variantId) else null
                    val key = if (item.variantId != null) "${item.productId}:${item.variantId}" else item.productId
                    val itemWeight = allocationInputs.find { it.id == key }?.grossWeightKg ?: BigDecimal.ZERO
                    LandingCostEngine.ShipmentProductInput(
                        productId = item.productId,
                        variantId = item.variantId,
                        productNameEn = prod?.nameEn ?: item.productId,
                        productNameBn = prod?.nameBn ?: "",
                        sku = variant?.sku ?: prod?.baseSku ?: "",
                        supplierId = po.supplierId,
                        purchaseOrderId = orderId,
                        categoryId = prod?.categoryId ?: "cat-uncategorized",
                        packedQuantity = item.quantity,
                        unitPurchasePriceForeignMinor = item.unitPriceMinor,
                        purchaseCurrency = po.currency,
                        purchaseExchangeRateToBdt = BigDecimal(po.exchangeRateToBdt.ifBlank { "1.0" }),
                        goodQuantity = item.quantity,
                        fallbackGrossWeightKg = itemWeight
                    )
                }
                val rateInputs = applicableRates.map { r ->
                    LandingCostEngine.CategoryFreightRateInput(
                        categoryId = r.categoryId,
                        categoryNameEn = r.categoryNameEn,
                        defaultRatePerKgBdtMinor = r.defaultRatePerKgBdtMinor,
                        overrideRatePerKgBdtMinor = r.overrideRatePerKgBdtMinor
                    )
                }
                val freightResults = LandingCostEngine.calculateCategoryInternationalFreight(
                    products = poProducts,
                    productWeightsKg = emptyMap(),
                    categoryRates = rateInputs
                )
                freightResults.forEach { res ->
                    res.productAllocationsBdtMinor.forEach { (key, amt) ->
                        freightPerKey[key] = (freightPerKey[key] ?: 0L) + amt
                        totalAllocatedPerItem[key] = (totalAllocatedPerItem[key] ?: 0L) + amt
                    }
                }
            }
        }

        // Build finalized immutable snapshots (per distinct productId + variantId)
        val now = System.currentTimeMillis()
        val snapshots = allocationInputs.map { input ->
            val parts = input.id.split(":")
            val productId = parts[0]
            val variantId = if (parts.size > 1) parts[1] else null

            val product = productDao.getProductById(productId)
            val variant = if (variantId != null) productVariantDao.getVariantById(variantId) else null

            val allocatedBdt = totalAllocatedPerItem[input.id] ?: 0L
            val purchaseBdt = input.purchaseValueBdtMinor
            val landedBdt = purchaseBdt + allocatedBdt
            val unitLandedBdt = if (input.quantity > 0) {
                BigDecimal(landedBdt).divide(BigDecimal(input.quantity), 0, RoundingMode.HALF_UP).longValueExact()
            } else {
                0L
            }

            LandedCostSnapshot(
                id = "lcs-${UUID.randomUUID()}",
                shipmentId = batchShipment.id,
                productId = productId,
                variantId = variantId,
                productNameEn = product?.nameEn ?: productId,
                productNameBn = product?.nameBn ?: "",
                sku = variant?.sku ?: product?.baseSku ?: "",
                totalReceivedQuantity = input.quantity,
                totalPurchaseCostBdtMinor = purchaseBdt,
                totalAllocatedCostsBdtMinor = allocatedBdt,
                totalLandedCostBdtMinor = landedBdt,
                unitLandedCostBdtMinor = unitLandedBdt,
                snapshotDate = now,
                isFinal = true,
                internationalFreightBdtMinor = freightPerKey[input.id] ?: 0L,
                packedQuantity = input.quantity,
                goodQuantity = input.quantity
            )
        }

        database.withTransaction {
            importCostDao.replaceAllocationsAndSnapshots(batchShipment.id, allAllocations, snapshots)
            if (actualCosts.isNotEmpty()) {
                importCostDao.markCostItemsCapitalized(actualCosts.map { it.id }, "FINALIZED")
            }

            // Update any pre-existing cost layers on this batch shipment per product+variant
            val existingLayers = inventoryCostLayerDao.getLayersForShipmentSync(batchShipment.id)
            for (layer in existingLayers) {
                val matchingSnapshot = snapshots.find { it.productId == layer.productId && it.variantId == layer.variantId }
                    ?: snapshots.find { it.productId == layer.productId && it.variantId == null }
                if (matchingSnapshot != null) {
                    val unitPurchase = if (matchingSnapshot.totalReceivedQuantity > 0) {
                        BigDecimal(matchingSnapshot.totalPurchaseCostBdtMinor)
                            .divide(BigDecimal(matchingSnapshot.totalReceivedQuantity), 0, RoundingMode.HALF_UP)
                            .longValueExact()
                    } else 0L
                    inventoryCostLayerDao.updateCostLayerUnitCost(
                        layerId = layer.id,
                        unitCostMinor = matchingSnapshot.unitLandedCostBdtMinor,
                        allocatedExpenseMinor = (matchingSnapshot.unitLandedCostBdtMinor - unitPurchase).coerceAtLeast(0L)
                    )
                }
            }

            // Mark PO and Batch as ACTUAL_COST_FINALIZED
            purchaseOrderDao.markActualCostFinalized(orderId, PurchaseOrderStatus.ACTUAL_COST_FINALIZED.name, now)
            importShipmentDao.updateShipmentStatus(batchShipment.id, ShipmentStatus.ARRIVED.name)
        }

        return snapshots
    }

    override suspend fun createPurchaseReceiving(
        orderId: String,
        warehouseId: String,
        receivingMethod: String,
        items: List<ReceiptItemDraft>,
        notes: String?,
        postImmediately: Boolean
    ): String = database.withTransaction {
        require(items.isNotEmpty()) { "Receiving must contain at least 1 item" }
        val po = purchaseOrderDao.getOrderById(orderId)
            ?: throw IllegalArgumentException("Purchase order not found: $orderId")
        val batchShipment = importShipmentDao.getBatchShipmentForOrder(orderId)
            ?: throw IllegalStateException("Batch shipment not found for order: $orderId")

        val receiptCount = warehouseReceiptDao.getReceiptCount()
        val receiptNumber = "WR-2026-%04d".format(receiptCount + 1)
        val receiptId = "wr-${UUID.randomUUID()}"

        // Snapshots if finalized
        val snapshots = importCostDao.getSnapshotsForShipmentSync(batchShipment.id).associateBy {
            if (it.variantId != null) "${it.productId}:${it.variantId}" else it.productId
        }

        var hasDiscrepancy = false
        val itemEntities = items.map { draft ->
            val key = if (draft.variantId != null) "${draft.productId}:${draft.variantId}" else draft.productId
            val snapshot = snapshots[key] ?: snapshots[draft.productId]
            val unitLandedCost = snapshot?.unitLandedCostBdtMinor ?: 0L

            val resolvedGood = if (draft.goodQuantity >= 0) {
                draft.goodQuantity
            } else if (draft.receivedQuantity + draft.damagedQuantity + draft.missingQuantity == draft.expectedQuantity) {
                draft.receivedQuantity
            } else {
                (draft.receivedQuantity - draft.damagedQuantity).coerceAtLeast(0)
            }
            val resolvedPending = if (draft.pendingQuantity >= 0) {
                draft.pendingQuantity
            } else {
                (draft.expectedQuantity - resolvedGood - draft.damagedQuantity - draft.missingQuantity).coerceAtLeast(0)
            }

            // Phase 3 Validation: Packed Quantity = Good + Damaged + Missing + Pending
            if (draft.excessQuantity == 0) {
                LandingCostEngine.validateReceivingQuantities(
                    packedQuantity = draft.expectedQuantity,
                    goodQuantity = resolvedGood,
                    damagedQuantity = draft.damagedQuantity,
                    missingQuantity = draft.missingQuantity,
                    pendingQuantity = resolvedPending
                )
            }

            if (resolvedGood != draft.expectedQuantity || draft.damagedQuantity > 0 || draft.missingQuantity > 0 || resolvedPending > 0) {
                hasDiscrepancy = true
            }

            WarehouseReceiptItem(
                id = "wri-${UUID.randomUUID()}",
                receiptId = receiptId,
                cartonId = draft.cartonId,
                productId = draft.productId,
                variantId = draft.variantId,
                expectedQuantity = draft.expectedQuantity,
                receivedQuantity = draft.receivedQuantity,
                damagedQuantity = draft.damagedQuantity,
                missingQuantity = draft.missingQuantity,
                excessQuantity = draft.excessQuantity,
                unitLandedCostMinor = unitLandedCost,
                conditionNotes = draft.conditionNotes,
                goodQuantity = resolvedGood,
                pendingQuantity = resolvedPending
            )
        }

        val receipt = WarehouseReceipt(
            id = receiptId,
            receiptNumber = receiptNumber,
            shipmentId = batchShipment.id,
            purchaseOrderId = orderId,
            receivingMethod = receivingMethod,
            warehouseId = warehouseId,
            receiptDate = System.currentTimeMillis(),
            status = ReceiptStatus.DRAFT.name,
            receivedBy = "Warehouse Receiving Manager",
            hasDiscrepancies = hasDiscrepancy,
            discrepancyApproved = false,
            notes = notes,
            createdAt = System.currentTimeMillis()
        )

        warehouseReceiptDao.saveReceiptWithItems(receipt, itemEntities)
        if (postImmediately) postPurchaseReceiving(receiptId, allowDiscrepancy = true)
        receiptId
    }

    override suspend fun postPurchaseReceiving(receiptId: String, allowDiscrepancy: Boolean) {
        database.withTransaction {
            val receipt = warehouseReceiptDao.getReceiptById(receiptId)
                ?: throw IllegalArgumentException("Warehouse receipt not found: $receiptId")
            check(receipt.status != ReceiptStatus.POSTED.name) {
                "Duplicate posting prevented: Warehouse receipt ${receipt.receiptNumber} is already posted!"
            }
            val orderId = receipt.purchaseOrderId
                ?: importShipmentDao.getShipmentById(receipt.shipmentId)?.purchaseOrderId
                ?: throw IllegalStateException("Receipt is not associated with a Purchase Order: $receiptId")
            val po = purchaseOrderDao.getOrderById(orderId)
                ?: throw IllegalArgumentException("Purchase order not found: $orderId")
            check(po.status != PurchaseOrderStatus.COMPLETED.name && po.status != PurchaseOrderStatus.CANCELLED.name) {
                "Cannot post a receipt for a closed purchase order"
            }
            check(po.isActualCostFinalized) {
                "Cannot post stock receiving: Actual Landing Cost must be finalized before posting stock into inventory!"
            }

            val items = warehouseReceiptDao.getItemsForReceiptSync(receiptId)
            require(items.isNotEmpty()) { "Cannot post empty warehouse receipt" }
            val batchShipment = importShipmentDao.getShipmentById(receipt.shipmentId)
            val snapshots = importCostDao.getSnapshotsForShipmentSync(receipt.shipmentId).associateBy {
                if (it.variantId != null) "${it.productId}:${it.variantId}" else it.productId
            }
            val currentItems = purchaseOrderDao.getItemsForOrderSync(orderId)
            val poItems = currentItems.associateBy {
                if (it.variantId != null) "${it.productId}:${it.variantId}" else it.productId
            }
            items.groupBy { it.productId to it.variantId }.forEach { (key, lines) ->
                val ordered = currentItems.firstOrNull { it.productId == key.first && it.variantId == key.second }
                    ?: error("Received variant is not in the PO")
                require(lines.sumOf { it.receivedQuantity.toLong() } <= ordered.quantity.toLong() - ordered.receivedQuantity) {
                    "Received quantity exceeds the remaining PO quantity"
                }
            }
            var totalGoodValueBdt = 0L
            var totalDamageValueBdt = 0L
            var totalMissingValueBdt = 0L

            items.forEach { item ->
                val key = if (item.variantId != null) "${item.productId}:${item.variantId}" else item.productId
                val snapshot = snapshots[key] ?: snapshots[item.productId]
                val poItem = poItems[key] ?: poItems[item.productId]
                val product = productDao.getProductById(item.productId)
                val variant = if (item.variantId != null) productVariantDao.getVariantById(item.variantId) else null

                // Calculate precise unit landed cost: finalized snapshot (purchase + freight + expenses) or PO line + allocated
                val unitPurchaseCostBdt = when {
                    poItem != null && poItem.quantity > 0 ->
                        BigDecimal(poItem.totalBdtAmountMinor)
                            .divide(BigDecimal(poItem.quantity), 0, RoundingMode.HALF_UP)
                            .longValueExact()
                    variant != null && variant.variantCostMinor > 0L -> variant.variantCostMinor
                    else -> product?.purchasePriceMinor ?: 0L
                }
                val unitAllocatedCostBdt = if (snapshot != null && snapshot.totalReceivedQuantity > 0) {
                    BigDecimal(snapshot.totalAllocatedCostsBdtMinor)
                        .divide(BigDecimal(snapshot.totalReceivedQuantity), 0, RoundingMode.HALF_UP)
                        .longValueExact()
                } else 0L

                val finalUnitCost = snapshot?.unitLandedCostBdtMinor?.takeIf { it > 0L }
                    ?: (unitPurchaseCostBdt + unitAllocatedCostBdt)
                val effectiveAllocatedPerUnit = (finalUnitCost - unitPurchaseCostBdt).coerceAtLeast(unitAllocatedCostBdt)

                val goodQuantity = (item.receivedQuantity - item.damagedQuantity).coerceAtLeast(0)

                var balanceAfterReceipt = (if (item.variantId != null) {
                    stockBalanceDao.getVariantBalance(receipt.warehouseId, item.productId, item.variantId)
                } else {
                    stockBalanceDao.getBalance(receipt.warehouseId, item.productId)
                })?.quantity ?: 0

                // 1. Post Sound / Good Quantity into Stock
                if (goodQuantity > 0) {
                    val existingBalance = if (item.variantId != null) {
                        stockBalanceDao.getVariantBalance(receipt.warehouseId, item.productId, item.variantId)
                    } else {
                        stockBalanceDao.getBalance(receipt.warehouseId, item.productId)
                    }

                    val newBalance = if (existingBalance != null) {
                        existingBalance.copy(
                            quantity = existingBalance.quantity + goodQuantity,
                            updatedAt = System.currentTimeMillis()
                        )
                    } else {
                        StockBalance(
                            id = "bal-${UUID.randomUUID()}",
                            warehouseId = receipt.warehouseId,
                            productId = item.productId,
                            variantId = item.variantId,
                            quantity = goodQuantity,
                            reservedQuantity = 0,
                            updatedAt = System.currentTimeMillis()
                        )
                    }
                    stockBalanceDao.insertOrUpdate(newBalance)
                    balanceAfterReceipt = newBalance.quantity

                    // Stock Movement
                    val movementNumber = MovementNumberGenerator.generate("SM-2026")
                    stockMovementDao.insertMovement(
                        StockMovement(
                            id = MovementNumberGenerator.generateId("mov"),
                            movementNumber = movementNumber,
                            warehouseId = receipt.warehouseId,
                            productId = item.productId,
                            variantId = item.variantId,
                            movementType = StockMovementType.PURCHASE_RECEIPT.code,
                            quantity = goodQuantity,
                            unitCostMinor = finalUnitCost,
                            balanceAfterMovement = newBalance.quantity,
                            totalCostImpactMinor = goodQuantity.toLong() * finalUnitCost,
                            referenceType = "WAREHOUSE_RECEIPT",
                            referenceId = receipt.receiptNumber,
                            purchaseOrderId = po.poNumber,
                            purchaseBatchId = receipt.receiptNumber,
                            notes = "Good stock check-in from ${po.poNumber}",
                            createdAt = System.currentTimeMillis()
                        )
                    )

                    // Distinct FIFO Cost Layer preserving exact PO buy price & unit landed cost (never averaged)
                    val goodValue = goodQuantity.toLong() * finalUnitCost
                    totalGoodValueBdt += goodValue

                    inventoryCostLayerDao.insertCostLayer(
                        InventoryCostLayer(
                            id = "icl-${UUID.randomUUID()}",
                            productId = item.productId,
                            variantId = item.variantId,
                            warehouseId = receipt.warehouseId,
                            shipmentId = receipt.shipmentId,
                            shipmentNumber = batchShipment?.shipmentNumber ?: "BATCH-${po.poNumber}",
                            warehouseReceiptId = receipt.id,
                            receiptNumber = receipt.receiptNumber,
                            initialQuantity = goodQuantity,
                            remainingQuantity = goodQuantity,
                            unitLandedCostBdtMinor = finalUnitCost,
                            originalCurrency = po.currency,
                            originalPurchasePriceMinor = poItem?.unitPriceMinor ?: variant?.variantCostMinor ?: product?.purchasePriceMinor ?: 0L,
                            exchangeRateToBdt = po.exchangeRateToBdt,
                            allocatedExpensePerUnitBdtMinor = effectiveAllocatedPerUnit,
                            receiptDate = receipt.receiptDate,
                            isActive = true,
                            purchaseOrderId = po.id,
                            purchaseBatchId = "BATCH-${po.poNumber}",
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }

                // 2. Damaged Stock Write-Off
                if (item.damagedQuantity > 0) {
                    val dmgLoss = item.damagedQuantity.toLong() * finalUnitCost
                    totalDamageValueBdt += dmgLoss
                    val curQty = (if (item.variantId != null) {
                        stockBalanceDao.getVariantBalance(receipt.warehouseId, item.productId, item.variantId)
                    } else {
                        stockBalanceDao.getBalance(receipt.warehouseId, item.productId)
                    })?.quantity ?: 0

                    val dmgNumber = MovementNumberGenerator.generate("DMG-2026")
                    minimalImportDao.insertDamageRecord(
                        DamageRecord(
                            id = MovementNumberGenerator.generateId("dmg"),
                            recordNumber = dmgNumber,
                            purchaseOrderId = po.id,
                            warehouseReceiptId = receipt.id,
                            warehouseId = receipt.warehouseId,
                            productId = item.productId,
                            variantId = item.variantId,
                            damagedQuantity = item.damagedQuantity,
                            unitCostMinor = finalUnitCost,
                            totalLossMinor = dmgLoss,
                            reason = item.conditionNotes ?: "Damaged during transit / handling",
                            disposition = "WRITE_OFF",
                            createdAt = System.currentTimeMillis()
                        )
                    )

                    stockMovementDao.insertMovement(
                        StockMovement(
                            id = MovementNumberGenerator.generateId("mov-dmg"),
                            movementNumber = dmgNumber,
                            warehouseId = receipt.warehouseId,
                            productId = item.productId,
                            variantId = item.variantId,
                            movementType = StockMovementType.DAMAGE_WRITE_OFF.code,
                            quantity = item.damagedQuantity,
                            unitCostMinor = finalUnitCost,
                            balanceAfterMovement = balanceAfterReceipt,
                            totalCostImpactMinor = dmgLoss,
                            referenceType = "WAREHOUSE_RECEIPT",
                            referenceId = receipt.receiptNumber,
                            purchaseOrderId = po.poNumber,
                            purchaseBatchId = receipt.receiptNumber,
                            notes = "Damaged during transit. Written off: ${item.conditionNotes ?: ""}",
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }

                // 3. Missing Stock Shortage Discrepancy
                if (item.missingQuantity > 0) {
                    val misLoss = item.missingQuantity.toLong() * finalUnitCost
                    totalMissingValueBdt += misLoss

                    val shtNumber = MovementNumberGenerator.generate("SHT-2026")
                    minimalImportDao.insertShortageRecord(
                        ShortageRecord(
                            id = MovementNumberGenerator.generateId("sht"),
                            recordNumber = shtNumber,
                            purchaseOrderId = po.id,
                            warehouseReceiptId = receipt.id,
                            warehouseId = receipt.warehouseId,
                            productId = item.productId,
                            variantId = item.variantId,
                            missingQuantity = item.missingQuantity,
                            unitCostMinor = finalUnitCost,
                            totalAmountMinor = misLoss,
                            disposition = "SUPPLIER_CLAIM",
                            claimReference = "CLM-${po.poNumber}",
                            notes = "Missing / Short shipment from supplier",
                            createdAt = System.currentTimeMillis()
                        )
                    )

                    stockMovementDao.insertMovement(
                        StockMovement(
                            id = MovementNumberGenerator.generateId("mov-mis"),
                            movementNumber = shtNumber,
                            warehouseId = receipt.warehouseId,
                            productId = item.productId,
                            variantId = item.variantId,
                            movementType = StockMovementType.SHORTAGE.code,
                            quantity = item.missingQuantity,
                            unitCostMinor = finalUnitCost,
                            balanceAfterMovement = balanceAfterReceipt,
                            totalCostImpactMinor = misLoss,
                            referenceType = "WAREHOUSE_RECEIPT",
                            referenceId = receipt.receiptNumber,
                            purchaseOrderId = po.poNumber,
                            purchaseBatchId = receipt.receiptNumber,
                            notes = "Missing / Shortage in transit. Excluded from stock.",
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }

                // Update PO item received quantity
                if (poItem != null) {
                    purchaseOrderDao.incrementReceivedQuantity(poItem.id, item.receivedQuantity)
                }
            }

            // 4. Mark receipt as POSTED
            warehouseReceiptDao.markReceiptPosted(
                receiptId = receiptId,
                status = ReceiptStatus.POSTED.name,
                postedAt = System.currentTimeMillis()
            )

            // 5. Update PO and batch shipment status
            val updatedPoItems = purchaseOrderDao.getItemsForOrderSync(orderId)
            val totalOrdered = updatedPoItems.sumOf { it.quantity }
            val totalRecv = updatedPoItems.sumOf { it.receivedQuantity }
            val newPoStatus = if (totalRecv >= totalOrdered && totalOrdered > 0) {
                PurchaseOrderStatus.RECEIVED.name
            } else {
                PurchaseOrderStatus.PARTIALLY_RECEIVED.name
            }
            purchaseOrderDao.updateOrderStatus(orderId, newPoStatus)
            importShipmentDao.updateShipmentStatus(receipt.shipmentId, ShipmentStatus.COMPLETED.name, arrivalDate = receipt.receiptDate)

            // 6. Post Double-Entry Accounting Journals
            // Good stock: Dr. 1210 Inventory Asset, Cr. 1220 In-Transit Inventory
            if (totalGoodValueBdt > 0L) {
                accountingRepository.postJournalEntry(
                    entryDate = receipt.receiptDate,
                    descEn = "Inventory Capitalization for ${receipt.receiptNumber}",
                    descBn = "মজুদ পণ্য ক্যাপিটালাইজেশন (${receipt.receiptNumber})",
                    referenceType = "WAREHOUSE_RECEIPT",
                    referenceId = receipt.receiptNumber,
                    lines = listOf(
                        JournalLineDraft(
                            accountId = "coa-1210", // Inventory Asset
                            isDebit = true,
                            amountMinor = totalGoodValueBdt,
                            description = "Capitalize good units into warehouse stock"
                        ),
                        JournalLineDraft(
                            accountId = "coa-1220", // In-Transit Inventory
                            isDebit = false,
                            amountMinor = totalGoodValueBdt,
                            description = "Clear in-transit inventory for received good units"
                        )
                    )
                )
            }

            // Damaged stock: Dr. 5050 Damaged Merchandise Write-Off, Cr. 1220 In-Transit Inventory
            if (totalDamageValueBdt > 0L) {
                accountingRepository.postJournalEntry(
                    entryDate = receipt.receiptDate,
                    descEn = "Damaged Goods Write-Off for ${receipt.receiptNumber}",
                    descBn = "নষ্ট পণ্য অবলোপন হিসাব (${receipt.receiptNumber})",
                    referenceType = "DAMAGE_WRITE_OFF",
                    referenceId = receipt.receiptNumber,
                    lines = listOf(
                        JournalLineDraft(
                            accountId = "coa-5050", // Damaged Merchandise Write-Off
                            isDebit = true,
                            amountMinor = totalDamageValueBdt,
                            description = "Loss on damaged goods during transit"
                        ),
                        JournalLineDraft(
                            accountId = "coa-1220", // In-Transit Inventory
                            isDebit = false,
                            amountMinor = totalDamageValueBdt,
                            description = "Clear in-transit inventory for damaged units"
                        )
                    )
                )
            }

            // Missing stock: Dr. 1050 Supplier Shortage & Claims Receivable, Cr. 1220 In-Transit Inventory
            if (totalMissingValueBdt > 0L) {
                accountingRepository.postJournalEntry(
                    entryDate = receipt.receiptDate,
                    descEn = "Shortage Discrepancy Claim for ${receipt.receiptNumber}",
                    descBn = "ঘাটতি ও সরবরাহকারী দাবি (${receipt.receiptNumber})",
                    referenceType = "SHORTAGE_CLAIM",
                    referenceId = receipt.receiptNumber,
                    lines = listOf(
                        JournalLineDraft(
                            accountId = "coa-1050", // Supplier Shortage & Claims Receivable
                            isDebit = true,
                            amountMinor = totalMissingValueBdt,
                            description = "Supplier claim receivable for missing units in shipment"
                        ),
                        JournalLineDraft(
                            accountId = "coa-1220", // In-Transit Inventory
                            isDebit = false,
                            amountMinor = totalMissingValueBdt,
                            description = "Clear in-transit inventory for missing shortage units"
                        )
                    )
                )
            }
        }
    }

    override fun getReceiptsForOrder(orderId: String): Flow<List<WarehouseReceipt>> =
        warehouseReceiptDao.getReceiptsByOrder(orderId)

    override suspend fun addAdditionalCost(
        orderId: String,
        warehouseId: String?,
        category: AdditionalCostCategory,
        description: String,
        amountMinor: Long,
        isCapitalizable: Boolean,
        allocationMethod: String,
        notes: String?
    ): String {
        require(amountMinor > 0) { "Cost amount must be positive" }
        val po = purchaseOrderDao.getOrderById(orderId)
            ?: throw IllegalArgumentException("Purchase order not found: $orderId")

        val costCount = minimalImportDao.getAdditionalCostCount()
        val costNumber = "AC-2026-%04d".format(costCount + 1)
        val costId = "ac-${UUID.randomUUID()}"

        val costEntity = AdditionalProductCost(
            id = costId,
            costNumber = costNumber,
            purchaseOrderId = orderId,
            warehouseId = warehouseId,
            category = category.name,
            description = description,
            amountMinor = amountMinor,
            currency = "BDT",
            exchangeRateToBdt = "1.0000",
            bdtAmountMinor = amountMinor,
            isCapitalizable = isCapitalizable,
            allocationMethod = allocationMethod,
            expenseDate = System.currentTimeMillis(),
            notes = notes,
            createdAt = System.currentTimeMillis()
        )

        minimalImportDao.insertAdditionalCost(costEntity)

        // Double-entry accounting for additional cost
        if (isCapitalizable) {
            accountingRepository.postJournalEntry(
                entryDate = System.currentTimeMillis(),
                descEn = "Capitalized Additional Logistics: $description",
                descBn = "ক্যাপিটালাইজড অতিরিক্ত পরিচালন ব্যয়: $description",
                referenceType = "ADDITIONAL_COST",
                referenceId = costNumber,
                lines = listOf(
                    JournalLineDraft(
                        accountId = "coa-1210", // Inventory Asset
                        isDebit = true,
                        amountMinor = amountMinor,
                        description = "Capitalize additional operational cost into inventory"
                    ),
                    JournalLineDraft(
                        accountId = "coa-1020", // City Bank Corporate
                        isDebit = false,
                        amountMinor = amountMinor,
                        description = "Bank payment for additional logistics: $description"
                    )
                )
            )
        } else {
            // Period Expense (e.g. Warehouse Rent) -> Profit & Loss
            accountingRepository.postJournalEntry(
                entryDate = System.currentTimeMillis(),
                descEn = "Period Expense: $description",
                descBn = "পিরিয়ড ব্যয়: $description",
                referenceType = "ADDITIONAL_COST",
                referenceId = costNumber,
                lines = listOf(
                    JournalLineDraft(
                        accountId = "coa-5110", // Office Rent & Warehouse Utilities
                        isDebit = true,
                        amountMinor = amountMinor,
                        description = "Operating period expense (P&L)"
                    ),
                    JournalLineDraft(
                        accountId = "coa-1020", // Bank
                        isDebit = false,
                        amountMinor = amountMinor,
                        description = "Bank payment for rent/period expense: $description"
                    )
                )
            )
        }

        return costId
    }

    override fun getAdditionalCostsForOrder(orderId: String): Flow<List<AdditionalProductCost>> =
        minimalImportDao.getAdditionalCostsForOrder(orderId)

    override suspend fun getAdditionalCostsForOrderSync(orderId: String): List<AdditionalProductCost> =
        minimalImportDao.getAdditionalCostsForOrderSync(orderId)

    override suspend fun completePurchase(
        orderId: String,
        disposition: String?,
        reason: String?,
        notes: String?
    ): PurchaseCompletionRecord = database.withTransaction {
        val existing = minimalImportDao.getCompletionRecordForOrder(orderId)
        if (existing != null) return@withTransaction existing
        val po = purchaseOrderDao.getOrderById(orderId)
            ?: throw IllegalArgumentException("Purchase order not found: $orderId")

        require(po.status !in setOf(PurchaseOrderStatus.DRAFT.name, PurchaseOrderStatus.CANCELLED.name, PurchaseOrderStatus.COMPLETED.name)) {
            "Only an active confirmed purchase can be completed"
        }

        val poItems = purchaseOrderDao.getItemsForOrderSync(orderId)
        val totalOrdered = poItems.sumOf { it.quantity }
        val totalReceived = poItems.sumOf { it.receivedQuantity }
        val remaining = (totalOrdered - totalReceived).coerceAtLeast(0)

        val receipts = warehouseReceiptDao.getReceiptsByOrderSync(orderId).filter { it.status == ReceiptStatus.POSTED.name }
        val allReceiptItems = receipts.flatMap { warehouseReceiptDao.getItemsForReceiptSync(it.id) }
        val goodQty = allReceiptItems.sumOf { (it.receivedQuantity - it.damagedQuantity).coerceAtLeast(0) }
        val damagedQty = allReceiptItems.sumOf { it.damagedQuantity }
        val missingQty = allReceiptItems.sumOf { it.missingQuantity }

        if (remaining > 0) {
            require(disposition in setOf("SHORTAGE", "CANCELLED_BALANCE", "SUPPLIER_CLAIM") && !reason.isNullOrBlank()) {
                "Purchase has $remaining unreceived units. A disposition reason (Shortage, Cancelled Balance, or Supplier Claim) is required!"
            }
        }

        val paymentDue = (po.totalAmountMinor - po.paidAmountMinor).coerceAtLeast(0L)

        val record = PurchaseCompletionRecord(
            id = "comp-${UUID.randomUUID()}",
            purchaseOrderId = orderId,
            completedAt = System.currentTimeMillis(),
            orderedQuantity = totalOrdered,
            receivedQuantity = totalReceived,
            goodQuantity = goodQty,
            damagedQuantity = damagedQty,
            missingQuantity = missingQty,
            remainingQuantity = remaining,
            paymentDueMinor = paymentDue,
            unreceivedDisposition = disposition,
            reason = reason,
            notes = notes
        )

        minimalImportDao.insertCompletionRecord(record)
        purchaseOrderDao.updateOrderStatus(orderId, PurchaseOrderStatus.COMPLETED.name)

        record
    }

    override fun getCompletionRecordForOrder(orderId: String): Flow<PurchaseCompletionRecord?> =
        minimalImportDao.getCompletionRecordForOrderFlow(orderId)

    override fun getDamageRecordsForOrder(orderId: String): Flow<List<DamageRecord>> =
        minimalImportDao.getDamageRecordsForOrder(orderId)

    override fun getShortageRecordsForOrder(orderId: String): Flow<List<ShortageRecord>> =
        minimalImportDao.getShortageRecordsForOrder(orderId)

    private fun cartonCbm(carton: PoMixedCartonDraft): BigDecimal {
        val dimensions = listOf(carton.lengthCm, carton.widthCm, carton.heightCm)
        require(dimensions.all { it.signum() == 0 } || dimensions.all { it > BigDecimal.ZERO }) {
            "Enter all three carton dimensions or leave all blank"
        }
        require(carton.cbm >= BigDecimal.ZERO) { "CBM cannot be negative" }
        return if (dimensions.all { it.signum() == 0 }) carton.cbm
            else CbmCalculator.calculateCbm(carton.lengthCm, carton.widthCm, carton.heightCm, 1)
    }

    private suspend fun requirePackingEditableOrder(orderId: String): PurchaseOrder {
        val po = purchaseOrderDao.getOrderById(orderId) ?: error("Purchase order not found")
        require(!po.isActualCostFinalized && po.status !in listOf(PurchaseOrderStatus.CANCELLED.name, PurchaseOrderStatus.COMPLETED.name)) {
            "Packing is locked for finalized, completed or cancelled orders"
        }
        require(warehouseReceiptDao.getReceiptsByOrderSync(orderId).isEmpty()) { "Packing is locked after receiving starts" }
        return po
    }

    private suspend fun requireEditableOrder(orderId: String): PurchaseOrder {
        val po = purchaseOrderDao.getOrderById(orderId) ?: error("Purchase order not found")
        require(!po.isActualCostFinalized && po.status !in listOf(PurchaseOrderStatus.CANCELLED.name, PurchaseOrderStatus.COMPLETED.name)) {
            "Finalized, completed or cancelled orders cannot be changed"
        }
        require(po.paidAmountMinor == 0L && warehouseReceiptDao.getReceiptsByOrderSync(orderId).isEmpty()) {
            "Financial changes are locked after payment or receiving"
        }
        return po
    }

    private suspend fun refreshOrderPacking(orderId: String) {
        val cartons = importShipmentDao.getCartonsForOrderSync(orderId)
        val count = cartons.map { it.cartonNumber }.distinct().size
        val gross = cartons.fold(BigDecimal.ZERO) { sum, line -> sum + BigDecimal(line.grossWeightKg) }.toPlainString()
        val net = cartons.fold(BigDecimal.ZERO) { sum, line -> sum + BigDecimal(line.netWeightKg) }.toPlainString()
        val cbm = cartons.distinctBy { it.cartonNumber }.fold(BigDecimal.ZERO) { sum, line -> sum + BigDecimal(line.cbm) }.toPlainString()
        purchaseOrderDao.updatePackingSummary(orderId, gross, net, cbm, count)
        importShipmentDao.getBatchShipmentForOrder(orderId)?.let {
            importShipmentDao.updateShipment(it.copy(totalCartons = count, totalQuantity = cartons.sumOf { line -> line.quantity },
                totalGrossWeightKg = gross, totalNetWeightKg = net, totalCbm = cbm))
            syncShipmentCategoryFreightRates(it.id)
        }
    }

    private suspend fun renumberOrderCartonsContinuously(orderId: String) {
        val current = importShipmentDao.getCartonsForOrderSync(orderId)
        if (current.isEmpty()) return
        val grouped = current
            .groupBy { it.cartonNumber }
            .entries
            .sortedWith(compareBy<Map.Entry<String, List<ShipmentCarton>>> { PoPackingEngine.cartonSortKey(it.key) }.thenBy { it.key })

        val oldToNew = LinkedHashMap<String, String>()
        grouped.forEachIndexed { idx, (oldNumber, rows) ->
            val newNumber = PoPackingEngine.formatCartonNumber(idx + 1)
            oldToNew[oldNumber] = newNumber
            if (oldNumber != newNumber) {
                rows.forEach { row ->
                    importShipmentDao.updateCartonNumber(row.id, newNumber)
                }
            }
        }

        val order = purchaseOrderDao.getOrderById(orderId) ?: return
        val parsed = PoNotesMetadataParser.parse(order.notes, defaultForwarder = "")
        if (parsed.cartonShippingMarks.isNotEmpty()) {
            val remappedMarks = PoNotesMetadataParser.remapCartonShippingMarks(parsed.cartonShippingMarks, oldToNew)
            if (remappedMarks != parsed.cartonShippingMarks) {
                val updatedNotes = PoNotesMetadataParser.serialize(
                    paymentTermLabel = parsed.rawTermsText,
                    forwarderAddress = parsed.forwarderAddress,
                    userNotes = parsed.userNotes,
                    defaultShippingMark = parsed.defaultShippingMark,
                    cartonShippingMarks = remappedMarks
                )
                purchaseOrderDao.updateOrder(order.copy(notes = updatedNotes, updatedAt = System.currentTimeMillis()))
            }
        }
    }

    override suspend fun deleteCarton(cartonId: String) = database.withTransaction {
        val carton = importShipmentDao.getCartonById(cartonId) ?: return@withTransaction
        carton.purchaseOrderId?.let { requirePackingEditableOrder(it) }
        importShipmentDao.deleteCartonById(cartonId)
        carton.purchaseOrderId?.let {
            renumberOrderCartonsContinuously(it)
            refreshOrderPacking(it)
        }
        Unit
    }

    override suspend fun deleteCartonByNumber(orderId: String, cartonNumber: String) = database.withTransaction {
        requirePackingEditableOrder(orderId)
        importShipmentDao.deleteCartonByNumber(orderId, cartonNumber)
        renumberOrderCartonsContinuously(orderId)
        refreshOrderPacking(orderId)
    }

    override suspend fun deletePurchaseCostItem(costItemId: String) = database.withTransaction {
        val cost = importCostDao.getCostItemById(costItemId) ?: return@withTransaction
        cost.purchaseOrderId?.let { requirePackingEditableOrder(it) }
        require(!cost.isCapitalized && !cost.isPosted) { "Posted or capitalized costs require a reversal or cost adjustment" }
        importCostDao.deleteCostItem(costItemId)
    }

    override suspend fun updatePurchaseOrder(
        orderId: String,
        supplierId: String,
        currency: String,
        exchangeRate: BigDecimal,
        items: List<PurchaseOrderItemDraft>,
        notes: String?,
        status: PurchaseOrderStatus,
        cartons: List<PoMixedCartonDraft>,
        chinaDeliveryChargeMinor: Long,
        categoryFreightRatesBdtMinor: Map<String, Long>,
        handlingCostMinor: Long,
        sampleCostMinor: Long,
        attachments: String?,
        orderDateMillis: Long?,
        expectedArrivalDateMillis: Long?,
        updateSchedule: Boolean,
        shippingMethod: String?
    ): Result<Unit> = runCatching { database.withTransaction {
        require(items.isNotEmpty()) { "Purchase order must contain at least 1 item" }
        require(exchangeRate > BigDecimal.ZERO) { "Exchange rate must be positive: $exchangeRate" }

        val existingOrder = purchaseOrderDao.getOrderById(orderId)
            ?: throw IllegalArgumentException("Purchase order not found: $orderId")

        require(!existingOrder.isActualCostFinalized && existingOrder.status != PurchaseOrderStatus.CANCELLED.name) {
            "Finalized or cancelled orders cannot be edited"
        }
        requireEditableOrder(orderId)
        val existingItems = purchaseOrderDao.getItemsForOrderSync(orderId)
        require(existingItems.all { it.receivedQuantity == 0 } && existingOrder.paidAmountMinor == 0L) {
            "Orders with receipts or payments cannot be financially edited"
        }
        require(items.map { it.productId to it.variantId }.distinct().size == items.size) { "Duplicate product variants are not allowed" }
        require(cartons.map { it.cartonNumber.trim() }.distinct().size == cartons.size && cartons.all { it.cartonNumber.isNotBlank() }) {
            "Each carton needs a unique non-empty number"
        }
        val quantities = items.associate { (it.productId to it.variantId) to it.quantity.toLong() }
        cartons.flatMap { it.lines }.groupBy { it.productId to it.variantId }.forEach { (key, rows) ->
            val ordered = requireNotNull(quantities[key]) { "Packed variant is not in the purchase order" }
            require(rows.sumOf { it.quantity.toLong() } <= ordered) { "Packed quantity exceeds the ordered quantity" }
        }
        var totalOriginalMinor = 0L
        var totalBdtMinor = 0L

        val poItems = items.map { draft ->
            require(draft.quantity > 0) { "Quantity must be greater than 0" }
            require(draft.unitPriceMinor > 0) { "Unit price must be greater than 0" }

            val lineOriginalMinor = Math.multiplyExact(draft.quantity.toLong(), draft.unitPriceMinor)
            val lineBdtMinor = BigDecimal(lineOriginalMinor)
                .multiply(exchangeRate)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact()

            totalOriginalMinor = Math.addExact(totalOriginalMinor, lineOriginalMinor)
            totalBdtMinor = Math.addExact(totalBdtMinor, lineBdtMinor)

            PurchaseOrderItem(
                id = existingItems.firstOrNull { it.productId == draft.productId && it.variantId == draft.variantId }?.id ?: "poi-${UUID.randomUUID()}",
                purchaseOrderId = orderId,
                productId = draft.productId,
                variantId = draft.variantId,
                quantity = draft.quantity,
                unitPriceMinor = draft.unitPriceMinor,
                totalAmountMinor = lineOriginalMinor,
                totalBdtAmountMinor = lineBdtMinor,
                receivedQuantity = 0,
                cartonQuantity = draft.cartonQuantity,
                pcsPerCarton = draft.pcsPerCarton,
                grossWeightKg = draft.grossWeightKg.toPlainString(),
                netWeightKg = draft.netWeightKg.toPlainString(),
                cbm = draft.cbm.toPlainString()
            )
        }

        val totalCartonsSum = if (cartons.isNotEmpty()) {
            cartons.map { it.cartonNumber.trim().ifBlank { "CTN" } }.distinct().size
        } else {
            poItems.sumOf { it.cartonQuantity }
        }
        val totalGrossWeightSum = if (cartons.isNotEmpty()) {
            cartons.fold(BigDecimal.ZERO) { acc, it -> acc.add(it.grossWeightKg) }
        } else {
            poItems.fold(BigDecimal.ZERO) { acc, it -> acc.add(BigDecimal(it.grossWeightKg.ifBlank { "0" })) }
        }
        val totalNetWeightSum = if (cartons.isNotEmpty()) {
            cartons.fold(BigDecimal.ZERO) { acc, it -> acc.add(it.netWeightKg) }
        } else {
            poItems.fold(BigDecimal.ZERO) { acc, it -> acc.add(BigDecimal(it.netWeightKg.ifBlank { "0" })) }
        }
        val totalCbmSum = if (cartons.isNotEmpty()) {
            cartons.fold(BigDecimal.ZERO) { acc, it ->
                val cbm = cartonCbm(it)
                acc.add(cbm)
            }
        } else {
            poItems.fold(BigDecimal.ZERO) { acc, it -> acc.add(BigDecimal(it.cbm.ifBlank { "0" })) }
        }

        val updatedOrder = existingOrder.copy(
            supplierId = supplierId,
            status = if (existingOrder.status == PurchaseOrderStatus.DRAFT.name) status.name else existingOrder.status,
            orderDate = if (updateSchedule) orderDateMillis ?: existingOrder.orderDate else existingOrder.orderDate,
            expectedArrivalDate = if (updateSchedule) expectedArrivalDateMillis else existingOrder.expectedArrivalDate,
            currency = currency,
            exchangeRateToBdt = exchangeRate.toPlainString(),
            totalAmountMinor = totalOriginalMinor,
            totalBdtAmountMinor = totalBdtMinor,
            notes = notes,
            attachments = attachments?.takeIf { it.isNotBlank() },
            totalCartons = totalCartonsSum,
            totalGrossWeightKg = totalGrossWeightSum.toPlainString(),
            totalNetWeightKg = totalNetWeightSum.toPlainString(),
            totalCbm = totalCbmSum.toPlainString(),
            updatedAt = System.currentTimeMillis()
        )

        val oldLiability = if (existingOrder.status == PurchaseOrderStatus.DRAFT.name) 0L else existingOrder.totalBdtAmountMinor
        val newLiability = if (updatedOrder.status == PurchaseOrderStatus.DRAFT.name) 0L else totalBdtMinor
        if (supplierId == existingOrder.supplierId) {
            supplierDao.getSupplierById(supplierId)?.let {
                supplierDao.updateOutstandingPayable(supplierId, Math.addExact(it.outstandingPayableMinor, newLiability - oldLiability))
            }
        } else {
            supplierDao.getSupplierById(existingOrder.supplierId)?.let {
                supplierDao.updateOutstandingPayable(it.id, (it.outstandingPayableMinor - oldLiability).coerceAtLeast(0L))
            }
            supplierDao.getSupplierById(supplierId)?.let {
                supplierDao.updateOutstandingPayable(it.id, Math.addExact(it.outstandingPayableMinor, newLiability))
            }
        }
        // Replace order items
        purchaseOrderDao.saveOrderWithItems(updatedOrder, poItems)

        // Sync batch shipment summary if exists
        val batchShipment = importShipmentDao.getBatchShipmentForOrder(orderId)
        if (batchShipment != null) {
            importShipmentDao.updateShipment(
                batchShipment.copy(
                    supplierId = supplierId,
                    shippingMethod = shippingMethod ?: batchShipment.shippingMethod,
                    totalCartons = totalCartonsSum,
                    totalQuantity = poItems.sumOf { it.quantity },
                    totalGrossWeightKg = totalGrossWeightSum.toPlainString(),
                    totalNetWeightKg = totalNetWeightSum.toPlainString(),
                    totalCbm = totalCbmSum.toPlainString(),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }

        // Sync cartons if provided
        run {
            importShipmentDao.deleteCartonsForOrder(orderId)
            val shipmentId = requireNotNull(batchShipment) { "Linked shipment not found" }.id
            cartons.forEach { ctn ->
                if (ctn.lines.isNotEmpty()) {
                    val lineInputs = ctn.lines.map {
                        MixedCartonLineInput(
                            productId = it.productId,
                            variantId = it.variantId,
                            productName = it.productName,
                            quantity = it.quantity,
                            manualGrossWeightKg = it.manualGrossWeightKg
                        )
                    }
                    val allocated = CartonWeightAllocationEngine.allocateWeights(
                        cartonGrossWeightKg = ctn.grossWeightKg,
                        cartonNetWeightKg = ctn.netWeightKg,
                        lines = lineInputs
                    )
                    val cbm = cartonCbm(ctn)

                    val totalQty = ctn.lines.sumOf { it.quantity }
                    allocated.forEach { alloc ->
                        val cartonId = "ctn-${UUID.randomUUID()}"
                        val product = productDao.getProductById(alloc.productId)
                        val resolvedCatId = product?.categoryId
                        val resolvedCatName = resolvedCatId?.let { database.productCategoryDao().getCategoryById(it)?.nameEn }
                        val cartonEntity = ShipmentCarton(
                            id = cartonId,
                            shipmentId = shipmentId,
                            cartonNumber = ctn.cartonNumber,
                            productId = alloc.productId,
                            variantId = alloc.variantId,
                            quantity = alloc.quantity,
                            lengthCm = ctn.lengthCm.toPlainString(),
                            widthCm = ctn.widthCm.toPlainString(),
                            heightCm = ctn.heightCm.toPlainString(),
                            grossWeightKg = alloc.allocatedGrossWeightKg.toPlainString(),
                            netWeightKg = alloc.allocatedNetWeightKg.toPlainString(),
                            cbm = cbm.toPlainString(),
                            purchaseOrderId = orderId,
                            isMixedCarton = ctn.lines.size > 1,
                            totalCartonQuantity = totalQty,
                            cartonGrossWeightKg = ctn.grossWeightKg.toPlainString(),
                            supplierId = supplierId,
                            categoryId = resolvedCatId,
                            categoryName = resolvedCatName
                        )
                        importShipmentDao.insertCarton(cartonEntity)
                    }
                }
            }
        }

        if (batchShipment != null) syncShipmentCategoryFreightRates(batchShipment.id)

        // Sync editable PO cost items (China Local Delivery, Handling, Sample) without duplicating
        if (batchShipment != null) {
            saveShipmentSupplierAndChinaDelivery(
                com.pocketdev.advance_erp.domain.repository.ShipmentSupplierDraft(
                    id = "ss-${batchShipment.id}-$supplierId", shipmentId = batchShipment.id,
                    supplierId = supplierId, purchaseOrderId = orderId, currency = currency,
                    exchangeRateToBdt = exchangeRate, chinaDeliveryChargeMinor = chinaDeliveryChargeMinor,
                    chinaDeliveryCurrency = currency, chinaDeliveryExchangeRateToBdt = exchangeRate,
                    chinaDeliveryAllocationMethod = AllocationMethod.QUANTITY
                )
            )
            val existingCosts = importCostDao.getCostItemsForOrderSync(orderId)

            suspend fun upsertPoCost(
                category: CostCategory,
                descMatch: String,
                defaultDesc: String,
                amountMinor: Long
            ) {
                val costCurrency = if (category == CostCategory.CHINA_DOMESTIC_TRANSPORT) currency else "BDT"
                val costRate = if (costCurrency == "BDT") BigDecimal.ONE else exchangeRate
                val existing = existingCosts.firstOrNull {
                    (it.costCategory == category.name ||
                        (category == CostCategory.OTHER_EXPENSES && it.costCategory == CostCategory.OTHER_CHARGES.name)) &&
                        it.description.contains(descMatch, ignoreCase = true)
                }
                if (amountMinor > 0L) {
                    val bdtMinor = BigDecimal(amountMinor)
                        .multiply(costRate)
                        .setScale(0, RoundingMode.HALF_UP)
                        .longValueExact()
                    if (existing != null) {
                        importCostDao.updateCostItem(
                            existing.copy(
                                costCategory = category.name,
                                amountMinor = amountMinor,
                                currency = costCurrency,
                                exchangeRateToBdt = costRate.toPlainString(),
                                bdtAmountMinor = bdtMinor
                            )
                        )
                    } else {
                        addPurchaseCostItem(
                            orderId = orderId,
                            category = category,
                            description = defaultDesc,
                            amountMinor = amountMinor,
                            currency = costCurrency,
                            exchangeRate = costRate,
                            method = AllocationMethod.QUANTITY,
                            isEstimated = false
                        )
                    }
                } else if (existing != null) {
                    importCostDao.deleteCostItem(existing.id)
                }
            }

            if (chinaDeliveryChargeMinor > 0L || existingCosts.any { it.costCategory == CostCategory.CHINA_DOMESTIC_TRANSPORT.name }) {
                upsertPoCost(
                    category = CostCategory.CHINA_DOMESTIC_TRANSPORT,
                    descMatch = "China Local",
                    defaultDesc = "China Local Delivery (চীন লোকাল ডেলিভারি)",
                    amountMinor = chinaDeliveryChargeMinor
                )
            }
            if (handlingCostMinor > 0L || existingCosts.any { it.description.contains("Handling", ignoreCase = true) }) {
                upsertPoCost(
                    category = CostCategory.OTHER_EXPENSES,
                    descMatch = "Handling",
                    defaultDesc = "Handling & Packaging Charge",
                    amountMinor = handlingCostMinor
                )
            }
            if (sampleCostMinor > 0L || existingCosts.any { it.description.contains("Sample", ignoreCase = true) }) {
                upsertPoCost(
                    category = CostCategory.OTHER_EXPENSES,
                    descMatch = "Sample",
                    defaultDesc = "Sample Cost",
                    amountMinor = sampleCostMinor
                )
            }

            if (categoryFreightRatesBdtMinor.isNotEmpty()) {
                for ((catId, rateMinor) in categoryFreightRatesBdtMinor) {
                    if (rateMinor >= 0L) {
                        overrideShipmentCategoryFreightRate(batchShipment.id, catId, rateMinor)
                    }
                }
                val positiveRatesMinor = categoryFreightRatesBdtMinor.values.filter { it > 0L }
                if (positiveRatesMinor.isNotEmpty()) {
                    val positiveRateMap = categoryFreightRatesBdtMinor.filterValues { it > 0L }
                    var weightedSum = BigDecimal.ZERO
                    var totalWeightedKg = BigDecimal.ZERO
                    for (item in poItems) {
                        val prod = productDao.getProductById(item.productId)
                        val catId = prod?.categoryId ?: continue
                        val rateMinor = positiveRateMap[catId] ?: continue
                        val itemWeight = BigDecimal(item.grossWeightKg.ifBlank { "0" })
                        if (itemWeight > BigDecimal.ZERO) {
                            weightedSum = weightedSum.add(BigDecimal(rateMinor).multiply(itemWeight))
                            totalWeightedKg = totalWeightedKg.add(itemWeight)
                        }
                    }
                    val effectiveRateBdt = if (totalWeightedKg > BigDecimal.ZERO) {
                        weightedSum
                            .divide(BigDecimal("100"))
                            .divide(totalWeightedKg, 2, RoundingMode.HALF_UP)
                    } else {
                        BigDecimal(positiveRatesMinor.sum())
                            .divide(BigDecimal("100"))
                            .divide(BigDecimal(positiveRatesMinor.size), 2, RoundingMode.HALF_UP)
                    }
                    purchaseOrderDao.updateOrder(
                        updatedOrder.copy(shippingRatePerKgBdt = effectiveRateBdt.toPlainString())
                    )
                }
            }
        }
        if (batchShipment != null) {
            val oldMirror = importCostDao.getCostItemsForOrderSync(orderId).filter {
                it.description.startsWith("Category Per-KG International Freight") && !it.isCapitalized
            }
            oldMirror.forEach { importCostDao.deleteCostItem(it.id) }
        }
        Unit
    } }

    override suspend fun updatePurchaseOrderPacking(
        orderId: String,
        totalCartons: Int,
        grossWeightKg: String,
        netWeightKg: String,
        cbm: String
    ) {
        purchaseOrderDao.updatePackingSummary(
            orderId = orderId,
            grossWeight = grossWeightKg,
            netWeight = netWeightKg,
            cbm = cbm,
            cartons = totalCartons,
            updatedAt = System.currentTimeMillis()
        )
    }

    override suspend fun updatePurchaseOrderItem(
        itemId: String,
        quantity: Int,
        unitPriceMinor: Long,
        cartonQuantity: Int,
        grossWeightKg: String
    ): Unit = database.withTransaction {
        val item = purchaseOrderDao.getItemById(itemId) ?: return@withTransaction
        val po = purchaseOrderDao.getOrderById(item.purchaseOrderId) ?: return@withTransaction
        requireEditableOrder(po.id)
        val packedForVariant = importShipmentDao.getCartonsForOrderSync(po.id)
            .filter { it.productId == item.productId && it.variantId == item.variantId }
            .sumOf { it.quantity }
        require(quantity >= packedForVariant) {
            "This variant already has $packedForVariant PCS allocated to cartons. Please adjust the carton packing before reducing the PO quantity."
        }
        require(quantity > 0 && unitPriceMinor > 0 && cartonQuantity >= 0 && BigDecimal(grossWeightKg) >= BigDecimal.ZERO) { "Invalid item quantity, price or packing" }
        val rateBd = BigDecimal(po.exchangeRateToBdt.ifBlank { "17.00" })
        val totalAmountMinor = quantity.toLong() * unitPriceMinor
        val totalBdtAmountMinor = BigDecimal(totalAmountMinor)
            .multiply(rateBd)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()

        val updatedItem = item.copy(
            quantity = quantity,
            unitPriceMinor = unitPriceMinor,
            totalAmountMinor = totalAmountMinor,
            totalBdtAmountMinor = totalBdtAmountMinor,
            cartonQuantity = cartonQuantity,
            grossWeightKg = grossWeightKg
        )
        purchaseOrderDao.updateItem(updatedItem)

        val allItems = purchaseOrderDao.getItemsForOrderSync(po.id)
        val newTotalMinor = allItems.sumOf { it.totalAmountMinor }
        val newTotalBdtMinor = allItems.sumOf { it.totalBdtAmountMinor }
        val newCartons = allItems.sumOf { it.cartonQuantity }
        val newGross = allItems.fold(BigDecimal.ZERO) { acc, itm -> acc.add(BigDecimal(itm.grossWeightKg.ifBlank { "0" })) }.toPlainString()

        if (po.status != PurchaseOrderStatus.DRAFT.name) supplierDao.getSupplierById(po.supplierId)?.let {
            supplierDao.updateOutstandingPayable(it.id, it.outstandingPayableMinor + newTotalBdtMinor - po.totalBdtAmountMinor)
        }
        purchaseOrderDao.updateOrder(
            po.copy(
                totalAmountMinor = newTotalMinor,
                totalBdtAmountMinor = newTotalBdtMinor,
                totalCartons = newCartons,
                totalGrossWeightKg = newGross,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun deletePurchaseOrderItem(itemId: String): Unit = database.withTransaction {
        val item = purchaseOrderDao.getItemById(itemId) ?: return@withTransaction
        val po = purchaseOrderDao.getOrderById(item.purchaseOrderId) ?: return@withTransaction
        requireEditableOrder(po.id)
        require(purchaseOrderDao.getItemsForOrderSync(po.id).size > 1) { "A PO needs at least one item" }
        require(importShipmentDao.getCartonsForOrderSync(po.id).none { it.productId == item.productId && it.variantId == item.variantId }) { "Remove this item from packing first" }
        purchaseOrderDao.deleteItemById(itemId)

        val allItems = purchaseOrderDao.getItemsForOrderSync(po.id)
        val newTotalMinor = allItems.sumOf { it.totalAmountMinor }
        val newTotalBdtMinor = allItems.sumOf { it.totalBdtAmountMinor }
        val newCartons = allItems.sumOf { it.cartonQuantity }
        val newGross = allItems.fold(BigDecimal.ZERO) { acc, itm -> acc.add(BigDecimal(itm.grossWeightKg.ifBlank { "0" })) }.toPlainString()

        if (po.status != PurchaseOrderStatus.DRAFT.name) supplierDao.getSupplierById(po.supplierId)?.let {
            supplierDao.updateOutstandingPayable(it.id, it.outstandingPayableMinor + newTotalBdtMinor - po.totalBdtAmountMinor)
        }
        purchaseOrderDao.updateOrder(
            po.copy(
                totalAmountMinor = newTotalMinor,
                totalBdtAmountMinor = newTotalBdtMinor,
                totalCartons = newCartons,
                totalGrossWeightKg = newGross,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    // =========================================================================
    // PHASE 2-5 SHIPMENT-CENTRIC IMPORT & LANDING COST EXTENSIONS
    // =========================================================================

    override fun getSuppliersForShipment(shipmentId: String): Flow<List<ShipmentSupplier>> =
        importShipmentDao.getSuppliersForShipment(shipmentId)

    override suspend fun getSuppliersForShipmentSync(shipmentId: String): List<ShipmentSupplier> =
        importShipmentDao.getSuppliersForShipmentSync(shipmentId)

    override fun getOrdersForShipment(shipmentId: String): Flow<List<PurchaseOrder>> =
        purchaseOrderDao.getOrdersForShipment(shipmentId)

    override suspend fun getOrdersForShipmentSync(shipmentId: String): List<PurchaseOrder> =
        purchaseOrderDao.getOrdersForShipmentSync(shipmentId)

    override suspend fun saveShipmentSupplierAndChinaDelivery(draft: ShipmentSupplierDraft): String {
        val shipment = importShipmentDao.getShipmentById(draft.shipmentId)
            ?: throw IllegalArgumentException("Shipment not found: ${draft.shipmentId}")

        require(draft.exchangeRateToBdt > BigDecimal.ZERO) { "Exchange rate must be positive" }
        require(draft.chinaDeliveryExchangeRateToBdt > BigDecimal.ZERO) { "China delivery exchange rate must be positive" }
        require(draft.chinaDeliveryChargeMinor >= 0L) { "China delivery charge cannot be negative" }

        val chinaDeliveryBdtMinor = LandingCostEngine.convertToBdtMinor(
            amountForeignMinor = draft.chinaDeliveryChargeMinor,
            currency = draft.chinaDeliveryCurrency,
            exchangeRateToBdt = draft.chinaDeliveryExchangeRateToBdt
        )

        val manualSpec = if (draft.chinaDeliveryAllocationMethod == AllocationMethod.MANUAL && draft.manualProductAllocationsForeignMinor.isNotEmpty()) {
            val manualSum = draft.manualProductAllocationsForeignMinor.values.sum()
            require(manualSum == draft.chinaDeliveryChargeMinor) {
                "Manual China delivery allocations ($manualSum) must equal supplier China delivery charge (${draft.chinaDeliveryChargeMinor})"
            }
            LandedCostAllocationEngine.formatManualAllocations(draft.manualProductAllocationsForeignMinor)
        } else null

        val recordId = draft.id ?: "ss-${shipment.id}-${draft.supplierId}-${draft.purchaseOrderId ?: UUID.randomUUID().toString().take(6)}"
        val entity = ShipmentSupplier(
            id = recordId,
            shipmentId = draft.shipmentId,
            supplierId = draft.supplierId,
            purchaseOrderId = draft.purchaseOrderId,
            currency = draft.currency,
            exchangeRateToBdt = draft.exchangeRateToBdt.toPlainString(),
            chinaDeliveryChargeMinor = draft.chinaDeliveryChargeMinor,
            chinaDeliveryCurrency = draft.chinaDeliveryCurrency,
            chinaDeliveryExchangeRateToBdt = draft.chinaDeliveryExchangeRateToBdt.toPlainString(),
            chinaDeliveryBdtMinor = chinaDeliveryBdtMinor,
            chinaDeliveryAllocationMethod = draft.chinaDeliveryAllocationMethod.name,
            manualAllocationsJson = manualSpec,
            notes = draft.notes
        )
        importShipmentDao.insertShipmentSupplier(entity)

        if (draft.purchaseOrderId != null) {
            purchaseOrderDao.updateOrderShipmentId(draft.purchaseOrderId, draft.shipmentId)
        }
        return recordId
    }

    override suspend fun deleteShipmentSupplier(id: String) {
        val existing = importShipmentDao.getShipmentSupplierById(id)
        if (existing?.purchaseOrderId != null) {
            purchaseOrderDao.updateOrderShipmentId(existing.purchaseOrderId, null)
        }
        importShipmentDao.deleteShipmentSupplierById(id)
    }

    override suspend fun linkPurchaseOrderToShipment(orderId: String, shipmentId: String?) {
        val po = purchaseOrderDao.getOrderById(orderId)
            ?: throw IllegalArgumentException("Purchase Order not found: $orderId")
        purchaseOrderDao.updateOrderShipmentId(orderId, shipmentId)

        if (shipmentId != null) {
            val existingSuppliers = importShipmentDao.getSuppliersForShipmentSync(shipmentId)
            if (existingSuppliers.none { it.purchaseOrderId == orderId }) {
                importShipmentDao.insertShipmentSupplier(
                    ShipmentSupplier(
                        id = "ss-$shipmentId-${po.supplierId}-$orderId",
                        shipmentId = shipmentId,
                        supplierId = po.supplierId,
                        purchaseOrderId = orderId,
                        currency = po.currency,
                        exchangeRateToBdt = po.exchangeRateToBdt,
                        chinaDeliveryCurrency = po.currency,
                        chinaDeliveryExchangeRateToBdt = po.exchangeRateToBdt
                    )
                )
            }
            syncShipmentCategoryFreightRates(shipmentId)
        }
    }

    override fun getAllCategoriesFlow(): Flow<List<ProductCategory>> =
        database.productCategoryDao().getAllCategories()

    override suspend fun updateCategoryDefaultFreightRate(categoryId: String, ratePerKgBdtMinor: Long) {
        require(ratePerKgBdtMinor >= 0L) { "Default freight rate cannot be negative" }
        database.productCategoryDao().updateCategoryDefaultFreightRate(categoryId, ratePerKgBdtMinor)
    }

    override fun getFreightRatesForShipment(shipmentId: String): Flow<List<ShipmentFreightRate>> =
        importShipmentDao.getFreightRatesForShipment(shipmentId)

    override suspend fun getFreightRatesForShipmentSync(shipmentId: String): List<ShipmentFreightRate> =
        importShipmentDao.getFreightRatesForShipmentSync(shipmentId)

    override suspend fun syncShipmentCategoryFreightRates(shipmentId: String): List<ShipmentFreightRate> {
        val cartons = importShipmentDao.getCartonsForShipmentSync(shipmentId)
        val existingRates = importShipmentDao.getFreightRatesForShipmentSync(shipmentId).associateBy { it.categoryId }

        val categoryIds = mutableSetOf<String>()
        cartons.forEach { c ->
            val catId = c.categoryId ?: productDao.getProductById(c.productId)?.categoryId
            if (!catId.isNullOrBlank()) categoryIds.add(catId)
        }

        // Also check linked PO items in case cartons haven't been added for all products yet
        val linkedPos = purchaseOrderDao.getOrdersForShipmentSync(shipmentId)
        val shipment = importShipmentDao.getShipmentById(shipmentId)
        val allPoIds = (linkedPos.map { it.id } + listOfNotNull(shipment?.purchaseOrderId)).distinct()
        for (poId in allPoIds) {
            val items = purchaseOrderDao.getItemsForOrderSync(poId)
            items.forEach { item ->
                val catId = productDao.getProductById(item.productId)?.categoryId
                if (!catId.isNullOrBlank()) categoryIds.add(catId)
            }
        }

        val updatedList = mutableListOf<ShipmentFreightRate>()
        for (catId in categoryIds) {
            val category = database.productCategoryDao().getCategoryById(catId) ?: continue
            val existing = existingRates[catId]
            val catWeight = cartons.filter { (it.categoryId ?: productDao.getProductById(it.productId)?.categoryId) == catId }
                .fold(BigDecimal.ZERO) { acc, c -> acc.add(BigDecimal(c.grossWeightKg.ifBlank { "0" })) }
                .setScale(3, RoundingMode.HALF_UP)

            if (existing != null) {
                // Historical preservation: keep existing.appliedRatePerKgBdtMinor unless overridden
                val appliedRate = existing.overrideRatePerKgBdtMinor ?: existing.appliedRatePerKgBdtMinor
                val totalFreight = catWeight.multiply(BigDecimal(appliedRate)).setScale(0, RoundingMode.HALF_UP).longValueExact()
                val updated = existing.copy(
                    totalCategoryGrossWeightKg = catWeight.toPlainString(),
                    totalCategoryFreightBdtMinor = totalFreight,
                    updatedAt = System.currentTimeMillis()
                )
                importShipmentDao.updateShipmentFreightRate(updated)
                updatedList.add(updated)
            } else {
                val defaultRate = category.defaultFreightRatePerKgBdtMinor
                val totalFreight = catWeight.multiply(BigDecimal(defaultRate)).setScale(0, RoundingMode.HALF_UP).longValueExact()
                val created = ShipmentFreightRate(
                    id = "sfr-$shipmentId-$catId",
                    shipmentId = shipmentId,
                    categoryId = catId,
                    categoryNameEn = category.nameEn,
                    categoryNameBn = category.nameBn,
                    defaultRatePerKgBdtMinor = defaultRate,
                    overrideRatePerKgBdtMinor = null,
                    appliedRatePerKgBdtMinor = defaultRate,
                    totalCategoryGrossWeightKg = catWeight.toPlainString(),
                    totalCategoryFreightBdtMinor = totalFreight,
                    updatedAt = System.currentTimeMillis()
                )
                importShipmentDao.insertShipmentFreightRate(created)
                updatedList.add(created)
            }
        }
        return importShipmentDao.getFreightRatesForShipmentSync(shipmentId)
    }

    override suspend fun overrideShipmentCategoryFreightRate(
        shipmentId: String,
        categoryId: String,
        overrideRatePerKgBdtMinor: Long?
    ): ShipmentFreightRate {
        if (overrideRatePerKgBdtMinor != null) {
            require(overrideRatePerKgBdtMinor >= 0L) { "Freight rate override cannot be negative" }
        }
        syncShipmentCategoryFreightRates(shipmentId)
        val existing = importShipmentDao.getFreightRateForShipmentAndCategorySync(shipmentId, categoryId)
            ?: run {
                val cat = database.productCategoryDao().getCategoryById(categoryId)
                    ?: throw IllegalArgumentException("Category not found: $categoryId")
                ShipmentFreightRate(
                    id = "sfr-$shipmentId-$categoryId",
                    shipmentId = shipmentId,
                    categoryId = categoryId,
                    categoryNameEn = cat.nameEn,
                    categoryNameBn = cat.nameBn,
                    defaultRatePerKgBdtMinor = cat.defaultFreightRatePerKgBdtMinor,
                    overrideRatePerKgBdtMinor = overrideRatePerKgBdtMinor,
                    appliedRatePerKgBdtMinor = overrideRatePerKgBdtMinor ?: cat.defaultFreightRatePerKgBdtMinor
                )
            }

        val appliedRate = overrideRatePerKgBdtMinor ?: existing.defaultRatePerKgBdtMinor
        val weightBd = BigDecimal(existing.totalCategoryGrossWeightKg.ifBlank { "0" })
        val totalFreight = weightBd.multiply(BigDecimal(appliedRate)).setScale(0, RoundingMode.HALF_UP).longValueExact()

        val updated = existing.copy(
            overrideRatePerKgBdtMinor = overrideRatePerKgBdtMinor,
            appliedRatePerKgBdtMinor = appliedRate,
            totalCategoryFreightBdtMinor = totalFreight,
            updatedAt = System.currentTimeMillis()
        )
        importShipmentDao.insertShipmentFreightRate(updated)
        return updated
    }

    override suspend fun addShipmentExpenseWithManualAllocations(
        shipmentId: String,
        category: CostCategory,
        description: String,
        amountMinor: Long,
        currency: String,
        exchangeRate: BigDecimal,
        method: AllocationMethod,
        paymentAccount: String,
        docRef: String?,
        supplierId: String?,
        manualAllocationsBdtMinor: Map<String, Long>,
        notes: String?,
        attachmentPath: String?,
        costDate: Long
    ): String = database.withTransaction {
        require(amountMinor > 0L) { "Expense amount must be greater than zero" }
        require(exchangeRate > BigDecimal.ZERO) { "Exchange rate must be positive" }

        val shipment = importShipmentDao.getShipmentById(shipmentId)
            ?: throw IllegalArgumentException("Shipment not found: $shipmentId")

        // Guard against duplicate expense entry (same shipment, category, description, amount, and document reference)
        val existingExpenses = importCostDao.getCostItemsForShipmentSync(shipmentId)
        val isDuplicate = existingExpenses.any {
            it.costCategory == category.name &&
                it.description.equals(description.trim(), ignoreCase = true) &&
                it.amountMinor == amountMinor &&
                it.currency.equals(currency, ignoreCase = true) &&
                (docRef != null && it.documentReference == docRef)
        }
        require(!isDuplicate) {
            "Duplicate expense rejected: '$description' ($currency ${amountMinor / 100.0}) with reference '$docRef' already exists on this shipment"
        }

        val bdtAmountMinor = LandingCostEngine.convertToBdtMinor(amountMinor, currency, exchangeRate)
        val manualSpec = if (method == AllocationMethod.MANUAL) {
            require(manualAllocationsBdtMinor.isNotEmpty()) {
                "Manual product-wise allocation amounts are required when MANUAL allocation method is selected"
            }
            val manualSum = manualAllocationsBdtMinor.values.sum()
            require(manualSum == bdtAmountMinor) {
                "Manual allocated amounts ($manualSum) must equal the expense BDT amount ($bdtAmountMinor)"
            }
            LandedCostAllocationEngine.formatManualAllocations(manualAllocationsBdtMinor)
        } else null

        val snapshots = importCostDao.getSnapshotsForShipmentSync(shipmentId)
        val isPostFinal = shipment.status == ShipmentStatus.COMPLETED.name || snapshots.any { it.isFinal }

        val costId = "cost-${UUID.randomUUID()}"
        val costItem = ImportCostItem(
            id = costId,
            shipmentId = shipmentId,
            costCategory = category.name,
            description = description.trim(),
            amountMinor = amountMinor,
            currency = currency.uppercase(),
            exchangeRateToBdt = exchangeRate.toPlainString(),
            bdtAmountMinor = bdtAmountMinor,
            costDate = costDate,
            paymentAccount = paymentAccount,
            documentReference = docRef,
            allocationMethod = method.name,
            isPosted = true,
            purchaseOrderId = shipment.purchaseOrderId,
            isEstimated = false,
            isPaid = true,
            notes = notes,
            attachmentPath = attachmentPath,
            createdAt = System.currentTimeMillis(),
            supplierId = supplierId,
            isPostFinalization = isPostFinal,
            isCapitalized = false,
            manualAllocationsJson = manualSpec
        )
        importCostDao.insertCostItem(costItem)

        try {
            accountingRepository.postJournalEntry(
                entryDate = costDate,
                descEn = "Import Expense: $description ($currency ${amountMinor / 100.0})",
                descBn = "আমদানি ব্যয়: $description",
                referenceType = "IMPORT_EXPENSE",
                referenceId = costId,
                lines = listOf(
                    JournalLineDraft(
                        accountId = "coa-1220",
                        isDebit = true,
                        amountMinor = bdtAmountMinor,
                        description = "Import expense recorded: $description"
                    ),
                    JournalLineDraft(
                        accountId = "coa-1020",
                        isDebit = false,
                        amountMinor = bdtAmountMinor,
                        description = "Payment for import expense: $description"
                    )
                )
            )
        } catch (e: Exception) {
            throw e
        }

        return@withTransaction costId
    }

    private suspend fun buildShipmentLandingCostEngineInputs(
        shipmentId: String
    ): LandingCostEngine.ShipmentLandingCostResult {
        val shipment = importShipmentDao.getShipmentById(shipmentId)
            ?: throw IllegalArgumentException("Shipment not found: $shipmentId")
        val cartons = importShipmentDao.getCartonsForShipmentSync(shipmentId)
        require(cartons.isNotEmpty()) { "Cannot calculate landing cost without cartons in shipment" }

        val shipmentSuppliers = importShipmentDao.getSuppliersForShipmentSync(shipmentId)
        val linkedPos = purchaseOrderDao.getOrdersForShipmentSync(shipmentId)
        val allPoIds = (
            linkedPos.map { it.id } +
                shipmentSuppliers.mapNotNull { it.purchaseOrderId } +
                cartons.mapNotNull { it.purchaseOrderId } +
                listOfNotNull(shipment.purchaseOrderId)
            ).distinct()

        val poById = allPoIds.mapNotNull { purchaseOrderDao.getOrderById(it) }.associateBy { it.id }
        val poItemsByKey = mutableMapOf<String, Pair<PurchaseOrder, PurchaseOrderItem>>()
        val poItemsByProductFallback = mutableMapOf<String, Pair<PurchaseOrder, PurchaseOrderItem>>()
        for (po in poById.values) {
            val items = purchaseOrderDao.getItemsForOrderSync(po.id)
            for (item in items) {
                val key = if (item.variantId != null) "${item.productId}:${item.variantId}" else item.productId
                poItemsByKey[key] = po to item
                poItemsByProductFallback.putIfAbsent(item.productId, po to item)
            }
        }

        // Aggregate receiving quantities from WarehouseReceipts for this shipment keyed by product+variant
        val receipts = warehouseReceiptDao.getReceiptsByShipmentSync(shipmentId)
            .filter { it.status != ReceiptStatus.CANCELLED.name }
        val allReceiptItems = receipts.flatMap { warehouseReceiptDao.getItemsForReceiptSync(it.id) }
        val receiptItemsByKey = allReceiptItems.groupBy {
            if (it.variantId != null) "${it.productId}:${it.variantId}" else it.productId
        }
        val receiptItemsByProduct = allReceiptItems.groupBy { it.productId }

        val cartonInputs = cartons.map { c ->
            val product = productDao.getProductById(c.productId)
            val resolvedCatId = c.categoryId ?: product?.categoryId ?: "cat-uncategorized"
            val cKey = if (c.variantId != null) "${c.productId}:${c.variantId}" else c.productId
            val poPair = poItemsByKey[cKey] ?: poItemsByProductFallback[c.productId]
            val resolvedSupplierId = c.supplierId ?: poPair?.first?.supplierId ?: shipment.supplierId
            LandingCostEngine.ShipmentCartonInput(
                cartonNumber = c.cartonNumber,
                supplierId = resolvedSupplierId,
                productId = c.productId,
                variantId = c.variantId,
                categoryId = resolvedCatId,
                quantity = c.quantity,
                grossWeightKg = BigDecimal(c.grossWeightKg.ifBlank { "0" }),
                cbm = BigDecimal(c.cbm.ifBlank { "0" }),
                lengthCm = BigDecimal(c.lengthCm.ifBlank { "0" }),
                widthCm = BigDecimal(c.widthCm.ifBlank { "0" }),
                heightCm = BigDecimal(c.heightCm.ifBlank { "0" })
            )
        }

        val itemGroups = cartons.groupBy {
            if (it.variantId != null) "${it.productId}:${it.variantId}" else it.productId
        }
        val productInputs = itemGroups.map { (groupKey, groupCartons) ->
            val firstCarton = groupCartons.first()
            val productId = firstCarton.productId
            val variantId = firstCarton.variantId
            val product = productDao.getProductById(productId)
            val variant = if (variantId != null) productVariantDao.getVariantById(variantId) else null
            val packedQty = groupCartons.sumOf { it.quantity }

            val poPair = poItemsByKey[groupKey] ?: poItemsByProductFallback[productId]
            val resolvedSupplierId = firstCarton.supplierId ?: poPair?.first?.supplierId ?: shipment.supplierId
            val supplierConfig = shipmentSuppliers.find { it.supplierId == resolvedSupplierId }

            val unitPriceForeign = poPair?.second?.unitPriceMinor
                ?: variant?.variantCostMinor
                ?: product?.purchasePriceMinor
                ?: 0L
            val currency = poPair?.first?.currency ?: supplierConfig?.currency ?: "BDT"
            val exchangeRate = BigDecimal(
                (poPair?.first?.exchangeRateToBdt ?: supplierConfig?.exchangeRateToBdt ?: "1.0").ifBlank { "1.0" }
            )

            val catId = firstCarton.categoryId ?: product?.categoryId ?: "cat-uncategorized"
            val catName = firstCarton.categoryName
                ?: database.productCategoryDao().getCategoryById(catId)?.nameEn
                ?: "Uncategorized"

            val recItems = receiptItemsByKey[groupKey]
                ?: if (variantId == null) receiptItemsByProduct[productId].orEmpty() else emptyList()
            val (goodQty, damagedQty, missingQty, pendingQty) = if (recItems.isNotEmpty()) {
                val dmg = recItems.sumOf { it.damagedQuantity }
                val mis = recItems.sumOf { it.missingQuantity }
                val gd = recItems.sumOf { item ->
                    if (item.goodQuantity > 0 || (item.damagedQuantity + item.missingQuantity + item.pendingQuantity == item.expectedQuantity)) {
                        item.goodQuantity
                    } else if (item.receivedQuantity + item.damagedQuantity + item.missingQuantity == item.expectedQuantity) {
                        item.receivedQuantity
                    } else {
                        (item.receivedQuantity - item.damagedQuantity).coerceAtLeast(0)
                    }
                }
                val cappedGood = gd.coerceAtMost(maxOf(0, packedQty - dmg - mis))
                val pend = maxOf(0, packedQty - cappedGood - dmg - mis)
                listOf(cappedGood, dmg, mis, pend)
            } else {
                listOf(packedQty, 0, 0, 0)
            }

            LandingCostEngine.ShipmentProductInput(
                productId = productId,
                variantId = variantId,
                productNameEn = product?.nameEn ?: productId,
                productNameBn = product?.nameBn ?: productId,
                sku = variant?.sku ?: product?.baseSku ?: "",
                supplierId = resolvedSupplierId,
                purchaseOrderId = firstCarton.purchaseOrderId ?: poPair?.first?.id ?: shipment.purchaseOrderId,
                categoryId = catId,
                categoryNameEn = catName,
                packedQuantity = packedQty,
                unitPurchasePriceForeignMinor = unitPriceForeign,
                purchaseCurrency = currency,
                purchaseExchangeRateToBdt = exchangeRate,
                goodQuantity = goodQty,
                damagedQuantity = damagedQty,
                missingQuantity = missingQty,
                pendingQuantity = pendingQty
            )
        }

        val chinaDeliveryInputs = shipmentSuppliers.filter { it.chinaDeliveryChargeMinor > 0L }.map { ss ->
            val method = try {
                AllocationMethod.valueOf(ss.chinaDeliveryAllocationMethod)
            } catch (_: Exception) {
                AllocationMethod.PURCHASE_VALUE
            }
            LandingCostEngine.SupplierChinaDeliveryInput(
                supplierId = ss.supplierId,
                deliveryChargeForeignMinor = ss.chinaDeliveryChargeMinor,
                currency = ss.chinaDeliveryCurrency,
                exchangeRateToBdt = BigDecimal(ss.chinaDeliveryExchangeRateToBdt.ifBlank { "1.0" }),
                allocationMethod = method,
                manualProductAllocationsForeignMinor = LandedCostAllocationEngine.parseManualAllocations(ss.manualAllocationsJson)
            )
        }

        val syncedRates = syncShipmentCategoryFreightRates(shipmentId)
        val costItems = importCostDao.getCostItemsForShipmentSync(shipmentId)

        // Avoid double-counting international freight if explicit INTERNATIONAL_FREIGHT cost item is already entered
        // AND category freight rates are 0; if category freight rates > 0, apply category freight rates!
        val categoryRateInputs = syncedRates.map { r ->
            LandingCostEngine.CategoryFreightRateInput(
                categoryId = r.categoryId,
                categoryNameEn = r.categoryNameEn,
                defaultRatePerKgBdtMinor = r.defaultRatePerKgBdtMinor,
                overrideRatePerKgBdtMinor = r.overrideRatePerKgBdtMinor
            )
        }

        val expenseInputs = costItems.map { ci ->
            val cat = try {
                CostCategory.valueOf(ci.costCategory)
            } catch (_: Exception) {
                CostCategory.OTHER_EXPENSES
            }
            val method = try {
                AllocationMethod.valueOf(ci.allocationMethod)
            } catch (_: Exception) {
                AllocationMethod.PURCHASE_VALUE
            }
            LandingCostEngine.ImportExpenseInput(
                expenseId = ci.id,
                expenseName = ci.description,
                category = cat,
                amountForeignMinor = ci.amountMinor,
                currency = ci.currency,
                exchangeRateToBdt = BigDecimal(ci.exchangeRateToBdt.ifBlank { "1.0" }),
                allocationMethod = method,
                supplierId = ci.supplierId,
                manualProductAllocationsBdtMinor = LandedCostAllocationEngine.parseManualAllocations(ci.manualAllocationsJson),
                isPostFinalization = ci.isPostFinalization,
                isAlreadyCapitalized = ci.isCapitalized
            )
        }

        return LandingCostEngine.calculateShipmentLandingCost(
            products = productInputs,
            cartons = cartonInputs,
            supplierChinaDeliveries = chinaDeliveryInputs,
            categoryFreightRates = categoryRateInputs,
            expenses = expenseInputs
        )
    }

    override suspend fun previewShipmentLandingCost(
        shipmentId: String
    ): LandingCostEngine.ShipmentLandingCostResult {
        return buildShipmentLandingCostEngineInputs(shipmentId)
    }

    private fun checkFinalizePermission(operatorRole: String) {
        val allowedRoles = setOf("ADMIN", "SYSTEM_ADMIN", "OWNER", "MANAGER", "ACCOUNTANT")
        require(operatorRole.uppercase() in allowedRoles) {
            "Permission denied: Role '$operatorRole' is not authorized to finalize or adjust Landing Cost"
        }
    }

    override suspend fun finalizeShipmentLandingCost(
        shipmentId: String,
        operatorRole: String,
        operatorName: String
    ): List<LandedCostSnapshot> {
        checkFinalizePermission(operatorRole)

        val shipment = importShipmentDao.getShipmentById(shipmentId)
            ?: throw IllegalArgumentException("Shipment not found: $shipmentId")

        val engineResult = buildShipmentLandingCostEngineInputs(shipmentId)
        val costItems = importCostDao.getCostItemsForShipmentSync(shipmentId)

        return database.withTransaction {
            val allAllocations = mutableListOf<LandedCostAllocation>()
            engineResult.expenseAllocationsByExpenseId.forEach { (expenseId, items) ->
                val ci = costItems.find { it.id == expenseId }
                val methodStr = ci?.allocationMethod ?: AllocationMethod.PURCHASE_VALUE.name
                items.forEach { res ->
                    val parts = res.itemId.split(":")
                    val pId = parts[0]
                    val vId = parts.getOrNull(1)
                    allAllocations.add(
                        LandedCostAllocation(
                            id = "lca-${UUID.randomUUID()}",
                            shipmentId = shipmentId,
                            costItemId = expenseId,
                            productId = pId,
                            variantId = vId,
                            allocationBasis = methodStr,
                            basisValue = res.basisValue.toPlainString(),
                            basisPercentage = res.basisPercentage.toPlainString(),
                            allocatedBdtMinor = res.allocatedBdtMinor,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }
            }

            val now = System.currentTimeMillis()
            val snapshots = engineResult.productBreakdowns.map { b ->
                LandedCostSnapshot(
                    id = "lcs-${UUID.randomUUID()}",
                    shipmentId = shipmentId,
                    productId = b.productId,
                    variantId = b.variantId,
                    productNameEn = b.productNameEn,
                    productNameBn = b.productNameBn,
                    sku = b.sku,
                    totalReceivedQuantity = b.packedQuantity,
                    totalPurchaseCostBdtMinor = b.purchaseCostBdtMinor,
                    totalAllocatedCostsBdtMinor = b.totalAllocatedExpensesBdtMinor,
                    totalLandedCostBdtMinor = b.totalProductImportCostBdtMinor,
                    unitLandedCostBdtMinor = b.unitImportCostBdtMinor,
                    snapshotDate = now,
                    isFinal = true,
                    chinaDeliveryBdtMinor = b.chinaDeliveryBdtMinor,
                    internationalFreightBdtMinor = b.internationalFreightBdtMinor,
                    bdTransportBdtMinor = b.bdTransportBdtMinor,
                    otherExpensesBdtMinor = b.otherImportExpensesBdtMinor,
                    packedQuantity = b.packedQuantity,
                    goodQuantity = b.goodQuantity,
                    damagedQuantity = b.damagedQuantity,
                    missingQuantity = b.missingQuantity,
                    pendingQuantity = b.pendingQuantity
                )
            }

            importCostDao.replaceAllocationsAndSnapshots(shipmentId, allAllocations, snapshots)

            // Mark all current cost items as capitalized
            if (costItems.isNotEmpty()) {
                importCostDao.markCostItemsCapitalized(costItems.map { it.id }, "FINALIZED")
            }

            // Update any existing FIFO layers for this shipment matching both productId and variantId
            val existingLayers = inventoryCostLayerDao.getLayersForShipmentSync(shipmentId)
            for (layer in existingLayers) {
                val matchingSnapshot = snapshots.find { it.productId == layer.productId && it.variantId == layer.variantId }
                    ?: snapshots.find { it.productId == layer.productId && it.variantId == null }
                if (matchingSnapshot != null) {
                    val unitPurchase = if (matchingSnapshot.packedQuantity > 0) {
                        BigDecimal(matchingSnapshot.totalPurchaseCostBdtMinor)
                            .divide(BigDecimal(matchingSnapshot.packedQuantity), 0, RoundingMode.HALF_UP)
                            .longValueExact()
                    } else 0L
                    inventoryCostLayerDao.updateCostLayerUnitCost(
                        layerId = layer.id,
                        unitCostMinor = matchingSnapshot.unitLandedCostBdtMinor,
                        allocatedExpenseMinor = (matchingSnapshot.unitLandedCostBdtMinor - unitPurchase).coerceAtLeast(0L)
                    )
                }
            }

            // Mark linked POs as Actual Cost Finalized so Warehouse Receiving is unlocked
            val linkedPos = purchaseOrderDao.getOrdersForShipmentSync(shipmentId)
            val allPoIds = (linkedPos.map { it.id } + listOfNotNull(shipment.purchaseOrderId)).distinct()
            for (poId in allPoIds) {
                val po = purchaseOrderDao.getOrderById(poId)
                if (po != null && !po.isActualCostFinalized) {
                    purchaseOrderDao.markActualCostFinalized(
                        orderId = poId,
                        status = PurchaseOrderStatus.ACTUAL_COST_FINALIZED.name,
                        finalizedAt = now
                    )
                }
            }

            database.auditLogDao().insertAuditLog(
                AuditLog(
                    recordType = "IMPORT_SHIPMENT",
                    recordId = shipmentId,
                    action = "FINALIZE_LANDING_COST",
                    details = "Finalized Landing Cost for ${shipment.shipmentNumber}. Grand Total: ৳${engineResult.grandTotalLandingCostBdtMinor / 100.0}",
                    operator = operatorName,
                    documentNumber = shipment.shipmentNumber,
                    operationType = "LANDING_COST_FINALIZE",
                    previousStatus = shipment.status,
                    newStatus = "COST_FINALIZED"
                )
            )

            snapshots
        }
    }

    override suspend fun applyPostFinalizationCostAdjustment(
        shipmentId: String,
        operatorRole: String,
        operatorName: String,
        reason: String
    ): LandingCostEngine.PostFinalizeAdjustmentResult = database.withTransaction {
        checkFinalizePermission(operatorRole)

        val shipment = importShipmentDao.getShipmentById(shipmentId)
            ?: throw IllegalArgumentException("Shipment not found: $shipmentId")
        val existingSnapshots = importCostDao.getSnapshotsForShipmentSync(shipmentId)
        require(existingSnapshots.isNotEmpty() && existingSnapshots.all { it.isFinal }) {
            "Shipment ${shipment.shipmentNumber} must be finalized before applying post-finalization cost adjustments"
        }

        val uncapitalizedItems = importCostDao.getUncapitalizedCostItemsForShipmentSync(shipmentId)
        require(uncapitalizedItems.isNotEmpty()) {
            "No new unallocated post-finalization expenses found for shipment ${shipment.shipmentNumber}"
        }

        val cartons = importShipmentDao.getCartonsForShipmentSync(shipmentId)
        val packagingSummary = LandingCostEngine.summarizePackagingList(
            cartons.map { c ->
                LandingCostEngine.ShipmentCartonInput(
                    cartonNumber = c.cartonNumber,
                    supplierId = c.supplierId ?: shipment.supplierId,
                    productId = c.productId,
                    variantId = c.variantId,
                    categoryId = c.categoryId ?: "cat-uncategorized",
                    quantity = c.quantity,
                    grossWeightKg = BigDecimal(c.grossWeightKg.ifBlank { "0" }),
                    cbm = BigDecimal(c.cbm.ifBlank { "0" })
                )
            }
        )

        val existingBreakdowns = existingSnapshots.map { s ->
            val packed = if (s.packedQuantity > 0) s.packedQuantity else s.totalReceivedQuantity
            LandingCostEngine.ProductLandingCostBreakdown(
                productId = s.productId,
                variantId = s.variantId,
                productNameEn = s.productNameEn,
                productNameBn = s.productNameBn,
                sku = s.sku,
                supplierId = shipment.supplierId,
                categoryId = "cat-uncategorized",
                packedQuantity = packed,
                goodQuantity = if (s.goodQuantity > 0) s.goodQuantity else packed,
                damagedQuantity = s.damagedQuantity,
                missingQuantity = s.missingQuantity,
                pendingQuantity = s.pendingQuantity,
                grossWeightKg = packagingSummary.productWeightMap[s.productId] ?: BigDecimal.ONE,
                cbm = packagingSummary.productCbmMap[s.productId] ?: BigDecimal.ONE,
                cartonCount = packagingSummary.productCartonCountMap[s.productId] ?: 1,
                purchaseCostForeignMinor = s.totalPurchaseCostBdtMinor,
                purchaseCurrency = "BDT",
                purchaseExchangeRateToBdt = BigDecimal.ONE,
                purchaseCostBdtMinor = s.totalPurchaseCostBdtMinor,
                chinaDeliveryForeignMinor = s.chinaDeliveryBdtMinor,
                chinaDeliveryBdtMinor = s.chinaDeliveryBdtMinor,
                internationalFreightBdtMinor = s.internationalFreightBdtMinor,
                bdTransportBdtMinor = s.bdTransportBdtMinor,
                otherImportExpensesBdtMinor = s.otherExpensesBdtMinor,
                totalAllocatedExpensesBdtMinor = s.totalAllocatedCostsBdtMinor,
                totalProductImportCostBdtMinor = s.totalLandedCostBdtMinor,
                unitImportCostBdtMinor = s.unitLandedCostBdtMinor,
                goodInventoryValuationBdtMinor = s.goodQuantity.toLong() * s.unitLandedCostBdtMinor,
                damagedWriteOffValuationBdtMinor = s.damagedQuantity.toLong() * s.unitLandedCostBdtMinor,
                missingShortageValuationBdtMinor = s.missingQuantity.toLong() * s.unitLandedCostBdtMinor,
                pendingInTransitValuationBdtMinor = s.pendingQuantity.toLong() * s.unitLandedCostBdtMinor
            )
        }

        val layers = inventoryCostLayerDao.getLayersForShipmentSync(shipmentId)
        val fifoLayersByProduct = layers.groupBy { it.productId }.mapValues { (_, prodLayers) ->
            prodLayers.map { layer ->
                val consumptions = database.fifoConsumptionDao().getConsumptionsForLayerSync(layer.id)
                val soldFromFifo = consumptions.sumOf { it.quantity }
                val effectiveSold = if (soldFromFifo > 0) soldFromFifo else (layer.initialQuantity - layer.remainingQuantity).coerceAtLeast(0)
                LandingCostEngine.FifoLayerAdjustmentInput(
                    layerId = layer.id,
                    productId = layer.productId,
                    variantId = layer.variantId,
                    warehouseId = layer.warehouseId,
                    initialQuantity = layer.initialQuantity,
                    remainingQuantity = layer.remainingQuantity,
                    soldQuantity = effectiveSold,
                    currentUnitLandedCostBdtMinor = layer.unitLandedCostBdtMinor
                )
            }
        }

        val newExpenseInputs = uncapitalizedItems.map { ci ->
            val cat = try {
                CostCategory.valueOf(ci.costCategory)
            } catch (_: Exception) {
                CostCategory.OTHER_EXPENSES
            }
            val method = try {
                AllocationMethod.valueOf(ci.allocationMethod)
            } catch (_: Exception) {
                AllocationMethod.PURCHASE_VALUE
            }
            LandingCostEngine.ImportExpenseInput(
                expenseId = ci.id,
                expenseName = ci.description,
                category = cat,
                amountForeignMinor = ci.amountMinor,
                currency = ci.currency,
                exchangeRateToBdt = BigDecimal(ci.exchangeRateToBdt.ifBlank { "1.0" }),
                allocationMethod = method,
                supplierId = ci.supplierId,
                manualProductAllocationsBdtMinor = LandedCostAllocationEngine.parseManualAllocations(ci.manualAllocationsJson),
                isPostFinalization = true,
                isAlreadyCapitalized = false
            )
        }

        val adjCount = database.inventoryCostAdjustmentDao().getAdjustmentCount()
        val adjustmentRef = "LCA-2026-%04d".format(adjCount + 1)

        val adjResult = LandingCostEngine.calculatePostFinalizeAdjustment(
            adjustmentReference = adjustmentRef,
            existingBreakdowns = existingBreakdowns,
            fifoLayersByProduct = fifoLayersByProduct,
            newPostFinalizeExpenses = newExpenseInputs
        )

        database.withTransaction {
            val now = System.currentTimeMillis()
            var journalEntryId: String? = null

            // Post Double-Entry Accounting for Post-Finalize Cost Adjustment:
            // Dr. 1210 Inventory Asset (Remaining FIFO Qty)
            // Dr. 5010 COGS (Already Sold FIFO Qty)
            // Dr. 5050 Write-off / 1220 In-Transit (Non-sellable / Pending Qty)
            // Cr. 1220 In-Transit Inventory / 2010 Clearing
            val journalLines = mutableListOf<JournalLineDraft>()
            if (adjResult.totalInventoryAssetAdjustmentBdtMinor > 0L) {
                journalLines.add(
                    JournalLineDraft(
                        accountId = "coa-1210",
                        isDebit = true,
                        amountMinor = adjResult.totalInventoryAssetAdjustmentBdtMinor,
                        description = "Post-finalize inventory valuation increase ($adjustmentRef)"
                    )
                )
            }
            if (adjResult.totalCogsAdjustmentBdtMinor > 0L) {
                journalLines.add(
                    JournalLineDraft(
                        accountId = "coa-5010",
                        isDebit = true,
                        amountMinor = adjResult.totalCogsAdjustmentBdtMinor,
                        description = "Post-finalize COGS adjustment for sold units ($adjustmentRef)"
                    )
                )
            }
            if (adjResult.totalOtherAdjustmentBdtMinor > 0L) {
                journalLines.add(
                    JournalLineDraft(
                        accountId = "coa-5050",
                        isDebit = true,
                        amountMinor = adjResult.totalOtherAdjustmentBdtMinor,
                        description = "Post-finalize adjustment on non-sellable/pending units ($adjustmentRef)"
                    )
                )
            }
            if (adjResult.totalAddedExpenseBdtMinor > 0L) {
                journalLines.add(
                    JournalLineDraft(
                        accountId = "coa-1220",
                        isDebit = false,
                        amountMinor = adjResult.totalAddedExpenseBdtMinor,
                        description = "Clear capitalized post-finalization import expenses ($adjustmentRef)"
                    )
                )
            }

            if (journalLines.size >= 2) {
                try {
                    journalEntryId = accountingRepository.postJournalEntry(
                        entryDate = now,
                        descEn = "Post-Finalize Landed Cost Adjustment $adjustmentRef (${shipment.shipmentNumber})",
                        descBn = "আমদানি পরবর্তী ব্যয় সমন্বয় $adjustmentRef (${shipment.shipmentNumber})",
                        referenceType = "LANDED_COST_ADJUSTMENT",
                        referenceId = adjustmentRef,
                        lines = journalLines
                    )
                } catch (e: Exception) {
                    throw e
                }
            }

            // Update FIFO layers and insert InventoryCostAdjustment audit trail
            val prodAdjByProduct = adjResult.productAdjustments.associateBy { it.productId }
            var seq = 1
            for (prodAdj in adjResult.productAdjustments) {
                val prodLayers = layers.filter { it.productId == prodAdj.productId }
                if (prodLayers.isNotEmpty()) {
                    prodLayers.forEach { layer ->
                        val newUnitCost = layer.unitLandedCostBdtMinor + prodAdj.unitVarianceBdtMinor
                        val newAllocExp = layer.allocatedExpensePerUnitBdtMinor + prodAdj.unitVarianceBdtMinor
                        inventoryCostLayerDao.updateCostLayerUnitCost(
                            layerId = layer.id,
                            unitCostMinor = newUnitCost,
                            allocatedExpenseMinor = newAllocExp
                        )

                        database.inventoryCostAdjustmentDao().insertAdjustment(
                            InventoryCostAdjustment(
                                id = "ica-${UUID.randomUUID()}",
                                adjustmentNumber = "$adjustmentRef-${seq++}",
                                costLayerId = layer.id,
                                productId = prodAdj.productId,
                                variantId = prodAdj.variantId,
                                warehouseId = layer.warehouseId,
                                purchaseOrderId = shipment.purchaseOrderId ?: shipmentId,
                                referenceType = "POST_FINALIZE_IMPORT_EXPENSE",
                                referenceId = shipmentId,
                                reason = "$reason [$adjustmentRef]",
                                oldUnitCostBdtMinor = prodAdj.oldUnitCostBdtMinor,
                                newUnitCostBdtMinor = prodAdj.newUnitCostBdtMinor,
                                unitVarianceBdtMinor = prodAdj.unitVarianceBdtMinor,
                                totalRemainingQty = prodAdj.totalRemainingQty,
                                totalSoldQty = prodAdj.totalSoldQty,
                                inventoryAdjustmentAmountMinor = prodAdj.inventoryAdjustmentBdtMinor,
                                cogsAdjustmentAmountMinor = prodAdj.cogsAdjustmentBdtMinor,
                                totalAdjustmentAmountMinor = prodAdj.addedCostBdtMinor,
                                journalEntryId = journalEntryId,
                                createdAt = now
                            )
                        )
                    }
                }
            }

            // Update snapshots with new total allocated expenses & unit cost
            val updatedSnapshots = existingSnapshots.map { snap ->
                val pAdj = prodAdjByProduct[snap.productId]
                if (pAdj != null) {
                    snap.copy(
                        totalAllocatedCostsBdtMinor = snap.totalAllocatedCostsBdtMinor + pAdj.addedCostBdtMinor,
                        totalLandedCostBdtMinor = snap.totalLandedCostBdtMinor + pAdj.addedCostBdtMinor,
                        unitLandedCostBdtMinor = pAdj.newUnitCostBdtMinor,
                        otherExpensesBdtMinor = snap.otherExpensesBdtMinor + pAdj.addedCostBdtMinor
                    )
                } else snap
            }
            importCostDao.deleteSnapshotsForShipment(shipmentId)
            importCostDao.insertSnapshots(updatedSnapshots)

            // Mark post-finalize expenses as capitalized with adjustmentRef
            importCostDao.markCostItemsCapitalized(uncapitalizedItems.map { it.id }, adjustmentRef)

            database.auditLogDao().insertAuditLog(
                AuditLog(
                    recordType = "IMPORT_SHIPMENT",
                    recordId = shipmentId,
                    action = "POST_FINALIZE_ADJUSTMENT",
                    details = "Post-finalize adjustment $adjustmentRef: Added ৳${adjResult.totalAddedExpenseBdtMinor / 100.0}, Inv Adj: ৳${adjResult.totalInventoryAssetAdjustmentBdtMinor / 100.0}, COGS Adj: ৳${adjResult.totalCogsAdjustmentBdtMinor / 100.0}",
                    operator = operatorName,
                    documentNumber = adjustmentRef,
                    operationType = "COST_ADJUSTMENT",
                    previousStatus = "FINALIZED",
                    newStatus = "ADJUSTED"
                )
            )
        }

        return@withTransaction adjResult
    }

    override fun getCostAdjustmentsForShipment(shipmentId: String): Flow<List<InventoryCostAdjustment>> =
        database.inventoryCostAdjustmentDao().getAdjustmentsForReference(shipmentId)

    // =========================================================================
    // PHASE 2G: WAREHOUSE MONTHLY RENT (OPERATING EXPENSE — EXCLUDED FROM LANDING COST)
    // =========================================================================

    override fun getWarehouseRentExpenses(warehouseId: String?): Flow<List<ExpenseVoucher>> =
        database.expenseDao().getWarehouseRentExpenses(warehouseId)

    override suspend fun recordWarehouseMonthlyRent(
        warehouseId: String,
        rentMonth: String,
        amountMinor: Long,
        paymentAccountId: String,
        voucherDate: Long,
        notes: String?
    ): String = database.withTransaction {
        require(amountMinor > 0L) { "Warehouse rent amount must be greater than zero" }
        require(rentMonth.isNotBlank()) { "Rent month (e.g. 2026-11) is required" }

        val warehouse = database.warehouseDao().getWarehouseById(warehouseId)
        val whName = warehouse?.name ?: warehouseId
        val count = database.expenseDao().getExpenseCount()
        val voucherNumber = "RENT-${rentMonth.replace("-", "")}-%04d".format(count + 1)
        val voucherId = "exp-rent-${UUID.randomUUID()}"

        val descEn = "Monthly Warehouse Rent ($whName - $rentMonth)"
        val descBn = "মাসিক গুদাম ভাড়া ($whName - $rentMonth)"

        var journalEntryId: String? = null
        try {
            journalEntryId = accountingRepository.postJournalEntry(
                entryDate = voucherDate,
                descEn = "$descEn [$voucherNumber]",
                descBn = "$descBn [$voucherNumber]",
                referenceType = "WAREHOUSE_RENT_EXPENSE",
                referenceId = voucherId,
                lines = listOf(
                    JournalLineDraft(
                        accountId = "coa-5110", // Office Rent & Warehouse Utilities (Operating Expense)
                        isDebit = true,
                        amountMinor = amountMinor,
                        description = descEn
                    ),
                    JournalLineDraft(
                        accountId = paymentAccountId,
                        isDebit = false,
                        amountMinor = amountMinor,
                        description = "Paid $descEn"
                    )
                )
            )
        } catch (e: Exception) {
            throw e
        }

        val voucher = ExpenseVoucher(
            id = voucherId,
            voucherNumber = voucherNumber,
            category = ExpenseCategory.WAREHOUSE_RENT.name,
            descriptionEn = descEn,
            descriptionBn = descBn,
            amountMinor = amountMinor,
            currency = "BDT",
            exchangeRateToBdt = "1.0000",
            bdtAmountMinor = amountMinor,
            paymentAccountId = paymentAccountId,
            expenseAccountId = "coa-5110",
            voucherDate = voucherDate,
            referenceNumber = rentMonth,
            isPosted = true,
            isReversed = false,
            journalEntryId = journalEntryId,
            notes = notes,
            createdAt = System.currentTimeMillis(),
            warehouseId = warehouseId,
            rentMonth = rentMonth
        )
        database.expenseDao().insertExpense(voucher)
        return@withTransaction voucherId
    }

    // =========================================================================
    // PHASE 2H: CUSTOMER DELIVERY EXPENSE (SALES EXPENSE — EXCLUDED FROM LANDING COST)
    // =========================================================================

    override suspend fun recordCustomerDeliveryExpense(
        salesInvoiceId: String,
        deliveryChallanNumber: String?,
        amountMinor: Long,
        paymentAccountId: String,
        voucherDate: Long,
        notes: String?
    ): String = database.withTransaction {
        require(amountMinor > 0L) { "Customer delivery expense must be greater than zero" }
        require(salesInvoiceId.isNotBlank()) { "Sales Invoice reference is required for Customer Delivery Expense" }

        val invoice = database.salesDao().getInvoiceById(salesInvoiceId)
        val invNum = invoice?.invoiceNumber ?: salesInvoiceId
        val challanTag = if (!deliveryChallanNumber.isNullOrBlank()) " / Challan #$deliveryChallanNumber" else ""
        val count = database.expenseDao().getExpenseCount()
        val voucherNumber = "CDEL-2026-%04d".format(count + 1)
        val voucherId = "exp-cdel-${UUID.randomUUID()}"

        val descEn = "Customer Delivery Expense for Invoice #$invNum$challanTag"
        val descBn = "ক্রেতা ডেলিভারি খরচ (ইনভয়েস #$invNum$challanTag)"

        var journalEntryId: String? = null
        try {
            journalEntryId = accountingRepository.postJournalEntry(
                entryDate = voucherDate,
                descEn = "$descEn [$voucherNumber]",
                descBn = "$descBn [$voucherNumber]",
                referenceType = "CUSTOMER_DELIVERY_EXPENSE",
                referenceId = voucherId,
                lines = listOf(
                    JournalLineDraft(
                        accountId = "coa-5040", // Delivery / Outbound Logistics Expense
                        isDebit = true,
                        amountMinor = amountMinor,
                        description = descEn
                    ),
                    JournalLineDraft(
                        accountId = paymentAccountId,
                        isDebit = false,
                        amountMinor = amountMinor,
                        description = "Paid $descEn"
                    )
                )
            )
        } catch (e: Exception) {
            throw e
        }

        val voucher = ExpenseVoucher(
            id = voucherId,
            voucherNumber = voucherNumber,
            category = ExpenseCategory.CUSTOMER_DELIVERY.name,
            descriptionEn = descEn,
            descriptionBn = descBn,
            amountMinor = amountMinor,
            currency = "BDT",
            exchangeRateToBdt = "1.0000",
            bdtAmountMinor = amountMinor,
            paymentAccountId = paymentAccountId,
            expenseAccountId = "coa-5040",
            voucherDate = voucherDate,
            referenceNumber = deliveryChallanNumber ?: invNum,
            isPosted = true,
            isReversed = false,
            journalEntryId = journalEntryId,
            notes = notes,
            createdAt = System.currentTimeMillis(),
            salesInvoiceId = salesInvoiceId,
            deliveryChallanNumber = deliveryChallanNumber
        )
        database.expenseDao().insertExpense(voucher)
        return@withTransaction voucherId
    }
}
