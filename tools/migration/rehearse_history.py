#!/usr/bin/env python3
"""Build a disposable history merge from saved snapshots; never touches ADB/live data."""
import argparse
import csv
from datetime import datetime, timedelta, timezone
from decimal import Decimal
from collections import defaultdict
from contextlib import closing
import json
import os
from pathlib import Path
import sqlite3

from prepare_tuktak import (analyze, connect_readonly, counts, digest, identity, insert,
                            minor, normalized, preview_masters, rows, verify_preview)

RETAIL = "shoes insole retail"
DECISIONS = {"payment_policy": "DATED_CUSTOMER_LEDGER_NO_INVOICE_ALLOCATIONS",
             "retail_insole_current_stock": 0, "source_journal_policy": "PRESERVE_SOURCE_AND_REPORT_DIFFERENCES_NO_CORRECTIONS",
             "confirmed_on": "2026-11-23"}


def date_ledger(source):
    """Current source documents + real dated adjustments, no inferred opening rows."""
    ledger = []
    orders = {r["id"]: r for r in rows(source, "sales_orders") if not r["is_deleted"]}
    for row in orders.values():
        ledger.append(dict(id=identity("sales_orders", row["id"]), customer_id=row["customer_id"],
            date=row["orderDate"], kind="INVOICE", reference=row["order_number"],
            delta_minor=minor(row["total"], 100)))
    for row in rows(source, "payments"):
        if not row["is_deleted"]:
            ledger.append(dict(id=identity("payments", row["id"]), customer_id=row["customer_id"],
                date=row["paymentDate"], kind="PAYMENT", reference=row["reference"] or row["id"],
                delta_minor=-minor(row["amount"], 100)))
    for row in rows(source, "sales_returns"):
        if not row["is_deleted"]:
            ledger.append(dict(id=identity("sales_returns", row["id"]),
                customer_id=orders[row["sale_order_id"]]["customer_id"], date=row["return_date"],
                kind="RETURN", reference=row["return_number"], delta_minor=-minor(row["total"], 100)))
    for row in rows(source, "customer_ledger"):
        if not row["is_deleted"] and row["transaction_type"] == "ADJUSTMENT":
            ledger.append(dict(id=identity("customer_ledger", row["id"]), customer_id=row["customer_id"],
                date=row["transactionDate"], kind="ADJUSTMENT", reference=row["notes"],
                delta_minor=minor(row["amount"], 100)))
    balance = defaultdict(int)
    for customer in rows(source, "customers"):
        if customer["opening_balance"]:
            raise ValueError("Nonzero explicit source opening balance needs implementation")
    for event in sorted(ledger, key=lambda r: (r["date"], r["id"])):
        balance[event["customer_id"]] += event["delta_minor"]
        event["running_balance_minor"] = balance[event["customer_id"]]
    for customer in rows(source, "customers"):
        if balance[customer["id"]] != minor(customer["total_due"], 100):
            raise ValueError("Dated source ledger does not match customer balance: " + customer["id"])
    return sorted(ledger, key=lambda r: (r["date"], r["id"]))


def stock_projection(source):
    result = []
    for product in rows(source, "products"):
        variants = [v for v in rows(source, "product_variants")
                    if v["product_id"] == product["id"] and not v["is_deleted"]]
        batches = [b for b in rows(source, "batches") if b["product_id"] == product["id"] and not b["is_deleted"]]
        quantity = sum(b["remaining_quantity"] for b in batches)
        if quantity != product["current_stock"]:
            raise ValueError("Product and batch stock disagree")
        if normalized(product["name"]) == RETAIL:
            if quantity != 0:
                raise ValueError("Confirmed zero-stock product has changed; review new snapshot")
            quantity = 0
        elif variants and sum(v["current_stock"] for v in variants) != quantity:
            raise ValueError("Unresolved variant stock conflict")
        result.append({"source_product_id": product["id"], "name": product["name"], "quantity": quantity,
                       "variant_quantities": {v["id"]: 0 if normalized(product["name"]) == RETAIL
                                              else v["current_stock"] for v in variants}})
    return result


