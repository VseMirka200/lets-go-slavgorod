package com.example.lets_go_slavgorod.data.workers

import android.content.Context
import androidx.work.*
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Менеджер для управления фоновой синхронизацией данных
 * 
 * Настраивает периодическую проверку обновлений расписания через WorkManager.
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 2.1
 */
object DataSyncManager {
    
    private const val SYNC_WORK_NAME = "data_sync_work"
    private const val SYNC_INTERVAL_HOURS = 24L
    
    /**
     * Запускает периодическую синхронизацию
     */
    fun schedulePeriodic(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)  // Требуется интернет
            .setRequiresBatteryNotLow(true)                 // Не запускать при низком заряде
            .build()
        
        val syncRequest = PeriodicWorkRequestBuilder<DataSyncWorker>(
            repeatInterval = SYNC_INTERVAL_HOURS,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
            flexTimeInterval = 2,  // Гибкий интервал в 2 часа
            flexTimeIntervalUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,  // Не перезаписываем существующую работу
            syncRequest
        )
        
        Timber.d("📅 Periodic data sync scheduled: every $SYNC_INTERVAL_HOURS hours")
    }
    
    /**
     * Запускает разовую синхронизацию
     */
    fun syncNow(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val syncRequest = OneTimeWorkRequestBuilder<DataSyncWorker>()
            .setConstraints(constraints)
            .build()
        
        WorkManager.getInstance(context).enqueue(syncRequest)
        
        Timber.d("🔄 One-time data sync started")
    }
    
    /**
     * Отменяет периодическую синхронизацию
     */
    fun cancelSync(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(SYNC_WORK_NAME)
        Timber.d("❌ Periodic data sync cancelled")
    }
}