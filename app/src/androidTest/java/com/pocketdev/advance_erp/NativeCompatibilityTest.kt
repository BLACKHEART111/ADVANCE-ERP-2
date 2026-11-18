package com.pocketdev.advance_erp

import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/** Loads the actual bundled binaries; no application database or real camera image is used. */
@RunWith(AndroidJUnit4::class)
class NativeCompatibilityTest {
    @Test fun bundledNativeLibrariesLoad() {
        System.loadLibrary("image_processing_util_jni")
        System.loadLibrary("surface_util_jni")
        System.loadLibrary("barhopper_v3")
    }

    @Test fun cameraProviderInitializes() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val provider=ProcessCameraProvider.getInstance(context).get(20,TimeUnit.SECONDS)
        assertNotNull(provider)
        assertTrue("Expected this phone to expose at least one camera",provider.availableCameraInfos.isNotEmpty())
    }

    @Test fun barcodeEngineDecodesOfflineQr() {
        val expected="ADVANCE-ERP-16KB-TEST"
        // Independent Python qrcode fixture; isolates native decoding from the app encoder.
        val rows=InstrumentationRegistry.getInstrumentation().context.assets
            .open("native-smoke-qr.txt").bufferedReader().use { it.readLines() }
        val scale=12
        val bitmap=Bitmap.createBitmap(rows.size*scale,rows.size*scale,Bitmap.Config.ARGB_8888)
        for (y in 0 until bitmap.height) for (x in 0 until bitmap.width) {
            bitmap.setPixel(x,y,if(rows[y/scale][x/scale]=='1') Color.BLACK else Color.WHITE)
        }
        val scanner=BarcodeScanning.getClient()
        try {
            val results=Tasks.await(scanner.process(InputImage.fromBitmap(bitmap,0)),30,TimeUnit.SECONDS)
            assertTrue("Bundled barcode engine must decode the QR",results.any { it.rawValue==expected })
        } finally {
            scanner.close()
            bitmap.recycle()
        }
    }
}
