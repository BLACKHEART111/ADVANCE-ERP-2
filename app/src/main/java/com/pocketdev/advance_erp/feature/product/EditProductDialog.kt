package com.pocketdev.advance_erp.feature.product

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import java.util.UUID
import com.pocketdev.advance_erp.core.model.CurrencyCode
import com.pocketdev.advance_erp.core.util.Formatters
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pocketdev.advance_erp.R
import com.pocketdev.advance_erp.core.designsystem.SuggestTextField
import com.pocketdev.advance_erp.data.entity.SuggestionType
import com.pocketdev.advance_erp.core.designsystem.ERPActionButton
import com.pocketdev.advance_erp.core.designsystem.ERPButtonVariant
import com.pocketdev.advance_erp.core.designsystem.ERPDivider
import com.pocketdev.advance_erp.core.designsystem.PrimaryPurple
import com.pocketdev.advance_erp.core.designsystem.SecondaryTeal
import com.pocketdev.advance_erp.core.designsystem.StatusError
import com.pocketdev.advance_erp.core.designsystem.TextMuted
import com.pocketdev.advance_erp.core.designsystem.TextPrimary
import com.pocketdev.advance_erp.core.designsystem.TextSecondary
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.data.dao.ProductWithDetails
import com.pocketdev.advance_erp.data.entity.ProductCategory
import com.pocketdev.advance_erp.data.entity.ProductImage
import com.pocketdev.advance_erp.data.entity.ProductVariant
import kotlinx.coroutines.launch

