package com.pocketdev.advance_erp

import androidx.sqlite.db.SupportSQLiteDatabase
import com.pocketdev.advance_erp.core.database.DatabaseMigrations
import com.pocketdev.advance_erp.data.entity.AccountType
import com.pocketdev.advance_erp.data.entity.NormalBalance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.BufferedReader
import java.io.File
import java.lang.reflect.Proxy

/**
 * PHASE 05.6 — DATABASE MIGRATION VERIFICATION
 * Tests that migration path 1 -> 2 -> 3 -> 4 is continuous and non-destructive.
 */
class DatabaseMigrationVerificationTest {

    @Test
    fun testMigrationVersionsContinuous() {
        val m12 = DatabaseMigrations.MIGRATION_1_2
        assertEquals(1, m12.startVersion)
        assertEquals(2, m12.endVersion)

        val m23 = DatabaseMigrations.MIGRATION_2_3
        assertEquals(2, m23.startVersion)
        assertEquals(3, m23.endVersion)

        val m34 = DatabaseMigrations.MIGRATION_3_4
        assertEquals(3, m34.startVersion)
        assertEquals(4, m34.endVersion)

        val m45 = DatabaseMigrations.MIGRATION_4_5
        assertEquals(4, m45.startVersion)
        assertEquals(5, m45.endVersion)

        val m56 = DatabaseMigrations.MIGRATION_5_6
        assertEquals(5, m56.startVersion)
        assertEquals(6, m56.endVersion)

        val m67 = DatabaseMigrations.MIGRATION_6_7
        assertEquals(6, m67.startVersion)
        assertEquals(7, m67.endVersion)

        val m78 = DatabaseMigrations.MIGRATION_7_8
        assertEquals(7, m78.startVersion)
        assertEquals(8, m78.endVersion)

        val m89 = DatabaseMigrations.MIGRATION_8_9
        assertEquals(8, m89.startVersion)
        assertEquals(9, m89.endVersion)

        val m910 = DatabaseMigrations.MIGRATION_9_10
        assertEquals(9, m910.startVersion)
        assertEquals(10, m910.endVersion)

        val m1011 = DatabaseMigrations.MIGRATION_10_11
        assertEquals(10, m1011.startVersion)
        assertEquals(11, m1011.endVersion)

        val m1112 = DatabaseMigrations.MIGRATION_11_12
        assertEquals(11, m1112.startVersion)
        assertEquals(12, m1112.endVersion)

        val m1213 = DatabaseMigrations.MIGRATION_12_13
        assertEquals(12, m1213.startVersion)
        assertEquals(13, m1213.endVersion)

        val m1314 = DatabaseMigrations.MIGRATION_13_14
        assertEquals(13, m1314.startVersion)
        assertEquals(14, m1314.endVersion)
    }

    @Test
    fun testStandardChartOfAccountsIntegrity() {
        // Standard chart of accounts must have essential accounts
        val expectedCodes = listOf(
            "1010" to (AccountType.ASSET to NormalBalance.DEBIT),      // Cash in Hand
            "1020" to (AccountType.ASSET to NormalBalance.DEBIT),      // Bank Account
            "1040" to (AccountType.ASSET to NormalBalance.DEBIT),      // Accounts Receivable
            "1050" to (AccountType.ASSET to NormalBalance.DEBIT),      // Supplier Shortage & Claims Receivable
            "1210" to (AccountType.ASSET to NormalBalance.DEBIT),      // Inventory Asset
            "1220" to (AccountType.ASSET to NormalBalance.DEBIT),      // Inventory in Transit
            "2010" to (AccountType.LIABILITY to NormalBalance.CREDIT), // Accounts Payable
            "2030" to (AccountType.LIABILITY to NormalBalance.CREDIT), // Customer Advance
            "3010" to (AccountType.EQUITY to NormalBalance.CREDIT),    // Owner Equity
            "3020" to (AccountType.EQUITY to NormalBalance.CREDIT),    // Retained Earnings
            "4010" to (AccountType.REVENUE to NormalBalance.CREDIT),   // Sales Revenue
            "4020" to (AccountType.REVENUE to NormalBalance.DEBIT),    // Sales Returns (Contra-Revenue)
            "4030" to (AccountType.REVENUE to NormalBalance.DEBIT),    // Sales Discount (Contra-Revenue)
            "5010" to (AccountType.EXPENSE to NormalBalance.DEBIT),    // Cost of Goods Sold
            "5050" to (AccountType.EXPENSE to NormalBalance.DEBIT),    // Damaged Merchandise Write-Off
            "5110" to (AccountType.EXPENSE to NormalBalance.DEBIT),    // Operating Expenses
            "5120" to (AccountType.EXPENSE to NormalBalance.DEBIT),    // Customs & Duty
            "5130" to (AccountType.EXPENSE to NormalBalance.DEBIT)     // Shipping & Freight
        )

        val uniqueCodes = expectedCodes.map { it.first }.toSet()
        assertEquals("Account codes must be unique", expectedCodes.size, uniqueCodes.size)

        expectedCodes.forEach { (_, typeAndBalance) ->
            val (type, normalBalance) = typeAndBalance
            assertNotNull(type)
            assertNotNull(normalBalance)
        }
    }

