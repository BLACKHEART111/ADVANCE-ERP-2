#!/usr/bin/env python3
"""Local-only migration preparation. Never writes to either installed app.

Creates coherent SQLite snapshots, an exact source archive, a master-data preview,
and a reconciliation report. Transaction history is retained in the source archive;
it is deliberately NOT posted into the preview while reconciliation is unresolved.
Python 3 standard library only. No root, restore, APK installation, or adb writes.
"""
import argparse
from contextlib import closing
import hashlib
import json
import os
from pathlib import Path
import sqlite3
import subprocess
import tempfile
import unicodedata
import uuid


def adb(*args):
    result = subprocess.run(["adb", *args], capture_output=True, check=True)
    return result.stdout


def quote(name):
    return '"' + name.replace('"', '""') + '"'


def connect_readonly(path):
    db = sqlite3.connect(Path(path).resolve().as_uri() + "?mode=ro", uri=True)
    db.row_factory = sqlite3.Row
    db.execute("PRAGMA query_only=ON")
    return closing(db)


def snapshot(package, filename, output):
    """Accept only two identical consecutive db+WAL reads; replay WAL locally.

    The remote SHM file is never reused. quick_check is necessary but does not
    replace the stable-byte test. Fail rather than take a changing database.
    """
    with tempfile.TemporaryDirectory(prefix="erp-snapshot-", dir=os.environ["TMPDIR"]) as tmp:
        for _ in range(5):
            bundles = []
            for _ in range(2):
                bundles.append({suffix: adb("exec-out", "run-as", package, "cat",
                                            "databases/" + filename + suffix)
                                for suffix in ("", "-wal")})
            if bundles[0] == bundles[1]:
                break
        else:
            raise RuntimeError("Database is changing; retry when the app is idle")
        source = Path(tmp) / filename
        for suffix, data in bundles[0].items():
            Path(str(source) + suffix).write_bytes(data)
        with sqlite3.connect(source) as local, sqlite3.connect(output) as target:
            if local.execute("PRAGMA quick_check").fetchall() != [("ok",)]:
                raise RuntimeError("Source snapshot integrity check failed")
            local.backup(target)