@Composable
fun EditProductDialog(
    product: ProductWithDetails,
    initialVariants: List<ProductVariant>,
    initialImages: List<ProductImage>,
    categories: List<ProductCategory>,
    language: AppLanguage,
    initialVariantStock: Map<String, Int> = emptyMap(),
    onDismiss: () -> Unit,
    onGenerateSku: suspend (categoryId: String, nameEn: String) -> String,
    onGenerateBarcode: suspend () -> String,
    onGenerateVariantSku: (suspend (baseSku: String, color: String, size: String) -> String)? = null,
    onValidateSku: suspend (sku: String) -> Boolean,
    onValidateBarcode: suspend (barcode: String) -> Boolean,
    onDeleteProduct: ((productId: String) -> Unit)? = null,
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
        deletedImageIds: List<String>
    ) -> Unit
) {
    val scope = rememberCoroutineScope()

    var nameEn by remember { mutableStateOf(product.nameEn) }
    var nameBn by remember { mutableStateOf(product.nameBn) }
    var baseSku by remember { mutableStateOf(product.baseSku) }
    var barcode by remember { mutableStateOf(product.barcode ?: "") }
    var unit by remember { mutableStateOf(product.unit) }
    var purchasePriceStr by remember { mutableStateOf((product.purchasePriceMinor / 100.0).toString()) }
    var sellingPriceStr by remember { mutableStateOf((product.sellingPriceMinor / 100.0).toString()) }
    var minStockAlertStr by remember { mutableStateOf(product.minStockAlert.toString()) }

    var selectedCategory by remember {
        mutableStateOf(categories.firstOrNull { it.id == product.categoryId } ?: categories.firstOrNull())
    }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    val productPhotos = remember {
        mutableStateListOf<GalleryItemDraft>().apply {
            addAll(initialImages.map {
                GalleryItemDraft(it.id, it.imageUri, it.isPrimary, it.sortOrder)
            })
        }
    }
    val deletedImageIds = remember { mutableStateListOf<String>() }

    val variants = remember {
        mutableStateListOf<NewVariantDraft>().apply {
            addAll(initialVariants.map {
                NewVariantDraft(
                    id = it.id,
                    sku = it.sku,
                    color = it.color ?: "",
                    size = it.size ?: "",
                    costMinor = it.variantCostMinor,
                    priceMinor = it.variantPriceMinor,
                    barcode = it.barcode,
                    qrCode = it.qrCode,
                    existingStock = initialVariantStock[it.id] ?: 0,
                    isExistingVariant = true
                )
            })
        }
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isCheckingDuplicates by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    // Variant Add / Edit state
    var showVariantForm by remember { mutableStateOf(false) }
    var editingVariantIndex by remember { mutableStateOf<Int?>(null) }
    var varColor by remember { mutableStateOf("") }
    var varSize by remember { mutableStateOf("") }
    var varSku by remember { mutableStateOf("") }
    var varBarcode by remember { mutableStateOf("") }
    var varCostStr by remember { mutableStateOf("") }
    var varPriceStr by remember { mutableStateOf("") }
    var varStockStr by remember { mutableStateOf("") }

    fun resetVariantForm() {
        editingVariantIndex = null
        varColor = ""
        varSize = ""
        varSku = ""
        varBarcode = ""
        varCostStr = purchasePriceStr
        varPriceStr = sellingPriceStr
        varStockStr = ""
    }

    LaunchedEffect(varColor, varSize, baseSku) {
        if (showVariantForm && editingVariantIndex == null && (varColor.isNotBlank() || varSize.isNotBlank())) {
            val generated = if (onGenerateVariantSku != null) {
                onGenerateVariantSku(baseSku, varColor, varSize)
            } else {
                listOf(baseSku.ifBlank { "VAR" }, varColor.trim().uppercase(), varSize.trim().uppercase())
                    .filter { it.isNotBlank() }
                    .joinToString("-")
            }
            varSku = generated
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .widthIn(max = 680.dp)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (language == AppLanguage.BANGLA) "পণ্য সম্পাদনা করুন" else "Edit Product",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Divider(color = ERPDivider)

                // Scrollable Body
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = StatusError,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // 1. Photo Gallery Section
                PhotoGalleryPicker(
                    title = if (language == AppLanguage.BANGLA) "পণ্যের ছবি" else "Product Photos",
                    photos = productPhotos,
                    onPhotosChanged = { updated ->
                        // Detect deleted initial images
                        val updatedIds = updated.map { it.id }.toSet()
                        for (initImg in initialImages) {
                            if (!updatedIds.contains(initImg.id) && !deletedImageIds.contains(initImg.id)) {
                                deletedImageIds.add(initImg.id)
                            }
                        }
                        productPhotos.clear()
                        productPhotos.addAll(updated)
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Category Dropdown
                Text(
                    text = stringResource(R.string.category),
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, ERPDivider, RoundedCornerShape(10.dp))
                            .clickable { categoryDropdownExpanded = true }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = if (language == AppLanguage.BANGLA) selectedCategory?.nameBn ?: "" else selectedCategory?.nameEn ?: "Select Category",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                    }
                    DropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(if (language == AppLanguage.BANGLA) cat.nameBn else cat.nameEn) },
                                onClick = {
                                    selectedCategory = cat
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Names
                OutlinedTextField(
                    value = nameEn,
                    onValueChange = { nameEn = it },
                    label = { Text(stringResource(R.string.product_name_en)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = nameBn,
                    onValueChange = { nameBn = it },
                    label = { Text(stringResource(R.string.product_name_bn)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 4. SKU with Auto-generate & Barcode with Auto-generate
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = baseSku,
                        onValueChange = { baseSku = it.uppercase() },
                        label = { Text(stringResource(R.string.sku)) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    IconButton(
                        onClick = {
                            val cat = selectedCategory
                            if (cat != null) {
                                scope.launch {
                                    val name = if (nameEn.isBlank()) "PRD" else nameEn
                                    val auto = onGenerateSku(cat.id, name)
                                    baseSku = auto
                                }
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(SecondaryTeal.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                    ) {
                        Icon(imageVector = Icons.Default.AutoFixHigh, contentDescription = "Auto SKU", tint = SecondaryTeal)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = { Text(stringResource(R.string.barcode)) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    IconButton(
                        onClick = {
                            scope.launch {
                                val auto = onGenerateBarcode()
                                barcode = auto
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(PrimaryPurple.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                    ) {
                        Icon(imageVector = Icons.Default.AutoFixHigh, contentDescription = "Auto Barcode", tint = PrimaryPurple)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 5. Prices
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = purchasePriceStr,
                        onValueChange = { purchasePriceStr = it },
                        label = { Text(stringResource(R.string.purchase_price)) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = sellingPriceStr,
                        onValueChange = { sellingPriceStr = it },
                        label = { Text(stringResource(R.string.selling_price)) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 6. Min Stock Alert & Unit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = minStockAlertStr,
                        onValueChange = { minStockAlertStr = it },
                        label = { Text(stringResource(R.string.min_stock_alert)) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text(stringResource(R.string.unit)) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))
                Divider(color = ERPDivider)
                Spacer(modifier = Modifier.height(14.dp))

                // 7. Product Variants Section (Add, Edit, Delete)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp)
                    ) {
                        Text(
                            text = if (language == AppLanguage.BANGLA) "ভ্যারিয়েন্ট সমূহ (${variants.size})" else "Product Variants (${variants.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (language == AppLanguage.BANGLA) "রঙ, সাইজ বা আলাদা মূল্য থাকলে ভ্যারিয়েন্ট যোগ/সম্পাদনা/ডিলিট করুন" else "Add, edit, or delete color/size variants",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    ERPActionButton(
                        text = if (showVariantForm && editingVariantIndex == null) {
                            if (language == AppLanguage.BANGLA) "বাতিল" else "Cancel"
                        } else {
                            if (language == AppLanguage.BANGLA) "ভ্যারিয়েন্ট যোগ" else "Add Variant"
                        },
                        icon = if (showVariantForm && editingVariantIndex == null) Icons.Default.Close else Icons.Default.Add,
                        onClick = {
                            if (showVariantForm && editingVariantIndex == null) {
                                showVariantForm = false
                                resetVariantForm()
                            } else {
                                resetVariantForm()
                                showVariantForm = true
                            }
                        },
                        variant = if (showVariantForm && editingVariantIndex == null) ERPButtonVariant.GHOST else ERPButtonVariant.SECONDARY,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (variants.isEmpty() && !showVariantForm) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF8FAFC))
                            .border(1.dp, ERPDivider, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = if (language == AppLanguage.BANGLA) "কোনো ভ্যারিয়েন্ট নেই। নতুন ভ্যারিয়েন্ট যোগ করতে উপরের 'ভ্যারিয়েন্ট যোগ' বাটনে চাপুন।" else "No variants added. Tap 'Add Variant' above to add color/size variants.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        variants.forEachIndexed { idx, v ->
                            val isBeingEdited = showVariantForm && editingVariantIndex == idx
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isBeingEdited) Color(0xFFEEF2FF) else Color(0xFFF8FAFC))
                                    .border(
                                        1.dp,
                                        if (isBeingEdited) PrimaryPurple else ERPDivider,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        val labelParts = listOfNotNull(
                                            v.color.takeIf { it.isNotBlank() },
                                            v.size.takeIf { it.isNotBlank() }
                                        )
                                        Text(
                                            text = if (labelParts.isNotEmpty()) labelParts.joinToString(" / ") else v.sku,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "SKU: ${v.sku}" + (if (!v.barcode.isNullOrBlank()) " • Barcode: ${v.barcode}" else ""),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                        val costFmt = Formatters.formatMoney(v.costMinor, CurrencyCode.BDT, language)
                                        val priceFmt = Formatters.formatMoney(v.priceMinor, CurrencyCode.BDT, language)
                                        Text(
                                            text = if (language == AppLanguage.BANGLA)
                                                "ক্রয়: $costFmt | বিক্রয়: $priceFmt"
                                            else
                                                "Cost: $costFmt | Price: $priceFmt",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = PrimaryPurple
                                        )
                                        if (v.existingStock > 0 || v.initialStock > 0) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier.padding(top = 2.dp)
                                            ) {
                                                if (v.existingStock > 0) {
                                                    Text(
                                                        text = if (language == AppLanguage.BANGLA) "মজুদ: ${v.existingStock} $unit" else "Stock: ${v.existingStock} $unit",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = SecondaryTeal,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                if (v.initialStock > 0) {
                                                    Text(
                                                        text = if (language == AppLanguage.BANGLA) "(+${v.initialStock} $unit যোগ হবে)" else "(+${v.initialStock} $unit to add)",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = PrimaryPurple,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                editingVariantIndex = idx
                                                varColor = v.color
                                                varSize = v.size
                                                varSku = v.sku
                                                varBarcode = v.barcode ?: ""
                                                varCostStr = (v.costMinor / 100.0).toString()
                                                varPriceStr = (v.priceMinor / 100.0).toString()
                                                varStockStr = if (v.initialStock > 0) v.initialStock.toString() else ""
                                                showVariantForm = true
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit Variant",
                                                tint = PrimaryPurple
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                if (editingVariantIndex == idx) {
                                                    showVariantForm = false
                                                    resetVariantForm()
                                                } else if (editingVariantIndex != null && editingVariantIndex!! > idx) {
                                                    editingVariantIndex = editingVariantIndex!! - 1
                                                }
                                                variants.removeAt(idx)
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Remove Variant",
                                                tint = StatusError
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Inline Add / Edit Variant Form
                if (showVariantForm) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF1F5F9))
                            .border(1.dp, SecondaryTeal.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = if (editingVariantIndex != null) {
                                if (language == AppLanguage.BANGLA) "ভ্যারিয়েন্ট সম্পাদনা করুন" else "Edit Variant"
                            } else {
                                if (language == AppLanguage.BANGLA) "নতুন ভ্যারিয়েন্ট যোগ করুন" else "New Variant Details"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = SecondaryTeal
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SuggestTextField(
                                fieldType = SuggestionType.COLOR,
                                value = varColor,
                                onValueChange = { varColor = it },
                                label = { Text(if (language == AppLanguage.BANGLA) "রঙ (Color)" else "Color") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                            SuggestTextField(
                                fieldType = SuggestionType.SIZE,
                                value = varSize,
                                onValueChange = { varSize = it },
                                label = { Text(if (language == AppLanguage.BANGLA) "সাইজ (Size)" else "Size") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = varSku,
                                onValueChange = { varSku = it.uppercase() },
                                label = { Text(if (language == AppLanguage.BANGLA) "ভ্যারিয়েন্ট SKU" else "Variant SKU") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        varSku = if (onGenerateVariantSku != null) {
                                            onGenerateVariantSku(baseSku, varColor, varSize)
                                        } else {
                                            listOf(baseSku.ifBlank { "VAR" }, varColor.trim().uppercase(), varSize.trim().uppercase())
                                                .filter { it.isNotBlank() }
                                                .joinToString("-")
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                                    .border(1.dp, ERPDivider, RoundedCornerShape(8.dp))
                            ) {
                                Icon(Icons.Default.AutoFixHigh, contentDescription = "Auto Variant SKU", tint = PrimaryPurple)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = varBarcode,
                                onValueChange = { varBarcode = it },
                                label = { Text(if (language == AppLanguage.BANGLA) "ভ্যারিয়েন্ট বারকোড (ঐচ্ছিক)" else "Variant Barcode (Optional)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        varBarcode = onGenerateBarcode()
                                    }
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                                    .border(1.dp, ERPDivider, RoundedCornerShape(8.dp))
                            ) {
                                Icon(Icons.Default.AutoFixHigh, contentDescription = "Auto Barcode", tint = PrimaryPurple)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = varCostStr,
                                onValueChange = { varCostStr = it },
                                label = { Text(stringResource(R.string.purchase_price)) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = RoundedCornerShape(8.dp)
                            )
                            OutlinedTextField(
                                value = varPriceStr,
                                onValueChange = { varPriceStr = it },
                                label = { Text(stringResource(R.string.selling_price)) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val currentEditingStock = editingVariantIndex?.let { idx ->
                            variants.getOrNull(idx)?.let { initialVariantStock[it.id] ?: 0 }
                        } ?: 0

                        OutlinedTextField(
                            value = varStockStr,
                            onValueChange = { varStockStr = it.filter { ch -> ch.isDigit() } },
                            label = {
                                Text(
                                    if (editingVariantIndex != null) {
                                        if (language == AppLanguage.BANGLA) "অতিরিক্ত মজুদ যোগ করুন ($unit)" else "Add Extra Stock ($unit)"
                                    } else {
                                        if (language == AppLanguage.BANGLA) "প্রারম্ভিক মজুদ ($unit)" else "Initial Stock ($unit)"
                                    }
                                )
                            },
                            placeholder = { Text("0") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp),
                            supportingText = {
                                if (editingVariantIndex != null && currentEditingStock > 0) {
                                    Text(
                                        text = if (language == AppLanguage.BANGLA)
                                            "বর্তমান মজুদ: $currentEditingStock $unit (নতুন যোগ করলে এই ক্রয়মূল্যে আলাদা ব্যাচ তৈরি হবে)"
                                        else
                                            "Current stock: $currentEditingStock $unit (added stock creates a separate cost layer)",
                                        color = TextSecondary,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            ERPActionButton(
                                text = stringResource(R.string.cancel),
                                onClick = {
                                    showVariantForm = false
                                    resetVariantForm()
                                },
                                variant = ERPButtonVariant.GHOST
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            ERPActionButton(
                                text = if (editingVariantIndex != null) {
                                    if (language == AppLanguage.BANGLA) "আপডেট করুন" else "Update Variant"
                                } else {
                                    if (language == AppLanguage.BANGLA) "ভ্যারিয়েন্ট যোগ করুন" else "Add Variant"
                                },
                                onClick = {
                                    if (varSku.isNotBlank()) {
                                        val cMinor = (varCostStr.toDoubleOrNull()?.times(100))?.toLong()
                                            ?: (purchasePriceStr.toDoubleOrNull()?.times(100))?.toLong() ?: 0L
                                        val pMinor = (varPriceStr.toDoubleOrNull()?.times(100))?.toLong()
                                            ?: (sellingPriceStr.toDoubleOrNull()?.times(100))?.toLong() ?: 0L
                                        val stockInput = varStockStr.toIntOrNull() ?: 0

                                        val editIdx = editingVariantIndex
                                        if (editIdx != null && editIdx in variants.indices) {
                                            val existing = variants[editIdx]
                                            variants[editIdx] = existing.copy(
                                                sku = varSku.trim().uppercase(),
                                                color = varColor.trim(),
                                                size = varSize.trim(),
                                                costMinor = cMinor,
                                                priceMinor = pMinor,
                                                barcode = varBarcode.trim().ifBlank { null },
                                                initialStock = stockInput
                                            )
                                        } else {
                                            variants.add(
                                                NewVariantDraft(
                                                    id = "var-" + UUID.randomUUID().toString().take(8),
                                                    sku = varSku.trim().uppercase(),
                                                    color = varColor.trim(),
                                                    size = varSize.trim(),
                                                    costMinor = cMinor,
                                                    priceMinor = pMinor,
                                                    barcode = varBarcode.trim().ifBlank { null },
                                                    initialStock = stockInput
                                                )
                                            )
                                        }
                                        showVariantForm = false
                                        resetVariantForm()
                                    }
                                },
                                variant = ERPButtonVariant.PRIMARY
                            )
                        }
                    }
                }

                }

                Divider(color = ERPDivider)

                // Fixed Bottom Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onDeleteProduct != null) {
                        ERPActionButton(
                            text = if (language == AppLanguage.BANGLA) "পণ্য মুছুন" else "Delete",
                            icon = Icons.Default.DeleteOutline,
                            onClick = { showDeleteConfirmation = true },
                            variant = ERPButtonVariant.GHOST
                        )
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Row(
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ERPActionButton(
                            text = stringResource(R.string.cancel),
                            onClick = onDismiss,
                            variant = ERPButtonVariant.GHOST
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        ERPActionButton(
                            text = if (language == AppLanguage.BANGLA) "সংরক্ষণ করুন" else "Save Changes",
                            onClick = {
                            if (nameEn.isBlank()) {
                                errorMessage = "Product English name cannot be empty"
                            } else if (baseSku.isBlank()) {
                                errorMessage = "Base SKU cannot be empty"
                            } else {
                                val purchaseMinor = (purchasePriceStr.toDoubleOrNull()?.times(100))?.toLong() ?: 0L
                                val sellingMinor = (sellingPriceStr.toDoubleOrNull()?.times(100))?.toLong() ?: 0L
                                val minStock = minStockAlertStr.toIntOrNull() ?: 10

                                scope.launch {
                                    isCheckingDuplicates = true
                                    val skuOk = onValidateSku(baseSku.trim())
                                    if (!skuOk) {
                                        errorMessage = "Base SKU '$baseSku' is already taken by another product"
                                        isCheckingDuplicates = false
                                    } else {
                                        var barOk = true
                                        if (barcode.isNotBlank()) {
                                            barOk = onValidateBarcode(barcode.trim())
                                            if (!barOk) {
                                                errorMessage = "Barcode '$barcode' is already in use"
                                            }
                                        }
                                        if (barOk) {
                                            val cat = selectedCategory
                                            if (cat != null) {
                                                val nameBnFinal = if (nameBn.isBlank()) nameEn.trim() else nameBn.trim()
                                                val barcodeFinal = if (barcode.isBlank()) null else barcode.trim()
                                                val unitFinal = if (unit.isBlank()) "PCS" else unit.trim()
                                                onConfirm(
                                                    nameEn.trim(),
                                                    nameBnFinal,
                                                    baseSku.trim().uppercase(),
                                                    barcodeFinal,
                                                    cat.id,
                                                    unitFinal,
                                                    purchaseMinor,
                                                    sellingMinor,
                                                    minStock,
                                                    productPhotos.toList(),
                                                    variants.toList(),
                                                    deletedImageIds.toList()
                                                )
                                            }
                                        }
                                        isCheckingDuplicates = false
                                    }
                                }
                            }
                        },
                            variant = ERPButtonVariant.PRIMARY
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirmation && onDeleteProduct != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = {
                Text(
                    text = if (language == AppLanguage.BANGLA) "পণ্য মুছে ফেলতে চান?" else "Delete Product?",
                    fontWeight = FontWeight.Bold,
                    color = StatusError
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${product.nameEn} (SKU: ${product.baseSku})",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    if (product.totalStock > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFEF2F2))
                                .border(1.dp, StatusError.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = if (language == AppLanguage.BANGLA)
                                    "⚠️ সতর্কতা: এই পণ্যের বর্তমানে ${product.totalStock} ${product.unit} মজুদ রয়েছে। পণ্যটি মুছে ফেললে ক্যাটালগ থেকে বাদ দেওয়া হবে।"
                                else
                                    "⚠️ Warning: This product has ${product.totalStock} ${product.unit} in stock. Deleting will remove it from active catalog.",
                                style = MaterialTheme.typography.bodySmall,
                                color = StatusError
                            )
                        }
                    } else {
                        Text(
                            text = if (language == AppLanguage.BANGLA)
                                "পণ্যটি সক্রিয় ক্যাটালগ ও বিক্রয় তালিকা থেকে মুছে ফেলা হবে। আপনি কি নিশ্চিত?"
                            else
                                "This product will be removed from your active catalog. Are you sure?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onDeleteProduct?.invoke(product.id)
                        onDismiss()
                    }
                ) {
                    Text(
                        text = if (language == AppLanguage.BANGLA) "হ্যাঁ, মুছে ফেলুন" else "Yes, Delete",
                        color = StatusError,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(
                        text = if (language == AppLanguage.BANGLA) "বাতিল" else "Cancel",
                        color = TextSecondary
                    )
                }
            }
        )
    }
}
