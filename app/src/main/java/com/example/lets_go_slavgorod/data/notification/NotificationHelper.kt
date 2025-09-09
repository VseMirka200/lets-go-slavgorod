package com.example.lets_go_slavgorod.data.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.lets_go_slavgorod.MainActivity
import com.example.lets_go_slavgorod.R
import timber.log.Timber

/**
 * Вспомогательный класс для работы с уведомлениями (v3.1)
 * 
 * Основные функции:
 * - Создание канала уведомлений
 * - Отображение уведомлений о времени отправления автобусов
 * - Отображение уведомлений об обновлениях приложения
 * - Обработка разрешений для уведомлений
 * - Использование кастомных иконок приложения
 * - Поддержка вибрации с настройками пользователя
 * 
 * Новые возможности v3.1:
 * - **Улучшенная вибрация**: Поддержка современных API вибрации
 * - **Детальное логирование**: Подробные логи на русском языке
 * - **Обработка ошибок**: Улучшенная обработка исключений
 * - **Безопасность**: Проверка разрешений и системных настроек
 * 
 * Иконки уведомлений:
 * - ic_stat_directions_bus - для уведомлений о времени отправления
 * - ic_launcher_foreground - большая иконка приложения
 * 
 * @author VseMirka200
 * @version 3.1
 * @since 1.0
 */
object NotificationHelper {
    // ID канала уведомлений
    private const val CHANNEL_ID = "bus_departure_channel"
    // ID канала для обновлений
    private const val UPDATE_CHANNEL_ID = "app_update_channel"
    // Название канала уведомлений
    private const val CHANNEL_NAME = "Уведомления об отправлении"
    // Название канала обновлений
    private const val UPDATE_CHANNEL_NAME = "Обновления приложения"
    // Базовый ID для уведомлений
    private const val NOTIFICATION_ID_BASE = 1000
    // ID уведомления об обновлении
    private const val UPDATE_NOTIFICATION_ID = 9999
    // ID уведомления об обновлении расписания
    private const val SCHEDULE_UPDATE_NOTIFICATION_ID = 9998
    // Группа для уведомлений об отправлении
    private const val NOTIFICATION_GROUP_DEPARTURE = "bus_departure_group"

