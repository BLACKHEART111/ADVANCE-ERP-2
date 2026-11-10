package com.pocketdev.advance_erp.core.engine

import com.pocketdev.advance_erp.data.entity.CostCategory
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * PHASE 4 — CENTRALIZED, REUSABLE & TESTABLE LANDING COST CALCULATION ENGINE
 *
 * Implements all Phase 2, 3, 4 & 5 business rules:
 * 1. Multi-Supplier, Multi-PO, Multi-Currency Purchase Cost conversion using saved manual exchange rates.
 * 2. Supplier-wise China Local Delivery allocation (Purchase Value default or Manual product-wise).
 * 3. Carton-wise Packaging List aggregation (Quantity, Gross Weight KG, CBM, Carton Count) with optional dimensions.
 * 4. Two-level Category-wise International Freight (Default Category Rate + Shipment Override Rate preserved historically).
 * 5. Bangladesh Local Transport & Additional Import Expenses supporting all 6 AllocationMethods:
 *    - GROSS_WEIGHT
 *    - CBM
 *    - CARTON_COUNT
 *    - PURCHASE_VALUE
 *    - QUANTITY
 *    - MANUAL (validates sum of manual allocations == expense amount)
 * 6. Duplicate expense allocation guard.
 * 7. Receiving reconciliation validation: Packed Quantity = Good + Damaged + Missing + Pending.
 * 8. Unit Import Cost = Total Allocated Product Import Cost / Packed Quantity, with consistent valuation
 *    across Good (1210), Damaged (5050), Missing (1050), and Pending (1220) quantities.
 * 9. Post-Finalize Cost Adjustment calculation splitting additional costs between remaining FIFO Inventory (1210)
 *    and already-sold COGS (5010) while preserving historical Sales Invoice prices.
 */
object LandingCostEngine {

    data class ShipmentProductInput(
        val productId: String,
        val variantId: String? = null,
        val productNameEn: String,
        val productNameBn: String = productNameEn,
        val sku: String = "",
        val supplierId: String,
        val purchaseOrderId: String? = null,
        val categoryId: String,
        val categoryNameEn: String = "",
        val packedQuantity: Int,
        val unitPurchasePriceForeignMinor: Long,
        val purchaseCurrency: String = "CNY",
        val purchaseExchangeRateToBdt: BigDecimal = BigDecimal.ONE,
        val goodQuantity: Int = packedQuantity,
        val damagedQuantity: Int = 0,
        val missingQuantity: Int = 0,
        val pendingQuantity: Int = 0,
        val fallbackGrossWeightKg: BigDecimal = BigDecimal.ZERO,
        val fallbackCbm: BigDecimal = BigDecimal.ZERO,
        val fallbackCartonCount: Int = 0
    ) {
        val itemKey: String
            get() = if (variantId != null) "$productId:$variantId" else productId
    }

