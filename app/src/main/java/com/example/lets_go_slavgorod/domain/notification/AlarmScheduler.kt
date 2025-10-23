package com.example.lets_go_slavgorod.domain.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.example.lets_go_slavgorod.core.Constants
import com.example.lets_go_slavgorod.data.local.NotificationPreferencesCache
import com.example.lets_go_slavgorod.data.model.FavoriteTime
import com.example.lets_go_slavgorod.data.notification.AlarmReceiver
import com.example.lets_go_slavgorod.domain.notification.AlarmScheduler.cancelAlarm
import com.example.lets_go_slavgorod.domain.notification.AlarmScheduler.scheduleAlarm
import com.example.lets_go_slavgorod.domain.notification.AlarmScheduler.updateAllAlarmsBasedOnSettings
import com.example.lets_go_slavgorod.ui.viewmodel.NotificationMode
import timber.log.Timber
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.util.Calendar

/**
 * Менеджер уведомлений для автобусов
 * 
 * Этот класс отвечает за планирование уведомлений о времени отправления автобусов.
 * Работает с системным AlarmManager и учитывает настройки пользователя.
 * 
 * Что делает:
 * - Планирует уведомления за N минут до отправления автобуса
 * - Учитывает режимы уведомлений (все дни, только будни, выбранные дни)
 * - Проверяет разрешения на точные будильники
 * - Отменяет уведомления при удалении из избранного
 * 
 * Особенности:
 * - Поддерживает Android 6.0+ с проверкой Doze Mode
 * - Работает с разными часовыми поясами
 * - Логирует все операции для отладки
 */
object AlarmScheduler {

    // =====================================================================================
    //                              КОНСТАНТЫ
    // =====================================================================================
    
    /** Префикс для генерации уникальных request codes для каждого будильника */
    private const val ALARM_REQUEST_CODE_PREFIX = Constants.ALARM_REQUEST_CODE_PREFIX

    /**
     * Проверяет, нужно ли отправлять уведомление
     * 
     * Смотрит на настройки пользователя и решает, должен ли он получать уведомления.
     * Учитывает режимы: все дни, только будни, выбранные дни или отключено.
     */
    fun shouldSendNotification(context: Context, routeId: String? = null): Boolean {
        // Используем кэш вместо runBlocking для избежания блокировки главного потока
        return NotificationPreferencesCache.shouldSendNotification(routeId)
    }

    /**
     * Планирует уведомление для избранного времени
     * 
     * Создает будильник, который сработает за N минут до отправления автобуса.
     * Учитывает настройки пользователя и системные ограничения.
     * 
     * Как работает:
     * 1. Получает время опережения из настроек (например, 5 минут)
     * 2. Вычисляет, когда должен отправиться автобус
     * 3. Вычитает время опережения (5 минут)
     * 4. Планирует будильник на это время
     * 
     * Особенности:
     * - На новых Android нужны разрешения на точные будильники
     * - Если разрешений нет, планирует приблизительно
     * - Проверяет, что время в будущем
     */
    fun scheduleAlarm(context: Context, favoriteTime: FavoriteTime) {
        // Проверка shouldSendNotification НЕ выполняется здесь преднамеренно:
        // - Будильники планируются заранее (могут быть на несколько дней вперед)
        // - Настройки пользователя могут измениться между планированием и срабатыванием
        // - Актуальная проверка выполняется в AlarmReceiver.onReceive() в момент срабатывания
        
        
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        if (alarmManager == null) {
            Timber.e("AlarmManager равен null. Невозможно запланировать будильник для ID ${favoriteTime.id}.")
            return
        }
        
        // Проверяем разрешения на точные будильники
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val canScheduleExact = alarmManager.canScheduleExactAlarms()
            if (!canScheduleExact) {
            }
        }

        // Получаем время уведомления для конкретного маршрута из кэша (синхронно, без блокировки)
        val leadTimeMinutes = try {
            NotificationPreferencesCache.getLeadTimeForRoute(favoriteTime.routeId)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get lead time for route ${favoriteTime.routeId}")
            Constants.DEFAULT_NOTIFICATION_LEAD_TIME
        }
        val leadTimeMillis = leadTimeMinutes * 60 * 1000L // Конвертируем минуты в миллисекунды
        

