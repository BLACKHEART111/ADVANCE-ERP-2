@file:OptIn(ExperimentalLayoutApi::class)

package com.pocketdev.advance_erp.feature.importvault

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FlightTakeoff
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pocketdev.advance_erp.core.designsystem.StatusInfo
import com.pocketdev.advance_erp.core.designsystem.StatusInfoContainer
import com.pocketdev.advance_erp.core.designsystem.StatusSuccess
import com.pocketdev.advance_erp.core.designsystem.StatusSuccessContainer
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.domain.model.VaultDocumentCategory
import com.pocketdev.advance_erp.domain.model.VaultDocumentRef
import com.pocketdev.advance_erp.domain.model.VaultTimelineEvent
import com.pocketdev.advance_erp.domain.model.VaultTimelineEventType

fun LazyListScope.timelineDocumentsItems(
    documents: List<VaultDocumentRef>,
    timelineEvents: List<VaultTimelineEvent>,
    subTabIdx: Int,
    selectedDocCategory: VaultDocumentCategory?,
    docSearchQuery: String = "",
    language: AppLanguage,
    onSubTabChange: (Int) -> Unit,
    onDocCategorySelected: (VaultDocumentCategory?) -> Unit,
    onDocSearchQueryChange: (String) -> Unit = {},
    onAddAttachmentClick: () -> Unit = {},
    onOpenExportClick: () -> Unit = {},
    onPreviewDocument: (VaultDocumentRef) -> Unit = {},
    onShareDocument: (VaultDocumentRef) -> Unit = {},
    onDeleteAttachment: (String) -> Unit = {},
    onNavigateToRoute: (String) -> Unit
) {
    val isBn = language == AppLanguage.BANGLA

    item(key = "tl_doc_tabs") {
        VaultWrappedTabBar(
            options = listOf(
                0 to (if (isBn) "নথিপত্র (${Formatters.formatNumber(documents.size.toLong(), language)})" else "Documents (${documents.size})"),
                1 to (if (isBn) "টাইমলাইন (${Formatters.formatNumber(timelineEvents.size.toLong(), language)})" else "Timeline (${timelineEvents.size})")
            ),
            selectedIndex = subTabIdx,
            onSelected = onSubTabChange
        )
    }

    if (subTabIdx == 0) {
        item(key = "doc_actions_toolbar") {
            VaultCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onAddAttachmentClick,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Outlined.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (isBn) "ছবি / PDF যোগ করুন" else "Attach Photo / PDF",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    OutlinedButton(
                        onClick = onOpenExportClick,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (isBn) "PDF / XLSX এক্সপোর্ট" else "Export PDF / XLSX",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                OutlinedTextField(
                    value = docSearchQuery,
                    onValueChange = onDocSearchQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = {
                        if (docSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { onDocSearchQueryChange("") }) {
                                Icon(Icons.Outlined.Close, contentDescription = null)
                            }
                        }
                    },
                    placeholder = {
                        Text(if (isBn) "নথি বা রেফারেন্স খুঁজুন..." else "Search documents or references...")
                    }
                )
            }
        }

        item(key = "doc_category_filter") {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterOptionChip(
                    label = if (isBn) "সব নথি" else "All Docs",
                    selected = selectedDocCategory == null,
                    onClick = { onDocCategorySelected(null) }
                )
                VaultDocumentCategory.entries.forEach { cat ->
                    FilterOptionChip(
                        label = vaultDocCategoryLabel(cat, language),
                        selected = selectedDocCategory == cat,
                        onClick = { onDocCategorySelected(if (selectedDocCategory == cat) null else cat) }
                    )
                }
            }
        }

        val needle = docSearchQuery.trim()
        val filteredDocs = documents.filter { doc ->
            val catMatch = selectedDocCategory == null || doc.category == selectedDocCategory
            val textMatch = needle.isEmpty() ||
                doc.titleEn.contains(needle, ignoreCase = true) ||
                doc.titleBn.contains(needle, ignoreCase = true) ||
                doc.referenceNumber.contains(needle, ignoreCase = true) ||
                doc.subtitle.contains(needle, ignoreCase = true)
            catMatch && textMatch
        }

        if (filteredDocs.isEmpty()) {
            item(key = "empty_docs") {
                VaultCard {
                    Text(
                        text = if (isBn) "এই ফিল্টারে কোনো নথি পাওয়া যায়নি।" else "No matching documents found.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(filteredDocs, key = { it.id }) { doc ->
                DocumentRefRowCard(
                    doc = doc,
                    language = language,
                    onOpenExportClick = onOpenExportClick,
                    onPreviewDocument = onPreviewDocument,
                    onShareDocument = onShareDocument,
                    onDeleteAttachment = onDeleteAttachment,
                    onNavigateToRoute = onNavigateToRoute
                )
            }
        }
    } else {
        if (timelineEvents.isEmpty()) {
            item(key = "empty_timeline") {
                VaultCard {
                    Text(
                        text = if (isBn) "এখনো কোনো টাইমলাইন ইভেন্ট রেকর্ড হয়নি।" else "No timeline events recorded yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(timelineEvents, key = { it.id }) { evt ->
                TimelineEventRowCard(
                    event = evt,
                    language = language,
                    onNavigateToRoute = onNavigateToRoute
                )
            }
        }
    }
}

@Composable
private fun DocumentRefRowCard(
    doc: VaultDocumentRef,
    language: AppLanguage,
    onOpenExportClick: () -> Unit,
    onPreviewDocument: (VaultDocumentRef) -> Unit,
    onShareDocument: (VaultDocumentRef) -> Unit,
    onDeleteAttachment: (String) -> Unit,
    onNavigateToRoute: (String) -> Unit
) {
    val isBn = language == AppLanguage.BANGLA
    val colors = MaterialTheme.colorScheme
    val icon = when (doc.category) {
        VaultDocumentCategory.PURCHASE -> Icons.Outlined.ShoppingCart
        VaultDocumentCategory.PACKING -> Icons.Outlined.Inventory2
        VaultDocumentCategory.FREIGHT -> Icons.Outlined.LocalShipping
        VaultDocumentCategory.RECEIVING -> Icons.Outlined.Inventory2
        VaultDocumentCategory.SALES -> Icons.Outlined.ReceiptLong
        VaultDocumentCategory.PAYMENTS -> Icons.Outlined.Payments
        VaultDocumentCategory.REPORTS -> Icons.Outlined.Assessment
        VaultDocumentCategory.ATTACHMENTS -> Icons.Outlined.AttachFile
    }

    VaultCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = colors.primary)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (isBn) doc.titleBn else doc.titleEn,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
                Text(
                    text = "${doc.referenceNumber} · ${doc.subtitle}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = Formatters.formatDate(doc.timestampMillis, language),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                    if (doc.amountMinor != null) {
                        Text(
                            text = ImportVaultUiLogic.formatCurrency(doc.amountMinor, doc.currencyCode, language),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary
                        )
                    }
                    when {
                        doc.isLiveRegeneratedReport -> {
                            VaultStatusBadge(
                                label = if (isBn) "লাইভ রিজেনারেটেড" else "Live Regenerated",
                                containerColor = StatusInfoContainer,
                                contentColor = StatusInfo
                            )
                        }
                        doc.isImmutableSnapshot -> {
                            VaultStatusBadge(
                                label = if (isBn) "সংরক্ষিত স্ন্যাপশট (Immutable)" else "Immutable Snapshot",
                                containerColor = StatusSuccessContainer,
                                contentColor = StatusSuccess
                            )
                        }
                        doc.attachmentId != null -> {
                            VaultStatusBadge(
                                label = if (isBn) "ভল্ট অ্যাটাচমেন্ট" else "Vault Attachment",
                                containerColor = colors.secondaryContainer,
                                contentColor = colors.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }

        // Context-aware document actions (>= 48dp touch targets)
        val hasLocalFile = !doc.localFilePath.isNullOrBlank()
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (doc.isLiveRegeneratedReport) {
                OutlinedButton(
                    onClick = onOpenExportClick,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isBn) "PDF / XLSX তৈরি করুন" else "Generate PDF / XLSX")
                }
            }
            if (hasLocalFile) {
                OutlinedButton(
                    onClick = { onPreviewDocument(doc) },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Outlined.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isBn) "দেখুন" else "Preview")
                }
                OutlinedButton(
                    onClick = { onShareDocument(doc) },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isBn) "শেয়ার" else "Share")
                }
            }
            if (doc.attachmentId != null) {
                OutlinedButton(
                    onClick = { onDeleteAttachment(doc.attachmentId) },
                    border = BorderStroke(1.dp, colors.error.copy(alpha = 0.5f)),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = colors.error, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (isBn) "মুছুন" else "Delete", color = colors.error)
                }
            }
            doc.targetRoute?.let { route ->
                OutlinedButton(
                    onClick = { onNavigateToRoute(route) },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(if (isBn) "সোর্স মডিউল" else "Open Module")
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun TimelineEventRowCard(
    event: VaultTimelineEvent,
    language: AppLanguage,
    onNavigateToRoute: (String) -> Unit
) {
    val isBn = language == AppLanguage.BANGLA
    val colors = MaterialTheme.colorScheme
    val icon = when (event.type) {
        VaultTimelineEventType.PO_CREATED -> Icons.Outlined.ShoppingCart
        VaultTimelineEventType.SHIPMENT_DISPATCHED, VaultTimelineEventType.SHIPMENT_ARRIVED -> Icons.Outlined.FlightTakeoff
        VaultTimelineEventType.COST_FINALIZED -> Icons.Outlined.Assessment
        VaultTimelineEventType.WAREHOUSE_RECEIVED -> Icons.Outlined.Inventory2
        VaultTimelineEventType.SALE_POSTED -> Icons.Outlined.ShoppingBag
        VaultTimelineEventType.SALES_RETURN -> Icons.Outlined.History
        VaultTimelineEventType.SUPPLIER_PAYMENT, VaultTimelineEventType.CUSTOMER_PAYMENT, VaultTimelineEventType.EXPENSE_RECORDED -> Icons.Outlined.Payments
        VaultTimelineEventType.COST_ADJUSTMENT -> Icons.Outlined.Description
    }

    VaultCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = colors.secondary, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (isBn) event.titleBn else event.titleEn,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
                Text(
                    text = "${event.referenceNumber} · ${event.subtitle}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = "${Formatters.formatDate(event.timestampMillis, language)} ${Formatters.formatTime(event.timestampMillis, language)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                    if (event.amountMinor != null) {
                        Text(
                            text = ImportVaultUiLogic.formatCurrency(event.amountMinor, event.currencyCode, language),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary
                        )
                    }
                }
            }
        }
        event.targetRoute?.let { route ->
            OutlinedButton(
                onClick = { onNavigateToRoute(route) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) {
                Text(if (isBn) "সোর্স দেখুন" else "View Source")
            }
        }
    }
}
