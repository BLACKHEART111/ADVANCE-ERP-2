package com.pocketdev.advance_erp.core.communication

import com.pocketdev.advance_erp.core.communication.graphics.BrandedDocumentFormatter
import com.pocketdev.advance_erp.core.communication.pdf.BrandedPdfSpecBuilder
import com.pocketdev.advance_erp.data.entity.CommunicationSettings
import kotlin.math.abs

object DynamicTemplateEngine {
    val labels = linkedMapOf(
        "customer_name" to ("Customer" to "গ্রাহক"),
        "customer_phone" to ("Phone" to "ফোন"),
        "supplier_name" to ("Supplier" to "সরবরাহকারী"),
        "invoice_number" to ("Invoice no." to "ইনভয়েস নং"),
        "invoice_date" to ("Invoice date" to "ইনভয়েস তারিখ"),
        "previous_due" to ("Previous balance" to "পূর্বের বকেয়া"),
        "invoice_amount" to ("Total bill" to "মোট বিল"),
        "return_amount" to ("Return credit" to "ফেরত সমন্বয়"),
        "received_payment" to ("Paid" to "পরিশোধ"),
        "invoice_due" to ("Invoice due" to "ইনভয়েস বকেয়া"),
        "total_outstanding" to ("Total balance" to "সর্বমোট বকেয়া"),
        "payment_number" to ("Payment no." to "পেমেন্ট নং"),
        "payment_method" to ("Payment method" to "পেমেন্ট মাধ্যম"),
        "payment_reference" to ("Reference" to "রেফারেন্স"),
        "payment_date" to ("Payment date" to "পেমেন্ট তারিখ"),
        "payment_amount" to ("Payment amount" to "পেমেন্টের পরিমাণ"),
        "due_date" to ("Due date" to "পরিশোধের তারিখ"),
        "document_number" to ("Document no." to "ডকুমেন্ট নং"),
        "document_date" to ("Date" to "তারিখ"),
        "document_amount" to ("Total amount" to "মোট পরিমাণ"),
        "total_quantity" to ("Total quantity" to "মোট পরিমাণ"),
        "total_carton" to ("Cartons" to "কার্টন"),
        "gross_weight" to ("Gross weight (kg)" to "মোট ওজন (কেজি)"),
        "total_cbm" to ("Total CBM" to "মোট সিবিএম"),
        "company_name" to ("Company" to "প্রতিষ্ঠান"),
        "company_phone" to ("Company phone" to "প্রতিষ্ঠানের ফোন"),
        "company_address" to ("Address" to "ঠিকানা"),
        "bank_details" to ("Bank details" to "ব্যাংকের তথ্য"),
        "date_range" to ("Period" to "সময়কাল"),
        "opening_balance" to ("Opening balance" to "প্রারম্ভিক ব্যালেন্স"),
        "closing_balance" to ("Closing balance" to "বর্তমান ব্যালেন্স"),
        "refund_amount" to ("Cash / bank refund" to "নগদ / ব্যাংক ফেরত"),
        "price_difference" to ("Price difference" to "মূল্যের পার্থক্য"),
        "valid_until" to ("Valid until" to "মেয়াদ শেষ"),
        "delivery_information" to ("Delivery information" to "ডেলিভারির তথ্য"),
        "items" to ("Items" to "পণ্য তালিকা"),
        "as_of" to ("Current figures as of" to "বর্তমান হিসাবের সময়"),
        "historical_as_of" to ("Historical snapshot at" to "ঐতিহাসিক হিসাবের সময়"),
        "historical_payment" to ("Payment allocated at posting" to "পোস্টিংয়ের সময় বরাদ্দ পেমেন্ট"),
        "historical_due" to ("Invoice due at posting" to "পোস্টিংয়ের সময় ইনভয়েস বকেয়া"),
        "historical_balance" to ("Customer balance at posting" to "পোস্টিংয়ের সময় গ্রাহকের ব্যালেন্স"),
        "payment_instructions" to ("Payment instructions" to "পেমেন্ট নির্দেশনা"),
        "status_text" to ("Status" to "অবস্থা"),
        "reminder_text" to ("Reminder" to "স্মরণিকা"),
        "customer_code" to ("Customer code" to "গ্রাহক কোড"),
        "customer_address" to ("Address" to "ঠিকানা"),
        "sales_type" to ("Sales type" to "বিক্রয়ের ধরন"),
        "subtotal_amount" to ("Subtotal" to "উপমোট"),
        "discount_amount" to ("Discount" to "ডিসকাউন্ট"),
        "additional_charges" to ("Additional charges" to "অতিরিক্ত চার্জ"),
        "payment_terms" to ("Payment terms" to "পেমেন্ট শর্তাবলি"),
        "company_website" to ("Website" to "ওয়েবসাইট"),
        "company_bin" to ("BIN" to "বিআইএন (BIN)"),
        "company_tagline" to ("Tagline" to "ট্যাগলাইন"),
        "previous_balance" to ("Previous balance" to "পূর্বের ব্যালেন্স"),
        "balance_after_payment" to ("Balance after payment" to "পেমেন্ট পরবর্তী ব্যালেন্স"),
        "total_sales" to ("Total sales" to "মোট ক্রয়"),
        "total_returns" to ("Total return" to "মোট ফেরত"),
        "total_payments" to ("Total payment" to "মোট পরিশোধ"),
        "reshare_notice" to ("Balance update note" to "হালনাগাদ ব্যালেন্স নোট"),
        "latest_invoice_summary" to ("Latest invoice" to "সর্বশেষ ইনভয়েস"),
        "latest_payment_summary" to ("Latest payment" to "সর্বশেষ পরিশোধ")
    )

    private val placeholder = Regex("\\{([^{}]+)\\}")
    fun unknown(template: String) = placeholder.findAll(template).map { it.groupValues[1] }.filter { it !in labels }.toSet()

