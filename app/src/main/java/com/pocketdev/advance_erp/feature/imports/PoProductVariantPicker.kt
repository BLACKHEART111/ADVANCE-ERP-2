package com.pocketdev.advance_erp.feature.imports

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.pocketdev.advance_erp.core.designsystem.ERPBackground
import com.pocketdev.advance_erp.core.designsystem.ERPDivider
import com.pocketdev.advance_erp.core.designsystem.ERPSurfaceVariant
import com.pocketdev.advance_erp.core.designsystem.PrimaryPurple
import com.pocketdev.advance_erp.core.designsystem.SecondaryTeal
import com.pocketdev.advance_erp.core.designsystem.StatusError
import com.pocketdev.advance_erp.core.designsystem.StatusSuccess
import com.pocketdev.advance_erp.core.designsystem.StatusSuccessContainer
import com.pocketdev.advance_erp.core.designsystem.StatusWarning
import com.pocketdev.advance_erp.core.designsystem.StatusWarningContainer
import com.pocketdev.advance_erp.core.designsystem.TextPrimary
import com.pocketdev.advance_erp.core.designsystem.TextSecondary
import com.pocketdev.advance_erp.core.designsystem.rememberImageModel
import com.pocketdev.advance_erp.core.engine.PoPackingEngine
import com.pocketdev.advance_erp.core.engine.VariantKey
import com.pocketdev.advance_erp.core.engine.VariantVisibility
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.data.entity.Product
import com.pocketdev.advance_erp.data.entity.ProductCategory
import com.pocketdev.advance_erp.data.entity.ProductVariant
import java.math.BigDecimal

data class CatalogVariantSelection(
    val productId: String,
    val variantId: String?,
    val suggestedUnitPriceMajor: String
)

data class CartonVariantAllocationPick(
    val productId: String,
    val variantId: String?,
    val quantity: Int
)

/**
 * Step 1 Searchable Product & Variant Picker (in-window bottom sheet overlay).
 * Groups active variants under their parent Product, allows multi-select without losing
 * selections across search/category filter changes, and avoids horizontal chip lists.
 */
