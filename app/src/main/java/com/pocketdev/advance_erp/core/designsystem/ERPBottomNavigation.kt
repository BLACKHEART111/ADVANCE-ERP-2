package com.pocketdev.advance_erp.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.selectable
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
    val colors = MaterialTheme.colorScheme
    val dark = LocalIsDarkTheme.current
    val shape = RoundedCornerShape(25.dp)
    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp).navigationBarsPadding(),
        shape = shape,
        color = colors.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineVariant.copy(alpha = .8f)),
        shadowElevation = 8.dp
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 5.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        BottomNavDestination.entries.forEach { destination ->
            val selected = destination.contains(currentRoute.substringBefore('?'))
            val itemShape = RoundedCornerShape(21.dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 58.dp)
                    .clip(itemShape)
                    .background(
                        if (selected) Brush.horizontalGradient(
                            if (dark) listOf(Color(0xFF7C22A5), Color(0xFF5C2387))
                            else listOf(colors.primary, colors.primary.copy(alpha = .84f))
                        ) else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                    )
                    .selectable(selected = selected, role = Role.Tab, onClick = { onNavigate(destination) })
                    .padding(horizontal = 2.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.layout.Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (selected) destination.selectedIcon else destination.icon,
                        contentDescription = null,
                        tint = if (selected) Color.White else colors.onSurfaceVariant,
                        modifier = Modifier.size(23.dp)
                    )
                    Spacer(Modifier.size(2.dp))
                    Text(
                        text = stringResource(destination.labelResId),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) Color.White else colors.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
      }
    }
}
