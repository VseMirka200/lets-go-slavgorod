package com.example.lets_go_slavgorod.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber

/**
 * DataStore для настроек disclaimer
 */
private val Context.disclaimerDataStore: DataStore<Preferences> by preferencesDataStore(name = "disclaimer_preferences")

/**
 * Менеджер для управления показом диалога с предупреждением
 * 
 * Основные функции:
 * - Проверяет, нужно ли показать диалог новому пользователю
 * - Запоминает выбор пользователя
 * - Управляет состоянием показа диалога
 * 
 * Использует DataStore для безопасного хранения данных
 * 
 * @author VseMirka200
 * @version 2.0
 * @since 1.0
 */
object DisclaimerManager {
    
    private object Keys {
        val DISCLAIMER_SHOWN = booleanPreferencesKey("disclaimer_shown")
        val DISCLAIMER_DONT_SHOW = booleanPreferencesKey("disclaimer_dont_show")
    }
    
    /**
     * Проверяет, нужно ли показать диалог с предупреждением
     * 
     * @param context контекст приложения
     * @return true если нужно показать диалог, false если нет
     */
    suspend fun shouldShowDisclaimer(context: Context): Boolean {
        return context.disclaimerDataStore.data.map { preferences ->
            val disclaimerShown = preferences[Keys.DISCLAIMER_SHOWN] ?: false
            val dontShowAgain = preferences[Keys.DISCLAIMER_DONT_SHOW] ?: false
            
            Timber.d("Disclaimer check: shown=$disclaimerShown, dontShow=$dontShowAgain")
            
            // Показываем диалог если:
            // 1. Пользователь еще не видел его ИЛИ
            // 2. Пользователь не выбрал "Не показывать снова"
            !disclaimerShown || !dontShowAgain
        }.first()
    }
    
    /**
     * Flow для реактивного наблюдения за статусом disclaimer
     */
    fun observeShouldShowDisclaimer(context: Context): Flow<Boolean> {
        return context.disclaimerDataStore.data.map { preferences ->
            val disclaimerShown = preferences[Keys.DISCLAIMER_SHOWN] ?: false
            val dontShowAgain = preferences[Keys.DISCLAIMER_DONT_SHOW] ?: false
            !disclaimerShown || !dontShowAgain
        }
    }
    
    /**
     * Отмечает, что пользователь принял условия
     * 
     * @param context контекст приложения
     */
    suspend fun markDisclaimerAccepted(context: Context) {
        context.disclaimerDataStore.edit { preferences ->
            preferences[Keys.DISCLAIMER_SHOWN] = true
        }
        Timber.d("Disclaimer accepted by user")
    }
    
    /**
     * Отмечает, что пользователь выбрал "Не показывать снова"
     * 
     * @param context контекст приложения
     */
    suspend fun markDisclaimerDontShowAgain(context: Context) {
        context.disclaimerDataStore.edit { preferences ->
            preferences[Keys.DISCLAIMER_SHOWN] = true
            preferences[Keys.DISCLAIMER_DONT_SHOW] = true
        }
        Timber.d("Disclaimer marked as 'don't show again'")
    }
    
    /**
     * Сбрасывает настройки диалога (для тестирования)
     * 
     * @param context контекст приложения
     */
    suspend fun resetDisclaimerSettings(context: Context) {
        context.disclaimerDataStore.edit { preferences ->
            preferences.clear()
        }
        Timber.d("Disclaimer settings reset")
    }
}

