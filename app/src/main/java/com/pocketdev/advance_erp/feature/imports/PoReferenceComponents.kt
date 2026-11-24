package com.pocketdev.advance_erp.feature.imports

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import com.pocketdev.advance_erp.core.designsystem.LocalIsDarkTheme
import java.math.BigDecimal
import java.math.RoundingMode

internal data class PoCategoryLandedInput(
    val categoryId: String,
    val categoryTitle: String,
    val totalPcs: Int,
    val grossWeightKg: BigDecimal,
    val productCostBdt: BigDecimal,
    val freightCostBdt: BigDecimal
)

internal data class PoCategoryLandedBreakdown(
    val categoryId: String,
    val categoryTitle: String,
    val totalPcs: Int,
    val grossWeightKg: BigDecimal,
    val productCostBdt: BigDecimal,
    val freightCostBdt: BigDecimal,
    val allocatedOverheadBdt: BigDecimal,
    val totalLandedCostBdt: BigDecimal,
    val averagePerPcsBdt: BigDecimal
)

internal fun calculatePoCategoryLandedBreakdowns(
    inputs: List<PoCategoryLandedInput>,
    sharedOverheadBdt: BigDecimal,
    expectedTotalProductBdt: BigDecimal? = null,
    expectedTotalFreightBdt: BigDecimal? = null
): List<PoCategoryLandedBreakdown> {
    val active = inputs.filter {
        it.totalPcs > 0 ||
            it.grossWeightKg > BigDecimal.ZERO ||
            it.productCostBdt > BigDecimal.ZERO ||
            it.freightCostBdt > BigDecimal.ZERO
    }
    if (active.isEmpty()) return emptyList()

    fun reconcileMinorList(rawValues: List<BigDecimal>, expectedTotal: BigDecimal?): List<Long> {
        val minors = rawValues.map {
            it.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact()
        }.toMutableList()
        if (expectedTotal != null) {
            val expectedMinor = expectedTotal.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact()
            val delta = expectedMinor - minors.sum()
            if (delta != 0L) {
                val targetIdx = minors.indexOfLast { it > 0L }.takeIf { it >= 0 } ?: minors.lastIndex
                minors[targetIdx] = (minors[targetIdx] + delta).coerceAtLeast(0L)
            }
        }
        return minors
    }

    val prodMinors = reconcileMinorList(active.map { it.productCostBdt }, expectedTotalProductBdt)
    val freightMinors = reconcileMinorList(active.map { it.freightCostBdt }, expectedTotalFreightBdt)

    val overheadMinor = sharedOverheadBdt.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact()
    val totalPcsSum = active.sumOf { it.totalPcs.coerceAtLeast(0).toLong() }
    val totalWeightSum = active.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.grossWeightKg.coerceAtLeast(BigDecimal.ZERO)) }
    val totalProdSum = prodMinors.sum()

    val weights: List<BigDecimal> = when {
        totalPcsSum > 0L -> active.map { BigDecimal.valueOf(it.totalPcs.coerceAtLeast(0).toLong()) }
        totalWeightSum > BigDecimal.ZERO -> active.map { it.grossWeightKg.coerceAtLeast(BigDecimal.ZERO) }
        totalProdSum > 0L -> prodMinors.map { BigDecimal.valueOf(it.coerceAtLeast(0L)) }
        else -> List(active.size) { BigDecimal.ONE }
    }
    val overheadMinors = allocateMinorProportionally(overheadMinor, weights)

    return active.mapIndexed { index, item ->
        val prodBdt = BigDecimal.valueOf(prodMinors[index], 2)
        val freightBdt = BigDecimal.valueOf(freightMinors[index], 2)
        val allocatedBdt = BigDecimal.valueOf(overheadMinors[index], 2)
        val totalLandedBdt = prodBdt.add(freightBdt).add(allocatedBdt).setScale(2, RoundingMode.HALF_UP)
        val avgPerPcsBdt = if (item.totalPcs > 0) {
            totalLandedBdt.divide(BigDecimal(item.totalPcs), 2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
        }
        PoCategoryLandedBreakdown(
            categoryId = item.categoryId,
            categoryTitle = item.categoryTitle,
            totalPcs = item.totalPcs,
            grossWeightKg = item.grossWeightKg.setScale(2, RoundingMode.HALF_UP),
            productCostBdt = prodBdt,
            freightCostBdt = freightBdt,
            allocatedOverheadBdt = allocatedBdt,
            totalLandedCostBdt = totalLandedBdt,
            averagePerPcsBdt = avgPerPcsBdt
        )
    }
}

