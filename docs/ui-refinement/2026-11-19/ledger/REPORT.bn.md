# Central Ledger — UI, statements ও accounting projection

## উদ্দেশ্য ও বর্তমান কাজ

Dashboard-এর Ledger route Central Ledger & Reports screen খোলে। এই পরিবর্তন সেই screen, party statement, linked transaction details, export এবং Ledger থেকে payment/collection workflow উন্নত করে। আলাদা Accounts/Journal/Bank Reconciliation module-এর হিসাবনীতি বদলানো হয়নি।

## গুরুত্বপূর্ণ সমস্যা ও সমাধান

- আগে presentation filter-এর পরে balance গণনা হতো। এখন এক party-এর সম্পূর্ণ dated timeline থেকে Opening → Debit/Credit → Closing গণনা হয়; search/amount/status/warehouse filter শুধু দেখানো rows বাছাই করে। Filter-এ payment আলাদা খুঁজলেও প্রকৃত running balance বদলায় না।
- Customer ও Supplier পরস্পর আলাদা; একই ID থাকলেও cross-party/type হিসাব মেশে না। Party না বাছলে একটি সম্মিলিত running balance দেখানো হয় না।
- Posted invoice ও confirmed/non-draft PO financial projection-এ আসে। Draft audit row-তে balance effect নেই। Supplier party ledger বর্তমান business policy অনুযায়ী confirmed PO commitment অনুসরণ করে; এটি GL journal timing বদলানো নয়।
- Invoice cancellation ও payment void-এর original entry রাখা হয়, প্রকৃত event date-এ signed reversal দেখানো হয়। Legacy cancellation/void timestamp না থাকলে পুরোনো updatedAt/payment-date fallback ব্যবহার হয়; সে ক্ষেত্রে exact historical event time-এর নিশ্চয়তা নেই। স্থির date/createdAt/type/id sorting একই সময়ে হওয়া লেনদেনের balance স্থির রাখে।
- Customer-credit return AR কমায়; cash/bank refund আবার customer due কমায় না। Receipt-কে revenue ধরে invoice-এর সঙ্গে দ্বিগুণ Income গণনা করা হয় না। Profit/Loss filter stored invoice margin ব্যবহার করে; discount দ্বিতীয়বার বাদ পড়ে না।
- Read-only report এক Room transaction-এর snapshot থেকে rows, statement ও summary বানায়। Room invalidation-এ নতুন payment/invoice/return update হয়; query debounce, conflation, loading, retry ও action-error state আছে। Export-এর snapshot filter-এর সঙ্গে না মিললে export বন্ধ থাকে।
- Supplier/customer ID-তে indexed SQL filtering হয়েছে। UI প্রতি ধাপে ১০০ rows দেখায়। Running balance/export-এর জন্য সম্পূর্ণ scoped history এখনও memory-তে তৈরি হয়; এটি সম্পূর্ণ SQL/keyset paging-এর দাবি নয়। অনেক বড় all-party history-তে আরও database aggregation/paging প্রয়োজন হতে পারে।

## UI ও workflow

- Customer/Supplier switch, নাম/code/phone দিয়ে searchable party picker; archived party-র history-ও দেখা যায়।
- Opening/Debit/Credit/Closing-এর দুই-column metric layout, signed advance/credit label এবং সম্পূর্ণ সময়সীমার balance বনাম filtered rows-এর পরিষ্কার ব্যাখ্যা।
- Transaction card-এ আলাদা debit/credit ও পূর্ণ প্রস্থে running balance; reference/date/status/notes আলাদা। Amount আর metadata একই সরু জায়গার জন্য প্রতিযোগিতা করে না।
- বর্তমান open-document aging: not due, ১–৩০, ৩১–৬০, ৬১–৯০, ৯০+ দিন এবং due date নেই। Aging statement-এর historical date filter থেকে আলাদা করে label করা। Supplier payment due date নেই বলে expected arrival-কে due date বানানো হয়নি।
- Unallocated customer credit আলাদা থাকে; automatic FIFO allocation নেই। Collection form-এ চাইলে নির্দিষ্ট posted invoice বেছে বরাদ্দ করা যায়।
- Ledger থেকেই collection/supplier payment form; server success না হওয়া পর্যন্ত form খোলা থাকে, error দেখা যায়, duplicate submit বন্ধ। Supplier BDT payment-এ rate ১; unlinked foreign payment-এ rate অনুমান না করে ব্যবহারকারী দেন।
- Payment allocation থেকে invoice, supplier payment থেকে PO-তে drill-down। Void reason/date/actor, allocation/unallocated amount ও payment reference audit details-এ থাকে। পুরোনো detail request cancel হয়; error/loading state আছে।

## Financial protection

Customer payment, invoice allocation, receipt ও journal posting এখন একই Room transaction-এ। Journal ব্যর্থ হলে partial payment বা invoice update রাখা হয় না। অন্য customer-এর invoice, draft/cancelled invoice, duplicate allocation, অতিরিক্ত allocation ও stale due reject হয়। Document number raw-IO allocation write transaction-এর আগে হয়।

Payment-এর পর পুরোনো master due-কে নতুন opening হিসেবে আবার যোগ করা হয় না: পূর্বের signed balance থেকে payment বাদ দিয়ে nonnegative master receivable রাখা হয়; advance আলাদা statement credit হিসেবে থাকে। Standalone payment-এর allocation rule ও existing journal account mapping অপরিবর্তিত। সাধারণভাবে সমস্ত stock, landed cost ও FIFO calculation অপরিবর্তিত।

## Export ও security

