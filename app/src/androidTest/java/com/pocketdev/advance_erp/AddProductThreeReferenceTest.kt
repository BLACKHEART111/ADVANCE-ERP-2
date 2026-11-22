package com.pocketdev.advance_erp

import android.app.ActivityManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pocketdev.advance_erp.core.database.AdvanceErpDatabase
import com.pocketdev.advance_erp.core.designsystem.AdvanceErpTheme
import com.pocketdev.advance_erp.core.designsystem.LocalERPOnNavigate
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.util.ProductImageStorage
import com.pocketdev.advance_erp.core.util.resolveVariantPhotoUris
import com.pocketdev.advance_erp.data.entity.Product
import com.pocketdev.advance_erp.data.entity.ProductCategory
import com.pocketdev.advance_erp.data.entity.ProductImage
import com.pocketdev.advance_erp.data.entity.ProductVariant
import com.pocketdev.advance_erp.data.entity.StockMovementType
import com.pocketdev.advance_erp.data.entity.Warehouse
import com.pocketdev.advance_erp.data.repository.ProductRepositoryImpl
import com.pocketdev.advance_erp.feature.product.AddProductDialog
import com.pocketdev.advance_erp.feature.product.GalleryItemDraft
import com.pocketdev.advance_erp.feature.product.NewVariantDraft
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileInputStream
import java.util.Locale

/**
 * Device UI verification, functional workflow tests, and isolated in-memory SQLite persistence tests
 * for the 3-step Add Product redesign. Never touches the user's live database.
 */
