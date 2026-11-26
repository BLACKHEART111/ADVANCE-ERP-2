package com.pocketdev.advance_erp.navigation

import android.app.Activity
import android.content.ContextWrapper
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pocketdev.advance_erp.R
import com.pocketdev.advance_erp.core.datastore.UserPreferencesRepository
import com.pocketdev.advance_erp.core.designsystem.BottomNavDestination
import com.pocketdev.advance_erp.core.designsystem.ERPBottomNavigation
import com.pocketdev.advance_erp.core.designsystem.ERPDrawer
import com.pocketdev.advance_erp.core.designsystem.ERPTopBar
import com.pocketdev.advance_erp.core.designsystem.LocalIsDarkTheme
import com.pocketdev.advance_erp.core.designsystem.SecondaryTeal
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.data.entity.BusinessProfile
import com.pocketdev.advance_erp.domain.repository.BackupRepository
import com.pocketdev.advance_erp.domain.repository.SettingsRepository
import com.pocketdev.advance_erp.feature.contacts.contactsHeroStatusBarColor
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@Composable
fun MainAppScaffold(
    preferencesRepository: UserPreferencesRepository,
    settingsRepository: SettingsRepository? = null,
    @Suppress("UNUSED_PARAMETER") backupRepository: BackupRepository? = null,
    isAppLockConfigured: Boolean = false,
    onLockApp: () -> Unit = {}
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val drawerViewModel: MainDrawerViewModel = hiltViewModel()
    val drawerMetrics by drawerViewModel.state.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route?.substringBefore('?') ?: Destinations.DASHBOARD

    val currentLanguage by preferencesRepository.selectedLanguage.collectAsState(initial = AppLanguage.BANGLA)
    val currentPhotoUri by preferencesRepository.profilePhotoUri.collectAsState(initial = null)
    val customUserName by preferencesRepository.userName.collectAsState(initial = null)
    val businessProfile by (settingsRepository?.getBusinessProfile() ?: flowOf(null)).collectAsState(initial = null)

    val currentUserName = customUserName?.ifBlank { null }
        ?: businessProfile?.companyName?.ifBlank { null }
        ?: if (currentLanguage == AppLanguage.BANGLA) "ব্যবসায়ী প্রোফাইল" else "Business Owner"

    var showSyncLogoutDialog by remember { mutableStateOf(false) }
    var restoreOutcomeDialog by remember {
        mutableStateOf<com.pocketdev.advance_erp.core.database.StagedRestoreOutcome?>(null)
    }
    LaunchedEffect(Unit) {
        restoreOutcomeDialog = com.pocketdev.advance_erp.core.database.StagedRestore.consumeRestoreResult(context)
    }

    val topBarTitle = when (currentRoute) {
        Destinations.DASHBOARD -> if (currentLanguage == AppLanguage.BANGLA) "ড্যাশবোর্ড" else "Dashboard"
        Destinations.PRODUCTS -> stringResource(R.string.nav_products)
        Destinations.INVENTORY -> stringResource(R.string.nav_inventory)
        Destinations.SALES -> stringResource(R.string.nav_sales)
        Destinations.ACCOUNTS -> stringResource(R.string.nav_accounts)
        Destinations.MORE -> stringResource(R.string.nav_more)
        Destinations.PURCHASES -> "ADVANCE ERP"
        Destinations.IMPORT_MANAGEMENT -> stringResource(R.string.nav_import_management)
        Destinations.SHIPMENTS -> stringResource(R.string.nav_shipments)
        Destinations.IMPORT_VAULT -> if (currentLanguage == AppLanguage.BANGLA) "আমদানি আর্কাইভ" else "Import Vault"
        Destinations.CUSTOMERS -> stringResource(R.string.nav_customers)
        Destinations.SUPPLIERS -> stringResource(R.string.nav_suppliers)
        Destinations.LEDGER -> stringResource(R.string.nav_ledger)
        Destinations.REPORTS -> stringResource(R.string.business_control_center)
        Destinations.SETTINGS ->
            if (currentLanguage == AppLanguage.BANGLA) "সেটিংস" else "Settings"
        else -> stringResource(R.string.app_name)
    }

    val hasIntegratedHeroTopBar = currentRoute in setOf(Destinations.CUSTOMERS, Destinations.SUPPLIERS)
    val view = LocalView.current
    val surfaceColor = MaterialTheme.colorScheme.surface
    val isDarkTheme = LocalIsDarkTheme.current
    val heroStatusBarColor = contactsHeroStatusBarColor(isDarkTheme)
    if (!view.isInEditMode) {
        SideEffect {
            val activity = generateSequence(view.context) { (it as? ContextWrapper)?.baseContext }
                .filterIsInstance<Activity>()
                .firstOrNull()
            activity?.window?.let { window ->
                val controller = WindowCompat.getInsetsController(window, view)
                if (hasIntegratedHeroTopBar) {
                    @Suppress("DEPRECATION")
                    window.statusBarColor = heroStatusBarColor.toArgb()
                    controller.isAppearanceLightStatusBars = false
                } else {
                    @Suppress("DEPRECATION")
                    window.statusBarColor = surfaceColor.toArgb()
                    controller.isAppearanceLightStatusBars = !isDarkTheme
                }
            }
        }
    }
    val resolvedCompanyName = businessProfile?.companyName?.ifBlank { null } ?: "ADVANCE ERP"
    val openDrawerAction: () -> Unit = { coroutineScope.launch { drawerState.open() } }
    val toggleLanguageAction: () -> Unit = {
        coroutineScope.launch {
            val newLang = if (currentLanguage == AppLanguage.BANGLA) AppLanguage.ENGLISH else AppLanguage.BANGLA
            preferencesRepository.setLanguage(newLang)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ERPDrawer(
                currentRoute = currentRoute,
                onNavigate = { destination ->
                    coroutineScope.launch {
                        drawerState.close()
                        if (currentRoute != destination.route) {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                },
                userName = currentUserName,
                profilePhotoUri = currentPhotoUri,
                onPhotoSelected = { newUri ->
                    coroutineScope.launch {
                        preferencesRepository.setProfilePhotoUri(newUri)
                    }
                },
                onUserNameChanged = { newName ->
                    coroutineScope.launch {
                        preferencesRepository.setUserName(newName)
                        settingsRepository?.let { repo ->
                            val current = repo.getBusinessProfile().firstOrNull()
                                ?: BusinessProfile(companyName = newName, phone = "", address = "")
                            repo.updateBusinessProfile(current.copy(companyName = newName))
                        }
                    }
                },
                onSyncLogout = {
                    showSyncLogoutDialog = true
                },
                onSyncClick = {
                    drawerViewModel.triggerSync(
                        onSuccess = {
                            Toast.makeText(
                                context,
                                context.getString(R.string.drawer_sync_success),
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onFailure = { errorDetail ->
                            Toast.makeText(
                                context,
                                "${context.getString(R.string.drawer_sync_failed)}: $errorDetail",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    )
                },
                onLogoutClick = {
                    showSyncLogoutDialog = true
                },
                onEditProfile = {
                    coroutineScope.launch {
                        drawerState.close()
                        if (currentRoute != Destinations.SETTINGS) {
                            navController.navigate(Destinations.SETTINGS) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                },
                language = currentLanguage,
                userRole = drawerMetrics.userRole,
                activeWarehouseName = drawerMetrics.activeWarehouseName,
                hasActiveWarehouse = drawerMetrics.hasActiveWarehouse,
                productCount = drawerMetrics.productCount,
                customerCount = drawerMetrics.customerCount,
                supplierCount = drawerMetrics.supplierCount,
                isSyncing = drawerMetrics.isSyncing
            )
        }
    ) {
        Scaffold(
            contentWindowInsets = if (hasIntegratedHeroTopBar) WindowInsets(0, 0, 0, 0) else ScaffoldDefaults.contentWindowInsets,
            topBar = {
                if (!hasIntegratedHeroTopBar) {
                    ERPTopBar(
                        title = topBarTitle,
                        subtitle = businessProfile?.companyName?.ifBlank { null } ?: "Advance Import Ltd.",
                        currentLanguage = currentLanguage,
                        onOpenDrawer = openDrawerAction,
                        onToggleLanguage = toggleLanguageAction
                    )
                }
            },
            bottomBar = {
                ERPBottomNavigation(
                    currentRoute = currentRoute,
                    onNavigate = { destination ->
                        if (currentRoute != destination.route) {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                AdvanceErpNavGraph(
                    navController = navController,
                    userRole = drawerMetrics.userRole,
                    companyName = resolvedCompanyName,
                    onOpenDrawer = openDrawerAction,
                    onToggleLanguage = toggleLanguageAction
                )
            }
        }
    }

    if (drawerState.isOpen) {
        BackHandler {
            coroutineScope.launch { drawerState.close() }
        }
    }

    restoreOutcomeDialog?.let { outcome ->
        val isBn = currentLanguage == AppLanguage.BANGLA
        AlertDialog(
            onDismissRequest = { restoreOutcomeDialog = null },
            title = {
                Text(
                    text = if (outcome.success) {
                        if (isBn) "ব্যাকআপ রিস্টোর সম্পন্ন" else "Backup Restore Complete"
                    } else {
                        if (isBn) "রিস্টোর সতর্কবার্তা" else "Restore Notice"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = { Text(outcome.message) },
            confirmButton = {
                TextButton(onClick = { restoreOutcomeDialog = null }) {
                    Text(if (isBn) "ঠিক আছে" else "OK")
                }
            }
        )
    }

    if (showSyncLogoutDialog) {
        val isBn = currentLanguage == AppLanguage.BANGLA
        AlertDialog(
            onDismissRequest = { showSyncLogoutDialog = false },
            title = {
                Text(
                    text = if (isAppLockConfigured) {
                        if (isBn) "নিরাপদ লগআউট ও লক" else "Secure Logout & Lock"
                    } else {
                        stringResource(R.string.drawer_sync_title)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isAppLockConfigured) {
                        if (isBn) {
                            "সেশন লক/লগআউট করার আগে লোকাল ডাটাবেজ ব্যাকআপ সিংক করতে পারেন অথবা সরাসরি লক করতে পারেন।"
                        } else {
                            "Sync a local database backup before locking your session, or lock immediately."
                        }
                    } else {
                        stringResource(R.string.drawer_sync_message)
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSyncLogoutDialog = false
                        drawerViewModel.triggerSync(
                            onSuccess = {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.drawer_sync_success),
                                    Toast.LENGTH_SHORT
                                ).show()
                                coroutineScope.launch {
                                    drawerState.close()
                                    if (isAppLockConfigured) {
                                        onLockApp()
                                    }
                                }
                            },
                            onFailure = {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.drawer_sync_failed),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal)
                ) {
                    Text(
                        if (isAppLockConfigured) {
                            if (isBn) "সিংক ও লক" else "Sync & Lock"
                        } else {
                            stringResource(R.string.drawer_sync_now)
                        }
                    )
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            showSyncLogoutDialog = false
                            coroutineScope.launch {
                                drawerState.close()
                                if (isAppLockConfigured) {
                                    onLockApp()
                                } else if (currentRoute != Destinations.SETTINGS) {
                                    navController.navigate(Destinations.SETTINGS) {
                                        launchSingleTop = true
                                    }
                                }
                            }
                        }
                    ) {
                        Text(
                            if (isAppLockConfigured) {
                                if (isBn) "লগআউট / লক" else "Logout / Lock"
                            } else {
                                if (isBn) "নিরাপত্তা সেটিংস" else "Security Settings"
                            }
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(onClick = { showSyncLogoutDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        )
    }
}
