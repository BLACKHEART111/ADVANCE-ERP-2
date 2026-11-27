package com.pocketdev.advance_erp.feature.sales

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.pocketdev.advance_erp.core.designsystem.ERPFullScreenEditor
import com.pocketdev.advance_erp.core.designsystem.LocalIsDarkTheme
import com.pocketdev.advance_erp.core.designsystem.StatusError
import com.pocketdev.advance_erp.core.designsystem.TextSecondary
import com.pocketdev.advance_erp.core.engine.LineCalculation
import com.pocketdev.advance_erp.core.engine.SalesInvoiceCalculator
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.model.BangladeshBanks
import com.pocketdev.advance_erp.core.security.SecurityManager
import com.pocketdev.advance_erp.core.security.UserRole
import com.pocketdev.advance_erp.core.security.rememberBiometricCostVisibility
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.core.util.firstInStockProduct
import com.pocketdev.advance_erp.core.util.firstInStockVariant
import com.pocketdev.advance_erp.core.util.resolveUnitCostMinorForSale
import com.pocketdev.advance_erp.core.util.sellableStockForProduct
import com.pocketdev.advance_erp.core.util.sellableStockForSelection
import com.pocketdev.advance_erp.core.util.sellableStockForVariant
import com.pocketdev.advance_erp.data.entity.ChartOfAccount
import com.pocketdev.advance_erp.data.entity.Customer
import com.pocketdev.advance_erp.data.entity.InventoryCostLayer
import com.pocketdev.advance_erp.data.entity.LiquidAccountKind
import com.pocketdev.advance_erp.data.entity.Product
import com.pocketdev.advance_erp.data.entity.ProductVariant
import com.pocketdev.advance_erp.data.entity.StockBalance
import com.pocketdev.advance_erp.data.entity.Warehouse
import com.pocketdev.advance_erp.domain.repository.CustomerSummary
import com.pocketdev.advance_erp.domain.repository.InvoiceItemDraft
import com.pocketdev.advance_erp.domain.repository.NewCustomerDraft
import com.pocketdev.advance_erp.domain.repository.SalesInvoiceDraft
import com.pocketdev.advance_erp.feature.inventory.BarcodeQrScannerDialog
import java.util.Locale
import java.util.UUID

class ItemDraftState(
    productId: String,
    variantId: String? = null,
    quantityStr: String = "1",
    unitPriceStr: String = "0.00",
    discountStr: String = "0.00",
    isPercentageDiscount: Boolean = true,
    isExpanded: Boolean = true,
    val uid: String = UUID.randomUUID().toString()
) {
    var productId by mutableStateOf(productId)
    var variantId by mutableStateOf(variantId)
    var quantityStr by mutableStateOf(quantityStr)
    var unitPriceStr by mutableStateOf(unitPriceStr)
    var discountStr by mutableStateOf(discountStr)
    var isPercentageDiscount by mutableStateOf(isPercentageDiscount)
    var isExpanded by mutableStateOf(isExpanded)
}