- PDF party statement: opening/debit/credit/closing, filtered rows-এর ব্যাখ্যা, readable columns, page breaks/repeated headers, FX amount/rate এবং signed balances।
- XLSX-এ period statement totals ও scoped party data; rows-এ প্রকৃত debit/credit/balance ও original currency/amount/rate। CSV UTF-8 BOM, quoted text, formula-injection escaping, exact minor-unit decimal ব্যবহার করে।
- Party না বাছলে transaction amount-কে running balance হিসেবে export করা হয় না।
- Cost/profit screen ও export-এ role permission এবং আগের biometric disclosure gate ব্যবহৃত। Cost/profit ছাড়া সাধারণ statement share করা যায়। Profit-filtered export-এ biometric/role যাচাই লাগে; hidden stock cost export-এ mask থাকে। WhatsApp share button কেবল ব্যবহারকারীর share intent খোলে; agent কোনো বার্তা পাঠায়নি।

## Legacy data ও সীমাবদ্ধতা

Opening-এর আলাদা historical field না থাকায় master due/payable ও posted transaction net-এর nonzero difference-কে পূর্বের/সমন্বয়যোগ্য balance হিসেবে চিহ্নিত করা হয়। এটি verified opening journal নয়; UI/PDF-তে উৎস যাচাইয়ের সতর্কতা আছে। Zero-clamped master দিয়ে advance মুছে ফেলা হয় না। কোনো পুরোনো journal বা financial amount স্বয়ংক্রিয়ভাবে rewrite হয়নি।

পুরোনো cancelled PO-তে confirmation/cancellation-এর পূর্ণ event history নেই; এমন PO audit-only থাকে। Exact historical PO liability reconstruction বা historical invoice aging-এর দাবি নেই। বর্তমান invoice due ও unallocated credits ব্যবহার করা হয়; পুরোনো allocation deletion-এর ইতিহাস বানানো হয়নি।

## যাচাই

Final source freeze-এর পর non-incremental Kotlin compile, assembleDebug, 355 JVM tests (০ failures/errors) এবং assembleDebugAndroidTest পাস। Build: 6m 55s। Source manifest-এর সব Kotlin hash final source-এর সঙ্গে মিলে গেছে।

নতুন direct engine/validation/export tests search/sort balance invariance, opening/closing reconciliation, cross-party isolation, reversal dates, drafts, cash refund, advance, actual profit/income filters, due-date aging, overflow, explicit allocation ও CSV precision/injection পরীক্ষা করে।

পাঁচটি isolated in-memory Room instrumentation test-এ repository filtering, live invalidation, advance, collection/master consistency এবং journal failure rollback রাখা হয়েছে। Device test APK compile পাস; ব্যবহারকারীর নির্দেশ অনুযায়ী ADB ব্যবহার বা device tests চালানো হয়নি। Rendered UI ও PDF before/after visual verification হয়নি; pixel-perfect দাবি করা হচ্ছে না।

## পরিবর্তিত source/test ফাইল

- app/src/androidTest/java/com/pocketdev/advance_erp/LedgerRepositoryRegressionTest.kt
- app/src/main/java/com/pocketdev/advance_erp/core/engine/LedgerReportEngine.kt
- app/src/main/java/com/pocketdev/advance_erp/core/engine/PaymentAllocationValidation.kt
- app/src/main/java/com/pocketdev/advance_erp/core/util/ControlCenterExcelGenerator.kt
- app/src/main/java/com/pocketdev/advance_erp/core/util/ControlCenterPdfGenerator.kt
- app/src/main/java/com/pocketdev/advance_erp/core/util/LedgerStatementExporter.kt
- app/src/main/java/com/pocketdev/advance_erp/data/dao/CustomerDao.kt
- app/src/main/java/com/pocketdev/advance_erp/data/dao/PurchaseOrderDao.kt
- app/src/main/java/com/pocketdev/advance_erp/data/dao/SupplierDao.kt
- app/src/main/java/com/pocketdev/advance_erp/data/dao/SupplierPaymentDao.kt
- app/src/main/java/com/pocketdev/advance_erp/data/repository/BusinessControlCenterRepositoryImpl.kt
- app/src/main/java/com/pocketdev/advance_erp/data/repository/SalesRepositoryImpl.kt
- app/src/main/java/com/pocketdev/advance_erp/domain/model/BusinessControlCenterModels.kt
- app/src/main/java/com/pocketdev/advance_erp/domain/repository/BusinessControlCenterRepository.kt
- app/src/main/java/com/pocketdev/advance_erp/feature/controlcenter/BusinessControlCenterScreen.kt
- app/src/main/java/com/pocketdev/advance_erp/feature/controlcenter/BusinessControlCenterViewModel.kt
- app/src/main/java/com/pocketdev/advance_erp/feature/controlcenter/components/ControlCenterFilterDialog.kt
- app/src/main/java/com/pocketdev/advance_erp/feature/controlcenter/components/ControlCenterTransactionDetailDialog.kt
- app/src/main/java/com/pocketdev/advance_erp/feature/controlcenter/components/PartyLedgerComponents.kt
- app/src/main/java/com/pocketdev/advance_erp/feature/imports/RecordSupplierPaymentDialog.kt
- app/src/main/java/com/pocketdev/advance_erp/feature/sales/RecordCustomerPaymentDialog.kt
- app/src/test/java/com/pocketdev/advance_erp/LedgerExportSafetyTest.kt
- app/src/test/java/com/pocketdev/advance_erp/LedgerReportEngineTest.kt
- app/src/test/java/com/pocketdev/advance_erp/PaymentAllocationValidationTest.kt

Final APK আগের public debug certificate দিয়ে signed এবং signature verification পাস। APK ও private signing key Git-এ রাখা হয়নি।