    fun validate(template: String) {
        require(template.length <= 16000) { "Template exceeds 16000 characters" }
        val bad = unknown(template)
        require(bad.isEmpty()) { "Unknown placeholders: ${bad.joinToString()}" }
        require(!template.replace(placeholder, "").contains(Regex("[\\{\\}]"))) { "Malformed placeholder braces" }
    }

    /** A line containing an unavailable variable is omitted as a whole, including its label. */
    fun render(template: String, fields: Map<String, String>): String = template.lines().mapNotNull { line ->
        val keys = placeholder.findAll(line).map { it.groupValues[1] }.toList()
        if (keys.any { it !in labels || fields[it].isNullOrBlank() }) null
        else placeholder.replace(line) { fields[it.groupValues[1]].orEmpty().replace('{', '(').replace('}', ')') }
    }.joinToString("\n").replace(Regex("[\\{\\}]"), "").replace(Regex("\n{3,}"), "\n\n").trim()

    fun allowed(module: CommunicationModule): Set<String> {
        val common = setOf(
            "customer_name", "customer_phone", "customer_code", "customer_address", "supplier_name",
            "document_number", "document_date",
            "company_name", "company_phone", "company_address",
            "company_website", "company_bin", "company_tagline",
            "bank_details", "payment_instructions",
            "as_of", "status_text", "reminder_text", "total_outstanding"
        )
        return common + when (module) {
            CommunicationModule.INVOICE -> setOf(
                "invoice_number", "invoice_date", "previous_due",
                "sales_type", "subtotal_amount", "discount_amount", "additional_charges", "payment_terms",
                "invoice_amount", "return_amount", "received_payment", "invoice_due", "due_date",
                "historical_as_of", "historical_payment", "historical_due", "historical_balance",
                "items", "total_quantity", "reshare_notice"
            )
            CommunicationModule.PAYMENT, CommunicationModule.SUPPLIER_PAYMENT -> setOf(
                "payment_number", "payment_date", "payment_amount", "payment_method", "payment_reference",
                "previous_balance", "balance_after_payment"
            )
            CommunicationModule.LEDGER, CommunicationModule.STATEMENT, CommunicationModule.SUPPLIER_STATEMENT -> setOf(
                "date_range", "opening_balance", "closing_balance",
                "total_sales", "total_returns", "total_payments"
            )
            CommunicationModule.REMINDER -> setOf(
                "due_date", "previous_balance", "latest_invoice_summary", "latest_payment_summary"
            )
            CommunicationModule.PACKING -> setOf("items", "total_quantity", "total_carton", "gross_weight", "total_cbm")
            CommunicationModule.RETURN -> setOf("items", "return_amount", "refund_amount", "payment_method")
            CommunicationModule.EXCHANGE -> setOf("items", "price_difference", "payment_method")
            CommunicationModule.QUOTATION, CommunicationModule.SALES_ORDER -> setOf(
                "items", "document_amount", "valid_until", "delivery_information", "total_quantity",
                "sales_type", "subtotal_amount", "discount_amount", "additional_charges", "payment_terms"
            )
            else -> setOf("items", "document_amount", "valid_until", "delivery_information", "total_quantity")
        }
    }

