# ADVANCE ERP — E2E Phase 0–3 Bugfix & Phase 4–6 Resume Checkpoint (2026-11-22)

## 1. Status & Context

- **Date**: 2026-11-22 (Asia/Dhaka)
- **App Package**: `com.pocketdev.advance_erp2`
- **Completed**: Real-device End-to-End (E2E) verification through **Phases 0–3**, bug fixing for all discovered Phase 0–3 issues (including the `InventoryViewModel` `flatMapLatest` search-reset race), JVM regression tests, in-place APK upgrade (`adb install -r`), and live-device UI re-verification.
- **Current Pause Point**: Paused right before **Phase 4** at the user's request so the working tree is clean, committed, and pushed to `origin/main` while the user performs emergency work in the **PO module**.
- **Rule for Resuming**: Do **not** restart from Phase 0. Preserve existing live device records unless intentionally altered by the user's PO work, inspect live DB counts first if PO changes touched stock/POs, and resume from **Phase 4**.

---

## 2. Completed & Verified Fixes (27 Modified Files + 1 New Unit Test)

### A. Add Product & Product Catalog
- `app/src/main/java/com/pocketdev/advance_erp/feature/product/AddProductDesignComponents.kt`
- `app/src/main/java/com/pocketdev/advance_erp/feature/product/AddProductDialog.kt`
- `app/src/main/java/com/pocketdev/advance_erp/feature/product/EditProductDialog.kt`
- `app/src/main/java/com/pocketdev/advance_erp/feature/product/PhotoGalleryPicker.kt`
- `app/src/main/java/com/pocketdev/advance_erp/feature/product/ProductListScreen.kt`
- `app/src/main/java/com/pocketdev/advance_erp/data/repository/ProductRepositoryImpl.kt`
- `app/src/androidTest/java/com/pocketdev/advance_erp/AddProductThreeReferenceTest.kt`
- **Summary**:
  - Replaced `ModalBottomSheet` category and warehouse pickers with in-window `AddProductInWindowSheetOverlay` inside `ERPFullScreenEditor` so pickers render visibly above full-screen editor dialogs.
  - Added Step 1 `onNext` validation with inline `isError` borders (`পণ্যের ইংরেজি নাম দেওয়া আবশ্যক`).
  - Fixed `normalizeCategorySlug` triple-dash bug (`Beauty & Care` -> `beauty-care`).
  - Localized Step 3 variant/photo/empty labels in Bangla.
  - Updated `AddProductThreeReferenceTest.kt` to match Step 1 `Next` validation behavior.

### B. Purchase / Import & Sales Localization
- `app/src/main/java/com/pocketdev/advance_erp/data/repository/ImportRepositoryImpl.kt`
- `app/src/main/java/com/pocketdev/advance_erp/feature/imports/ImportManagementScreen.kt`
- `app/src/main/java/com/pocketdev/advance_erp/feature/imports/PurchaseOrderDetailDialog.kt`
- `app/src/main/java/com/pocketdev/advance_erp/feature/imports/CreatePurchaseOrderDialog.kt`
- `app/src/main/java/com/pocketdev/advance_erp/feature/imports/AddCartonDialog.kt`
- `app/src/main/java/com/pocketdev/advance_erp/feature/imports/AddCostItemDialog.kt`
- `app/src/main/java/com/pocketdev/advance_erp/feature/imports/PurchaseCompletionDialog.kt`
- `app/src/main/java/com/pocketdev/advance_erp/feature/imports/RecordSupplierPaymentDialog.kt`
- `app/src/main/java/com/pocketdev/advance_erp/feature/imports/WarehouseReceivingDialog.kt`
- `app/src/main/java/com/pocketdev/advance_erp/feature/sales/CreateInvoiceDialog.kt`
- **Summary**:
  - Included `OTHER_CHARGES` / capitalized costs in PO Detail Total Landed Cost (`PO-2026-001` verified at `৳ 29,105.00`).
  - Persisted Handling/Sample charges as `OTHER_EXPENSES` on PO update.
  - Persisted and displayed Step 4 Freight Rate (`shippingRatePerKgBdt` + `freightRates` + `costItems` fallback; shows `N/A` for local orders).
  - Set `LOCAL` shippingMethod/origin/destination for local BDT batches and filtered `BATCH-` local rows out of the Shipments tab (`চালানসমূহ`).
  - Enforced `Locale.ROOT` on carton/shipment codes (`CTN-01`, `CTN-003`, `SHP-2026-0004`).
  - Fixed fractional decimal truncation in `PurchaseCompletionDialog` using `Formatters.formatMinorUnit`.
  - Passed `isBangla` to `RecordSupplierPaymentDialog` and localized Import and `CreateInvoiceDialog` strings.
  - Audited `CbmCalculator`: no math bug (`CTN-02` had `lengthCm = '8740'` stored from ADB text input).

