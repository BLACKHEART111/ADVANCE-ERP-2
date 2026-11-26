package com.pocketdev.advance_erp.feature.communication

import android.app.DatePickerDialog
import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.pocketdev.advance_erp.core.communication.*
import com.pocketdev.advance_erp.core.util.ControlCenterShareService
import com.pocketdev.advance_erp.data.entity.CommunicationSettings
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CommunicationLauncher(
    module: CommunicationModule? = null,
    documentId: String? = null,
    isBangla: Boolean = true,
    settings: Boolean = false
) {
    var open by rememberSaveable { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, modifier = Modifier.padding(horizontal = 16.dp)) {
        Icon(Icons.Outlined.Forum, null)
        Spacer(Modifier.width(8.dp))
        Text(if (isBangla) "স্মার্ট যোগাযোগ / WhatsApp" else "Smart Communication / WhatsApp")
    }
    if (open) {
        CommunicationDialog(
            initialRequest = if (module != null && documentId != null) CommunicationRequest(module, documentId) else null,
            initialModule = module ?: CommunicationModule.INVOICE,
            settingsMode = settings,
            dialogKey = "launcher_${module?.name ?: "all"}_${documentId ?: "none"}_$settings",
            onDismiss = { open = false }
        )
    }
}

private sealed interface PendingPreviewOverwrite {
    data class SwitchLanguage(val language: String) : PendingPreviewOverwrite
    data class SwitchStatusTemplate(val status: CommunicationStatus) : PendingPreviewOverwrite
    data object ReloadCurrentTemplate : PendingPreviewOverwrite
    data object ResetSystemDefault : PendingPreviewOverwrite
}

private sealed interface PendingEditorSwitch {
    data class Load(
        val module: CommunicationModule,
        val status: CommunicationStatus,
        val language: String
    ) : PendingEditorSwitch
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CommunicationDialog(
    initialRequest: CommunicationRequest? = null,
    initialModule: CommunicationModule = CommunicationModule.INVOICE,
    settingsMode: Boolean = false,
    success: Boolean = false,
    dialogKey: String = "comm_${initialRequest?.module?.name ?: initialModule.name}_${initialRequest?.documentId ?: "hub"}_$settingsMode",
    onDismiss: () -> Unit,
    viewModel: CommunicationViewModel = hiltViewModel(key = dialogKey)
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var page by rememberSaveable(dialogKey) {
        mutableStateOf(if (settingsMode) "settings" else if (initialRequest != null) "preview" else "documents")
    }
    var forceExpandedPreview by rememberSaveable(dialogKey) { mutableStateOf(false) }
    var showPdf by remember { mutableStateOf(false) }
    var from by rememberSaveable(dialogKey) { mutableStateOf<Long?>(null) }
    var to by rememberSaveable(dialogKey) { mutableStateOf<Long?>(null) }
    var search by rememberSaveable(dialogKey) { mutableStateOf("") }
    var historySearch by rememberSaveable(dialogKey) { mutableStateOf("") }
    var historyModuleFilter by rememberSaveable(dialogKey) { mutableStateOf<CommunicationModule?>(null) }
    var historyMessage by remember { mutableStateOf<String?>(null) }
    var pendingOverwrite by remember { mutableStateOf<PendingPreviewOverwrite?>(null) }
    var pendingEditorSwitch by remember { mutableStateOf<PendingEditorSwitch?>(null) }
    var deliveryDraft by remember(state.prepared?.document?.request?.documentId) {
        mutableStateOf(state.prepared?.document?.scheduledDeliveryInfo.orEmpty())
    }

    val bn = (state.prepared?.language ?: state.settings.defaultLanguage) == "bn"
    fun label(en: String, bangla: String) = if (bn) bangla else en

    LaunchedEffect(initialRequest, settingsMode) {
        viewModel.reset()
        if (initialRequest != null) {
            viewModel.prepare(initialRequest)
        } else {
            val defaultMod = if (initialModule in state.visibleModules) initialModule else state.visibleModules.firstOrNull() ?: initialModule
            viewModel.selectModule(defaultMod)
        }
    }

    val contactPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.data?.let { uri ->
            try {
                context.contentResolver.query(
                    uri,
                    arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                    null,
                    null,
                    null
                )?.use { phones ->
                    if (phones.moveToFirst()) {
                        viewModel.phone(phones.getString(0), manualOverride = true)
                    } else {
                        viewModel.error(label("Contact has no phone number", "কন্টাক্টে কোনো ফোন নম্বর নেই"))
                    }
                }
            } catch (e: Exception) {
                viewModel.error(e.message ?: label("Contact unavailable", "কন্টাক্ট পড়া যায়নি"))
            }
        }
    }