    fun defaultTemplate(module: CommunicationModule, status: CommunicationStatus, language: String): String {
        val bn = language == "bn"
        val isSupplier = module.isSupplierModule()
        val nameKey = if (isSupplier) "supplier_name" else "customer_name"

        val greetingHeader = if (bn) {
            "আসসালামু আলাইকুম,\n\nসম্মানিত {$nameKey},"
        } else {
            "Dear {$nameKey},"
        }
        val signOff = if (bn) {
            "ধন্যবাদ,\n{company_name}\n📞 {company_phone}"
        } else {
            "Thank you,\n{company_name}\n📞 {company_phone}"
        }

        return when (module) {
            CommunicationModule.INVOICE -> buildInvoiceTemplate(greetingHeader, signOff, status, bn)
            CommunicationModule.PAYMENT -> buildPaymentTemplate(greetingHeader, signOff, status, bn, isSupplier = false)
            CommunicationModule.SUPPLIER_PAYMENT -> buildPaymentTemplate(greetingHeader, signOff, status, bn, isSupplier = true)
            CommunicationModule.REMINDER -> buildReminderTemplate(greetingHeader, signOff, status, bn)
            CommunicationModule.LEDGER, CommunicationModule.STATEMENT -> buildStatementTemplate(greetingHeader, signOff, bn, isSupplier = false)
            CommunicationModule.SUPPLIER_STATEMENT -> buildStatementTemplate(greetingHeader, signOff, bn, isSupplier = true)
            CommunicationModule.DELIVERY -> listOf(
                greetingHeader,
                if (bn) "আপনার ডেলিভারি চালান ও প্রেরিত পণ্যের বিবরণ নিচে দেওয়া হলো।" else "Please find your delivery challan and dispatched item details below.",
                if (bn) "🚚 চালান নং: {document_number}\n📅 তারিখ: {document_date}\n📦 মোট পরিমাণ: {total_quantity}\n🛣️ ডেলিভারি: {delivery_information}"
                else "🚚 Challan No: {document_number}\n📅 Date: {document_date}\n📦 Total Qty: {total_quantity}\n🛣️ Delivery: {delivery_information}",
                "{items}",
                if (bn) "পণ্য বুঝে পাওয়ার পর অনুগ্রহ করে যাচাই করে নেবেন।" else "Please inspect the dispatched items upon delivery.",
                signOff
            ).joinToString("\n\n")
            CommunicationModule.EXCHANGE -> listOf(
                greetingHeader,
                if (bn) "আপনার পণ্য বিনিময়ের (এক্সচেঞ্জ) হিসাব নিচে দেওয়া হলো।" else "Here is the summary of your product exchange:",
                if (bn) "🔄 এক্সচেঞ্জ নং: {document_number}\n📅 তারিখ: {document_date}\n💰 মূল্যের পার্থক্য: {price_difference}\n💳 মাধ্যম: {payment_method}\n📊 বর্তমান ব্যালেন্স: {total_outstanding}"
                else "🔄 Exchange No: {document_number}\n📅 Date: {document_date}\n💰 Price Difference: {price_difference}\n💳 Method: {payment_method}\n📊 Current Balance: {total_outstanding}",
                "{items}",
                signOff
            ).joinToString("\n\n")
            CommunicationModule.RETURN -> listOf(
                greetingHeader,
                if (bn) "আপনার বিক্রয় ফেরত (রিটার্ন) ও সমন্বয়ের হিসাব নিচে দেওয়া হলো।" else "Please find your sales return and adjustment summary below:",
                if (bn) "↩️ রিটার্ন নং: {document_number}\n📅 তারিখ: {document_date}\n💰 ফেরত মূল্য: {return_amount}\n💵 নগদ/ব্যাংক রিফান্ড: {refund_amount}\n💳 মাধ্যম: {payment_method}\n📊 বর্তমান ব্যালেন্স: {total_outstanding}"
                else "↩️ Return No: {document_number}\n📅 Date: {document_date}\n💰 Return Value: {return_amount}\n💵 Cash/Bank Refund: {refund_amount}\n💳 Mode: {payment_method}\n📊 Current Balance: {total_outstanding}",
                signOff
            ).joinToString("\n\n")
            CommunicationModule.QUOTATION -> listOf(
                greetingHeader,
                if (bn) "আপনার অনুরোধকৃত পণ্যের কোটেশন নিচে দেওয়া হলো।" else "Please find the quotation prepared for you below.",
                if (bn) "🧾 কোটেশন নং: {document_number}\n📅 তারিখ: {document_date}\n💰 প্রাক্কলিত মূল্য: {document_amount}\n⏰ মেয়াদ শেষ: {valid_until}\n📝 শর্তাবলি: {payment_terms}"
                else "🧾 Quotation No: {document_number}\n📅 Date: {document_date}\n💰 Quoted Total: {document_amount}\n⏰ Valid Until: {valid_until}\n📝 Terms: {payment_terms}",
                if (bn) "বিস্তারিত জানতে সংযুক্ত PDF কোটেশন দেখুন।" else "See the attached PDF quotation for complete item details.",
                signOff
            ).joinToString("\n\n")
            CommunicationModule.SALES_ORDER -> listOf(
                greetingHeader,
                if (bn) "আপনার বিক্রয় আদেশের বিবরণ নিচে দেওয়া হলো।" else "Please find your sales order summary below:",
                if (bn) "🧾 অর্ডার নং: {document_number}\n📅 তারিখ: {document_date}\n💰 মোট মূল্য: {document_amount}\n🚚 ডেলিভারি তথ্য: {delivery_information}\n📝 শর্তাবলি: {payment_terms}"
                else "🧾 Order No: {document_number}\n📅 Date: {document_date}\n💰 Order Total: {document_amount}\n🚚 Delivery Info: {delivery_information}\n📝 Terms: {payment_terms}",
                if (bn) "বিস্তারিত জানতে সংযুক্ত PDF দেখুন।" else "See the attached PDF for full order details.",
                signOff
            ).joinToString("\n\n")
            CommunicationModule.PURCHASE_ORDER -> listOf(
                greetingHeader,
                if (bn) "আমাদের ক্রয় আদেশের সারসংক্ষেপ নিচে দেওয়া হলো।" else "Please review our purchase order details below:",
                if (bn) "🧾 পিও নং: {document_number}\n📅 তারিখ: {document_date}\n📦 মোট পরিমাণ: {total_quantity}\n💰 মোট অর্ডার মূল্য: {document_amount}"
                else "🧾 PO No: {document_number}\n📅 Date: {document_date}\n📦 Total Qty: {total_quantity}\n💰 Order Total: {document_amount}",
                if (bn) "বিস্তারিত জানতে সংযুক্ত PDF দেখুন।" else "Please review the attached PDF for item specifications.",
                signOff
            ).joinToString("\n\n")
            CommunicationModule.PACKING -> listOf(
                greetingHeader,
                if (bn) "শিপমেন্ট ও প্যাকিংয়ের বিবরণ নিচে দেওয়া হলো।" else "Please verify the shipment packing summary below:",
                if (bn) "📦 রেফারেন্স নং: {document_number}\n📅 তারিখ: {document_date}\n📦 মোট পরিমাণ: {total_quantity}\n📥 মোট কার্টন: {total_carton}\n⚖️ ওজন (কেজি): {gross_weight}\n📐 মোট সিবিএম: {total_cbm}"
                else "📦 Reference No: {document_number}\n📅 Date: {document_date}\n📦 Total Qty: {total_quantity}\n📥 Cartons: {total_carton}\n⚖️ Gross Weight: {gross_weight}\n📐 Total CBM: {total_cbm}",
                "{items}",
                signOff
            ).joinToString("\n\n")
        }
    }

