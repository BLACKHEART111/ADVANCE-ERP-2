package com.pocketdev.advance_erp.feature.communication

import android.app.DatePickerDialog
import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.pocketdev.advance_erp.core.communication.*
import com.pocketdev.advance_erp.core.communication.graphics.BrandedDocumentFormatter
import com.pocketdev.advance_erp.core.communication.graphics.BrandedDocumentPalette
import com.pocketdev.advance_erp.core.designsystem.*
import com.pocketdev.advance_erp.core.util.ControlCenterShareService
import com.pocketdev.advance_erp.data.entity.CommunicationSettings
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CommunicationLauncher(
    module: CommunicationModule? = null,
    documentId: String? = null,
    isBangla: Boolean = true,
    settings: Boolean = false,
    initialTab: String? = null
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
            initialTab = initialTab,
            dialogKey = "launcher_${module?.name ?: "all"}_${documentId ?: "none"}_${settings}_${initialTab ?: "default"}",
            onDismiss = { open = false }
        )
    }
}

internal sealed interface PendingPreviewOverwrite {
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
    initialTab: String? = null,
    dialogKey: String = "comm_${initialRequest?.module?.name ?: initialModule.name}_${initialRequest?.documentId ?: "hub"}_${settingsMode}_${initialTab ?: "default"}",
    onDismiss: () -> Unit,
    viewModel: CommunicationViewModel = hiltViewModel(key = dialogKey)
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var page by rememberSaveable(dialogKey) {
        mutableStateOf(
            initialTab ?: when {
                settingsMode -> "settings"
                initialRequest != null -> "preview"
                else -> "documents"
            }
        )
    }
    var showPreviewSheet by rememberSaveable(dialogKey) { mutableStateOf(false) }
    var forceExpandedPreview by rememberSaveable(dialogKey) { mutableStateOf(false) }
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

    val installedApps = remember(context, state.prepared, page) {
        WhatsAppShareManager.installed(context)
    }

    LaunchedEffect(installedApps, state.application) {
        if (state.application == "ASK" && installedApps.size == 1) {
            viewModel.application(installedApps.first())
        }
    }

    LaunchedEffect(initialRequest, settingsMode, initialTab) {
        viewModel.reset()
        if (initialRequest != null) {
            viewModel.prepare(initialRequest)
        } else {
            val defaultMod = if (initialModule in state.visibleModules) initialModule else state.visibleModules.firstOrNull() ?: initialModule
            if (initialTab == "templates") {
                viewModel.loadTemplate(defaultMod, state.editorStatus, state.editorLanguage)
            } else {
                viewModel.selectModule(defaultMod)
            }
        }
    }

