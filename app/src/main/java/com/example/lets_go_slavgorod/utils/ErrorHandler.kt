package com.example.lets_go_slavgorod.utils

import android.database.SQLException
import android.database.sqlite.SQLiteException
import com.example.lets_go_slavgorod.data.model.AppError
import com.example.lets_go_slavgorod.data.model.getUserMessage
import timber.log.Timber
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

// Type alias для избежания конфликта с kotlin.Result
typealias AppResult<T> = com.example.lets_go_slavgorod.data.model.Result<T>

/**
 * Центральный обработчик ошибок приложения
 * 
 * Преобразует системные исключения в типобезопасные AppError.
 * Обеспечивает единообразную обработку ошибок во всем приложении.
 * 
 * Основные функции:
 * - Маппинг Exception -> AppError
 * - Логирование ошибок
 * - Извлечение понятных сообщений для пользователя
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 2.0
 */
object ErrorHandler {
    
    /**
     * Преобразует Exception в AppError
     * 
     * Автоматически определяет тип ошибки и создает соответствующий AppError.
     * Логирует ошибку для отладки.
     * 
     * @param exception исходное исключение
     * @param context дополнительный контекст для логирования
     * @return соответствующий AppError
     */
    fun handle(exception: Throwable, context: String = ""): AppError {
        // Логируем ошибку
        if (context.isNotEmpty()) {
            Timber.e(exception, "Error in $context")
        } else {
            Timber.e(exception, "Error occurred")
        }
        
        // Маппинг исключения в AppError
        return when (exception) {
            // Network errors
            is UnknownHostException -> {
                Timber.w("No internet connection")
                AppError.Network.NoConnection
            }
            is SocketTimeoutException -> {
                Timber.w("Request timeout")
                AppError.Network.Timeout
            }
            is IOException -> {
                Timber.e(exception, "Network IO error")
                AppError.Network.Generic(
                    message = exception.message ?: "Ошибка сети",
                    cause = exception
                )
            }
            
            // Database errors
            is SQLiteException, is SQLException -> {
                Timber.e(exception, "Database error")
                AppError.Database.Generic(
                    message = exception.message ?: "Ошибка базы данных",
                    cause = exception
                )
            }
            
            // Permission errors
            is SecurityException -> {
                Timber.e(exception, "Security/Permission error")
                AppError.Permission.Denied(
                    permission = exception.message ?: "Неизвестное разрешение"
                )
            }
            
            // Memory errors
            is OutOfMemoryError -> {
                Timber.e(exception, "Out of memory")
                AppError.System.OutOfMemory
            }
            
            // Generic/Unknown
            else -> {
                Timber.e(exception, "Unknown error")
                AppError.Unknown(
                    message = exception.message ?: "Неизвестная ошибка",
                    cause = exception
                )
            }
        }
    }
    
    /**
     * Обрабатывает ошибку и возвращает сообщение для пользователя
     * 
     * @param exception исходное исключение
     * @param context дополнительный контекст
     * @return сообщение для отображения пользователю
     */
    fun getUserMessage(exception: Throwable, context: String = ""): String {
        val error = handle(exception, context)
        return error.getUserMessage()
    }
    
    /**
     * Безопасное выполнение блока кода с обработкой ошибок
     * 
     * @param T тип возвращаемого значения
     * @param context контекст для логирования
     * @param block блок кода для выполнения
     * @return Result.Success с данными или Result.Error с Exception
     */
    inline fun <T> runCatching(
        context: String = "",
        block: () -> T
    ): AppResult<T> {
        return try {
            com.example.lets_go_slavgorod.data.model.Result.Success(block())
        } catch (e: Throwable) {
            handle(e, context)
            com.example.lets_go_slavgorod.data.model.Result.Error(e)
        }
    }
    
    /**
     * Безопасное выполнение suspend блока кода
     * 
     * @param T тип возвращаемого значения
     * @param context контекст для логирования
     * @param block suspend блок кода
     * @return Result.Success с данными или Result.Error с Exception
     */
    suspend inline fun <T> runCatchingSuspend(
        context: String = "",
        crossinline block: suspend () -> T
    ): AppResult<T> {
        return try {
            com.example.lets_go_slavgorod.data.model.Result.Success(block())
        } catch (e: Throwable) {
            handle(e, context)
            com.example.lets_go_slavgorod.data.model.Result.Error(e)
        }
    }
}

/**
 * Extension для Result для извлечения пользовательского сообщения
 */
fun <T> AppResult<T>.getUserMessage(): String {
    return when (this) {
        is com.example.lets_go_slavgorod.data.model.Result.Success -> "Успешно"
        is com.example.lets_go_slavgorod.data.model.Result.Error -> ErrorHandler.getUserMessage(this.exception, "")
        is com.example.lets_go_slavgorod.data.model.Result.Loading -> "Загрузка..."
    }
}

/**
 * Extension для Result для извлечения данных или null
 */
fun <T> AppResult<T>.getDataOrNull(): T? {
    return when (this) {
        is com.example.lets_go_slavgorod.data.model.Result.Success -> this.data
        else -> null
    }
}

/**
 * Extension для Result для извлечения ошибки как AppError или null
 */
fun <T> AppResult<T>.getAppErrorOrNull(): AppError? {
    return when (this) {
        is com.example.lets_go_slavgorod.data.model.Result.Error -> ErrorHandler.handle(this.exception, "")
        else -> null
    }
}