    private fun buildInvoiceTemplate(
        greetingHeader: String,
        signOff: String,
        status: CommunicationStatus,
        bn: Boolean
    ): String {
        val intro = when (status) {
            CommunicationStatus.PAID -> if (bn) "আপনার ক্রয়ের ইনভয়েসটি সম্পূর্ণ পরিশোধিত হয়েছে। বিবরণ নিচে দেওয়া হলো।" else "Your invoice has been paid in full. Summary below:"
            CommunicationStatus.PARTIAL -> if (bn) "আপনার ইনভয়েসে আংশিক পেমেন্ট গৃহীত হয়েছে। হালনাগাদ হিসাব নিচে দেওয়া হলো।" else "A partial payment has been received for your invoice. Summary below:"
            CommunicationStatus.OVERDUE -> if (bn) "আপনার নিচের ইনভয়েসটির পরিশোধের নির্ধারিত তারিখ অতিক্রান্ত হয়েছে।" else "Your invoice below is past its due date. Please review the summary:"
            CommunicationStatus.SETTLED -> if (bn) "ফেরত/সমন্বয়ের পর আপনার ইনভয়েসটি নিষ্পত্তি হয়েছে।" else "Your invoice has been settled after return/adjustment."
            CommunicationStatus.CREDIT -> if (bn) "আপনার ক্রয়ের ইনভয়েস ও অগ্রিম ক্রেডিট ব্যালেন্স নিচে দেওয়া হলো।" else "Please find your invoice and advance credit balance below:"
            CommunicationStatus.PREVIOUS_DUE -> if (bn) "আপনার ক্রয়ের ইনভয়েসের সাথে পূর্বের হিসাব ও বর্তমান ব্যালেন্স পৃথকভাবে দেওয়া হলো।" else "Please find your invoice summary with previous and current balances below:"
            CommunicationStatus.UNPAID, CommunicationStatus.GENERAL -> if (bn) "আপনার ক্রয়ের ইনভয়েসের বিবরণ নিচে দেওয়া হলো।" else "Please find your sales invoice summary below:"
        }

        val dueBullet = if (status == CommunicationStatus.PAID || status == CommunicationStatus.SETTLED) {
            if (bn) "🟢 ইনভয়েস বকেয়া: {invoice_due}" else "🟢 Invoice Due: {invoice_due}"
        } else {
            if (bn) "🔴 ইনভয়েস বকেয়া: {invoice_due}" else "🔴 Invoice Due: {invoice_due}"
        }

        val balanceBullet = if (status == CommunicationStatus.CREDIT) {
            if (bn) "💳 বর্তমান ব্যালেন্স: {total_outstanding}" else "💳 Account Balance: {total_outstanding}"
        } else {
            if (bn) "📊 সর্বমোট বকেয়া: {total_outstanding}" else "📊 Total Balance: {total_outstanding}"
        }

        val details = if (bn) {
            listOf(
                "🧾 ইনভয়েস নং: {invoice_number}",
                "📅 তারিখ: {invoice_date}",
                "💰 মোট বিল: {invoice_amount}",
                "🎁 ডিসকাউন্ট: {discount_amount}",
                "↩️ ফেরত সমন্বয়: {return_amount}",
                "✅ পরিশোধ: {received_payment}",
                dueBullet,
                "⏰ পরিশোধের তারিখ: {due_date}",
                "🏦 পূর্বের বকেয়া: {previous_due}",
                balanceBullet
            ).joinToString("\n")
        } else {
            listOf(
                "🧾 Invoice No: {invoice_number}",
                "📅 Date: {invoice_date}",
                "💰 Total Bill: {invoice_amount}",
                "🎁 Discount: {discount_amount}",
                "↩️ Return Credit: {return_amount}",
                "✅ Paid: {received_payment}",
                dueBullet,
                "⏰ Due Date: {due_date}",
                "🏦 Previous Balance: {previous_due}",
                balanceBullet
            ).joinToString("\n")
        }

        return listOf(
            greetingHeader,
            intro,
            details,
            "{reshare_notice}",
            "{payment_instructions}",
            "{bank_details}",
            "{reminder_text}",
            if (bn) "বিস্তারিত জানতে সংযুক্ত PDF ইনভয়েস দেখুন।" else "See attached PDF invoice for full details.",
            signOff
        ).filter { it.isNotBlank() }.joinToString("\n\n")
    }

    private fun buildPaymentTemplate(
        greetingHeader: String,
        signOff: String,
        status: CommunicationStatus,
        bn: Boolean,
        isSupplier: Boolean
    ): String {
        val intro = when {
            isSupplier -> if (bn) "আপনার অ্যাকাউন্টে প্রদত্ত পেমেন্টের বিবরণ নিচে দেওয়া হলো।" else "Please find the payment recorded to your supplier account below:"
            status == CommunicationStatus.CREDIT -> if (bn) "আপনার পেমেন্ট সফলভাবে গ্রহণ করা হয়েছে এবং অতিরিক্ত অর্থ অগ্রিম ক্রেডিট হিসেবে জমা হয়েছে।" else "Your payment has been received and your advance credit has been updated."
            else -> if (bn) "আপনার পেমেন্ট সফলভাবে গ্রহণ করা হয়েছে।" else "Thank you! Your payment has been received successfully."
        }
        val details = if (bn) {
            listOf(
                "🧾 রসিদ নং: {payment_number}",
                "📅 তারিখ: {payment_date}",
                "✅ পরিশোধের পরিমাণ: {payment_amount}",
                "💳 মাধ্যম: {payment_method}",
                "🔖 রেফারেন্স: {payment_reference}",
                "🏦 পূর্বের ব্যালেন্স: {previous_balance}",
                "📊 বর্তমান ব্যালেন্স: {total_outstanding}"
            ).joinToString("\n")
        } else {
            listOf(
                "🧾 Receipt No: {payment_number}",
                "📅 Date: {payment_date}",
                "✅ Amount: {payment_amount}",
                "💳 Method: {payment_method}",
                "🔖 Ref: {payment_reference}",
                "🏦 Previous Balance: {previous_balance}",
                "📊 Updated Balance: {total_outstanding}"
            ).joinToString("\n")
        }
        return listOf(
            greetingHeader,
            intro,
            details,
            signOff
        ).joinToString("\n\n")
    }