@Composable
fun PoCatalogProductVariantPickerSheet(
    products: List<Product>,
    variants: List<ProductVariant>,
    categories: List<ProductCategory>,
    currency: String,
    isBangla: Boolean,
    singleSelectMode: Boolean = false,
    existingOrderQtyByVariant: Map<VariantKey, Int> = emptyMap(),
    resolveProductImage: (suspend (String, String?) -> String?)? = null,
    onAddNewVariant: ((productId: String) -> Unit)? = null,
    onDismiss: () -> Unit,
    onConfirmSelection: (selections: List<CatalogVariantSelection>, defaultQtyStr: String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    val selectedKeys = remember { mutableStateMapOf<VariantKey, Boolean>() }
    val collapsedProductIds = remember { mutableStateMapOf<String, Boolean>() }

    val activeVariantsByProduct = remember(variants) {
        variants.filter { it.isActive }.groupBy { it.productId }
    }
    val categoryMap = remember(categories) { categories.associateBy { it.id } }

    val filteredGroups = remember(products, activeVariantsByProduct, searchQuery, selectedCategoryId) {
        val q = searchQuery.trim().lowercase()
        products.mapNotNull { prod ->
            if (!selectedCategoryId.isNullOrBlank() && prod.categoryId != selectedCategoryId) {
                return@mapNotNull null
            }
            val prodVariants = activeVariantsByProduct[prod.id].orEmpty()
            val productMatches = q.isEmpty() ||
                prod.nameEn.lowercase().contains(q) ||
                prod.nameBn.lowercase().contains(q) ||
                prod.baseSku.lowercase().contains(q) ||
                prod.barcode?.lowercase()?.contains(q) == true

            val matchedVariants = if (productMatches || q.isEmpty()) {
                prodVariants
            } else {
                prodVariants.filter { v ->
                    v.sku.lowercase().contains(q) ||
                        v.color?.lowercase()?.contains(q) == true ||
                        v.size?.lowercase()?.contains(q) == true ||
                        v.barcode?.lowercase()?.contains(q) == true
                }
            }

            if (productMatches || matchedVariants.isNotEmpty()) {
                prod to matchedVariants
            } else {
                null
            }
        }
    }

    fun suggestedPriceStr(prod: Product, variant: ProductVariant?): String {
        val minor = variant?.variantCostMinor?.takeIf { it > 0L } ?: prod.purchasePriceMinor
        return if (minor > 0L) Formatters.formatMinorUnit(minor) else ""
    }

    fun toggleKey(key: VariantKey) {
        if (singleSelectMode) {
            selectedKeys.clear()
            selectedKeys[key] = true
        } else {
            val cur = selectedKeys[key] == true
            if (cur) selectedKeys.remove(key) else selectedKeys[key] = true
        }
    }

    fun buildSelectedPicks(): List<CatalogVariantSelection> {
        val picks = mutableListOf<CatalogVariantSelection>()
        products.forEach { prod ->
            val prodVariants = activeVariantsByProduct[prod.id].orEmpty()
            if (prodVariants.isEmpty()) {
                val baseKey = VariantKey(prod.id, null)
                if (selectedKeys[baseKey] == true) {
                    picks += CatalogVariantSelection(
                        productId = prod.id,
                        variantId = null,
                        suggestedUnitPriceMajor = suggestedPriceStr(prod, null)
                    )
                }
            } else {
                prodVariants.forEach { variant ->
                    val vKey = VariantKey(prod.id, variant.id)
                    if (selectedKeys[vKey] == true) {
                        picks += CatalogVariantSelection(
                            productId = prod.id,
                            variantId = variant.id,
                            suggestedUnitPriceMajor = suggestedPriceStr(prod, variant)
                        )
                    }
                }
            }
        }
        return picks
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .imePadding()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                ),
            shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
            color = ERPBackground,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Sheet Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (singleSelectMode) {
                                if (isBangla) "পণ্য ও ভ্যারিয়েন্ট পরিবর্তন" else "Change Product & Variant"
                            } else {
                                if (isBangla) "পণ্য ও ভ্যারিয়েন্ট নির্বাচন" else "Select Products & Variants"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (isBangla)
                                "নাম, SKU, বারকোড, রঙ বা সাইজ দিয়ে খুঁজুন"
                            else
                                "Search by Name, SKU, Barcode, Color or Size",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                if (isBangla) "পণ্য, SKU, বারকোড, রঙ বা সাইজ খুঁজুন…"
                                else "Search product, SKU, barcode, color, size…"
                            )
                        },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = PrimaryPurple) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Outlined.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("po_catalog_search_input")
                    )

                    if (categories.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(bottom = 10.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedCategoryId == null,
                                    onClick = { selectedCategoryId = null },
                                    label = { Text(if (isBangla) "সব ক্যাটাগরি" else "All Categories") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryPurple,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                            items(categories) { cat ->
                                FilterChip(
                                    selected = selectedCategoryId == cat.id,
                                    onClick = {
                                        selectedCategoryId = if (selectedCategoryId == cat.id) null else cat.id
                                    },
                                    label = {
                                        Text(if (isBangla && cat.nameBn.isNotBlank()) cat.nameBn else cat.nameEn)
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryPurple,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    val collapsibleProducts = filteredGroups.filter { it.second.isNotEmpty() }.map { it.first }
                    if (collapsibleProducts.isNotEmpty()) {
                        val allMinimized = collapsibleProducts.all { collapsedProductIds[it.id] == true }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isBangla)
                                    "মোট পণ্য: ${filteredGroups.size}"
                                else
                                    "${filteredGroups.size} matching products",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            TextButton(
                                onClick = {
                                    val nextState = !allMinimized
                                    collapsibleProducts.forEach { collapsedProductIds[it.id] = nextState }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.testTag("po_catalog_minimize_all_btn")
                            ) {
                                Icon(
                                    imageVector = if (allMinimized) Icons.Outlined.ExpandMore else Icons.Outlined.ExpandLess,
                                    contentDescription = null,
                                    tint = PrimaryPurple,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (allMinimized) {
                                        if (isBangla) "সব প্রসারিত করুন" else "Expand All"
                                    } else {
                                        if (isBangla) "সব মিনিমাইজ" else "Minimize All"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryPurple
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = ERPDivider)

                // Product + Grouped Variants vertical list
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (filteredGroups.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isBangla) "কোনো মিল পাওয়া যায়নি" else "No matching products or variants found",
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    items(filteredGroups, key = { it.first.id }) { (prod, prodVariants) ->
                        var thumbUri by remember(prod.id) { mutableStateOf<String?>(null) }
                        LaunchedEffect(prod.id) {
                            thumbUri = resolveProductImage?.invoke(prod.id, prodVariants.firstOrNull()?.id)
                        }
                        val cat = categoryMap[prod.categoryId]
                        val catTitle = cat?.let { if (isBangla && it.nameBn.isNotBlank()) it.nameBn else it.nameEn }.orEmpty()
                        val isCollapsed = prodVariants.isNotEmpty() && collapsedProductIds[prod.id] == true
                        val selectedInProductCount = if (prodVariants.isEmpty()) {
                            if (selectedKeys[VariantKey(prod.id, null)] == true) 1 else 0
                        } else {
                            prodVariants.count { selectedKeys[VariantKey(prod.id, it.id)] == true }
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, ERPDivider)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                // Parent Product Header
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = ERPSurfaceVariant,
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        if (thumbUri != null) {
                                            AsyncImage(
                                                model = rememberImageModel(thumbUri),
                                                contentDescription = prod.nameEn,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Outlined.Inventory2,
                                                    contentDescription = null,
                                                    tint = PrimaryPurple,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isBangla && prod.nameBn.isNotBlank()) "${prod.nameBn} (${prod.nameEn})" else prod.nameEn,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = buildString {
                                                append("SKU: ${prod.baseSku}")
                                                if (catTitle.isNotBlank()) append(" • $catTitle")
                                                if (!prod.barcode.isNullOrBlank()) append(" • Barcode: ${prod.barcode}")
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (prodVariants.isNotEmpty() || selectedInProductCount > 0) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (prodVariants.isNotEmpty()) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = ERPSurfaceVariant
                                                    ) {
                                                        Text(
                                                            text = if (isBangla) "${prodVariants.size} ভ্যারিয়েন্ট" else "${prodVariants.size} variants",
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = TextSecondary,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    }
                                                }
                                                if (selectedInProductCount > 0) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = PrimaryPurple.copy(alpha = 0.12f)
                                                    ) {
                                                        Text(
                                                            text = if (prodVariants.isEmpty()) {
                                                                if (isBangla) "নির্বাচিত" else "Selected"
                                                            } else if (isBangla) {
                                                                "নির্বাচিত: $selectedInProductCount/${prodVariants.size}"
                                                            } else {
                                                                "Selected: $selectedInProductCount/${prodVariants.size}"
                                                            },
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = PrimaryPurple,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if (!singleSelectMode && prodVariants.size > 1) {
                                        val allChecked = prodVariants.all { selectedKeys[VariantKey(prod.id, it.id)] == true }
                                        TextButton(
                                            onClick = {
                                                prodVariants.forEach { v ->
                                                    val k = VariantKey(prod.id, v.id)
                                                    if (allChecked) selectedKeys.remove(k) else selectedKeys[k] = true
                                                }
                                            },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = if (allChecked) {
                                                    if (isBangla) "বাতিল" else "Deselect All"
                                                } else {
                                                    if (isBangla) "সব নিন (${prodVariants.size})" else "Select All (${prodVariants.size})"
                                                },
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PrimaryPurple
                                            )
                                        }
                                    }
                                    if (prodVariants.isNotEmpty()) {
                                        IconButton(
                                            onClick = { collapsedProductIds[prod.id] = !isCollapsed },
                                            modifier = Modifier
                                                .size(40.dp)
                                                .testTag("po_catalog_minimize_${prod.id}")
                                        ) {
                                            Icon(
                                                imageVector = if (isCollapsed) Icons.Outlined.ExpandMore else Icons.Outlined.ExpandLess,
                                                contentDescription = if (isCollapsed) {
                                                    if (isBangla) "ভ্যারিয়েন্ট দেখুন" else "Expand variants"
                                                } else {
                                                    if (isBangla) "ভ্যারিয়েন্ট মিনিমাইজ করুন" else "Minimize variants"
                                                },
                                                tint = PrimaryPurple
                                            )
                                        }
                                    }
                                }

                                if (!isCollapsed) {
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = ERPSurfaceVariant)
                                Spacer(modifier = Modifier.height(6.dp))

                                if (prodVariants.isEmpty()) {
                                    // Base product row
                                    val baseKey = VariantKey(prod.id, null)
                                    val isSel = selectedKeys[baseKey] == true
                                    val inPoQty = existingOrderQtyByVariant[baseKey] ?: 0
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSel) PrimaryPurple.copy(alpha = 0.1f) else ERPSurfaceVariant.copy(alpha = 0.55f))
                                            .border(
                                                width = if (isSel) 1.2.dp else 0.5.dp,
                                                color = if (isSel) PrimaryPurple else ERPDivider,
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable { toggleKey(baseKey) }
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isSel,
                                            onCheckedChange = { toggleKey(baseKey) },
                                            colors = CheckboxDefaults.colors(checkedColor = PrimaryPurple)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isBangla) "বেস পণ্য (ভ্যারিয়েন্ট ছাড়া)" else "Base Product (Standard)",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = "SKU: ${prod.baseSku} • ${currency} ${suggestedPriceStr(prod, null).ifBlank { "0.00" }}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSecondary
                                            )
                                        }
                                        if (inPoQty > 0) {
                                            Surface(shape = RoundedCornerShape(6.dp), color = SecondaryTeal.copy(alpha = 0.14f)) {
                                                Text(
                                                    text = "PO: $inPoQty ${prod.unit}",
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = SecondaryTeal,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        prodVariants.forEach { variant ->
                                            val vKey = VariantKey(prod.id, variant.id)
                                            val isSel = selectedKeys[vKey] == true
                                            val inPoQty = existingOrderQtyByVariant[vKey] ?: 0
                                            val colorText = variant.color?.takeIf { it.isNotBlank() } ?: "-"
                                            val sizeText = variant.size?.takeIf { it.isNotBlank() } ?: "-"
                                            val priceText = suggestedPriceStr(prod, variant).ifBlank { "0.00" }
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(
                                                        if (isSel) PrimaryPurple.copy(alpha = 0.1f)
                                                        else ERPSurfaceVariant.copy(alpha = 0.45f)
                                                    )
                                                    .border(
                                                        width = if (isSel) 1.2.dp else 0.5.dp,
                                                        color = if (isSel) PrimaryPurple else ERPDivider,
                                                        shape = RoundedCornerShape(10.dp)
                                                    )
                                                    .clickable { toggleKey(vKey) }
                                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = isSel,
                                                    onCheckedChange = { toggleKey(vKey) },
                                                    colors = CheckboxDefaults.colors(checkedColor = PrimaryPurple)
                                                )
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Surface(
                                                            shape = RoundedCornerShape(5.dp),
                                                            color = PrimaryPurple.copy(alpha = 0.12f)
                                                        ) {
                                                            Text(
                                                                text = colorText,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = PrimaryPurple
                                                            )
                                                        }
                                                        Surface(
                                                            shape = RoundedCornerShape(5.dp),
                                                            color = SecondaryTeal.copy(alpha = 0.12f)
                                                        ) {
                                                            Text(
                                                                text = sizeText,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = SecondaryTeal
                                                            )
                                                        }
                                                    }
                                                    Text(
                                                        text = "SKU: ${variant.sku} • $currency $priceText",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = TextSecondary,
                                                        modifier = Modifier.padding(top = 2.dp)
                                                    )
                                                }
                                                if (inPoQty > 0) {
                                                    Surface(shape = RoundedCornerShape(6.dp), color = StatusSuccessContainer) {
                                                        Text(
                                                            text = "PO: $inPoQty",
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = StatusSuccess,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                if (onAddNewVariant != null) {
                                    TextButton(
                                        onClick = { onAddNewVariant(prod.id) },
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(15.dp), tint = SecondaryTeal)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isBangla) "+ নতুন ভ্যারিয়েন্ট যোগ করুন" else "+ Add New Variant",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = SecondaryTeal
                                        )
                                    }
                                }
                                }
                            }
                        }
                    }
                }

                // Bottom Sticky Confirm Bar
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val selectedCount = selectedKeys.count { it.value }
                        Button(
                            enabled = selectedCount > 0,
                            onClick = {
                                onConfirmSelection(buildSelectedPicks(), "")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("po_catalog_confirm_btn")
                        ) {
                            Text(
                                text = if (singleSelectMode) {
                                    if (isBangla) "নির্বাচন নিশ্চিত করুন" else "Confirm Variant"
                                } else {
                                    if (isBangla) "যোগ করুন ($selectedCount)" else "Add Selected ($selectedCount)"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Step 2 Smart Carton Variant Picker (in-window bottom sheet overlay).
 * Supports Available / All / Completed visibility modes, hides 100% packed variants
 * from the default Available list, prevents duplicate variants inside a carton,
 * and rejects quantities above Remaining + Current Carton allocation.
 */
@Composable
fun PoCartonVariantPickerSheet(
    title: String,
    products: List<Product>,
    variants: List<ProductVariant>,
    statuses: List<com.pocketdev.advance_erp.core.engine.VariantPackingStatus>,
    maxAllowedByVariant: Map<VariantKey, Int>,
    alreadyInCartonKeys: Set<VariantKey>,
    isBangla: Boolean,
    singleSelectMode: Boolean = false,
    resolveProductImage: (suspend (String, String?) -> String?)? = null,
    onDismiss: () -> Unit,
    onConfirm: (List<CartonVariantAllocationPick>) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var visibility by remember { mutableStateOf(VariantVisibility.AVAILABLE) }
    val selectedKeys = remember { mutableStateMapOf<VariantKey, Boolean>() }
    val qtyStrings = remember {
        mutableStateMapOf<VariantKey, String>().apply {
            statuses.forEach { st ->
                val maxAllowed = maxAllowedByVariant[st.key] ?: st.remaining.coerceAtLeast(0)
                if (maxAllowed > 0) {
                    put(st.key, maxAllowed.toString())
                }
            }
        }
    }

    val availableCount = remember(statuses) { statuses.count { it.remaining > 0 } }
    val completedCount = remember(statuses) { statuses.count { it.remaining <= 0 } }
    val allCount = statuses.size

    val visibleStatuses = remember(statuses, visibility, searchQuery, products, variants) {
        val q = searchQuery.trim().lowercase()
        PoPackingEngine.filterByVisibility(statuses, visibility).filter { st ->
            if (q.isEmpty()) return@filter true
            val prod = products.find { it.id == st.key.productId }
            val v = st.key.variantId?.let { vid -> variants.find { it.id == vid } }
            prod?.nameEn?.lowercase()?.contains(q) == true ||
                prod?.nameBn?.lowercase()?.contains(q) == true ||
                prod?.baseSku?.lowercase()?.contains(q) == true ||
                v?.sku?.lowercase()?.contains(q) == true ||
                v?.color?.lowercase()?.contains(q) == true ||
                v?.size?.lowercase()?.contains(q) == true
        }
    }

    val groupedByProduct = remember(visibleStatuses) {
        visibleStatuses.groupBy { it.key.productId }
    }

    val hasAnyInvalidSelected = selectedKeys.entries.any { (key, checked) ->
        if (!checked) return@any false
        val maxAllowed = maxAllowedByVariant[key] ?: 0
        val qty = qtyStrings[key]?.trim()?.toIntOrNull() ?: 0
        qty <= 0 || qty > maxAllowed || (key in alreadyInCartonKeys && !singleSelectMode)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .imePadding()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                ),
            shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
            color = ERPBackground,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (isBangla)
                                "বাকি থাকা ভ্যারিয়েন্ট নির্বাচন ও পরিমাণ নির্ধারণ করুন"
                            else
                                "Select variants & specify carton allocation quantities",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(if (isBangla) "পণ্য, রঙ, সাইজ বা SKU খুঁজুন…" else "Search product, color, size, SKU…")
                        },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = PrimaryPurple) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("po_carton_picker_search")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Available / All / Completed tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val tabs = listOf(
                            VariantVisibility.AVAILABLE to (if (isBangla) "বাকি ($availableCount)" else "Available ($availableCount)"),
                            VariantVisibility.ALL to (if (isBangla) "সব ($allCount)" else "All ($allCount)"),
                            VariantVisibility.COMPLETED to (if (isBangla) "সম্পন্ন ($completedCount)" else "Completed ($completedCount)")
                        )
                        tabs.forEach { (mode, label) ->
                            FilterChip(
                                selected = visibility == mode,
                                onClick = { visibility = mode },
                                label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryPurple,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                HorizontalDivider(color = ERPDivider)

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (groupedByProduct.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (visibility) {
                                        VariantVisibility.AVAILABLE ->
                                            if (isBangla) "এই PO-এর সব ভ্যারিয়েন্ট ইতোমধ্যে প্যাক করা হয়ে গেছে।"
                                            else "All variants in this PO are already 100% packed."
                                        VariantVisibility.COMPLETED ->
                                            if (isBangla) "এখনও কোনো ভ্যারিয়েন্ট সম্পূর্ণ প্যাক হয়নি।"
                                            else "No completed variants yet."
                                        VariantVisibility.ALL ->
                                            if (isBangla) "কোনো ভ্যারিয়েন্ট পাওয়া যায়নি।"
                                            else "No variants found."
                                    },
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    items(groupedByProduct.entries.toList(), key = { it.key }) { (productId, groupStatuses) ->
                        val prod = products.find { it.id == productId }
                        var thumbUri by remember(productId) { mutableStateOf<String?>(null) }
                        LaunchedEffect(productId) {
                            thumbUri = resolveProductImage?.invoke(productId, groupStatuses.firstOrNull()?.key?.variantId)
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, ERPDivider)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = ERPSurfaceVariant,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        if (thumbUri != null) {
                                            AsyncImage(
                                                model = rememberImageModel(thumbUri),
                                                contentDescription = prod?.nameEn,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = PrimaryPurple)
                                            }
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = prod?.let { if (isBangla && it.nameBn.isNotBlank()) it.nameBn else it.nameEn } ?: productId,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "SKU: ${prod?.baseSku.orEmpty()}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                groupStatuses.forEach { st ->
                                    val v = st.key.variantId?.let { vid -> variants.find { it.id == vid } }
                                    val maxAllowed = maxAllowedByVariant[st.key] ?: st.remaining.coerceAtLeast(0)
                                    val isDuplicateInCarton = st.key in alreadyInCartonKeys && !singleSelectMode
                                    val canPick = maxAllowed > 0 && !isDuplicateInCarton
                                    val isChecked = selectedKeys[st.key] == true
                                    val qtyText = qtyStrings[st.key].orEmpty()
                                    val parsedQty = qtyText.trim().toIntOrNull() ?: 0
                                    val qtyError = isChecked && (parsedQty <= 0 || parsedQty > maxAllowed)

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                when {
                                                    isChecked -> PrimaryPurple.copy(alpha = 0.09f)
                                                    st.isComplete -> StatusSuccessContainer.copy(alpha = 0.4f)
                                                    else -> ERPSurfaceVariant.copy(alpha = 0.45f)
                                                }
                                            )
                                            .border(
                                                width = if (isChecked) 1.2.dp else 0.5.dp,
                                                color = if (isChecked) PrimaryPurple else ERPDivider,
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable(enabled = canPick) {
                                                if (singleSelectMode) {
                                                    selectedKeys.clear()
                                                    selectedKeys[st.key] = true
                                                } else {
                                                    if (isChecked) selectedKeys.remove(st.key) else selectedKeys[st.key] = true
                                                }
                                            }
                                            .padding(8.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(
                                                checked = isChecked,
                                                enabled = canPick,
                                                onCheckedChange = { checked ->
                                                    if (!canPick) return@Checkbox
                                                    if (singleSelectMode) {
                                                        selectedKeys.clear()
                                                        if (checked) selectedKeys[st.key] = true
                                                    } else {
                                                        if (checked) selectedKeys[st.key] = true else selectedKeys.remove(st.key)
                                                    }
                                                },
                                                colors = CheckboxDefaults.colors(checkedColor = PrimaryPurple)
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                val vDesc = if (v != null) {
                                                    listOfNotNull(
                                                        v.color?.takeIf { it.isNotBlank() },
                                                        v.size?.takeIf { it.isNotBlank() }
                                                    ).joinToString(" / ").ifBlank { v.sku }
                                                } else {
                                                    if (isBangla) "বেস পণ্য" else "Base Product"
                                                }
                                                Text(
                                                    text = vDesc,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary
                                                )
                                                Text(
                                                    text = "SKU: ${v?.sku ?: prod?.baseSku.orEmpty()}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextSecondary
                                                )
                                                Text(
                                                    text = if (isBangla)
                                                        "অর্ডার: ${st.ordered} • প্যাকড: ${st.packed} • বাকি: ${st.remaining} PCS"
                                                    else
                                                        "Ordered: ${st.ordered} • Packed: ${st.packed} • Remaining: ${st.remaining} PCS",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (st.remaining > 0) PrimaryPurple else StatusSuccess
                                                )
                                                if (isDuplicateInCarton) {
                                                    Text(
                                                        text = if (isBangla) "এই কার্টনে ইতোমধ্যেই যুক্ত আছে" else "Already in this carton",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = StatusWarning,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                            if (st.isComplete) {
                                                Icon(
                                                    Icons.Outlined.CheckCircle,
                                                    contentDescription = "Completed",
                                                    tint = StatusSuccess,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        if (isChecked) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            OutlinedTextField(
                                                value = qtyText,
                                                onValueChange = { qtyStrings[st.key] = it.filter { ch -> ch.isDigit() } },
                                                label = {
                                                    Text(
                                                        if (isBangla) "কার্টনে প্যাক পরিমাণ (সর্বোচ্চ $maxAllowed)"
                                                        else "Pack Quantity (Max $maxAllowed)"
                                                    )
                                                },
                                                isError = qtyError,
                                                supportingText = if (qtyError) {
                                                    {
                                                        Text(
                                                            if (isBangla)
                                                                "১ থেকে $maxAllowed PCS-এর মধ্যে পরিমাণ দিন"
                                                            else
                                                                "Enter between 1 and $maxAllowed PCS (remaining limit)"
                                                        )
                                                    }
                                                } else null,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Confirm Bar
                Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
                    val selectedCount = selectedKeys.count { it.value }
                    Button(
                        enabled = selectedCount > 0 && !hasAnyInvalidSelected,
                        onClick = {
                            val picks = selectedKeys.entries
                                .filter { it.value }
                                .mapNotNull { (key, _) ->
                                    val qty = qtyStrings[key]?.trim()?.toIntOrNull() ?: 0
                                    if (qty > 0) CartonVariantAllocationPick(key.productId, key.variantId, qty) else null
                                }
                            onConfirm(picks)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .heightIn(min = 48.dp)
                            .testTag("po_carton_picker_confirm")
                    ) {
                        Text(
                            text = if (isBangla) "কার্টনে যোগ করুন ($selectedCount)" else "Add Selected to Carton ($selectedCount)",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * In-window dialog for creating multiple identical cartons while strictly enforcing
 * [PoPackingEngine.maxIdenticalCartons] so overpacking can never occur.
 */
@Composable
fun BulkCreateCartonsDialog(
    products: List<Product>,
    variants: List<ProductVariant>,
    statuses: List<com.pocketdev.advance_erp.core.engine.VariantPackingStatus>,
    isBangla: Boolean,
    onDismiss: () -> Unit,
    onConfirmBulk: (
        cartonCount: Int,
        grossWeightKg: BigDecimal,
        netWeightKg: BigDecimal,
        measurementStr: String,
        perCartonAllocations: List<CartonVariantAllocationPick>
    ) -> Unit
) {
    val availableStatuses = remember(statuses) { statuses.filter { it.remaining > 0 } }
    val selectedPerCartonQty = remember {
        mutableStateMapOf<VariantKey, String>().apply {
            availableStatuses.firstOrNull()?.let { first ->
                put(first.key, minOf(first.remaining, 20).coerceAtLeast(1).toString())
            }
        }
    }
    var cartonCountStr by remember { mutableStateOf("1") }
    var grossWeightStr by remember { mutableStateOf("15.0") }
    var netWeightStr by remember { mutableStateOf("") }
    var measurementStr by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    val perCartonMap = selectedPerCartonQty.mapNotNull { (key, raw) ->
        val q = raw.trim().toIntOrNull() ?: 0
        if (q > 0) key to q else null
    }.toMap()

    val orderLines = remember(statuses) {
        statuses.map { com.pocketdev.advance_erp.core.engine.PackingOrderLine(it.key, it.ordered) }
    }
    val existingCartonLines = remember(statuses) {
        statuses.mapNotNull { st ->
            if (st.packed > 0) com.pocketdev.advance_erp.core.engine.PackingCartonLine("existing", st.key, st.packed) else null
        }
    }
    val maxAllowedCartons = remember(orderLines, existingCartonLines, perCartonMap) {
        if (perCartonMap.isEmpty()) 0
        else PoPackingEngine.maxIdenticalCartons(orderLines, existingCartonLines, perCartonMap)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.86f)
                .imePadding()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                ),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isBangla) "একসাথে একাধিক কার্টন তৈরি (Bulk)" else "Bulk Create Identical Cartons",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isBangla)
                        "সর্বোচ্চ তৈরিযোগ্য সমরূপ কার্টন: $maxAllowedCartons CTN"
                    else
                        "Max identical cartons allowed from remaining qty: $maxAllowedCartons CTN",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (maxAllowedCartons > 0) SecondaryTeal else StatusError
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = cartonCountStr,
                                onValueChange = { cartonCountStr = it.filter { c -> c.isDigit() } },
                                label = { Text(if (isBangla) "কার্টন সংখ্যা *" else "Carton Count *") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = grossWeightStr,
                                onValueChange = { grossWeightStr = it },
                                label = { Text(if (isBangla) "গ্রস ওজন/কার্টন (KG)" else "Gross/CTN (KG)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = netWeightStr,
                                onValueChange = { netWeightStr = it },
                                label = { Text(if (isBangla) "নেট ওজন (KG)" else "Net/CTN (KG)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = measurementStr,
                                onValueChange = { measurementStr = it },
                                label = { Text(if (isBangla) "মাপ L*W*H (cm)" else "Dim L*W*H (cm)") },
                                placeholder = { Text("50*40*30") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        Text(
                            text = if (isBangla) "প্রতি কার্টনে পণ্যের পরিমাণ:" else "PCS per Carton for Each Variant:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    items(availableStatuses, key = { "${it.key.productId}:${it.key.variantId}" }) { st ->
                        val prod = products.find { it.id == st.key.productId }
                        val v = st.key.variantId?.let { vid -> variants.find { it.id == vid } }
                        val label = buildString {
                            append(prod?.let { if (isBangla && it.nameBn.isNotBlank()) it.nameBn else it.nameEn } ?: st.key.productId)
                            if (v != null) {
                                val spec = listOfNotNull(v.color, v.size).joinToString("/")
                                if (spec.isNotBlank()) append(" ($spec)")
                            }
                        }
                        val isSelected = selectedPerCartonQty.containsKey(st.key)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) PrimaryPurple.copy(alpha = 0.08f) else ERPSurfaceVariant.copy(alpha = 0.45f))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        selectedPerCartonQty[st.key] = minOf(st.remaining, 20).coerceAtLeast(1).toString()
                                    } else {
                                        selectedPerCartonQty.remove(st.key)
                                    }
                                }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = if (isBangla) "বাকি আছে: ${st.remaining} PCS" else "Remaining: ${st.remaining} PCS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PrimaryPurple
                                )
                            }
                            if (isSelected) {
                                OutlinedTextField(
                                    value = selectedPerCartonQty[st.key].orEmpty(),
                                    onValueChange = { selectedPerCartonQty[st.key] = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("PCS/CTN", fontSize = 10.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.width(96.dp)
                                )
                            }
                        }
                    }
                }

                localError?.let { err ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(err, color = StatusError, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text(if (isBangla) "বাতিল" else "Cancel")
                    }
                    Button(
                        onClick = {
                            val count = cartonCountStr.trim().toIntOrNull() ?: 0
                            val gw = grossWeightStr.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
                            val nw = netWeightStr.trim().toBigDecimalOrNull() ?: gw
                            when {
                                perCartonMap.isEmpty() ->
                                    localError = if (isBangla) "অন্তত একটি ভ্যারিয়েন্ট ও পরিমাণ দিন।" else "Select at least one variant with PCS/carton."
                                count <= 0 ->
                                    localError = if (isBangla) "কার্টন সংখ্যা ১ বা তার বেশি হতে হবে।" else "Carton count must be at least 1."
                                count > maxAllowedCartons ->
                                    localError = if (isBangla)
                                        "$count টি কার্টন তৈরি করলে অর্ডারের পরিমাণ ছাড়িয়ে যাবে। সর্বোচ্চ $maxAllowedCartons টি তৈরি করা যাবে।"
                                    else
                                        "Creating $count cartons exceeds remaining PO quantities (max $maxAllowedCartons allowed)."
                                gw <= BigDecimal.ZERO || nw < BigDecimal.ZERO || nw > gw ->
                                    localError = if (isBangla) "বৈধ গ্রস ও নেট ওজন দিন।" else "Enter valid gross and net weights (0 <= Net <= Gross)."
                                else -> {
                                    localError = null
                                    val picks = perCartonMap.entries.map { (k, q) ->
                                        CartonVariantAllocationPick(k.productId, k.variantId, q)
                                    }
                                    onConfirmBulk(count, gw, nw, measurementStr, picks)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                        modifier = Modifier.weight(1.4f)
                    ) {
                        Text(if (isBangla) "কার্টন তৈরি করুন" else "Create Cartons", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
