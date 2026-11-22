# ছয়-reference PO UI — implementation ও verification

এই প্রতিবেদন একই `com.pocketdev.advance_erp2` application-এর ছয়টি production Compose screen নিয়ে। Reference-এর static business values production-এ বসানো হয়নি। Screenshot tests production components-কে isolated fixture data দিয়ে device-এ render করে; Room-এ fixture ঢোকায় না।

## Screen mapping ও feature preservation

| Screen | নতুন presentation | সংরক্ষিত কার্যক্রম / তথ্য |
|---|---|---|
| 01 — PO List | Wave banner, এক সারিতে চার status card, Create PO, search/filter, compact order cards | বাস্তব order count/status, PO/supplier/notes/shipment search, সব status, supplier filtering; order খুলে confirmation, PDF, payment; অন্যান্য import module drawer/import hub-এ |
| 02 — Information | Compact header/stepper, supplier card, PO reference, পাশাপাশি dates, purchase-type selector, currency/rate, payment terms, notes | Searchable supplier ও inline create; supplier currency defaults; auto-generated unique PO number; order/arrival dates; CNY/USD/BDT; manual rate; সব payment terms; shipping method, forwarder/contact, delivery charge/BDT preview expandable |
| 03 — Products | দুই primary action, information banner, thumbnail/SKU/variant/quantity/price/total cards | Real local photos থাকলে load; product/variant/base selection, new product, all variants, new variant, quantity/unit price, line remove; pencil খুললে existing editors ও fallback packing fields; quantity/line totals |
| 04 — Packaging | চার shipment metric, Add Carton, collapsible carton cards, compact totals | Carton identity, gross/net, dimensions/CBM, auto-fill, duplicate/delete, mixed products/variants, per-line quantities/manual weights ও automatic allocation; existing validation; guide expandable |
| 05 — Landed Cost | Weight-based costing panel, category freight inputs, financial rows, yellow/teal total | পুরোনো category-weight/freight calculations, delivery conversion, handling/sample capitalization, per-unit preview, OPEX explanation; payment schedule, notes/attachments/camera/gallery/file workflows |
| 06 — Details | Purple/teal summary with dates/currency/terms, five tabs, summary/action cards | Supplier/contact/attachments, items edit/delete, carton edit/delete, costs/finalization, payments/schedule/history, receiving/posting, damage/shortage, additional costs; header menu-তে confirm/edit/duplicate/cancel/completion-এর existing actions |

## Architecture / data

- Production application ID, signing setup, label/icon, entities, Room database version ও migrations পরিবর্তিত হয়নি।
- PO posting, draft confirmation, payable/payment journals, receiving/FIFO, allocation engines ও PDF generation পরিবর্তিত হয়নি।
- Photo resolution existing ProductRepository/ImageStorage ব্যবহার করে; runtime decorative downloads বা নতুন dependency নেই।
- Dialog-এর navigation existing shared ERPBottomNavigation ও একই NavController ব্যবহার করে। Changed wizard থেকে tab বদলালে existing draft/discard/continue prompt চলে।
- Large font/narrow display-তে text wrap ও natural vertical/horizontal scrolling বজায় আছে। Reference-এর collage উচ্চতায় form জোর করে বসানো হয়নি।

## Visual review policy

প্রতিটি reference-এর সঙ্গে device screenshot তুলনা করা হয়েছে। Device display 1272×2772, density 560 dpi; reference PNG আলাদা viewport ও font rendering ব্যবহার করে। Screenshot comparison-এ business data, Android status icons, vendor font এবং AntiGem floating overlay-কে pixel-equality হিসেবে দাবি করা যাবে না।

Reference ও final screenshot পাশাপাশি [comparison.html](comparison.html)-এ আছে।


## Final screen-by-screen comparison

