# ADVANCE ERP — Import Management / PO সংশোধনের রিপোর্ট

**শেষ আপডেট: ১৮ নভেম্বর ২০২৬।** কাজের project: `/data/user/0/com.termux/files/home/ADVANCE-ERP-2`। ফোনে ইনস্টল থাকা মূল APK-এর সঙ্গে এই project-এর APK মিলে যাওয়ায় এখানেই পরিবর্তন করা হয়েছে। আগে থেকে থাকা PDF, Excel ও share পরিবর্তন সংরক্ষণ করা হয়েছে; সংশ্লিষ্ট চারটি export/parser ফাইল source backup-এর সঙ্গে byte-for-byte অপরিবর্তিত।

আগের পূর্ণ audit: `/data/user/0/com.termux/files/home/ADVANCE-ERP-GPT/docs/ui-audit/2026-11-17/import-management/REPORT.bn.md`।

## ফল ও বর্তমান সীমা

PO form এখন **তথ্য → পণ্য → প্যাকিং → হিসাব**—এই চার ধাপে চলে। বিদ্যমান supplier/product/variant তৈরি, সব variant যোগ, single/mixed carton, duplicate/autofill, manual/auto weight, freight, অতিরিক্ত খরচ, attachment, payment term, draft/confirm এবং share কাজগুলো রাখা হয়েছে।

এটি পরীক্ষার **debug APK**, production release sign-off নয়। Code build ও automated test-এর ফল নিচে দেওয়া হয়েছে। শেষ বাংলা/বড়-font visual পরিদর্শন এবং সম্পূর্ণ before/after তুলনা বাকি। ADB সংযোগ বন্ধ হয়ে যাওয়ায় সেগুলো সম্পূর্ণ করা যায়নি।

## অগ্রাধিকারভিত্তিক সমস্যা ও সংশোধন

| অগ্রাধিকার | সমস্যা | সংশোধন |
|---|---|---|
| P0 | PO save-এর পরে carton/cost ব্যর্থ হয়ে অসম্পূর্ণ order থাকত | পুরো form save একটি Room transaction; ব্যর্থ হলে rollback |
| P0 | Edit-এ REPLACE parent মুছে shipment link হারাত | বিদ্যমান parent UPDATE এবং item identity সংরক্ষণ |
| P0 | Manual carton weight save-এ quantity ratio হয়ে যেত | Draft ও shared engine দিয়ে manual share রাখা; অবশিষ্ট ওজন auto allocation |
| P0 | Ordered quantity save-এর সময় packing quantity হয়ে যেত | Ordered ও packed quantity আলাদা; অতিরিক্ত packing ও অর্ডারের বাইরের variant প্রত্যাখ্যান |
| P0 | Receiving-এ missing আবার good quantity থেকে বাদ যেত | Good = physical received − damaged; একই receipt দ্বিতীয়বার এবং অতিরিক্ত receiving-এর transaction guard |
| P0 | Journal ব্যর্থ হলেও payment/expense/stock লেখা থেকে যেত | সংশ্লিষ্ট posting transaction-এ; journal error আর চেপে রাখা হয় না |
| P1 | Create-এ handling/sample খরচ বাদ পড়ত; edit-এ BDT-কে CNY ধরা হতো | Create/edit উভয় পথে save; handling/sample BDT ও rate 1 |
| P1 | Dimension না দিলে carton save ব্যর্থ; edit-এ net/dimension/CBM হারাত | Dimension ঐচ্ছিক; অসম্পূর্ণ dimension validation; পুরোনো net, dimensions ও explicit CBM সংরক্ষণ |
| P1 | পুরোনো carton gross-total শূন্য হলে ভুল total আসত | Declared positive total না থাকলে line weight-এর যোগফল ব্যবহার |
| P1 | Generated freight/China delivery দ্বিগুণ গণনা হতে পারত | পরিচিত generated mirror deduplicate; স্বতন্ত্র user expense রাখা |
| P1 | Local purchase-এ category-র import freight যোগ হতে পারত | Local form-এ international category rate শূন্য; delivery/handling BDT হিসাব রাখা |
| P1 | `product:variant` manual allocation key ভেঙে যেত | শেষ colon ধরে amount parse; variant key অক্ষত |
| P1 | Manual allocation option-এ বরাদ্দ দেওয়ার input ছিল না | প্রতি item-এর BDT allocation input; মোটের সঙ্গে validation এবং finalization-এ ব্যবহার |
| P1 | Edit-এ lifecycle/paid/received তথ্য নষ্ট হতে পারত | Financial edit lock; note/address/attachment-এর আলাদা metadata edit রাখা |
| P1 | Advance payment থাকলে packing বন্ধ হয়ে যেত | Finalization/receiving শুরু হওয়ার আগে packing অনুমোদিত; commercial financial edit আলাদা lock |
| P1 | Duplicate PO-এর স্বতন্ত্র batch থাকত না | নতুন draft ও batch; packing copy; paid/finalized flags reset |
| P1 | Auto batch থাকার কারণে PO cancel বন্ধ ছিল | কেবল অনির্বাহিত auto draft batch থাকলে cancellation সম্ভব; transaction-এ payable সমন্বয় |
| P1 | পুরোনো selection-এর collectors নতুন detail overwrite করত | Selection বদলালে আগের collector job cancel; একই shipment-এর duplicate subscription বন্ধ |
| P1 | Shipment workspace ও drawer route ঠিকমতো পৌঁছাত না | Dedicated shipment list, search, workspace entry, create ও বিদ্যমান packing-list share |
| P1 | Shipment/carton input-এ variant হারাত; field state update হতো না | Variant selection/propagation ও Compose-observable draft state |
| P2 | সব input এক দীর্ঘ form-এ; async save-এ dialog আগে বন্ধ হতো | চার ধাপ, scroll reset, busy state, সফল save-এর পর dismiss; error-এ input রাখা |
| P2 | বাংলা label ও action এক সারিতে গাদাগাদি | ছোট label, variant action আলাদা সারি, যথাস্থানে 48dp touch target |
| P2 | Typography-এর স্থির color button text ঢেকে দিত | Material content color অনুসরণ; shared button বড় font-এ বাড়তে পারে |
| P2 | Receiving-এর চারটি input অতিরিক্ত সরু | দুই সারিতে দুটি করে input |
| P2 | Extra cost/completion ছোট screen বা keyboard-এ আটকে যেত | Height cap, vertical scroll ও IME padding |
| P2 | Draft/cancelled PO overview-তে payable বাড়াত | দায়ের মোটে কার্যকর order |
| P2 | Supplier বদলালেও পুরোনো PO/amount থাকত | Dependent payment selection reset; PO নিলে remaining amount; draft/cancelled filter |

