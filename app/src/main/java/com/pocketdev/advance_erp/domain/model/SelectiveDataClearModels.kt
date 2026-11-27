package com.pocketdev.advance_erp.domain.model

import com.pocketdev.advance_erp.core.security.UserRole

/**
 * Selectable modules for the Advanced Module-Based Data Clear flow.
 * Master entities (Products, Variants, Photos, Customers, Suppliers, Warehouses,
 * Chart of Accounts names, and PO headers/items) are preserved while quantities,
 * packaging, receipts, payments, and ledger balances can be cleanly reset.
 */
enum class DataClearModule(
    val titleEn: String,
    val titleBn: String,
    val keepNoteEn: String,
    val keepNoteBn: String,
    val resetNoteEn: String,
    val resetNoteBn: String
) {
    INVENTORY_STOCK_RESET(
        titleEn = "Inventory Stock Quantities",
        titleBn = "ইনভেন্টরি স্টক কোয়ান্টিটি রিসেট",
        keepNoteEn = "Keeps all products, variants (color/size/SKU/price), categories, photos & warehouses.",
        keepNoteBn = "সকল প্রোডাক্ট, ভ্যারিয়েন্ট (কালার/সাইজ/SKU/মূল্য), ক্যাটাগরি, ছবি ও ওয়্যারহাউস অক্ষত থাকবে।",
        resetNoteEn = "Resets all warehouse stock quantities to 0 and clears stock movements, transfers, reconciliations & FIFO layers.",
        resetNoteBn = "সকল পণ্যের স্টক পরিমাণ ০ হবে এবং স্টক মুভমেন্ট, ট্রান্সফার, রিকনসিলিয়েশন ও FIFO লেয়ার ক্লিয়ার হবে।"
    ),
    PO_RESET_KEEP_STRUCTURE(
        titleEn = "PO Receiving & Payment Reset (Keep Full PO)",
        titleBn = "PO রিসিভিং ও পেমেন্ট রিসেট (পুরো PO অক্ষত থাকবে)",
        keepNoteEn = "Keeps entire PO intact: products, ordered quantities, unit/total prices, cartons & packaging.",
        keepNoteBn = "পুরো PO হুবহু অক্ষত থাকবে (প্রোডাক্ট, অর্ডার কোয়ান্টিটি, দাম, মোট বিল, কার্টন ও প্যাকেজিং কিছুই মুছবে না)।",
        resetNoteEn = "Unreceives received POs back to CONFIRMED (receivedQty = 0), clears warehouse receipts & resets supplier payments (paid = 0).",
        resetNoteBn = "শুধুমাত্র রিসিভ করা থাকলে তা আনরিসিভ হয়ে CONFIRMED অবস্থায় ফিরবে (receivedQty = 0) এবং সাপ্লায়ার পেমেন্ট রিসেট হবে।"
    ),
    SALES_FULL_CLEAR(
        titleEn = "Sales Invoices & Customer Payments (Full Clear)",
        titleBn = "সেলস ইনভয়েস ও কাস্টমার পেমেন্ট (ফুল ক্লিয়ার)",
        keepNoteEn = "Keeps all customer names, codes, phones & profile photos.",
        keepNoteBn = "সকল কাস্টমারের নাম, কোড, ফোন ও ছবি অক্ষত থাকবে।",
        resetNoteEn = "Deletes all sales invoices, quotations, delivery challans, returns & customer payments.",
        resetNoteBn = "সকল সেলস ইনভয়েস, কোটেশন, ডেলিভারি চালান, রিটার্ন ও কাস্টমার পেমেন্ট সম্পূর্ণ মুছে যাবে।"
    ),
    LEDGER_AMOUNTS_RESET(
        titleEn = "Ledger & Account Balances",
        titleBn = "লেজার ও হিসাবের ব্যালেন্স রিসেট",
        keepNoteEn = "Keeps all account names (Cash/Bank/MFS/Expenses) and customer/supplier names.",
        keepNoteBn = "সকল হিসাব খাতের নাম (ক্যাশ/ব্যাংক/বিকাশ/খরচ) এবং কাস্টমার-সাপ্লায়ারের নাম অক্ষত থাকবে।",
        resetNoteEn = "Clears all journal entries, expenses, bank transfers/reconciliations & resets opening/party balances to ৳0.00.",
        resetNoteBn = "সকল জার্নাল, খরচ ভাউচার, ট্রান্সফার ও ব্যাংক রিকনসিলিয়েশন মুছে সকল ব্যালেন্স ৳০.০০ হবে।"
    ),
    IMPORT_VAULT_CLEAR(
        titleEn = "Import Vault Records & Files",
        titleBn = "ইমপোর্ট ভল্ট রেকর্ড ও সংযুক্তি",
        keepNoteEn = "Keeps PO attachments and product media.",
        keepNoteBn = "PO সংযুক্তি ও প্রোডাক্টের ছবি অক্ষত থাকবে।",
        resetNoteEn = "Deletes all Import Vault entries and stored vault attachment files.",
        resetNoteBn = "ইমপোর্ট ভল্টের সকল এন্ট্রি ও সংরক্ষিত ফাইল মুছে যাবে।"
    ),
    COMMUNICATION_LOGS_CLEAR(
        titleEn = "Smart Communication Logs & Export Cache",
        titleBn = "কমিউনিকেশন হিস্টোরি ও এক্সপোর্ট ক্যাশ",
        keepNoteEn = "Keeps your WhatsApp preferences & custom message templates.",
        keepNoteBn = "WhatsApp সেটিংস ও কাস্টম মেসেজ টেমপ্লেট অক্ষত থাকবে।",
        resetNoteEn = "Clears share history logs, historical snapshots & cached PDF/PNG files.",
        resetNoteBn = "শেয়ার হিস্টোরি, স্ন্যাপশট ও ক্যাশ করা PDF/PNG ফাইল মুছে যাবে।"
    ),
    AUDIT_AND_DRAFTS_CLEAR(
        titleEn = "Audit Logs & Unsaved Drafts",
        titleBn = "অডিট লগ ও অসংরক্ষিত ড্রাফট",
        keepNoteEn = "Keeps backup history & backup audit trail intact.",
        keepNoteBn = "ব্যাকআপ হিস্টোরি ও ব্যাকআপ অডিট লগ অক্ষত থাকবে।",
        resetNoteEn = "Clears transaction audit logs, saved form suggestions & unsaved form drafts.",
        resetNoteBn = "লেনদেনের অডিট লগ, অটো-সাজেশন ও অসংরক্ষিত ফর্ম ড্রাফট মুছে যাবে।"
    )
}