def accounting_differences(source):
    ar = source.execute("SELECT COALESCE(SUM(l.debit-l.credit),0) FROM journal_entry_lines l "
        "JOIN journal_entries j ON j.id=l.journal_entry_id JOIN accounts a ON a.id=l.account_id "
        "WHERE j.status='POSTED' AND a.account_class='RECEIVABLE'").fetchone()[0]
    due = source.execute("SELECT SUM(total_due) FROM customers").fetchone()[0]
    adjustment = source.execute("SELECT COALESCE(SUM(amount),0) FROM customer_ledger "
        "WHERE transaction_type='ADJUSTMENT' AND is_deleted=0").fetchone()[0]
    inventory = source.execute("SELECT COALESCE(SUM(l.debit-l.credit),0) FROM journal_entry_lines l "
        "JOIN journal_entries j ON j.id=l.journal_entry_id JOIN accounts a ON a.id=l.account_id "
        "WHERE j.status='POSTED' AND a.account_class='INVENTORY'").fetchone()[0]
    actual_value = source.execute("SELECT SUM(remaining_quantity*purchasePrice) FROM batches WHERE is_deleted=0").fetchone()[0]
    return {"source_journal_receivable_major": ar, "customer_master_due_major": due,
            "source_ledger_adjustment_major": adjustment,
            "remaining_ar_difference_after_adjustment_major": ar+adjustment-due,
            "source_journal_inventory_major": inventory, "remaining_batch_value_major": actual_value,
            "inventory_difference_major": inventory-actual_value}


def export_ledger(source, events, path):
    names = {r["id"]: r["name"] for r in rows(source,"customers")}
    def text(value):
        value = str(value)
        return "'"+value if value.startswith(("=", "+", "-", "@", "\t", "\r")) else value
    def taka(value):
        return format(Decimal(value)/100, ".2f")
    with path.open("w",encoding="utf-8-sig",newline="") as output:
        writer=csv.writer(output)
        writer.writerow(["Customer", "Date (Asia/Dhaka)", "Entry", "Reference", "Debit BDT", "Credit BDT", "Balance BDT"])
        for event in events:
            delta=event["delta_minor"]
            writer.writerow([text(names[event["customer_id"]]),
                datetime.fromtimestamp(event["date"]/1000,timezone(timedelta(hours=6))).isoformat(),
                event["kind"], text(event["reference"]), taka(max(delta,0)), taka(max(-delta,0)),
                taka(event["running_balance_minor"])])


