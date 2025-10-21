package com.example.lets_go_slavgorod.domain.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.lets_go_slavgorod.core.Constants
import com.example.lets_go_slavgorod.data.local.NotificationPreferencesCache
import com.example.lets_go_slavgorod.data.model.FavoriteTime
import com.example.lets_go_slavgorod.data.notification.AlarmReceiver
import com.example.lets_go_slavgorod.domain.notification.AlarmScheduler.cancelAlarm
import com.example.lets_go_slavgorod.ui.viewmodel.NotificationMode
import timber.log.Timber
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.util.Calendar

/**
 * Центральный менеджер планирования уведомлений о времени отправления автобусов
 * 
 * Отвечает за создание, управление и отмену уведомлений о времени отправления
 * автобусов с учетом пользовательских настроек и системных ограничений.
 * 
 * Основные функции:
 * - Планирование уведомлений с учетом пользовательских настроек
 * - Отмена уведомлений при удалении избранного времени
 * - Проверка разрешений на уведомления
 * - Учет тихого режима и расписания уведомлений
 * - Поддержка различных режимов уведомлений (будни, выходные, выбранные дни)
 * - Обработка системных событий (перезагрузка, обновление настроек)
 * 
 * Архитектура:
 * - Использует AlarmManager для точного планирования
 * - Интегрируется с DataStore для настроек пользователя
 * - Поддерживает Android 6.0+ с проверкой Doze Mode
 * - Обрабатывает edge cases (выходные, праздники, изменения расписания)
 * 
 * @author VseMirka200
 * @version 2.0
 * @since 1.0
 */
object AlarmScheduler {

    // =====================================================================================
    //                              КОНСТАНТЫ
    // =====================================================================================
    
    /** Префикс для генерации уникальных request codes для каждого будильника */
    private const val ALARM_REQUEST_CODE_PREFIX = Constants.ALARM_REQUEST_CODE_PREFIX

    /**
     * Проверяет, должны ли отправляться уведомления в соответствии с настройками пользователя
     * 
     * Учитывает различные режимы уведомлений:
     * - DISABLED: уведомления отключены
     * - ALL_DAYS: уведомления каждый день
     * - WEEKDAYS: уведомления только в будни
     * - SELECTED_DAYS: уведомления в выбранные дни недели
     * 
     * @param context контекст приложения для доступа к настройкам
     * @param routeId ID маршрута для проверки индивидуальных настроек
     * @return true если уведомление должно быть отправлено
     */
    fun shouldSendNotification(context: Context, routeId: String? = null): Boolean {
        // Используем кэш вместо runBlocking для избежания блокировки главного потока
        return NotificationPreferencesCache.shouldSendNotification(routeId)
    }

