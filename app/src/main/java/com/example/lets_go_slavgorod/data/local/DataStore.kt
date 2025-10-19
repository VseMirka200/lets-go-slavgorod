package com.example.lets_go_slavgorod.data.local

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

/**
 * Глобальный DataStore для хранения настроек приложения
 * 
 * Версия: 2.0
 * Последнее обновление: Октябрь 2025
 * 
 * Предоставляет единый экземпляр DataStore для всего приложения через
 * extension свойство Context. Используется для хранения пользовательских настроек
 * таких как тема, режим отображения, настройки уведомлений и т.д.
 * 
 * Используется в:
 * - ThemeViewModel (настройки темы)
 * - DisplaySettingsViewModel (настройки отображения)
 * - NotificationSettingsViewModel (настройки уведомлений)
 * - QuietModeViewModel (режим "Не беспокоить")
 * - UpdateSettingsViewModel (настройки обновлений)
 * - VibrationSettingsViewModel (настройки вибрации)
 * 
 * Хранимые данные:
 * - Режим темы (System/Light/Dark)
 * - Режим отображения маршрутов (Grid/List)
 * - Количество колонок в сетке
 * - Настройки уведомлений для каждого маршрута
 * - Режим "Не беспокоить"
 * - Настройки автообновлений
 * - Настройки вибрации
 * 
 * @author VseMirka200
 * @version 2.0
 * @since 1.0
 */
val Context.dataStore by preferencesDataStore(name = "settings")