### C. Inventory & Stock Screen
- `app/src/main/java/com/pocketdev/advance_erp/feature/inventory/InventoryViewModel.kt`
- `app/src/main/java/com/pocketdev/advance_erp/feature/inventory/InventoryScreen.kt`
- `app/src/main/java/com/pocketdev/advance_erp/data/dao/StockBalanceDao.kt`
- `app/src/main/java/com/pocketdev/advance_erp/feature/inventory/StockAdjustmentDialog.kt`
- `app/src/main/java/com/pocketdev/advance_erp/feature/inventory/costhistory/ProductCostHistoryDialog.kt`
- `app/src/main/java/com/pocketdev/advance_erp/feature/inventory/reconciliation/InventoryReconciliationView.kt`
- `app/src/main/java/com/pocketdev/advance_erp/feature/inventory/cogs/CogsTraceabilityView.kt`
- **Summary**:
  - Fixed `InventoryViewModel` `flatMapLatest` search-reset race by moving `_searchQuery`, `_selectedCategoryId`, `_sortOption`, `_stockStatusFilter`, audit flows, and dialog flags outside `flatMapLatest` (now keyed only by `_selectedWarehouseId`) and exposing `viewModel.searchQuery` directly to `InventoryScreen`'s `BasicTextField`.
  - Fixed `CompactStockProductCard` variant pill wrapping (`2 ভেরিয়েন্ট`) and 3-column `CompactMetricPill` alignment.
  - Added `categoryNameBn` to `StockItemOverview` and localized Inventory subviews.

### D. Contacts & Navigation
- `app/src/main/java/com/pocketdev/advance_erp/feature/contacts/ContactsScreen.kt`
- `app/src/main/java/com/pocketdev/advance_erp/feature/contacts/ContactsViewModel.kt`
- `app/src/main/java/com/pocketdev/advance_erp/navigation/AdvanceErpNavGraph.kt`
- **Summary**:
  - Synced Top App Bar title (`সরবরাহকারী` <-> `গ্রাহকবৃন্দ`) on Contacts tab switch via `onSwitchTabRoute`.
  - Fixed `Add Customer` header overlap.
  - Added Supplier Country/Currency presets (`বাংলাদেশ (BDT)`, `চীন (CNY)`, `অন্যান্য (USD)`), inline empty-form validation, and Bangla labels.

### E. Unit Test Added
- `app/src/test/java/com/pocketdev/advance_erp/E2eBugFixesRegressionTest.kt` (category slug normalization, `Locale.ROOT` carton/shipment formatting under Bangla locale, fractional minor-unit formatting).

---

## 3. Exact Build & Verification Evidence

- **Formatting check**: `git diff --check` passed.
- **Native `adev` build & JVM tests**:
  `:app:assembleDebug :app:testDebugUnitTest` -> **BUILD SUCCESSFUL**, **384/384** JVM tests passed across **72** files (`0` failures, `0` errors, `0` skipped).
- **Debug APK**:
  - Path: `app/build/outputs/apk/debug/app-debug.apk`
  - SHA-256: `eeb7169fc6a885476778a17fed315af6c0db9c8f4472a530458d65fd8d1f2303`
  - Installed in-place on live device (`localhost:36429`) via `adb install -r` (`Success`) without uninstalling or clearing user data.
- **Instrumentation Limitation**:
  - Direct `am instrument` execution on the live device was blocked by `SecurityException` because the pre-existing `com.pocketdev.advance_erp2.test` package on the device carries an older signing key (`[51806981]` vs `[f7267c35]`), intentionally left uninstalled per `AGENTS.md`. Do **not** claim Android instrumentation tests executed on device for these fixes.
