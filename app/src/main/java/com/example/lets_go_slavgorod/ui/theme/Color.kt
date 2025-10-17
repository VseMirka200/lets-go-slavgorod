package com.example.lets_go_slavgorod.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Цветовая палитра приложения "Поехали! Славгород"
 * 
 * Версия: 2.0
 * Последнее обновление: Октябрь 2025
 * 
 * Определяет все цвета, используемые в приложении для обеих тем (светлой и темной).
 * Цвета подобраны в соответствии с Material Design 3 и тематикой общественного транспорта.
 * 
 * Категории цветов:
 * 
 * **Основные цвета транспорта**:
 * - BusBlue (#1976D2) - основной синий, ассоциируется с автобусами
 * - BusBlueLight (#63A4FF) - светлый вариант для акцентов
 * - BusBlueDark (#004BA0) - темный вариант для контраста
 * 
 * **Функциональные цвета**:
 * - TransportGreen (#4CAF50) - успешные операции, подтверждения
 * - TransportOrange (#FF9800) - предупреждения, важная информация
 * - TransportRed (#F44336) - ошибки, критические уведомления
 * - TransportYellow (#FFC107) - заметки, специальные отметки
 * 
 * **Нейтральные цвета**:
 * - SurfaceLight/Dark - фоновые поверхности
 * - OnSurfaceLight/Dark - текст на поверхностях
 * 
 * **Акцентные цвета**:
 * - AccentBlue (#2196F3) - дополнительные акценты
 * - AccentTeal (#009688) - альтернативные акценты
 * 
 * Цвета используются в:
 * - lightColorScheme (светлая тема)
 * - darkColorScheme (темная тема)
 * - Индивидуальные цвета маршрутов (из данных)
 * 
 * @author VseMirka200
 * @version 2.0
 * @since 1.0
 */

// Основные цвета транспорта
val BusBlue = Color(0xFF1976D2)        // Основной синий автобусов
val BusBlueLight = Color(0xFF63A4FF)   // Светло-синий
val BusBlueDark = Color(0xFF004BA0)    // Темно-синий

// Дополнительные цвета
val TransportGreen = Color(0xFF4CAF50) // Зеленый для успешных действий
val TransportOrange = Color(0xFFFF9800) // Оранжевый для предупреждений
val TransportRed = Color(0xFFF44336)   // Красный для ошибок
val TransportYellow = Color(0xFFFFC107) // Желтый для важной информации

// Нейтральные цвета
val SurfaceLight = Color(0xFFF5F5F5)   // Светлая поверхность
val SurfaceDark = Color(0xFF1E1E1E)    // Темная поверхность
val OnSurfaceLight = Color(0xFF212121) // Текст на светлой поверхности
val OnSurfaceDark = Color(0xFFE0E0E0)  // Текст на темной поверхности

// Акцентные цвета
val AccentBlue = Color(0xFF2196F3)     // Акцентный синий
val AccentTeal = Color(0xFF009688)     // Акцентный бирюзовый

// Старые цвета (для совместимости)
val Purple80 = BusBlueLight
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = TransportOrange

val Purple40 = BusBlue
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = TransportRed