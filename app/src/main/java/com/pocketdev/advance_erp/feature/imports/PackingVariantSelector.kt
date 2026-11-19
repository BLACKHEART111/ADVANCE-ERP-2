package com.pocketdev.advance_erp.feature.imports

import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.pocketdev.advance_erp.data.entity.ProductVariant

@Composable
internal fun PackingVariantSelector(productId: String, variantId: String?, variants: List<ProductVariant>,
    isBangla: Boolean, onSelect: (String?) -> Unit) {
    val choices = variants.filter { it.productId == productId && it.isActive }
    if (choices.isEmpty()) return
    var expanded by rememberSaveable { mutableStateOf(false) }
    fun label(v: ProductVariant) = listOfNotNull(v.color, v.size, v.sku).filter { it.isNotBlank() }.joinToString(" / ")
    val base = if (isBangla) "ভ্যারিয়েন্ট ছাড়া" else "Base product"
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(choices.find { it.id == variantId }?.let { label(it) } ?: base)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text(base) }, onClick = { onSelect(null); expanded = false })
            choices.forEach { v -> DropdownMenuItem(text = { Text(label(v)) }, onClick = { onSelect(v.id); expanded = false }) }
        }
    }
}
