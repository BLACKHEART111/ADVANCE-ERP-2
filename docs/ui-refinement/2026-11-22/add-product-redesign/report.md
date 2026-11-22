# ADVANCE ERP — প্রিমিয়াম ৩-ধাপের Add Product UI Redesign ও যাচাই প্রতিবেদন

তারিখ: ২০২৬-১১-২২ (Asia/Dhaka)  
প্যাকেজ / applicationId: `com.pocketdev.advance_erp2` (অপরিবর্তিত)  
ডাটাবেস ভার্সন: v13 (অপরিবর্তিত; কোনো schema/migration পরিবর্তন ছাড়াই বিদ্যমান `Product.description` কলাম সংযুক্ত করা হয়েছে)  
ডিবাগ APK SHA-256: `b78618b3bbdaebf8c1fb59b4f07c353d7da6e6d9442f420a2db62c1416cb172c`

---

## ১. ৩-ধাপের স্ক্রিন ম্যাপিং ও ফিচার সংরক্ষণ

| ধাপ | শিরোনাম | রেফারেন্স উপাদান ও নতুন UI | সংরক্ষিত ও উন্নত কার্যকারিতা |
|---|---|---|---|
| **ধাপ ১** | `১. মূল তথ্য` (`1. Basic Info`) | পার্পল-টু-টিল ওয়েভ গ্রেডিয়েন্ট হেডার, ৩-ধাপের কার্ড স্টেপার, ড্যাশড ছবি আপলোড জোন, ক্যামেরা/গ্যালারি/ফাইল বাটন, ক্যাটাগরি সিলেক্টর, সফট ল্যাভেন্ডার ইনফো কার্ড, ইংরেজি/বাংলা নাম, SKU + টিল অটো-SKU বাটন, নিচে `ধাপ ১ / ৩` ও `পরবর্তী →` ফুটার | সর্বোচ্চ ১০টি পণ্যের ছবি, ফুল-স্ক্রিন প্রিভিউ, Primary ছবি নির্ধারণ, আগে-পিছে রি-অর্ডার, ডিলিট কনফার্মেশন ও DB ডিলিট ট্র্যাকিং; Searchable Category BottomSheet + Inline নতুন ক্যাটাগরি তৈরি (`hideStock = true` হলেও ক্যাটাগরি সর্বদা দৃশ্যমান); ক্যাটাগরির বিদ্যমান পণ্যে Restock/Edit মোড |
| **ধাপ ২** | `২. অন্যান্য` (`2. Other Info`) | বারকোড + পার্পল অটো-বারকোড বাটন, ২-কলাম ক্রয় খরচ ও বিক্রয় মূল্য (`৳` BDT), ২-কলাম স্বল্প স্টক সতর্কতা সীমা ও প্রারম্ভিক মজুদ, এক-সারিতে Unit চিপস, ওয়্যারহাউস সিলেক্টর বটম শিট, `0 / 500` কাউন্টারসহ পণ্যের বিবরণ, অ্যাম্বার `ভ্যারিয়েন্ট ব্যবহার করবেন?` টগল কার্ড, `← পূর্ববর্তী` ও `পরবর্তী →` / `✓ পণ্য যোগ করুন` | ভ্যারিয়েন্ট টগল বন্ধ থাকলে ধাপ ২ থেকেই সরাসরি পণ্য সংরক্ষণ; টগল চালু থাকলে ধাপ ৩-এ যাওয়া; ভ্যারিয়েন্ট স্টক থাকলে প্রারম্ভিক মজুদ অটো-যোগ হয়ে লক হয়; Restock মোডে আগের+নতুন+মোট স্টকের হিসাব; `Product.description` এন্ড-টু-এন্ড সংরক্ষণ; `hideStock = true` (PO ফ্লো) হলে স্টক ও ওয়্যারহাউস নিরাপদে লুকানো থাকে |
| **ধাপ ৩** | `৩. ভ্যারিয়েন্ট` (`3. Variants`) | `পণ্যের ভ্যারিয়েন্ট (N)` ও `+ যোগ করুন` পিল, আলাদা ভ্যারিয়েন্ট এডিটর কার্ড (ল্যাভেন্ডার ইনফো ব্যানার, ২-কলাম `SuggestTextField` রং/সাইজ, ভ্যারিয়েন্ট SKU + অটো জেনারেটর, ভ্যারিয়েন্ট বারকোড + অটো জেনারেটর, ২-কলাম ক্রয়/বিক্রয় মূল্য, ভ্যারিয়েন্ট পরিমাণ, `Variant Photos (0/4)` ড্যাশড আপলোডার, পূর্ণ-প্রস্থ টিল `তালিকায় ভ্যারিয়েন্ট যোগ করুন` বাটন) ও কমপ্যাক্ট ভ্যারিয়েন্ট কার্ড তালিকা | একাধিক ভ্যারিয়েন্ট যোগ, ডুপ্লিকেট SKU/বারকোড প্রতিরোধ, এডিট পেনসিল দিয়ে ভ্যারিয়েন্ট ও ভ্যারিয়েন্টের ছবি লোড ও আপডেট, ড্রাফট ভ্যারিয়েন্ট ডিলিট, `+10`/`+50`/`+100` দ্রুত স্টক যোগ, বিদ্যমান ভ্যারিয়েন্টে Restock ব্যাজ, ভ্যারিয়েন্ট ছবি না থাকলে মূল পণ্যের ছবির ফলব্যাক ও `BASE` ব্যাজ |