    /**
     * Создает канал уведомлений для Android 8.0+
     * 
     * Настраивает канал с высоким приоритетом для уведомлений о времени отправления
     * 
     * @param context контекст приложения
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Канал для уведомлений об отправлении автобусов
            val departureChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомления о скором отправлении автобуса"
            }
            notificationManager.createNotificationChannel(departureChannel)
            
            // Канал для уведомлений об обновлениях
            val updateChannel = NotificationChannel(
                UPDATE_CHANNEL_ID,
                UPDATE_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Уведомления о доступных обновлениях приложения"
            }
            notificationManager.createNotificationChannel(updateChannel)
            
        }
    }

    /**
     * Отображает уведомление о времени отправления автобуса
     * 
     * Создает уведомление с информацией о маршруте, времени отправления и точке отправления.
     * Проверяет разрешения для Android 13+ и обрабатывает ошибки.
     * 
     * @param context контекст приложения
     * @param favoriteTimeId ID избранного времени
     * @param routeInfo информация о маршруте
     * @param departureTimeInfo время отправления
     * @param departurePointInfo информация о точке отправления
     */
    fun showDepartureNotification(
        context: Context,
        favoriteTimeId: String,
        routeInfo: String,
        departureTimeInfo: String,
        departurePointInfo: String,
        enableVibration: Boolean = true
    ) {
        // Канал уже создан в BusApplication.onCreate()
        
        // Вибрация при получении уведомления
        if (enableVibration) {
            try {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
                    vibratorManager?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                }
                
                if (vibrator != null && vibrator.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        // Используем VibrationEffect для Android 8.0+
                        val effect = android.os.VibrationEffect.createWaveform(
                            longArrayOf(0, 250, 100, 250), // Пауза, вибрация, пауза, вибрация
                            -1 // Не повторять
                        )
                        vibrator.vibrate(effect)
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(500) // 500ms вибрация для старых версий
                    }
                } else {
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка запуска вибрации")
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Deep Link: передаем данные для открытия конкретного маршрута
            putExtra("OPEN_FAVORITE_ID", favoriteTimeId)
            putExtra("FROM_NOTIFICATION", true)
        }

        val uniqueRequestId = (NOTIFICATION_ID_BASE.toString() + favoriteTimeId).hashCode()

        val pendingIntent = PendingIntent.getActivity(
            context,
            uniqueRequestId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Используем стандартную иконку автобуса для уведомлений
        val smallIconResId = R.drawable.ic_stat_directions_bus
        val largeIconResId = R.drawable.ic_launcher_foreground
        val finalSmallIcon = smallIconResId
        
        
        // Улучшенная обработка routeInfo для уведомления
        val safeRouteInfo = routeInfo.ifBlank {
            "Автобус"
        }
        
        val combinedTitleText = "$safeRouteInfo ${departureTimeInfo.lowercase(java.util.Locale.getDefault())}"
        
        val subTextParts = mutableListOf<String>()
        if (departurePointInfo.isNotBlank()) {
            subTextParts.add(departurePointInfo)
        }
        subTextParts.add("Не опаздывайте!")

        val contentSubText = subTextParts.joinToString(separator = ". ")
        
        // Обработка большой иконки с fallback
        val largeIcon = try {
            android.graphics.BitmapFactory.decodeResource(context.resources, largeIconResId)
        } catch (e: Exception) {
            null
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(finalSmallIcon)
            .apply {
                largeIcon?.let { setLargeIcon(it) }
            }
            .setContentTitle(combinedTitleText)
            .setContentText(contentSubText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setGroup(NOTIFICATION_GROUP_DEPARTURE)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_ALL)
            .build()

        val notificationManager = NotificationManagerCompat.from(context)

        // Проверка разрешений
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }
        
        // Проверка, включены ли уведомления в системе
        if (!notificationManager.areNotificationsEnabled()) {
            return
        }
        
        // Отменяем предыдущее уведомление с тем же ID (если есть)
        notificationManager.cancel(uniqueRequestId)

        notificationManager.notify(uniqueRequestId, notification)

    }

    /**
     * Отображает уведомление о доступном обновлении приложения
     * 
     * Показывает уведомление с информацией о новой версии и кнопкой для скачивания.
     * При нажатии на уведомление открывается приложение.
     * 
     * @param context контекст приложения
     * @param versionName версия доступного обновления
     * @param releaseNotes описание изменений (опционально)
     */
    fun showUpdateNotification(
        context: Context,
        versionName: String,
        releaseNotes: String? = null
    ) {
        // Создаем Intent для открытия приложения
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            UPDATE_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Используем иконку приложения для уведомлений об обновлениях
        val smallIconResId = R.drawable.ic_launcher_foreground
        val title = "Доступно обновление $versionName"
        val contentText = releaseNotes?.takeIf { it.isNotBlank() } 
            ?: "Доступна новая версия приложения с улучшениями и исправлениями."
        val finalSmallIcon = smallIconResId

        // Обработка большой иконки для уведомлений об обновлениях
        val largeIcon = try {
            android.graphics.BitmapFactory.decodeResource(context.resources, R.drawable.ic_launcher_foreground)
        } catch (e: Exception) {
            null
        }

        val notification = NotificationCompat.Builder(context, UPDATE_CHANNEL_ID)
            .setSmallIcon(finalSmallIcon)
            .apply {
                largeIcon?.let { setLargeIcon(it) }
            }
            .setContentTitle(title)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(contentText)
            )
            .build()

        val notificationManager = NotificationManagerCompat.from(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        notificationManager.notify(UPDATE_NOTIFICATION_ID, notification)

    }
    
    /**
     * Отображает уведомление о доступности обновления расписания
     * 
     * Создает уведомление которое информирует пользователя о том,
     * что доступна новая версия расписания автобусов на GitHub.
     * 
     * @param context контекст приложения
     * @param dataVersion версия данных (опционально)
     */
    fun showScheduleUpdateNotification(
        context: Context,
        dataVersion: String? = null
    ) {
        // Создаем Intent для открытия настроек (Управление данными)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Можно добавить параметр для автоматического открытия экрана обновления
            putExtra("open_data_management", true)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            SCHEDULE_UPDATE_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val smallIconResId = R.drawable.ic_launcher_foreground
        val title = "Доступно обновление расписания"
        val contentText = if (dataVersion != null) {
            "Доступна версия $dataVersion расписания автобусов. Потяните экран вниз для обновления."
        } else {
            "Доступно новое расписание автобусов. Потяните экран вниз или откройте настройки для обновления."
        }

        val largeIcon = try {
            android.graphics.BitmapFactory.decodeResource(context.resources, R.drawable.ic_launcher_foreground)
        } catch (e: Exception) {
            null
        }

        val notification = NotificationCompat.Builder(context, UPDATE_CHANNEL_ID)
            .setSmallIcon(smallIconResId)
            .apply {
                largeIcon?.let { setLargeIcon(it) }
            }
            .setContentTitle(title)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(contentText)
            )
            .build()

        val notificationManager = NotificationManagerCompat.from(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        notificationManager.notify(SCHEDULE_UPDATE_NOTIFICATION_ID, notification)

    }
}