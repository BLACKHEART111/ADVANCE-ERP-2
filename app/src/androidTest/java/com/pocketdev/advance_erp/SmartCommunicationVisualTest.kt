package com.pocketdev.advance_erp

import android.app.ActivityManager
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.PixelCopy
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.inspector.WindowInspector
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.window.DialogWindowProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.security.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Pure app-window / dialog-window visual QA using PixelCopy on com.pocketdev.advance_erp2.communicationqa.
 * Never uses screen-level screencap and never touches production com.pocketdev.advance_erp2.
 */
@RunWith(AndroidJUnit4::class)
class SmartCommunicationVisualTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun capturePureCommunicationQaWindowsAndPreservedScreens() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals(
            "Visual QA MUST run ONLY on com.pocketdev.advance_erp2.communicationqa",
            "com.pocketdev.advance_erp2.communicationqa",
            context.packageName
        )

        runBlocking {
            val prefs = compose.activity.preferencesRepository
            prefs.setAppLock(enabled = false)
            prefs.setUserRole(UserRole.OWNER_ADMIN)
            prefs.setLanguage(AppLanguage.BANGLA)
        }
        compose.waitForIdle()
        SystemClock.sleep(500)

        val outDir = File(context.cacheDir, "qa_previews").apply { mkdirs() }

        fun findTopDialogWindow(): Window? {
            fun findIn(v: View): Window? {
                if (v is DialogWindowProvider) return v.window
                if (v is ViewGroup) {
                    for (i in 0 until v.childCount) findIn(v.getChildAt(i))?.let { return it }
                }
                return null
            }
            return WindowInspector.getGlobalWindowViews()
                .mapNotNull { findIn(it) }
                .lastOrNull {
                    val d = it.decorView
                    d.isAttachedToWindow && d.isShown && d.width > 0 && d.height > 0
                }
        }

        fun captureWindow(preferDialog: Boolean, fileName: String) {
            compose.waitForIdle()
            SystemClock.sleep(500)
            compose.runOnIdle {
                compose.activity.getSystemService(ActivityManager::class.java).appTasks
                    .firstOrNull { it.taskInfo.taskId == compose.activity.taskId }?.moveToFront()
            }
            var captured: Bitmap? = null
            for (attempt in 1..6) {
                val latch = CountDownLatch(1)
                var result = PixelCopy.ERROR_SOURCE_NO_DATA
                var candidate: Bitmap? = null
                compose.runOnIdle {
                    val targetWindow = if (preferDialog) {
                        findTopDialogWindow() ?: compose.activity.window
                    } else {
                        compose.activity.window
                    }
                    val decor = targetWindow.decorView
                    if (decor.width <= 0 || decor.height <= 0) {
                        latch.countDown()
                        return@runOnIdle
                    }
                    candidate = Bitmap.createBitmap(decor.width, decor.height, Bitmap.Config.ARGB_8888)
                    try {
                        PixelCopy.request(targetWindow, candidate!!, { res ->
                            result = res
                            latch.countDown()
                        }, Handler(Looper.getMainLooper()))
                    } catch (_: Throwable) {
                        latch.countDown()
                    }
                }
                assertTrue("PixelCopy timeout on $fileName", latch.await(5, TimeUnit.SECONDS))
                if (result == PixelCopy.SUCCESS && candidate != null) {
                    captured = candidate
                    break
                }
                candidate?.recycle()
                SystemClock.sleep(200)
            }
            val bmp = requireNotNull(captured) { "Failed to capture window for $fileName" }
            val colors = mutableSetOf<Int>()
            for (y in 0 until bmp.height step 20) {
                for (x in 0 until bmp.width step 20) {
                    colors.add(bmp.getPixel(x, y))
                }
            }
            assertTrue("Captured window for $fileName looks blank", colors.size > 4)
            File(outDir, fileName).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 95, it) }
            bmp.recycle()
        }

        fun matcherFor(text: String, substring: Boolean, inDialog: Boolean) =
            if (inDialog) {
                hasAnyAncestor(isDialog()) and hasText(text, substring = substring)
            } else {
                hasText(text, substring = substring)
            }

        fun hasAnyText(vararg candidates: String, substring: Boolean = true, inDialog: Boolean = false): Boolean =
            candidates.any { text ->
                compose.onAllNodes(matcherFor(text, substring, inDialog)).fetchSemanticsNodes().isNotEmpty()
            }

        fun clickAny(substring: Boolean = true, vararg candidates: String) {
            compose.waitUntil(timeoutMillis = 8_000) {
                hasAnyText(*candidates, substring = substring, inDialog = false)
            }
            for (text in candidates) {
                val nodes = compose.onAllNodes(matcherFor(text, substring, inDialog = false))
                if (nodes.fetchSemanticsNodes().isNotEmpty()) {
                    val target = nodes.onFirst()
                    runCatching { target.performScrollTo() }
                    target.performClick()
                    compose.waitForIdle()
                    return
                }
            }
        }

        fun clickInDialog(substring: Boolean = true, vararg candidates: String) {
            compose.waitUntil(timeoutMillis = 8_000) {
                hasAnyText(*candidates, substring = substring, inDialog = true)
            }
            for (text in candidates) {
                val nodes = compose.onAllNodes(matcherFor(text, substring, inDialog = true))
                if (nodes.fetchSemanticsNodes().isNotEmpty()) {
                    val target = nodes.onFirst()
                    runCatching { target.performScrollTo() }
                    target.performClick()
                    compose.waitForIdle()
                    return
                }
            }
        }

        fun clickFabAddCustomer() {
            compose.waitUntil(timeoutMillis = 8_000) {
                compose.onAllNodesWithContentDescription("নতুন গ্রাহক যোগ করুন").fetchSemanticsNodes().isNotEmpty() ||
                    compose.onAllNodesWithContentDescription("Add Customer").fetchSemanticsNodes().isNotEmpty()
            }
            val nodes = compose.onAllNodesWithContentDescription("নতুন গ্রাহক যোগ করুন").let {
                if (it.fetchSemanticsNodes().isNotEmpty()) it else compose.onAllNodesWithContentDescription("Add Customer")
            }
            nodes.onFirst().performClick()
            compose.waitForIdle()
        }

        fun inputIntoField(value: String, vararg labelPrefixes: String) {
            compose.waitUntil(timeoutMillis = 8_000) {
                labelPrefixes.any { prefix ->
                    compose.onAllNodes(hasSetTextAction() and hasText(prefix, substring = true))
                        .fetchSemanticsNodes().isNotEmpty()
                }
            }
            for (prefix in labelPrefixes) {
                val nodes = compose.onAllNodes(hasSetTextAction() and hasText(prefix, substring = true))
                if (nodes.fetchSemanticsNodes().isNotEmpty()) {
                    val field = nodes.onFirst()
                    runCatching { field.performScrollTo() }
                    field.performTextInput(value)
                    compose.waitForIdle()
                    return
                }
            }
        }

        // 1. Navigate More -> Customers ("আরও" -> "গ্রাহকবৃন্দ")
        clickAny(substring = false, "আরও", "More")
        compose.waitUntil(timeoutMillis = 8_000) {
            hasAnyText("গ্রাহকবৃন্দ", "Customers", substring = true) ||
                compose.onAllNodes(hasScrollAction()).fetchSemanticsNodes().isNotEmpty()
        }
        if (!hasAnyText("গ্রাহকবৃন্দ", "Customers", substring = true)) {
            runCatching {
                compose.onAllNodes(hasScrollAction()).onFirst()
                    .performScrollToNode(hasText("গ্রাহকবৃন্দ", substring = true))
            }.onFailure {
                compose.onAllNodes(hasScrollAction()).onFirst()
                    .performScrollToNode(hasText("Customers", substring = true))
            }
            compose.waitForIdle()
        }
        clickAny(substring = true, "গ্রাহকবৃন্দ", "Customers")

        // Wait for Contacts screen FAB
        compose.waitUntil(timeoutMillis = 8_000) {
            compose.onAllNodesWithContentDescription("নতুন গ্রাহক যোগ করুন").fetchSemanticsNodes().isNotEmpty() ||
                compose.onAllNodesWithContentDescription("Add Customer").fetchSemanticsNodes().isNotEmpty()
        }
        compose.waitForIdle()
        SystemClock.sleep(600)

        // 2. Ensure dummy QA customer "নমুনা গ্রাহক QA" exists via Contacts UI
        if (!hasAnyText("নমুনা গ্রাহক QA", substring = true)) {
            val searchNodes = compose.onAllNodes(
                hasSetTextAction() and (hasText("নাম / কোড", substring = true))
            )
            if (searchNodes.fetchSemanticsNodes().isNotEmpty()) {
                searchNodes.onFirst().performTextInput("নমুনা গ্রাহক QA")
                compose.waitForIdle()
                SystemClock.sleep(400)
            }
            val foundViaSearch = hasAnyText("বকেয়া তাগাদা", "Remind", substring = false)
            if (searchNodes.fetchSemanticsNodes().isNotEmpty()) {
                searchNodes.onFirst().performTextClearance()
                compose.waitForIdle()
                SystemClock.sleep(300)
            }
            if (!foundViaSearch) {
                clickFabAddCustomer()
                compose.waitUntil(timeoutMillis = 8_000) {
                    hasAnyText("নতুন গ্রাহক নিবন্ধন", "Add Customer", substring = true)
                }
                inputIntoField("নমুনা গ্রাহক QA", "গ্রাহকের নাম", "Name")
                inputIntoField("01700000000", "মোবাইল নম্বর", "Phone")
                inputIntoField("5000", "পূর্বের বকেয়া", "Opening / Previous Due")
                runCatching { Espresso.closeSoftKeyboard() }
                clickAny(substring = false, "সংরক্ষণ", "Save")
                compose.waitUntil(timeoutMillis = 10_000) {
                    hasAnyText("নমুনা গ্রাহক QA", substring = true)
                }
            }
        }

        // Capture preserved Contacts screen (pure activity window)
        SystemClock.sleep(600)
        captureWindow(preferDialog = false, "qa_ui_contacts_screen.png")

        // Ensure dummy QA customer card with "বকেয়া তাগাদা" button is visible before opening reminder
        if (!hasAnyText("বকেয়া তাগাদা", "Remind", substring = false)) {
            inputIntoField("নমুনা গ্রাহক QA", "নাম / কোড", "Search")
            compose.waitUntil(timeoutMillis = 8_000) {
                hasAnyText("বকেয়া তাগাদা", "Remind", substring = false)
            }
        }

        // 3. Open Smart Communication Dialog via Contacts "বকেয়া তাগাদা" (REMINDER) in Light mode
        runBlocking { compose.activity.preferencesRepository.setTheme("light") }
        clickAny(substring = false, "বকেয়া তাগাদা", "Remind")
        compose.waitUntil(timeoutMillis = 8_000) { findTopDialogWindow() != null }
        compose.waitUntil(timeoutMillis = 15_000) {
            hasAnyText("WhatsApp-এ শেয়ার", "Share via WhatsApp", substring = true, inDialog = true)
        }
        SystemClock.sleep(900)
        captureWindow(preferDialog = true, "qa_ui_share_send_light.png")

        // 3b. Open Share Preview bottom sheet/dialog & capture PDF, Image Card, Text tabs
        clickInDialog(true, "প্রিভিউ দেখুন", "Preview")
        compose.waitUntil(timeoutMillis = 15_000) {
            hasAnyText("টেক্সট", "Text", substring = false, inDialog = true) &&
                hasAnyText("ইমেজ", "Image", substring = false, inDialog = true)
        }
        SystemClock.sleep(600)
        captureWindow(preferDialog = true, "qa_ui_preview_tab_pdf.png")

        clickInDialog(false, "ইমেজ", "Image")
        SystemClock.sleep(600)
        captureWindow(preferDialog = true, "qa_ui_preview_tab_image.png")

        clickInDialog(false, "টেক্সট", "Text")
        SystemClock.sleep(500)
        captureWindow(preferDialog = true, "qa_ui_preview_tab_text.png")

        // Dismiss Share Preview sheet back to Share & Send dialog
        val previewCloseMatcher = hasAnyAncestor(isDialog()) and
            (hasContentDescription("প্রিভিউ বন্ধ") or hasContentDescription("Close preview"))
        compose.onAllNodes(previewCloseMatcher).onLast().performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            hasAnyText("ডকুমেন্ট", "Documents", substring = false, inDialog = true)
        }
        SystemClock.sleep(400)
        captureWindow(preferDialog = true, "qa_ui_comm_preview.png")

        // 4. Documents tab inside dialog
        clickInDialog(false, "ডকুমেন্ট", "Documents")
        compose.waitUntil(timeoutMillis = 5_000) {
            hasAnyText("নমুনা গ্রাহক QA", substring = true, inDialog = true)
        }
        SystemClock.sleep(400)
        captureWindow(preferDialog = true, "qa_ui_comm_documents.png")

        // 5. Settings tab inside dialog (Light & Dark)
        clickInDialog(false, "সেটিংস", "Settings")
        SystemClock.sleep(400)
        captureWindow(preferDialog = true, "qa_ui_comm_settings.png")

        // 6. Templates tab inside dialog
        clickInDialog(false, "টেমপ্লেট", "Templates")
        SystemClock.sleep(500)
        captureWindow(preferDialog = true, "qa_ui_comm_templates.png")

        // 7. History tab inside dialog
        clickInDialog(false, "ইতিহাস", "History")
        SystemClock.sleep(400)
        captureWindow(preferDialog = true, "qa_ui_comm_history.png")

        // 7b. Dark mode capture of Settings & Share & Send
        runBlocking { compose.activity.preferencesRepository.setTheme("dark") }
        clickInDialog(false, "সেটিংস", "Settings")
        SystemClock.sleep(600)
        captureWindow(preferDialog = true, "qa_ui_comm_settings_dark.png")
        clickInDialog(false, "শেয়ার", "Share")
        SystemClock.sleep(600)
        captureWindow(preferDialog = true, "qa_ui_share_send_dark.png")
        runBlocking { compose.activity.preferencesRepository.setTheme("system") }

        // 8. Close dialog & capture Sales screen (pure activity window)
        val closeMatcher = hasAnyAncestor(isDialog()) and
            (hasContentDescription("বন্ধ") or hasContentDescription("Close"))
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodes(closeMatcher).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodes(closeMatcher).onFirst().performClick()
        compose.waitUntil(timeoutMillis = 5_000) { findTopDialogWindow() == null }

        clickAny(substring = false, "বিক্রয়", "বিক্রয়", "Sales", "Sell")
        SystemClock.sleep(600)
        captureWindow(preferDialog = false, "qa_ui_sales_screen.png")

        // 9. Navigate to Settings -> Backup & Restore -> Selective Module Data Clear Dialog (Light & Dark)
        runBlocking { compose.activity.preferencesRepository.setTheme("light") }
        val drawerDesc = compose.activity.getString(R.string.menu_drawer_open)
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithContentDescription(drawerDesc).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithContentDescription(drawerDesc).onFirst().performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            hasAnyText("সেটিংস", "Settings", substring = false) ||
                compose.onAllNodes(hasScrollAction()).fetchSemanticsNodes().isNotEmpty()
        }
        if (!hasAnyText("সেটিংস", "Settings", substring = false)) {
            runCatching {
                compose.onAllNodes(hasScrollAction()).onFirst()
                    .performScrollToNode(hasText("সেটিংস", substring = false))
            }.onFailure {
                compose.onAllNodes(hasScrollAction()).onFirst()
                    .performScrollToNode(hasText("Settings", substring = false))
            }
            compose.waitForIdle()
        }
        clickAny(substring = false, "সেটিংস", "Settings")

        compose.waitUntil(timeoutMillis = 8_000) {
            compose.onAllNodesWithTag("settings_cat_appearance").fetchSemanticsNodes().isNotEmpty() &&
                compose.onAllNodes(hasScrollAction()).fetchSemanticsNodes().isNotEmpty()
        }
        SystemClock.sleep(500)
        val scrollNodes = compose.onAllNodes(hasScrollAction())
        val scrollCount = scrollNodes.fetchSemanticsNodes().size
        for (i in 0 until scrollCount) {
            if (runCatching { scrollNodes[i].performScrollToNode(hasTestTag("settings_cat_backup")) }.isSuccess) {
                break
            }
        }
        compose.onNodeWithTag("settings_cat_backup").performClick()
        compose.waitForIdle()
        val afterExpandScrolls = compose.onAllNodes(hasScrollAction())
        val afterCount = afterExpandScrolls.fetchSemanticsNodes().size
        for (i in 0 until afterCount) {
            if (runCatching {
                    afterExpandScrolls[i].performScrollToNode(hasText("মডিউল সিলেক্ট করে ডাটা ক্লিয়ার করুন", substring = true))
                }.isSuccess ||
                runCatching {
                    afterExpandScrolls[i].performScrollToNode(hasText("Select Modules & Clear Data", substring = true))
                }.isSuccess
            ) {
                break
            }
        }
        compose.waitUntil(timeoutMillis = 8_000) {
            hasAnyText("মডিউল সিলেক্ট করে ডাটা ক্লিয়ার করুন", "Select Modules & Clear Data", substring = true)
        }
        clickAny(true, "মডিউল সিলেক্ট করে ডাটা ক্লিয়ার করুন", "Select Modules & Clear Data")

        compose.waitUntil(timeoutMillis = 8_000) {
            findTopDialogWindow() != null &&
                hasAnyText("স্মার্ট ফুল রিসেট", "Smart Full Reset", substring = true, inDialog = true)
        }
        SystemClock.sleep(800)
        captureWindow(preferDialog = true, "qa_ui_selective_clear_step1_light.png")

        runBlocking { compose.activity.preferencesRepository.setTheme("dark") }
        SystemClock.sleep(600)
        captureWindow(preferDialog = true, "qa_ui_selective_clear_step1_dark.png")

        clickInDialog(true, "পরবর্তী: যাচাই", "Next: Verify")
        compose.waitUntil(timeoutMillis = 5_000) {
            hasAnyText("CLEAR", "পিন", "PIN", substring = true, inDialog = true)
        }
        SystemClock.sleep(500)
        captureWindow(preferDialog = true, "qa_ui_selective_clear_step2_dark.png")

        runBlocking { compose.activity.preferencesRepository.setTheme("light") }
        SystemClock.sleep(500)
        captureWindow(preferDialog = true, "qa_ui_selective_clear_step2_light.png")

        runBlocking { compose.activity.preferencesRepository.setTheme("system") }
        compose.onAllNodes(closeMatcher).onFirst().performClick()
        compose.waitUntil(timeoutMillis = 5_000) { findTopDialogWindow() == null }
    }
}