    fun pickDate(start: Boolean) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, y, m, d ->
                calendar.set(y, m, d, if (start) 0 else 23, if (start) 0 else 59, if (start) 0 else 59)
                calendar.set(Calendar.MILLISECOND, if (start) 0 else 999)
                if (start) from = calendar.timeInMillis else to = calendar.timeInMillis
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun pickQuotationValidityDate(currentMillis: Long?) {
        val calendar = Calendar.getInstance().apply {
            if (currentMillis != null) timeInMillis = currentMillis else add(Calendar.DAY_OF_MONTH, 7)
        }
        DatePickerDialog(
            context,
            { _, y, m, d ->
                calendar.set(y, m, d, 23, 59, 59)
                viewModel.updateQuotationValidity(calendar.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun executeOverwrite(action: PendingPreviewOverwrite) {
        val p = state.prepared ?: return
        when (action) {
            is PendingPreviewOverwrite.SwitchLanguage -> viewModel.prepare(p.document.request, language = action.language)
            is PendingPreviewOverwrite.SwitchStatusTemplate -> viewModel.prepare(p.document.request, language = p.language, status = action.status)
            PendingPreviewOverwrite.ReloadCurrentTemplate -> viewModel.prepare(p.document.request, language = p.language, useSystemDefaultTemplate = false)
            PendingPreviewOverwrite.ResetSystemDefault -> viewModel.prepare(p.document.request, language = p.language, useSystemDefaultTemplate = true)
        }
    }

    fun requestOverwrite(action: PendingPreviewOverwrite) {
        if (state.isMessageEdited) {
            pendingOverwrite = action
        } else {
            executeOverwrite(action)
        }
    }

    fun requestEditorSwitch(mod: CommunicationModule, st: CommunicationStatus, lang: String) {
        if (state.isTemplateEdited) {
            pendingEditorSwitch = PendingEditorSwitch.Load(mod, st, lang)
        } else {
            viewModel.loadTemplate(mod, st, lang)
        }
    }

    fun share(pdfOnly: Boolean = false, other: Boolean = false) {
        val p = state.prepared ?: return
        if (state.busy) return
        try {
            require(!state.message.contains(Regex("\\{[^{}]*\\}"))) {
                label("Remove unresolved placeholders first", "আগে অসম্পূর্ণ placeholder সরিয়ে নিন")
            }
            val apps = WhatsAppShareManager.installed(context)
            val resolvedApp = when {
                other -> null
                state.application in apps -> state.application
                state.application != "ASK" && state.application !in apps -> {
                    viewModel.error(
                        label(
                            "Preferred WhatsApp app is not installed. Select an available app or Other Share.",
                            "পছন্দের WhatsApp অ্যাপটি ইনস্টল নেই। নিচের উপলব্ধ অ্যাপ অথবা অন্যান্য শেয়ার বাছাই করুন।"
                        )
                    )
                    return
                }
                else -> {
                    viewModel.error(
                        label(
                            "Choose WhatsApp Messenger or WhatsApp Business first",
                            "আগে WhatsApp Messenger অথবা WhatsApp Business নির্বাচন করুন"
                        )
                    )
                    return
                }
            }
            if (p.attachment != null && !WhatsAppShareManager.isAttachmentReady(context, p.attachment)) {
                viewModel.error(label("Attachment file missing or expired; regenerate PDF/XLSX.", "সংযুক্ত ফাইলটি নেই বা মেয়াদোত্তীর্ণ; পুনরায় তৈরি করুন।"))
                return
            }
            if (pdfOnly) {
                require(p.attachment?.extension == "pdf" && WhatsAppShareManager.isAttachmentReady(context, p.attachment)) {
                    label("Generate PDF attachment first", "আগে PDF সংযুক্তি তৈরি করুন")
                }
            }
            val text = if (pdfOnly) "" else state.message
            val event = WhatsAppShareManager.share(context, p.attachment, text, resolvedApp)
            viewModel.record(event, resolvedApp ?: "SYSTEM_CHOOSER", p.attachment != null, text)
        } catch (e: Exception) {
            viewModel.error(e.message ?: label("Share failed", "শেয়ার ব্যর্থ হয়েছে"))
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, label("Close", "বন্ধ")) }
                    Text(
                        text = if (success && page == "preview") {
                            when (initialRequest?.module) {
                                CommunicationModule.DELIVERY -> label("Delivery challan dispatched", "ডেলিভারি চালান সফলভাবে সম্পন্ন হয়েছে")
                                CommunicationModule.EXCHANGE -> label("Exchange completed", "পণ্য বিনিময় সম্পন্ন হয়েছে")
                                else -> label("Invoice created successfully", "ইনভয়েস সফলভাবে তৈরি হয়েছে")
                            }
                        } else {
                            label("Smart Communication", "স্মার্ট যোগাযোগ")
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }

                FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val navTabs = buildList {
                        add("documents" to label("Documents", "ডকুমেন্ট"))
                        add("history" to label("History", "ইতিহাস"))
                        add("settings" to label("Settings", "সেটিংস"))
                        add("templates" to label("Templates", "টেমপ্লেট"))
                    }
                    navTabs.forEach { (key, title) ->
                        FilterChip(
                            selected = page == key,
                            onClick = {
                                page = key
                                if (key == "documents" && state.choices.isEmpty() && !state.busy) {
                                    viewModel.selectModule(state.module)
                                } else if (key == "templates" && state.editor.isEmpty()) {
                                    viewModel.loadTemplate(state.module, state.editorStatus, state.editorLanguage)
                                }
                            },
                            label = { Text(title) }
                        )
                    }
                    if (state.prepared != null || state.busy) {
                        FilterChip(
                            selected = page == "preview",
                            onClick = { page = "preview" },
                            label = { Text(label("Preview", "প্রিভিউ")) }
                        )
                    }
                }

                if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())

                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    state.error?.let {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                    state.notice?.let {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    when (page) {
                        "documents" -> {
                            ModulePicker(state.module, state.visibleModules, bn) {
                                viewModel.selectModule(it)
                                search = ""
                            }
                            if (state.module in setOf(CommunicationModule.STATEMENT, CommunicationModule.LEDGER, CommunicationModule.SUPPLIER_STATEMENT)) {
                                Text(label("Optional statement date range", "বিবরণীর সময়কাল (ঐচ্ছিক)"))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { pickDate(true) }) {
                                        Text(from?.let { SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(it)) } ?: label("From", "শুরু"))
                                    }
                                    OutlinedButton(onClick = { pickDate(false) }) {
                                        Text(to?.let { SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(it)) } ?: label("To", "শেষ"))
                                    }
                                    TextButton(onClick = { from = null; to = null }) {
                                        Text(label("All time", "সব সময়"))
                                    }
                                }
                            }
                            OutlinedTextField(
                                value = search,
                                onValueChange = { search = it },
                                label = { Text(label("Find document / contact", "ডকুমেন্ট / গ্রাহক খুঁজুন")) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (!state.busy && state.choices.isEmpty()) {
                                Text(
                                    label(
                                        "No available documents. Older exchanges without saved details cannot be reconstructed.",
                                        "কোনো ডকুমেন্ট পাওয়া যায়নি। সংরক্ষিত বিবরণ ছাড়া পুরোনো exchange পুনর্গঠন করা যাবে না।"
                                    )
                                )
                            }
                            state.choices.filter { it.label.contains(search, true) }.forEach { choice ->
                                OutlinedCard(
                                    onClick = {
                                        viewModel.prepare(choice.request.copy(from = from, to = to))
                                        page = "preview"
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(choice.label, Modifier.padding(16.dp))
                                }
                            }
                        }

                        "preview" -> state.prepared?.let { p ->
                            val apps = WhatsAppShareManager.installed(context)
                            val showCompactQuickShare =
                                initialRequest != null && !settingsMode && !state.settings.previewBeforeSharing && !forceExpandedPreview

                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(p.document.number, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                        AssistChip(
                                            onClick = {},
                                            label = { Text(p.document.status.title(p.language)) }
                                        )
                                    }
                                    Text(p.document.partyName, style = MaterialTheme.typography.titleMedium)
                                    Text(p.document.request.module.title(p.language), style = MaterialTheme.typography.labelMedium)
                                    p.document.fields.filterKeys {
                                        it in setOf(
                                            "invoice_date", "previous_due", "invoice_amount", "return_amount",
                                            "received_payment", "invoice_due", "total_outstanding",
                                            "historical_as_of", "historical_payment", "historical_due", "historical_balance",
                                            "valid_until", "delivery_information", "date_range", "opening_balance", "closing_balance"
                                        )
                                    }.forEach { (key, value) ->
                                        val pair = DynamicTemplateEngine.labels.getValue(key)
                                        Text("${if (bn) pair.second else pair.first}: $value", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }

                            if (p.isCustomTemplate && !p.template.contains("{status_text}")) {
                                Text(
                                    text = label(
                                        "Notice: Custom template does not include {status_text}; verify actual status above (${p.document.status.title(p.language)}).",
                                        "সতর্কতা: কাস্টম টেমপ্লেটে {status_text} নেই; উপরের প্রকৃত অবস্থা (${p.document.status.title(p.language)}) দেখে নিন।"
                                    ),
                                    color = MaterialTheme.colorScheme.tertiary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            p.document.warnings.forEach { Text(it, color = MaterialTheme.colorScheme.tertiary) }

                            if (p.document.request.module == CommunicationModule.QUOTATION) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedButton(onClick = { pickQuotationValidityDate(p.document.validUntilMillis) }, enabled = !state.busy) {
                                        Icon(Icons.Outlined.Event, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            if (p.document.validUntilMillis != null) label("Change Valid Until", "মেয়াদ পরিবর্তন")
                                            else label("Set Quotation Validity", "কোটেশনের মেয়াদ দিন")
                                        )
                                    }
                                }
                            } else if (p.document.request.module == CommunicationModule.SALES_ORDER) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = deliveryDraft,
                                        onValueChange = { deliveryDraft = it },
                                        label = { Text(label("Scheduled delivery info", "ডেলিভারির নির্ধারিত তথ্য")) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedButton(
                                        onClick = { viewModel.updateSalesOrderDeliveryInfo(deliveryDraft) },
                                        enabled = !state.busy
                                    ) {
                                        Text(label("Save", "সংরক্ষণ"))
                                    }
                                }
                            }

                            if (showCompactQuickShare) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            label(
                                                "Quick share mode is active (Preview before sharing is OFF). Review details above and tap to open WhatsApp.",
                                                "কুইক শেয়ার মোড চালু (শেয়ারের আগে প্রিভিউ বন্ধ)। উপরের তথ্য যাচাই করে নিচে চাপুন।"
                                            ),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        TextButton(onClick = { forceExpandedPreview = true }) {
                                            Text(label("Open full preview & message editor", "পূর্ণ প্রিভিউ ও বার্তা সম্পাদনা খুলুন"))
                                        }
                                    }
                                }
                            } else {
                                OutlinedTextField(
                                    value = state.phone,
                                    onValueChange = { viewModel.phone(it, manualOverride = true) },
                                    label = { Text(label("Recipient WhatsApp number", "প্রাপকের WhatsApp নম্বর")) },
                                    supportingText = {
                                        val norm = CommunicationPhone.normalize(state.phone)
                                        Text(
                                            norm?.let { label("Normalized: $it", "সঠিক ফরম্যাট: $it") }
                                                ?: label(
                                                    "Invalid/empty number (e.g. +88017...). Direct Chat is disabled, but Share via WhatsApp still lets you pick a contact inside WhatsApp.",
                                                    "নম্বর অকার্যকর/খালি (যেমন +88017...)। সরাসরি চ্যাট বন্ধ থাকবে, তবে WhatsApp শেয়ার করে ভেতর থেকে প্রাপক বাছাই করা যাবে।"
                                                )
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                TextButton(onClick = {
                                    try {
                                        contactPicker.launch(
                                            Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                                        )
                                    } catch (e: Exception) {
                                        viewModel.error(
                                            label(
                                                "No phone contact picker app available: ${e.message}",
                                                "ফোন কন্টাক্ট বাছাইয়ের কোনো অ্যাপ পাওয়া যায়নি: ${e.message}"
                                            )
                                        )
                                    }
                                }) {
                                    Icon(Icons.Outlined.Contacts, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(label("Choose phone contact", "ফোনের কন্টাক্ট নির্বাচন"))
                                }

                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("bn" to "বাংলা", "en" to "English").forEach { (code, title) ->
                                        FilterChip(
                                            selected = p.language == code,
                                            onClick = {
                                                if (p.language != code) {
                                                    requestOverwrite(PendingPreviewOverwrite.SwitchLanguage(code))
                                                }
                                            },
                                            label = { Text(title) }
                                        )
                                    }
                                    TextButton(onClick = { viewModel.savePartyLanguage(p.language) }) {
                                        Text(label("Save recipient language", "গ্রাহকের ভাষা সংরক্ষণ"))
                                    }
                                }

                                Text(label("Template variants (regenerates message)", "টেমপ্লেট ধরন (বার্তা নতুন করে তৈরি হবে)"))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    val statuses = listOf(CommunicationStatus.GENERAL, p.document.status) + listOfNotNull(
                                        CommunicationStatus.PREVIOUS_DUE.takeIf { (p.document.previousMinor ?: 0L) > 0L },
                                        CommunicationStatus.CREDIT.takeIf { (p.document.balanceMinor ?: 0L) < 0L }
                                    )
                                    statuses.distinct().forEach { statusOption ->
                                        AssistChip(
                                            onClick = { requestOverwrite(PendingPreviewOverwrite.SwitchStatusTemplate(statusOption)) },
                                            label = { Text(statusOption.title(p.language)) }
                                        )
                                    }
                                }

                                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                                    OutlinedTextField(
                                        value = state.message,
                                        onValueChange = viewModel::message,
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        label = { Text(label("Editable message", "বার্তা সম্পাদনা")) },
                                        minLines = 8
                                    )
                                }
                            }

                            if (p.document.request.module in setOf(CommunicationModule.PURCHASE_ORDER, CommunicationModule.PACKING)) {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { viewModel.generateXlsx() }, enabled = !state.busy) {
                                        Text(if (p.attachment?.extension == "xlsx") "XLSX ✓" else label("Generate XLSX", "XLSX তৈরি"))
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.prepare(p.document.request, p.language, forcePdf = true) },
                                        enabled = !state.busy
                                    ) {
                                        Text(if (p.attachment?.extension == "pdf") "PDF ✓" else label("Generate PDF", "PDF তৈরি"))
                                    }
                                }
                            }

                            p.attachment?.let { file ->
                                val isPdfFile = file.extension.equals("pdf", ignoreCase = true)
                                OutlinedCard(
                                    onClick = {
                                        if (isPdfFile) showPdf = true
                                        else viewModel.notice(label("XLSX spreadsheet ready (${file.length() / 1024} KB). PDF is not previewed when XLSX is active; tap Generate PDF to switch.", "XLSX ফাইল প্রস্তুত (${file.length() / 1024} KB)। প্রিভিউ/প্রিন্টের জন্য PDF বেছে নিন।"))
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(if (isPdfFile) Icons.Outlined.PictureAsPdf else Icons.Outlined.TableChart, null)
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            if (isPdfFile) label("PDF attachment · Tap to preview (${file.length() / 1024} KB)", "PDF সংযুক্তি · প্রিভিউ দেখুন (${file.length() / 1024} KB)")
                                            else "XLSX · ${file.length() / 1024} KB (${label("Spreadsheet active", "এক্সেল সংযুক্তি সক্রিয়")})"
                                        )
                                    }
                                }
                            }

