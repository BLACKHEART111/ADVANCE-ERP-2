package com.pocketdev.advance_erp.core.communication

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.pocketdev.advance_erp.core.communication.image.BrandedImageCardGenerator
import com.pocketdev.advance_erp.core.communication.pdf.BrandedCommunicationPdfGenerator
import com.pocketdev.advance_erp.core.communication.pdf.BrandedPdfSpecBuilder
import com.pocketdev.advance_erp.core.database.AdvanceErpDatabase
import com.pocketdev.advance_erp.data.entity.CommunicationSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentAttachmentProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AdvanceErpDatabase
) {
    private val retentionMs = 72L * 60 * 60 * 1000

    private fun cleanupExpiredCache(root: File, keepEntry: File) {
        val now = System.currentTimeMillis()
        root.listFiles()
            ?.filter { it != keepEntry && now - it.lastModified() > retentionMs }
            ?.forEach { it.deleteRecursively() }
    }

    suspend fun xlsx(document: SafeDocument): File = withContext(Dispatchers.IO) {
        require(document.request.module in setOf(CommunicationModule.PURCHASE_ORDER, CommunicationModule.PACKING))
        val po = requireNotNull(db.purchaseOrderDao().getOrderById(document.request.documentId)).copy(notes = null)
        val items = db.purchaseOrderDao().getItemsForOrderSync(po.id)
        val products = items.mapNotNull { db.productDao().getProductById(it.productId) }.associateBy { it.id }
        val supplier = db.supplierDao().getSupplierById(po.supplierId)
        val profile = db.businessProfileDao().getProfileSync()
        val variants = db.productVariantDao().getAllVariantsSync().associateBy { it.id }
        val cartons = db.importShipmentDao().getAllCartonsSync().filter { it.purchaseOrderId == po.id }
        val source = if (document.request.module == CommunicationModule.PURCHASE_ORDER) {
            com.pocketdev.advance_erp.core.util.ImportExcelGenerator.generateSupplierOrderExcel(
                context, po, items, products, supplier, profile, variants, cartons = cartons
            )
        } else {
            com.pocketdev.advance_erp.core.util.ImportExcelGenerator.generateFreightForwarderPackingListExcel(
                context, po, items, products, supplier, profile, variants, cartons = cartons
            )
        }
        val root = File(context.cacheDir, "communication").apply { mkdirs() }
        val batchDir = File(root, UUID.randomUUID().toString().take(8)).apply { mkdirs() }
        val target = File(batchDir, "${BrandedPdfSpecBuilder.safeFileStem(document)}.xlsx")
        source.copyTo(target, overwrite = true)
        source.delete()
        cleanupExpiredCache(root, batchDir)
        target
    }

    suspend fun pdf(
        document: SafeDocument,
        language: String,
        settings: CommunicationSettings? = null
    ): File = withContext(Dispatchers.IO) {
        val effectiveSettings = settings ?: db.communicationDao().settings() ?: CommunicationSettings()
        val root = File(context.cacheDir, "communication").apply { mkdirs() }
        val batchDir = File(root, UUID.randomUUID().toString().take(8)).apply { mkdirs() }
        val target = File(batchDir, "${BrandedPdfSpecBuilder.safeFileStem(document)}.pdf")
        BrandedCommunicationPdfGenerator.generate(
            context = context,
            document = document,
            settings = effectiveSettings,
            language = language,
            outputFile = target
        )
        cleanupExpiredCache(root, batchDir)
        target
    }

    suspend fun imageCards(
        document: SafeDocument,
        language: String,
        settings: CommunicationSettings? = null
    ): List<File> = withContext(Dispatchers.IO) {
        val effectiveSettings = settings ?: db.communicationDao().settings() ?: CommunicationSettings()
        val root = File(context.cacheDir, "communication").apply { mkdirs() }
        val batchDir = File(root, UUID.randomUUID().toString().take(8)).apply { mkdirs() }
        val files = BrandedImageCardGenerator.generatePages(
            context = context,
            document = document,
            settings = effectiveSettings,
            language = language,
            outputDir = batchDir
        )
        cleanupExpiredCache(root, batchDir)
        files
    }

    suspend fun saveImagesToGallery(files: List<File>): List<String> = withContext(Dispatchers.IO) {
        BrandedImageCardGenerator.saveToGallery(context, files)
    }

    suspend fun saveDocumentToDownloads(sourceFile: File): String = withContext(Dispatchers.IO) {
        require(sourceFile.isFile && sourceFile.length() > 0L) {
            "File unavailable to save / সংরক্ষণের জন্য ফাইল পাওয়া যায়নি"
        }
        val mimeType = WhatsAppShareManager.mimeTypeForFile(sourceFile)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, sourceFile.name)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/AdvanceERP")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                try {
                    val outStream = resolver.openOutputStream(uri)
                        ?: error("Unable to open Downloads output stream")
                    outStream.use { out ->
                        FileInputStream(sourceFile).use { input ->
                            input.copyTo(out)
                            out.flush()
                        }
                    }
                    values.clear()
                    values.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                    return@withContext "Downloads/AdvanceERP/${sourceFile.name}"
                } catch (e: Exception) {
                    runCatching { resolver.delete(uri, null, null) }
                    throw e
                }
            }
        }
        val targetDir = runCatching {
            @Suppress("DEPRECATION")
            val pub = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            File(pub, "AdvanceERP").apply { mkdirs() }
        }.getOrElse {
            File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "AdvanceERP").apply { mkdirs() }
        }
        val targetFile = File(targetDir, sourceFile.name)
        FileInputStream(sourceFile).use { input ->
            FileOutputStream(targetFile).use { out ->
                input.copyTo(out)
                out.flush()
            }
        }
        MediaScannerConnection.scanFile(context, arrayOf(targetFile.absolutePath), arrayOf(mimeType), null)
        targetFile.absolutePath
    }

    suspend fun getCacheStats(): CommunicationCacheStats = withContext(Dispatchers.IO) {
        val root = File(context.cacheDir, "communication")
        if (!root.exists()) return@withContext CommunicationCacheStats()
        var count = 0
        var bytes = 0L
        root.walkTopDown().forEach { file ->
            if (file.isFile) {
                count++
                bytes += file.length()
            }
        }
        CommunicationCacheStats(fileCount = count, totalBytes = bytes)
    }

    suspend fun clearCache(): Int = withContext(Dispatchers.IO) {
        val root = File(context.cacheDir, "communication")
        if (!root.exists()) return@withContext 0
        val count = root.walkTopDown().count { it.isFile }
        root.listFiles()?.forEach { it.deleteRecursively() }
        count
    }
}
