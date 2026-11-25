package com.pocketdev.advance_erp.core.designsystem

import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FlightTakeoff
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.Warehouse
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.pocketdev.advance_erp.R
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.security.UserRole
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.navigation.formatDrawerDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class DrawerDestination(
    val route: String,
    val icon: ImageVector,
    val labelResId: Int
) {
    DASHBOARD("dashboard", Icons.Outlined.GridView, R.string.nav_dashboard),
    PRODUCTS("products", Icons.Outlined.Category, R.string.nav_products),
    INVENTORY("inventory", Icons.Outlined.Inventory, R.string.nav_inventory),
    PURCHASES("purchases", Icons.Outlined.ShoppingCart, R.string.nav_purchases),
    IMPORT_MANAGEMENT("import_management", Icons.Outlined.Public, R.string.nav_import_management),
    SHIPMENTS("shipments", Icons.Outlined.FlightTakeoff, R.string.nav_shipments),
    IMPORT_VAULT("import_vault", Icons.Outlined.FolderSpecial, R.string.nav_import_vault),
    CUSTOMERS("customers", Icons.Outlined.Group, R.string.nav_customers),
    SUPPLIERS("suppliers", Icons.Outlined.Storefront, R.string.nav_suppliers),
    SALES("sales", Icons.Outlined.ShoppingBag, R.string.nav_sales),
    ACCOUNTS("accounts", Icons.Outlined.AccountBalance, R.string.nav_accounts),
    LEDGER("ledger", Icons.AutoMirrored.Outlined.MenuBook, R.string.nav_ledger),
    REPORTS("reports", Icons.Outlined.Assessment, R.string.nav_reports),
    SETTINGS("settings", Icons.Outlined.Settings, R.string.nav_settings)
}

internal val drawerPrimaryMenuItems: List<DrawerDestination> = listOf(
    DrawerDestination.DASHBOARD,
    DrawerDestination.PRODUCTS,
    DrawerDestination.INVENTORY,
    DrawerDestination.PURCHASES,
    DrawerDestination.IMPORT_MANAGEMENT,
    DrawerDestination.SHIPMENTS,
    DrawerDestination.IMPORT_VAULT,
    DrawerDestination.CUSTOMERS,
    DrawerDestination.SUPPLIERS,
    DrawerDestination.SALES,
    DrawerDestination.ACCOUNTS,
    DrawerDestination.LEDGER,
    DrawerDestination.SETTINGS
)

internal fun visibleDrawerMenuItems(userRole: UserRole): List<DrawerDestination> {
    return if (com.pocketdev.advance_erp.core.security.SecurityManager.canAccessImportVault(userRole)) {
        drawerPrimaryMenuItems
    } else {
        drawerPrimaryMenuItems.filter { it != DrawerDestination.IMPORT_VAULT }
    }
}

internal fun isDrawerDestinationSelected(
    destination: DrawerDestination,
    currentRoute: String
): Boolean {
    return when {
        destination == DrawerDestination.LEDGER ->
            currentRoute == "ledger" || currentRoute == "reports"
        else -> currentRoute == destination.route
    }
}

internal fun usesTealIconAccent(destination: DrawerDestination): Boolean {
    return destination == DrawerDestination.INVENTORY ||
        destination == DrawerDestination.SHIPMENTS ||
        destination == DrawerDestination.CUSTOMERS
}

internal fun drawerDestinationLabel(
    destination: DrawerDestination,
    language: AppLanguage
): String {
    val isBn = language == AppLanguage.BANGLA
    return when (destination) {
        DrawerDestination.DASHBOARD -> if (isBn) "ড্যাশবোর্ড" else "Dashboard"
        DrawerDestination.PRODUCTS -> if (isBn) "পণ্যসমূহ" else "Products"
        DrawerDestination.INVENTORY -> if (isBn) "ইনভেন্টরি" else "Inventory"
        DrawerDestination.PURCHASES -> if (isBn) "ক্রয়" else "Purchase"
        DrawerDestination.IMPORT_MANAGEMENT -> if (isBn) "আমদানি ব্যবস্থাপনা" else "Import Management"
        DrawerDestination.SHIPMENTS -> if (isBn) "চালানসমূহ" else "Delivery Challans"
        DrawerDestination.IMPORT_VAULT -> if (isBn) "আমদানি আর্কাইভ" else "Import Vault"
        DrawerDestination.CUSTOMERS -> if (isBn) "গ্রাহকবৃন্দ" else "Customers"
        DrawerDestination.SUPPLIERS -> if (isBn) "সরবরাহকারী" else "Suppliers"
        DrawerDestination.SALES -> if (isBn) "বিক্রয়" else "Sales"
        DrawerDestination.ACCOUNTS -> if (isBn) "হিসাব ও লেজার" else "Accounting & Ledger"
        DrawerDestination.LEDGER, DrawerDestination.REPORTS ->
            if (isBn) "খতিয়ান ও রিপোর্ট" else "Reports & Statements"
        DrawerDestination.SETTINGS -> if (isBn) "সেটিংস" else "Settings"
    }
}

