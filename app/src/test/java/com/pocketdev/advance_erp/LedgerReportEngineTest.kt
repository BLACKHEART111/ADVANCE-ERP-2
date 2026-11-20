package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.core.engine.LedgerReportEngine
import com.pocketdev.advance_erp.domain.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class LedgerReportEngineTest {
    private fun day(n: Long) = LocalDate.of(2026,11,1).plusDays(n).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    private val customer = LedgerParty("p","Party","C001","CUSTOMER",null,null)
    private val supplier = customer.copy(type="SUPPLIER")
    private fun row(id: String, kind: String, amount: Long, date: Long = day(7), party: String="p") = UnifiedTransactionItem(
        id,id,date,when(kind){"PURCHASE"->ReportCategory.PURCHASE;"SUPPLIER_PAYMENT"->ReportCategory.PAYMENT;"PAYMENT"->ReportCategory.RECEIPT;"RETURN"->ReportCategory.SALES_RETURN;else->ReportCategory.SALES},
        "Party",party,"Details",amount,isInflow=kind!="PURCHASE",status="POSTED",rawType=kind)
    private fun filter() = ControlCenterFilterState(DateRangeOption.CUSTOM,day(5),day(10),ReportCategory.CUSTOMER_LEDGER,selectedCustomerId="p")
    private fun report(rows: List<UnifiedTransactionItem>, f:ControlCenterFilterState=filter(), p:LedgerParty?=customer) = LedgerReportEngine.build(rows,f,p,day(19))

    @Test fun searchDoesNotRecalculateTheActualPartyBalance() {
        val rows=listOf(row("invoice","INVOICE",1000000),row("receipt","PAYMENT",400000,day(8)))
        val full=report(rows)
        val searched=report(rows,filter().copy(searchQuery="receipt"))
        assertEquals(600000L,searched.statement!!.closingBalanceMinor)
        assertEquals(full.statement,searched.statement)
        assertEquals(1,searched.transactions.size)
        assertEquals(600000L,searched.transactions.single().runningBalanceMinor)
        assertEquals(0L,searched.summary.totalSalesMinor)
        assertEquals(400000L,searched.summary.totalReceivedMinor)
    }
    @Test fun openingAndCreditsReconcileToClosing() {
        val r=report(listOf(row("before","INVOICE",500,day(2)),row("bill","INVOICE",1000),row("pay","PAYMENT",300,day(8))))
        val s=r.statement!!
        assertEquals(500L,s.openingBalanceMinor)
        assertEquals(1200L,s.closingBalanceMinor)
        assertEquals(s.closingBalanceMinor,s.openingBalanceMinor+s.totalDebitMinor-s.totalCreditMinor)
    }
    @Test fun sortingAndAmountFiltersPreserveTransactionBalances() {
        val rows=listOf(row("bill","INVOICE",1000000),row("receipt","PAYMENT",400000,day(8)))
        val normal=report(rows)
        val sorted=report(rows,filter().copy(sortBy=SortOption.HIGHEST_AMOUNT))
        assertEquals(normal.transactions.associate { it.rawType+it.id to it.runningBalanceMinor },sorted.transactions.associate { it.rawType+it.id to it.runningBalanceMinor })
        assertEquals(600000L,report(rows,filter().copy(amountFilter=AmountRangeFilter.RANGE_1K_10K,customMinAmountMinor=0)).statement!!.closingBalanceMinor)
    }
    @Test fun multiPartyListDoesNotPretendToBeOneAccount() {
        val r=report(listOf(row("a","INVOICE",100),row("b","INVOICE",200,party="other")),filter().copy(selectedCustomerId=null),null)
        assertNull(r.statement)
        assertTrue(r.transactions.all { it.runningBalanceMinor==null })
    }
    @Test fun sameIdAcrossTransactionTypesDoesNotOverwriteRunningBalance() {
        val r=report(listOf(row("same","INVOICE",1000),row("same","PAYMENT",300,day(8))))
        assertEquals(1000L,r.transactions.first { it.rawType=="INVOICE" }.runningBalanceMinor)
        assertEquals(700L,r.transactions.first { it.rawType=="PAYMENT" }.runningBalanceMinor)
    }
    @Test fun cancellationUsesReversalDateAndKeepsHistoricalStatement() {
        val bill=row("bill","INVOICE",1000,day(2)).copy(status="CANCELLED")
        val reversal=bill.copy(id="cancel",sourceId="bill",rawType="INVOICE_REVERSAL",effectSign=-1,date=day(8),createdAt=day(8))
        assertEquals(1000L,report(listOf(bill,reversal),filter().copy(customEndDate=day(6))).statement!!.closingBalanceMinor)
        val final=report(listOf(bill,reversal)).statement!!
        assertEquals(1000L,final.openingBalanceMinor);assertEquals(0L,final.closingBalanceMinor)
    }
    @Test fun voidedReceiptIsOffsetAtVoidDate() {
        val pay=row("receipt","PAYMENT",300,day(2)).copy(status="VOIDED")
        val reversal=pay.copy(id="void",sourceId="receipt",rawType="PAYMENT_REVERSAL",effectSign=-1,date=day(8),createdAt=day(8))
        assertEquals(-300L,report(listOf(pay,reversal),filter().copy(customEndDate=day(6))).statement!!.closingBalanceMinor)
        assertEquals(0L,report(listOf(pay,reversal)).statement!!.closingBalanceMinor)
    }
    @Test fun draftsHaveNoFinancialEffect() {
        val r=report(listOf(row("draft","INVOICE",1000).copy(accountingPosted=false)))
        assertEquals(0L,r.statement!!.closingBalanceMinor);assertEquals(0L,r.summary.totalSalesMinor)
        assertEquals(0L,r.transactions.single().ledgerDebitMinor)
    }
    @Test fun cashRefundDoesNotReduceReceivableAgain() {
        val refund=row("return","RETURN",100).copy(paymentMethod="CASH_REFUND")
        assertEquals(1000L,report(listOf(row("bill","INVOICE",1000),refund)).statement!!.closingBalanceMinor)
        assertEquals(900L,report(listOf(row("bill","INVOICE",1000),refund.copy(paymentMethod="CUSTOMER_CREDIT"))).statement!!.closingBalanceMinor)
    }
    @Test fun supplierDebitsPaymentsAndCreditsBillsInBdt() {
        val bill=row("PO","PURCHASE",17000).copy(originalCurrency="CNY",originalAmountMinor=1000,exchangeRate="17")
        val f=filter().copy(category=ReportCategory.SUPPLIER_LEDGER,selectedCustomerId=null,selectedSupplierId="p")
        val s=report(listOf(bill,row("TT","SUPPLIER_PAYMENT",7000,day(8))),f,supplier).statement!!
        assertEquals(17000L,s.totalCreditMinor);assertEquals(7000L,s.totalDebitMinor);assertEquals(10000L,s.closingBalanceMinor)
    }
    @Test fun discountIsNotSubtractedTwiceFromNetInvoiceRevenue() {
        val r=report(listOf(row("bill","INVOICE",900).copy(discountMinor=100,profitMinor=300,cogsMinor=600)))
        assertEquals(300L,r.summary.grossProfitMinor)
    }
    @Test fun profitFiltersUseActualMarginAndTaxUsesStoredMetadata() {
        val rows=listOf(row("profit","INVOICE",1000).copy(profitMinor=100),row("loss","INVOICE",2000).copy(profitMinor=-100,taxMinor=50))
        assertEquals("profit",report(rows,filter().copy(financialMetric=FinancialMetricFilter.PROFIT)).transactions.single().id)
        assertEquals("loss",report(rows,filter().copy(financialMetric=FinancialMetricFilter.LOSS)).transactions.single().id)
        assertEquals("loss",report(rows,filter().copy(financialMetric=FinancialMetricFilter.TAX)).transactions.single().id)
    }
    @Test fun agingUsesDueDateAndDoesNotAutomaticallyAllocateCredit() {
        val rows=listOf(row("overdue","INVOICE",1000).copy(secondaryAmountMinor=1000,dueDate=day(0)),row("undated","INVOICE",500).copy(secondaryAmountMinor=500),row("advance","PAYMENT",600).copy(secondaryAmountMinor=600))
        val r=report(rows)
        assertEquals(1000L,r.aging.first { it.key=="1_30" }.amountMinor)
        assertEquals(500L,r.aging.first { it.key=="NO_DUE_DATE" }.amountMinor)
        assertEquals(600L,r.unallocatedCreditMinor)
    }
    @Test fun legacyCreditRemainsSignedAndIsNotAPeriodSale() {
        val r=report(listOf(row("opening","OPENING_CUSTOMER",500,0).copy(effectSign=-1)))
        assertEquals(-500L,r.statement!!.openingBalanceMinor);assertEquals(-500L,r.statement!!.closingBalanceMinor)
        assertEquals(0L,r.summary.totalSalesMinor);assertTrue(r.transactions.isEmpty())
    }
    @Test fun samePartyIdInCustomerAndSupplierCannotCrossContaminate() {
        val r=report(listOf(row("bill","INVOICE",1000),row("po","PURCHASE",999999)))
        assertEquals(1000L,r.statement!!.closingBalanceMinor)
    }
    @Test fun warehouseFilteringRequiresAnActualMatchingWarehouseId() {
        val r=report(listOf(row("a","INVOICE",100).copy(warehouseId="w"),row("b","INVOICE",200)),filter().copy(selectedWarehouseId="w"))
        assertEquals(listOf("a"),r.transactions.map { it.id })
        assertEquals(300L,r.statement!!.closingBalanceMinor)
    }
    @Test(expected=ArithmeticException::class) fun overflowFailsRatherThanWrappingAStatement() {
        report(listOf(row("a","INVOICE",Long.MAX_VALUE),row("b","INVOICE",1,day(8))))
    }
    @Test fun clampedMasterDueMustNotInventOpeningDebtToCancelAnAdvance() {
        assertEquals(0L,LedgerReportEngine.openingAdjustment(0,-500))
        assertEquals(400L,LedgerReportEngine.openingAdjustment(1400,1000))
    }
    @Test fun incomeFilterDoesNotCountAnInvoiceAndItsReceiptTwice() {
        val rows=listOf(row("invoice","INVOICE",1000),row("receipt","PAYMENT",1000))
        val report=report(rows,filter().copy(category=ReportCategory.INCOME))
        assertEquals(listOf("invoice"),report.transactions.map { it.id })
        assertEquals(1000L,report.summary.totalSalesMinor)
        assertEquals(0L,report.summary.totalReceivedMinor)
    }

    @Test fun extendedFiltersFilterByProductCategoryPaymentMethodDirectionAndChannel() {
        val wholesaleInv = row("inv-w", "INVOICE", 150000).copy(
            productIds = setOf("prod-1"),
            categoryIds = setOf("cat-1"),
            isRetailSale = false
        )
        val retailInv = row("inv-r", "INVOICE", 50000).copy(
            productIds = setOf("prod-2"),
            categoryIds = setOf("cat-2"),
            isRetailSale = true
        )
        val cashPay = row("pay-c", "PAYMENT", 40000, day(8)).copy(
            paymentMethod = "CASH"
        )
        val bankPay = row("pay-b", "SUPPLIER_PAYMENT", 30000, day(8)).copy(
            paymentMethod = "BANK_TT",
            isInflow = false
        )
        val allRows = listOf(wholesaleInv, retailInv, cashPay, bankPay)
        val base = ControlCenterFilterState(dateRangeOption = DateRangeOption.ALL_TIME)

        assertEquals(listOf("inv-w"), LedgerReportEngine.build(allRows, base.copy(selectedProductId = "prod-1"), null, day(19)).transactions.map { it.id })
        assertEquals(listOf("inv-r"), LedgerReportEngine.build(allRows, base.copy(selectedCategoryId = "cat-2"), null, day(19)).transactions.map { it.id })
        assertEquals(listOf("pay-c"), LedgerReportEngine.build(allRows, base.copy(paymentMethodFilter = PaymentMethodFilter.CASH), null, day(19)).transactions.map { it.id })
        assertEquals(listOf("pay-b"), LedgerReportEngine.build(allRows, base.copy(paymentMethodFilter = PaymentMethodFilter.BANK), null, day(19)).transactions.map { it.id })
        assertEquals(listOf("pay-b"), LedgerReportEngine.build(allRows, base.copy(transactionDirection = TransactionDirectionFilter.OUTFLOW), null, day(19)).transactions.map { it.id })
        assertEquals(listOf("inv-w"), LedgerReportEngine.build(allRows, base.copy(saleChannel = SaleChannelFilter.WHOLESALE), null, day(19)).transactions.map { it.id })
        assertEquals(listOf("inv-r"), LedgerReportEngine.build(allRows, base.copy(saleChannel = SaleChannelFilter.RETAIL), null, day(19)).transactions.map { it.id })

        val sortedByType = LedgerReportEngine.build(allRows, base.copy(sortBy = SortOption.TRANSACTION_TYPE), null, day(19)).transactions
        assertEquals(4, sortedByType.size)
        assertEquals(ReportCategory.SALES, sortedByType.first().category)
    }
}