                            p.attachmentError?.let {
                                Text(label("PDF failed: ", "PDF ব্যর্থ: ") + it, color = MaterialTheme.colorScheme.error)
                            }
                            if (p.attachment == null) {
                                OutlinedButton(
                                    onClick = { viewModel.prepare(p.document.request, p.language, forcePdf = true) },
                                    enabled = !state.busy
                                ) {
                                    Text(label("Generate / retry PDF", "PDF তৈরি / পুনরায় চেষ্টা"))
                                }
                            }

                            if (apps.isEmpty()) {
                                Text(
                                    label(
                                        "Neither WhatsApp Messenger nor Business is installed. Copy Message or use Other Share.",
                                        "WhatsApp Messenger বা Business কোনোটিই ইনস্টল নেই। বার্তা কপি অথবা অন্যান্য শেয়ার ব্যবহার করুন।"
                                    )
                                )
                            } else {
                                if (state.settings.preferredApp != "ASK" && state.settings.preferredApp !in apps) {
                                    Text(
                                        text = label(
                                            "Saved preferred WhatsApp app is not installed on this device; choose an installed app below.",
                                            "সেটিংসের পছন্দের WhatsApp অ্যাপটি ফোনে ইনস্টল নেই; নিচে থেকে ইনস্টল করা অ্যাপ বাছাই করুন।"
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    apps.forEach { app ->
                                        FilterChip(
                                            selected = state.application == app,
                                            onClick = { viewModel.application(app) },
                                            label = {
                                                Text(if (app == WhatsAppShareManager.BUSINESS) "WhatsApp Business" else "WhatsApp Messenger")
                                            }
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = { share() },
                                enabled = !state.busy && apps.isNotEmpty(),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Outlined.Share, null)
                                Spacer(Modifier.width(8.dp))
                                Text(label("Share via WhatsApp", "WhatsApp-এ শেয়ার"))
                            }

                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        WhatsAppShareManager.copy(context, state.message)
                                        viewModel.notice(label("Message copied", "বার্তা কপি হয়েছে"))
                                    },
                                    enabled = state.message.isNotBlank()
                                ) {
                                    Text(label("Copy Message", "বার্তা কপি"))
                                }
                                OutlinedButton(onClick = { share(other = true) }, enabled = !state.busy) {
                                    Text(label("Other Share", "অন্যান্য শেয়ার"))
                                }
                                OutlinedButton(
                                    onClick = {
                                        p.attachment?.let { ControlCenterShareService.printPdf(context, it, p.document.number) }
                                    },
                                    enabled = p.attachment?.extension == "pdf" && !state.busy
                                ) {
                                    Text(label("Print PDF", "PDF প্রিন্ট"))
                                }
                                if (!showCompactQuickShare) {
                                    TextButton(
                                        onClick = { requestOverwrite(PendingPreviewOverwrite.ReloadCurrentTemplate) },
                                        enabled = !state.busy
                                    ) {
                                        Text(label("Reload custom template", "কাস্টম টেমপ্লেট রিলোড"))
                                    }
                                    TextButton(
                                        onClick = { requestOverwrite(PendingPreviewOverwrite.ResetSystemDefault) },
                                        enabled = !state.busy
                                    ) {
                                        Text(label("Reset to system default", "সিস্টেম ডিফল্টে ফিরুন"))
                                    }
                                }
                            }

                            HorizontalDivider()
                            Text(label("If WhatsApp omits the caption", "WhatsApp-এ caption না গেলে"), style = MaterialTheme.typography.titleMedium)
                            Text(
                                label(
                                    "Copy Message → Share PDF → Open Customer Chat → Paste and Send. Select the recipient in WhatsApp; the app never sends automatically.",
                                    "বার্তা কপি → PDF শেয়ার → গ্রাহকের চ্যাট খুলুন → Paste করে Send। WhatsApp-এ প্রাপক নির্বাচন করুন; অ্যাপ নিজে বার্তা পাঠায় না।"
                                )
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { share(pdfOnly = true) },
                                    enabled = p.attachment?.extension == "pdf" && !state.busy && apps.isNotEmpty()
                                ) {
                                    Text(label("Share PDF Only", "শুধু PDF শেয়ার"))
                                }
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            val event = WhatsAppShareManager.openChat(context, state.phone, state.application)
                                            viewModel.record(event, state.application, false)
                                        } catch (e: Exception) {
                                            viewModel.error(e.message ?: label("Chat unavailable", "চ্যাট খোলা যায়নি"))
                                        }
                                    },
                                    enabled = CommunicationPhone.normalize(state.phone) != null && state.application in apps && !state.busy
                                ) {
                                    Text(label("Open Recipient Chat", "প্রাপকের চ্যাট খুলুন"))
                                }
                            }
                        } ?: run {
                            if (!state.busy) {
                                Text(label("Select a document to prepare a message.", "বার্তা তৈরির জন্য ডকুমেন্ট নির্বাচন করুন।"))
                            }
                        }

                        "settings" -> CommunicationSettingsEditor(state.settings, viewModel::saveSettings, bn)

                        "templates" -> {
                            ModulePicker(state.module, state.visibleModules, bn) {
                                requestEditorSwitch(it, state.editorStatus, state.editorLanguage)
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                CommunicationStatus.entries.forEach { st ->
                                    FilterChip(
                                        selected = state.editorStatus == st,
                                        onClick = { requestEditorSwitch(state.module, st, state.editorLanguage) },
                                        label = { Text(st.title(if (bn) "bn" else "en")) }
                                    )
                                }
                                listOf("bn" to "বাংলা", "en" to "English").forEach { (code, title) ->
                                    FilterChip(
                                        selected = state.editorLanguage == code,
                                        onClick = { requestEditorSwitch(state.module, state.editorStatus, code) },
                                        label = { Text(title) }
                                    )
                                }
                            }
                            Text(
                                text = if (state.isCustomTemplateLoaded) {
                                    label("Editing custom saved template", "সংরক্ষিত কাস্টম টেমপ্লেট সম্পাদনা করছেন")
                                } else {
                                    label("Using system default template", "সিস্টেম ডিফল্ট টেমপ্লেট ব্যবহৃত হচ্ছে")
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(label("Available placeholders — tap to insert", "ব্যবহারযোগ্য placeholder — যুক্ত করতে চাপুন"))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                DynamicTemplateEngine.allowed(state.module).forEach { key ->
                                    AssistChip(
                                        onClick = { viewModel.editTemplate(state.editor + "\n{$key}") },
                                        label = { Text("{$key}") }
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = state.editor,
                                onValueChange = viewModel::editTemplate,
                                minLines = 10,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(label("Template body", "টেমপ্লেট")) }
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { viewModel.saveTemplate() }) {
                                    Text(label("Save custom template", "কাস্টম টেমপ্লেট সংরক্ষণ"))
                                }
                                OutlinedButton(onClick = { viewModel.resetTemplate() }) {
                                    Text(label("Reset to system default", "সিস্টেম ডিফল্টে ফিরুন"))
                                }
                            }
                            Text(label("Live sample preview (fictional sample numbers)", "নমুনা প্রিভিউ (কাল্পনিক হিসাব)"), style = MaterialTheme.typography.titleMedium)
                            val sample = DynamicTemplateEngine.sampleFields(state.module, state.editorLanguage)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = DynamicTemplateEngine.render(state.editor, sample),
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        "history" -> {
                            Text(
                                label(
                                    "Local handoff events only; not WhatsApp Sent, Delivered or Read confirmation.",
                                    "এগুলো শুধু স্থানীয় share/chat খোলার লগ; প্রেরিত/পঠিত হওয়ার নিশ্চিত প্রমাণ নয়।"
                                ),
                                style = MaterialTheme.typography.bodySmall
                            )
                            OutlinedTextField(
                                value = historySearch,
                                onValueChange = { historySearch = it },
                                label = { Text(label("Search history (doc / party / phone)", "ইতিহাস খুঁজুন (নম্বর / নাম / ফোন)")) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = historyModuleFilter == null,
                                    onClick = { historyModuleFilter = null },
                                    label = { Text(label("All", "সব")) }
                                )
                                state.visibleModules.forEach { mod ->
                                    FilterChip(
                                        selected = historyModuleFilter == mod,
                                        onClick = { historyModuleFilter = if (historyModuleFilter == mod) null else mod },
                                        label = { Text(mod.title(if (bn) "bn" else "en")) }
                                    )
                                }
                            }
                            val filteredHistory = state.history.filter { h ->
                                val matchesModule = historyModuleFilter == null || h.module == historyModuleFilter?.name
                                val q = historySearch.trim()
                                val matchesSearch = q.isEmpty() ||
                                    h.documentNumber.contains(q, true) ||
                                    h.partyName.contains(q, true) ||
                                    h.phone.contains(q, true)
                                matchesModule && matchesSearch
                            }
                            if (filteredHistory.isEmpty()) {
                                Text(label("No matching history entries.", "কোনো লগ পাওয়া যায়নি।"))
                            }
                            filteredHistory.forEach { h ->
                                OutlinedCard(Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("${h.documentNumber} · ${h.partyName}", style = MaterialTheme.typography.titleMedium)
                                        Text("${h.event} · ${h.application} · ${h.language}\n${SimpleDateFormat("dd MMM yyyy HH:mm", Locale.US).format(Date(h.createdAt))}\n${h.phone}")
                                        Text(h.attachmentReference ?: label("No attachment", "সংযুক্তি নেই"))
                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            TextButton(onClick = { historyMessage = h.message }) {
                                                Text(label("Original message snapshot", "মূল বার্তার snapshot"))
                                            }
                                            OutlinedButton(onClick = {
                                                runCatching { CommunicationModule.valueOf(h.module) }.getOrNull()?.let { mod ->
                                                    viewModel.prepare(
                                                        CommunicationRequest(mod, h.documentId, h.rangeFrom, h.rangeTo),
                                                        h.language
                                                    )
                                                    page = "preview"
                                                    viewModel.notice(
                                                        label(
                                                            "Resend prepared with fresh current balances and regenerated attachment; original log stays unchanged.",
                                                            "বর্তমান হিসাব ও নতুন সংযুক্তিসহ পুনঃশেয়ার প্রস্তুত; মূল বার্তার লগ অপরিবর্তিত আছে।"
                                                        )
                                                    )
                                                }
                                            }) {
                                                Text(label("Prepare resend", "পুনঃশেয়ার প্রস্তুত"))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }

        if (showPdf) {
            state.prepared?.attachment?.takeIf { it.extension.equals("pdf", ignoreCase = true) }?.let { file ->
                CommunicationPdfPreview(file = file, bn = bn, onDismiss = { showPdf = false })
            }
        }

        pendingOverwrite?.let { action ->
            AlertDialog(
                onDismissRequest = { pendingOverwrite = null },
                title = { Text(label("Replace edited message?", "সম্পাদিত বার্তা পরিবর্তন করবেন?")) },
                text = { Text(label("Your manual edits to this message will be replaced.", "এই বার্তায় আপনার করা পরিবর্তনগুলো মুছে নতুন করে তৈরি হবে।")) },
                confirmButton = {
                    TextButton(onClick = {
                        executeOverwrite(action)
                        pendingOverwrite = null
                    }) {
                        Text(label("Replace", "পরিবর্তন করুন"))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingOverwrite = null }) {
                        Text(label("Cancel", "বাতিল"))
                    }
                }
            )
        }

        pendingEditorSwitch?.let { switchAction ->
            AlertDialog(
                onDismissRequest = { pendingEditorSwitch = null },
                title = { Text(label("Discard template edits?", "অসংরক্ষিত পরিবর্তন বাদ দেবেন?")) },
                text = { Text(label("Switching templates will discard unsaved edits.", "অন্য টেমপ্লেটে গেলে অসংরক্ষিত পরিবর্তন মুছে যাবে।")) },
                confirmButton = {
                    TextButton(onClick = {
                        if (switchAction is PendingEditorSwitch.Load) {
                            viewModel.loadTemplate(switchAction.module, switchAction.status, switchAction.language)
                        }
                        pendingEditorSwitch = null
                    }) {
                        Text(label("Discard & Switch", "বাদ দিয়ে যান"))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingEditorSwitch = null }) {
                        Text(label("Cancel", "বাতিল"))
                    }
                }
            )
        }

        historyMessage?.let { text ->
            AlertDialog(
                onDismissRequest = { historyMessage = null },
                title = { Text(label("Original message", "মূল বার্তা")) },
                text = { Text(text, Modifier.verticalScroll(rememberScrollState())) },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            WhatsAppShareManager.copy(context, text)
                            viewModel.notice(label("Original snapshot copied", "মূল বার্তা কপি হয়েছে"))
                            historyMessage = null
                        }) {
                            Text(label("Copy", "কপি"))
                        }
                        TextButton(onClick = { historyMessage = null }) { Text("OK") }
                    }
                }
            )
        }
    }
}

