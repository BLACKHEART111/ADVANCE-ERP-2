# ADVANCE ERP — Premium UI implementation checkpoint

## Comprehensive Backup & Restore Audit Fixes (v2 Archive Format) — Verified Build & Unit Suite (2026-11-27)

Upgraded offline ZIP backup and staged startup restore to cover 100% of app modules, media directories, branding, portable preferences, and report filter presets while keeping Room Schema v15 (`15.json`, `62` entities) untouched:
- **Pure-JVM Backup Contract & Atomic Snapshot (`BackupArchiveContract.kt`, `BackupRepositoryImpl.kt`)**:
  - Added `BackupArchiveContract` (`BACKUP_FORMAT_VERSION = 2`) with pure-Kotlin RFC-8259 JSON encoding/decoding for `PortableBackupPreferences`, safe ZIP entry allowlisting, and path extraction/anchoring/rewriting (single, any-subdir, root prefix, and CSV attachment lists).
  - Updated `BackupRepositoryImpl.createBackup` to capture a consistent SQLite snapshot via `VACUUM INTO` (with `PRAGMA wal_checkpoint(FULL)` + file copy fallback on older SQLite), bundle all 6 managed media directories (`product_images`, `po_attachments`, `vault_attachments`, `customer_photos`, `company_logo`, `company_signature`) + root `avatars/`, serialize portable DataStore settings (`preferences.json`) and Control Center saved presets (`control_center_presets.json`), and emit enriched v2 `metadata.json`.
- **Staged Restore Hardening & Startup Preference Apply (`StagedRestore.kt`, `UserPreferencesRepository.kt`, `AdvanceErpApplication.kt`)**:
  - Updated `StagedRestore` to validate and restore all v2 media directories (preserving v2-only dirs when restoring older v1 backups), rewrite cross-package `/files/...` paths across `product_images`, `import_vault_attachments`, `purchase_orders.attachments`, `customers.profileImagePath`, `business_profiles.logoPath`, `import_cost_items.attachmentPath`, and `expense_vouchers.attachmentPath`, restore `control_center_presets`, clear stale `editor_drafts_v1` (with full rollback on interrupted restore), and defer portable DataStore apply (`applyPendingPreferences`) safely to `AdvanceErpApplication.onCreate` without touching App Lock PIN/biometric secrets or device-bound SAF URIs.
- **Supporting Resilience Fixes (`MainAppScaffold.kt`, `ProductImageStorage.kt`, `BackupManagementSection.kt`)**:
  - Synced top-bar language toggle through `SettingsRepository.updateLanguage`, added same-dir fast-path + stream copy fallback in `ProductImageStorage.saveImageFromUri` to avoid storing raw `content://` URIs on codec errors, and updated backup UI copy + missing-file guard toast on historical Share/Restore actions.
- **Verification (Run After Final Code Edits)**:
  - `git diff --check`: clean (`0` issues).
  - `adev` Debug Build & Unit Suite (`:app:assembleDebug :app:testDebugUnitTest --console=plain --max-workers=1`): **BUILD SUCCESSFUL in 10m 3s**, **`87` suites / `485` tests passed (`0` failures, `0` errors, `0` skipped)**, including `BackupAndRestoreValidationTest` (`7/7`), `PlainBackupExportTest` (`4/4`), `SecurityAndBackupTest` (`5/5`), `ImportVaultPhase4Test` (`6/6`), `SettingsLogicTest` (`6/6`), and `DatabaseMigrationVerificationTest` (`5/5`).
  - `adev` QA AndroidTest Compilation (`-PcommunicationQa=true :app:compileCommunicationqaAndroidTestKotlin --console=plain --max-workers=1`): **BUILD SUCCESSFUL in 6m 11s** (updated `SmartCommunicationQaAndroidTest.stagedRestoreRehearsalCrossPackageRewriteRejectsCorruptAndRollsBackInterruptedRestore`).
  - **Live In-Place Update & Scope (`com.pocketdev.advance_erp2`)**: Updated strictly in-place via `adb install -r app/build/outputs/apk/debug/app-debug.apk` (APK SHA-256 `103cf57856c188e4612ccbb09cbc658c90d224f7b32018a3896cdc3f8d84e422`) on `emulator-5554` (`CPH2745`); `dumpsys package` confirmed preserved `firstInstallTime=2026-11-22 02:39:36`, `lastUpdateTime=2026-11-27 02:37:32`, and matching signing key `[f7267c35]`. Read-only cold launch (`MainActivity`, PID `15679`, `TotalTime=695ms`) passed with zero `AndroidRuntime`/`Room`/`SQLite`/`StagedRestore` errors in logcat. Per user instruction, no on-device restore test was executed against live or QA packages.

## Smart WhatsApp Communication — Verified Implementation, QA & Live In-Place Update (2026-11-27)

Completed, hardened, device-tested (`com.pocketdev.advance_erp2.communicationqa`), rebuilt, and updated in-place on `com.pocketdev.advance_erp2`:
- **Database Schema v15 & Accounting Corrections**:
  - Verified Room `v14 -> v15` (`MIGRATION_14_15`, `identityHash = f2fae23d8e89b2567becffcf6879f53d`, 62/62 entities matching `15.json`) via KSP and SQLite `DatabaseMigrationVerificationTest`.
  - Fixed return + payment allocation/void invoice due recalculation (`PaymentAllocationValidation.kt`, `SalesRepositoryImpl.kt`), opening/advance credit handling, and immutable posting-time snapshots (`CommunicationEngine.kt`).
  - Wired single `CommunicationDialog` host and fixed named `onSuccess`/`onError` callback handling in `SalesScreen.kt` (`#15`, `#16`), plus Contacts/Imports/Control Center/Settings entrypoints while keeping accepted `ContactsHeroHeader`, `PremiumSettingsHeader`, and bottom navigation intact.
- **Staged Backup/Restore Hardening (`StagedRestore.kt`, `BackupRepositoryImpl.kt`, `SettingsScreen.kt`, `MainAppScaffold.kt`)**:
  - Cross-package backup staging/validation, corrupt ZIP rejection, rollback recovery on interrupted restore, and restart/outcome dialog integration tested in isolated QA instrumentation (`SmartCommunicationQaAndroidTest`).
- **Real-Device Bugs Caught & Resolved During QA**:
  - `DocumentNumberService.kt`: Fixed SQLite connection-pool deadlock by running `runDbQuery` directly when `database.inTransaction()` is active, replaced non-thread-safe `SimpleDateFormat` field with `Calendar`, and wrapped cursor in `.use`.
  - `CommunicationEngine.kt`: Pre-fetched supplier ledger statements outside `db.withTransaction` and replaced `Long.MAX_VALUE` date bound with safe `allTimeUpper`.
  - Android ICU regex brace escaping in `DynamicTemplateEngine.kt`, `CommunicationRepositoryImpl.kt`, and `CommunicationDialog.kt`.
  - `CommunicationDialog.kt`: Added lazy loading of `viewModel.selectModule(state.module)` when switching to the `documents` tab from an `initialRequest != null` flow.
  - `ContactsScreen.kt` & `SettingsScreen.kt`: Placed `CommunicationLauncher` below the purple gradient `ContactsHeroHeader` / `PremiumSettingsHeader` so top headers stay flush.
- **Final Verification (All Run After Final Main-Source Edits)**:
  - `git diff --check`: clean (`0` issues).
  - JVM Build & Unit Suite (`:app:assembleDebug :app:testDebugUnitTest --max-workers=1`): **BUILD SUCCESSFUL in 7m 57s**, **`87` suites / `482` tests passed (`0` failures, `0` errors, `0` skipped)**, including `DatabaseMigrationVerificationTest`, `SettingsLogicTest`, and `SmartCommunicationUnitTest`.
  - QA Android Instrumentation (`-PcommunicationQa=true :app:assembleCommunicationqa :app:assembleCommunicationqaAndroidTest --max-workers=1` on `com.pocketdev.advance_erp2.communicationqa`): **`OK (10 tests)` in 22.564s (`0` failures)** — `SmartCommunicationQaAndroidTest` (`4/4`), `LedgerRepositoryRegressionTest` (`5/5`), `SmartCommunicationVisualTest` (`1/1` pure `PixelCopy` window capture across Contacts, Communication Preview/Documents/Settings/Templates/History, and Sales + 4 rendered PDF PNGs visually inspected).
  - Live In-Place Update (`com.pocketdev.advance_erp2`): `apksigner verify --print-certs app/build/outputs/apk/debug/app-debug.apk` SHA-256 `70a28cffe1b6c502cd5847dc8c3afed59d931a4ecc975a747fd81d82fbfa8d43` matched live app; `adb install -r` preserved `firstInstallTime=2026-11-22 02:39:36` (`lastUpdateTime=2026-11-27 01:29:40`); read-only launch (`MainActivity`, PID `26968`) produced zero `AndroidRuntime`/`Room`/`SQLite` errors in `com.pocketdev.advance_erp2`.