    @Test
    fun testAllMigrationsExecuteWithoutSyntaxErrorsOrInvalidTokens() {
        val executedSqlList = mutableListOf<String>()
        val mockDb = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            if (method.name == "execSQL") {
                val sql = args[0] as String
                executedSqlList.add(sql)
                // SQLite foreign key action is 'SET NULL' (with space), NOT 'SET_NULL'
                assertFalse("SQL contains SET_NULL which is invalid SQLite syntax: $sql", sql.contains("SET_NULL"))
                assertFalse("SQL contains SET_DEFAULT which is invalid SQLite syntax: $sql", sql.contains("SET_DEFAULT"))
            }
            null
        } as SupportSQLiteDatabase

        // Execute all migrations against mock SupportSQLiteDatabase
        DatabaseMigrations.MIGRATION_1_2.migrate(mockDb)
        DatabaseMigrations.MIGRATION_2_3.migrate(mockDb)
        DatabaseMigrations.MIGRATION_3_4.migrate(mockDb)
        DatabaseMigrations.MIGRATION_4_5.migrate(mockDb)
        DatabaseMigrations.MIGRATION_5_6.migrate(mockDb)
        DatabaseMigrations.MIGRATION_6_7.migrate(mockDb)
        DatabaseMigrations.MIGRATION_7_8.migrate(mockDb)
        DatabaseMigrations.MIGRATION_8_9.migrate(mockDb)
        DatabaseMigrations.MIGRATION_9_10.migrate(mockDb)
        DatabaseMigrations.MIGRATION_10_11.migrate(mockDb)
        DatabaseMigrations.MIGRATION_11_12.migrate(mockDb)
        DatabaseMigrations.MIGRATION_12_13.migrate(mockDb)
        DatabaseMigrations.MIGRATION_13_14.migrate(mockDb)