@RunWith(AndroidJUnit4::class)
class AddProductThreeReferenceTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun sampleCategories() = listOf(
        ProductCategory(id = "cat-uncategorized", nameEn = "Uncategorized", nameBn = "শ্রেণীবিহীন", code = "UNCAT"),
        ProductCategory(id = "cat-elec", nameEn = "Electronics", nameBn = "ইলেকট্রনিক্স", code = "ELEC")
    )

    private fun sampleWarehouses() = listOf(
        Warehouse(id = "wh-main", name = "Main Warehouse", code = "WH-MAIN", location = "Dhaka", isDefault = true),
        Warehouse(id = "wh-ctg", name = "Chattogram Hub", code = "WH-CTG", location = "Chattogram", isDefault = false)
    )

    @Test
    fun lightThreeStepsAndVariantsCapture() {
        renderReferenceCapture(dark = false, captureWithVariants = true)
    }

    @Test
    fun darkThreeStepsCapture() {
        renderReferenceCapture(dark = true, captureWithVariants = false)
    }

    private fun renderReferenceCapture(dark: Boolean, captureWithVariants: Boolean) {
        val categories = sampleCategories()
        val warehouses = sampleWarehouses()
        val suffix = if (dark) "dark" else "light"

        compose.setContent {
            val base = LocalContext.current
            val cfg = Configuration(LocalConfiguration.current).apply { setLocale(Locale("bn")) }
            CompositionLocalProvider(
                LocalContext provides base.createConfigurationContext(cfg),
                LocalConfiguration provides cfg,
                LocalERPOnNavigate provides {}
            ) {
                AdvanceErpTheme(darkTheme = dark) {
                    AddProductDialog(
                        categories = categories,
                        warehouses = warehouses,
                        language = AppLanguage.BANGLA,
                        onDismiss = {},
                        onGenerateSku = { _, _ -> "UNC-PRD-001" },
                        onGenerateVariantSku = { baseSku, color, size ->
                            listOfNotNull(baseSku.ifBlank { "PRD" }, color?.take(3)?.uppercase(), size?.uppercase())
                                .joinToString("-")
                        },
                        onGenerateBarcode = { "8901234567890" },
                        onLoadCategoryProducts = { emptyList() },
                        onConfirm = { _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> }
                    )
                }
            }
        }

        compose.onNodeWithText("১. মূল তথ্য").assertExists()
        capture("01_add_product_step1_$suffix")
        compose.onNodeWithTag("add_product_base_sku").performScrollTo()
        capture("01b_add_product_step1_bottom_$suffix")

        compose.onNodeWithTag("add_product_next_btn").performClick()
        compose.onNodeWithText("ভ্যারিয়েন্ট ব্যবহার করবেন?").performScrollTo().assertExists()
        compose.onNodeWithTag("add_product_variant_switch").performClick()
        capture("02b_add_product_step2_bottom_$suffix")
        compose.onNodeWithTag("add_product_step2_top").performScrollTo()
        capture("02_add_product_step2_$suffix")

        compose.onNodeWithTag("add_product_next_btn").performClick()
        compose.onNodeWithText("নতুন ভ্যারিয়েন্ট তথ্য যোগ করুন").assertExists()
        capture("03_add_product_step3_$suffix")
        compose.onNodeWithTag("add_product_save_variant_btn").performScrollTo()
        capture("03b_add_product_step3_bottom_$suffix")

        if (captureWithVariants) {
            compose.onNodeWithTag("add_product_variant_color").performScrollTo().performTextInput("Red")
            compose.onNodeWithTag("add_product_variant_size").performTextInput("M")
            compose.onNodeWithTag("add_product_variant_sku").performTextInput("POLO-RED-M")
            compose.onNodeWithTag("add_product_variant_cost").performScrollTo().performTextInput("450")
            compose.onNodeWithTag("add_product_variant_price").performTextInput("750")
            compose.onNodeWithTag("add_product_variant_qty").performScrollTo().performTextInput("25")
            compose.onNodeWithTag("add_product_save_variant_btn").performScrollTo().performClick()

            compose.onNodeWithText("POLO-RED-M").assertExists()
            capture("04_add_product_step3_with_variants_$suffix")
        }
    }

    @Test
    fun createProductWithoutVariantsSubmitsFromStep2() {
        var confirmedNameEn: String? = null
        var confirmedNameBn: String? = null
        var confirmedSku: String? = null
        var confirmedBarcode: String? = null
        var confirmedPurchase: Long = -1L
        var confirmedSelling: Long = -1L
        var confirmedMinStock: Int = -1
        var confirmedOpeningStock: Int = -1
        var confirmedWarehouseId: String? = null
        var confirmedDescription: String? = null
        var confirmedVariantsCount: Int = -1

        compose.setContent {
            val base = LocalContext.current
            val cfg = Configuration(LocalConfiguration.current).apply { setLocale(Locale("bn")) }
            CompositionLocalProvider(
                LocalContext provides base.createConfigurationContext(cfg),
                LocalConfiguration provides cfg,
                LocalERPOnNavigate provides {}
            ) {
                AdvanceErpTheme(darkTheme = false) {
                    AddProductDialog(
                        categories = sampleCategories(),
                        warehouses = sampleWarehouses(),
                        language = AppLanguage.BANGLA,
                        onDismiss = {},
                        onGenerateSku = { _, _ -> "UNC-COT-001" },
                        onGenerateBarcode = { "8909876543210" },
                        onValidateSku = { _, _ -> true },
                        onValidateBarcode = { _, _ -> true },
                        onLoadCategoryProducts = { emptyList() },
                        onConfirm = { nameEn, nameBn, sku, bar, _, _, buy, sell, minAlert, _, vars, initStock, whId, desc ->
                            confirmedNameEn = nameEn
                            confirmedNameBn = nameBn
                            confirmedSku = sku
                            confirmedBarcode = bar
                            confirmedPurchase = buy
                            confirmedSelling = sell
                            confirmedMinStock = minAlert
                            confirmedOpeningStock = initStock
                            confirmedWarehouseId = whId
                            confirmedDescription = desc
                            confirmedVariantsCount = vars.size
                        }
                    )
                }
            }
        }

        compose.onNodeWithTag("add_product_name_en").performScrollTo().performTextInput("Cotton Polo")
        compose.onNodeWithTag("add_product_name_bn").performScrollTo().performTextInput("কটন পোলো")
        compose.onNodeWithTag("add_product_auto_sku_btn").performScrollTo().performClick()
        compose.onNodeWithText("UNC-COT-001").assertExists()

        compose.onNodeWithTag("add_product_next_btn").performClick()

        compose.onNodeWithTag("add_product_auto_barcode_btn").performClick()
        compose.onNodeWithText("8909876543210").assertExists()
        compose.onNodeWithTag("add_product_purchase_price").performTextInput("320.50")
        compose.onNodeWithTag("add_product_selling_price").performTextInput("550.00")
        compose.onNodeWithTag("add_product_min_stock").performTextClearance()
        compose.onNodeWithTag("add_product_min_stock").performTextInput("5")
        compose.onNodeWithTag("add_product_opening_stock").performTextClearance()
        compose.onNodeWithTag("add_product_opening_stock").performTextInput("40")
        compose.onNodeWithTag("add_product_description").performScrollTo().performTextInput("100% combed cotton fabric")

        // Variant toggle is OFF by default so Step 2 directly exposes Add Product CTA
        compose.onNodeWithTag("add_product_submit_btn").performClick()
        compose.waitUntil(timeoutMillis = 5_000L) { confirmedNameEn != null }

        assertEquals("Cotton Polo", confirmedNameEn)
        assertEquals("কটন পোলো", confirmedNameBn)
        assertEquals("UNC-COT-001", confirmedSku)
        assertEquals("8909876543210", confirmedBarcode)
        assertEquals(32050L, confirmedPurchase)
        assertEquals(55000L, confirmedSelling)
        assertEquals(5, confirmedMinStock)
        assertEquals(40, confirmedOpeningStock)
        assertEquals("wh-main", confirmedWarehouseId)
        assertEquals("100% combed cotton fabric", confirmedDescription)
        assertEquals(0, confirmedVariantsCount)
    }

    @Test
    fun createProductWithVariantsEditDeleteAndSummedStock() {
        var submittedVariants: List<NewVariantDraft> = emptyList()
        var submittedStock = -1

        compose.setContent {
            val base = LocalContext.current
            val cfg = Configuration(LocalConfiguration.current).apply { setLocale(Locale("bn")) }
            CompositionLocalProvider(
                LocalContext provides base.createConfigurationContext(cfg),
                LocalConfiguration provides cfg,
                LocalERPOnNavigate provides {}
            ) {
                AdvanceErpTheme(darkTheme = false) {
                    AddProductDialog(
                        categories = sampleCategories(),
                        warehouses = sampleWarehouses(),
                        language = AppLanguage.BANGLA,
                        onDismiss = {},
                        onGenerateSku = { _, _ -> "ELE-USB-001" },
                        onGenerateVariantSku = { baseSku, color, _ -> "${baseSku}-${color?.uppercase()}" },
                        onGenerateBarcode = { "8905551112223" },
                        onValidateSku = { _, _ -> true },
                        onValidateBarcode = { _, _ -> true },
                        onLoadCategoryProducts = { emptyList() },
                        onConfirm = { _, _, _, _, _, _, _, _, _, _, vars, initStock, _, _ ->
                            submittedVariants = vars
                            submittedStock = initStock
                        }
                    )
                }
            }
        }

        compose.onNodeWithTag("add_product_name_en").performScrollTo().performTextInput("USB Cable")
        compose.onNodeWithTag("add_product_auto_sku_btn").performScrollTo().performClick()
        compose.onNodeWithTag("add_product_next_btn").performClick()

        compose.onNodeWithTag("add_product_purchase_price").performTextInput("100")
        compose.onNodeWithTag("add_product_selling_price").performTextInput("200")
        compose.onNodeWithTag("add_product_variant_switch").performScrollTo().performClick()
        compose.onNodeWithTag("add_product_next_btn").performClick()

        // Variant 1
        compose.onNodeWithTag("add_product_variant_color").performScrollTo().performTextInput("Black")
        compose.onNodeWithTag("add_product_variant_size").performScrollTo().performTextInput("1M")
        compose.onNodeWithTag("add_product_variant_sku").performScrollTo().performTextInput("USB-BLK-1M")
        compose.onNodeWithTag("add_product_variant_qty").performScrollTo().performTextInput("10")
        compose.onNodeWithTag("add_product_save_variant_btn").performScrollTo().performClick()

        // Variant 2
        compose.onNodeWithTag("add_product_open_variant_editor").performScrollTo().performClick()
        compose.onNodeWithTag("add_product_variant_color").performScrollTo().performTextInput("White")
        compose.onNodeWithTag("add_product_variant_size").performScrollTo().performTextInput("2M")
        compose.onNodeWithTag("add_product_variant_sku").performScrollTo().performTextInput("USB-WHT-2M")
        compose.onNodeWithTag("add_product_variant_qty").performScrollTo().performTextInput("8")
        compose.onNodeWithTag("add_product_save_variant_btn").performScrollTo().performClick()

        // Variant 3 (to delete)
        compose.onNodeWithTag("add_product_open_variant_editor").performScrollTo().performClick()
        compose.onNodeWithTag("add_product_variant_color").performScrollTo().performTextInput("Green")
        compose.onNodeWithTag("add_product_variant_sku").performScrollTo().performTextInput("USB-GRN")
        compose.onNodeWithTag("add_product_variant_qty").performScrollTo().performTextInput("5")
        compose.onNodeWithTag("add_product_save_variant_btn").performScrollTo().performClick()

        compose.onNodeWithTag("delete_variant_2").performScrollTo().performClick()
        compose.onNodeWithTag("edit_variant_0").performScrollTo().performClick()
        compose.onNodeWithTag("add_product_variant_qty").performScrollTo().performTextClearance()
        compose.onNodeWithTag("add_product_variant_qty").performScrollTo().performTextInput("15")
        compose.onNodeWithTag("add_product_save_variant_btn").performScrollTo().performClick()

        compose.onNodeWithTag("add_product_submit_btn").performClick()
        compose.waitUntil(timeoutMillis = 5_000L) { submittedVariants.isNotEmpty() }

        assertEquals(2, submittedVariants.size)
        assertEquals("USB-BLK-1M", submittedVariants[0].sku)
        assertEquals(15, submittedVariants[0].initialStock)
        assertEquals("USB-WHT-2M", submittedVariants[1].sku)
        assertEquals(8, submittedVariants[1].initialStock)
        assertEquals(23, submittedStock)
    }

    @Test
    fun searchableCategorySheetAndCreateNewCategory() {
        val cats = mutableStateListOf(*sampleCategories().toTypedArray())
        var submittedCategoryId: String? = null

        compose.setContent {
            val base = LocalContext.current
            val cfg = Configuration(LocalConfiguration.current).apply { setLocale(Locale("bn")) }
            CompositionLocalProvider(
                LocalContext provides base.createConfigurationContext(cfg),
                LocalConfiguration provides cfg,
                LocalERPOnNavigate provides {}
            ) {
                AdvanceErpTheme(darkTheme = false) {
                    AddProductDialog(
                        categories = cats,
                        warehouses = sampleWarehouses(),
                        language = AppLanguage.BANGLA,
                        onDismiss = {},
                        onAddNewCategory = { nameEn, nameBn, onCreated ->
                            val created = ProductCategory(
                                id = "cat-beauty",
                                nameEn = nameEn,
                                nameBn = nameBn.ifBlank { nameEn },
                                code = "BTY"
                            )
                            cats.add(created)
                            onCreated(created)
                        },
                        onLoadCategoryProducts = { emptyList() },
                        onConfirm = { _, _, _, _, catId, _, _, _, _, _, _, _, _, _ ->
                            submittedCategoryId = catId
                        }
                    )
                }
            }
        }

        compose.onNodeWithTag("add_product_category_selector").performScrollTo().performClick()
        compose.onNodeWithText("+ নতুন ক্যাটাগরি তৈরি করুন").performClick()
        compose.onNodeWithText("ক্যাটাগরির নাম (ইংরেজি) *").performTextInput("Beauty")
        compose.onNodeWithText("ক্যাটাগরির নাম (বাংলা)").performTextInput("বিউটি")
        compose.onNodeWithTag("add_category_confirm_btn").performClick()

        compose.onNodeWithText("বিউটি").assertExists()
        compose.onNodeWithTag("add_product_name_en").performScrollTo().performTextInput("Face Wash")
        compose.onNodeWithTag("add_product_base_sku").performScrollTo().performTextInput("BTY-FW-01")
        compose.onNodeWithTag("add_product_next_btn").performClick()
        compose.onNodeWithTag("add_product_submit_btn").performClick()
        compose.waitUntil(timeoutMillis = 5_000L) { submittedCategoryId != null }

        assertEquals("cat-beauty", submittedCategoryId)
    }

    @Test
    fun validationRedirectsToStep1AndCloseShowsUnsavedConfirmation() {
        var dismissed = false

        compose.setContent {
            val base = LocalContext.current
            val cfg = Configuration(LocalConfiguration.current).apply { setLocale(Locale("bn")) }
            CompositionLocalProvider(
                LocalContext provides base.createConfigurationContext(cfg),
                LocalConfiguration provides cfg,
                LocalERPOnNavigate provides {}
            ) {
                AdvanceErpTheme(darkTheme = false) {
                    AddProductDialog(
                        categories = sampleCategories(),
                        warehouses = sampleWarehouses(),
                        language = AppLanguage.BANGLA,
                        onDismiss = { dismissed = true },
                        onLoadCategoryProducts = { emptyList() },
                        onConfirm = { _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> }
                    )
                }
            }
        }

        // Go to Step 2 without entering nameEn -> submit -> must redirect to Step 1 with error
        compose.onNodeWithTag("add_product_next_btn").performClick()
        compose.onNodeWithTag("add_product_submit_btn").performClick()
        compose.onNodeWithText("পণ্যের ইংরেজি নাম দেওয়া আবশ্যক").assertIsDisplayed()

        // Enter text and attempt close -> shows Unsaved confirmation
        compose.onNodeWithTag("add_product_name_en").performScrollTo().performTextInput("Draft Item")
        compose.onNodeWithTag("add_product_close").performClick()
        compose.onNodeWithText("অসংরক্ষিত তথ্য বাদ দেবেন?").assertIsDisplayed()

        compose.onNodeWithText("সম্পাদনা চালান").performClick()
        assertEquals(false, dismissed)

        compose.onNodeWithTag("add_product_close").performClick()
        compose.onNodeWithText("বাদ দিন").performClick()
        assertEquals(true, dismissed)
    }

    @Test
    fun englishLanguageModeRendersAllSteps() {
        compose.setContent {
            val base = LocalContext.current
            val cfg = Configuration(LocalConfiguration.current).apply { setLocale(Locale("en")) }
            CompositionLocalProvider(
                LocalContext provides base.createConfigurationContext(cfg),
                LocalConfiguration provides cfg,
                LocalERPOnNavigate provides {}
            ) {
                AdvanceErpTheme(darkTheme = false) {
                    AddProductDialog(
                        categories = sampleCategories(),
                        warehouses = sampleWarehouses(),
                        language = AppLanguage.ENGLISH,
                        onDismiss = {},
                        onLoadCategoryProducts = { emptyList() },
                        onConfirm = { _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> }
                    )
                }
            }
        }

        compose.onNodeWithText("1. Basic Info").assertExists()
        compose.onNodeWithText("2. Other Info").assertExists()
        compose.onNodeWithText("3. Variants").assertExists()
        compose.onNodeWithTag("add_product_next_btn").performClick()
        compose.onNodeWithText("Use Product Variants?").performScrollTo().assertExists()
        compose.onNodeWithTag("add_product_variant_switch").performClick()
        compose.onNodeWithTag("add_product_next_btn").performClick()
        compose.onNodeWithText("Add Variant to List").performScrollTo().assertExists()
        compose.onNodeWithTag("add_product_submit_btn").assertExists()
    }

    @Test
    fun roomPersistenceWithoutAndWithVariantsAndPhotos() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, AdvanceErpDatabase::class.java).build()
        try {
            val repo = ProductRepositoryImpl(
                db = db,
                productDao = db.productDao(),
                categoryDao = db.productCategoryDao(),
                variantDao = db.productVariantDao(),
                imageDao = db.productImageDao(),
                stockBalanceDao = db.stockBalanceDao(),
                stockMovementDao = db.stockMovementDao(),
                inventoryCostLayerDao = db.inventoryCostLayerDao(),
                imageStorage = ProductImageStorage(context)
            )
            db.productCategoryDao().insertCategory(
                ProductCategory(id = "cat-1", nameEn = "Apparel", nameBn = "পোশাক", code = "APP")
            )
            db.warehouseDao().insertWarehouse(
                Warehouse(id = "wh-1", name = "Main Warehouse", code = "WH-1", location = "Dhaka", isDefault = true)
            )

            // 1. Product without variants + description + initialStock
            val p1 = Product(
                id = "prod-no-var",
                categoryId = "cat-1",
                nameEn = "Basic Tee",
                nameBn = "বেসিক টি",
                baseSku = "APP-TEE-001",
                barcode = "8901112223334",
                description = "Soft cotton crew neck",
                unit = "PCS",
                purchasePriceMinor = 25000L,
                sellingPriceMinor = 45000L,
                minStockAlert = 10
            )
            repo.addProductWithMedia(
                product = p1,
                variants = emptyList(),
                images = emptyList(),
                initialStock = 25,
                warehouseId = "wh-1"
            )

            val savedP1 = repo.getProductWithDetailsById("prod-no-var")
            assertNotNull(savedP1)
            assertEquals("Soft cotton crew neck", savedP1?.description)
            assertEquals(25, savedP1?.totalStock)

            val p1Movements = db.stockMovementDao().getMovementsForProduct("prod-no-var").first()
            assertEquals(1, p1Movements.size)
            assertEquals(StockMovementType.OPENING_STOCK.code, p1Movements.first().movementType)
            assertEquals(25, p1Movements.first().quantity)

            val p1Layers = db.inventoryCostLayerDao().getCostLayersForProductSync("prod-no-var")
            assertEquals(1, p1Layers.size)
            assertEquals(25, p1Layers.first().initialQuantity)

            // 2. Product with 2 variants + photos + per-variant stock + fallback check
            val tmpImg = File(context.cacheDir, "test_prod_photo.png").apply {
                val bmp = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
                Canvas(bmp).drawColor(android.graphics.Color.MAGENTA)
                outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 90, it) }
                bmp.recycle()
            }
            val p2 = Product(
                id = "prod-with-vars",
                categoryId = "cat-1",
                nameEn = "Polo Shirt",
                nameBn = "পোলো শার্ট",
                baseSku = "APP-PLO-002",
                description = "Two variants product",
                unit = "PCS",
                purchasePriceMinor = 40000L,
                sellingPriceMinor = 70000L
            )
            val v1 = ProductVariant(
                id = "var-red-m",
                productId = p2.id,
                sku = "APP-PLO-002-RED-M",
                color = "Red",
                size = "M",
                variantCostMinor = 42000L,
                variantPriceMinor = 72000L
            )
            val v2 = ProductVariant(
                id = "var-red-l",
                productId = p2.id,
                sku = "APP-PLO-002-RED-L",
                color = "Red",
                size = "L",
                variantCostMinor = 42000L,
                variantPriceMinor = 75000L
            )
            val images = listOf(
                ProductImage(id = "img-base", productId = p2.id, variantId = null, imageUri = tmpImg.absolutePath, isPrimary = true, sortOrder = 0),
                ProductImage(id = "img-v1", productId = p2.id, variantId = v1.id, imageUri = tmpImg.absolutePath, isPrimary = true, sortOrder = 0)
            )
            repo.addProductWithMedia(
                product = p2,
                variants = listOf(v1, v2),
                images = images,
                initialStock = 0,
                warehouseId = "wh-1",
                variantInitialStock = mapOf(v1.id to 12, v2.id to 8)
            )

            val savedP2 = repo.getProductWithDetailsById(p2.id)
            assertNotNull(savedP2)
            assertEquals(20, savedP2?.totalStock)
            assertEquals(2, savedP2?.variantCount)

            val stockMap = repo.getVariantStockMap(p2.id)
            assertEquals(12, stockMap[v1.id])
            assertEquals(8, stockMap[v2.id])

            val layers = db.inventoryCostLayerDao().getCostLayersForProductSync(p2.id)
            assertEquals(2, layers.size)

            val v1Imgs = repo.getImagesForVariantSync(v1.id)
            assertEquals(1, v1Imgs.size)
            val resolved = resolveVariantPhotoUris(
                variants = listOf(v1, v2),
                photosByVariant = mapOf(v1.id to v1Imgs.first().imageUri)
            )
            assertEquals(tmpImg.absolutePath, resolved[v1.id])
            assertEquals(tmpImg.absolutePath, resolved[v2.id])
            assertTrue(File(tmpImg.absolutePath).exists())
        } finally {
            db.close()
        }
    }

    private fun capture(name: String) {
        compose.runOnIdle {
            val activity = compose.activity
            activity.getSystemService(ActivityManager::class.java)
                .appTasks
                .firstOrNull { it.taskInfo.taskId == activity.taskId }
                ?.moveToFront()
        }
        compose.waitForIdle()
        SystemClock.sleep(350)
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("screencap -p /data/local/tmp/advance-addproduct-$name.png")
            .use { descriptor -> FileInputStream(descriptor.fileDescriptor).readBytes() }
        val image = requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        val dir = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "add-product-verification").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        image.recycle()
    }
}