- **Environment Scope / Limitations**:
  - Test device has WhatsApp Business (`com.whatsapp.w4b`) installed and does not have `com.whatsapp` (Messenger) installed; real customers were never messaged, and external WhatsApp caption stripping remains guarded by in-dialog copy/paste fallback instructions. No live production data restore rehearsal was executed against `com.pocketdev.advance_erp2`.

## Smart WhatsApp Communication — previous handoff checkpoint (2026-11-26)

**User requested implementation stop, checkpoint, commit and push so another developer can continue. This is unfinished source, not a verified release.**

- **Read [full implementation handoff](docs/communication/HANDOFF.md) before resuming.** It lists implemented files/behavior, accepted decisions, known defects, restore/accounting risks, and 32 concrete completion/verification items.
- Added centralized communication adapters/templates/secure native sharing, Compose workspace and real PDF preview, settings/history, Room v15 additive migration and schema, invoice/exchange snapshots, module entrypoints, and separate `communicationqa` build type.
- Also started staged backup restore/startup integration and accounting transaction/snapshot changes. **These need isolated QA verification before production install.**
- One intermediate `:app:compileDebugKotlin` passed (3m50s; [log](docs/communication/build.log)); edits continued afterward. **Final checkpoint tree NOT rebuilt. No feature unit/device tests, new APK install, screenshots, WhatsApp compatibility verification or restore tests completed.**
- Important unresolved defect: later payment allocation/void recomputes invoice due from original total minus paid and can ignore credit returns. `previewBeforeSharing` currently stores a preference without changing behavior. Other gaps are explicit in the handoff.
- User decisions: show historical + current balances separately on resend; do not fabricate missing historical snapshots; test on separate QA app `com.pocketdev.advance_erp2.communicationqa`. Preserve production `com.pocketdev.advance_erp2`, live data and signing; never uninstall, clear, seed or restore live app for tests.
- Existing dirty Contacts/Import Vault/navigation work predates this task and is preserved in the checkpoint. Latest compile process finished; do not poll its old session.


## Import Vault Spec Cross-Check Audit Fixes & Build/Install Verification — 2026-11-26

Resolved all 6 functional and UI gaps identified against `ADVANCE_ERP_Import_Vault_Master_Spec_V2.md` while keeping Room `v14` (`14.json`), `MIGRATION_13_14`, and `ERPBottomNavigation.kt` intact:
- **Cohort vs Activity Date Separation, Calendar Pickers & Opening/Closing Stock (`ImportVaultModels.kt`, `ImportVaultAnalyticsEngine.kt`, `ImportVaultUiLogic.kt`, `ImportVaultComponents.kt`, `ImportVaultHomeScreen.kt`, `ImportVaultViewModel.kt`, `ImportVaultExportBuilder.kt`)**: Added `VaultDateFilterTarget` (`ACTIVITY_DATES` vs `IMPORT_COHORT_DATES`), `TODAY` and `CUSTOM_RANGE` presets, Android `DatePickerDialog` pickers in `VaultFilterDialog`, separate period activity net profit vs cumulative `lifetimeNetProfitMinor`/`lifetimeRoiPercent`, and period `openingStockCostMinor`/`closingStockCostMinor`.
- **Shared Overhead Opt-In Rule UI & Uniform UOM Enforcement (`ImportVaultAnalyticsEngine.kt`, `ImportVaultCapitalScreen.kt`, `ImportVaultViewModel.kt`)**: Enforced single-UOM validation for `QUANTITY` shared overhead allocation (rejecting mixed units such as `PCS` + `PAIR`), and added `CreateOverheadRuleDialog` plus active/deactivate toggles on `SharedOverheadPolicySection`.
- **ROI %, Velocity, Time-Series Trend, Table Fallback & Complete Folder Cross-Links (`ImportVaultProfitScreen.kt`, `ImportVaultProductDrillDownScreen.kt`, `ImportVaultDetailScreen.kt`, `ImportVaultRootScreen.kt`)**: Added `roiPercent`, `activeAgeDays`, and `sellingVelocityPerDay` on products/variants with 10 `ProfitSortOption` rankings; added `SalesProfitTrendChartCard`, accessible `Table Fallback` views on charts, tap-to-drill-down `VaultSourceTransactionsDrillDialog`, portfolio `Saved Portfolio Snapshots & Files` management on Vault Home, and wired live `costLayers`/`saleSources`/`supplierPaid`/`supplierPayable`/`additionalCosts`/`damageRecords`/`shortageRecords`/`importCosts`/`landedCostSnapshots` across Capital & Detail screens.
- **Verification & In-Place Install (`ImportVaultAuditFixesTest.kt`)**:
  - `git diff --check` clean; `:app:assembleDebug :app:testDebugUnitTest --console=plain` **BUILD SUCCESSFUL** (`471/471` JVM unit tests passed across `85` suites; `0` failures, `0` errors, `0` skipped, including `ImportVaultAuditFixesTest` `5/5`, `ImportVaultPhase4Test` `6/6`, `ImportVaultPhase3UiLogicTest` `5/5`, `ImportVaultPhase2Test` `11/11`).
  - Debug APK (`app/build/outputs/apk/debug/app-debug.apk`, SHA-256 `65331417282c4949e5dea86f1308a34aca04bb12310f09900b69d02ca416d293`) installed in-place on `OnePlus CPH2745` (`emulator-5554`, `com.pocketdev.advance_erp2`, `firstInstallTime=2026-11-22 02:39:36` intact) and launched cleanly on `ENGLISH` + Light mode.

## Import Vault Phase 4 (Documents, Local Attachments, PDF/XLSX Export, Backup/Restore & Security) — 2026-11-26

Implemented **Phase 4 ONLY** of `ADVANCE_ERP_Import_Vault_Master_Spec_V2.md` across Screen F Document Management & Local Attachments, Reconciled PDF/XLSX Export, ZIP Backup/Restore Integration, and Strict Owner/Admin Guards (Phase 5 ADB Verification NOT started; Room v14 `14.json` and `ERPBottomNavigation.kt` untouched):
- **Managed Attachment Storage & ZIP Backup/Restore (`ImportVaultAttachmentStorage.kt`, `BackupRepositoryImpl.kt`, `ImportVaultDao.kt`)**:
  - Created `ImportVaultAttachmentFileStore` (pure-JVM) & `ImportVaultAttachmentStorage` (EXIF image compression/orientation, PDF/document copy into `filesDir/vault_attachments/`, immutable snapshot persistence, `FileProvider` viewing, `ControlCenterShareService` sharing, and canonical-path safe delete refusing to touch `po_attachments/`, `product_images/`, or ERP records).
  - Updated `BackupRepositoryImpl` (`createBackup`, `verifyBackupZipEntriesIntegrity`, `extractVaultAttachmentsFromZip`, `restoreBackup`, `isSafeBackupZipEntry`, `resetTestTransactionsKeepMasters`) to archive, CRC-verify, restore, path-traversal-guard, and reset `vault_attachments/`.
