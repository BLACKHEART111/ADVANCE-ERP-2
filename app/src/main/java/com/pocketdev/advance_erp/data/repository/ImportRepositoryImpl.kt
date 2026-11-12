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

    override suspend fun createPurchaseOrder(
        supplierId: String,
        currency: String,
        exchangeRate: BigDecimal,
        items: List<PurchaseOrderItemDraft>,
        notes: String?,
        status: PurchaseOrderStatus
    ): String {
        require(items.isNotEmpty()) { "Purchase order must contain at least 1 item" }
        require(exchangeRate > BigDecimal.ZERO) { "Exchange rate must be positive: $exchangeRate" }

        val orderCount = purchaseOrderDao.getOrderCount()
        val poNumber = "PO-2026-CN-%03d".format(orderCount + 1)
        val orderId = "po-${UUID.randomUUID()}"

        var totalOriginalMinor = 0L
        var totalBdtMinor = 0L

        val poItems = items.map { draft ->
            require(draft.quantity > 0) { "Quantity must be greater than 0" }
            require(draft.unitPriceMinor > 0) { "Unit price must be greater than 0" }

            val lineOriginalMinor = draft.quantity.toLong() * draft.unitPriceMinor
            val lineBdtMinor = BigDecimal(lineOriginalMinor)
                .multiply(exchangeRate)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact()

            totalOriginalMinor += lineOriginalMinor
            totalBdtMinor += lineBdtMinor

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
            orderDate = System.currentTimeMillis(),
            notes = notes,
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
        val batchNumber = "BATCH-$poNumber"
        val batchShipment = ImportShipment(
            id = "shp-${UUID.randomUUID()}",
            shipmentNumber = batchNumber,
            supplierId = supplierId,
            purchaseOrderId = orderId,
            shippingMethod = ShippingMethod.SEA.name,
            origin = "Guangzhou, China",
            destination = "Chattogram Port, Bangladesh",
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

    override suspend fun duplicatePurchaseOrder(orderId: String): String {
        val originalOrder = purchaseOrderDao.getOrderById(orderId)
            ?: throw IllegalArgumentException("Purchase Order not found: $orderId")
        val originalItems = purchaseOrderDao.getItemsForOrderSync(orderId)

        val poCount = purchaseOrderDao.getOrderCount()
        val newPoNumber = "PO-%s-%04d".format(
            java.text.SimpleDateFormat("yyyyMM", java.util.Locale.US).format(java.util.Date()),
            poCount + 1
        )
        val newOrderId = "po-${UUID.randomUUID()}"

        val newOrder = originalOrder.copy(
            id = newOrderId,
            poNumber = newPoNumber,
            status = PurchaseOrderStatus.DRAFT.name,
            paidAmountMinor = 0L,
            paidBdtAmountMinor = 0L,
            orderDate = System.currentTimeMillis(),
            notes = "Duplicated from ${originalOrder.poNumber}. ${originalOrder.notes ?: ""}".trim(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        val newItems = originalItems.map { item ->
            item.copy(
                id = "poi-${UUID.randomUUID()}",
                purchaseOrderId = newOrderId,
                receivedQuantity = 0
            )
        }

        database.withTransaction {
            purchaseOrderDao.insertOrder(newOrder)
            purchaseOrderDao.insertItems(newItems)
        }
        return newOrderId
    }

    override suspend fun cancelPurchaseOrder(orderId: String, reason: String?) {
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
        check(existingShipments.isEmpty()) {
            "Cannot cancel Purchase Order: linked shipments exist (${existingShipments.size} shipment(s))"
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
        notes: String?
    ): String {
        require(amountMinor > 0) { "Payment amount must be positive" }
        require(exchangeRate > BigDecimal.ZERO) { "Exchange rate must be positive" }

        var targetOrder: PurchaseOrder? = null
        if (orderId != null) {
            targetOrder = purchaseOrderDao.getOrderById(orderId)
                ?: throw IllegalArgumentException("Purchase Order not found: $orderId")
            val remainingBalance = targetOrder.totalAmountMinor - targetOrder.paidAmountMinor
            require(amountMinor <= remainingBalance) {
                "Payment amount ($amountMinor) exceeds remaining PO balance ($remainingBalance)"
            }
        }

        val paymentCount = supplierPaymentDao.getPaymentCount()
        val paymentNumber = "SP-2026-%04d".format(paymentCount + 1)
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
            paymentDate = System.currentTimeMillis(),
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
            val newStatus = if (newPaidAmount >= targetOrder.totalAmountMinor) {
                PurchaseOrderStatus.PAID.name
            } else {
                PurchaseOrderStatus.PARTIALLY_PAID.name
            }
            purchaseOrderDao.recordPayment(orderId, amountMinor, bdtAmountMinor, newStatus)
        }

        // Automatic Double-Entry Accounting:
        // Dr. Accounts Payable (2010)
        // Cr. Bank (1020)
        try {
            accountingRepository.postJournalEntry(
                entryDate = System.currentTimeMillis(),
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
                        accountId = "coa-1020", // City Bank Corporate
                        isDebit = false,
                        amountMinor = bdtAmountMinor,
                        description = "Credit Bank Account for remittance $paymentNumber"
                    )
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return paymentId
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
        val shipmentNumber = "SHP-2026-%04d".format(shipmentCount + 1)
        val shipmentId = "shp-${UUID.randomUUID()}"

        var totalQty = 0
        var totalGrossWeight = BigDecimal.ZERO
        var totalNetWeight = BigDecimal.ZERO
        var totalCbm = BigDecimal.ZERO

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
            totalCbm = totalCbm.add(cbm)

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
            totalCartons = cartons.map { it.cartonNumber }.distinct().size.coerceAtLeast(cartons.size),
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
        docRef: String?
    ): String {
        require(amountMinor > 0) { "Cost amount must be greater than zero" }
        require(exchangeRate > BigDecimal.ZERO) { "Exchange rate must be positive" }

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
            e.printStackTrace()
        }

        return costId
    }

    override suspend fun deleteCostItem(costItemId: String) {
        importCostDao.deleteCostItem(costItemId)
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
    ): CostAdjustmentResult {
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
                e.printStackTrace()
            }
        }

        return CostAdjustmentResult(
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
        notes: String?
    ): String {
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
            val snapshot = snapshots[key]
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
        return receiptId
    }

    override suspend fun postWarehouseReceipt(receiptId: String, allowDiscrepancy: Boolean) {
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
        val linkedPoId = receipt.purchaseOrderId ?: shipment?.purchaseOrderId
        if (linkedPoId != null) {
            val linkedPo = purchaseOrderDao.getOrderById(linkedPoId)
            if (linkedPo != null && !linkedPo.isActualCostFinalized) {
                throw IllegalStateException("Cannot post stock receiving: Actual Landing Cost must be finalized before posting stock into inventory!")
            }
        }

        // ATOMIC DATABASE TRANSACTION
        database.withTransaction {
            var totalCapitalizedValueBdt = 0L
            var totalDamagedValueBdt = 0L
            var totalMissingValueBdt = 0L
            var hasRemainingPending = false

            items.forEach { item ->
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
                            unitCostMinor = item.unitLandedCostMinor,
                            balanceAfterMovement = newBalance.quantity,
                            totalCostImpactMinor = goodQuantity.toLong() * item.unitLandedCostMinor,
                            referenceType = "WAREHOUSE_RECEIPT",
                            referenceId = receipt.receiptNumber,
                            purchaseOrderId = shipment?.shipmentNumber ?: receipt.shipmentId,
                            purchaseBatchId = receipt.receiptNumber,
                            notes = "Import receiving check-in from ${shipment?.shipmentNumber ?: receipt.shipmentId}",
                            createdAt = System.currentTimeMillis()
                        )
                    )

                    // 3. Batch Cost Layer for sound stock
                    val product = productDao.getProductById(item.productId)
                    val unitCost = item.unitLandedCostMinor
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
                            originalCurrency = shipment?.let { "CNY" } ?: "BDT",
                            originalPurchasePriceMinor = product?.purchasePriceMinor ?: 0L,
                            exchangeRateToBdt = "17.0000",
                            allocatedExpensePerUnitBdtMinor = (unitCost - (product?.purchasePriceMinor ?: 0L)).coerceAtLeast(0L),
                            receiptDate = receipt.receiptDate,
                            isActive = true,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }

                // 4. Record Damaged Stock (Excluded from sellable stock -> 5050 Write-Off)
                if (item.damagedQuantity > 0) {
                    val dmgLoss = item.damagedQuantity.toLong() * item.unitLandedCostMinor
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
                            unitCostMinor = item.unitLandedCostMinor,
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
                    val misLoss = item.missingQuantity.toLong() * item.unitLandedCostMinor
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
                            unitCostMinor = item.unitLandedCostMinor,
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
                    e.printStackTrace()
                }
            }
        }
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
    ): List<String> {
        require(lines.isNotEmpty()) { "Mixed carton must have at least one product line" }
        val po = purchaseOrderDao.getOrderById(orderId)
            ?: throw IllegalArgumentException("Purchase order not found: $orderId")
        val batchShipment = importShipmentDao.getBatchShipmentForOrder(orderId)
            ?: throw IllegalStateException("Batch shipment not found for order: $orderId")

        // Run allocation engine
        val lineInputs = lines.map {
            MixedCartonLineInput(
                productId = it.productId,
                variantId = it.variantId,
                productName = it.productName,
                quantity = it.quantity
            )
        }
        val allocated = CartonWeightAllocationEngine.allocateWeights(
            cartonGrossWeightKg = grossWeightKg,
            cartonNetWeightKg = netWeightKg,
            lines = lineInputs
        )

        val cbm = CbmCalculator.calculateCbm(lengthCm, widthCm, heightCm, 1)
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
                cartonNumber = cartonNumber,
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

        return insertedIds
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
        notes: String?
    ): String {
        val po = purchaseOrderDao.getOrderById(orderId)
            ?: throw IllegalArgumentException("Purchase order not found: $orderId")
        val batchShipment = importShipmentDao.getBatchShipmentForOrder(orderId)
            ?: throw IllegalStateException("Batch shipment not found for order: $orderId")

        val bdtAmountMinor = BigDecimal(amountMinor)
            .multiply(exchangeRate)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()

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

        val actualCosts = importCostDao.getActualCostItemsForOrderSync(orderId)
        require(actualCosts.isNotEmpty()) {
            "Cannot finalize landed cost: No actual cost items recorded for purchase ${po.poNumber}!"
        }

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
            val method = AllocationMethod.valueOf(costItem.allocationMethod)
            val results = LandedCostAllocationEngine.allocate(costItem.bdtAmountMinor, allocationInputs, method)

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

        // Build finalized immutable snapshots
        val snapshots = allocationInputs.map { input ->
            val parts = input.id.split(":")
            val productId = parts[0]
            val variantId = if (parts.size > 1) parts[1] else null

            val product = productDao.getProductById(productId)
            val variant = if (variantId != null) productVariantDao.getVariantById(variantId) else null

            val allocatedBdt = totalAllocatedPerItem[input.id] ?: 0L
            val purchaseBdt = input.purchaseValueBdtMinor
            val landedBdt = purchaseBdt + allocatedBdt
            val unitLandedBdt = if (input.quantity > 0) landedBdt / input.quantity.toLong() else 0L

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
                snapshotDate = System.currentTimeMillis(),
                isFinal = true
            )
        }

        importCostDao.replaceAllocationsAndSnapshots(batchShipment.id, allAllocations, snapshots)

        // Mark PO and Batch as ACTUAL_COST_FINALIZED
        purchaseOrderDao.markActualCostFinalized(orderId, PurchaseOrderStatus.ACTUAL_COST_FINALIZED.name)
        importShipmentDao.updateShipmentStatus(batchShipment.id, ShipmentStatus.ARRIVED.name)

        return snapshots
    }

    override suspend fun createPurchaseReceiving(
        orderId: String,
        warehouseId: String,
        receivingMethod: String,
        items: List<ReceiptItemDraft>,
        notes: String?
    ): String {
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
            val snapshot = snapshots[key]
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
        return receiptId
    }

    override suspend fun postPurchaseReceiving(receiptId: String, allowDiscrepancy: Boolean) {
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

        // CRITICAL BUSINESS RULE: Posting stock is strictly blocked until Actual Landing Cost is finalized!
        check(po.isActualCostFinalized) {
            "Cannot post stock receiving: Actual Landing Cost must be finalized before posting stock into inventory!"
        }

        val items = warehouseReceiptDao.getItemsForReceiptSync(receiptId)
        require(items.isNotEmpty()) { "Cannot post empty warehouse receipt" }

        val batchShipment = importShipmentDao.getShipmentById(receipt.shipmentId)
        val snapshots = importCostDao.getSnapshotsForShipmentSync(receipt.shipmentId).associateBy {
            if (it.variantId != null) "${it.productId}:${it.variantId}" else it.productId
        }
        val poItems = purchaseOrderDao.getItemsForOrderSync(orderId).associateBy {
            if (it.variantId != null) "${it.productId}:${it.variantId}" else it.productId
        }

        database.withTransaction {
            var totalGoodValueBdt = 0L
            var totalDamageValueBdt = 0L
            var totalMissingValueBdt = 0L

            items.forEach { item ->
                val key = if (item.variantId != null) "${item.productId}:${item.variantId}" else item.productId
                val snapshot = snapshots[key]
                val poItem = poItems[key]

                // Calculate precise unit landed cost: purchase cost + allocated landed cost
                val unitPurchaseCostBdt = if (poItem != null && poItem.quantity > 0) {
                    poItem.totalBdtAmountMinor / poItem.quantity.toLong()
                } else {
                    productDao.getProductById(item.productId)?.purchasePriceMinor ?: 0L
                }
                val unitAllocatedCostBdt = if (snapshot != null && snapshot.totalReceivedQuantity > 0) {
                    snapshot.totalAllocatedCostsBdtMinor / snapshot.totalReceivedQuantity.toLong()
                } else 0L

                val finalUnitCost = unitPurchaseCostBdt + unitAllocatedCostBdt

                val goodQuantity = (item.receivedQuantity - item.damagedQuantity - item.missingQuantity).coerceAtLeast(0)

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

                    // FIFO Cost Layer
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
                            originalPurchasePriceMinor = poItem?.unitPriceMinor ?: 0L,
                            exchangeRateToBdt = po.exchangeRateToBdt,
                            allocatedExpensePerUnitBdtMinor = unitAllocatedCostBdt,
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
                    val curQty = stockBalanceDao.getBalance(receipt.warehouseId, item.productId)?.quantity ?: 0

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
    ): PurchaseCompletionRecord {
        val po = purchaseOrderDao.getOrderById(orderId)
            ?: throw IllegalArgumentException("Purchase order not found: $orderId")

        val poItems = purchaseOrderDao.getItemsForOrderSync(orderId)
        val totalOrdered = poItems.sumOf { it.quantity }
        val totalReceived = poItems.sumOf { it.receivedQuantity }
        val remaining = (totalOrdered - totalReceived).coerceAtLeast(0)

        val receipts = warehouseReceiptDao.getReceiptsByOrderSync(orderId)
        val allReceiptItems = receipts.flatMap { warehouseReceiptDao.getItemsForReceiptSync(it.id) }
        val goodQty = allReceiptItems.sumOf { (it.receivedQuantity - it.damagedQuantity - it.missingQuantity).coerceAtLeast(0) }
        val damagedQty = allReceiptItems.sumOf { it.damagedQuantity }
        val missingQty = allReceiptItems.sumOf { it.missingQuantity }

        if (remaining > 0) {
            require(!disposition.isNullOrBlank()) {
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

        return record
    }

    override fun getCompletionRecordForOrder(orderId: String): Flow<PurchaseCompletionRecord?> =
        minimalImportDao.getCompletionRecordForOrderFlow(orderId)

    override fun getDamageRecordsForOrder(orderId: String): Flow<List<DamageRecord>> =
        minimalImportDao.getDamageRecordsForOrder(orderId)

    override fun getShortageRecordsForOrder(orderId: String): Flow<List<ShortageRecord>> =
        minimalImportDao.getShortageRecordsForOrder(orderId)

    override suspend fun deleteCarton(cartonId: String) {
        importShipmentDao.deleteCartonById(cartonId)
    }

    override suspend fun deleteCartonByNumber(orderId: String, cartonNumber: String) {
        importShipmentDao.deleteCartonByNumber(orderId, cartonNumber)
    }

    override suspend fun deletePurchaseCostItem(costItemId: String) {
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
        attachments: String?
    ): Result<Unit> = runCatching {
        require(items.isNotEmpty()) { "Purchase order must contain at least 1 item" }
        require(exchangeRate > BigDecimal.ZERO) { "Exchange rate must be positive: $exchangeRate" }

        val existingOrder = purchaseOrderDao.getOrderById(orderId)
            ?: throw IllegalArgumentException("Purchase order not found: $orderId")

        var totalOriginalMinor = 0L
        var totalBdtMinor = 0L

        val poItems = items.map { draft ->
            require(draft.quantity > 0) { "Quantity must be greater than 0" }
            require(draft.unitPriceMinor > 0) { "Unit price must be greater than 0" }

            val lineOriginalMinor = draft.quantity.toLong() * draft.unitPriceMinor
            val lineBdtMinor = BigDecimal(lineOriginalMinor)
                .multiply(exchangeRate)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact()

            totalOriginalMinor += lineOriginalMinor
            totalBdtMinor += lineBdtMinor

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

        val totalCartonsSum = if (cartons.isNotEmpty()) cartons.size else poItems.sumOf { it.cartonQuantity }
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
                val cbm = if (it.lengthCm > BigDecimal.ZERO && it.widthCm > BigDecimal.ZERO && it.heightCm > BigDecimal.ZERO) {
                    CbmCalculator.calculateCbm(it.lengthCm, it.widthCm, it.heightCm, 1)
                } else BigDecimal.ZERO
                acc.add(cbm)
            }
        } else {
            poItems.fold(BigDecimal.ZERO) { acc, it -> acc.add(BigDecimal(it.cbm.ifBlank { "0" })) }
        }

        val updatedOrder = existingOrder.copy(
            supplierId = supplierId,
            status = status.name,
            currency = currency,
            exchangeRateToBdt = exchangeRate.toPlainString(),
            totalAmountMinor = totalOriginalMinor,
            totalBdtAmountMinor = totalBdtMinor,
            notes = notes,
            attachments = attachments ?: existingOrder.attachments,
            totalCartons = totalCartonsSum,
            totalGrossWeightKg = totalGrossWeightSum.toPlainString(),
            totalNetWeightKg = totalNetWeightSum.toPlainString(),
            totalCbm = totalCbmSum.toPlainString(),
            updatedAt = System.currentTimeMillis()
        )

        // Replace order items
        purchaseOrderDao.saveOrderWithItems(updatedOrder, poItems)

        // Sync batch shipment summary if exists
        val batchShipment = importShipmentDao.getBatchShipmentForOrder(orderId)
        if (batchShipment != null) {
            importShipmentDao.updateShipment(
                batchShipment.copy(
                    supplierId = supplierId,
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
        if (cartons.isNotEmpty()) {
            importShipmentDao.deleteCartonsForOrder(orderId)
            val shipmentId = batchShipment?.id ?: "shp-${UUID.randomUUID()}"
            cartons.forEach { ctn ->
                if (ctn.lines.isNotEmpty()) {
                    val lineInputs = ctn.lines.map {
                        MixedCartonLineInput(
                            productId = it.productId,
                            variantId = it.variantId,
                            productName = it.productName,
                            quantity = it.quantity
                        )
                    }
                    val allocated = CartonWeightAllocationEngine.allocateWeights(
                        cartonGrossWeightKg = ctn.grossWeightKg,
                        cartonNetWeightKg = ctn.netWeightKg,
                        lines = lineInputs
                    )
                    val cbm = if (ctn.lengthCm > BigDecimal.ZERO && ctn.widthCm > BigDecimal.ZERO && ctn.heightCm > BigDecimal.ZERO) {
                        CbmCalculator.calculateCbm(ctn.lengthCm, ctn.widthCm, ctn.heightCm, 1)
                    } else BigDecimal.ZERO

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
    }

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
    ) {
        val item = purchaseOrderDao.getItemById(itemId) ?: return
        val po = purchaseOrderDao.getOrderById(item.purchaseOrderId) ?: return
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

    override suspend fun deletePurchaseOrderItem(itemId: String) {
        val item = purchaseOrderDao.getItemById(itemId) ?: return
        val po = purchaseOrderDao.getOrderById(item.purchaseOrderId) ?: return
        purchaseOrderDao.deleteItemById(itemId)

        val allItems = purchaseOrderDao.getItemsForOrderSync(po.id)
        val newTotalMinor = allItems.sumOf { it.totalAmountMinor }
        val newTotalBdtMinor = allItems.sumOf { it.totalBdtAmountMinor }
        val newCartons = allItems.sumOf { it.cartonQuantity }
        val newGross = allItems.fold(BigDecimal.ZERO) { acc, itm -> acc.add(BigDecimal(itm.grossWeightKg.ifBlank { "0" })) }.toPlainString()

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
    ): String {
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
            e.printStackTrace()
        }

        return costId
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
        val poItemsByProduct = mutableMapOf<String, Pair<PurchaseOrder, PurchaseOrderItem>>()
        for (po in poById.values) {
            val items = purchaseOrderDao.getItemsForOrderSync(po.id)
            for (item in items) {
                poItemsByProduct[item.productId] = po to item
            }
        }

        // Aggregate receiving quantities from WarehouseReceipts for this shipment
        val receipts = warehouseReceiptDao.getReceiptsByShipmentSync(shipmentId)
            .filter { it.status != ReceiptStatus.CANCELLED.name }
        val receiptItemsByProduct = receipts
            .flatMap { warehouseReceiptDao.getItemsForReceiptSync(it.id) }
            .groupBy { it.productId }

        val cartonInputs = cartons.map { c ->
            val product = productDao.getProductById(c.productId)
            val resolvedCatId = c.categoryId ?: product?.categoryId ?: "cat-uncategorized"
            val resolvedSupplierId = c.supplierId ?: poItemsByProduct[c.productId]?.first?.supplierId ?: shipment.supplierId
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

        val productGroups = cartons.groupBy { it.productId }
        val productInputs = productGroups.map { (productId, groupCartons) ->
            val product = productDao.getProductById(productId)
            val firstCarton = groupCartons.first()
            val variantId = firstCarton.variantId
            val variant = if (variantId != null) productVariantDao.getVariantById(variantId) else null
            val packedQty = groupCartons.sumOf { it.quantity }

            val poPair = poItemsByProduct[productId]
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

            val recItems = receiptItemsByProduct[productId].orEmpty()
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
                    allAllocations.add(
                        LandedCostAllocation(
                            id = "lca-${UUID.randomUUID()}",
                            shipmentId = shipmentId,
                            costItemId = expenseId,
                            productId = res.itemId,
                            variantId = null,
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

            // Update any existing FIFO layers for this shipment with the finalized unit cost
            val existingLayers = inventoryCostLayerDao.getLayersForShipmentSync(shipmentId)
            for (layer in existingLayers) {
                val matchingSnapshot = snapshots.find { it.productId == layer.productId }
                if (matchingSnapshot != null) {
                    val unitPurchase = if (matchingSnapshot.packedQuantity > 0) {
                        matchingSnapshot.totalPurchaseCostBdtMinor / matchingSnapshot.packedQuantity
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
    ): LandingCostEngine.PostFinalizeAdjustmentResult {
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
                    e.printStackTrace()
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

        return adjResult
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
    ): String {
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
            e.printStackTrace()
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
        return voucherId
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
    ): String {
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
            e.printStackTrace()
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
        return voucherId
    }
}