private fun allocateMinorProportionally(
    totalMinor: Long,
    weights: List<BigDecimal>
): List<Long> {
    if (weights.isEmpty() || totalMinor == 0L) return List(weights.size) { 0L }
    val weightSum = weights.fold(BigDecimal.ZERO, BigDecimal::add)
    if (weightSum <= BigDecimal.ZERO) return List(weights.size) { 0L }
    val lastPositiveIdx = weights.indexOfLast { it > BigDecimal.ZERO }.takeIf { it >= 0 } ?: weights.lastIndex
    var allocatedSoFar = 0L
    return weights.indices.map { i ->
        if (i == lastPositiveIdx) {
            totalMinor - allocatedSoFar
        } else {
            val share = BigDecimal.valueOf(totalMinor)
                .multiply(weights[i])
                .divide(weightSum, 0, RoundingMode.DOWN)
                .toLong()
            allocatedSoFar += share
            share
        }
    }
}

internal fun allocateAmountBdtAcrossCategoriesByWeight(
    totalAmountMinor: Long,
    weightsKg: List<BigDecimal>,
    pcsList: List<Int>,
    productMinors: List<Long>
): List<BigDecimal> {
    if (weightsKg.isEmpty() || totalAmountMinor == 0L) {
        return List(weightsKg.size) { BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) }
    }
    val totalWeight = weightsKg.fold(BigDecimal.ZERO) { acc, w -> acc.add(w.coerceAtLeast(BigDecimal.ZERO)) }
    val totalPcs = pcsList.sumOf { it.coerceAtLeast(0).toLong() }
    val totalProd = productMinors.sum()
    val basis = when {
        totalWeight > BigDecimal.ZERO -> weightsKg.map { it.coerceAtLeast(BigDecimal.ZERO) }
        totalPcs > 0L -> pcsList.map { BigDecimal.valueOf(it.coerceAtLeast(0).toLong()) }
        totalProd > 0L -> productMinors.map { BigDecimal.valueOf(it.coerceAtLeast(0L)) }
        else -> List(weightsKg.size) { BigDecimal.ONE }
    }
    return allocateMinorProportionally(totalAmountMinor, basis).map { BigDecimal.valueOf(it, 2) }
}

/** Scoped PO palette: the global theme preference still determines light/dark. */
@Composable internal fun PoReferenceTheme(content: @Composable () -> Unit) {
    val dark = LocalIsDarkTheme.current
    val base = MaterialTheme.colorScheme
    MaterialTheme(colorScheme = base.copy(
        primary = if (dark) Color(0xFFD786EF) else Color(0xFF862891),
        onPrimary = if (dark) Color(0xFF24112F) else Color.White,
        secondary = if (dark) Color(0xFF64D8D6) else Color(0xFF087D88),
        background = if (dark) Color(0xFF0D1425) else Color(0xFFF8F6FC),
        surface = if (dark) Color(0xFF17243A) else Color.White,
        surfaceVariant = if (dark) Color(0xFF202D43) else Color(0xFFF3EDF8),
        onSurface = if (dark) Color(0xFFF5F1FA) else Color(0xFF211B2B),
        onSurfaceVariant = if (dark) Color(0xFFBDC4D7) else Color(0xFF746D7D),
        secondaryContainer = if(dark) Color(0xFF71308D) else Color(0xFF862891),
        onSecondaryContainer = Color.White,
        outline = if(dark) Color(0xFF52617A) else Color(0xFFE0D6E9),
        outlineVariant = if (dark) Color(0xFF384961) else Color(0xFFE8DFEF)
    ), content = content)
}

