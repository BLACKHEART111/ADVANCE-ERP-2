package com.pocketdev.advance_erp.core.model

import com.pocketdev.advance_erp.data.entity.ChartOfAccount
import com.pocketdev.advance_erp.data.entity.LiquidAccountKind

enum class BankCategory(val titleEn: String, val titleBn: String) {
    STATE_OWNED("State-Owned Commercial", "রাষ্ট্রায়ত্ত বাণিজ্যিক ব্যাংক"),
    SPECIALIZED("Specialized Bank", "বিশেষায়িত ব্যাংক"),
    PRIVATE_CONVENTIONAL("Private Commercial", "বেসরকারি বাণিজ্যিক ব্যাংক"),
    ISLAMI("Islami Shariah-Based", "ইসলামী শরীয়াহ ভিত্তিক"),
    FOREIGN("Foreign Commercial", "বিদেশি বাণিজ্যিক ব্যাংক"),
    NON_SCHEDULED("Other / Non-Scheduled", "অন্যান্য ব্যাংক")
}

data class BangladeshBankInfo(
    val code: String,
    val nameEn: String,
    val nameBn: String,
    val shortName: String,
    val category: BankCategory
)

data class MobileBankingProvider(
    val code: String,
    val nameEn: String,
    val nameBn: String,
    val paymentMethodCode: String
)

/**
 * Complete directory of Bangladesh Bank scheduled commercial/specialized banks,
 * major non-scheduled banks, and Bangladesh MFS (Mobile Financial Services) providers.
 */
object BangladeshBanks {

