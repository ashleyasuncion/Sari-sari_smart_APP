package com.example.sari_sari_smart

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.sari_sari_smart.data.local.AppDatabase
import com.example.sari_sari_smart.data.notifications.DailyCheckWorker
import com.example.sari_sari_smart.data.notifications.NotificationChannels
import java.util.concurrent.TimeUnit

class SariSariApp : Application() {

    /** Lazy-initialized Room database singleton */
    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }

    override fun onCreate() {
        super.onCreate()
        // V2.70: notification channels (idempotent) + daily check scheduling.
        NotificationChannels.createAll(this)
        scheduleNotificationChecks()
    }

    /** Schedule the two periodic workers (inexact timing only — battery-friendly,
     *  Doze-safe; no exact alarms per the V2.70 plan NFR-6). */
    private fun scheduleNotificationChecks() {
        val workManager = WorkManager.getInstance(this)

        val morning = PeriodicWorkRequestBuilder<DailyCheckWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(6, TimeUnit.HOURS)   // first run ~06:30
            .setInputData(workDataOf(DailyCheckWorker.KEY_CHECK to DailyCheckWorker.CHECK_MORNING))
            .build()
        workManager.enqueueUniquePeriodicWork(
            DailyCheckWorker.WORK_MORNING, ExistingPeriodicWorkPolicy.KEEP, morning
        )

        val closing = PeriodicWorkRequestBuilder<DailyCheckWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(18, TimeUnit.HOURS)  // first run ~18:00
            .setInputData(workDataOf(DailyCheckWorker.KEY_CHECK to DailyCheckWorker.CHECK_CLOSING))
            .build()
        workManager.enqueueUniquePeriodicWork(
            DailyCheckWorker.WORK_CLOSING, ExistingPeriodicWorkPolicy.KEEP, closing
        )
    }
}
