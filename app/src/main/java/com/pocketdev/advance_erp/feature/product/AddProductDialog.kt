package com.pocketdev.advance_erp.feature.product

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.ViewWeek
import androidx.compose.material.icons.outlined.Warehouse
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.pocketdev.advance_erp.R
import com.pocketdev.advance_erp.core.designsystem.ERPFullScreenEditor
import com.pocketdev.advance_erp.core.designsystem.SuggestTextField
import com.pocketdev.advance_erp.core.designsystem.rememberImageModel
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.model.CurrencyCode
import com.pocketdev.advance_erp.core.model.Money
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.data.dao.ProductWithDetails
import com.pocketdev.advance_erp.data.entity.ProductCategory
import com.pocketdev.advance_erp.data.entity.SuggestionType
import com.pocketdev.advance_erp.data.entity.Warehouse
import kotlinx.coroutines.launch

private val UNIT_PRESETS = listOf("PCS", "BOX", "KG", "SET", "PAIR", "PACK", "DOZEN")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddProductDialog(
    categories: List<ProductCategory>,
    warehouses: List<Warehouse>,
    language: AppLanguage,
    hideStock: Boolean = false,
    initialStep: Int = 0,
    initialVariantsEnabled: Boolean = false,
    onDismiss: () -> Unit,
    onAddNewCategory: ((nameEn: String, nameBn: String, onCreated: (ProductCategory) -> Unit) -> Unit)? = null,
    onGenerateSku: (suspend (categoryId: String, nameEn: String) -> String)? = null,
    onGenerateVariantSku: (suspend (baseSku: String, color: String?, size: String?) -> String)? = null,
    onGenerateBarcode: (suspend () -> String)? = null,
    onValidateSku: (suspend (sku: String, excludeId: String?) -> Boolean)? = null,
    onValidateBarcode: (suspend (barcode: String, excludeId: String?) -> Boolean)? = null,
    onLoadCategoryProducts: (suspend (categoryId: String) -> List<ProductWithDetails>)? = null,
    onLoadProductRestockBundle: (suspend (productId: String) -> ExistingProductRestockBundle?)? = null,
    onUpdateExistingProduct: ((
        productId: String,
        nameEn: String,
        nameBn: String,
        baseSku: String,
        barcode: String?,
        categoryId: String,
        unit: String,
        purchasePriceMinor: Long,
        sellingPriceMinor: Long,
        minStockAlert: Int,
        productPhotos: List<GalleryItemDraft>,
        variants: List<NewVariantDraft>,
        deletedImageIds: List<String>,
        additionalStock: Int,
        warehouseId: String,
        description: String?
    ) -> Unit)? = null,
    onConfirm: (
        nameEn: String,
        nameBn: String,
        baseSku: String,
        barcode: String?,
        categoryId: String,
        unit: String,
        purchasePriceMinor: Long,
        sellingPriceMinor: Long,
        minStockAlert: Int,
        productPhotos: List<GalleryItemDraft>,
        variants: List<NewVariantDraft>,
        initialStock: Int,
        warehouseId: String,
        description: String?
    ) -> Unit
) {
    val isBangla = language == AppLanguage.BANGLA
    val palette = rememberAddProductPalette()
    val scope = rememberCoroutineScope()

    var currentStep by rememberSaveable { mutableIntStateOf(initialStep.coerceIn(0, 2)) }
    var variantsEnabled by rememberSaveable {
        mutableStateOf(initialVariantsEnabled || initialStep == 2)
    }

    var nameEn by rememberSaveable { mutableStateOf("") }
    var nameBn by rememberSaveable { mutableStateOf("") }
    var baseSku by rememberSaveable { mutableStateOf("") }
    var barcode by rememberSaveable { mutableStateOf("") }
    var unit by rememberSaveable { mutableStateOf("PCS") }
    var purchasePriceStr by rememberSaveable { mutableStateOf("") }
    var sellingPriceStr by rememberSaveable { mutableStateOf("") }
    var minStockAlertStr by rememberSaveable { mutableStateOf("10") }
    var initialStockStr by rememberSaveable { mutableStateOf("0") }
    var description by rememberSaveable { mutableStateOf("") }

    var selectedCategoryId by rememberSaveable { mutableStateOf(categories.firstOrNull()?.id) }
    var selectedWarehouseId by rememberSaveable { mutableStateOf(warehouses.firstOrNull()?.id) }

    LaunchedEffect(categories) {
        if (selectedCategoryId == null || categories.none { it.id == selectedCategoryId }) {
            selectedCategoryId = categories.firstOrNull()?.id
        }
    }
    LaunchedEffect(warehouses) {
        if (selectedWarehouseId == null || warehouses.none { it.id == selectedWarehouseId }) {
            selectedWarehouseId = warehouses.firstOrNull()?.id
        }
    }

    val selectedCategory = categories.firstOrNull { it.id == selectedCategoryId } ?: categories.firstOrNull()
    val selectedWarehouse = warehouses.firstOrNull { it.id == selectedWarehouseId } ?: warehouses.firstOrNull()

    // Existing products in selected category (Restock / Edit support)
    var existingCategoryProducts by remember { mutableStateOf<List<ProductWithDetails>>(emptyList()) }
    var selectedExistingProduct by remember { mutableStateOf<ProductWithDetails?>(null) }
    var productDropdownExpanded by rememberSaveable { mutableStateOf(false) }
    var existingBaseStock by rememberSaveable { mutableIntStateOf(0) }
    val deletedImageIds = remember { mutableStateListOf<String>() }

    // Product Photos
    val productPhotos = remember { mutableStateListOf<GalleryItemDraft>() }

    // Variants & Draft Editor
    val variants = remember { mutableStateListOf<NewVariantDraft>() }
    var showVariantInputs by rememberSaveable { mutableStateOf(true) }
    var variantColor by rememberSaveable { mutableStateOf("") }
    var variantSize by rememberSaveable { mutableStateOf("") }
    var variantSku by rememberSaveable { mutableStateOf("") }
    var variantBarcode by rememberSaveable { mutableStateOf("") }
    var variantQtyStr by rememberSaveable { mutableStateOf("") }
    var variantCostStr by rememberSaveable { mutableStateOf("") }
    var variantPriceStr by rememberSaveable { mutableStateOf("") }
    val variantPhotos = remember { mutableStateListOf<GalleryItemDraft>() }
    var editingVariantIdx by remember { mutableStateOf<Int?>(null) }
    var variantErrorMessage by remember { mutableStateOf<String?>(null) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var showAddCategoryDialog by rememberSaveable { mutableStateOf(false) }
    var showCategorySheet by rememberSaveable { mutableStateOf(false) }
    var showWarehouseSheet by rememberSaveable { mutableStateOf(false) }
    var showUnsavedDiscardDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(selectedCategory?.id) {
        val catId = selectedCategory?.id
        existingCategoryProducts = if (catId != null && onLoadCategoryProducts != null) {
            onLoadCategoryProducts(catId)
        } else {
            emptyList()
        }
    }

    val clearVariantEditor: (Boolean) -> Unit = { keepOpen ->
        variantColor = ""
        variantSize = ""
        variantSku = ""
        variantBarcode = ""
        variantCostStr = ""
        variantPriceStr = ""
        variantQtyStr = ""
        variantPhotos.clear()
        editingVariantIdx = null
        variantErrorMessage = null
        showVariantInputs = keepOpen
    }

    val clearToNewProduct: () -> Unit = {
        selectedExistingProduct = null
        nameEn = ""
        nameBn = ""
        baseSku = ""
        barcode = ""
        unit = "PCS"
        purchasePriceStr = ""
        sellingPriceStr = ""
        minStockAlertStr = "10"
        initialStockStr = "0"
        description = ""
        existingBaseStock = 0
        productPhotos.clear()
        deletedImageIds.clear()
        variants.clear()
        variantsEnabled = initialVariantsEnabled || currentStep == 2
        clearVariantEditor(true)
        errorMessage = null
    }

    val selectProduct: (ProductWithDetails) -> Unit = { p ->
        selectedExistingProduct = p
        editingVariantIdx = null
        errorMessage = null
        scope.launch {
            val bundle = onLoadProductRestockBundle?.invoke(p.id)
            if (bundle != null) {
                nameEn = bundle.product.nameEn
                nameBn = bundle.product.nameBn
                baseSku = bundle.product.baseSku
                barcode = bundle.product.barcode ?: ""
                unit = bundle.product.unit
                purchasePriceStr = if (bundle.product.purchasePriceMinor > 0L) {
                    formatDecimalAmount(bundle.product.purchasePriceMinor)
                } else ""
                sellingPriceStr = if (bundle.product.sellingPriceMinor > 0L) {
                    formatDecimalAmount(bundle.product.sellingPriceMinor)
                } else ""
                minStockAlertStr = bundle.product.minStockAlert.toString()
                description = normalizeProductDescription(bundle.product.description ?: "")
                existingBaseStock = bundle.baseStock
                initialStockStr = "0"

                productPhotos.clear()
                productPhotos.addAll(bundle.productPhotos)
                deletedImageIds.clear()

                variants.clear()
                variants.addAll(bundle.variants)
                variantsEnabled = bundle.variants.isNotEmpty()
                clearVariantEditor(bundle.variants.isEmpty())
            }
        }
    }

    val hasUnsavedChanges = nameEn.isNotBlank() ||
        nameBn.isNotBlank() ||
        baseSku.isNotBlank() ||
        barcode.isNotBlank() ||
        purchasePriceStr.isNotBlank() ||
        sellingPriceStr.isNotBlank() ||
        description.isNotBlank() ||
        productPhotos.isNotEmpty() ||
        variants.isNotEmpty() ||
        (initialStockStr != "0" && initialStockStr.isNotBlank()) ||
        (minStockAlertStr != "10" && minStockAlertStr.isNotBlank()) ||
        selectedExistingProduct != null ||
        variantColor.isNotBlank() ||
        variantSize.isNotBlank() ||
        variantSku.isNotBlank() ||
        variantBarcode.isNotBlank() ||
        variantPhotos.isNotEmpty()

    val handleCloseRequest: () -> Unit = {
        if (!isSubmitting) {
            if (hasUnsavedChanges) {
                showUnsavedDiscardDialog = true
            } else {
                onDismiss()
            }
        }
    }

    BackHandler(onBack = handleCloseRequest)

    val handleSubmit: () -> Unit = {
        if (!isSubmitting) {
            when {
                selectedCategory == null -> {
                    currentStep = 0
                    errorMessage = if (isBangla) "অনুগ্রহ করে একটি ক্যাটাগরি নির্বাচন করুন" else "Please select a category"
                }
                nameEn.isBlank() -> {
                    currentStep = 0
                    errorMessage = if (isBangla) "পণ্যের ইংরেজি নাম দেওয়া আবশ্যক" else "Product English name cannot be empty"
                }
                baseSku.isBlank() -> {
                    currentStep = 0
                    errorMessage = if (isBangla) "পণ্যের SKU দেওয়া আবশ্যক" else "Base SKU cannot be empty"
                }
                !hideStock && selectedWarehouse == null -> {
                    currentStep = 1
                    errorMessage = if (isBangla) "অনুগ্রহ করে একটি ওয়্যারহাউস নির্বাচন করুন" else "Please select a warehouse"
                }
                else -> {
                    val activeVariants = if (variantsEnabled || (selectedExistingProduct != null && variants.any { it.isExistingVariant })) {
                        variants.toList()
                    } else {
                        emptyList()
                    }
                    val purchaseMinor = parseSafeMinor(purchasePriceStr)
                    val sellingMinor = parseSafeMinor(sellingPriceStr)
                    val minStock = minStockAlertStr.toIntOrNull()?.coerceAtLeast(0) ?: 10
                    val effectiveStock = computeEffectiveAddProductStock(
                        hideStock = hideStock,
                        variantsEnabled = variantsEnabled || activeVariants.isNotEmpty(),
                        variants = activeVariants,
                        initialStockInput = initialStockStr
                    )
                    val whId = if (hideStock) "" else (selectedWarehouse?.id ?: "")
                    val excludeId = selectedExistingProduct?.id
                    val cleanDescription = normalizeProductDescription(description).trim().ifBlank { null }

                    if (purchaseMinor < 0L || sellingMinor < 0L) {
                        currentStep = 1
                        errorMessage = if (isBangla) "মূল্য সঠিক সংখ্যায় লিখুন" else "Please enter valid prices"
                    } else {
                        errorMessage = null
                        isSubmitting = true
                        scope.launch {
                            try {
                                val normalizedBaseSku = baseSku.trim().uppercase()
                                val skuOk = if (onValidateSku != null) {
                                    onValidateSku(normalizedBaseSku, excludeId)
                                } else true

                                if (!skuOk) {
                                    currentStep = 0
                                    errorMessage = if (isBangla) {
                                        "SKU '$normalizedBaseSku' ইতিমধ্যে ব্যবহৃত হয়েছে"
                                    } else {
                                        "Base SKU '$normalizedBaseSku' is already in use"
                                    }
                                    isSubmitting = false
                                    return@launch
                                }

                                val cleanBarcode = barcode.trim().ifBlank { null }
                                val barcodeOk = if (onValidateBarcode != null && cleanBarcode != null) {
                                    onValidateBarcode(cleanBarcode, excludeId)
                                } else true

                                if (!barcodeOk) {
                                    currentStep = 1
                                    errorMessage = if (isBangla) {
                                        "বারকোড '$cleanBarcode' ইতিমধ্যে ব্যবহৃত হয়েছে"
                                    } else {
                                        "Barcode '$cleanBarcode' is already in use"
                                    }
                                    isSubmitting = false
                                    return@launch
                                }

                                val isRestock = selectedExistingProduct != null
                                val nameBnFinal = if (nameBn.isBlank()) nameEn.trim() else nameBn.trim()
                                val unitFinal = if (unit.isBlank()) "PCS" else unit.trim()

                                if (isRestock && onUpdateExistingProduct != null && selectedExistingProduct != null) {
                                    onUpdateExistingProduct(
                                        selectedExistingProduct!!.id,
                                        nameEn.trim(),
                                        nameBnFinal,
                                        normalizedBaseSku,
                                        cleanBarcode,
                                        selectedCategory.id,
                                        unitFinal,
                                        purchaseMinor,
                                        sellingMinor,
                                        minStock,
                                        productPhotos.toList(),
                                        activeVariants,
                                        deletedImageIds.toList(),
                                        effectiveStock,
                                        whId,
                                        cleanDescription
                                    )
                                } else {
                                    onConfirm(
                                        nameEn.trim(),
                                        nameBnFinal,
                                        normalizedBaseSku,
                                        cleanBarcode,
                                        selectedCategory.id,
                                        unitFinal,
                                        purchaseMinor,
                                        sellingMinor,
                                        minStock,
                                        productPhotos.toList(),
                                        activeVariants.map { if (hideStock) it.copy(initialStock = 0) else it },
                                        effectiveStock,
                                        whId,
                                        cleanDescription
                                    )
                                }
                            } catch (t: Throwable) {
                                errorMessage = t.message ?: if (isBangla) "সংরক্ষণ ব্যর্থ হয়েছে" else "Save failed"
                            } finally {
                                isSubmitting = false
                            }
                        }
                    }
                }
            }
        }
    }

    ERPFullScreenEditor(
        onDismissRequest = handleCloseRequest,
        applySafeDrawingPadding = true,
        lightStatusBarIcons = null,
        statusBarColor = palette.headerGradientStart.toArgb(),
        navigationBarColor = palette.footerBg.toArgb()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(palette.pageBg)
                    .imePadding()
            ) {
            val headerTitle = if (selectedExistingProduct != null) {
                if (isBangla) "পণ্য স্টক ও সম্পাদনা" else "Restock & Edit Product"
            } else {
                if (isBangla) "পণ্য যোগ করুন" else "Add Product"
            }
            val headerSubtitle = if (selectedExistingProduct != null) {
                "${selectedExistingProduct?.nameEn} • ${selectedExistingProduct?.baseSku}"
            } else {
                if (isBangla) "নতুন পণ্য তথ্য যুক্ত করুন" else "Add new product details"
            }

            AddProductWaveHeader(
                title = headerTitle,
                subtitle = headerSubtitle,
                palette = palette,
                onClose = handleCloseRequest
            )

            AddProductStepBar(
                currentStep = currentStep,
                variantsEnabled = variantsEnabled,
                language = language,
                palette = palette,
                onStepSelected = { target ->
                    errorMessage = null
                    if (target == 2 && !variantsEnabled) {
                        variantsEnabled = true
                    }
                    currentStep = target
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Inline Error Banner
            if (errorMessage != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = palette.dangerContainer,
                    border = BorderStroke(1.dp, palette.danger.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = palette.danger,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = palette.danger,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { errorMessage = null },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Dismiss error",
                                tint = palette.danger,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Scrollable Step Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        val dir = if (targetState >= initialState) 1 else -1
                        (slideInHorizontally { width -> dir * width / 4 } + fadeIn())
                            .togetherWith(slideOutHorizontally { width -> -dir * width / 4 } + fadeOut())
                    },
                    label = "addProductStepTransition"
                ) { step ->
                    when (step) {
                        0 -> AddProductStep1BasicInfo(
                            isBangla = isBangla,
                            language = language,
                            palette = palette,
                            unit = unit,
                            selectedExistingProduct = selectedExistingProduct,
                            existingBaseStock = existingBaseStock,
                            productPhotos = productPhotos,
                            onProductPhotosChanged = { updated ->
                                val updatedIds = updated.map { it.id }.toSet()
                                for (img in productPhotos) {
                                    if (!updatedIds.contains(img.id) &&
                                        !img.id.startsWith("draft-") &&
                                        !deletedImageIds.contains(img.id)
                                    ) {
                                        deletedImageIds.add(img.id)
                                    }
                                }
                                productPhotos.clear()
                                productPhotos.addAll(updated)
                            },
                            selectedCategory = selectedCategory,
                            onOpenCategorySheet = { showCategorySheet = true },
                            existingCategoryProducts = existingCategoryProducts,
                            productDropdownExpanded = productDropdownExpanded,
                            onProductDropdownExpandedChange = { productDropdownExpanded = it },
                            onSelectExistingProduct = selectProduct,
                            onClearToNewProduct = clearToNewProduct,
                            nameEn = nameEn,
                            onNameEnChange = {
                                nameEn = it
                                if (errorMessage != null) errorMessage = null
                            },
                            nameBn = nameBn,
                            onNameBnChange = { nameBn = it },
                            baseSku = baseSku,
                            onBaseSkuChange = {
                                baseSku = it.uppercase()
                                if (errorMessage != null) errorMessage = null
                            },
                            onAutoGenerateSku = if (onGenerateSku != null) {
                                {
                                    val cat = selectedCategory
                                    if (cat != null) {
                                        scope.launch {
                                            val seedName = if (nameEn.isBlank()) "PRD" else nameEn
                                            baseSku = onGenerateSku(cat.id, seedName)
                                            if (errorMessage != null) errorMessage = null
                                        }
                                    }
                                }
                            } else null,
                            isNameEnError = errorMessage != null && nameEn.isBlank(),
                            isBaseSkuError = errorMessage != null && baseSku.isBlank()
                        )

                        1 -> AddProductStep2OtherInfo(
                            isBangla = isBangla,
                            language = language,
                            palette = palette,
                            hideStock = hideStock,
                            isRestock = selectedExistingProduct != null,
                            existingBaseStock = existingBaseStock,
                            barcode = barcode,
                            onBarcodeChange = {
                                barcode = it
                                if (errorMessage != null) errorMessage = null
                            },
                            onAutoGenerateBarcode = if (onGenerateBarcode != null) {
                                {
                                    scope.launch {
                                        barcode = onGenerateBarcode()
                                    }
                                }
                            } else null,
                            purchasePriceStr = purchasePriceStr,
                            onPurchasePriceChange = { purchasePriceStr = filterDecimalInput(it) },
                            sellingPriceStr = sellingPriceStr,
                            onSellingPriceChange = { sellingPriceStr = filterDecimalInput(it) },
                            minStockAlertStr = minStockAlertStr,
                            onMinStockAlertChange = { minStockAlertStr = it.filter(Char::isDigit) },
                            initialStockStr = initialStockStr,
                            onInitialStockChange = { initialStockStr = it.filter(Char::isDigit) },
                            variantStockTotal = if (variantsEnabled) variants.sumOf { it.initialStock } else 0,
                            unit = unit,
                            onUnitChange = { unit = it },
                            selectedWarehouse = selectedWarehouse,
                            onOpenWarehouseSheet = { showWarehouseSheet = true },
                            description = description,
                            onDescriptionChange = { description = normalizeProductDescription(it) },
                            variantsEnabled = variantsEnabled,
                            onVariantsEnabledChange = { enabled ->
                                variantsEnabled = enabled
                                if (enabled && variants.isEmpty()) {
                                    showVariantInputs = true
                                }
                            }
                        )

                        else -> AddProductStep3Variants(
                            isBangla = isBangla,
                            language = language,
                            palette = palette,
                            hideStock = hideStock,
                            unit = unit,
                            baseSku = baseSku,
                            baseBarcode = barcode,
                            basePurchasePriceStr = purchasePriceStr,
                            baseSellingPriceStr = sellingPriceStr,
                            productPhotos = productPhotos,
                            variants = variants,
                            showVariantInputs = showVariantInputs,
                            onToggleShowVariantInputs = { show ->
                                if (!show && variants.isNotEmpty()) {
                                    clearVariantEditor(false)
                                } else {
                                    clearVariantEditor(true)
                                }
                            },
                            editingVariantIdx = editingVariantIdx,
                            variantColor = variantColor,
                            onVariantColorChange = { variantColor = it },
                            variantSize = variantSize,
                            onVariantSizeChange = { variantSize = it },
                            variantSku = variantSku,
                            onVariantSkuChange = {
                                variantSku = it.uppercase()
                                variantErrorMessage = null
                            },
                            variantBarcode = variantBarcode,
                            onVariantBarcodeChange = {
                                variantBarcode = it
                                variantErrorMessage = null
                            },
                            variantCostStr = variantCostStr,
                            onVariantCostChange = { variantCostStr = filterDecimalInput(it) },
                            variantPriceStr = variantPriceStr,
                            onVariantPriceChange = { variantPriceStr = filterDecimalInput(it) },
                            variantQtyStr = variantQtyStr,
                            onVariantQtyChange = { variantQtyStr = it.filter(Char::isDigit) },
                            variantPhotos = variantPhotos,
                            onVariantPhotosChanged = { updated ->
                                variantPhotos.clear()
                                variantPhotos.addAll(updated)
                            },
                            variantErrorMessage = variantErrorMessage,
                            onAutoGenerateVariantSku = if (onGenerateVariantSku != null) {
                                {
                                    scope.launch {
                                        variantSku = onGenerateVariantSku(
                                            baseSku.ifBlank { "PRD" },
                                            variantColor.ifBlank { null },
                                            variantSize.ifBlank { null }
                                        )
                                        variantErrorMessage = null
                                    }
                                }
                            } else null,
                            onAutoGenerateVariantBarcode = if (onGenerateBarcode != null) {
                                {
                                    scope.launch {
                                        variantBarcode = onGenerateBarcode()
                                        variantErrorMessage = null
                                    }
                                }
                            } else null,
                            onSaveVariantDraft = {
                                val skuToUse = if (variantSku.isNotBlank()) {
                                    variantSku.trim().uppercase()
                                } else {
                                    "${baseSku.ifBlank { "PRD" }.trim().uppercase()}-${variants.size + 1}"
                                }
                                val dupError = findVariantDuplicateError(
                                    candidateSku = skuToUse,
                                    candidateBarcode = variantBarcode,
                                    baseSku = baseSku,
                                    baseBarcode = barcode,
                                    existingVariants = variants,
                                    editingIndex = editingVariantIdx,
                                    isBangla = isBangla
                                )
                                if (dupError != null) {
                                    variantErrorMessage = dupError
                                } else {
                                    val costMinor = parseSafeMinor(variantCostStr.ifBlank { purchasePriceStr })
                                    val priceMinor = parseSafeMinor(variantPriceStr.ifBlank { sellingPriceStr })
                                    val qty = variantQtyStr.toIntOrNull()?.coerceAtLeast(0) ?: 0
                                    val editIdx = editingVariantIdx
                                    if (editIdx != null && editIdx in variants.indices) {
                                        val existing = variants[editIdx]
                                        variants[editIdx] = existing.copy(
                                            sku = skuToUse,
                                            color = variantColor.trim(),
                                            size = variantSize.trim(),
                                            costMinor = costMinor,
                                            priceMinor = priceMinor,
                                            barcode = variantBarcode.trim().ifBlank { null },
                                            photos = variantPhotos.toList(),
                                            initialStock = if (hideStock) existing.initialStock else qty
                                        )
                                    } else {
                                        variants.add(
                                            NewVariantDraft(
                                                sku = skuToUse,
                                                color = variantColor.trim(),
                                                size = variantSize.trim(),
                                                costMinor = costMinor,
                                                priceMinor = priceMinor,
                                                barcode = variantBarcode.trim().ifBlank { null },
                                                photos = variantPhotos.toList(),
                                                initialStock = if (hideStock) 0 else qty,
                                                existingStock = 0,
                                                isExistingVariant = false
                                            )
                                        )
                                    }
                                    variantsEnabled = true
                                    clearVariantEditor(false)
                                }
                            },
                            onStartEditVariant = { idx ->
                                val item = variants[idx]
                                editingVariantIdx = idx
                                variantColor = item.color
                                variantSize = item.size
                                variantSku = item.sku
                                variantBarcode = item.barcode ?: ""
                                variantCostStr = if (item.costMinor > 0L) formatDecimalAmount(item.costMinor) else ""
                                variantPriceStr = if (item.priceMinor > 0L) formatDecimalAmount(item.priceMinor) else ""
                                variantQtyStr = if (item.initialStock > 0) item.initialStock.toString() else ""
                                variantPhotos.clear()
                                variantPhotos.addAll(item.photos)
                                variantErrorMessage = null
                                showVariantInputs = true
                            },
                            onRemoveVariant = { idx ->
                                if (idx in variants.indices) {
                                    variants.removeAt(idx)
                                    if (editingVariantIdx == idx) {
                                        clearVariantEditor(variants.isEmpty())
                                    } else if (editingVariantIdx != null && editingVariantIdx!! > idx) {
                                        editingVariantIdx = editingVariantIdx!! - 1
                                    }
                                    if (variants.isEmpty()) {
                                        showVariantInputs = true
                                    }
                                }
                            },
                            onUpdateVariantStock = { idx, newQty ->
                                if (idx in variants.indices) {
                                    variants[idx] = variants[idx].copy(initialStock = newQty.coerceAtLeast(0))
                                }
                            }
                        )
                    }
                }
            }

            AddProductStickyFooter(
                currentStep = currentStep,
                variantsEnabled = variantsEnabled,
                isRestock = selectedExistingProduct != null,
                isSubmitting = isSubmitting,
                language = language,
                palette = palette,
                onPrevious = {
                    errorMessage = null
                    if (currentStep > 0) currentStep -= 1
                },
                onNext = {
                    when (currentStep) {
                        0 -> {
                            when {
                                selectedCategory == null -> {
                                    errorMessage = if (isBangla) "অনুগ্রহ করে একটি ক্যাটাগরি নির্বাচন করুন" else "Please select a category"
                                }
                                nameEn.isBlank() -> {
                                    errorMessage = if (isBangla) "পণ্যের ইংরেজি নাম দেওয়া আবশ্যক" else "Product English name cannot be empty"
                                }
                                else -> {
                                    errorMessage = null
                                    currentStep = 1
                                }
                            }
                        }
                        1 -> {
                            errorMessage = null
                            if (variantsEnabled) currentStep = 2 else handleSubmit()
                        }
                        else -> errorMessage = null
                    }
                },
                onSubmit = handleSubmit
            )
            }

            if (showCategorySheet) {
                AddProductSearchableCategorySheet(
                    categories = categories,
                    selectedCategory = selectedCategory,
                    language = language,
                    palette = palette,
                    canCreateCategory = onAddNewCategory != null,
                    onSelectCategory = { cat ->
                        if (cat.id != selectedCategoryId) {
                            selectedCategoryId = cat.id
                            clearToNewProduct()
                        }
                    },
                    onRequestCreateCategory = { showAddCategoryDialog = true },
                    onDismiss = { showCategorySheet = false }
                )
            }

            if (showWarehouseSheet) {
                AddProductWarehouseSheet(
                    warehouses = warehouses,
                    selectedWarehouse = selectedWarehouse,
                    language = language,
                    palette = palette,
                    onSelectWarehouse = { wh -> selectedWarehouseId = wh.id },
                    onDismiss = { showWarehouseSheet = false }
                )
            }
        }
    }

    // Add Category Dialog
    if (showAddCategoryDialog) {
        var catEn by rememberSaveable { mutableStateOf("") }
        var catBn by rememberSaveable { mutableStateOf("") }
        var catErr by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            containerColor = palette.cardSurface,
            title = {
                Text(
                    text = if (isBangla) "নতুন ক্যাটাগরি যোগ করুন" else "Add New Category",
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (catErr != null) {
                        Text(text = catErr!!, color = palette.danger, style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedTextField(
                        value = catEn,
                        onValueChange = { catEn = it; catErr = null },
                        label = { Text(if (isBangla) "ক্যাটাগরির নাম (ইংরেজি) *" else "Category Name (English) *") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = addProductFieldColors(palette),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = catBn,
                        onValueChange = { catBn = it; catErr = null },
                        label = { Text(if (isBangla) "ক্যাটাগরির নাম (বাংলা)" else "Category Name (Bangla)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = addProductFieldColors(palette),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (catEn.isBlank()) {
                            catErr = if (isBangla) "ইংরেজি নাম দেওয়া আবশ্যক" else "English name required"
                            return@TextButton
                        }
                        onAddNewCategory?.invoke(catEn.trim(), catBn.trim()) { newCat ->
                            selectedCategoryId = newCat.id
                            showAddCategoryDialog = false
                        }
                    },
                    modifier = Modifier.testTag("add_category_confirm_btn")
                ) {
                    Text(
                        text = if (isBangla) "নিশ্চিত করুন" else "Confirm",
                        color = palette.primaryPurple,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text(
                        text = if (isBangla) "বাতিল" else "Cancel",
                        color = palette.textSecondary
                    )
                }
            }
        )
    }

    // Unsaved Changes Confirmation
    if (showUnsavedDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDiscardDialog = false },
            containerColor = palette.cardSurface,
            title = {
                Text(
                    text = if (isBangla) "অসংরক্ষিত তথ্য বাদ দেবেন?" else "Discard unsaved changes?",
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
            },
            text = {
                Text(
                    text = if (isBangla) {
                        "আপনি পণ্যের কিছু তথ্য লিখেছেন। এখন বন্ধ করলে এই তথ্যগুলো মুছে যাবে।"
                    } else {
                        "You have entered product details. Closing now will discard them."
                    },
                    color = palette.textSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUnsavedDiscardDialog = false
                        onDismiss()
                    }
                ) {
                    Text(
                        text = if (isBangla) "বাদ দিন" else "Discard",
                        color = palette.danger,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnsavedDiscardDialog = false }) {
                    Text(
                        text = if (isBangla) "সম্পাদনা চালান" else "Keep Editing",
                        color = palette.primaryPurple,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }
}

@Composable
private fun AddProductStep1BasicInfo(
    isBangla: Boolean,
    language: AppLanguage,
    palette: AddProductPalette,
    unit: String,
    selectedExistingProduct: ProductWithDetails?,
    existingBaseStock: Int,
    productPhotos: List<GalleryItemDraft>,
    onProductPhotosChanged: (List<GalleryItemDraft>) -> Unit,
    selectedCategory: ProductCategory?,
    onOpenCategorySheet: () -> Unit,
    existingCategoryProducts: List<ProductWithDetails>,
    productDropdownExpanded: Boolean,
    onProductDropdownExpandedChange: (Boolean) -> Unit,
    onSelectExistingProduct: (ProductWithDetails) -> Unit,
    onClearToNewProduct: () -> Unit,
    nameEn: String,
    onNameEnChange: (String) -> Unit,
    nameBn: String,
    onNameBnChange: (String) -> Unit,
    baseSku: String,
    onBaseSkuChange: (String) -> Unit,
    onAutoGenerateSku: (() -> Unit)?,
    isNameEnError: Boolean = false,
    isBaseSkuError: Boolean = false
) {
    val scrollState = rememberScrollState()
    val fieldColors = addProductFieldColors(palette)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        if (selectedExistingProduct != null) {
            AddProductBannerInfoCard(
                title = if (isBangla) "বিদ্যমান পণ্যের স্টক বৃদ্ধি মোড সক্রিয়" else "Existing Product Restock Mode Active",
                subtitle = if (isBangla) {
                    "পূর্বের মোট মজুদ: $existingBaseStock $unit • নতুন স্টক বা ভ্যারিয়েন্ট আপডেট করুন"
                } else {
                    "Current Stock: $existingBaseStock $unit • Add extra quantity or edit details"
                },
                palette = palette,
                actionLabel = if (isBangla) "+ নতুন তৈরি" else "+ New",
                onActionClick = onClearToNewProduct
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        // 1. Product Photos
        AddProductPhotoUploader(
            title = if (isBangla) "পণ্যের ছবি" else "Product Photos",
            photos = productPhotos,
            onPhotosChanged = onProductPhotosChanged,
            maxPhotos = 10,
            language = language,
            palette = palette,
            showSourceButtons = true
        )

        Spacer(modifier = Modifier.height(18.dp))

        // 2. Category Selection (always visible even when hideStock = true)
        AddProductFieldLabel(
            text = if (isBangla) "ক্যাটাগরি" else stringResource(R.string.category),
            required = true,
            palette = palette
        )
        AddProductSelectorCard(
            text = if (isBangla) selectedCategory?.nameBn.orEmpty() else selectedCategory?.nameEn.orEmpty(),
            placeholder = if (isBangla) "ক্যাটাগরি নির্বাচন করুন" else "Select Category",
            leadingIcon = Icons.Outlined.GridView,
            palette = palette,
            isSelected = selectedCategory != null,
            onClick = onOpenCategorySheet,
            testTag = "add_product_category_selector"
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 2.1 Existing Category Products Info / Restock Selector
        if (selectedCategory != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = palette.lavenderTint,
                border = BorderStroke(1.dp, palette.lavenderBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = palette.primaryPurple,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isBangla) {
                                "এই ক্যাটাগরির পণ্যসমূহ (${existingCategoryProducts.size})"
                            } else {
                                "Products in this Category (${existingCategoryProducts.size})"
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.primaryPurple,
                            modifier = Modifier.weight(1f)
                        )
                        if (selectedExistingProduct != null) {
                            TextButton(
                                onClick = onClearToNewProduct,
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                            ) {
                                Text(
                                    text = if (isBangla) "+ নতুন পণ্য মোড" else "+ New Product",
                                    fontSize = 12.sp,
                                    color = palette.premiumTeal,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (existingCategoryProducts.isEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isBangla) {
                                "এই ক্যাটাগরিতে এখনো কোন পণ্য নেই।\nনিচে নতুন পণ্যের তথ্য দিন।"
                            } else {
                                "No existing products in this category yet.\nEnter new product details below."
                            },
                            fontSize = 12.5.sp,
                            color = palette.textSecondary,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(start = 28.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                onClick = { onProductDropdownExpandedChange(true) },
                                shape = RoundedCornerShape(12.dp),
                                color = palette.cardSurface,
                                border = BorderStroke(
                                    1.dp,
                                    if (selectedExistingProduct != null) palette.primaryPurple else palette.fieldBorder
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (selectedExistingProduct != null) {
                                            "${selectedExistingProduct.nameEn} (${selectedExistingProduct.baseSku}) • ${selectedExistingProduct.totalStock} $unit"
                                        } else {
                                            if (isBangla) {
                                                "বিদ্যমান পণ্য সিলেক্ট করুন অথবা নতুন তৈরি করুন"
                                            } else {
                                                "Select existing product to restock or create new"
                                            }
                                        },
                                        fontSize = 12.5.sp,
                                        fontWeight = if (selectedExistingProduct != null) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedExistingProduct != null) palette.primaryPurple else palette.textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = Icons.Outlined.ArrowDropDown,
                                        contentDescription = null,
                                        tint = if (selectedExistingProduct != null) palette.primaryPurple else palette.textSecondary
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = productDropdownExpanded,
                                onDismissRequest = { onProductDropdownExpandedChange(false) }
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (isBangla) "+ নতুন পণ্য তৈরি করুন" else "+ Create Brand New Product",
                                            fontWeight = FontWeight.Bold,
                                            color = palette.premiumTeal
                                        )
                                    },
                                    onClick = {
                                        onProductDropdownExpandedChange(false)
                                        onClearToNewProduct()
                                    }
                                )
                                HorizontalDivider(color = palette.fieldBorder)
                                existingCategoryProducts.forEach { p ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = "${p.nameEn} (${p.baseSku})",
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = palette.textPrimary
                                                )
                                                Text(
                                                    text = "${if (isBangla) "বর্তমান স্টক:" else "Stock:"} ${p.totalStock} ${p.unit} • ${p.variantCount} ${if (isBangla) "ভ্যারিয়েন্ট" else "variants"}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = palette.textSecondary
                                                )
                                            }
                                        },
                                        onClick = {
                                            onProductDropdownExpandedChange(false)
                                            onSelectExistingProduct(p)
                                        }
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = selectedExistingProduct == null,
                                onClick = onClearToNewProduct,
                                label = {
                                    Text(
                                        text = if (isBangla) "+ নতুন পণ্য" else "+ New Product",
                                        fontSize = 12.sp,
                                        fontWeight = if (selectedExistingProduct == null) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = palette.primaryButtonFill,
                                    selectedLabelColor = Color.White
                                )
                            )
                            existingCategoryProducts.forEach { p ->
                                val isSel = selectedExistingProduct?.id == p.id
                                FilterChip(
                                    selected = isSel,
                                    onClick = {
                                        if (isSel) onClearToNewProduct() else onSelectExistingProduct(p)
                                    },
                                    label = {
                                        Text(
                                            text = "${p.nameEn} (${p.totalStock})",
                                            fontSize = 12.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = palette.primaryButtonFill,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 3. Product Name (English) *
        AddProductFieldLabel(
            text = if (isBangla) "পণ্যের নাম (ইংরেজি)" else stringResource(R.string.product_name_en),
            required = true,
            palette = palette
        )
        OutlinedTextField(
            value = nameEn,
            onValueChange = onNameEnChange,
            placeholder = {
                Text(
                    text = "Product Name (English)",
                    color = palette.textMuted
                )
            },
            leadingIcon = {
                Text(
                    text = "A",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textSecondary
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_product_name_en"),
            isError = isNameEnError,
            shape = RoundedCornerShape(14.dp),
            colors = fieldColors,
            singleLine = true
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 4. Product Name (Bangla)
        AddProductFieldLabel(
            text = if (isBangla) "পণ্যের নাম (বাংলা)" else stringResource(R.string.product_name_bn),
            required = false,
            palette = palette
        )
        OutlinedTextField(
            value = nameBn,
            onValueChange = onNameBnChange,
            placeholder = {
                Text(
                    text = if (isBangla) "পণ্যের নাম (বাংলা)" else "Product Name (Bangla)",
                    color = palette.textMuted
                )
            },
            leadingIcon = {
                Text(
                    text = "অ",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textSecondary
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_product_name_bn"),
            shape = RoundedCornerShape(14.dp),
            colors = fieldColors,
            singleLine = true
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 5. Base SKU + Auto-generate Button
        AddProductFieldLabel(
            text = if (isBangla) "এসকেইউ (SKU)" else stringResource(R.string.sku),
            required = true,
            palette = palette
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = baseSku,
                onValueChange = onBaseSkuChange,
                placeholder = {
                    Text(
                        text = if (isBangla) "SKU (যেমন: PRD-001)" else "SKU (e.g. PRD-001)",
                        color = palette.textMuted
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.ViewWeek,
                        contentDescription = null,
                        tint = palette.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("add_product_base_sku"),
                isError = isBaseSkuError,
                shape = RoundedCornerShape(14.dp),
                colors = fieldColors,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                singleLine = true
            )

            if (onAutoGenerateSku != null) {
                AddProductMagicButton(
                    tint = palette.premiumTeal,
                    containerColor = palette.cyanTint,
                    borderColor = palette.cyanBorder,
                    contentDescription = "Auto SKU",
                    onClick = onAutoGenerateSku,
                    modifier = Modifier.testTag("add_product_auto_sku_btn")
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddProductStep2OtherInfo(
    isBangla: Boolean,
    language: AppLanguage,
    palette: AddProductPalette,
    hideStock: Boolean,
    isRestock: Boolean,
    existingBaseStock: Int,
    barcode: String,
    onBarcodeChange: (String) -> Unit,
    onAutoGenerateBarcode: (() -> Unit)?,
    purchasePriceStr: String,
    onPurchasePriceChange: (String) -> Unit,
    sellingPriceStr: String,
    onSellingPriceChange: (String) -> Unit,
    minStockAlertStr: String,
    onMinStockAlertChange: (String) -> Unit,
    initialStockStr: String,
    onInitialStockChange: (String) -> Unit,
    variantStockTotal: Int,
    unit: String,
    onUnitChange: (String) -> Unit,
    selectedWarehouse: Warehouse?,
    onOpenWarehouseSheet: () -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    variantsEnabled: Boolean,
    onVariantsEnabledChange: (Boolean) -> Unit
) {
    val scrollState = rememberScrollState()
    val fieldColors = addProductFieldColors(palette)
    val hasVariantStock = variantStockTotal > 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        // 1. Barcode + Auto-generate
        AddProductFieldLabel(
            text = if (isBangla) "বারকোড" else stringResource(R.string.barcode),
            palette = palette,
            modifier = Modifier.testTag("add_product_step2_top")
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = barcode,
                onValueChange = onBarcodeChange,
                placeholder = {
                    Text(
                        text = if (isBangla) "বারকোড" else "Barcode",
                        color = palette.textMuted
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.ViewWeek,
                        contentDescription = null,
                        tint = palette.primaryPurple,
                        modifier = Modifier.size(20.dp)
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("add_product_barcode"),
                shape = RoundedCornerShape(14.dp),
                colors = fieldColors,
                singleLine = true
            )

            if (onAutoGenerateBarcode != null) {
                AddProductMagicButton(
                    tint = palette.primaryPurple,
                    containerColor = palette.lavenderTint,
                    borderColor = palette.lavenderBorder,
                    contentDescription = "Auto Barcode",
                    onClick = onAutoGenerateBarcode,
                    modifier = Modifier.testTag("add_product_auto_barcode_btn")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Two-Column Prices: Purchase Cost & Selling Price
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                AddProductFieldLabel(
                    text = if (isBangla) "ক্রয় খরচ" else stringResource(R.string.purchase_price),
                    leadingIcon = Icons.Outlined.AccountBalanceWallet,
                    iconTint = palette.textSecondary,
                    palette = palette
                )
                OutlinedTextField(
                    value = purchasePriceStr,
                    onValueChange = onPurchasePriceChange,
                    placeholder = { Text(if (isBangla) "০.০০" else "0.00", color = palette.textMuted) },
                    leadingIcon = {
                        Text(
                            text = "৳",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textSecondary
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_product_purchase_price"),
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                AddProductFieldLabel(
                    text = if (isBangla) "বিক্রয় মূল্য" else stringResource(R.string.selling_price),
                    leadingIcon = Icons.Outlined.LocalOffer,
                    iconTint = palette.textSecondary,
                    palette = palette
                )
                OutlinedTextField(
                    value = sellingPriceStr,
                    onValueChange = onSellingPriceChange,
                    placeholder = { Text(if (isBangla) "০.০০" else "0.00", color = palette.textMuted) },
                    leadingIcon = {
                        Text(
                            text = "৳",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textSecondary
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_product_selling_price"),
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Two-Column Stock Threshold & Opening Stock
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.padding(bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.NotificationsNone,
                        contentDescription = null,
                        tint = palette.textSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isBangla) "স্বল্প স্টক সতর্কতা সীমা" else stringResource(R.string.min_stock_alert),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                OutlinedTextField(
                    value = minStockAlertStr,
                    onValueChange = onMinStockAlertChange,
                    placeholder = { Text("10", color = palette.textMuted) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Inventory2,
                            contentDescription = null,
                            tint = palette.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_product_min_stock"),
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }

            if (!hideStock) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.padding(bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Inventory2,
                            contentDescription = null,
                            tint = palette.textSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isRestock) {
                                if (isBangla) "নতুন স্টক যোগ ($unit)" else "Add Stock ($unit)"
                            } else {
                                if (isBangla) "প্রারম্ভিক মজুদ ($unit)" else "Opening Stock ($unit)"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    OutlinedTextField(
                        value = if (hasVariantStock) variantStockTotal.toString() else initialStockStr,
                        onValueChange = onInitialStockChange,
                        enabled = !hasVariantStock,
                        placeholder = { Text("0", color = palette.textMuted) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Inventory2,
                                contentDescription = null,
                                tint = palette.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        supportingText = if (isRestock) {
                            val addedNow = if (hasVariantStock) variantStockTotal else (initialStockStr.toIntOrNull() ?: 0)
                            {
                                Text(
                                    text = if (isBangla) {
                                        "বর্তমান: $existingBaseStock • যোগ: +$addedNow • মোট: ${existingBaseStock + addedNow}"
                                    } else {
                                        "Now: $existingBaseStock • Add: +$addedNow • Total: ${existingBaseStock + addedNow}"
                                    },
                                    fontSize = 11.sp,
                                    color = palette.premiumTeal
                                )
                            }
                        } else if (hasVariantStock) {
                            {
                                Text(
                                    text = if (isBangla) "ভ্যারিয়েন্টের মোট" else "Sum of variants",
                                    fontSize = 11.sp,
                                    color = palette.primaryPurple
                                )
                            }
                        } else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_product_opening_stock"),
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 4. Unit Selector (single compact scrollable row)
        AddProductFieldLabel(
            text = if (isBangla) "পণ্যের একক (Unit)" else stringResource(R.string.unit),
            palette = palette
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val unitsToShow = remember(unit) {
                if (unit.isNotBlank() && UNIT_PRESETS.none { it.equals(unit, ignoreCase = true) }) {
                    UNIT_PRESETS + unit.uppercase()
                } else {
                    UNIT_PRESETS
                }
            }
            unitsToShow.forEach { preset ->
                val selected = unit.equals(preset, ignoreCase = true)
                Surface(
                    onClick = { onUnitChange(preset) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (selected) palette.lavenderTint else palette.cardSurface,
                    border = BorderStroke(1.dp, if (selected) palette.primaryPurple else palette.fieldBorder)
                ) {
                    Text(
                        text = preset,
                        fontSize = 11.5.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) palette.primaryPurple else palette.textSecondary,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                    )
                }
            }
        }

        if (!hideStock) {
            Spacer(modifier = Modifier.height(12.dp))

            // 5. Warehouse Selection
            AddProductFieldLabel(
                text = if (isBangla) "ওয়্যারহাউস" else stringResource(R.string.warehouse),
                required = true,
                palette = palette
            )
            AddProductSelectorCard(
                text = selectedWarehouse?.name.orEmpty(),
                placeholder = if (isBangla) "ওয়্যারহাউস নির্বাচন করুন" else "Select Warehouse",
                leadingIcon = Icons.Outlined.Warehouse,
                palette = palette,
                isSelected = selectedWarehouse != null,
                onClick = onOpenWarehouseSheet,
                testTag = "add_product_warehouse_selector"
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 6. Product Description (500 chars)
        AddProductFieldLabel(
            text = if (isBangla) "পণ্যের বিবরণ" else "Product Description",
            leadingIcon = Icons.Outlined.Description,
            iconTint = palette.primaryPurple,
            palette = palette
        )
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            placeholder = {
                Text(
                    text = if (isBangla) "পণ্যের সংক্ষিপ্ত বিবরণ (ঐচ্ছিক)" else "Brief product description (optional)",
                    color = palette.textMuted
                )
            },
            supportingText = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "${description.length} / 500",
                        fontSize = 11.5.sp,
                        color = palette.textMuted
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 104.dp)
                .testTag("add_product_description"),
            shape = RoundedCornerShape(16.dp),
            colors = fieldColors,
            minLines = 2,
            maxLines = 4
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 7. Variant Enable / Disable Card
        AddProductVariantToggleCard(
            checked = variantsEnabled,
            onCheckedChange = onVariantsEnabledChange,
            language = language,
            palette = palette
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun AddProductStep3Variants(
    isBangla: Boolean,
    language: AppLanguage,
    palette: AddProductPalette,
    hideStock: Boolean,
    unit: String,
    baseSku: String,
    baseBarcode: String,
    basePurchasePriceStr: String,
    baseSellingPriceStr: String,
    productPhotos: List<GalleryItemDraft>,
    variants: List<NewVariantDraft>,
    showVariantInputs: Boolean,
    onToggleShowVariantInputs: (Boolean) -> Unit,
    editingVariantIdx: Int?,
    variantColor: String,
    onVariantColorChange: (String) -> Unit,
    variantSize: String,
    onVariantSizeChange: (String) -> Unit,
    variantSku: String,
    onVariantSkuChange: (String) -> Unit,
    variantBarcode: String,
    onVariantBarcodeChange: (String) -> Unit,
    variantCostStr: String,
    onVariantCostChange: (String) -> Unit,
    variantPriceStr: String,
    onVariantPriceChange: (String) -> Unit,
    variantQtyStr: String,
    onVariantQtyChange: (String) -> Unit,
    variantPhotos: List<GalleryItemDraft>,
    onVariantPhotosChanged: (List<GalleryItemDraft>) -> Unit,
    variantErrorMessage: String?,
    onAutoGenerateVariantSku: (() -> Unit)?,
    onAutoGenerateVariantBarcode: (() -> Unit)?,
    onSaveVariantDraft: () -> Unit,
    onStartEditVariant: (Int) -> Unit,
    onRemoveVariant: (Int) -> Unit,
    onUpdateVariantStock: (Int, Int) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val fieldColors = addProductFieldColors(palette)
    val productFallbackPhoto = remember(productPhotos) {
        productPhotos.firstOrNull { it.isPrimary } ?: productPhotos.firstOrNull()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        // Section Header: পণ্যের ভ্যারিয়েন্ট (N) + Close + যোগ করুন
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isBangla) "পণ্যের ভ্যারিয়েন্ট (${variants.size})" else "Product Variants (${variants.size})",
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary,
                modifier = Modifier.weight(1f)
            )
            if (showVariantInputs && variants.isNotEmpty()) {
                IconButton(
                    onClick = { onToggleShowVariantInputs(false) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close editor",
                        tint = palette.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Button(
                onClick = { onToggleShowVariantInputs(true) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.primaryButtonFill,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier
                    .heightIn(min = 38.dp)
                    .testTag("add_product_open_variant_editor")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isBangla) "যোগ করুন" else "Add",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Variant Editor Card
        if (showVariantInputs || variants.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = palette.cardSurface,
                border = BorderStroke(1.dp, palette.fieldBorder),
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    AddProductBannerInfoCard(
                        title = when {
                            editingVariantIdx != null && isBangla -> "ভ্যারিয়েন্ট সম্পাদনা করুন"
                            editingVariantIdx != null -> "Edit Variant Details"
                            isBangla -> "নতুন ভ্যারিয়েন্ট তথ্য যোগ করুন"
                            else -> "Add New Variant Details"
                        },
                        subtitle = if (isBangla) {
                            "রং, সাইজ, মডেল ইত্যাদি অনুযায়ী ভ্যারিয়েন্ট তৈরি করুন।"
                        } else {
                            "Create variations by color, size, model, or packaging."
                        },
                        palette = palette
                    )

                    if (variantErrorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = palette.dangerContainer,
                            border = BorderStroke(1.dp, palette.danger.copy(alpha = 0.45f))
                        ) {
                            Text(
                                text = variantErrorMessage,
                                color = palette.danger,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Two-column Color & Size with SuggestTextField
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            AddProductFieldLabel(
                                text = if (isBangla) "রং" else stringResource(R.string.color),
                                leadingIcon = Icons.Outlined.Palette,
                                iconTint = palette.primaryPurple,
                                palette = palette
                            )
                            SuggestTextField(
                                fieldType = SuggestionType.COLOR,
                                value = variantColor,
                                onValueChange = onVariantColorChange,
                                placeholder = {
                                    Text(
                                        text = if (isBangla) "রং নির্বাচন" else "Color",
                                        fontSize = 13.sp,
                                        color = palette.textMuted
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Palette,
                                        contentDescription = null,
                                        tint = palette.textSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = fieldColors,
                                modifier = Modifier.fillMaxWidth(),
                                inputModifier = Modifier.testTag("add_product_variant_color"),
                                singleLine = true
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            AddProductFieldLabel(
                                text = if (isBangla) "সাইজ" else stringResource(R.string.size),
                                leadingIcon = Icons.Outlined.Straighten,
                                iconTint = palette.primaryPurple,
                                palette = palette
                            )
                            SuggestTextField(
                                fieldType = SuggestionType.SIZE,
                                value = variantSize,
                                onValueChange = onVariantSizeChange,
                                placeholder = {
                                    Text(
                                        text = if (isBangla) "সাইজ" else "Size",
                                        fontSize = 13.sp,
                                        color = palette.textMuted
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Edit,
                                        contentDescription = null,
                                        tint = palette.textSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = fieldColors,
                                modifier = Modifier.fillMaxWidth(),
                                inputModifier = Modifier.testTag("add_product_variant_size"),
                                singleLine = true
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Variant SKU
                    AddProductFieldLabel(
                        text = if (isBangla) "ভ্যারিয়েন্ট এসকেইউ" else stringResource(R.string.variant_sku),
                        palette = palette
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = variantSku,
                            onValueChange = onVariantSkuChange,
                            placeholder = {
                                Text(
                                    text = if (baseSku.isNotBlank()) "$baseSku-V1" else if (isBangla) "ভ্যারিয়েন্ট SKU" else "Variant SKU",
                                    color = palette.textMuted
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.ViewWeek,
                                    contentDescription = null,
                                    tint = palette.primaryPurple,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("add_product_variant_sku"),
                            shape = RoundedCornerShape(14.dp),
                            colors = fieldColors,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                            singleLine = true
                        )
                        if (onAutoGenerateVariantSku != null) {
                            AddProductMagicButton(
                                tint = palette.premiumTeal,
                                containerColor = palette.cyanTint,
                                borderColor = palette.cyanBorder,
                                contentDescription = "Auto Variant SKU",
                                onClick = onAutoGenerateVariantSku,
                                modifier = Modifier.testTag("add_product_auto_variant_sku_btn")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Variant Barcode
                    AddProductFieldLabel(
                        text = if (isBangla) "ভ্যারিয়েন্ট বারকোড (ঐচ্ছিক)" else "Variant Barcode (Optional)",
                        palette = palette
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = variantBarcode,
                            onValueChange = onVariantBarcodeChange,
                            placeholder = {
                                Text(
                                    text = if (isBangla) "বারকোড (ঐচ্ছিক)" else "Barcode (Optional)",
                                    color = palette.textMuted
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.QrCode2,
                                    contentDescription = null,
                                    tint = palette.primaryPurple,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("add_product_variant_barcode"),
                            shape = RoundedCornerShape(14.dp),
                            colors = fieldColors,
                            singleLine = true
                        )
                        if (onAutoGenerateVariantBarcode != null) {
                            AddProductMagicButton(
                                tint = palette.primaryPurple,
                                containerColor = palette.lavenderTint,
                                borderColor = palette.lavenderBorder,
                                contentDescription = "Auto Variant Barcode",
                                onClick = onAutoGenerateVariantBarcode,
                                modifier = Modifier.testTag("add_product_auto_variant_barcode_btn")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Variant Cost & Selling Price
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.padding(bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = palette.textSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isBangla) "ভ্যারিয়েন্ট ক্রয় খরচ" else "Variant Cost",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            OutlinedTextField(
                                value = variantCostStr,
                                onValueChange = onVariantCostChange,
                                placeholder = {
                                    Text(
                                        text = basePurchasePriceStr.ifBlank { if (isBangla) "০.০০" else "0.00" },
                                        color = palette.textMuted
                                    )
                                },
                                leadingIcon = {
                                    Text(
                                        text = "৳",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.textSecondary
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("add_product_variant_cost"),
                                shape = RoundedCornerShape(14.dp),
                                colors = fieldColors,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.padding(bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.LocalOffer,
                                    contentDescription = null,
                                    tint = palette.textSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isBangla) "ভ্যারিয়েন্ট বিক্রয় মূল্য" else "Variant Price",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            OutlinedTextField(
                                value = variantPriceStr,
                                onValueChange = onVariantPriceChange,
                                placeholder = {
                                    Text(
                                        text = baseSellingPriceStr.ifBlank { if (isBangla) "০.০০" else "0.00" },
                                        color = palette.textMuted
                                    )
                                },
                                leadingIcon = {
                                    Text(
                                        text = "৳",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.textSecondary
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("add_product_variant_price"),
                                shape = RoundedCornerShape(14.dp),
                                colors = fieldColors,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true
                            )
                        }
                    }

                    if (!hideStock) {
                        Spacer(modifier = Modifier.height(12.dp))
                        AddProductFieldLabel(
                            text = if (isBangla) "এই ভ্যারিয়েন্টের পরিমাণ ($unit)" else "Variant Quantity ($unit)",
                            palette = palette
                        )
                        OutlinedTextField(
                            value = variantQtyStr,
                            onValueChange = onVariantQtyChange,
                            placeholder = { Text("0", color = palette.textMuted) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Inventory2,
                                    contentDescription = null,
                                    tint = palette.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_product_variant_qty"),
                            shape = RoundedCornerShape(14.dp),
                            colors = fieldColors,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Variant Photos (max 4)
                    AddProductPhotoUploader(
                        title = if (isBangla) "ভ্যারিয়েন্টের ছবি" else "Variant Photos",
                        photos = variantPhotos,
                        onPhotosChanged = onVariantPhotosChanged,
                        maxPhotos = 4,
                        language = language,
                        palette = palette,
                        showSourceButtons = false,
                        dropzoneTitle = if (isBangla) "ভ্যারিয়েন্টের ছবি নির্বাচন করুন" else "Select variant photos"
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Full-Width Teal Action Button
                    Button(
                        onClick = onSaveVariantDraft,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("add_product_save_variant_btn"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.tealButtonFill,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Layers,
                            contentDescription = null,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                editingVariantIdx != null && isBangla -> "ভ্যারিয়েন্ট আপডেট করুন"
                                editingVariantIdx != null -> "Update Variant"
                                isBangla -> "তালিকায় ভ্যারিয়েন্ট যোগ করুন"
                                else -> "Add Variant to List"
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Added Variants List
        if (variants.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            variants.forEachIndexed { idx, v ->
                val vPhoto = v.photos.firstOrNull { it.isPrimary } ?: v.photos.firstOrNull()
                val displayPhoto = vPhoto ?: productFallbackPhoto
                val usesFallback = vPhoto == null && productFallbackPhoto != null

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = palette.cardSurface,
                    border = BorderStroke(
                        1.dp,
                        if (v.isExistingVariant) palette.cyanBorder else palette.lavenderBorder
                    ),
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(palette.lavenderTint)
                                    .border(1.dp, palette.fieldBorder, RoundedCornerShape(10.dp))
                            ) {
                                if (displayPhoto != null) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(rememberImageModel(displayPhoto.uriString))
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = v.sku,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    if (usesFallback) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .fillMaxWidth()
                                                .background(Color.Black.copy(alpha = 0.55f))
                                                .padding(vertical = 1.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (isBangla) "মূল" else "BASE",
                                                fontSize = 8.sp,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.Image,
                                        contentDescription = null,
                                        tint = palette.textMuted,
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .size(22.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = v.sku,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (v.isExistingVariant) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(palette.cyanTint)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (isBangla) "বিদ্যমান" else "Existing",
                                                fontSize = 10.sp,
                                                color = palette.premiumTeal,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = if (isBangla) {
                                        "রং: ${v.color.ifBlank { "-" }} • সাইজ: ${v.size.ifBlank { "-" }}"
                                    } else {
                                        "Color: ${v.color.ifBlank { "-" }} • Size: ${v.size.ifBlank { "-" }}"
                                    },
                                    fontSize = 12.sp,
                                    color = palette.textSecondary
                                )
                                if (!v.barcode.isNullOrBlank()) {
                                    Text(
                                        text = "Barcode: ${v.barcode}",
                                        fontSize = 11.sp,
                                        color = palette.textMuted
                                    )
                                }
                            }

                            IconButton(
                                onClick = { onStartEditVariant(idx) },
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("edit_variant_$idx")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = "Edit Variant",
                                    tint = palette.primaryPurple,
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            if (!v.isExistingVariant) {
                                IconButton(
                                    onClick = { onRemoveVariant(idx) },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .testTag("delete_variant_$idx")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.DeleteOutline,
                                        contentDescription = "Delete Variant",
                                        tint = palette.danger,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val costFmt = Formatters.formatMoney(v.costMinor, CurrencyCode.BDT, language)
                        val priceFmt = Formatters.formatMoney(v.priceMinor, CurrencyCode.BDT, language)
                        Text(
                            text = if (isBangla) {
                                "ক্রয়: $costFmt • বিক্রয়: $priceFmt"
                            } else {
                                "Cost: $costFmt • Sell: $priceFmt"
                            },
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.primaryPurple
                        )

                        if (v.isExistingVariant) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = palette.lavenderTint
                                ) {
                                    Text(
                                        text = "${if (isBangla) "আগের:" else "Prev:"} ${v.existingStock} $unit",
                                        fontSize = 11.sp,
                                        color = palette.textPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (v.initialStock > 0) palette.cyanTint else palette.fieldBorder.copy(alpha = 0.4f)
                                ) {
                                    Text(
                                        text = "${if (isBangla) "নতুন যোগ:" else "Add:"} +${v.initialStock} $unit",
                                        fontSize = 11.sp,
                                        color = if (v.initialStock > 0) palette.premiumTeal else palette.textMuted,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = palette.lavenderTint
                                ) {
                                    Text(
                                        text = "${if (isBangla) "মোট:" else "Total:"} ${v.existingStock + v.initialStock} $unit",
                                        fontSize = 11.sp,
                                        color = palette.primaryPurple,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        if (!hideStock) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = if (v.initialStock > 0) v.initialStock.toString() else "",
                                    onValueChange = { input ->
                                        val qty = input.filter(Char::isDigit).toIntOrNull() ?: 0
                                        onUpdateVariantStock(idx, qty)
                                    },
                                    label = {
                                        Text(
                                            text = if (v.isExistingVariant) {
                                                if (isBangla) "নতুন যোগ ($unit)" else "Add Qty ($unit)"
                                            } else {
                                                if (isBangla) "পরিমাণ ($unit)" else "Qty ($unit)"
                                            },
                                            fontSize = 11.5.sp
                                        )
                                    },
                                    placeholder = { Text("0", color = palette.textMuted) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = fieldColors
                                )

                                listOf(10, 50, 100).forEach { bump ->
                                    Surface(
                                        onClick = { onUpdateVariantStock(idx, v.initialStock + bump) },
                                        shape = RoundedCornerShape(10.dp),
                                        color = palette.lavenderTint,
                                        border = BorderStroke(1.dp, palette.lavenderBorder)
                                    ) {
                                        Text(
                                            text = "+$bump",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = palette.primaryPurple,
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

private fun filterDecimalInput(raw: String): String {
    var hasDot = false
    val sb = StringBuilder()
    for (ch in raw) {
        if (ch.isDigit()) {
            sb.append(ch)
        } else if (ch == '.' && !hasDot) {
            hasDot = true
            sb.append(ch)
        }
    }
    return sb.toString()
}

private fun parseSafeMinor(raw: String): Long {
    val clean = raw.trim()
    if (clean.isBlank() || clean == ".") return 0L
    return try {
        Money.parseFromString(clean).minorUnits
    } catch (_: Exception) {
        -1L
    }
}

private fun formatDecimalAmount(minor: Long): String {
    val major = minor / 100L
    val frac = (minor % 100L).toInt()
    return if (frac == 0) major.toString() else "$major.${frac.toString().padStart(2, '0')}"
}