def merge(source, db, media, output):
    """Append records in one transaction. Normal posting APIs must not replay history."""
    mapping = []
    def add(table, source_table, sid, **fields):
        tid = identity(source_table, sid)
        insert(db, table, dict(id=tid, **fields))
        mapping.append(dict(target_table=table, source_table=source_table, source_id=sid, target_id=tid))
        return tid
    def ref(table, sid):
        return identity(table, sid) if sid else None
    def money(value):
        return minor(value, 100)
    def note(row):
        return "Tuktak source record: " + json.dumps(row, ensure_ascii=False, separators=(",", ":"))
    customers = {r["id"]: r for r in rows(source, "customers")}
    products = {r["id"]: r for r in rows(source, "products")}
    orders = {r["id"]: r for r in rows(source, "sales_orders")}
    purchases = {r["id"]: r for r in rows(source, "purchase_orders")}
    items = {r["id"]: r for r in rows(source, "sales_order_items")}
    accounts = {r["id"]: r for r in rows(source, "accounts")}
    batches = {r["id"]: r for r in rows(source, "batches")}
    source_allocations = rows(source, "sales_batch_allocations")
    all_returns = rows(source, "sales_returns")
    return_items = rows(source, "sales_return_items")
    # Historical source sales have no warehouse or variant attribution. Preserve
    # that distinction in a dedicated logical source warehouse, rather than guess.
    warehouse = add("warehouses", "warehouses", "unassigned", name="Tuktak Legacy Stock",
        code="TTK-LEGACY", location="Imported source had no warehouse attribution", isDefault=0, isActive=1)
    for row in accounts.values():
        kind = "REVENUE" if row["account_type"] == "INCOME" else row["account_type"]
        subtype = {"CASH": "CASH", "BANK": "BANK", "BKASH": "MOBILE_BANKING", "NAGAD": "MOBILE_BANKING"}.get(row["account_class"])
        add("chart_of_accounts", "accounts", row["id"], accountCode="TTK-"+row["code"],
            nameEn=row["name"], nameBn=row["name"], accountType=kind,
            normalBalance="DEBIT" if kind in ("ASSET", "EXPENSE") else "CREDIT",
            description=note(row), isActive=int(bool(row["is_active"]) and not row["is_deleted"]),
            accountSubType=subtype, institutionName=None, branchName=None, accountNumber=None)
    cash = next(r for r in accounts.values() if r["account_class"] == "CASH")
    for row in purchases.values():
        paid = sum(p["amount"] for p in rows(source, "purchase_payments")
                   if p["purchase_order_id"] == row["id"] and not p["is_deleted"])
        if row["is_deleted"] or row["purchase_status"] != "RECEIVED":
            raise ValueError("Unreviewed purchase status")
        add("purchase_orders", "purchase_orders", row["id"], poNumber="TTK-"+row["purchase_number"],
            supplierId=ref("suppliers", row["supplier_id"]), status="RECEIVED", currency="BDT",
            exchangeRateToBdt="1", totalAmountMinor=money(row["total"]), totalBdtAmountMinor=money(row["total"]),
            paidAmountMinor=money(paid), paidBdtAmountMinor=money(paid), orderDate=row["purchaseDate"],
            expectedArrivalDate=None, notes=note(row), isActualCostFinalized=1, actualCostFinalizedAt=row["purchaseDate"],
            shippingRatePerKgBdt="0", totalGrossWeightKg="0", totalNetWeightKg="0", totalCbm="0", totalCartons=0,
            attachments=None, createdAt=row["created_at"], updatedAt=row["updated_at"], shipmentId=None)
    for row in rows(source, "purchase_order_items"):
        add("purchase_order_items", "purchase_order_items", row["id"], purchaseOrderId=ref("purchase_orders", row["purchase_order_id"]),
            productId=ref("products", row["product_id"]), variantId=ref("product_variants", row["variantId"]),
            quantity=row["quantity"], unitPriceMinor=money(row["purchasePrice"]), totalAmountMinor=money(row["total"]),
            totalBdtAmountMinor=money(row["total"]), receivedQuantity=row["receivedQuantity"], cartonQuantity=0,
            pcsPerCarton=0, grossWeightKg="0", netWeightKg="0", cbm="0")
    for row in rows(source, "purchase_payments"):
        add("supplier_payments", "purchase_payments", row["id"], paymentNumber="TTK-SP-"+row["id"],
            supplierId=ref("suppliers", row["supplier_id"]), purchaseOrderId=ref("purchase_orders", row["purchase_order_id"]),
            amountMinor=money(row["amount"]), bdtAmountMinor=money(row["amount"]), currency="BDT", exchangeRateToBdt="1",
            paymentMethod=row["payment_method"], paymentAccount=identity("accounts", cash["id"]),
            referenceNumber=row["reference"], notes=note(row), paymentDate=row["paymentDate"], createdAt=row["created_at"])
    for row in orders.values():
        if row["is_deleted"] or row["order_status"] not in ("CONFIRMED", "DELIVERED"):
            raise ValueError("Unreviewed source sale status")
        returned = sum(r["total"] for r in all_returns if r["sale_order_id"] == row["id"] and not r["is_deleted"])
        cogs = sum(a["quantity"] * a["purchase_price"] for a in source_allocations if a["sale_order_id"] == row["id"])
        add("sales_invoices", "sales_orders", row["id"], invoiceNumber="TTK-"+row["order_number"],
            customerId=ref("customers", row["customer_id"]), customerName=row["customer_name"], customerPhone=row["customer_phone"],
            warehouseId=warehouse, warehouseName="Tuktak Legacy Stock", status=row["order_status"],
            subtotalMinor=money(row["subtotal"]), discountMinor=money(row["discountAmount"]),
            taxMinor=money(row["vatAmount"]+row["shippingCharge"]), totalAmountMinor=money(row["total"]),
            paidAmountMinor=0, dueAmountMinor=money(max(0, row["total"]-returned)), totalCogsMinor=money(cogs),
            invoiceDate=row["orderDate"], dueDate=None, notes=note(row), isPosted=1, postedAt=row["created_at"],
            createdAt=row["created_at"], updatedAt=row["updated_at"], cancellationReason=None, cancelledAt=None)
    for row in items.values():
        allocations = [a for a in source_allocations if a["sale_item_id"] == row["id"]]
        if sum(a["quantity"] for a in allocations) != row["quantity"]:
            raise ValueError("Historical FIFO allocation does not match sale quantity")
        returned = sum(r["quantity"] for r in return_items if r["sale_item_id"] == row["id"])
        total_cost = money(sum(a["quantity"]*a["purchase_price"] for a in allocations))
        add("sales_invoice_items", "sales_order_items", row["id"], invoiceId=ref("sales_orders", row["order_id"]),
            productId=ref("products", row["product_id"]), variantId=ref("product_variants", row["variantId"]),
            productNameEn=row["productName"], productNameBn=row["productName"], sku="TTK-"+row["productSku"],
            quantity=row["quantity"], returnedQuantity=returned, unitPriceMinor=money(row["unitPrice"]),
            discountMinor=money(row["discountAmount"]), lineTotalMinor=money(row["total"]),
            unitCostMinor=money(row["unit_cost"]), totalCogsMinor=total_cost)
    for row in rows(source, "payments"):
        if row["order_id"]:
            raise ValueError("Source has bill-linked payment; user chose customer-ledger policy")
        if row["payment_method"] != "CASH" or row["is_deleted"]:
            raise ValueError("Unreviewed customer payment method or deletion")
        add("customer_payments", "payments", row["id"], paymentNumber="TTK-CP-"+row["id"],
            customerId=ref("customers", row["customer_id"]), customerName=customers[row["customer_id"]]["name"],
            amountMinor=money(row["amount"]), allocatedAmountMinor=0, unallocatedCreditMinor=money(row["amount"]),
            paymentDate=row["paymentDate"], paymentMethod=row["payment_method"], accountId=identity("accounts", cash["id"]),
            referenceNote=note(row), isAdvance=1, isPosted=1, isVoided=0, voidedAt=None, voidReason=None,
            voidedBy=None, createdAt=row["created_at"])
    for row in all_returns:
        order = orders[row["sale_order_id"]]
        returned = [r for r in return_items if r["return_id"] == row["id"]]
        cogs = sum(r["quantity"]*items[r["sale_item_id"]]["unit_cost"] for r in returned)
        add("sales_returns", "sales_returns", row["id"], returnNumber="TTK-"+row["return_number"],
            invoiceId=ref("sales_orders", row["sale_order_id"]), invoiceNumber="TTK-"+order["order_number"],
            customerId=ref("customers", order["customer_id"]), customerName=order["customer_name"], warehouseId=warehouse,
            totalReturnAmountMinor=money(row["total"]), totalCogsReversalMinor=money(cogs),
            refundOption="CUSTOMER_CREDIT", reason=row["reason"], returnDate=row["return_date"], isPosted=1, createdAt=row["created_at"])
    for row in return_items:
        original = items[row["sale_item_id"]]
        cost = money(original["unit_cost"])
        add("sales_return_items", "sales_return_items", row["id"], returnId=ref("sales_returns", row["return_id"]),
            invoiceItemId=ref("sales_order_items", row["sale_item_id"]), productId=ref("products", row["product_id"]),
            variantId=ref("product_variants", original["variantId"]), productNameEn=row["product_name"], productNameBn=row["product_name"],
            returnQuantity=row["quantity"], unitPriceMinor=money(row["unit_price"]), returnTotalMinor=money(row["total"]),
            originalUnitCostMinor=cost, cogsReversalMinor=cost*row["quantity"])
    for row in batches.values():
        purchase_id = row["reference_id"] if row["reference_id"] in purchases else None
        # Cost-layer link fields have no FK; unknown shipment/receipt identifiers
        # remain explicitly empty rather than fabricating logistics history.
        add("inventory_cost_layers", "batches", row["id"], productId=ref("products", row["product_id"]),
            variantId=None, warehouseId=warehouse, shipmentId="", shipmentNumber="TTK-"+row["batch_number"],
            warehouseReceiptId="", receiptNumber="TTK-"+row["batch_number"], initialQuantity=row["quantity"],
            remainingQuantity=row["remaining_quantity"], unitLandedCostBdtMinor=money(row["purchasePrice"]),
            originalCurrency="BDT", originalPurchasePriceMinor=money(row["purchasePrice"]), exchangeRateToBdt="1",
            allocatedExpensePerUnitBdtMinor=0, receiptDate=row["purchase_date"], isActive=1,
            purchaseOrderId=ref("purchase_orders", purchase_id), purchaseBatchId=ref("batches", row["id"]), createdAt=row["created_at"])
    for row in source_allocations:
        item = items[row["sale_item_id"]]
        add("sales_cost_layer_usages", "sales_batch_allocations", row["id"], invoiceId=ref("sales_orders", row["sale_order_id"]),
            invoiceItemId=ref("sales_order_items", row["sale_item_id"]), costLayerId=ref("batches", row["batch_id"]),
            quantityDepleted=row["quantity"], unitCostMinor=money(row["purchase_price"]))
        add("fifo_consumptions", "fifo", row["id"], invoiceId=ref("sales_orders", row["sale_order_id"]),
            invoiceItemId=ref("sales_order_items", row["sale_item_id"]), costLayerId=ref("batches", row["batch_id"]),
            productId=ref("products", row["product_id"]), variantId=ref("product_variants", item["variantId"]),
            warehouseId=warehouse, quantity=row["quantity"]-row["returned_quantity"], unitCostBdtMinor=money(row["purchase_price"]),
            totalCogsBdtMinor=money(row["purchase_price"]*(row["quantity"]-row["returned_quantity"])),
            referenceType="SALE", referenceId=ref("sales_orders", row["sale_order_id"]), createdAt=row["created_at"])
    for record in stock_projection(source):
        product = products[record["source_product_id"]]
        for variant, quantity in [(None, record["quantity"])] + list(record["variant_quantities"].items()):
            add("stock_balances", "stock_balances", product["id"]+":"+(variant or "base"), warehouseId=warehouse,
                productId=ref("products", product["id"]), variantId=ref("product_variants", variant), quantity=quantity,
                reservedQuantity=0, updatedAt=product["updated_at"])
    movement_types = {"PURCHASE": "PURCHASE_RECEIPT", "SALE": "SALE_DISPATCH", "RETURN_IN": "SALE_RETURN"}
    for row in rows(source, "stock_movements"):
        kind = movement_types.get(row["movement_type"])
        if row["movement_type"] == "ADJUSTMENT":
            kind = "STOCK_ADJUSTMENT_IN" if row["quantity"] >= 0 else "STOCK_ADJUSTMENT_OUT"
        if not kind or row["afterStock"]-row["beforeStock"] != row["quantity"]:
            raise ValueError("Unreviewed stock movement")
        source_table = {"SALES_ORDER": "sales_orders", "PURCHASE_ORDER": "purchase_orders", "SALES_RETURN": "sales_returns"}.get(row["referenceType"], "inventory_adjustments")
        add("stock_movements", "stock_movements", row["id"], movementNumber="TTK-MOV-"+row["id"],
            warehouseId=warehouse, productId=ref("products", row["product_id"]), variantId=ref("product_variants", row["variant_id"]),
            movementType=kind, quantity=abs(row["quantity"]), unitCostMinor=money(row["unit_cost"]),
            balanceAfterMovement=row["afterStock"], totalCostImpactMinor=money(abs(row["quantity"])*row["unit_cost"]),
            referenceType=row["referenceType"], referenceId=ref(source_table, row["reference_id"]),
            purchaseOrderId=ref("purchase_orders", row["reference_id"]) if row["referenceType"]=="PURCHASE_ORDER" else None,
            purchaseBatchId=None, salesInvoiceId=ref("sales_orders", row["reference_id"]) if row["referenceType"]=="SALES_ORDER" else None,
            stockTransferId=None, operatorName=row["userName"], notes=note(row), createdAt=row["created_at"])
    journal_lines = rows(source, "journal_entry_lines")
    for row in rows(source, "journal_entries"):
        lines = [l for l in journal_lines if l["journal_entry_id"] == row["id"]]
        debit, credit = sum(l["debit"] for l in lines), sum(l["credit"] for l in lines)
        if debit != credit or row["status"] not in ("POSTED", "CANCELLED"):
            raise ValueError("Unbalanced/unreviewed source journal")
        add("journal_entries", "journal_entries", row["id"], entryNumber="TTK-"+row["entry_number"],
            entryDate=row["entry_date"], descriptionEn=row["description"], descriptionBn=row["description"],
            referenceType="TUKTAK_"+row["reference_type"], referenceId=row["reference_id"],
            totalDebitMinor=money(debit), totalCreditMinor=money(credit), isPosted=int(row["status"]=="POSTED"),
            isReversed=0, reversalOfEntryId=None, reversalEntryId=None, createdAt=row["created_at"])
    for row in journal_lines:
        account = accounts[row["account_id"]]
        if row["debit"] and row["credit"]:
            raise ValueError("Journal line has both debit and credit")
        add("journal_lines", "journal_entry_lines", row["id"], journalEntryId=ref("journal_entries", row["journal_entry_id"]),
            accountId=ref("accounts", row["account_id"]), accountCode="TTK-"+account["code"], accountName=account["name"],
            isDebit=int(row["debit"]>0), amountMinor=money(row["debit"]+row["credit"]), description=row["description"])
    # The source adjustment explicitly says FIRST ORDER DISCOUNT. Its journal is
    # absent from source; represent the dated ledger event with a balanced entry
    # and an explicit legacy discount account, not a made-up cash receipt.
    adjustments = [r for r in rows(source, "customer_ledger") if not r["is_deleted"] and r["transaction_type"]=="ADJUSTMENT"]
    if adjustments:
        discount_id = add("chart_of_accounts", "accounts", "ledger-discount", accountCode="TTK-LEDGER-DISCOUNT",
            nameEn="Tuktak Legacy Sales Discounts", nameBn="Tuktak Legacy Sales Discounts", accountType="REVENUE",
            normalBalance="DEBIT", description="Source customer-ledger discounts without source journals",
            isActive=1, accountSubType=None, institutionName=None, branchName=None, accountNumber=None)
        receivable = next(r for r in accounts.values() if r["account_class"]=="RECEIVABLE")
        for row in adjustments:
            if row["amount"] >= 0 or normalized(row["notes"]) != "first order discount":
                raise ValueError("Unreviewed source ledger adjustment")
            amount = -money(row["amount"])
            entry = add("journal_entries", "customer_ledger", row["id"], entryNumber="TTK-ADJ-"+row["id"],
                entryDate=row["transactionDate"], descriptionEn=row["notes"], descriptionBn=row["notes"],
                referenceType="CUSTOMER_LEDGER_CREDIT_ADJUSTMENT", referenceId=ref("customers", row["customer_id"]),
                totalDebitMinor=amount, totalCreditMinor=amount, isPosted=1, isReversed=0,
                reversalOfEntryId=None, reversalEntryId=None, createdAt=row["created_at"])
            for suffix, aid, code, name, debit in [("discount", discount_id,"TTK-LEDGER-DISCOUNT","Tuktak Legacy Sales Discounts",1),
                ("receivable",ref("accounts",receivable["id"]),"TTK-"+receivable["code"],receivable["name"],0)]:
                add("journal_lines", "adjustment_lines", row["id"]+":"+suffix, journalEntryId=entry,
                    accountId=aid, accountCode=code, accountName=name, isDebit=debit, amountMinor=amount, description=note(row))
    assets = {r["source_path"]: r for r in media}
    for row in rows(source, "product_images"):
        asset = assets.get(row["image_url"])
        if not asset:
            raise ValueError("Source product image not archived")
        # Candidate paths are local only; production copying/remapping still required.
        add("product_images", "product_images", row["id"], productId=ref("products",row["product_id"]),
            variantId=None, imageUri=str(output/asset["local_path"]), isPrimary=row["is_primary"],
            sortOrder=row["sort_order"], createdAt=row["created_at"])
    return mapping


