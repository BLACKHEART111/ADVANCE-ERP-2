# ADVANCE ERP — Complete Import Module & Landing Cost Audit Report

## 1. Executive Summary
An in-depth audit of the existing Import Management, Purchase, Supplier, Inventory Receiving, Stock Movement, Landing Cost Calculation, FIFO Cost Layer, and Accounting modules (`com.pocketdev.advance_erp`, Room Database v10) was conducted prior to refactoring.

The existing system contains two parallel workflows inside `ImportRepositoryImpl`:
1. **Single-PO ("One Purchase = One Batch") Direct Flow**: Managed via `PurchaseOrderDetailDialog`, supporting single-supplier PO creation, supplier payment schedules, direct PO warehouse receiving (`createPurchaseReceiving` / `postPurchaseReceiving`), `AdditionalProductCost`, and `PurchaseCompletionRecord`.
2. **Legacy Shipment-Based Flow**: Managed via `ImportShipment`, `ShipmentCarton`, `ImportCostItem`, `LandedCostAllocation`, `LandedCostSnapshot`, and `WarehouseReceipt`.

While foundational engines (`CbmCalculator`, `CartonWeightAllocationEngine`, `LandedCostAllocationEngine`, double-entry `AccountingRepository`, and `InventoryCostLayer` / `FifoConsumption`) are solid and reusable, the Shipment-based Import Management workflow has critical structural and calculation gaps against the Phase 2–6 business requirements.

---

## 2. Existing Functionality That Works (Reusable Components)
- **Geometry & Weight Engines**:
  - `CbmCalculator.kt`: Accurate `BigDecimal` calculation of single carton CBM (`L * W * H / 1,000,000`), total CBM, and volumetric weight.
  - `CartonWeightAllocationEngine.kt`: Proportional carton gross weight allocation and discrepancy detection.
- ** Core Landed Cost Allocation Math**:
  - `LandedCostAllocationEngine.kt`: Deterministic `BigDecimal` allocation with largest-remainder / last-item rounding reconciliation so allocated minor units always sum exactly to the expense total. Currently supports `QUANTITY`, `GROSS_WEIGHT`, `CBM`, `PURCHASE_VALUE`, and `MANUAL`.
- **Payment Terms & Multi-Currency Supplier Payments**:
  - `PaymentTermsCalculator.kt`: Supports `FULL_ADVANCE`, `THIRTY_SEVENTY` (30% advance / 70% balance), `FIFTY_FIFTY`, `FULL_ON_DELIVERY`, and `CUSTOM` schedules with exact minor-unit reconciliation.
  - `SupplierPayment` & `ImportRepositoryImpl.recordSupplierPayment`: Overpayment guards, foreign currency to BDT conversion, and double-entry posting (`Dr. Accounts Payable 2010 / Cr. Cash 1010 or Bank 1020`).
- **FIFO Cost Layers & Sales Depletion**:
  - `InventoryCostLayer`, `SalesCostLayerUsage`, `FifoConsumption`, and `InventoryCostAdjustment` tables exist and are wired into `SalesRepositoryImpl.postInvoice()`, depleting oldest active layers per warehouse and product/variant and posting `Dr. COGS 5010 / Cr. Inventory Asset 1210`.
- **Double-Entry Chart of Accounts**:
  - 27 standard accounts seeded in `DatabaseMigrations.seedStandardAccounts`, including `1210` (Inventory Asset), `1220` (In-Transit Inventory), `1050` (Supplier Shortage & Claims Receivable), `2010` (Accounts Payable), `5010` (COGS), `5050` (Damaged Merchandise Write-Off), and `5110` (Rent & Utilities).

---

## 3. Existing Functionality That Is Incomplete
1. **Multi-Supplier & Multi-PO Shipment Structure (Phase 2A)**:
   - `ImportShipment` currently enforces a single `supplierId: String` (non-null FK) and a single `purchaseOrderId: String?`.
   - Cannot attach multiple suppliers, multiple Purchase Orders, or multiple supplier-specific currencies/exchange rates under one Shipment.
2. **Supplier-Wise China Local Delivery (Phase 2B)**:
   - No supplier-level China Local Delivery structure exists on a Shipment.
   - Cannot automatically allocate each supplier's China Delivery Charge across only that supplier's products using `Supplier Delivery Charge * (Product Purchase Value / Supplier Total Purchase Value)` or optional manual product-wise allocation in foreign currency converted via the supplier's exchange rate.
3. **Carton & Packaging List (Phase 2C)**:
   - `ShipmentCarton` lacks `supplierId`, `categoryId`, and `categoryName`.
   - `CreateShipmentDialog` and `ImportRepositoryImpl.createShipment` require `lengthCm > 0 && widthCm > 0 && heightCm > 0`, whereas dimensions must be optional (allowing direct CBM entry when dimensions are omitted).
