package com.example.lets_go_slavgorod.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.lets_go_slavgorod.data.local.NotificationPreferencesCache
import com.example.lets_go_slavgorod.data.model.FavoriteTime
import com.example.lets_go_slavgorod.notifications.AlarmScheduler.cancelAlarm
import com.example.lets_go_slavgorod.ui.viewmodel.NotificationMode
import com.example.lets_go_slavgorod.utils.Constants
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
    //                              КОНСТАНТЫ И КЛЮЧИ
    // =====================================================================================
    
    /** Префикс для генерации уникальных request codes для каждого будильника */
    private const val ALARM_REQUEST_CODE_PREFIX = Constants.ALARM_REQUEST_CODE_PREFIX
    
    /** 
     * Время опережения уведомления перед отправлением автобуса
     * По умолчанию 5 минут = 300 000 миллисекунд
     */
    private const val FIVE_MINUTES_IN_MILLIS = Constants.NOTIFICATION_LEAD_TIME_MINUTES * 60 * 1000L
    
    /** Ключ DataStore для хранения текущего тихого режима */
    private val QUIET_MODE_KEY = stringPreferencesKey("quiet_mode")
    
    /** Ключ DataStore для хранения времени окончания временного отключения */
    private val QUIET_UNTIL_KEY = longPreferencesKey("quiet_until_time")

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
     * - Время опережения (5 минут до отправления)
     * 
     * Алгоритм планирования:
     * 1. Получает AlarmManager из системы
     * 2. Вычисляет следующее время отправления согласно режиму уведомлений
     * 3. Вычитает 5 минут для опережающего уведомления
     * 4. Проверяет что время в будущем
     * 5. Создает PendingIntent с данными маршрута
     * 6. Планирует будильник учитывая версию Android
     * 
     * Особенности работы:
     * - На Android S+ (API 31+) требуется разрешение SCHEDULE_EXACT_ALARM
     * - На Android M-R (API 23-30) использует setExactAndAllowWhileIdle
     * - При отсутствии разрешений планирует приблизительный будильник с окном ±1 минута
     * - Проверка shouldSendNotification откладывается до срабатывания в AlarmReceiver
     * 
     * @param context контекст приложения для доступа к системным сервисам
     * @param favoriteTime избранное время отправления с метаданными (ID, время, маршрут и т.д.)
     * 
     * @see cancelAlarm для отмены запланированного уведомления
     * @see updateAllAlarmsBasedOnSettings для обновления всех уведомлений
     * @see AlarmReceiver.onReceive для обработки срабатывания будильника
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

        val calculatedDepartureTime = calculateNextDepartureTimeInMillis(context, favoriteTime)
        if (calculatedDepartureTime == -1L) {
            Timber.e("Failed to calculate a valid departure time for ${favoriteTime.id}. Not scheduling.")
            return
        }

        val triggerAtMillis = calculatedDepartureTime - FIVE_MINUTES_IN_MILLIS

        if (triggerAtMillis <= System.currentTimeMillis()) {
            Timber.w(
                "Alarm time for ${favoriteTime.id} (Route ${favoriteTime.routeNumber} at ${favoriteTime.departureTime}) " +
                        "is in the past or too soon (${formatMillis(triggerAtMillis)}). Not scheduling."
            )
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

        val targetDayOfWeek = favoriteTime.dayOfWeek
        if (targetDayOfWeek !in Calendar.SUNDAY..Calendar.SATURDAY) {
            Timber.e("Invalid dayOfWeek: $targetDayOfWeek for ID ${favoriteTime.id}. Expected ${Calendar.SUNDAY}-${Calendar.SATURDAY}.")
            return -1L
        }

        val now = Calendar.getInstance()
        val nextDepartureBase = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Получаем режим уведомлений из кэша (без runBlocking)
        val notificationMode = NotificationPreferencesCache.getNotificationMode(favoriteTime.routeId)

        // Получаем выбранные дни из кэша
        val selectedDays = if (notificationMode == NotificationMode.SELECTED_DAYS) {
            NotificationPreferencesCache.getSelectedDays(favoriteTime.routeId)
        } else {
            emptySet()
        }

        when (notificationMode) {
            NotificationMode.ALL_DAYS -> {
                // Планируем на каждый день в указанное время
                val nextDeparture = (nextDepartureBase.clone() as Calendar).apply {
                    if (!after(now)) {
                        add(Calendar.DAY_OF_YEAR, 1)
                    }
                }
                Timber.d("Calculated next departure for ${favoriteTime.id} (${favoriteTime.departureTime}, ALL_DAYS): ${formatMillis(nextDeparture.timeInMillis)}")
                return nextDeparture.timeInMillis
            }
            NotificationMode.WEEKDAYS -> {
                // Планируем только на будние дни (пн-пт)
                for (i in 0..14) {  // Проверяем 2 недели вперед
                    val candidateDeparture = (nextDepartureBase.clone() as Calendar).apply {
                        add(Calendar.DAY_OF_YEAR, i)
                    }
                    val candidateDay = candidateDeparture.get(Calendar.DAY_OF_WEEK)
                    val isWeekday = candidateDay in Calendar.MONDAY..Calendar.FRIDAY
                    
                    if (isWeekday && candidateDeparture.after(now)) {
                        Timber.d("Calculated next departure for ${favoriteTime.id} (${favoriteTime.departureTime}, WEEKDAYS): ${formatMillis(candidateDeparture.timeInMillis)} (found after $i day iterations)")
                        return candidateDeparture.timeInMillis
                    }
                }
            }
            NotificationMode.SELECTED_DAYS -> {
                // Планируем только на выбранные дни
                for (i in 0..14) {  // Проверяем 2 недели вперед
                    val candidateDeparture = (nextDepartureBase.clone() as Calendar).apply {
                        add(Calendar.DAY_OF_YEAR, i)
                    }
                    val candidateDay = candidateDeparture.get(Calendar.DAY_OF_WEEK)
                    val candidateDayOfWeek = when (candidateDay) {
                        Calendar.SUNDAY -> DayOfWeek.SUNDAY
                        Calendar.MONDAY -> DayOfWeek.MONDAY
                        Calendar.TUESDAY -> DayOfWeek.TUESDAY
                        Calendar.WEDNESDAY -> DayOfWeek.WEDNESDAY
                        Calendar.THURSDAY -> DayOfWeek.THURSDAY
                        Calendar.FRIDAY -> DayOfWeek.FRIDAY
                        Calendar.SATURDAY -> DayOfWeek.SATURDAY
                        else -> null
                    }
                    val isSelectedDay = candidateDayOfWeek != null && candidateDayOfWeek in selectedDays
                    
                    if (isSelectedDay && candidateDeparture.after(now)) {
                        Timber.d("Calculated next departure for ${favoriteTime.id} (${favoriteTime.departureTime}, SELECTED_DAYS): ${formatMillis(candidateDeparture.timeInMillis)} (found after $i day iterations)")
                        return candidateDeparture.timeInMillis
                    }
                }
            }
            NotificationMode.DISABLED -> {
                Timber.d("Notifications disabled, not scheduling for ${favoriteTime.id}")
                return -1L
            }
        }

        Timber.e("Could not find a suitable future departure day within a week for ${favoriteTime.id}")
        return -1L
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
     * @param context контекст для доступа к системным сервисам
     * @param favoriteTime избранное время для проверки и обновления
     * 
     * @see shouldSendNotification для проверки необходимости уведомления
     * @see scheduleAlarm для планирования уведомления
     * @see cancelAlarm для отмены уведомления
     */
    fun checkAndUpdateNotifications(context: Context, favoriteTime: FavoriteTime) {
        if (shouldSendNotification(context, favoriteTime.routeId)) {
            scheduleAlarm(context, favoriteTime)
            Timber.d("Notification scheduled for ${favoriteTime.id}")
        } else {
            cancelAlarm(context, favoriteTime.id)
            Timber.d("Notification cancelled for ${favoriteTime.id} due to settings")
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
}