    LaunchedEffect(page) {
        if (page == "settings") {
            viewModel.refreshCacheStats()
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

    fun resolveTargetApp(other: Boolean): String? {
        if (other) return null
        return when {
            state.application in installedApps -> state.application
            installedApps.size == 1 -> installedApps.first()
            state.application != "ASK" && state.application !in installedApps -> {
                viewModel.error(
                    label(
                        "Preferred WhatsApp app is not installed. Select an available app or Other Share.",
                        "পছন্দের WhatsApp অ্যাপটি ইনস্টল নেই। উপলব্ধ অ্যাপ অথবা অন্যান্য শেয়ার বাছাই করুন।"
                    )
                )
                null
            }
            else -> {
                viewModel.error(
                    label(
                        "Choose WhatsApp Messenger or WhatsApp Business first",
                        "আগে WhatsApp Messenger অথবা WhatsApp Business নির্বাচন করুন"
                    )
                )
                null
            }
        }
    }

    fun validateTextIfNeeded(needsText: Boolean): Boolean {
        if (!needsText) return true
        if (state.message.contains(Regex("\\{[^{}]*\\}"))) {
            viewModel.error(label("Remove unresolved placeholders first", "আগে অসম্পূর্ণ placeholder সরিয়ে নিন"))
            return false
        }
        return true
    }

    fun launchDocumentStep(
        docFile: File,
        resolvedApp: String?,
        steps: List<PlannedShareStep>,
        stepIndex: Int
    ) {
        val docExt = docFile.extension.lowercase()
        val sharedCsv = if (steps.size > 1) {
            ShareWorkflowPlanner.formatSharedFormatsCsv(state.selectedFormats, docExt)
        } else {
            if (docExt == "xlsx") "XLSX" else "PDF"
        }
        val eventName = if (steps.size > 1) "MULTI_STEP_HANDOFF" else "SHARE_INITIATED"
        val launchRes = runCatching {
            WhatsAppShareManager.share(context, docFile, "", resolvedApp)
        }
        launchRes.onFailure { e ->
            viewModel.error(e.message ?: label("Document share failed", "ডকুমেন্ট শেয়ার ব্যর্থ হয়েছে"))
            return
        }
        viewModel.record(
            event = eventName,
            application = resolvedApp ?: "SYSTEM_CHOOSER",
            attachment = true,
            message = if (ShareOutputFormat.TEXT_MESSAGE in state.selectedFormats) state.message else "",
            sharedFormatsOverride = sharedCsv,
            includePdfRef = true,
            includeImageRef = false
        )
        if (steps.size > 1) {
            viewModel.updateShareWorkflow(steps, completedIndex = stepIndex)
        }
    }

    fun launchImageStep(
        pages: List<File>,
        withCaption: Boolean,
        resolvedApp: String?,
        steps: List<PlannedShareStep>,
        stepIndex: Int
    ) {
        val sharedCsv = if (steps.size > 1) {
            val docExt = (state.prepared?.attachment ?: state.prepared?.pdfFile)?.extension ?: "pdf"
            ShareWorkflowPlanner.formatSharedFormatsCsv(state.selectedFormats, docExt)
        } else if (withCaption) {
            "IMAGE_CARD,TEXT_MESSAGE"
        } else {
            "IMAGE_CARD"
        }
        val caption = if (withCaption && pages.size == 1) state.message else ""
        if (withCaption && state.message.isNotBlank()) {
            WhatsAppShareManager.copy(context, state.message)
        }
        val eventName = if (steps.size > 1) "MULTI_STEP_HANDOFF" else "SHARE_INITIATED"
        val launchRes = runCatching {
            WhatsAppShareManager.shareFiles(context, pages, "image/png", caption, resolvedApp)
        }
        launchRes.onFailure { e ->
            viewModel.error(e.message ?: label("Image card share failed", "ইমেজ কার্ড শেয়ার ব্যর্থ হয়েছে"))
            return
        }
        viewModel.record(
            event = eventName,
            application = resolvedApp ?: "SYSTEM_CHOOSER",
            attachment = false,
            message = if (withCaption) state.message else "",
            sharedFormatsOverride = sharedCsv,
            includePdfRef = false,
            includeImageRef = true
        )
        if (steps.size > 1) {
            viewModel.updateShareWorkflow(steps, completedIndex = stepIndex)
        } else if (withCaption) {
            viewModel.notice(
                label(
                    "Image card + caption opened in share sheet (text also copied to clipboard).",
                    "ইমেজ কার্ড ও বার্তা শেয়ার শিটে খোলা হয়েছে (বার্তাটি ক্লিপবোর্ডেও কপি করা আছে)।"
                )
            )
        }
    }

    fun executeShareStep(
        step: PlannedShareStep,
        steps: List<PlannedShareStep>,
        other: Boolean
    ) {
        val p = state.prepared ?: return
        if (state.busy || state.generatingFormat != null) return
        val needsText = step.kind == ShareStepKind.SEND_TEXT || step.kind == ShareStepKind.SEND_IMAGE_WITH_CAPTION
        if (!validateTextIfNeeded(needsText)) return

        val resolvedApp = if (other) null else {
            if (installedApps.isEmpty()) {
                viewModel.error(label("WhatsApp is not installed on this device.", "এই ডিভাইসে WhatsApp ইনস্টল নেই।"))
                return
            }
            resolveTargetApp(other = false) ?: return
        }

        if ((ShareOutputFormat.TEXT_MESSAGE in state.selectedFormats || needsText) && state.message.isNotBlank()) {
            WhatsAppShareManager.copy(context, state.message)
        }

        when (step.kind) {
            ShareStepKind.SEND_TEXT -> {
                if (state.message.isBlank()) {
                    viewModel.error(label("Text message is empty", "টেক্সট মেসেজ খালি রয়েছে"))
                    return
                }
                val launchRes = runCatching {
                    WhatsAppShareManager.share(context, null, state.message, resolvedApp)
                }
                launchRes.onFailure { e ->
                    viewModel.error(e.message ?: label("Text share failed", "বার্তা শেয়ার ব্যর্থ হয়েছে"))
                    return
                }
                val docExt = (p.attachment ?: p.pdfFile)?.extension ?: "pdf"
                val sharedCsv = if (steps.size > 1) {
                    ShareWorkflowPlanner.formatSharedFormatsCsv(state.selectedFormats, docExt)
                } else {
                    ShareOutputFormat.TEXT_MESSAGE.name
                }
                viewModel.record(
                    event = if (steps.size > 1) "MULTI_STEP_HANDOFF" else "SHARE_INITIATED",
                    application = resolvedApp ?: "SYSTEM_CHOOSER",
                    attachment = false,
                    message = state.message,
                    sharedFormatsOverride = sharedCsv,
                    includePdfRef = false,
                    includeImageRef = false
                )
                if (steps.size > 1) {
                    viewModel.updateShareWorkflow(steps, completedIndex = step.stepIndex)
                }
            }

            ShareStepKind.SEND_DOCUMENT -> {
                val existingDoc = p.attachment ?: p.pdfFile
                if (existingDoc != null && WhatsAppShareManager.isAttachmentReady(context, existingDoc)) {
                    launchDocumentStep(existingDoc, resolvedApp, steps, step.stepIndex)
                } else {
                    viewModel.ensurePdfGenerated { freshPdf ->
                        launchDocumentStep(freshPdf, resolvedApp, steps, step.stepIndex)
                    }
                }
            }

            ShareStepKind.SEND_IMAGE_CARDS, ShareStepKind.SEND_IMAGE_WITH_CAPTION -> {
                val withCaption = step.kind == ShareStepKind.SEND_IMAGE_WITH_CAPTION
                val pages = p.imageCardPages
                val ready = pages.isNotEmpty() && pages.all { WhatsAppShareManager.isAttachmentReady(context, it) }
                if (ready) {
                    launchImageStep(pages, withCaption, resolvedApp, steps, step.stepIndex)
                } else {
                    viewModel.ensureImageCardsGenerated { freshPages ->
                        if (freshPages.isNotEmpty()) {
                            launchImageStep(freshPages, withCaption, resolvedApp, steps, step.stepIndex)
                        }
                    }
                }
            }
        }
    }

    fun triggerSelectedShare(other: Boolean = false) {
        val p = state.prepared ?: return
        if (state.busy || state.generatingFormat != null) return
        val docExt = (p.attachment ?: p.pdfFile)?.extension ?: "pdf"
        val steps = ShareWorkflowPlanner.buildShareSteps(
            selectedFormats = state.selectedFormats,
            imagePageCount = p.imageCardPages.size.coerceAtLeast(1),
            documentExtension = docExt
        )
        if (steps.size > 1) {
            viewModel.updateShareWorkflow(steps, completedIndex = null)
        } else {
            viewModel.clearShareWorkflow()
        }
        executeShareStep(steps.first(), steps, other)
    }

    fun shareSingleTabFromPreview(tab: SharePreviewTab) {
        val p = state.prepared ?: return
        val docExt = (p.attachment ?: p.pdfFile)?.extension ?: "pdf"
        val singleFormat = when (tab) {
            SharePreviewTab.PDF -> ShareOutputFormat.PDF
            SharePreviewTab.IMAGE -> ShareOutputFormat.IMAGE_CARD
            SharePreviewTab.TEXT -> ShareOutputFormat.TEXT_MESSAGE
        }
        val singleSteps = ShareWorkflowPlanner.buildShareSteps(
            selectedFormats = setOf(singleFormat),
            imagePageCount = p.imageCardPages.size.coerceAtLeast(1),
            documentExtension = docExt
        )
        executeShareStep(singleSteps.first(), singleSteps, other = true)
    }

    ERPFullScreenEditor(onDismissRequest = onDismiss) {
        Surface(Modifier.fillMaxSize(), color = ERPBackground) {
            Column(Modifier.fillMaxSize().imePadding()) {
                // Header
                Surface(color = ERPSurface, tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Outlined.Close, label("Close", "বন্ধ"))
                            }
                            Text(
                                text = when {
                                    success && page == "preview" -> when (initialRequest?.module) {
                                        CommunicationModule.DELIVERY -> label("Delivery challan dispatched", "ডেলিভারি চালান সফলভাবে সম্পন্ন হয়েছে")
                                        CommunicationModule.EXCHANGE -> label("Exchange completed", "পণ্য বিনিময় সম্পন্ন হয়েছে")
                                        CommunicationModule.PAYMENT -> label("Payment receipt recorded", "পেমেন্ট রসিদ সংরক্ষিত হয়েছে")
                                        CommunicationModule.RETURN -> label("Sales return completed", "বিক্রয় ফেরত সম্পন্ন হয়েছে")
                                        else -> label("Invoice created successfully", "ইনভয়েস সফলভাবে তৈরি হয়েছে")
                                    }
                                    page == "preview" -> label("Share Document", "ডকুমেন্ট শেয়ার")
                                    else -> label("Smart Communication", "স্মার্ট যোগাযোগ")
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (page == "preview" && state.prepared != null) {
                                TextButton(
                                    onClick = { showPreviewSheet = true },
                                    contentPadding = PaddingValues(horizontal = 10.dp)
                                ) {
                                    Icon(Icons.Outlined.Visibility, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(label("Preview", "প্রিভিউ"))
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(start = 14.dp, end = 14.dp, bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (state.prepared != null || state.busy) {
                                FilterChip(
                                    selected = page == "preview",
                                    onClick = { page = "preview" },
                                    label = { Text(label("Share", "শেয়ার")) }
                                )
                            }
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
                        }
                    }
                }

                if (state.busy || state.generatingFormat != null) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }

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
                            color = StatusSuccessContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = it,
                                color = StatusSuccess,
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
                            val isDark = LocalIsDarkTheme.current
                            val showQuickShareHint =
                                initialRequest != null && !settingsMode && !state.settings.previewBeforeSharing && !forceExpandedPreview

                            // 1. Reference #1 Document Summary Card
                            ShareDocumentSummaryCard(
                                prepared = p,
                                bn = bn,
                                onPickQuotationDate = { pickQuotationValidityDate(p.document.validUntilMillis) },
                                deliveryDraft = deliveryDraft,
                                onDeliveryDraftChange = { deliveryDraft = it },
                                onSaveDeliveryInfo = { viewModel.updateSalesOrderDeliveryInfo(deliveryDraft) },
                                busy = state.busy
                            )

                            if (p.isCustomTemplate && !p.template.contains("{status_text}")) {
                                Surface(
                                    color = StatusWarningContainer,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = label(
                                            "Notice: Custom template omits {status_text}; verify status above (${p.document.status.title(p.language)}).",
                                            "সতর্কতা: কাস্টম টেমপ্লেটে {status_text} নেই; উপরের প্রকৃত অবস্থা (${p.document.status.title(p.language)}) দেখে নিন।"
                                        ),
                                        color = StatusWarning,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }

                            p.document.warnings.forEach { w ->
                                Surface(
                                    color = StatusWarningContainer,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = w,
                                        color = StatusWarning,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }

                            // 2. Reference #1 Recipient / Customer Card
                            ShareRecipientCard(
                                prepared = p,
                                phone = state.phone,
                                bn = bn,
                                installedApps = installedApps,
                                activeApp = state.application,
                                busy = state.busy,
                                onPhoneChange = { viewModel.phone(it, manualOverride = true) },
                                onPickContact = {
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
                                },
                                onSwitchLanguage = { code ->
                                    if (p.language != code) {
                                        requestOverwrite(PendingPreviewOverwrite.SwitchLanguage(code))
                                    }
                                },
                                onSavePartyLanguage = { viewModel.savePartyLanguage(p.language) },
                                onOpenChat = {
                                    try {
                                        val appPkg = resolveTargetApp(other = false) ?: return@ShareRecipientCard
                                        val event = WhatsAppShareManager.openChat(context, state.phone, appPkg)
                                        viewModel.record(event, appPkg, false)
                                    } catch (e: Exception) {
                                        viewModel.error(e.message ?: label("Chat unavailable", "চ্যাট খোলা যায়নি"))
                                    }
                                }
                            )

                            // 3. Reference #1 "Select Share Format" Section
                            Text(
                                text = label("Select Share Format", "শেয়ার ফরম্যাট নির্বাচন করুন"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            val docFile = p.attachment ?: p.pdfFile
                            val isXlsxActive = p.attachment?.extension.equals("xlsx", ignoreCase = true)
                            val supportsXlsx = p.document.request.module in setOf(CommunicationModule.PURCHASE_ORDER, CommunicationModule.PACKING)
                            val docReadySubtitle = when {
                                state.generatingFormat == ShareOutputFormat.PDF ->
                                    label("Generating document...", "ডকুমেন্ট তৈরি হচ্ছে...")
                                p.attachmentError != null ->
                                    label("PDF error · Tap Retry", "PDF ত্রুটি · পুনরায় চেষ্টা করুন")
                                docFile != null && docFile.isFile ->
                                    if (isXlsxActive) {
                                        "XLSX · ${docFile.length() / 1024} KB (${label("Spreadsheet active", "এক্সেল সংযুক্তি")})"
                                    } else {
                                        label(
                                            "Full Professional Invoice · ${docFile.length() / 1024} KB",
                                            "সম্পূর্ণ প্রফেশনাল PDF সংযুক্তি · ${docFile.length() / 1024} KB"
                                        )
                                    }
                                else ->
                                    label(
                                        "Full Professional Document · PDF সংযুক্তি (on-demand)",
                                        "সম্পূর্ণ প্রফেশনাল PDF সংযুক্তি (প্রয়োজনে তৈরি হবে)"
                                    )
                            }

                            ShareFormatOptionCard(
                                title = label("PDF Invoice / Document", "PDF ইনভয়েস / ডকুমেন্ট"),
                                subtitle = docReadySubtitle,
                                icon = if (isXlsxActive) Icons.Outlined.TableChart else Icons.Outlined.PictureAsPdf,
                                iconBg = if (isDark) Color(0xFF3E1F28) else Color(0xFFFDECEC),
                                iconTint = if (isXlsxActive) SecondaryTeal else Color(0xFFC62828),
                                checked = ShareOutputFormat.PDF in state.selectedFormats,
                                isLoading = state.generatingFormat == ShareOutputFormat.PDF,
                                onToggle = { viewModel.toggleShareFormat(ShareOutputFormat.PDF) },
                                onCardClick = {
                                    viewModel.selectPreviewTab(SharePreviewTab.PDF)
                                    showPreviewSheet = true
                                },
                                extraContent = {
                                    if (supportsXlsx || p.attachmentError != null) {
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.padding(top = 6.dp)
                                        ) {
                                            if (supportsXlsx) {
                                                FilterChip(
                                                    selected = !isXlsxActive,
                                                    onClick = { viewModel.activatePdfAttachment() },
                                                    label = { Text("PDF") }
                                                )
                                                FilterChip(
                                                    selected = isXlsxActive,
                                                    onClick = { viewModel.generateXlsx() },
                                                    label = { Text("XLSX") }
                                                )
                                            }
                                            if (p.attachmentError != null) {
                                                TextButton(
                                                    onClick = { viewModel.ensurePdfGenerated() },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                                ) {
                                                    Text(label("Retry PDF", "পুনরায় PDF তৈরি"))
                                                }
                                            }
                                        }
                                    }
                                }
                            )

                            val imgCount = p.imageCardPages.size
                            val imgKb = p.imageCardPages.sumOf { it.length() } / 1024
                            val imgSubtitle = when {
                                state.generatingFormat == ShareOutputFormat.IMAGE_CARD ->
                                    label("Rendering HD PNG card...", "HD PNG কার্ড তৈরি হচ্ছে...")
                                p.imageError != null ->
                                    label("Image card failed · Tap Preview/Retry", "ইমেজ কার্ড ত্রুটি · পুনরায় চেষ্টা করুন")
                                imgCount > 1 ->
                                    label(
                                        "Branded Summary Image ($imgCount pages · $imgKb KB)",
                                        "ব্র্যান্ডেড সারাংশ ইমেজ কার্ড ($imgCount পৃষ্ঠা · $imgKb KB)"
                                    )
                                imgCount == 1 ->
                                    label(
                                        "Branded Summary Image · PNG ($imgKb KB)",
                                        "ব্র্যান্ডেড সারাংশ ইমেজ কার্ড · PNG ($imgKb KB)"
                                    )
                                else ->
                                    label(
                                        "Branded Summary Image (auto-generated on select)",
                                        "ব্র্যান্ডেড সারাংশ ইমেজ কার্ড (নির্বাচনে তৈরি হবে)"
                                    )
                            }

                            ShareFormatOptionCard(
                                title = label("Image Card", "ইমেজ কার্ড"),
                                subtitle = imgSubtitle,
                                icon = Icons.Outlined.Image,
                                iconBg = if (isDark) Color(0xFF332042) else Color(0xFFF3E8FF),
                                iconTint = PrimaryPurple,
                                checked = ShareOutputFormat.IMAGE_CARD in state.selectedFormats,
                                isLoading = state.generatingFormat == ShareOutputFormat.IMAGE_CARD,
                                onToggle = { viewModel.toggleShareFormat(ShareOutputFormat.IMAGE_CARD) },
                                onCardClick = {
                                    viewModel.selectPreviewTab(SharePreviewTab.IMAGE)
                                    showPreviewSheet = true
                                }
                            )

                            val textLineCount = state.message.lines().count { it.isNotBlank() }
                            ShareFormatOptionCard(
                                title = label("Text Message", "টেক্সট মেসেজ"),
                                subtitle = label(
                                    "WhatsApp Message (Caption) · $textLineCount lines",
                                    "WhatsApp বার্তা (ক্যাপশন) · $textLineCount লাইন"
                                ),
                                icon = Icons.Outlined.ChatBubbleOutline,
                                iconBg = StatusSuccessContainer,
                                iconTint = WhatsAppCtaGreen,
                                checked = ShareOutputFormat.TEXT_MESSAGE in state.selectedFormats,
                                isLoading = false,
                                onToggle = { viewModel.toggleShareFormat(ShareOutputFormat.TEXT_MESSAGE) },
                                onCardClick = {
                                    viewModel.selectPreviewTab(SharePreviewTab.TEXT)
                                    showPreviewSheet = true
                                },
                                extraContent = {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        TextButton(
                                            onClick = {
                                                WhatsAppShareManager.copy(context, state.message)
                                                viewModel.notice(label("Message copied", "বার্তা কপি হয়েছে"))
                                            },
                                            enabled = state.message.isNotBlank(),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Outlined.ContentCopy, null, modifier = Modifier.size(14.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(label("Copy", "কপি"), style = MaterialTheme.typography.labelMedium)
                                        }
                                        TextButton(
                                            onClick = {
                                                viewModel.selectPreviewTab(SharePreviewTab.TEXT)
                                                showPreviewSheet = true
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(14.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(label("Edit Text", "সম্পাদনা"), style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                            )

                            // Inline snippet or Quick Share Mode bar
                            if (showQuickShareHint) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = label(
                                                "Quick Share mode active",
                                                "কুইক শেয়ার মোড চালু আছে"
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.weight(1f)
                                        )
                                        TextButton(onClick = { forceExpandedPreview = true }) {
                                            Text(label("Show message", "বার্তা দেখুন"))
                                        }
                                    }
                                }
                            } else if (ShareOutputFormat.TEXT_MESSAGE in state.selectedFormats && state.message.isNotBlank()) {
                                Surface(
                                    color = ERPSurface,
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            viewModel.selectPreviewTab(SharePreviewTab.TEXT)
                                            showPreviewSheet = true
                                        }
                                ) {
                                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = label("Message Snippet", "বার্তার সংক্ষিপ্ত প্রিভিউ"),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = PrimaryPurple
                                            )
                                            Text(
                                                text = label("Tap to preview & edit", "সম্পূর্ণ দেখতে চাপুন"),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = state.message,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 4,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // 4. WhatsApp App Selector (if multiple installed or missing)
                            if (installedApps.isEmpty()) {
                                Surface(
                                    color = StatusWarningContainer,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = label(
                                            "Neither WhatsApp Messenger nor Business is installed. Use Preview, Copy Message, Save or Other Share.",
                                            "WhatsApp Messenger বা Business কোনোটিই ইনস্টল নেই। প্রিভিউ, বার্তা কপি, সেভ বা অন্যান্য শেয়ার ব্যবহার করুন।"
                                        ),
                                        color = StatusWarning,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            } else {
                                if (state.settings.preferredApp != "ASK" && state.settings.preferredApp !in installedApps) {
                                    Text(
                                        text = label(
                                            "Saved preferred WhatsApp app is not installed on this device; using available app below.",
                                            "সেটিংসের পছন্দের WhatsApp অ্যাপটি ফোনে নেই; উপলব্ধ অ্যাপ নিচে দেওয়া হলো।"
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                                if (installedApps.size > 1 || state.application == "ASK") {
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        installedApps.forEach { app ->
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
                            }

                            // 5. Reference #1 Outlined Purple "Preview" CTA
                            OutlinedButton(
                                onClick = {
                                    viewModel.selectPreviewTab(
                                        ShareWorkflowPlanner.resolveInitialPreviewTab(state.selectedFormats)
                                    )
                                    showPreviewSheet = true
                                },
                                enabled = !state.busy,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.5.dp, PrimaryPurple),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryPurple),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            ) {
                                Icon(Icons.Outlined.Visibility, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(label("Preview", "প্রিভিউ দেখুন"), fontWeight = FontWeight.Bold)
                            }

                            // 6. Reference #1 Green "Send via WhatsApp" Primary CTA
                            Button(
                                onClick = { triggerSelectedShare(other = false) },
                                enabled = !state.busy && state.generatingFormat == null && installedApps.isNotEmpty(),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = WhatsAppCtaGreen,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Outlined.Send, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = label("Send via WhatsApp", "WhatsApp-এ শেয়ার করুন"),
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // 7. Sequential Multi-Format Handoff Card (when >1 format selected & triggered)
                            if (state.plannedSteps.size > 1) {
                                SequentialShareWorkflowCard(
                                    steps = state.plannedSteps,
                                    completedIndices = state.completedStepIndices,
                                    bn = bn,
                                    busy = state.busy || state.generatingFormat != null,
                                    hasNormalizedPhone = CommunicationPhone.normalize(state.phone) != null && installedApps.isNotEmpty(),
                                    onExecuteStep = { step -> executeShareStep(step, state.plannedSteps, other = false) },
                                    onOpenChat = {
                                        try {
                                            val appPkg = resolveTargetApp(other = false) ?: return@SequentialShareWorkflowCard
                                            val event = WhatsAppShareManager.openChat(context, state.phone, appPkg)
                                            viewModel.record(event, appPkg, false)
                                        } catch (e: Exception) {
                                            viewModel.error(e.message ?: label("Chat unavailable", "চ্যাট খোলা যায়নি"))
                                        }
                                    },
                                    onDismissWorkflow = { viewModel.clearShareWorkflow() }
                                )
                            } else if (
                                ShareOutputFormat.IMAGE_CARD in state.selectedFormats &&
                                ShareOutputFormat.TEXT_MESSAGE in state.selectedFormats &&
                                ShareOutputFormat.PDF !in state.selectedFormats
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val steps = ShareWorkflowPlanner.buildShareSteps(setOf(ShareOutputFormat.TEXT_MESSAGE))
                                        executeShareStep(steps.first(), steps, other = installedApps.isEmpty())
                                    },
                                    enabled = state.message.isNotBlank() && !state.busy,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(label("Send Text Separately (if caption omitted)", "ক্যাপশন না গেলে শুধু টেক্সট আলাদা পাঠান"))
                                }
                            }

                            // 8. Secondary Quick Actions Row (System Share / Save / Print)
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { triggerSelectedShare(other = true) },
                                    enabled = !state.busy && state.generatingFormat == null
                                ) {
                                    Icon(Icons.Outlined.Share, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(label("Other Share", "অন্যান্য শেয়ার"))
                                }
                                OutlinedButton(
                                    onClick = {
                                        if (ShareOutputFormat.IMAGE_CARD in state.selectedFormats && ShareOutputFormat.PDF !in state.selectedFormats) {
                                            viewModel.saveImageCardsLocally()
                                        } else {
                                            viewModel.savePdfOrXlsxLocally()
                                        }
                                    },
                                    enabled = !state.busy && state.generatingFormat == null
                                ) {
                                    Icon(Icons.Outlined.Download, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(label("Save", "সেভ"))
                                }
                                OutlinedButton(
                                    onClick = {
                                        val existingPdf = p.pdfFile ?: p.attachment?.takeIf { it.extension.equals("pdf", ignoreCase = true) }
                                        if (existingPdf != null && existingPdf.isFile) {
                                            ControlCenterShareService.printPdf(context, existingPdf, p.document.number)
                                        } else {
                                            viewModel.ensurePdfGenerated { fresh ->
                                                ControlCenterShareService.printPdf(context, fresh, p.document.number)
                                            }
                                        }
                                    },
                                    enabled = !state.busy && state.generatingFormat == null
                                ) {
                                    Icon(Icons.Outlined.Print, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(label("Print", "প্রিন্ট"))
                                }
                            }
                        } ?: run {
                            if (!state.busy) {
                                Text(label("Select a document to prepare sharing.", "শেয়ার করার জন্য ডকুমেন্ট নির্বাচন করুন।"))
                            }
                        }

                        "settings" -> CommunicationSettingsEditor(
                            settings = state.settings,
                            cacheStats = state.cacheStats,
                            historyCount = state.history.size,
                            save = viewModel::saveSettings,
                            onClearCache = viewModel::clearAttachmentCache,
                            onClearHistory = viewModel::clearShareHistory,
                            onNotice = viewModel::notice,
                            bn = bn
                        )

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
                                    "Local handoff & save logs only; not WhatsApp Sent, Delivered or Read confirmation.",
                                    "এগুলো শুধু স্থানীয় share/save লগ; WhatsApp প্রেরিত বা পঠিত হওয়ার নিশ্চিত প্রমাণ নয়।"
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
                                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "${h.documentNumber} · ${h.partyName}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Surface(
                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = h.event,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            "${h.application} · ${h.language.uppercase()} · ${SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(h.createdAt))}" +
                                                if (h.phone.isNotBlank()) " · ${h.phone}" else "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        val formatTokens = h.sharedFormats.split(",").map { it.trim() }.filter { it.isNotBlank() }
                                        if (formatTokens.isNotEmpty()) {
                                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                formatTokens.forEach { tok ->
                                                    Surface(
                                                        color = PrimaryPurple.copy(alpha = 0.12f),
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Text(
                                                            text = tok,
                                                            color = PrimaryPurple,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.SemiBold,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        if (!h.attachmentReference.isNullOrBlank() || !h.imageReference.isNullOrBlank()) {
                                            val refs = listOfNotNull(
                                                h.attachmentReference?.let { "Doc: $it" },
                                                h.imageReference?.let { "Img: $it" }
                                            ).joinToString(" · ")
                                            Text(refs, style = MaterialTheme.typography.bodySmall)
                                        }
                                        if (!h.localSavedPath.isNullOrBlank()) {
                                            Text(
                                                text = label("Saved: ${h.localSavedPath}", "সেভ করা হয়েছে: ${h.localSavedPath}"),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = StatusSuccess
                                            )
                                        }
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
                                                            "Resend prepared with fresh current balances and regenerated attachments; original log stays unchanged.",
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

        if (showPreviewSheet && state.prepared != null) {
            val prep = state.prepared!!
            CommunicationSharePreviewSheet(
                state = state,
                prepared = prep,
                bn = bn,
                hasWhatsApp = installedApps.isNotEmpty(),
                onDismiss = { showPreviewSheet = false },
                onSelectTab = { tab -> viewModel.selectPreviewTab(tab) },
                onMessageChange = viewModel::message,
                onRequestOverwrite = { action -> requestOverwrite(action) },
                onEnsurePdf = { viewModel.ensurePdfGenerated() },
                onActivatePdf = { viewModel.activatePdfAttachment() },
                onGenerateXlsx = { viewModel.generateXlsx() },
                onEnsureImage = { viewModel.ensureImageCardsGenerated() },
                onCopyMessage = {
                    WhatsAppShareManager.copy(context, state.message)
                    viewModel.notice(label("Message copied", "বার্তা কপি হয়েছে"))
                },
                onShareActiveTab = { tab -> shareSingleTabFromPreview(tab) },
                onSaveActiveTab = { tab ->
                    when (tab) {
                        SharePreviewTab.PDF -> viewModel.savePdfOrXlsxLocally()
                        SharePreviewTab.IMAGE -> viewModel.saveImageCardsLocally()
                        SharePreviewTab.TEXT -> {
                            WhatsAppShareManager.copy(context, state.message)
                            viewModel.notice(label("Message copied to clipboard", "বার্তাটি ক্লিপবোর্ডে কপি হয়েছে"))
                        }
                    }
                },
                onPrintPdf = {
                    val existingPdf = prep.pdfFile ?: prep.attachment?.takeIf { it.extension.equals("pdf", ignoreCase = true) }
                    if (existingPdf != null && existingPdf.isFile) {
                        ControlCenterShareService.printPdf(context, existingPdf, prep.document.number)
                    } else {
                        viewModel.ensurePdfGenerated { fresh ->
                            ControlCenterShareService.printPdf(context, fresh, prep.document.number)
                        }
                    }
                },
                onSendViaWhatsApp = {
                    showPreviewSheet = false
                    triggerSelectedShare(other = false)
                }
            )
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShareDocumentSummaryCard(
    prepared: PreparedCommunication,
    bn: Boolean,
    onPickQuotationDate: () -> Unit,
    deliveryDraft: String,
    onDeliveryDraftChange: (String) -> Unit,
    onSaveDeliveryInfo: () -> Unit,
    busy: Boolean
) {
    fun label(en: String, bangla: String) = if (bn) bangla else en
    val doc = prepared.document
    val badge = BrandedDocumentPalette.resolveStatusBadge(doc.status, bn)

    Surface(
        color = ERPSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                            contentDescription = null,
                            tint = PrimaryPurple
                        )
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = doc.request.module.title(prepared.language),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = doc.number,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryPurple
                    )
                    doc.documentDateMillis?.let { dt ->
                        Text(
                            text = BrandedDocumentFormatter.formatDateTime(dt, bn, includeTime = true, shortMonth = true),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Surface(
                    color = Color(badge.bgColor),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(badge.borderColor))
                ) {
                    Text(
                        text = badge.text,
                        color = Color(badge.textColor),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                doc.invoiceBreakdown?.let { inv ->
                    SummaryMetricChip(label("Bill", "মোট বিল"), BrandedDocumentFormatter.formatTaka(inv.netInvoiceAmountMinor))
                    SummaryMetricChip(label("Paid", "পরিশোধ"), BrandedDocumentFormatter.formatTaka(inv.allocatedPaymentMinor), StatusSuccess)
                    SummaryMetricChip(label("Invoice Due", "ইনভয়েস বকেয়া"), BrandedDocumentFormatter.formatTaka(inv.invoiceDueMinor), if (inv.invoiceDueMinor > 0L) StatusError else StatusSuccess)
                    SummaryMetricChip(
                        label("Total Balance", "সর্বমোট ব্যালেন্স"),
                        BrandedDocumentFormatter.formatTaka(inv.currentAccountBalanceMinor),
                        if (inv.currentAccountBalanceMinor > 0L) StatusError else if (inv.currentAccountBalanceMinor < 0L) StatusInfo else StatusSuccess
                    )
                } ?: doc.paymentBreakdown?.let { pay ->
                    SummaryMetricChip(label("Amount", "পরিমাণ"), BrandedDocumentFormatter.formatTaka(pay.paymentAmountMinor), StatusSuccess)
                    pay.previousBalanceMinor?.let {
                        SummaryMetricChip(label("Previous", "পূর্বের"), BrandedDocumentFormatter.formatTaka(it))
                    }
                    SummaryMetricChip(
                        label("Updated", "বর্তমান"),
                        BrandedDocumentFormatter.formatTaka(pay.currentAccountBalanceMinor),
                        if (pay.currentAccountBalanceMinor > 0L) StatusError else StatusSuccess
                    )
                } ?: doc.reminderBreakdown?.let { rem ->
                    SummaryMetricChip(label("Total Due", "সর্বমোট বকেয়া"), BrandedDocumentFormatter.formatTaka(rem.currentTotalDueMinor), StatusError)
                    SummaryMetricChip(label("Invoices", "ইনভয়েস"), rem.unpaidInvoiceCount.toString())
                } ?: doc.statementBreakdown?.let { st ->
                    SummaryMetricChip(label("Opening", "প্রারম্ভিক"), BrandedDocumentFormatter.formatTaka(st.openingBalanceMinor))
                    SummaryMetricChip(label("Closing", "বর্তমান"), BrandedDocumentFormatter.formatTaka(st.closingBalanceMinor), if (st.closingBalanceMinor > 0L) StatusError else StatusSuccess)
                } ?: run {
                    doc.fields["document_amount"]?.let { SummaryMetricChip(label("Amount", "পরিমাণ"), it) }
                    doc.fields["return_amount"]?.let { SummaryMetricChip(label("Return", "ফেরত"), it) }
                    doc.fields["price_difference"]?.let { SummaryMetricChip(label("Difference", "পার্থক্য"), it) }
                    doc.fields["total_quantity"]?.let { SummaryMetricChip(label("Qty", "পরিমাণ"), it) }
                    doc.fields["valid_until"]?.let { SummaryMetricChip(label("Valid Until", "মেয়াদ"), it) }
                    doc.fields["delivery_information"]?.let { SummaryMetricChip(label("Delivery", "ডেলিভারি"), it) }
                }
            }

            if (doc.request.module == CommunicationModule.QUOTATION) {
                OutlinedButton(onClick = onPickQuotationDate, enabled = !busy) {
                    Icon(Icons.Outlined.Event, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (doc.validUntilMillis != null) label("Change Valid Until", "মেয়াদ পরিবর্তন")
                        else label("Set Quotation Validity", "কোটেশনের মেয়াদ দিন")
                    )
                }
            } else if (doc.request.module == CommunicationModule.SALES_ORDER) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = deliveryDraft,
                        onValueChange = onDeliveryDraftChange,
                        label = { Text(label("Scheduled delivery info", "ডেলিভারির নির্ধারিত তথ্য")) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedButton(onClick = onSaveDeliveryInfo, enabled = !busy) {
                        Text(label("Save", "সংরক্ষণ"))
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryMetricChip(
    caption: String,
    value: String,
    valueColor: Color? = null
) {
    Column {
        Text(caption, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = valueColor ?: TextPrimary)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShareRecipientCard(
    prepared: PreparedCommunication,
    phone: String,
    bn: Boolean,
    installedApps: List<String>,
    activeApp: String,
    busy: Boolean,
    onPhoneChange: (String) -> Unit,
    onPickContact: () -> Unit,
    onSwitchLanguage: (String) -> Unit,
    onSavePartyLanguage: () -> Unit,
    onOpenChat: () -> Unit
) {
    fun label(en: String, bangla: String) = if (bn) bangla else en
    val doc = prepared.document
    val normalized = CommunicationPhone.normalize(phone)

    Surface(
        color = ERPSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (doc.supplier) label("Supplier", "সরবরাহকারী") else label("Customer", "গ্রাহক"),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    listOf("bn" to "বাংলা", "en" to "English").forEach { (code, title) ->
                        FilterChip(
                            selected = prepared.language == code,
                            onClick = { onSwitchLanguage(code) },
                            label = { Text(title, style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                    TextButton(
                        onClick = onSavePartyLanguage,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(label("Save", "সংরক্ষণ"), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.Person, null, tint = PrimaryPurple, modifier = Modifier.size(20.dp))
                Column(Modifier.weight(1f)) {
                    Text(doc.partyName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    doc.party?.code?.takeIf { it.isNotBlank() }?.let { code ->
                        Text(code, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (normalized != null && activeApp in installedApps) {
                    TextButton(onClick = onOpenChat, enabled = !busy) {
                        Icon(Icons.AutoMirrored.Outlined.Chat, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(label("Open Chat", "চ্যাট"))
                    }
                }
            }

            OutlinedTextField(
                value = phone,
                onValueChange = onPhoneChange,
                singleLine = true,
                label = { Text(label("Recipient WhatsApp number", "প্রাপকের WhatsApp নম্বর")) },
                leadingIcon = { Icon(Icons.Outlined.Phone, null) },
                trailingIcon = {
                    IconButton(onClick = onPickContact) {
                        Icon(Icons.Outlined.Contacts, label("Choose contact", "কন্টাক্ট বাছাই"))
                    }
                },
                supportingText = {
                    Text(
                        normalized?.let { label("Verified: $it", "সঠিক নম্বর: $it") }
                            ?: label(
                                "Enter e.g. 017... or +88017... (Direct Chat needs valid number; WhatsApp Share still works).",
                                "উদাহরণ: 017... বা +88017... (চ্যাটের জন্য বৈধ নম্বর প্রয়োজন; WhatsApp শেয়ারে ভেতর থেকে বাছাই করা যায়)।"
                            )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ShareFormatOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    checked: Boolean,
    isLoading: Boolean,
    onToggle: () -> Unit,
    onCardClick: () -> Unit,
    extraContent: (@Composable () -> Unit)? = null
) {
    val borderColor = if (checked) PrimaryPurple else MaterialTheme.colorScheme.outlineVariant
    Surface(
        color = ERPSurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(if (checked) 1.5.dp else 1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onCardClick)
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = iconBg,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = iconTint)
                        } else {
                            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
                        }
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Checkbox(
                    checked = checked,
                    onCheckedChange = { onToggle() }
                )
            }
            extraContent?.invoke()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SequentialShareWorkflowCard(
    steps: List<PlannedShareStep>,
    completedIndices: Set<Int>,
    bn: Boolean,
    busy: Boolean,
    hasNormalizedPhone: Boolean,
    onExecuteStep: (PlannedShareStep) -> Unit,
    onOpenChat: () -> Unit,
    onDismissWorkflow: () -> Unit
) {
    fun label(en: String, bangla: String) = if (bn) bangla else en
    val allDone = completedIndices.size >= steps.size
    val nextStep = steps.firstOrNull { it.stepIndex !in completedIndices }

    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (allDone) {
                        label("All ${steps.size} share steps launched", "সব ${steps.size}টি ধাপ সম্পন্ন হয়েছে")
                    } else {
                        label(
                            "Multi-format WhatsApp flow (${completedIndices.size}/${steps.size})",
                            "একাধিক ফরম্যাট WhatsApp-এ পাঠানোর ধাপ (${completedIndices.size}/${steps.size})"
                        )
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryPurple,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onDismissWorkflow, contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Text(label("Done", "সম্পন্ন"))
                }
            }

            Text(
                text = label(
                    "WhatsApp separates PDF, Image Card and Text safely in sequence. Tap each remaining step after returning from WhatsApp.",
                    "WhatsApp একসাথে PDF, Image ও Text আলাদাভাবে গ্রহণ করে। WhatsApp থেকে ফিরে পরবর্তী ধাপে চাপুন।"
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            steps.forEach { step ->
                val done = step.stepIndex in completedIndices
                val isCurrent = !done && nextStep?.stepIndex == step.stepIndex
                Surface(
                    color = if (isCurrent) ERPSurface else ERPSurface.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        if (isCurrent) 1.5.dp else 1.dp,
                        when {
                            done -> StatusSuccess.copy(alpha = 0.45f)
                            isCurrent -> PrimaryPurple
                            else -> MaterialTheme.colorScheme.outlineVariant
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = !busy) { onExecuteStep(step) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = when {
                                done -> StatusSuccess
                                isCurrent -> PrimaryPurple
                                else -> ERPSurfaceVariant
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(26.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (done) {
                                    Icon(Icons.Outlined.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                } else {
                                    Text(
                                        text = "${step.stepIndex + 1}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = if (bn) step.titleBn else step.titleEn,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                            )
                            Text(
                                text = if (bn) step.subtitleBn else step.subtitleEn,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(
                            onClick = { onExecuteStep(step) },
                            enabled = !busy,
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text(
                                if (done) label("Resend", "আবার পাঠান")
                                else if (isCurrent) label("Send Now", "এখন পাঠান")
                                else label("Send", "পাঠান")
                            )
                        }
                    }
                }
            }

            if (nextStep != null) {
                Button(
                    onClick = { onExecuteStep(nextStep) },
                    enabled = !busy,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WhatsAppCtaGreen,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp)
                ) {
                    Icon(Icons.AutoMirrored.Outlined.Send, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (bn) {
                            "পরবর্তী ধাপ (${nextStep.stepIndex + 1}/${nextStep.totalSteps}): ${nextStep.titleBn}"
                        } else {
                            "Next Step (${nextStep.stepIndex + 1}/${nextStep.totalSteps}): ${nextStep.titleEn}"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (hasNormalizedPhone) {
                OutlinedButton(
                    onClick = onOpenChat,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.AutoMirrored.Outlined.Chat, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(label("Open Recipient Chat (paste if needed)", "প্রাপকের চ্যাট খুলুন (প্রয়োজনে Paste করুন)"))
                }
            }
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
    cacheStats: CommunicationCacheStats,
    historyCount: Int,
    save: (CommunicationSettings) -> Unit,
    onClearCache: () -> Unit,
    onClearHistory: () -> Unit,
    onNotice: (String?) -> Unit,
    bn: Boolean
) {
    fun label(en: String, bangla: String) = if (bn) bangla else en
    var confirmClearHistory by remember { mutableStateOf(false) }

    fun updateOutputFormatToggle(target: ShareOutputFormat, checked: Boolean) {
        val next = when (target) {
            ShareOutputFormat.PDF -> settings.copy(includePdf = checked)
            ShareOutputFormat.IMAGE_CARD -> settings.copy(includeImageCard = checked)
            ShareOutputFormat.TEXT_MESSAGE -> settings.copy(includeTextMessage = checked)
        }
        if (!checked && !next.includePdf && !next.includeImageCard && !next.includeTextMessage) {
            onNotice(label("Keep at least one default sharing format enabled.", "কমপক্ষে একটি ডিফল্ট শেয়ার ফরম্যাট সক্রিয় রাখুন।"))
            return
        }
        onNotice(null)
        save(next)
    }

    // 1. Default Sharing Formats & Workflow
    SettingsSectionCard(
        title = label("Default Sharing Formats", "ডিফল্ট শেয়ার ফরম্যাট"),
        subtitle = label(
            "Pre-selected formats when opening Share Document from any module",
            "যেকোনো মডিউল থেকে শেয়ার খুললে স্বয়ংক্রিয়ভাবে নির্বাচিত ফরম্যাটসমূহ"
        )
    ) {
        SettingsToggleRow(
            title = label("Include PDF Document", "PDF ডকুমেন্ট সংযুক্তি"),
            checked = settings.includePdf,
            onCheckedChange = { updateOutputFormatToggle(ShareOutputFormat.PDF, it) }
        )
        SettingsToggleRow(
            title = label("Include Image Card (PNG)", "ইমেজ কার্ড (PNG) সংযুক্তি"),
            checked = settings.includeImageCard,
            onCheckedChange = { updateOutputFormatToggle(ShareOutputFormat.IMAGE_CARD, it) }
        )
        SettingsToggleRow(
            title = label("Include Text Message", "টেক্সট মেসেজ"),
            checked = settings.includeTextMessage,
            onCheckedChange = { updateOutputFormatToggle(ShareOutputFormat.TEXT_MESSAGE, it) }
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SettingsToggleRow(
            title = label("Auto generate message", "স্বয়ংক্রিয় বার্তা তৈরি"),
            checked = settings.autoGenerate,
            onCheckedChange = { save(settings.copy(autoGenerate = it)) }
        )
        SettingsToggleRow(
            title = label("Show message snippet before sharing", "শেয়ারের আগে পূর্ণ প্রিভিউ / বার্তা দেখান"),
            checked = settings.previewBeforeSharing,
            onCheckedChange = { save(settings.copy(previewBeforeSharing = it)) }
        )
    }

    // 2. Default Language & Sharing Channel
    SettingsSectionCard(
        title = label("Default Language & Channel", "ডিফল্ট ভাষা ও মাধ্যম"),
        subtitle = label(
            "Fallback language when a recipient has no saved preference, plus preferred WhatsApp app",
            "গ্রাহকের নিজস্ব ভাষা সংরক্ষিত না থাকলে ব্যবহৃত ভাষা ও পছন্দের WhatsApp অ্যাপ"
        )
    ) {
        Text(label("Default language", "ডিফল্ট ভাষা"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("bn" to "বাংলা", "en" to "English").forEach { (code, title) ->
                FilterChip(
                    selected = settings.defaultLanguage == code,
                    onClick = { save(settings.copy(defaultLanguage = code)) },
                    label = { Text(title) }
                )
            }
        }
        Text(label("Preferred sharing channel", "পছন্দের শেয়ারিং অ্যাপ"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                "ASK" to label("Ask every time", "প্রতিবার বাছাই"),
                WhatsAppShareManager.MESSENGER to "WhatsApp Messenger",
                WhatsAppShareManager.BUSINESS to "WhatsApp Business"
            ).forEach { (code, title) ->
                FilterChip(
                    selected = settings.preferredApp == code,
                    onClick = { save(settings.copy(preferredApp = code)) },
                    label = { Text(title) }
                )
            }
        }
    }

    // 3. Document Branding & Element Visibility
    SettingsSectionCard(
        title = label("Document Elements & Branding", "ডকুমেন্ট উপাদান ও ব্র্যান্ডিং"),
        subtitle = label(
            "PDF & Image Card exports use light-branded templates. Configure logo, signature, BIN, tagline & website under Settings → Business Profile.",
            "PDF ও ইমেজ কার্ড ব্র্যান্ডেড হালকা থিমে তৈরি হয়। লোগো, স্বাক্ষর, BIN, ট্যাগলাইন ও ওয়েবসাইট সেট করতে Settings → Business Profile দেখুন।"
        )
    ) {
        SettingsToggleRow(
            title = label("Show product thumbnails", "পণ্যের ছবি দেখান"),
            checked = settings.showProductPhoto,
            onCheckedChange = { save(settings.copy(showProductPhoto = it)) }
        )
        SettingsToggleRow(
            title = label("Show variant SKU codes", "ভ্যারিয়েন্ট SKU কোড দেখান"),
            checked = settings.showVariantSku,
            onCheckedChange = { save(settings.copy(showVariantSku = it)) }
        )
        SettingsToggleRow(
            title = label("Show invoice QR verification code", "ইনভয়েস QR যাচাইকরণ কোড দেখান"),
            checked = settings.showQrCode,
            onCheckedChange = { save(settings.copy(showQrCode = it)) }
        )
        SettingsToggleRow(
            title = label("Include company signature in messages/PDF", "প্রতিষ্ঠানের স্বাক্ষর ও নাম যুক্ত করুন"),
            checked = settings.includeSignature,
            onCheckedChange = { save(settings.copy(includeSignature = it)) }
        )
    }

    // 4. Financial Content & Payment Instructions
    SettingsSectionCard(
        title = label("Message Details & Payment Terms", "বার্তার তথ্য ও পেমেন্ট শর্তাবলি"),
        subtitle = label(
            "Control which recipient-safe financial figures and payment details appear by default",
            "ডিফল্ট বার্তায় কোন কোন হিসাব ও পেমেন্ট নির্দেশনা দেখাবে তা নির্ধারণ করুন"
        )
    ) {
        SettingsToggleRow(
            title = label("Include previous due balance", "পূর্বের বকেয়া যুক্ত করুন"),
            checked = settings.includePreviousDue,
            onCheckedChange = { save(settings.copy(includePreviousDue = it)) }
        )
        SettingsToggleRow(
            title = label("Include invoice amount", "ইনভয়েসের পরিমাণ যুক্ত করুন"),
            checked = settings.includeInvoiceAmount,
            onCheckedChange = { save(settings.copy(includeInvoiceAmount = it)) }
        )
        SettingsToggleRow(
            title = label("Include payment details", "পেমেন্টের বিবরণ যুক্ত করুন"),
            checked = settings.includePaymentDetails,
            onCheckedChange = { save(settings.copy(includePaymentDetails = it)) }
        )
        SettingsToggleRow(
            title = label("Include invoice due", "ইনভয়েস বকেয়া যুক্ত করুন"),
            checked = settings.includeInvoiceDue,
            onCheckedChange = { save(settings.copy(includeInvoiceDue = it)) }
        )
        SettingsToggleRow(
            title = label("Include total outstanding", "সর্বমোট বকেয়া যুক্ত করুন"),
            checked = settings.includeOutstanding,
            onCheckedChange = { save(settings.copy(includeOutstanding = it)) }
        )
        SettingsToggleRow(
            title = label("Include payment method & reference", "পেমেন্ট মাধ্যম ও রেফারেন্স"),
            checked = settings.includeMethodReference,
            onCheckedChange = { save(settings.copy(includeMethodReference = it)) }
        )
        SettingsToggleRow(
            title = label("Include payment instructions & bank details", "পেমেন্ট নির্দেশনা ও ব্যাংকের তথ্য"),
            checked = settings.includeInstructions,
            onCheckedChange = { save(settings.copy(includeInstructions = it)) }
        )
        SettingsToggleRow(
            title = label("Append polite due reminder note", "নম্র বকেয়া স্মরণিকা নোট যুক্ত করুন"),
            checked = settings.includeReminder,
            onCheckedChange = { save(settings.copy(includeReminder = it)) }
        )

        var instructions by remember(settings.paymentInstructions) { mutableStateOf(settings.paymentInstructions) }
        var bank by remember(settings.bankDetails) { mutableStateOf(settings.bankDetails) }
        var terms by remember(settings.defaultPaymentTerms) { mutableStateOf(settings.defaultPaymentTerms) }

        OutlinedTextField(
            value = terms,
            onValueChange = { terms = it },
            label = { Text(label("Default payment terms", "ডিফল্ট পেমেন্ট শর্তাবলি")) },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = instructions,
            onValueChange = { instructions = it },
            label = { Text(label("Payment instructions (bKash / Nagad / Cash)", "পেমেন্ট নির্দেশনা (বিকাশ / নগদ / ক্যাশ)")) },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = bank,
            onValueChange = { bank = it },
            label = { Text(label("Bank account details", "ব্যাংক অ্যাকাউন্টের তথ্য")) },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = { save(settings.copy(paymentInstructions = instructions, bankDetails = bank, defaultPaymentTerms = terms)) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(label("Save instructions & terms", "নির্দেশনা ও শর্তাবলি সংরক্ষণ করুন"))
        }
    }

    // 5. Local Export Cache & Share History Management
    val cacheKb = cacheStats.totalBytes / 1024L
    SettingsSectionCard(
        title = label("Local Cache & Share History", "লোকাল ক্যাশ ও শেয়ার ইতিহাস"),
        subtitle = label(
            "Saved PNGs remain in Pictures/AdvanceERP and PDFs in Downloads/AdvanceERP. Manage temporary preview cache or history logs here.",
            "সংরক্ষিত PNG ও PDF যথাক্রমে Pictures/AdvanceERP ও Downloads/AdvanceERP ফোল্ডারে থাকে। এখান থেকে অস্থায়ী ক্যাশ বা লগ পরিষ্কার করা যাবে।"
        )
    ) {
        Text(
            text = label(
                "Temporary generated files: ${cacheStats.fileCount} file(s) ($cacheKb KB)",
                "অস্থায়ী তৈরি ফাইল: ${cacheStats.fileCount}টি ($cacheKb KB)"
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = label(
                "Recorded communication log entries: $historyCount",
                "সংরক্ষিত যোগাযোগ লগ: $historyCount টি"
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onClearCache,
                enabled = cacheStats.fileCount > 0
            ) {
                Icon(Icons.Outlined.DeleteOutline, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(label("Clear Export Cache", "এক্সপোর্ট ক্যাশ পরিষ্কার"))
            }
            OutlinedButton(
                onClick = { confirmClearHistory = true },
                enabled = historyCount > 0,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError)
            ) {
                Icon(Icons.Outlined.DeleteOutline, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(label("Clear Share History", "শেয়ার ইতিহাস মুছুন"))
            }
        }
    }

    if (confirmClearHistory) {
        AlertDialog(
            onDismissRequest = { confirmClearHistory = false },
            title = { Text(label("Clear share history?", "শেয়ার ইতিহাস মুছে ফেলবেন?")) },
            text = {
                Text(
                    label(
                        "This removes local share/save log entries only. Invoices, payments, balances, and saved files in Downloads or Gallery remain intact.",
                        "এতে শুধু স্থানীয় শেয়ার লগ মুছবে। আপনার ইনভয়েস, পেমেন্ট, লেজার বা Downloads/Gallery-তে সেভ করা ফাইল অক্ষত থাকবে।"
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onClearHistory()
                    confirmClearHistory = false
                }) {
                    Text(label("Clear History", "ইতিহাস মুছুন"), color = StatusError)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearHistory = false }) {
                    Text(label("Cancel", "বাতিল"))
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color = ERPSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PrimaryPurple)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            content()
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
