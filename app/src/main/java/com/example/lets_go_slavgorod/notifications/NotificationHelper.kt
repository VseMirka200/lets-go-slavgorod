package com.example.lets_go_slavgorod.notifications

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
 * Вспомогательный класс для работы с уведомлениями
 * 
 * Основные функции:
 * - Создание канала уведомлений
 * - Отображение уведомлений о времени отправления автобусов
 * - Отображение уведомлений об обновлениях приложения
 * - Обработка разрешений для уведомлений
 * - Использование кастомных иконок приложения
 * 
 * Иконки уведомлений:
 * - ic_notification_app - для уведомлений о времени отправления
 * - ic_notification_update - для уведомлений об обновлениях
 * - ic_launcher_foreground - большая иконка приложения
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
            
            Timber.d("Notification channels created: $CHANNEL_ID, $UPDATE_CHANNEL_ID")
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
        createNotificationChannel(context.applicationContext)
        
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
                    Timber.d("Vibration triggered for notification")
                } else {
                    Timber.w("Vibrator not available or doesn't have vibrator")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error triggering vibration")
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val uniqueRequestId = (NOTIFICATION_ID_BASE.toString() + favoriteTimeId).hashCode()

        val pendingIntent = PendingIntent.getActivity(
            context,
            uniqueRequestId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Временно используем стандартную иконку до исправления проблем с линковкой
        val smallIconResId = R.drawable.ic_stat_directions_bus
        val largeIconResId = R.drawable.ic_launcher_foreground
        
        // Используем стандартную иконку
        val finalSmallIcon = smallIconResId
        
        Timber.d("Notification icon: smallIcon=$smallIconResId, largeIcon=$largeIconResId")
        
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
        
        Timber.d("Notification title: '$combinedTitleText' (routeInfo: '$routeInfo' -> '$safeRouteInfo')")
        Timber.d("Notification content: '$contentSubText'")
        Timber.d("Notification data: favoriteTimeId='$favoriteTimeId', routeInfo='$routeInfo', departureTime='$departureTimeInfo'")
        // Обработка большой иконки с fallback
        val largeIcon = try {
            android.graphics.BitmapFactory.decodeResource(context.resources, largeIconResId)
        } catch (e: Exception) {
            Timber.w("Failed to load large icon, using null: ${e.message}")
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
            .build()

        val notificationManager = NotificationManagerCompat.from(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Timber.w("POST_NOTIFICATIONS permission not granted for favoriteTimeId: $favoriteTimeId. Notification will not be shown on Android 13+.")
                return
            }
        }

        notificationManager.notify(uniqueRequestId, notification)

        Timber.i("Notification shown with ID $uniqueRequestId for $favoriteTimeId. Combined Title: '$combinedTitleText', SubText: '$contentSubText'")
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

        // Временно используем стандартную иконку до исправления проблем с линковкой
        val smallIconResId = R.drawable.ic_launcher_foreground
        val title = "Доступно обновление $versionName"
        val contentText = releaseNotes?.takeIf { it.isNotBlank() } 
            ?: "Доступна новая версия приложения с улучшениями и исправлениями."

        // Используем стандартную иконку
        val finalSmallIcon = smallIconResId

        // Обработка большой иконки для уведомлений об обновлениях
        val largeIcon = try {
            android.graphics.BitmapFactory.decodeResource(context.resources, R.drawable.ic_launcher_foreground)
        } catch (e: Exception) {
            Timber.w("Failed to load large icon for update notification, using null: ${e.message}")
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
                Timber.w("POST_NOTIFICATIONS permission not granted for update notification. Notification will not be shown on Android 13+.")
                return
            }
        }

        notificationManager.notify(UPDATE_NOTIFICATION_ID, notification)

        Timber.i("Update notification shown for version $versionName")
    }
}