| Screen | মিল ও refinement | অবশিষ্ট পার্থক্য |
|---|---|---|
| [PO List](screenshots/01_PO_List.png) | Branded header, wave, চার status card, Create PO, search/filter, compact order cards ও selected bottom tab। বাড়তি payment row সরিয়ে menu-তে রাখা হয়েছে; status label ছোট ও purple selection সংশোধিত। | মূল app logo ও Material icon family রাখা হয়েছে। Reference-এর static counts/amounts copy করা হয়নি; import tools menu দিয়ে accessible। |
| [PO Information](screenshots/02_PO_Information.png) | Main field order, supplier/PO/date/type/currency groups, four steps, payment terms ও notes। Date fields card-এ বদলানো; currency wrapping ও footer inset ঠিক; supplier create picker-এ রাখা। | Rate সরাসরি numeric input; generated PO number read-only। Shipping/delivery/contact অতিরিক্ত section-এ। ছোট viewport/বড় font-এ notes পেতে scroll লাগে। |
| [Products & Variants](screenshots/03_PO_Products.png) | Purple/teal Add actions, information banner, thumbnail/SKU/variant badge, quantity-price-total, edit/delete ও summary। বড় selector forms compact cards-এর pencil editor-এ গেছে; existing local photos resolve করা হয়েছে। | Fixture products-এর photos নেই, তাই fallback icon দেখা যায়; বাস্তব local photos থাকলে thumbnail আসে। সব variant ও packing controls edit panel-এ থাকায় expanded layout বড়। |
| [Packaging](screenshots/04_PO_Packaging.png) | চার shipment metric, Add Carton, collapsed/expanded carton cards, dimensions, gross/net ও CBM। Guide নিচে সরানো; mixed header ছোট; manual assignments expandable; editable carton name-এ expansion reset বন্ধ। | Reference-এর simple carton-এর পাশাপাশি mixed-carton/manual weight allocation রাখতে বাড়তি controls আছে। প্রথম viewport-এ সব carton না-ও দেখা যেতে পারে। |
| [Landed Cost](screenshots/05_PO_Landed_Cost.png) | Weight summary, rate/charge ও handling/sample inputs, financial rows, yellow/teal total ও average। Input card, breakdown ও total আলাদা করা; category rates ও existing calculations retained। | প্রতি category-র আলাদা freight rate এবং sample/OPEX/attachments/payment schedule বজায় আছে; তাই reference-এর single-rate screen-এর চেয়ে scroll বেশি হতে পারে। |
| [PO Details](screenshots/06_PO_Details.png) | Purple-to-teal hero, status, date/arrival/currency/term metadata, পাঁচ tabs, breakdown এবং payment/receiving/PDF cards। Contrast/status colors, metadata icon containers, compact tabs; legacy operations context menu-তে। | Dates, totals ও cost rows real record/fixture অনুযায়ী বদলায়। Native icon/typography/gradient shape একেবারে identical নয়; history, corrections ও additional operations retained। |

## Verification

- `assembleDebug`, `testDebugUnitTest`, `assembleDebugAndroidTest`: logs/build.log-এ actual ফল।
- JVM summary: `{"tests": 376, "failures": 0, "errors": 0, "skipped": 0}`।
- ADB runner: `PurchaseOrderSixReferenceTest`, `PurchaseOrderVisualTest`, `PremiumUiRegressionTest`, `ImportPersistenceRegressionTest`; logs/device-tests.log-এ actual ফল।
- পূর্বের Gradle connected-test teardown-এর missing `aapt` সমস্যার জন্য APK install এবং instrumentation runner সরাসরি ADB দিয়ে চালানো হয়েছে। এটি wrapper task-এর pass দাবি নয়।
- Test fixtures: list/details, four steps, Light/Dark, Bangla/English, font scale, narrow viewport, restoration; real SQLite in-memory tests-এ draft/edit/confirm, payment rollback, receiving, mixed weights/CBM, costs এবং schedule/shipping round-trip।
- PDF/share action production callback-এ পৌঁছানো ও JVM PDF tests যাচাই; কোনো external share target-এ document পাঠানো হয়নি। Full unrelated instrumentation suite run করা হয়নি।

