package com.example.lets_go_slavgorod.data.remote

import kotlinx.coroutines.delay
import timber.log.Timber
import kotlin.math.pow
import kotlin.random.Random

/**
 * Политика повторных попыток с экспоненциальной задержкой
 * 
 * Реализует стратегию exponential backoff с jitter для повышения
 * надежности загрузки данных с GitHub.
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 2.1
 */
object RetryPolicy {
    
    private const val MAX_RETRIES = 3
    private const val BASE_DELAY_MS = 1000L
    private const val MAX_DELAY_MS = 8000L
    private const val JITTER_MS = 500L
    
    /**
     * Выполняет операцию с повторными попытками
     * 
     * @param operation операция для выполнения
     * @param isRetryable функция для определения, стоит ли повторять попытку
     * @return результат операции или null при исчерпании попыток
     */
    suspend fun <T> executeWithRetry(
        operation: suspend (attempt: Int) -> T?,
        isRetryable: (Exception) -> Boolean = ::isRetryableException
    ): T? {
        var lastException: Exception? = null
        
        for (attempt in 0 until MAX_RETRIES) {
            try {
                if (attempt > 0) {
                }
                
                val result = operation(attempt)
                if (result != null) {
                    if (attempt > 0) {
                    }
                    return result
                }
                
                // Если результат null, но нет исключения, считаем это временной проблемой
                if (attempt < MAX_RETRIES - 1) {
                    val delayMs = calculateDelay(attempt)
                    delay(delayMs)
                }
                
            } catch (e: Exception) {
                lastException = e
                
                if (!isRetryable(e)) {
                    return null
                }
                
                if (attempt < MAX_RETRIES - 1) {
                    val delayMs = calculateDelay(attempt)
                    delay(delayMs)
                }
            }
        }
        
        Timber.e("Все попытки повтора исчерпаны. Последняя ошибка: ${lastException?.message}")
        return null
    }
    
    /**
     * Вычисляет задержку для следующей попытки с экспоненциальным backoff
     */
    private fun calculateDelay(attempt: Int): Long {
        // Экспоненциальная задержка: baseDelay * 2^attempt
        val exponentialDelay = BASE_DELAY_MS * (2.0.pow(attempt.toDouble())).toLong()
        
        // Ограничиваем максимальной задержкой
        val cappedDelay = minOf(exponentialDelay, MAX_DELAY_MS)
        
        // Добавляем jitter для предотвращения thundering herd
        val jitter = Random.nextLong(0, JITTER_MS)
        
        return cappedDelay + jitter
    }
    
    /**
     * Проверяет, стоит ли повторять попытку для данного исключения
     */
    private fun isRetryableException(exception: Exception): Boolean {
        return when (exception) {
            is java.net.SocketTimeoutException -> true
            is java.net.UnknownHostException -> true
            is java.net.ConnectException -> true
            is java.io.IOException -> true
            is java.net.ProtocolException -> false  // Ошибка протокола - не повторяем
            is java.net.MalformedURLException -> false  // Неправильный URL - не повторяем
            else -> false
        }
    }
}