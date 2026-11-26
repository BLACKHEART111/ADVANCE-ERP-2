package com.pocketdev.advance_erp.feature.communication

import android.app.DatePickerDialog
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.pocketdev.advance_erp.core.communication.*
import com.pocketdev.advance_erp.core.pdf.PdfViewerDialog
import com.pocketdev.advance_erp.core.util.ControlCenterShareService
import com.pocketdev.advance_erp.data.entity.CommunicationSettings
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CommunicationLauncher(module: CommunicationModule? = null, documentId: String? = null, isBangla: Boolean = true, settings: Boolean = false) {
    var open by rememberSaveable { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, modifier = Modifier.padding(horizontal = 16.dp)) {
        Icon(Icons.Outlined.Forum, null); Spacer(Modifier.width(8.dp))
        Text(if(isBangla) "স্মার্ট যোগাযোগ / WhatsApp" else "Smart Communication / WhatsApp")
    }
    if(open) CommunicationDialog(initialRequest = if(module != null && documentId != null) CommunicationRequest(module, documentId) else null,
        initialModule = module ?: CommunicationModule.INVOICE, settingsMode = settings, onDismiss = { open = false })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CommunicationDialog(initialRequest: CommunicationRequest? = null, initialModule: CommunicationModule = CommunicationModule.INVOICE,
    settingsMode: Boolean = false, success: Boolean = false, onDismiss: () -> Unit,
    viewModel: CommunicationViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var page by rememberSaveable { mutableStateOf(if(settingsMode) "settings" else if(initialRequest != null) "preview" else "documents") }
    var showPdf by remember { mutableStateOf(false) }
    var from by rememberSaveable { mutableStateOf<Long?>(null) }
    var to by rememberSaveable { mutableStateOf<Long?>(null) }
    var search by rememberSaveable { mutableStateOf("") }
    var historyMessage by remember { mutableStateOf<String?>(null) }
    var pendingLanguage by remember { mutableStateOf<String?>(null) }
    val bn = (state.prepared?.language ?: state.settings.defaultLanguage) == "bn"
    fun label(en: String, bangla: String) = if(bn) bangla else en
    LaunchedEffect(initialRequest, settingsMode) {
        viewModel.reset()
        if(initialRequest != null) viewModel.prepare(initialRequest) else viewModel.selectModule(initialModule)
    }
    val contactPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.data?.let { uri ->
            try {
                context.contentResolver.query(uri, arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER), null, null, null)?.use { phones ->
                    if(phones.moveToFirst()) viewModel.phone(phones.getString(0)) else viewModel.error("Contact has no phone / নম্বর নেই")
                }
            } catch(e: Exception) { viewModel.error(e.message ?: "Contact unavailable") }
        }
    }
    fun pickDate(start: Boolean) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(context, { _, y, m, d ->
            calendar.set(y,m,d, if(start) 0 else 23, if(start) 0 else 59, if(start) 0 else 59)
            calendar.set(Calendar.MILLISECOND, if(start) 0 else 999)
            if(start) from = calendar.timeInMillis else to = calendar.timeInMillis
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }
    fun share(pdfOnly: Boolean = false, other: Boolean = false) {
        val p = state.prepared ?: return
        try {
            require(!state.message.contains(Regex("\\{[^{}]*}"))) { "Remove unresolved placeholders / অসম্পূর্ণ placeholder সরান" }
            if(pdfOnly) requireNotNull(p.attachment) { "Prepare PDF first / PDF তৈরি করুন" }
            val app = if(other) null else state.application.takeIf { it != "ASK" }
            if(!other && app == null) { viewModel.error(label("Choose WhatsApp Messenger or Business first", "আগে WhatsApp Messenger অথবা Business নির্বাচন করুন")); return }
            val text = if(pdfOnly) "" else state.message
            val event = WhatsAppShareManager.share(context, p.attachment, text, app)
            viewModel.record(event, app ?: "SYSTEM_CHOOSER", p.attachment != null, text)
        } catch(e: Exception) { viewModel.error(e.message ?: "Share failed") }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, label("Close", "বন্ধ")) }
                    Text(if(success && page == "preview") label("Invoice created successfully", "ইনভয়েস সফলভাবে তৈরি হয়েছে") else label("Smart Communication", "স্মার্ট যোগাযোগ"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                }
                FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("documents" to label("Documents", "ডকুমেন্ট"), "history" to label("History", "ইতিহাস"), "settings" to label("Settings", "সেটিংস"), "templates" to label("Templates", "টেমপ্লেট")).forEach { (key, title) ->
                        FilterChip(selected = page == key, onClick = { page = key; if(key == "templates") viewModel.loadTemplate(state.module, state.editorStatus, state.editorLanguage) }, label = { Text(title) })
                    }
                    if(state.prepared != null) FilterChip(selected = page == "preview", onClick = { page = "preview" }, label = { Text(label("Preview", "প্রিভিউ")) })
                }
                if(state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    state.notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                    when(page) {
                        "documents" -> {
                            ModulePicker(state.module, bn) { viewModel.selectModule(it); search = "" }
                            if(state.module in setOf(CommunicationModule.STATEMENT, CommunicationModule.LEDGER)) {
                                Text(label("Optional statement date range", "বিবরণীর সময়কাল (ঐচ্ছিক)"))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { pickDate(true) }) { Text(from?.let { SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(it)) } ?: label("From", "শুরু")) }
                                    OutlinedButton(onClick = { pickDate(false) }) { Text(to?.let { SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(it)) } ?: label("To", "শেষ")) }
                                    TextButton(onClick = { from = null; to = null }) { Text(label("All time", "সব সময়")) }
                                }
                            }
                            OutlinedTextField(search, { search = it }, label = { Text(label("Find document / contact", "ডকুমেন্ট / গ্রাহক খুঁজুন")) }, modifier = Modifier.fillMaxWidth())
                            if(!state.busy && state.choices.isEmpty()) Text(label("No available documents. Older exchanges without saved details cannot be reconstructed.", "কোনো ডকুমেন্ট পাওয়া যায়নি। সংরক্ষিত বিবরণ ছাড়া পুরোনো exchange পুনর্গঠন করা যাবে না।"))
                            state.choices.filter { it.label.contains(search, true) }.forEach { choice ->
                                OutlinedCard(onClick = { viewModel.prepare(choice.request.copy(from = from, to = to)); page = "preview" }, modifier = Modifier.fillMaxWidth()) {
                                    Text(choice.label, Modifier.padding(16.dp))
                                }
                            }
                        }
                        "preview" -> state.prepared?.let { p ->
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(p.document.number, style = MaterialTheme.typography.titleLarge)
                                    Text(p.document.partyName, style = MaterialTheme.typography.titleMedium)
                                    Text(p.document.request.module.title(p.language))
                                    p.document.fields.filterKeys { it in setOf("invoice_date", "previous_due", "invoice_amount", "received_payment", "invoice_due", "total_outstanding", "historical_as_of", "historical_payment", "historical_due", "historical_balance") }.forEach { (key, value) ->
                                        val pair = DynamicTemplateEngine.labels.getValue(key)
                                        Text("${if(bn) pair.second else pair.first}: $value", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                            p.document.warnings.forEach { Text(it, color = MaterialTheme.colorScheme.tertiary) }
                            OutlinedTextField(state.phone, viewModel::phone, label = { Text(label("Recipient WhatsApp number", "প্রাপকের WhatsApp নম্বর")) }, supportingText = { Text(CommunicationPhone.normalize(state.phone) ?: label("Invalid number: use +country code", "সঠিক নম্বর দিন: +দেশের কোড")) }, modifier = Modifier.fillMaxWidth())
                            TextButton(onClick = { contactPicker.launch(android.content.Intent(android.content.Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)) }) { Text(label("Choose phone contact", "ফোনের কন্টাক্ট নির্বাচন")) }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("bn" to "বাংলা", "en" to "English").forEach { (code,title) -> FilterChip(selected = p.language == code, onClick = { pendingLanguage = code }, label = { Text(title) }) }
                                TextButton(onClick = { viewModel.savePartyLanguage(p.language) }) { Text(label("Save customer language", "গ্রাহকের ভাষা সংরক্ষণ")) }
                            }
                            Text(label("Template (regenerates message)", "টেমপ্লেট (বার্তা নতুন করে তৈরি হবে)"))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                val statuses = listOf(CommunicationStatus.GENERAL, p.document.status) + listOfNotNull(
                                    CommunicationStatus.PREVIOUS_DUE.takeIf { (p.document.previousMinor ?: 0) > 0 }, CommunicationStatus.CREDIT.takeIf { (p.document.balanceMinor ?: 0) < 0 })
                                statuses.distinct().forEach { status -> AssistChip(onClick = { viewModel.prepare(p.document.request, p.language, status) }, label = { Text(status.name) }) }
                            }
                            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                                OutlinedTextField(state.message, viewModel::message, modifier = Modifier.fillMaxWidth().padding(12.dp), label = { Text(label("Editable message", "বার্তা সম্পাদনা")) }, minLines = 8)
                            }
                            if(p.document.request.module in setOf(CommunicationModule.PURCHASE_ORDER, CommunicationModule.PACKING)) {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { viewModel.generateXlsx() }, enabled = !state.busy) { Text("XLSX") }
                                    OutlinedButton(onClick = { viewModel.prepare(p.document.request, p.language, forcePdf = true) }, enabled = !state.busy) { Text("PDF") }
                                }
                            }
                            p.attachment?.let { file ->
                                OutlinedCard(onClick = { if(file.extension == "pdf") showPdf = true else viewModel.notice(label("XLSX attachment ready", "XLSX সংযুক্তি প্রস্তুত")) }, modifier = Modifier.fillMaxWidth()) {
                                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.PictureAsPdf, null); Spacer(Modifier.width(12.dp)); Text(if(file.extension == "pdf") label("PDF attachment · Preview", "PDF সংযুক্তি · প্রিভিউ") else "XLSX · ${file.length() / 1024} KB") }
                                }
                            }
                            p.attachmentError?.let { Text(label("PDF failed: ", "PDF ব্যর্থ: ") + it, color = MaterialTheme.colorScheme.error) }
                            if(p.attachment == null) OutlinedButton(onClick = { viewModel.prepare(p.document.request, p.language, forcePdf = true) }, enabled = !state.busy) { Text(label("Generate / retry PDF", "PDF তৈরি / পুনরায় চেষ্টা")) }
                            val apps = WhatsAppShareManager.installed(context)
                            if(apps.isEmpty()) Text(label("WhatsApp is not installed. Copy or use Other Share.", "WhatsApp ইনস্টল নেই। Copy অথবা Other Share ব্যবহার করুন।"))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                apps.forEach { app -> FilterChip(selected = state.application == app, onClick = { viewModel.application(app) }, label = { Text(if(app == WhatsAppShareManager.BUSINESS) "WhatsApp Business" else "WhatsApp Messenger") }) }
                            }
                            Button(onClick = { share() }, enabled = !state.busy && apps.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Share, null); Text(label(" Share via WhatsApp", " WhatsApp-এ শেয়ার")) }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { WhatsAppShareManager.copy(context, state.message); viewModel.notice(label("Message copied", "বার্তা কপি হয়েছে")) }) { Text(label("Copy Message", "বার্তা কপি")) }
                                OutlinedButton(onClick = { share(other = true) }, enabled = !state.busy) { Text(label("Other Share", "অন্যান্য শেয়ার")) }
                                OutlinedButton(onClick = { p.attachment?.let { ControlCenterShareService.printPdf(context, it, p.document.number) } }, enabled = p.attachment?.extension == "pdf") { Text(label("Print", "প্রিন্ট")) }
                                TextButton(onClick = { viewModel.prepare(p.document.request, p.language) }) { Text(label("Reset to default", "ডিফল্টে ফিরুন")) }
                            }
                            HorizontalDivider()
                            Text(label("If WhatsApp omits the caption", "WhatsApp-এ caption না গেলে"), style = MaterialTheme.typography.titleMedium)
                            Text(label("Copy Message → Share PDF → Open Customer Chat → Paste and Send. Select the recipient in WhatsApp; the app never sends automatically.", "বার্তা কপি → PDF শেয়ার → গ্রাহকের চ্যাট খুলুন → Paste করে Send। WhatsApp-এ প্রাপক নির্বাচন করুন; অ্যাপ নিজে বার্তা পাঠায় না।"))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { share(pdfOnly = true) }, enabled = p.attachment?.extension == "pdf" && !state.busy) { Text(label("Share PDF", "PDF শেয়ার")) }
                                OutlinedButton(onClick = {
                                    try { val event = WhatsAppShareManager.openChat(context, state.phone, state.application); viewModel.record(event, state.application, false) }
                                    catch(e: Exception) { viewModel.error(e.message ?: "Chat unavailable") }
                                }, enabled = CommunicationPhone.normalize(state.phone) != null && state.application in apps) { Text(label("Open Customer Chat", "গ্রাহকের চ্যাট খুলুন")) }
                            }
                        } ?: Text(label("Select a document to prepare a message.", "বার্তা তৈরির জন্য ডকুমেন্ট নির্বাচন করুন।"))
                        "settings" -> CommunicationSettingsEditor(state.settings, viewModel::saveSettings, bn)
                        "templates" -> {
                            ModulePicker(state.module, bn) { viewModel.loadTemplate(it, state.editorStatus, state.editorLanguage) }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                CommunicationStatus.values().forEach { status -> FilterChip(state.editorStatus == status, { viewModel.loadTemplate(state.module, status, state.editorLanguage) }, label = { Text(status.name) }) }
                                listOf("bn", "en").forEach { code -> FilterChip(state.editorLanguage == code, { viewModel.loadTemplate(state.module, state.editorStatus, code) }, label = { Text(code) }) }
                            }
                            Text(label("Available placeholders — tap to insert", "ব্যবহারযোগ্য placeholder — যুক্ত করতে চাপুন"))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                DynamicTemplateEngine.allowed(state.module).forEach { key -> AssistChip(onClick = { viewModel.editTemplate(state.editor + "\n{$key}") }, label = { Text("{$key}") }) }
                            }
                            OutlinedTextField(state.editor, viewModel::editTemplate, minLines = 10, modifier = Modifier.fillMaxWidth(), label = { Text(label("Template", "টেমপ্লেট")) })
                            FlowRow { Button(onClick = { viewModel.saveTemplate() }) { Text(label("Save custom template", "টেমপ্লেট সংরক্ষণ")) }; TextButton(onClick = { viewModel.resetTemplate() }) { Text(label("Reset to default", "ডিফল্টে ফিরুন")) } }
                            Text(label("Live sample preview (fictional)", "নমুনা প্রিভিউ (কাল্পনিক)"), style = MaterialTheme.typography.titleMedium)
                            val sample = DynamicTemplateEngine.allowed(state.module).associateWith { key -> when(key) { "customer_name", "supplier_name" -> "Sample / নমুনা"; "company_name" -> "ADVANCE ERP"; "company_phone", "customer_phone" -> "+8801700000000"; else -> "[${DynamicTemplateEngine.labels[key]?.first ?: key}]" } }
                            Text(DynamicTemplateEngine.render(state.editor, sample))
                        }
                        "history" -> {
                            Text(label("Local handoff events only; not Sent, Delivered or Read.", "শুধু স্থানীয় share/chat খোলার ঘটনা; Sent, Delivered বা Read নয়।"))
                            state.history.forEach { h ->
                                OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("${h.documentNumber} · ${h.partyName}", style = MaterialTheme.typography.titleMedium)
                                    Text("${h.event} · ${h.application} · ${h.language}\n${Date(h.createdAt)}\n${h.phone}")
                                    Text(h.attachmentReference ?: label("No attachment", "সংযুক্তি নেই"))
                                    TextButton(onClick = { historyMessage = h.message }) { Text(label("Original message snapshot", "মূল বার্তার snapshot")) }
                                    OutlinedButton(onClick = { viewModel.prepare(CommunicationRequest(CommunicationModule.valueOf(h.module), h.documentId, h.rangeFrom, h.rangeTo), h.language); page = "preview"; viewModel.notice(label("Resend: fresh balances and regenerated attachment; original message remains in history.", "পুনঃশেয়ার: বর্তমান হিসাব ও নতুন সংযুক্তি; মূল বার্তা ইতিহাসে থাকবে।")) }) { Text(label("Prepare resend", "পুনঃশেয়ার প্রস্তুত")) }
                                } }
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
        if(showPdf) state.prepared?.attachment?.let { file -> CommunicationPdfPreview(file = file, bn = bn, onDismiss = { showPdf = false }) }
        pendingLanguage?.let { code -> AlertDialog(onDismissRequest = { pendingLanguage = null }, title = { Text(label("Regenerate message?", "বার্তা নতুন করে তৈরি করবেন?")) }, text = { Text(label("Current edits will be replaced.", "বর্তমান সম্পাদনা প্রতিস্থাপিত হবে।")) }, confirmButton = { TextButton(onClick = { state.prepared?.let { viewModel.prepare(it.document.request, code) }; pendingLanguage = null }) { Text(label("Regenerate", "তৈরি করুন")) } }, dismissButton = { TextButton(onClick = { pendingLanguage = null }) { Text(label("Cancel", "বাতিল")) } }) }
        historyMessage?.let { text -> AlertDialog(onDismissRequest = { historyMessage = null }, title = { Text(label("Original message", "মূল বার্তা")) }, text = { Text(text, Modifier.verticalScroll(rememberScrollState())) }, confirmButton = { TextButton(onClick = { historyMessage = null }) { Text("OK") } }) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModulePicker(selected: CommunicationModule, bn: Boolean, onSelect: (CommunicationModule) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box { OutlinedButton(onClick = { expanded = true }) { Text(selected.title(if(bn) "bn" else "en")); Icon(Icons.Outlined.ExpandMore, null) }
        DropdownMenu(expanded, { expanded = false }) { CommunicationModule.values().forEach { module -> DropdownMenuItem(text = { Text(module.title(if(bn) "bn" else "en")) }, onClick = { expanded = false; onSelect(module) }) } }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CommunicationSettingsEditor(settings: CommunicationSettings, save: (CommunicationSettings) -> Unit, bn: Boolean) {
    val switches = listOf(
        Triple("Auto generate message", "স্বয়ংক্রিয় বার্তা", settings.autoGenerate), Triple("Include PDF", "PDF সংযুক্তি", settings.includePdf),
        Triple("Previous due", "পূর্বের বকেয়া", settings.includePreviousDue), Triple("Invoice amount", "ইনভয়েসের পরিমাণ", settings.includeInvoiceAmount),
        Triple("Payment details", "পেমেন্টের বিবরণ", settings.includePaymentDetails), Triple("Invoice due", "ইনভয়েস বকেয়া", settings.includeInvoiceDue),
        Triple("Total outstanding", "মোট বকেয়া", settings.includeOutstanding), Triple("Method and reference", "মাধ্যম ও রেফারেন্স", settings.includeMethodReference),
        Triple("Payment instructions", "পেমেন্ট নির্দেশনা", settings.includeInstructions), Triple("Due reminder", "বকেয়া স্মরণিকা", settings.includeReminder),
        Triple("Company signature", "প্রতিষ্ঠানের স্বাক্ষর", settings.includeSignature), Triple("Preview before sharing", "শেয়ারের আগে প্রিভিউ", settings.previewBeforeSharing))
    switches.forEachIndexed { i, (en, bangla, value) -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(if(bn) bangla else en, Modifier.weight(1f)); Switch(value, { checked -> save(when(i) {
            0 -> settings.copy(autoGenerate = checked); 1 -> settings.copy(includePdf = checked); 2 -> settings.copy(includePreviousDue = checked)
            3 -> settings.copy(includeInvoiceAmount = checked); 4 -> settings.copy(includePaymentDetails = checked); 5 -> settings.copy(includeInvoiceDue = checked)
            6 -> settings.copy(includeOutstanding = checked); 7 -> settings.copy(includeMethodReference = checked); 8 -> settings.copy(includeInstructions = checked)
            9 -> settings.copy(includeReminder = checked); 10 -> settings.copy(includeSignature = checked); else -> settings.copy(previewBeforeSharing = checked)
        }) })
    } }
    Text(if(bn) "ডিফল্ট ভাষা" else "Default language")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("bn" to "বাংলা", "en" to "English").forEach { (code,title) -> FilterChip(settings.defaultLanguage == code, { save(settings.copy(defaultLanguage = code)) }, label = { Text(title) }) } }
    Text(if(bn) "পছন্দের অ্যাপ" else "Preferred application")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("ASK" to "Choose / নির্বাচন", WhatsAppShareManager.MESSENGER to "Messenger", WhatsAppShareManager.BUSINESS to "Business").forEach { (code,title) -> FilterChip(settings.preferredApp == code, { save(settings.copy(preferredApp = code)) }, label = { Text(title) }) } }
    var instructions by remember(settings.paymentInstructions) { mutableStateOf(settings.paymentInstructions) }
    var bank by remember(settings.bankDetails) { mutableStateOf(settings.bankDetails) }
    OutlinedTextField(instructions, { instructions = it }, label = { Text(if(bn) "পেমেন্ট নির্দেশনা" else "Payment instructions") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(bank, { bank = it }, label = { Text(if(bn) "ব্যাংকের তথ্য" else "Bank details") }, modifier = Modifier.fillMaxWidth())
    Button(onClick = { save(settings.copy(paymentInstructions = instructions, bankDetails = bank)) }) { Text(if(bn) "নির্দেশনা সংরক্ষণ" else "Save instructions") }
}