## Modified source files

- `app/src/androidTest/java/com/pocketdev/advance_erp/ImportPersistenceRegressionTest.kt`
- `app/src/androidTest/java/com/pocketdev/advance_erp/PremiumUiRegressionTest.kt`
- `app/src/androidTest/java/com/pocketdev/advance_erp/PurchaseOrderVisualTest.kt`
- `app/src/main/java/com/pocketdev/advance_erp/core/designsystem/ERPBottomNavigation.kt`
- `app/src/main/java/com/pocketdev/advance_erp/core/designsystem/ERPFullScreenEditor.kt`
- `app/src/main/java/com/pocketdev/advance_erp/core/designsystem/ERPTopBar.kt`
- `app/src/main/java/com/pocketdev/advance_erp/feature/imports/CreatePurchaseOrderDialog.kt`
- `app/src/main/java/com/pocketdev/advance_erp/feature/imports/ImportManagementScreen.kt`
- `app/src/main/java/com/pocketdev/advance_erp/feature/imports/ImportViewModel.kt`
- `app/src/main/java/com/pocketdev/advance_erp/feature/imports/PremiumPurchaseOrderUi.kt`
- `app/src/main/java/com/pocketdev/advance_erp/feature/imports/PurchaseOrderDetailDialog.kt`
- `app/src/main/java/com/pocketdev/advance_erp/navigation/AdvanceErpNavGraph.kt`
- `app/src/main/java/com/pocketdev/advance_erp/navigation/MainAppScaffold.kt`
- `app/src/androidTest/java/com/pocketdev/advance_erp/PurchaseOrderSixReferenceTest.kt`
- `app/src/main/java/com/pocketdev/advance_erp/feature/imports/PoListContent.kt`
- `app/src/main/java/com/pocketdev/advance_erp/feature/imports/PoReferenceComponents.kt`

## Final verified delivery — 2026-11-22

- Final build: **BUILD SUCCESSFUL**; `assembleDebug`, `testDebugUnitTest`, `assembleDebugAndroidTest` সম্পন্ন। [Build log](logs/build.log)।
- Unit tests: **376 passed**, 0 failures/errors/skipped। Device instrumentation: **37/37 passed**, `OK (37 tests)`। [Device log](logs/device-tests.log)।
- Application ID: `com.pocketdev.advance_erp2`; package identity/signing/database schema অপরিবর্তিত।
- ছয়টি Light এবং ছয়টি Dark screenshot actual Android device থেকে ADB screencap দিয়ে নেওয়া। Production Compose components-এ isolated fixture data ব্যবহার করা হয়েছে; live business database-এ fixture লেখা হয়নি।
- Scroll করে সম্পূর্ণ landed-cost highlight: [Light](screenshots/05_PO_Cost_Summary.png), [Dark](screenshots/05_PO_Cost_Summary_dark.png)।
- বাস্তব installed app-এ numeric keyboard পরীক্ষা: [Screenshot](screenshots/keyboard-check.png)। Field, fixed actions ও navigation keyboard-এর ওপরে থাকে; value না বদলে editor বন্ধ করা হয়েছে।
- Dark screenshot-এ system navigation strip/device status chrome reference থেকে আলাদা। Details tab strip horizontal-scrollable; fifth tab সরিয়ে দেখা যায়। Reference-এর তুলনায় header/card proportions ও whitespace এখনও ভিন্ন, তাই pixel-perfect acceptance দাবি করা হচ্ছে না।
- Full external PDF sharing এবং সব unrelated ERP screen-এর end-to-end পরীক্ষা এই run-এর অংশ নয়।

APK SHA-256: `d9b3a08537672464fc5db31146c46f638694cfe0583e0c984ca5093db1130dd0`