    private fun buildReminderTemplate(
        greetingHeader: String,
        signOff: String,
        status: CommunicationStatus,
        bn: Boolean
    ): String {
        if (status == CommunicationStatus.CREDIT) {
            return listOf(
                greetingHeader,
                if (bn) "আপনার হিসাবে বর্তমানে অগ্রিম ক্রেডিট রয়েছে; কোনো বকেয়া নেই।" else "Your account currently holds an advance credit balance with zero overdue due.",
                if (bn) "💳 বর্তমান ব্যালেন্স: {total_outstanding}" else "💳 Current Balance: {total_outstanding}",
                signOff
            ).joinToString("\n\n")
        }
        if (status == CommunicationStatus.PAID || status == CommunicationStatus.SETTLED) {
            return listOf(
                greetingHeader,
                if (bn) "আপনার হিসাব বর্তমানে সম্পূর্ণ পরিশোধিত রয়েছে।" else "Your account is currently settled with zero balance due.",
                if (bn) "✅ বর্তমান ব্যালেন্স: {total_outstanding}" else "✅ Current Balance: {total_outstanding}",
                signOff
            ).joinToString("\n\n")
        }

        val intro = when (status) {
            CommunicationStatus.OVERDUE -> if (bn) "আপনার হিসাবে বকেয়া পরিশোধের নির্ধারিত সময় অতিক্রান্ত হয়েছে। অনুগ্রহ করে বকেয়াটি পরিশোধ করে হিসাব আপডেট করুন।" else "A gentle reminder that your outstanding balance is past due. Kindly arrange payment at your earliest convenience."
            else -> if (bn) "আপনার হিসাবে কিছু বকেয়া রয়েছে। অনুগ্রহ করে সুবিধামতো সময়ে হিসাবটি আপডেট করুন।" else "Please review your current account balance summary below."
        }
        val details = if (bn) {
            listOf(
                "🏦 পূর্বের ব্যালেন্স: {previous_balance}",
                "🧾 সর্বশেষ ক্রয়: {latest_invoice_summary}",
                "✅ সর্বশেষ পরিশোধ: {latest_payment_summary}",
                "⏰ নির্ধারিত তারিখ: {due_date}",
                "🔴 বর্তমান মোট বকেয়া: {total_outstanding}"
            ).joinToString("\n")
        } else {
            listOf(
                "🏦 Prior Balance: {previous_balance}",
                "🧾 Latest Invoice: {latest_invoice_summary}",
                "✅ Latest Payment: {latest_payment_summary}",
                "⏰ Due Date: {due_date}",
                "🔴 Total Due: {total_outstanding}"
            ).joinToString("\n")
        }

        return listOf(
            greetingHeader,
            intro,
            details,
            "{payment_instructions}",
            "{bank_details}",
            "{reminder_text}",
            signOff
        ).filter { it.isNotBlank() }.joinToString("\n\n")
    }

    private fun buildStatementTemplate(
        greetingHeader: String,
        signOff: String,
        bn: Boolean,
        isSupplier: Boolean
    ): String {
        val intro = if (bn) {
            "নির্বাচিত সময়ের আপনার হিসাবের সারসংক্ষেপ নিচে দেওয়া হলো।"
        } else {
            "Here is your account statement summary for the selected period:"
        }
        val salesLabel = if (isSupplier) {
            if (bn) "🛒 মোট ক্রয়" else "🛒 Total Purchases"
        } else {
            if (bn) "🛒 মোট ক্রয়" else "🛒 Total Sales"
        }
        val details = if (bn) {
            listOf(
                "📅 সময়কাল: {date_range}",
                "🏦 প্রারম্ভিক ব্যালেন্স: {opening_balance}",
                "$salesLabel: {total_sales}",
                "↩️ মোট ফেরত: {total_returns}",
                "✅ মোট পরিশোধ: {total_payments}",
                "📊 বর্তমান ব্যালেন্স: {closing_balance}"
            ).joinToString("\n")
        } else {
            listOf(
                "📅 Period: {date_range}",
                "🏦 Opening Balance: {opening_balance}",
                "$salesLabel: {total_sales}",
                "↩️ Total Return: {total_returns}",
                "✅ Total Paid: {total_payments}",
                "📊 Closing Balance: {closing_balance}"
            ).joinToString("\n")
        }
        return listOf(
            greetingHeader,
            intro,
            details,
            if (bn) "বিস্তারিত লেনদেনের জন্য সংযুক্ত স্টেটমেন্ট (PDF) দেখুন।" else "See the attached PDF statement for complete transaction history.",
            signOff
        ).joinToString("\n\n")
    }

    private fun isOverdueDocument(document: SafeDocument): Boolean = when (document.request.module) {
        CommunicationModule.INVOICE -> document.invoiceBreakdown?.let {
            it.invoiceDueMinor > 0L && it.dueDateMillis != null && it.dueDateMillis < document.asOfMillis
        } == true
        CommunicationModule.REMINDER -> document.reminderBreakdown?.let {
            it.currentTotalDueMinor > 0L && it.earliestDueDateMillis != null && it.earliestDueDateMillis < document.asOfMillis
        } == true
        else -> false
    }

    private fun cleanBalanceText(minor: Long, currency: String, bn: Boolean): String {
        val formatted = BrandedPdfSpecBuilder.formatMoney(abs(minor), currency)
        return when {
            minor < 0L -> if (bn) "$formatted (অগ্রিম ক্রেডিট)" else "$formatted (Advance Credit)"
            else -> formatted
        }
    }

