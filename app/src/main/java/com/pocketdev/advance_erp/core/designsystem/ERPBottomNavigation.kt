package com.pocketdev.advance_erp.core.designsystem

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pocketdev.advance_erp.R

enum class BottomNavDestination(val route: String, val icon: ImageVector, val selectedIcon: ImageVector, val labelResId: Int) {
    DASHBOARD("dashboard", Icons.Outlined.Home, Icons.Filled.Home, R.string.nav_home),
    PURCHASES("purchases", Icons.Outlined.ShoppingCart, Icons.Filled.ShoppingCart, R.string.nav_purchase_short),
    INVENTORY("inventory", Icons.Outlined.Inventory2, Icons.Filled.Inventory2, R.string.nav_stock),
    SALES("sales", Icons.Outlined.PointOfSale, Icons.Filled.PointOfSale, R.string.nav_sales),
    MORE("more", Icons.Outlined.MoreHoriz, Icons.Filled.MoreHoriz, R.string.nav_more);

    fun contains(route: String): Boolean = when (this) {
        PURCHASES -> route in setOf("purchases", "import_management", "shipments")
        MORE -> route !in setOf("dashboard", "purchases", "import_management", "shipments", "inventory", "sales")
        else -> this.route == route
    }
}

@Composable
fun ERPBottomNavigation(currentRoute: String, onNavigate: (BottomNavDestination) -> Unit, modifier: Modifier = Modifier) {
    NavigationBar(modifier = modifier, containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
        BottomNavDestination.entries.forEach { destination ->
            val selected = destination.contains(currentRoute.substringBefore('?'))
            NavigationBarItem(
                selected = selected, onClick = { onNavigate(destination) },
                modifier = Modifier.heightIn(min = 64.dp),
                icon = { Icon(if (selected) destination.selectedIcon else destination.icon, contentDescription = null) },
                label = { Text(stringResource(destination.labelResId), style = MaterialTheme.typography.labelMedium, maxLines = 1) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
