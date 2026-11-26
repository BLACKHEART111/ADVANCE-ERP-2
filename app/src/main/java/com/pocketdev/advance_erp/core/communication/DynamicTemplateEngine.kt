package com.pocketdev.advance_erp.core.communication

import com.pocketdev.advance_erp.data.entity.CommunicationSettings

object DynamicTemplateEngine {
    val labels = linkedMapOf(
        "customer_name" to ("Customer" to "গ্রাহক"), "customer_phone" to ("Phone" to "ফোন"),
        "supplier_name" to ("Supplier" to "সরবরাহকারী"), "invoice_number" to ("Invoice no." to "ইনভয়েস নং"),
        "invoice_date" to ("Invoice date" to "ইনভয়েস তারিখ"), "previous_due" to ("Previous balance at posting" to "পোস্টিংয়ের পূর্ববর্তী ব্যালেন্স"),
        "invoice_amount" to ("Original invoice amount" to "মূল ইনভয়েসের পরিমাণ"),
        "received_payment" to ("Allocated payment as of now" to "বর্তমানে বরাদ্দকৃত পেমেন্ট"),
        "invoice_due" to ("Invoice due as of now" to "বর্তমান ইনভয়েস বকেয়া"),
        "total_outstanding" to ("Current balance (due / credit)" to "বর্তমান ব্যালেন্স (বকেয়া / ক্রেডিট)"),
        "payment_number" to ("Payment no." to "পেমেন্ট নং"), "payment_method" to ("Payment method" to "পেমেন্ট মাধ্যম"),
        "payment_reference" to ("Reference" to "রেফারেন্স"), "payment_date" to ("Payment date" to "পেমেন্ট তারিখ"),
        "payment_amount" to ("Payment amount" to "পেমেন্টের পরিমাণ"), "due_date" to ("Due date" to "পরিশোধের তারিখ"),
        "document_number" to ("Document no." to "ডকুমেন্ট নং"), "document_date" to ("Document date" to "ডকুমেন্টের তারিখ"),
        "document_amount" to ("Document amount" to "ডকুমেন্টের পরিমাণ"), "total_quantity" to ("Total quantity" to "মোট পরিমাণ"),
        "total_carton" to ("Cartons" to "কার্টন"), "gross_weight" to ("Gross weight (kg)" to "মোট ওজন (কেজি)"),
        "total_cbm" to ("Total CBM" to "মোট সিবিএম"), "company_name" to ("Company" to "প্রতিষ্ঠান"),
        "company_phone" to ("Company phone" to "প্রতিষ্ঠানের ফোন"), "company_address" to ("Address" to "ঠিকানা"),
        "bank_details" to ("Bank details" to "ব্যাংকের তথ্য"), "date_range" to ("Date range" to "সময়কাল"),
        "opening_balance" to ("Opening balance" to "প্রারম্ভিক ব্যালেন্স"), "closing_balance" to ("Closing balance" to "সমাপনী ব্যালেন্স"),
        "return_amount" to ("Returned value" to "ফেরতের মূল্য"), "refund_amount" to ("Cash / bank refund" to "নগদ / ব্যাংক ফেরত"),
        "price_difference" to ("Price difference (+ collection / − refund)" to "মূল্যের পার্থক্য (+ গ্রহণ / − ফেরত)"),
        "valid_until" to ("Valid until" to "মেয়াদ"), "delivery_information" to ("Delivery information" to "ডেলিভারির তথ্য"),
        "items" to ("Products / quantities" to "পণ্য / পরিমাণ"), "as_of" to ("Current figures as of" to "বর্তমান হিসাবের সময়"),
        "historical_as_of" to ("Historical snapshot at" to "ঐতিহাসিক হিসাবের সময়"),
        "historical_payment" to ("Payment allocated at posting" to "পোস্টিংয়ের সময় বরাদ্দ পেমেন্ট"),
        "historical_due" to ("Invoice due at posting" to "পোস্টিংয়ের সময় ইনভয়েস বকেয়া"),
        "historical_balance" to ("Customer balance at posting" to "পোস্টিংয়ের সময় গ্রাহকের ব্যালেন্স"),
        "payment_instructions" to ("Payment instructions" to "পেমেন্ট নির্দেশনা"),
        "status_text" to ("Status" to "অবস্থা"), "reminder_text" to ("Reminder" to "স্মরণিকা")
    )
    private val placeholder = Regex("\\{([^{}]+)}")
    fun unknown(template: String) = placeholder.findAll(template).map { it.groupValues[1] }.filter { it !in labels }.toSet()
    fun validate(template: String) {
        require(template.length <= 16000) { "Template exceeds 16000 characters" }
        require(unknown(template).isEmpty()) { "Unknown placeholders: ${unknown(template).joinToString()}" }
        require(!template.replace(placeholder, "").contains(Regex("[{}]"))) { "Malformed placeholder" }
    }
    /** A line containing an unavailable variable is omitted as a whole, including its label. */
    fun render(template: String, fields: Map<String, String>): String = template.lines().mapNotNull { line ->
        val keys = placeholder.findAll(line).map { it.groupValues[1] }.toList()
        if (keys.any { it !in labels || fields[it].isNullOrBlank() }) null
        else placeholder.replace(line) { fields[it.groupValues[1]].orEmpty().replace('{', '(').replace('}', ')') }
    }.joinToString("\n").replace(Regex("[{}]"), "").replace(Regex("\n{3,}"), "\n\n").trim()