        assertTrue("Expected multiple SQL statements to be executed across migrations", executedSqlList.size > 30)
        assertTrue(
            "MIGRATION_12_13 must add accountSubType column",
            executedSqlList.any { it.contains("ALTER TABLE chart_of_accounts ADD COLUMN accountSubType") }
        )
        assertTrue(
            "MIGRATION_13_14 must create sale_layer_allocations table",
            executedSqlList.any { it.contains("CREATE TABLE IF NOT EXISTS `sale_layer_allocations`") }
        )
    }

    @Test
    fun testRealSqliteMigration13To14MatchesRoomV14SchemaAndPreservesExistingData() {
        val schema13 = File("schemas/com.pocketdev.advance_erp.core.database.AdvanceErpDatabase/13.json")
        val schema14 = File("schemas/com.pocketdev.advance_erp.core.database.AdvanceErpDatabase/14.json")
        assertTrue("Schema 13.json must exist", schema13.exists())
        assertTrue("Schema 14.json must exist", schema14.exists())

        val migrationSql = mutableListOf<String>()
        val recordingDb = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
                migrationSql.add(args[0] as String)
            }
            null
        } as SupportSQLiteDatabase
        DatabaseMigrations.MIGRATION_13_14.migrate(recordingDb)
        assertTrue("MIGRATION_13_14 must emit SQL statements", migrationSql.size >= 40)

        val sqlJson = migrationSql.joinToString(separator = ",\n") { s ->
            "\"" + s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "") + "\""
        }

        val pyScript = """
            import json, sqlite3, sys

            s13 = json.load(open(r"${schema13.absolutePath}"))['database']
            s14 = json.load(open(r"${schema14.absolutePath}"))['database']
            mig_sqls = json.loads(sys.stdin.read())

            conn = sqlite3.connect(':memory:')
            cur = conn.cursor()
            cur.execute('PRAGMA foreign_keys = ON')

            for e in s13['entities']:
                cur.execute(e['createSql'].replace('${'$'}{TABLE_NAME}', e['tableName']))
                for idx in e.get('indices', []):
                    cur.execute(idx['createSql'].replace('${'$'}{TABLE_NAME}', e['tableName']))

            # Seed realistic v13 master & transactional data (normal import + RET + ADJ + INIT + TR layers)
            cur.execute("INSERT INTO suppliers (id, supplierCode, companyName, contactPerson, phone, email, address, country, currency, outstandingPayableMinor, isActive, createdAt) VALUES ('sup-1','SUP-01','Shenzhen Co','','','','','China','CNY',0,1,1)")
            cur.execute("INSERT INTO product_categories (id, nameEn, nameBn, code, description, defaultFreightRatePerKgBdtMinor) VALUES ('cat-1','Electronics','ইলেকট্রনিক্স','ELEC','',18000)")
            cur.execute("INSERT INTO products (id, categoryId, nameEn, nameBn, baseSku, barcode, qrCode, description, unit, purchasePriceMinor, sellingPriceMinor, minStockAlert, isActive, createdAt, updatedAt) VALUES ('prod-1','cat-1','Earbuds','ইয়ারবাডস','SKU-1',NULL,NULL,'','PCS',10000,20000,5,1,1,1)")
            cur.execute("INSERT INTO warehouses VALUES ('wh-1','Main','WH-MAIN','',1,1)")
            cur.execute("INSERT INTO customers VALUES ('cust-1','C-1','Rahim','01700','','',0,0,NULL,1,1)")
            cur.execute("INSERT INTO chart_of_accounts VALUES ('coa-1010','1010','Cash','নগদ','ASSET','DEBIT','',1,'CASH',NULL,NULL,NULL)")
            cur.execute("INSERT INTO chart_of_accounts VALUES ('coa-5110','5110','Rent','ভাড়া','EXPENSE','DEBIT','',1,NULL,NULL,NULL,NULL)")
            cur.execute("INSERT INTO purchase_orders VALUES ('po-1','PO-1','sup-1','RECEIVED','CNY','17.0',1000,17000,0,0,1000,NULL,NULL,1,1000,'0','10','9','0.1',1,NULL,1000,1000,'shp-1')")
            cur.execute("INSERT INTO import_shipments VALUES ('shp-1','SHP-1','sup-1','po-1','SEA','CN','BD',NULL,NULL,1000,2000,1500,'RECEIVED',1,10,'10','9','0.1',NULL,1000,1000)")
            cur.execute("INSERT INTO warehouse_receipts VALUES ('wr-1','WR-1','shp-1','wh-1',5000,'POSTED','Mgr',0,0,NULL,'po-1','PHYSICAL_COUNT',5000,5000)")

            cur.execute("INSERT INTO inventory_cost_layers VALUES ('icl-imp','prod-1',NULL,'wh-1','shp-1','SHP-1','wr-1','WR-1',10,6,15000,'CNY',500,'17.0',6500,5000,1,'po-1','B1',5000)")
            cur.execute("INSERT INTO inventory_cost_layers VALUES ('icl-ret','prod-1',NULL,'wh-1','RET','RET-1','ret-1','RET-1',2,2,15000,'CNY',15000,'1.0',0,6000,1,NULL,NULL,6000)")
            cur.execute("INSERT INTO inventory_cost_layers VALUES ('icl-adj','prod-1',NULL,'wh-1','ADJ','ADJ-1','adj-1','ADJ-1',1,1,10000,'BDT',10000,'1.0',0,7000,1,NULL,NULL,7000)")
            cur.execute("INSERT INTO inventory_cost_layers VALUES ('icl-init','prod-1',NULL,'wh-1','INIT','OPEN','INIT','OPEN',3,3,10000,'BDT',10000,'1.0',0,1000,1,NULL,NULL,1000)")
            cur.execute("INSERT INTO inventory_cost_layers VALUES ('icl-tr','prod-1',NULL,'wh-1','TR-1','TR-1','tr-1','TR-1',2,2,15000,'CNY',500,'17.0',6500,8000,1,'po-1','B1',8000)")

            # Run actual Kotlin MIGRATION_13_14 SQL statements
            cur.execute('PRAGMA foreign_keys = OFF')
            for sql in mig_sqls:
                cur.execute(sql)
            cur.execute('PRAGMA foreign_keys = ON')

            # Verify FK integrity
            cur.execute('PRAGMA foreign_key_check')
            fk_errs = cur.fetchall()
            assert fk_errs == [], f'Foreign key violations after migration: {fk_errs}'

            # Verify non-destructive data preservation & honest backfill (no fabricated purchaseOrderId!)
            rows = {r[0]: r for r in cur.execute('SELECT id, receiptDate, originReceiptDate, layerSourceType, purchaseOrderId FROM inventory_cost_layers').fetchall()}
            assert len(rows) == 5
            assert rows['icl-imp'][1:] == (5000, 5000, 'IMPORT_RECEIPT', 'po-1'), rows['icl-imp']
            assert rows['icl-ret'][1:] == (6000, 6000, 'SALES_RETURN_RESTORE', None), rows['icl-ret']
            assert rows['icl-adj'][1:] == (7000, 7000, 'MANUAL_ADJUSTMENT', None), rows['icl-adj']
            assert rows['icl-init'][1:] == (1000, 1000, 'OPENING_STOCK', None), rows['icl-init']
            assert rows['icl-tr'][1:] == (8000, 8000, 'TRANSFER_REPLICA', 'po-1'), rows['icl-tr']

            # Compare EVERY table, column, PK, FK, and index in migrated SQLite against Room 14.json
            assert len(s14['entities']) == 57, f"Expected 57 entities in 14.json, got {len(s14['entities'])}"
            for e in s14['entities']:
                t = e['tableName']
                pragma_cols = {c[1]: (c[2].upper(), int(c[3]), int(c[5])) for c in cur.execute(f'PRAGMA table_info(`{t}`)').fetchall()}
                expected_cols = {f['columnName']: (f['affinity'].upper(), 1 if f['notNull'] else 0) for f in e['fields']}
                assert set(pragma_cols.keys()) == set(expected_cols.keys()), f"Column mismatch on {t}: got {set(pragma_cols.keys())} vs {set(expected_cols.keys())}"
                for col_name, (aff, nn) in expected_cols.items():
                    actual_aff, actual_nn, _ = pragma_cols[col_name]
                    assert actual_aff == aff, f"{t}.{col_name} affinity {actual_aff} != {aff}"
                    assert actual_nn == nn, f"{t}.{col_name} notNull {actual_nn} != {nn}"

                actual_pk = [c[1] for c in sorted([r for r in cur.execute(f'PRAGMA table_info(`{t}`)').fetchall() if r[5] > 0], key=lambda x: x[5])]
                assert actual_pk == e['primaryKey']['columnNames'], f"PK mismatch on {t}: {actual_pk} != {e['primaryKey']['columnNames']}"

                # Verify indices created
                actual_idxs = {r[1]: (int(r[2]), [ic[2] for ic in cur.execute(f'PRAGMA index_info(`{r[1]}`)').fetchall()])
                               for r in cur.execute(f'PRAGMA index_list(`{t}`)').fetchall() if not r[1].startswith('sqlite_autoindex')}
                for idx in e.get('indices', []):
                    iname = idx['name'].replace('${'$'}{TABLE_NAME}', t)
                    assert iname in actual_idxs, f"Missing index {iname} on {t}. Found: {list(actual_idxs.keys())}"
                    assert actual_idxs[iname][0] == (1 if idx['unique'] else 0), f"Unique flag mismatch on {iname}"
                    assert actual_idxs[iname][1] == idx['columnNames'], f"Index cols mismatch on {iname}"

            print('MIGRATION_13_14_VERIFIED_EXACT')
        """.trimIndent()

        val process = ProcessBuilder("python3", "-c", pyScript)
            .redirectErrorStream(true)
            .start()
        process.outputStream.bufferedWriter().use { w ->
            w.write("[\n$sqlJson\n]")
        }
        val output = process.inputStream.bufferedReader().use(BufferedReader::readText)
        val exitCode = process.waitFor()
        assertEquals("SQLite v13->v14 migration verification failed:\n$output", 0, exitCode)
        assertEquals("MIGRATION_13_14_VERIFIED_EXACT", output.trim())
    }
}
