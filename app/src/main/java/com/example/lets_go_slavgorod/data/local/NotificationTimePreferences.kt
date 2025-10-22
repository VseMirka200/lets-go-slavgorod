package com.example.lets_go_slavgorod.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.example.lets_go_slavgorod.core.Constants
import com.example.lets_go_slavgorod.core.toFavoriteTime
import com.example.lets_go_slavgorod.data.repository.BusRouteRepository
import com.example.lets_go_slavgorod.domain.notification.AlarmScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import timber.log.Timber

/**
 * Управление настройками времени уведомлений
 * 
 * Хранит:
 * - Глобальное время уведомления (по умолчанию для всех маршрутов)
 * - Индивидуальные настройки времени для каждого маршрута
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 2.1
 */
class NotificationTimePreferences(private val context: Context) {
    
    companion object {
        private val GLOBAL_LEAD_TIME_KEY = intPreferencesKey("global_notification_lead_time")
        
        /**
         * Создает ключ для времени уведомления конкретного маршрута
         */
        private fun routeLeadTimeKey(routeId: String) = 
            intPreferencesKey("route_${routeId}_lead_time")
    }
    
    /**
     * Глобальное время уведомления (по умолчанию)
     */
    val globalLeadTime: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[GLOBAL_LEAD_TIME_KEY] ?: Constants.DEFAULT_NOTIFICATION_LEAD_TIME
    }
    
    /**
     * Устанавливает глобальное время уведомления
     * 
     * После сохранения автоматически обновляет все активные уведомления
     */
    suspend fun setGlobalLeadTime(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[GLOBAL_LEAD_TIME_KEY] = minutes
        }
        Timber.d("✅ Global lead time set to $minutes minutes")
        
        // Временно отключено для предотвращения крашей
        // updateAllAlarmsAfterTimeChange()
    }
    
    /**
     * Получает время уведомления для конкретного маршрута
     * 
     * @param routeId ID маршрута
     * @return Flow с временем в минутах (глобальное, если не задано индивидуальное)
     */
    fun getLeadTimeForRoute(routeId: String): Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[routeLeadTimeKey(routeId)] ?: preferences[GLOBAL_LEAD_TIME_KEY] 
            ?: Constants.DEFAULT_NOTIFICATION_LEAD_TIME
    }
    
    /**
     * Устанавливает индивидуальное время уведомления для маршрута
     * 
     * После сохранения автоматически обновляет все активные уведомления для этого маршрута
     * 
     * @param routeId ID маршрута
     * @param minutes время в минутах (null = использовать глобальное)
     */
    suspend fun setLeadTimeForRoute(routeId: String, minutes: Int?) {
        Timber.d("═══════════════════════════════════════════════════")
        Timber.d("💾 SAVING LEAD TIME")
        Timber.d("   Route: $routeId")
        Timber.d("   Minutes: $minutes")
        
        val key = routeLeadTimeKey(routeId)
        Timber.d("   Key: ${key.name}")
        
        context.dataStore.edit { preferences ->
            if (minutes == null) {
                // Удаляем индивидуальную настройку, используем глобальную
                preferences.remove(key)
                Timber.d("   ✓ Removed custom lead time (will use global)")
            } else {
                preferences[key] = minutes
                Timber.d("   ✓ Written to DataStore: ${key.name} = $minutes")
            }
        }
        
        // Проверка
        val verification = context.dataStore.data.firstOrNull()
        val savedValue = verification?.get(key)
        Timber.d("   VERIFICATION: Read back = $savedValue")
        
        if (minutes != null && savedValue == minutes) {
            Timber.d("   ✅ SUCCESS: Lead time verified in DataStore!")
        } else if (minutes == null && savedValue == null) {
            Timber.d("   ✅ SUCCESS: Custom lead time removed!")
        } else {
            Timber.e("   ❌ ERROR: Data mismatch! Expected: $minutes, Got: $savedValue")
        }
        
        // Обновляем кэш настроек
        NotificationPreferencesCache.updateCache(context)
        Timber.d("   ✓ Updated NotificationPreferencesCache")
        
        // Временно отключено для предотвращения крашей
        // updateAllAlarmsAfterTimeChange()
        
        Timber.d("═══════════════════════════════════════════════════")
    }
    
    /**
     * Проверяет, есть ли индивидуальная настройка для маршрута
     */
    fun hasCustomLeadTime(routeId: String): Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences.contains(routeLeadTimeKey(routeId))
    }
    
    /**
     * Удаляет индивидуальную настройку для маршрута
     */
    suspend fun removeCustomLeadTime(routeId: String) {
        setLeadTimeForRoute(routeId, null)
    }
    
    /**
     * Обновляет все активные уведомления после изменения времени уведомления
     * 
     * Загружает все избранные времена и перепланирует их уведомления
     * с учетом новых настроек leadTime.
     */
    private suspend fun updateAllAlarmsAfterTimeChange() {
        try {
            Timber.d("📢 Updating all alarms after lead time change...")
            
            val database = AppDatabase.getDatabase(context)
            val favoriteTimeDao = database.favoriteTimeDao()
            val repository = BusRouteRepository(context)
            
            val favoriteTimeEntities = favoriteTimeDao.getAllFavoriteTimes().firstOrNull() ?: emptyList()
            
            val activeFavoriteTimes = favoriteTimeEntities
                .filter { entity -> entity.isActive }
                .map { entity -> entity.toFavoriteTime(repository) }
            
            AlarmScheduler.updateAllAlarmsBasedOnSettings(context, activeFavoriteTimes)
            Timber.d("✅ Updated ${activeFavoriteTimes.size} active alarms with new lead time")
            
        } catch (e: Exception) {
            Timber.e(e, "❌ Error updating alarms after lead time change")
        }
    }
}