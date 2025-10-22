package com.example.lets_go_slavgorod.core

import kotlinx.coroutines.delay
import timber.log.Timber

/**
 * Утилиты для повторных попыток выполнения операций
 * 
 * Предоставляет функции для retry логики с exponential backoff
 * для сетевых запросов и других операций, которые могут временно не удаться.
 * 
 * Особенности:
 * - Exponential backoff для избежания перегрузки сервера
 * - Максимальное количество попыток
 * - Настраиваемые задержки
 * - Логирование попыток
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 2.0
 */
object RetryUtils {
    
    /**
     * Выполняет операцию с повторными попытками
     * 
     * @param T тип возвращаемого значения
     * @param times количество попыток (по умолчанию 3)
     * @param initialDelay начальная задержка в миллисекундах (по умолчанию 1000)
     * @param maxDelay максимальная задержка в миллисекундах (по умолчанию 10000)
     * @param factor множитель для exponential backoff (по умолчанию 2.0)
     * @param operation операция для выполнения
     * @return результат операции
     * @throws Exception если все попытки исчерпаны
     */
    suspend fun <T> retry(
        times: Int = 3,
        initialDelay: Long = 1000L,
        maxDelay: Long = 10000L,
        factor: Double = 2.0,
        operation: suspend () -> T
    ): T {
        var currentDelay = initialDelay
        var lastException: Exception? = null
        
        repeat(times) { attempt ->
            try {
                Timber.d("🔄 Retry attempt ${attempt + 1}/$times")
                return operation()
            } catch (e: Exception) {
                lastException = e
                Timber.w(e, "❌ Attempt ${attempt + 1} failed: ${e.message}")
                
                // Если это последняя попытка, не ждем
                if (attempt == times - 1) {
                    return@repeat
                }
                
                // Ждем перед следующей попыткой
                Timber.d("⏳ Waiting ${currentDelay}ms before next attempt...")
                delay(currentDelay)
                
                // Увеличиваем задержку для следующей попытки
                currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
            }
        }
        
        // Если все попытки исчерпаны, выбрасываем последнее исключение
        Timber.e(lastException, "💥 All $times attempts failed")
        throw lastException ?: Exception("All retry attempts failed")
    }
    
    /**
     * Выполняет операцию с повторными попытками и возвращает Result
     * 
     * @param T тип возвращаемого значения
     * @param times количество попыток
     * @param initialDelay начальная задержка
     * @param maxDelay максимальная задержка
     * @param factor множитель для exponential backoff
     * @param operation операция для выполнения
     * @return Result.Success с данными или Result.Error с исключением
     */
    suspend fun <T> retryWithResult(
        times: Int = 3,
        initialDelay: Long = 1000L,
        maxDelay: Long = 10000L,
        factor: Double = 2.0,
        operation: suspend () -> T
    ): Result<T> {
        return try {
            val result = retry(times, initialDelay, maxDelay, factor, operation)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Выполняет операцию с повторными попытками для сетевых запросов
     * 
     * Специализированная версия для сетевых операций с оптимизированными параметрами.
     * 
     * @param T тип возвращаемого значения
     * @param operation сетевая операция
     * @return результат операции
     */
    suspend fun <T> retryNetwork(
        operation: suspend () -> T
    ): T {
        return retry(
            times = 3,
            initialDelay = 1000L,
            maxDelay = 8000L,
            factor = 2.0,
            operation = operation
        )
    }
    
    /**
     * Выполняет операцию с повторными попытками для операций с базой данных
     * 
     * Специализированная версия для операций с БД с быстрыми повторными попытками.
     * 
     * @param T тип возвращаемого значения
     * @param operation операция с БД
     * @return результат операции
     */
    suspend fun <T> retryDatabase(
        operation: suspend () -> T
    ): T {
        return retry(
            times = 2,
            initialDelay = 100L,
            maxDelay = 1000L,
            factor = 3.0,
            operation = operation
        )
    }
}