- **Verification Screenshots (`docs/e2e-verification/2026-11-22/screenshots/`)**:
  - Phase 0–3 baseline captures: `01_dashboard_initial.png` through `09_inventory_valuation.png`.
  - Post-fix live UI verifications:
    - `fix_01_inventory_cards.png` — compact stock cards & variant pill layout
    - `fix_01_inventory_search_pass.png` — live `BCC-FBU` search filtering cleanly to `ফাস্ট ইউএসবি-সি কেবল` after the `InventoryViewModel` fix (`fix_01b_inventory_filtered.png` and `fix_01c_inventory_search_live.png` retained as earlier intermediate captures)
    - `fix_02_add_supplier_currency_validation.png` — supplier country/currency presets & inline empty-form validation
    - `fix_02b_add_customer_header.png` — Add Customer header spacing fix
    - `fix_02c_contacts_customer_title_sync.png` — Top App Bar title sync (`গ্রাহকবৃন্দ`)
    - `fix_03_add_product_category_sheet.png` — in-window Add Product category picker sheet
    - `fix_04_po1_landed_cost.png` — `PO-2026-001` Total Landed Cost `৳ 29,105.00`
    - `fix_04b_po1_cost_rate.png` — `PO-2026-001` Cost tab showing `12.000 KG × ৳850.00/KG` (`৳ 10200.00 BDT`)
    - `fix_05_shipments_filtered.png` — Shipments tab hiding local `BATCH-` shipments

---

## 4. Live Device Database State & Safety Rules

- **Strict Safety Rules**:
  - **NEVER** uninstall `com.pocketdev.advance_erp2` or clear app data.
  - **NEVER** delete pre-existing user records or E2E sample records.
  - **NEVER** run destructive backup restores against the live database.
- **Pre-existing User Products Intact**:
  - `EVA 3.5CM HIGHT INSOLE` (`BCC-E35-005`, `0 PAIR`)
  - `HEIGHT INSOLE PU 3.5 CM` (`BCC-HIP-006`, `0 PAIR`)
- **Saved Phase 0–3 Sample Data Intact**:
  - **Suppliers (3)**: `Yiwu Glow Cosmetics Ltd.`, `Shenzhen TechPulse Co.`, `Dhaka Loom Textiles`
  - **Customers (3)**: `Rahman Mart`, `Sadia Beauty Shop`, `Metro Fashion House`
  - **Categories (3)**: `Beauty & Care`, `Electronics`, `Fashion Apparel`
  - **Products & Variants (3 / 6)**:
    - `Hydra Ice Eye Mask` (`BCC-HIE-001`): `Blue/Std` (70), `Pink/Std` (45)
    - `Fast Braided USB-C Cable` (`BCC-FBU-002`): `Black/1M` (65), `White/2M` (50)
    - `Premium Cotton Polo` (`BCC-PCP-003`): `Navy/M` (36), `Maroon/L` (35)
  - **Purchase Orders (3)**:
    - `PO-2026-001` (CNY Import, `COMPLETED`, landed cost `৳ 29,105.00`)
    - `PO-2026-002` (USD Import, `RECEIVED`)
    - `PO-2026-003` (BDT Local, `RECEIVED`)
  - **Inventory & Sales State at Checkpoint**:
    - Inventory: `301 PCS`, valuation `৳ 96,375.10`, 5-way reconciliation matched
    - Sales: `0` invoices (`কোন বিক্রয় চালান পাওয়া যায়নি`)
- **Note for Resume**: Because the user is about to work urgently in the **PO module**, re-query live DB stock/PO/balance counts before running Phase 4–6 numeric assertions.

---

## 5. Remaining E2E Scope to Resume (Phases 4–6)

1. **Phase 4 — Sales, POS, Delivery, Customer Ledger, Due Collection & Returns**:
   - Inspect live DB `customers`, `products`, `product_variants`, and `chart_of_accounts` IDs plus `CreateInvoiceDialog` / `PosDialog` controls first.
   - **Negative checks**: over-stock qty (`999`) blocking/error & empty/0-qty submit blocking in `CreateInvoiceDialog`.
   - **Sale #1 (`Rahman Mart`, Regular Invoice)**: `Blue/Std` x10 @350 + `Black/1M` x10 @260 = `৳6,100`, Cash Paid `৳3,000` -> `নিশ্চিত বিক্রয়`; verify `InvoiceDetailDialog` `ডেলিভারি চালান` (`DispatchDeliveryDialog`), `PDF`, and `রশিদ (Thermal)`.
   - **Customer Ledger & Due Collection (`Rahman Mart`)**: open ledger/collection, snap `10_customer_ledger.png`, record `৳1,500` via bKash (`coa-1060`), verify remaining due `৳1,600` in UI & DB.
   - **Sale #2 (`Sadia Beauty Shop`, POS Quick Sale)**: `দ্রুত বিক্রয়` (`PosDialog`), `Pink/Std` x5 @360 + `Navy/M` x4 @750 = `৳4,800`, Full Payment, snap `11_pos_sale.png`.
   - **Sale #3 (`Metro Fashion House`, Regular Invoice + Sales Return)**: `Maroon/L` x6 @780 + `White/2M` x5 @320 = `৳6,280`, Cash Paid `৳2,000`; open `InvoiceDetailDialog` -> `ফেরত` (`ProcessSalesReturnDialog`), return `2` x `Maroon/L` (`Defective`), verify `+2` stock restored & due reduced (`৳2,720`).
   - Verify Sales search & filter chips (`সব`, `খসড়া`, `অনুমোদিত (Posted)`, `আংশিক পরিশোধ`), snap `12_sales_list.png`, verify `sales_invoices`, `sales_returns`, `customer_payments`, `stock_balances`, `customers` in DB.