    fun fields(document: SafeDocument, settings: CommunicationSettings, language: String): Map<String, String> {
        val bn = language == "bn"
        val result = document.fields.filterKeys { it in allowed(document.request.module) }.toMutableMap()
        val currency = document.brand?.currencyCode ?: "BDT"

        val resolvedParty = document.party?.name?.trim()?.takeIf { it.isNotEmpty() } ?: document.partyName
        if (!document.supplier && result["customer_name"].isNullOrBlank() && resolvedParty.isNotBlank()) {
            result["customer_name"] = resolvedParty
        } else if (document.supplier && result["supplier_name"].isNullOrBlank() && resolvedParty.isNotBlank()) {
            result["supplier_name"] = resolvedParty
        }
        if (result["document_number"].isNullOrBlank() && document.number.isNotBlank()) {
            result["document_number"] = document.number
        }
        document.brand?.let { b ->
            if (result["company_name"].isNullOrBlank() && b.companyName.isNotBlank()) result["company_name"] = b.companyName
            if (result["company_phone"].isNullOrBlank() && b.phone.isNotBlank()) result["company_phone"] = b.phone
            if (result["company_address"].isNullOrBlank() && b.address.isNotBlank()) result["company_address"] = b.address
        }

        // Reformat validated breakdowns into concise, recipient-friendly Taka / currency strings
        document.invoiceBreakdown?.let { inv ->
            if (result["invoice_number"].isNullOrBlank()) result["invoice_number"] = document.number
            document.documentDateMillis?.let { ms ->
                result["invoice_date"] = BrandedDocumentFormatter.formatDateTime(ms, bn, includeTime = true, shortMonth = true)
            }
            result["invoice_amount"] = BrandedPdfSpecBuilder.formatMoney(inv.netInvoiceAmountMinor, currency)
            result["received_payment"] = BrandedPdfSpecBuilder.formatMoney(inv.allocatedPaymentMinor, currency)
            result["invoice_due"] = BrandedPdfSpecBuilder.formatMoney(inv.invoiceDueMinor, currency)
            if (inv.invoiceDiscountMinor > 0L) {
                result["discount_amount"] = BrandedPdfSpecBuilder.formatMoney(inv.invoiceDiscountMinor, currency)
            } else {
                result.remove("discount_amount")
            }
            if (inv.creditReturnMinor > 0L) {
                result["return_amount"] = BrandedPdfSpecBuilder.formatMoney(inv.creditReturnMinor, currency)
            } else {
                result.remove("return_amount")
            }
            if (inv.dueDateMillis != null && inv.invoiceDueMinor > 0L) {
                result["due_date"] = BrandedDocumentFormatter.formatDateTime(inv.dueDateMillis, bn, includeTime = false, shortMonth = true)
            } else {
                result.remove("due_date")
            }
            val prev = inv.historicalPreviousBalanceMinor
            if (inv.hasHistoricalSnapshot && prev != null && prev != 0L) {
                result["previous_due"] = cleanBalanceText(prev, currency, bn)
            } else {
                result.remove("previous_due")
            }
            result["total_outstanding"] = cleanBalanceText(inv.currentAccountBalanceMinor, currency, bn)
            if (inv.isReshareAfterLaterActivity && inv.hasHistoricalSnapshot) {
                result["reshare_notice"] = if (bn) {
                    "📌 বিঃদ্রঃ এই ইনভয়েসের পর আরও লেনদেন হয়েছে; হালনাগাদ মোট ব্যালেন্স দেখানো হলো।"
                } else {
                    "📌 Note: Later account activity occurred after this invoice; live current balance is shown."
                }
            }
        }

        document.paymentBreakdown?.let { pay ->
            val payCurrency = pay.currencyCode
            result["payment_number"] = pay.paymentNumber
            result["payment_date"] = BrandedDocumentFormatter.formatDateTime(pay.paymentDateMillis, bn, includeTime = true, shortMonth = true)
            result["payment_amount"] = BrandedPdfSpecBuilder.formatMoney(pay.paymentAmountMinor, payCurrency)
            result["payment_method"] = pay.paymentMethod
            if (!pay.referenceNote.isNullOrBlank()) {
                result["payment_reference"] = pay.referenceNote
            } else {
                result.remove("payment_reference")
            }
            val prev = pay.previousBalanceMinor
            if (prev != null && prev != 0L) {
                result["previous_balance"] = cleanBalanceText(prev, currency, bn)
            } else {
                result.remove("previous_balance")
            }
            result["total_outstanding"] = cleanBalanceText(pay.currentAccountBalanceMinor, currency, bn)
        }

        document.reminderBreakdown?.let { rem ->
            result["total_outstanding"] = cleanBalanceText(rem.currentTotalDueMinor, currency, bn)
            if (rem.priorBalanceMinor != 0L) {
                result["previous_balance"] = cleanBalanceText(rem.priorBalanceMinor, currency, bn)
            } else {
                result.remove("previous_balance")
            }
            if (rem.latestInvoiceNumber != null && rem.latestInvoiceAmountMinor != null) {
                result["latest_invoice_summary"] = "${rem.latestInvoiceNumber} (${BrandedPdfSpecBuilder.formatMoney(rem.latestInvoiceAmountMinor, currency)})"
            }
            if (rem.latestPaymentNumber != null && rem.latestPaymentAmountMinor != null) {
                result["latest_payment_summary"] = "${rem.latestPaymentNumber} (${BrandedPdfSpecBuilder.formatMoney(rem.latestPaymentAmountMinor, currency)})"
            }
            if (rem.currentTotalDueMinor > 0L && rem.earliestDueDateMillis != null) {
                result["due_date"] = BrandedDocumentFormatter.formatDateTime(rem.earliestDueDateMillis, bn, includeTime = false, shortMonth = true)
            } else {
                result.remove("due_date")
            }
        }

        document.statementBreakdown?.let { stmt ->
            val sCur = stmt.currencyCode
            result["date_range"] = BrandedDocumentFormatter.formatDateRange(stmt.fromMillis, stmt.toMillis, stmt.isAllTime, isBangla = false)
            result["opening_balance"] = cleanBalanceText(stmt.openingBalanceMinor, sCur, bn)
            result["total_sales"] = BrandedPdfSpecBuilder.formatMoney(stmt.totalSalesOrPurchasesMinor, sCur)
            if (document.supplier || stmt.totalReturnMinor == 0L) {
                result.remove("total_returns")
            } else {
                result["total_returns"] = BrandedPdfSpecBuilder.formatMoney(stmt.totalReturnMinor, sCur)
            }
            result["total_payments"] = BrandedPdfSpecBuilder.formatMoney(stmt.totalPaymentMinor, sCur)
            result["closing_balance"] = cleanBalanceText(stmt.closingBalanceMinor, sCur, bn)
        }

        document.returnExchangeBreakdown?.let { re ->
            document.documentDateMillis?.let { ms ->
                result["document_date"] = BrandedDocumentFormatter.formatDateTime(ms, bn, includeTime = true, shortMonth = true)
            }
            if (document.request.module == CommunicationModule.RETURN) {
                result["return_amount"] = BrandedPdfSpecBuilder.formatMoney(re.returnAmountMinor, currency)
                if (re.cashOrBankRefundMinor > 0L) {
                    result["refund_amount"] = BrandedPdfSpecBuilder.formatMoney(re.cashOrBankRefundMinor, currency)
                } else {
                    result.remove("refund_amount")
                }
            } else if (document.request.module == CommunicationModule.EXCHANGE) {
                result["price_difference"] = when {
                    re.netPriceDifferenceMinor > 0L -> "+ ${BrandedPdfSpecBuilder.formatMoney(re.netPriceDifferenceMinor, currency)}"
                    re.netPriceDifferenceMinor < 0L -> "- ${BrandedPdfSpecBuilder.formatMoney(-re.netPriceDifferenceMinor, currency)}"
                    else -> BrandedPdfSpecBuilder.formatMoney(0L, currency)
                }
            }
            result["total_outstanding"] = cleanBalanceText(re.currentAccountBalanceMinor, currency, bn)
        }

        document.deliveryBreakdown?.let { del ->
            document.documentDateMillis?.let { ms ->
                result["document_date"] = BrandedDocumentFormatter.formatDateTime(ms, bn, includeTime = true, shortMonth = true)
            }
            result["total_quantity"] = "${del.totalDispatchedQuantity} PCS"
            val info = listOfNotNull(del.deliveryPerson, del.transportInfo).filter { it.isNotBlank() }.joinToString(" · ")
            if (info.isNotBlank()) result["delivery_information"] = info else result.remove("delivery_information")
        }

        if (document.lineItems.isNotEmpty() && document.request.module in setOf(
                CommunicationModule.DELIVERY,
                CommunicationModule.EXCHANGE,
                CommunicationModule.PACKING
            )
        ) {
            result["items"] = document.lineItems.joinToString("\n") { item ->
                val title = if (bn) item.titleBn.ifBlank { item.titleEn } else item.titleEn
                val variant = listOfNotNull(item.color, item.size).filter { it.isNotBlank() }.joinToString(" / ")
                val titleWithVar = if (variant.isNotEmpty()) "$title ($variant)" else title
                when (document.request.module) {
                    CommunicationModule.EXCHANGE -> {
                        val dir = item.secondaryQuantityText?.ifBlank { "-" } ?: "-"
                        "• [$dir] $titleWithVar × ${item.quantity}"
                    }
                    CommunicationModule.PACKING -> {
                        val ctn = item.secondaryQuantityText?.let { " · $it" } ?: ""
                        "• $titleWithVar — ${item.quantity} ${item.unit}$ctn"
                    }
                    else -> "• $titleWithVar — ${item.quantity} ${item.unit}"
                }
            }
        }

        fun remove(enabled: Boolean, vararg keys: String) {
            if (!enabled) keys.forEach { result.remove(it) }
        }
        remove(settings.includePreviousDue, "previous_due", "previous_balance")
        remove(settings.includeInvoiceAmount, "invoice_amount")
        remove(settings.includePaymentDetails, "received_payment", "historical_payment", "payment_amount")
        remove(settings.includeInvoiceDue, "invoice_due", "historical_due")
        remove(settings.includeOutstanding, "total_outstanding", "historical_balance")
        remove(settings.includeMethodReference, "payment_method", "payment_reference")
        remove(settings.includeSignature, "company_name", "company_phone", "company_address", "company_website", "company_bin", "company_tagline")
        if (settings.includeInstructions) {
            result["payment_instructions"] = settings.paymentInstructions
            result["bank_details"] = settings.bankDetails
        }
        if (settings.defaultPaymentTerms.isNotBlank() && result["payment_terms"].isNullOrBlank() &&
            document.request.module in setOf(CommunicationModule.INVOICE, CommunicationModule.QUOTATION, CommunicationModule.SALES_ORDER)
        ) {
            result["payment_terms"] = settings.defaultPaymentTerms
        }
        result["status_text"] = when (document.status) {
            CommunicationStatus.PAID -> if (bn) "এই ইনভয়েস সম্পূর্ণ পরিশোধিত। গ্রাহকের মোট ব্যালেন্স আলাদাভাবে দেখানো হয়েছে।" else "This invoice is fully paid. Your overall account balance is shown separately."
            CommunicationStatus.PARTIAL -> if (bn) "এই ইনভয়েস আংশিক পরিশোধিত।" else "This invoice is partially paid."
            CommunicationStatus.UNPAID -> if (bn) "এই ইনভয়েসে কোনো পেমেন্ট বরাদ্দ হয়নি।" else "No payment has been allocated to this invoice."
            CommunicationStatus.OVERDUE -> if (bn) "এই ইনভয়েসটির পরিশোধের তারিখ অতিক্রান্ত হয়েছে।" else "This invoice is past its due date."
            CommunicationStatus.SETTLED -> if (bn) "ফেরত / সমন্বয়ের পরে এই ইনভয়েসে বকেয়া নেই।" else "No invoice balance remains after returns / adjustments."
            else -> ""
        }
        if (document.balanceMinor != null && document.balanceMinor < 0 && settings.includeOutstanding) {
            val creditNote = if (bn) "আপনার হিসাবে অগ্রিম / ক্রেডিট রয়েছে।" else "Your account has an advance / credit balance."
            result["status_text"] = listOf(result["status_text"].orEmpty(), creditNote).filter { it.isNotBlank() }.joinToString("\n")
        }
        val showReminder = (settings.includeReminder || document.request.module == CommunicationModule.REMINDER || isOverdueDocument(document)) &&
            (document.balanceMinor ?: 0L) > 0L
        if (showReminder) {
            result["reminder_text"] = if (bn) {
                "সুবিধামতো বকেয়া পরিশোধের অনুরোধ রইল। ইতিমধ্যে পরিশোধ করে থাকলে অনুগ্রহ করে আমাদের জানান।"
            } else {
                "Kindly arrange payment at your convenience. Please let us know if payment has already been made."
            }
        }
        return result
    }

