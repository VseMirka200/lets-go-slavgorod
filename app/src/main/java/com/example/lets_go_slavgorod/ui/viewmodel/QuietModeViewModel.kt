package com.example.lets_go_slavgorod.ui.viewmodel

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lets_go_slavgorod.data.local.AppDatabase
import com.example.lets_go_slavgorod.data.local.dataStore
import com.example.lets_go_slavgorod.data.local.NotificationPreferencesCache
import com.example.lets_go_slavgorod.data.model.FavoriteTime
import com.example.lets_go_slavgorod.notifications.AlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Calendar

/**
 * ViewModel для управления тихим режимом (глобальное отключение) уведомлений
 * 
 * Предоставляет возможность временного или постоянного отключения всех уведомлений
 * о предстоящих отправлениях автобусов. Полезно для ситуаций когда пользователь
 * не хочет получать уведомления (например, в отпуске или на выходных).
 * 
 * Основные функции:
 * - Управление режимами тихого режима (включено/отключено/на N дней)
 * - Автоматическое включение уведомлений по истечению таймера
 * - Обновление всех активных будильников при изменении режима
 * - Синхронизация с кэшем настроек для быстрого доступа
 * 
 * Режимы работы:
 * - ENABLED: уведомления работают (по умолчанию)
 * - DISABLED: все уведомления отключены
 * - CUSTOM_DAYS: уведомления отключены на N дней
 * 
 * @param context контекст для доступа к DataStore и базе данных
 * 
 * @author VseMirka200
 * @version 2.0
 * @since 1.0
 * 
 * @see QuietMode
 * @see NotificationPreferencesCache
 */
class QuietModeViewModel(private val context: Context) : ViewModel() {
    
    /** Текущий режим тихого режима */
    private val _quietMode = MutableStateFlow(QuietMode.ENABLED)
    val quietMode: StateFlow<QuietMode> = _quietMode.asStateFlow()
    
    /** Timestamp окончания тихого режима (для CUSTOM_DAYS) */
    private val _quietUntilTime = MutableStateFlow<Long?>(null)
    val quietUntilTime: StateFlow<Long?> = _quietUntilTime.asStateFlow()
    
    /** Количество дней для режима CUSTOM_DAYS */
    private val _customDays = MutableStateFlow(0)
    val customDays: StateFlow<Int> = _customDays.asStateFlow()
    
    companion object {
        /** Ключ для хранения режима тихого режима в DataStore */
        private val QUIET_MODE_KEY = stringPreferencesKey("quiet_mode")
        
        /** Ключ для хранения времени окончания тихого режима */
        private val QUIET_UNTIL_KEY = longPreferencesKey("quiet_until_time")
        
        /** Ключ для хранения количества дней в DataStore */
        private val CUSTOM_DAYS_KEY = stringPreferencesKey("custom_days_count")
    }
    
    init {
        // Загружаем сохраненные настройки при создании ViewModel
        loadQuietMode()
    }
    
    /**
     * Загружает настройки тихого режима из DataStore
     * 
     * Автоматически проверяет истечение таймера для режима CUSTOM_DAYS
     * и включает уведомления если время истекло.
     */
    private fun loadQuietMode() {
        viewModelScope.launch {
            context.dataStore.data.collect { preferences ->
                val modeString = preferences[QUIET_MODE_KEY] ?: QuietMode.ENABLED.name
                val untilTime = preferences[QUIET_UNTIL_KEY]
                val customDaysStr = preferences[CUSTOM_DAYS_KEY]
                
                try {
                    val mode = QuietMode.valueOf(modeString)
                    
                    // Проверяем, не истек ли срок отключения
                    if (untilTime != null && untilTime < System.currentTimeMillis()) {
                        // Время истекло - включаем уведомления
                        setQuietMode(QuietMode.ENABLED)
                    } else {
                        _quietMode.value = mode
                        _quietUntilTime.value = untilTime
                        _customDays.value = customDaysStr?.toIntOrNull() ?: 0
                    }
                } catch (e: IllegalArgumentException) {
                    Timber.w("Invalid quiet mode: $modeString, resetting to ENABLED")
                    _quietMode.value = QuietMode.ENABLED
                }
            }
        }
    }
    
