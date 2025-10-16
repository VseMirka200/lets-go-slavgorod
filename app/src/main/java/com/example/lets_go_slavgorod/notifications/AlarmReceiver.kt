package com.example.lets_go_slavgorod.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.lets_go_slavgorod.data.local.AppDatabase
import com.example.lets_go_slavgorod.data.local.dataStore
import com.example.lets_go_slavgorod.data.local.NotificationPreferencesCache
import com.example.lets_go_slavgorod.data.repository.BusRouteRepository
import com.example.lets_go_slavgorod.utils.toFavoriteTime
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
        Timber.d("Alarm received: ${intent.action}")
        
        // Получаем данные из Intent
        val favoriteId = intent.getStringExtra("FAVORITE_ID")
        val routeId = intent.getStringExtra("ROUTE_ID")
        val routeInfo = intent.getStringExtra("ROUTE_INFO")
        val departureTimeInfo = intent.getStringExtra("DEPARTURE_TIME_INFO")
        val destinationInfo = intent.getStringExtra("DESTINATION_INFO")
        val departurePointInfo = intent.getStringExtra("DEPARTURE_POINT_INFO")
        
        Timber.d("Alarm data - favoriteId: $favoriteId, routeId: $routeId, routeInfo: $routeInfo")
        
        // Отправляем уведомление
        if (favoriteId != null) {
            // Проверяем настройки уведомлений перед отправкой
            val shouldSend = AlarmScheduler.shouldSendNotification(context, routeId)
            
            if (shouldSend) {
                val safeRouteInfo = if (routeInfo.isNullOrBlank()) "Автобус" else routeInfo
                val safeDepartureTimeInfo = if (departureTimeInfo.isNullOrBlank()) "Время отправления" else departureTimeInfo
                val safeDeparturePointInfo = if (departurePointInfo.isNullOrBlank()) "Пункт отправления" else departurePointInfo
                
                // Получаем настройки вибрации из кэша (без runBlocking)
                val vibrationEnabled = NotificationPreferencesCache.isVibrationEnabled()
                
                Timber.d("Sending notification with: routeInfo='$safeRouteInfo', departureTime='$safeDepartureTimeInfo', vibration=$vibrationEnabled")
                
                NotificationHelper.showDepartureNotification(
                    context = context,
                    favoriteTimeId = favoriteId,
                    routeInfo = safeRouteInfo,
                    departureTimeInfo = safeDepartureTimeInfo,
                    departurePointInfo = safeDeparturePointInfo,
                    enableVibration = vibrationEnabled
                )
            } else {
                Timber.d("Notification skipped for $favoriteId - current day/mode settings don't allow it")
            }
            
            // ВАЖНО: Перепланируем уведомление на следующий раз
            rescheduleNotification(context, favoriteId)
        } else {
            Timber.w("No favoriteId found in alarm intent")
        }
    }
    
    // Перепланирование уведомления на следующий день
    private fun rescheduleNotification(context: Context, favoriteId: String) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val database = AppDatabase.getDatabase(context)
                val favoriteTimeDao = database.favoriteTimeDao()
                val repository = BusRouteRepository()
                
                // Получаем данные из БД
                val favoriteEntity = favoriteTimeDao.getAllFavoriteTimes().firstOrNull()
                    ?.find { it.id == favoriteId }
                
                if (favoriteEntity != null && favoriteEntity.isActive) {
                    val favoriteTime = favoriteEntity.toFavoriteTime(repository)
                    
                    // Планируем следующее уведомление
                    AlarmScheduler.scheduleAlarm(context, favoriteTime)
                    Timber.d("Rescheduled notification for next occurrence: $favoriteId")
                } else {
                    Timber.w("FavoriteTime not found or inactive, not rescheduling: $favoriteId")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error rescheduling notification for $favoriteId")
            }
        }
    }
}
