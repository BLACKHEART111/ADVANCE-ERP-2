package com.pocketdev.advance_erp.core.communication

import com.pocketdev.advance_erp.core.engine.PaymentAllocationValidation
import com.pocketdev.advance_erp.data.entity.CommunicationSnapshot
import com.pocketdev.advance_erp.data.entity.CustomerPayment
import com.pocketdev.advance_erp.data.entity.PaymentAllocation
import com.pocketdev.advance_erp.data.entity.SalesInvoice
import com.pocketdev.advance_erp.data.entity.SalesInvoiceItem
import com.pocketdev.advance_erp.domain.model.PartyLedgerStatement
import com.pocketdev.advance_erp.domain.repository.CustomerLedgerEntry
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pure-JVM deterministic accounting reconciler for the centralized communication engine.
 * Reconciles invoice settlements, historical posting snapshots vs live ledger balances,
 * payment receipts, due reminders, and ledger statements without mutating any repository state.
 */
object CommunicationAccountingReconciler {

    fun buildInvoiceBreakdown(
        invoice: SalesInvoice,
        items: List<SalesInvoiceItem>,
        allocatedPaymentMinor: Long,
        creditReturnMinor: Long,
        snapshot: CommunicationSnapshot?,
        currentAccountBalanceMinor: Long,
        status: CommunicationStatus,
        showQrCode: Boolean,
        defaultPaymentTerms: String = "",
        asOfMillis: Long = System.currentTimeMillis()
    ): InvoiceFinancialBreakdown {
        val grossItemsTotalMinor = if (items.isNotEmpty()) {
            items.sumOf { it.quantity.toLong() * it.unitPriceMinor }
        } else {
            invoice.subtotalMinor
        }
        val itemDiscountTotalMinor = items.sumOf { it.discountMinor }
        val settlement = PaymentAllocationValidation.computeInvoiceSettlement(
            totalAmountMinor = invoice.totalAmountMinor,
            activeAllocatedMinor = allocatedPaymentMinor,
            creditReturnMinor = creditReturnMinor,
            isPosted = invoice.isPosted,
            currentStatus = invoice.status
        )
        val effectiveTerms = invoice.paymentTerms?.trim()?.takeIf { it.isNotEmpty() }
            ?: defaultPaymentTerms.trim().takeIf { it.isNotEmpty() }
        val qrPayload = if (showQrCode) buildInvoiceQrPayload(invoice, status) else ""

        val hasSnapshot = snapshot != null
        val isReshare = if (snapshot != null) {
            snapshot.closingBalanceMinor != currentAccountBalanceMinor ||
                snapshot.allocatedPaymentMinor != allocatedPaymentMinor ||
                snapshot.invoiceDueMinor != settlement.dueAmountMinor ||
                creditReturnMinor > 0L
        } else {
            true
        }

        return InvoiceFinancialBreakdown(
            salesType = invoice.salesType,
            grossItemsTotalMinor = grossItemsTotalMinor,
            itemDiscountTotalMinor = itemDiscountTotalMinor,
            subtotalMinor = invoice.subtotalMinor,
            invoiceDiscountMinor = invoice.discountMinor,
            invoiceDiscountPercentBps = invoice.discountPercentBps,
            additionalChargesMinor = invoice.taxMinor,
            netInvoiceAmountMinor = invoice.totalAmountMinor,
            creditReturnMinor = creditReturnMinor,
            allocatedPaymentMinor = allocatedPaymentMinor,
            invoiceDueMinor = settlement.dueAmountMinor,
            dueDateMillis = invoice.dueDate,
            paymentTerms = effectiveTerms,
            notes = invoice.notes?.trim()?.takeIf { it.isNotEmpty() },
            qrVerificationPayload = qrPayload,
            hasHistoricalSnapshot = hasSnapshot,
            historicalAsOfMillis = snapshot?.createdAt,
            historicalPreviousBalanceMinor = snapshot?.previousBalanceMinor,
            historicalAllocatedPaymentMinor = snapshot?.allocatedPaymentMinor,
            historicalInvoiceDueMinor = snapshot?.invoiceDueMinor,
            historicalClosingBalanceMinor = snapshot?.closingBalanceMinor,
            currentAccountBalanceMinor = currentAccountBalanceMinor,
            asOfMillis = asOfMillis,
            isReshareAfterLaterActivity = isReshare
        )
    }