    /**
     * Устанавливает режим тихого режима уведомлений
     * 
     * Сохраняет новый режим в DataStore, обновляет кэш настроек и
     * перепланирует все активные уведомления в соответствии с новым режимом.
     * 
     * Для режима CUSTOM_DAYS вычисляется timestamp окончания на основе
     * количества дней.
     * 
     * @param mode новый режим тихого режима
     * @param customDays количество дней для режима CUSTOM_DAYS (игнорируется для других режимов)
     */
    fun setQuietMode(mode: QuietMode, customDays: Int = 0) {
        viewModelScope.launch {
            try {
                val untilTime = if (mode == QuietMode.CUSTOM_DAYS && customDays > 0) {
                    calculateCustomDaysUntilTime(customDays)
                } else {
                    null
                }
                
                context.dataStore.edit { preferences ->
                    preferences[QUIET_MODE_KEY] = mode.name
                    if (untilTime != null) {
                        preferences[QUIET_UNTIL_KEY] = untilTime
                        preferences[CUSTOM_DAYS_KEY] = customDays.toString()
                    } else {
                        preferences.remove(QUIET_UNTIL_KEY)
                        preferences.remove(CUSTOM_DAYS_KEY)
                    }
                }
                
                _quietMode.value = mode
                _quietUntilTime.value = untilTime
                _customDays.value = customDays
                
                Timber.d("Notifications mode set to: $mode, days: $customDays, until: $untilTime")
                
                // ВАЖНО: Обновляем кэш перед обновлением будильников
                NotificationPreferencesCache.updateCache(context)
                
                // Обновляем все уведомления в соответствии с новым режимом
                updateAllAlarms()
            } catch (e: Exception) {
                Timber.e(e, "Error setting quiet mode")
            }
        }
    }
    
    /**
     * Обновляет все активные уведомления при изменении тихого режима
     * 
     * Загружает все активные избранные времена из базы данных
     * и вызывает AlarmScheduler для их перепланирования с учетом
     * нового режима тихого режима.
     */
    private fun updateAllAlarms() {
        viewModelScope.launch {
            try {
                val database = AppDatabase.getDatabase(context)
                val favoriteTimeDao = database.favoriteTimeDao()
                
                val favoriteTimeEntities = favoriteTimeDao.getAllFavoriteTimes().firstOrNull() ?: emptyList()
                val activeFavoriteTimes = favoriteTimeEntities
                    .filter { it.isActive }
                    .map { entity ->
                        FavoriteTime(
                            id = entity.id,
                            routeId = entity.routeId,
                            routeNumber = "N/A",
                            routeName = "Маршрут",
                            stopName = entity.stopName,
                            departureTime = entity.departureTime,
                            dayOfWeek = entity.dayOfWeek,
                            departurePoint = entity.departurePoint,
                            addedDate = entity.addedDate,
                            isActive = entity.isActive
                        )
                    }
                
                AlarmScheduler.updateAllAlarmsBasedOnSettings(context, activeFavoriteTimes)
                Timber.d("Updated all alarms based on quiet mode change")
            } catch (e: Exception) {
                Timber.e(e, "Error updating alarms after quiet mode change")
            }
        }
    }
    
    /**
     * Вычисляет timestamp окончания для режима CUSTOM_DAYS
     * 
     * Рассчитывает время окончания тихого режима на основе количества дней.
     * Время окончания устанавливается на начало дня (00:00:00).
     * 
     * @param days количество дней отключения уведомлений
     * @return timestamp окончания тихого режима (в миллисекундах)
     * 
     * @sample
     * Если сегодня 12.10.2025 15:30 и days=3,
     * вернет timestamp для 15.10.2025 00:00:00
     */
    private fun calculateCustomDaysUntilTime(days: Int): Long {
        return Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, days)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    
    /**
     * Проверяет, активен ли тихий режим в данный момент
     * 
     * Проверяет текущий режим и для CUSTOM_DAYS также проверяет
     * не истекло ли время. Если время истекло, автоматически
     * включает уведомления.
     * 
     * @return true если тихий режим активен (уведомления отключены),
     *         false если уведомления должны работать
     */
    fun isQuietModeActive(): Boolean {
        val mode = _quietMode.value
        val untilTime = _quietUntilTime.value
        
        return when {
            mode == QuietMode.ENABLED -> false
            mode == QuietMode.DISABLED -> true
            mode == QuietMode.CUSTOM_DAYS && untilTime != null -> {
                val isActive = System.currentTimeMillis() < untilTime
                if (!isActive) {
                    // Время истекло - включаем уведомления
                    setQuietMode(QuietMode.ENABLED)
                }
                isActive
            }
            else -> false
        }
    }
}
