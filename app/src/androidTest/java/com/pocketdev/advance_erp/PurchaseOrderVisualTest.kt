package com.pocketdev.advance_erp

import android.graphics.Bitmap
import android.app.ActivityManager
import android.view.PixelCopy
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.inspector.WindowInspector
import androidx.compose.ui.window.DialogWindowProvider
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.activity.ComponentActivity
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import com.pocketdev.advance_erp.core.designsystem.AdvanceErpTheme
import com.pocketdev.advance_erp.data.entity.*
import com.pocketdev.advance_erp.domain.repository.*
import com.pocketdev.advance_erp.feature.imports.CreatePurchaseOrderDialog
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import java.io.File

/** Renders the real production form on the connected phone; screenshots contain test fixtures only. */
class PurchaseOrderVisualTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @Test fun banglaMixedCartonFlow() = verify(true,1f)
    @Test fun englishMixedCartonFlow() = verify(false,1f)
    @Test fun banglaLargeFontFlow() = verify(true,1.3f)
    @Test fun localPurchaseDoesNotApplyInternationalFreight() = verify(true,1f,"BDT")
    @Test fun paidOrderRetainsMetadataEditing() = verify(true,1f,metadataOnly=true)
    private fun verify(bn: Boolean, fontScale: Float, currency: String = "CNY", metadataOnly: Boolean = false) {
        val product=Product("p","c","Doctor Insole","ডক্টর ইনসোল","DOC",purchasePriceMinor=4500,sellingPriceMinor=9000)
        val supplier=Supplier("s","S","UI TEST SUPPLIER",phone="",address="China")
        val variants=listOf("S","M").map { ProductVariant(it,"p","DOC-$it",size=it,variantPriceMinor=9000,variantCostMinor=4500) }
        val items=listOf("S" to 20,"M" to 10).map { (v,q) -> PurchaseOrderItem("i-$v","po","p",v,q,4500,q*4500L,q*76500L,cartonQuantity=1,grossWeightKg=if(v=="S") "12" else "3",netWeightKg=if(v=="S") "12" else "3") }
        val cartons=items.map { ShipmentCarton(it.id,"batch","CTN-01","p",it.variantId,it.quantity,
            lengthCm="50",widthCm="40",heightCm="30",grossWeightKg=it.grossWeightKg,netWeightKg=if(it.variantId=="S") "11" else "2.5",cbm="0.06",purchaseOrderId="po",isMixedCarton=true,totalCartonQuantity=30,cartonGrossWeightKg="0.000") }
        var metadataSaved = false
        var savedRates: Map<String, Long>? = null
        var savedItems: List<PurchaseOrderItemDraft>? = null
        var savedCartons: List<PoMixedCartonDraft>? = null
        compose.setContent {
            val density=LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density,fontScale)) {
                AdvanceErpTheme(darkTheme=false) {
                    CreatePurchaseOrderDialog(listOf(supplier),listOf(product),variants,
                        categories=listOf(ProductCategory("c","Accessories","অ্যাকসেসরিজ","ACC",defaultFreightRatePerKgBdtMinor=12000)),
                        isBangla=bn,onDismiss={},metadataOnly=metadataOnly,
                        onUpdateMetadata={ _,_ -> metadataSaved=true },editingOrder=PurchaseOrder("po","PO-UI-TEST","s",currency=currency,exchangeRateToBdt=if(currency=="BDT") "1" else "17",paidAmountMinor=if(metadataOnly) 100L else 0L),
                        initialEditingItems=items,initialEditingCartons=cartons,
                        onUpdatePo={ _,_,_,_,lines,_,_,boxes,_,rates,_,_,_ -> savedItems=lines; savedCartons=boxes; savedRates=rates },
                        onSavePo={ _,_,_,_,_,_,_,_,_,_,_,_ -> })
                }
            }
        }
        if(metadataOnly) {
            compose.onNodeWithText("1. তথ্য").assertDoesNotExist()
            compose.onNodeWithText("আপডেট").performClick()
            compose.runOnIdle { assertTrue(metadataSaved); assertNull(savedItems) }
            return
        }
        val title=if(bn) "ক্রয়াদেশ সম্পাদনা" else "Edit purchase order"
        val labels=if(bn) listOf("1. তথ্য","2. পণ্য","3. প্যাকিং","4. হিসাব") else listOf("1. Details","2. Items","3. Packing","4. Review")
        val prefix=(if(bn) "bn" else "en")+"-"+fontScale+"-"+currency
        fun capture(name: String) {
            compose.runOnIdle {
                val activity=compose.activity
                activity.getSystemService(ActivityManager::class.java).appTasks
                    .firstOrNull { it.taskInfo.taskId==activity.taskId }?.moveToFront()
            }
            compose.waitForIdle()
            SystemClock.sleep(350)
            var dialogWindow: Window? = null
            val size=IntArray(2)
            compose.runOnIdle {
                fun findWindow(view: View): Window? {
                    if(view is DialogWindowProvider) return view.window
                    if(view is ViewGroup) for(index in 0 until view.childCount) {
                        findWindow(view.getChildAt(index))?.let { return it }
                    }
                    return null
                }
                dialogWindow=WindowInspector.getGlobalWindowViews().mapNotNull { findWindow(it) }.lastOrNull()
                dialogWindow?.decorView?.let { size[0]=it.width; size[1]=it.height }
            }
            val window=requireNotNull(dialogWindow) { "Production dialog window was not found" }
            val bitmap=Bitmap.createBitmap(size[0],size[1],Bitmap.Config.ARGB_8888)
            val ready=CountDownLatch(1)
            var copyResult=-1
            PixelCopy.request(window,bitmap,{ result -> copyResult=result; ready.countDown() },Handler(Looper.getMainLooper()))
            assertTrue("Dialog PixelCopy timed out",ready.await(5,TimeUnit.SECONDS))
            assertEquals("Dialog PixelCopy must succeed",PixelCopy.SUCCESS,copyResult)
            val colors=mutableSetOf<Int>()
            for (y in 0 until bitmap.height step 16) for (x in 0 until bitmap.width step 16) colors.add(bitmap.getPixel(x,y))
            val dir=File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,"import-ui-verification").apply { mkdirs() }
            File(dir,"$prefix-$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
            assertTrue("Screenshot must contain rendered UI, not a blank inactive window",colors.size > 8)
        }
        labels.forEachIndexed { index,label ->
            compose.onNodeWithText(label).performClick()
            capture("step-${index+1}")
            if(index==1 || index==2 || index==3) {
                val root=compose.onAllNodes(isRoot()).filter(hasAnyDescendant(hasText(title))).onFirst()
                repeat(3) { compose.onAllNodes(hasScrollAction()).onFirst().performTouchInput { swipeUp() }; compose.waitForIdle() }
                capture("step-${index+1}-scrolled")
            }
        }
        compose.onNodeWithText(if(bn) "ড্রাফট" else "Draft").performClick()
        compose.runOnIdle {
            if(currency=="BDT") assertTrue(savedRates!!.values.all { it==0L })
            assertEquals(30,savedItems!!.sumOf { it.quantity })
            assertEquals(1,savedCartons!!.size)
            assertEquals(0,java.math.BigDecimal("13.5").compareTo(savedCartons!!.single().netWeightKg))
            assertEquals(0,java.math.BigDecimal("13.5").compareTo(savedItems!!.sumOf { it.netWeightKg }))
            assertEquals(0,java.math.BigDecimal("50").compareTo(savedCartons!!.single().lengthCm))
            assertEquals(0,java.math.BigDecimal("0.06").compareTo(savedCartons!!.single().cbm))
            assertEquals(listOf("12","3"),savedCartons!!.single().lines.map { it.manualGrossWeightKg!!.stripTrailingZeros().toPlainString() })
        }
    }
}
