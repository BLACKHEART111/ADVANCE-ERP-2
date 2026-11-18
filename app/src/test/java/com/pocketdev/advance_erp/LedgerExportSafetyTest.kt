package com.pocketdev.advance_erp

import com.pocketdev.advance_erp.core.util.LedgerStatementExporter
import org.junit.Assert.*
import org.junit.Test

class LedgerExportSafetyTest {
    @Test fun preservesExactMinorUnitsBeyondDoublePrecision() {
        assertEquals("90071992547409.93",LedgerStatementExporter.money(9007199254740993L))
        assertEquals("-0.01",LedgerStatementExporter.money(-1))
    }
    @Test fun spreadsheetTextCannotBecomeAFormula() {
        assertEquals("\"'=SUM(A1:A2)\"",LedgerStatementExporter.csvCell("=SUM(A1:A2)"))
        assertEquals("\"'@SUM(A1:A2)\"",LedgerStatementExporter.csvCell("@SUM(A1:A2)"))
    }
    @Test fun csvQuotesAndNewLinesRemainInsideOneCell() {
        assertEquals("\"A,\"\"B\"\"\nC\"",LedgerStatementExporter.csvCell("A,\"B\"\nC"))
    }
    @Test fun leadingSpacesDoNotBypassCsvFormulaEscaping() {
        assertEquals("\"'  =SUM(A1:A2)\"",LedgerStatementExporter.csvCell("  =SUM(A1:A2)"))
    }
}
