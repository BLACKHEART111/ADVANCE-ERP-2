package com.pocketdev.advance_erp.feature.more

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.FlightTakeoff
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pocketdev.advance_erp.R
import com.pocketdev.advance_erp.core.designsystem.LocalIsDarkTheme
import com.pocketdev.advance_erp.core.security.SecurityManager
import com.pocketdev.advance_erp.core.security.UserRole

private data class MoreHubItem(
    val route: String,
    val titleResId: Int,
    val subtitleResId: Int,
    val icon: ImageVector,
    val accent: Color
)

@Composable
fun MoreHubScreen(
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    userRole: UserRole = UserRole.OWNER_ADMIN
) {
    val colors = MaterialTheme.colorScheme
    val allItems = listOf(
        MoreHubItem("products", R.string.nav_products, R.string.more_products_subtitle, Icons.Outlined.Category, Color(0xFF087E8B)),
        MoreHubItem("import_management", R.string.nav_import_management, R.string.more_import_subtitle, Icons.Outlined.Public, Color(0xFF087E8B)),
        MoreHubItem("purchases", R.string.nav_purchases, R.string.more_purchases_subtitle, Icons.Outlined.ShoppingCart, Color(0xFFD04C72)),
        MoreHubItem("shipments", R.string.nav_shipments, R.string.more_shipments_subtitle, Icons.Outlined.FlightTakeoff, Color(0xFF3977B8)),
        MoreHubItem("import_vault", R.string.nav_import_vault, R.string.more_import_vault_subtitle, Icons.Outlined.FolderSpecial, Color(0xFF6B2485)),
        MoreHubItem("customers", R.string.nav_customers, R.string.more_customers_subtitle, Icons.Outlined.Group, Color(0xFF23845B)),
        MoreHubItem("suppliers", R.string.nav_suppliers, R.string.more_suppliers_subtitle, Icons.Outlined.Storefront, Color(0xFFAA7417)),
        MoreHubItem("accounts", R.string.accounts_ledger, R.string.more_accounts_subtitle, Icons.Outlined.AccountBalance, Color(0xFF8726A0)),
        MoreHubItem("reports", R.string.nav_reports, R.string.more_reports_subtitle, Icons.Outlined.Assessment, Color(0xFF23845B)),
        MoreHubItem("settings", R.string.nav_settings, R.string.more_settings_subtitle, Icons.Outlined.Settings, Color(0xFF6C6074))
    )
    val items = if (SecurityManager.canAccessImportVault(userRole)) {
        allItems
    } else {
        allItems.filter { it.route != "import_vault" }
    }
    val dark = LocalIsDarkTheme.current

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 158.dp),
        modifier = modifier.fillMaxSize().background(colors.background),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 24.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            MorePremiumBanner(dark = dark)
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)) {
                Text(
                    text = stringResource(R.string.more_modules_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.onBackground,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.more_modules_subtitle),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurfaceVariant
                )
            }
        }
        items(items, key = { it.route }) { item ->
            MoreModuleCard(item = item, dark = dark, onClick = { onNavigate(item.route) })
        }
    }
}

@Composable
private fun MorePremiumBanner(dark: Boolean) {
    val shape = RoundedCornerShape(26.dp)
    val background = if (dark) {
        listOf(Color(0xFF281238), Color(0xFF32104A), Color(0xFF12313B))
    } else {
        listOf(Color(0xFF8724B0), Color(0xFF6322A0), Color(0xFF097F8B))
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 124.dp)
            .clip(shape)
            .background(Brush.linearGradient(background))
    ) {
        Canvas(Modifier.matchParentSize()) { drawBannerWaves(dark) }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(50.dp).clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = if (dark) .13f else .20f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Category,
                    contentDescription = stringResource(R.string.more_modules_icon_description),
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp, end = 4.dp)) {
                Text(
                    text = stringResource(R.string.nav_more),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = stringResource(R.string.more_banner_subtitle),
                    color = Color.White.copy(alpha = .86f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            CubeArtwork(dark = dark, modifier = Modifier.size(width = 74.dp, height = 68.dp))
        }
    }
}