    /** Realistic non-production sample map for live template editor previews. */
    fun sampleFields(module: CommunicationModule, language: String): Map<String, String> {
        val bn = language == "bn"
        val map = linkedMapOf(
            "customer_name" to if (bn) "Hasan Mojumdar" else "Sample Traders (Preview)",
            "customer_phone" to "+8801700000000",
            "supplier_name" to if (bn) "নমুনা সাপ্লায়ার লিমিটেড" else "Sample Supplier Co. Ltd.",
            "invoice_number" to "INV-2026-0001",
            "invoice_date" to if (bn) "২৭ নভে ২০২৬, ০১:৪০" else "27 Nov 2026, 01:40 AM",
            "previous_due" to "৳29,560.00",
            "invoice_amount" to "৳1,380.00",
            "return_amount" to "৳500.00",
            "received_payment" to "৳0.00",
            "invoice_due" to "৳1,380.00",
            "total_outstanding" to "৳30,940.00",
            "payment_number" to "PAY-2026-0005",
            "payment_method" to "Cash",
            "payment_reference" to "TRX-998877",
            "payment_date" to if (bn) "২৭ নভে ২০২৬, ০২:৩০" else "27 Nov 2026, 02:30 PM",
            "payment_amount" to "৳5,000.00",
            "due_date" to if (bn) "০৫ ডিসে ২০২৬" else "05 Dec 2026",
            "document_number" to "${module.name.take(3)}-2026-0010",
            "document_date" to if (bn) "২৭ নভে ২০২৬, ০১:৪০" else "27 Nov 2026, 01:40 PM",
            "document_amount" to "৳12,500.00",
            "total_quantity" to "24 PCS",
            "total_carton" to "4",
            "gross_weight" to "48.500",
            "total_cbm" to "0.320",
            "company_name" to "TUKTAK BD",
            "company_phone" to "01639484547",
            "company_address" to if (bn) "৪১, নিউ এলিফ্যান্ট রোড, ঢাকা-১২০৫" else "41, New Elephant Road, Dhaka-1205",
            "bank_details" to "City Bank · A/C 1234567890",
            "date_range" to "01 Nov 2026 – 27 Nov 2026",
            "opening_balance" to "৳29,560.00",
            "closing_balance" to "৳25,940.00",
            "refund_amount" to "৳0.00",
            "price_difference" to "+ ৳1,500.00",
            "valid_until" to if (bn) "১০ ডিসে ২০২৬" else "10 Dec 2026",
            "delivery_information" to if (bn) "সুন্দরবন কুরিয়ার · ২৮ নভে ২০২৬" else "Sundarban Courier · 28 Nov 2026",
            "items" to "• 4D Cloud Insole Wholesale (Gray / 37-38) — 12 PCS",
            "as_of" to "27 Nov 2026 01:40",
            "historical_as_of" to "27 Nov 2026 01:40",
            "historical_payment" to "৳0.00",
            "historical_due" to "৳1,380.00",
            "historical_balance" to "৳30,940.00",
            "payment_instructions" to if (bn) "বিকাশ / ব্যাংক ট্রান্সফার অনুমোদিত" else "bKash or Bank Transfer accepted",
            "status_text" to if (bn) "আংশিক পরিশোধিত" else "Partially paid",
            "reminder_text" to if (bn) "সুবিধামতো বকেয়া পরিশোধের অনুরোধ রইল।" else "Kindly settle due at your convenience.",
            "customer_code" to "CUST-9668",
            "customer_address" to if (bn) "ঢাকা, বাংলাদেশ" else "Dhaka, Bangladesh",
            "sales_type" to "Wholesale",
            "subtotal_amount" to "৳1,380.00",
            "discount_amount" to "৳100.00",
            "additional_charges" to "৳0.00",
            "payment_terms" to if (bn) "৭ দিনের মধ্যে পরিশোধযোগ্য" else "Net 7 days",
            "company_website" to "www.example-erp.com",
            "company_bin" to "002891928-0101",
            "company_tagline" to "Your Trusted Sourcing & Wholesale Partner",
            "previous_balance" to "৳30,940.00",
            "balance_after_payment" to "৳25,940.00",
            "total_sales" to "৳1,380.00",
            "total_returns" to "৳0.00",
            "total_payments" to "৳5,000.00",
            "reshare_notice" to if (bn) "📌 বিঃদ্রঃ হালনাগাদ ব্যালেন্স দেখানো হলো।" else "📌 Note: Live updated balance shown.",
            "latest_invoice_summary" to "INV-2026-0001 (৳1,380.00)",
            "latest_payment_summary" to "PAY-2026-0005 (৳5,000.00)"
        )
        return map.filterKeys { it in allowed(module) }
    }
}
