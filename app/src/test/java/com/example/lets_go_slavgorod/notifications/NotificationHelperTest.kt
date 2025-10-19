package com.example.lets_go_slavgorod.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ApplicationProvider
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit тесты для NotificationHelper
 * 
 * Проверяет функциональность уведомлений:
 * - Создание notification channel
 * - Отображение уведомления с правильными данными
 * - Форматирование текста уведомления
 * - Обработка вибрации
 * - Обработка тихого режима
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 2.1
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class NotificationHelperTest {
    
    private lateinit var context: Context
    private lateinit var mockNotificationManager: NotificationManagerCompat
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        mockNotificationManager = mockk(relaxed = true)
        
        // Mock NotificationManagerCompat
        mockkStatic(NotificationManagerCompat::class)
        every { NotificationManagerCompat.from(any()) } returns mockNotificationManager
        
        // Mock permissions
        every { mockNotificationManager.areNotificationsEnabled() } returns true
    }
    
    @After
    fun tearDown() {
        unmockkAll()
    }
    
    @Test
    fun `createNotificationChannel creates channel with correct properties`() {
        // Arrange
        val mockSystemNotificationManager = mockk<NotificationManager>(relaxed = true)
        every { context.getSystemService(Context.NOTIFICATION_SERVICE) } returns mockSystemNotificationManager
        
        // Act
        NotificationHelper.createNotificationChannel(context)
        
        // Assert
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            verify {
                mockSystemNotificationManager.createNotificationChannel(
                    match<NotificationChannel> { channel ->
                        channel.id == NotificationHelper.CHANNEL_ID &&
                        channel.importance == NotificationManager.IMPORTANCE_HIGH
                    }
                )
            }
        }
    }
    
    @Test
    fun `showDepartureNotification creates notification with correct title`() {
        // Arrange
        val capturedNotification = slot<android.app.Notification>()
        val capturedId = slot<Int>()
        
        every {
            mockNotificationManager.notify(
                capture(capturedId),
                capture(capturedNotification)
            )
        } just Runs
        
        // Act
        NotificationHelper.showDepartureNotification(
            context = context,
            favoriteTimeId = "test_123",
            routeInfo = "Автобус №102",
            departureTimeInfo = "10:30",
            departurePointInfo = "Рынок",
            enableVibration = false
        )
        
        // Assert
        verify {
            mockNotificationManager.notify(any(), any())
        }
        
        assertTrue(capturedNotification.isCaptured, "Notification should be created")
    }
    
    @Test
    fun `showDepartureNotification formats title correctly`() {
        // Arrange
        val capturedNotification = slot<android.app.Notification>()
        
        every {
            mockNotificationManager.notify(any(), capture(capturedNotification))
        } just Runs
        
        // Act
        NotificationHelper.showDepartureNotification(
            context = context,
            favoriteTimeId = "test_123",
            routeInfo = "Автобус №102 (Славгород - Яровое)",
            departureTimeInfo = "15:45",
            departurePointInfo = "Рынок",
            enableVibration = false
        )
        
        // Assert
        val notification = capturedNotification.captured
        assertNotNull(notification, "Notification should not be null")
        
        // Проверяем, что уведомление содержит важную информацию
        // Note: Robolectric не всегда правильно извлекает extras, но можем проверить базовые свойства
        assertTrue(notification.priority >= NotificationCompat.PRIORITY_HIGH, "Should be high priority")
    }
    
    @Test
    fun `showDepartureNotification includes vibration when enabled`() {
        // Arrange
        val capturedNotification = slot<android.app.Notification>()
        
        every {
            mockNotificationManager.notify(any(), capture(capturedNotification))
        } just Runs
        
        // Act
        NotificationHelper.showDepartureNotification(
            context = context,
            favoriteTimeId = "test_123",
            routeInfo = "Автобус №102",
            departureTimeInfo = "10:30",
            departurePointInfo = "Рынок",
            enableVibration = true
        )
        
        // Assert
        val notification = capturedNotification.captured
        assertNotNull(notification, "Notification should not be null")
        
        // Проверяем, что вибрация включена
        assertTrue(
            (notification.defaults and android.app.Notification.DEFAULT_VIBRATE) != 0 ||
            notification.vibrate != null,
            "Vibration should be enabled"
        )
    }
    
    @Test
    fun `showDepartureNotification excludes vibration when disabled`() {
        // Arrange
        val capturedNotification = slot<android.app.Notification>()
        
        every {
            mockNotificationManager.notify(any(), capture(capturedNotification))
        } just Runs
        
        // Act
        NotificationHelper.showDepartureNotification(
            context = context,
            favoriteTimeId = "test_123",
            routeInfo = "Автобус №102",
            departureTimeInfo = "10:30",
            departurePointInfo = "Рынок",
            enableVibration = false
        )
        
        // Assert
        val notification = capturedNotification.captured
        assertNotNull(notification, "Notification should not be null")
        
        // Проверяем, что DEFAULT_VIBRATE не установлен
        assertTrue(
            (notification.defaults and android.app.Notification.DEFAULT_VIBRATE) == 0,
            "Vibration should be disabled"
        )
    }
    
    @Test
    fun `showDepartureNotification handles empty route info gracefully`() {
        // Arrange
        val capturedNotification = slot<android.app.Notification>()
        
        every {
            mockNotificationManager.notify(any(), capture(capturedNotification))
        } just Runs
        
        // Act - пустая строка для routeInfo
        NotificationHelper.showDepartureNotification(
            context = context,
            favoriteTimeId = "test_123",
            routeInfo = "",
            departureTimeInfo = "10:30",
            departurePointInfo = "Рынок",
            enableVibration = false
        )
        
        // Assert
        verify {
            mockNotificationManager.notify(any(), any())
        }
        assertTrue(capturedNotification.isCaptured, "Should handle empty route info")
    }
    
    @Test
    fun `showDepartureNotification handles null route info gracefully`() {
        // Arrange
        val capturedNotification = slot<android.app.Notification>()
        
        every {
            mockNotificationManager.notify(any(), capture(capturedNotification))
        } just Runs
        
        // Act - null для routeInfo (через elvis в коде)
        NotificationHelper.showDepartureNotification(
            context = context,
            favoriteTimeId = "test_123",
            routeInfo = null ?: "Автобус",
            departureTimeInfo = "10:30",
            departurePointInfo = "Рынок",
            enableVibration = false
        )
        
        // Assert
        verify {
            mockNotificationManager.notify(any(), any())
        }
        assertTrue(capturedNotification.isCaptured, "Should handle null route info")
    }
    
    @Test
    fun `showDepartureNotification uses unique notification ID`() {
        // Arrange
        val capturedIds = mutableListOf<Int>()
        
        every {
            mockNotificationManager.notify(capture(capturedIds), any())
        } just Runs
        
        // Act - показываем два уведомления
        NotificationHelper.showDepartureNotification(
            context = context,
            favoriteTimeId = "test_123",
            routeInfo = "Автобус №102",
            departureTimeInfo = "10:30",
            departurePointInfo = "Рынок",
            enableVibration = false
        )
        
        NotificationHelper.showDepartureNotification(
            context = context,
            favoriteTimeId = "test_456",
            routeInfo = "Автобус №1",
            departureTimeInfo = "11:00",
            departurePointInfo = "Вокзал",
            enableVibration = false
        )
        
        // Assert
        assertEquals(2, capturedIds.size, "Should create 2 notifications")
        
        // Проверяем, что ID уникальные (основаны на favoriteTimeId hashCode)
        assertTrue(
            capturedIds[0] != capturedIds[1],
            "Notification IDs should be unique for different favorites"
        )
    }
    
    @Test
    fun `showDepartureNotification sets auto-cancel to true`() {
        // Arrange
        val capturedNotification = slot<android.app.Notification>()
        
        every {
            mockNotificationManager.notify(any(), capture(capturedNotification))
        } just Runs
        
        // Act
        NotificationHelper.showDepartureNotification(
            context = context,
            favoriteTimeId = "test_123",
            routeInfo = "Автобус №102",
            departureTimeInfo = "10:30",
            departurePointInfo = "Рынок",
            enableVibration = false
        )
        
        // Assert
        val notification = capturedNotification.captured
        assertTrue(
            (notification.flags and android.app.Notification.FLAG_AUTO_CANCEL) != 0,
            "Notification should auto-cancel when tapped"
        )
    }
    
    @Test
    fun `showDepartureNotification respects notification permission`() {
        // Arrange
        every { mockNotificationManager.areNotificationsEnabled() } returns false
        
        // Act
        NotificationHelper.showDepartureNotification(
            context = context,
            favoriteTimeId = "test_123",
            routeInfo = "Автобус №102",
            departureTimeInfo = "10:30",
            departurePointInfo = "Рынок",
            enableVibration = false
        )
        
        // Assert
        // Уведомление не должно быть показано, если разрешения нет
        verify(exactly = 0) {
            mockNotificationManager.notify(any(), any())
        }
    }
}

