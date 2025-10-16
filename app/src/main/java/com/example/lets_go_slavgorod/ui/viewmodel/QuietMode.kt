package com.example.lets_go_slavgorod.ui.viewmodel

/**
 * Режимы уведомлений
 */
enum class QuietMode(val displayName: String) {
    ENABLED("Включены"),
    DISABLED("Выключены"),
    CUSTOM_DAYS("Отключить на N дней")
}
