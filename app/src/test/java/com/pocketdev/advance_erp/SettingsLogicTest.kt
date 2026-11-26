package com.pocketdev.advance_erp

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.pocketdev.advance_erp.core.database.AdvanceErpDatabase
import com.pocketdev.advance_erp.core.designsystem.DarkSettingsTokens
import com.pocketdev.advance_erp.core.designsystem.LightSettingsTokens
import com.pocketdev.advance_erp.core.model.CurrencyCode
import com.pocketdev.advance_erp.feature.settings.components.SettingsCategory
import com.pocketdev.advance_erp.feature.settings.components.requiresCurrencyChangeConfirmation
import com.pocketdev.advance_erp.feature.settings.components.toggleCategoryExpanded
import com.pocketdev.advance_erp.feature.settings.components.validateBusinessProfileName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.BufferedReader
import java.io.File

class SettingsLogicTest {

    @Test
    fun accordionState_supportsAllCollapsedDefaultAndMultiExpand() {
        val initial = emptySet<SettingsCategory>()
        assertTrue(initial.isEmpty())

        val step1 = toggleCategoryExpanded(initial, SettingsCategory.APPEARANCE_LANGUAGE)
        val step2 = toggleCategoryExpanded(step1, SettingsCategory.BUSINESS_PROFILE)
        assertEquals(
            setOf(SettingsCategory.APPEARANCE_LANGUAGE, SettingsCategory.BUSINESS_PROFILE),
            step2
        )

        val step3 = toggleCategoryExpanded(step2, SettingsCategory.APPEARANCE_LANGUAGE)
        assertEquals(setOf(SettingsCategory.BUSINESS_PROFILE), step3)
    }

    @Test
    fun validateBusinessProfileName_rejectsBlankStrings() {
        assertFalse(validateBusinessProfileName(""))
        assertFalse(validateBusinessProfileName("   "))
        assertTrue(validateBusinessProfileName("My Business"))
        assertTrue(validateBusinessProfileName("অ্যাডভান্স ট্রেডার্স"))
    }

    @Test
    fun currencyConfirmation_onlyPromptsOnActualChange() {
        assertFalse(requiresCurrencyChangeConfirmation(CurrencyCode.BDT, CurrencyCode.BDT))
        assertTrue(requiresCurrencyChangeConfirmation(CurrencyCode.BDT, CurrencyCode.USD))
        assertTrue(requiresCurrencyChangeConfirmation(CurrencyCode.USD, CurrencyCode.CNY))
    }

    @Test
    fun settingsTokens_enforceContrastAndNonPureBlackDarkMode() {
        assertFalse(LightSettingsTokens.isDark)
        assertTrue(DarkSettingsTokens.isDark)

        assertNotEquals(Color.Black, DarkSettingsTokens.pageBgTop)
        assertNotEquals(Color.Black, DarkSettingsTokens.cardBg)
        assertTrue(DarkSettingsTokens.cardBg.luminance() < 0.08f)
        assertTrue(DarkSettingsTokens.titleText.luminance() > 0.85f)

        assertTrue(LightSettingsTokens.cardBg.luminance() > 0.90f)
        assertTrue(LightSettingsTokens.titleText.luminance() < 0.12f)
    }

    @Test
    fun databaseVersionConstant_matchesRoomSchemaV15() {
        assertEquals(15, AdvanceErpDatabase.DATABASE_VERSION)
    }