@Composable
fun ERPDrawer(
    currentRoute: String,
    onNavigate: (DrawerDestination) -> Unit,
    userName: String = "",
    profilePhotoUri: String? = null,
    onPhotoSelected: (String?) -> Unit = {},
    onUserNameChanged: (String) -> Unit = {},
    onSyncLogout: () -> Unit = {},
    onEditProfile: () -> Unit = {},
    language: AppLanguage = AppLanguage.BANGLA,
    userRole: UserRole = UserRole.OWNER_ADMIN,
    activeWarehouseName: String? = null,
    hasActiveWarehouse: Boolean = false,
    productCount: Int = 0,
    customerCount: Int = 0,
    supplierCount: Int = 0,
    isSyncing: Boolean = false,
    appVersion: String = "",
    onSyncClick: () -> Unit = onSyncLogout,
    onLogoutClick: () -> Unit = onSyncLogout,
    modifier: Modifier = Modifier
) {
    val tokens = LocalDrawerTokens.current
    val context = LocalContext.current

    val resolvedVersion = remember(appVersion, context) {
        val raw = appVersion.ifBlank {
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
                pInfo.versionName ?: BrandConfig.BRAND_VERSION
            } catch (_: Exception) {
                BrandConfig.BRAND_VERSION
            }
        }
        val clean = raw.removePrefix("v").removePrefix("V").trim()
        "v${clean.ifEmpty { BrandConfig.BRAND_VERSION }}"
    }

    val drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)

    ModalDrawerSheet(
        modifier = modifier
            .fillMaxWidth(0.86f)
            .widthIn(min = 288.dp, max = 360.dp),
        drawerShape = drawerShape,
        drawerContainerColor = tokens.drawerBackground,
        windowInsets = WindowInsets(0, 0, 0, 0)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(drawerShape)
                .background(tokens.drawerBackground)
                .border(
                    width = if (tokens.isDark) 1.2.dp else 0.8.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            tokens.drawerEdgeGlowStart,
                            tokens.drawerEdgeGlowEnd,
                            tokens.drawerEdgeGlowStart.copy(alpha = if (tokens.isDark) 0.55f else 0.35f)
                        )
                    ),
                    shape = drawerShape
                )
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header + Overlapping Wave Profile Section
                PremiumDrawerHeaderAndProfile(
                    userName = userName,
                    profilePhotoUri = profilePhotoUri,
                    language = language,
                    userRole = userRole,
                    isSyncing = isSyncing,
                    onPhotoSelected = onPhotoSelected,
                    onUserNameChanged = onUserNameChanged,
                    onSyncClick = onSyncClick,
                    onLogoutClick = onLogoutClick,
                    onEditProfile = onEditProfile
                )

                // 3 Horizontally Aligned Business Overview Mini Cards
                DrawerOverviewCardsRow(
                    activeWarehouseName = activeWarehouseName,
                    hasActiveWarehouse = hasActiveWarehouse,
                    productCount = productCount,
                    customerCount = customerCount,
                    language = language,
                    tokens = tokens
                )

                // Smooth Scrollable Main Navigation Menu
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val visibleMenuItems = visibleDrawerMenuItems(userRole)
                    visibleMenuItems.forEachIndexed { index, destination ->
                        val isSelected = isDrawerDestinationSelected(destination, currentRoute)
                        val countBadge: Int? = when (destination) {
                            DrawerDestination.CUSTOMERS -> customerCount
                            DrawerDestination.SUPPLIERS -> supplierCount
                            else -> null
                        }

                        PremiumDrawerMenuItemRow(
                            destination = destination,
                            label = drawerDestinationLabel(destination, language),
                            isSelected = isSelected,
                            countBadge = countBadge,
                            language = language,
                            showDivider = index < visibleMenuItems.lastIndex && !isSelected,
                            tokens = tokens,
                            onClick = { onNavigate(destination) }
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Branded Footer with Subtle Wave Artwork & Dynamic Version Badge
                PremiumDrawerFooter(
                    versionLabel = resolvedVersion,
                    tokens = tokens
                )
            }
        }
    }
}