---

## ২. পরিবর্তিত ও নতুন ফাইলসমূহ

### নতুন ফাইল
- `app/src/main/java/com/pocketdev/advance_erp/feature/product/AddProductDesignComponents.kt`
- `app/src/test/java/com/pocketdev/advance_erp/AddProductWorkflowLogicTest.kt`
- `app/src/androidTest/java/com/pocketdev/advance_erp/AddProductThreeReferenceTest.kt`

### পরিবর্তিত ফাইল
- `app/src/main/java/com/pocketdev/advance_erp/feature/product/AddProductDialog.kt`
- `app/src/main/java/com/pocketdev/advance_erp/core/designsystem/SuggestTextField.kt`
- `app/src/main/java/com/pocketdev/advance_erp/core/designsystem/ERPFullScreenEditor.kt`
- `app/src/main/java/com/pocketdev/advance_erp/core/designsystem/Theme.kt`
- `app/src/main/java/com/pocketdev/advance_erp/feature/product/ProductViewModel.kt`
- `app/src/main/java/com/pocketdev/advance_erp/feature/product/ProductListScreen.kt`
- `app/src/main/java/com/pocketdev/advance_erp/feature/product/EditProductDialog.kt`
- `app/src/main/java/com/pocketdev/advance_erp/feature/product/ProductDetailDialog.kt`
- `app/src/main/java/com/pocketdev/advance_erp/feature/imports/CreatePurchaseOrderDialog.kt`
- `app/src/main/java/com/pocketdev/advance_erp/feature/imports/ImportViewModel.kt`
- `app/src/androidTest/java/com/pocketdev/advance_erp/PurchaseOrderVisualTest.kt`

---

## ৩. বিল্ড ও টেস্ট ফলাফল

- **Gradle Build & JVM Tests (`adev`)**:
  `BUILD SUCCESSFUL` (`:app:assembleDebug`, `:app:testDebugUnitTest`, `:app:assembleDebugAndroidTest`)
  JVM Unit Test Summary: **71 test files, 381 tests, 0 failures, 0 errors, 0 skipped** (নতুন `AddProductWorkflowLogicTest`-এর ৫টি টেস্টসহ)।
