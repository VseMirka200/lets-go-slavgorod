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
        
        val favoriteId = intent.getStringExtra("FAVORITE_ID")
        val routeId = intent.getStringExtra("ROUTE_ID")
        val routeInfo = intent.getStringExtra("ROUTE_INFO")
        val departureTimeInfo = intent.getStringExtra("DEPARTURE_TIME_INFO")
        val destinationInfo = intent.getStringExtra("DESTINATION_INFO")
        val departurePointInfo = intent.getStringExtra("DEPARTURE_POINT_INFO")
        
        // Отправляем уведомление
        if (favoriteId != null) {
            val shouldSend = AlarmScheduler.shouldSendNotification(context, routeId)
            
            if (shouldSend) {
                val safeRouteInfo = if (routeInfo.isNullOrBlank()) "Автобус" else routeInfo
                val safeDepartureTimeInfo = if (departureTimeInfo.isNullOrBlank()) "Время отправления" else departureTimeInfo
                val safeDeparturePointInfo = if (departurePointInfo.isNullOrBlank()) "Пункт отправления" else departurePointInfo
                
                val vibrationEnabled = NotificationPreferencesCache.isVibrationEnabled()
                
                NotificationHelper.showDepartureNotification(
                    context = context,
                    favoriteTimeId = favoriteId,
                    routeInfo = safeRouteInfo,
                    departureTimeInfo = safeDepartureTimeInfo,
                    departurePointInfo = safeDeparturePointInfo,
                    enableVibration = vibrationEnabled
                )
                
            } else {
            }
            
            // ВАЖНО: Перепланируем уведомление на следующий раз
            rescheduleNotification(context, favoriteId, pendingResult)
        } else {
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
                
                if (favoriteEntity != null && favoriteEntity.isActive) {
                    val favoriteTime = favoriteEntity.toFavoriteTime(repository)
                    AlarmScheduler.scheduleAlarm(context, favoriteTime)
                } else {
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка перепланирования уведомления для $favoriteId")
            } finally {
                // ВАЖНО: Сигнализируем что BroadcastReceiver завершил работу
                pendingResult.finish()
            }
        }
    }
}