@Composable
private fun PremiumDrawerHeaderAndProfile(
    userName: String,
    profilePhotoUri: String?,
    language: AppLanguage,
    userRole: UserRole,
    isSyncing: Boolean,
    onPhotoSelected: (String?) -> Unit,
    onUserNameChanged: (String) -> Unit,
    onSyncClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onEditProfile: () -> Unit
) {
    val tokens = LocalDrawerTokens.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showPhotoOptionsDialog by remember { mutableStateOf(false) }

    val isBn = language == AppLanguage.BANGLA
    val displayUserName = userName.ifBlank {
        if (isBn) "ব্যবসায়ী প্রোফাইল" else "Business Owner"
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val destFile = File(context.filesDir, "avatar_${System.currentTimeMillis()}.jpg")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        destFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    onPhotoSelected(destFile.absolutePath)
                } catch (_: Exception) {
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(tokens.profileBackground)
    ) {
        // Upper Gradient Header Box with Port Artwork & Overlapping Avatar
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Gradient Canvas Background
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(bottom = 22.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                tokens.headerGradientStart,
                                tokens.headerGradientMid,
                                tokens.headerGradientEnd
                            )
                        )
                    )
            ) {
                // China Import / Shipping Port Vector Artwork sitting below clock & above S-wave
                Image(
                    painter = painterResource(
                        id = if (tokens.isDark) R.drawable.bg_drawer_port_dark else R.drawable.bg_drawer_port_light
                    ),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 22.dp)
                        .fillMaxWidth(0.66f)
                        .height(82.dp)
                        .alpha(if (tokens.isDark) 0.94f else 0.92f)
                )

                // Top decorative purple-teal accent wave + lower S-wave separator
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Top subtle wave crest
                    val topCrest = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(w * 0.42f, 0f)
                        cubicTo(
                            w * 0.28f, h * 0.14f,
                            w * 0.12f, h * 0.15f,
                            0f, h * 0.08f
                        )
                        close()
                    }
                    drawPath(
                        path = topCrest,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                tokens.headerWavePrimary.copy(alpha = if (tokens.isDark) 0.45f else 0.55f),
                                tokens.headerWaveSecondary.copy(alpha = 0.05f)
                            )
                        )
                    )

                    // Smooth S-curve cutout filled with profileBackground at bottom of header
                    val waveCutout = Path().apply {
                        moveTo(0f, h * 0.62f)
                        cubicTo(
                            w * 0.30f, h * 0.58f,
                            w * 0.65f, h * 0.90f,
                            w, h * 0.76f
                        )
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }
                    drawPath(path = waveCutout, color = tokens.profileBackground)

                    // Gradient wave ribbon highlight tracing the S-curve
                    val waveLine = Path().apply {
                        moveTo(0f, h * 0.62f)
                        cubicTo(
                            w * 0.30f, h * 0.58f,
                            w * 0.65f, h * 0.90f,
                            w, h * 0.76f
                        )
                    }
                    drawPath(
                        path = waveLine,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                tokens.headerWavePrimary.copy(alpha = if (tokens.isDark) 0.85f else 0.75f),
                                tokens.headerWaveSecondary.copy(alpha = if (tokens.isDark) 0.85f else 0.80f)
                            )
                        ),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            // Foreground Header Contents (Brand + SYNC/Logout Pill + Live Clock + Profile Section)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 6.dp, start = 12.dp, end = 12.dp, bottom = 4.dp)
            ) {
                // Row 1: ADVANCE ERP Brand Logo + SYNC | Power Capsule Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        AdvanceErpLogo(
                            variant = BrandConfig.LogoVariant.SYMBOL,
                            isDarkBackground = tokens.isDark,
                            modifier = Modifier.size(31.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(
                                        SpanStyle(
                                            color = tokens.brandAdvanceText,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    ) {
                                        append("ADVANCE ")
                                    }
                                    withStyle(
                                        SpanStyle(
                                            color = tokens.brandErpText,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    ) {
                                        append("ERP")
                                    }
                                },
                                fontSize = 15.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = "Import Business Management",
                                fontSize = 8.5.sp,
                                letterSpacing = (-0.2).sp,
                                fontWeight = FontWeight.Medium,
                                color = tokens.brandSubtitleText,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // SYNC & Power Capsule Pill
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = tokens.syncPillBackground,
                        border = BorderStroke(1.dp, tokens.syncPillBorder),
                        shadowElevation = if (tokens.isDark) 0.dp else 2.dp
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.height(30.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp))
                                    .clickable(enabled = !isSyncing, onClick = onSyncClick)
                                    .padding(start = 8.dp, end = 6.dp, top = 5.dp, bottom = 5.dp)
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(13.dp),
                                        strokeWidth = 2.dp,
                                        color = tokens.headerWaveSecondary
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.CloudSync,
                                        contentDescription = "SYNC",
                                        tint = tokens.syncPillContent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "SYNC",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = tokens.syncPillContent,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(16.dp)
                                    .background(tokens.syncPillDivider)
                            )

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(topEnd = 50.dp, bottomEnd = 50.dp))
                                    .clickable(onClick = onLogoutClick)
                                    .padding(start = 7.dp, end = 8.dp, top = 5.dp, bottom = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.PowerSettingsNew,
                                    contentDescription = if (isBn) "লগ আউট" else "Logout",
                                    tint = tokens.syncPillContent,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Row 2: Inline Date (Left) & Compact Time Pill (Right)
                DrawerLiveClock(
                    language = language,
                    tokens = tokens
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Row 3: Premium Profile Section overlapping the S-curve boundary
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onEditProfile() }
                        .padding(vertical = 2.dp, horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Compact Circular User Avatar with floating Teal Edit badge
                    Box(modifier = Modifier.size(64.dp)) {
                        Surface(
                            modifier = Modifier
                                .size(60.dp)
                                .align(Alignment.BottomStart)
                                .border(2.5.dp, tokens.avatarBorder, CircleShape)
                                .clip(CircleShape)
                                .clickable { showPhotoOptionsDialog = true },
                            shape = CircleShape,
                            color = tokens.menuIconBgPurple,
                            shadowElevation = if (tokens.isDark) 4.dp else 6.dp
                        ) {
                            DrawerAvatarContent(
                                profilePhotoUri = profilePhotoUri,
                                displayName = displayUserName,
                                tokens = tokens
                            )
                        }

                        // Small floating Teal Edit Icon at top-right
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = (-2).dp, y = 2.dp)
                                .size(22.dp)
                                .background(tokens.avatarEditBadgeBg, CircleShape)
                                .border(1.5.dp, tokens.avatarEditBadgeBorder, CircleShape)
                                .clip(CircleShape)
                                .clickable { onEditProfile() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = if (isBn) "প্রোফাইল সম্পাদনা" else "Edit Profile",
                                tint = Color.White,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Display Name + Role Badge with Crown for Owner/Admin
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = displayUserName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = tokens.profileNameText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        DrawerRoleBadge(
                            userRole = userRole,
                            language = language,
                            tokens = tokens
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = if (isBn) "প্রোফাইল সেটিংস" else "Profile Settings",
                        tint = tokens.profileChevronTint,
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .size(20.dp)
                    )
                }
            }
        }
    }

    if (showPhotoOptionsDialog) {
        var nameInput by remember(userName) { mutableStateOf(userName) }

        AlertDialog(
            onDismissRequest = { showPhotoOptionsDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.drawer_profile_options),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier
                            .size(72.dp)
                            .border(2.dp, tokens.headerWaveSecondary, CircleShape),
                        shape = CircleShape,
                        color = tokens.menuIconBgPurple
                    ) {
                        DrawerAvatarContent(
                            profilePhotoUri = profilePhotoUri,
                            displayName = displayUserName,
                            tokens = tokens
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            showPhotoOptionsDialog = false
                            photoPickerLauncher.launch("image/*")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.drawer_select_photo))
                    }

                    if (!profilePhotoUri.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                onPhotoSelected(null)
                                showPhotoOptionsDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.drawer_remove_photo))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text(stringResource(R.string.drawer_edit_user_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (nameInput.isNotBlank()) {
                            onUserNameChanged(nameInput.trim())
                        }
                        showPhotoOptionsDialog = false
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            showPhotoOptionsDialog = false
                            onEditProfile()
                        }
                    ) {
                        Text(if (isBn) "বিস্তারিত এডিট" else "Full Edit")
                    }
                    TextButton(onClick = { showPhotoOptionsDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        )
    }
}

