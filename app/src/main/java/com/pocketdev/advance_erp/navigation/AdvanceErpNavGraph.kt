package com.pocketdev.advance_erp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.pocketdev.advance_erp.domain.model.ReportCategory
import com.pocketdev.advance_erp.feature.accounts.AccountsScreen
import com.pocketdev.advance_erp.feature.accounts.AccountsTab
import com.pocketdev.advance_erp.feature.contacts.ContactsScreen
import com.pocketdev.advance_erp.feature.contacts.ContactsTab
import com.pocketdev.advance_erp.feature.controlcenter.BusinessControlCenterScreen
import com.pocketdev.advance_erp.feature.dashboard.DashboardScreen
import com.pocketdev.advance_erp.feature.imports.ImportManagementScreen
import com.pocketdev.advance_erp.feature.imports.ImportModuleTab
import com.pocketdev.advance_erp.feature.inventory.InventoryScreen
import com.pocketdev.advance_erp.feature.more.MoreHubScreen
import com.pocketdev.advance_erp.feature.product.ProductListScreen
import com.pocketdev.advance_erp.feature.sales.SalesScreen
import com.pocketdev.advance_erp.feature.settings.SettingsScreen

object Destinations {
    const val DASHBOARD = "dashboard"
    const val PRODUCTS = "products"
    const val INVENTORY = "inventory"
    const val SALES = "sales"
    const val ACCOUNTS = "accounts"
    const val MORE = "more"
    const val PURCHASES = "purchases"
    const val IMPORT_MANAGEMENT = "import_management"
    const val SHIPMENTS = "shipments"
    const val CUSTOMERS = "customers"
    const val SUPPLIERS = "suppliers"
    const val LEDGER = "ledger"
    const val REPORTS = "reports"
    const val SETTINGS = "settings"
}

@Composable
fun AdvanceErpNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    androidx.compose.runtime.CompositionLocalProvider(com.pocketdev.advance_erp.core.designsystem.LocalERPOnNavigate provides { destination ->
        navController.navigate(destination.route) {
            popUpTo(navController.graph.startDestinationId) { saveState=true }
            launchSingleTop=true
            restoreState=true
        }
    }) {
    NavHost(
        navController = navController,
        startDestination = Destinations.DASHBOARD,
        modifier = modifier
    ) {
        composable(Destinations.DASHBOARD) {
            val navigateSingleTop: (String) -> Unit = { route ->
                navController.navigate(route) { launchSingleTop = true }
            }
            DashboardScreen(
                viewModel = hiltViewModel(),
                onNavigateToAddProduct = { navigateSingleTop("${Destinations.PRODUCTS}?action=add") },
                onNavigateToStockAdjust = { navigateSingleTop("${Destinations.INVENTORY}?action=adjust") },
                onNavigateToLedger = { navigateSingleTop(Destinations.LEDGER) },
                onNavigateToImports = { navigateSingleTop(Destinations.PURCHASES) },
                onNavigateToSales = { navigateSingleTop(Destinations.SALES) },
                onNavigateToTransactions = { navigateSingleTop(Destinations.INVENTORY) }
            )
        }

        composable("${Destinations.PRODUCTS}?action={action}", arguments = listOf(navArgument("action") { type = NavType.StringType; defaultValue = "" })) { entry ->
            ProductListScreen(viewModel = hiltViewModel(), initialAction = entry.arguments?.getString("action").orEmpty())
        }

        composable("${Destinations.INVENTORY}?action={action}", arguments = listOf(navArgument("action") { type = NavType.StringType; defaultValue = "" })) { entry ->
            InventoryScreen(viewModel = hiltViewModel(), initialAction = entry.arguments?.getString("action").orEmpty())
        }

        composable(Destinations.SALES) {
            SalesScreen()
        }

        composable(Destinations.ACCOUNTS) {
            AccountsScreen()
        }

        composable(Destinations.MORE) {
            MoreHubScreen(onNavigate = { route -> navController.navigate(route) })
        }

        composable(Destinations.PURCHASES) {
            ImportManagementScreen(initialTab = ImportModuleTab.PURCHASE_ORDERS)
        }

        composable(Destinations.IMPORT_MANAGEMENT) {
            ImportManagementScreen(initialTab = ImportModuleTab.OVERVIEW)
        }

        composable(Destinations.SHIPMENTS) {
            ImportManagementScreen(initialTab = ImportModuleTab.SHIPMENTS)
        }

        composable(Destinations.CUSTOMERS) {
            ContactsScreen(viewModel = hiltViewModel(), initialTab = ContactsTab.CUSTOMERS)
        }

        composable(Destinations.SUPPLIERS) {
            ContactsScreen(viewModel = hiltViewModel(), initialTab = ContactsTab.SUPPLIERS)
        }

        composable(Destinations.LEDGER) {
            BusinessControlCenterScreen(
                viewModel = hiltViewModel(),
                initialCategory = ReportCategory.CUSTOMER_LEDGER
            )
        }

        composable(Destinations.REPORTS) {
            BusinessControlCenterScreen(viewModel = hiltViewModel())
        }

        composable(Destinations.SETTINGS) {
            SettingsScreen(viewModel = hiltViewModel())
        }
    }
    }
}