Purple + Teal পরিচয় বজায় রাখা হয়েছে। অপ্রয়োজনীয় নতুন feature বা decoration যোগ করা হয়নি। Shipment list-এ আগে থেকে থাকা shipment card ও share ক্ষমতা ব্যবহার করা হয়েছে।

## হিসাব ও metadata সুরক্ষা

- Full PO save, payment, packing এবং সংশ্লিষ্ট financial posting-এ transaction boundary রয়েছে।
- `addCostItem`, `adjustLandedCost`, `postWarehouseReceipt`, `addShipmentExpenseWithManualAllocations`, `applyPostFinalizationCostAdjustment`, warehouse rent ও customer delivery voucher-এ journal failure propagate করে rollback হয়।
- Posted/capitalized expense সরাসরি delete না করে reversal/cost-adjustment প্রয়োজনের validation রাখা হয়েছে।
- Paid/finalized/completed বা receiving শুরু হওয়া PO-তে non-financial editor শুধু note, forwarder/delivery address ও attachment বদলায়। Amount, paid balance, status, item, packing, cost ও shipment link বদলায় না।
- নতুন database schema/migration প্রয়োজন হয়নি। বিদ্যমান repositories/ViewModels-এর কাজ বজায় রেখে নির্দিষ্ট ত্রুটি সংশোধিত হয়েছে।

## যাচাইয়ের প্রকৃত ফল

| পরীক্ষা | ফল |
|---|---|
| `assembleDebug` | সফল |
| `testDebugUnitTest` | **৫৯ suite, ২৯৭ test; failure/error/skipped = ০** |
| `assembleDebugAndroidTest` | সফল; ১৩টি Room ও ৫টি form test প্রস্তুত |
| সর্বশেষ main APK-তে real Room device test | **১৩/১৩ পাস**; পৃথক in-memory database |
| Paid-PO metadata form behavior | পাস |
| Corrected dialog-window capture দিয়ে English mixed-carton flow | পাস; ordered quantity, manual gross, net, dimension ও CBM save callback যাচাই |
| APK signature | মূল app-এর একই debug certificate; `install -r` সফল |
| APK package/CRC | arm64-v8a; archive integrity ঠিক |
| Source whitespace check | `git diff --check` পাস |

Device test-এ atomic save/rollback, shipment link, manual weights, BDT costs, draft liability/cancel, payment/expense journal rollback, receiving/missing ও duplicate posting, duplicate PO, manual variant allocation, local freight, net/dimension/CBM, advance-এর পরে packing এবং metadata edit-এর আর্থিক তথ্য সংরক্ষণ যাচাই করা হয়েছে।

