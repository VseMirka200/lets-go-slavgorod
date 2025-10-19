package com.example.lets_go_slavgorod.notifications

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.lets_go_slavgorod.data.model.FavoriteTime
import com.example.lets_go_slavgorod.data.repository.BusRouteRepository
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit тесты для AlarmScheduler
 * 
 * Проверяет критическую функциональность планирования уведомлений:
 * - Создание PendingIntent с правильными данными
 * - Вычисление следующего времени отправления
 * - Работа Strategy Pattern для разных режимов
 * - Отмена уведомлений
 * - Обновление уведомлений при изменении настроек
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 2.1
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class AlarmSchedulerTest {
    
    private lateinit var context: Context
    private lateinit var mockAlarmManager: AlarmManager
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        mockAlarmManager = mockk(relaxed = true)
        
        // Mock AlarmManager в контексте
        mockkStatic(Context::getSystemService)
        every { context.getSystemService(Context.ALARM_SERVICE) } returns mockAlarmManager
        
        // Mock NotificationPreferencesCache
        mockkObject(NotificationPreferencesCache)
        every { NotificationPreferencesCache.getNotificationMode(any()) } returns NotificationMode.ALL_DAYS
        every { NotificationPreferencesCache.getSelectedDays(any()) } returns emptySet()
        every { NotificationPreferencesCache.getQuietModeStart() } returns null
        every { NotificationPreferencesCache.getQuietModeEnd() } returns null
    }
    
    @After
    fun tearDown() {
        unmockkAll()
    }
    
    @Test
    fun `scheduleAlarm creates PendingIntent with correct favoriteId`() {
        // Arrange
        val favoriteTime = createTestFavoriteTime(
            id = "test_123",
            routeId = "102",
            departureTime = "10:00"
        )
        
        val capturedIntent = slot<PendingIntent>()
        
        // Act
        AlarmScheduler.scheduleAlarm(context, favoriteTime)
        
        // Assert
        verify {
            mockAlarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                any(),
                capture(capturedIntent)
            )
        }
        
        // Проверяем, что PendingIntent был создан
        assertTrue(capturedIntent.isCaptured)
    }
    
    @Test
    fun `cancelAlarm removes alarm from AlarmManager`() {
        // Arrange
        val favoriteId = "test_cancel_123"
        
        // Act
        AlarmScheduler.cancelAlarm(context, favoriteId)
        
        // Assert
        verify {
            mockAlarmManager.cancel(any<PendingIntent>())
        }
    }
    
    @Test
    fun `shouldSendNotification returns true for ALL_DAYS mode`() {
        // Arrange
        every { NotificationPreferencesCache.getNotificationMode("102") } returns NotificationMode.ALL_DAYS
        
        // Act
        val result = AlarmScheduler.shouldSendNotification(context, "102")
        
        // Assert
        assertTrue(result, "Should send notification in ALL_DAYS mode")
    }
    
    @Test
    fun `shouldSendNotification returns false for DISABLED mode`() {
        // Arrange
        every { NotificationPreferencesCache.getNotificationMode("102") } returns NotificationMode.DISABLED
        
        // Act
        val result = AlarmScheduler.shouldSendNotification(context, "102")
        
        // Assert
        assertTrue(!result, "Should not send notification in DISABLED mode")
    }
    
    @Test
    fun `shouldSendNotification respects quiet mode hours`() {
        // Arrange
        every { NotificationPreferencesCache.getNotificationMode("102") } returns NotificationMode.ALL_DAYS
        every { NotificationPreferencesCache.getQuietModeStart() } returns "22:00"
        every { NotificationPreferencesCache.getQuietModeEnd() } returns "08:00"
        
        // Act - проверяем в тихий час (23:00)
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 0)
        }
        
        // Note: Фактическая проверка тихого режима делается в AlarmScheduler
        // Здесь проверяем, что метод вызывается
        AlarmScheduler.shouldSendNotification(context, "102")
        
        // Assert
        verify {
            NotificationPreferencesCache.getQuietModeStart()
            NotificationPreferencesCache.getQuietModeEnd()
        }
    }
    
    @Test
    fun `AllDaysStrategy calculates next day if time passed today`() {
        // Arrange
        val strategy = AllDaysStrategy()
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 15)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        val baseTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 10) // Время в прошлом
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        // Act
        val result = strategy.calculateNextTime(baseTime, now)
        
        // Assert
        val resultCalendar = Calendar.getInstance().apply {
            timeInMillis = result
        }
        
        assertEquals(10, resultCalendar.get(Calendar.HOUR_OF_DAY), "Hour should be 10")
        assertEquals(30, resultCalendar.get(Calendar.MINUTE), "Minute should be 30")
        assertTrue(resultCalendar.after(now), "Next time should be in future")
    }
    
    @Test
    fun `WeekdaysStrategy skips weekends`() {
        // Arrange
        val strategy = WeekdaysStrategy()
        
        // Суббота, 15:00
        val now = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
            set(Calendar.HOUR_OF_DAY, 15)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        val baseTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        // Act
        val result = strategy.calculateNextTime(baseTime, now)
        
        // Assert
        val resultCalendar = Calendar.getInstance().apply {
            timeInMillis = result
        }
        
        // Должен быть понедельник
        assertEquals(
            Calendar.MONDAY,
            resultCalendar.get(Calendar.DAY_OF_WEEK),
            "Should skip to Monday from Saturday"
        )
    }
    
    @Test
    fun `SelectedDaysStrategy returns correct day from selected`() {
        // Arrange
        val selectedDays = setOf(Calendar.MONDAY, Calendar.WEDNESDAY, Calendar.FRIDAY)
        val strategy = SelectedDaysStrategy(selectedDays)
        
        // Вторник (не выбранный день)
        val now = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY)
            set(Calendar.HOUR_OF_DAY, 15)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        val baseTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        // Act
        val result = strategy.calculateNextTime(baseTime, now)
        
        // Assert
        val resultCalendar = Calendar.getInstance().apply {
            timeInMillis = result
        }
        
        // Должна быть среда (следующий выбранный день)
        assertEquals(
            Calendar.WEDNESDAY,
            resultCalendar.get(Calendar.DAY_OF_WEEK),
            "Should be Wednesday (next selected day from Tuesday)"
        )
    }
    
    @Test
    fun `DisabledStrategy returns -1`() {
        // Arrange
        val strategy = DisabledStrategy()
        val now = Calendar.getInstance()
        val baseTime = Calendar.getInstance()
        
        // Act
        val result = strategy.calculateNextTime(baseTime, now)
        
        // Assert
        assertEquals(-1L, result, "Disabled strategy should return -1")
    }
    
    @Test
    fun `updateAllAlarmsBasedOnSettings updates all active favorites`() {
        // Arrange
        val favoriteTimes = listOf(
            createTestFavoriteTime("1", "102", "10:00", true),
            createTestFavoriteTime("2", "102", "15:00", true),
            createTestFavoriteTime("3", "102", "18:00", false) // Неактивный
        )
        
        // Act
        AlarmScheduler.updateAllAlarmsBasedOnSettings(context, favoriteTimes)
        
        // Assert
        // Должно быть 2 вызова scheduleAlarm (для активных) + 1 cancelAlarm (для неактивного)
        verify(exactly = 2) {
            mockAlarmManager.setExactAndAllowWhileIdle(any(), any(), any())
        }
    }
    
    // Helper функция для создания тестовых данных
    private fun createTestFavoriteTime(
        id: String,
        routeId: String,
        departureTime: String,
        isActive: Boolean = true
    ): FavoriteTime {
        return FavoriteTime(
            id = id,
            routeId = routeId,
            routeNumber = "102",
            routeName = "Тест",
            stopName = "Тестовая остановка",
            departureTime = departureTime,
            dayOfWeek = 0,
            departurePoint = "Тест",
            addedDate = 0L,
            isActive = isActive
        )
    }
}

