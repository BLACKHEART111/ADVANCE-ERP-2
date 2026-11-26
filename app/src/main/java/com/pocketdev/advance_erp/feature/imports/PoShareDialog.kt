package com.pocketdev.advance_erp.feature.imports

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pocketdev.advance_erp.core.communication.CommunicationModule
import com.pocketdev.advance_erp.core.communication.CommunicationRequest
import com.pocketdev.advance_erp.core.designsystem.ERPDivider
import com.pocketdev.advance_erp.core.designsystem.ERPSurfaceVariant
import com.pocketdev.advance_erp.core.designsystem.PrimaryPurple
import com.pocketdev.advance_erp.core.designsystem.SecondaryTeal
import com.pocketdev.advance_erp.core.designsystem.StatusError
import com.pocketdev.advance_erp.core.designsystem.StatusInfo
import com.pocketdev.advance_erp.core.designsystem.StatusInfoContainer
import com.pocketdev.advance_erp.core.designsystem.StatusSuccess
import com.pocketdev.advance_erp.core.designsystem.StatusSuccessContainer
import com.pocketdev.advance_erp.core.designsystem.StatusWarning
import com.pocketdev.advance_erp.core.designsystem.StatusWarningContainer
import com.pocketdev.advance_erp.core.designsystem.TextMuted
import com.pocketdev.advance_erp.core.designsystem.TextPrimary
import com.pocketdev.advance_erp.core.designsystem.TextSecondary
import com.pocketdev.advance_erp.core.util.ControlCenterShareService
import com.pocketdev.advance_erp.core.util.ImportExcelGenerator
import com.pocketdev.advance_erp.core.util.ImportPdfGenerator
import com.pocketdev.advance_erp.core.util.LabelPrintingEngine
import com.pocketdev.advance_erp.core.util.PoAttachmentInfo
import com.pocketdev.advance_erp.core.util.PoAttachmentKind
import com.pocketdev.advance_erp.core.util.PoAttachmentStorage
import com.pocketdev.advance_erp.core.util.PoExportDataBuilder
import com.pocketdev.advance_erp.data.entity.BusinessProfile
import com.pocketdev.advance_erp.data.entity.Product
import com.pocketdev.advance_erp.data.entity.ProductVariant
import com.pocketdev.advance_erp.data.entity.PurchaseOrder
import com.pocketdev.advance_erp.data.entity.PurchaseOrderItem
import com.pocketdev.advance_erp.data.entity.ShipmentCarton
import com.pocketdev.advance_erp.data.entity.Supplier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.math.RoundingMode