### Visual যাচাই সম্পর্কে স্পষ্ট সীমা

প্রথম iteration-এর production screen ফোনে দেখে mode button-এর অসম উচ্চতা, দীর্ঘ বাংলা label ও variant action crowding শনাক্ত করে সংশোধন করা হয়েছে।

শেষ device suite-এ ৪টি screenshot test blank window ধরায় থেমেছিল। সেগুলো পাস হিসেবে গণনা করা হয়নি। Capture পদ্ধতি আসল dialog window-তে বদলানোর পরে English flow ও nonblank-image guard পাস করেছে এবং ফোনে সাতটি image তৈরি হয়েছে। **সংযোগ চলে যাওয়ায় সেগুলো এখানে এনে চোখে পরিদর্শন করা যায়নি।** সংশোধিত capture দিয়ে বাকি বাংলা, ১৩০% font এবং local-purchase flow পুনরায় চালানো বাকি। অতএব শেষ সংস্করণকে pixel-perfect বা সম্পূর্ণ visually verified বলা হচ্ছে না।

প্রাসঙ্গিক evidence:

- `unit-test-summary.json`
- `build.log` এবং `capture-test-build.log`
- `device-tests-final-before-capture-fix.log` — ১৩টি database ও metadata behavior পাস; পুরোনো capture-এ ৪টি failure
- `english-flow-after-capture-fix.log` — সংশোধিত capture-এ English flow পাস
- `source-manifest.json`, `apk-signature.txt`

Android SDK 37-এর InputManager API-এর সঙ্গে test runner মিলাতে শুধু test dependency-তে Espresso 3.7.0 ও AndroidX JUnit 1.3.0 নেওয়া হয়েছে। Production dependency বদলানো হয়নি। [AndroidX Test release notes](https://developer.android.com/jetpack/androidx/releases/test)।

## Data protection

মূল APK, source patch/archive ও app-এর private data backup রাখা হয়েছে। App uninstall করা হয়নি। আগের একটি পূর্ণ row comparison-এ মূল database-এর **changedTables = []**, `integrity_check = ok` পাওয়া গেছে (`data-protection.json`)। সর্বশেষ install-এর আগেও নতুন backup নেওয়া হয়েছে। Regression test ব্যবহারকারীর বাস্তব financial database ব্যবহার করেনি; সর্বশেষ device disconnect-এর পরে আর একটি row comparison করা যায়নি।

## পরিবর্তিত ফাইলের দল

- `feature/imports/CreatePurchaseOrderDialog.kt`, `PurchaseOrderDetailDialog.kt`, `ImportManagementScreen.kt`, `ImportViewModel.kt`
- `feature/imports/PackingVariantSelector.kt`, `AddCartonDialog.kt`, `CreateShipmentDialog.kt`, `AddCostItemDialog.kt`
- `feature/imports/RecordSupplierPaymentDialog.kt`, `WarehouseReceivingDialog.kt`, `AddAdditionalCostDialog.kt`, `PurchaseCompletionDialog.kt`
- `core/engine/CartonWeightAllocationEngine.kt`, `LandedCostAllocationEngine.kt`, `LandingCostEngine.kt`
- `core/designsystem/Type.kt`, `ERPActionButton.kt`
- `domain/repository/ImportRepository.kt`, `data/repository/ImportRepositoryImpl.kt`
- `data/dao/PurchaseOrderDao.kt`, `ImportShipmentDao.kt`
- `navigation/AdvanceErpNavGraph.kt`, বাংলা/ইংরেজি import notice এবং test dependencies
- `ImportAuditRegressionTest.kt`, `ImportPersistenceRegressionTest.kt`, `PurchaseOrderVisualTest.kt`

## অবশিষ্ট কাজ

1. সংশোধিত capture-এ বাকি তিনটি locale/font flow চালিয়ে সব image কপি ও পরিদর্শন; চূড়ান্ত before/after তুলনা।
2. নতুন net-weight input, metadata editor ও shipment list-এর keyboard/small-screen visual পরিদর্শন।
3. Bundled Camera/ML Kit native library-এর বিদ্যমান **16 KB alignment warning**-এর পৃথক compatibility কাজ।
4. Tablet/foldable hardware এবং camera/gallery/external sharing-এর বাস্তব device পরীক্ষা।

## APK

পরীক্ষার মূল APK: `/data/user/0/com.termux/files/home/ADVANCE-ERP-builds/import-ui-20261118/ADVANCE-ERP-import-ui-debug.apk`।

Test APK ও `SHA256SUMS.txt` একই directory-তে আছে। মূল APK সর্বশেষ সংযোগে ফোনে install করা হয়েছে।
