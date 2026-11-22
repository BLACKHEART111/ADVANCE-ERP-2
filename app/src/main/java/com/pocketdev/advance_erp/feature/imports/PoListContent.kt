package com.pocketdev.advance_erp.feature.imports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.data.entity.*

internal fun LazyListScope.poReferenceListItems(
    orders:List<PurchaseOrder>,suppliers:List<Supplier>,visibleOrders:List<PurchaseOrder>,language:AppLanguage,
    query:String,status:PurchaseOrderStatus?,supplierFilter:String?,onQuery:(String)->Unit,onStatus:(PurchaseOrderStatus?)->Unit,onSupplier:(String?)->Unit,
    onCreate:()->Unit,onPayment:()->Unit,onOpen:(PurchaseOrder)->Unit,onTools:(()->Unit)?=null
) {
    val bn=language==AppLanguage.BANGLA
    item { PoListHeader(bn,listOf(orders.size,orders.count {it.status !in setOf("DRAFT","COMPLETED","RECEIVED","CANCELLED")},orders.count {it.status in setOf("COMPLETED","RECEIVED")},orders.count {it.status=="CANCELLED"}),onCreate,onPayment,onTools) }
    item {
        var filters by rememberSaveable {mutableStateOf(false)}
        Row(horizontalArrangement=Arrangement.spacedBy(7.dp)) {
            OutlinedTextField(query,onQuery,modifier=Modifier.weight(1f),singleLine=true,shape=RoundedCornerShape(13.dp),placeholder={Text(if(bn) "PO নং, সরবরাহকারী বা পণ্য খুঁজুন…" else "Search PO or supplier…",style=MaterialTheme.typography.bodySmall)},leadingIcon={Icon(Icons.Outlined.Search,null)})
            OutlinedIconButton(onClick={filters=true},modifier=Modifier.size(56.dp),shape=RoundedCornerShape(13.dp)) {Icon(Icons.Outlined.FilterAlt,if(bn) "ফিল্টার" else "Filters")}
        }
        if(filters) AlertDialog(onDismissRequest={filters=false},title={Text(if(bn) "সরবরাহকারী ফিল্টার" else "Supplier filter")},text={LazyColumn {item {TextButton(onClick={onSupplier(null);filters=false}){Text(if(bn) "সব সরবরাহকারী" else "All suppliers")}};items(suppliers) {supplier-> TextButton(onClick={onSupplier(supplier.id);filters=false}){Text(supplier.companyName)} } }},confirmButton={TextButton(onClick={filters=false}){Text(if(bn) "বন্ধ" else "Close")}})
    }
    item {LazyRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
        item {FilterChip(selected=status==null,onClick={onStatus(null)},label={Text(if(bn) "সবগুলো" else "All")})}
        items(PurchaseOrderStatus.entries) {value->FilterChip(selected=status==value,onClick={onStatus(if(status==value)null else value)},label={Text(poCompactStatusLabel(value.name,bn))})}
    } }
    if(visibleOrders.isEmpty()) item {Surface(shape=RoundedCornerShape(15.dp),color=MaterialTheme.colorScheme.surface){Text(if(bn) "কোনো ক্রয়াদেশ পাওয়া যায়নি। অনুসন্ধান/ফিল্টার বদলান অথবা নতুন ক্রয়াদেশ তৈরি করুন।" else "No purchase orders found. Change your filters or create a purchase order.",Modifier.padding(22.dp))}}
    items(visibleOrders,key={it.id}) {order->PoCompactOrderCard(order,suppliers.find{it.id==order.supplierId}?.companyName.orEmpty(),language){onOpen(order)}}
}
