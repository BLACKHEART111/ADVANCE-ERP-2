package com.pocketdev.advance_erp.feature.settings

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DesktopWindows
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockReset
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.pocketdev.advance_erp.R
import com.pocketdev.advance_erp.core.database.AdvanceErpDatabase
import com.pocketdev.advance_erp.core.designsystem.BrandConfig
import com.pocketdev.advance_erp.core.designsystem.ERPActionButton
import com.pocketdev.advance_erp.core.designsystem.LocalSettingsTokens
import com.pocketdev.advance_erp.core.designsystem.PrimaryPurple
import com.pocketdev.advance_erp.core.designsystem.SecondaryTeal
import com.pocketdev.advance_erp.core.designsystem.StatusSuccess
import com.pocketdev.advance_erp.core.designsystem.TextMuted
import com.pocketdev.advance_erp.core.designsystem.TextPrimary
import com.pocketdev.advance_erp.core.designsystem.TextSecondary
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.model.CurrencyCode
import com.pocketdev.advance_erp.core.security.BiometricHelper
import com.pocketdev.advance_erp.core.util.AppMediaStorage
import com.pocketdev.advance_erp.core.util.CompanyBrandingHelper
import androidx.compose.material.icons.outlined.Draw
import com.pocketdev.advance_erp.feature.settings.components.BackupManagementSection
import com.pocketdev.advance_erp.feature.settings.components.ChangePinDialog
import com.pocketdev.advance_erp.feature.settings.components.DisablePinDialog
import com.pocketdev.advance_erp.feature.settings.components.PremiumCurrencyRow
import com.pocketdev.advance_erp.feature.settings.components.PremiumLanguageRow
import com.pocketdev.advance_erp.feature.settings.components.PremiumSettingsHeader
import com.pocketdev.advance_erp.feature.settings.components.SetPinDialog
import com.pocketdev.advance_erp.feature.settings.components.SettingsAccordionCard
import com.pocketdev.advance_erp.feature.settings.components.ThemeSegmentedSelector
import com.pocketdev.advance_erp.feature.settings.components.validateBusinessProfileName
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val tokens = LocalSettingsTokens.current
    val isBn = state.language == AppLanguage.BANGLA
    val coroutineScope = rememberCoroutineScope()

    // Accordion expanded state — all 5 categories collapsed by default,
    // multiple categories can remain open concurrently, preserved across config changes.
    var expandAppearance by rememberSaveable { mutableStateOf(false) }
    var expandBusinessProfile by rememberSaveable { mutableStateOf(false) }
    var expandSecurity by rememberSaveable { mutableStateOf(false) }
    var expandBackup by rememberSaveable { mutableStateOf(false) }
    var expandSystemInfo by rememberSaveable { mutableStateOf(false) }

    // Security dialogs
    var showSetPinDialog by rememberSaveable { mutableStateOf(false) }
    var showChangePinDialog by rememberSaveable { mutableStateOf(false) }
    var showDisablePinDialog by rememberSaveable { mutableStateOf(false) }

    // Currency change confirmation dialog
    var pendingCurrencyConfirm by remember { mutableStateOf<CurrencyCode?>(null) }

    // Business Profile state preserved across configuration changes & unsaved edits
    var profileInitialized by rememberSaveable { mutableStateOf(false) }
    var companyName by rememberSaveable { mutableStateOf("") }
    var taxNumber by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var companyNameError by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.profile) {
        val loaded = state.profile
        if (loaded != null && !profileInitialized) {
            companyName = loaded.companyName
            taxNumber = loaded.taxNumber.orEmpty()
            phone = loaded.phone
            email = loaded.email.orEmpty()
            address = loaded.address
            profileInitialized = true
        }
    }

    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                try {
                    val savedPath = AppMediaStorage.saveCompanyLogo(context, it)
                    viewModel.updateCompanyLogo(savedPath)
                } catch (_: Exception) {
                    snackbarHostState.showSnackbar(
                        if (isBn) "লোগো সংরক্ষণ করা যায়নি" else "Could not save selected logo"
                    )
                }
            }
        }
    }

    val appVersionInfo = remember(context) {
        try {
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            val versionName = pInfo.versionName ?: BrandConfig.BRAND_VERSION
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode.toLong()
            }
            "v$versionName (Build $versionCode)"
        } catch (_: Exception) {
            "v${BrandConfig.BRAND_VERSION}"
        }
    }

    LaunchedEffect(state.userMessage) {
        state.userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(tokens.pageBgTop, tokens.pageBgBottom)
                    )
                )
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 28.dp)
            ) {
                // Premium Decorative Settings Header
                item(key = "settings_header") {
                    PremiumSettingsHeader(
                        language = state.language,
                        tokens = tokens
                    )
                }

                // Category 1: Appearance & Language
                item(key = "cat_appearance") {
                    SettingsAccordionCard(
                        title = if (isBn) "চেহারা ও ভাষা" else "Appearance & Language",
                        subtitle = if (isBn) "থিম, বাংলা/English, মুদ্রা" else "Theme, বাংলা/English, Currency",
                        icon = Icons.Outlined.Palette,
                        expanded = expandAppearance,
                        onToggle = { expandAppearance = !expandAppearance },
                        tokens = tokens,
                        testTag = "settings_cat_appearance",
                        modifier = Modifier.padding(horizontal = 14.dp)
                    ) {
                        // A. App Theme
                        SubSectionTitle(
                            icon = Icons.Outlined.DesktopWindows,
                            title = if (isBn) "অ্যাপের থিম" else "App Theme",
                            iconTint = tokens.subSectionIconTint,
                            titleColor = tokens.titleText
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        ThemeSegmentedSelector(
                            selectedMode = state.themeMode,
                            language = state.language,
                            tokens = tokens,
                            onSelectMode = { mode -> viewModel.onThemeSelected(mode) }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // B. Language Selection
                        SubSectionTitle(
                            icon = Icons.Outlined.Language,
                            title = if (isBn) "ভাষা / Language" else "Language / ভাষা",
                            iconTint = tokens.subSectionIconTint,
                            titleColor = tokens.titleText
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            AppLanguage.entries.forEach { lang ->
                                PremiumLanguageRow(
                                    languageOption = lang,
                                    selected = state.language == lang,
                                    tokens = tokens,
                                    onClick = { viewModel.onLanguageSelected(lang) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // C. Primary Operational Currency
                        SubSectionTitle(
                            icon = Icons.Outlined.Payments,
                            title = if (isBn) "মূল মুদ্রা (কারেন্সি)" else "Primary Currency",
                            iconTint = tokens.subSectionIconTint,
                            titleColor = tokens.titleText
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            CurrencyCode.entries.forEach { curr ->
                                PremiumCurrencyRow(
                                    currency = curr,
                                    displayLanguage = state.language,
                                    selected = state.currency == curr,
                                    tokens = tokens,
                                    onClick = {
                                        if (state.currency != curr) {
                                            pendingCurrencyConfirm = curr
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Category 2: Business Profile
                item(key = "cat_profile") {
                    SettingsAccordionCard(
                        title = if (isBn) "ব্যবসায়িক প্রোফাইল" else "Business Profile",
                        subtitle = if (isBn) "কোম্পানি লোগো, নাম, ফোন, ঠিকানা" else "Company logo, name, phone, address",
                        icon = Icons.Outlined.Business,
                        expanded = expandBusinessProfile,
                        onToggle = { expandBusinessProfile = !expandBusinessProfile },
                        tokens = tokens,
                        testTag = "settings_cat_profile",
                        modifier = Modifier.padding(horizontal = 14.dp)
                    ) {
                        Text(
                            text = if (isBn) "কোম্পানি লোগো (ইনভয়েস ও স্টেটমেন্টে দৃশ্যমান)" else "Company Logo (Visible on invoices & statements)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = tokens.titleText
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = tokens.innerPanelBg,
                            border = BorderStroke(1.dp, tokens.innerPanelBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val logoPath = state.companyLogoPath
                                val hasLogo = !logoPath.isNullOrBlank() && File(logoPath).exists()

                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .border(1.dp, PrimaryPurple.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (hasLogo) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(File(logoPath!!))
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = "Company Logo",
                                            modifier = Modifier
                                                .size(60.dp)
                                                .padding(4.dp)
                                        )
                                    } else {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                Icons.Outlined.Business,
                                                contentDescription = null,
                                                tint = TextMuted,
                                                modifier = Modifier.size(28.dp)
                                            )
                                            Text(
                                                text = if (isBn) "লোগো নেই" else "No Logo",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextMuted,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    OutlinedButton(
                                        onClick = {
                                            logoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            Icons.Outlined.AddPhotoAlternate,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = PrimaryPurple
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (hasLogo) {
                                                if (isBn) "লোগো পরিবর্তন করুন" else "Change Logo"
                                            } else {
                                                if (isBn) "লোগো নির্বাচন করুন" else "Select Logo"
                                            },
                                            fontSize = 12.5.sp,
                                            color = PrimaryPurple,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    if (hasLogo) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        OutlinedButton(
                                            onClick = { viewModel.updateCompanyLogo(null) },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                Icons.Outlined.DeleteOutline,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isBn) "লোগো সরান" else "Remove Logo",
                                                fontSize = 12.5.sp,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = if (isBn) "পিডিএফ রিপোর্ট ও ইনভয়েসের শীর্ষে এই লোগোটি প্রিন্ট হবে।" else "Displayed on PDF invoices and ledger statements.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = tokens.subtitleText
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Authorized Signature Section (Drawn on Mobile, used across PDFs)
                        var signatureRevision by remember { mutableStateOf(0) }
                        var showSignaturePadDialog by rememberSaveable { mutableStateOf(false) }
                        val signatureBitmap = remember(signatureRevision) {
                            CompanyBrandingHelper.loadAuthorizedSignatureBitmap(context)
                        }

                        Text(
                            text = if (isBn) "অথরাইজড স্বাক্ষর (ইনভয়েস, পিও ও রসিদে ব্যবহৃত হবে)" else "Authorized Signature (Visible on Invoices, POs & Receipts)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = tokens.titleText
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = tokens.innerPanelBg,
                            border = BorderStroke(1.dp, tokens.innerPanelBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(104.dp)
                                        .height(64.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.White)
                                        .border(1.dp, PrimaryPurple.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (signatureBitmap != null && !signatureBitmap.isRecycled) {
                                        Image(
                                            bitmap = signatureBitmap.asImageBitmap(),
                                            contentDescription = "Authorized Signature",
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(6.dp)
                                        )
                                    } else {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                Icons.Outlined.Draw,
                                                contentDescription = null,
                                                tint = TextMuted,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Text(
                                                text = if (isBn) "স্বাক্ষর নেই" else "No Signature",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextMuted,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    OutlinedButton(
                                        onClick = { showSignaturePadDialog = true },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            Icons.Outlined.Draw,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = PrimaryPurple
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (signatureBitmap != null) {
                                                if (isBn) "স্বাক্ষর পরিবর্তন করুন" else "Redraw Signature"
                                            } else {
                                                if (isBn) "মোবাইলে স্বাক্ষর করুন" else "Sign on Mobile"
                                            },
                                            fontSize = 12.5.sp,
                                            color = PrimaryPurple,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    if (signatureBitmap != null) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        OutlinedButton(
                                            onClick = {
                                                CompanyBrandingHelper.clearAuthorizedSignature(context)
                                                signatureRevision++
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        if (isBn) "স্বাক্ষর সরানো হয়েছে।" else "Authorized signature removed."
                                                    )
                                                }
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                Icons.Outlined.DeleteOutline,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isBn) "স্বাক্ষর মুছুন" else "Remove Signature",
                                                fontSize = 12.5.sp,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = if (isBn) "সংরক্ষিত স্বাক্ষর সকল Authorized Signature স্থানে স্বয়ংক্রিয়ভাবে বসবে।" else "Automatically printed above Authorized Signature lines in PDFs.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = tokens.subtitleText
                                    )
                                }
                            }
                        }

                        if (showSignaturePadDialog) {
                            AuthorizedSignaturePadDialog(
                                isBn = isBn,
                                onDismiss = { showSignaturePadDialog = false },
                                onSave = { bmp ->
                                    CompanyBrandingHelper.saveAuthorizedSignatureBitmap(context, bmp)
                                    signatureRevision++
                                    showSignaturePadDialog = false
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            if (isBn) "অথরাইজড স্বাক্ষর সংরক্ষিত হয়েছে।" else "Authorized signature saved."
                                        )
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = tokens.dividerColor)
                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = companyName,
                            onValueChange = {
                                companyName = it
                                if (companyNameError && validateBusinessProfileName(it)) {
                                    companyNameError = false
                                }
                            },
                            label = { Text(stringResource(R.string.company_name)) },
                            isError = companyNameError,
                            supportingText = if (companyNameError) {
                                {
                                    Text(
                                        text = if (isBn) "কোম্পানির নাম আবশ্যক" else "Company name is required",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            } else null,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = taxNumber,
                            onValueChange = { taxNumber = it },
                            label = { Text(stringResource(R.string.tax_id)) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text(stringResource(R.string.contact_phone)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text(if (isBn) "ব্যবসায়িক ইমেইল (ঐচ্ছিক)" else "Business Email (Optional)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text(stringResource(R.string.contact_address)) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 3
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        ERPActionButton(
                            text = stringResource(R.string.save_changes),
                            onClick = {
                                if (!validateBusinessProfileName(companyName)) {
                                    companyNameError = true
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            if (isBn) "অনুগ্রহ করে কোম্পানির নাম লিখুন" else "Please enter a company name"
                                        )
                                    }
                                } else {
                                    companyNameError = false
                                    viewModel.updateProfile(
                                        name = companyName,
                                        taxNumber = taxNumber,
                                        phone = phone,
                                        email = email,
                                        address = address
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Category 3: Security & App Lock
                item(key = "cat_security") {
                    SettingsAccordionCard(
                        title = if (isBn) "নিরাপত্তা ও অ্যাপ লক" else "Security & App Lock",
                        subtitle = if (isBn) "PIN ও Fingerprint" else "PIN & Fingerprint protection",
                        icon = Icons.Outlined.Shield,
                        expanded = expandSecurity,
                        onToggle = { expandSecurity = !expandSecurity },
                        tokens = tokens,
                        testTag = "settings_cat_security",
                        modifier = Modifier.padding(horizontal = 14.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = tokens.innerPanelBg,
                            border = BorderStroke(1.dp, tokens.innerPanelBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // PIN Lock Toggle Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Lock,
                                            contentDescription = null,
                                            tint = if (state.isAppLockEnabled) PrimaryPurple else tokens.subtitleText,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = if (isBn) "পিন কোড লক (৪–৮ ডিজিট)" else "PIN Code Lock (4-8 Digits)",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = if (state.isAppLockEnabled) {
                                                    if (isBn) "সক্রিয় • ৪ থেকে ৮ ডিজিটের পিন সক্রিয়" else "Active • 4-8 Digit PIN enabled"
                                                } else {
                                                    if (isBn) "নিষ্ক্রিয় • অ্যাপে প্রবেশের জন্য পিন লাগবে না" else "Disabled • No PIN required"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (state.isAppLockEnabled) StatusSuccess else TextMuted
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Switch(
                                        checked = state.isAppLockEnabled,
                                        onCheckedChange = { enable ->
                                            if (enable) {
                                                showSetPinDialog = true
                                            } else {
                                                showDisablePinDialog = true
                                            }
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = PrimaryPurple
                                        )
                                    )
                                }

                                if (state.isAppLockEnabled) {
                                    HorizontalDivider(color = tokens.dividerColor)

                                    OutlinedButton(
                                        onClick = { showChangePinDialog = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.LockReset,
                                            contentDescription = null,
                                            modifier = Modifier.size(17.dp),
                                            tint = PrimaryPurple
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isBn) "পিন কোড পরিবর্তন করুন" else "Change PIN Code",
                                            fontWeight = FontWeight.SemiBold,
                                            color = PrimaryPurple,
                                            fontSize = 13.sp
                                        )
                                    }

                                    if (state.isBiometricSupported) {
                                        HorizontalDivider(color = tokens.dividerColor)

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Fingerprint,
                                                    contentDescription = null,
                                                    tint = if (state.isBiometricEnabled) SecondaryTeal else tokens.subtitleText,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(
                                                        text = if (isBn) "ফিঙ্গারপ্রিন্ট / বায়োমেট্রিক আনলক" else "Fingerprint / Biometric Unlock",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = TextPrimary
                                                    )
                                                    Text(
                                                        text = if (state.isBiometricEnabled) {
                                                            if (isBn) "সক্রিয় • ফিঙ্গারপ্রিন্ট দিয়ে দ্রুত আনলক" else "Enabled • Unlock instantly with fingerprint"
                                                        } else {
                                                            if (isBn) "নিষ্ক্রিয়" else "Disabled"
                                                        },
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (state.isBiometricEnabled) SecondaryTeal else TextMuted
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            Switch(
                                                checked = state.isBiometricEnabled,
                                                onCheckedChange = { enable ->
                                                    if (enable && context is FragmentActivity) {
                                                        BiometricHelper.showBiometricPrompt(
                                                            activity = context,
                                                            title = if (isBn) "ফিঙ্গারপ্রিন্ট নিশ্চিতকরণ" else "Confirm Fingerprint",
                                                            subtitle = if (isBn) "সক্রিয় করতে আঙুলের ছাপ দিন" else "Touch sensor to enable biometrics",
                                                            onAuthenticated = { viewModel.toggleBiometric(true) },
                                                            onError = { err ->
                                                                coroutineScope.launch {
                                                                    snackbarHostState.showSnackbar(err)
                                                                }
                                                            }
                                                        )
                                                    } else {
                                                        viewModel.toggleBiometric(false)
                                                    }
                                                },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = Color.White,
                                                    checkedTrackColor = SecondaryTeal
                                                )
                                            )
                                        }
                                    } else {
                                        HorizontalDivider(color = tokens.dividerColor)
                                        Text(
                                            text = if (isBn) "এই ডিভাইসে বায়োমেট্রিক/ফিঙ্গারপ্রিন্ট সেটআপ পাওয়া যায়নি।"
                                            else "Biometric fingerprint hardware is not enrolled or supported on this device.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Category 4: Data Backup & Restore
                item(key = "cat_backup") {
                    SettingsAccordionCard(
                        title = if (isBn) "ডাটা ব্যাকআপ ও রিস্টোর" else "Data Backup & Restore",
                        subtitle = if (isBn) "ম্যানুয়াল, অটো ব্যাকআপ ও টেস্ট ডাটা রিসেট" else "Manual, auto backup & test data reset",
                        icon = Icons.Outlined.Storage,
                        expanded = expandBackup,
                        onToggle = { expandBackup = !expandBackup },
                        tokens = tokens,
                        testTag = "settings_cat_backup",
                        modifier = Modifier.padding(horizontal = 14.dp)
                    ) {
                        BackupManagementSection(
                            backupRecords = state.backupRecords,
                            isFridayAutoBackupEnabled = state.isFridayAutoBackupEnabled,
                            lastAutoBackupTime = state.lastAutoBackupTime,
                            backupFolderUri = state.backupFolderUri,
                            language = state.language,
                            onCreateBackup = { onFinished -> viewModel.createInstantBackup(onFinished) },
                            onRestoreBackupFile = { file -> viewModel.restoreBackup(file) },
                            onRestoreFromUri = { uri -> viewModel.restoreFromUri(uri) },
                            onShareBackup = { file -> viewModel.shareBackup(file) },
                            onDeleteBackup = { recordId -> viewModel.deleteBackup(recordId) },
                            onToggleFridayAutoBackup = { enabled -> viewModel.toggleFridayAutoBackup(enabled) },
                            onSelectBackupFolder = { uri -> viewModel.setBackupFolderUri(uri) },
                            onResetTestDataKeepMasters = { onFinished -> viewModel.resetTestDataKeepProductsAndContacts(onFinished) },
                            embeddedInAccordion = true
                        )
                    }
                }

                // Category 5: System Information (Dynamic & Read-only)
                item(key = "cat_system") {
                    SettingsAccordionCard(
                        title = if (isBn) "সিস্টেম তথ্য" else "System Information",
                        subtitle = if (isBn) "অ্যাপ ভার্সন ও ডাটাবেজ তথ্য" else "App version & database diagnostics",
                        icon = Icons.Outlined.Info,
                        expanded = expandSystemInfo,
                        onToggle = { expandSystemInfo = !expandSystemInfo },
                        tokens = tokens,
                        testTag = "settings_cat_system",
                        modifier = Modifier.padding(horizontal = 14.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = tokens.innerPanelBg,
                            border = BorderStroke(1.dp, tokens.innerPanelBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                SystemInfoRow(
                                    label = if (isBn) "অ্যাপ ভার্সন" else "Application Version",
                                    value = appVersionInfo
                                )
                                HorizontalDivider(color = tokens.dividerColor)
                                SystemInfoRow(
                                    label = if (isBn) "সিস্টেম মোড" else "System Mode",
                                    value = if (isBn) "একক ব্যবহারকারী • অফলাইন-ফার্স্ট" else "Single User • Offline-First"
                                )
                                HorizontalDivider(color = tokens.dividerColor)
                                SystemInfoRow(
                                    label = if (isBn) "আর্কিটেকচার" else "Architecture",
                                    value = "Room SQLite + Hilt + Compose M3"
                                )
                                HorizontalDivider(color = tokens.dividerColor)
                                SystemInfoRow(
                                    label = if (isBn) "ডাটাবেজ ভার্সন" else "Database Version",
                                    value = "v${AdvanceErpDatabase.DATABASE_VERSION} (${AdvanceErpDatabase.DATABASE_NAME})"
                                )
                                HorizontalDivider(color = tokens.dividerColor)
                                SystemInfoRow(
                                    label = if (isBn) "ফরেন কী সুরক্ষা" else "Foreign Key Enforcement",
                                    value = if (isBn) "সক্রিয় (ON)" else "ENFORCED (PRAGMA ON)"
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Accounting-safe Currency Change Confirmation Dialog
    pendingCurrencyConfirm?.let { targetCurrency ->
        AlertDialog(
            onDismissRequest = { pendingCurrencyConfirm = null },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.WarningAmber,
                    contentDescription = null,
                    tint = SecondaryTeal
                )
            },
            title = {
                Text(
                    text = if (isBn) "মূল মুদ্রা পরিবর্তন নিশ্চিতকরণ" else "Confirm Primary Currency Change",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isBn) {
                        "মূল মুদ্রা ${targetCurrency.code} (${targetCurrency.symbol})-এ পরিবর্তন করলে এটি শুধু প্রাইমারি মুদ্রার পছন্দ হিসেবে সংরক্ষিত হবে।\n\nপূর্বে সংরক্ষিত কোনো ইনভয়েস, ক্রয় বা লেজার হিসাবের অংক স্বয়ংক্রিয়ভাবে রূপান্তর বা পরিবর্তন করা হবে না।"
                    } else {
                        "Changing primary currency to ${targetCurrency.code} (${targetCurrency.symbol}) updates your operational preference only.\n\nPreviously saved invoices, purchase orders, and ledger monetary amounts will NOT be converted or rewritten."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onCurrencySelected(targetCurrency)
                        pendingCurrencyConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal)
                ) {
                    Text(if (isBn) "নিশ্চিত করুন" else "Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingCurrencyConfirm = null }) {
                    Text(if (isBn) "বাতিল" else "Cancel")
                }
            }
        )
    }

    if (showSetPinDialog) {
        SetPinDialog(
            language = state.language,
            onPinSet = { pin -> viewModel.setAppLock(pin) },
            onDismiss = { showSetPinDialog = false }
        )
    }

    if (showChangePinDialog) {
        ChangePinDialog(
            storedHash = state.pinCodeHash,
            storedSalt = state.pinSalt,
            language = state.language,
            onPinChanged = { newPin -> viewModel.changePin(newPin) },
            onDismiss = { showChangePinDialog = false }
        )
    }

    if (showDisablePinDialog) {
        DisablePinDialog(
            storedHash = state.pinCodeHash,
            storedSalt = state.pinSalt,
            language = state.language,
            onPinVerifiedAndDisabled = { viewModel.disableAppLock() },
            onDismiss = { showDisablePinDialog = false }
        )
    }
}

@Composable
private fun SubSectionTitle(
    icon: ImageVector,
    title: String,
    iconTint: Color,
    titleColor: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(19.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            ),
            color = titleColor
        )
    }
}

@Composable
private fun SystemInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            color = TextSecondary,
            modifier = Modifier.weight(0.42f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.58f)
        )
    }
}

@Composable
private fun AuthorizedSignaturePadDialog(
    isBn: Boolean,
    onDismiss: () -> Unit,
    onSave: (Bitmap) -> Unit
) {
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    var activeStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var canvasWidthPx by remember { mutableStateOf(1f) }
    var canvasHeightPx by remember { mutableStateOf(1f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isBn) "অথরাইজড স্বাক্ষর আঁকুন" else "Draw Authorized Signature",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (isBn) "নিচের সাদা বক্সে আঙুল দিয়ে আপনার স্বাক্ষর দিন:" else "Sign inside the white box below using your finger:",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(1.5.dp, PrimaryPurple.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { start ->
                                        activeStroke = listOf(start)
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        activeStroke = activeStroke + change.position
                                    },
                                    onDragEnd = {
                                        if (activeStroke.size >= 2) {
                                            strokes.add(activeStroke)
                                        }
                                        activeStroke = emptyList()
                                    },
                                    onDragCancel = {
                                        activeStroke = emptyList()
                                    }
                                )
                            }
                    ) {
                        canvasWidthPx = size.width.coerceAtLeast(1f)
                        canvasHeightPx = size.height.coerceAtLeast(1f)

                        fun drawPointList(pts: List<Offset>) {
                            if (pts.size < 2) return
                            val path = Path().apply {
                                moveTo(pts.first().x, pts.first().y)
                                for (i in 1 until pts.size) {
                                    lineTo(pts[i].x, pts[i].y)
                                }
                            }
                            drawPath(
                                path = path,
                                color = Color(0xFF0F172A),
                                style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }

                        strokes.forEach { drawPointList(it) }
                        drawPointList(activeStroke)
                    }
                }
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = {
                        strokes.clear()
                        activeStroke = emptyList()
                    }
                ) {
                    Text(if (isBn) "মুছুন" else "Clear")
                }
                TextButton(onClick = onDismiss) {
                    Text(if (isBn) "বাতিল" else "Cancel")
                }
            }
        },
        confirmButton = {
            Button(
                enabled = strokes.isNotEmpty(),
                onClick = {
                    val outW = 600
                    val outH = 240
                    val bmp = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
                    val c = AndroidCanvas(bmp)
                    val p = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.parseColor("#0F172A")
                        style = AndroidPaint.Style.STROKE
                        strokeWidth = 6.5f
                        strokeCap = AndroidPaint.Cap.ROUND
                        strokeJoin = AndroidPaint.Join.ROUND
                    }
                    val scaleX = outW.toFloat() / canvasWidthPx
                    val scaleY = outH.toFloat() / canvasHeightPx
                    strokes.forEach { pts ->
                        if (pts.size >= 2) {
                            val ap = AndroidPath().apply {
                                moveTo(pts.first().x * scaleX, pts.first().y * scaleY)
                                for (i in 1 until pts.size) {
                                    lineTo(pts[i].x * scaleX, pts[i].y * scaleY)
                                }
                            }
                            c.drawPath(ap, p)
                        }
                    }
                    onSave(bmp)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
            ) {
                Text(if (isBn) "সংরক্ষণ করুন" else "Save Signature")
            }
        }
    )
}