2. **Phase 5 — Accounts, Control Center / Reports, Exports, Dashboard, Settings & Cold Restart**:
   - **Accounts (`হিসাব ও লেজার`)**: Cash/Bank (`13_accounts_cash_bank.png`), Expense Voucher (`৳5,000`, Office Rent & Utilities), Account Transfer (`৳2,000`), Financial Statements (`14_financial_statements.png`), Trial Balance, COA, Journals, Reconciliation.
   - **Control Center / Reports (`খতিয়ান ও রিপোর্ট`)**: date/category/search filters, Party Ledger / Due views, `রিপোর্ট / এক্সপোর্ট`, trigger PDF & XLSX exports, snap `15_reports_control_center.png`, verify generated files via `adb exec-out run-as com.pocketdev.advance_erp2 ...` and `/sdcard/Download/`.
   - **Dashboard (`হোম`)**: KPI cards, Quick Actions FAB, date filter -> snap `16_dashboard_populated.png`.
   - **Settings (`সেটিংস`)**: Business Profile, Currency, Backup UI (read-only only — **NEVER** restore), Dark Mode toggle -> snap `17_dark_mode_check.png` -> back to Light Mode; top-bar `BN` -> `EN` -> back to `BN`.
   - **Cold Restart Persistence**: `adb shell am force-stop com.pocketdev.advance_erp2` -> relaunch -> snap `18_after_restart_dashboard.png`.

3. **Phase 6 — Final WAL SQLite Audit, Logcat Audit & Deliverables**:
   - Run read-only WAL SQLite audit verifying all saved IDs, global & per-journal `SUM(debitMinor) == SUM(creditMinor)`, `stock_balances` vs `SUM(stock_movements)` vs `SUM(inventory_cost_layers.remainingQuantity)`, customer/supplier dues, revenue/COGS/expenses/net profit, and exported PDF/XLSX files.
   - Save filtered Logcat (`com.pocketdev.advance_erp2` / `FATAL` / `AndroidRuntime` / `Exception`) to `docs/e2e-verification/2026-11-22/logcat-audit.txt`.
   - Write full PASS/FAIL report at `docs/e2e-verification/2026-11-22/e2e-test-report.md` (with module-wise results, screenshots, severity-ranked bug list separating Phase 0–3 fixed bugs from any new Phase 4–5 bugs, missing features/recommendations, saved IDs, and tested/failed/untested summary).

---

## 6. Resume Instructions

- **Environment notes**:
  - Check ADB device connection first with `adb devices` (wireless debugging port changes across reconnects; last used was `localhost:36429`).
  - Temporary ADB UI helper script (`/data/user/0/ai.antigem/files/usr/tmp/e2e_ui.py`) resides in `$TMPDIR` and is **not** committed to Git; recreate or inspect it before writing new UI scripts.
  - Zero app source code edits during Phases 4–6 unless specifically approved; report any new Phase 4–5 bugs or missing features first.
- **Suggested Bengali prompt to resume**:

> `docs/e2e-verification/2026-11-22/checkpoint.md` এবং `WORK_IN_PROGRESS.md` পড়ো। Phase 0–3 ইতোমধ্যে সম্পন্ন ও ভেরিফাইড (PO মডিউলে নতুন কোনো পরিবর্তন বা ডেটা থাকলে আগে live DB ও git diff দেখে নাও)। এবার সরাসরি live device-এ E2E Phase 4, Phase 5 এবং Phase 6 সম্পন্ন করে চূড়ান্ত রিপোর্ট দাও।