    fun allowed(module: CommunicationModule): Set<String> {
        val common = setOf("customer_name", "customer_phone", "supplier_name", "document_number", "document_date", "company_name", "company_phone", "company_address", "bank_details", "payment_instructions", "as_of", "status_text", "reminder_text", "total_outstanding")
        return common + when (module) {
            CommunicationModule.INVOICE -> setOf("invoice_number", "invoice_date", "previous_due", "invoice_amount", "received_payment", "invoice_due", "due_date", "historical_as_of", "historical_payment", "historical_due", "historical_balance", "items", "total_quantity")
            CommunicationModule.PAYMENT, CommunicationModule.SUPPLIER_PAYMENT -> setOf("payment_number", "payment_date", "payment_amount", "payment_method", "payment_reference")
            CommunicationModule.LEDGER, CommunicationModule.STATEMENT -> setOf("date_range", "opening_balance", "closing_balance")
            CommunicationModule.REMINDER -> setOf("due_date")
            CommunicationModule.PACKING -> setOf("items", "total_quantity", "total_carton", "gross_weight", "total_cbm")
            CommunicationModule.RETURN -> setOf("items", "return_amount", "refund_amount", "payment_method")
            CommunicationModule.EXCHANGE -> setOf("items", "price_difference", "payment_method")
            else -> setOf("items", "document_amount", "valid_until", "delivery_information", "total_quantity")
        }
    }
    fun defaultTemplate(module: CommunicationModule, status: CommunicationStatus, language: String): String {
        val bn = language == "bn"
        val supplier = module in setOf(CommunicationModule.PURCHASE_ORDER, CommunicationModule.SUPPLIER_PAYMENT, CommunicationModule.PACKING)
        val name = if (supplier) "supplier_name" else "customer_name"
        val introduction = when (module) {
            CommunicationModule.INVOICE -> if (bn) "আপনার ক্রয়ের ইনভয়েসের বিবরণ দেওয়া হলো।" else "Please find the details of your purchase invoice."
            CommunicationModule.PAYMENT -> if (bn) "আপনার পেমেন্ট গ্রহণ করা হয়েছে। ধন্যবাদ।" else "Thank you. Your payment has been received."
            CommunicationModule.REMINDER -> if (bn) "আপনার বকেয়া হিসাবটি অনুগ্রহ করে যাচাই করুন।" else "Please review your outstanding account balance."
            CommunicationModule.DELIVERY -> if (bn) "পণ্য প্রেরণের বিবরণ দেওয়া হলো।" else "Please review the dispatched products and quantities."
            CommunicationModule.RETURN -> if (bn) "আপনার পণ্য ফেরতের বিবরণ দেওয়া হলো।" else "Please review your product return and refund details."
            CommunicationModule.EXCHANGE -> if (bn) "ফেরত ও প্রতিস্থাপিত পণ্যের বিবরণ দেওয়া হলো।" else "Please review the returned and replacement products."
            CommunicationModule.QUOTATION -> if (bn) "আপনার জন্য প্রস্তুতকৃত কোটেশন দেওয়া হলো।" else "Please review the quotation prepared for you."
            CommunicationModule.SALES_ORDER -> if (bn) "আপনার বিক্রয় আদেশের বিবরণ দেওয়া হলো।" else "Please review your sales order and delivery information."
            CommunicationModule.PURCHASE_ORDER -> if (bn) "আমাদের ক্রয় আদেশের বিবরণ দেওয়া হলো।" else "Please review our purchase order."
            CommunicationModule.SUPPLIER_PAYMENT -> if (bn) "আমাদের পরিশোধিত পেমেন্টের বিবরণ দেওয়া হলো।" else "Please review the payment made to your account."
            CommunicationModule.PACKING -> if (bn) "প্যাকিং ও পণ্যের পরিমাণ যাচাই করুন।" else "Please verify the packing details and quantities."
            else -> if (bn) "নির্বাচিত সময়ের হিসাব বিবরণী যাচাই করুন।" else "Please review your account statement for the selected period."
        }
        val fields = allowed(module) - setOf("customer_name", "supplier_name", "customer_phone", "company_name", "company_phone", "company_address", "status_text", "reminder_text")
        val lines = labels.filterKeys { it in fields }.map { (key, label) -> "${if (bn) label.second else label.first}: {$key}" }
        return listOf(if (bn) "আসসালামু আলাইকুম,\n\nসম্মানিত {$name}," else "Dear {$name},", introduction,
            module.title(language),
            when(status) {
                CommunicationStatus.PREVIOUS_DUE -> if(bn) "পূর্ববর্তী হিসাব ও বর্তমান ব্যালেন্স পৃথকভাবে দেওয়া হলো।" else "Previous account figures and current balance are listed separately."
                CommunicationStatus.CREDIT -> if(bn) "আপনার হিসাবের ক্রেডিট বিবরণ নিচে দেওয়া হলো।" else "Your account credit details are shown below."
                else -> ""
            }, "━━━━━━━━━━━━━━━━", lines.joinToString("\n"), "{status_text}", "{reminder_text}",
            if (bn) "অনুগ্রহ করে আপনার হিসাবটি যাচাই করে নেবেন।\nআমাদের সঙ্গে ব্যবসা করার জন্য আন্তরিক ধন্যবাদ।\n\nশুভেচ্ছান্তে," else "Please let us know if any details need clarification.\nThank you for doing business with us.\n\nKind regards,",
            "{company_name}", "📞 {company_phone}", "{company_address}").joinToString("\n\n")
    }
    fun fields(document: SafeDocument, settings: CommunicationSettings, language: String): Map<String, String> {
        val bn = language == "bn"
        val result = document.fields.filterKeys { it in allowed(document.request.module) }.toMutableMap()
        fun remove(enabled: Boolean, vararg keys: String) { if (!enabled) keys.forEach { result.remove(it) } }
        remove(settings.includePreviousDue, "previous_due")
        remove(settings.includeInvoiceAmount, "invoice_amount")
        remove(settings.includePaymentDetails, "received_payment", "historical_payment", "payment_amount")
        remove(settings.includeInvoiceDue, "invoice_due", "historical_due")
        remove(settings.includeOutstanding, "total_outstanding", "historical_balance")
        remove(settings.includeMethodReference, "payment_method", "payment_reference")
        remove(settings.includeSignature, "company_name", "company_phone", "company_address")
        if (settings.includeInstructions) { result["payment_instructions"] = settings.paymentInstructions; result["bank_details"] = settings.bankDetails }
        result["status_text"] = when (document.status) {
            CommunicationStatus.PAID -> if (bn) "এই ইনভয়েস সম্পূর্ণ পরিশোধিত। গ্রাহকের মোট ব্যালেন্স আলাদাভাবে দেখানো হয়েছে।" else "This invoice is fully paid. Your overall account balance is shown separately."
            CommunicationStatus.PARTIAL -> if (bn) "এই ইনভয়েস আংশিক পরিশোধিত।" else "This invoice is partially paid."
            CommunicationStatus.UNPAID -> if (bn) "এই ইনভয়েসে কোনো পেমেন্ট বরাদ্দ হয়নি।" else "No payment has been allocated to this invoice."
            CommunicationStatus.SETTLED -> if (bn) "ফেরত / সমন্বয়ের পরে এই ইনভয়েসে বকেয়া নেই।" else "No invoice balance remains after returns / adjustments."
            else -> ""
        }
        if (document.balanceMinor != null && document.balanceMinor < 0 && settings.includeOutstanding)
            result["status_text"] = result["status_text"].orEmpty() + if (bn) "\nআপনার হিসাবে অগ্রিম / ক্রেডিট রয়েছে।" else "\nYour account has an advance / credit balance."
        if (settings.includeReminder && (document.balanceMinor ?: 0) > 0 || document.request.module == CommunicationModule.REMINDER && (document.balanceMinor ?: 0) > 0)
            result["reminder_text"] = if (bn) "সুবিধামতো বকেয়া পরিশোধের অনুরোধ রইল। ইতিমধ্যে পরিশোধ করলে আমাদের জানান।" else "Kindly arrange payment at your convenience. Please let us know if payment has already been made."
        return result
    }
}