- **Auto-Linked Documents, Owner-Only Entrypoints & Repository Enforcement (`ImportVaultModels.kt`, `ImportVaultAnalyticsEngine.kt`, `ImportVaultRepository.kt`, `ImportVaultRepositoryImpl.kt`, `ERPDrawer.kt`, `MoreHubScreen.kt`, `AdvanceErpNavGraph.kt`, `MainAppScaffold.kt`)**:
  - Extended `VaultDocumentRef` (`attachmentId`, `localFilePath`, `mimeType`, `fileSizeBytes`, `isLiveRegeneratedReport`) and updated `ImportVaultAnalyticsEngine.buildDocumentRefs` to auto-link `PurchaseOrder.attachments` (supplier invoices), `ImportCostItem.attachmentPath`, `AdditionalProductCost`, `ExpenseVoucher.attachmentPath`, `SalesReturn`, plus separate `Live Regenerated Statement`, `Immutable Snapshot`, and `Local Vault Attachment` rows.
  - Enforced `ImportVaultAccessGuard.requireVaultAccess(role)` (`OWNER_ADMIN` only) across `ImportVaultRepository`, `ImportVaultExportBuilder`, `ImportVaultExcelGenerator`, `ImportVaultPdfGenerator`, and `ImportVaultAttachmentFileStore`/`Storage`, and hid `IMPORT_VAULT` in `ERPDrawer` (`visibleDrawerMenuItems`) and `MoreHubScreen` for non-owner roles.
- **Reconciled PDF & 8-Sheet OpenXML `.xlsx` Export (`ImportVaultExportBuilder.kt`, `ImportVaultExcelGenerator.kt`, `ImportVaultPdfGenerator.kt`)**:
  - Added 5-scope export builder (`PORTFOLIO`, `SHIPMENT`, `PURCHASE_ORDER`, `PRODUCT`, `VARIANT`) reconciling 100% with screen KPIs and surfacing Currency (`BDT`), As-Of Date, active Filters, Assumptions, and Provisional/Pending/Unattributed/Non-Merchandise-Tax/Missing-Price Warnings.
  - Generated 8-sheet OpenXML `.xlsx` (`Summary`, `Products`, `Variants`, `Sales Allocations`, `Expenses`, `Stock`, `Receivables`, `Documents Index`) with UTF-8 Bangla encoding, numeric minor-to-BDT cells, and `excelColName` `>Z` column support (`AA`–`AD`), plus Bangla A4 PDF via `CentralizedPdfEngine` with Page-1 Metadata/Assumptions/Warnings section.
- **Interactive Screen F & Export Sheet (`ImportVaultExportSheet.kt`, `ImportVaultTimelineDocumentsSection.kt`, `ImportVaultViewModel.kt`, `ImportVaultRootScreen.kt`, `ImportVaultHomeScreen.kt`, `ImportVaultDetailScreen.kt`, `ImportVaultProfitScreen.kt`, `ImportVaultProductDrillDownScreen.kt`, `ImportVaultCapitalScreen.kt`)**:
  - Added Export Bottom Sheet (scope/format/date preset/immutable snapshot toggle, live preview & warnings, PDF viewer/XLSX open, system share) and Screen F document management (`ActivityResultContracts.OpenDocument` picker, category/title dialog, search, `FullScreenImageDialog`/`PdfViewerDialog` preview, share, and delete-attachment-only confirmation).
- **Verification (`ImportVaultPhase4Test.kt`, `BackupAndRestoreValidationTest.kt`, `DrawerLogicTest.kt`)**:
  - Verified via `adev` pinned Gradle 8.13: `git diff --check` passed; `:app:assembleDebug :app:testDebugUnitTest --console=plain` **BUILD SUCCESSFUL**, **466/466 JVM tests passed** across **84** test suites (`0` failures, `0` errors, `0` skipped), including `ImportVaultPhase4Test` (**6/6**), `BackupAndRestoreValidationTest` (**4/4**), `DrawerLogicTest` (**7/7**), `ImportVaultPhase3UiLogicTest` (**5/5**), and `ImportVaultPhase2Test` (**11/11**).

## Import Vault Phase 3 (UI, Navigation, Analytics Charts & Localization) — 2026-11-26

Implemented **Phase 3 ONLY** of `ADVANCE_ERP_Import_Vault_Master_Spec_V2.md` across Navigation, Access Control UI, Screens A–F, Compose Canvas Charts, and BN/EN Localization while preserving Room v14 (`14.json` untouched), `ERPBottomNavigation.kt`, and all existing ERP modules (Phase 4 PDF/XLSX export, attachment picker/backup NOT started):
- **Navigation & Access Gate (`ERPDrawer.kt`, `MoreHubScreen.kt`, `AdvanceErpNavGraph.kt`, `MainAppScaffold.kt`, `strings.xml`, `values-bn/strings.xml`, `ImportVaultAccessGate.kt`, `ImportVaultRootScreen.kt`)**:
  - Added **Import Vault / আমদানি আর্কাইভ** (`import_vault`) to `ERPDrawer` and `MoreHubScreen` without altering `ERPBottomNavigation`.
  - Applied `ImportVaultAccessGuard` at entry (`OWNER_ADMIN` check + `BiometricPrompt` with `ON_STOP`/`onDispose` auto-lock via `CostRevealGate` and `SAFE_FALLBACK_NO_BIOMETRIC` fallback).
- **Additive Domain, Engine & Background Threading Enrichments (`ProductImageDao.kt`, `ImportVaultModels.kt`, `ImportVaultAnalyticsEngine.kt`, `ImportVaultRepository.kt`, `ImportVaultRepositoryImpl.kt`)**:
  - Offloaded Room queries onto `Dispatchers.IO` and analytics aggregation onto `Dispatchers.Default`.
  - Added `ProductImageDao.getAllImagesSync()`, primary product/variant thumbnail resolution, separated Product vs PO vs Shipment contribution shares (`profitContributionToProduct`, `profitContributionToPo`, `profitContributionToShipment`), non-duplicated shipment-PO allocation slicing (`poAllocations` / `shipmentAllocations`), source transaction/document/timeline builders, and expanded search (displayTitle, tags, notes, BN/EN product name, SKU, barcode, QR, cartonNumber).
- **Screens A–F & Charts (`ImportVaultUiLogic.kt`, `ImportVaultViewModel.kt`, `ImportVaultComponents.kt`, `ImportVaultHomeScreen.kt`, `ImportVaultDetailScreen.kt`, `ImportVaultProfitScreen.kt`, `ImportVaultProductDrillDownScreen.kt`, `ImportVaultCapitalScreen.kt`, `ImportVaultTimelineDocumentsSection.kt`)**:
  - Live data screens with Light/Dark Material 3 Purple/Teal tokens, dynamic BN/EN localization, BDT/origin currency formatting, `48.dp`+ touch targets, wrapped `FlowRow` tabs/filters (zero horizontal chip scrolling, zero truncated essential labels), and empty/loading/error/provisional/unattributed states.
  - Strict §14 chart policy (`ImportVaultChartPolicy`): Donut only for nonnegative positive totals (with unallocated residual reconciliation), diverging bars for mixed/loss bases, `N/A` for zero totals, plus Cash Recovery progress (`> 100%` supported) and `0–30`/`31–60`/`61–90`/`90+` stock aging charts.
- **Verification (`ImportVaultPhase3UiLogicTest.kt`, `DrawerLogicTest.kt`)**:
  - Verified via `adev` pinned Gradle 8.13: `:app:assembleDebug :app:testDebugUnitTest` **BUILD SUCCESSFUL**, **459/459 JVM tests passed** across **83** test suites (`0` failures, `0` errors, `0` skipped), including `ImportVaultPhase3UiLogicTest` (**5/5**), `DrawerLogicTest` (**6/6**), `ImportVaultPhase2Test` (**11/11**), `DatabaseMigrationVerificationTest` (**4/4**), and `SettingsLogicTest` (**6/6**); `git diff --check` passed.

## Import Vault Phase 2 (Schema v14, Core Bug Fixes, Allocation & Analytics Engine) — 2026-11-26

Implemented **Phase 2 ONLY** of `ADVANCE_ERP_Import_Vault_Master_Spec_V2.md` across Room v14 schema/migration, core inventory/sales/import repositories, access control, allocation & reporting engine, and automated unit + SQLite migration verification tests (Phase 3 UI NOT started):
- **Room Schema v14 & Non-Destructive `MIGRATION_13_14` (`AdvanceErpDatabase.kt`, `DatabaseMigrations.kt`, `14.json`, `ImportVaultEntities.kt`, `ImportVaultDao.kt`, `DatabaseModule.kt`)**:
  - Bumped `DATABASE_VERSION = 14` (`57` entities in `14.json`, `290,697` bytes) and added `importVaultDao()`.
  - Extended 6 existing tables (`shipment_cartons`, `warehouse_receipt_items`, `landed_cost_allocations`, `landed_cost_snapshots`, `inventory_cost_layers`, `expense_vouchers`) with lineage, `supplierDeclaredGrossWeightKg`, `originCostLayerId`, `originReceiptDate`, `layerSourceType`, `purchaseOrderId`, `purchaseOrderItemId`, and `shipmentId` columns + indices, using honest non-destructive backfills (no fabricated PO/shipment IDs on legacy `RET`/`ADJ`/`INIT` layers).
  - Created 6 new tables (`sale_layer_allocations`, `sales_return_layer_allocations`, `expense_allocation_rules`, `expense_allocations`, `import_vault_entries`, `import_vault_attachments`).
  - Updated `BackupDao.kt` & `BackupRepositoryImpl.kt` (`resetTestTransactionsKeepMasters()` clearing the 6 new v14 tables in FK-safe order; `47` reset calls verified in `SettingsLogicTest.kt`).