def run(preparation, output):
    os.umask(0o077)
    preparation, output = preparation.resolve(), output.resolve()
    repo = Path(__file__).resolve().parents[2]
    if output == repo or repo in output.parents:
        raise ValueError("Keep business data outside Git")
    report = json.loads((preparation/"report.json").read_text())
    for filename, expected in report["snapshot_sha256"].items():
        if digest(preparation/filename) != expected:
            raise ValueError("Snapshot checksum mismatch")
    for asset in report["media"]:
        if digest(preparation/asset["local_path"]) != asset["sha256"]:
            raise ValueError("Asset checksum mismatch")
    output.mkdir(parents=True, exist_ok=False)
    candidate = output/"history-preview-not-for-restore.db"
    with connect_readonly(preparation/"tuktak.db") as source, connect_readonly(preparation/"advance-before.db") as baseline:
        analysis = analyze(source, baseline)
        if analysis["possible_duplicates"]:
            raise ValueError("Duplicate identities require review")
        events = date_ledger(source)
        stock = stock_projection(source)
        mapping = preview_masters(source, preparation/"advance-before.db", candidate, 100)
        with closing(sqlite3.connect(candidate)) as db, db:
            db.execute("PRAGMA foreign_keys=ON")
            mapping.extend(merge(source, db, report["media"], preparation))
            if db.execute("PRAGMA foreign_key_check").fetchall():
                raise ValueError("History preview foreign key check failed")
            if db.execute("PRAGMA quick_check").fetchall() != [("ok",)]:
                raise ValueError("History preview integrity failed")
            if db.execute("SELECT COUNT(*) FROM payment_allocations WHERE id LIKE 'ttk-%'").fetchone()[0]:
                raise ValueError("User prohibited invoice allocations")
        validation = verify_preview(preparation/"advance-before.db", candidate, mapping)
        validation.pop("non_master_tables_unchanged")
        validation["only_expected_history_rows_added"] = True
        with connect_readonly(candidate) as staged:
            validation["dated_history"] = validate_history(source, staged)
        export_ledger(source, events, output/"customer-ledger-preview.csv")
        result = dict(live_import_ready=False, decisions=DECISIONS, validation=validation,
            ledger_events=events, stock=stock, id_mapping=mapping,
            source_accounting_differences=accounting_differences(source),
            remaining_limitations=[
                "Native Room/UI validation of this candidate remains before any live import",
                "Source journal AR, historical stock audit and current subledger differ; no balancing corrections invented",
                "Source account chart retained separately; integration with target standard-account reports needs review",
                "Some historical audit references point to removed source sales; source archive retains all original records",
                "Shipment/receipt details absent in source cannot be reconstructed; cost layers retain source batch numbers",
                "Media uses local rehearsal paths; native import must copy assets to app-private storage",
                "Source timelines, source settings/users and unsupported audit metadata remain in full source archive only",
            ])
    (output/"history-report.json").write_text(json.dumps(result, ensure_ascii=False, indent=2)+"\n")
    (output/"READ-ME.txt").write_text("DISPOSABLE HISTORY REHEARSAL. DO NOT RESTORE TO THE LIVE APP.\n"
        "Customer payments are dated ledger credits, with zero invoice allocations.\n"
        "Retail insole stock is zero as confirmed by the user.\n"
        "Original snapshots remain in "+str(preparation)+"\n")
    print(json.dumps(dict(output=str(output), ledger_events=len(events),
        mapped_records=len(mapping), existing_rows_unchanged=True, live_import_ready=False), indent=2))