        val calculatedDepartureTime = calculateNextDepartureTimeInMillis(context, favoriteTime)
        if (calculatedDepartureTime == -1L) {
            Timber.e("Не удалось вычислить корректное время отправления для ${favoriteTime.id}. Не планируем.")
            return
        }

        val triggerAtMillis = calculatedDepartureTime - leadTimeMillis


        if (triggerAtMillis <= System.currentTimeMillis()) {
            return
        }

        // Улучшенная обработка номера маршрута для уведомления
        val routeNumber = favoriteTime.routeNumber.trim()
        val routeInfoForNotification = when {
            routeNumber.isNotBlank() && routeNumber != "N/A" -> {
                "Автобус №$routeNumber"
            }
            favoriteTime.routeName.isNotBlank() && favoriteTime.routeName != "Маршрут" -> {
                favoriteTime.routeName
            }
            else -> {
                // Fallback: используем routeId или общее название
                val fallbackNumber = favoriteTime.routeId ?: "Неизвестный"
                "Маршрут $fallbackNumber"
            }
        }
        val departureTimeInfoForNotification = "в ${favoriteTime.departureTime.trim()}"
        
        val destinationInfoForNotification = ""
        val departurePointStr = favoriteTime.departurePoint.trim()
        val departurePointInfoForNotification = if (departurePointStr.isNotBlank()) {
            "От: $departurePointStr"
        } else {
            ""
        }


        val intent = Intent(context.applicationContext, AlarmReceiver::class.java).apply {
            action = "com.example.lets_go_slavgorod.ALARM_TRIGGER_${favoriteTime.id}"
            putExtra("FAVORITE_ID", favoriteTime.id)
            putExtra("ROUTE_ID", favoriteTime.routeId)
            putExtra("ROUTE_INFO", routeInfoForNotification)
            putExtra("DEPARTURE_TIME_INFO", departureTimeInfoForNotification)
            putExtra("DESTINATION_INFO", destinationInfoForNotification)
            putExtra("DEPARTURE_POINT_INFO", departurePointInfoForNotification)
        }

        val requestCode = (ALARM_REQUEST_CODE_PREFIX + favoriteTime.id).hashCode()

        val pendingIntent = PendingIntent.getBroadcast(
            context.applicationContext,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )


        try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    val canScheduleExact = alarmManager.canScheduleExactAlarms()
                    if (canScheduleExact) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                    } else {
                        alarmManager.setWindow(AlarmManager.RTC_WAKEUP, triggerAtMillis, 60_000L, pendingIntent)
                    }
                }
                else -> {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            }
        } catch (se: SecurityException) {
            Timber.e("Невозможно запланировать будильник для ${favoriteTime.id}: ${se.message}")
        } catch (e: Exception) {
            Timber.e(e, "Ошибка планирования будильника для ${favoriteTime.id}")
        }
    }

    /**
     * Отменяет уведомление для избранного времени
     * 
     * Удаляет будильник, который был создан для этого избранного времени.
     * Использует тот же ID, что и при создании, чтобы найти правильный будильник.
     * 
     * Безопасно работает - если будильник уже отменен, ничего не происходит.
     */
    fun cancelAlarm(context: Context, favoriteTimeId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        if (alarmManager == null) {
            Timber.e("AlarmManager равен null. Невозможно отменить будильник для ID $favoriteTimeId.")
            return
        }

        val intent = Intent(context.applicationContext, AlarmReceiver::class.java).apply {
            action = "com.example.lets_go_slavgorod.ALARM_TRIGGER_${favoriteTimeId}"
        }
        val requestCode = (ALARM_REQUEST_CODE_PREFIX + favoriteTimeId).hashCode()

        val pendingIntent = PendingIntent.getBroadcast(
            context.applicationContext,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        )

        if (pendingIntent != null) {
            try {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            } catch (e: Exception) {
                Timber.e(e, "Ошибка отмены будильника для $favoriteTimeId")
            }
        } else {
        }
    }

    /**
     * Вычисляет, когда в следующий раз отправится автобус
     * 
     * Смотрит на избранное время и режим уведомлений, чтобы понять,
     * когда должен сработать следующий будильник.
     * 
     * Примеры:
     * - Избранное: Понедельник 08:00, режим "Все дни" → завтра в 08:00
     * - Избранное: Понедельник 08:00, режим "Только будни" → завтра в 08:00 (если завтра будний)
     * - Избранное: Суббота 10:00, режим "Только будни" → понедельник в 10:00
     * 
     * Ищет максимум на 2 недели вперед.
     */
    private fun calculateNextDepartureTimeInMillis(context: Context, favoriteTime: FavoriteTime): Long {
        if (favoriteTime.departureTime.isBlank()) {
            Timber.e("Время отправления пустое для ID ${favoriteTime.id}")
            return -1L
        }
        val timeParts = favoriteTime.departureTime.split(":")
        if (timeParts.size != 2) {
            Timber.e("Неверный формат времени отправления: '${favoriteTime.departureTime}' для ID ${favoriteTime.id}")
            return -1L
        }

        val hour: Int
        val minute: Int
        try {
            hour = timeParts[0].trim().toInt()
            minute = timeParts[1].trim().toInt()
        } catch (nfe: NumberFormatException) {
            Timber.e(nfe, "Неверный формат числа в частях времени отправления: '${favoriteTime.departureTime}' для ID ${favoriteTime.id}")
            return -1L
        }

        if (hour !in 0..23 || minute !in 0..59) {
            Timber.e("Неверные значения времени: час=$hour, минута=$minute для ID ${favoriteTime.id}")
            return -1L
        }

        // Используем текущий часовой пояс устройства
        val now = Calendar.getInstance()
        val nextDepartureBase = Calendar.getInstance().apply {
            // Важно: сохраняем текущий часовой пояс
            timeZone = now.timeZone
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        

        // Получаем режим уведомлений и создаем соответствующую стратегию
        val notificationMode = try {
            NotificationPreferencesCache.getNotificationMode(favoriteTime.routeId)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get notification mode for route ${favoriteTime.routeId}")
            NotificationMode.ALL_DAYS
        }
        
        if (notificationMode == NotificationMode.DISABLED) {
            return -1L
        }
        
        if (!shouldSendNotificationForToday(notificationMode, favoriteTime.routeId)) {
            return -1L
        }
        
        val strategy: DepartureTimeStrategy = when (notificationMode) {
            NotificationMode.ALL_DAYS -> AllDaysStrategy()
            NotificationMode.WEEKDAYS -> WeekdaysStrategy()
            NotificationMode.SELECTED_DAYS -> {
                val selectedDays = NotificationPreferencesCache.getSelectedDays(favoriteTime.routeId)
                // Конвертируем день недели избранного времени в DayOfWeek
                val targetDayOfWeek = convertCalendarDayToDayOfWeek(favoriteTime.dayOfWeek)
                SelectedDaysStrategy(selectedDays, targetDayOfWeek)
            }
            NotificationMode.DISABLED -> DisabledStrategy()
        }
        
        // Используем стратегию для вычисления времени
        val nextTimeMillis = strategy.calculateNextTime(nextDepartureBase, now)
        
        if (nextTimeMillis == -1L) {
            Timber.e("Не удалось найти подходящее время для ${favoriteTime.id}")
        }
        
        return nextTimeMillis
    }

    /**
     * Обновляет все уведомления при изменении настроек
     * 
     * Вызывается когда пользователь меняет настройки уведомлений.
     * Отменяет старые будильники и создает новые с учетом новых настроек.
     * 
     * Когда вызывается:
     * - Пользователь изменил режим уведомлений
     * - Пользователь изменил выбранные дни
     * - Пользователь включил/отключил уведомления
     * 
     * Безопасно - если что-то пойдет не так с одним избранным временем,
     * остальные все равно обновятся.
     */
    fun updateAllAlarmsBasedOnSettings(context: Context, favoriteTimes: List<FavoriteTime>) {
        favoriteTimes.forEach { favoriteTime ->
            try {
                cancelAlarm(context, favoriteTime.id)
                
                val notificationMode = NotificationPreferencesCache.getNotificationMode(favoriteTime.routeId)
                
                if (notificationMode != NotificationMode.DISABLED) {
                    scheduleAlarm(context, favoriteTime)
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка обновления будильника для ${favoriteTime.id}")
            }
        }
    }

    /**
     * Проверяет и обновляет уведомление для одного избранного времени
     * 
     * Быстрая версия обновления для одного элемента.
     * Смотрит на настройки и решает: планировать уведомление или отменить.
     * 
     * Используется когда:
     * - Добавили новое избранное время
     * - Изменили настройки для конкретного маршрута
     * - Нужно быстро обновить без пересчета всех уведомлений
     */
    fun checkAndUpdateNotifications(context: Context, favoriteTime: FavoriteTime) {
        val notificationMode = NotificationPreferencesCache.getNotificationMode(favoriteTime.routeId)
        
        if (notificationMode != NotificationMode.DISABLED) {
            scheduleAlarm(context, favoriteTime)
        } else {
            cancelAlarm(context, favoriteTime.id)
        }
    }

    /**
     * Форматирует время для логов
     * 
     * Превращает миллисекунды в читаемый формат "2024-01-15 14:30:00".
     * Используется для отладки - чтобы в логах было понятно, когда сработает будильник.
     */
    private fun formatMillis(millis: Long): String {
        return try {
            if (millis <= 0) return "Invalid or Past Millis"
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(millis)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка форматирования миллисекунд: $millis")
            "Error formatting timestamp"
        }
    }
    
    /**
     * Конвертирует день недели между форматами
     * 
     * В Android Calendar: 1=воскресенье, 2=понедельник, ..., 7=суббота
     * В Java Time: 1=понедельник, 2=вторник, ..., 7=воскресенье
     * 
     * Эта функция переводит из Android формата в Java Time формат.
     */
    private fun convertCalendarDayToDayOfWeek(calendarDay: Int): DayOfWeek? {
        return when (calendarDay) {
            1 -> DayOfWeek.SUNDAY
            2 -> DayOfWeek.MONDAY
            3 -> DayOfWeek.TUESDAY
            4 -> DayOfWeek.WEDNESDAY
            5 -> DayOfWeek.THURSDAY
            6 -> DayOfWeek.FRIDAY
            7 -> DayOfWeek.SATURDAY
            else -> {
                Timber.e("Неверный день календаря: $calendarDay")
                null
            }
        }
    }
    
    /**
     * Проверяет, нужно ли отправлять уведомления сегодня
     * 
     * Смотрит на режим уведомлений и текущий день недели.
     * Решает, должен ли пользователь получать уведомления сегодня.
     */
    private fun shouldSendNotificationForToday(notificationMode: NotificationMode, routeId: String?): Boolean {
        val currentDay = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)
        val currentDayOfWeek = when (currentDay) {
            java.util.Calendar.SUNDAY -> DayOfWeek.SUNDAY
            java.util.Calendar.MONDAY -> DayOfWeek.MONDAY
            java.util.Calendar.TUESDAY -> DayOfWeek.TUESDAY
            java.util.Calendar.WEDNESDAY -> DayOfWeek.WEDNESDAY
            java.util.Calendar.THURSDAY -> DayOfWeek.THURSDAY
            java.util.Calendar.FRIDAY -> DayOfWeek.FRIDAY
            java.util.Calendar.SATURDAY -> DayOfWeek.SATURDAY
            else -> null
        }
        
        return when (notificationMode) {
            NotificationMode.ALL_DAYS -> true
            NotificationMode.WEEKDAYS -> {
                currentDay in java.util.Calendar.MONDAY..java.util.Calendar.FRIDAY
            }
            NotificationMode.SELECTED_DAYS -> {
                val selectedDays = NotificationPreferencesCache.getSelectedDays(routeId)
                currentDayOfWeek != null && currentDayOfWeek in selectedDays
            }
            NotificationMode.DISABLED -> false
        }
    }
}