package com.pocketdev.advance_erp.core.database

/**
 * Portable DataStore preferences included in comprehensive backup archives.
 * Device-bound security secrets (PIN hash/salt, biometric lock) and SAF folder URIs are excluded.
 */
data class PortableBackupPreferences(
    val appTheme: String? = null,
    val appLanguage: String? = null,
    val appCurrency: String? = null,
    val userName: String? = null,
    val userRole: String? = null,
    val profilePhotoRelativePath: String? = null,
    val companyLogoRelativePath: String? = null,
    val fridayAutoBackupEnabled: Boolean? = null,
    val legacyMode: Boolean = false
)

/**
 * Pure-JVM backup archive contract shared across backup creation, validation, staged restore,
 * and unit tests without Android framework or `org.json` dependencies.
 */
object BackupArchiveContract {
    const val BACKUP_FORMAT_VERSION = 2

    const val DB_ENTRY = "database/advance_erp.db"
    const val METADATA_ENTRY = "metadata.json"
    const val PREFERENCES_ENTRY = "preferences.json"
    const val CONTROL_CENTER_PRESETS_ENTRY = "control_center_presets.json"

    val LEGACY_MEDIA_DIRS = listOf(
        "product_images",
        "po_attachments",
        "vault_attachments"
    )

    val V2_EXTRA_MEDIA_DIRS = listOf(
        "customer_photos",
        "company_logo",
        "company_signature"
    )

    val MANAGED_MEDIA_DIRS = LEGACY_MEDIA_DIRS + V2_EXTRA_MEDIA_DIRS
    val ALLOWED_ZIP_DIRS = MANAGED_MEDIA_DIRS + "avatars"
    val ALLOWED_ROOT_FILES = setOf(METADATA_ENTRY, PREFERENCES_ENTRY, CONTROL_CENTER_PRESETS_ENTRY)

    private val MANAGED_MEDIA_DIR_SET = MANAGED_MEDIA_DIRS.toSet()
    private val ALLOWED_ZIP_DIR_SET = ALLOWED_ZIP_DIRS.toSet()
    private val ALLOWED_DIR_ENTRIES = ALLOWED_ZIP_DIRS.map { "$it/" }.toSet()

    fun isSafeBackupZipEntry(name: String): Boolean {
        if (name.isBlank() || name.startsWith('/') || '\\' in name) return false
        val parts = name.split('/')
        if (parts.any { it == "." || it == ".." }) return false
        if (name == DB_ENTRY || name in ALLOWED_ROOT_FILES || name in ALLOWED_DIR_ENTRIES) return true
        return parts.size == 2 && parts[1].isNotBlank() && parts[0] in ALLOWED_ZIP_DIR_SET
    }

    fun extractManagedRelativePath(rawPath: String?, filesDirPath: String): String? {
        val clean = stripFileScheme(rawPath) ?: return null
        if (isExternalScheme(clean) || '\\' in clean) return null

        val normalizedBase = filesDirPath.trimEnd('/')
        val remainder = when {
            normalizedBase.isNotEmpty() && clean.startsWith("$normalizedBase/") ->
                clean.removePrefix("$normalizedBase/")
            clean.contains("/files/") ->
                clean.substringAfterLast("/files/")
            else -> return null
        }

        if (remainder.isBlank()) return null
        val parts = remainder.split('/')
        if (parts.any { it.isBlank() || it == "." || it == ".." }) return null

        return when (parts.size) {
            1 -> parts[0]
            2 -> if (parts[0] in MANAGED_MEDIA_DIR_SET) "${parts[0]}/${parts[1]}" else null
            else -> null
        }
    }

    fun anchorManagedRelativePath(relativePath: String?, targetFilesDirPath: String): String? {
        val rel = relativePath?.trim()?.takeIf { it.isNotBlank() } ?: return null
        if (rel.startsWith('/') || rel.endsWith('/') || '\\' in rel) return null
        val parts = rel.split('/')
        if (parts.any { it.isBlank() || it == "." || it == ".." }) return null

        val base = targetFilesDirPath.trimEnd('/')
        return when (parts.size) {
            1 -> "$base/${parts[0]}"
            2 -> if (parts[0] in MANAGED_MEDIA_DIR_SET) "$base/${parts[0]}/${parts[1]}" else null
            else -> null
        }
    }

    fun rewriteManagedSinglePath(
        rawPath: String?,
        targetFilesDirPath: String,
        expectedSubdir: String
    ): String? {
        if (expectedSubdir !in MANAGED_MEDIA_DIR_SET) return null
        val clean = stripFileScheme(rawPath) ?: return null
        if (isExternalScheme(clean)) return null
        val marker = "/files/$expectedSubdir/"
        if (!clean.contains(marker)) return null
        val fileName = clean.substringAfterLast(marker)
        if (!isSafeSingleFileName(fileName)) return null
        return "${targetFilesDirPath.trimEnd('/')}/$expectedSubdir/$fileName"
    }

