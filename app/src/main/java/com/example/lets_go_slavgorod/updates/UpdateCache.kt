package com.example.lets_go_slavgorod.updates

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.lets_go_slavgorod.BuildConfig
import com.example.lets_go_slavgorod.updates.UpdateManager.UpdateInfo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Extension для DataStore обновлений
 */
private val Context.updateCacheDataStore: DataStore<Preferences> by preferencesDataStore(name = "update_cache")

/**
 * Кэш для информации об обновлениях приложения
 * 
 * Оптимизирует проверку обновлений путем кэширования результатов
 * и уменьшения количества запросов к GitHub API.
 * 
 * Особенности:
 * - Кэширование последнего результата проверки
 * - Временные метки для определения актуальности кэша
 * - Автоматическая инвалидация устаревших данных
 * - Защита от rate limiting GitHub API (60 запросов/час)
 * 
 * @param context контекст приложения
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 2.0
 */
class UpdateCache(private val context: Context) {
    
    private val dataStore = context.updateCacheDataStore
    
    companion object {
        // Ключи для DataStore
        private val KEY_VERSION_NAME = stringPreferencesKey("cached_version_name")
        private val KEY_DOWNLOAD_URL = stringPreferencesKey("cached_download_url")
        private val KEY_RELEASE_NOTES = stringPreferencesKey("cached_release_notes")
        private val KEY_CHECK_TIME = longPreferencesKey("last_check_time")
        private val KEY_LAST_SHOWN_VERSION = stringPreferencesKey("last_shown_version")
        
        // Минимальный интервал между проверками (из BuildConfig)
        private val MIN_CHECK_INTERVAL_MS = TimeUnit.HOURS.toMillis(BuildConfig.UPDATE_CHECK_INTERVAL_HOURS)
        
        // Время жизни кэша (из BuildConfig)
        private val CACHE_TTL_MS = TimeUnit.HOURS.toMillis(BuildConfig.UPDATE_CACHE_TTL_HOURS)
    }
    
    /**
     * Сохраняет информацию об обновлении в кэш
     * 
     * @param updateInfo информация об обновлении
     */
    suspend fun cacheUpdateInfo(updateInfo: UpdateInfo) {
        try {
            dataStore.edit { preferences ->
                preferences[KEY_VERSION_NAME] = updateInfo.versionName
                preferences[KEY_DOWNLOAD_URL] = updateInfo.downloadUrl
                preferences[KEY_RELEASE_NOTES] = updateInfo.releaseNotes
                preferences[KEY_CHECK_TIME] = System.currentTimeMillis()
            }
            Timber.d("Cached update info: ${updateInfo.versionName}")
        } catch (e: Exception) {
            Timber.e(e, "Failed to cache update info")
        }
    }
    
    /**
     * Получает кэшированную информацию об обновлении
     * 
     * @return UpdateInfo если кэш валиден, null если устарел или отсутствует
     */
    suspend fun getCachedUpdateInfo(): UpdateInfo? {
        return try {
            dataStore.data.map { preferences ->
                val versionName = preferences[KEY_VERSION_NAME]
                val downloadUrl = preferences[KEY_DOWNLOAD_URL]
                val releaseNotes = preferences[KEY_RELEASE_NOTES]
                val checkTime = preferences[KEY_CHECK_TIME] ?: 0L
                
                // Проверяем валидность кэша
                val cacheAge = System.currentTimeMillis() - checkTime
                if (cacheAge > CACHE_TTL_MS) {
                    Timber.d("Update cache expired (age: ${cacheAge / 1000 / 60}min)")
                    return@map null
                }
                
                // Проверяем наличие всех необходимых данных
                if (versionName != null && downloadUrl != null && releaseNotes != null) {
                    Timber.d("Returning cached update info: $versionName")
                    UpdateInfo(versionName, downloadUrl, releaseNotes)
                } else {
                    null
                }
            }.first()
        } catch (e: Exception) {
            Timber.e(e, "Failed to get cached update info")
            null
        }
    }
    
    /**
     * Проверяет, нужно ли выполнять проверку обновлений
     * 
     * Возвращает false если с последней проверки прошло меньше MIN_CHECK_INTERVAL_MS
     * 
     * @return true если нужно проверять, false если можно использовать кэш
     */
    suspend fun shouldCheckUpdate(): Boolean {
        return try {
            val lastCheckTime = dataStore.data.map { preferences ->
                preferences[KEY_CHECK_TIME] ?: 0L
            }.first()
            
            val timeSinceLastCheck = System.currentTimeMillis() - lastCheckTime
            val shouldCheck = timeSinceLastCheck > MIN_CHECK_INTERVAL_MS
            
            Timber.d("Should check update: $shouldCheck (last check: ${timeSinceLastCheck / 1000 / 60}min ago)")
            
            shouldCheck
        } catch (e: Exception) {
            Timber.e(e, "Failed to check if should update")
            true // При ошибке разрешаем проверку
        }
    }
    
    /**
     * Обновляет время последней проверки
     */
    suspend fun updateLastCheckTime() {
        try {
            dataStore.edit { preferences ->
                preferences[KEY_CHECK_TIME] = System.currentTimeMillis()
            }
            Timber.d("Updated last check time")
        } catch (e: Exception) {
            Timber.e(e, "Failed to update last check time")
        }
    }
    
    /**
     * Сохраняет версию, которую показали пользователю
     * 
     * Используется чтобы не показывать один и тот же диалог обновления многократно
     * 
     * @param version версия обновления
     */
    suspend fun markVersionAsShown(version: String) {
        try {
            dataStore.edit { preferences ->
                preferences[KEY_LAST_SHOWN_VERSION] = version
            }
            Timber.d("Marked version as shown: $version")
        } catch (e: Exception) {
            Timber.e(e, "Failed to mark version as shown")
        }
    }
    
    /**
     * Проверяет, показывали ли уже эту версию пользователю
     * 
     * @param version версия для проверки
     * @return true если уже показывали, false иначе
     */
    suspend fun wasVersionShown(version: String): Boolean {
        return try {
            val lastShownVersion = dataStore.data.map { preferences ->
                preferences[KEY_LAST_SHOWN_VERSION]
            }.first()
            
            val wasShown = lastShownVersion == version
            Timber.d("Was version $version shown: $wasShown")
            
            wasShown
        } catch (e: Exception) {
            Timber.e(e, "Failed to check if version was shown")
            false
        }
    }
    
    /**
     * Очищает кэш обновлений
     */
    suspend fun clearCache() {
        try {
            dataStore.edit { preferences ->
                preferences.remove(KEY_VERSION_NAME)
                preferences.remove(KEY_DOWNLOAD_URL)
                preferences.remove(KEY_RELEASE_NOTES)
                preferences.remove(KEY_CHECK_TIME)
                // Не удаляем KEY_LAST_SHOWN_VERSION, чтобы не показывать диалог снова
            }
            Timber.d("Cleared update cache")
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear update cache")
        }
    }
}

