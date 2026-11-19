package com.pocketdev.advance_erp.feature.more

import com.pocketdev.advance_erp.core.designsystem.StatusWarning
import com.pocketdev.advance_erp.core.designsystem.StatusInfo
import com.pocketdev.advance_erp.core.designsystem.StatusSuccess
import com.pocketdev.advance_erp.core.designsystem.StatusError
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.FlightTakeoff
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pocketdev.advance_erp.R
import com.pocketdev.advance_erp.core.designsystem.ERPBackground
import com.pocketdev.advance_erp.core.designsystem.ERPCard
import com.pocketdev.advance_erp.core.designsystem.ERPSectionHeader
import com.pocketdev.advance_erp.core.designsystem.PrimaryPurple
import com.pocketdev.advance_erp.core.designsystem.SecondaryTeal
import com.pocketdev.advance_erp.core.designsystem.TextPrimary
import com.pocketdev.advance_erp.core.designsystem.TextSecondary

data class MoreHubItem(
    val route: String,
    val titleResId: Int,
    val subtitleResId: Int,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun MoreHubScreen(
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        MoreHubItem("accounts", R.string.nav_accounts, R.string.more_accounts_subtitle, Icons.Outlined.AccountBalance, PrimaryPurple),
        MoreHubItem("products", R.string.nav_products, R.string.more_products_subtitle, Icons.Outlined.Category, PrimaryPurple),
        MoreHubItem("import_management", R.string.nav_import_management, R.string.more_import_subtitle, Icons.Outlined.Public, SecondaryTeal),
        MoreHubItem("purchases", R.string.nav_purchases, R.string.more_purchases_subtitle, Icons.Outlined.ShoppingCart, StatusError),
        MoreHubItem("shipments", R.string.nav_shipments, R.string.more_shipments_subtitle, Icons.Outlined.FlightTakeoff, StatusInfo),
        MoreHubItem("customers", R.string.nav_customers, R.string.more_customers_subtitle, Icons.Outlined.Group, StatusSuccess),
        MoreHubItem("suppliers", R.string.nav_suppliers, R.string.more_suppliers_subtitle, Icons.Outlined.Storefront, StatusWarning),
        MoreHubItem("ledger", R.string.nav_ledger, R.string.more_ledger_subtitle, Icons.Outlined.MenuBook, PrimaryPurple),
        MoreHubItem("reports", R.string.nav_reports, R.string.more_reports_subtitle, Icons.Outlined.Assessment, StatusSuccess),
        MoreHubItem("settings", R.string.nav_settings, R.string.more_settings_subtitle, Icons.Outlined.Settings, TextSecondary)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ERPBackground)
            .padding(16.dp)
    ) {
        ERPSectionHeader(
            title = stringResource(R.string.more_modules_title),
            subtitle = stringResource(R.string.more_modules_subtitle)
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(items) { item ->
                ERPCard(
                    onClick = { onNavigate(item.route) },
                    contentPadding = 14.dp
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(item.color.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                tint = item.color,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = stringResource(item.titleResId),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = stringResource(item.subtitleResId),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}