@Composable
private fun DrawerAvatarContent(
    profilePhotoUri: String?,
    displayName: String,
    tokens: DrawerColorTokens
) {
    val context = LocalContext.current
    val customFile = remember(profilePhotoUri) { profilePhotoUri?.let { File(it) } }
    if (customFile != null && customFile.exists()) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(customFile)
                .crossfade(true)
                .build(),
            contentDescription = displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    } else {
        val initials = remember(displayName) {
            displayName.trim()
                .split(Regex("\\s+"))
                .filter { it.isNotEmpty() }
                .take(2)
                .joinToString("") { it.take(1).uppercase() }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            tokens.selectedGradientStart,
                            tokens.headerWaveSecondary
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (initials.isNotEmpty()) {
                Text(
                    text = initials,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = displayName,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun DrawerRoleBadge(
    userRole: UserRole,
    language: AppLanguage,
    tokens: DrawerColorTokens
) {
    val isOwnerAdmin = userRole == UserRole.OWNER_ADMIN
    val roleLabel = if (language == AppLanguage.BANGLA && !isOwnerAdmin) {
        userRole.titleBn
    } else if (language == AppLanguage.BANGLA) {
        // Matches reference bilingual badge style while respecting role
        userRole.titleEn
    } else {
        userRole.titleEn
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = tokens.roleBadgeBg,
        border = BorderStroke(1.dp, tokens.roleBadgeBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (isOwnerAdmin) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_drawer_crown),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(13.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.VerifiedUser,
                    contentDescription = null,
                    tint = tokens.roleBadgeText,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = roleLabel,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = tokens.roleBadgeText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Isolated clock composable that aligns refreshes to minute rollovers so
 * the drawer header/menu do not recompose needlessly every second.
 */
@Composable
private fun DrawerLiveClock(
    language: AppLanguage,
    tokens: DrawerColorTokens
) {
    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            val current = System.currentTimeMillis()
            nowMillis = current
            val millisIntoMinute = current % 60_000L
            val waitMillis = (60_000L - millisIntoMinute).coerceAtLeast(250L)
            delay(waitMillis)
        }
    }

    val calendar = remember(nowMillis) {
        Calendar.getInstance().apply { timeInMillis = nowMillis }
    }
    val dateLabel = remember(calendar.get(Calendar.YEAR), calendar.get(Calendar.DAY_OF_YEAR), language) {
        formatDrawerDate(calendar, language)
    }
    val timeParts = remember(nowMillis) {
        val rawTime = SimpleDateFormat("hh:mm", Locale.ENGLISH).format(calendar.time)
        val amPm = SimpleDateFormat("a", Locale.ENGLISH).format(calendar.time).uppercase(Locale.ENGLISH)
        rawTime to amPm
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = tokens.clockIconTint,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = dateLabel,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = tokens.clockDateText,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = tokens.syncPillBackground.copy(alpha = if (tokens.isDark) 0.72f else 0.80f),
            border = BorderStroke(0.8.dp, tokens.syncPillBorder.copy(alpha = 0.7f))
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = timeParts.first,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp,
                    color = tokens.clockTimeText,
                    maxLines = 1,
                    softWrap = false
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = timeParts.second,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = tokens.clockDateText,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(bottom = 1.dp)
                )
            }
        }
    }
}

@Composable
private fun DrawerOverviewCardsRow(
    activeWarehouseName: String?,
    hasActiveWarehouse: Boolean,
    productCount: Int,
    customerCount: Int,
    language: AppLanguage,
    tokens: DrawerColorTokens
) {
    val isBn = language == AppLanguage.BANGLA
    val warehouseLabel = activeWarehouseName?.takeIf { it.isNotBlank() }
        ?: if (isBn) "কোনো গুদাম নেই" else "No Warehouse"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Card 1: Active Warehouse
        DrawerMiniOverviewCard(
            icon = Icons.Outlined.Warehouse,
            iconTint = tokens.miniCardIconTintPurple,
            tokens = tokens,
            modifier = Modifier
                .weight(1.22f)
                .fillMaxHeight()
        ) {
            Text(
                text = warehouseLabel,
                fontSize = 9.8.sp,
                lineHeight = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = tokens.miniCardTitle,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = if (hasActiveWarehouse) tokens.activeStatusBg else tokens.inactiveStatusBg
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.5.dp)
                            .clip(CircleShape)
                            .background(
                                if (hasActiveWarehouse) tokens.activeStatusDot else tokens.inactiveStatusDot
                            )
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (hasActiveWarehouse) {
                            if (isBn) "সক্রিয়" else "Active"
                        } else {
                            if (isBn) "নিষ্ক্রিয়" else "None"
                        },
                        fontSize = 9.2.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (hasActiveWarehouse) tokens.activeStatusText else tokens.inactiveStatusText,
                        maxLines = 1
                    )
                }
            }
        }

        // Card 2: Total Products
        DrawerMiniOverviewCard(
            icon = Icons.Outlined.Inventory2,
            iconTint = tokens.miniCardIconTintPurple,
            tokens = tokens,
            modifier = Modifier
                .weight(0.89f)
                .fillMaxHeight()
        ) {
            Text(
                text = if (isBn) "মোট পণ্য" else "Products",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = tokens.miniCardTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = Formatters.formatNumber(productCount.toLong(), language),
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = tokens.miniCardValue,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Card 3: Total Customers
        DrawerMiniOverviewCard(
            icon = Icons.Outlined.Group,
            iconTint = tokens.miniCardIconTintPurple,
            tokens = tokens,
            modifier = Modifier
                .weight(0.89f)
                .fillMaxHeight()
        ) {
            Text(
                text = if (isBn) "মোট গ্রাহক" else "Customers",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = tokens.miniCardTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = Formatters.formatNumber(customerCount.toLong(), language),
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = tokens.miniCardValue,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DrawerMiniOverviewCard(
    icon: ImageVector,
    iconTint: Color,
    tokens: DrawerColorTokens,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val cardShape = RoundedCornerShape(11.dp)
    Surface(
        modifier = modifier.defaultMinSize(minHeight = 48.dp),
        shape = cardShape,
        color = tokens.miniCardBg,
        border = BorderStroke(1.dp, tokens.miniCardBorder),
        shadowElevation = if (tokens.isDark) 0.dp else 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                content()
            }
        }
    }
}

@Composable
private fun PremiumDrawerMenuItemRow(
    destination: DrawerDestination,
    label: String,
    isSelected: Boolean,
    countBadge: Int?,
    language: AppLanguage,
    showDivider: Boolean,
    tokens: DrawerColorTokens,
    onClick: () -> Unit
) {
    val itemShape = RoundedCornerShape(14.dp)
    val isTealAccent = usesTealIconAccent(destination)
    val textColor by animateColorAsState(
        targetValue = if (isSelected) tokens.selectedContent else tokens.menuItemText,
        animationSpec = tween(durationMillis = 160),
        label = "drawerItemTextColor"
    )

    val rowBackgroundModifier = if (isSelected) {
        Modifier
            .then(
                if (!tokens.isDark) {
                    Modifier.shadow(
                        elevation = 5.dp,
                        shape = itemShape,
                        ambientColor = tokens.selectedGradientStart.copy(alpha = 0.35f),
                        spotColor = tokens.selectedGradientEnd.copy(alpha = 0.45f)
                    )
                } else {
                    Modifier
                }
            )
            .clip(itemShape)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        tokens.selectedGradientStart,
                        tokens.selectedGradientMid,
                        tokens.selectedGradientEnd
                    )
                )
            )
            .border(
                width = if (tokens.isDark) 1.2.dp else 0.8.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(tokens.selectedBorderStart, tokens.selectedBorderEnd)
                ),
                shape = itemShape
            )
    } else {
        Modifier.clip(itemShape)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(rowBackgroundModifier)
                .semantics { selected = isSelected }
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 4.5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Rounded Icon Container
            val iconContainerBg = when {
                isSelected -> Color.White.copy(alpha = 0.18f)
                isTealAccent -> tokens.menuIconBgTeal
                else -> tokens.menuIconBgPurple
            }
            val iconTint = when {
                isSelected -> tokens.selectedContent
                isTealAccent -> tokens.menuIconTintTeal
                else -> tokens.menuIconTintPurple
            }

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconContainerBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = destination.icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = label,
                fontSize = 13.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (countBadge != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isSelected) tokens.selectedCountBadgeBg else tokens.countBadgeBg,
                    border = if (isSelected) null else BorderStroke(0.9.dp, tokens.countBadgeBorder)
                ) {
                    Text(
                        text = Formatters.formatNumber(countBadge.toLong(), language),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) tokens.selectedCountBadgeText else tokens.countBadgeText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = if (isSelected) tokens.selectedContent else tokens.menuItemChevron,
                modifier = Modifier.size(18.dp)
            )
        }

        if (showDivider) {
            HorizontalDivider(
                color = tokens.menuDivider,
                thickness = 0.7.dp,
                modifier = Modifier.padding(start = 52.dp, end = 12.dp)
            )
        }
    }
}

