package com.pocketdev.advance_erp

import android.app.Application
import com.pocketdev.advance_erp.core.database.StagedRestore
import com.pocketdev.advance_erp.core.datastore.UserPreferencesRepository
import com.pocketdev.advance_erp.core.worker.FridayBackupScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltAndroidApp
class AdvanceErpApplication : Application() {

    @Inject
    lateinit var preferencesRepository: UserPreferencesRepository

    override fun attachBaseContext(base: android.content.Context) {
        super.attachBaseContext(base)
        StagedRestore.applyPending(base)
    }

    override fun onCreate() {
        super.onCreate()
        try {
            if (::preferencesRepository.isInitialized && StagedRestore.hasPendingPreferences(this)) {
                val autoBackupEnabled = runBlocking(Dispatchers.IO) {
                    StagedRestore.applyPendingPreferences(this@AdvanceErpApplication, preferencesRepository)
                    preferencesRepository.isFridayAutoBackupEnabled.first()
                }
                if (autoBackupEnabled) {
                    FridayBackupScheduler.schedule(this)
                } else {
                    FridayBackupScheduler.cancel(this)
                }
            } else {
                FridayBackupScheduler.schedule(this)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
