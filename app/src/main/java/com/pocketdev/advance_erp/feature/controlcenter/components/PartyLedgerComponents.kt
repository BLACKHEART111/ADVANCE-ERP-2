package com.pocketdev.advance_erp.feature.controlcenter.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.model.CurrencyCode
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.data.entity.Customer
import com.pocketdev.advance_erp.data.entity.Supplier
import com.pocketdev.advance_erp.domain.model.*

@Composable
fun PartyLedgerPanel(report: LedgerReport, filter: ControlCenterFilterState, customers: List<Customer>, suppliers: List<Supplier>,
    language: AppLanguage, ready: Boolean, onParty: (String?, Boolean) -> Unit, onPayment: () -> Unit, onShare: () -> Unit) {
    val bn = language == AppLanguage.BANGLA
    val supplier = filter.category == ReportCategory.SUPPLIER_LEDGER
    var picker by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    fun money(value: Long) = Formatters.formatMoney(value,CurrencyCode.BDT,language)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !supplier, onClick = { onParty(null,false) }, label = { Text(if(bn) "কাস্টমার" else "Customer") }, modifier = Modifier.weight(1f))
            FilterChip(selected = supplier, onClick = { onParty(null,true) }, label = { Text(if(bn) "সাপ্লায়ার" else "Supplier") }, modifier = Modifier.weight(1f))
        }
        val name = if (supplier) suppliers.find { it.id == filter.selectedSupplierId }?.companyName else customers.find { it.id == filter.selectedCustomerId }?.name
        OutlinedButton(onClick = { query=""; picker=true }, modifier = Modifier.fillMaxWidth().heightIn(min=48.dp)) {
            Text(name ?: if(bn) "হিসাব দেখতে একজন পার্টি নির্বাচন করুন" else "Select a party for a statement", maxLines=2, overflow=TextOverflow.Ellipsis)
        }
        report.statement?.takeIf { ready }?.let { statement ->
            Text(statement.partyCode + " • " + statement.phone.orEmpty(), style=MaterialTheme.typography.bodySmall)
            val metrics = listOf((if(bn) "প্রারম্ভিক" else "Opening") to statement.openingBalanceMinor,
                (if(bn) "ডেবিট" else "Debit") to statement.totalDebitMinor,
                (if(bn) "ক্রেডিট" else "Credit") to statement.totalCreditMinor,
                (if(bn) "সমাপনী" else "Closing") to statement.closingBalanceMinor)
            metrics.chunked(2).forEach { pair -> Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                pair.forEach { (label,value) -> Card(modifier=Modifier.weight(1f)) { Column(Modifier.padding(12.dp)) {
                    Text(label,style=MaterialTheme.typography.labelMedium)
                    Text(money(value),style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,maxLines=1,softWrap=false,overflow=TextOverflow.Ellipsis)
                } } }
            } }
            if(statement.closingBalanceMinor<0) Text(if(bn) "অগ্রিম / ক্রেডিট ব্যালেন্স" else "Advance / credit balance",color=MaterialTheme.colorScheme.primary)
            Text(if(bn) "ব্যালেন্স সম্পূর্ণ সময়সীমার। Search/filter শুধু নিচের তালিকা ছোট করে।" else "Balances cover the full period. Search/filters narrow the rows below.",style=MaterialTheme.typography.bodySmall)
            if(report.legacyOpeningMinor!=0L) Text(if(bn) "পূর্বের/সমন্বয়যোগ্য balance: ${money(report.legacyOpeningMinor)}। মূল opening হিসাব মিলিয়ে দেখুন।" else "Legacy/reconciliation opening: ${money(report.legacyOpeningMinor)}. Verify the source opening balance.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.tertiary)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Button(onClick=onPayment,modifier=Modifier.weight(1f)) { Text(if(bn) {if(supplier) "পেমেন্ট দিন" else "টাকা আদায়"} else if(supplier) "Pay supplier" else "Collect payment") }
                OutlinedButton(onClick=onShare,modifier=Modifier.weight(1f)) { Text(if(bn) "স্টেটমেন্ট শেয়ার" else "Share statement") }
            }
            Text(if(bn) "বর্তমান খোলা বিলের বকেয়া (statement-এর তারিখসীমা থেকে আলাদা)" else "Current open-document aging (separate from statement dates)",style=MaterialTheme.typography.titleSmall)
            if(report.unallocatedCreditMinor>0) Text((if(bn) "বরাদ্দ হয়নি এমন credit: " else "Unallocated credit: ")+money(report.unallocatedCreditMinor),style=MaterialTheme.typography.bodySmall)
            val order=listOf("NOT_DUE","1_30","31_60","61_90","90_PLUS","NO_DUE_DATE")
            report.aging.sortedBy { order.indexOf(it.key) }.forEach { bucket ->
                val label=when(bucket.key){"NOT_DUE"->if(bn)"মেয়াদ হয়নি" else "Not due";"NO_DUE_DATE"->if(bn)"পরিশোধের তারিখ নেই" else "No due date";"90_PLUS"->if(bn)"৯০ দিনের বেশি" else "Over 90 days";else->bucket.key.replace('_','–')+(if(bn)" দিন" else " days")}
                Row { Text("$label (${bucket.documents})",modifier=Modifier.weight(1f),style=MaterialTheme.typography.bodySmall);Text(money(bucket.amountMinor),style=MaterialTheme.typography.bodySmall) }
            }
            if(report.aging.isEmpty()) Text(if(bn)"খোলা বিলের বকেয়া নেই।" else "No open-document dues.",style=MaterialTheme.typography.bodySmall)
        }
    }
    if(picker) AlertDialog(onDismissRequest={picker=false}, title={Text(if(bn)"পার্টি নির্বাচন" else "Select party")}, text={
        Column {
            OutlinedTextField(query,{query=it},label={Text(if(bn)"নাম, কোড, ফোন" else "Name, code, phone")},singleLine=true,modifier=Modifier.fillMaxWidth())
            val parties=if(supplier) suppliers.map { LedgerParty(it.id,it.companyName,it.supplierCode,"SUPPLIER",it.phone,it.address) } else customers.map { LedgerParty(it.id,it.name,it.customerCode,"CUSTOMER",it.phone,it.address) }
            val matching=parties.filter { listOf(it.name,it.code,it.phone.orEmpty()).any { value -> value.contains(query,true) } }
            LazyColumn(Modifier.heightIn(max=360.dp)) {
                items(matching,key={it.id}) { party -> Column(Modifier.fillMaxWidth().clickable { onParty(party.id,supplier);picker=false }.padding(vertical=12.dp)) {
                    Text(party.name,fontWeight=FontWeight.SemiBold);Text(party.code+" • "+party.phone.orEmpty(),style=MaterialTheme.typography.bodySmall)
                } }
                if(matching.isEmpty()) item { Text(if(bn)"কোনো পার্টি পাওয়া যায়নি" else "No matching party",modifier=Modifier.padding(vertical=12.dp)) }
            }
        }
    },confirmButton={TextButton(onClick={picker=false}){Text(if(bn)"বন্ধ করুন" else "Close")}})
}

