# ADVANCE ERP — PO Workflow, Smart Packing, Shipping Mark, Supplier PDF & 4-Sheet Excel Implementation Report

- **Date:** 2026-11-22
- **Application ID:** `com.pocketdev.advance_erp2`
- **Database Compatibility:** Room v13 preserved cleanly (no destructive migrations; existing `PO-2026-001`..`PO-2026-004` preserved untouched)
- **Debug APK SHA-256:** `8dad73803ee84dee0f2e98123d0a1661851860e917a4d0a3278c6524d10f91cd`
- **Test Device:** `CPH2745` (`localhost:45453`, `1272x2772`)

---

## 1. Implemented Architecture & Modified Files

### New Core Engines & Pickers
- [PoPackingEngine.kt](file:///data/user/0/ai.antigem/files/home/ADVANCE-ERP-2/app/src/main/java/com/pocketdev/advance_erp/core/engine/PoPackingEngine.kt) — Pure-Kotlin engine providing `VariantKey(productId, variantId)`, immutable `cartonUid`, `formatCartonNumber(seq)` (`CTN-001` format), continuous renumbering, `resolveShippingMark` (`{CTN}` placeholder), live `summarize`, per-carton `maxAllowedForCarton`, `VariantVisibility` filtering (`AVAILABLE`, `ALL`, `COMPLETED`), Option A `checkOrderQuantityChange`, `maxIdenticalCartons`, Draft vs Confirm blockers.
- [PoExportDataBuilder.kt](file:///data/user/0/ai.antigem/files/home/ADVANCE-ERP-2/app/src/main/java/com/pocketdev/advance_erp/core/util/PoExportDataBuilder.kt) — Single-source-of-truth supplier export builder & cross-format validator shared across Supplier PDF, 4-sheet Supplier XLSX, CTN Labels PDF, and `PoShareDialog`, supporting full & `selectedCartonNumbers` partial export subsets.
- [PoProductVariantPicker.kt](file:///data/user/0/ai.antigem/files/home/ADVANCE-ERP-2/app/src/main/java/com/pocketdev/advance_erp/feature/imports/PoProductVariantPicker.kt) — Searchable grouped Product/Variant picker (`PoCatalogProductVariantPickerSheet`), Carton Variant picker with `Available / All / Completed` filters (`PoCartonVariantPickerSheet`), and quantity-capped `BulkCreateCartonsDialog`.
- [PoSmartPackingAndExportTest.kt](file:///data/user/0/ai.antigem/files/home/ADVANCE-ERP-2/app/src/test/java/com/pocketdev/advance_erp/PoSmartPackingAndExportTest.kt) — Unit test suite covering Scenarios 2–13, Option A guards, Shipping Mark resolution, PDF/Excel reconciliation, and `CTN-001` renumbering.

### Updated Application & Export Files
- [CreatePurchaseOrderDialog.kt](file:///data/user/0/ai.antigem/files/home/ADVANCE-ERP-2/app/src/main/java/com/pocketdev/advance_erp/feature/imports/CreatePurchaseOrderDialog.kt) — Searchable Product/Variant selection, Option A PO quantity reduction guard, live Step 2 Packing Progress card, Default & Individual Shipping Marks, Fast Carton Entry (`Auto-Pack Left`, `Bulk Create`, `Add Carton`, duplicate guard), continuous renumbering on delete/reorder, incomplete Draft warning vs Confirm blocker, and positive weight fallback fix in `Auto-Pack Left` (`> 0` check before `"15.0"` fallback).
- [AddCartonDialog.kt](file:///data/user/0/ai.antigem/files/home/ADVANCE-ERP-2/app/src/main/java/com/pocketdev/advance_erp/feature/imports/AddCartonDialog.kt) — Integrated searchable variant selection & strict remaining-quantity validation.
- [PurchaseOrderDetailDialog.kt](file:///data/user/0/ai.antigem/files/home/ADVANCE-ERP-2/app/src/main/java/com/pocketdev/advance_erp/feature/imports/PurchaseOrderDetailDialog.kt) — Grouped `GroupedCartonCard` view with `MIXED CARTON` / `SINGLE PRODUCT` badges, resolved company Shipping Marks (`businessProfile?.companyName`), weight/dimension/CBM breakdown, and CTN Label & Export entry point.
- [ImportManagementScreen.kt](file:///data/user/0/ai.antigem/files/home/ADVANCE-ERP-2/app/src/main/java/com/pocketdev/advance_erp/feature/imports/ImportManagementScreen.kt) & [ImportViewModel.kt](file:///data/user/0/ai.antigem/files/home/ADVANCE-ERP-2/app/src/main/java/com/pocketdev/advance_erp/feature/imports/ImportViewModel.kt) — Confirmation packing enforcement (`confirmPurchaseOrder`) & `businessProfile` propagation.
- [PoNotesMetadataParser.kt](file:///data/user/0/ai.antigem/files/home/ADVANCE-ERP-2/app/src/main/java/com/pocketdev/advance_erp/core/util/PoNotesMetadataParser.kt) — Pure-Kotlin + `java.util.Base64` serialization & `remapCartonShippingMarks` for `[ShippingMark: ...]` and `[CartonMarks: ...]`.
- [ImportRepository.kt](file:///data/user/0/ai.antigem/files/home/ADVANCE-ERP-2/app/src/main/java/com/pocketdev/advance_erp/domain/repository/ImportRepository.kt), [ImportShipmentDao.kt](file:///data/user/0/ai.antigem/files/home/ADVANCE-ERP-2/app/src/main/java/com/pocketdev/advance_erp/data/dao/ImportShipmentDao.kt), & [ImportRepositoryImpl.kt](file:///data/user/0/ai.antigem/files/home/ADVANCE-ERP-2/app/src/main/java/com/pocketdev/advance_erp/data/repository/ImportRepositoryImpl.kt) — Continuous `CTN-001` renumbering (`renumberOrderCartonsContinuously`), Shipping Mark remapping on delete, and Option A repository guard in `updatePurchaseOrderItem`.
- [ImportPdfGenerator.kt](file:///data/user/0/ai.antigem/files/home/ADVANCE-ERP-2/app/src/main/java/com/pocketdev/advance_erp/core/util/ImportPdfGenerator.kt) — Approved-reference Supplier PO PDF layout: crisp white `logo_monochrome_white` header banner, side-by-side Supplier & Order Summary cards, 9-column Product table with actual `CTN-001` references + photos + pagination headers, full unwrapped 6-column Carton Packing Breakdown table (`selectedCartonNumbers` subtotal + PO reference total), Delivery Info, Payment Terms, Notes & Signatures.
- [ImportExcelGenerator.kt](file:///data/user/0/ai.antigem/files/home/ADVANCE-ERP-2/app/src/main/java/com/pocketdev/advance_erp/core/util/ImportExcelGenerator.kt) — 4-sheet OOXML `.xlsx` workbook (`Purchase Order`, `Packing List (Carton Wise)`, `Carton Summary`, `Shipping Mark & Delivery`), embedded product images (`xl/media/`), currency/weight/CBM formatting, live Excel formulas (`=H*I`, `=SUM(...)` avoiding mixed-carton CBM double-counting), frozen panes, AutoFilters & print setup.
- [LabelPrintingEngine.kt](file:///data/user/0/ai.antigem/files/home/ADVANCE-ERP-2/app/src/main/java/com/pocketdev/advance_erp/core/util/LabelPrintingEngine.kt) — Dedicated `generatePoCartonLabelsPdf` with clean product table, totals bar, and separated Shipping Mark section.
- [PoShareDialog.kt](file:///data/user/0/ai.antigem/files/home/ADVANCE-ERP-2/app/src/main/java/com/pocketdev/advance_erp/feature/imports/PoShareDialog.kt) — Cross-format blocker/warning banners, All vs Selected Cartons export scope, live selected summary, In-App `PdfRenderer` preview (`InAppPdfPreviewDialog`), PDF/XLSX share/save/print & CTN Label workflows.
- [SupplierPoExportTest.kt](file:///data/user/0/ai.antigem/files/home/ADVANCE-ERP-2/app/src/test/java/com/pocketdev/advance_erp/SupplierPoExportTest.kt) — Updated for the 4-sheet Supplier Excel workbook and verified passing.

---

## 2. Build & Automated JVM Test Results (Post-Final Edit)
- `git diff --check`: Clean (0 trailing/whitespace errors).
- `adev` Gradle 8.13 `:app:assembleDebug :app:testDebugUnitTest --console=plain`: **BUILD SUCCESSFUL in 4m 5s**
- **Unit Test Tally:** `73` test files, **`389` tests passed**, `0` failures, `0` errors, `0` skipped.

---

## 3. Real-Device ADB Verification (`localhost:45453`, Mandatory Tests 1–14)

| Test | Scenario Verified on Device | Screenshot Evidence |
| --- | --- | --- |
| Test 1 | Searchable Product & Variant Picker (`4D CLOUD`, multi-select `BLACK/35-36` & `BLACK/37-38`, qty `200`, `2.10 CNY`) | [01](docs/ui-refinement/2026-11-22/po-smart-packing-export/screenshots/01_searchable_product_variant_picker.png), [02](docs/ui-refinement/2026-11-22/po-smart-packing-export/screenshots/02_po_items_selected.png) |
| Test 2 | Available/All/Completed Carton Picker & Strict Overpacking Rejection (`300 > 200` rejected; `120` PCS leaves `80` PCS) | [03](docs/ui-refinement/2026-11-22/po-smart-packing-export/screenshots/03_carton_variant_picker_available.png), [04](docs/ui-refinement/2026-11-22/po-smart-packing-export/screenshots/04_overpacking_rejected.png) |
| Test 3 & 4 | Partial `120 + 80` PCS removes `35-36` from `বাকি (1)` into `সম্পন্ন (1)`; deleting `CTN-002` returns `80` PCS | [05](docs/ui-refinement/2026-11-22/po-smart-packing-export/screenshots/05_partial_and_completed_filter.png) |
| Test 5 | Option A blocks reducing ordered qty from `200` to `150` while `200 PCS` allocated | [06](docs/ui-refinement/2026-11-22/po-smart-packing-export/screenshots/06_option_a_qty_reduction_blocked.png) |
| Test 6 | Incomplete packing (`200 PCS` remaining) blocks Confirm & allows Save Draft with warning | [07](docs/ui-refinement/2026-11-22/po-smart-packing-export/screenshots/07_incomplete_packing_confirm_blocked.png) |
| Test 7 | Reopen `PO-2026-005` Draft, pack remaining `200 PCS` -> `5 CTN`, `400/400`, `100%` | [08](docs/ui-refinement/2026-11-22/po-smart-packing-export/screenshots/08_packing_progress_100_percent.png) |
| Test 8 | Delete `CTN-003` -> continuous auto-renumbering `CTN-001..CTN-004` (`4 CTN`, `340/400`, `85%`) | [09](docs/ui-refinement/2026-11-22/po-smart-packing-export/screenshots/09_continuous_carton_renumbering.png) |
| Test 9 | Default Mark (`TUKTAK BD / UPDATED / {CTN}`) updates non-overridden cartons while `CTN-002` preserves `CUSTOM-CTN-002 / FRAGILE`; `Auto-Pack Left` allocates remaining `60 PCS` at `15.0 KG` and confirms `PO-2026-005` | [10](docs/ui-refinement/2026-11-22/po-smart-packing-export/screenshots/10_shipping_mark_default_and_override.png) |
| Tests 10–12 | Read-only export on live `PO-2026-004`: Share dialog, 2-page Supplier PDF (white logo, actual `CTN-001..005`, unwrapped `CTN-002`), CTN Labels PDF, 4-sheet `.xlsx` with 14 images & live formulas | [11](docs/ui-refinement/2026-11-22/po-smart-packing-export/screenshots/11_po_share_export_dialog.png), [12](docs/ui-refinement/2026-11-22/po-smart-packing-export/screenshots/12_supplier_pdf_preview.png), [13](docs/ui-refinement/2026-11-22/po-smart-packing-export/screenshots/13_ctn_labels_preview.png) |
| Test 13 | Selected Cartons (`CTN-001`, `CTN-002` only) export generates `Supplier_PO-2026-004_Selected_2CTN.pdf` & `.xlsx` reconciling `2 CTN / 680 pcs / 45.0 KG` with `Complete PO Reference Total` (`5 CTN / 1,860 pcs / 168.0 KG`) | [14](docs/ui-refinement/2026-11-22/po-smart-packing-export/screenshots/14_selected_cartons_partial_export.png) |
| Test 14 | `am force-stop` cold restart persistence on `PO-2026-005` (`CONFIRMED`, `CTN-001..CTN-005`, `400 pcs`, `75.0 KG`, preserved default + custom marks) + SQLite & Logcat (`0` crashes) | [15](docs/ui-refinement/2026-11-22/po-smart-packing-export/screenshots/15_after_restart_po_packing_persisted.png) |

---

## 4. Defect Fixed & Notes Observed During Verification
1. **PDF Header Logo & Multi-Variant Wrapping (`ImportPdfGenerator.kt`)**: Replaced dark-purple-`A` `logo_compact_white` with `logo_monochrome_white` so `ADVANCE ERP` is crisp white on the dark navy banner, and increased carton breakdown wrapping lines (`maxOf(5, ctn.lines.size + 1)`) so 6-variant `CTN-002` renders completely (`× 20 pcs`) without truncation.
2. **UI vs Export Company Shipping Mark Consistency (`PurchaseOrderDetailDialog.kt`, `ImportManagementScreen.kt`)**: Passed `uiState.businessProfile` into `PurchaseOrderDetailDialog` so `GroupedCartonCard` displays `TUKTAK BD / PO-2026-004 / CTG PORT` consistently with PDF, Excel, and CTN Labels.
3. **`Auto-Pack Left` Zero-Weight Fallback on Reopened Draft PO (`CreatePurchaseOrderDialog.kt`)**: Fixed `Auto-Pack Left` checking `.ifBlank` when unpacked Room PO items carried `"0"` kg (`> BigDecimal.ZERO` check now falls back to `"15.0"` KG) and added carton-numbered weight guard before `CartonWeightAllocationEngine.allocateWeights`.
4. **Live DB State Note**: Existing user orders `PO-2026-001`..`PO-2026-004` were untouched and verified intact via SQLite. QA order `PO-2026-005` (`CONFIRMED`, `400 pcs`, `5 CTN`, `75 KG`, `0 CBM` since `CTN মাপ` remained blank during the automated Test 9 ADB run — carton CBM math was verified in JVM tests) remains recorded in the database from Tests 1–9 & 14.
