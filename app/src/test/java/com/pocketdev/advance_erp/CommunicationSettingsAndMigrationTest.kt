package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.core.communication.ShareOutputFormat
import com.pocketdev.advance_erp.core.communication.defaultSelectedFormats
import com.pocketdev.advance_erp.data.entity.CommunicationSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CommunicationSettingsAndMigrationTest {

    @Test
    fun settingsDefaultSelectedFormats_respectsCombinationsAndNeverReturnsEmpty() {
        val defaultAll = CommunicationSettings()
        assertEquals(
            setOf(ShareOutputFormat.PDF, ShareOutputFormat.IMAGE_CARD, ShareOutputFormat.TEXT_MESSAGE),
            defaultAll.defaultSelectedFormats()
        )
        assertTrue(defaultAll.showProductPhoto)
        assertTrue(defaultAll.showVariantSku)
        assertTrue(defaultAll.showQrCode)
        assertTrue(defaultAll.includeSignature)

        val pdfAndImageOnly = defaultAll.copy(
            includePdf = true,
            includeImageCard = true,
            includeTextMessage = false
        )
        assertEquals(
            setOf(ShareOutputFormat.PDF, ShareOutputFormat.IMAGE_CARD),
            pdfAndImageOnly.defaultSelectedFormats()
        )

        val imageOnly = defaultAll.copy(
            includePdf = false,
            includeImageCard = true,
            includeTextMessage = false
        )
        assertEquals(setOf(ShareOutputFormat.IMAGE_CARD), imageOnly.defaultSelectedFormats())

        // Even if all booleans are off via legacy DB rows, defaultSelectedFormats safely falls back to TEXT_MESSAGE
        val allDisabled = defaultAll.copy(
            includePdf = false,
            includeImageCard = false,
            includeTextMessage = false
        )
        assertEquals(setOf(ShareOutputFormat.TEXT_MESSAGE), allDisabled.defaultSelectedFormats())
    }

    @Test
    fun legacyDuplicateDialogsRemovedAndAllModulesRouteToCentralizedEngine() {
        val mainRoot = File("src/main/java/com/pocketdev/advance_erp")
        assertTrue("Main source root must exist at ${mainRoot.absolutePath}", mainRoot.exists())

        // 1. Deleted duplicate files must remain removed
        assertFalse(File(mainRoot, "feature/sales/DueCollectionStatementShareDialog.kt").exists())
        assertFalse(File(mainRoot, "feature/sales/DueReminderDialog.kt").exists())
        assertFalse(File(mainRoot, "core/util/StatementShareHelper.kt").exists())

        // 2. InvoicePdfGenerator preserves thermal ESC/POS printing and drops legacy A4 generators
        val invoicePdfGen = File(mainRoot, "core/util/InvoicePdfGenerator.kt").readText()
        assertTrue(invoicePdfGen.contains("fun generateThermalReceiptPdf"))
        assertTrue(invoicePdfGen.contains("fun createSharePdfIntent"))
        assertFalse(invoicePdfGen.contains("fun generateInvoicePdf"))
        assertFalse(invoicePdfGen.contains("fun generatePaymentReceiptPdf"))

        // 3. All active screens integrate centralized CommunicationDialog / CommunicationRequest
        val salesScreen = File(mainRoot, "feature/sales/SalesScreen.kt").readText()
        assertTrue(salesScreen.contains("CommunicationDialog("))
        assertTrue(salesScreen.contains("CommunicationModule.REMINDER"))
        assertFalse(salesScreen.contains("DueCollectionStatementShareDialog"))
        assertFalse(salesScreen.contains("DueReminderDialog"))

        val contactsScreen = File(mainRoot, "feature/contacts/ContactsScreen.kt").readText()
        val contactsVm = File(mainRoot, "feature/contacts/ContactsViewModel.kt").readText()
        assertTrue(contactsScreen.contains("CommunicationDialog("))
        assertFalse(contactsScreen.contains("DueReminderDialog"))
        assertFalse(contactsVm.contains("StatementShareHelper"))
        assertTrue(contactsVm.contains("CommunicationRequest(CommunicationModule.PAYMENT"))

        val ccScreen = File(mainRoot, "feature/controlcenter/BusinessControlCenterScreen.kt").readText()
        val ccVm = File(mainRoot, "feature/controlcenter/BusinessControlCenterViewModel.kt").readText()
        assertTrue(ccScreen.contains("CommunicationDialog("))
        assertTrue(ccScreen.contains("onShareSmartDocument"))
        assertFalse(ccVm.contains("StatementShareHelper"))
        assertFalse(ccVm.contains("fun shareReminder("))

        val importsScreen = File(mainRoot, "feature/imports/ImportManagementScreen.kt").readText()
        assertTrue(importsScreen.contains("CommunicationDialog("))

        // 4. Settings exposes Communication Hub and persists website + tagline on BusinessProfile
        val settingsScreen = File(mainRoot, "feature/settings/SettingsScreen.kt").readText()
        val settingsVm = File(mainRoot, "feature/settings/SettingsViewModel.kt").readText()
        assertTrue(settingsScreen.contains("SettingsCommunicationCard"))
        assertTrue(settingsScreen.contains("website"))
        assertTrue(settingsScreen.contains("tagline"))
        assertTrue(settingsVm.contains("website: String?"))
        assertTrue(settingsVm.contains("tagline: String?"))
    }
}