    val allBanks: List<BangladeshBankInfo> = listOf(
        // State-Owned Commercial Banks (6)
        BangladeshBankInfo("SONALI", "Sonali Bank PLC", "সোনালী ব্যাংক পিএলসি", "Sonali", BankCategory.STATE_OWNED),
        BangladeshBankInfo("JANATA", "Janata Bank PLC", "জনতা ব্যাংক পিএলসি", "Janata", BankCategory.STATE_OWNED),
        BangladeshBankInfo("AGRANI", "Agrani Bank PLC", "অগ্রণী ব্যাংক পিএলসি", "Agrani", BankCategory.STATE_OWNED),
        BangladeshBankInfo("RUPALI", "Rupali Bank PLC", "রূপালী ব্যাংক পিএলসি", "Rupali", BankCategory.STATE_OWNED),
        BangladeshBankInfo("BASIC", "BASIC Bank Limited", "বেসিক ব্যাংক লিমিটেড", "BASIC", BankCategory.STATE_OWNED),
        BangladeshBankInfo("BDBL", "Bangladesh Development Bank PLC", "বাংলাদেশ ডেভেলপমেন্ট ব্যাংক পিএলসি", "BDBL", BankCategory.STATE_OWNED),

        // Specialized Banks (3)
        BangladeshBankInfo("BKB", "Bangladesh Krishi Bank", "বাংলাদেশ কৃষি ব্যাংক", "BKB / Krishi", BankCategory.SPECIALIZED),
        BangladeshBankInfo("RAKUB", "Rajshahi Krishi Unnayan Bank", "রাজশাহী কৃষি উন্নয়ন ব্যাংক", "RAKUB", BankCategory.SPECIALIZED),
        BangladeshBankInfo("PKB", "Probashi Kallyan Bank", "প্রবাসী কল্যাণ ব্যাংক", "PKB", BankCategory.SPECIALIZED),

        // Private Commercial Banks - Conventional (33)
        BangladeshBankInfo("ABBL", "AB Bank PLC", "এবি ব্যাংক পিএলসি", "AB Bank", BankCategory.PRIVATE_CONVENTIONAL),
        BangladeshBankInfo("BCBL", "Bangladesh Commerce Bank Limited", "বাংলাদেশ কমার্স ব্যাংক লিমিটেড", "BCBL", BankCategory.PRIVATE_CONVENTIONAL),
        BangladeshBankInfo("BANKASIA", "Bank Asia PLC", "ব্যাংক এশিয়া পিএলসি", "Bank Asia", BankCategory.PRIVATE_CONVENTIONAL),
        BangladeshBankInfo("BENGAL", "Bengal Commercial Bank PLC", "বেঙ্গল কমার্শিয়াল ব্যাংক পিএলসি", "Bengal Bank", BankCategory.PRIVATE_CONVENTIONAL),
        BangladeshBankInfo("BRAC", "BRAC Bank PLC", "ব্র্যাক ব্যাংক পিএলসি", "BRAC", BankCategory.PRIVATE_CONVENTIONAL),
        BangladeshBankInfo("CITIZENS", "Citizens Bank PLC", "সিটিজেন্স ব্যাংক পিএলসি", "Citizens", BankCategory.PRIVATE_CONVENTIONAL),
        BangladeshBankInfo("CBBL", "Community Bank Bangladesh PLC", "কমিউনিটি ব্যাংক বাংলাদেশ পিএলসি", "Community", BankCategory.PRIVATE_CONVENTIONAL),
        BangladeshBankInfo("DHAKA", "Dhaka Bank PLC", "ঢাকা ব্যাংক পিএলসি", "Dhaka Bank", BankCategory.PRIVATE_CONVENTIONAL),
        BangladeshBankInfo("DBBL", "Dutch-Bangla Bank PLC", "ডাচ-বাংলা ব্যাংক পিএলসি", "DBBL", BankCategory.PRIVATE_CONVENTIONAL),
        BangladeshBankInfo("EBL", "Eastern Bank PLC", "ইস্টার্ন ব্যাংক পিএলসি", "EBL", BankCategory.PRIVATE_CONVENTIONAL),
        BangladeshBankInfo("IFIC", "IFIC Bank PLC", "আইএফআইসি ব্যাংক পিএলসি", "IFIC", BankCategory.PRIVATE_CONVENTIONAL),
        BangladeshBankInfo("JAMUNA", "Jamuna Bank PLC", "যমুনা ব্যাংক পিএলসি", "Jamuna", BankCategory.PRIVATE_CONVENTIONAL),
        BangladeshBankInfo("MEGHNA", "Meghna Bank PLC", "মেঘনা ব্যাংক পিএলসি", "Meghna", BankCategory.PRIVATE_CONVENTIONAL),
        BangladeshBankInfo("MBL", "Mercantile Bank PLC", "মার্কেন্টাইল ব্যাংক পিএলসি", "Mercantile", BankCategory.PRIVATE_CONVENTIONAL),
        BangladeshBankInfo("MDB", "Midland Bank PLC", "মিডল্যান্ড ব্যাংক পিএলসি", "Midland", BankCategory.PRIVATE_CONVENTIONAL),
        BangladeshBankInfo("MODHUMOTI", "Modhumoti Bank PLC", "মধুমতি ব্যাংক পিএলসি", "Modhumoti", BankCategory.PRIVATE_CONVENTIONAL),
        BangladeshBankInfo("MTB", "Mutual Trust Bank PLC", "মিউচুয়াল ট্রাস্ট ব্যাংক পিএলসি", "MTB", BankCategory.PRIVATE_CONVENTIONAL),
        BangladeshBankInfo("NBL", "National Bank Limited", "ন্যাশনাল ব্যাংক লিমিটেড", "NBL", BankCategory.PRIVATE_CONVENTIONAL),
        BangladeshBankInfo("NCC", "NCC Bank PLC", "এনসিসি ব্যাংক পিএলসি", "NCC Bank", BankCategory.PRIVATE_CONVENTIONAL),
        BangladeshBankInfo("NRB", "NRB Bank PLC", "এনআরবি ব্যাংক পিএলসি", "NRB", BankCategory.PRIVATE_CONVENTIONAL),
        BangladeshBankInfo("NRBC", "NRBC Bank PLC", "এনআরবিসি ব্যাংক পিএলসি", "NRBC", BankCategory.PRIVATE_CONVENTIONAL),
        BangladeshBankInfo("ONE", "One Bank PLC", "ওয়ান ব্যাংক পিএলসি", "One Bank", BankCategory.PRIVATE_CONVENTIONAL),
        BangladeshBankInfo("PADMA", "Padma Bank PLC", "পদ্মা ব্যাংক পিএলসি", "Padma", BankCategory.PRIVATE_CONVENTIONAL),
        BangladeshBankInfo("PREMIER", "Premier Bank PLC", "প্রিমিয়ার ব্যাংক পিএলসি", "Premier", BankCategory.PRIVATE_CONVENTIONAL),
        BangladeshBankInfo("PRIME", "Prime Bank PLC", "প্রাইম ব্যাংক পিএলসি", "Prime Bank", BankCategory.PRIVATE_CONVENTIONAL),
        BangladeshBankInfo("PUBALI", "Pubali Bank PLC", "পূবালী ব্যাংক পিএলসি", "Pubali", BankCategory.PRIVATE_CONVENTIONAL),
        BangladeshBankInfo("SHIMANTO", "Shimanto Bank PLC", "সীমান্ত ব্যাংক পিএলসি", "Shimanto", BankCategory.PRIVATE_CONVENTIONAL),
        BangladeshBankInfo("SBAC", "SBAC Bank PLC", "এসবিএসি ব্যাংক পিএলসি", "SBAC", BankCategory.PRIVATE_CONVENTIONAL),
        BangladeshBankInfo("SEBL", "Southeast Bank PLC", "সাউথইস্ট ব্যাংক পিএলসি", "Southeast", BankCategory.PRIVATE_CONVENTIONAL),
        BangladeshBankInfo("CITY", "The City Bank PLC", "সিটি ব্যাংক পিএলসি", "City Bank", BankCategory.PRIVATE_CONVENTIONAL),
        BangladeshBankInfo("TRUST", "Trust Bank PLC", "ট্রাস্ট ব্যাংক পিএলসি", "Trust Bank", BankCategory.PRIVATE_CONVENTIONAL),
        BangladeshBankInfo("UCB", "United Commercial Bank PLC", "ইউনাইটেড কমার্শিয়াল ব্যাংক পিএলসি", "UCB", BankCategory.PRIVATE_CONVENTIONAL),
        BangladeshBankInfo("UTTARA", "Uttara Bank PLC", "উত্তরা ব্যাংক পিএলসি", "Uttara", BankCategory.PRIVATE_CONVENTIONAL),

        // Islami Shariah-Based Private Commercial Banks (10)
        BangladeshBankInfo("AIBL", "Al-Arafah Islami Bank PLC", "আল-আরাফাহ ইসলামী ব্যাংক পিএলসি", "Al-Arafah", BankCategory.ISLAMI),
        BangladeshBankInfo("EXIM", "EXIM Bank PLC", "এক্সিম ব্যাংক পিএলসি", "EXIM", BankCategory.ISLAMI),
        BangladeshBankInfo("FSIBL", "First Security Islami Bank PLC", "ফার্স্ট সিকিউরিটি ইসলামী ব্যাংক পিএলসি", "FSIBL", BankCategory.ISLAMI),
        BangladeshBankInfo("GIB", "Global Islami Bank PLC", "গ্লোবাল ইসলামী ব্যাংক পিএলসি", "Global Islami", BankCategory.ISLAMI),
        BangladeshBankInfo("ICBIB", "ICB Islamic Bank Limited", "আইসিবি ইসলামিক ব্যাংক লিমিটেড", "ICB Islamic", BankCategory.ISLAMI),
        BangladeshBankInfo("IBBL", "Islami Bank Bangladesh PLC", "ইসলামী ব্যাংক বাংলাদেশ পিএলসি", "IBBL", BankCategory.ISLAMI),
        BangladeshBankInfo("SJIBL", "Shahjalal Islami Bank PLC", "শাহজালাল ইসলামী ব্যাংক পিএলসি", "Shahjalal", BankCategory.ISLAMI),
        BangladeshBankInfo("SIBL", "Social Islami Bank PLC", "সোশ্যাল ইসলামী ব্যাংক পিএলসি", "SIBL", BankCategory.ISLAMI),
        BangladeshBankInfo("SBL", "Standard Bank PLC", "স্ট্যান্ডার্ড ব্যাংক পিএলসি", "Standard Bank", BankCategory.ISLAMI),
        BangladeshBankInfo("UNION", "Union Bank PLC", "ইউনিয়ন ব্যাংক পিএলসি", "Union Bank", BankCategory.ISLAMI),

        // Foreign Commercial Banks (9)
        BangladeshBankInfo("ALFALAH", "Bank Al-Falah Limited", "ব্যাংক আল-ফালাহ লিমিটেড", "Al-Falah", BankCategory.FOREIGN),
        BangladeshBankInfo("CITI", "Citibank N.A.", "সিটিব্যাংক এন.এ.", "Citibank", BankCategory.FOREIGN),
        BangladeshBankInfo("CBC", "Commercial Bank of Ceylon PLC", "কমার্শিয়াল ব্যাংক অব সিলন পিএলসি", "Ceylon", BankCategory.FOREIGN),
        BangladeshBankInfo("HBL", "Habib Bank Limited", "হাবিব ব্যাংক লিমিটেড", "HBL", BankCategory.FOREIGN),
        BangladeshBankInfo("HSBC", "HSBC Bangladesh", "এইচএসবিসি বাংলাদেশ", "HSBC", BankCategory.FOREIGN),
        BangladeshBankInfo("NBP", "National Bank of Pakistan", "ন্যাশনাল ব্যাংক অব পাকিস্তান", "NBP", BankCategory.FOREIGN),
        BangladeshBankInfo("SCB", "Standard Chartered Bank", "স্ট্যান্ডার্ড চার্টার্ড ব্যাংক", "SCB", BankCategory.FOREIGN),
        BangladeshBankInfo("SBI", "State Bank of India", "স্টেট ব্যাংক অব ইন্ডিয়া", "SBI", BankCategory.FOREIGN),
        BangladeshBankInfo("WOORI", "Woori Bank", "উরি ব্যাংক", "Woori", BankCategory.FOREIGN),

        // Non-Scheduled / Specialized Domestic Banks (4)
        BangladeshBankInfo("GRAMEEN", "Grameen Bank", "গ্রামীণ ব্যাংক", "Grameen", BankCategory.NON_SCHEDULED),
        BangladeshBankInfo("KARMASANGSTHAN", "Karmasangsthan Bank", "কর্মসংস্থান ব্যাংক", "Karmasangsthan", BankCategory.NON_SCHEDULED),
        BangladeshBankInfo("ANSARVDP", "Ansar-VDP Unnayan Bank", "আনসার-ভিডিপি উন্নয়ন ব্যাংক", "Ansar-VDP", BankCategory.NON_SCHEDULED),
        BangladeshBankInfo("PALLISANCHAY", "Palli Sanchay Bank", "পল্লী সঞ্চয় ব্যাংক", "Palli Sanchay", BankCategory.NON_SCHEDULED)
    )

