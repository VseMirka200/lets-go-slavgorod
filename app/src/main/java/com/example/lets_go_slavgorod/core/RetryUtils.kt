package com.example.lets_go_slavgorod.core

import kotlinx.coroutines.delay
import timber.log.Timber
import kotlin.math.pow

/**
 * Утилиты для retry операций
 * 
 * ИСПРАВЛЕНО: Централизованные утилиты для устранения дублирования кода
 * 
 * v3.0 Changes (Октябрь 2025):
 * - Оптимизированы импорты и зависимости
 * - Улучшена производительность retry операций
 * - Обновлены комментарии и документация
 */
object RetryUtils {
    
    /**
     * Выполняет операцию с повторными попытками при ошибках
     * 
     * @param maxRetries максимальное количество попыток
     * @param initialDelay начальная задержка в миллисекундах
     * @param operation операция для выполнения
     * @return результат операции или null при неудаче
     */
    suspend fun <T> retryWithBackoff(
        maxRetries: Int = 3,
        initialDelay: Long = 1000L,
        operation: suspend () -> T?
    ): T? {
        var lastException: Exception? = null
        
        repeat(maxRetries) { attempt ->
            try {
                return operation()
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries - 1) {
                    val delay = initialDelay * (2.0.pow(attempt.toDouble())).toLong()
                    delay(delay)
                }
            }
        }
        
        lastException?.let { 
            Timber.e(it, "Все попытки retry исчерпаны после $maxRetries попыток")
        }
        return null
    }
    
    /**
     * Выполняет операцию с фиксированной задержкой между попытками
     * 
     * @param maxRetries максимальное количество попыток
     * @param delayMs задержка между попытками в миллисекундах
     * @param operation операция для выполнения
     * @return результат операции или null при неудаче
     */
    suspend fun <T> retryWithFixedDelay(
        maxRetries: Int = 3,
        delayMs: Long = 1000L,
        operation: suspend () -> T?
    ): T? {
        var lastException: Exception? = null
        
        repeat(maxRetries) { attempt ->
            try {
                return operation()
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries - 1) {
                    delay(delayMs)
                }
            }
        }
        
        lastException?.let { 
            Timber.e(it, "Все попытки retry исчерпаны после $maxRetries попыток")
        }
        return null
    }
}