    data class ShipmentCartonInput(
        val cartonNumber: String,
        val supplierId: String,
        val productId: String,
        val variantId: String? = null,
        val categoryId: String,
        val quantity: Int,
        val grossWeightKg: BigDecimal,
        val cbm: BigDecimal = BigDecimal.ZERO,
        val lengthCm: BigDecimal? = null,
        val widthCm: BigDecimal? = null,
        val heightCm: BigDecimal? = null
    ) {
        val effectiveCbm: BigDecimal
            get() {
                if (cbm > BigDecimal.ZERO) return cbm.setScale(6, RoundingMode.HALF_UP)
                val l = lengthCm ?: BigDecimal.ZERO
                val w = widthCm ?: BigDecimal.ZERO
                val h = heightCm ?: BigDecimal.ZERO
                return if (l > BigDecimal.ZERO && w > BigDecimal.ZERO && h > BigDecimal.ZERO) {
                    CbmCalculator.calculateCbm(l, w, h)
                } else {
                    BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP)
                }
            }
    }

    data class PackagingSummary(
        val totalQuantity: Int,
        val totalCartonCount: Int,
        val totalGrossWeightKg: BigDecimal,
        val totalCbm: BigDecimal,
        val productQuantityMap: Map<String, Int>,
        val productWeightMap: Map<String, BigDecimal>,
        val productCbmMap: Map<String, BigDecimal>,
        val productCartonCountMap: Map<String, Int>,
        val categoryWeightMap: Map<String, BigDecimal>
    )

    data class SupplierChinaDeliveryInput(
        val supplierId: String,
        val deliveryChargeForeignMinor: Long,
        val currency: String = "CNY",
        val exchangeRateToBdt: BigDecimal = BigDecimal.ONE,
        val allocationMethod: AllocationMethod = AllocationMethod.PURCHASE_VALUE,
        val manualProductAllocationsForeignMinor: Map<String, Long> = emptyMap()
    ) {
        val deliveryChargeBdtMinor: Long
            get() = convertToBdtMinor(deliveryChargeForeignMinor, currency, exchangeRateToBdt)
    }

    data class CategoryFreightRateInput(
        val categoryId: String,
        val categoryNameEn: String,
        val defaultRatePerKgBdtMinor: Long,
        val overrideRatePerKgBdtMinor: Long? = null
    ) {
        val appliedRatePerKgBdtMinor: Long
            get() = overrideRatePerKgBdtMinor ?: defaultRatePerKgBdtMinor
    }

    data class CategoryFreightResult(
        val categoryId: String,
        val categoryNameEn: String,
        val defaultRatePerKgBdtMinor: Long,
        val overrideRatePerKgBdtMinor: Long?,
        val appliedRatePerKgBdtMinor: Long,
        val totalCategoryGrossWeightKg: BigDecimal,
        val totalCategoryFreightBdtMinor: Long,
        val productAllocationsBdtMinor: Map<String, Long>
    )

    data class ImportExpenseInput(
        val expenseId: String,
        val expenseName: String,
        val category: CostCategory,
        val amountForeignMinor: Long,
        val currency: String = "BDT",
        val exchangeRateToBdt: BigDecimal = BigDecimal.ONE,
        val allocationMethod: AllocationMethod = category.defaultAllocationMethod,
        val supplierId: String? = null,
        val manualProductAllocationsBdtMinor: Map<String, Long> = emptyMap(),
        val isPostFinalization: Boolean = false,
        val isAlreadyCapitalized: Boolean = false
    ) {
        val amountBdtMinor: Long
            get() = convertToBdtMinor(amountForeignMinor, currency, exchangeRateToBdt)
    }

    data class ProductLandingCostBreakdown(
        val productId: String,
        val variantId: String?,
        val productNameEn: String,
        val productNameBn: String,
        val sku: String,
        val supplierId: String,
        val categoryId: String,
        val packedQuantity: Int,
        val goodQuantity: Int,
        val damagedQuantity: Int,
        val missingQuantity: Int,
        val pendingQuantity: Int,
        val grossWeightKg: BigDecimal,
        val cbm: BigDecimal,
        val cartonCount: Int,
        val purchaseCostForeignMinor: Long,
        val purchaseCurrency: String,
        val purchaseExchangeRateToBdt: BigDecimal,
        val purchaseCostBdtMinor: Long,
        val chinaDeliveryForeignMinor: Long,
        val chinaDeliveryBdtMinor: Long,
        val internationalFreightBdtMinor: Long,
        val bdTransportBdtMinor: Long,
        val otherImportExpensesBdtMinor: Long,
        val totalAllocatedExpensesBdtMinor: Long,
        val totalProductImportCostBdtMinor: Long,
        val unitImportCostBdtMinor: Long,
        val goodInventoryValuationBdtMinor: Long,
        val damagedWriteOffValuationBdtMinor: Long,
        val missingShortageValuationBdtMinor: Long,
        val pendingInTransitValuationBdtMinor: Long
    )

    data class ShipmentLandingCostResult(
        val packagingSummary: PackagingSummary,
        val categoryFreightResults: List<CategoryFreightResult>,
        val productBreakdowns: List<ProductLandingCostBreakdown>,
        val expenseAllocationsByExpenseId: Map<String, List<AllocationResultItem>>,
        val totalPurchaseCostBdtMinor: Long,
        val totalChinaDeliveryBdtMinor: Long,
        val totalInternationalFreightBdtMinor: Long,
        val totalBdTransportBdtMinor: Long,
        val totalOtherImportExpensesBdtMinor: Long,
        val totalImportExpensesBdtMinor: Long,
        val grandTotalLandingCostBdtMinor: Long,
        val totalGoodInventoryAssetBdtMinor: Long,
        val totalDamagedWriteOffBdtMinor: Long,
        val totalMissingShortageBdtMinor: Long,
        val totalPendingInTransitBdtMinor: Long
    )

    data class FifoLayerAdjustmentInput(
        val layerId: String,
        val productId: String,
        val variantId: String? = null,
        val warehouseId: String,
        val initialQuantity: Int,
        val remainingQuantity: Int,
        val soldQuantity: Int,
        val currentUnitLandedCostBdtMinor: Long
    )

    data class ProductPostFinalizeAdjustment(
        val productId: String,
        val variantId: String?,
        val packedQuantity: Int,
        val addedCostBdtMinor: Long,
        val oldUnitCostBdtMinor: Long,
        val newUnitCostBdtMinor: Long,
        val unitVarianceBdtMinor: Long,
        val totalRemainingQty: Int,
        val totalSoldQty: Int,
        val nonSellableOrPendingQty: Int,
        val inventoryAdjustmentBdtMinor: Long,
        val cogsAdjustmentBdtMinor: Long,
        val otherAdjustmentBdtMinor: Long
    )

    data class PostFinalizeAdjustmentResult(
        val adjustmentReference: String,
        val totalAddedExpenseBdtMinor: Long,
        val productAdjustments: List<ProductPostFinalizeAdjustment>,
        val totalInventoryAssetAdjustmentBdtMinor: Long,
        val totalCogsAdjustmentBdtMinor: Long,
        val totalOtherAdjustmentBdtMinor: Long
    )

    /**
     * Converts a foreign currency minor amount to BDT minor units using the manually entered exchange rate.
     */
    fun convertToBdtMinor(
        amountForeignMinor: Long,
        currency: String,
        exchangeRateToBdt: BigDecimal
    ): Long {
        require(amountForeignMinor >= 0L) { "Amount cannot be negative: $amountForeignMinor" }
        if (currency.equals("BDT", ignoreCase = true)) {
            return amountForeignMinor
        }
        require(exchangeRateToBdt > BigDecimal.ZERO) {
            "Exchange rate for $currency must be positive: $exchangeRateToBdt"
        }
        return BigDecimal(amountForeignMinor)
            .multiply(exchangeRateToBdt)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()
    }

    /**
     * Phase 3 Validation:
     * Packed Quantity = Good + Damaged + Missing + Pending.
     */
    fun validateReceivingQuantities(
        packedQuantity: Int,
        goodQuantity: Int,
        damagedQuantity: Int,
        missingQuantity: Int,
        pendingQuantity: Int
    ) {
        require(packedQuantity > 0) { "Packed quantity must be greater than zero: $packedQuantity" }
        require(goodQuantity >= 0) { "Good quantity cannot be negative: $goodQuantity" }
        require(damagedQuantity >= 0) { "Damaged quantity cannot be negative: $damagedQuantity" }
        require(missingQuantity >= 0) { "Missing quantity cannot be negative: $missingQuantity" }
        require(pendingQuantity >= 0) { "Pending quantity cannot be negative: $pendingQuantity" }
        val sum = goodQuantity + damagedQuantity + missingQuantity + pendingQuantity
        require(sum == packedQuantity) {
            "Receiving reconciliation mismatch: Packed ($packedQuantity) != Good ($goodQuantity) + Damaged ($damagedQuantity) + Missing ($missingQuantity) + Pending ($pendingQuantity) [Sum=$sum]"
        }
    }

    /**
     * Phase 2C: Aggregates carton-wise packaging list without assuming uniform carton weights or quantities.
     */
    fun summarizePackagingList(cartons: List<ShipmentCartonInput>): PackagingSummary {
        var totalQty = 0
        var totalWeight = BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP)
        var totalCbm = BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP)
        val distinctCartonNumbers = mutableSetOf<String>()

        val prodQty = mutableMapOf<String, Int>()
        val prodWeight = mutableMapOf<String, BigDecimal>()
        val prodCbm = mutableMapOf<String, BigDecimal>()
        val prodCartons = mutableMapOf<String, MutableSet<String>>()
        val catWeight = mutableMapOf<String, BigDecimal>()

        cartons.forEachIndexed { idx, c ->
            require(c.quantity > 0) { "Carton ${c.cartonNumber} quantity must be > 0" }
            require(c.grossWeightKg >= BigDecimal.ZERO) { "Carton ${c.cartonNumber} weight cannot be negative" }
            val cbmVal = c.effectiveCbm
            val cartonKey = c.cartonNumber.ifBlank { "CTN-${idx + 1}" }
            distinctCartonNumbers.add(cartonKey)

            totalQty += c.quantity
            totalWeight = totalWeight.add(c.grossWeightKg).setScale(3, RoundingMode.HALF_UP)
            totalCbm = totalCbm.add(cbmVal).setScale(6, RoundingMode.HALF_UP)

            prodQty[c.productId] = (prodQty[c.productId] ?: 0) + c.quantity
            prodWeight[c.productId] = (prodWeight[c.productId] ?: BigDecimal.ZERO)
                .add(c.grossWeightKg)
                .setScale(3, RoundingMode.HALF_UP)
            prodCbm[c.productId] = (prodCbm[c.productId] ?: BigDecimal.ZERO)
                .add(cbmVal)
                .setScale(6, RoundingMode.HALF_UP)
            prodCartons.getOrPut(c.productId) { mutableSetOf() }.add(cartonKey)

            catWeight[c.categoryId] = (catWeight[c.categoryId] ?: BigDecimal.ZERO)
                .add(c.grossWeightKg)
                .setScale(3, RoundingMode.HALF_UP)
        }

        return PackagingSummary(
            totalQuantity = totalQty,
            totalCartonCount = distinctCartonNumbers.size,
            totalGrossWeightKg = totalWeight,
            totalCbm = totalCbm,
            productQuantityMap = prodQty,
            productWeightMap = prodWeight,
            productCbmMap = prodCbm,
            productCartonCountMap = prodCartons.mapValues { it.value.size },
            categoryWeightMap = catWeight
        )
    }

    /**
     * Phase 2B: Allocates China Local Delivery Charge separately for each Supplier.
     * Default formula:
     * Product Delivery Allocation = Supplier Delivery Charge * (Product Purchase Value / Supplier Total Purchase Value)
     * Also supports optional manual product-wise allocation.
     * Returns pair of (productId -> foreignMinor, productId -> bdtMinor).
     */
    fun allocateSupplierChinaDelivery(
        supplierDelivery: SupplierChinaDeliveryInput,
        supplierProducts: List<ShipmentProductInput>
    ): Pair<Map<String, Long>, Map<String, Long>> {
        if (supplierProducts.isEmpty() || supplierDelivery.deliveryChargeForeignMinor == 0L) {
            val zeroMap = supplierProducts.associate { it.productId to 0L }
            return zeroMap to zeroMap
        }

        val totalForeignMinor = supplierDelivery.deliveryChargeForeignMinor
        val totalBdtMinor = supplierDelivery.deliveryChargeBdtMinor

        // Allocate in foreign currency first so foreign-currency breakdown (e.g. CNY 100 / CNY 200) is exact
        val foreignInputs = supplierProducts.map { prod ->
            val purchaseForeignMinor = prod.packedQuantity.toLong() * prod.unitPurchasePriceForeignMinor
            val manualForeign = supplierDelivery.manualProductAllocationsForeignMinor[prod.productId]
            AllocationItemInput(
                id = prod.productId,
                name = prod.productNameEn,
                quantity = prod.packedQuantity,
                purchaseValueBdtMinor = purchaseForeignMinor,
                manualAmountBdtMinor = if (supplierDelivery.allocationMethod == AllocationMethod.MANUAL) manualForeign else null
            )
        }

        val foreignAllocated = LandedCostAllocationEngine.allocate(
            totalCostBdtMinor = totalForeignMinor,
            items = foreignInputs,
            method = supplierDelivery.allocationMethod
        ).associate { it.itemId to it.allocatedBdtMinor }

        // Convert to BDT using exact reconciliation against totalBdtMinor
        val bdtInputs = supplierProducts.map { prod ->
            val fShare = foreignAllocated[prod.productId] ?: 0L
            AllocationItemInput(
                id = prod.productId,
                name = prod.productNameEn,
                quantity = prod.packedQuantity,
                purchaseValueBdtMinor = fShare
            )
        }
        val bdtAllocated = LandedCostAllocationEngine.allocate(
            totalCostBdtMinor = totalBdtMinor,
            items = bdtInputs,
            method = AllocationMethod.PURCHASE_VALUE
        ).associate { it.itemId to it.allocatedBdtMinor }

        return foreignAllocated to bdtAllocated
    }

    /**
     * Phase 2D: Two-level Category International Freight Calculation.
     * Formula:
     * Category Freight = Total Category Gross Weight * Applicable Rate/KG.
     * Allocates the resulting category freight to products in that category based on their actual gross weight.
     */
    fun calculateCategoryInternationalFreight(
        products: List<ShipmentProductInput>,
        productWeightsKg: Map<String, BigDecimal>,
        categoryRates: List<CategoryFreightRateInput>
    ): List<CategoryFreightResult> {
        val ratesByCategory = categoryRates.associateBy { it.categoryId }
        val productsByCategory = products.groupBy { it.categoryId }

        return productsByCategory.mapNotNull { (categoryId, catProducts) ->
            val rateInput = ratesByCategory[categoryId] ?: return@mapNotNull null
            val appliedRateMinor = rateInput.appliedRatePerKgBdtMinor
            if (appliedRateMinor <= 0L) {
                return@mapNotNull CategoryFreightResult(
                    categoryId = categoryId,
                    categoryNameEn = rateInput.categoryNameEn,
                    defaultRatePerKgBdtMinor = rateInput.defaultRatePerKgBdtMinor,
                    overrideRatePerKgBdtMinor = rateInput.overrideRatePerKgBdtMinor,
                    appliedRatePerKgBdtMinor = 0L,
                    totalCategoryGrossWeightKg = BigDecimal.ZERO,
                    totalCategoryFreightBdtMinor = 0L,
                    productAllocationsBdtMinor = catProducts.associate { it.productId to 0L }
                )
            }

            val totalCategoryWeight = catProducts.fold(BigDecimal.ZERO) { acc, p ->
                acc.add(productWeightsKg[p.productId] ?: p.fallbackGrossWeightKg)
            }.setScale(3, RoundingMode.HALF_UP)

            val totalCategoryFreightMinor = totalCategoryWeight
                .multiply(BigDecimal(appliedRateMinor))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact()

            val allocInputs = catProducts.map { p ->
                val weight = productWeightsKg[p.productId] ?: p.fallbackGrossWeightKg
                AllocationItemInput(
                    id = p.productId,
                    name = p.productNameEn,
                    quantity = p.packedQuantity,
                    grossWeightKg = weight
                )
            }

            val allocated = LandedCostAllocationEngine.allocate(
                totalCostBdtMinor = totalCategoryFreightMinor,
                items = allocInputs,
                method = AllocationMethod.GROSS_WEIGHT
            ).associate { it.itemId to it.allocatedBdtMinor }

            CategoryFreightResult(
                categoryId = categoryId,
                categoryNameEn = rateInput.categoryNameEn,
                defaultRatePerKgBdtMinor = rateInput.defaultRatePerKgBdtMinor,
                overrideRatePerKgBdtMinor = rateInput.overrideRatePerKgBdtMinor,
                appliedRatePerKgBdtMinor = appliedRateMinor,
                totalCategoryGrossWeightKg = totalCategoryWeight,
                totalCategoryFreightBdtMinor = totalCategoryFreightMinor,
                productAllocationsBdtMinor = allocated
            )
        }
    }

    /**
     * Master Shipment Landing Cost Calculation (Phase 4).
     */
    fun calculateShipmentLandingCost(
        products: List<ShipmentProductInput>,
        cartons: List<ShipmentCartonInput>,
        supplierChinaDeliveries: List<SupplierChinaDeliveryInput>,
        categoryFreightRates: List<CategoryFreightRateInput>,
        expenses: List<ImportExpenseInput>
    ): ShipmentLandingCostResult {
        require(products.isNotEmpty()) { "Shipment must contain at least one product" }

        // 1. Validate receiving quantities for every product
        products.forEach { p ->
            validateReceivingQuantities(
                packedQuantity = p.packedQuantity,
                goodQuantity = p.goodQuantity,
                damagedQuantity = p.damagedQuantity,
                missingQuantity = p.missingQuantity,
                pendingQuantity = p.pendingQuantity
            )
        }

        // 2. Summarize carton packaging list
        val packagingSummary = summarizePackagingList(cartons)

        // Resolve effective weight, CBM, and carton count per product
        val effectiveWeightMap = products.associate { p ->
            val w = packagingSummary.productWeightMap[p.productId]
            p.productId to (if (w != null && w > BigDecimal.ZERO) w else p.fallbackGrossWeightKg)
        }
        val effectiveCbmMap = products.associate { p ->
            val c = packagingSummary.productCbmMap[p.productId]
            p.productId to (if (c != null && c > BigDecimal.ZERO) c else p.fallbackCbm)
        }
        val effectiveCartonCountMap = products.associate { p ->
            val cnt = packagingSummary.productCartonCountMap[p.productId] ?: 0
            p.productId to (if (cnt > 0) cnt else p.fallbackCartonCount)
        }

        // 3. Calculate Product Purchase Cost in Foreign & BDT Minor
        val purchaseForeignByProduct = products.associate { p ->
            p.productId to (p.packedQuantity.toLong() * p.unitPurchasePriceForeignMinor)
        }
        val purchaseBdtByProduct = products.associate { p ->
            val foreignTotal = purchaseForeignByProduct[p.productId] ?: 0L
            p.productId to convertToBdtMinor(foreignTotal, p.purchaseCurrency, p.purchaseExchangeRateToBdt)
        }

        // 4. Allocate Supplier-wise China Local Delivery
        val chinaForeignByProduct = mutableMapOf<String, Long>()
        val chinaBdtByProduct = mutableMapOf<String, Long>()
        val deliveriesBySupplier = supplierChinaDeliveries.groupBy { it.supplierId }
        val productsBySupplier = products.groupBy { it.supplierId }

        for ((supplierId, supProducts) in productsBySupplier) {
            val supDeliveries = deliveriesBySupplier[supplierId].orEmpty()
            for (delivery in supDeliveries) {
                val (fMap, bMap) = allocateSupplierChinaDelivery(delivery, supProducts)
                fMap.forEach { (pid, amt) -> chinaForeignByProduct[pid] = (chinaForeignByProduct[pid] ?: 0L) + amt }
                bMap.forEach { (pid, amt) -> chinaBdtByProduct[pid] = (chinaBdtByProduct[pid] ?: 0L) + amt }
            }
        }

        // 5. Calculate Category-wise International Freight
        val categoryFreightResults = calculateCategoryInternationalFreight(
            products = products,
            productWeightsKg = effectiveWeightMap,
            categoryRates = categoryFreightRates
        )
        val freightBdtByProduct = mutableMapOf<String, Long>()
        categoryFreightResults.forEach { res ->
            res.productAllocationsBdtMinor.forEach { (pid, amt) ->
                freightBdtByProduct[pid] = (freightBdtByProduct[pid] ?: 0L) + amt
            }
        }

        // 6. Process Additional Import Expenses (BD Transport, Customs, C&F, Port, etc.)
        // Guard against duplicate expense IDs!
        val seenExpenseIds = mutableSetOf<String>()
        val bdTransportBdtByProduct = mutableMapOf<String, Long>()
        val otherExpensesBdtByProduct = mutableMapOf<String, Long>()
        val expenseAllocationsByExpenseId = mutableMapOf<String, List<AllocationResultItem>>()

        for (expense in expenses) {
            require(seenExpenseIds.add(expense.expenseId)) {
                "Duplicate expense allocation detected for expenseId: ${expense.expenseId} (${expense.expenseName})"
            }
            // Skip PRODUCT_PURCHASE as purchase value is kept strictly separate from import expenses
            if (expense.category == CostCategory.PRODUCT_PURCHASE) continue

            val targetProducts = if (expense.supplierId != null) {
                products.filter { it.supplierId == expense.supplierId }.ifEmpty { products }
            } else {
                products
            }

            val expenseBdt = expense.amountBdtMinor
            val allocInputs = targetProducts.map { p ->
                val manualAmt = if (expense.allocationMethod == AllocationMethod.MANUAL) {
                    expense.manualProductAllocationsBdtMinor[p.productId] ?: 0L
                } else null
                AllocationItemInput(
                    id = p.productId,
                    name = p.productNameEn,
                    quantity = p.packedQuantity,
                    grossWeightKg = effectiveWeightMap[p.productId] ?: BigDecimal.ZERO,
                    cbm = effectiveCbmMap[p.productId] ?: BigDecimal.ZERO,
                    cartonCount = effectiveCartonCountMap[p.productId] ?: 0,
                    purchaseValueBdtMinor = purchaseBdtByProduct[p.productId] ?: 0L,
                    manualAmountBdtMinor = manualAmt
                )
            }

            val allocatedItems = LandedCostAllocationEngine.allocate(
                totalCostBdtMinor = expenseBdt,
                items = allocInputs,
                method = expense.allocationMethod
            )
            expenseAllocationsByExpenseId[expense.expenseId] = allocatedItems

            for (alloc in allocatedItems) {
                when (expense.category) {
                    CostCategory.CHINA_DOMESTIC_TRANSPORT -> {
                        chinaBdtByProduct[alloc.itemId] = (chinaBdtByProduct[alloc.itemId] ?: 0L) + alloc.allocatedBdtMinor
                    }
                    CostCategory.INTERNATIONAL_FREIGHT,
                    CostCategory.SEA_FREIGHT,
                    CostCategory.AIR_FREIGHT -> {
                        // Only add if category freight wasn't already covering this or if entered as explicit freight cost
                        freightBdtByProduct[alloc.itemId] = (freightBdtByProduct[alloc.itemId] ?: 0L) + alloc.allocatedBdtMinor
                    }
                    CostCategory.LOCAL_TRANSPORTATION,
                    CostCategory.BD_LOCAL_TRANSPORT -> {
                        bdTransportBdtByProduct[alloc.itemId] = (bdTransportBdtByProduct[alloc.itemId] ?: 0L) + alloc.allocatedBdtMinor
                    }
                    else -> {
                        otherExpensesBdtByProduct[alloc.itemId] = (otherExpensesBdtByProduct[alloc.itemId] ?: 0L) + alloc.allocatedBdtMinor
                    }
                }
            }
        }

        // 7. Build per-product breakdown & quantity outcome valuations
        val breakdowns = products.map { p ->
            val pid = p.productId
            val purchaseForeign = purchaseForeignByProduct[pid] ?: 0L
            val purchaseBdt = purchaseBdtByProduct[pid] ?: 0L
            val chinaForeign = chinaForeignByProduct[pid] ?: 0L
            val chinaBdt = chinaBdtByProduct[pid] ?: 0L
            val freightBdt = freightBdtByProduct[pid] ?: 0L
            val bdTransportBdt = bdTransportBdtByProduct[pid] ?: 0L
            val otherExpBdt = otherExpensesBdtByProduct[pid] ?: 0L

            val totalAllocatedExpBdt = chinaBdt + freightBdt + bdTransportBdt + otherExpBdt
            val totalImportCostBdt = purchaseBdt + totalAllocatedExpBdt

            // Unit Import Cost = Total Allocated Product Import Cost / Packed Quantity
            val unitImportCostBdt = BigDecimal(totalImportCostBdt)
                .divide(BigDecimal(p.packedQuantity), 0, RoundingMode.HALF_UP)
                .longValueExact()

            // Calculate quantity outcome valuations consistent with unitImportCostBdt
            // Reconcile any rounding remainder onto the largest bucket so accounting balances to the exact paisa
            val rawGoodVal = p.goodQuantity.toLong() * unitImportCostBdt
            val rawDamagedVal = p.damagedQuantity.toLong() * unitImportCostBdt
            val rawMissingVal = p.missingQuantity.toLong() * unitImportCostBdt
            val rawPendingVal = p.pendingQuantity.toLong() * unitImportCostBdt
            val rawSum = rawGoodVal + rawDamagedVal + rawMissingVal + rawPendingVal
            val diff = totalImportCostBdt - rawSum

            var goodVal = rawGoodVal
            var damagedVal = rawDamagedVal
            var missingVal = rawMissingVal
            var pendingVal = rawPendingVal

            if (diff != 0L) {
                when {
                    p.goodQuantity > 0 -> goodVal += diff
                    p.pendingQuantity > 0 -> pendingVal += diff
                    p.damagedQuantity > 0 -> damagedVal += diff
                    else -> missingVal += diff
                }
            }

            ProductLandingCostBreakdown(
                productId = pid,
                variantId = p.variantId,
                productNameEn = p.productNameEn,
                productNameBn = p.productNameBn,
                sku = p.sku,
                supplierId = p.supplierId,
                categoryId = p.categoryId,
                packedQuantity = p.packedQuantity,
                goodQuantity = p.goodQuantity,
                damagedQuantity = p.damagedQuantity,
                missingQuantity = p.missingQuantity,
                pendingQuantity = p.pendingQuantity,
                grossWeightKg = effectiveWeightMap[pid] ?: BigDecimal.ZERO,
                cbm = effectiveCbmMap[pid] ?: BigDecimal.ZERO,
                cartonCount = effectiveCartonCountMap[pid] ?: 0,
                purchaseCostForeignMinor = purchaseForeign,
                purchaseCurrency = p.purchaseCurrency,
                purchaseExchangeRateToBdt = p.purchaseExchangeRateToBdt,
                purchaseCostBdtMinor = purchaseBdt,
                chinaDeliveryForeignMinor = chinaForeign,
                chinaDeliveryBdtMinor = chinaBdt,
                internationalFreightBdtMinor = freightBdt,
                bdTransportBdtMinor = bdTransportBdt,
                otherImportExpensesBdtMinor = otherExpBdt,
                totalAllocatedExpensesBdtMinor = totalAllocatedExpBdt,
                totalProductImportCostBdtMinor = totalImportCostBdt,
                unitImportCostBdtMinor = unitImportCostBdt,
                goodInventoryValuationBdtMinor = goodVal,
                damagedWriteOffValuationBdtMinor = damagedVal,
                missingShortageValuationBdtMinor = missingVal,
                pendingInTransitValuationBdtMinor = pendingVal
            )
        }

        val totalPurchaseBdt = breakdowns.sumOf { it.purchaseCostBdtMinor }
        val totalChinaBdt = breakdowns.sumOf { it.chinaDeliveryBdtMinor }
        val totalFreightBdt = breakdowns.sumOf { it.internationalFreightBdtMinor }
        val totalBdTransportBdt = breakdowns.sumOf { it.bdTransportBdtMinor }
        val totalOtherBdt = breakdowns.sumOf { it.otherImportExpensesBdtMinor }
        val totalExpensesBdt = totalChinaBdt + totalFreightBdt + totalBdTransportBdt + totalOtherBdt

        return ShipmentLandingCostResult(
            packagingSummary = packagingSummary,
            categoryFreightResults = categoryFreightResults,
            productBreakdowns = breakdowns,
            expenseAllocationsByExpenseId = expenseAllocationsByExpenseId,
            totalPurchaseCostBdtMinor = totalPurchaseBdt,
            totalChinaDeliveryBdtMinor = totalChinaBdt,
            totalInternationalFreightBdtMinor = totalFreightBdt,
            totalBdTransportBdtMinor = totalBdTransportBdt,
            totalOtherImportExpensesBdtMinor = totalOtherBdt,
            totalImportExpensesBdtMinor = totalExpensesBdt,
            grandTotalLandingCostBdtMinor = totalPurchaseBdt + totalExpensesBdt,
            totalGoodInventoryAssetBdtMinor = breakdowns.sumOf { it.goodInventoryValuationBdtMinor },
            totalDamagedWriteOffBdtMinor = breakdowns.sumOf { it.damagedWriteOffValuationBdtMinor },
            totalMissingShortageBdtMinor = breakdowns.sumOf { it.missingShortageValuationBdtMinor },
            totalPendingInTransitBdtMinor = breakdowns.sumOf { it.pendingInTransitValuationBdtMinor }
        )
    }

    /**
     * Phase 5: Post-Finalize Cost Adjustment Calculation.
     * Preserves original finalized snapshot while allocating new post-finalization expenses to products,
     * updating remaining FIFO inventory valuation (1210) and adjusting COGS (5010) for sold quantities.
     */
    fun calculatePostFinalizeAdjustment(
        adjustmentReference: String,
        existingBreakdowns: List<ProductLandingCostBreakdown>,
        fifoLayersByProduct: Map<String, List<FifoLayerAdjustmentInput>>,
        newPostFinalizeExpenses: List<ImportExpenseInput>
    ): PostFinalizeAdjustmentResult {
        require(existingBreakdowns.isNotEmpty()) { "Cannot adjust empty product breakdown list" }
        val uncapitalizedExpenses = newPostFinalizeExpenses.filter { !it.isAlreadyCapitalized }
        require(uncapitalizedExpenses.isNotEmpty()) {
            "No new uncapitalized post-finalization expenses to allocate"
        }

        val seenIds = mutableSetOf<String>()
        val addedCostByProduct = mutableMapOf<String, Long>()

        for (exp in uncapitalizedExpenses) {
            require(seenIds.add(exp.expenseId)) {
                "Duplicate post-finalize expense ID: ${exp.expenseId}"
            }
            val targetBreakdowns = if (exp.supplierId != null) {
                existingBreakdowns.filter { it.supplierId == exp.supplierId }.ifEmpty { existingBreakdowns }
            } else {
                existingBreakdowns
            }

            val allocInputs = targetBreakdowns.map { b ->
                val manualAmt = if (exp.allocationMethod == AllocationMethod.MANUAL) {
                    exp.manualProductAllocationsBdtMinor[b.productId] ?: 0L
                } else null
                AllocationItemInput(
                    id = b.productId,
                    name = b.productNameEn,
                    quantity = b.packedQuantity,
                    grossWeightKg = b.grossWeightKg,
                    cbm = b.cbm,
                    cartonCount = b.cartonCount,
                    purchaseValueBdtMinor = b.purchaseCostBdtMinor,
                    manualAmountBdtMinor = manualAmt
                )
            }

            val allocated = LandedCostAllocationEngine.allocate(
                totalCostBdtMinor = exp.amountBdtMinor,
                items = allocInputs,
                method = exp.allocationMethod
            )
            allocated.forEach { item ->
                addedCostByProduct[item.itemId] = (addedCostByProduct[item.itemId] ?: 0L) + item.allocatedBdtMinor
            }
        }

        val productAdjustments = existingBreakdowns.mapNotNull { b ->
            val addedCost = addedCostByProduct[b.productId] ?: 0L
            if (addedCost == 0L) return@mapNotNull null

            val layers = fifoLayersByProduct[b.productId].orEmpty()
            val totalRemainingQty = if (layers.isNotEmpty()) {
                layers.sumOf { it.remainingQuantity }
            } else {
                b.goodQuantity
            }
            val totalSoldQty = if (layers.isNotEmpty()) {
                layers.sumOf { it.soldQuantity }
            } else {
                0
            }
            val nonSellableOrPendingQty = maxOf(0, b.packedQuantity - totalRemainingQty - totalSoldQty)

            val newTotalProductCost = b.totalProductImportCostBdtMinor + addedCost
            val newUnitCost = BigDecimal(newTotalProductCost)
                .divide(BigDecimal(b.packedQuantity), 0, RoundingMode.HALF_UP)
                .longValueExact()
            val unitVariance = newUnitCost - b.unitImportCostBdtMinor

            // Proportional split across Remaining Inventory, Sold COGS, and Non-sellable/Pending
            val splitInputs = listOf(
                AllocationItemInput(id = "INVENTORY", name = "Remaining Inventory", quantity = totalRemainingQty),
                AllocationItemInput(id = "COGS", name = "Sold COGS", quantity = totalSoldQty),
                AllocationItemInput(id = "OTHER", name = "Damaged/Missing/Pending", quantity = nonSellableOrPendingQty)
            )
            val splitResult = LandedCostAllocationEngine.allocate(
                totalCostBdtMinor = addedCost,
                items = splitInputs,
                method = AllocationMethod.QUANTITY
            ).associate { it.itemId to it.allocatedBdtMinor }

            ProductPostFinalizeAdjustment(
                productId = b.productId,
                variantId = b.variantId,
                packedQuantity = b.packedQuantity,
                addedCostBdtMinor = addedCost,
                oldUnitCostBdtMinor = b.unitImportCostBdtMinor,
                newUnitCostBdtMinor = newUnitCost,
                unitVarianceBdtMinor = unitVariance,
                totalRemainingQty = totalRemainingQty,
                totalSoldQty = totalSoldQty,
                nonSellableOrPendingQty = nonSellableOrPendingQty,
                inventoryAdjustmentBdtMinor = splitResult["INVENTORY"] ?: 0L,
                cogsAdjustmentBdtMinor = splitResult["COGS"] ?: 0L,
                otherAdjustmentBdtMinor = splitResult["OTHER"] ?: 0L
            )
        }

        return PostFinalizeAdjustmentResult(
            adjustmentReference = adjustmentReference,
            totalAddedExpenseBdtMinor = productAdjustments.sumOf { it.addedCostBdtMinor },
            productAdjustments = productAdjustments,
            totalInventoryAssetAdjustmentBdtMinor = productAdjustments.sumOf { it.inventoryAdjustmentBdtMinor },
            totalCogsAdjustmentBdtMinor = productAdjustments.sumOf { it.cogsAdjustmentBdtMinor },
            totalOtherAdjustmentBdtMinor = productAdjustments.sumOf { it.otherAdjustmentBdtMinor }
        )
    }
}