    @Test
    fun resetTestTransactionsKeepMasters_preservesProductsAndContactsAndClearsStockAndLedgersWithoutFkErrors() {
        val schemaFile = File("schemas/com.pocketdev.advance_erp.core.database.AdvanceErpDatabase/15.json")
        val daoFile = File("src/main/java/com/pocketdev/advance_erp/data/dao/BackupDao.kt")
        val commDaoFile = File("src/main/java/com/pocketdev/advance_erp/data/dao/CommunicationDao.kt")
        val repoFile = File("src/main/java/com/pocketdev/advance_erp/data/repository/BackupRepositoryImpl.kt")
        assertTrue("Schema 15.json must exist at ${schemaFile.absolutePath}", schemaFile.exists())
        assertTrue("BackupDao.kt must exist at ${daoFile.absolutePath}", daoFile.exists())
        assertTrue("CommunicationDao.kt must exist at ${commDaoFile.absolutePath}", commDaoFile.exists())
        assertTrue("BackupRepositoryImpl.kt must exist at ${repoFile.absolutePath}", repoFile.exists())

        val script = """
            import json, re, sqlite3

            s = json.load(open(r"${schemaFile.absolutePath}"))['database']
            assert len(s['entities']) == 62, f"Expected 62 entities in 15.json, got {len(s['entities'])}"
            conn = sqlite3.connect(':memory:')
            cur = conn.cursor()
            cur.execute('PRAGMA foreign_keys = ON')
            for e in s['entities']:
                cur.execute(e['createSql'].replace('${'$'}{TABLE_NAME}', e['tableName']))

            entities = {e['tableName']: e for e in s['entities']}
            inserted = set()

            def insert_entity(tname):
                if tname in inserted:
                    return
                e = entities[tname]
                for fk in e.get('foreignKeys', []):
                    if fk['table'] != tname:
                        insert_entity(fk['table'])
                fk_map = {}
                for fk in e.get('foreignKeys', []):
                    for col, ref_col in zip(fk['columns'], fk['referencedColumns']):
                        fk_map[col] = (fk['table'], ref_col)
                cols = []
                vals = []
                for f in e['fields']:
                    cname = f['columnName']
                    cols.append(f'`{cname}`')
                    if cname in fk_map:
                        ref_tbl, ref_col = fk_map[cname]
                        vals.append(f'ref-{ref_tbl}-1' if ref_col == 'id' else '1')
                    elif f['affinity'] == 'INTEGER':
                        if cname == 'isActive':
                            vals.append(0 if tname in ('customers', 'suppliers') else 1)
                        elif cname in ('outstandingBalanceMinor', 'outstandingPayableMinor'):
                            vals.append(50000)
                        else:
                            vals.append(1)
                    elif f['affinity'] == 'REAL':
                        vals.append(1.0)
                    elif cname == 'id':
                        vals.append(f'ref-{tname}-1')
                    elif cname == 'recordType' and tname == 'audit_logs':
                        vals.append('SALE')
                    else:
                        vals.append(f'{tname}-{cname}-1')
                placeholders = ','.join(['?'] * len(cols))
                cur.execute(f"INSERT INTO `{tname}` ({','.join(cols)}) VALUES ({placeholders})", vals)
                inserted.add(tname)

            for t in entities:
                insert_entity(t)

            cur.execute("INSERT INTO audit_logs (id, recordType, recordId, action, details, operator, timestamp) VALUES ('audit-bak-1', 'BACKUP', 'bak-1', 'BACKUP', 'Backup kept', 'SYSTEM_ADMIN', 1)")

            dao_src = open(r"${daoFile.absolutePath}").read()
            sql_by_method = {}
            for m in re.finditer(r'@Query\("([^"]+)"\)\s+suspend fun (\w+)\(', dao_src):
                sql_by_method[m.group(2)] = m.group(1)

            comm_dao_src = open(r"${commDaoFile.absolutePath}").read()
            comm_sql_by_method = {}
            for m in re.finditer(r'@Query\("([^"]+)"\)\s+suspend fun (\w+)\(', comm_dao_src):
                comm_sql_by_method[m.group(2)] = m.group(1)

            repo_src = open(r"${repoFile.absolutePath}").read()
            reset_body = repo_src.split('override suspend fun resetTestTransactionsKeepMasters()')[1].split('auditLogDao.insertAuditLog')[0]
            calls = re.findall(r'backupDao\.(\w+)\(\)', reset_body)
            comm_calls = re.findall(r'communicationDao\(\)\.(\w+)\(\)', reset_body)
            assert len(calls) == 47, f'Expected 47 reset DAO calls, got {len(calls)}'
            assert len(comm_calls) == 2, f'Expected 2 communicationDao calls, got {len(comm_calls)}'

            for fn in calls:
                cur.execute(sql_by_method[fn])
            for fn in comm_calls:
                cur.execute(comm_sql_by_method[fn])

            cur.execute('PRAGMA foreign_key_check')
            assert cur.fetchall() == [], 'Foreign key check must pass with zero violations'

            preserved = {
                'business_profiles', 'product_categories', 'products', 'product_variants',
                'product_images', 'warehouses', 'customers', 'suppliers', 'app_settings',
                'chart_of_accounts', 'backup_records', 'saved_suggestions',
                'communication_settings', 'communication_templates', 'communication_party_preferences'
            }
            for t in entities:
                cnt = cur.execute(f'SELECT COUNT(*) FROM `{t}`').fetchone()[0]
                if t in preserved:
                    assert cnt == 1, f'{t} should preserve 1 row, got {cnt}'
                elif t == 'audit_logs':
                    assert cnt == 1, f'audit_logs should keep 1 BACKUP row, got {cnt}'
                else:
                    assert cnt == 0, f'{t} should be cleared to 0, got {cnt}'

            assert cur.execute('SELECT outstandingBalanceMinor, isActive FROM customers').fetchone() == (0, 1)
            assert cur.execute('SELECT outstandingPayableMinor, isActive FROM suppliers').fetchone() == (0, 1)
            print('RESET_VERIFIED_62_TABLES')
        """.trimIndent()

        val process = ProcessBuilder("python3", "-c", script)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().use(BufferedReader::readText)
        val exitCode = process.waitFor()
        assertEquals("SQLite test-reset verification failed:\n$output", 0, exitCode)
        assertEquals("RESET_VERIFIED_62_TABLES", output.trim())
    }
}