4. **Two-Level International Freight by Category (Phase 2D)**:
   - `ProductCategory` has no `defaultFreightRatePerKgMinor`, and there is no `ShipmentCategoryFreightRate` table to store shipment-specific rate overrides per category while preserving historical rates.
   - `ShippingCostCalculator` only computes a flat shipment-wide rate instead of `Category Freight = Total Category Gross Weight * Applicable Category Rate/KG` allocated to products in that category by actual gross weight.
5. **Bangladesh Local Transport & 6 Allocation Methods (Phase 2E)**:
   - `AllocationMethod` in `LandedCostAllocationEngine.kt` is missing `CARTON_COUNT` (Allocation by Carton Count).
   - Manual product-wise allocation does not validate that `sum(manualProductAllocations) == expenseTotal` in the UI/engine workflow.
6. **Inventory Receiving & Pending Quantity Reconciliation (Phase 3)**:
   - `WarehouseReceiptItem` tracks `expectedQuantity`, `receivedQuantity`, `damagedQuantity`, and `missingQuantity`, but does not explicitly persist `goodQuantity` and `pendingQuantity` with the strict invariant `Packed Quantity = Good + Damaged + Missing + Pending` across multiple partial receiving sessions.
   - Pending quantities are not tracked in In-Transit Inventory (`1220`) during partial receiving / finalization.
7. **Warehouse Monthly Rent & Customer Delivery Separation (Phase 2G & 2H)**:
   - `ExpenseVoucher` does not link monthly rent entries to a specific `warehouseId` and `rentMonth` for warehouse-wise monthly rent history/reporting, nor does it explicitly link customer delivery expenses to `salesInvoiceId` / `deliveryChallanNumber` while preventing their inclusion in Import Landing Cost.

---

## 4. Existing Functionality That Is UI-Only or Broken in Wiring
1. **Receive Button Wiring Bug in `ImportManagementScreen.kt` (Line 1402)**:
   - When receiving against an active PO in `WarehouseReceivingDialog`, `onConfirmPost` calls `viewModel.createPurchaseReceiving(...)` which only creates a `DRAFT` receipt and never calls `postPurchaseReceiving(...)`, leaving stock balances and accounting untouched unless posted from inside `PurchaseOrderDetailDialog`.
2. **Add Cost Item Manual Allocation UI (`AddCostItemDialog.kt`)**:
   - Selecting `MANUAL` in `AddCostItemDialog` does not provide per-product manual allocation inputs or validate that their sum equals the expense total.
3. **Shipment Landing Cost Review & Post-Finalize Adjustment Screen**:
   - `ImportManagementScreen` has no dedicated Shipment Landing Cost Review dialog/screen showing the 5-part cost breakdown (Purchase Cost, China Local Delivery, Category International Freight, Bangladesh Transport, Other Import Expenses), Good/Damaged/Missing/Pending quantity valuation, Finalize lock, and Post-Finalize Cost Adjustment History.

---

## 5. Database & Relationship Problems
1. `import_shipments` requires a single `supplierId` FK; multi-supplier shipments need a `shipment_suppliers` junction/detail table (`ShipmentSupplier`) and optional `shipmentId` linkage on `purchase_orders`.
2. `shipment_cartons` lacks `supplierId` and `categoryId` columns needed for supplier-wise and category-wise aggregation.
3. `product_categories` lacks `defaultFreightRatePerKgBdtMinor` for two-level category freight rate management, and there is no `shipment_freight_rates` table to snapshot applied category freight rates per shipment.
4. `import_cost_items` lacks `supplierId`, `isPostFinalization`, `isCapitalized`, `costAdjustmentReference`, and `manualAllocationsJson` to prevent duplicate expense allocation and trace post-finalize adjustments.
5. `landed_cost_snapshots` lacks granular breakdown columns (`chinaDeliveryBdtMinor`, `internationalFreightBdtMinor`, `bdTransportBdtMinor`, `otherExpensesBdtMinor`, `packedQuantity`, `goodQuantity`, `damagedQuantity`, `missingQuantity`, `pendingQuantity`).

---

## 6. Calculation Problems
1. **Catalog Price Used Instead of Actual PO/Shipment Purchase Price**:
   - In `ImportRepositoryImpl.calculateAndSaveLandedCost(shipmentId)` (line 604), the code reads `productDao.getProductById(productId)?.purchasePriceMinor ?: 0L` instead of the actual foreign currency purchase price and saved exchange rate from the Shipment's Purchase Orders!
