package com.example.lets_go_slavgorod.data.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.lets_go_slavgorod.data.repository.BusRouteRepository
import com.example.lets_go_slavgorod.data.notification.NotificationHelper
import timber.log.Timber

/**
 * Worker для фоновой синхронизации данных с GitHub
 * 
 * Выполняется автоматически раз в сутки для проверки обновлений расписания.
 * Работает даже когда приложение закрыто.
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 2.1
 */
class DataSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        
        return try {
            val repository = BusRouteRepository(applicationContext)
            
            // Проверяем наличие обновлений
            val hasUpdates = repository.checkForDataUpdates()
            
            if (hasUpdates) {
                
                // Получаем версию обновления
                val newVersion = repository.getRemoteDataVersion()
                
                // Показываем уведомление
                if (newVersion != null) {
                    NotificationHelper.showScheduleUpdateNotification(
                        context = applicationContext,
                        dataVersion = newVersion
                    )
                }
            } else {
            }
            
            Result.success()
            
        } catch (e: Exception) {
            Timber.e(e, "DataSyncWorker не удался")
            // Повторяем при ошибке
            Result.retry()
        }
    }
}