    fun buildInvoiceQrPayload(invoice: SalesInvoice, status: CommunicationStatus): String {
        val dateToken = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(invoice.invoiceDate))
        val majorTotal = BigDecimal.valueOf(invoice.totalAmountMinor, 2).toPlainString()
        val raw = "ADV-ERP|INV|${invoice.invoiceNumber}|$dateToken|$majorTotal|${status.name}"
        return raw.filter { it.code in 32..126 }.take(80)
    }

    fun buildPaymentBreakdown(
        payment: CustomerPayment,
        allocations: List<PaymentAllocation>,
        snapshot: CommunicationSnapshot?,
        customerLedger: List<CustomerLedgerEntry>,
        currentAccountBalanceMinor: Long,
        currencyCode: String = "BDT"
    ): PaymentReceiptBreakdown {
        val ledgerPaymentEntry = customerLedger.firstOrNull {
            it.transactionType == "PAYMENT" && (it.id == payment.id || it.referenceNumber == payment.paymentNumber)
        }
        val balanceAfterPayment = snapshot?.closingBalanceMinor ?: ledgerPaymentEntry?.runningBalanceMinor
        val previousBalance = snapshot?.previousBalanceMinor ?: balanceAfterPayment?.let { it + payment.amountMinor }
        val hasLaterActivity = balanceAfterPayment != null && balanceAfterPayment != currentAccountBalanceMinor

        return PaymentReceiptBreakdown(
            paymentNumber = payment.paymentNumber,
            paymentDateMillis = payment.paymentDate,
            paymentAmountMinor = payment.amountMinor,
            paymentMethod = payment.paymentMethod,
            referenceNote = payment.referenceNote?.trim()?.takeIf { it.isNotEmpty() },
            allocatedToInvoicesMinor = allocations.sumOf { it.amountMinor },
            unallocatedAdvanceMinor = payment.unallocatedCreditMinor,
            allocations = allocations.map {
                PaymentReceiptAllocation(
                    invoiceId = it.invoiceId,
                    invoiceNumber = it.invoiceNumber,
                    amountMinor = it.amountMinor
                )
            },
            previousBalanceMinor = previousBalance,
            balanceAfterPaymentMinor = balanceAfterPayment,
            currentAccountBalanceMinor = currentAccountBalanceMinor,
            hasLaterActivityAfterPayment = hasLaterActivity,
            currencyCode = currencyCode
        )
    }

    fun buildDueReminderBreakdown(
        customerLedger: List<CustomerLedgerEntry>,
        customerOutstandingFallback: Long,
        unpaidInvoiceDueDates: List<Long?>,
        asOfMillis: Long = System.currentTimeMillis()
    ): DueReminderBreakdown {
        val currentTotalDueMinor = customerLedger.lastOrNull()?.runningBalanceMinor ?: customerOutstandingFallback
        val cancelledInvoiceIds = customerLedger
            .filter { it.transactionType == "INVOICE_CANCELLED" }
            .map { it.id.removeSuffix("-cancelled") }
            .toSet()
        val voidedPaymentIds = customerLedger
            .filter { it.transactionType == "PAYMENT_VOID" }
            .map { it.id.removeSuffix("-voided") }
            .toSet()

        val activeInvoices = customerLedger.withIndex().filter { (_, entry) ->
            entry.transactionType == "INVOICE" && entry.id !in cancelledInvoiceIds
        }
        val activePayments = customerLedger.withIndex().filter { (_, entry) ->
            entry.transactionType == "PAYMENT" && entry.id !in voidedPaymentIds
        }

        val latestInv = activeInvoices.lastOrNull()
        val latestPay = activePayments.lastOrNull()
        val refIndices = listOfNotNull(latestInv?.index, latestPay?.index)
        val earliestRefIdx = refIndices.minOrNull()

        val priorBalanceMinor = when {
            earliestRefIdx == null -> currentTotalDueMinor
            earliestRefIdx == 0 -> 0L
            else -> customerLedger[earliestRefIdx - 1].runningBalanceMinor
        }
        val latestInvAmount = latestInv?.value?.debitMinor
        val latestPayAmount = latestPay?.value?.creditMinor
        val otherNetAdjustmentMinor = currentTotalDueMinor -
            (priorBalanceMinor + (latestInvAmount ?: 0L) - (latestPayAmount ?: 0L))

        return DueReminderBreakdown(
            currentTotalDueMinor = currentTotalDueMinor,
            earliestDueDateMillis = unpaidInvoiceDueDates.filterNotNull().minOrNull(),
            unpaidInvoiceCount = unpaidInvoiceDueDates.size,
            priorBalanceMinor = priorBalanceMinor,
            latestInvoiceNumber = latestInv?.value?.referenceNumber,
            latestInvoiceDateMillis = latestInv?.value?.date,
            latestInvoiceAmountMinor = latestInvAmount,
            latestPaymentNumber = latestPay?.value?.referenceNumber,
            latestPaymentDateMillis = latestPay?.value?.date,
            latestPaymentAmountMinor = latestPayAmount,
            otherNetAdjustmentMinor = otherNetAdjustmentMinor,
            asOfMillis = asOfMillis
        )
    }

    fun buildCustomerStatementBreakdown(
        customerLedger: List<CustomerLedgerEntry>,
        fromMillis: Long,
        toMillis: Long,
        isAllTime: Boolean = (fromMillis == 0L),
        currencyCode: String = "BDT"
    ): LedgerStatementBreakdown {
        val openingBalanceMinor = customerLedger
            .lastOrNull { it.transactionType == "OPENING_BALANCE" || it.date < fromMillis }
            ?.runningBalanceMinor ?: 0L
        val selected = customerLedger.filter {
            it.transactionType != "OPENING_BALANCE" && it.date in fromMillis..toMillis
        }

        val totalSalesMinor = selected.filter { it.transactionType == "INVOICE" }.sumOf { it.debitMinor } -
            selected.filter { it.transactionType == "INVOICE_CANCELLED" }.sumOf { it.creditMinor }
        val totalReturnMinor = selected.filter { it.transactionType == "RETURN" }.sumOf { it.creditMinor }
        val totalRefundPaidMinor = selected.filter { it.transactionType == "REFUND" }.sumOf { it.debitMinor }
        val totalPaymentMinor = selected.filter { it.transactionType == "PAYMENT" }.sumOf { it.creditMinor } -
            selected.filter { it.transactionType == "PAYMENT_VOID" }.sumOf { it.debitMinor }
        val netAdjustmentMinor = selected.filter { it.transactionType == "ADJUSTMENT" }
            .sumOf { it.debitMinor - it.creditMinor }
        val closingBalanceMinor = selected.lastOrNull()?.runningBalanceMinor ?: openingBalanceMinor

        return LedgerStatementBreakdown(
            fromMillis = fromMillis,
            toMillis = toMillis,
            isAllTime = isAllTime,
            openingBalanceMinor = openingBalanceMinor,
            totalSalesOrPurchasesMinor = totalSalesMinor,
            totalReturnMinor = totalReturnMinor,
            totalRefundPaidMinor = totalRefundPaidMinor,
            totalPaymentMinor = totalPaymentMinor,
            netAdjustmentMinor = netAdjustmentMinor,
            closingBalanceMinor = closingBalanceMinor,
            transactions = selected.map {
                LedgerStatementRow(
                    dateMillis = it.date,
                    referenceNumber = it.referenceNumber,
                    transactionType = it.transactionType,
                    description = it.description,
                    debitMinor = it.debitMinor,
                    creditMinor = it.creditMinor,
                    runningBalanceMinor = it.runningBalanceMinor
                )
            },
            currencyCode = currencyCode
        )
    }

    fun buildSupplierStatementBreakdown(
        statement: PartyLedgerStatement?,
        fromMillis: Long,
        toMillis: Long,
        fallbackBalanceMinor: Long,
        currencyCode: String = "BDT"
    ): LedgerStatementBreakdown {
        val opening = statement?.openingBalanceMinor ?: 0L
        val closing = statement?.closingBalanceMinor ?: fallbackBalanceMinor
        val txs = statement?.transactions.orEmpty().asReversed()
        return LedgerStatementBreakdown(
            fromMillis = fromMillis,
            toMillis = toMillis,
            isAllTime = (fromMillis == 0L),
            openingBalanceMinor = opening,
            totalSalesOrPurchasesMinor = statement?.totalCreditMinor ?: 0L,
            totalReturnMinor = 0L,
            totalRefundPaidMinor = 0L,
            totalPaymentMinor = statement?.totalDebitMinor ?: 0L,
            netAdjustmentMinor = 0L,
            closingBalanceMinor = closing,
            transactions = txs.map { tx ->
                LedgerStatementRow(
                    dateMillis = tx.date,
                    referenceNumber = tx.referenceNumber,
                    transactionType = tx.rawType,
                    description = tx.particulars,
                    debitMinor = tx.ledgerDebitMinor ?: 0L,
                    creditMinor = tx.ledgerCreditMinor ?: 0L,
                    runningBalanceMinor = tx.runningBalanceMinor ?: 0L
                )
            },
            currencyCode = currencyCode
        )
    }
}