- **Core Bug Fixes & Policy Gates #1–#8 (`DeterministicAllocation.kt`, `LandingCostEngine.kt`, `ImportRepositoryImpl.kt`, `InventoryRepositoryImpl.kt`, `ProductRepositoryImpl.kt`, `SalesRepositoryImpl.kt`, `SalesDao.kt`, plus DAO/repository signatures)**:
  - Fixed multi-PO collision disambiguation in `LandingCostEngine` / `ImportRepositoryImpl` using `purchaseOrderItemId`, variant/PO isolation in `applyPostFinalizationCostAdjustment` (`SALE`-only consumptions), and `addAdditionalCost` capitalizable vs operating split across FIFO layers + GL `1210`/`5010`.
  - Fixed `InventoryRepositoryImpl.adjustStock` (FIFO depletion + `FifoConsumption`) & `receiveStockTransfer` (preserving `originCostLayerId`, `originReceiptDate`, `purchaseOrderItemId`, `layerSourceType = "TRANSFER_REPLICA"`), plus `ProductRepositoryImpl` opening/manual layer tagging.
  - Fixed `SalesRepositoryImpl` (`postInvoiceInternal`, `dispatchDelivery`, `processDirectExchange`, `processSalesReturn`, `convertSalesOrderToInvoice`, `cancelOrDeleteInvoice`) to persist `SaleLayerAllocation` & `SalesReturnLayerAllocation`, allocate invoice-wide discounts proportionally with deterministic rounding residuals, exclude `CANCELLED` invoices via `SalesDao.getActivePostedInvoicesInRange` (keeping `getPostedInvoicesInRange` intact for `getCustomerLedger`), and exclude `SalesInvoice.taxMinor` from merchandise Gross Profit.
- **Access Control, Analytics Engine & Repository (`UserRole.kt`, `ImportVaultAccessGuard.kt`, `ImportVaultModels.kt`, `ImportVaultAnalyticsEngine.kt`, `ImportVaultRepository.kt`, `ImportVaultRepositoryImpl.kt`, `RepositoryModule.kt`)**:
  - Added `OWNER_ADMIN` gating (`SecurityManager.canAccessImportVault`) + `BiometricCostGuard` with safe non-enrolled fallback (`SAFE_FALLBACK_NO_BIOMETRIC`).
  - Implemented attributable cash collection strictly from `payment_allocations` minus `CASH_REFUND`/`BANK_REFUND` (separate unattributed collections & opening due), aging buckets (`0-30`, `31-60`, `61-90`, `90+` from `originReceiptDate`), expected remaining profit (`Current Selling Price`), as-of date reconstruction, `Unknown / Unattributed` legacy bucket with `revenueAttributionCoveragePercent`, default `UNALLOCATED` overhead ("Attributed Net Profit"), and opt-in allocation preview.
- **Verification (`ImportVaultPhase2Test.kt`, `DatabaseMigrationVerificationTest.kt`, `SettingsLogicTest.kt`)**:
  - Verified via `adev` pinned Gradle 8.13: `:app:assembleDebug :app:testDebugUnitTest` **BUILD SUCCESSFUL**, **454/454 JVM tests passed** across **82** test suites (`0` failures, `0` errors, `0` skipped), including `ImportVaultPhase2Test` (**11/11** passed), `DatabaseMigrationVerificationTest` (**4/4** passed with real SQLite v13→v14 column/PK/FK/index/backfill check against `14.json`), and `SettingsLogicTest` (**6/6** passed); `git diff --check` passed.

## Selective Test-Data Reset (Keep Products & Contacts) — 2026-11-25

Added an in-app **Clear Test Data (Keep Products & Contacts) / প্রোডাক্ট ও কন্টাক্ট রেখে টেস্ট ডাটা ক্লিয়ার করুন** option inside **Settings → Data Backup & Restore (ডাটা ব্যাকআপ ও রিস্টোর)** so the user can repeatedly clear test transactions, ledgers, and stock back to `0` while keeping all products, categories, variants, photos, customers, and suppliers intact:
- **DAO & Repository Transaction (`BackupDao.kt`, `BackupRepository.kt`, `BackupRepositoryImpl.kt`)**: Added foreign-key-safe child-to-parent Room `@Query` clear methods (`resetTestTransactionsKeepMasters()`) inside `database.withTransaction { ... }` covering all 38 transactional/stock tables + transaction audit logs (`recordType != 'BACKUP'`), resetting `customers.outstandingBalanceMinor = 0` & `suppliers.outstandingPayableMinor = 0` (with `isActive = 1`), creating an automatic pre-reset `.db.bak` safety snapshot under `filesDir/safety_backups`, and clearing unposted `editor_drafts_v1` & `po_attachments/` while keeping `products`, `product_categories`, `product_variants`, `product_images`, `customers`, `suppliers`, `warehouses`, `chart_of_accounts`, `business_profiles`, `app_settings`, `backup_records`, `saved_suggestions`, and `product_images/` / `avatars` intact.
- **Settings ViewModel & UI (`SettingsViewModel.kt`, `BackupManagementSection.kt`, `SettingsScreen.kt`)**: Added `resetTestDataKeepProductsAndContacts`, loading state, localized BN/EN action button, and a detailed confirmation dialog clearly listing preserved vs cleared data before executing.
- **Unit Test (`SettingsLogicTest.kt`)**: Added `resetTestTransactionsKeepMasters_preservesProductsAndContactsAndClearsStockAndLedgersWithoutFkErrors` verifying the exact 41 Room DAO queries against all 51 v13 schema entities with `PRAGMA foreign_keys = ON`.

Verified via `adev` pinned Gradle 8.13: `:app:assembleDebug :app:testDebugUnitTest` **BUILD SUCCESSFUL**, **442/442 JVM tests passed** across 81 files (`0` failures, `0` skipped, including `SettingsLogicTest` 6/6); `git diff --check` passed. Debug APK (`app/build/outputs/apk/debug/app-debug.apk`, SHA-256 `9a36d090c5f1f25a621ffe42a6ad9669b6dc1e277d3eccfd47c924b75d6050ea`) installed in-place on `emulator-5554` (`com.pocketdev.advance_erp2`, `firstInstallTime=2026-11-22 02:39:36` intact; live data untouched).

## Sales Invoice & POS Biometric Cost/Profit Protection — 2026-11-25

Protected purchase cost (`Cost`) and profit (`Profit` / `Estimated Profit`) behind Android system biometric/fingerprint authentication in both **New Sales Invoice** and **Quick POS**, keeping them hidden by default (`isVisible = false`):
- **Custom Biometric Prompt Strings (`BiometricCostVisibility.kt`)**: Added optional `promptTitle` and `promptSubtitle` parameters to `rememberBiometricCostVisibility` while preserving `BIOMETRIC_STRONG` gating and `ON_STOP` / disposal auto-lock (`CostRevealGate`).
- **New Sales Invoice & Quick POS Gating (`CreateInvoiceDialog.kt`, `PosDialog.kt`)**: Replaced open-by-default `showCostAndProfit` state with `rememberBiometricCostVisibility`, requiring fingerprint verification when tapping the visibility eye button (`VisibilityOff` -> `Visibility`), while enforcing role permissions (`SecurityManager.canViewCost` / `canViewProfit`).
- **Unit Test (`SalesInvoiceRedesignCalculationsTest.kt`)**: Added `salesInvoiceCostAndProfitHiddenByDefaultAndUnlockedOnlyAfterBiometricAuth`.