- **ADB Instrumentation Tests (`com.pocketdev.advance_erp2.premiumpoqa.test`)**:
  - `AddProductThreeReferenceTest`: **8/8 passed** (`OK (8 tests)`):
    - `lightThreeStepsAndVariantsCapture`
    - `darkThreeStepsCapture`
    - `createProductWithoutVariantsSubmitsFromStep2`
    - `createProductWithVariantsEditDeleteAndSummedStock`
    - `searchableCategorySheetAndCreateNewCategory`
    - `validationRedirectsToStep1AndCloseShowsUnsavedConfirmation`
    - `englishLanguageModeRendersAllSteps`
    - `roomPersistenceWithoutAndWithVariantsAndPhotos` (ইন-মেমোরি Room ডেটাবেস)
  - `PremiumUiRegressionTest`: **5/5 passed** (`OK (5 tests)`).

---

## ৪. ডিভাইস স্ক্রিনশটসমূহ (Light ও Dark Mode)

- Light Step 1: `screenshots/advance-addproduct-01_add_product_step1_light.png` & `screenshots/advance-addproduct-01b_add_product_step1_bottom_light.png`
- Light Step 2: `screenshots/advance-addproduct-02_add_product_step2_light.png` & `screenshots/advance-addproduct-02b_add_product_step2_bottom_light.png`
- Light Step 3: `screenshots/advance-addproduct-03_add_product_step3_light.png`, `screenshots/advance-addproduct-03b_add_product_step3_bottom_light.png`, `screenshots/advance-addproduct-04_add_product_step3_with_variants_light.png`
- Dark Step 1: `screenshots/advance-addproduct-01_add_product_step1_dark.png` & `screenshots/advance-addproduct-01b_add_product_step1_bottom_dark.png`
- Dark Step 2: `screenshots/advance-addproduct-02_add_product_step2_dark.png` & `screenshots/advance-addproduct-02b_add_product_step2_bottom_dark.png`
- Dark Step 3: `screenshots/advance-addproduct-03_add_product_step3_dark.png` & `screenshots/advance-addproduct-03b_add_product_step3_bottom_dark.png`

---

## ৫. রেফারেন্সের সাথে অবশিষ্ট পার্থক্য ও সীমাবদ্ধতা

- রেফারেন্স মকআপের স্টেপার লেবেল (`১. মূল তথ্য → ২. ভ্যারিয়েন্ট → ৩. অন্যান্য`) বনাম প্রম্পটের ওয়ার্কফ্লো অনুযায়ী এখানে `১. মূল তথ্য → ২. অন্যান্য → ৩. ভ্যারিয়েন্ট` ক্রম ব্যবহার করা হয়েছে যাতে ধাপ ২-এর `ভ্যারিয়েন্ট ব্যবহার করবেন?` টগল অনুযায়ী সরাসরি সেভ বা ধাপ ৩-এ যাওয়া যায়।
- অ্যান্ড্রয়েড সিস্টেম স্ট্যাটাস বার, নেটিভ Material Outlined আইকন ও ডিভাইসের ডিফল্ট বাংলা ফন্ট রেফারেন্সের কাস্টম ইলাস্ট্রেশন/ফন্ট থেকে সামান্য ভিন্ন হতে পারে।
- বিদ্যমান ERP ফিচার সংরক্ষণের জন্য ধাপ ২-এ `পণ্যের একক (Unit)` চিপ সারি এবং ধাপ ৩-এ প্রি-ফিল/রিস্টক/কুইক-অ্যাড বাটন রাখা হয়েছে; ফলে প্রতিটি ধাপ স্বাভাবিকভাবে ভার্টিক্যালি স্ক্রলযোগ্য।
- লাইভ অ্যাপের স্ক্রিনে সিস্টেম ওভারলে/ডায়ালগ থাকায় চলমান সেশন স্পর্শ না করে সমস্ত কীবোর্ড/ইনপুট/স্ক্রলিং/পার্সিস্টেন্স যাচাই আইসোলেটেড ডিভাইস ইন্সট্রুমেন্টেশন টেস্টের মাধ্যমে সম্পন্ন করা হয়েছে।