def validate_history(source, staged):
    for source_table, target_table, source_date, target_date, source_amount, target_amount in [
        ("sales_orders", "sales_invoices", "orderDate", "invoiceDate", "total", "totalAmountMinor"),
        ("payments", "customer_payments", "paymentDate", "paymentDate", "amount", "amountMinor"),
        ("purchase_orders", "purchase_orders", "purchaseDate", "orderDate", "total", "totalAmountMinor"),
        ("purchase_payments", "supplier_payments", "paymentDate", "paymentDate", "amount", "amountMinor"),
        ("sales_returns", "sales_returns", "return_date", "returnDate", "total", "totalReturnAmountMinor"),
    ]:
        for row in rows(source, source_table):
            imported = staged.execute("SELECT * FROM "+target_table+" WHERE id=?",
                                      (identity(source_table, row["id"]),)).fetchone()
            if imported is None or imported[target_date] != row[source_date] or imported[target_amount] != minor(row[source_amount],100):
                raise ValueError("History date/amount validation failed for "+source_table)
    for customer in rows(source,"customers"):
        cid=identity("customers",customer["id"])
        net=staged.execute("SELECT COALESCE(SUM(totalAmountMinor),0) FROM sales_invoices WHERE customerId=? AND isPosted=1",(cid,)).fetchone()[0]
        net-=staged.execute("SELECT COALESCE(SUM(amountMinor),0) FROM customer_payments WHERE customerId=? AND isPosted=1",(cid,)).fetchone()[0]
        net-=staged.execute("SELECT COALESCE(SUM(totalReturnAmountMinor),0) FROM sales_returns WHERE customerId=? AND isPosted=1",(cid,)).fetchone()[0]
        net-=staged.execute("SELECT COALESCE(SUM(totalDebitMinor),0) FROM journal_entries WHERE referenceId=? AND referenceType='CUSTOMER_LEDGER_CREDIT_ADJUSTMENT' AND isPosted=1",(cid,)).fetchone()[0]
        if net != minor(customer["total_due"],100):
            raise ValueError("Imported customer ledger failed reconciliation")
    for stock in stock_projection(source):
        quantity=staged.execute("SELECT SUM(quantity) FROM stock_balances WHERE productId=?",
                              (identity("products",stock["source_product_id"]),)).fetchone()[0]
        if quantity != stock["quantity"]:
            raise ValueError("Imported current stock failed reconciliation")
    if staged.execute("SELECT COUNT(*) FROM (SELECT journalEntryId FROM journal_lines WHERE id LIKE 'ttk-%' GROUP BY journalEntryId HAVING SUM(CASE WHEN isDebit=1 THEN amountMinor ELSE -amountMinor END)!=0)").fetchone()[0]:
        raise ValueError("Imported journal is not balanced")
    return {"all_source_document_dates_and_amounts_preserved":True,
            "all_customer_balances_match":True,"confirmed_current_stock_matches":True,
            "all_imported_journals_balance":True,"invoice_allocations_created":0}


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--preparation", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    run(args.preparation, args.output)
