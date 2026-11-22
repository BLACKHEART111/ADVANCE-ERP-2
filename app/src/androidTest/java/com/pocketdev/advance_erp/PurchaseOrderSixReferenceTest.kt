package com.pocketdev.advance_erp

import android.app.ActivityManager
import android.content.res.Configuration
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.pocketdev.advance_erp.core.designsystem.*
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.data.entity.*
import com.pocketdev.advance_erp.feature.imports.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

/** On-device production components with isolated fixtures; never inserts into Room. */
class PurchaseOrderSixReferenceTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    @Test fun lightListAndDetails()=render(false)
    @Test fun darkListAndDetails()=render(true)
    private fun render(dark:Boolean) {
        val supplier=Supplier("visual-s","S","Yiwu Beauty Co., Ltd.",phone="",address="China")
        val order=PurchaseOrder("visual-po","PO-2026-0049",supplier.id,status="CONFIRMED",totalAmountMinor=612000,totalBdtAmountMinor=10404000,exchangeRateToBdt="17.00",orderDate=1795219200000,expectedArrivalDate=1796860800000,totalCartons=3,totalGrossWeightKg="105",notes="Payment Terms: 30% Advance + 70% Balance")
        val products=listOf(Product("visual-p","c","Eye Mask with Ice Pack","আইস প্যাক আই মাস্ক","EM-001",purchasePriceMinor=1000,sellingPriceMinor=2000))
        val lines=listOf(PurchaseOrderItem("visual-i",order.id,products.first().id,quantity=500,unitPriceMinor=1000,totalAmountMinor=500000,totalBdtAmountMinor=8500000))
        var selected by mutableStateOf<PurchaseOrder?>(null)
        var query by mutableStateOf("")
        var status by mutableStateOf<PurchaseOrderStatus?>(null)
        var filter by mutableStateOf<String?>(null)
        var shared=0
        var edits=0
        val costs=listOf(
            ImportCostItem("visual-delivery","visual-batch",costCategory="CHINA_DOMESTIC_TRANSPORT",description="China delivery",amountMinor=15000,currency="CNY",exchangeRateToBdt="17",bdtAmountMinor=255000,purchaseOrderId=order.id),
            ImportCostItem("visual-freight","visual-batch",costCategory="INTERNATIONAL_FREIGHT",description="International freight",amountMinor=2940000,bdtAmountMinor=2940000,purchaseOrderId=order.id),
            ImportCostItem("visual-handling","visual-batch",costCategory="LOADING_UNLOADING",description="Handling",amountMinor=550000,bdtAmountMinor=550000,purchaseOrderId=order.id)
        )
        compose.setContent {
            val base=LocalContext.current
            val cfg=Configuration(LocalConfiguration.current).apply {setLocale(java.util.Locale("bn"))}
            CompositionLocalProvider(LocalContext provides base.createConfigurationContext(cfg),LocalConfiguration provides cfg,LocalERPOnNavigate provides {}) {
                AdvanceErpTheme(darkTheme=dark) {PoReferenceTheme {
                    Scaffold(topBar={ERPTopBar(title="ADVANCE ERP",subtitle="My Business",currentLanguage=AppLanguage.BANGLA,onOpenDrawer={},onToggleLanguage={})},bottomBar={ERPBottomNavigation("purchases",{})}) {padding ->
                        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                            poReferenceListItems(listOf(order,order.copy(id="v2",poNumber="PO-2026-0048",status="PARTIALLY_PAID"),order.copy(id="v3",poNumber="PO-2026-0047",status="DRAFT")),listOf(supplier),listOf(order,order.copy(id="v2",poNumber="PO-2026-0048",status="PARTIALLY_PAID"),order.copy(id="v3",poNumber="PO-2026-0047",status="DRAFT")),AppLanguage.BANGLA,query,status,filter,{query=it},{status=it},{filter=it},{},{},{selected=it})
                        }
                    }
                    selected?.let {po -> PurchaseOrderDetailDialog(po,supplier,lines,products,emptyList(),emptyList(),costItems=costs,onEditFullPo={edits++},language=AppLanguage.BANGLA,onDismiss={selected=null},onConfirmPo={},onMakePayment={},onCreateShipment={},onSharePdf={shared++},onCancelPo={_,_->})}
                } }
            }
        }
        capture(if(dark) "01_PO_List_dark" else "01_PO_List")
        compose.onNodeWithText("PO-2026-0049").performClick()
        compose.onNodeWithText("PO বিস্তারিত").assertExists()
        compose.onNodeWithText("৳ 141,490.00").assertExists()
        capture(if(dark) "06_PO_Details_dark" else "06_PO_Details")
        compose.onNodeWithContentDescription("সম্পাদনা").performClick()
        compose.runOnIdle {assertEquals(1,edits)}
        compose.onNodeWithContentDescription("PDF প্রিন্ট / শেয়ার").performClick()
        compose.runOnIdle {assertEquals(1,shared)}
        compose.onNodeWithText("পণ্য (1)").performClick()
        compose.onNodeWithText("Eye Mask with Ice Pack",substring=true).assertExists()
    }
    private fun capture(name:String) {
        compose.runOnIdle {val activity=compose.activity;activity.getSystemService(ActivityManager::class.java).appTasks.firstOrNull {it.taskInfo.taskId==activity.taskId}?.moveToFront()}
        compose.waitForIdle();SystemClock.sleep(350)
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("screencap -p /data/local/tmp/advance-$name.png").use {descriptor -> java.io.FileInputStream(descriptor.fileDescriptor).readBytes()}
        val image=requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        val directory=File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,"import-ui-verification").apply{mkdirs()}
        File(directory,"$name.png").outputStream().use {image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
        image.recycle()
    }
}
