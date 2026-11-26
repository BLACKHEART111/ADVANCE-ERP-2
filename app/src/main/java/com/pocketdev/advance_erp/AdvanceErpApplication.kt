package com.pocketdev.advance_erp

import android.app.Application
import com.pocketdev.advance_erp.core.worker.FridayBackupScheduler
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AdvanceErpApplication : Application() {
    override fun attachBaseContext(base: android.content.Context) {
        super.attachBaseContext(base)
        com.pocketdev.advance_erp.core.database.StagedRestore.applyPending(base)
    }

    override fun onCreate() {
        super.onCreate()
        try {
            FridayBackupScheduler.schedule(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
