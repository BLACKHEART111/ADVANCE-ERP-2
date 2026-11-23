# Tuktak migration preparation — 2026-11-23

This is **preparation and isolated history rehearsal, not a production importer**. It reads the installed
Tuktak v33 and Advance v13 databases through non-root ADB `run-as`. There are no
commands to restore, clear, uninstall, push files, post transactions, or modify
either installed app. No Kotlin dependency or Room schema changes are required.

Run from the checkout with Python 3 and an already-authorized ADB device:

```sh
python tools/migration/prepare_tuktak.py \
  --output "$HOME/.local/share/advance-erp-migration/NEW-UNIQUE-DIRECTORY" \
  --source-money-scale 100
python -m unittest discover -s tools/migration -v
```

The output directory must be new and outside the checkout. Files are private
to the local user. They contain business data and must not be committed or
uploaded. Two consecutive identical database + WAL reads are required, then
SQLite's backup API consolidates WAL locally. This fails if either file is
missing or the database keeps changing. It does not stop either app.

Output includes complete `tuktak.db` history, `advance-before.db`, source assets,
SHA-256 checksums, source-to-target ID mappings, reconciliation results, and
`masters-preview-not-for-restore.db`. The preview adds categories, products,
variants, customers (including archived parties), and suppliers. It does **not**
post source sales, payments, stock, or journals. Balances/prices in that disposable
preview are converted from source major BDT to Advance minor units (x100), based
on source database values compared against the installed app's displayed dues.
All original target rows, non-master tables, schema and Room version are checked
for exact preservation. Deterministic IDs and prefixed SKUs/codes keep provenance;
possible duplicate names/phones/SKUs are reported, never automatically merged.

## Verified checkpoint

Actual-device preparation succeeded: 4 source categories, 6 products, 6 variants,
8 customers (2 archived), and 1 supplier were staged: 25 mapped records plus an
uncategorized fallback. Source history remains intact in the separate source
archive (32 sales, 61 sale items, 62 customer payments, 1 purchase, 11 purchase
items, 1 supplier payment, 1 return, 120 journals, and 108 stock movements).
No name/SKU/normalized-phone matches against the current target were detected.
These are checkpoint counts, not assertions for future runs.

Private artifacts are outside Git under:
`$HOME/.local/share/advance-erp-migration/2026-11-23-preparation-verified/`.
This is not a complete Advance app backup: target images/preferences are not
copied. Do not uninstall or restore the live app using these artifacts.

## Required before implementing full history import

- **Confirmed by user on 2026-11-23:** SHOES INSOLE RETAIL is out of stock (0).
  Ignore its stale 240 variant total; all six current variant balances are 0.
- **Confirmed by user on 2026-11-23:** receipts and invoices stay in the customer
  ledger by their own dates; no bill allocation or FIFO payment distribution.
  Preserve dates, totals and the -400 BDT customer credit.
- Explicitly represent the -240 BDT customer adjustment and 2800 BDT return;
  all 8 customer balances reconcile when these are included. Do not treat a
  returned amount as a missing payment or silently manufacture opening balances.
- Preserve archived customers referenced by four source sales. Purchase header
  paid=0 is stale: the separate supplier payment is 100000 BDT, leaving 267970.
- Reconcile source financial history: 111 posted and 9 cancelled journals;
  all source entries balance, but chart-of-account mapping, AR versus customer
  subledger, historical COGS and edited/cancelled sale references need scrutiny.
- Convert sale/purchase documents and returns with warehouse/FIFO/usage links,
  image-path remapping, movement directions, idempotency and rollback. Never
  replay normal posting APIs over imported journals or ending stock.
- Validate full history in an isolated Room database/native app fixture before
  any production merge. No signing changes, live instrumentation fixture writes,
  live restore or live database replacement have been performed.

## History rehearsal after the user's decisions

```sh
python tools/migration/rehearse_history.py \
  --preparation "$HOME/.local/share/advance-erp-migration/2026-11-23-preparation-verified" \
  --output "$HOME/.local/share/advance-erp-migration/NEW-HISTORY-DIRECTORY"
```

The complete source archive remains unchanged. A new disposable Advance v13 copy
receives current source documents, independent customer receipts, returns, purchase
payments, stock ending balances, batch cost layers, FIFO usages, stock audit movements,
source account chart/journals, and product-image references. 848 mapped records were
added at this checkpoint; baseline target rows and schema were preserved exactly.
Source log/settings/user/timeline tables without a target equivalent stay in the
source archive. No absent logistics documents or variant-to-batch attribution is
invented. Native movement quantities use positive magnitudes plus direction types;
the signed source movements remain verbatim in their notes and source archive.

There are **96 dated customer events**: 32 invoices, 62 payments, one return and one
explicit discount adjustment. Payments create no allocation rows; native invoice dues
remain document dues and customer net balance comes from the dated ledger. The
240 BDT source FIRST ORDER DISCOUNT becomes a balanced, dated, separately typed
customer adjustment journal, with a legacy discount account. Native Sales ledger and
Control Center projections read these entries; they are not receipts or opening due.
Room version remains 13. No migrations or dependency upgrades are involved.

Output includes `history-report.json`, `customer-ledger-preview.csv`, and
`history-preview-not-for-restore.db`. The latter is not suitable for restoring:
asset paths still point to local rehearsal files, legacy source accounts are separate,
and old source accounting inconsistencies remain explicit. Full operational row
coverage does not certify production/accounting readiness.

The source posted AR is 107245 BDT versus 100445 customer net due; after the real
-240 discount adjustment the unexplained AR excess is 6560: one duplicate sale
journal of 5760 and an orphaned sale journal of 800. Source inventory journal value
is 472754 versus remaining batches 3696; net unposted sales cost is 469058. These
figures are derived from the captured snapshot. No correction to these old financial
errors has been posted. The user declined extra correction entries on 2026-11-23:
retain original source journals and report the differences. The dated source
customer discount itself remains represented as its own actual ledger event.

For native fixture validation, build only the separate QA application using
`-I tools/migration/migrationqa.init.gradle` with pinned Gradle 8.13 through `adev`.
It uses `com.pocketdev.advance_erp2.migrationqa` and its own test APK. The optional
`TuktakMigrationRehearsalTest` takes `migrationDbPath` pointing to a copy in that
QA package's private files directory; it **refuses the production package**.
It opens a separate disposable Room database and compares both native ledgers,
all 8 customer balances, date ordering, zero allocations and confirmed zero stock.
Never stage this fixture in the live application or change the existing app ID.