@Composable
fun CreateInvoiceDialog(
    customers: List<Customer>,
    warehouses: List<Warehouse>,
    products: List<Product>,
    variants: List<ProductVariant> = emptyList(),
    stockBalances: List<StockBalance> = emptyList(),
    costLayers: List<InventoryCostLayer> = emptyList(),
    liquidAccounts: List<ChartOfAccount> = emptyList(),
    productImages: Map<String, String> = emptyMap(),
    variantImages: Map<String, String> = emptyMap(),
    userRole: UserRole = UserRole.OWNER_ADMIN,
    customerSummary: CustomerSummary? = null,
    isBangla: Boolean,
    onDismiss: () -> Unit,
    onCustomerSelected: (String) -> Unit = {},
    onCreateCustomerImmediate: ((NewCustomerDraft, onCreated: (Customer) -> Unit, onError: (String) -> Unit) -> Unit)? = null,
    onSubmit: (SalesInvoiceDraft) -> Unit = {},
    onSubmitDraft: (SalesInvoiceDraft) -> Unit = onSubmit,
    onConfirmSale: (SalesInvoiceDraft) -> Unit = onSubmit
) {
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current
    val palette = rememberSalesInvoicePalette()

    val canViewCost = SecurityManager.canViewCost(userRole)
    val canViewProfit = SecurityManager.canViewProfit(userRole)
    val canViewAnyCostOrProfit = canViewCost || canViewProfit

    var selectedCustomerId by rememberSaveable { mutableStateOf(customers.firstOrNull()?.id ?: "") }
    var pendingNewCustomerDraft by remember { mutableStateOf<NewCustomerDraft?>(null) }
    var showCustomerLookup by rememberSaveable { mutableStateOf(false) }

    var selectedWarehouseId by rememberSaveable { mutableStateOf(warehouses.firstOrNull()?.id ?: "") }

    var invoiceDiscountStr by rememberSaveable { mutableStateOf("0.00") }
    var isInvoiceDiscountPercentage by rememberSaveable { mutableStateOf(true) }
    var additionalChargesStr by rememberSaveable { mutableStateOf("0.00") }
    var paidAmountStr by rememberSaveable { mutableStateOf("0.00") }
    var isFullPaymentSelected by rememberSaveable { mutableStateOf(false) }
    var paymentMethod by rememberSaveable { mutableStateOf("CASH") }
    var selectedPaymentAccountId by rememberSaveable {
        mutableStateOf(
            liquidAccounts.firstOrNull { it.liquidKind == LiquidAccountKind.CASH }?.id ?: "coa-1010"
        )
    }
    var notes by rememberSaveable { mutableStateOf("") }
    val costProfitVisibility = rememberBiometricCostVisibility(
        key = "sales-invoice-cost-profit",
        allowed = canViewAnyCostOrProfit,
        isBangla = isBangla,
        promptTitle = if (isBangla) "ক্রয়মূল্য ও লাভ দেখুন" else "View Cost & Profit",
        promptSubtitle = if (isBangla) "ক্রয়মূল্য ও লাভ দেখতে ফিঙ্গারপ্রিন্ট দিয়ে যাচাই করুন" else "Verify your fingerprint to view purchase price & profit"
    )
    val showCostAndProfit = costProfitVisibility.isVisible
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var showItemSearchBar by rememberSaveable { mutableStateOf(false) }
    var itemSearchQuery by rememberSaveable { mutableStateOf("") }
    var showAddItemPicker by rememberSaveable { mutableStateOf(false) }
    var pickingForItemIndex by remember { mutableStateOf<Int?>(null) }
    var showBarcodeScanner by rememberSaveable { mutableStateOf(false) }

    // Option C Credit Limit Control State
    var showCreditLimitWarningDialog by rememberSaveable { mutableStateOf(false) }
    var creditLimitApproved by rememberSaveable { mutableStateOf(false) }
    var creditApproverName by rememberSaveable { mutableStateOf("Manager") }
    var creditApprovalReason by rememberSaveable { mutableStateOf("Authorized credit override") }

    LaunchedEffect(selectedCustomerId) {
        if (selectedCustomerId.isNotBlank()) {
            onCustomerSelected(selectedCustomerId)
        }
    }

    val variantsByProduct = remember(variants) { variants.groupBy { it.productId } }
    val productsMap = remember(products) { products.associateBy { it.id } }

    fun createDefaultItemDraft(warehouseId: String): ItemDraftState {
        val p = firstInStockProduct(stockBalances, warehouseId, products, variantsByProduct)
            ?: return ItemDraftState(productId = "", variantId = null, quantityStr = "1", unitPriceStr = "0.00", isExpanded = true)
        val pVars = variantsByProduct[p.id].orEmpty()
        val firstV = if (pVars.isNotEmpty()) firstInStockVariant(stockBalances, warehouseId, p.id, pVars) else null
        val initialPriceMinor = firstV?.variantPriceMinor ?: p.sellingPriceMinor
        return ItemDraftState(
            productId = p.id,
            variantId = firstV?.id,
            quantityStr = "1",
            unitPriceStr = SalesInvoiceCalculator.formatMajorEditable(initialPriceMinor),
            isExpanded = true
        )
    }

    val itemsList = remember {
        mutableStateListOf<ItemDraftState>().apply {
            if (products.isNotEmpty()) {
                add(createDefaultItemDraft(selectedWarehouseId))
            }
        }
    }

    LaunchedEffect(selectedWarehouseId, stockBalances, products, variants, warehouses) {
        if (selectedWarehouseId.isBlank() && warehouses.isNotEmpty()) {
            selectedWarehouseId = warehouses.first().id
        }
        val whId = selectedWarehouseId
        if (whId.isBlank()) return@LaunchedEffect
        itemsList.forEach { itemDraft ->
            if (itemDraft.productId.isBlank()) return@forEach
            val pVars = variantsByProduct[itemDraft.productId].orEmpty()
            if (sellableStockForProduct(stockBalances, whId, itemDraft.productId, pVars) <= 0) {
                itemDraft.productId = ""
                itemDraft.variantId = null
                itemDraft.unitPriceStr = "0.00"
            } else if (pVars.isNotEmpty()) {
                val curVarId = itemDraft.variantId
                val curVarInStock = curVarId != null &&
                    sellableStockForVariant(stockBalances, whId, itemDraft.productId, curVarId) > 0
                if (!curVarInStock) {
                    val nextV = firstInStockVariant(stockBalances, whId, itemDraft.productId, pVars)
                    itemDraft.variantId = nextV?.id
                    if (nextV != null) {
                        itemDraft.unitPriceStr = SalesInvoiceCalculator.formatMajorEditable(nextV.variantPriceMinor)
                    }
                }
            }
        }
    }

    val selectedCustomer = customers.find { it.id == selectedCustomerId }
    val selectedWarehouse = warehouses.find { it.id == selectedWarehouseId }

    fun getSellableStock(productId: String, variantId: String?): Int =
        sellableStockForSelection(
            stockBalances = stockBalances,
            warehouseId = selectedWarehouseId,
            productId = productId,
            variantId = variantId,
            productVariants = variantsByProduct[productId].orEmpty()
        )

    fun resolveItemUnitCostMinor(product: Product?, variant: ProductVariant?): Long {
        if (product == null) return 0L
        return resolveUnitCostMinorForSale(
            layers = costLayers,
            productId = product.id,
            variantId = variant?.id,
            warehouseId = selectedWarehouseId,
            variantCostMinor = variant?.variantCostMinor,
            productPurchasePriceMinor = product.purchasePriceMinor
        )
    }

    // Live per-line & invoice totals
    val lineCalculations: List<LineCalculation> = itemsList.map { item ->
        val prod = productsMap[item.productId]
        val prodVariants = variantsByProduct[item.productId].orEmpty()
        val curVar = prodVariants.find { it.id == item.variantId }
        val unitCostMinor = resolveItemUnitCostMinor(prod, curVar)
        val qty = Formatters.normalizeAsciiNumberInput(item.quantityStr).toIntOrNull() ?: 0
        val priceMinor = SalesInvoiceCalculator.parseMajorToMinor(item.unitPriceStr)
        SalesInvoiceCalculator.computeLineAmounts(
            quantity = qty,
            unitPriceMinor = priceMinor,
            discountInput = item.discountStr,
            isPercentDiscount = item.isPercentageDiscount,
            unitCostMinor = unitCostMinor
        )
    }

    val totals = SalesInvoiceCalculator.computeInvoiceTotals(
        lines = lineCalculations,
        invoiceDiscountInput = invoiceDiscountStr,
        isInvoicePercentDiscount = isInvoiceDiscountPercentage,
        additionalChargesInput = additionalChargesStr,
        paidAmountInput = paidAmountStr,
        isFullPay = isFullPaymentSelected,
        isCreditMethod = paymentMethod == "CREDIT"
    )

    fun handleScannedCode(rawCode: String) {
        val code = rawCode.trim()
        if (code.isBlank()) return
        val whId = selectedWarehouseId
        val matchedVariant = variants.find {
            it.barcode.equals(code, ignoreCase = true) ||
                it.qrCode.equals(code, ignoreCase = true) ||
                it.sku.equals(code, ignoreCase = true)
        }
        if (matchedVariant != null) {
            val prod = productsMap[matchedVariant.productId]
            if (prod != null) {
                val stock = sellableStockForVariant(stockBalances, whId, prod.id, matchedVariant.id)
                if (stock <= 0) {
                    errorMessage = if (isBangla) "স্ক্যান করা ভেরিয়েন্টটি স্টকে নেই" else "Scanned variant is out of stock"
                    return
                }
                val existing = itemsList.find { it.productId == prod.id && it.variantId == matchedVariant.id }
                if (existing != null) {
                    val curQty = Formatters.normalizeAsciiNumberInput(existing.quantityStr).toIntOrNull() ?: 0
                    existing.quantityStr = (curQty + 1).toString()
                    existing.isExpanded = true
                } else {
                    itemsList.add(
                        ItemDraftState(
                            productId = prod.id,
                            variantId = matchedVariant.id,
                            quantityStr = "1",
                            unitPriceStr = SalesInvoiceCalculator.formatMajorEditable(matchedVariant.variantPriceMinor),
                            isExpanded = true
                        )
                    )
                }
                errorMessage = null
                Toast.makeText(context, if (isBangla) "যোগ হয়েছে: ${prod.nameEn}" else "Added: ${prod.nameEn}", Toast.LENGTH_SHORT).show()
                return
            }
        }
        val matchedProduct = products.find {
            it.barcode.equals(code, ignoreCase = true) ||
                it.qrCode.equals(code, ignoreCase = true) ||
                it.baseSku.equals(code, ignoreCase = true)
        }
        if (matchedProduct != null) {
            val pVars = variantsByProduct[matchedProduct.id].orEmpty()
            val prodStock = sellableStockForProduct(stockBalances, whId, matchedProduct.id, pVars)
            if (prodStock <= 0) {
                errorMessage = if (isBangla) "স্ক্যান করা পণ্যটি স্টকে নেই" else "Scanned product is out of stock"
                return
            }
            val firstV = if (pVars.isNotEmpty()) firstInStockVariant(stockBalances, whId, matchedProduct.id, pVars) else null
            val existing = itemsList.find { it.productId == matchedProduct.id && it.variantId == firstV?.id }
            if (existing != null) {
                val curQty = Formatters.normalizeAsciiNumberInput(existing.quantityStr).toIntOrNull() ?: 0
                existing.quantityStr = (curQty + 1).toString()
                existing.isExpanded = true
            } else {
                val priceMinor = firstV?.variantPriceMinor ?: matchedProduct.sellingPriceMinor
                itemsList.add(
                    ItemDraftState(
                        productId = matchedProduct.id,
                        variantId = firstV?.id,
                        quantityStr = "1",
                        unitPriceStr = SalesInvoiceCalculator.formatMajorEditable(priceMinor),
                        isExpanded = true
                    )
                )
            }
            errorMessage = null
            Toast.makeText(context, if (isBangla) "যোগ হয়েছে: ${matchedProduct.nameEn}" else "Added: ${matchedProduct.nameEn}", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, if (isBangla) "কোড পাওয়া যায়নি: $code" else "Product not found for code: $code", Toast.LENGTH_SHORT).show()
        }
    }

    fun buildInvoiceDraft(): SalesInvoiceDraft? {
        if (selectedCustomerId.isBlank() && pendingNewCustomerDraft == null) {
            errorMessage = if (isBangla) "অনুগ্রহ করে একজন গ্রাহক নির্বাচন করুন অথবা নতুন গ্রাহক যোগ করুন" else "Please select or create a customer"
            return null
        }
        if (selectedWarehouseId.isBlank()) {
            errorMessage = if (isBangla) "অনুগ্রহ করে ওয়্যারহাউস নির্বাচন করুন" else "Please select a warehouse"
            return null
        }
        if (itemsList.isEmpty()) {
            errorMessage = if (isBangla) "কমপক্ষে একটি পণ্য যোগ করুন" else "Please add at least one item"
            return null
        }
        if (itemsList.any { it.productId.isBlank() }) {
            errorMessage = if (isBangla) "অনুগ্রহ করে স্টকে থাকা পণ্য নির্বাচন করুন" else "Please select an in-stock product for every row"
            return null
        }

        val draftItems = itemsList.mapIndexedNotNull { idx, item ->
            val calc = lineCalculations[idx]
            if (calc.quantity > 0) {
                InvoiceItemDraft(
                    productId = item.productId,
                    variantId = item.variantId,
                    quantity = calc.quantity,
                    unitPriceMinor = calc.unitPriceMinor,
                    discountMinor = calc.discountMinor,
                    discountPercentBps = if (item.isPercentageDiscount && calc.discountMinor > 0L) {
                        SalesInvoiceCalculator.parsePercentBasisPoints(item.discountStr).takeIf { it > 0L }
                    } else null
                )
            } else null
        }

        if (draftItems.size != itemsList.size || draftItems.isEmpty()) {
            errorMessage = if (isBangla) "সব পণ্যের সঠিক পরিমাণ (> ০) দিন" else "All items must have quantity > 0"
            return null
        }

        // Validate aggregated requested qty per product/variant against warehouse sellable stock
        for ((selection, grouped) in draftItems.groupBy { it.productId to it.variantId }) {
            val stock = getSellableStock(selection.first, selection.second)
            val totalReq = grouped.sumOf { it.quantity }
            if (stock <= 0 || totalReq > stock) {
                val pName = productsMap[selection.first]?.nameEn ?: "Product"
                errorMessage = if (isBangla) {
                    "'$pName'-এর জন্য পর্যাপ্ত স্টক নেই (মজুদ: $stock, চাওয়া হয়েছে: $totalReq)"
                } else {
                    "Insufficient stock for '$pName' (Available: $stock, Requested: $totalReq)"
                }
                return null
            }
        }

        val resolvedAccount = liquidAccounts.find { it.id == selectedPaymentAccountId }
        val resolvedAccountId = when (paymentMethod) {
            "CASH" -> resolvedAccount?.takeIf { it.liquidKind == LiquidAccountKind.CASH }?.id
                ?: liquidAccounts.firstOrNull { it.liquidKind == LiquidAccountKind.CASH }?.id
                ?: "coa-1010"
            "BANK" -> resolvedAccount?.takeIf { it.liquidKind == LiquidAccountKind.BANK }?.id
                ?: liquidAccounts.firstOrNull { it.liquidKind == LiquidAccountKind.BANK }?.id
                ?: "coa-1020"
            "MFS" -> resolvedAccount?.takeIf { it.liquidKind == LiquidAccountKind.MOBILE_BANKING }?.id
                ?: liquidAccounts.firstOrNull { it.liquidKind == LiquidAccountKind.MOBILE_BANKING }?.id
                ?: "coa-1060"
            else -> "coa-1010"
        }
        val resolvedPaymentMethod = if (paymentMethod == "MFS") {
            liquidAccounts.find { it.id == resolvedAccountId }?.let { BangladeshBanks.inferPaymentMethod(it) } ?: "MFS"
        } else {
            paymentMethod
        }

        return SalesInvoiceDraft(
            customerId = selectedCustomerId,
            warehouseId = selectedWarehouseId,
            invoiceDate = System.currentTimeMillis(),
            discountMinor = totals.invoiceDiscountMinor,
            additionalChargesMinor = totals.additionalChargesMinor,
            paidAmountMinor = totals.paidMinor,
            paymentMethod = resolvedPaymentMethod,
            paymentAccountId = resolvedAccountId,
            notes = notes.trim().ifBlank { null },
            items = draftItems,
            newCustomerDraft = pendingNewCustomerDraft,
            discountPercentBps = if (isInvoiceDiscountPercentage && totals.invoiceDiscountMinor > 0L) {
                SalesInvoiceCalculator.parsePercentBasisPoints(invoiceDiscountStr).takeIf { it > 0L }
            } else null
        )
    }

    val visibleIndices = remember(itemsList.size, itemsList.map { "${it.productId}|${it.variantId}" }, itemSearchQuery, productsMap, variantsByProduct) {
        val q = itemSearchQuery.trim()
        itemsList.indices.filter { idx ->
            if (q.isBlank()) true
            else {
                val item = itemsList[idx]
                val p = productsMap[item.productId]
                val v = variantsByProduct[item.productId]?.find { it.id == item.variantId }
                (p?.nameEn?.contains(q, true) == true) ||
                    (p?.nameBn?.contains(q, true) == true) ||
                    (p?.baseSku?.contains(q, true) == true) ||
                    (v?.sku?.contains(q, true) == true) ||
                    (v?.color?.contains(q, true) == true) ||
                    (v?.size?.contains(q, true) == true)
            }
        }
    }

    ERPFullScreenEditor(
        onDismissRequest = onDismiss,
        applySafeDrawingPadding = false,
        lightStatusBarIcons = false,
        statusBarColor = android.graphics.Color.TRANSPARENT
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            SalesInvoiceHeroBackground(
                isDark = isDark,
                modifier = Modifier.height(180.dp)
            )

            Column(modifier = Modifier.fillMaxSize()) {
                SalesInvoiceHeroHeader(
                    title = if (isBangla) "নতুন বিক্রয় চালান" else "New Sales Invoice",
                    subtitle = if (isBangla) "চালান তৈরি, পণ্য যোগ ও পেমেন্ট নিন" else "Create invoice, add items & take payment",
                    canToggleCostOrProfit = canViewAnyCostOrProfit,
                    showCostAndProfit = showCostAndProfit,
                    onBack = onDismiss,
                    onToggleCostProfit = costProfitVisibility.toggle,
                    onScanBarcode = { showBarcodeScanner = true }
                )

                // Main Rounded Sheet overlapping bottom of hero gradient
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
                    color = palette.sheetBg
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // 1. Customer Summary Card + 4-Across Historical Metrics
                            item(key = "customer_section") {
                                SalesCustomerHeroSection(
                                    customer = selectedCustomer,
                                    pendingNewCustomer = pendingNewCustomerDraft,
                                    summary = customerSummary,
                                    isBangla = isBangla,
                                    palette = palette,
                                    onChangeCustomerClick = { showCustomerLookup = true }
                                )
                            }

                            // 2. Dispatch Warehouse Card
                            item(key = "warehouse_section") {
                                SalesWarehouseSelectorCard(
                                    warehouses = warehouses,
                                    selectedWarehouse = selectedWarehouse,
                                    isBangla = isBangla,
                                    palette = palette,
                                    onSelectWarehouse = { wh ->
                                        selectedWarehouseId = wh.id
                                        errorMessage = null
                                    }
                                )
                            }

                            // 3. Invoice Items Header
                            item(key = "items_header") {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.ViewInAr,
                                                contentDescription = null,
                                                tint = palette.primaryPurple,
                                                modifier = Modifier.size(19.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isBangla) "চালানের পণ্যসমূহ (${itemsList.size})" else "Invoice Items (${itemsList.size})",
                                                fontSize = 14.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = palette.textPrimary
                                            )
                                        }

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                modifier = Modifier.size(34.dp),
                                                onClick = { showItemSearchBar = !showItemSearchBar },
                                                shape = RoundedCornerShape(9.dp),
                                                color = if (showItemSearchBar) palette.iconBadgeBg else palette.cardBg,
                                                border = BorderStroke(1.dp, palette.cardBorder)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Search,
                                                        contentDescription = "Search Items",
                                                        tint = palette.primaryPurple,
                                                        modifier = Modifier.size(17.dp)
                                                    )
                                                }
                                            }

                                            Surface(
                                                modifier = Modifier.size(34.dp),
                                                onClick = { showBarcodeScanner = true },
                                                shape = RoundedCornerShape(9.dp),
                                                color = palette.cardBg,
                                                border = BorderStroke(1.dp, palette.cardBorder)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.QrCodeScanner,
                                                        contentDescription = "Scan Item Barcode",
                                                        tint = palette.primaryPurple,
                                                        modifier = Modifier.size(17.dp)
                                                    )
                                                }
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .height(34.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(purplePillGradient)
                                                    .clickable {
                                                        pickingForItemIndex = null
                                                        showAddItemPicker = true
                                                    }
                                                    .padding(horizontal = 12.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Add,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text(
                                                        text = if (isBangla) "পণ্য যোগ" else "Add Item",
                                                        color = Color.White,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (showItemSearchBar) {
                                        OutlinedTextField(
                                            value = itemSearchQuery,
                                            onValueChange = { itemSearchQuery = it },
                                            placeholder = {
                                                Text(
                                                    text = if (isBangla) "তালিকায় থাকা পণ্য খুঁজুন (নাম, SKU, ভেরিয়েন্ট)..." else "Filter added items by name, SKU or variant...",
                                                    fontSize = 12.sp
                                                )
                                            },
                                            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                            trailingIcon = {
                                                if (itemSearchQuery.isNotBlank()) {
                                                    IconButton(onClick = { itemSearchQuery = "" }) {
                                                        Icon(Icons.Outlined.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            },
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }

                            // Empty items card if user removed all items
                            if (itemsList.isEmpty()) {
                                item(key = "empty_items") {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        color = palette.cardBg,
                                        border = BorderStroke(1.dp, palette.cardBorder)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(20.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = if (isBangla) "এখনও কোনো পণ্য যোগ করা হয়নি" else "No items added yet",
                                                fontWeight = FontWeight.SemiBold,
                                                color = palette.textSecondary
                                            )
                                            Button(
                                                onClick = {
                                                    pickingForItemIndex = null
                                                    showAddItemPicker = true
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = palette.primaryPurpleStrong),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(if (isBangla) "পণ্য নির্বাচন করুন" else "Select Item")
                                            }
                                        }
                                    }
                                }
                            }

                            // 4. Item Cards (Expanded & Minimized states matching reference 1:1)
                            itemsIndexed(
                                items = visibleIndices,
                                key = { _, realIndex -> itemsList[realIndex].uid }
                            ) { _, index ->
                                val itemDraft = itemsList[index]
                                val calc = lineCalculations[index]
                                val curProduct = productsMap[itemDraft.productId]
                                val prodVariants = variantsByProduct[itemDraft.productId].orEmpty()
                                val curVariant = prodVariants.find { it.id == itemDraft.variantId }
                                val sellableStock = getSellableStock(itemDraft.productId, itemDraft.variantId)
                                val thumbUri = curVariant?.let { variantImages[it.id] }
                                    ?: curProduct?.let { productImages[it.id] }

                                val unitCostMinor = resolveItemUnitCostMinor(curProduct, curVariant)

                                SalesInvoiceItemCard(
                                    index = index,
                                    itemDraft = itemDraft,
                                    calculation = calc,
                                    product = curProduct,
                                    variants = prodVariants,
                                    selectedVariant = curVariant,
                                    thumbnailUri = thumbUri,
                                    sellableStock = sellableStock,
                                    unitCostMinor = unitCostMinor,
                                    stockBalances = stockBalances,
                                    warehouseId = selectedWarehouseId,
                                    showCost = canViewCost && showCostAndProfit,
                                    showProfit = canViewProfit && showCostAndProfit,
                                    isBangla = isBangla,
                                    palette = palette,
                                    onToggleExpand = { itemDraft.isExpanded = !itemDraft.isExpanded },
                                    onDuplicate = {
                                        val clone = ItemDraftState(
                                            productId = itemDraft.productId,
                                            variantId = itemDraft.variantId,
                                            quantityStr = itemDraft.quantityStr,
                                            unitPriceStr = itemDraft.unitPriceStr,
                                            discountStr = itemDraft.discountStr,
                                            isPercentageDiscount = itemDraft.isPercentageDiscount,
                                            isExpanded = true
                                        )
                                        itemsList.add(index + 1, clone)
                                        errorMessage = null
                                    },
                                    onDelete = {
                                        itemsList.removeAt(index)
                                        errorMessage = null
                                    },
                                    onOpenProductPicker = {
                                        pickingForItemIndex = index
                                        showAddItemPicker = true
                                    },
                                    onSelectVariant = { chosenVariant ->
                                        itemDraft.variantId = chosenVariant.id
                                        itemDraft.unitPriceStr = SalesInvoiceCalculator.formatMajorEditable(chosenVariant.variantPriceMinor)
                                        errorMessage = null
                                    }
                                )
                            }

                            // 5. Invoice Discount + Additional Charges Side-by-Side
                            item(key = "discount_and_charges") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Invoice Discount Card
                                    Surface(
                                        modifier = Modifier.weight(1.38f),
                                        shape = RoundedCornerShape(14.dp),
                                        color = palette.cardBg,
                                        border = BorderStroke(1.dp, palette.cardBorder),
                                        shadowElevation = if (palette.isDark) 0.dp else 1.dp
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 7.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .clip(RoundedCornerShape(5.dp))
                                                    .background(palette.iconBadgeBg),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.LocalOffer,
                                                    contentDescription = null,
                                                    tint = palette.primaryPurple,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                            Text(
                                                text = if (isBangla) "বাট্টা" else "Invoice Discount",
                                                fontSize = 9.sp,
                                                letterSpacing = (-0.25).sp,
                                                color = palette.textSecondary,
                                                maxLines = 1,
                                                softWrap = false,
                                                fontWeight = FontWeight.Medium
                                            )
                                            DiscountModeToggle(
                                                isPercent = isInvoiceDiscountPercentage,
                                                palette = palette,
                                                height = 31.dp,
                                                segmentWidth = 21.dp,
                                                onToggle = { pct ->
                                                    if (isInvoiceDiscountPercentage != pct) {
                                                        isInvoiceDiscountPercentage = pct
                                                        if (pct) {
                                                            val bps = SalesInvoiceCalculator.parsePercentBasisPoints(invoiceDiscountStr)
                                                            if (SalesInvoiceCalculator.parseMajorToMinor(invoiceDiscountStr) > 10_000L && bps == 10_000L) {
                                                                invoiceDiscountStr = "100.00"
                                                            }
                                                        }
                                                    }
                                                }
                                            )
                                            CompactNumericInputBox(
                                                value = invoiceDiscountStr,
                                                onValueChange = { invoiceDiscountStr = it },
                                                palette = palette,
                                                height = 31.dp,
                                                fontSize = 11.sp,
                                                suffix = if (isInvoiceDiscountPercentage) "%" else "৳",
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }

                                    // Additional Charges Card
                                    Surface(
                                        modifier = Modifier.weight(0.92f),
                                        shape = RoundedCornerShape(14.dp),
                                        color = palette.cardBg,
                                        border = BorderStroke(1.dp, palette.cardBorder),
                                        shadowElevation = if (palette.isDark) 0.dp else 1.dp
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 7.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(RoundedCornerShape(7.dp))
                                                    .background(palette.iconBadgeBg),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Layers,
                                                    contentDescription = null,
                                                    tint = palette.primaryPurple,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = if (isBangla) "অতিরিক্ত চার্জ (৳)" else "Additional Charges (৳)",
                                                    fontSize = 8.5.sp,
                                                    letterSpacing = (-0.25).sp,
                                                    color = palette.textSecondary,
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    overflow = TextOverflow.Ellipsis,
                                                    lineHeight = 11.sp
                                                )
                                                BasicTextField(
                                                    value = additionalChargesStr,
                                                    onValueChange = { additionalChargesStr = it },
                                                    singleLine = true,
                                                    textStyle = TextStyle(
                                                        color = palette.textPrimary,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                    cursorBrush = SolidColor(palette.primaryPurple),
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 6. Notes / Terms Row Card
                            item(key = "notes_section") {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    color = palette.cardBg,
                                    border = BorderStroke(1.dp, palette.cardBorder),
                                    shadowElevation = if (palette.isDark) 0.dp else 1.dp
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(palette.iconBadgeBg),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Description,
                                                contentDescription = null,
                                                tint = palette.primaryPurple,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                            if (notes.isEmpty()) {
                                                Text(
                                                    text = if (isBangla) "নোট / শর্তাবলী (ঐচ্ছিক)" else "Notes / Terms (optional)",
                                                    fontSize = 13.sp,
                                                    color = palette.textMuted
                                                )
                                            }
                                            BasicTextField(
                                                value = notes,
                                                onValueChange = { notes = it },
                                                singleLine = true,
                                                textStyle = TextStyle(
                                                    color = palette.textPrimary,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                cursorBrush = SolidColor(palette.primaryPurple),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }

                            // 7. Payment Method & Settlement Card
                            item(key = "payment_section") {
                                SalesPaymentSettlementCard(
                                    paymentMethod = paymentMethod,
                                    selectedAccountId = selectedPaymentAccountId,
                                    liquidAccounts = liquidAccounts,
                                    paidAmountStr = paidAmountStr,
                                    isFullPaySelected = isFullPaymentSelected,
                                    grandTotalMinor = totals.grandTotalMinor,
                                    isBangla = isBangla,
                                    palette = palette,
                                    onSelectPaymentMethod = { code ->
                                        paymentMethod = code
                                        when (code) {
                                            "CREDIT" -> {
                                                isFullPaymentSelected = false
                                                paidAmountStr = "0.00"
                                            }
                                            "CASH" -> selectedPaymentAccountId =
                                                liquidAccounts.firstOrNull { it.liquidKind == LiquidAccountKind.CASH }?.id ?: "coa-1010"
                                            "BANK" -> selectedPaymentAccountId =
                                                liquidAccounts.firstOrNull { it.liquidKind == LiquidAccountKind.BANK }?.id ?: "coa-1020"
                                            "MFS" -> selectedPaymentAccountId =
                                                liquidAccounts.firstOrNull { it.liquidKind == LiquidAccountKind.MOBILE_BANKING }?.id ?: "coa-1060"
                                        }
                                    },
                                    onSelectAccountId = { selectedPaymentAccountId = it },
                                    onPaidAmountChange = {
                                        isFullPaymentSelected = false
                                        paidAmountStr = it
                                    },
                                    onFullPayClick = {
                                        if (paymentMethod == "CREDIT") {
                                            paymentMethod = "CASH"
                                            selectedPaymentAccountId = liquidAccounts.firstOrNull { it.liquidKind == LiquidAccountKind.CASH }?.id ?: "coa-1010"
                                        }
                                        isFullPaymentSelected = true
                                        paidAmountStr = SalesInvoiceCalculator.formatMajorEditable(totals.grandTotalMinor)
                                    }
                                )
                            }

                            // 8. Dark Purple Wave Invoice Summary Card
                            item(key = "summary_section") {
                                SalesInvoiceSummaryCard(
                                    itemCount = itemsList.size,
                                    totals = totals,
                                    showProfit = canViewProfit && showCostAndProfit,
                                    hasItems = itemsList.isNotEmpty(),
                                    isBangla = isBangla
                                )
                            }

                            if (errorMessage != null) {
                                item(key = "error_banner") {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        color = palette.kpiDueBg,
                                        border = BorderStroke(1.dp, palette.errorText.copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            text = errorMessage!!,
                                            color = palette.errorText,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                                        )
                                    }
                                }
                            }

                            item(key = "bottom_spacer") {
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }

                        // Sticky Bottom Action Bar
                        SalesInvoiceStickyFooter(
                            isBangla = isBangla,
                            palette = palette,
                            onCancel = onDismiss,
                            onSaveDraft = {
                                val draft = buildInvoiceDraft() ?: return@SalesInvoiceStickyFooter
                                onSubmitDraft(draft)
                            },
                            onConfirmSale = {
                                val draft = buildInvoiceDraft() ?: return@SalesInvoiceStickyFooter
                                val cust = customers.find { it.id == selectedCustomerId }
                                val existingDue = customerSummary?.outstandingBalanceMinor ?: cust?.outstandingBalanceMinor ?: 0L
                                val expectedDue = existingDue + totals.dueMinor
                                val limit = cust?.creditLimitMinor ?: 0L

                                if (limit > 0L && expectedDue > limit && !creditLimitApproved) {
                                    showCreditLimitWarningDialog = true
                                } else {
                                    val finalDraft = if (creditLimitApproved) {
                                        draft.copy(
                                            notes = "${draft.notes ?: ""}\n[CREDIT OVERRIDE APPROVED by $creditApproverName: $creditApprovalReason]".trim()
                                        )
                                    } else draft
                                    onConfirmSale(finalDraft)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showCustomerLookup) {
        CustomerLookupDialog(
            customers = customers,
            selectedCustomerId = selectedCustomerId,
            pendingNewCustomer = pendingNewCustomerDraft,
            isBangla = isBangla,
            onDismiss = { showCustomerLookup = false },
            onSelectCustomer = { cust ->
                selectedCustomerId = cust.id
                pendingNewCustomerDraft = null
            },
            onCreateNewCustomer = { newCustDraft ->
                pendingNewCustomerDraft = newCustDraft
                selectedCustomerId = ""
            },
            onCreateCustomerImmediate = onCreateCustomerImmediate?.let { createFn ->
                { draft, onDone, onErr ->
                    createFn(
                        draft,
                        { createdCustomer ->
                            selectedCustomerId = createdCustomer.id
                            pendingNewCustomerDraft = null
                            onDone(createdCustomer)
                        },
                        onErr
                    )
                }
            }
        )
    }

    if (showAddItemPicker) {
        SalesProductVariantPickerDialog(
            products = products,
            variantsByProduct = variantsByProduct,
            stockBalances = stockBalances,
            warehouseId = selectedWarehouseId,
            productImages = productImages,
            variantImages = variantImages,
            isBangla = isBangla,
            palette = palette,
            onDismiss = {
                showAddItemPicker = false
                pickingForItemIndex = null
            },
            onSelectProductAndVariant = { chosenProduct, chosenVariant ->
                val priceMinor = chosenVariant?.variantPriceMinor ?: chosenProduct.sellingPriceMinor
                val targetIndex = pickingForItemIndex
                if (targetIndex != null && targetIndex in itemsList.indices) {
                    val target = itemsList[targetIndex]
                    target.productId = chosenProduct.id
                    target.variantId = chosenVariant?.id
                    target.unitPriceStr = SalesInvoiceCalculator.formatMajorEditable(priceMinor)
                } else {
                    itemsList.add(
                        ItemDraftState(
                            productId = chosenProduct.id,
                            variantId = chosenVariant?.id,
                            quantityStr = "1",
                            unitPriceStr = SalesInvoiceCalculator.formatMajorEditable(priceMinor),
                            isExpanded = true
                        )
                    )
                }
                errorMessage = null
                showAddItemPicker = false
                pickingForItemIndex = null
            }
        )
    }

    if (showBarcodeScanner) {
        BarcodeQrScannerDialog(
            language = if (isBangla) AppLanguage.BANGLA else AppLanguage.ENGLISH,
            onDismiss = { showBarcodeScanner = false },
            onCodeScanned = { scannedCode ->
                showBarcodeScanner = false
                handleScannedCode(scannedCode)
            }
        )
    }

    if (showCreditLimitWarningDialog) {
        val cust = customers.find { it.id == selectedCustomerId }
        val existingDue = customerSummary?.outstandingBalanceMinor ?: cust?.outstandingBalanceMinor ?: 0L
        val expectedDue = existingDue + totals.dueMinor
        val limit = cust?.creditLimitMinor ?: 0L

        Dialog(onDismissRequest = { showCreditLimitWarningDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(0.95f)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = if (isBangla) "ক্রেডিট লিমিট সতর্কবার্তা (Option C)" else "Credit Limit Warning (Option C)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = StatusError
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isBangla)
                            "গ্রাহক ${cust?.name ?: ""}-এর অনুমোদিত ক্রেডিট সীমা ৳${Formatters.formatMinorUnit(limit)} কিন্তু এই বিক্রয়ের পর সম্ভাব্য মোট বকেয়া হবে ৳${Formatters.formatMinorUnit(expectedDue)}। এটি নিশ্চিত করতে অনুমোদিত অনুমোদন প্রয়োজন।"
                        else
                            "Customer credit limit is ৳${Formatters.formatMinorUnit(limit)}, but expected due will reach ৳${Formatters.formatMinorUnit(expectedDue)}. Confirmation requires authorized approval.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = creditApproverName,
                        onValueChange = { creditApproverName = it },
                        label = { Text(if (isBangla) "অনুমোদনকারীর নাম / পদবী" else "Approver Name / Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = creditApprovalReason,
                        onValueChange = { creditApprovalReason = it },
                        label = { Text(if (isBangla) "অনুমোদনের কারণ / নোট" else "Approval Reason / Justification") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showCreditLimitWarningDialog = false }) {
                            Text(if (isBangla) "বাতিল" else "Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (creditApproverName.isNotBlank() && creditApprovalReason.isNotBlank()) {
                                    creditLimitApproved = true
                                    showCreditLimitWarningDialog = false
                                    val draft = buildInvoiceDraft() ?: return@Button
                                    val finalDraft = draft.copy(
                                        notes = "${draft.notes ?: ""}\n[CREDIT OVERRIDE APPROVED by $creditApproverName: $creditApprovalReason]".trim()
                                    )
                                    onConfirmSale(finalDraft)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                        ) {
                            Text(if (isBangla) "অনুমোদন ও নিশ্চিত করুন" else "Approve & Confirm")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SalesInvoiceItemCard(
    index: Int,
    itemDraft: ItemDraftState,
    calculation: LineCalculation,
    product: Product?,
    variants: List<ProductVariant>,
    selectedVariant: ProductVariant?,
    thumbnailUri: String?,
    sellableStock: Int,
    unitCostMinor: Long,
    stockBalances: List<StockBalance>,
    warehouseId: String,
    showCost: Boolean,
    showProfit: Boolean,
    isBangla: Boolean,
    palette: SalesInvoicePalette,
    onToggleExpand: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onOpenProductPicker: () -> Unit,
    onSelectVariant: (ProductVariant) -> Unit
) {
    val variantChipLabel = remember(selectedVariant) {
        selectedVariant?.let { v ->
            listOfNotNull(v.color, v.size).joinToString("-").ifBlank { v.sku }
        }
    }
    val productName = if (product != null) {
        if (isBangla && product.nameBn.isNotBlank()) product.nameBn else product.nameEn
    } else {
        if (isBangla) "পণ্য নির্বাচন করুন" else "Select Item"
    }

    var variantMenuExpanded by rememberSaveable(itemDraft.uid) { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (!itemDraft.isExpanded) {
                    Modifier.clickable(onClick = onToggleExpand)
                } else Modifier
            ),
        shape = RoundedCornerShape(15.dp),
        color = palette.cardBg,
        border = BorderStroke(1.dp, palette.cardBorder),
        shadowElevation = if (palette.isDark) 0.dp else 1.5.dp
    ) {
        if (!itemDraft.isExpanded) {
            // MINIMIZED STATE
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 9.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SalesProductThumbnail(
                    imageUri = thumbnailUri,
                    contentDescription = productName,
                    size = 42.dp,
                    cornerRadius = 10.dp,
                    palette = palette
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f).padding(end = 4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = "#${index + 1} $productName",
                            fontSize = 13.sp,
                            letterSpacing = (-0.2).sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (variantChipLabel != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = palette.variantChipBg
                            ) {
                                Text(
                                    text = variantChipLabel,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.variantChipText,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    val qtyPart = if (isBangla) "পরিমাণ: ${calculation.quantity}" else "Qty: ${calculation.quantity}"
                    val pricePart = "${if (isBangla) "দর" else "Price"}: ৳${Formatters.formatMinorUnit(calculation.unitPriceMinor)}"
                    val totalPrefix = "${if (isBangla) "মোট" else "Total"}: "
                    val totalVal = "৳${Formatters.formatMinorUnit(calculation.netTotalMinor)}"
                    val fullSummaryLen = qtyPart.length + pricePart.length + totalPrefix.length + totalVal.length + 6
                    val summaryFontSize = when {
                        fullSummaryLen >= 44 -> 9.sp
                        fullSummaryLen >= 38 -> 9.8.sp
                        else -> 10.8.sp
                    }

                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = palette.textSecondary, fontWeight = FontWeight.Normal)) {
                                append("$qtyPart | $pricePart | $totalPrefix")
                            }
                            withStyle(SpanStyle(color = palette.primaryPurple, fontWeight = FontWeight.Bold)) {
                                append(totalVal)
                            }
                        },
                        fontSize = summaryFontSize,
                        letterSpacing = (-0.2).sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                ItemActionIconsRow(
                    isExpanded = false,
                    palette = palette,
                    onDuplicate = onDuplicate,
                    onDelete = onDelete,
                    onToggleExpand = onToggleExpand
                )
            }
        } else {
            // EXPANDED STATE
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 11.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(modifier = Modifier.clickable(onClick = onOpenProductPicker)) {
                        SalesProductThumbnail(
                            imageUri = thumbnailUri,
                            contentDescription = productName,
                            size = 56.dp,
                            cornerRadius = 12.dp,
                            palette = palette
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(
                                    text = "#${index + 1} $productName",
                                    fontSize = 13.sp,
                                    letterSpacing = (-0.2).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .weight(1f, fill = false)
                                        .clickable(onClick = onOpenProductPicker)
                                )
                                if (variantChipLabel != null) {
                                    Surface(
                                        onClick = { if (variants.isNotEmpty()) variantMenuExpanded = true else onOpenProductPicker() },
                                        shape = RoundedCornerShape(8.dp),
                                        color = palette.variantChipBg
                                    ) {
                                        Text(
                                            text = variantChipLabel,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = palette.variantChipText,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            ItemActionIconsRow(
                                isExpanded = true,
                                palette = palette,
                                onDuplicate = onDuplicate,
                                onDelete = onDelete,
                                onToggleExpand = onToggleExpand
                            )
                        }

                        if (product != null) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${product.nameEn} [${product.baseSku}]",
                                fontSize = 10.5.sp,
                                letterSpacing = (-0.15).sp,
                                fontWeight = FontWeight.Medium,
                                color = palette.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(onClick = onOpenProductPicker)
                            )
                        } else {
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = if (isBangla) "পণ্য নির্বাচন করতে এখানে ট্যাপ করুন" else "Tap here to choose a product",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.primaryPurple,
                                modifier = Modifier.clickable(onClick = onOpenProductPicker)
                            )
                        }

                        if (variants.isNotEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                val variantFullLabel = selectedVariant?.let { v ->
                                    listOfNotNull(v.color, v.size, v.sku).joinToString(" - ")
                                } ?: if (isBangla) "নির্বাচন করুন" else "Select"
                                Text(
                                    text = "Variant: $variantFullLabel",
                                    fontSize = 10.5.sp,
                                    letterSpacing = (-0.15).sp,
                                    color = palette.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 1.dp)
                                        .clickable { variantMenuExpanded = true }
                                )

                                DropdownMenu(
                                    expanded = variantMenuExpanded,
                                    onDismissRequest = { variantMenuExpanded = false },
                                    modifier = Modifier.background(palette.cardBg)
                                ) {
                                    variants.forEach { v ->
                                        val vStock = sellableStockForVariant(stockBalances, warehouseId, itemDraft.productId, v.id)
                                        val vInStock = vStock > 0
                                        DropdownMenuItem(
                                            enabled = vInStock,
                                            colors = MenuDefaults.itemColors(
                                                textColor = palette.textPrimary,
                                                disabledTextColor = palette.textMuted.copy(alpha = 0.45f)
                                            ),
                                            text = {
                                                Text(
                                                    text = "${listOfNotNull(v.color, v.size, v.sku).joinToString(" - ")} • ৳${Formatters.formatMinorUnit(v.variantPriceMinor)} ($vStock)",
                                                    fontSize = 12.sp
                                                )
                                            },
                                            onClick = {
                                                onSelectVariant(v)
                                                variantMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Editable Row: Qty [- qty +] | Price (৳) | Discount [% | ৳ value] | Item Total (৳)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // 1. Qty Stepper
                    Column(
                        modifier = Modifier.weight(0.92f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isBangla) "পরিমাণ" else "Qty",
                            fontSize = 10.sp,
                            color = palette.textSecondary,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(33.dp),
                            shape = RoundedCornerShape(9.dp),
                            color = palette.inputBg,
                            border = BorderStroke(1.dp, palette.inputBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable {
                                            val cur = Formatters.normalizeAsciiNumberInput(itemDraft.quantityStr).toIntOrNull() ?: 1
                                            if (cur > 1) itemDraft.quantityStr = (cur - 1).toString()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Remove,
                                        contentDescription = "Decrease Qty",
                                        tint = palette.primaryPurple,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }

                                BasicTextField(
                                    value = itemDraft.quantityStr,
                                    onValueChange = { itemDraft.quantityStr = it },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        color = palette.textPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    ),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    cursorBrush = SolidColor(palette.primaryPurple),
                                    modifier = Modifier.weight(1f)
                                )

                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable {
                                            val cur = Formatters.normalizeAsciiNumberInput(itemDraft.quantityStr).toIntOrNull() ?: 0
                                            itemDraft.quantityStr = (cur + 1).toString()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Add,
                                        contentDescription = "Increase Qty",
                                        tint = palette.primaryPurple,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 2. Price (৳)
                    Column(
                        modifier = Modifier.weight(0.85f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isBangla) "দর (৳)" else "Price (৳)",
                            fontSize = 10.sp,
                            color = palette.textSecondary,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        CompactNumericInputBox(
                            value = itemDraft.unitPriceStr,
                            onValueChange = { itemDraft.unitPriceStr = it },
                            palette = palette,
                            height = 33.dp,
                            fontSize = 11.5.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // 3. Discount [% | ৳] + Input
                    Column(
                        modifier = Modifier.weight(1.60f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isBangla) "বাট্টা" else "Discount",
                            fontSize = 10.sp,
                            color = palette.textSecondary,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            DiscountModeToggle(
                                isPercent = itemDraft.isPercentageDiscount,
                                palette = palette,
                                height = 33.dp,
                                segmentWidth = 23.dp,
                                onToggle = { pct ->
                                    if (itemDraft.isPercentageDiscount != pct) {
                                        itemDraft.isPercentageDiscount = pct
                                        if (pct) {
                                            val bps = SalesInvoiceCalculator.parsePercentBasisPoints(itemDraft.discountStr)
                                            if (SalesInvoiceCalculator.parseMajorToMinor(itemDraft.discountStr) > 10_000L && bps == 10_000L) {
                                                itemDraft.discountStr = "100.00"
                                            }
                                        }
                                    }
                                }
                            )
                            CompactNumericInputBox(
                                value = itemDraft.discountStr,
                                onValueChange = { itemDraft.discountStr = it },
                                palette = palette,
                                height = 33.dp,
                                fontSize = 11.sp,
                                suffix = if (itemDraft.isPercentageDiscount) "%" else "৳",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // 4. Live Item Total Box
                    Surface(
                        modifier = Modifier
                            .weight(0.98f)
                            .height(47.dp),
                        shape = RoundedCornerShape(11.dp),
                        color = palette.itemTotalBg
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (isBangla) "মোট (৳)" else "Item Total (৳)",
                                fontSize = 8.5.sp,
                                color = palette.itemTotalLabel,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val totalStr = Formatters.formatMinorUnit(calculation.netTotalMinor)
                            Text(
                                text = totalStr,
                                fontSize = if (totalStr.length > 9) 11.sp else 12.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = palette.itemTotalValue,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = palette.cardBorder.copy(alpha = 0.65f), thickness = 0.7.dp)
                Spacer(modifier = Modifier.height(6.dp))

                // Bottom Stock / Restricted Cost / Restricted Profit strip
                val stockInsufficient = sellableStock <= 0 || calculation.quantity > sellableStock
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Layers,
                            contentDescription = null,
                            tint = if (stockInsufficient) palette.errorText else palette.stockOkText,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = if (isBangla) "স্টক: $sellableStock টি" else "Stock: $sellableStock units",
                            fontSize = 9.sp,
                            letterSpacing = (-0.2).sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (stockInsufficient) palette.errorText else palette.textPrimary,
                            maxLines = 1
                        )
                    }

                    if (showCost && product != null) {
                        Text("|", color = palette.cardBorder, fontSize = 9.5.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.LocalOffer,
                                contentDescription = null,
                                tint = palette.primaryPurple,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = if (isBangla) "ক্রয়: ৳${Formatters.formatMinorUnit(unitCostMinor)}/pc"
                                else "Cost: ৳${Formatters.formatMinorUnit(unitCostMinor)}/pc",
                                fontSize = 9.sp,
                                letterSpacing = (-0.2).sp,
                                fontWeight = FontWeight.Medium,
                                color = palette.textSecondary,
                                maxLines = 1
                            )
                        }
                    }

                    if (showProfit && product != null) {
                        val profitPos = calculation.profitMinor >= 0L
                        val pColor = if (profitPos) palette.profitText else palette.errorText
                        Text("|", color = palette.cardBorder, fontSize = 9.5.sp)
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Equalizer,
                                contentDescription = null,
                                tint = pColor,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = if (isBangla) {
                                    "লাভ: ৳${Formatters.formatMinorUnit(calculation.profitMinor)} (${String.format(Locale.US, "%.1f", calculation.profitMarginPercent)}%)"
                                } else {
                                    "Profit: ৳${Formatters.formatMinorUnit(calculation.profitMinor)} (${String.format(Locale.US, "%.1f", calculation.profitMarginPercent)}%)"
                                },
                                fontSize = 9.sp,
                                letterSpacing = (-0.2).sp,
                                fontWeight = FontWeight.Bold,
                                color = pColor,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemActionIconsRow(
    isExpanded: Boolean,
    palette: SalesInvoicePalette,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onToggleExpand: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(1.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onDuplicate,
            modifier = Modifier.size(23.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.ContentCopy,
                contentDescription = "Duplicate Item",
                tint = palette.primaryPurple,
                modifier = Modifier.size(15.dp)
            )
        }
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(23.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.DeleteOutline,
                contentDescription = "Delete Item",
                tint = palette.errorText,
                modifier = Modifier.size(16.dp)
            )
        }
        Box(
            modifier = Modifier
                .size(25.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(palette.chevronBoxBg)
                .clickable(onClick = onToggleExpand),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse Item" else "Expand Item",
                tint = palette.primaryPurple,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