enum class DataClearPreset(val titleEn: String, val titleBn: String) {
    SMART_FULL_RESET("Smart Full Reset", "স্মার্ট ফুল রিসেট"),
    CORE_OPERATIONS_RESET("Core Operations Only", "শুধু মূল অপারেশন রিসেট"),
    CUSTOM("Custom Selection", "কাস্টম নির্বাচন")
}

data class ModuleImpactCount(
    val module: DataClearModule,
    val primaryCount: Int,
    val secondaryCount: Int = 0,
    val badgeTextEn: String,
    val badgeTextBn: String
)

data class DataClearImpactPreview(
    val moduleCounts: Map<DataClearModule, ModuleImpactCount>,
    val preservedProductCount: Int,
    val preservedVariantCount: Int,
    val preservedPoCount: Int,
    val preservedAccountCount: Int,
    val preservedCustomerCount: Int,
    val preservedSupplierCount: Int,
    val poReceiptLayerCount: Int
)

data class SelectiveDataClearRequest(
    val selectedModules: Set<DataClearModule>,
    val createSafetyBackup: Boolean = true,
    val operatorRole: UserRole = UserRole.OWNER_ADMIN
)

data class SelectiveDataClearResult(
    val clearedModules: Set<DataClearModule>,
    val safetyBackupRecordId: String?,
    val safetyBackupFileName: String?,
    val summaryEn: String,
    val summaryBn: String
)

object SelectiveDataClearPlanner {

    fun modulesForPreset(preset: DataClearPreset): Set<DataClearModule> = when (preset) {
        DataClearPreset.SMART_FULL_RESET -> setOf(
            DataClearModule.INVENTORY_STOCK_RESET,
            DataClearModule.PO_RESET_KEEP_STRUCTURE,
            DataClearModule.SALES_FULL_CLEAR,
            DataClearModule.LEDGER_AMOUNTS_RESET,
            DataClearModule.COMMUNICATION_LOGS_CLEAR,
            DataClearModule.AUDIT_AND_DRAFTS_CLEAR
        )
        DataClearPreset.CORE_OPERATIONS_RESET -> setOf(
            DataClearModule.INVENTORY_STOCK_RESET,
            DataClearModule.PO_RESET_KEEP_STRUCTURE,
            DataClearModule.SALES_FULL_CLEAR,
            DataClearModule.LEDGER_AMOUNTS_RESET
        )
        DataClearPreset.CUSTOM -> emptySet()
    }