2. **Unit Import Cost Divisor**:
   - Phase 4 requires `Unit Import Cost = Total Allocated Product Import Cost / Packed Quantity`, with consistent unit cost applied across `Good`, `Damaged`, `Missing`, and `Pending` quantities.
3. **Post-Finalize Cost Adjustment Allocation**:
   - In `ImportRepositoryImpl.adjustLandedCost(shipmentId)` (line 975), post-finalization variance is divided flatly by `totalInitialQty` across all layers in the shipment (`varianceBdtMinor / totalInitialQty`), ignoring the post-finalize expense's selected `AllocationMethod` and product-specific weights/CBM/values, and failing to insert `InventoryCostAdjustment` audit rows per affected cost layer.

---

## 7. Accounting Integration Problems
1. **Shipment Finalization Accounting**:
   - `calculateAndSaveLandedCost` marks `shipment.isCostFinalized = true` without posting the accounting capitalization / reclassification for `Good` (`1210` Inventory Asset), `Damaged` (`5050` Write-Off), `Missing` (`1050` Shortage/Loss), and `Pending` (`1220` In-Transit Inventory), or preventing duplicate capitalization.
2. **Post-Finalize Adjustment COGS vs. Inventory Split**:
   - When an additional expense is recorded after finalization, the allocated adjustment for each product must split accurately between remaining unsold FIFO inventory (`Dr. 1210 Inventory Asset`) and already sold FIFO quantities (`Dr. 5010 COGS`), while preserving original `SalesInvoice` customer prices.

---

## 8. Required Changes & Refactoring Plan
1. **Database Migration (`MIGRATION_10_11`, Version 11)**:
   - Non-destructively upgrade `AdvanceErpDatabase` from v10 to v11:
     - Add `defaultFreightRatePerKgBdtMinor` to `product_categories`.
     - Create `shipment_suppliers` (`ShipmentSupplier`) for multi-supplier, multi-PO, and supplier-wise China Local Delivery tracking.
     - Create `shipment_freight_rates` (`ShipmentFreightRate`) for category-wise default + shipment-override freight rates.
     - Extend `purchase_orders` with nullable `shipmentId`.
     - Extend `shipment_cartons` with `supplierId`, `categoryId`, `categoryName`.
     - Extend `import_cost_items` with `supplierId`, `isPostFinalization`, `isCapitalized`, `costAdjustmentReference`, `manualAllocationsJson`.
     - Extend `warehouse_receipt_items` with `goodQuantity` and `pendingQuantity`.
     - Extend `landed_cost_snapshots` with cost component breakdown and quantity outcome columns.
     - Extend `expense_vouchers` with `warehouseId`, `rentMonth`, `salesInvoiceId`, `deliveryChallanNumber` for Warehouse Rent (Phase 2G) and Customer Delivery (Phase 2H).
2. **Centralized `LandingCostEngine`**:
   - Add `AllocationMethod.CARTON_COUNT` to `LandedCostAllocationEngine`.
   - Implement `LandingCostEngine` to deterministically compute:
     - Foreign-currency Product Purchase Value -> BDT using saved exchange rate.
     - Supplier-wise China Local Delivery allocation (Purchase Value default or Manual product-wise).
     - Category-wise International Freight (`Total Category Gross Weight * Applicable Rate/KG`, allocated to category products by Gross Weight).
     - Bangladesh Local Transport (supporting all 6 allocation methods).
     - Additional Import Expenses (pre- and post-finalization, preventing duplicate allocation).
     - Unit Import Cost (`Total Allocated Product Import Cost / Packed Quantity`) and valuation split across `Good`, `Damaged`, `Missing`, and `Pending` quantities.
3. **Receiving, Finalization & Post-Finalize Adjustments**:
   - Enforce `Packed Quantity = Good + Damaged + Missing + Pending` across single and multiple partial receiving sessions.
   - Implement transactional, permission-checked `finalizeShipmentLandingCost` and `recordPostFinalizationExpenseAdjustment` with `InventoryCostLayer` updates, `InventoryCostAdjustment` records, and balanced journal entries (`1210`, `1220`, `5050`, `1050`, `5010`).
4. **UI Refactoring (`feature/imports/`)**:
   - Enhance `ImportManagementScreen` and dialogs with Shipment-centric workspace supporting Multi-Supplier & Multi-PO management, China Delivery, Carton Packaging List (with optional dimensions), Category Freight Rates, Bangladesh Transport (6 methods), Additional Expenses, Partial Receiving with Pending Qty, Landing Cost Review & Finalization, Post-Finalize Cost Adjustment History, and Monthly Warehouse Rent tracking.