@Composable internal fun PoTopBar(title: String, subtitle: String? = null, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().heightIn(min=54.dp), verticalAlignment=Alignment.CenterVertically) {
        IconButton(onClick=onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") }
        Column(Modifier.weight(1f)) {
            Text(title, style=MaterialTheme.typography.titleMedium, fontWeight=FontWeight.Bold)
            subtitle?.let { Text(it, style=MaterialTheme.typography.labelSmall, color=MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        actions()
    }
    HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
}

@Composable internal fun PoValueCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, trailing: ImageVector? = null, showIcon:Boolean=true) {
    val shape=RoundedCornerShape(14.dp)
    Surface(modifier=modifier.then(if(onClick!=null) Modifier.clickable(onClick=onClick) else Modifier), shape=shape, color=MaterialTheme.colorScheme.surface, border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.padding(horizontal=10.dp,vertical=9.dp).heightIn(min=34.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(9.dp)) {
            if(showIcon) Surface(shape=RoundedCornerShape(10.dp),color=MaterialTheme.colorScheme.primary.copy(alpha=.09f)) { Icon(icon,null,Modifier.padding(7.dp).size(20.dp), tint=MaterialTheme.colorScheme.primary) }
            Column(Modifier.weight(1f)) { Text(label,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1); Text(value,style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.SemiBold) }
            trailing?.let { Icon(it,null,Modifier.size(18.dp)) }
        }
    }
}

@Composable internal fun PoNumberInput(label:String,value:String,onValueChange:(String)->Unit,modifier:Modifier=Modifier,decimal:Boolean=true) {
    Column(modifier) {
        Text(label,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(value,onValueChange,modifier=Modifier.fillMaxWidth(),singleLine=true,shape=RoundedCornerShape(10.dp),textStyle=MaterialTheme.typography.bodyMedium,keyboardOptions=KeyboardOptions(keyboardType=if(decimal) KeyboardType.Decimal else KeyboardType.Number))
    }
}

@Composable internal fun PoShipmentMetrics(values:List<Pair<String,String>>) {
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)) {
        val icons=listOf(Icons.Outlined.Inventory2,Icons.Outlined.Widgets,Icons.Outlined.Scale,Icons.Outlined.ViewInAr)
        values.forEachIndexed { index,(label,value) ->
            Surface(Modifier.weight(1f),shape=RoundedCornerShape(13.dp),color=MaterialTheme.colorScheme.surface,border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant),shadowElevation=1.dp) {
                Column(Modifier.padding(horizontal=3.dp,vertical=9.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                    Icon(icons[index%icons.size],null,Modifier.size(23.dp),tint=if(index>1) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(5.dp)); Text(label,style=MaterialTheme.typography.labelSmall)
                    Text(value,style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.Bold)
                }
            }
        }
    }
}

@Composable internal fun PoLandedHighlight(
    total:String,
    unit:String?=null,
    isBangla:Boolean,
    categoryBreakdowns:List<PoCategoryLandedBreakdown> = emptyList(),
    productBreakdowns:List<PoProductLandedBreakdown> = emptyList()
) {
    val dark=LocalIsDarkTheme.current
    Surface(shape=RoundedCornerShape(14.dp),color=if(dark) Color(0xFF343329) else Color(0xFFFFF4D8)) {
        Column(Modifier.fillMaxWidth().padding(13.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Outlined.Layers,null,Modifier.size(29.dp),tint=if(dark) Color(0xFFFFD27C) else Color(0xFF93600E))
                Column(Modifier.weight(1f)) { Text(if(isBangla) "সর্বমোট ল্যান্ডিং কস্ট" else "Total landed cost",fontWeight=FontWeight.Bold,style=MaterialTheme.typography.labelLarge); Text(total,color=MaterialTheme.colorScheme.secondary,fontWeight=FontWeight.ExtraBold,style=MaterialTheme.typography.titleLarge) }
            }
            if (productBreakdowns.isNotEmpty()) {
                HorizontalDivider(color=MaterialTheme.colorScheme.secondary.copy(alpha=.15f))
                Text(
                    text = if (isBangla) "পণ্য অনুযায়ী গড় প্রতি পিস ল্যান্ডিং কস্ট:" else "Product-wise average per unit landed cost:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                productBreakdowns.forEach { prod ->
                    val prodTotalStr = BigDecimal.valueOf(prod.totalLandedBdtMinor, 2).setScale(2, RoundingMode.HALF_UP)
                    val prodWeightStr = prod.grossWeightKg.setScale(2, RoundingMode.HALF_UP)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = prod.productTitle,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isBangla) {
                                    "${prod.totalPcs} PCS • $prodWeightStr KG • মোট: ৳ $prodTotalStr"
                                } else {
                                    "${prod.totalPcs} PCS • $prodWeightStr KG • Total: ৳ $prodTotalStr"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = (if (isBangla) "গড় প্রতি পিস: " else "Avg: ") + "৳ ${prod.unitLandedBdt} / PCS",
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
            if (categoryBreakdowns.isNotEmpty() && (productBreakdowns.isEmpty() || categoryBreakdowns.size > 1)) {
                HorizontalDivider(color=MaterialTheme.colorScheme.secondary.copy(alpha=.15f))
                Text(
                    text = if (isBangla) "ক্যাটাগরি অনুযায়ী গড় প্রতি পিস ল্যান্ডিং কস্ট:" else "Category-wise average per unit landed cost:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                categoryBreakdowns.forEach { cat ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = cat.categoryTitle,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isBangla) {
                                    "${cat.totalPcs} PCS • ${cat.grossWeightKg} KG • মোট: ৳ ${cat.totalLandedCostBdt}"
                                } else {
                                    "${cat.totalPcs} PCS • ${cat.grossWeightKg} KG • Total: ৳ ${cat.totalLandedCostBdt}"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = (if (isBangla) "গড় প্রতি পিস: " else "Avg: ") + "৳ ${cat.averagePerPcsBdt} / PCS",
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
            unit?.let { HorizontalDivider(color=MaterialTheme.colorScheme.secondary.copy(alpha=.15f)); Text(it,color=MaterialTheme.colorScheme.secondary,style=MaterialTheme.typography.bodySmall,fontWeight=FontWeight.Bold) }
        }
    }
}

@Composable internal fun PoListHeader(isBangla:Boolean,counts:List<Int>,onCreate:()->Unit,onPayment:()->Unit,onTools:(()->Unit)?=null) {
    val dark=LocalIsDarkTheme.current
    Box(Modifier.fillMaxWidth().heightIn(min=85.dp)) {
        Canvas(Modifier.matchParentSize()) {
            val wave=Path().apply {moveTo(0f,size.height*.85f);cubicTo(size.width*.4f,size.height*.35f,size.width*.58f,size.height*1.25f,size.width,size.height*.22f);lineTo(size.width,size.height*.62f);cubicTo(size.width*.55f,size.height*1.25f,size.width*.32f,size.height*.65f,0f,size.height);close()}
            drawPath(wave,Brush.horizontalGradient(listOf(Color(0xFFCE76F1).copy(alpha=.25f),Color(0xFF13B9BD).copy(alpha=.6f))))
        }
        Row(Modifier.padding(vertical=13.dp,horizontal=7.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.AccountBalanceWallet,null,Modifier.size(36.dp),tint=MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {Text(if(isBangla) "ক্রয় আদেশ (PO)" else "Purchase orders (PO)",fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium);Text(if(isBangla) "সরবরাহকারীর নিকট পণ্য ক্রয়ের আদেশ" else "Purchase orders from your suppliers",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
            var menu by remember {mutableStateOf(false)}
            Box {
                IconButton(onClick={menu=true}) {Icon(Icons.Outlined.MoreVert,if(isBangla) "ক্রয়ের কার্যক্রম" else "Purchase actions",tint=MaterialTheme.colorScheme.primary)}
                DropdownMenu(expanded=menu,onDismissRequest={menu=false}) {
                    DropdownMenuItem(text={Text(if(isBangla) "পেমেন্ট রেকর্ড করুন" else "Record payment")},onClick={menu=false;onPayment()})
                    if(onTools!=null) DropdownMenuItem(text={Text(if(isBangla) "আমদানি ও অন্যান্য টুল" else "Import & other tools")},onClick={menu=false;onTools()})
                }
            }
        }
    }
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)) {
        val labels=if(isBangla)listOf("মোট PO","চলমান","সম্পন্ন","বাতিল")else listOf("Total PO","Active","Complete","Cancelled")
        val icons=listOf(Icons.Outlined.Description,Icons.Outlined.HourglassEmpty,Icons.Outlined.CheckCircle,Icons.Outlined.Cancel)
        counts.forEachIndexed { i,count -> Surface(Modifier.weight(1f),shape=RoundedCornerShape(15.dp),color=MaterialTheme.colorScheme.surface,shadowElevation=1.dp,border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)) {
            Column(Modifier.padding(horizontal=8.dp,vertical=10.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                Icon(icons[i],null,Modifier.size(25.dp),tint=listOf(MaterialTheme.colorScheme.primary,Color(0xFF9D6C1E),Color(0xFF25834F),Color(0xFFC74747))[i])
                Text(labels[i],style=MaterialTheme.typography.labelSmall);Text(count.toString(),style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
            }
        } }
    }
    Button(onClick=onCreate,modifier=Modifier.fillMaxWidth().padding(top=9.dp),shape=RoundedCornerShape(12.dp)) { Icon(Icons.Outlined.Add,null,Modifier.size(19.dp));Spacer(Modifier.width(8.dp));Text(if(isBangla) "নতুন ক্রয়াদেশ (PO)" else "New purchase order (PO)") }

}

@Composable internal fun PoCompactOrderCard(order:com.pocketdev.advance_erp.data.entity.PurchaseOrder,supplierName:String,language:com.pocketdev.advance_erp.core.model.AppLanguage,onClick:()->Unit) {
    val bn=language==com.pocketdev.advance_erp.core.model.AppLanguage.BANGLA
    Surface(onClick=onClick,shape=RoundedCornerShape(15.dp),color=MaterialTheme.colorScheme.surface,border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant),shadowElevation=1.dp) {
        Row(Modifier.fillMaxWidth().padding(horizontal=11.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            Surface(shape=RoundedCornerShape(10.dp),color=MaterialTheme.colorScheme.primary.copy(alpha=.09f)) {Icon(Icons.Outlined.Inventory2,null,Modifier.padding(8.dp).size(23.dp),tint=MaterialTheme.colorScheme.primary)}
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(2.dp)) {
                Text(order.poNumber,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.bodyMedium)
                Text(supplierName,style=MaterialTheme.typography.bodySmall,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text(com.pocketdev.advance_erp.core.util.Formatters.formatDate(order.orderDate,language),style=MaterialTheme.typography.labelSmall)
            }
            Column(horizontalAlignment=Alignment.End,verticalArrangement=Arrangement.spacedBy(7.dp)) {
                val status=com.pocketdev.advance_erp.data.entity.PurchaseOrderStatus.entries.find {it.name==order.status}
                val color=when(order.status){"DRAFT"->com.pocketdev.advance_erp.core.designsystem.StatusWarning;"CANCELLED"->com.pocketdev.advance_erp.core.designsystem.StatusError;"PARTIALLY_PAID"->MaterialTheme.colorScheme.primary;else->com.pocketdev.advance_erp.core.designsystem.StatusSuccess}
                Surface(shape=RoundedCornerShape(20.dp),color=color.copy(alpha=.13f)) {Text(poCompactStatusLabel(order.status,bn),Modifier.padding(horizontal=7.dp,vertical=2.dp),style=MaterialTheme.typography.labelSmall,color=color)}
                Text("${order.currency} ${com.pocketdev.advance_erp.core.util.Formatters.formatMinorUnit(order.totalAmountMinor)}",style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.Bold)
            }
            Icon(Icons.Outlined.ChevronRight,null,Modifier.size(17.dp))
        }
    }
}

@Composable internal fun PoDetailAction(label:String,icon:ImageVector,modifier:Modifier=Modifier,onClick:()->Unit) {
    Surface(onClick=onClick,modifier=modifier,shape=RoundedCornerShape(15.dp),color=MaterialTheme.colorScheme.surface,border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant),shadowElevation=2.dp) {
        Column(Modifier.padding(horizontal=6.dp,vertical=14.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)) {Icon(icon,null,Modifier.size(28.dp),tint=MaterialTheme.colorScheme.primary);Text(label,style=MaterialTheme.typography.labelLarge,textAlign=androidx.compose.ui.text.style.TextAlign.Center)}
    }
}

@Composable internal fun PoHeroMetadata(label:String,value:String,icon:ImageVector,modifier:Modifier=Modifier) {
    Row(modifier,verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(7.dp)) {
        Surface(shape=RoundedCornerShape(9.dp),color=Color.White.copy(alpha=.13f)) {Icon(icon,null,Modifier.padding(6.dp).size(19.dp),tint=Color.White)}
        Column(Modifier.weight(1f)) {Text(label,color=Color.White.copy(alpha=.8f),style=MaterialTheme.typography.labelSmall);Text(value,color=Color.White,style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.SemiBold)}
    }
}

internal fun poCompactStatusLabel(status:String,bangla:Boolean):String {
    val value=com.pocketdev.advance_erp.data.entity.PurchaseOrderStatus.entries.find {it.name==status}
    if(!bangla) return value?.labelEn ?: status
    return when(status) {
        "DRAFT" -> "ড্রাফট"
        "CONFIRMED" -> "নিশ্চিত"
        "PARTIALLY_PAID" -> "আংশিক"
        "PAID" -> "পরিশোধিত"
        "COMPLETED" -> "সম্পন্ন"
        "CANCELLED" -> "বাতিল"
        else -> value?.labelBn ?: status
    }
}