    fun rewriteManagedAnySubdirPath(rawPath: String?, targetFilesDirPath: String): String? {
        for (subdir in MANAGED_MEDIA_DIRS) {
            val rewritten = rewriteManagedSinglePath(rawPath, targetFilesDirPath, subdir)
            if (rewritten != null) return rewritten
        }
        return null
    }

    fun rewriteManagedRootPrefixPath(
        rawPath: String?,
        targetFilesDirPath: String,
        prefix: String
    ): String? {
        val clean = stripFileScheme(rawPath) ?: return null
        if (isExternalScheme(clean)) return null
        val marker = "/files/$prefix"
        if (!clean.contains(marker)) return null
        val fileName = clean.substringAfterLast("/files/")
        if (!fileName.startsWith(prefix) || !isSafeSingleFileName(fileName)) return null
        return "${targetFilesDirPath.trimEnd('/')}/$fileName"
    }

    fun rewriteManagedCsvPaths(
        rawCsv: String?,
        targetFilesDirPath: String,
        expectedSubdir: String
    ): String? {
        if (rawCsv.isNullOrBlank()) return null
        val items = rawCsv.split(',')
            .map { it.trim() }
            .filter { it.isNotBlank() }
        if (items.isEmpty()) return null

        val updated = items.map { item ->
            if (isExternalScheme(item)) {
                item
            } else {
                rewriteManagedSinglePath(item, targetFilesDirPath, expectedSubdir) ?: item
            }
        }.distinct()

        val joined = updated.joinToString(",")
        return joined.takeIf { it != rawCsv }
    }

    fun encodePreferencesJson(prefs: PortableBackupPreferences): String = buildString {
        append("{\n")
        appendField("appTheme", prefs.appTheme, trailingComma = true)
        appendField("appLanguage", prefs.appLanguage, trailingComma = true)
        appendField("appCurrency", prefs.appCurrency, trailingComma = true)
        appendField("userName", prefs.userName, trailingComma = true)
        appendField("userRole", prefs.userRole, trailingComma = true)
        appendField("profilePhotoRelativePath", prefs.profilePhotoRelativePath, trailingComma = true)
        appendField("companyLogoRelativePath", prefs.companyLogoRelativePath, trailingComma = true)
        append("  \"fridayAutoBackupEnabled\": ").append(prefs.fridayAutoBackupEnabled?.toString() ?: "null").append(",\n")
        append("  \"legacyMode\": ").append(prefs.legacyMode).append('\n')
        append('}')
    }

    fun decodePreferencesJson(raw: String): PortableBackupPreferences {
        val map = MinimalJsonObjectParser(raw).parseRootObject()
        return PortableBackupPreferences(
            appTheme = map.optionalString("appTheme"),
            appLanguage = map.optionalString("appLanguage"),
            appCurrency = map.optionalString("appCurrency"),
            userName = map.optionalString("userName"),
            userRole = map.optionalString("userRole"),
            profilePhotoRelativePath = map.optionalString("profilePhotoRelativePath"),
            companyLogoRelativePath = map.optionalString("companyLogoRelativePath"),
            fridayAutoBackupEnabled = map.optionalBoolean("fridayAutoBackupEnabled"),
            legacyMode = map.optionalBoolean("legacyMode") ?: false
        )
    }

    private fun stripFileScheme(rawPath: String?): String? {
        val trimmed = rawPath?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return if (trimmed.startsWith("file://", ignoreCase = true)) {
            trimmed.substring(7).trim()
        } else {
            trimmed
        }
    }

    private fun isExternalScheme(value: String): Boolean =
        value.startsWith("http://", ignoreCase = true) ||
            value.startsWith("https://", ignoreCase = true) ||
            value.startsWith("content://", ignoreCase = true)

    private fun isSafeSingleFileName(fileName: String): Boolean =
        fileName.isNotBlank() &&
            '/' !in fileName &&
            '\\' !in fileName &&
            fileName != "." &&
            fileName != ".."

    private fun StringBuilder.appendField(key: String, value: String?, trailingComma: Boolean) {
        append("  \"").append(escapeJsonString(key)).append("\": ")
        if (value == null) {
            append("null")
        } else {
            append('"').append(escapeJsonString(value)).append('"')
        }
        if (trailingComma) append(',')
        append('\n')
    }

    private fun escapeJsonString(value: String): String = buildString(value.length + 8) {
        for (ch in value) {
            when (ch) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (ch.code < 0x20) {
                    append("\\u%04x".format(ch.code))
                } else {
                    append(ch)
                }
            }
        }
    }

    private fun Map<String, Any?>.optionalString(key: String): String? {
        val v = this[key] ?: return null
        return v as? String ?: throw IllegalArgumentException("Expected string or null for '$key'")
    }

    private fun Map<String, Any?>.optionalBoolean(key: String): Boolean? {
        val v = this[key] ?: return null
        return v as? Boolean ?: throw IllegalArgumentException("Expected boolean or null for '$key'")
    }
}