def digest(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def rows(db, table):
    return [dict(r) for r in db.execute("SELECT * FROM " + quote(table))]


def counts(db):
    return {r[0]: db.execute("SELECT COUNT(*) FROM " + quote(r[0])).fetchone()[0]
            for r in db.execute("SELECT name FROM sqlite_master WHERE type='table' "
                                "AND name NOT LIKE 'sqlite_%' ORDER BY name")}


def normalized(value):
    return " ".join(unicodedata.normalize("NFKC", value or "").casefold().split())


def phone_key(value):
    value = "".join(str(unicodedata.decimal(c)) for c in
                    unicodedata.normalize("NFKC", value or "") if c.isdecimal())
    if value.startswith("00880"):
        value = "0" + value[5:]
    elif value.startswith("880"):
        value = "0" + value[3:]
    return value


def identity(table, source_id):
    # Stable across reruns; no collision with original UUIDs.
    return "ttk-" + str(uuid.uuid5(uuid.NAMESPACE_URL, "tuktak:" + table + ":" + source_id))


def minor(value, scale):
    if not isinstance(value, int):
        raise ValueError("Expected integer source money, got " + repr(value))
    result = value * scale
    if not -(2**63) <= result < 2**63:
        raise ValueError("Money exceeds SQLite signed 64-bit range")
    return result


def analyze(source, target):
    report = {"live_import_ready": False,
              "preview_scope": "Master records only. History remains in tuktak.db, not posted in preview.",
              "source_counts": counts(source), "target_counts_before": counts(target),
              "blockers": [], "possible_duplicates": [], "stock_conflicts": [],
              "customer_reconciliation": [], "history_mapping": {
                  "sales_orders + sales_order_items": "sales_invoices + sales_invoice_items",
                  "payments": "customer_payments; invoice allocations require an explicit policy",
                  "purchase_orders + purchase_order_items": "purchase_orders + purchase_order_items",
                  "purchase_payments": "supplier_payments",
                  "sales_returns + sales_return_items": "sales_returns + sales_return_items",
                  "batches + sales_batch_allocations": "inventory_cost_layers + FIFO/usage history",
                  "journal_entries + journal_entry_lines": "journal_entries + journal_lines; account mapping required",
                  "stock_movements": "stock_movements; direction, warehouse and variant reconciliation required",
                  "customer_ledger + supplier_ledger": "Preserve source ledger; reconcile adjustments and deleted parties",
              }}
    for label, db in (("source", source), ("target", target)):
        report[label + "_version"] = db.execute("PRAGMA user_version").fetchone()[0]
        report[label + "_quick_check"] = [r[0] for r in db.execute("PRAGMA quick_check")]
        report[label + "_foreign_key_errors"] = [list(r) for r in db.execute("PRAGMA foreign_key_check")]
        if report[label + "_quick_check"] != ["ok"] or report[label + "_foreign_key_errors"]:
            raise ValueError(label + " integrity validation failed")
    if (report["source_version"], report["target_version"]) != (33, 13):
        raise ValueError("Only inspected Tuktak v33 / Advance v13 schemas are supported")
    comparisons = [
        ("products", "products", "name", "nameEn", normalized),
        ("products", "products", "sku", "baseSku", normalized),
        ("product_variants", "product_variants", "sku", "sku", normalized),
        ("customers", "customers", "name", "name", normalized),
        ("customers", "customers", "phone", "phone", phone_key),
        ("suppliers", "suppliers", "name", "companyName", normalized),
        ("suppliers", "suppliers", "phone", "phone", phone_key),
    ]
    for source_table, target_table, source_field, target_field, key_fn in comparisons:
        existing = {}
        for row in rows(target, target_table):
            key = key_fn(row[target_field])
            if key:
                existing.setdefault(key, []).append(row["id"])
        for row in rows(source, source_table):
            key = key_fn(row[source_field])
            if key and key in existing:
                report["possible_duplicates"].append({"source_table": source_table,
                    "source_id": row["id"], "field": source_field,
                    "target_ids": existing[key]})
    for product in rows(source, "products"):
        variants = [v for v in rows(source, "product_variants") if v["product_id"] == product["id"]]
        batches = [b for b in rows(source, "batches") if b["product_id"] == product["id"]]
        batch_qty = sum(b["remaining_quantity"] for b in batches if not b["is_deleted"])
        variant_qty = sum(v["current_stock"] for v in variants if not v["is_deleted"])
        if product["current_stock"] != batch_qty or (variants and variant_qty != product["current_stock"]):
            report["stock_conflicts"].append({"product_id": product["id"], "name": product["name"],
                "product_quantity": product["current_stock"], "batch_quantity": batch_qty,
                "variant_quantity": variant_qty})
    for customer in rows(source, "customers"):
        customer_id = customer["id"]
        def total(table, column, key):
            return source.execute("SELECT COALESCE(SUM(" + quote(column) + "),0) FROM " +
                quote(table) + " WHERE " + quote(key) + "=? AND is_deleted=0", (customer_id,)).fetchone()[0]
        sales = total("sales_orders", "total", "customer_id")
        payments = total("payments", "amount", "customer_id")
        returns = source.execute("SELECT COALESCE(SUM(r.total),0) FROM sales_returns r JOIN sales_orders o "
            "ON o.id=r.sale_order_id WHERE o.customer_id=? AND r.is_deleted=0", (customer_id,)).fetchone()[0]
        adjustment = source.execute("SELECT COALESCE(SUM(amount),0) FROM customer_ledger WHERE "
            "customer_id=? AND transaction_type='ADJUSTMENT' AND is_deleted=0", (customer_id,)).fetchone()[0]
        expected = customer["opening_balance"] + sales - payments - returns + adjustment
        report["customer_reconciliation"].append({"source_id": customer_id, "name": customer["name"],
            "archived": bool(customer["is_deleted"]), "sales_major": sales, "payments_major": payments,
            "returns_major": returns, "adjustment_major": adjustment,
            "expected_due_major": expected, "stored_due_major": customer["total_due"],
            "matches": expected == customer["total_due"]})
    unlinked = source.execute("SELECT COUNT(*) FROM payments WHERE is_deleted=0 AND "
                             "(order_id IS NULL OR order_id='')").fetchone()[0]
    report["unlinked_customer_payments"] = unlinked
    report["unbalanced_source_journals"] = source.execute("SELECT COUNT(*) FROM (SELECT journal_entry_id "
        "FROM journal_entry_lines GROUP BY journal_entry_id HAVING SUM(debit)!=SUM(credit))").fetchone()[0]
    if report["stock_conflicts"]:
        report["blockers"].append("Physical stock confirmation required for conflicting product/variant balances")
    if report["possible_duplicates"]:
        report["blockers"].append("Potential duplicate parties/products need review; no automatic merging")
    if any(not r["matches"] for r in report["customer_reconciliation"]):
        report["blockers"].append("Customer balances do not reconcile to transactions plus adjustments")
    if unlinked:
        report["blockers"].append("Invoice allocation policy required for unlinked customer payments")
    report["blockers"] += [
        "Account mapping and source cancelled/duplicate financial history need reconciliation",
        "Warehouse, FIFO variant attribution and historical return costing need verification",
        "Customer ledger adjustment must be represented explicitly; do not disguise as opening balance",
        "End-to-end history conversion and native Room/UI tests remain; preview is not a restore artifact",
    ]
    return report


def insert(db, table, record):
    fields = list(record)
    db.execute("INSERT INTO " + quote(table) + " (" + ",".join(map(quote, fields)) + ") VALUES (" +
               ",".join("?" for _ in fields) + ")", [record[k] for k in fields])


def preview_masters(source, target_path, output_path, scale):
    """Build a new disposable copy. No SQL updates/deletes of baseline records."""
    with connect_readonly(target_path) as baseline, sqlite3.connect(output_path) as out:
        baseline.backup(out)
    out = sqlite3.connect(output_path)
    out.execute("PRAGMA foreign_keys=ON")
    mapping = []
    def add(table, source_table, source_id, **fields):
        target_id = identity(source_table, source_id)
        insert(out, table, dict(id=target_id, **fields))
        mapping.append({"source_table": source_table, "source_id": source_id,
                        "target_table": table, "target_id": target_id})
    try:
        with out:
            categories = rows(source, "categories")
            for row in categories:
                name = row.get("name") or row.get("nameEn")
                if not name:
                    raise ValueError("Category name unavailable")
                add("product_categories", "categories", row["id"], nameEn=name, nameBn=name,
                    code="TTK-" + row["id"], description="Imported preview from Tuktak ERP",
                    defaultFreightRatePerKgBdtMinor=0)
            fallback = identity("categories", "uncategorized")
            insert(out, "product_categories", dict(id=fallback, nameEn="Tuktak Uncategorized",
                nameBn="Tuktak Uncategorized", code="TTK-UNCATEGORIZED", description=None,
                defaultFreightRatePerKgBdtMinor=0))
            category_ids = {r["id"] for r in categories}
            for row in rows(source, "products"):
                add("products", "products", row["id"],
                    categoryId=identity("categories", row["category_id"]) if row["category_id"] in category_ids else fallback,
                    nameEn=row["name"], nameBn=row["name"], baseSku="TTK-" + (row["sku"] or row["id"]),
                    barcode=row["barcode"], qrCode=None, description=row["description"] or row["notes"],
                    unit=row["unit"], purchasePriceMinor=minor(row["purchasePrice"], scale),
                    sellingPriceMinor=minor(row["selling_price"], scale), minStockAlert=row["reorder_level"],
                    isActive=int(bool(row["is_active"]) and not row["is_deleted"]),
                    createdAt=row["created_at"], updatedAt=row["updated_at"])
            for row in rows(source, "product_variants"):
                add("product_variants", "product_variants", row["id"], productId=identity("products", row["product_id"]),
                    sku="TTK-" + (row["sku"] or row["id"]), color=row["color"], size=row["size"],
                    variantPriceMinor=minor(row["sellingPrice"], scale), variantCostMinor=minor(row["purchasePrice"], scale),
                    barcode=row["barcode"], qrCode=None,
                    isActive=int(bool(row["is_active"]) and not row["is_deleted"]))
            for row in rows(source, "customers"):
                add("customers", "customers", row["id"], customerCode="TTK-" + (row["customer_code"] or row["id"]),
                    name=row["name"], phone=row["phone"] or "", email=row["email"], address=row["address"] or "",
                    creditLimitMinor=minor(row["credit_limit"], scale), outstandingBalanceMinor=minor(row["total_due"], scale),
                    isActive=int(bool(row["is_active"]) and not row["is_deleted"]),
                    createdAt=row["created_at"], profileImagePath=None)
            for row in rows(source, "suppliers"):
                add("suppliers", "suppliers", row["id"], supplierCode="TTK-" + (row["supplier_code"] or row["id"]),
                    companyName=row["company_name"] or row["name"], contactPerson=row["contactPerson"],
                    phone=row["phone"] or "", email=row["email"], address=row["address"] or "",
                    country="", currency="BDT", outstandingPayableMinor=minor(row["total_due"], scale),
                    isActive=int(bool(row["is_active"]) and not row["is_deleted"]), createdAt=row["created_at"])
            if out.execute("PRAGMA foreign_key_check").fetchall():
                raise ValueError("Preview foreign key validation failed")
        if out.execute("PRAGMA quick_check").fetchall() != [("ok",)]:
            raise ValueError("Preview integrity failed")
    finally:
        out.close()
    return mapping


def verify_preview(baseline_path, preview_path, mapping):
    """Every preexisting row and every non-master table must be identical."""
    additions = {}
    for entry in mapping:
        additions.setdefault(entry["target_table"], set()).add(entry["target_id"])
    additions.setdefault("product_categories", set()).add(identity("categories", "uncategorized"))
    with connect_readonly(baseline_path) as baseline, connect_readonly(preview_path) as preview:
        schema = lambda db: [tuple(r) for r in db.execute(
            "SELECT type,name,tbl_name,sql FROM sqlite_master ORDER BY type,name")]
        if schema(baseline) != schema(preview):
            raise ValueError("Preview changed the Room schema")
        if baseline.execute("PRAGMA user_version").fetchone()[0] != preview.execute("PRAGMA user_version").fetchone()[0]:
            raise ValueError("Preview changed the Room version")
        for table, original_count in counts(baseline).items():
            expected_ids = additions.get(table, set())
            old_rows = {tuple(r) for r in baseline.execute("SELECT * FROM " + quote(table))}
            new_rows = {tuple(r) for r in preview.execute("SELECT * FROM " + quote(table))}
            if not old_rows <= new_rows or len(new_rows) != original_count + len(expected_ids):
                raise ValueError("Baseline preservation failed for " + table)
            if expected_ids:
                old_ids = {r[0] for r in baseline.execute("SELECT id FROM " + quote(table))}
                new_ids = {r[0] for r in preview.execute("SELECT id FROM " + quote(table))}
                if new_ids - old_ids != expected_ids:
                    raise ValueError("Unexpected added master IDs in " + table)
        return {"existing_rows_unchanged": True, "room_schema_unchanged": True,
                "non_master_tables_unchanged": True, "counts_after": counts(preview)}


def export_media(output):
    # Copy only known asset directories. No settings, tokens or preferences.
    directory = output / "tuktak-media"
    directory.mkdir()
    manifest = []
    for folder in ("product_images", "business_logo", "purchase_attachments"):
        listing = adb("exec-out", "run-as", "com.tuktak.erp", "find", "files/" + folder,
                      "-type", "f").decode().splitlines()
        for name in listing:
            relative = Path(name)
            if relative.is_absolute() or ".." in relative.parts or not name.startswith("files/" + folder + "/"):
                raise ValueError("Unsafe media path")
            destination = directory / Path(*relative.parts[1:])
            destination.parent.mkdir(parents=True, exist_ok=True)
            destination.write_bytes(adb("exec-out", "run-as", "com.tuktak.erp", "cat", name))
            manifest.append({"source_path": "/data/user/0/com.tuktak.erp/" + name,
                "local_path": str(destination.relative_to(output)), "sha256": digest(destination)})
    return manifest


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", required=True, type=Path, help="NEW private directory outside the repository")
    parser.add_argument("--source-money-scale", type=int, required=True, choices=[100],
                        help="Confirmed Tuktak major BDT -> Advance poisha multiplier")
    args = parser.parse_args()
    os.umask(0o077)
    output = args.output.resolve()
    repo = Path(__file__).resolve().parents[2]
    if output == repo or repo in output.parents:
        parser.error("Business data must be stored outside the Git checkout")
    output.mkdir(parents=True, exist_ok=False)
    source_path, target_path = output / "tuktak.db", output / "advance-before.db"
    snapshot("com.tuktak.erp", "tuktak_erp.db", source_path)
    snapshot("com.pocketdev.advance_erp2", "advance_erp.db", target_path)
    before_hashes = {str(p.name): digest(p) for p in (source_path, target_path)}
    with connect_readonly(source_path) as source, connect_readonly(target_path) as target:
        report = analyze(source, target)
        report["money_scale"] = args.source_money_scale
        report["id_mapping"] = preview_masters(source, target_path,
            output / "masters-preview-not-for-restore.db", args.source_money_scale)
    report["preview_validation"] = verify_preview(target_path,
        output / "masters-preview-not-for-restore.db", report["id_mapping"])
    report["media"] = export_media(output)
    with connect_readonly(source_path) as source:
        archived_paths = {r["source_path"] for r in report["media"]}
        report["missing_product_images"] = [r["image_url"] for r in rows(source, "product_images")
            if r["image_url"] and r["image_url"] not in archived_paths]
    if report["missing_product_images"]:
        report["blockers"].append("Some source product image references could not be archived")
    report["snapshot_sha256"] = before_hashes
    if any(digest(output / name) != expected for name, expected in before_hashes.items()):
        raise RuntimeError("Input snapshot unexpectedly modified")
    (output / "report.json").write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n")
    (output / "READ-ME.txt").write_text(
        "PRIVATE BUSINESS DATA. Keep this directory outside Git.\n"
        "tuktak.db: complete SQLite source archive including all history.\n"
        "advance-before.db: coherent baseline snapshot, not a complete app/media backup.\n"
        "masters-preview-not-for-restore.db: DISPOSABLE MASTER-ONLY PREVIEW.\n"
        "DO NOT restore/install it: no historical posting, stock or payment allocation.\n"
        "report.json contains the blocking reconciliation issues and stable ID map.\n")
    print(json.dumps({"output": str(output), "live_import_ready": False,
                      "master_records_staged": len(report["id_mapping"]),
                      "blockers": report["blockers"]}, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