    fun detectMatchingPreset(selected: Set<DataClearModule>): DataClearPreset = when (selected) {
        modulesForPreset(DataClearPreset.SMART_FULL_RESET) -> DataClearPreset.SMART_FULL_RESET
        modulesForPreset(DataClearPreset.CORE_OPERATIONS_RESET) -> DataClearPreset.CORE_OPERATIONS_RESET
        else -> DataClearPreset.CUSTOM
    }

    /**
     * Returns `(en, bn)` smart dependency notices explaining how partial module
     * combinations reconcile stock layers, journals, and party balances safely.
     */
    fun buildWarnings(
        selected: Set<DataClearModule>,
        preview: DataClearImpactPreview?
    ): List<Pair<String, String>> {
        if (selected.isEmpty()) return emptyList()
        val warnings = mutableListOf<Pair<String, String>>()

        if (DataClearModule.PO_RESET_KEEP_STRUCTURE in selected &&
            DataClearModule.INVENTORY_STOCK_RESET !in selected &&
            (preview?.poReceiptLayerCount ?: 0) > 0
        ) {
            warnings += (
                "Unreceiving POs will also remove ${preview!!.poReceiptLayerCount} PO-received FIFO stock batch(es) from warehouse stock while keeping manual opening/adjustment stock." to
                    "PO আনরিসিভ করলে উক্ত PO থেকে গুদামে যুক্ত ${preview!!.poReceiptLayerCount}টি স্টক ব্যাচও স্বয়ংক্রিয়ভাবে বাদ যাবে (ম্যানুয়াল স্টক অক্ষত থাকবে)।"
                )
        }

        if (DataClearModule.SALES_FULL_CLEAR in selected &&
            DataClearModule.INVENTORY_STOCK_RESET !in selected
        ) {
            warnings += (
                "Clearing sales invoices will NOT automatically re-add sold stock back to inventory unless Inventory Stock Reset is also checked." to
                    "সেলস ইনভয়েস মুছলে পূর্বে বিক্রি হওয়া পণ্যের পরিমাণ স্বয়ংক্রিয়ভাবে স্টকে যোগ হবে না (সব স্টক ০ করতে চাইলে ইনভেন্টরি স্টক রিসেটও সিলেক্ট করুন)।"
                )
        }

        if (DataClearModule.LEDGER_AMOUNTS_RESET in selected &&
            (DataClearModule.SALES_FULL_CLEAR !in selected || DataClearModule.PO_RESET_KEEP_STRUCTURE !in selected)
        ) {
            warnings += (
                "All journals and opening balances will reset to ৳0.00, while remaining unselected unpaid invoices/confirmed POs will keep their live party balance synced." to
                    "সব জার্নাল ও ওপেনিং ব্যালেন্স ৳০.০০ হবে; তবে আনসিলেক্টেড সক্রিয় বকেয়া ইনভয়েস/PO থাকলে সেগুলোর বকেয়া গ্রাহক/সাপ্লায়ার ব্যালেন্সে সমন্বয় হয়ে থাকবে।"
                )
        } else if (DataClearModule.LEDGER_AMOUNTS_RESET in selected &&
            DataClearModule.PO_RESET_KEEP_STRUCTURE in selected &&
            (preview?.preservedPoCount ?: 0) > 0
        ) {
            warnings += (
                "Cash, bank, and expense ledgers will reset to ৳0.00, while preserved CONFIRMED PO bills (${preview!!.preservedPoCount}) remain in Supplier Payable so they can be paid or received directly." to
                    "ক্যাশ, ব্যাংক ও খরচের লেজার ৳০.০০ হবে; তবে অক্ষত থাকা CONFIRMED PO (${preview!!.preservedPoCount}টি)-এর বিল সাপ্লায়ার লেজারে বকেয়া দেখাবে যাতে সরাসরি পেমেন্ট/রিসিভ করা যায়।"
                )
        }

        return warnings
    }

    fun validateConfirmationInput(
        isAppLockEnabled: Boolean,
        storedPinHash: String?,
        storedPinSalt: String?,
        rawInput: String,
        pinVerifier: (enteredPin: String, storedHash: String?, storedSalt: String?) -> Boolean
    ): Boolean {
        val trimmed = rawInput.trim()
        if (isAppLockEnabled && !storedPinHash.isNullOrBlank()) {
            return trimmed.isNotEmpty() && pinVerifier(trimmed, storedPinHash, storedPinSalt)
        }
        return trimmed.equals("CLEAR", ignoreCase = true)
    }
}