private class MinimalJsonObjectParser(private val src: String) {
    private var idx = 0

    fun parseRootObject(): Map<String, Any?> {
        skipWs()
        val obj = readObject()
        skipWs()
        require(idx == src.length) { "Unexpected trailing content in preferences JSON at index $idx" }
        return obj
    }

    private fun readObject(): Map<String, Any?> {
        expect('{')
        skipWs()
        val result = LinkedHashMap<String, Any?>()
        if (peek() == '}') {
            idx++
            return result
        }
        while (true) {
            skipWs()
            val key = readString()
            skipWs()
            expect(':')
            skipWs()
            val value = readValue()
            result[key] = value
            skipWs()
            when (peek()) {
                ',' -> idx++
                '}' -> {
                    idx++
                    return result
                }
                else -> throw IllegalArgumentException("Expected ',' or '}' at index $idx")
            }
        }
    }

    private fun readArray(): List<Any?> {
        expect('[')
        skipWs()
        val list = ArrayList<Any?>()
        if (peek() == ']') {
            idx++
            return list
        }
        while (true) {
            skipWs()
            list.add(readValue())
            skipWs()
            when (peek()) {
                ',' -> idx++
                ']' -> {
                    idx++
                    return list
                }
                else -> throw IllegalArgumentException("Expected ',' or ']' at index $idx")
            }
        }
    }

    private fun readValue(): Any? {
        skipWs()
        return when (val ch = peek()) {
            '"' -> readString()
            '{' -> readObject()
            '[' -> readArray()
            't' -> {
                expectLiteral("true")
                true
            }
            'f' -> {
                expectLiteral("false")
                false
            }
            'n' -> {
                expectLiteral("null")
                null
            }
            '-', in '0'..'9' -> readNumberToken()
            else -> throw IllegalArgumentException("Unexpected JSON token '$ch' at index $idx")
        }
    }

    private fun readString(): String {
        expect('"')
        val out = StringBuilder()
        while (idx < src.length) {
            val ch = src[idx++]
            when (ch) {
                '"' -> return out.toString()
                '\\' -> {
                    require(idx < src.length) { "Unterminated JSON escape" }
                    when (val esc = src[idx++]) {
                        '"' -> out.append('"')
                        '\\' -> out.append('\\')
                        '/' -> out.append('/')
                        'b' -> out.append('\b')
                        'f' -> out.append('\u000C')
                        'n' -> out.append('\n')
                        'r' -> out.append('\r')
                        't' -> out.append('\t')
                        'u' -> {
                            require(idx + 4 <= src.length) { "Incomplete unicode escape at index $idx" }
                            val hex = src.substring(idx, idx + 4)
                            val code = hex.toIntOrNull(16)
                                ?: throw IllegalArgumentException("Invalid unicode escape '\\u$hex'")
                            out.append(code.toChar())
                            idx += 4
                        }
                        else -> throw IllegalArgumentException("Invalid JSON escape '\\$esc'")
                    }
                }
                else -> {
                    require(ch.code >= 0x20) { "Unescaped control character in JSON string" }
                    out.append(ch)
                }
            }
        }
        throw IllegalArgumentException("Unterminated JSON string")
    }

    private fun readNumberToken(): Double {
        val start = idx
        if (peek() == '-') idx++
        require(idx < src.length && src[idx] in '0'..'9') { "Invalid JSON number at index $start" }
        while (idx < src.length && src[idx] in '0'..'9') idx++
        if (idx < src.length && src[idx] == '.') {
            idx++
            require(idx < src.length && src[idx] in '0'..'9') { "Invalid JSON fraction at index $idx" }
            while (idx < src.length && src[idx] in '0'..'9') idx++
        }
        if (idx < src.length && (src[idx] == 'e' || src[idx] == 'E')) {
            idx++
            if (idx < src.length && (src[idx] == '+' || src[idx] == '-')) idx++
            require(idx < src.length && src[idx] in '0'..'9') { "Invalid JSON exponent at index $idx" }
            while (idx < src.length && src[idx] in '0'..'9') idx++
        }
        return src.substring(start, idx).toDouble()
    }

    private fun expectLiteral(lit: String) {
        require(src.regionMatches(idx, lit, 0, lit.length)) { "Expected '$lit' at index $idx" }
        idx += lit.length
    }

    private fun expect(expected: Char) {
        require(idx < src.length && src[idx] == expected) { "Expected '$expected' at index $idx" }
        idx++
    }

    private fun peek(): Char? = if (idx < src.length) src[idx] else null

    private fun skipWs() {
        while (idx < src.length && src[idx] in charArrayOf(' ', '\n', '\r', '\t')) {
            idx++
        }
    }
}
