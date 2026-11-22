package com.pocketdev.advance_erp.feature.product

import com.pocketdev.advance_erp.core.designsystem.StatusWarning
import com.pocketdev.advance_erp.core.designsystem.ERPSurfaceVariant
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.pocketdev.advance_erp.R
import com.pocketdev.advance_erp.core.designsystem.DeepNavy
import com.pocketdev.advance_erp.core.designsystem.ERPBackground
import com.pocketdev.advance_erp.core.designsystem.ERPCard
import com.pocketdev.advance_erp.core.designsystem.ERPDivider
import com.pocketdev.advance_erp.core.designsystem.ERPEmptyState
import com.pocketdev.advance_erp.core.designsystem.PrimaryPurple
import com.pocketdev.advance_erp.core.designsystem.SecondaryTeal
import com.pocketdev.advance_erp.core.designsystem.StatusError
import com.pocketdev.advance_erp.core.designsystem.StatusSuccess
import com.pocketdev.advance_erp.core.designsystem.TextMuted
import com.pocketdev.advance_erp.core.designsystem.TextPrimary
import com.pocketdev.advance_erp.core.designsystem.TextSecondary
import com.pocketdev.advance_erp.core.designsystem.rememberImageModel
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.data.entity.ProductCategory
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductListScreen(
    viewModel: ProductViewModel,
    modifier: Modifier = Modifier,
    initialAction: String = ""
) {
    var actionHandled by rememberSaveable(initialAction) { mutableStateOf(false) }
    LaunchedEffect(initialAction) {
        if (!actionHandled && initialAction == "add") {
            viewModel.setAddDialogVisible(true)
            actionHandled = true
        }
    }
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val canViewCost by viewModel.canViewCost.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddCategoryDialog by rememberSaveable { mutableStateOf(false) }
    var showManageCategoriesDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.userMessage) {
        state.userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ERPBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.setAddDialogVisible(true) },
                containerColor = PrimaryPurple,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.add_product)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                placeholder = {
                    Text(
                        text = stringResource(R.string.search_products),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Outlined.Clear,
                                contentDescription = "Clear",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryPurple,
                    unfocusedBorderColor = ERPDivider,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Category Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    val isAllSelected = state.selectedCategoryId == null
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isAllSelected) PrimaryPurple else MaterialTheme.colorScheme.surface)
                            .border(1.dp, if (isAllSelected) PrimaryPurple else ERPDivider, RoundedCornerShape(20.dp))
                            .clickable { viewModel.onCategorySelect(null) }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.all_categories),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isAllSelected) Color.White else TextSecondary
                        )
                    }
                }

                items(state.categories, key = { it.id }) { cat ->
                    val isSelected = state.selectedCategoryId == cat.id
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) PrimaryPurple else MaterialTheme.colorScheme.surface)
                            .border(1.dp, if (isSelected) PrimaryPurple else ERPDivider, RoundedCornerShape(20.dp))
                            .clickable { viewModel.onCategorySelect(cat.id) }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (state.language == AppLanguage.BANGLA) cat.nameBn else cat.nameEn,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else TextSecondary
                        )
                    }
                }

                item {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, SecondaryTeal, RoundedCornerShape(20.dp))
                            .clickable { showManageCategoriesDialog = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Settings, contentDescription = null, tint = SecondaryTeal, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (state.language == AppLanguage.BANGLA) "ক্যাটাগরি পরিচালনা" else "Manage",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = SecondaryTeal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Products List
            if (state.products.isEmpty()) {
                ERPEmptyState(
                    icon = Icons.Outlined.Inventory2,
                    title = stringResource(R.string.no_products_found),
                    message = if (state.searchQuery.isNotBlank()) {
                        if (state.language == AppLanguage.BANGLA) {
                            "'${state.searchQuery}' এর সাথে কোনো পণ্য পাওয়া যায়নি"
                        } else {
                            "No products match '${state.searchQuery}'"
                        }
                    } else {
                        if (state.language == AppLanguage.BANGLA) {
                            "প্রথম পণ্য যোগ করতে '+' বোতামে ট্যাপ করুন।"
                        } else {
                            "Tap '+' to add your first product."
                        }
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(state.products, key = { it.id }) { product ->
                        val isLowStock = product.totalStock <= product.minStockAlert
                        val stockColor = when {
                            product.totalStock == 0 -> StatusError
                            isLowStock -> StatusWarning
                            else -> StatusSuccess
                        }

                        ERPCard(
                            onClick = { viewModel.openProductDetail(product) },
                            contentPadding = 12.dp
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Product Photo Thumbnail
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(ERPSurfaceVariant)
                                    ) {
                                        if (!product.primaryImageUri.isNullOrBlank()) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(context)
                                                    .data(rememberImageModel(product.primaryImageUri))
                                                    .crossfade(true)
                                                    .build(),
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Outlined.Inventory2,
                                                contentDescription = null,
                                                tint = TextMuted,
                                                modifier = Modifier.align(Alignment.Center).size(24.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (state.language == AppLanguage.BANGLA) product.nameBn else product.nameEn,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = if (state.language == AppLanguage.BANGLA) product.nameEn else product.nameBn,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextMuted
                                        )
                                    }

                                    // Stock Status Pill
                                    Box(
                                        modifier = Modifier
                                            .background(stockColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "${Formatters.formatNumber(product.totalStock.toLong(), state.language)} ${product.unit}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = stockColor
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Metadata wraps as complete items instead of consuming the price width.
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(PrimaryPurple.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (state.language == AppLanguage.BANGLA) product.categoryNameBn else product.categoryNameEn,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = PrimaryPurple,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Text(
                                        text = "SKU: ${product.baseSku}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )

                                    if (product.variantCount > 0) {
                                        Text(
                                            text = if (state.language == AppLanguage.BANGLA)
                                                "${Formatters.formatNumber(product.variantCount.toLong(), state.language)} ভ্যারিয়েন্ট"
                                            else "${product.variantCount} variants",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SecondaryTeal
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = Formatters.formatMoney(product.sellingPriceMinor, state.currency, state.language),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryPurple,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.End,
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

    // Detail Dialog
    if (state.selectedProductForDetail != null) {
        ProductDetailDialog(
            product = state.selectedProductForDetail!!,
            variants = state.selectedProductVariants,
            productImages = state.selectedProductImages,
            variantStockMap = state.variantStockMap,
            language = state.language,
            currency = state.currency,
            preSelectedVariantId = state.preSelectedVariantId,
            canViewCost = canViewCost,
            onObserveCostLayers = viewModel::observeProductCostLayers,
            onLoadVariantImages = { variantId -> viewModel.loadVariantImages(variantId) },
            onEditProduct = { viewModel.setEditDialogVisible(true) },
            onDeleteProduct = { prodId -> viewModel.deleteProduct(prodId) },
            onDismiss = viewModel::closeProductDetail
        )
    }

    // Add Product Dialog
    if (state.showAddDialog) {
        AddProductDialog(
            categories = state.categories,
            warehouses = state.warehouses,
            language = state.language,
            onDismiss = { viewModel.setAddDialogVisible(false) },
            onAddNewCategory = { nameEn, nameBn, onCreated ->
                viewModel.createCategory(nameEn, nameBn, onCreated)
            },
            onGenerateSku = { catId, nameEn ->
                viewModel.generateBaseSku(catId, nameEn)
            },
            onGenerateVariantSku = { baseSku, color, size ->
                viewModel.generateVariantSku(baseSku, color, size)
            },
            onGenerateBarcode = {
                viewModel.generateBarcode()
            },
            onValidateSku = { sku, excludeId ->
                viewModel.isBaseSkuAvailable(sku, excludeId)
            },
            onValidateBarcode = { barcode, excludeId ->
                viewModel.isBarcodeAvailable(barcode, excludeId)
            },
            onLoadCategoryProducts = { categoryId ->
                viewModel.loadProductsForCategory(categoryId)
            },
            onLoadProductRestockBundle = { productId ->
                viewModel.loadProductRestockBundle(productId)
            },
            onUpdateExistingProduct = { productId, nameEn, nameBn, baseSku, barcode, catId, unit, purchaseMinor, sellMinor, minStock, productPhotos, variants, deletedImageIds, additionalStock, whId, description ->
                viewModel.updateProductWithMedia(
                    productId = productId,
                    nameEn = nameEn,
                    nameBn = nameBn,
                    baseSku = baseSku,
                    barcode = barcode,
                    categoryId = catId,
                    unit = unit,
                    purchasePriceMinor = purchaseMinor,
                    sellingPriceMinor = sellMinor,
                    minStockAlert = minStock,
                    productPhotos = productPhotos,
                    variants = variants,
                    deletedImageIds = deletedImageIds,
                    additionalStock = additionalStock,
                    warehouseId = whId,
                    description = description
                )
            },
            onConfirm = { nameEn, nameBn, baseSku, barcode, catId, unit, purchaseMinor, sellMinor, minStock, productPhotos, variants, initialStock, whId, description ->
                viewModel.createProductWithMedia(
                    nameEn, nameBn, baseSku, barcode, catId, unit, purchaseMinor, sellMinor, minStock, productPhotos, variants, initialStock, whId, description
                )
            }
        )
    }

    // Edit Product Dialog
    if (state.showEditDialog && state.selectedProductForDetail != null) {
        EditProductDialog(
            product = state.selectedProductForDetail!!,
            initialVariants = state.selectedProductVariants,
            initialImages = state.selectedProductImages,
            categories = state.categories,
            language = state.language,
            initialVariantStock = state.variantStockMap,
            onDismiss = { viewModel.setEditDialogVisible(false) },
            onGenerateSku = { catId, nameEn ->
                viewModel.generateBaseSku(catId, nameEn)
            },
            onGenerateBarcode = {
                viewModel.generateBarcode()
            },
            onGenerateVariantSku = { baseSku, color, size ->
                viewModel.generateVariantSku(baseSku, color, size)
            },
            onValidateSku = { sku ->
                viewModel.isBaseSkuAvailable(sku, state.selectedProductForDetail!!.id)
            },
            onValidateBarcode = { barcode ->
                viewModel.isBarcodeAvailable(barcode, state.selectedProductForDetail!!.id)
            },
            onDeleteProduct = { prodId ->
                viewModel.deleteProduct(prodId)
            },
            onConfirm = { nameEn, nameBn, baseSku, barcode, catId, unit, purchaseMinor, sellMinor, minStock, productPhotos, variants, deletedImageIds, description ->
                viewModel.updateProductWithMedia(
                    productId = state.selectedProductForDetail!!.id,
                    nameEn = nameEn,
                    nameBn = nameBn,
                    baseSku = baseSku,
                    barcode = barcode,
                    categoryId = catId,
                    unit = unit,
                    purchasePriceMinor = purchaseMinor,
                    sellingPriceMinor = sellMinor,
                    minStockAlert = minStock,
                    productPhotos = productPhotos,
                    variants = variants,
                    deletedImageIds = deletedImageIds,
                    description = description
                )
            }
        )
    }

    // Add Category Dialog
    if (showAddCategoryDialog) {
        var catEn by rememberSaveable { mutableStateOf("") }
        var catBn by rememberSaveable { mutableStateOf("") }
        var catErr by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = {
                Text(
                    text = if (state.language == AppLanguage.BANGLA) "নতুন ক্যাটাগরি যোগ করুন" else "Add New Category",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (catErr != null) {
                        Text(text = catErr!!, color = StatusError, style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedTextField(
                        value = catEn,
                        onValueChange = { catEn = it; catErr = null },
                        label = { Text("Category Name (English)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = catBn,
                        onValueChange = { catBn = it; catErr = null },
                        label = { Text("ক্যাটাগরির নাম (বাংলা)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (catEn.isBlank()) {
                            catErr = "English name required"
                            return@TextButton
                        }
                        viewModel.createCategory(catEn.trim(), catBn.trim()) {
                            showAddCategoryDialog = false
                        }
                    }
                ) {
                    Text(stringResource(R.string.confirm), color = PrimaryPurple, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text(stringResource(R.string.cancel), color = TextSecondary)
                }
            }
        )
    }

    // Manage Categories Dialog
    if (showManageCategoriesDialog) {
        val coroutineScope = rememberCoroutineScope()
        var categoryToDelete by remember { mutableStateOf<ProductCategory?>(null) }
        var linkedCountForDelete by remember { mutableStateOf(0) }
        var showReassignDialog by rememberSaveable { mutableStateOf(false) }
        var showConfirmEmptyDeleteDialog by rememberSaveable { mutableStateOf(false) }
        var targetCategoryId by rememberSaveable { mutableStateOf("") }

        Dialog(
            onDismissRequest = { showManageCategoriesDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .clip(RoundedCornerShape(16.dp)),
                color = ERPBackground
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (state.language == AppLanguage.BANGLA) "ক্যাটাগরি পরিচালনা ও মুছে ফেলা" else "Manage & Delete Categories",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        IconButton(onClick = { showManageCategoriesDialog = false }) {
                            Icon(Icons.Outlined.Clear, contentDescription = "Close", tint = TextMuted)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                    ) {
                        items(state.categories, key = { it.id }) { cat ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp)),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (state.language == AppLanguage.BANGLA) cat.nameBn else cat.nameEn,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "${cat.code} • ${if (state.language == AppLanguage.BANGLA) cat.nameEn else cat.nameBn}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary
                                        )
                                    }

                                    val isSystemUncat = cat.code.equals("UNCAT", ignoreCase = true) || cat.id == "cat-uncategorized"
                                    if (isSystemUncat) {
                                        Box(
                                            modifier = Modifier
                                                .background(SecondaryTeal.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = if (state.language == AppLanguage.BANGLA) "ডিফল্ট ক্যাটাগরি" else "System Default",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = SecondaryTeal,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    } else {
                                        IconButton(
                                            onClick = {
                                                coroutineScope.launch {
                                                    val count = viewModel.getProductCountForCategory(cat.id)
                                                    categoryToDelete = cat
                                                    linkedCountForDelete = count
                                                    if (count > 0) {
                                                        val fallback = state.categories.firstOrNull { it.id != cat.id }
                                                        targetCategoryId = fallback?.id ?: "cat-uncategorized"
                                                        showReassignDialog = true
                                                    } else {
                                                        showConfirmEmptyDeleteDialog = true
                                                    }
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete Category", tint = StatusError)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    TextButton(
                        onClick = { showAddCategoryDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (state.language == AppLanguage.BANGLA) "+ নতুন ক্যাটাগরি যোগ করুন" else "+ Add New Category",
                            color = PrimaryPurple,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Dialog for reassigning products when count > 0
        if (showReassignDialog && categoryToDelete != null) {
            val fromCat = categoryToDelete!!
            val eligibleTargets = state.categories.filter { it.id != fromCat.id }
            AlertDialog(
                onDismissRequest = { showReassignDialog = false },
                title = {
                    Text(
                        text = if (state.language == AppLanguage.BANGLA) "পণ্য স্থানান্তর ও ক্যাটাগরি মুছুন" else "Reassign Products & Delete",
                        fontWeight = FontWeight.Bold,
                        color = StatusError
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (state.language == AppLanguage.BANGLA)
                                "⚠️ '${fromCat.nameEn}' ক্যাটাগরির সাথে বর্তমানে $linkedCountForDelete টি পণ্য যুক্ত রয়েছে। সরাসরি মোছা সম্ভব নয়।\n\nপণ্যগুলো কোন ক্যাটাগরিতে স্থানান্তর করবেন নির্বাচন করুন:"
                            else
                                "⚠️ Category '${fromCat.nameEn}' has $linkedCountForDelete linked products. It cannot be deleted directly.\n\nSelect a category to reassign these products to:",
                            style = MaterialTheme.typography.bodySmall
                        )
                        eligibleTargets.forEach { targetCat ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { targetCategoryId = targetCat.id }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = targetCategoryId == targetCat.id,
                                    onClick = { targetCategoryId = targetCat.id },
                                    colors = RadioButtonDefaults.colors(selectedColor = PrimaryPurple)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (state.language == AppLanguage.BANGLA) targetCat.nameBn else targetCat.nameEn,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val fId = fromCat.id
                            val tId = targetCategoryId
                            showReassignDialog = false
                            categoryToDelete = null
                            viewModel.reassignCategory(fId, tId)
                        },
                        enabled = targetCategoryId.isNotBlank()
                    ) {
                        Text(
                            text = if (state.language == AppLanguage.BANGLA) "স্থানান্তর ও মুছুন" else "Reassign & Delete",
                            color = StatusError,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showReassignDialog = false }) {
                        Text(if (state.language == AppLanguage.BANGLA) "বাতিল" else "Cancel", color = TextSecondary)
                    }
                }
            )
        }

        // Dialog for deleting empty category when count == 0
        if (showConfirmEmptyDeleteDialog && categoryToDelete != null) {
            val emptyCat = categoryToDelete!!
            AlertDialog(
                onDismissRequest = { showConfirmEmptyDeleteDialog = false },
                title = {
                    Text(
                        text = if (state.language == AppLanguage.BANGLA) "ক্যাটাগরি মুছুন" else "Delete Category",
                        fontWeight = FontWeight.Bold,
                        color = StatusError
                    )
                },
                text = {
                    Text(
                        text = if (state.language == AppLanguage.BANGLA)
                            "আপনি কি নিশ্চিত যে '${emptyCat.nameEn}' ক্যাটাগরি মুছে ফেলতে চান? এই ক্যাটাগরিতে কোনো পণ্য নেই।"
                        else
                            "Are you sure you want to delete '${emptyCat.nameEn}'? No products are linked to this category.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val cId = emptyCat.id
                            showConfirmEmptyDeleteDialog = false
                            categoryToDelete = null
                            viewModel.deleteCategory(cId)
                        }
                    ) {
                        Text(if (state.language == AppLanguage.BANGLA) "হ্যাঁ, মুছুন" else "Delete", color = StatusError, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmEmptyDeleteDialog = false }) {
                        Text(if (state.language == AppLanguage.BANGLA) "বাতিল" else "Cancel", color = TextSecondary)
                    }
                }
            )
        }
    }
}