private fun shareFileToApp(
    context: Context,
    file: File,
    mimeType: String,
    chooserTitle: String,
    includeAttachments: Boolean = false,
    attachments: List<PoAttachmentInfo> = emptyList()
) {
    try {
        if (!file.exists() || file.length() <= 0L) {
            Toast.makeText(context, "ফাইল তৈরি বা পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
            return
        }
        if (!includeAttachments || attachments.isEmpty()) {
            ImportPdfGenerator.launchShareChooser(context, file, mimeType, chooserTitle)
            return
        }
        val extraFiles = attachments.mapNotNull { info ->
            info.localFile?.takeIf { it.exists() && it.length() > 0L }?.let { local ->
                local to PoAttachmentStorage.getMimeTypeForFile(local)
            }
        }
        val linkLines = attachments.filter { it.kind == PoAttachmentKind.LINK }.map { it.rawValue }
        val extraText = if (linkLines.isNotEmpty()) linkLines.joinToString("\n") else null
        ImportPdfGenerator.launchShareFilesChooser(
            context = context,
            files = listOf(file to mimeType) + extraFiles,
            chooserTitle = chooserTitle,
            extraText = extraText
        )
    } catch (t: Throwable) {
        Toast.makeText(context, "শেয়ার করতে সমস্যা হয়েছে: ${t.message ?: t::class.java.simpleName}", Toast.LENGTH_LONG).show()
    }
}

enum class PoExportPerspective {
    SUPPLIER_COPY,
    FREIGHT_FORWARDER_PACKING_LIST,
    COMPANY_INTERNAL_COPY
}

@Composable
fun PoShareDialog(
    order: PurchaseOrder,
    items: List<PurchaseOrderItem>,
    productsMap: Map<String, Product>,
    supplier: Supplier?,
    businessProfile: BusinessProfile?,
    isBangla: Boolean = false,
    variantsMap: Map<String, ProductVariant> = emptyMap(),
    cartons: List<ShipmentCarton> = emptyList(),
    onResolveItemImages: suspend (List<PurchaseOrderItem>) -> Map<String, String> = { emptyMap() },
    onOpenSmartWhatsApp: ((CommunicationRequest) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedPerspective by remember { mutableStateOf(PoExportPerspective.SUPPLIER_COPY) }
    var isExporting by rememberSaveable { mutableStateOf(false) }
    var exportError by remember { mutableStateOf<String?>(null) }
    var previewPdfFile by remember { mutableStateOf<File?>(null) }
    var previewPdfTitle by remember { mutableStateOf("") }
    var exportSelectedOnly by rememberSaveable(order.id) { mutableStateOf(false) }

    val baseExportPreview = remember(order, items, cartons, productsMap, variantsMap, supplier, businessProfile) {
        PoExportDataBuilder.build(
            order = order,
            items = items,
            productsMap = productsMap,
            supplier = supplier,
            businessProfile = businessProfile,
            variantsMap = variantsMap,
            itemImagePaths = emptyMap(),
            cartons = cartons,
            selectedCartonNumbers = null
        )
    }
    val selectedCartonNumbers = remember(order.id, baseExportPreview.allCartons) {
        mutableStateListOf<String>().apply {
            addAll(baseExportPreview.allCartons.map { it.cartonNumber })
        }
    }

    val activeCartonFilter: Set<String>? = if (
        selectedPerspective != PoExportPerspective.COMPANY_INTERNAL_COPY &&
        exportSelectedOnly &&
        baseExportPreview.allCartons.isNotEmpty()
    ) {
        selectedCartonNumbers.toSet()
    } else {
        null
    }

    val validatedExport = remember(order, items, cartons, productsMap, variantsMap, supplier, businessProfile, activeCartonFilter) {
        PoExportDataBuilder.build(
            order = order,
            items = items,
            productsMap = productsMap,
            supplier = supplier,
            businessProfile = businessProfile,
            variantsMap = variantsMap,
            itemImagePaths = emptyMap(),
            cartons = cartons,
            selectedCartonNumbers = activeCartonFilter
        )
    }

    val isLocalOrder = order.currency == "BDT"
    val attachmentInfos = remember(order.attachments) {
        PoAttachmentStorage.parseAttachments(order.attachments)
            .map { PoAttachmentStorage.inspectAttachment(it) }
            .filter { it.localFile != null || it.kind == PoAttachmentKind.LINK }
    }
    var includeAttachments by rememberSaveable(order.id) { mutableStateOf(false) }

    val canExportSupplier = when (selectedPerspective) {
        PoExportPerspective.SUPPLIER_COPY -> validatedExport.isValidForSupplierExport
        PoExportPerspective.FREIGHT_FORWARDER_PACKING_LIST -> validatedExport.blockingErrors.isEmpty() && (!exportSelectedOnly || selectedCartonNumbers.isNotEmpty())
        PoExportPerspective.COMPANY_INTERNAL_COPY -> true
    }

    suspend fun buildSupplierOrInternalPdf(): File = withContext(Dispatchers.IO) {
        when (selectedPerspective) {
            PoExportPerspective.SUPPLIER_COPY -> {
                val itemImagePaths = onResolveItemImages(items)
                ImportPdfGenerator.generateSupplierCommercialOrderPdf(
                    context = context,
                    order = order,
                    items = items,
                    productsMap = productsMap,
                    supplier = supplier,
                    businessProfile = businessProfile,
                    variantsMap = variantsMap,
                    itemImagePaths = itemImagePaths,
                    cartons = cartons,
                    selectedCartonNumbers = activeCartonFilter
                )
            }
            PoExportPerspective.FREIGHT_FORWARDER_PACKING_LIST -> {
                val itemImagePaths = onResolveItemImages(items)
                ImportPdfGenerator.generateFreightForwarderPackingListPdf(
                    context = context,
                    order = order,
                    items = items,
                    productsMap = productsMap,
                    supplier = supplier,
                    businessProfile = businessProfile,
                    variantsMap = variantsMap,
                    itemImagePaths = itemImagePaths,
                    cartons = cartons,
                    selectedCartonNumbers = activeCartonFilter
                )
            }
            PoExportPerspective.COMPANY_INTERNAL_COPY -> {
                ImportPdfGenerator.generateCompanyInternalOrderPdf(
                    context, order, items, productsMap, supplier, businessProfile, isBangla
                )
            }
        }
    }

    suspend fun buildSupplierOrInternalExcel(): File = withContext(Dispatchers.IO) {
        when (selectedPerspective) {
            PoExportPerspective.SUPPLIER_COPY -> {
                val itemImagePaths = onResolveItemImages(items)
                ImportExcelGenerator.generateSupplierOrderExcel(
                    context = context,
                    order = order,
                    items = items,
                    productsMap = productsMap,
                    supplier = supplier,
                    businessProfile = businessProfile,
                    variantsMap = variantsMap,
                    itemImagePaths = itemImagePaths,
                    cartons = cartons,
                    selectedCartonNumbers = activeCartonFilter
                )
            }
            PoExportPerspective.FREIGHT_FORWARDER_PACKING_LIST -> {
                val itemImagePaths = onResolveItemImages(items)
                ImportExcelGenerator.generateFreightForwarderPackingListExcel(
                    context = context,
                    order = order,
                    items = items,
                    productsMap = productsMap,
                    supplier = supplier,
                    businessProfile = businessProfile,
                    variantsMap = variantsMap,
                    itemImagePaths = itemImagePaths,
                    cartons = cartons,
                    selectedCartonNumbers = activeCartonFilter
                )
            }
            PoExportPerspective.COMPANY_INTERNAL_COPY -> {
                ImportExcelGenerator.generateCompanyInternalOrderExcel(
                    context, order, items, productsMap, supplier, businessProfile
                )
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 760.dp)
                .clip(RoundedCornerShape(18.dp)),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isBangla) "পারচেজ অর্ডার শেয়ার ও এক্সপোর্ট" else "Share & Export Purchase Order",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${order.poNumber} • ${supplier?.companyName ?: "Supplier"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Perspective Selector Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedPerspective == PoExportPerspective.SUPPLIER_COPY)
                            PrimaryPurple.copy(alpha = 0.12f) else ERPSurfaceVariant,
                        border = BorderStroke(
                            1.5.dp,
                            if (selectedPerspective == PoExportPerspective.SUPPLIER_COPY) PrimaryPurple else ERPDivider
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedPerspective = PoExportPerspective.SUPPLIER_COPY }
                            .padding(vertical = 8.dp, horizontal = 6.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = (if (isLocalOrder) "🇧🇩 " else "🇨🇳 ") + if (isBangla) "সরবরাহকারী" else "Supplier Copy",
                                fontWeight = FontWeight.Bold,
                                color = if (selectedPerspective == PoExportPerspective.SUPPLIER_COPY) PrimaryPurple else TextPrimary,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                            Text(
                                text = if (isLocalOrder)
                                    (if (isBangla) "(মার্জিন বাদ)" else "(No Margin)")
                                else
                                    (if (isBangla) "(ফ্যাক্টরি কপি)" else "(No BDT)"),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                fontSize = 9.5.sp,
                                maxLines = 1
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedPerspective == PoExportPerspective.FREIGHT_FORWARDER_PACKING_LIST)
                            Color(0xFF0D9488).copy(alpha = 0.12f) else ERPSurfaceVariant,
                        border = BorderStroke(
                            1.5.dp,
                            if (selectedPerspective == PoExportPerspective.FREIGHT_FORWARDER_PACKING_LIST) Color(0xFF0D9488) else ERPDivider
                        ),
                        modifier = Modifier
                            .weight(1.05f)
                            .clickable { selectedPerspective = PoExportPerspective.FREIGHT_FORWARDER_PACKING_LIST }
                            .padding(vertical = 8.dp, horizontal = 6.dp)
                            .testTag("po_forwarder_tab")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "🚢 " + if (isBangla) "ফরওয়ার্ডার" else "Forwarder",
                                fontWeight = FontWeight.Bold,
                                color = if (selectedPerspective == PoExportPerspective.FREIGHT_FORWARDER_PACKING_LIST) Color(0xFF0D9488) else TextPrimary,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                            Text(
                                text = if (isBangla) "(প্যাকিং + ছবি)" else "(Packing + Photo)",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                fontSize = 9.5.sp,
                                maxLines = 1
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedPerspective == PoExportPerspective.COMPANY_INTERNAL_COPY)
                            SecondaryTeal.copy(alpha = 0.12f) else ERPSurfaceVariant,
                        border = BorderStroke(
                            1.5.dp,
                            if (selectedPerspective == PoExportPerspective.COMPANY_INTERNAL_COPY) SecondaryTeal else ERPDivider
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedPerspective = PoExportPerspective.COMPANY_INTERNAL_COPY }
                            .padding(vertical = 8.dp, horizontal = 6.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "🏢 " + if (isBangla) "অডিট কপি" else "Internal Audit",
                                fontWeight = FontWeight.Bold,
                                color = if (selectedPerspective == PoExportPerspective.COMPANY_INTERNAL_COPY) SecondaryTeal else TextPrimary,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                            Text(
                                text = if (isBangla) "(পূর্ণ কস্টিং)" else "(Full Audit)",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                fontSize = 9.5.sp,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Perspective Info Box
                if (selectedPerspective != PoExportPerspective.COMPANY_INTERNAL_COPY) {
                    val isForwarder = selectedPerspective == PoExportPerspective.FREIGHT_FORWARDER_PACKING_LIST
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isForwarder) Color(0xFFCCFBF1) else StatusSuccessContainer
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Outlined.Lock,
                                    contentDescription = null,
                                    tint = if (isForwarder) Color(0xFF0F766E) else StatusSuccess,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = when {
                                        isForwarder && isBangla -> "ফ্রেইট ফরওয়ার্ডার প্যাকিং লিস্ট (ছবি, ওজন, মাপ ও CBM — দাম ও রঙ গোপন)"
                                        isForwarder -> "Freight Forwarder Packing List (Photos, Weight, L×W×H & CBM — No Color or Prices)"
                                        isLocalOrder && isBangla -> "দেশীয় সরবরাহকারী কপি (PDF ও ৪-শিট Excel একক ডেটা)"
                                        isLocalOrder -> "Local Supplier Copy (Unified PDF & 4-Sheet Excel)"
                                        isBangla -> "ওভারসিজ কারখানা কপি (PDF ও ৪-শিট Excel একক ডেটা)"
                                        else -> "Overseas Supplier Copy (Unified PDF & 4-Sheet Excel)"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    color = if (isForwarder) Color(0xFF0F766E) else StatusSuccess,
                                    fontSize = 11.5.sp
                                )
                            }
                        }
                    }

                    // Phase 8 Validation Banner
                    if (validatedExport.blockingErrors.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = StatusError.copy(alpha = 0.1f)),
                            border = BorderStroke(1.dp, StatusError),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = StatusError, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isBangla) "এক্সপোর্ট ব্লক (গুরুত্বপূর্ণ অসঙ্গতি):" else "Export Blocked (Critical Validation Error):",
                                        fontWeight = FontWeight.Bold,
                                        color = StatusError,
                                        fontSize = 11.5.sp
                                    )
                                }
                                validatedExport.blockingErrors.forEach { err ->
                                    Text("• $err", style = MaterialTheme.typography.bodySmall, color = StatusError, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    if (validatedExport.warnings.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = StatusWarningContainer),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = StatusWarning, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isBangla) "তথ্য সতর্কতা (${validatedExport.warnings.size}):" else "Data Warnings (${validatedExport.warnings.size}):",
                                        fontWeight = FontWeight.Bold,
                                        color = StatusWarning,
                                        fontSize = 11.sp
                                    )
                                }
                                validatedExport.warnings.take(4).forEach { warn ->
                                    Text("• $warn", style = MaterialTheme.typography.labelSmall, color = StatusWarning)
                                }
                            }
                        }
                    }

                    // Phase 9: Export All vs Selected Cartons
                    if (baseExportPreview.allCartons.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = ERPSurfaceVariant),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, ERPDivider)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = if (isBangla) "কার্টন এক্সপোর্ট পরিসর (All / Selected Cartons):" else "Carton Export Scope (All / Selected Cartons):",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = !exportSelectedOnly,
                                        onClick = { exportSelectedOnly = false },
                                        label = { Text(if (isBangla) "সব কার্টন (${baseExportPreview.allCartons.size})" else "All Cartons (${baseExportPreview.allCartons.size})", fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryPurple, selectedLabelColor = MaterialTheme.colorScheme.onPrimary),
                                        modifier = Modifier.weight(1f).testTag("export_all_cartons_chip")
                                    )
                                    FilterChip(
                                        selected = exportSelectedOnly,
                                        onClick = { exportSelectedOnly = true },
                                        label = { Text(if (isBangla) "নির্বাচিত কার্টন" else "Selected Cartons", fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SecondaryTeal, selectedLabelColor = Color.White),
                                        modifier = Modifier.weight(1f).testTag("export_selected_cartons_chip")
                                    )
                                }

                                if (exportSelectedOnly) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(
                                            onClick = {
                                                selectedCartonNumbers.clear()
                                                selectedCartonNumbers.addAll(baseExportPreview.allCartons.map { it.cartonNumber })
                                            },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(if (isBangla) "সব নির্বাচন করুন" else "Select All", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        TextButton(
                                            onClick = { selectedCartonNumbers.clear() },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(if (isBangla) "সব বাদ দিন" else "Deselect All", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StatusError)
                                        }
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        baseExportPreview.allCartons.forEach { ctn ->
                                            val checked = ctn.cartonNumber in selectedCartonNumbers
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (checked) SecondaryTeal.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface)
                                                    .clickable {
                                                        if (checked) selectedCartonNumbers.remove(ctn.cartonNumber)
                                                        else selectedCartonNumbers.add(ctn.cartonNumber)
                                                    }
                                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = checked,
                                                    onCheckedChange = { isChk ->
                                                        if (isChk) {
                                                            if (ctn.cartonNumber !in selectedCartonNumbers) selectedCartonNumbers.add(ctn.cartonNumber)
                                                        } else {
                                                            selectedCartonNumbers.remove(ctn.cartonNumber)
                                                        }
                                                    }
                                                )
                                                Text(
                                                    text = "${ctn.cartonNumber} • ${ctn.totalQty} PCS • ${ctn.grossWeightKg.setScale(1, RoundingMode.HALF_UP)} KG",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = if (checked) FontWeight.Bold else FontWeight.Normal,
                                                    color = TextPrimary
                                                )
                                            }
                                        }
                                    }

                                    // Selected Cartons Live Summary
                                    val gwSel = validatedExport.exportedGrossWeightKg.setScale(2, RoundingMode.HALF_UP)
                                    val cbmSel = validatedExport.exportedCbm.setScale(3, RoundingMode.HALF_UP)
                                    Surface(shape = RoundedCornerShape(8.dp), color = PrimaryPurple.copy(alpha = 0.1f)) {
                                        Text(
                                            text = if (isBangla)
                                                "নির্বাচিত: ${validatedExport.exportedCartonCount} / ${validatedExport.allCartons.size} CTN • ${validatedExport.exportedPackedQuantity} PCS • $gwSel KG • $cbmSel CBM"
                                            else
                                                "Selected: ${validatedExport.exportedCartonCount} of ${validatedExport.allCartons.size} CTN • ${validatedExport.exportedPackedQuantity} PCS • $gwSel KG • $cbmSel CBM",
                                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryPurple
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = StatusInfoContainer),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Visibility, contentDescription = null, tint = StatusInfo, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isBangla) "কোম্পানি অভ্যন্তরীণ নিরীক্ষা ও হিসাব" else "Company Internal Costing & Audit",
                                    fontWeight = FontWeight.Bold,
                                    color = StatusInfo,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = ERPDivider)
                Spacer(modifier = Modifier.height(10.dp))

                if (attachmentInfos.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(ERPSurfaceVariant)
                            .clickable { includeAttachments = !includeAttachments }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = includeAttachments, onCheckedChange = { includeAttachments = it })
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isBangla) "PO-এর সংযুক্ত ফাইলও শেয়ার করুন (${attachmentInfos.size}টি)" else "Also share attached files (${attachmentInfos.size})",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (exportError != null) {
                    Text(
                        text = exportError!!,
                        color = StatusError,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Action Buttons Row 1: Preview PDF, Share PDF, Print PDF
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        enabled = !isExporting && canExportSupplier,
                        onClick = {
                            if (isExporting) return@OutlinedButton
                            isExporting = true
                            exportError = null
                            coroutineScope.launch {
                                try {
                                    val pdf = buildSupplierOrInternalPdf()
                                    previewPdfTitle = when (selectedPerspective) {
                                        PoExportPerspective.SUPPLIER_COPY ->
                                            if (validatedExport.isPartialCartonExport) "${order.poNumber} (Selected ${validatedExport.exportedCartonCount} CTN)"
                                            else "${order.poNumber} Supplier PO PDF"
                                        PoExportPerspective.FREIGHT_FORWARDER_PACKING_LIST ->
                                            "${order.poNumber} Forwarder Packing List"
                                        PoExportPerspective.COMPANY_INTERNAL_COPY ->
                                            "${order.poNumber} Internal Audit PDF"
                                    }
                                    previewPdfFile = pdf
                                } catch (t: Throwable) {
                                    exportError = t.message ?: t::class.java.simpleName
                                } finally {
                                    isExporting = false
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("po_preview_pdf_btn")
                    ) {
                        Icon(Icons.Outlined.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isBangla) "প্রিভিউ" else "Preview", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        enabled = !isExporting && canExportSupplier,
                        onClick = {
                            if (isExporting) return@Button
                            isExporting = true
                            exportError = null
                            coroutineScope.launch {
                                try {
                                    val pdfFile = buildSupplierOrInternalPdf()
                                    val title = when (selectedPerspective) {
                                        PoExportPerspective.SUPPLIER_COPY -> "Share Supplier Commercial PO PDF"
                                        PoExportPerspective.FREIGHT_FORWARDER_PACKING_LIST -> "Share Freight Forwarder Packing List PDF"
                                        PoExportPerspective.COMPANY_INTERNAL_COPY -> "Share Internal Audit PO PDF"
                                    }
                                    shareFileToApp(context, pdfFile, "application/pdf", title, includeAttachments, attachmentInfos)
                                } catch (t: Throwable) {
                                    exportError = t.message ?: t::class.java.simpleName
                                } finally {
                                    isExporting = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("po_share_pdf_btn")
                    ) {
                        Icon(Icons.Outlined.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isBangla) "PDF শেয়ার" else "Share PDF", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        enabled = !isExporting && canExportSupplier,
                        onClick = {
                            if (isExporting) return@OutlinedButton
                            isExporting = true
                            exportError = null
                            coroutineScope.launch {
                                try {
                                    val pdf = buildSupplierOrInternalPdf()
                                    ControlCenterShareService.printPdf(context, pdf, "PO_${order.poNumber}")
                                } catch (t: Throwable) {
                                    exportError = t.message ?: t::class.java.simpleName
                                } finally {
                                    isExporting = false
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(0.9f)
                    ) {
                        Icon(Icons.Outlined.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isBangla) "প্রিন্ট" else "Print", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action Row 2: Share Excel & Save to Device (PDF + XLSX)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        enabled = !isExporting && canExportSupplier,
                        onClick = {
                            if (isExporting) return@OutlinedButton
                            isExporting = true
                            exportError = null
                            coroutineScope.launch {
                                try {
                                    val xlsxFile = buildSupplierOrInternalExcel()
                                    val title = when (selectedPerspective) {
                                        PoExportPerspective.SUPPLIER_COPY -> "Share Supplier Commercial PO (Excel)"
                                        PoExportPerspective.FREIGHT_FORWARDER_PACKING_LIST -> "Share Freight Forwarder Packing List (Excel)"
                                        PoExportPerspective.COMPANY_INTERNAL_COPY -> "Share Internal Audit PO (Excel)"
                                    }
                                    shareFileToApp(
                                        context,
                                        xlsxFile,
                                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                        title,
                                        includeAttachments,
                                        attachmentInfos
                                    )
                                } catch (t: Throwable) {
                                    exportError = t.message ?: t::class.java.simpleName
                                } finally {
                                    isExporting = false
                                }
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SecondaryTeal),
                        border = BorderStroke(1.3.dp, SecondaryTeal),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("po_share_excel_btn")
                    ) {
                        Icon(Icons.Outlined.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isBangla) "Excel শেয়ার" else "Share Excel", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        enabled = !isExporting && canExportSupplier,
                        onClick = {
                            if (isExporting) return@OutlinedButton
                            isExporting = true
                            exportError = null
                            coroutineScope.launch {
                                try {
                                    val pdf = buildSupplierOrInternalPdf()
                                    val xlsx = buildSupplierOrInternalExcel()
                                    val savedPdf = withContext(Dispatchers.IO) { ControlCenterShareService.saveToDownloads(context, pdf) }
                                    val savedXlsx = withContext(Dispatchers.IO) { ControlCenterShareService.saveToDownloads(context, xlsx) }
                                    val msg = if (isBangla)
                                        "সংরক্ষিত: ${savedPdf?.name ?: pdf.name} ও ${savedXlsx?.name ?: xlsx.name}"
                                    else
                                        "Saved: ${savedPdf?.name ?: pdf.name} & ${savedXlsx?.name ?: xlsx.name}"
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                } catch (t: Throwable) {
                                    exportError = t.message ?: t::class.java.simpleName
                                } finally {
                                    isExporting = false
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("po_save_both_btn")
                    ) {
                        Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isBangla) "সেভ PDF+XLSX" else "Save PDF+XLSX", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (selectedPerspective != PoExportPerspective.COMPANY_INTERNAL_COPY &&
                    order.status != "CANCELLED" &&
                    onOpenSmartWhatsApp != null
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        enabled = canExportSupplier,
                        onClick = {
                            val mod = if (selectedPerspective == PoExportPerspective.FREIGHT_FORWARDER_PACKING_LIST) {
                                CommunicationModule.PACKING
                            } else {
                                CommunicationModule.PURCHASE_ORDER
                            }
                            onOpenSmartWhatsApp(CommunicationRequest(mod, order.id))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF128C7E)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("po_smart_whatsapp_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isBangla) "স্মার্ট WhatsApp শেয়ার" else "Smart WhatsApp Share",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // CTN Labels Preview / Print / Share (Phase 5.3)
                if (selectedPerspective != PoExportPerspective.COMPANY_INTERNAL_COPY && validatedExport.exportedCartons.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        enabled = !isExporting && canExportSupplier,
                        onClick = {
                            if (isExporting) return@Button
                            isExporting = true
                            exportError = null
                            coroutineScope.launch {
                                try {
                                    val labelsPdf = withContext(Dispatchers.IO) {
                                        LabelPrintingEngine.generatePoCartonLabelsPdf(context, validatedExport)
                                    }
                                    previewPdfTitle = "${order.poNumber} CTN Labels (${validatedExport.exportedCartonCount} CTN)"
                                    previewPdfFile = labelsPdf
                                } catch (t: Throwable) {
                                    exportError = t.message ?: t::class.java.simpleName
                                } finally {
                                    isExporting = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("po_ctn_labels_btn")
                    ) {
                        Icon(Icons.Outlined.Label, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isBangla)
                                "কার্টন লেবেল ও শিপিং মার্ক প্রিভিউ/প্রিন্ট (${validatedExport.exportedCartonCount} CTN)"
                            else
                                "CTN Labels & Shipping Marks Preview / Print (${validatedExport.exportedCartonCount} CTN)",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // In-App Multi-Page PDF Preview Dialog (Phase 10)
    val fileToPreview = previewPdfFile
    if (fileToPreview != null) {
        InAppPdfPreviewDialog(
            pdfFile = fileToPreview,
            title = previewPdfTitle,
            isBangla = isBangla,
            onShare = {
                shareFileToApp(context, fileToPreview, "application/pdf", previewPdfTitle, includeAttachments, attachmentInfos)
            },
            onPrint = {
                ControlCenterShareService.printPdf(context, fileToPreview, previewPdfTitle)
            },
            onSave = {
                ControlCenterShareService.saveToDownloads(context, fileToPreview)
            },
            onDismiss = { previewPdfFile = null }
        )
    }
}

@Composable
private fun InAppPdfPreviewDialog(
    pdfFile: File,
    title: String,
    isBangla: Boolean,
    onShare: () -> Unit,
    onPrint: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val pageBitmaps = remember(pdfFile.absolutePath, pdfFile.lastModified()) {
        val rendered = mutableListOf<Bitmap>()
        runCatching {
            ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val maxPages = minOf(renderer.pageCount, 20)
                    for (i in 0 until maxPages) {
                        renderer.openPage(i).use { page ->
                            val scale = 2
                            val bmp = Bitmap.createBitmap(page.width * scale, page.height * scale, Bitmap.Config.ARGB_8888)
                            bmp.eraseColor(android.graphics.Color.WHITE)
                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            rendered += bmp
                        }
                    }
                }
            }
        }
        rendered
    }

    DisposableEffect(pageBitmaps) {
        onDispose {
            pageBitmaps.forEach { if (!it.isRecycled) it.recycle() }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.96f).fillMaxHeight(0.92f).clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${pdfFile.name} • ${pageBitmaps.size} ${if (isBangla) "পৃষ্ঠা" else "page(s)"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close Preview")
                    }
                }
                HorizontalDivider(color = ERPDivider)

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth().background(Color(0xFFE2E8F0)),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(pageBitmaps) { bmp ->
                        Card(shape = RoundedCornerShape(8.dp), elevation = CardDefaults.cardElevation(4.dp)) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "PDF Page",
                                modifier = Modifier.fillMaxWidth(),
                                contentScale = ContentScale.FillWidth
                            )
                        }
                    }
                }

                Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(onClick = onSave, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isBangla) "সেভ" else "Save", fontSize = 12.sp)
                        }
                        OutlinedButton(onClick = onPrint, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Outlined.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isBangla) "প্রিন্ট" else "Print", fontSize = 12.sp)
                        }
                        Button(
                            onClick = onShare,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isBangla) "শেয়ার" else "Share", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
