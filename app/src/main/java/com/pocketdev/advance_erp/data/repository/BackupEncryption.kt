package com.pocketdev.advance_erp.data.repository

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

internal object BackupEncryption {
    private val magic = "AERPENC1".toByteArray(Charsets.US_ASCII)
    private const val SALT_SIZE = 16
    private const val IV_SIZE = 12
    private const val ITERATIONS = 250_000
    private const val BUFFER_SIZE = 64 * 1024

    fun isEncrypted(file: File): Boolean {
        if (!file.isFile || file.length() < magic.size) return false
        return try {
            DataInputStream(FileInputStream(file)).use { input ->
                val header = ByteArray(magic.size)
                input.readFully(header)
                header.contentEquals(magic)
            }
        } catch (_: Exception) {
            false
        }
    }

    fun encrypt(inputFile: File, outputFile: File, passphrase: CharArray) {
        require(passphrase.size >= 8) { "Backup passphrase must have at least 8 characters" }
        val random = SecureRandom()
        val salt = ByteArray(SALT_SIZE).also { random.nextBytes(it) }
        val iv = ByteArray(IV_SIZE).also { random.nextBytes(it) }
        val cipher = newCipher(Cipher.ENCRYPT_MODE, passphrase, salt, iv)
        try {
            DataOutputStream(FileOutputStream(outputFile)).use { output ->
                output.write(magic)
                output.write(salt)
                output.write(iv)
                FileInputStream(inputFile).use { input ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        val encrypted = cipher.update(buffer, 0, count)
                        if (encrypted != null) output.write(encrypted)
                    }
                    output.write(cipher.doFinal())
                }
            }
        } catch (e: Exception) {
            outputFile.delete()
            throw e
        }
    }

    fun decrypt(inputFile: File, outputFile: File, passphrase: CharArray) {
        require(passphrase.isNotEmpty()) { "Backup passphrase is required" }
        try {
            DataInputStream(FileInputStream(inputFile)).use { input ->
                val header = ByteArray(magic.size)
                input.readFully(header)
                require(header.contentEquals(magic)) { "Unsupported encrypted backup format" }
                val salt = ByteArray(SALT_SIZE).also { input.readFully(it) }
                val iv = ByteArray(IV_SIZE).also { input.readFully(it) }
                val cipher = newCipher(Cipher.DECRYPT_MODE, passphrase, salt, iv)
                FileOutputStream(outputFile).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        val decrypted = cipher.update(buffer, 0, count)
                        if (decrypted != null) output.write(decrypted)
                    }
                    output.write(cipher.doFinal())
                }
            }
        } catch (e: Exception) {
            outputFile.delete()
            if (e is AEADBadTagException) throw IllegalArgumentException("Incorrect passphrase or damaged backup", e)
            throw e
        }
    }

    private fun newCipher(mode: Int, passphrase: CharArray, salt: ByteArray, iv: ByteArray): Cipher {
        val spec = PBEKeySpec(passphrase, salt, ITERATIONS, 256)
        val keyBytes = try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
        return try {
            Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(mode, SecretKeySpec(keyBytes, "AES"), GCMParameterSpec(128, iv))
            }
        } finally {
            keyBytes.fill(0)
        }
    }
}
