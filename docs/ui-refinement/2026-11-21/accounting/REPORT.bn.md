# হিসাব ও অর্থায়ন UI — বাস্তবায়ন ও যাচাই

তারিখ: ২১ নভেম্বর ২০২৬

## সম্পন্ন পরিবর্তন

- `AccountsScreen`-এর আটটি বিদ্যমান বিভাগ—নগদ/ব্যাংক, ব্যয়, আর্থিক বিবরণী, ব্যাংক সমন্বয়, COA, জাবেদা, রেওয়ামিল ও ব্যাকআপ—আলাদা vertically scrollable pager page হিসেবে রাখা হয়েছে। chip tap ও horizontal swipe sync করা হয়; pager স্থির হওয়ার আগে selected tab বদলানো বন্ধ করা হয়েছে। এতে রিপোর্ট পেজ আড়াআড়ি কেটে যাওয়ার দৃশ্যমান সমস্যা ঠিক হয়েছে।
- Purple/Teal gradient accounting banner, সক্রিয় হিসাব সংখ্যা, Light/Dark surface treatment, reusable `FinanceCard`, account accent, এবং responsive action rows যোগ/পরিমার্জন করা হয়েছে। বিদ্যমান top bar ও global five-tab bottom navigation ব্যবহার করা হয়েছে।
- Cash/Bank summary ও account list বিদ্যমান Trial Balance/Room data থেকেই হিসাব করে। আগে ViewModel-এ load হলেও screen-এ না থাকা cash flow summary দৃশ্যমান করা হয়েছে।
- Expense summary, voucher list/empty state এবং voucher-এর category/description/payment details রাখা হয়েছে। Recurring-expense workflow source-এ নেই বলে কৃত্রিম UI যোগ করা হয়নি।
- Financial statements-এ sales return/adjustment ও existing cash flow view যোগ; heading/export actions আলাদা করা হয়েছে। P&L এবং P&L export Owner/Admin role-এ সীমাবদ্ধ। CSV-তেও permission guard যোগ হয়েছে।
- COA search, filters এবং existing protected-account rules অক্ষত; Journal-এ search/period filter ও debit/credit totals; Trial Balance-এ debit, credit, difference ও balanced/unbalanced status; reconciliation/backup-এর existing workflows-কে একই premium card styling-এ রাখা হয়েছে।
- Screen-এর string resources locale-কে persisted preference-এর সঙ্গে মিলিয়ে দেওয়া হয়েছে; chip এবং resource-backed heading/subtitle একই Bangla/English locale পায়। Cash account accent dark mode-এ পাঠযোগ্য Purple token ব্যবহার করে।
- Accounting-related নতুন label values/values-bn-এ যোগ করা হয়েছে। Existing repositories, Room entities, calculation/posting engine বা schema বদলানো হয়নি।

## যাচাই

- Build command (`adev`, pinned Gradle 8.13): `:app:assembleDebug :app:testDebugUnitTest :app:assembleDebugAndroidTest` — **BUILD SUCCESSFUL**, ৪ মিনিট ৯ সেকেন্ড।
- `git diff --check` — পাস।
- Android device QA isolated package `com.pocketdev.advance_erp2.accountingqa`-এ হয়েছে; production package/app uninstall করা হয়নি, live ERP database ব্যবহার করা হয়নি। Pager chip selection, P&L screen width, Bangla/English resource switching এবং Light/Dark palette screen tree/screenshot দিয়ে দেখা হয়েছে। শেষ contrast tweak-এর পর নতুন device screenshot নেওয়া হয়নি।
- Build চলাকালে পূর্বের SDK/AAPT2 “No package ID 7f” warning এবং source deprecation/unused warnings এসেছে; Gradle task ব্যর্থ হয়নি।

## Screenshots

`cash-bank-bn-light.jpg`, `cash-bank-bn-dark.jpg`, `expense-bn-light.jpg`, `statements-bn-light.jpg` — QA captures। `statements-bn-dark.jpg` pager/localization fixes-এর আগের mixed-locale snapshot; এটিকে final visual acceptance হিসেবে গণ্য করবেন না।

## সীমা ও পরবর্তী যাচাই

- Repository-র Trial Balance API date range গ্রহণ করে না; তাই date-range selector বানিয়ে মিথ্যা filtered data দেখানো হয়নি। বর্তমান Trial Balance সর্বকালীন balance দেখায়।
- Recurring expenses implementation নেই।
- Restore flow-এর database-open অবস্থায় SQLite replace করার বিদ্যমান ঝুঁকি checkpoint audit-এ চিহ্নিত; এ কাজে restore engine পরিবর্তন বা live database-এ পরীক্ষা করা হয়নি।
- Build/unit suite পাস হলেও account create, transfer, expense/journal posting, reconciliation, PDF export ও backup/restore-এর end-to-end workflow device-এ transaction করে যাচাই করা হয়নি। Financial data শূন্য/ডেমো QA state-এ visual review করা হয়েছে।
- আটটি tab-এর Light/Dark × Bangla/English screenshot matrix, font-scale/narrow-width acceptance, permissions-role matrix, all-category gesture/back-stack regression visual pass সম্পূর্ণ নয়।

## Source files

- `app/src/main/java/com/pocketdev/advance_erp/feature/accounts/AccountsScreen.kt`
- `app/src/main/java/com/pocketdev/advance_erp/feature/accounts/AccountsViewModel.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-bn/strings.xml`

এই turn-এ More hub, global bottom navigation ও navigation scaffold-এর পরিবর্তনও working tree-তে আছে; সেগুলো আগের একই redesign checkpoint-এর অংশ।