@Composable
private fun ModulePicker(
    selected: CommunicationModule,
    modules: List<CommunicationModule>,
    bn: Boolean,
    onSelect: (CommunicationModule) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(selected.title(if (bn) "bn" else "en"))
            Icon(Icons.Outlined.ExpandMore, null)
        }
        DropdownMenu(expanded, { expanded = false }) {
            modules.forEach { module ->
                DropdownMenuItem(
                    text = { Text(module.title(if (bn) "bn" else "en")) },
                    onClick = {
                        expanded = false
                        onSelect(module)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CommunicationSettingsEditor(
    settings: CommunicationSettings,
    save: (CommunicationSettings) -> Unit,
    bn: Boolean
) {
    val switches = listOf(
        Triple("Auto generate message", "স্বয়ংক্রিয় বার্তা", settings.autoGenerate),
        Triple("Include PDF", "PDF সংযুক্তি", settings.includePdf),
        Triple("Previous due", "পূর্বের বকেয়া", settings.includePreviousDue),
        Triple("Invoice amount", "ইনভয়েসের পরিমাণ", settings.includeInvoiceAmount),
        Triple("Payment details", "পেমেন্টের বিবরণ", settings.includePaymentDetails),
        Triple("Invoice due", "ইনভয়েস বকেয়া", settings.includeInvoiceDue),
        Triple("Total outstanding", "মোট বকেয়া", settings.includeOutstanding),
        Triple("Method and reference", "মাধ্যম ও রেফারেন্স", settings.includeMethodReference),
        Triple("Payment instructions", "পেমেন্ট নির্দেশনা", settings.includeInstructions),
        Triple("Due reminder", "বকেয়া স্মরণিকা", settings.includeReminder),
        Triple("Company signature", "প্রতিষ্ঠানের স্বাক্ষর", settings.includeSignature),
        Triple("Preview before sharing", "শেয়ারের আগে পূর্ণ প্রিভিউ", settings.previewBeforeSharing)
    )
    switches.forEachIndexed { i, (en, bangla, value) ->
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(if (bn) bangla else en, Modifier.weight(1f))
            Switch(
                checked = value,
                onCheckedChange = { checked ->
                    save(
                        when (i) {
                            0 -> settings.copy(autoGenerate = checked)
                            1 -> settings.copy(includePdf = checked)
                            2 -> settings.copy(includePreviousDue = checked)
                            3 -> settings.copy(includeInvoiceAmount = checked)
                            4 -> settings.copy(includePaymentDetails = checked)
                            5 -> settings.copy(includeInvoiceDue = checked)
                            6 -> settings.copy(includeOutstanding = checked)
                            7 -> settings.copy(includeMethodReference = checked)
                            8 -> settings.copy(includeInstructions = checked)
                            9 -> settings.copy(includeReminder = checked)
                            10 -> settings.copy(includeSignature = checked)
                            else -> settings.copy(previewBeforeSharing = checked)
                        }
                    )
                }
            )
        }
    }
    Text(if (bn) "ডিফল্ট ভাষা" else "Default language")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("bn" to "বাংলা", "en" to "English").forEach { (code, title) ->
            FilterChip(
                selected = settings.defaultLanguage == code,
                onClick = { save(settings.copy(defaultLanguage = code)) },
                label = { Text(title) }
            )
        }
    }
    Text(if (bn) "পছন্দের অ্যাপ" else "Preferred application")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            "ASK" to (if (bn) "প্রতিবার বাছাই" else "Ask every time"),
            WhatsAppShareManager.MESSENGER to "Messenger",
            WhatsAppShareManager.BUSINESS to "Business"
        ).forEach { (code, title) ->
            FilterChip(
                selected = settings.preferredApp == code,
                onClick = { save(settings.copy(preferredApp = code)) },
                label = { Text(title) }
            )
        }
    }
    var instructions by remember(settings.paymentInstructions) { mutableStateOf(settings.paymentInstructions) }
    var bank by remember(settings.bankDetails) { mutableStateOf(settings.bankDetails) }
    OutlinedTextField(
        value = instructions,
        onValueChange = { instructions = it },
        label = { Text(if (bn) "পেমেন্ট নির্দেশনা" else "Payment instructions") },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = bank,
        onValueChange = { bank = it },
        label = { Text(if (bn) "ব্যাংকের তথ্য" else "Bank details") },
        modifier = Modifier.fillMaxWidth()
    )
    Button(onClick = { save(settings.copy(paymentInstructions = instructions, bankDetails = bank)) }) {
        Text(if (bn) "নির্দেশনা সংরক্ষণ" else "Save instructions")
    }
}
