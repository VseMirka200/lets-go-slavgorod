package com.example.lets_go_slavgorod.core

import com.example.lets_go_slavgorod.data.model.AppError
import timber.log.Timber

/**
 * Метрики ошибок для мониторинга и аналитики
 * 
 * Собирает статистику ошибок для улучшения стабильности приложения.
 * Предоставляет централизованную систему сбора метрик ошибок.
 * 
 * Особенности:
 * - Счетчики ошибок по типам
 * - Временные метрики (частота ошибок)
 * - Контекстная информация
 * - Автоматическая отправка в аналитику
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 2.0
 */
object ErrorMetrics {
    
    // Счетчики ошибок по типам
    private val errorCounts = mutableMapOf<String, Int>()
    private val errorTimestamps = mutableListOf<Long>()
    
    /**
     * Записывает ошибку в метрики
     * 
     * @param error тип ошибки
     * @param context контекст возникновения ошибки
     * @param additionalInfo дополнительная информация
     */
    fun recordError(error: AppError, context: String, additionalInfo: Map<String, Any> = emptyMap()) {
        val errorType = getErrorType(error)
        val timestamp = System.currentTimeMillis()
        
        // Увеличиваем счетчик ошибок
        errorCounts[errorType] = (errorCounts[errorType] ?: 0) + 1
        errorTimestamps.add(timestamp)
        
        // Логируем ошибку
        Timber.e("📊 Error recorded: $errorType in $context")
        
        // Отправляем в аналитику (если настроена)
        sendToAnalytics(errorType, context, additionalInfo)
        
        // Очищаем старые записи (оставляем только последние 1000)
        if (errorTimestamps.size > 1000) {
            errorTimestamps.removeAt(0)
        }
    }
    
    /**
     * Получает статистику ошибок
     * 
     * @return карта с количеством ошибок по типам
     */
    fun getErrorStats(): Map<String, Int> {
        return errorCounts.toMap()
    }
    
    /**
     * Получает частоту ошибок за последние N минут
     * 
     * @param minutes количество минут для анализа
     * @return количество ошибок за указанный период
     */
    fun getErrorFrequency(minutes: Int = 60): Int {
        val cutoffTime = System.currentTimeMillis() - (minutes * 60 * 1000)
        return errorTimestamps.count { it > cutoffTime }
    }
    
    /**
     * Сбрасывает метрики ошибок
     */
    fun resetMetrics() {
        errorCounts.clear()
        errorTimestamps.clear()
        Timber.d("📊 Error metrics reset")
    }
    
    /**
     * Определяет тип ошибки для категоризации
     */
    private fun getErrorType(error: AppError): String {
        return when (error) {
            is AppError.Network.NoConnection -> "network_no_connection"
            is AppError.Network.Timeout -> "network_timeout"
            is AppError.Network.HttpError -> "network_http_${error.code}"
            is AppError.Network.Generic -> "network_generic"
            is AppError.Database.NotFound -> "database_not_found"
            is AppError.Database.ReadError -> "database_read_error"
            is AppError.Database.WriteError -> "database_write_error"
            is AppError.Database.Generic -> "database_generic"
            is AppError.Permission.NotGranted -> "permission_not_granted"
            is AppError.Permission.Denied -> "permission_denied"
            is AppError.System.OutOfMemory -> "system_out_of_memory"
            is AppError.System.OutOfStorage -> "system_out_of_storage"
            is AppError.System.Generic -> "system_generic"
            is AppError.Validation.InvalidFormat -> "validation_invalid_format"
            is AppError.Validation.MissingField -> "validation_missing_field"
            is AppError.Validation.OutOfRange -> "validation_out_of_range"
            is AppError.Unknown -> "unknown"
        }
    }
    
    /**
     * Отправляет метрики в аналитику
     */
    private fun sendToAnalytics(errorType: String, context: String, additionalInfo: Map<String, Any>) {
        // Здесь можно интегрировать с Firebase Analytics, Crashlytics и т.д.
        Timber.d("📈 Analytics: Error $errorType in $context with info: $additionalInfo")
    }
}