Verified via `adev` pinned Gradle 8.13: `:app:assembleDebug :app:testDebugUnitTest :app:assembleDebugAndroidTest` **BUILD SUCCESSFUL**, **441/441 JVM tests passed** across 81 files (`0` failures, `0` skipped); `git diff --check` passed. Debug APK (`app/build/outputs/apk/debug/app-debug.apk`, SHA-256 `757357f282b44d46a1b851a529996f5b8991d4f0b34f8f315d30fc7439549cc4`) installed in-place on `OnePlus CPH2745` (`com.pocketdev.advance_erp2`, `firstInstallTime=2026-11-22 02:39:36` intact) and tested by the user on-device.

## Customer Lookup & Selection UI Redesign — 2026-11-25

Redesigned `CustomerLookupDialog` to match the premium purple-teal modal reference in both Light and Dark modes while preserving Room DB v13 schema and existing invoice/customer state:
- **Redesigned Modal Header & List (`CustomerLookupDialog.kt`)**: Soft lavender wave banner, `Groups` rounded icon badge, circular `X` dismiss button, purple-bordered search bar with QR/Barcode scan trigger (`BarcodeQrScannerDialog`), teal-to-purple gradient `+ New Customer — Create & Continue >` CTA, `Customers (N)` header, avatar/name/mobile/code cards, and selected purple checkmark (`✓`) indicator.
- **Bangla/English/Phone/ID Search & QR Lookup (`CustomerLookupMatcher`)**: Supports instant filtering by English/Bangla customer name, English/Bangla mobile digits (`০`–`৯`), customer ID (`CUST-xxxx`), address, or scanned QR/Barcode value.
- **Inline Customer Persistence (`SalesViewModel.createCustomerInline`, `SalesScreen.kt`, `CreateInvoiceDialog.kt`)**: Persists newly created customers via `ContactsRepository.addCustomerWithOptionalOpeningDue`, generates a unique `CUST-xxxx` code, loads their summary, and auto-selects them on the active invoice without losing items or calculations.
- **Unit Tests (`SalesInvoiceRedesignCalculationsTest.kt`)**: Added `customerLookupMatchesEnglishBanglaMobileCustomerIdAndScannedQr`.

Verified via `adev` pinned Gradle 8.13: `:app:assembleDebug :app:testDebugUnitTest :app:assembleDebugAndroidTest` **BUILD SUCCESSFUL**, **440/440 JVM tests passed** across 81 files (`0` failures, `0` skipped); `git diff --check` passed. Debug APK (`app/build/outputs/apk/debug/app-debug.apk`, SHA-256 `d7c70227bc98a2f1d74b4b2d49686cf22bcb3852128cdf5c25b356f5e2f8eb55`) installed in-place on `OnePlus CPH2745` (`com.pocketdev.advance_erp2`, `firstInstallTime=2026-11-22 02:39:36` intact) and checked by the user on-device.

## New Sales Invoice Redesign, Editable Contacts/Opening Due & Received PO Reopen/Edit — 2026-11-25

Implemented and verified three major enhancements without modifying Room DB v13 schema or live database records:
1. **New Sales Invoice UI Redesign (`CreateInvoiceDialog.kt`, `SalesInvoiceDesignComponents.kt`, `SalesInvoiceCalculator.kt`, `SalesRepositoryImpl.kt`, `ProductImageDao.kt`, `SalesViewModel.kt`, `SalesScreen.kt`, `SalesInvoiceRedesignCalculationsTest.kt`)**:
   - Purple-teal wave hero header, customer banner with separate 4-across historical KPI strip (`Total Sales`, `Paid`, `Due`, `Returns`), warehouse selector, `Invoice Items` card list with product thumbnails, **Expand/Minimize**, **Duplicate**, and **Delete** controls.
   - Exact `BigDecimal` calculation engine supporting both `%` and `৳` discounts at item and invoice levels, additional charges, notes/terms, 4-across payment settlement tiles + account dropdown + `Full Pay`, dark-purple wave `Invoice Summary`, sticky bottom footer, full Light/Dark theme support, and role-gated Cost (`canViewCost`) & Profit (`canViewProfit`) visibility.
2. **Editable Customers & Suppliers + Opening Due / Payable (`ContactsScreen.kt`, `ContactsViewModel.kt`, `ContactsRepository.kt`, `ContactsRepositoryImpl.kt`, `RecordCustomerPaymentDialog.kt`, `CustomerLedgerAdjustment.kt`, `PaymentAllocationValidation.kt`, `LedgerReportEngine.kt`, `AccountingDao.kt`, `BusinessControlCenterRepositoryImpl.kt`, `AccountingRepositoryImpl.kt`, `CustomerLedgerAdjustmentTest.kt`)**:
   - Full edit dialogs and opening due/payable balance tracking integrated into customer & supplier ledgers and reports.
3. **Received/Paid PO Reopen & Edit (`ImportRepository.kt`, `ImportRepositoryImpl.kt`, `ImportViewModel.kt`, `ImportManagementScreen.kt`, `PurchaseOrderDetailDialog.kt`, `PurchaseOrderDao.kt`, `ImportCostDao.kt`, `InventoryCostLayerDao.kt`, `PoPackingEngine.kt`, `PoSmartPackingAndExportTest.kt`)**:
   - Safe reopen & product/carton modification for received/paid POs with full accounting and inventory reversal integrity.

Verified via `adev` pinned Gradle 8.13: `:app:assembleDebug :app:testDebugUnitTest :app:assembleDebugAndroidTest` **BUILD SUCCESSFUL**, **439/439 JVM tests passed** across 81 files (`0` failures, `0` skipped); `git diff --check` passed. Debug APK (`app/build/outputs/apk/debug/app-debug.apk`, SHA-256 `16b69a99b0889d8ff1d5871d1c75cf894e92b3b38dc96c18ac3119346dd11ac0`) installed in-place on `OnePlus CPH2745` (`com.pocketdev.advance_erp2`, `firstInstallTime=2026-11-22 02:39:36` intact) and visually confirmed by the user on-device.

## PO Product Selection Minimization, First-Variant Propagation & Product Summary — 2026-11-25

Implemented the four requested PO Section 2 (**পণ্য / Products**) enhancements across `PoPackingEngine.kt`, `PoProductVariantPicker.kt`, and `CreatePurchaseOrderDialog.kt`, plus unit test coverage in `PoSmartPackingAndExportTest.kt`:
- **Picker variant minimization (`PoCatalogProductVariantPickerSheet`)**: Added per-product Minimize/Expand buttons and a header **সব মিনিমাইজ / Minimize All** toggle while keeping variant counts, selected badge counts, and **সব নিন / Select All** accessible.
- **No pre-filled default `"100"` quantity**: Removed the picker's `ডিফল্ট পরিমাণ (PCS)` input (`"100"` default) and pre-inserted blank/100 item defaults; selected variants enter Step 2 (**পণ্য**) with empty quantity and unit price fields.
- **First-variant auto-propagation with optional override (`PoPackingEngine.updateProductVariantQuantity` / `updateProductVariantPrice`)**: Entering or updating Quantity or Unit Price on the 1st variant of a product automatically propagates to the remaining variants of that product unless individually edited (marked custom and preserved across subsequent 1st-variant edits).
- **Product-wise grouping, minimization & summary totals (`CreatePurchaseOrderDialog` Step 1)**: Grouped PO items under parent product cards with inline Quantity/Unit Price fields, per-product Minimize/Expand toggle (and **সব মিনিমাইজ / Minimize All**), and always-visible **মোট অর্ডার পণ্য (Total Ordered Qty)** & **মোট মূল্য (Total Amount)** summary badges (`PoPackingEngine.summarizeProductGroup`).

Verified via `adev` pinned Gradle 8.13: `:app:assembleDebug :app:testDebugUnitTest :app:assembleDebugAndroidTest` **BUILD SUCCESSFUL**, **412/412 JVM tests passed** across 77 files (0 failures, 0 errors, 0 skipped), including `PoSmartPackingAndExportTest` (6/6 passed). `git diff --check` passed. Debug APK (`app/build/outputs/apk/debug/app-debug.apk`, SHA-256 `478f4d348e041a1e6473945b2daf8ddbf0c1e1d8706ad29ad2599e7f92acec57`) installed in-place on `OnePlus CPH2745` (`com.pocketdev.advance_erp2`, live data preserved); picker minimization, empty initial fields, and live PO product workflow verified on device and confirmed by the user.

