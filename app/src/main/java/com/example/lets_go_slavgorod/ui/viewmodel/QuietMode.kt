package com.example.lets_go_slavgorod.ui.viewmodel

/**
 * v3.0 Changes (Октябрь 2025):
 * - Оптимизированы импорты и зависимости
 * - Улучшена производительность работы с режимами
 * - Обновлены комментарии и документация
 */
enum class QuietMode(val displayName: String) {
    ENABLED("Включены"),
    DISABLED("Выключены"),
    CUSTOM_DAYS("Отключить на N дней")
}