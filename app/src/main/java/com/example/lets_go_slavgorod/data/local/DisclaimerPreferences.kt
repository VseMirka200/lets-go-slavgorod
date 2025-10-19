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
 * DataStore для настроек диалога с предупреждением (disclaimer)
 * 
 * Хранит состояние показа диалога с предупреждением о том, что приложение
 * содержит неофициальные данные и предоставляется "как есть".
 * 
 * Используется extension property паттерн для type-safe доступа к DataStore.
 * Singleton per Context через делегат preferencesDataStore.
 * 
 * @see DisclaimerManager для работы с этим DataStore
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
    
    /**
     * Ключи для хранения настроек в DataStore
     * 
     * DISCLAIMER_SHOWN - был ли показан диалог хотя бы один раз
     * DISCLAIMER_DONT_SHOW - выбрал ли пользователь "Не показывать снова"
     */
    private object Keys {
        /** Флаг показа диалога (true = диалог был показан хотя бы раз) */
        val DISCLAIMER_SHOWN = booleanPreferencesKey("disclaimer_shown")
        
        /** Флаг "Не показывать снова" (true = пользователь выбрал не показывать) */
        val DISCLAIMER_DONT_SHOW = booleanPreferencesKey("disclaimer_dont_show")
    }
    
    /**
     * Проверяет, нужно ли показать диалог с предупреждением
     * 
     * Логика показа диалога:
     * - Показывать при первом запуске приложения
     * - Не показывать если пользователь выбрал "Не показывать снова"
     * - Показывать снова если пользователь просто закрыл диалог (без кнопки "Не показывать")
     * 
     * Таблица истинности:
     * | shown | dontShow | Результат |
     * |-------|----------|-----------|
     * | false | false    | SHOW      | ← Первый запуск
     * | true  | false    | DON'T     | ← Пользователь принял
     * | true  | true     | DON'T     | ← "Не показывать снова"
     * | false | true     | DON'T     | ← Невозможная комбинация
     * 
     * @param context контекст приложения для доступа к DataStore
     * @return true если нужно показать диалог, false если нет
     */
    suspend fun shouldShowDisclaimer(context: Context): Boolean {
        return context.disclaimerDataStore.data.map { preferences ->
            val disclaimerShown = preferences[Keys.DISCLAIMER_SHOWN] ?: false
            val dontShowAgain = preferences[Keys.DISCLAIMER_DONT_SHOW] ?: false
            
            Timber.d("Disclaimer check: shown=$disclaimerShown, dontShow=$dontShowAgain")
            
            // Показываем диалог если:
            // 1. Пользователь еще не видел его (!disclaimerShown) ИЛИ
            // 2. Видел, но не выбрал "Не показывать снова" (shown=true, dontShow=false)
            // Логика упрощается до: !(disclaimerShown && dontShowAgain)
            !disclaimerShown || !dontShowAgain
        }.first()
    }
    
    /**
     * Отмечает, что пользователь принял условия disclaimer
     * 
     * Устанавливает флаг DISCLAIMER_SHOWN = true, но не устанавливает DISCLAIMER_DONT_SHOW.
     * Это означает, что диалог может быть показан снова в будущем (например, после
     * обновления условий использования).
     * 
     * Вызывается при нажатии кнопки "Принять" в диалоге.
     * 
     * @param context контекст приложения для доступа к DataStore
     */
    suspend fun markDisclaimerAccepted(context: Context) {
        context.disclaimerDataStore.edit { preferences ->
            preferences[Keys.DISCLAIMER_SHOWN] = true
            // Намеренно НЕ устанавливаем DISCLAIMER_DONT_SHOW
        }
        Timber.d("Disclaimer accepted by user")
    }
    
    /**
     * Отмечает, что пользователь выбрал "Не показывать снова"
     * 
     * Устанавливает оба флага:
     * - DISCLAIMER_SHOWN = true (диалог был показан)
     * - DISCLAIMER_DONT_SHOW = true (больше не показывать)
     * 
     * После этого диалог не будет показываться до переустановки приложения
     * или очистки данных приложения.
     * 
     * Вызывается при нажатии кнопки "Не показывать снова" в диалоге.
     * 
     * @param context контекст приложения для доступа к DataStore
     */
    suspend fun markDisclaimerDontShowAgain(context: Context) {
        context.disclaimerDataStore.edit { preferences ->
            preferences[Keys.DISCLAIMER_SHOWN] = true
            preferences[Keys.DISCLAIMER_DONT_SHOW] = true
        }
        Timber.d("Disclaimer marked as 'don't show again'")
    }
}