    /**
     * Планирует уведомление для избранного времени отправления
     * 
     * Создает точное уведомление с учетом всех пользовательских настроек:
     * - Режим уведомлений (будни/выходные/выбранные дни)
     * - Тихий режим и его расписание
     * - Системные разрешения на уведомления
     * - Индивидуальное время опережения для маршрута (из NotificationTimePreferences)
     * 
     * Алгоритм планирования:
     * 1. Получает AlarmManager из системы
     * 2. Загружает leadTime для конкретного маршрута из DataStore
     * 3. Вычисляет следующее время отправления согласно режиму уведомлений
     * 4. Вычитает leadTime для опережающего уведомления
     * 5. Проверяет что время в будущем
     * 6. Создает PendingIntent с данными маршрута
     * 7. Планирует будильник учитывая версию Android
     * 
     * Особенности работы:
     * - На Android S+ (API 31+) требуется разрешение SCHEDULE_EXACT_ALARM
     * - На Android M-R (API 23-30) использует setExactAndAllowWhileIdle
     * - При отсутствии разрешений планирует приблизительный будильник с окном ±1 минута
     * - Проверка shouldSendNotification откладывается до срабатывания в AlarmReceiver
     * - leadTime читается из настроек (глобальных или индивидуальных для маршрута)
     * 
     * @param context контекст приложения для доступа к системным сервисам
     * @param favoriteTime избранное время отправления с метаданными (ID, время, маршрут и т.д.)
     * 
     * @see cancelAlarm для отмены запланированного уведомления
     * @see updateAllAlarmsBasedOnSettings для обновления всех уведомлений
     * @see AlarmReceiver.onReceive для обработки срабатывания будильника
     * @see NotificationTimePreferences.getLeadTimeForRoute для получения времени опережения
     */
    fun scheduleAlarm(context: Context, favoriteTime: FavoriteTime) {
        // Проверка shouldSendNotification НЕ выполняется здесь преднамеренно:
        // - Будильники планируются заранее (могут быть на несколько дней вперед)
        // - Настройки пользователя могут измениться между планированием и срабатыванием
        // - Актуальная проверка выполняется в AlarmReceiver.onReceive() в момент срабатывания
        
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        if (alarmManager == null) {
            Timber.e("AlarmManager is null. Cannot schedule alarm for ID ${favoriteTime.id}.")
            return
        }

        // Получаем время уведомления для конкретного маршрута из кэша (синхронно, без блокировки)
        val leadTimeMinutes = NotificationPreferencesCache.getLeadTimeForRoute(favoriteTime.routeId)
        val leadTimeMillis = leadTimeMinutes * 60 * 1000L
        
        Timber.d("Lead time for route ${favoriteTime.routeId}: $leadTimeMinutes minutes ($leadTimeMillis ms)")

        val calculatedDepartureTime = calculateNextDepartureTimeInMillis(context, favoriteTime)
        if (calculatedDepartureTime == -1L) {
            Timber.e("Failed to calculate a valid departure time for ${favoriteTime.id}. Not scheduling.")
            return
        }

        val triggerAtMillis = calculatedDepartureTime - leadTimeMillis

        if (triggerAtMillis <= System.currentTimeMillis()) {
            Timber.w(
                "Alarm time for ${favoriteTime.id} (Route ${favoriteTime.routeNumber} at ${favoriteTime.departureTime}) " +
                        "is in the past or too soon (${formatMillis(triggerAtMillis)}). Not scheduling."
            )
            return
        }
        
        Timber.d("Scheduling alarm: departure=${formatMillis(calculatedDepartureTime)}, " +
                "notification=${formatMillis(triggerAtMillis)} ($leadTimeMinutes min before)")

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
        
        Timber.d("Route number for notification: '${favoriteTime.routeNumber}' -> '$routeInfoForNotification'")
        Timber.d("Full favoriteTime data: routeId='${favoriteTime.routeId}', routeNumber='${favoriteTime.routeNumber}', routeName='${favoriteTime.routeName}'")
        val destinationInfoForNotification = ""
        val departurePointStr = favoriteTime.departurePoint.trim()
        val departurePointInfoForNotification = if (departurePointStr.isNotBlank()) {
            "От: $departurePointStr"
        } else {
            ""
        }

        Timber.d(
            "Data for Intent: favoriteId='${favoriteTime.id}', " +
                    "routeInfo='$routeInfoForNotification', " +
                    "departureTimeInfo='$departureTimeInfoForNotification'"
        )

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

        Timber.d("Attempting to schedule alarm for ID ${favoriteTime.id} at ${formatMillis(triggerAtMillis)} (requestCode: $requestCode, action: ${intent.action})")
        Timber.d("Current time: ${formatMillis(System.currentTimeMillis())}, Target departure: ${formatMillis(calculatedDepartureTime)}")

        try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    val canScheduleExact = alarmManager.canScheduleExactAlarms()
                    Timber.i("Android S+ detected. Can schedule exact alarms for ID ${favoriteTime.id}? $canScheduleExact")
                    if (canScheduleExact) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                        Timber.d("Exact alarm scheduled successfully for ID ${favoriteTime.id} at ${formatMillis(triggerAtMillis)}")
                    } else {
                        Timber.w(
                            "Exact alarms NOT PERMITTED for ID ${favoriteTime.id}. Scheduling inexact alarm (setWindow)." +
                                    " User may need to grant permission in settings: ${Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM}"
                        )
                        alarmManager.setWindow(AlarmManager.RTC_WAKEUP, triggerAtMillis, 60_000L, pendingIntent)
                        Timber.d("Inexact (window) alarm scheduled for ID ${favoriteTime.id} around ${formatMillis(triggerAtMillis)}")
                    }
                }
                else -> {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                    Timber.d(
                        "Alarm (setExactAndAllowWhileIdle) scheduled for ID ${favoriteTime.id} at ${
                            formatMillis(triggerAtMillis)
                        } on Android M-R"
                    )
                }
            }
        } catch (se: SecurityException) {
            Timber.e("SecurityException: Cannot schedule alarm for ID ${favoriteTime.id}. " +
                    "Check permissions (e.g., SCHEDULE_EXACT_ALARM, WAKE_LOCK).", se)
        } catch (e: Exception) {
            Timber.e(e, "Failed to schedule alarm for ID ${favoriteTime.id}")
        }
    }

    /**
     * Отменяет запланированное уведомление для указанного избранного времени
     * 
     * Использует тот же requestCode и action что были при создании будильника
     * для точной идентификации и отмены. Если будильник не найден (уже отменен
     * или никогда не был создан), операция завершается безопасно.
     * 
     * Алгоритм отмены:
     * 1. Получает AlarmManager из системы
     * 2. Создает Intent с тем же action что при планировании
     * 3. Вычисляет тот же requestCode из ID избранного времени
     * 4. Получает PendingIntent с FLAG_NO_CREATE (не создавать новый)
     * 5. Если найден - отменяет через AlarmManager
     * 6. Логирует результат операции
     * 
     * @param context контекст приложения для доступа к системным сервисам
     * @param favoriteTimeId уникальный идентификатор избранного времени
     * 
     * @see scheduleAlarm для планирования уведомления
     * @see updateAllAlarmsBasedOnSettings для массовой отмены и перепланирования
     */
    fun cancelAlarm(context: Context, favoriteTimeId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        if (alarmManager == null) {
            Timber.e("AlarmManager is null. Cannot cancel alarm for ID $favoriteTimeId.")
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
                Timber.d("Alarm cancelled successfully for ID $favoriteTimeId (requestCode: $requestCode, action: ${intent.action})")
            } catch (e: Exception) {
                Timber.e(e, "Error cancelling alarm for ID $favoriteTimeId")
            }
        } else {
            Timber.w("No alarm found to cancel for ID $favoriteTimeId (PendingIntent was null). " +
                    "This is normal if it was already cancelled or never scheduled with this ID/action/requestCode.")
        }
    }

    /**
     * Вычисляет время следующего отправления в миллисекундах с учетом режима уведомлений
     * 
     * Определяет когда должно сработать следующее уведомление исходя из:
     * - Времени отправления (HH:mm)
     * - Дня недели из favoriteTime
     * - Режима уведомлений (ALL_DAYS/WEEKDAYS/SELECTED_DAYS)
     * - Выбранных дней недели пользователем
     * 
     * Алгоритм:
     * 1. Парсит и валидирует время отправления (HH:mm)
     * 2. Валидирует день недели (1-7, где 1=воскресенье)
     * 3. Создает базовый Calendar с указанным временем на сегодня
     * 4. В зависимости от режима уведомлений:
     *    - ALL_DAYS: берет следующее вхождение этого времени
     *    - WEEKDAYS: ищет ближайший будний день (пн-пт)
     *    - SELECTED_DAYS: ищет ближайший выбранный день
     * 5. Возвращает timestamp в миллисекундах или -1 при ошибке
     * 
     * Ограничения:
     * - Поиск ограничен 14 днями вперед (2 недели)
     * - Если подходящий день не найден, возвращает -1
     * 
     * @param context контекст для доступа к кэшу настроек
     * @param favoriteTime данные избранного времени с расписанием
     * @return timestamp следующего отправления в миллисекундах или -1 при ошибке
     */
    private fun calculateNextDepartureTimeInMillis(context: Context, favoriteTime: FavoriteTime): Long {
        if (favoriteTime.departureTime.isBlank()) {
            Timber.e("Departure time is blank for ID ${favoriteTime.id}")
            return -1L
        }
        val timeParts = favoriteTime.departureTime.split(":")
        if (timeParts.size != 2) {
            Timber.e("Invalid departure time format: '${favoriteTime.departureTime}' for ID ${favoriteTime.id}")
            return -1L
        }

        val hour: Int
        val minute: Int
        try {
            hour = timeParts[0].trim().toInt()
            minute = timeParts[1].trim().toInt()
        } catch (nfe: NumberFormatException) {
            Timber.e(nfe, "Invalid number format in departure time parts: '${favoriteTime.departureTime}' for ID ${favoriteTime.id}")
            return -1L
        }

        if (hour !in 0..23 || minute !in 0..59) {
            Timber.e("Invalid time values: hour=$hour, minute=$minute for ID ${favoriteTime.id}")
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
        
        Timber.d("Calculating departure time for ${favoriteTime.id} in timezone: ${now.timeZone.id}")

        // Получаем режим уведомлений и создаем соответствующую стратегию
        val notificationMode = NotificationPreferencesCache.getNotificationMode(favoriteTime.routeId)
        
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
            Timber.e("Strategy ${strategy::class.simpleName} failed to find suitable time for ${favoriteTime.id}")
        } else {
            Timber.d("Calculated next departure for ${favoriteTime.id} using ${strategy::class.simpleName}: ${formatMillis(nextTimeMillis)}")
        }
        
        return nextTimeMillis
    }

    /**
     * Обновляет все активные уведомления в соответствии с текущими настройками
     * 
     * Вызывается при изменении пользовательских настроек уведомлений:
     * - Изменение режима уведомлений (ALL_DAYS -> WEEKDAYS и т.д.)
     * - Изменение выбранных дней недели
     * - Включение/отключение уведомлений
     * - Изменение тихого режима
     * 
     * Алгоритм обновления:
     * 1. Для каждого избранного времени:
     *    a) Отменяет существующий будильник
     *    b) Получает актуальный режим уведомлений из кэша
     *    c) Если уведомления не отключены - планирует новый будильник
     *    d) Логирует результат операции
     * 2. Обрабатывает ошибки индивидуально для каждого элемента
     * 
     * Безопасность:
     * - Ошибки при обновлении одного элемента не влияют на остальные
     * - Все операции логируются для отладки
     * - Используется кэш настроек для избежания блокировки потока
     * 
     * @param context контекст для доступа к системным сервисам и настройкам
     * @param favoriteTimes список всех избранных времен для обновления
     * 
     * @see NotificationPreferencesCache.getNotificationMode для получения режима
     * @see scheduleAlarm для планирования отдельного уведомления
     * @see cancelAlarm для отмены уведомления
     */
    fun updateAllAlarmsBasedOnSettings(context: Context, favoriteTimes: List<FavoriteTime>) {
        Timber.d("Updating all alarms based on current notification settings")
        
        favoriteTimes.forEach { favoriteTime ->
            try {
                cancelAlarm(context, favoriteTime.id)
                
                // Проверяем режим уведомлений из кэша (без runBlocking)
                val notificationMode = NotificationPreferencesCache.getNotificationMode(favoriteTime.routeId)
                
                if (notificationMode != NotificationMode.DISABLED) {
                    scheduleAlarm(context, favoriteTime)
                    Timber.d("Rescheduled alarm for ${favoriteTime.id} based on settings")
                } else {
                    Timber.d("Alarm for ${favoriteTime.id} cancelled - notifications DISABLED")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error updating alarm for ${favoriteTime.id}")
            }
        }
    }

    /**
     * Проверяет и обновляет уведомление для одного избранного времени
     * 
     * Упрощенная версия updateAllAlarmsBasedOnSettings для одного элемента.
     * Проверяет текущие настройки и либо планирует, либо отменяет уведомление.
     * 
     * Используется когда:
     * - Добавляется новое избранное время
     * - Изменяются настройки уведомлений для конкретного маршрута
     * - Требуется быстрое обновление без полного пересканирования
     * 
     * ВАЖНО: НЕ проверяет shouldSendNotification, так как планирование
     * должно происходить независимо от текущего дня - scheduleAlarm 
     * сам найдет следующий подходящий день согласно режиму.
     * 
     * @param context контекст для доступа к системным сервисам
     * @param favoriteTime избранное время для проверки и обновления
     * 
     * @see scheduleAlarm для планирования уведомления
     * @see cancelAlarm для отмены уведомления
     */
    fun checkAndUpdateNotifications(context: Context, favoriteTime: FavoriteTime) {
        // Проверяем только режим уведомлений (не текущий день!)
        val notificationMode = NotificationPreferencesCache.getNotificationMode(favoriteTime.routeId)
        
        if (notificationMode != NotificationMode.DISABLED) {
            scheduleAlarm(context, favoriteTime)
            Timber.d("Notification scheduled for ${favoriteTime.id} (mode: $notificationMode)")
        } else {
            cancelAlarm(context, favoriteTime.id)
            Timber.d("Notification cancelled for ${favoriteTime.id} - DISABLED mode")
        }
    }

    /**
     * Форматирует timestamp в читаемую строку для логирования
     * 
     * Используется для отладки и логирования времен срабатывания будильников.
     * Преобразует миллисекунды Unix timestamp в формат "yyyy-MM-dd HH:mm:ss".
     * 
     * @param millis timestamp в миллисекундах (Unix time)
     * @return отформатированная строка даты и времени или сообщение об ошибке
     */
    private fun formatMillis(millis: Long): String {
        return try {
            if (millis <= 0) return "Invalid or Past Millis"
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(millis)
        } catch (e: Exception) {
            Timber.e(e, "Error formatting millis: $millis")
            "Error formatting timestamp"
        }
    }
    
    /**
     * Конвертирует день недели из формата Calendar в формат java.time.DayOfWeek
     * 
     * Calendar формат (из BusSchedule):
     * - 1 = Воскресенье
     * - 2 = Понедельник
     * - 3 = Вторник
     * - 4 = Среда
     * - 5 = Четверг
     * - 6 = Пятница
     * - 7 = Суббота
     * 
     * @param calendarDay день недели в формате Calendar (1-7)
     * @return DayOfWeek или null если неверное значение
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
                Timber.e("Invalid calendar day: $calendarDay")
                null
            }
        }
    }
}