    val mfsProviders: List<MobileBankingProvider> = listOf(
        MobileBankingProvider("BKASH", "bKash", "বিকাশ", "BKASH"),
        MobileBankingProvider("NAGAD", "Nagad", "নগদ", "NAGAD"),
        MobileBankingProvider("ROCKET", "Rocket (DBBL)", "রকেট (ডাচ-বাংলা)", "ROCKET"),
        MobileBankingProvider("UPAY", "Upay", "উপায়", "UPAY"),
        MobileBankingProvider("TAP", "Tap", "ট্যাপ", "MFS"),
        MobileBankingProvider("SURECASH", "SureCash", "শিওরক্যাশ", "MFS"),
        MobileBankingProvider("ISLAMIC_WALLET", "Islamic Wallet", "ইসলামিক ওয়ালেট", "MFS"),
        MobileBankingProvider("OK_WALLET", "OK Wallet", "ওকে ওয়ালেট", "MFS"),
        MobileBankingProvider("MCASH", "mCash", "এমক্যাশ", "MFS"),
        MobileBankingProvider("MYCASH", "MyCash", "মাইক্যাশ", "MFS")
    )

    /** Infers the canonical payment method string from a liquid [ChartOfAccount]. */
    fun inferPaymentMethod(account: ChartOfAccount): String {
        return when (account.liquidKind) {
            LiquidAccountKind.CASH -> "CASH"
            LiquidAccountKind.BANK -> "BANK"
            LiquidAccountKind.MOBILE_BANKING -> {
                val hint = "${account.institutionName.orEmpty()} ${account.nameEn}".lowercase()
                when {
                    "bkash" in hint -> "BKASH"
                    "nagad" in hint -> "NAGAD"
                    "rocket" in hint -> "ROCKET"
                    "upay" in hint -> "UPAY"
                    else -> "MFS"
                }
            }
            null -> when (account.accountCode) {
                "1020", "1030" -> "BANK"
                "1060" -> "BKASH"
                "1061" -> "NAGAD"
                else -> "CASH"
            }
        }
    }
}
