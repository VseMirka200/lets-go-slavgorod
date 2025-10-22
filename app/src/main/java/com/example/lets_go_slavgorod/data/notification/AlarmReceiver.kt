package com.example.lets_go_slavgorod.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.lets_go_slavgorod.data.local.AppDatabase
import com.example.lets_go_slavgorod.data.local.NotificationPreferencesCache
import com.example.lets_go_slavgorod.data.repository.BusRouteRepository
import com.example.lets_go_slavgorod.domain.notification.AlarmScheduler
import com.example.lets_go_slavgorod.core.toFavoriteTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * BroadcastReceiver для обработки срабатывания запланированных уведомлений
 * 
 * Получает события от AlarmManager когда наступает время отправки уведомления
 * пользователю о скором отправлении автобуса. Выполняет проверки настроек
 * и отправляет уведомление через NotificationHelper.
 * 
 * Процесс обработки:
 * 1. Получение данных о маршруте и времени из Intent
 * 2. Проверка настроек уведомлений (включены ли, тихий режим)
 * 3. Проверка активности избранного времени в базе данных
 * 4. Отправка уведомления пользователю
 * 5. Перепланирование уведомления на следующую неделю
 * 
 * Особенности:
 * - Асинхронная обработка через Coroutines
 * - Проверка тихого режима и расписания
 * - Автоматическое перепланирование уведомлений
 * - Подробное логирование для отладки
 * 
 * @author VseMirka200
 * @version 2.0
 * @since 1.0
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // goAsync() предотвращает завершение BroadcastReceiver пока не завершится асинхронная работа
        val pendingResult = goAsync()
        
        Timber.d("═══════════════════════════════════════════════════")
        Timber.d("Alarm received: ${intent.action}")
        
        // Получаем данные из Intent
        val favoriteId = intent.getStringExtra("FAVORITE_ID")
        val routeId = intent.getStringExtra("ROUTE_ID")
        val routeInfo = intent.getStringExtra("ROUTE_INFO")
        val departureTimeInfo = intent.getStringExtra("DEPARTURE_TIME_INFO")
        val destinationInfo = intent.getStringExtra("DESTINATION_INFO")
        val departurePointInfo = intent.getStringExtra("DEPARTURE_POINT_INFO")
        
        Timber.d("Alarm data:")
        Timber.d("  favoriteId: $favoriteId")
        Timber.d("  routeId: $routeId")
        Timber.d("  routeInfo: $routeInfo")
        Timber.d("  departureTime: $departureTimeInfo")
        Timber.d("  departurePoint: $departurePointInfo")
        
        // Отправляем уведомление
        if (favoriteId != null) {
            // Проверяем настройки уведомлений перед отправкой
            Timber.d("───────────────────────────────────────────────────")
            Timber.d("Checking notification settings...")
            
            val shouldSend = AlarmScheduler.shouldSendNotification(context, routeId)
            
            Timber.d("Should send notification: $shouldSend")
            Timber.d("───────────────────────────────────────────────────")
            
            if (shouldSend) {
                val safeRouteInfo = if (routeInfo.isNullOrBlank()) "Автобус" else routeInfo
                val safeDepartureTimeInfo = if (departureTimeInfo.isNullOrBlank()) "Время отправления" else departureTimeInfo
                val safeDeparturePointInfo = if (departurePointInfo.isNullOrBlank()) "Пункт отправления" else departurePointInfo
                
                // Получаем настройки вибрации из кэша (без runBlocking)
                val vibrationEnabled = NotificationPreferencesCache.isVibrationEnabled()
                
                Timber.d("✓ Sending notification:")
                Timber.d("  Route: $safeRouteInfo")
                Timber.d("  Departure: $safeDepartureTimeInfo at $safeDeparturePointInfo")
                Timber.d("  Vibration: $vibrationEnabled")
                
                NotificationHelper.showDepartureNotification(
                    context = context,
                    favoriteTimeId = favoriteId,
                    routeInfo = safeRouteInfo,
                    departureTimeInfo = safeDepartureTimeInfo,
                    departurePointInfo = safeDeparturePointInfo,
                    enableVibration = vibrationEnabled
                )
                
                Timber.d("✓ Notification sent successfully")
            } else {
                Timber.d("✗ Notification SKIPPED")
                Timber.d("  Reason: Current settings don't allow notification")
                Timber.d("  FavoriteId: $favoriteId")
                Timber.d("  RouteId: $routeId")
            }
            
            // ВАЖНО: Перепланируем уведомление на следующий раз
            Timber.d("───────────────────────────────────────────────────")
            Timber.d("Rescheduling notification for next occurrence...")
            rescheduleNotification(context, favoriteId, pendingResult)
        } else {
            Timber.w("✗ No favoriteId found in alarm intent - cannot process")
            Timber.d("═══════════════════════════════════════════════════")
            pendingResult.finish()  // Завершаем BroadcastReceiver
        }
    }
    
    /**
     * Перепланирование уведомления на следующее срабатывание
     * 
     * После срабатывания уведомления автоматически планирует следующее
     * уведомление для этого же избранного времени. Процесс выполняется
     * асинхронно через корутины, чтобы не блокировать главный поток.
     * 
     * Алгоритм:
     * 1. Загружает избранное время из БД по favoriteId
     * 2. Проверяет что избранное время активно (isActive = true)
     * 3. Конвертирует Entity → FavoriteTime с данными маршрута
     * 4. Вызывает AlarmScheduler.scheduleAlarm() для планирования следующего уведомления
     * 5. Новое уведомление запланируется на следующую неделю (тот же день недели)
     * 
     * @param context контекст приложения
     * @param favoriteId ID избранного времени для перепланирования
     * @param pendingResult результат goAsync() для управления жизненным циклом BroadcastReceiver
     */
    private fun rescheduleNotification(context: Context, favoriteId: String, pendingResult: PendingResult) {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getDatabase(context)
                val favoriteTimeDao = database.favoriteTimeDao()
                val repository = BusRouteRepository(context.applicationContext)
                
                // Получаем данные из БД
                val favoriteEntity = favoriteTimeDao.getAllFavoriteTimes().firstOrNull()
                    ?.find { it.id == favoriteId }
                
                if (favoriteEntity != null) {
                    if (favoriteEntity.isActive) {
                        val favoriteTime = favoriteEntity.toFavoriteTime(repository)
                        
                        Timber.d("Rescheduling:")
                        Timber.d("  FavoriteId: $favoriteId")
                        Timber.d("  Route: ${favoriteTime.routeNumber}")
                        Timber.d("  Time: ${favoriteTime.departureTime}")
                        Timber.d("  Day: ${favoriteTime.dayOfWeek}")
                        Timber.d("  Active: ${favoriteEntity.isActive}")
                        
                        // Планируем следующее уведомление
                        AlarmScheduler.scheduleAlarm(context, favoriteTime)
                        
                        Timber.d("✓ Rescheduled successfully for next week")
                    } else {
                        Timber.w("✗ FavoriteTime is inactive, not rescheduling: $favoriteId")
                    }
                } else {
                    Timber.w("✗ FavoriteTime not found in database: $favoriteId")
                }
                
                Timber.d("═══════════════════════════════════════════════════")
            } catch (e: Exception) {
                Timber.e(e, "✗ Error rescheduling notification for $favoriteId")
                Timber.d("═══════════════════════════════════════════════════")
            } finally {
                // ВАЖНО: Сигнализируем что BroadcastReceiver завершил работу
                pendingResult.finish()
            }
        }
    }
}