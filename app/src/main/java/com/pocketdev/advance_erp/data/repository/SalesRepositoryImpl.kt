package com.pocketdev.advance_erp.data.repository

import androidx.room.withTransaction
import com.pocketdev.advance_erp.core.database.AdvanceErpDatabase
import com.pocketdev.advance_erp.core.engine.DeterministicAllocation
import com.pocketdev.advance_erp.core.util.MovementNumberGenerator
import com.pocketdev.advance_erp.data.dao.CustomerDao
import com.pocketdev.advance_erp.data.dao.CustomerPaymentDao
import com.pocketdev.advance_erp.data.dao.InventoryCostLayerDao
import com.pocketdev.advance_erp.data.dao.ProductDao
import com.pocketdev.advance_erp.data.dao.SalesDao
import com.pocketdev.advance_erp.data.dao.SalesReturnDao
import com.pocketdev.advance_erp.data.dao.StockBalanceDao
import com.pocketdev.advance_erp.data.dao.StockMovementDao
import com.pocketdev.advance_erp.data.dao.WarehouseDao
import com.pocketdev.advance_erp.data.entity.AuditLog
import com.pocketdev.advance_erp.data.entity.Customer
import com.pocketdev.advance_erp.data.entity.CustomerPayment
import com.pocketdev.advance_erp.data.entity.FifoConsumption
import com.pocketdev.advance_erp.data.entity.InventoryCostLayer
import com.pocketdev.advance_erp.data.entity.InvoiceStatus
import com.pocketdev.advance_erp.data.entity.PaymentAllocation
import com.pocketdev.advance_erp.data.entity.ReturnRefundOption
import com.pocketdev.advance_erp.data.entity.SaleLayerAllocation
import com.pocketdev.advance_erp.data.entity.SalesCostLayerUsage
import com.pocketdev.advance_erp.data.entity.SalesInvoice
import com.pocketdev.advance_erp.data.entity.SalesInvoiceItem
import com.pocketdev.advance_erp.data.entity.SalesReturn
import com.pocketdev.advance_erp.data.entity.SalesReturnItem
import com.pocketdev.advance_erp.data.entity.SalesReturnLayerAllocation
import com.pocketdev.advance_erp.data.entity.StockBalance
import com.pocketdev.advance_erp.data.entity.StockMovement
import com.pocketdev.advance_erp.data.entity.StockMovementType
import com.pocketdev.advance_erp.domain.repository.CustomerLedgerEntry
import com.pocketdev.advance_erp.domain.repository.CustomerPaymentDraft
import com.pocketdev.advance_erp.domain.repository.CustomerSummary
import com.pocketdev.advance_erp.domain.repository.CustomerWiseSalesItem
import com.pocketdev.advance_erp.domain.repository.NewCustomerDraft
import com.pocketdev.advance_erp.domain.repository.ProductWiseSalesItem
import com.pocketdev.advance_erp.domain.repository.AccountingRepository
import com.pocketdev.advance_erp.domain.repository.JournalLineDraft
import com.pocketdev.advance_erp.domain.repository.SalesInvoiceDraft
import com.pocketdev.advance_erp.domain.repository.SalesReportSummary
import com.pocketdev.advance_erp.domain.repository.SalesRepository
import com.pocketdev.advance_erp.domain.repository.SalesReturnDraft
import com.pocketdev.advance_erp.domain.repository.DeliveryDispatchDraft
import com.pocketdev.advance_erp.domain.repository.DeliveryDispatchItemDraft
import com.pocketdev.advance_erp.domain.repository.DeliveryChallanInfo
import com.pocketdev.advance_erp.domain.repository.DeliveryChallanItemInfo
import com.pocketdev.advance_erp.domain.repository.UndeliveredItemInfo
import com.pocketdev.advance_erp.domain.repository.DirectExchangeDraft
import com.pocketdev.advance_erp.domain.repository.ReturnItemDraft
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SalesRepositoryImpl @Inject constructor(
    private val db: AdvanceErpDatabase,
    private val salesDao: SalesDao,
    private val customerPaymentDao: CustomerPaymentDao,
    private val salesReturnDao: SalesReturnDao,
    private val customerDao: CustomerDao,
    private val warehouseDao: WarehouseDao,
    private val productDao: ProductDao,
    private val stockBalanceDao: StockBalanceDao,
    private val stockMovementDao: StockMovementDao,
    private val inventoryCostLayerDao: InventoryCostLayerDao,
    private val accountingRepository: AccountingRepository,
    private val documentNumberService: com.pocketdev.advance_erp.core.util.DocumentNumberService
) : SalesRepository {

    private val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.US)

    private suspend fun generateInvoiceNumber(): String {
        return documentNumberService.getNextInvoiceNumber()
    }

    private suspend fun generatePaymentNumber(): String {
        return documentNumberService.getNextCustomerPaymentNumber()
    }

    private suspend fun generateReturnNumber(): String {
        return documentNumberService.getNextSalesReturnNumber()
    }

    private data class ConsumedLayerSlice(
        val layer: InventoryCostLayer?,
        val quantity: Int,
        val unitCostMinor: Long,
        val totalCogsMinor: Long
    )

    private fun resolveLayerProvenance(layer: InventoryCostLayer?): Triple<String?, String?, String?> {
        if (layer == null) return Triple(null, null, null)
        val isSynthetic = layer.layerSourceType in setOf("OPENING_STOCK", "MANUAL_ADJUSTMENT", "LEGACY_UNKNOWN") ||
            layer.shipmentId in setOf("ADJ", "RESTOCK", "INIT") ||
            (layer.shipmentId == "RET" && layer.purchaseOrderId == null)
        if (isSynthetic) return Triple(null, null, null)
        val poId = layer.purchaseOrderId
        val poItemId = layer.purchaseOrderItemId
        val shipmentId = layer.shipmentId.takeIf {
            it.isNotBlank() && it !in setOf("RET", "ADJ", "RESTOCK", "INIT") && !it.startsWith("TR-")
        }
        return Triple(poId, poItemId, shipmentId)
    }

    private suspend fun buildSaleLayerAllocationsForItem(
        invoice: SalesInvoice,
        item: SalesInvoiceItem,
        allocatedItemInvoiceDiscountMinor: Long,
        slices: List<ConsumedLayerSlice>,
        sourceType: String,
        referenceId: String?,
        now: Long
    ): List<SaleLayerAllocation> {
        if (slices.isEmpty()) return emptyList()
        val batchQty = slices.sumOf { it.quantity }
        if (batchQty <= 0) return emptyList()

        val totalItemGross = item.quantity.toLong() * item.unitPriceMinor
        val existingForItem = if (sourceType == "DELIVERY_DISPATCH") {
            db.importVaultDao().getSaleLayerAllocationsForInvoiceItemSync(item.id)
        } else {
            emptyList()
        }
        val prevQty = existingForItem.sumOf { it.quantity }
        val prevGross = existingForItem.sumOf { it.allocatedGrossRevenueMinor }
        val prevLineDisc = existingForItem.sumOf { it.allocatedLineDiscountMinor }
        val prevInvDisc = existingForItem.sumOf { it.allocatedInvoiceDiscountMinor }

        val remGross = maxOf(0L, totalItemGross - prevGross)
        val remLineDisc = maxOf(0L, item.discountMinor - prevLineDisc)
        val remInvDisc = maxOf(0L, allocatedItemInvoiceDiscountMinor - prevInvDisc)
        val futureQty = maxOf(0, item.quantity - prevQty - batchQty)

        val batchGross: Long
        val batchLineDisc: Long
        val batchInvDisc: Long
        if (futureQty == 0 && prevQty + batchQty >= item.quantity) {
            batchGross = remGross
            batchLineDisc = remLineDisc
            batchInvDisc = remInvDisc
        } else {
            val splitWeights = listOf(
                "THIS" to java.math.BigDecimal(batchQty),
                "FUTURE" to java.math.BigDecimal(futureQty)
            )
            batchGross = DeterministicAllocation.allocateProportionally(remGross, splitWeights)["THIS"] ?: 0L
            batchLineDisc = DeterministicAllocation.allocateProportionally(remLineDisc, splitWeights)["THIS"] ?: 0L
            batchInvDisc = DeterministicAllocation.allocateProportionally(remInvDisc, splitWeights)["THIS"] ?: 0L
        }

        val indexedSlices = slices.mapIndexed { idx, slice -> idx.toString() to slice }
        val sliceWeights = indexedSlices.map { (k, s) -> k to java.math.BigDecimal(s.quantity) }
        val grossBySlice = DeterministicAllocation.allocateProportionally(batchGross, sliceWeights)
        val lineDiscBySlice = DeterministicAllocation.allocateProportionally(batchLineDisc, sliceWeights)
        val invDiscBySlice = DeterministicAllocation.allocateProportionally(batchInvDisc, sliceWeights)

        return indexedSlices.map { (k, slice) ->
            val g = grossBySlice[k] ?: 0L
            val ld = lineDiscBySlice[k] ?: 0L
            val idisc = invDiscBySlice[k] ?: 0L
            val net = maxOf(0L, g - ld - idisc)
            val (poId, poItemId, shipId) = resolveLayerProvenance(slice.layer)
            val unattributed = (slice.layer == null) || (poId == null && shipId == null)
            SaleLayerAllocation(
                id = UUID.randomUUID().toString(),
                invoiceId = invoice.id,
                invoiceItemId = item.id,
                costLayerId = slice.layer?.id,
                productId = item.productId,
                variantId = item.variantId,
                warehouseId = invoice.warehouseId,
                purchaseOrderId = poId,
                purchaseOrderItemId = poItemId,
                shipmentId = shipId,
                quantity = slice.quantity,
                unitCostBdtMinor = slice.unitCostMinor,
                totalCogsBdtMinor = slice.totalCogsMinor,
                allocatedGrossRevenueMinor = g,
                allocatedLineDiscountMinor = ld,
                allocatedInvoiceDiscountMinor = idisc,
                allocatedNetRevenueMinor = net,
                isUnattributedFallback = unattributed,
                isReversed = false,
                sourceType = sourceType,
                referenceId = referenceId,
                createdAt = now
            )
        }
    }

    override suspend fun createInvoice(draft: SalesInvoiceDraft): String {
        val customer = if (draft.newCustomerDraft != null) {
            val normalizedPhone = draft.newCustomerDraft.phone.replace("[^0-9+]".toRegex(), "").trim()
            val existing = customerDao.getCustomerByPhone(normalizedPhone)
            if (existing != null) {
                existing
            } else {
                val customerCode = "CUST-" + String.format(Locale.US, "%06d", (System.currentTimeMillis() % 1000000))
                val newCust = Customer(
                    id = UUID.randomUUID().toString(),
                    customerCode = customerCode,
                    name = draft.newCustomerDraft.name.trim(),
                    phone = normalizedPhone,
                    email = draft.newCustomerDraft.email?.trim()?.takeIf { it.isNotBlank() },
                    address = draft.newCustomerDraft.address.trim(),
                    creditLimitMinor = 0L,
                    outstandingBalanceMinor = draft.newCustomerDraft.openingBalanceMinor,
                    profileImagePath = draft.newCustomerDraft.profileImagePath,
                    isActive = true,
                    createdAt = System.currentTimeMillis()
                )
                customerDao.insertCustomer(newCust)
                newCust
            }
        } else {
            customerDao.getCustomerById(draft.customerId)
                ?: throw IllegalArgumentException("Customer not found: ${draft.customerId}")
        }

        val warehouse = warehouseDao.getWarehouseById(draft.warehouseId)
            ?: throw IllegalArgumentException("Warehouse not found: ${draft.warehouseId}")

        val invoiceId = UUID.randomUUID().toString()
        val invoiceNumber = generateInvoiceNumber()

        var subtotalMinor = 0L
        val invoiceItems = draft.items.map { itemDraft ->
            val product = productDao.getProductById(itemDraft.productId)
                ?: throw IllegalArgumentException("Product not found: ${itemDraft.productId}")
            val variantSku = itemDraft.variantId?.let { db.productVariantDao().getVariantById(it)?.sku }

            val lineGross = itemDraft.quantity.toLong() * itemDraft.unitPriceMinor
            val lineTotal = maxOf(0L, lineGross - itemDraft.discountMinor)
            subtotalMinor += lineTotal

            SalesInvoiceItem(
                id = UUID.randomUUID().toString(),
                invoiceId = invoiceId,
                productId = itemDraft.productId,
                variantId = itemDraft.variantId,
                productNameEn = product.nameEn,
                productNameBn = product.nameBn,
                sku = variantSku ?: product.baseSku,
                quantity = itemDraft.quantity,
                returnedQuantity = 0,
                unitPriceMinor = itemDraft.unitPriceMinor,
                discountMinor = itemDraft.discountMinor,
                lineTotalMinor = lineTotal,
                unitCostMinor = 0L,
                totalCogsMinor = 0L
            )
        }

        val totalAmount = maxOf(0L, subtotalMinor - draft.discountMinor + draft.additionalChargesMinor)
        val initialPaid = minOf(draft.paidAmountMinor, totalAmount)
        val initialDue = maxOf(0L, totalAmount - initialPaid)

        val invoice = SalesInvoice(
            id = invoiceId,
            invoiceNumber = invoiceNumber,
            customerId = customer.id,
            customerName = customer.name,
            customerPhone = customer.phone,
            warehouseId = warehouse.id,
            warehouseName = warehouse.name,
            status = InvoiceStatus.DRAFT.name,
            subtotalMinor = subtotalMinor,
            discountMinor = draft.discountMinor,
            taxMinor = draft.additionalChargesMinor,
            totalAmountMinor = totalAmount,
            paidAmountMinor = initialPaid,
            dueAmountMinor = initialDue,
            totalCogsMinor = 0L,
            invoiceDate = draft.invoiceDate,
            dueDate = draft.dueDate,
            notes = draft.notes,
            isPosted = false,
            validUntilDate = draft.validUntilDate,
            scheduledDeliveryInfo = draft.scheduledDeliveryInfo?.trim()?.ifBlank { null }
        )

        salesDao.insertInvoice(invoice)
        salesDao.insertInvoiceItems(invoiceItems)
        return invoiceId
    }

    override suspend fun createAndPostInvoice(draft: SalesInvoiceDraft): Result<String> {
        return try {
            val invoiceId = db.withTransaction {
                val id = createInvoice(draft)
                val postResult = postInvoiceInternal(
                    invoiceId = id,
                    paymentMethod = draft.paymentMethod,
                    paymentAccountId = draft.paymentAccountId
                )
                if (postResult.isFailure) {
                    throw postResult.exceptionOrNull() ?: IllegalStateException("Failed to post invoice")
                }
                id
            }
            Result.success(invoiceId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateInvoice(invoiceId: String, draft: SalesInvoiceDraft) {
        val existing = salesDao.getInvoiceById(invoiceId)
            ?: throw IllegalArgumentException("Invoice not found: $invoiceId")
        if (existing.isPosted) {
            throw IllegalStateException("Cannot edit a posted invoice. Use sales return or adjustment instead.")
        }

        val customer = customerDao.getCustomerById(draft.customerId)
            ?: throw IllegalArgumentException("Customer not found")
        val warehouse = warehouseDao.getWarehouseById(draft.warehouseId)
            ?: throw IllegalArgumentException("Warehouse not found")

        var subtotalMinor = 0L
        val invoiceItems = draft.items.map { itemDraft ->
            val product = productDao.getProductById(itemDraft.productId)
                ?: throw IllegalArgumentException("Product not found")
            val variantSku = itemDraft.variantId?.let { db.productVariantDao().getVariantById(it)?.sku }
            val lineGross = itemDraft.quantity.toLong() * itemDraft.unitPriceMinor
            val lineTotal = maxOf(0L, lineGross - itemDraft.discountMinor)
            subtotalMinor += lineTotal

            SalesInvoiceItem(
                id = UUID.randomUUID().toString(),
                invoiceId = invoiceId,
                productId = itemDraft.productId,
                variantId = itemDraft.variantId,
                productNameEn = product.nameEn,
                productNameBn = product.nameBn,
                sku = variantSku ?: product.baseSku,
                quantity = itemDraft.quantity,
                returnedQuantity = 0,
                unitPriceMinor = itemDraft.unitPriceMinor,
                discountMinor = itemDraft.discountMinor,
                lineTotalMinor = lineTotal,
                unitCostMinor = 0L,
                totalCogsMinor = 0L
            )
        }

        val totalAmount = maxOf(0L, subtotalMinor - draft.discountMinor + draft.additionalChargesMinor)
        val initialPaid = minOf(draft.paidAmountMinor, totalAmount)
        val initialDue = maxOf(0L, totalAmount - initialPaid)

        val updated = existing.copy(
            customerId = customer.id,
            customerName = customer.name,
            customerPhone = customer.phone,
            warehouseId = warehouse.id,
            warehouseName = warehouse.name,
            subtotalMinor = subtotalMinor,
            discountMinor = draft.discountMinor,
            taxMinor = draft.additionalChargesMinor,
            totalAmountMinor = totalAmount,
            paidAmountMinor = if (draft.paidAmountMinor > 0L) initialPaid else existing.paidAmountMinor,
            dueAmountMinor = if (draft.paidAmountMinor > 0L) initialDue else maxOf(0L, totalAmount - existing.paidAmountMinor),
            invoiceDate = draft.invoiceDate,
            dueDate = draft.dueDate,
            notes = draft.notes,
            updatedAt = System.currentTimeMillis(),
            validUntilDate = draft.validUntilDate ?: existing.validUntilDate,
            scheduledDeliveryInfo = draft.scheduledDeliveryInfo?.trim()?.ifBlank { null } ?: existing.scheduledDeliveryInfo
        )

        salesDao.updateInvoice(updated)
        salesDao.insertInvoiceItems(invoiceItems)
    }

    override fun getAllInvoices(): Flow<List<SalesInvoice>> = salesDao.getAllInvoices()

    override suspend fun getInvoiceById(id: String): SalesInvoice? = salesDao.getInvoiceById(id)

    override suspend fun getInvoiceItems(invoiceId: String): List<SalesInvoiceItem> =
        salesDao.getItemsForInvoice(invoiceId)

    override fun getInvoiceItemsFlow(invoiceId: String): Flow<List<SalesInvoiceItem>> =
        salesDao.getItemsForInvoiceFlow(invoiceId)

    override suspend fun postInvoice(invoiceId: String): Result<Unit> =
        try {
            db.withTransaction { postInvoiceInternal(invoiceId, paymentMethod = "CASH", paymentAccountId = "coa-1010").getOrThrow() }
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Result.failure(e)
        }

    private suspend fun postInvoiceInternal(
        invoiceId: String,
        paymentMethod: String,
        paymentAccountId: String
    ): Result<Unit> {
        return try {
            val invoice = salesDao.getInvoiceById(invoiceId)
                ?: return Result.failure(IllegalArgumentException("Invoice not found"))

            if (invoice.isPosted) {
                return Result.failure(IllegalStateException("Invoice $invoiceId is already posted"))
            }

            val items = salesDao.getItemsForInvoice(invoiceId)
            if (items.isEmpty()) {
                return Result.failure(IllegalStateException("Invoice has no items"))
            }
            val customerBalanceBaseline = captureCustomerBalanceBaseline(invoice.customerId)

            // 1. Aggregate stock availability validation across repeated product/variant lines
            val requestedBySelection = items.groupBy { it.productId to it.variantId }
            for ((selection, groupItems) in requestedBySelection) {
                val (prodId, varId) = selection
                val balance = if (varId != null) {
                    stockBalanceDao.getVariantBalance(invoice.warehouseId, prodId, varId)
                        ?: stockBalanceDao.getBalance(invoice.warehouseId, prodId)
                } else {
                    stockBalanceDao.getBalance(invoice.warehouseId, prodId)
                }
                val sellableQty = maxOf(0, (balance?.quantity ?: 0) - (balance?.reservedQuantity ?: 0))
                val totalRequested = groupItems.sumOf { it.quantity }
                if (sellableQty < totalRequested) {
                    return Result.failure(
                        IllegalStateException(
                            "Insufficient sellable stock for '${groupItems.first().productNameEn}'. Available: $sellableQty, Requested: $totalRequested"
                        )
                    )
                }
            }

            // 2. Atomic Stock Reduction, COGS & Cost Layer Depletion
            val now = System.currentTimeMillis()
            val invoiceDiscountByItem = DeterministicAllocation.allocateInvoiceDiscountAcrossItems(
                invoiceDiscountMinor = invoice.discountMinor,
                items = items
            )
            var totalInvoiceCogs = 0L
            val costLayerUsages = mutableListOf<SalesCostLayerUsage>()
            val allFifoConsumptions = mutableListOf<FifoConsumption>()
            val allSaleLayerAllocations = mutableListOf<SaleLayerAllocation>()
            val updatedItems = mutableListOf<SalesInvoiceItem>()

            for (item in items) {
                // Deduct stock balance (variant-specific if variantId is present)
                val currentBalance = if (item.variantId != null) {
                    stockBalanceDao.getVariantBalance(invoice.warehouseId, item.productId, item.variantId)
                        ?: stockBalanceDao.getBalance(invoice.warehouseId, item.productId)
                } else {
                    stockBalanceDao.getBalance(invoice.warehouseId, item.productId)
                }
                val existingQty = currentBalance?.quantity ?: 0
                val newQty = existingQty - item.quantity
                if (currentBalance != null) {
                    stockBalanceDao.insertOrUpdate(
                        currentBalance.copy(
                            quantity = newQty,
                            updatedAt = now
                        )
                    )
                } else {
                    stockBalanceDao.insertOrUpdate(
                        StockBalance(
                            warehouseId = invoice.warehouseId,
                            productId = item.productId,
                            variantId = item.variantId,
                            quantity = newQty,
                            updatedAt = now
                        )
                    )
                }

                // Deplete active cost layers (FIFO by receipt date, strictly warehouse & variant isolated)
                var neededQty = item.quantity
                var itemTotalCogs = 0L
                val itemSlices = mutableListOf<ConsumedLayerSlice>()
                val activeLayers = if (item.variantId != null) {
                    val variantLayers = inventoryCostLayerDao.getActiveLayersForVariantAndWarehouseSync(
                        productId = item.productId,
                        variantId = item.variantId,
                        warehouseId = invoice.warehouseId
                    )
                    if (variantLayers.isNotEmpty()) variantLayers else {
                        inventoryCostLayerDao.getActiveLayersForProductAndWarehouseSync(
                            productId = item.productId,
                            warehouseId = invoice.warehouseId
                        )
                    }
                } else {
                    inventoryCostLayerDao.getActiveLayersForProductAndWarehouseSync(
                        productId = item.productId,
                        warehouseId = invoice.warehouseId
                    )
                }

                for (layer in activeLayers) {
                    if (neededQty <= 0) break
                    val qtyToTake = minOf(neededQty, layer.remainingQuantity)
                    if (qtyToTake <= 0) continue
                    val layerCost = qtyToTake.toLong() * layer.unitLandedCostBdtMinor
                    itemTotalCogs += layerCost
                    neededQty -= qtyToTake

                    val newRemaining = layer.remainingQuantity - qtyToTake
                    inventoryCostLayerDao.updateRemainingQuantity(
                        layerId = layer.id,
                        remainingQuantity = newRemaining,
                        isActive = newRemaining > 0
                    )

                    costLayerUsages.add(
                        SalesCostLayerUsage(
                            invoiceId = invoice.id,
                            invoiceItemId = item.id,
                            costLayerId = layer.id,
                            quantityDepleted = qtyToTake,
                            unitCostMinor = layer.unitLandedCostBdtMinor
                        )
                    )

                    allFifoConsumptions.add(
                        FifoConsumption(
                            invoiceId = invoice.id,
                            invoiceItemId = item.id,
                            costLayerId = layer.id,
                            productId = item.productId,
                            variantId = item.variantId,
                            warehouseId = invoice.warehouseId,
                            quantity = qtyToTake,
                            unitCostBdtMinor = layer.unitLandedCostBdtMinor,
                            totalCogsBdtMinor = layerCost,
                            referenceType = "SALE",
                            referenceId = invoice.id,
                            createdAt = now
                        )
                    )

                    itemSlices.add(
                        ConsumedLayerSlice(
                            layer = layer,
                            quantity = qtyToTake,
                            unitCostMinor = layer.unitLandedCostBdtMinor,
                            totalCogsMinor = layerCost
                        )
                    )
                }

                // If layers are exhausted (e.g. legacy/adjusted stock), fallback to product/variant purchase price & log unattributed slice
                if (neededQty > 0) {
                    val variant = item.variantId?.let { db.productVariantDao().getVariantById(it) }
                    val prod = productDao.getProductById(item.productId)
                    val fallbackUnitCost = variant?.variantCostMinor?.takeIf { it > 0L } ?: prod?.purchasePriceMinor ?: 0L
                    val fallbackCogs = neededQty.toLong() * fallbackUnitCost
                    itemTotalCogs += fallbackCogs
                    itemSlices.add(
                        ConsumedLayerSlice(
                            layer = null,
                            quantity = neededQty,
                            unitCostMinor = fallbackUnitCost,
                            totalCogsMinor = fallbackCogs
                        )
                    )
                }

                allSaleLayerAllocations.addAll(
                    buildSaleLayerAllocationsForItem(
                        invoice = invoice,
                        item = item,
                        allocatedItemInvoiceDiscountMinor = invoiceDiscountByItem[item.id] ?: 0L,
                        slices = itemSlices,
                        sourceType = "INVOICE_POST",
                        referenceId = invoice.id,
                        now = now
                    )
                )

                val unitCogs = if (item.quantity > 0) itemTotalCogs / item.quantity else 0L
                totalInvoiceCogs += itemTotalCogs

                // Stock Movement Audit with running balance, cost impact, and sales reference
                val movement = StockMovement(
                    id = MovementNumberGenerator.generateId("mov-sale"),
                    movementNumber = MovementNumberGenerator.generate("MOV-SALE"),
                    warehouseId = invoice.warehouseId,
                    productId = item.productId,
                    variantId = item.variantId,
                    movementType = StockMovementType.SALE_DISPATCH.code,
                    quantity = item.quantity,
                    unitCostMinor = unitCogs,
                    balanceAfterMovement = newQty,
                    totalCostImpactMinor = itemTotalCogs,
                    referenceType = "SALES_INVOICE",
                    referenceId = invoice.id,
                    salesInvoiceId = invoice.id,
                    notes = "Sales Invoice #${invoice.invoiceNumber}",
                    createdAt = now
                )
                stockMovementDao.insertMovement(movement)

                updatedItems.add(
                    item.copy(
                        unitCostMinor = unitCogs,
                        totalCogsMinor = itemTotalCogs
                    )
                )
            }

            salesDao.insertInvoiceItems(updatedItems)
            if (costLayerUsages.isNotEmpty()) {
                salesDao.insertCostLayerUsages(costLayerUsages)
            }
            if (allFifoConsumptions.isNotEmpty()) {
                db.fifoConsumptionDao().insertConsumptions(allFifoConsumptions)
            }
            if (allSaleLayerAllocations.isNotEmpty()) {
                db.importVaultDao().insertSaleLayerAllocations(allSaleLayerAllocations)
            }

            // Update Invoice as Posted
            val finalStatus = when {
                invoice.dueAmountMinor == 0L && invoice.totalAmountMinor > 0L -> InvoiceStatus.PAID.name
                invoice.paidAmountMinor > 0L -> InvoiceStatus.PARTIALLY_PAID.name
                else -> InvoiceStatus.POSTED.name
            }
            val postedInvoice = invoice.copy(
                isPosted = true,
                status = finalStatus,
                totalCogsMinor = totalInvoiceCogs,
                postedAt = now,
                updatedAt = now
            )
            salesDao.updateInvoice(postedInvoice)

            // If an initial cash payment was attached at draft creation, record customer payment & allocation
            val cashPaid = invoice.paidAmountMinor
            val arDue = invoice.dueAmountMinor

            if (cashPaid > 0L) {
                val resolvedMethod = paymentMethod.ifBlank { "CASH" }
                val resolvedAccountId = paymentAccountId.ifBlank { "coa-1010" }
                val paymentId = UUID.randomUUID().toString()
                val paymentNumber = generatePaymentNumber()
                val payment = CustomerPayment(
                    id = paymentId,
                    paymentNumber = paymentNumber,
                    customerId = invoice.customerId,
                    customerName = invoice.customerName,
                    amountMinor = cashPaid,
                    allocatedAmountMinor = cashPaid,
                    unallocatedCreditMinor = 0L,
                    paymentDate = now,
                    paymentMethod = resolvedMethod,
                    accountId = resolvedAccountId,
                    referenceNote = "Initial payment on Invoice #${invoice.invoiceNumber}",
                    isAdvance = false,
                    isPosted = true
                )
                customerPaymentDao.insertPayment(payment)
                val allocation = PaymentAllocation(
                    id = UUID.randomUUID().toString(),
                    paymentId = paymentId,
                    invoiceId = invoice.id,
                    invoiceNumber = invoice.invoiceNumber,
                    amountMinor = cashPaid,
                    allocatedAt = now
                )
                customerPaymentDao.insertAllocations(listOf(allocation))
            }

            // 3. Double-Entry Accounting Integration
            // Entry A: Sales Revenue, Accounts Receivable, Additional Charges, and Cash/Bank/MFS Received
            val revenueLines = mutableListOf<JournalLineDraft>()
            val grossCreditRevenue = invoice.subtotalMinor + invoice.taxMinor
            val effectiveDiscount = minOf(invoice.discountMinor, grossCreditRevenue)

            if (cashPaid > 0L) {
                revenueLines.add(
                    JournalLineDraft(
                        accountId = paymentAccountId.ifBlank { "coa-1010" },
                        isDebit = true,
                        amountMinor = cashPaid,
                        description = "Payment Received (${paymentMethod.ifBlank { "CASH" }}): Inv #${invoice.invoiceNumber}"
                    )
                )
            }
            if (arDue > 0L) {
                revenueLines.add(
                    JournalLineDraft(
                        accountId = "coa-1040",
                        isDebit = true,
                        amountMinor = arDue,
                        description = "Accounts Receivable: Inv #${invoice.invoiceNumber}"
                    )
                )
            }
            if (cashPaid == 0L && arDue == 0L && invoice.totalAmountMinor > 0L) {
                revenueLines.add(
                    JournalLineDraft(
                        accountId = "coa-1040",
                        isDebit = true,
                        amountMinor = invoice.totalAmountMinor,
                        description = "Accounts Receivable: Inv #${invoice.invoiceNumber}"
                    )
                )
            }
            if (effectiveDiscount > 0L) {
                revenueLines.add(
                    JournalLineDraft(
                        accountId = "coa-4030",
                        isDebit = true,
                        amountMinor = effectiveDiscount,
                        description = "Sales Discount: Inv #${invoice.invoiceNumber}"
                    )
                )
            }
            if (grossCreditRevenue > 0L) {
                revenueLines.add(
                    JournalLineDraft(
                        accountId = "coa-4010",
                        isDebit = false,
                        amountMinor = grossCreditRevenue,
                        description = "Sales Revenue: Inv #${invoice.invoiceNumber}"
                    )
                )
                accountingRepository.postJournalEntry(
                    entryDate = now,
                    descEn = "Sales Invoice #${invoice.invoiceNumber} posted for ${invoice.customerName}",
                    descBn = "গ্রাহক ${invoice.customerName} এর জন্য বিক্রয় চালান #${invoice.invoiceNumber} পোস্ট করা হয়েছে",
                    referenceType = "SALES_INVOICE",
                    referenceId = invoice.id,
                    lines = revenueLines
                )
            }

            // Entry B: COGS Recognition & Inventory Asset Deduction
            if (totalInvoiceCogs > 0) {
                val cogsLines = listOf(
                    JournalLineDraft(
                        accountId = "coa-5010",
                        isDebit = true,
                        amountMinor = totalInvoiceCogs,
                        description = "Cost of Goods Sold: Inv #${invoice.invoiceNumber}"
                    ),
                    JournalLineDraft(
                        accountId = "coa-1210",
                        isDebit = false,
                        amountMinor = totalInvoiceCogs,
                        description = "Inventory Asset deduction: Inv #${invoice.invoiceNumber}"
                    )
                )
                accountingRepository.postJournalEntry(
                    entryDate = now,
                    descEn = "COGS for Sales Invoice #${invoice.invoiceNumber}",
                    descBn = "বিক্রয় চালান #${invoice.invoiceNumber} এর বিক্রীত পণ্যের মোট ব্যয়",
                    referenceType = "SALES_COGS",
                    referenceId = invoice.id,
                    lines = cogsLines
                )
            }

            // 4. Update Customer Outstanding Balance preserving genuine opening balance
            updateCustomerBalanceFromBaseline(invoice.customerId, customerBalanceBaseline)
            val closing = getCustomerLedger(invoice.customerId).lastOrNull()?.runningBalanceMinor ?: 0L
            db.communicationDao().snapshot(com.pocketdev.advance_erp.data.entity.CommunicationSnapshot(
                module = "INVOICE", documentId = invoice.id, number = invoice.invoiceNumber,
                partyId = invoice.customerId, createdAt = now, originalAmountMinor = invoice.totalAmountMinor,
                previousBalanceMinor = closing - invoice.totalAmountMinor + cashPaid,
                allocatedPaymentMinor = cashPaid, invoiceDueMinor = arDue, closingBalanceMinor = closing
            ))

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun recordCustomerPayment(draft: CustomerPaymentDraft): String {
        require(draft.amountMinor > 0) { "Payment amount must be positive" }
        val paymentNumber = generatePaymentNumber()
        return db.withTransaction {
            val customer = customerDao.getCustomerById(draft.customerId)
                ?: throw IllegalArgumentException("Customer not found: ${draft.customerId}")

            val netBefore = computeCustomerTransactionNet(draft.customerId)
            val allocationInvoices = draft.allocations.associate { allocation ->
                allocation.invoiceId to requireNotNull(salesDao.getInvoiceById(allocation.invoiceId)) { "Invoice no longer exists" }
            }
            com.pocketdev.advance_erp.core.engine.PaymentAllocationValidation.validate(draft, allocationInvoices)
            val paymentId = UUID.randomUUID().toString()
            val allocations = mutableListOf<PaymentAllocation>()
            val affectedInvoiceIds = linkedSetOf<String>()

            var totalAllocated = 0L

            if (draft.allocations.isNotEmpty()) {
                for (allocDraft in draft.allocations) {
                    val inv = salesDao.getInvoiceById(allocDraft.invoiceId) ?: continue
                    val maxAlloc = minOf(allocDraft.amountMinor, inv.dueAmountMinor)
                    if (maxAlloc > 0) {
                        allocations.add(
                            PaymentAllocation(
                                id = UUID.randomUUID().toString(),
                                paymentId = paymentId,
                                invoiceId = inv.id,
                                invoiceNumber = inv.invoiceNumber,
                                amountMinor = maxAlloc,
                                allocatedAt = draft.paymentDate
                            )
                        )
                        affectedInvoiceIds.add(inv.id)
                        totalAllocated += maxAlloc
                    }
                }
            } else {
                // Confirmed Business Rule 4.2: Customer payments must NOT automatically be distributed
                // across old invoice dues via FIFO. Standalone payments are recorded as customer ledger credits.
                totalAllocated = 0L
            }

            val prePaymentReceivableMinor = maxOf(
                0L,
                if (customer.outstandingBalanceMinor != 0L) customer.outstandingBalanceMinor else netBefore
            )
            val (arSettledMinor, advanceCreditMinor) = com.pocketdev.advance_erp.core.engine.PaymentAllocationValidation
                .splitPaymentIntoReceivableAndAdvance(draft.amountMinor, totalAllocated, prePaymentReceivableMinor)
            val isAdvance = advanceCreditMinor > 0L && arSettledMinor == 0L

            val payment = CustomerPayment(
                id = paymentId,
                paymentNumber = paymentNumber,
                customerId = customer.id,
                customerName = customer.name,
                amountMinor = draft.amountMinor,
                allocatedAmountMinor = arSettledMinor,
                unallocatedCreditMinor = advanceCreditMinor,
                paymentDate = draft.paymentDate,
                paymentMethod = draft.paymentMethod,
                accountId = draft.accountId,
                referenceNote = draft.referenceNote,
                isAdvance = isAdvance,
                isPosted = true
            )

            customerPaymentDao.insertPayment(payment)
            if (allocations.isNotEmpty()) {
                customerPaymentDao.insertAllocations(allocations)
            }
            for (affectedId in affectedInvoiceIds) {
                recalculateAndPersistInvoiceSettlement(affectedId, draft.paymentDate)
            }

            // Double-Entry Accounting
            val journalLines = mutableListOf<JournalLineDraft>()
            // Debit: Cash/Bank
            journalLines.add(
                JournalLineDraft(
                    accountId = draft.accountId,
                    isDebit = true,
                    amountMinor = draft.amountMinor,
                    description = "Customer payment received: Receipt #${paymentNumber}"
                )
            )
            // Credit: Accounts Receivable for invoiced + prior/opening receivable settlement
            if (arSettledMinor > 0L) {
                journalLines.add(
                    JournalLineDraft(
                        accountId = "coa-1040",
                        isDebit = false,
                        amountMinor = arSettledMinor,
                        description = if (totalAllocated > 0L) {
                            "Payment allocated to invoice(s): Receipt #${paymentNumber}"
                        } else {
                            "Customer receivable collection: Receipt #${paymentNumber}"
                        }
                    )
                )
            }
            // Credit: Customer Advance for overpayment beyond current receivable
            if (advanceCreditMinor > 0L) {
                journalLines.add(
                    JournalLineDraft(
                        accountId = "coa-2030",
                        isDebit = false,
                        amountMinor = advanceCreditMinor,
                        description = "Customer advance deposit: Receipt #${paymentNumber}"
                    )
                )
            }

            accountingRepository.postJournalEntry(
                entryDate = draft.paymentDate,
                descEn = "Customer payment of ৳${draft.amountMinor / 100} from ${customer.name}",
                descBn = "গ্রাহক ${customer.name} এর কাছ থেকে ৳${draft.amountMinor / 100} পেমেন্ট গ্রহণ",
                referenceType = "CUSTOMER_PAYMENT",
                referenceId = paymentId,
                lines = journalLines
            )

            customerDao.updateOutstandingBalance(draft.customerId,
                com.pocketdev.advance_erp.core.engine.PaymentAllocationValidation.outstandingAfterPayment(customer.outstandingBalanceMinor, netBefore, draft.amountMinor))
            paymentNumber
        }
    }

    override suspend fun previewNextCustomerPaymentNumber(): String {
        return documentNumberService.previewNextCustomerPaymentNumber()
    }

    override fun getAllPayments(): Flow<List<CustomerPayment>> = customerPaymentDao.getAllPayments()

    override fun getPaymentsForCustomer(customerId: String): Flow<List<CustomerPayment>> =
        customerPaymentDao.getPaymentsForCustomer(customerId)

    override fun getAllocationsForInvoice(invoiceId: String): Flow<List<PaymentAllocation>> =
        customerPaymentDao.getAllocationsForInvoice(invoiceId)

    override suspend fun getAllocationsForInvoiceSync(invoiceId: String): List<PaymentAllocation> =
        customerPaymentDao.getAllocationsForInvoiceSync(invoiceId)

    override suspend fun processSalesReturn(draft: SalesReturnDraft): String {
        return db.withTransaction {
            val invoice = salesDao.getInvoiceById(draft.invoiceId)
                ?: throw IllegalArgumentException("Invoice not found: ${draft.invoiceId}")
            if (!invoice.isPosted) {
                throw IllegalStateException("Cannot return items on an unposted invoice")
            }
            if (invoice.status == InvoiceStatus.CANCELLED.name) {
                throw IllegalStateException("Cannot process sales return on a cancelled invoice")
            }
            val customerBalanceBaseline = captureCustomerBalanceBaseline(invoice.customerId)

            val returnId = UUID.randomUUID().toString()
            val returnNumber = generateReturnNumber()

            var totalReturnAmount = 0L
            var totalSoundCogsReversal = 0L
            var totalDamageLoss = 0L
            val returnItems = mutableListOf<SalesReturnItem>()
            val allReturnLayerAllocs = mutableListOf<SalesReturnLayerAllocation>()
            val existingItemsList = salesDao.getItemsForInvoice(draft.invoiceId)
            val existingItems = existingItemsList.associateBy { it.id }
            val invoiceDiscountByItem = DeterministicAllocation.allocateInvoiceDiscountAcrossItems(
                invoiceDiscountMinor = invoice.discountMinor,
                items = existingItemsList
            )

            val allSaleAllocationsForInv = db.importVaultDao().getSaleLayerAllocationsForInvoiceSync(invoice.id)
                .filter { !it.isReversed && it.invoiceItemId != null }
            val existingReturnAllocsForInv = db.importVaultDao().getReturnLayerAllocationsForInvoiceSync(invoice.id)
            val returnedQtyBySaleAllocId = existingReturnAllocsForInv
                .filter { it.saleLayerAllocationId != null }
                .groupBy { it.saleLayerAllocationId!! }
                .mapValues { (_, list) -> list.sumOf { it.soundQuantity + it.damagedQuantity } }
                .toMutableMap()
            val returnedNetRevBySaleAllocId = existingReturnAllocsForInv
                .filter { it.saleLayerAllocationId != null }
                .groupBy { it.saleLayerAllocationId!! }
                .mapValues { (_, list) -> list.sumOf { it.reversedNetRevenueMinor } }
                .toMutableMap()
            val returnedGrossRevBySaleAllocId = existingReturnAllocsForInv
                .filter { it.saleLayerAllocationId != null }
                .groupBy { it.saleLayerAllocationId!! }
                .mapValues { (_, list) -> list.sumOf { it.reversedGrossRevenueMinor } }
                .toMutableMap()
            val returnedDiscBySaleAllocId = existingReturnAllocsForInv
                .filter { it.saleLayerAllocationId != null }
                .groupBy { it.saleLayerAllocationId!! }
                .mapValues { (_, list) -> list.sumOf { it.reversedDiscountMinor } }
                .toMutableMap()

            val existingReturnsForInv = salesReturnDao.getReturnsForCustomerSync(invoice.customerId)
                .filter { it.isPosted && it.invoiceId == invoice.id }
                .flatMap { salesReturnDao.getItemsForReturn(it.id) }
            val layerCache = mutableMapOf<String, InventoryCostLayer?>()

            suspend fun loadLayerCached(layerId: String?): InventoryCostLayer? {
                if (layerId == null) return null
                return layerCache.getOrPut(layerId) { inventoryCostLayerDao.getCostLayerById(layerId) }
            }

            for (itemDraft in draft.items) {
                val invItem = existingItems[itemDraft.invoiceItemId]
                    ?: throw IllegalArgumentException("Invoice item not found: ${itemDraft.invoiceItemId}")

                val maxReturnable = invItem.quantity - invItem.returnedQuantity
                if (itemDraft.returnQuantity <= 0 || itemDraft.returnQuantity > maxReturnable) {
                    throw IllegalStateException(
                        "Invalid return quantity for '${invItem.productNameEn}'. Requested: ${itemDraft.returnQuantity}, Max returnable: $maxReturnable"
                    )
                }

                val damagedQty = if (itemDraft.isDamaged) itemDraft.returnQuantity else minOf(itemDraft.damagedQuantity, itemDraft.returnQuantity)
                val soundQty = itemDraft.returnQuantity - damagedQty
                val returnItemId = UUID.randomUUID().toString()

                val itemSaleAllocs = allSaleAllocationsForInv.filter { it.invoiceItemId == invItem.id }
                var lineReturnAmount = 0L
                var lineSoundCogs = 0L
                var lineDamageCogs = 0L

                if (itemSaleAllocs.isNotEmpty()) {
                    // Preload layer provenance so targetCostLayerId can match both direct and origin layer IDs
                    for (alloc in itemSaleAllocs) {
                        loadLayerCached(alloc.costLayerId)
                    }
                    val orderedAllocs = if (itemDraft.targetCostLayerId != null) {
                        val target = itemDraft.targetCostLayerId
                        itemSaleAllocs.sortedWith(
                            compareByDescending<SaleLayerAllocation> { it.costLayerId == target }
                                .thenByDescending { layerCache[it.costLayerId]?.originCostLayerId == target }
                                .thenByDescending { it.createdAt }
                                .thenByDescending { it.id }
                        )
                    } else {
                        itemSaleAllocs.sortedWith(
                            compareByDescending<SaleLayerAllocation> { it.createdAt }
                                .thenByDescending { it.id }
                        )
                    }

                    data class ReturnSlicePlan(
                        val alloc: SaleLayerAllocation,
                        val takeQty: Int
                    )
                    val slicePlans = mutableListOf<ReturnSlicePlan>()
                    var qtyLeftToReturn = itemDraft.returnQuantity
                    for (alloc in orderedAllocs) {
                        if (qtyLeftToReturn <= 0) break
                        val alreadyRet = returnedQtyBySaleAllocId[alloc.id] ?: 0
                        val available = (alloc.quantity - alreadyRet).coerceAtLeast(0)
                        if (available <= 0) continue
                        val take = minOf(qtyLeftToReturn, available)
                        slicePlans.add(ReturnSlicePlan(alloc, take))
                        qtyLeftToReturn -= take
                    }

                    // Split damagedQty across slicePlans deterministically without exceeding each slice's takeQty
                    val damagedBySlice = mutableMapOf<Int, Int>()
                    var remDmgToAssign = damagedQty
                    slicePlans.forEachIndexed { idx, plan ->
                        if (remDmgToAssign > 0) {
                            val d = minOf(plan.takeQty, remDmgToAssign)
                            damagedBySlice[idx] = d
                            remDmgToAssign -= d
                        } else {
                            damagedBySlice[idx] = 0
                        }
                    }

                    slicePlans.forEachIndexed { idx, plan ->
                        val alloc = plan.alloc
                        val take = plan.takeQty
                        val takeDamaged = damagedBySlice[idx] ?: 0
                        val takeSound = take - takeDamaged

                        val prevRetQty = returnedQtyBySaleAllocId[alloc.id] ?: 0
                        val remainingAllocQtyAfter = maxOf(0, alloc.quantity - prevRetQty - take)
                        val prevNet = returnedNetRevBySaleAllocId[alloc.id] ?: 0L
                        val prevGross = returnedGrossRevBySaleAllocId[alloc.id] ?: 0L
                        val prevDisc = returnedDiscBySaleAllocId[alloc.id] ?: 0L
                        val remNet = maxOf(0L, alloc.allocatedNetRevenueMinor - prevNet)
                        val remGross = maxOf(0L, alloc.allocatedGrossRevenueMinor - prevGross)
                        val remDisc = maxOf(0L, (alloc.allocatedLineDiscountMinor + alloc.allocatedInvoiceDiscountMinor) - prevDisc)

                        val revWeights = listOf(
                            "RET" to java.math.BigDecimal(take),
                            "KEEP" to java.math.BigDecimal(remainingAllocQtyAfter)
                        )
                        val revNet = if (remainingAllocQtyAfter == 0) remNet else DeterministicAllocation.allocateProportionally(remNet, revWeights)["RET"] ?: 0L
                        val revGross = if (remainingAllocQtyAfter == 0) remGross else DeterministicAllocation.allocateProportionally(remGross, revWeights)["RET"] ?: 0L
                        val revDisc = if (remainingAllocQtyAfter == 0) remDisc else DeterministicAllocation.allocateProportionally(remDisc, revWeights)["RET"] ?: 0L

                        returnedQtyBySaleAllocId[alloc.id] = prevRetQty + take
                        returnedNetRevBySaleAllocId[alloc.id] = prevNet + revNet
                        returnedGrossRevBySaleAllocId[alloc.id] = prevGross + revGross
                        returnedDiscBySaleAllocId[alloc.id] = prevDisc + revDisc

                        val sliceSoundCogs = takeSound.toLong() * alloc.unitCostBdtMinor
                        val sliceDmgCogs = takeDamaged.toLong() * alloc.unitCostBdtMinor

                        lineReturnAmount += revNet
                        lineSoundCogs += sliceSoundCogs
                        lineDamageCogs += sliceDmgCogs

                        val origLayer = loadLayerCached(alloc.costLayerId)
                        val restoredLayerId: String? = if (takeSound > 0) "icl-${UUID.randomUUID()}" else null
                        if (restoredLayerId != null && takeSound > 0) {
                            val origReceiptDate = origLayer?.originReceiptDate ?: origLayer?.receiptDate ?: draft.returnDate
                            inventoryCostLayerDao.insertCostLayer(
                                InventoryCostLayer(
                                    id = restoredLayerId,
                                    productId = itemDraft.productId,
                                    variantId = itemDraft.variantId,
                                    warehouseId = draft.warehouseId,
                                    shipmentId = origLayer?.shipmentId ?: alloc.shipmentId ?: "RET",
                                    shipmentNumber = origLayer?.shipmentNumber ?: returnNumber,
                                    warehouseReceiptId = returnId,
                                    receiptNumber = returnNumber,
                                    initialQuantity = takeSound,
                                    remainingQuantity = takeSound,
                                    unitLandedCostBdtMinor = alloc.unitCostBdtMinor,
                                    originalCurrency = origLayer?.originalCurrency ?: "BDT",
                                    originalPurchasePriceMinor = origLayer?.originalPurchasePriceMinor ?: alloc.unitCostBdtMinor,
                                    exchangeRateToBdt = origLayer?.exchangeRateToBdt ?: "1.0",
                                    allocatedExpensePerUnitBdtMinor = origLayer?.allocatedExpensePerUnitBdtMinor ?: 0L,
                                    receiptDate = origReceiptDate,
                                    isActive = true,
                                    purchaseOrderId = origLayer?.purchaseOrderId ?: alloc.purchaseOrderId,
                                    purchaseOrderItemId = origLayer?.purchaseOrderItemId ?: alloc.purchaseOrderItemId,
                                    purchaseBatchId = origLayer?.purchaseBatchId,
                                    originCostLayerId = origLayer?.originCostLayerId ?: origLayer?.id ?: alloc.costLayerId,
                                    originReceiptDate = origReceiptDate,
                                    layerSourceType = "SALES_RETURN_RESTORE",
                                    createdAt = System.currentTimeMillis()
                                )
                            )
                        }

                        allReturnLayerAllocs.add(
                            SalesReturnLayerAllocation(
                                id = UUID.randomUUID().toString(),
                                returnId = returnId,
                                returnItemId = returnItemId,
                                invoiceId = invoice.id,
                                invoiceItemId = invItem.id,
                                saleLayerAllocationId = alloc.id,
                                costLayerId = alloc.costLayerId,
                                restoredCostLayerId = restoredLayerId,
                                productId = itemDraft.productId,
                                variantId = itemDraft.variantId,
                                purchaseOrderId = origLayer?.purchaseOrderId ?: alloc.purchaseOrderId,
                                purchaseOrderItemId = origLayer?.purchaseOrderItemId ?: alloc.purchaseOrderItemId,
                                shipmentId = origLayer?.shipmentId ?: alloc.shipmentId,
                                soundQuantity = takeSound,
                                damagedQuantity = takeDamaged,
                                reversedGrossRevenueMinor = revGross,
                                reversedDiscountMinor = revDisc,
                                reversedNetRevenueMinor = revNet,
                                reversedCogsMinor = sliceSoundCogs,
                                damageLossMinor = sliceDmgCogs,
                                isUnattributed = alloc.isUnattributedFallback,
                                createdAt = draft.returnDate
                            )
                        )
                    }
                } else {
                    // Legacy pre-v14 invoice fallback: discount-adjusted net return amount, no fabricated PO provenance
                    val invDisc = invoiceDiscountByItem[invItem.id] ?: 0L
                    val itemNetTotal = maxOf(0L, DeterministicAllocation.eligibleLineNetValue(invItem) - invDisc)
                    val prevReturnedAmountForItem = existingReturnsForInv.filter { it.invoiceItemId == invItem.id }.sumOf { it.returnTotalMinor }
                    val remainingItemNet = maxOf(0L, itemNetTotal - prevReturnedAmountForItem)
                    val keepQtyAfter = maxOf(0, maxReturnable - itemDraft.returnQuantity)
                    lineReturnAmount = if (keepQtyAfter == 0) remainingItemNet else {
                        DeterministicAllocation.allocateProportionally(
                            remainingItemNet,
                            listOf("RET" to java.math.BigDecimal(itemDraft.returnQuantity), "KEEP" to java.math.BigDecimal(keepQtyAfter))
                        )["RET"] ?: 0L
                    }
                    lineSoundCogs = soundQty.toLong() * invItem.unitCostMinor
                    lineDamageCogs = damagedQty.toLong() * invItem.unitCostMinor

                    val restoredLayerId: String? = if (soundQty > 0) "icl-${UUID.randomUUID()}" else null
                    if (restoredLayerId != null && soundQty > 0) {
                        inventoryCostLayerDao.insertCostLayer(
                            InventoryCostLayer(
                                id = restoredLayerId,
                                productId = itemDraft.productId,
                                variantId = itemDraft.variantId,
                                warehouseId = draft.warehouseId,
                                shipmentId = "RET",
                                shipmentNumber = returnNumber,
                                warehouseReceiptId = returnId,
                                receiptNumber = returnNumber,
                                initialQuantity = soundQty,
                                remainingQuantity = soundQty,
                                unitLandedCostBdtMinor = invItem.unitCostMinor,
                                originalCurrency = "BDT",
                                originalPurchasePriceMinor = invItem.unitCostMinor,
                                exchangeRateToBdt = "1.0",
                                allocatedExpensePerUnitBdtMinor = 0L,
                                receiptDate = draft.returnDate,
                                isActive = true,
                                originReceiptDate = draft.returnDate,
                                layerSourceType = "SALES_RETURN_RESTORE",
                                createdAt = System.currentTimeMillis()
                            )
                        )
                    }
                    val grossRev = itemDraft.returnQuantity.toLong() * invItem.unitPriceMinor
                    allReturnLayerAllocs.add(
                        SalesReturnLayerAllocation(
                            id = UUID.randomUUID().toString(),
                            returnId = returnId,
                            returnItemId = returnItemId,
                            invoiceId = invoice.id,
                            invoiceItemId = invItem.id,
                            saleLayerAllocationId = null,
                            costLayerId = null,
                            restoredCostLayerId = restoredLayerId,
                            productId = itemDraft.productId,
                            variantId = itemDraft.variantId,
                            purchaseOrderId = null,
                            purchaseOrderItemId = null,
                            shipmentId = null,
                            soundQuantity = soundQty,
                            damagedQuantity = damagedQty,
                            reversedGrossRevenueMinor = grossRev,
                            reversedDiscountMinor = maxOf(0L, grossRev - lineReturnAmount),
                            reversedNetRevenueMinor = lineReturnAmount,
                            reversedCogsMinor = lineSoundCogs,
                            damageLossMinor = lineDamageCogs,
                            isUnattributed = true,
                            createdAt = draft.returnDate
                        )
                    )
                }

                totalReturnAmount += lineReturnAmount
                totalSoundCogsReversal += lineSoundCogs
                totalDamageLoss += lineDamageCogs
                val lineCogsReversal = lineSoundCogs + lineDamageCogs
                val effectiveReturnUnitCost = if (itemDraft.returnQuantity > 0) lineCogsReversal / itemDraft.returnQuantity else invItem.unitCostMinor

                // Only sound returned goods restore sellable warehouse stock
                if (soundQty > 0) {
                    val balance = if (itemDraft.variantId != null) {
                        stockBalanceDao.getVariantBalance(draft.warehouseId, itemDraft.productId, itemDraft.variantId)
                            ?: stockBalanceDao.getBalance(draft.warehouseId, itemDraft.productId)
                    } else {
                        stockBalanceDao.getBalance(draft.warehouseId, itemDraft.productId)
                    }
                    val newQty = (balance?.quantity ?: 0) + soundQty
                    if (balance != null) {
                        stockBalanceDao.insertOrUpdate(
                            balance.copy(quantity = newQty, updatedAt = System.currentTimeMillis())
                        )
                    } else {
                        stockBalanceDao.insertOrUpdate(
                            StockBalance(
                                warehouseId = draft.warehouseId,
                                productId = itemDraft.productId,
                                variantId = itemDraft.variantId,
                                quantity = newQty
                            )
                        )
                    }

                    val soundUnitCost = if (soundQty > 0) lineSoundCogs / soundQty else invItem.unitCostMinor
                    val movement = StockMovement(
                        id = MovementNumberGenerator.generateId("mov-ret"),
                        movementNumber = MovementNumberGenerator.generate("MOV-RET"),
                        warehouseId = draft.warehouseId,
                        productId = itemDraft.productId,
                        variantId = itemDraft.variantId,
                        movementType = StockMovementType.SALE_RETURN.code,
                        quantity = soundQty,
                        unitCostMinor = soundUnitCost,
                        balanceAfterMovement = newQty,
                        totalCostImpactMinor = lineSoundCogs,
                        referenceType = "SALES_RETURN",
                        referenceId = returnId,
                        salesInvoiceId = invoice.id,
                        notes = "Sales Return #${returnNumber} (Sound goods) for Invoice #${invoice.invoiceNumber}",
                        createdAt = System.currentTimeMillis()
                    )
                    stockMovementDao.insertMovement(movement)
                }

                // Damaged returned goods are excluded from sellable stock and recorded as write-off
                if (damagedQty > 0) {
                    val currentBal = (if (itemDraft.variantId != null) {
                        stockBalanceDao.getVariantBalance(draft.warehouseId, itemDraft.productId, itemDraft.variantId)
                            ?: stockBalanceDao.getBalance(draft.warehouseId, itemDraft.productId)
                    } else {
                        stockBalanceDao.getBalance(draft.warehouseId, itemDraft.productId)
                    })?.quantity ?: 0
                    val dmgUnitCost = lineDamageCogs / damagedQty
                    val dmgMovement = StockMovement(
                        id = MovementNumberGenerator.generateId("mov-dmg"),
                        movementNumber = MovementNumberGenerator.generate("MOV-DMG"),
                        warehouseId = draft.warehouseId,
                        productId = itemDraft.productId,
                        variantId = itemDraft.variantId,
                        movementType = StockMovementType.DAMAGE_WRITE_OFF.code,
                        quantity = damagedQty,
                        unitCostMinor = dmgUnitCost,
                        balanceAfterMovement = currentBal,
                        totalCostImpactMinor = lineDamageCogs,
                        referenceType = "SALES_RETURN",
                        referenceId = returnId,
                        salesInvoiceId = invoice.id,
                        notes = "Damaged returned goods written off from Invoice #${invoice.invoiceNumber}. Excluded from sellable stock.",
                        createdAt = System.currentTimeMillis()
                    )
                    stockMovementDao.insertMovement(dmgMovement)
                }

                // Update item returned quantity
                salesDao.recordItemReturn(invItem.id, itemDraft.returnQuantity)

                returnItems.add(
                    SalesReturnItem(
                        id = returnItemId,
                        returnId = returnId,
                        invoiceItemId = invItem.id,
                        productId = itemDraft.productId,
                        variantId = itemDraft.variantId,
                        productNameEn = invItem.productNameEn,
                        productNameBn = invItem.productNameBn,
                        returnQuantity = itemDraft.returnQuantity,
                        unitPriceMinor = if (itemDraft.returnQuantity > 0) lineReturnAmount / itemDraft.returnQuantity else invItem.unitPriceMinor,
                        returnTotalMinor = lineReturnAmount,
                        originalUnitCostMinor = effectiveReturnUnitCost,
                        cogsReversalMinor = lineCogsReversal
                    )
                )
            }

            val salesReturn = SalesReturn(
                id = returnId,
                returnNumber = returnNumber,
                invoiceId = invoice.id,
                invoiceNumber = invoice.invoiceNumber,
                customerId = invoice.customerId,
                customerName = invoice.customerName,
                warehouseId = draft.warehouseId,
                totalReturnAmountMinor = totalReturnAmount,
                totalCogsReversalMinor = totalSoundCogsReversal + totalDamageLoss,
                refundOption = draft.refundOption.name,
                reason = draft.reason,
                returnDate = draft.returnDate,
                isPosted = true
            )

            salesReturnDao.insertReturn(salesReturn)
            salesReturnDao.insertReturnItems(returnItems)
            if (allReturnLayerAllocs.isNotEmpty()) {
                db.importVaultDao().insertSalesReturnLayerAllocations(allReturnLayerAllocs)
            }

            // Adjust Invoice Due Amount authoritatively when Customer Credit was chosen
            if (draft.refundOption == ReturnRefundOption.CUSTOMER_CREDIT) {
                recalculateAndPersistInvoiceSettlement(invoice.id, draft.returnDate)
            }

            // Double-Entry Accounting
            // 1. Sales Reversal
            val creditAccount = when (draft.refundOption) {
                ReturnRefundOption.CUSTOMER_CREDIT -> "coa-1040" // Accounts Receivable
                ReturnRefundOption.CASH_REFUND -> "coa-1010"     // Cash in Hand
                ReturnRefundOption.BANK_REFUND -> "coa-1020"     // Bank
            }
            if (draft.refundOption in setOf(ReturnRefundOption.CASH_REFUND, ReturnRefundOption.BANK_REFUND) &&
                totalReturnAmount > 0L
            ) {
                accountingRepository.requireSufficientLiquidBalance(creditAccount, totalReturnAmount)
            }
            val returnRevenueLines = listOf(
                JournalLineDraft(
                    accountId = "coa-4020",
                    isDebit = true,
                    amountMinor = totalReturnAmount,
                    description = "Sales Return #${returnNumber} on Invoice #${invoice.invoiceNumber}"
                ),
                JournalLineDraft(
                    accountId = creditAccount,
                    isDebit = false,
                    amountMinor = totalReturnAmount,
                    description = "Refund/Credit for Return #${returnNumber}"
                )
            )
            accountingRepository.postJournalEntry(
                entryDate = draft.returnDate,
                descEn = "Sales Return #${returnNumber} for ${invoice.customerName}",
                descBn = "গ্রাহক ${invoice.customerName} এর জন্য বিক্রয় ফেরত #${returnNumber}",
                referenceType = "SALES_RETURN",
                referenceId = returnId,
                lines = returnRevenueLines
            )

            // 2. COGS Reversal & Inventory Restoration / Damage Write-Off
            if (totalSoundCogsReversal > 0) {
                val cogsReversalLines = listOf(
                    JournalLineDraft(
                        accountId = "coa-1210",
                        isDebit = true,
                        amountMinor = totalSoundCogsReversal,
                        description = "Restored Inventory Asset: Return #${returnNumber}"
                    ),
                    JournalLineDraft(
                        accountId = "coa-5010",
                        isDebit = false,
                        amountMinor = totalSoundCogsReversal,
                        description = "COGS Reversal: Return #${returnNumber}"
                    )
                )
                accountingRepository.postJournalEntry(
                    entryDate = draft.returnDate,
                    descEn = "COGS Reversal for Return #${returnNumber}",
                    descBn = "ফেরত #${returnNumber} এর জন্য COGS সমন্বয়",
                    referenceType = "RETURN_COGS",
                    referenceId = returnId,
                    lines = cogsReversalLines
                )
            }

            if (totalDamageLoss > 0) {
                val damageLines = listOf(
                    JournalLineDraft(
                        accountId = "coa-5050", // Damaged Merchandise Write-Off
                        isDebit = true,
                        amountMinor = totalDamageLoss,
                        description = "Damaged Return Write-Off: Return #${returnNumber}"
                    ),
                    JournalLineDraft(
                        accountId = "coa-5010", // COGS
                        isDebit = false,
                        amountMinor = totalDamageLoss,
                        description = "COGS Reversal for Damaged Return: Return #${returnNumber}"
                    )
                )
                accountingRepository.postJournalEntry(
                    entryDate = draft.returnDate,
                    descEn = "Damaged Return Write-Off #${returnNumber}",
                    descBn = "ক্ষতিগ্রস্ত ফেরত পণ্য অবলোপন #${returnNumber}",
                    referenceType = "RETURN_DAMAGE",
                    referenceId = returnId,
                    lines = damageLines
                )
            }

            updateCustomerBalanceFromBaseline(invoice.customerId, customerBalanceBaseline)
            returnId
        }
    }

    override fun getAllReturns(): Flow<List<SalesReturn>> = salesReturnDao.getAllReturns()

    override fun getReturnsForInvoice(invoiceId: String): Flow<List<SalesReturn>> =
        salesReturnDao.getReturnsForInvoice(invoiceId)

    override suspend fun getItemsForReturn(returnId: String): List<SalesReturnItem> =
        salesReturnDao.getItemsForReturn(returnId)

    override suspend fun getCustomerLedger(customerId: String): List<CustomerLedgerEntry> {
        val ledgerItems = mutableListOf<CustomerLedgerEntry>()
        val customer = customerDao.getCustomerById(customerId)

        // 1. Invoices (Debit AR)
        val invoices = salesDao.getPostedInvoicesInRange(0L, Long.MAX_VALUE)
            .filter { it.customerId == customerId }

        for (inv in invoices) {
            ledgerItems.add(
                CustomerLedgerEntry(
                    id = inv.id,
                    date = inv.invoiceDate,
                    transactionType = "INVOICE",
                    referenceNumber = inv.invoiceNumber,
                    description = "Sales Invoice #${inv.invoiceNumber} (${inv.warehouseName})",
                    debitMinor = inv.totalAmountMinor,
                    creditMinor = 0L,
                    runningBalanceMinor = 0L
                )
            )
            // If invoice was cancelled, record offset in ledger
            if (inv.status == InvoiceStatus.CANCELLED.name) {
                ledgerItems.add(
                    CustomerLedgerEntry(
                        id = "${inv.id}-cancelled",
                        date = inv.cancelledAt ?: (inv.updatedAt),
                        transactionType = "INVOICE_CANCELLED",
                        referenceNumber = "CANCEL-${inv.invoiceNumber}",
                        description = "Cancelled Invoice #${inv.invoiceNumber}${if (!inv.cancellationReason.isNullOrBlank()) ": " + inv.cancellationReason else ""}",
                        debitMinor = 0L,
                        creditMinor = inv.totalAmountMinor,
                        runningBalanceMinor = 0L
                    )
                )
            }
        }

        // 2. Payments (Credit AR / Advance)
        val allCustomerPayments = customerPaymentDao.getPaymentsForCustomerSync(customerId).filter { it.isPosted }

        for (pay in allCustomerPayments) {
            ledgerItems.add(
                CustomerLedgerEntry(
                    id = pay.id,
                    date = pay.paymentDate,
                    transactionType = "PAYMENT",
                    referenceNumber = pay.paymentNumber,
                    description = "Payment Receipt #${pay.paymentNumber} (${pay.paymentMethod})",
                    debitMinor = 0L,
                    creditMinor = pay.amountMinor,
                    runningBalanceMinor = 0L
                )
            )
            // If payment was voided, record debit reversal offset in ledger
            if (pay.isVoided) {
                ledgerItems.add(
                    CustomerLedgerEntry(
                        id = "${pay.id}-voided",
                        date = pay.voidedAt ?: (pay.paymentDate + 1),
                        transactionType = "PAYMENT_VOID",
                        referenceNumber = "VOID-${pay.paymentNumber}",
                        description = "Void Payment #${pay.paymentNumber}${if (!pay.voidReason.isNullOrBlank()) ": " + pay.voidReason else ""}",
                        debitMinor = pay.amountMinor,
                        creditMinor = 0L,
                        runningBalanceMinor = 0L
                    )
                )
            }
        }

        // 3. Returns (Credit AR)
        val allReturns = salesReturnDao.getReturnsForCustomerSync(customerId).filter { it.isPosted }
        val adjustments = db.accountingDao().getCustomerLedgerAdjustments(customerId)
        for (adjustment in adjustments) {
            val delta = com.pocketdev.advance_erp.core.engine.CustomerLedgerAdjustment.delta(adjustment)
            ledgerItems.add(CustomerLedgerEntry(
                id = adjustment.id, date = adjustment.entryDate, transactionType = "ADJUSTMENT",
                referenceNumber = adjustment.entryNumber, description = adjustment.descriptionEn,
                debitMinor = delta.coerceAtLeast(0L), creditMinor = (-delta).coerceAtLeast(0L),
                runningBalanceMinor = 0L
            ))
        }

        // 0. Opening Due / Previous Balance (Confirmed Rule 4.2)
        val activeInvoices = invoices.filter { it.status != InvoiceStatus.CANCELLED.name }
        val activePayments = allCustomerPayments.filter { !it.isVoided }
        val netTransactions = activeInvoices.sumOf { it.totalAmountMinor } -
            activePayments.sumOf { it.amountMinor } -
            allReturns.filter { it.refundOption == ReturnRefundOption.CUSTOMER_CREDIT.name }.sumOf { it.totalReturnAmountMinor } +
            adjustments.sumOf { com.pocketdev.advance_erp.core.engine.CustomerLedgerAdjustment.delta(it) }
        val openingBal = if ((customer?.outstandingBalanceMinor ?: 0L) == 0L) 0L else (customer?.outstandingBalanceMinor ?: 0L) - netTransactions
        if (openingBal != 0L) {
            val earliestEventDate = ledgerItems.minOfOrNull { it.date } ?: System.currentTimeMillis()
            val initialDate = minOf(
                (customer?.createdAt ?: System.currentTimeMillis()) - (365L * 24 * 3600 * 1000),
                earliestEventDate - 1000L
            )
            ledgerItems.add(
                CustomerLedgerEntry(
                    id = "opening-$customerId",
                    date = initialDate,
                    transactionType = "OPENING_BALANCE",
                    referenceNumber = "OPENING",
                    description = "Opening / Previous Due / পূর্বের বকেয়া",
                    debitMinor = openingBal.coerceAtLeast(0L),
                    creditMinor = (-openingBal).coerceAtLeast(0L),
                    runningBalanceMinor = openingBal
                )
            )
        }

        for (ret in allReturns) {
            if (ret.refundOption == ReturnRefundOption.CUSTOMER_CREDIT.name) {
                ledgerItems.add(
                    CustomerLedgerEntry(
                        id = ret.id,
                        date = ret.returnDate,
                        transactionType = "RETURN",
                        referenceNumber = ret.returnNumber,
                        description = "Sales Return #${ret.returnNumber} on #${ret.invoiceNumber}",
                        debitMinor = 0L,
                        creditMinor = ret.totalReturnAmountMinor,
                        runningBalanceMinor = 0L
                    )
                )
            } else {
                ledgerItems.add(
                    CustomerLedgerEntry(
                        id = "${ret.id}-credit",
                        date = ret.returnDate,
                        transactionType = "RETURN",
                        referenceNumber = ret.returnNumber,
                        description = "Sales Return #${ret.returnNumber} on #${ret.invoiceNumber}",
                        debitMinor = 0L,
                        creditMinor = ret.totalReturnAmountMinor,
                        runningBalanceMinor = 0L
                    )
                )
                ledgerItems.add(
                    CustomerLedgerEntry(
                        id = "${ret.id}-refund",
                        date = ret.returnDate + 1,
                        transactionType = "REFUND",
                        referenceNumber = "REF-${ret.returnNumber}",
                        description = "Refund Paid: #${ret.returnNumber} (${ret.refundOption})",
                        debitMinor = ret.totalReturnAmountMinor,
                        creditMinor = 0L,
                        runningBalanceMinor = 0L
                    )
                )
            }
        }

        // Sort deterministically by date, transaction kind, and ID
        fun txOrder(type: String): Int = when (type) {
            "OPENING_BALANCE" -> 0
            "INVOICE" -> 1
            "ADJUSTMENT" -> 2
            "PAYMENT" -> 3
            "RETURN" -> 4
            "REFUND" -> 5
            "PAYMENT_VOID" -> 6
            "INVOICE_CANCELLED" -> 7
            else -> 8
        }
        ledgerItems.sortWith(compareBy<CustomerLedgerEntry> { it.date }.thenBy { txOrder(it.transactionType) }.thenBy { it.id })

        // Compute running balance: Previous Balance + Debit - Credit = Closing Balance
        var runningBal = 0L
        return ledgerItems.map { entry ->
            runningBal += (entry.debitMinor - entry.creditMinor)
            entry.copy(runningBalanceMinor = runningBal)
        }
    }

    private suspend fun computeCustomerTransactionNet(customerId: String): Long {
        val postedInvoices = salesDao.getInvoicesForCustomerSync(customerId)
            .filter { it.isPosted && it.status != InvoiceStatus.CANCELLED.name }
            .sumOf { it.totalAmountMinor }
        val activePayments = customerPaymentDao.getPaymentsForCustomerSync(customerId)
            .filter { it.isPosted && !it.isVoided }
            .sumOf { it.amountMinor }
        val creditReturns = salesReturnDao.getReturnsForCustomerSync(customerId)
            .filter { it.isPosted && it.refundOption == ReturnRefundOption.CUSTOMER_CREDIT.name }
            .sumOf { it.totalReturnAmountMinor }
        val adjustments = db.accountingDao().getCustomerLedgerAdjustments(customerId)
            .sumOf { com.pocketdev.advance_erp.core.engine.CustomerLedgerAdjustment.delta(it) }
        return postedInvoices - activePayments - creditReturns + adjustments
    }

    private suspend fun recalculateAndPersistInvoiceSettlement(
        invoiceId: String,
        updatedAt: Long = System.currentTimeMillis()
    ) {
        val inv = salesDao.getInvoiceById(invoiceId) ?: return
        val allocations = customerPaymentDao.getAllocationsForInvoiceSync(inv.id)
        var activeAllocatedMinor = 0L
        for (alloc in allocations) {
            val pay = customerPaymentDao.getPaymentById(alloc.paymentId)
            if (pay != null && pay.isPosted && !pay.isVoided) {
                activeAllocatedMinor += alloc.amountMinor
            }
        }
        val creditReturnMinor = salesReturnDao.getReturnsForCustomerSync(inv.customerId)
            .filter { it.invoiceId == inv.id && it.isPosted && it.refundOption == ReturnRefundOption.CUSTOMER_CREDIT.name }
            .sumOf { it.totalReturnAmountMinor }
        val settlement = com.pocketdev.advance_erp.core.engine.PaymentAllocationValidation.computeInvoiceSettlement(
            totalAmountMinor = inv.totalAmountMinor,
            activeAllocatedMinor = activeAllocatedMinor,
            creditReturnMinor = creditReturnMinor,
            isPosted = inv.isPosted,
            currentStatus = inv.status
        )
        salesDao.updateInvoicePaymentStatus(
            invoiceId = inv.id,
            status = settlement.status,
            paidMinor = settlement.paidAmountMinor,
            dueMinor = settlement.dueAmountMinor,
            now = updatedAt
        )
    }

    private suspend fun captureCustomerBalanceBaseline(customerId: String): Pair<Long, Long> {
        val customer = customerDao.getCustomerById(customerId)
        return (customer?.outstandingBalanceMinor ?: 0L) to computeCustomerTransactionNet(customerId)
    }

    private suspend fun updateCustomerBalanceFromBaseline(
        customerId: String,
        baseline: Pair<Long, Long>
    ): Long {
        val netAfter = computeCustomerTransactionNet(customerId)
        val updated = com.pocketdev.advance_erp.core.engine.PaymentAllocationValidation.outstandingAfterTransaction(
            masterDueBefore = baseline.first,
            transactionNetBefore = baseline.second,
            transactionNetAfter = netAfter
        )
        customerDao.updateOutstandingBalance(customerId, updated)
        return updated
    }

    override suspend fun recalculateCustomerBalance(customerId: String): Long {
        val ledger = getCustomerLedger(customerId)
        val outstandingMinor = ledger.lastOrNull()?.runningBalanceMinor ?: 0L
        customerDao.updateOutstandingBalance(customerId, maxOf(0L, outstandingMinor))
        return outstandingMinor
    }

    override fun getCustomerAdvanceBalance(customerId: String): Flow<Long> =
        customerPaymentDao.getCustomerAdvanceCreditMinor(customerId)

    override suspend fun getSalesReportSummary(fromMillis: Long, toMillis: Long): SalesReportSummary {
        val invoices = salesDao.getActivePostedInvoicesInRange(fromMillis, toMillis)

        var totalGross = 0L
        var totalDiscount = 0L
        var totalNet = 0L
        var totalCogs = 0L
        var totalDue = 0L

        for (inv in invoices) {
            totalGross += inv.subtotalMinor
            totalDiscount += inv.discountMinor
            totalNet += inv.totalAmountMinor
            totalCogs += inv.totalCogsMinor
            totalDue += inv.dueAmountMinor
        }

        val grossProfit = totalNet - totalCogs
        val margin = if (totalNet > 0) (grossProfit.toDouble() / totalNet.toDouble()) * 100.0 else 0.0

        val allPayments = customerPaymentDao.getAllPayments().first()
            .filter { it.isPosted && !it.isVoided && it.paymentDate in fromMillis..toMillis }
        val totalCollected = allPayments.sumOf { it.amountMinor }

        val allReturns = salesReturnDao.getAllReturns().first()
            .filter { it.isPosted && it.returnDate in fromMillis..toMillis }
        val totalReturns = allReturns.sumOf { it.totalReturnAmountMinor }

        return SalesReportSummary(
            totalGrossSalesMinor = totalGross,
            totalDiscountMinor = totalDiscount,
            totalNetSalesMinor = totalNet,
            totalCogsMinor = totalCogs,
            totalGrossProfitMinor = grossProfit,
            grossProfitMarginPercent = margin,
            totalCollectedMinor = totalCollected,
            totalReceivableMinor = totalDue,
            totalReturnsMinor = totalReturns
        )
    }

    override suspend fun getProductWiseSalesReport(fromMillis: Long, toMillis: Long): List<ProductWiseSalesItem> {
        val invoices = salesDao.getActivePostedInvoicesInRange(fromMillis, toMillis)
        val productMap = mutableMapOf<String, MutableList<SalesInvoiceItem>>()

        for (inv in invoices) {
            val items = salesDao.getItemsForInvoice(inv.id)
            for (item in items) {
                productMap.getOrPut(item.productId) { mutableListOf() }.add(item)
            }
        }

        return productMap.map { (prodId, items) ->
            val first = items.first()
            val totalQty = items.sumOf { it.quantity - it.returnedQuantity }
            val totalRev = items.sumOf { it.lineTotalMinor }
            val totalCogs = items.sumOf { it.totalCogsMinor }
            ProductWiseSalesItem(
                productId = prodId,
                productNameEn = first.productNameEn,
                productNameBn = first.productNameBn,
                sku = first.sku,
                unitsSold = totalQty,
                revenueMinor = totalRev,
                cogsMinor = totalCogs,
                grossProfitMinor = totalRev - totalCogs
            )
        }.sortedByDescending { it.revenueMinor }
    }

    override suspend fun getCustomerWiseSalesReport(fromMillis: Long, toMillis: Long): List<CustomerWiseSalesItem> {
        val invoices = salesDao.getActivePostedInvoicesInRange(fromMillis, toMillis)
        val customersById = customerDao.getLedgerCustomersSync().associateBy { it.id }
        val customerMap = invoices.groupBy { it.customerId }

        return customerMap.map { (custId, invList) ->
            val first = invList.first()
            val totalPurchased = invList.sumOf { it.totalAmountMinor }
            val totalPaid = invList.sumOf { it.paidAmountMinor }
            val totalDue = invList.sumOf { it.dueAmountMinor }
            val canonicalDue = customersById[custId]?.outstandingBalanceMinor ?: totalDue
            CustomerWiseSalesItem(
                customerId = custId,
                customerName = customersById[custId]?.name ?: first.customerName,
                customerPhone = customersById[custId]?.phone ?: (first.customerPhone ?: ""),
                invoiceCount = invList.size,
                totalPurchasedMinor = totalPurchased,
                totalPaidMinor = totalPaid,
                outstandingBalanceMinor = canonicalDue
            )
        }.sortedByDescending { it.totalPurchasedMinor }
    }

    override suspend fun getCustomerSummary(customerId: String): CustomerSummary? {
        val customer = customerDao.getCustomerById(customerId) ?: return null
        val invoices = salesDao.getInvoicesForCustomerSync(customerId)
        val postedInvoices = invoices.filter { it.isPosted && it.status != InvoiceStatus.CANCELLED.name }
        val totalSalesMinor = postedInvoices.sumOf { it.totalAmountMinor }
        val payments = customerPaymentDao.getPaymentsForCustomerSync(customerId).filter { it.isPosted && !it.isVoided }
        val lifetimePaidMinor = payments.sumOf { it.amountMinor }
        val returns = salesReturnDao.getReturnsForCustomerSync(customerId).filter { it.isPosted }
        val totalReturnsMinor = returns.sumOf { it.totalReturnAmountMinor }

        return CustomerSummary(
            customer = customer,
            totalSalesMinor = totalSalesMinor,
            lifetimePaidMinor = lifetimePaidMinor,
            outstandingBalanceMinor = customer.outstandingBalanceMinor,
            totalReturnsMinor = totalReturnsMinor
        )
    }

    override suspend fun searchCustomers(query: String): List<Customer> {
        return customerDao.searchCustomersSync(query)
    }

    // ==============================================================
    // Quotation & Sales Order Management (Confirmed Section 7)
    // ==============================================================

    override suspend fun createQuotation(draft: SalesInvoiceDraft): String {
        val quoId = UUID.randomUUID().toString()
        val quoNumber = documentNumberService.getNextQuotationNumber()

        val customer = if (draft.customerId.isNotBlank()) customerDao.getCustomerById(draft.customerId) else null
        val warehouse = warehouseDao.getWarehouseById(draft.warehouseId)
            ?: throw IllegalArgumentException("Warehouse not found: ${draft.warehouseId}")

        var subtotal = 0L
        val items = draft.items.map { itemDraft ->
            val p = productDao.getProductById(itemDraft.productId)
                ?: throw IllegalArgumentException("Product not found: ${itemDraft.productId}")
            val variantSku = itemDraft.variantId?.let { db.productVariantDao().getVariantById(it)?.sku }
            val lineGross = itemDraft.quantity.toLong() * itemDraft.unitPriceMinor
            val lineTotal = maxOf(0L, lineGross - itemDraft.discountMinor)
            subtotal += lineTotal

            SalesInvoiceItem(
                id = UUID.randomUUID().toString(),
                invoiceId = quoId,
                productId = itemDraft.productId,
                variantId = itemDraft.variantId,
                productNameEn = p.nameEn,
                productNameBn = p.nameBn,
                sku = variantSku ?: p.baseSku,
                quantity = itemDraft.quantity,
                returnedQuantity = 0,
                unitPriceMinor = itemDraft.unitPriceMinor,
                discountMinor = itemDraft.discountMinor,
                lineTotalMinor = lineTotal,
                unitCostMinor = 0L,
                totalCogsMinor = 0L
            )
        }

        val totalAmount = maxOf(0L, subtotal - draft.discountMinor + draft.additionalChargesMinor)

        val quotation = SalesInvoice(
            id = quoId,
            invoiceNumber = quoNumber,
            customerId = customer?.id ?: "",
            customerName = customer?.name ?: "Valued Customer",
            customerPhone = customer?.phone,
            warehouseId = warehouse.id,
            warehouseName = warehouse.name,
            status = InvoiceStatus.QUOTATION.name,
            subtotalMinor = subtotal,
            discountMinor = draft.discountMinor,
            taxMinor = draft.additionalChargesMinor,
            totalAmountMinor = totalAmount,
            paidAmountMinor = 0L,
            dueAmountMinor = totalAmount,
            totalCogsMinor = 0L,
            invoiceDate = draft.invoiceDate,
            dueDate = draft.dueDate,
            notes = draft.notes,
            isPosted = false,
            createdAt = System.currentTimeMillis(),
            validUntilDate = draft.validUntilDate,
            scheduledDeliveryInfo = draft.scheduledDeliveryInfo?.trim()?.ifBlank { null }
        )

        db.withTransaction {
            salesDao.insertInvoice(quotation)
            salesDao.insertInvoiceItems(items)
        }
        return quoId
    }

    override suspend fun updateDocumentCommunicationMetadata(
        invoiceId: String,
        validUntilDate: Long?,
        scheduledDeliveryInfo: String?
    ) {
        salesDao.updateDocumentCommunicationMetadata(
            invoiceId = invoiceId,
            validUntilDate = validUntilDate,
            scheduledDeliveryInfo = scheduledDeliveryInfo?.trim()?.ifBlank { null },
            updatedAt = System.currentTimeMillis()
        )
    }

    override suspend fun convertQuotationToSalesOrder(quotationId: String): Result<String> {
        val quo = salesDao.getInvoiceById(quotationId)
            ?: return Result.failure(IllegalArgumentException("Quotation not found: $quotationId"))

        if (quo.status != InvoiceStatus.QUOTATION.name) {
            return Result.failure(IllegalStateException("Document is already converted or in status: ${quo.status}"))
        }

        val quoItems = salesDao.getItemsForInvoice(quotationId)
        val soId = UUID.randomUUID().toString()
        val soNumber = documentNumberService.getNextSalesOrderNumber()

        val salesOrder = quo.copy(
            id = soId,
            invoiceNumber = soNumber,
            status = InvoiceStatus.SALES_ORDER.name,
            notes = "Converted from Quotation #${quo.invoiceNumber}. ${quo.notes ?: ""}".trim(),
            createdAt = System.currentTimeMillis()
        )

        val soItems = quoItems.map { it.copy(id = UUID.randomUUID().toString(), invoiceId = soId) }

        db.withTransaction {
            salesDao.insertInvoice(salesOrder)
            salesDao.insertInvoiceItems(soItems)
            salesDao.updateInvoiceStatus(quotationId, InvoiceStatus.CANCELLED.name)
        }

        return Result.success(soId)
    }

    override suspend fun convertSalesOrderToInvoice(salesOrderId: String): Result<String> {
        val so = salesDao.getInvoiceById(salesOrderId)
            ?: return Result.failure(IllegalArgumentException("Sales Order not found: $salesOrderId"))

        if (so.status != InvoiceStatus.SALES_ORDER.name) {
            return Result.failure(IllegalStateException("Document is not a Sales Order: ${so.status}"))
        }

        val soItems = salesDao.getItemsForInvoice(salesOrderId)
        val invId = UUID.randomUUID().toString()
        val invNumber = documentNumberService.getNextInvoiceNumber()

        val invoice = so.copy(
            id = invId,
            invoiceNumber = invNumber,
            status = InvoiceStatus.CONFIRMED.name,
            paidAmountMinor = 0L,
            dueAmountMinor = so.totalAmountMinor,
            isPosted = true,
            postedAt = System.currentTimeMillis(),
            notes = "Converted from Sales Order #${so.invoiceNumber}. ${so.notes ?: ""}".trim(),
            createdAt = System.currentTimeMillis()
        )

        val invItems = soItems.map { it.copy(id = UUID.randomUUID().toString(), invoiceId = invId) }

        db.withTransaction {
            val baseline = captureCustomerBalanceBaseline(invoice.customerId)
            salesDao.insertInvoice(invoice)
            salesDao.insertInvoiceItems(invItems)
            salesDao.updateInvoiceStatus(salesOrderId, InvoiceStatus.DELIVERED.name)

            // Post Accounting receivable & revenue (balanced with discount & additional charges)
            val now = System.currentTimeMillis()
            val grossCreditRevenue = invoice.subtotalMinor + invoice.taxMinor
            val effectiveDiscount = minOf(invoice.discountMinor, grossCreditRevenue)
            val lines = mutableListOf<JournalLineDraft>()
            if (invoice.totalAmountMinor > 0L) {
                lines.add(JournalLineDraft("coa-1040", true, invoice.totalAmountMinor, "Accounts Receivable: Inv #$invNumber"))
            }
            if (effectiveDiscount > 0L) {
                lines.add(JournalLineDraft("coa-4030", true, effectiveDiscount, "Sales Discount: Inv #$invNumber"))
            }
            if (grossCreditRevenue > 0L) {
                lines.add(JournalLineDraft("coa-4010", false, grossCreditRevenue, "Sales Revenue: Inv #$invNumber"))
                accountingRepository.postJournalEntry(
                    entryDate = now,
                    descEn = "Sales Invoice #$invNumber posted from SO #${so.invoiceNumber}",
                    descBn = "সেলস অর্ডার #${so.invoiceNumber} হতে বিক্রয় চালান #$invNumber পোস্ট করা হয়েছে",
                    referenceType = "SALES_INVOICE",
                    referenceId = invId,
                    lines = lines
                )
            }
            updateCustomerBalanceFromBaseline(invoice.customerId, baseline)
            val closing = getCustomerLedger(invoice.customerId).lastOrNull()?.runningBalanceMinor ?: 0L
            db.communicationDao().snapshot(com.pocketdev.advance_erp.data.entity.CommunicationSnapshot(
                "INVOICE", invId, invNumber, invoice.customerId, now, invoice.totalAmountMinor,
                closing - invoice.totalAmountMinor, 0L, invoice.dueAmountMinor, closing
            ))
        }

        return Result.success(invId)
    }

    // ==============================================================
    // Delivery Management & Partial Dispatch (Confirmed Section 5.4)
    // ==============================================================

    override suspend fun dispatchDelivery(draft: DeliveryDispatchDraft): Result<String> {
        return try {
            val deliveryNumber = db.withTransaction {
                val invoice = salesDao.getInvoiceById(draft.invoiceId)
                    ?: throw IllegalArgumentException("Invoice not found: ${draft.invoiceId}")

                val invoiceItemsList = salesDao.getItemsForInvoice(draft.invoiceId)
                val invoiceItems = invoiceItemsList.associateBy { it.id }
                val invoiceDiscountByItem = DeterministicAllocation.allocateInvoiceDiscountAcrossItems(
                    invoiceDiscountMinor = invoice.discountMinor,
                    items = invoiceItemsList
                )
                val delNumber = documentNumberService.getNextDeliveryNumber()
                val now = draft.deliveryDate
                val dispatchCostUsages = mutableListOf<SalesCostLayerUsage>()
                val dispatchFifoConsumptions = mutableListOf<FifoConsumption>()
                val dispatchSaleAllocations = mutableListOf<SaleLayerAllocation>()

                for (dispItem in draft.items) {
                    if (dispItem.quantityToDispatch <= 0) continue

                    val invItem = invoiceItems[dispItem.invoiceItemId]
                        ?: throw IllegalArgumentException("Invoice item not found: ${dispItem.invoiceItemId}")

                    // Check already dispatched
                    val alreadyDispatched = stockMovementDao.getDispatchedQuantityForInvoiceItem(
                        invoiceId = draft.invoiceId,
                        productId = dispItem.productId,
                        variantId = dispItem.variantId
                    )
                    val remainingToDispatch = invItem.quantity - alreadyDispatched
                    if (dispItem.quantityToDispatch > remainingToDispatch) {
                        throw IllegalStateException(
                            "Cannot dispatch ${dispItem.quantityToDispatch} for '${invItem.productNameEn}'. Only $remainingToDispatch remaining."
                        )
                    }

                    // Check sellable stock in warehouse
                    val balance = if (dispItem.variantId != null) {
                        stockBalanceDao.getVariantBalance(draft.warehouseId, dispItem.productId, dispItem.variantId)
                            ?: stockBalanceDao.getBalance(draft.warehouseId, dispItem.productId)
                    } else {
                        stockBalanceDao.getBalance(draft.warehouseId, dispItem.productId)
                    }
                    val sellable = maxOf(0, (balance?.quantity ?: 0) - (balance?.reservedQuantity ?: 0))
                    if (sellable < dispItem.quantityToDispatch) {
                        throw IllegalStateException(
                            "Insufficient sellable stock for '${invItem.productNameEn}'. Sellable: $sellable, Requested: ${dispItem.quantityToDispatch}"
                        )
                    }

                    // Deduct stock balance
                    val currentQty = balance?.quantity ?: 0
                    val newQty = currentQty - dispItem.quantityToDispatch
                    if (balance != null) {
                        stockBalanceDao.insertOrUpdate(balance.copy(quantity = newQty, updatedAt = now))
                    } else {
                        stockBalanceDao.insertOrUpdate(
                            StockBalance(
                                warehouseId = draft.warehouseId,
                                productId = dispItem.productId,
                                variantId = dispItem.variantId,
                                quantity = newQty,
                                updatedAt = now
                            )
                        )
                    }

                    // Deplete FIFO cost layers
                    var neededQty = dispItem.quantityToDispatch
                    var dispatchTotalCogs = 0L
                    val dispatchSlices = mutableListOf<ConsumedLayerSlice>()
                    val activeLayers = if (dispItem.variantId != null) {
                        val vLayers = inventoryCostLayerDao.getActiveLayersForVariantAndWarehouseSync(
                            productId = dispItem.productId,
                            variantId = dispItem.variantId,
                            warehouseId = draft.warehouseId
                        )
                        if (vLayers.isNotEmpty()) vLayers else {
                            inventoryCostLayerDao.getActiveLayersForProductAndWarehouseSync(
                                productId = dispItem.productId,
                                warehouseId = draft.warehouseId
                            )
                        }
                    } else {
                        inventoryCostLayerDao.getActiveLayersForProductAndWarehouseSync(
                            productId = dispItem.productId,
                            warehouseId = draft.warehouseId
                        )
                    }

                    for (layer in activeLayers) {
                        if (neededQty <= 0) break
                        val qtyToTake = minOf(neededQty, layer.remainingQuantity)
                        if (qtyToTake <= 0) continue
                        val layerCost = qtyToTake.toLong() * layer.unitLandedCostBdtMinor
                        dispatchTotalCogs += layerCost
                        neededQty -= qtyToTake

                        val newRemaining = layer.remainingQuantity - qtyToTake
                        inventoryCostLayerDao.updateRemainingQuantity(layer.id, newRemaining, newRemaining > 0)

                        dispatchCostUsages.add(
                            SalesCostLayerUsage(
                                invoiceId = invoice.id,
                                invoiceItemId = invItem.id,
                                costLayerId = layer.id,
                                quantityDepleted = qtyToTake,
                                unitCostMinor = layer.unitLandedCostBdtMinor
                            )
                        )
                        dispatchFifoConsumptions.add(
                            FifoConsumption(
                                invoiceId = invoice.id,
                                invoiceItemId = invItem.id,
                                costLayerId = layer.id,
                                productId = dispItem.productId,
                                variantId = dispItem.variantId,
                                warehouseId = draft.warehouseId,
                                quantity = qtyToTake,
                                unitCostBdtMinor = layer.unitLandedCostBdtMinor,
                                totalCogsBdtMinor = layerCost,
                                referenceType = "SALE",
                                referenceId = invoice.id,
                                createdAt = now
                            )
                        )
                        dispatchSlices.add(
                            ConsumedLayerSlice(
                                layer = layer,
                                quantity = qtyToTake,
                                unitCostMinor = layer.unitLandedCostBdtMinor,
                                totalCogsMinor = layerCost
                            )
                        )
                    }

                    if (neededQty > 0) {
                        val variant = dispItem.variantId?.let { db.productVariantDao().getVariantById(it) }
                        val prod = productDao.getProductById(dispItem.productId)
                        val fallbackCost = variant?.variantCostMinor?.takeIf { it > 0L } ?: prod?.purchasePriceMinor ?: 0L
                        val fallbackCogs = neededQty.toLong() * fallbackCost
                        dispatchTotalCogs += fallbackCogs
                        dispatchSlices.add(
                            ConsumedLayerSlice(
                                layer = null,
                                quantity = neededQty,
                                unitCostMinor = fallbackCost,
                                totalCogsMinor = fallbackCogs
                            )
                        )
                    }

                    dispatchSaleAllocations.addAll(
                        buildSaleLayerAllocationsForItem(
                            invoice = invoice,
                            item = invItem,
                            allocatedItemInvoiceDiscountMinor = invoiceDiscountByItem[invItem.id] ?: 0L,
                            slices = dispatchSlices,
                            sourceType = "DELIVERY_DISPATCH",
                            referenceId = delNumber,
                            now = now
                        )
                    )

                    val effectiveUnitCost = if (dispItem.quantityToDispatch > 0) dispatchTotalCogs / dispItem.quantityToDispatch else 0L
                    val updatedItemCogs = invItem.totalCogsMinor + dispatchTotalCogs
                    val totalDispatchedAfter = alreadyDispatched + dispItem.quantityToDispatch
                    val updatedItemUnitCogs = if (totalDispatchedAfter > 0) updatedItemCogs / totalDispatchedAfter else effectiveUnitCost
                    salesDao.updateInvoiceItemCogs(invItem.id, updatedItemUnitCogs, updatedItemCogs)

                    // Insert StockMovement
                    stockMovementDao.insertMovement(
                        StockMovement(
                            movementNumber = "$delNumber-${dispItem.productId.take(4)}",
                            warehouseId = draft.warehouseId,
                            productId = dispItem.productId,
                            variantId = dispItem.variantId,
                            movementType = StockMovementType.SALE_DISPATCH.code,
                            quantity = dispItem.quantityToDispatch,
                            unitCostMinor = effectiveUnitCost,
                            balanceAfterMovement = newQty,
                            totalCostImpactMinor = dispatchTotalCogs,
                            referenceType = "DELIVERY_CHALLAN",
                            referenceId = delNumber,
                            salesInvoiceId = draft.invoiceId,
                            notes = "Challan #$delNumber | Driver: ${draft.deliveryPerson ?: "N/A"}, Transport: ${draft.transportInfo ?: "N/A"}",
                            createdAt = now
                        )
                    )

                    // Post COGS vs Inventory for this dispatch
                    if (dispatchTotalCogs > 0) {
                        val cogsLines = listOf(
                            JournalLineDraft("coa-5010", true, dispatchTotalCogs, "COGS: Challan #$delNumber"),
                            JournalLineDraft("coa-1210", false, dispatchTotalCogs, "Inventory Dispatch: Challan #$delNumber")
                        )
                        accountingRepository.postJournalEntry(
                            entryDate = now,
                            descEn = "COGS for Delivery Challan #$delNumber (Inv #${invoice.invoiceNumber})",
                            descBn = "ডেলিভারি চালান #$delNumber এর বিক্রীত পণ্যের ব্যয়",
                            referenceType = "DELIVERY_CHALLAN",
                            referenceId = delNumber,
                            lines = cogsLines
                        )
                    }
                }

                if (dispatchCostUsages.isNotEmpty()) {
                    salesDao.insertCostLayerUsages(dispatchCostUsages)
                }
                if (dispatchFifoConsumptions.isNotEmpty()) {
                    db.fifoConsumptionDao().insertConsumptions(dispatchFifoConsumptions)
                }
                if (dispatchSaleAllocations.isNotEmpty()) {
                    db.importVaultDao().insertSaleLayerAllocations(dispatchSaleAllocations)
                }

                // Check overall invoice delivery progress & sync invoice totalCogsMinor
                val allItems = salesDao.getItemsForInvoice(draft.invoiceId)
                salesDao.updateInvoiceCogs(draft.invoiceId, allItems.sumOf { it.totalCogsMinor }, now)
                var totalOrdered = 0
                var totalDispatched = 0
                for (item in allItems) {
                    totalOrdered += item.quantity
                    totalDispatched += stockMovementDao.getDispatchedQuantityForInvoiceItem(
                        invoiceId = draft.invoiceId,
                        productId = item.productId,
                        variantId = item.variantId
                    )
                }

                val newInvoiceStatus = if (totalDispatched >= totalOrdered) {
                    InvoiceStatus.DELIVERED.name
                } else if (totalDispatched > 0) {
                    InvoiceStatus.PARTIALLY_DELIVERED.name
                } else {
                    InvoiceStatus.CONFIRMED.name
                }

                salesDao.updateInvoiceStatus(draft.invoiceId, newInvoiceStatus, now)
                delNumber
            }
            Result.success(deliveryNumber)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getRemainingUndeliveredItems(invoiceId: String): List<UndeliveredItemInfo> {
        val items = salesDao.getItemsForInvoice(invoiceId)
        return items.map { item ->
            val dispatched = stockMovementDao.getDispatchedQuantityForInvoiceItem(
                invoiceId = invoiceId,
                productId = item.productId,
                variantId = item.variantId
            )
            UndeliveredItemInfo(
                invoiceItemId = item.id,
                productId = item.productId,
                variantId = item.variantId,
                productNameEn = item.productNameEn,
                productNameBn = item.productNameBn,
                sku = item.sku,
                orderedQuantity = item.quantity,
                dispatchedQuantity = dispatched,
                remainingQuantity = maxOf(0, item.quantity - dispatched)
            )
        }
    }

    override suspend fun getDeliveryChallansForInvoice(invoiceId: String): List<DeliveryChallanInfo> {
        val invoice = salesDao.getInvoiceById(invoiceId) ?: return emptyList()
        val movements = stockMovementDao.getMovementsForInvoiceSync(invoiceId)
            .filter { it.referenceType == "DELIVERY_CHALLAN" }

        val byDeliveryNumber = movements.groupBy { it.referenceId ?: it.movementNumber }
        return byDeliveryNumber.map { (delNum, movList) ->
            val firstMov = movList.first()
            DeliveryChallanInfo(
                deliveryNumber = delNum,
                invoiceId = invoiceId,
                invoiceNumber = invoice.invoiceNumber,
                customerName = invoice.customerName,
                customerPhone = invoice.customerPhone,
                deliveryDate = firstMov.createdAt,
                deliveryPerson = firstMov.notes?.substringAfter("Driver: ")?.substringBefore(",")?.trim(),
                transportInfo = firstMov.notes?.substringAfter("Transport: ")?.trim(),
                notes = firstMov.notes,
                items = movList.map { m ->
                    val p = productDao.getProductById(m.productId)
                    DeliveryChallanItemInfo(
                        productId = m.productId,
                        variantId = m.variantId,
                        productNameEn = p?.nameEn ?: m.productId,
                        productNameBn = p?.nameBn ?: m.productId,
                        sku = p?.baseSku ?: "",
                        dispatchedQuantity = m.quantity
                    )
                }
            )
        }
    }

    // ==============================================================
    // Direct Product Exchange (Method A, Confirmed Section 6)
    // ==============================================================

    override suspend fun processDirectExchange(draft: DirectExchangeDraft): Result<String> {
        return try {
            val exchangeNumber = db.withTransaction {
                val exNum = documentNumberService.getNextSalesReturnNumber()
                val now = System.currentTimeMillis()
                val verifiedOrigInvoice = draft.originalInvoiceId?.let { salesDao.getInvoiceById(it) }
                val origAllocsByProduct = if (verifiedOrigInvoice != null) {
                    db.importVaultDao().getSaleLayerAllocationsForInvoiceSync(verifiedOrigInvoice.id)
                        .filter { !it.isReversed }
                } else {
                    emptyList()
                }
                val origItemsById = if (verifiedOrigInvoice != null) {
                    salesDao.getItemsForInvoice(verifiedOrigInvoice.id).associateBy { it.id }
                } else {
                    emptyMap()
                }

                var totalSoundReturnCogs = 0L
                var totalDamageLoss = 0L
                var totalReplacementCogs = 0L
                val exchangeFifoConsumptions = mutableListOf<FifoConsumption>()
                val exchangeSaleAllocations = mutableListOf<SaleLayerAllocation>()

                // 1. Process returned products (variant-aware + FIFO layer restore / quarantine)
                for (ret in draft.returnedItems) {
                    val damagedQty = if (ret.isDamaged) ret.returnQuantity else minOf(ret.damagedQuantity, ret.returnQuantity)
                    val soundQty = (ret.returnQuantity - damagedQty).coerceAtLeast(0)

                    val matchedAlloc = origAllocsByProduct.firstOrNull {
                        it.productId == ret.productId && (ret.variantId == null || it.variantId == ret.variantId) &&
                            (ret.targetCostLayerId == null || it.costLayerId == ret.targetCostLayerId)
                    }
                    val matchedInvItem = origItemsById[ret.invoiceItemId]
                    val variant = ret.variantId?.let { db.productVariantDao().getVariantById(it) }
                    val prod = productDao.getProductById(ret.productId)
                    val retUnitCost = matchedAlloc?.unitCostBdtMinor
                        ?: matchedInvItem?.unitCostMinor?.takeIf { it > 0L }
                        ?: variant?.variantCostMinor?.takeIf { it > 0L }
                        ?: prod?.purchasePriceMinor
                        ?: 0L
                    val origLayer = matchedAlloc?.costLayerId?.let { inventoryCostLayerDao.getCostLayerById(it) }

                    if (soundQty > 0) {
                        val bal = if (ret.variantId != null) {
                            stockBalanceDao.getVariantBalance(draft.warehouseId, ret.productId, ret.variantId)
                                ?: stockBalanceDao.getBalance(draft.warehouseId, ret.productId)
                        } else {
                            stockBalanceDao.getBalance(draft.warehouseId, ret.productId)
                        }
                        val newQty = (bal?.quantity ?: 0) + soundQty
                        if (bal != null) {
                            stockBalanceDao.insertOrUpdate(bal.copy(quantity = newQty, updatedAt = now))
                        } else {
                            stockBalanceDao.insertOrUpdate(
                                StockBalance(
                                    warehouseId = draft.warehouseId,
                                    productId = ret.productId,
                                    variantId = ret.variantId,
                                    quantity = newQty,
                                    updatedAt = now
                                )
                            )
                        }

                        val restoredLayerId = "icl-${UUID.randomUUID()}"
                        val origReceiptDate = origLayer?.originReceiptDate ?: origLayer?.receiptDate ?: now
                        val (poId, poItemId, shipId) = resolveLayerProvenance(origLayer)
                        inventoryCostLayerDao.insertCostLayer(
                            InventoryCostLayer(
                                id = restoredLayerId,
                                productId = ret.productId,
                                variantId = ret.variantId,
                                warehouseId = draft.warehouseId,
                                shipmentId = origLayer?.shipmentId ?: shipId ?: "RET",
                                shipmentNumber = origLayer?.shipmentNumber ?: exNum,
                                warehouseReceiptId = exNum,
                                receiptNumber = exNum,
                                initialQuantity = soundQty,
                                remainingQuantity = soundQty,
                                unitLandedCostBdtMinor = retUnitCost,
                                originalCurrency = origLayer?.originalCurrency ?: "BDT",
                                originalPurchasePriceMinor = origLayer?.originalPurchasePriceMinor ?: retUnitCost,
                                exchangeRateToBdt = origLayer?.exchangeRateToBdt ?: "1.0",
                                allocatedExpensePerUnitBdtMinor = origLayer?.allocatedExpensePerUnitBdtMinor ?: 0L,
                                receiptDate = origReceiptDate,
                                isActive = true,
                                purchaseOrderId = origLayer?.purchaseOrderId ?: poId,
                                purchaseOrderItemId = origLayer?.purchaseOrderItemId ?: poItemId,
                                purchaseBatchId = origLayer?.purchaseBatchId,
                                originCostLayerId = origLayer?.originCostLayerId ?: origLayer?.id,
                                originReceiptDate = origReceiptDate,
                                layerSourceType = "SALES_RETURN_RESTORE",
                                createdAt = now
                            )
                        )

                        val soundCogs = soundQty.toLong() * retUnitCost
                        totalSoundReturnCogs += soundCogs
                        stockMovementDao.insertMovement(
                            StockMovement(
                                movementNumber = "$exNum-RET-${ret.productId.take(4)}",
                                warehouseId = draft.warehouseId,
                                productId = ret.productId,
                                variantId = ret.variantId,
                                movementType = StockMovementType.SALE_RETURN.code,
                                quantity = soundQty,
                                unitCostMinor = retUnitCost,
                                balanceAfterMovement = newQty,
                                totalCostImpactMinor = soundCogs,
                                referenceType = "DIRECT_EXCHANGE",
                                referenceId = exNum,
                                salesInvoiceId = verifiedOrigInvoice?.id,
                                notes = "Good stock returned in Direct Exchange #$exNum",
                                createdAt = now
                            )
                        )
                    }

                    if (damagedQty > 0) {
                        val dmgLoss = damagedQty.toLong() * retUnitCost
                        totalDamageLoss += dmgLoss
                        val curQty = (if (ret.variantId != null) {
                            stockBalanceDao.getVariantBalance(draft.warehouseId, ret.productId, ret.variantId)
                                ?: stockBalanceDao.getBalance(draft.warehouseId, ret.productId)
                        } else {
                            stockBalanceDao.getBalance(draft.warehouseId, ret.productId)
                        })?.quantity ?: 0
                        stockMovementDao.insertMovement(
                            StockMovement(
                                movementNumber = "$exNum-DAM-${ret.productId.take(4)}",
                                warehouseId = draft.warehouseId,
                                productId = ret.productId,
                                variantId = ret.variantId,
                                movementType = StockMovementType.DAMAGE_WRITE_OFF.code,
                                quantity = damagedQty,
                                unitCostMinor = retUnitCost,
                                balanceAfterMovement = curQty,
                                totalCostImpactMinor = dmgLoss,
                                referenceType = "DIRECT_EXCHANGE",
                                referenceId = exNum,
                                salesInvoiceId = verifiedOrigInvoice?.id,
                                notes = "Damaged return quarantined during Direct Exchange #$exNum",
                                createdAt = now
                            )
                        )
                    }
                }

                // 2. Validate, deduct and FIFO-deplete replacement products
                for (rep in draft.replacementItems) {
                    val bal = if (rep.variantId != null) {
                        stockBalanceDao.getVariantBalance(draft.warehouseId, rep.productId, rep.variantId)
                            ?: stockBalanceDao.getBalance(draft.warehouseId, rep.productId)
                    } else {
                        stockBalanceDao.getBalance(draft.warehouseId, rep.productId)
                    }
                    val sellable = maxOf(0, (bal?.quantity ?: 0) - (bal?.reservedQuantity ?: 0))
                    if (sellable < rep.quantity) {
                        throw IllegalStateException("Insufficient sellable stock for replacement product: ${rep.productId}")
                    }
                    val newQty = (bal?.quantity ?: 0) - rep.quantity
                    stockBalanceDao.insertOrUpdate(bal!!.copy(quantity = newQty, updatedAt = now))

                    var neededQty = rep.quantity
                    var repTotalCogs = 0L
                    val repSlices = mutableListOf<ConsumedLayerSlice>()
                    val activeLayers = if (rep.variantId != null) {
                        val vLayers = inventoryCostLayerDao.getActiveLayersForVariantAndWarehouseSync(
                            productId = rep.productId,
                            variantId = rep.variantId,
                            warehouseId = draft.warehouseId
                        )
                        if (vLayers.isNotEmpty()) vLayers else {
                            inventoryCostLayerDao.getActiveLayersForProductAndWarehouseSync(rep.productId, draft.warehouseId)
                        }
                    } else {
                        inventoryCostLayerDao.getActiveLayersForProductAndWarehouseSync(rep.productId, draft.warehouseId)
                    }

                    for (layer in activeLayers) {
                        if (neededQty <= 0) break
                        val take = minOf(neededQty, layer.remainingQuantity)
                        if (take <= 0) continue
                        val cogs = take.toLong() * layer.unitLandedCostBdtMinor
                        repTotalCogs += cogs
                        neededQty -= take
                        val newRem = layer.remainingQuantity - take
                        inventoryCostLayerDao.updateRemainingQuantity(layer.id, newRem, newRem > 0)

                        exchangeFifoConsumptions.add(
                            FifoConsumption(
                                invoiceId = verifiedOrigInvoice?.id,
                                invoiceItemId = null,
                                costLayerId = layer.id,
                                productId = rep.productId,
                                variantId = rep.variantId,
                                warehouseId = draft.warehouseId,
                                quantity = take,
                                unitCostBdtMinor = layer.unitLandedCostBdtMinor,
                                totalCogsBdtMinor = cogs,
                                referenceType = "SALE",
                                referenceId = exNum,
                                createdAt = now
                            )
                        )
                        repSlices.add(ConsumedLayerSlice(layer, take, layer.unitLandedCostBdtMinor, cogs))
                    }
                    if (neededQty > 0) {
                        val variant = rep.variantId?.let { db.productVariantDao().getVariantById(it) }
                        val prod = productDao.getProductById(rep.productId)
                        val fallbackCost = variant?.variantCostMinor?.takeIf { it > 0L } ?: prod?.purchasePriceMinor ?: 0L
                        val fallbackCogs = neededQty.toLong() * fallbackCost
                        repTotalCogs += fallbackCogs
                        repSlices.add(ConsumedLayerSlice(null, neededQty, fallbackCost, fallbackCogs))
                    }

                    val repGrossTotal = rep.quantity.toLong() * rep.unitPriceMinor
                    val repLineDiscTotal = rep.discountMinor
                    val sliceWeights = repSlices.mapIndexed { idx, s -> idx.toString() to java.math.BigDecimal(s.quantity) }
                    val grossMap = DeterministicAllocation.allocateProportionally(repGrossTotal, sliceWeights)
                    val discMap = DeterministicAllocation.allocateProportionally(repLineDiscTotal, sliceWeights)
                    repSlices.forEachIndexed { idx, s ->
                        val g = grossMap[idx.toString()] ?: 0L
                        val d = discMap[idx.toString()] ?: 0L
                        val (poId, poItemId, shipId) = resolveLayerProvenance(s.layer)
                        val unattributed = (s.layer == null) || (poId == null && shipId == null)
                        exchangeSaleAllocations.add(
                            SaleLayerAllocation(
                                id = UUID.randomUUID().toString(),
                                invoiceId = verifiedOrigInvoice?.id,
                                invoiceItemId = null,
                                costLayerId = s.layer?.id,
                                productId = rep.productId,
                                variantId = rep.variantId,
                                warehouseId = draft.warehouseId,
                                purchaseOrderId = poId,
                                purchaseOrderItemId = poItemId,
                                shipmentId = shipId,
                                quantity = s.quantity,
                                unitCostBdtMinor = s.unitCostMinor,
                                totalCogsBdtMinor = s.totalCogsMinor,
                                allocatedGrossRevenueMinor = g,
                                allocatedLineDiscountMinor = d,
                                allocatedInvoiceDiscountMinor = 0L,
                                allocatedNetRevenueMinor = maxOf(0L, g - d),
                                isUnattributedFallback = unattributed,
                                isReversed = false,
                                sourceType = "DIRECT_EXCHANGE",
                                referenceId = exNum,
                                createdAt = now
                            )
                        )
                    }

                    totalReplacementCogs += repTotalCogs
                    val repUnitCost = if (rep.quantity > 0) repTotalCogs / rep.quantity else 0L
                    stockMovementDao.insertMovement(
                        StockMovement(
                            movementNumber = "$exNum-DISP-${rep.productId.take(4)}",
                            warehouseId = draft.warehouseId,
                            productId = rep.productId,
                            variantId = rep.variantId,
                            movementType = StockMovementType.SALE_DISPATCH.code,
                            quantity = rep.quantity,
                            unitCostMinor = repUnitCost,
                            balanceAfterMovement = newQty,
                            totalCostImpactMinor = repTotalCogs,
                            referenceType = "DIRECT_EXCHANGE",
                            referenceId = exNum,
                            salesInvoiceId = verifiedOrigInvoice?.id,
                            notes = "Replacement goods dispatched in Direct Exchange #$exNum",
                            createdAt = now
                        )
                    )
                }

                if (exchangeFifoConsumptions.isNotEmpty()) {
                    db.fifoConsumptionDao().insertConsumptions(exchangeFifoConsumptions)
                }
                if (exchangeSaleAllocations.isNotEmpty()) {
                    db.importVaultDao().insertSaleLayerAllocations(exchangeSaleAllocations)
                }

                // 3. Post inventory/COGS/damage journals for returned & replacement stock
                if (totalSoundReturnCogs > 0L) {
                    accountingRepository.postJournalEntry(
                        entryDate = now,
                        descEn = "Inventory Restored (Direct Exchange #$exNum)",
                        descBn = "সরাসরি বিনিময় #$exNum-এ ফেরতকৃত পণ্যের মজুদ সমন্বয়",
                        referenceType = "DIRECT_EXCHANGE_RETURN_COGS",
                        referenceId = exNum,
                        lines = listOf(
                            JournalLineDraft("coa-1210", true, totalSoundReturnCogs, "Restored Inventory: #$exNum"),
                            JournalLineDraft("coa-5010", false, totalSoundReturnCogs, "COGS Reversal: #$exNum")
                        )
                    )
                }
                if (totalDamageLoss > 0L) {
                    accountingRepository.postJournalEntry(
                        entryDate = now,
                        descEn = "Damaged Return Write-Off (Direct Exchange #$exNum)",
                        descBn = "সরাসরি বিনিময় #$exNum-এ ক্ষতিগ্রস্ত পণ্যের অবলোপন",
                        referenceType = "DIRECT_EXCHANGE_DAMAGE",
                        referenceId = exNum,
                        lines = listOf(
                            JournalLineDraft("coa-5050", true, totalDamageLoss, "Damaged Write-Off: #$exNum"),
                            JournalLineDraft("coa-5010", false, totalDamageLoss, "COGS Reversal (Damaged): #$exNum")
                        )
                    )
                }
                if (totalReplacementCogs > 0L) {
                    accountingRepository.postJournalEntry(
                        entryDate = now,
                        descEn = "Replacement COGS (Direct Exchange #$exNum)",
                        descBn = "সরাসরি বিনিময় #$exNum-এ পরিবর্তিত পণ্যের ব্যয়",
                        referenceType = "DIRECT_EXCHANGE_COGS",
                        referenceId = exNum,
                        lines = listOf(
                            JournalLineDraft("coa-5010", true, totalReplacementCogs, "Replacement COGS: #$exNum"),
                            JournalLineDraft("coa-1210", false, totalReplacementCogs, "Inventory Dispatch: #$exNum")
                        )
                    )
                }

                // 4. Post accounting price difference without duplicating full revenue
                if (draft.netDifferenceMinor != 0L) {
                    val lines = if (draft.netDifferenceMinor > 0) {
                        // Customer owes more -> Debit Cash/AR, Credit Sales Revenue
                        listOf(
                            JournalLineDraft(draft.paymentAccountId, true, draft.netDifferenceMinor, "Direct Exchange Extra Collection: #$exNum"),
                            JournalLineDraft("coa-4010", false, draft.netDifferenceMinor, "Net Sales Revenue Adjustment: #$exNum")
                        )
                    } else {
                        // Customer owed refund -> Debit Sales Returns, Credit Cash/AP
                        val refundAmt = -draft.netDifferenceMinor
                        listOf(
                            JournalLineDraft("coa-4020", true, refundAmt, "Sales Return Allowance: #$exNum"),
                            JournalLineDraft(draft.paymentAccountId, false, refundAmt, "Direct Exchange Refund Paid: #$exNum")
                        )
                    }
                    accountingRepository.postJournalEntry(
                        entryDate = now,
                        descEn = "Direct Product Exchange #$exNum for customer ${draft.customerId}",
                        descBn = "সরাসরি পণ্য বিনিময় #$exNum",
                        referenceType = "DIRECT_EXCHANGE",
                        referenceId = exNum,
                        lines = lines
                    )
                }

                val safeRows = org.json.JSONArray()
                for (item in draft.returnedItems) {
                    val product = productDao.getProductById(item.productId)
                    safeRows.put(org.json.JSONArray(listOf("Return / ফেরত", product?.nameEn ?: item.productId, item.returnQuantity.toString())))
                }
                for (item in draft.replacementItems) {
                    val product = productDao.getProductById(item.productId)
                    safeRows.put(org.json.JSONArray(listOf("Replacement / প্রতিস্থাপন", product?.nameEn ?: item.productId, item.quantity.toString())))
                }
                db.communicationDao().snapshot(com.pocketdev.advance_erp.data.entity.CommunicationSnapshot(
                    "EXCHANGE", exNum, exNum, draft.customerId, now, draft.netDifferenceMinor,
                    0L, 0L, 0L, 0L, org.json.JSONObject().put("rows", safeRows).put("method", draft.paymentMethod).toString()
                ))
                exNum
            }
            Result.success(exchangeNumber)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun voidCustomerPayment(
        paymentId: String,
        reason: String,
        operator: String
    ): Result<Unit> {
        return try {
            val payment = customerPaymentDao.getPaymentById(paymentId)
                ?: return Result.failure(IllegalArgumentException("Payment receipt not found: $paymentId"))

            if (payment.isVoided) {
                return Result.failure(IllegalStateException("Payment receipt #${payment.paymentNumber} has already been voided."))
            }

            if (reason.isBlank()) {
                return Result.failure(IllegalArgumentException("A valid cancellation reason is required to void a payment."))
            }

            db.withTransaction {
                val now = System.currentTimeMillis()
                val customerBalanceBaseline = captureCustomerBalanceBaseline(payment.customerId)

                // 1. Reverse payment allocations and authoritatively recalculate invoices (respecting credit returns)
                val allocations = customerPaymentDao.getAllocationsForPayment(paymentId)
                val affectedInvoiceIds = allocations.map { it.invoiceId }.distinct()
                customerPaymentDao.deleteAllocationsForPayment(paymentId)

                // Mark payment record voided before recalculating so voided payment is excluded
                val updatedPayment = payment.copy(
                    isVoided = true,
                    voidedAt = now,
                    voidReason = reason.trim(),
                    voidedBy = operator,
                    allocatedAmountMinor = 0L,
                    unallocatedCreditMinor = 0L
                )
                customerPaymentDao.updatePayment(updatedPayment)

                for (invId in affectedInvoiceIds) {
                    recalculateAndPersistInvoiceSettlement(invId, now)
                }

                // 2. Reverse accounting entries:
                val activeJournals = db.accountingDao().getActiveEntriesForReference("CUSTOMER_PAYMENT", paymentId)
                var revJournalId: String? = null
                if (activeJournals.isNotEmpty()) {
                    for (entry in activeJournals) {
                        revJournalId = accountingRepository.reverseJournalEntry(entry.id)
                    }
                } else {
                    val revLines = mutableListOf<JournalLineDraft>()
                    if (payment.allocatedAmountMinor > 0) {
                        revLines.add(
                            JournalLineDraft(
                                accountId = "coa-1040",
                                isDebit = true,
                                amountMinor = payment.allocatedAmountMinor,
                                description = "Restore AR: Void payment #${payment.paymentNumber}"
                            )
                        )
                    }
                    if (payment.unallocatedCreditMinor > 0) {
                        revLines.add(
                            JournalLineDraft(
                                accountId = "coa-2030",
                                isDebit = true,
                                amountMinor = payment.unallocatedCreditMinor,
                                description = "Reverse advance credit: Void payment #${payment.paymentNumber}"
                            )
                        )
                    }
                    revLines.add(
                        JournalLineDraft(
                            accountId = payment.accountId,
                            isDebit = false,
                            amountMinor = payment.amountMinor,
                            description = "Reverse Cash/Bank debit: Void payment #${payment.paymentNumber}"
                        )
                    )
                    revJournalId = accountingRepository.postJournalEntry(
                        entryDate = now,
                        descEn = "Void Payment Receipt #${payment.paymentNumber} ($reason)",
                        descBn = "পেমেন্ট রশিদ #${payment.paymentNumber} বাতিল ($reason)",
                        referenceType = "VOID_PAYMENT",
                        referenceId = paymentId,
                        lines = revLines
                    )
                }

                // 3. Payment record was already marked voided above before recalculating affected invoices

                // 4. Restore customer balance in database preserving genuine opening balance
                updateCustomerBalanceFromBaseline(payment.customerId, customerBalanceBaseline)

                // 5. Audit trail
                db.auditLogDao().insertAuditLog(
                    AuditLog(
                        recordType = "CUSTOMER_PAYMENT",
                        recordId = payment.id,
                        action = "VOID",
                        details = "Payment receipt #${payment.paymentNumber} of ৳${payment.amountMinor / 100} voided. Allocations reversed. Reason: $reason",
                        operator = operator,
                        documentNumber = payment.paymentNumber,
                        operationType = "PAYMENT_VOID",
                        previousStatus = "POSTED",
                        newStatus = "VOIDED",
                        cancellationReason = reason.trim(),
                        reversalReference = revJournalId
                    )
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun cancelOrDeleteInvoice(
        invoiceId: String,
        reason: String,
        operator: String
    ): Result<Unit> {
        return try {
            val invoice = salesDao.getInvoiceById(invoiceId)
                ?: return Result.failure(IllegalArgumentException("Invoice not found: $invoiceId"))

            if (invoice.status == InvoiceStatus.CANCELLED.name) {
                return Result.failure(IllegalStateException("Invoice #${invoice.invoiceNumber} is already cancelled."))
            }

            // Case A: Unposted invoice (DRAFT, QUOTATION, etc.)
            if (!invoice.isPosted) {
                db.withTransaction {
                    salesDao.deleteInvoiceItems(invoiceId)
                    salesDao.deleteInvoice(invoiceId)
                    db.auditLogDao().insertAuditLog(
                        AuditLog(
                            recordType = "SALES_INVOICE",
                            recordId = invoiceId,
                            action = "HARD_DELETE",
                            details = "Unposted draft invoice #${invoice.invoiceNumber} deleted. Reason: ${reason.ifBlank { "Deleted by user" }}",
                            operator = operator,
                            documentNumber = invoice.invoiceNumber,
                            operationType = "HARD_DELETE",
                            previousStatus = invoice.status,
                            newStatus = "DELETED",
                            cancellationReason = reason.ifBlank { "User deletion" }
                        )
                    )
                }
                return Result.success(Unit)
            }

            // Case B: Posted invoice (POSTED, PARTIALLY_PAID, PAID, etc.)
            if (reason.isBlank()) {
                return Result.failure(IllegalArgumentException("A valid cancellation reason is required to cancel a posted invoice."))
            }

            db.withTransaction {
                val now = System.currentTimeMillis()
                val customerBalanceBaseline = captureCustomerBalanceBaseline(invoice.customerId)

                // 1. Stock Restoration & FIFO Cost Layer Recovery
                val items = salesDao.getItemsForInvoice(invoiceId)
                val costLayerUsages = salesDao.getCostLayerUsagesForInvoice(invoiceId)

                for (item in items) {
                    val netDispatched = maxOf(0, item.quantity - item.returnedQuantity)
                    if (netDispatched > 0) {
                        val currentBalance = if (item.variantId != null) {
                            stockBalanceDao.getVariantBalance(invoice.warehouseId, item.productId, item.variantId)
                                ?: stockBalanceDao.getBalance(invoice.warehouseId, item.productId)
                        } else {
                            stockBalanceDao.getBalance(invoice.warehouseId, item.productId)
                        }
                        val existingQty = currentBalance?.quantity ?: 0
                        val newQty = existingQty + netDispatched
                        if (currentBalance != null) {
                            stockBalanceDao.insertOrUpdate(
                                currentBalance.copy(
                                    quantity = newQty,
                                    updatedAt = now
                                )
                            )
                        } else {
                            stockBalanceDao.insertOrUpdate(
                                StockBalance(
                                    warehouseId = invoice.warehouseId,
                                    productId = item.productId,
                                    variantId = item.variantId,
                                    quantity = newQty,
                                    updatedAt = now
                                )
                            )
                        }

                        // Stock Movement audit for restoration
                        val movement = StockMovement(
                            id = MovementNumberGenerator.generateId("mov-cancel"),
                            movementNumber = MovementNumberGenerator.generate("MOV-CAN"),
                            warehouseId = invoice.warehouseId,
                            productId = item.productId,
                            variantId = item.variantId,
                            movementType = StockMovementType.SALE_RETURN.code,
                            quantity = netDispatched,
                            unitCostMinor = item.unitCostMinor,
                            balanceAfterMovement = newQty,
                            totalCostImpactMinor = netDispatched.toLong() * item.unitCostMinor,
                            referenceType = "SALES_INVOICE_CANCEL",
                            referenceId = invoice.id,
                            salesInvoiceId = invoice.id,
                            operatorName = operator,
                            notes = "Stock restored from cancelled invoice #${invoice.invoiceNumber}. Reason: $reason",
                            createdAt = now
                        )
                        stockMovementDao.insertMovement(movement)
                    }
                }

                // Mark persisted sale_layer_allocations reversed and restore unreturned FIFO cost layers
                db.importVaultDao().markSaleLayerAllocationsReversedForInvoice(invoiceId)
                val returnedQtyByLayer = db.importVaultDao().getReturnLayerAllocationsForInvoiceSync(invoiceId)
                    .filter { it.costLayerId != null }
                    .groupBy { it.costLayerId!! }
                    .mapValues { (_, list) -> list.sumOf { it.soundQuantity + it.damagedQuantity } }
                    .toMutableMap()
                for (usage in costLayerUsages) {
                    val alreadyReturned = returnedQtyByLayer[usage.costLayerId] ?: 0
                    val deductFromUsage = minOf(usage.quantityDepleted, alreadyReturned)
                    returnedQtyByLayer[usage.costLayerId] = alreadyReturned - deductFromUsage
                    val qtyToRestore = usage.quantityDepleted - deductFromUsage
                    if (qtyToRestore > 0) {
                        val layer = inventoryCostLayerDao.getCostLayerById(usage.costLayerId)
                        if (layer != null) {
                            val newRemaining = layer.remainingQuantity + qtyToRestore
                            inventoryCostLayerDao.updateRemainingQuantity(layer.id, newRemaining, isActive = true)
                        }
                    }
                }

                // 2. Handle previously received payments allocated to this invoice:
                // Convert payments into customer advance credit (unallocated)
                val allocations = customerPaymentDao.getAllocationsForInvoiceSync(invoiceId)
                for (alloc in allocations) {
                    val payment = customerPaymentDao.getPaymentById(alloc.paymentId)
                    if (payment != null && !payment.isVoided) {
                        val freedAmount = minOf(alloc.amountMinor, payment.allocatedAmountMinor)
                        val updatedPayment = payment.copy(
                            allocatedAmountMinor = maxOf(0L, payment.allocatedAmountMinor - freedAmount),
                            unallocatedCreditMinor = payment.unallocatedCreditMinor + freedAmount,
                            isAdvance = true
                        )
                        customerPaymentDao.updatePayment(updatedPayment)

                        // Journal entry: reclassify from AR to Customer Advance
                        if (freedAmount > 0) {
                            accountingRepository.postJournalEntry(
                                entryDate = now,
                                descEn = "Convert payment #${payment.paymentNumber} to customer advance on cancelled invoice #${invoice.invoiceNumber}",
                                descBn = "বাতিলকৃত ইনভয়েস #${invoice.invoiceNumber} বাবদ পেমেন্ট #${payment.paymentNumber} অগ্রিমে রূপান্তর",
                                referenceType = "PAYMENT_TO_ADVANCE",
                                referenceId = invoice.id,
                                lines = listOf(
                                    JournalLineDraft(
                                        accountId = "coa-1040",
                                        isDebit = true,
                                        amountMinor = freedAmount,
                                        description = "Reclassify AR to Advance: Inv #${invoice.invoiceNumber}"
                                    ),
                                    JournalLineDraft(
                                        accountId = "coa-2030",
                                        isDebit = false,
                                        amountMinor = freedAmount,
                                        description = "Customer advance credit: Receipt #${payment.paymentNumber}"
                                    )
                                )
                            )
                        }
                    }
                }
                customerPaymentDao.deleteAllocationsForInvoice(invoiceId)

                // 3. Reverse Accounting Entries (Revenue and COGS)
                val activeRevenueJournals = db.accountingDao().getActiveEntriesForReference("SALES_INVOICE", invoiceId)
                var revRevId: String? = null
                if (activeRevenueJournals.isNotEmpty()) {
                    for (entry in activeRevenueJournals) {
                        revRevId = accountingRepository.reverseJournalEntry(entry.id)
                    }
                } else {
                    val revLines = mutableListOf<JournalLineDraft>()
                    val grossRevReversal = invoice.subtotalMinor + invoice.taxMinor
                    if (grossRevReversal > 0L) {
                        revLines.add(
                            JournalLineDraft(
                                accountId = "coa-4010",
                                isDebit = true,
                                amountMinor = grossRevReversal,
                                description = "Reversal of Sales Revenue: Inv #${invoice.invoiceNumber}"
                            )
                        )
                    }
                    if (invoice.discountMinor > 0) {
                        revLines.add(
                            JournalLineDraft(
                                accountId = "coa-4030",
                                isDebit = false,
                                amountMinor = invoice.discountMinor,
                                description = "Reversal of Sales Discount: Inv #${invoice.invoiceNumber}"
                            )
                        )
                    }
                    val netArReversal = invoice.totalAmountMinor
                    if (netArReversal > 0) {
                        revLines.add(
                            JournalLineDraft(
                                accountId = "coa-1040",
                                isDebit = false,
                                amountMinor = netArReversal,
                                description = "Reversal of AR: Inv #${invoice.invoiceNumber}"
                            )
                        )
                    }
                    if (revLines.isNotEmpty() && grossRevReversal > 0L) {
                        revRevId = accountingRepository.postJournalEntry(
                            entryDate = now,
                            descEn = "Cancellation reversal for Invoice #${invoice.invoiceNumber}",
                            descBn = "ইনভয়েস #${invoice.invoiceNumber} বাতিলের বিপরীত দাখিলা",
                            referenceType = "VOID_INVOICE_SALES",
                            referenceId = invoiceId,
                            lines = revLines
                        )
                    }
                }

                // Reverse COGS
                val activeCogsJournals = db.accountingDao().getActiveEntriesForReference("SALES_COGS", invoiceId)
                if (activeCogsJournals.isNotEmpty()) {
                    for (entry in activeCogsJournals) {
                        accountingRepository.reverseJournalEntry(entry.id)
                    }
                } else if (invoice.totalCogsMinor > 0) {
                    accountingRepository.postJournalEntry(
                        entryDate = now,
                        descEn = "Reversal of COGS for cancelled Invoice #${invoice.invoiceNumber}",
                        descBn = "বাতিলকৃত ইনভয়েস #${invoice.invoiceNumber} এর বিক্রীত পণ্যের ব্যয়ের বিপরীত দাখিলা",
                        referenceType = "VOID_INVOICE_COGS",
                        referenceId = invoiceId,
                        lines = listOf(
                            JournalLineDraft(
                                accountId = "coa-1210",
                                isDebit = true,
                                amountMinor = invoice.totalCogsMinor,
                                description = "Restore Inventory Asset: Inv #${invoice.invoiceNumber}"
                            ),
                            JournalLineDraft(
                                accountId = "coa-5010",
                                isDebit = false,
                                amountMinor = invoice.totalCogsMinor,
                                description = "Reverse COGS: Inv #${invoice.invoiceNumber}"
                            )
                        )
                    )
                }

                // 4. Update Invoice Record
                val cancelledInvoice = invoice.copy(
                    status = InvoiceStatus.CANCELLED.name,
                    dueAmountMinor = 0L,
                    paidAmountMinor = 0L,
                    cancellationReason = reason.trim(),
                    cancelledAt = now,
                    updatedAt = now
                )
                salesDao.updateInvoice(cancelledInvoice)

                // 5. Restore Customer Balance preserving genuine opening balance
                updateCustomerBalanceFromBaseline(invoice.customerId, customerBalanceBaseline)

                // 6. Audit Trail
                db.auditLogDao().insertAuditLog(
                    AuditLog(
                        recordType = "SALES_INVOICE",
                        recordId = invoice.id,
                        action = "CANCEL",
                        details = "Posted invoice #${invoice.invoiceNumber} (Total: ৳${invoice.totalAmountMinor / 100}) cancelled. Stock, FIFO, and accounting reversed. Reason: $reason",
                        operator = operator,
                        documentNumber = invoice.invoiceNumber,
                        operationType = "INVOICE_CANCEL",
                        previousStatus = invoice.status,
                        newStatus = "CANCELLED",
                        cancellationReason = reason.trim(),
                        reversalReference = revRevId
                    )
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