private fun DrawScope.drawBannerWaves(dark: Boolean) {
    val teal = Color(0xFF32D6D0).copy(alpha = if (dark) .26f else .24f)
    val violet = Color(0xFFE78BFF).copy(alpha = if (dark) .25f else .30f)
    val wave = Path().apply {
        moveTo(size.width * .42f, 0f)
        cubicTo(size.width * .58f, size.height * .55f, size.width * .72f, size.height * .95f, size.width, size.height * .48f)
        lineTo(size.width, size.height)
        lineTo(size.width * .45f, size.height)
        close()
    }
    drawPath(wave, teal)
    val topGlow = Path().apply {
        moveTo(size.width * .34f, 0f)
        cubicTo(size.width * .58f, size.height * .08f, size.width * .55f, size.height * .82f, size.width, size.height * .40f)
        lineTo(size.width, 0f)
        close()
    }
    drawPath(topGlow, violet)
}

@Composable
private fun CubeArtwork(dark: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        fun cube(cx: Float, cy: Float, side: Float, tint: Color) {
            val dx = side * .5f
            val dy = side * .28f
            val top = Path().apply {
                moveTo(cx, cy - dy); lineTo(cx + dx, cy); lineTo(cx, cy + dy); lineTo(cx - dx, cy); close()
            }
            val left = Path().apply {
                moveTo(cx - dx, cy); lineTo(cx, cy + dy); lineTo(cx, cy + dy + side * .48f); lineTo(cx - dx, cy + side * .48f); close()
            }
            val right = Path().apply {
                moveTo(cx + dx, cy); lineTo(cx, cy + dy); lineTo(cx, cy + dy + side * .48f); lineTo(cx + dx, cy + side * .48f); close()
            }
            drawPath(left, tint.copy(alpha = .75f))
            drawPath(right, tint.copy(alpha = .48f))
            drawPath(top, tint.copy(alpha = .95f))
            drawPath(top, Color.White.copy(alpha = .42f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()))
        }
        val purple = if (dark) Color(0xFFB77CE6) else Color(0xFF9C37D7)
        val cyan = if (dark) Color(0xFF38D5D0) else Color(0xFF19B9BD)
        val side = size.minDimension * .44f
        cube(size.width * .53f, size.height * .43f, side, purple)
        cube(size.width * .29f, size.height * .62f, side * .78f, cyan)
        cube(size.width * .76f, size.height * .62f, side * .78f, purple)
        cube(size.width * .53f, size.height * .77f, side * .70f, cyan)
    }
}

@Composable
private fun MoreModuleCard(item: MoreHubItem, dark: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(22.dp)
    val cardSurface = if (dark) Color(0xFF211D2C) else Color.White
    val accent = if (dark) androidx.compose.ui.graphics.lerp(item.accent, Color.White, .34f) else item.accent
    val outline = if (dark) accent.copy(alpha = .58f) else accent.copy(alpha = .16f)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 188.dp)
            .shadow(if (dark) 0.dp else 7.dp, shape, ambientColor = Color(0xFF9D5EB5).copy(alpha = .12f), spotColor = Color(0xFF9D5EB5).copy(alpha = .12f))
            .clip(shape)
            .clickable(role = Role.Button, onClick = onClick),
        shape = shape,
        color = cardSurface,
        border = BorderStroke(1.dp, outline),
        tonalElevation = if (dark) 1.dp else 0.dp
    ) {
        Box {
            Canvas(Modifier.matchParentSize()) { drawCardWave(accent, dark) }
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(54.dp).clip(RoundedCornerShape(17.dp))
                            .background(accent.copy(alpha = if (dark) .22f else .12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(item.icon, contentDescription = null, tint = accent, modifier = Modifier.size(29.dp))
                    }
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = if (dark) colors.primary else Color(0xFF792B87), modifier = Modifier.size(21.dp))
                }
                Spacer(Modifier.height(13.dp))
                Text(
                    text = stringResource(item.titleResId),
                    color = colors.onSurface,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = stringResource(item.subtitleResId),
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

private fun DrawScope.drawCardWave(accent: Color, dark: Boolean) {
    val path = Path().apply {
        moveTo(size.width * .42f, size.height)
        cubicTo(size.width * .60f, size.height * .88f, size.width * .80f, size.height * .68f, size.width, size.height * .78f)
        lineTo(size.width, size.height)
        close()
    }
    drawPath(path, accent.copy(alpha = if (dark) .12f else .075f))
    val stroke = Path().apply {
        moveTo(size.width * .43f, size.height * .985f)
        cubicTo(size.width * .62f, size.height * .87f, size.width * .80f, size.height * .70f, size.width, size.height * .79f)
    }
    drawPath(stroke, accent.copy(alpha = if (dark) .72f else .40f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.4.dp.toPx()))
}
