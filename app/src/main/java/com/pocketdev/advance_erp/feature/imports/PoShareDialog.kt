package com.pocketdev.advance_erp.feature.imports

import android.content.Context
import android.content.Intent
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.pocketdev.advance_erp.core.designsystem.DeepNavy
import com.pocketdev.advance_erp.core.designsystem.ERPDivider
import com.pocketdev.advance_erp.core.designsystem.PrimaryPurple
import com.pocketdev.advance_erp.core.designsystem.SecondaryTeal
import com.pocketdev.advance_erp.core.designsystem.TextMuted
import com.pocketdev.advance_erp.core.designsystem.TextPrimary
import com.pocketdev.advance_erp.core.designsystem.TextSecondary
import com.pocketdev.advance_erp.core.util.ImportExcelGenerator
import com.pocketdev.advance_erp.core.util.ImportPdfGenerator
import com.pocketdev.advance_erp.core.util.PoAttachmentInfo
import com.pocketdev.advance_erp.core.util.PoAttachmentKind
import com.pocketdev.advance_erp.core.util.PoAttachmentStorage
import com.pocketdev.advance_erp.data.entity.BusinessProfile
import com.pocketdev.advance_erp.data.entity.Product
import com.pocketdev.advance_erp.data.entity.PurchaseOrder
import com.pocketdev.advance_erp.data.entity.PurchaseOrderItem
import com.pocketdev.advance_erp.data.entity.Supplier
import java.io.File

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
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedPerspective by remember { mutableStateOf(PoExportPerspective.SUPPLIER_COPY) }
    var isExporting by remember { mutableStateOf(false) }
    var exportError by remember { mutableStateOf<String?>(null) }
    val isLocalOrder = order.currency == "BDT"
    val attachmentInfos = remember(order.attachments) {
        PoAttachmentStorage.parseAttachments(order.attachments)
            .map { PoAttachmentStorage.inspectAttachment(it) }
            .filter { it.localFile != null || it.kind == PoAttachmentKind.LINK }
    }
    var includeAttachments by remember(order.id) { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isBangla) "পারচেজ অর্ডার শেয়ার ও এক্সপোর্ট" else "Share & Export Purchase Order",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = DeepNavy
                        )
                        Text(
                            text = "${order.poNumber} • ${supplier?.companyName ?: "Supplier"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Perspective Selector Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Supplier Copy Tab
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedPerspective == PoExportPerspective.SUPPLIER_COPY)
                            PrimaryPurple.copy(alpha = 0.12f) else Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            if (selectedPerspective == PoExportPerspective.SUPPLIER_COPY) PrimaryPurple else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedPerspective = PoExportPerspective.SUPPLIER_COPY }
                            .padding(vertical = 8.dp, horizontal = 10.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = (if (isLocalOrder) "🇧🇩 " else "🇨🇳 ") + if (isBangla) "সরবরাহকারী কপি" else "Supplier Copy",
                                fontWeight = FontWeight.Bold,
                                color = if (selectedPerspective == PoExportPerspective.SUPPLIER_COPY) PrimaryPurple else TextPrimary,
                                fontSize = 12.sp
                            )
                            Text(
                                text = if (isLocalOrder)
                                    (if (isBangla) "(নো অভ্যন্তরীণ মার্জিন)" else "(No Internal Margin)")
                                else
                                    (if (isBangla) "(গোপনীয় - নো BDT)" else "(Factory Confidential)"),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Company Internal Copy Tab
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedPerspective == PoExportPerspective.COMPANY_INTERNAL_COPY)
                            SecondaryTeal.copy(alpha = 0.12f) else Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            if (selectedPerspective == PoExportPerspective.COMPANY_INTERNAL_COPY) SecondaryTeal else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedPerspective = PoExportPerspective.COMPANY_INTERNAL_COPY }
                            .padding(vertical = 8.dp, horizontal = 10.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "🏢 " + if (isBangla) "কোম্পানি অডিট কপি" else "Internal Audit",
                                fontWeight = FontWeight.Bold,
                                color = if (selectedPerspective == PoExportPerspective.COMPANY_INTERNAL_COPY) SecondaryTeal else TextPrimary,
                                fontSize = 12.sp
                            )
                            Text(
                                text = if (isBangla) "(পূর্ণাঙ্গ কস্টিং ও অডিট)" else "(Full Costing & Audit)",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Detail Description Card based on selected perspective
                if (selectedPerspective == PoExportPerspective.SUPPLIER_COPY) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF166534), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isLocalOrder)
                                        (if (isBangla) "দেশীয় সরবরাহকারী / ভেন্ডরের জন্য তৈরি" else "Tailored for Local Supplier")
                                    else
                                        (if (isBangla) "চীনা কারখানা / বৈদেশিক সরবরাহকারীর জন্য তৈরি" else "Tailored for Overseas Factory / Supplier"),
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF166534),
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isLocalOrder) {
                                    if (isBangla)
                                        "• বাণিজ্যিক গোপনীয়তা: কোনো অভ্যন্তরীণ পরিবহন, আনলোডিং খরচ বা কোম্পানির মুনাফা মার্জিন থাকবে না।\n• পণ্য, ভ্যারিয়েন্ট, পিস ও নির্ধারিত ক্রয়মূল্য (৳ BDT)।\n• ডেলিভারি ঠিকানা ও অর্ডার নোট অন্তর্ভুক্ত।"
                                    else
                                        "• Commercial Privacy: No internal transport, unloading or company profit margins.\n• Product specs, variants, pieces & contract price (৳ BDT).\n• Local delivery point & order instructions included."
                                } else {
                                    if (isBangla)
                                        "• কঠোর তথ্য গোপনীয়তা: কোনো বাংলাদেশি টাকা (BDT), এক্সচেঞ্জ রেট বা স্থানীয় মার্জিন থাকবে না।\n• পণ্য মডেল, ভ্যারিয়েন্ট, কার্টন সংখ্যা, পিস ও বৈদেশিক মুদ্রা (${order.currency})।\n• পেমেন্ট শর্ত (Advance/Balance) ও চীনের ফরোয়ার্ডার গুদাম ডেলিভারি ঠিকানা অন্তর্ভুক্ত।"
                                    else
                                        "• Strict Privacy: Absolutely NO BDT amounts, FX rates, or local profit margins.\n• Product specs, variants, carton packing, pieces & currency (${order.currency}).\n• Payment terms & China forwarder warehouse handover instructions."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF15803D),
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Visibility, contentDescription = null, tint = Color(0xFF1E40AF), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isBangla) "কোম্পানি অভ্যন্তরীণ নিরীক্ষা ও হিসাব" else "Company Internal Costing & Audit",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E40AF),
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isLocalOrder) {
                                    if (isBangla)
                                        "• পণ্যের মোট ক্রয়মূল্য (BDT) এবং স্থানীয় পরিবহন ও লেবার খরচ।\n• ইউনিট প্রতি চূড়ান্ত ল্যান্ডেড কস্ট হিসাব।\n• গুদাম ভাড়া পরিচালন ব্যয় (OPEX) হিসেবে মাসিক লাভে সমন্বয়ের স্পষ্ট অডিট নোট।"
                                    else
                                        "• Complete BDT purchase total with local transport and labour costs.\n• Final landed cost per unit calculation.\n• Clear note on Warehouse Rent being classified as OPEX from Net Profit."
                                } else {
                                    if (isBangla)
                                        "• বৈদেশিক মূল্যের সাথে নির্ধারিত বিনিময় হারে BDT সমমূল্য।\n• আন্তর্জাতিক ফ্রেইট, হ্যান্ডলিং খরচ ও স্যাম্পল খরচ ক্যাপিটালাইজড ল্যান্ডেড কস্ট।\n• গুদাম ভাড়া পরিচালন ব্যয় (OPEX) হিসেবে মাসিক লাভে সমন্বয়ের স্পষ্ট নোট।"
                                    else
                                        "• Complete BDT breakdown with applied foreign exchange rates.\n• Capitalized Landed Cost including freight, handling, and sample costs.\n• Clear note on Warehouse Rent being classified as OPEX from Net Profit."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF1D4ED8),
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                HorizontalDivider(color = ERPDivider)
                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons
                if (attachmentInfos.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF8FAFC))
                            .clickable { includeAttachments = !includeAttachments }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = includeAttachments,
                            onCheckedChange = { includeAttachments = it }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isBangla)
                                "PO-এর সংযুক্ত ফাইল/ছবিও একসাথে শেয়ার করুন (${attachmentInfos.size}টি)"
                            else
                                "Also share attached files/photos (${attachmentInfos.size})",
                            style = MaterialTheme.typography.bodySmall,
                            color = DeepNavy,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Text(
                    text = if (isBangla) "এক্সপোর্ট ও শেয়ারের মাধ্যম নির্বাচন করুন:" else "Select Export & Sharing Format:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = DeepNavy
                )
                if (exportError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = exportError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Export PDF Button
                    Button(
                        enabled = !isExporting,
                        onClick = {
                            if (isExporting) return@Button
                            isExporting = true
                            exportError = null
                            coroutineScope.launch {
                                try {
                                    val pdfFile = withContext(Dispatchers.IO) {
                                        if (selectedPerspective == PoExportPerspective.SUPPLIER_COPY) {
                                            ImportPdfGenerator.generateSupplierCommercialOrderPdf(
                                                context, order, items, productsMap, supplier, businessProfile
                                            )
                                        } else {
                                            ImportPdfGenerator.generateCompanyInternalOrderPdf(
                                                context, order, items, productsMap, supplier, businessProfile, isBangla
                                            )
                                        }
                                    }
                                    val title = if (selectedPerspective == PoExportPerspective.SUPPLIER_COPY)
                                        "Share Supplier Commercial PO PDF"
                                    else
                                        "Share Internal Audit PO PDF"
                                    shareFileToApp(
                                        context = context,
                                        file = pdfFile,
                                        mimeType = "application/pdf",
                                        chooserTitle = title,
                                        includeAttachments = includeAttachments,
                                        attachments = attachmentInfos
                                    )
                                } catch (t: Throwable) {
                                    val msg = t.message ?: t::class.java.simpleName
                                    exportError = if (isBangla) "PDF তৈরি করতে সমস্যা হয়েছে: $msg" else "PDF export failed: $msg"
                                    Toast.makeText(context, exportError, Toast.LENGTH_LONG).show()
                                } finally {
                                    isExporting = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PDF " + if (isBangla) "শেয়ার" else "Export", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Export Excel Button
                    OutlinedButton(
                        enabled = !isExporting,
                        onClick = {
                            if (isExporting) return@OutlinedButton
                            isExporting = true
                            exportError = null
                            coroutineScope.launch {
                                try {
                                    val xlsxFile = withContext(Dispatchers.IO) {
                                        if (selectedPerspective == PoExportPerspective.SUPPLIER_COPY) {
                                            ImportExcelGenerator.generateSupplierOrderExcel(
                                                context, order, items, productsMap, supplier, businessProfile
                                            )
                                        } else {
                                            ImportExcelGenerator.generateCompanyInternalOrderExcel(
                                                context, order, items, productsMap, supplier, businessProfile
                                            )
                                        }
                                    }
                                    val title = if (selectedPerspective == PoExportPerspective.SUPPLIER_COPY)
                                        "Share Supplier Commercial PO (Excel)"
                                    else
                                        "Share Internal Audit PO (Excel)"
                                    shareFileToApp(
                                        context = context,
                                        file = xlsxFile,
                                        mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                        chooserTitle = title,
                                        includeAttachments = includeAttachments,
                                        attachments = attachmentInfos
                                    )
                                } catch (t: Throwable) {
                                    val msg = t.message ?: t::class.java.simpleName
                                    exportError = if (isBangla) "Excel তৈরি করতে সমস্যা হয়েছে: $msg" else "Excel export failed: $msg"
                                    Toast.makeText(context, exportError, Toast.LENGTH_LONG).show()
                                } finally {
                                    isExporting = false
                                }
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SecondaryTeal),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, SecondaryTeal),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Excel " + if (isBangla) "শেয়ার" else "Export", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