## Password-free backup/restore — 2026-11-23

User requested removing backup encryption and all password prompts. Manual and
automatic backups now always create ordinary ZIP files, regardless of any old
saved backup key. Share exports a plain ZIP (or an existing raw SQLite backup)
without a password dialog. Settings encryption controls and restore password fields
were removed. Backup repository/UI interfaces no longer accept passphrases.
Archive detection uses contents instead of filename, including picked/renamed files.

Legacy encrypted archives are read-only compatible using an already saved key;
that key is retained solely for old files, and is no longer exposed in Settings.
An encrypted legacy archive without its key cannot be made password-free; it fails
before changing active data and reports that it needs prior conversion to ZIP.
Old backups are not deleted or rewritten. Four `PlainBackupExportTest` cases cover
plain sharing, old saved-key compatibility, missing-key cleanup and renamed ZIPs.

Verified with `adev` pinned Gradle 8.13: `assembleDebug`, `testDebugUnitTest`,
`assembleDebugAndroidTest` **BUILD SUCCESSFUL**, **408 JVM tests passed** (0 failures,
0 errors, 0 skipped), including four new plaintext/legacy-sharing tests.
`git diff --check` passed. Production-package APK, build log and verification JSON:
`$HOME/.local/share/advance-erp-artifacts/2026-11-23-password-free-backup/`.
APK: `AdvanceERP-password-free-backup.apk`, SHA-256
`bd4882477fa58f8b73ef55169f1c67f38c9d6346b58181347587aa93e37ae25d`.
ADB is disconnected (old localhost:34729 refuses connection); user was asked to
reconnect through AntiGem ADB Connect. **The production APK is NOT installed**;
live restore and runtime backup UI checks were not executed. The separate migration
QA APK was installed earlier, but its native fixture test did not run because ADB
disconnected. Its test APK was preserved in the private history review directory as
`migrationqa-test.apk` for resuming without rebuilding; verify package before use.

## Tuktak migration preparation — 2026-11-23

**Latest accepted decisions and history rehearsal:** user confirmed dated customer
ledger entries for invoices/receipts with **no invoice allocations**, and actual
SHOES INSOLE RETAIL ending stock **0**, including all six stale source variants.
`tools/migration/rehearse_history.py` now produces an isolated v13 history candidate
with 848 mapped records and 96 customer ledger events (32 invoices, 62 receipts,
1 return, 1 explicit dated discount adjustment). All 8 customer closing balances,
source document dates/amounts, stock, balanced journals and unchanged baseline rows
passed SQLite verification. Full source archive stays intact. Native Sales/Control
Center gained typed dated customer journal adjustments without Room schema changes.

Latest private candidate/report/CSV:
`$HOME/.local/share/advance-erp-migration/2026-11-23-history-review-verified/`.
**Still NOT a live-import artifact**: legacy source account chart is separate,
media paths are local rehearsal references, and original source GL inconsistencies
remain. Posted AR exceeds source customer net due by 6560 BDT after the real -240
discount (duplicate 5760 sale + orphaned 800 sale). Inventory GL exceeds remaining
batch value by 469058 BDT (historical sales COGS not posted in source). **User
declined additional correction entries**: preserve original source journals and
show old accounting differences in the report. Do not add financial corrections.

Build via `adev` pinned Gradle 8.13 succeeded with **404 JVM tests passing**.
Nine Python safety/policy tests passed. A separate `.migrationqa` package build and
optional `TuktakMigrationRehearsalTest` validate both native ledgers against the
private history candidate; check the latest verification report for their outcome.
The test refuses the production package and uses its own disposable Room DB.
No live import/restore or production app install has occurred during this work.
Reusable QA Gradle init script: `tools/migration/migrationqa.init.gradle`.

The paragraphs below record the earlier master-only preparation stage; their
"pending" stock/payment questions have been answered as described above.

User requested merging installed `com.tuktak.erp` **including full history** into
Advance while preserving current Advance records. Preparation only is implemented
in `tools/migration/prepare_tuktak.py`; read its README before resuming. Six Python
safety tests passed. Actual ADB snapshots (source v33, target v13) and a disposable
master-only preview succeeded; all existing target rows, non-master tables and
Room schema were verified unchanged. 25 mapped master records plus a fallback
category were staged. **No live import, restore, app installation or Kotlin build
was performed. Full history conversion is NOT implemented or certified.**

Private SQLite archives, source media, checksums, mappings and report are outside
Git: `$HOME/.local/share/advance-erp-migration/2026-11-23-preparation-verified/`.
Never restore `masters-preview-not-for-restore.db`: it contains no imported
transaction history or stock. Earlier first/validated rehearsal folders also exist.

User answers pending: SHOES INSOLE RETAIL stock (product/batches 0 vs variants 240)
and allocation policy for 62 payments without invoice IDs (oldest due first versus
unallocated customer credit). Questions already presented. Source customer dues
reconcile for all 8 parties when the 2800 BDT return and -240 BDT adjustment are
included; preserve 2 archived customers and -400 BDT credit. Source major BDT
requires x100 conversion. Financial account mapping, journal/subledger reconciliation,
historical FIFO/return costing, warehouse assignment and native isolated validation
remain required before any production merge. Do not infer readiness from the
preparation tests or claim that full migration is complete.

## Latest PO Smart Packing & Premium Export Checkpoint — 2026-11-22

Implemented and verified the complete Purchase Order workflow overhaul across Searchable Product/Variant Selection (`PoCatalogProductVariantPickerSheet`, `PoCartonVariantPickerSheet`, `BulkCreateCartonsDialog`), Smart Carton Packing & Validation (`PoPackingEngine`), Continuous `CTN-001` Carton Numbering (`renumberOrderCartonsContinuously`), Option A PO quantity reduction guards, Default & Individual `{CTN}` Shipping Marks, CTN Labels (`LabelPrintingEngine.generatePoCartonLabelsPdf`), Approved-Reference Supplier PDF (`ImportPdfGenerator`), 4-Sheet OOXML Supplier Excel (`ImportExcelGenerator`), Shared Single-Source-of-Truth Export Builder (`PoExportDataBuilder`), and `All` vs `Selected Cartons` Share/Preview/Print/Save workflows (`PoShareDialog`), preserving Room v13 schema and existing records (`PO-2026-001`..`PO-2026-004`). [Full verification report & screenshots](docs/ui-refinement/2026-11-22/po-smart-packing-export/report.md).

Verified on `adev` + live ADB device (`com.pocketdev.advance_erp2`, APK SHA-256 `8dad73803ee84dee0f2e98123d0a1661851860e917a4d0a3278c6524d10f91cd`):
- `git diff --check` passed; `:app:assembleDebug :app:testDebugUnitTest` -> **BUILD SUCCESSFUL**; **389/389** JVM tests passed across 73 files (0 failures, 0 errors, 0 skipped), including `PoSmartPackingAndExportTest` & `SupplierPoExportTest`.
- Real-device ADB verification (`localhost:45453`, `CPH2745`): Mandatory Tests **1–14** executed with 15 captured screenshots (`01`..`15`), read-only export inspection of live `PO-2026-004` (full + `Selected_2CTN` PDF & 4-sheet `.xlsx` with 14 images & live Excel formulas), `PO-2026-005` (`CONFIRMED`, `5 CTN`, `400 pcs`, `75.0 KG`) persistence after `am force-stop` cold restart, SQLite verification, and clean Logcat (`0` crashes).

## Latest E2E Phase 0–3 Bugfix & Resume Checkpoint — 2026-11-22

Real-device E2E verification progressed through **Phases 0–3** on `com.pocketdev.advance_erp2`, followed by fixes for all discovered Phase 0–3 bugs across Add Product, Product Catalog, PO/Import landed cost & freight rate, Shipments filtering, Sales invoice localization, Inventory `flatMapLatest` search/layout, Contacts title sync/validation, and Bangla localization (27 modified Kotlin files + `E2eBugFixesRegressionTest.kt`). [Full E2E checkpoint & Phase 4–6 resume plan](docs/e2e-verification/2026-11-22/checkpoint.md).