@Composable
fun PartyLedgerRow(row: UnifiedTransactionItem, language: AppLanguage, onClick: () -> Unit) {
    val bn=language==AppLanguage.BANGLA
    fun money(value:Long)=Formatters.formatMoney(value,CurrencyCode.BDT,language)
    Card(Modifier.fillMaxWidth().clickable(onClick=onClick).padding(vertical=3.dp)) {
        Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
            Text(row.referenceNumber,fontWeight=FontWeight.Bold,maxLines=2,overflow=TextOverflow.Ellipsis)
            Text(Formatters.formatDate(row.date,language)+" • "+row.status,style=MaterialTheme.typography.labelMedium)
            Text(row.particulars,style=MaterialTheme.typography.bodySmall)
            if(!row.accountingPosted) Text(if(bn)"হিসাবে পোস্ট হয়নি — balance অপরিবর্তিত" else "Not posted — no balance effect",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.tertiary)
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)){Text(if(bn)"ডেবিট" else "Debit",style=MaterialTheme.typography.labelSmall);Text(money(row.ledgerDebitMinor?:0),maxLines=1,overflow=TextOverflow.Ellipsis)}
                Column(Modifier.weight(1f)){Text(if(bn)"ক্রেডিট" else "Credit",style=MaterialTheme.typography.labelSmall);Text(money(row.ledgerCreditMinor?:0),maxLines=1,overflow=TextOverflow.Ellipsis)}
            }
            Text((if(bn)"চলতি ব্যালেন্স: " else "Balance: ")+money(row.runningBalanceMinor?:0),fontWeight=FontWeight.SemiBold,color=MaterialTheme.colorScheme.primary)
            row.originalCurrency?.let { Text("$it ${java.math.BigDecimal.valueOf(row.originalAmountMinor?:0,2).toPlainString()} • FX ${row.exchangeRate.orEmpty()} → BDT",style=MaterialTheme.typography.bodySmall) }
            row.notes?.takeIf { it.isNotBlank() }?.let { Text(it,style=MaterialTheme.typography.bodySmall,maxLines=2,overflow=TextOverflow.Ellipsis) }
        }
    }
}