@Composable
private fun PremiumDrawerFooter(
    versionLabel: String,
    tokens: DrawerColorTokens
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = tokens.footerBg,
        border = BorderStroke(0.8.dp, tokens.footerDivider)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // Layered Purple (Left) & Teal (Right) Decorative Waves matching Reference Images
            Canvas(
                modifier = Modifier
                    .matchParentSize()
            ) {
                val w = size.width
                val h = size.height

                // Left Purple Wave
                val leftWave = Path().apply {
                    moveTo(0f, h * 0.42f)
                    cubicTo(
                        w * 0.25f, h * 0.65f,
                        w * 0.55f, h * 0.92f,
                        w * 0.90f, h
                    )
                    lineTo(0f, h)
                    close()
                }
                drawPath(
                    path = leftWave,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            tokens.footerWaveLeft.copy(alpha = if (tokens.isDark) 0.50f else 0.34f),
                            tokens.footerWaveRight.copy(alpha = 0.08f)
                        )
                    )
                )

                // Right Teal / Cyan Wave
                val rightWave = Path().apply {
                    moveTo(w * 0.25f, h)
                    cubicTo(
                        w * 0.62f, h * 0.85f,
                        w * 0.85f, h * 0.48f,
                        w, h * 0.36f
                    )
                    lineTo(w, h)
                    close()
                }
                drawPath(
                    path = rightWave,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            tokens.footerWaveLeft.copy(alpha = 0.12f),
                            tokens.footerWaveRight.copy(alpha = if (tokens.isDark) 0.52f else 0.35f)
                        )
                    )
                )

                // Crisp luminous top edge highlight along footer waves in Dark/Light Mode
                val sheenLine = Path().apply {
                    moveTo(0f, h * 0.46f)
                    cubicTo(
                        w * 0.35f, h * 0.78f,
                        w * 0.72f, h * 0.82f,
                        w, h * 0.42f
                    )
                }
                drawPath(
                    path = sheenLine,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            tokens.footerWaveLeft.copy(alpha = if (tokens.isDark) 0.75f else 0.45f),
                            tokens.footerWaveRight.copy(alpha = if (tokens.isDark) 0.85f else 0.50f)
                        )
                    ),
                    style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    AdvanceErpLogo(
                        variant = BrandConfig.LogoVariant.SYMBOL,
                        isDarkBackground = tokens.isDark,
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = buildAnnotatedString {
                                withStyle(
                                    SpanStyle(
                                        color = tokens.brandAdvanceText,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                ) {
                                    append("ADVANCE ")
                                }
                                withStyle(
                                    SpanStyle(
                                        color = tokens.brandErpText,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                ) {
                                    append("ERP")
                                }
                            },
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = BrandConfig.TAGLINE_EN,
                            fontSize = 9.5.sp,
                            color = tokens.brandSubtitleText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(50),
                    color = tokens.versionBadgeBg,
                    border = BorderStroke(1.dp, tokens.versionBadgeBorder)
                ) {
                    Text(
                        text = versionLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = tokens.versionBadgeText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        maxLines = 1
                    )
                }
            }
        }
    }
}