Verified on `adev` + live ADB device (`com.pocketdev.advance_erp2`, APK SHA-256 `eeb7169fc6a885476778a17fed315af6c0db9c8f4472a530458d65fd8d1f2303`):
- `git diff --check` passed; `:app:assembleDebug :app:testDebugUnitTest` -> **BUILD SUCCESSFUL**; **384/384** JVM tests passed across 72 files (0 failures, 0 errors, 0 skipped).
- Upgraded debug APK installed in-place via `adb install -r` (live DB/sample data untouched); 20 baseline & post-fix verification screenshots saved under `docs/e2e-verification/2026-11-22/screenshots/`.
- Direct `am instrument` on the live device remained blocked by an older pre-existing `com.pocketdev.advance_erp2.test` signature (`[51806981]` vs `[f7267c35]`), intentionally left uninstalled per `AGENTS.md`.
- **Remaining E2E scope (paused for user's emergency PO module work)**: **Phase 4** (Sales, POS, Delivery Challan, Customer Ledger, Due Collection, Returns), **Phase 5** (Accounts, Control Center/Reports/PDF/XLSX exports, Dashboard, Settings Dark/Light & BN/EN, Cold Restart), and **Phase 6** (WAL SQLite & Logcat audit + final `e2e-test-report.md`). Never uninstall `com.pocketdev.advance_erp2`, clear user data, or run destructive restores.

## Latest Add Product 3-Step Redesign checkpoint — 2026-11-22

Three-step premium Light & Dark Add Product wizard implemented in `AddProductDesignComponents.kt` and `AddProductDialog.kt`, plus end-to-end `Product.description` persistence across `ProductViewModel`, `ProductListScreen`, `EditProductDialog`, `ProductDetailDialog`, `CreatePurchaseOrderDialog`, and `ImportViewModel` without any Room DB schema/migration or financial-engine changes. Also fixed `SuggestTextField` non-Hilt fallback, `Theme.kt` `findActivity()` ContextWrapper unwrapping, and `ERPFullScreenEditor` optional inset/status-bar controls. [Full report](docs/ui-refinement/2026-11-22/add-product-redesign/report.md).

Verified on `adev` + ADB (`com.pocketdev.advance_erp2`, APK SHA-256 `b78618b3bbdaebf8c1fb59b4f07c353d7da6e6d9442f420a2db62c1416cb172c`):
- `assembleDebug`, `testDebugUnitTest`, `assembleDebugAndroidTest`: **BUILD SUCCESSFUL**; **381/381** JVM tests passed across 71 files (0 failures/errors/skipped).
- Direct ADB instrumentation: `AddProductThreeReferenceTest` **8/8 passed**, `PremiumUiRegressionTest` **5/5 passed**.
- Actual Light & Dark device screenshots for Steps 1–3 and variant cards captured under `docs/ui-refinement/2026-11-22/add-product-redesign/screenshots/` using isolated fixtures; existing app/user data never uninstalled, cleared, or modified.

## Latest PO checkpoint — 2026-11-22

Six-reference PO UI refinement implemented across List, Information, Products, Packaging, Landed Cost and Details. Continue from this implementation; do not restart. [Full feature checklist, modified files, visual differences and verification](docs/ui-refinement/2026-11-22/po-six-reference/implementation-review.md). [Offline side-by-side comparison](docs/ui-refinement/2026-11-22/po-six-reference/comparison.html).

Final native `adev` build (`assembleDebug`, `testDebugUnitTest`, `assembleDebugAndroidTest`) succeeded; 376 JVM tests passed. Direct ADB instrumentation passed 37 tests. Gradle connected wrapper has an environment missing-aapt teardown limitation; do not claim that wrapper passed. Pinned Gradle 8.13/dependencies preserved.

Actual device captures: six Light, six Dark, extra scrolled cost summaries and live keyboard check in report directory. Screenshots use production Compose UI with isolated fixtures; no fixture records posted to live database. Package/applicationId remains `com.pocketdev.advance_erp2`; no schema/migration/financial-engine changes. Existing app/data not cleared or uninstalled. User draft was opened without edits and closed.

Residuals: fonts/icons/system chrome, header/card proportions and extra preserved fields differ from references; no 100% pixel-perfect claim. External share target/full unrelated instrumentation not tested. This PO verification does not certify the earlier whole-ERP audit or backup/restore work below. This checkpoint accompanies the six-reference PO implementation commit; verify remote tracking with git status when resuming. Do not touch preexisting untracked `.antigem/`.


> ২০ নভেম্বরের আপডেট ও যাচাইয়ের ফল [পরবর্তী অডিটে](docs/ui-refinement/2026-11-19/premium/resume-audit.md) আছে। নিচের অংশটি ১৯ নভেম্বরের চেকপয়েন্ট।

**Saved at the user's request on 2026-11-19 (Asia/Dhaka), before uninstall/reinstall of their development/chat app.**

## Resume instruction / এখান থেকে শুরু করুন

Read this file before changing anything. The redesign is **work in progress, not a verified release**. Preserve the existing implementation and continue from the checks below; do not restart the redesign or ask the user to repeat settled preferences.

The user originally requested a full UI and feature-logic audit, discussed the design, accepted the implementation plan, and said “Implement the plan.” They subsequently asked to stop implementation, document current progress, and commit/push everything so work can resume after reinstall. Resume implementation only when they return and request it.

Communicate in Bengali. Do not spawn agents unless explicitly requested. Work in the actual checkout, not the stale `/workspace/clever-lovelace` path mentioned in GEMINI.md.

## Agreed product/design decisions

- Native Kotlin/Jetpack Compose + Room, current offline architecture; no framework migration/cloud sync/new accounting policy.
- Refined Purple branding, balanced information density, complete Light and Dark themes.
- Primary workflow priority: **Purchase → Stock**, then other modules.
- Bottom navigation: **Home / Purchase / Stock / Sales / More**. Accounts, Products, Contacts, Ledger/Reports and Settings remain reachable through More/drawer.
- Icons: consistent outline family, filled selected bottom-navigation icon.
- Large PO, packing and receiving forms: full-screen native editors, fixed actions, keyboard/system-inset handling.
- Leaving a changed editor: Save draft / Discard / Continue editing. Preserve text through recreation.
- Completing a purchase with supplier money still due is allowed. Completion must not settle the payable or change payment ledger balances.
- Receiving shortages need disposition/reason at completion; all monetary/inventory invariants must remain correct.
- Light: brand #792B87, background #F7F5F9, surface white, text #241E2B.
- Dark: primary #D5A4E0, background #151218, surface #211B27, text #F4EEF7.
- Suggested sizes: screen padding 16 dp, 16 dp card radius, icons 20–24 dp, touch targets >=48 dp. Full visual/accessibility verification remains required.

## Implemented in source (not all verified)

### Theme, icons, navigation and screens

- Rebuilt `core/designsystem/Theme.kt` light/dark palettes; semantic tokens in `Color.kt` now read MaterialTheme through composable getters.
- Converted many hardcoded surfaces/status colours across feature screens to semantic tokens. Broad direct Material filled icon usages were changed to outline variants.
- Native Material3 bottom navigation now has Home/Purchase/Stock/Sales/More with selected/unselected icons and route grouping (shipments/imports belong to Purchase; Accounts belongs to More).
- Added Accounts entry to More. Updated top bar and drawer surfaces/selection colours.
- Dashboard Purchase shortcut moved first; overview and PDF export moved below business metrics. Existing metrics remain.
- Settings offers persistent System/Light/Dark preference through DataStore, applied in MainActivity.
- Dashboard Add Product/Stock Adjust navigation now carries an action query to open the corresponding action; consumption is saveable.
- Converted many primitive form/tab flags to rememberSaveable. This is not comprehensive state restoration of every module.

### Purchase editors and draft buffers

- Added `core/designsystem/ERPFullScreenEditor.kt`: native full-screen dialog with safe drawing + IME insets and shared leave-editor prompt.
- Applied it to CreatePurchaseOrderDialog, AddCartonDialog, WarehouseReceivingDialog, ShipmentWorkspaceDialog. They remain owned by their screen, not separate Navigation destinations.
- Added `core/util/EditorDraftStore.kt`: private SharedPreferences JSON buffers. They are **unposted editor text**, not database PO/receipt records; can retain incomplete text without changing stock/payables.
- PO state includes custom JSON savers for nested item/carton lists and freight map; leave prompt and resume buffer; parent clears buffer after successful save.
- Packing and receiving also have saveable JSON lists and local leave/resume buffers.
- ReceivingLineState fields now use mutableStateOf (previously plain mutable fields, which were not observable by Compose).
- Receiving rejects empty lists and invalid quantities; sum comparison uses Long.
- PO edit identity restoration was added in ImportManagementScreen; inspect its two LaunchedEffects for races on recreation.

### Logic and persistence

- `ImportRepositoryImpl.completePurchase`: one Room transaction; returns an existing completion on retry; rejects draft/cancelled/completed-without-record; validates shortage disposition + reason. Leaves supplier payable untouched.
- `MinimalImportDao`: deterministic latest completion query (`ORDER BY completedAt DESC, id DESC LIMIT 1`). Historical duplicate records are **preserved**, not deleted. No unique constraint/migration introduced; transaction/repository guard prevents new duplicates on this path.
- `FinancialRepositoryImpl`: expense posting, reversal and transfer now wrap journal/document/audit writes in one database transaction.
- ImportViewModel has explicit completion, receiving and carton saving/error state + success callbacks. Editors close on success, keep data on failure. Broad exception handlers rethrow coroutine cancellation.
- Landing preview uses a cancellable job, clears stale preview, shows loading/error and Retry in shipment workspace.
- No database version change (still 13), no migration or asset DB changes.

### Tests added (not yet successfully run)

- Extended `ImportPersistenceRegressionTest`: completion with due/idempotency, status-write failure rollback, invalid disposition/draft rejection, concurrent completion, preservation of historical duplicates, expense-document failure rolls back journals.
- New `PremiumUiRegressionTest`: navigation selection and incomplete PO draft recreation/explicit save/resume using a test-specific buffer key.
- Existing PurchaseOrderVisualTest and the broader JVM/instrumentation suite remain available.

## Exact verification status at checkpoint

1. `git diff --check` passed before checkpoint.
2. First `:app:compileDebugKotlin` failed on composable colour reads inside scanner Canvas callbacks and missing TextPrimary in AppLockScreen. Those issues were patched: capture colours outside Canvas, keep deliberate dark lock-screen surface.
3. Second build requested `:app:assembleDebug :app:testDebugUnitTest :app:assembleDebugAndroidTest`.
4. It **failed at :app:compileDebugKotlin** with one reported error:
   `feature/product/VariantPhotoSliderDialog.kt:317:29 Unresolved reference: DeepNavy`.
5. That missing import was added immediately before this checkpoint. **The fix has NOT been rebuilt.** Do not claim a passing build, passing tests or a usable new APK.
6. No fresh app install, no new runtime screenshots, no device regression results yet. Existing screenshots/reports elsewhere in the repo predate this work.
7. Both attempted builds have finished; no build should be resumed by polling an old session ID.

Build logs committed here:
- `docs/ui-refinement/2026-11-19/premium/build.log`
- `docs/ui-refinement/2026-11-19/premium/verification.log`

## Environment and rebuild instructions

Original checkout: `/data/user/0/com.termux/files/home/ADVANCE-ERP-2`.
Remote: `https://github.com/BLACKHEART111/ADVANCE-ERP-2.git`; branch `main`.
Base before redesign: `c4fbec7` (custom POS icon change).

Android/Termux, no root/sudo. Use `$TMPDIR`, not host `/tmp`. Edits run on Termux; Android builds through `adev` (Ubuntu proot).
Android Development plugin skill was read: `~/extensions/android-dev/skills/android-dev.md`, plus `kotlin-compose.md` and `run-on-phone.md`. Re-read appropriate environment instructions if reinstall changed setup.

Setup check:

```sh
bash "$PREFIX/share/antigem-android/android-setup.sh" status
adb devices
```

At checkpoint container/base/java/sdk/launcher/adb all reported yes. ADB had one connected device. Do not assume it reconnects automatically after reinstall.

No Gradle wrapper exists in the repo. Termux global Gradle is 9.9.0; project AGP 8.11.0 uses Gradle **8.13** for this work. Downloaded Gradle 8.13 distribution under `~/.cache/advance-erp-tools/gradle-8.13`; this cache is NOT in Git and may disappear on uninstall. Recreate it from the official Gradle distribution if necessary. Do not upgrade Kotlin/AGP merely to use global Gradle.

Working build command (from repo; paths may need adjustment after reinstall):

```sh
adev 'GRADLE_OPTS="-Xmx2g -XX:MaxMetaspaceSize=768m" /data/data/com.termux/files/home/.cache/advance-erp-tools/gradle-8.13/bin/gradle -Dorg.gradle.jvmargs= --no-daemon --max-workers=2 :app:assembleDebug :app:testDebugUnitTest :app:assembleDebugAndroidTest --console=plain'
```

Container SDK is `/root/android-sdk`, Java 21. Container global Gradle properties already specify Termux arm64 aapt2. AntiGem init script restricts native ABI to arm64. First builds download substantial dependencies; later builds reuse them. Do not clean routinely; don't run two builds concurrently. Gradle logged a single-use daemon despite --no-daemon; it stops after each invocation.

## Resume checklist — do these next, in order

1. Check git status/current commit; read this file and the two logs. Preserve all checkpoint work.
2. Rebuild after the DeepNavy import fix. Resolve any additional compilation errors, especially in new androidTest code. Capture fresh logs.
3. Review draft/recreation behaviour before trusting it:
   - PO items/cartons/freight/notes/attachments and payment term survive recreation.
   - Delayed async initial values do not overwrite edited/restored values.
   - PO edit identity restoration does not clear the saved order ID on first composition.
   - Unchanged form closes without unnecessary prompt; changed form has all three exit choices.
   - Save buffer never confirms/posts; successful serverless repository save clears buffer; failed save leaves editor open.
   - Packing/receiving Save buttons should be visibly disabled/loading while saving, not merely guarded in ViewModel.
   - Receiving method switching and refreshed expected quantities must not silently discard edits or restore obsolete quantities.
   - Current receiving/packing buffer writes use synchronous SharedPreferences.commit; consider moving off main while retaining visible error handling.
4. Run JVM suite; inspect failures for real regressions versus old source-string/style assertions. Do not simply delete failing assertions.
5. Run real SQLite + Compose instrumentation tests with fixture data. Verify expense rollback test and completion concurrency test.
6. Verify app signing before replacing installed `com.pocketdev.advance_erp2`. Never uninstall the user's installed ERP to solve signing mismatch; that deletes data. Use an isolated preview build if needed.
7. Capture Light/Dark, Bangla/English, large-font, keyboard and narrow-screen examples. Audit all broad colour replacements, especially deliberate dark headers/white text and primary button foreground contrast. Check drawer footer colour and remaining hardcoded white backgrounds.
8. Finish full module audit: Product/Stock/FIFO/transfers; Sales/delivery/return/collection; Accounts/journals/reversal/reports; Contacts; Settings/security/backup/export. Existing unit tests alone do not prove screen behaviour.
9. Important newly noticed audit concern, NOT fixed: BackupRepositoryImpl.restoreBackup checkpoints then overwrites the SQLite file while the Room instance may still be open, removes WAL/SHM, then writes an audit entry through the existing DAO. Review safe staged restore/restart/rollback carefully with isolated test databases; never trial restore on user data. File validation uses SQLite quick_check; examine schema compatibility as well.
10. Complete a final audit report distinguishing confirmed fixes, tested outcomes and open risks; only then produce verified APK/screenshots and summarize completion.

## Remaining scope / do not overclaim

The entire original plan is **not complete**. Most shared styling and key purchase changes are in source, but broad screen redesign, every 48 dp touch target, all labels/localization, complete state persistence, full runtime audit, comprehensive backup verification and visual acceptance are unfinished. There is no completed release artifact yet.

No user business database/media backup was created or uploaded by this checkpoint. Git contains project source and its already-tracked assets, not a backup of the currently installed ERP app's live data. Uninstalling the ERP itself can erase its private database unless separately exported.

## User message to resume after reinstall

> ADVANCE-ERP-2 repo-এর main branch নাও। প্রথমে AGENTS.md ও WORK_IN_PROGRESS.md পড়ো। Premium UI checkpoint থেকে বাকি implementation চালিয়ে যাও; নতুন করে redesign শুরু কোরো না। আগে build ও regression tests ঠিক করো, তারপর বাকি UI/logic audit শেষ করো।
