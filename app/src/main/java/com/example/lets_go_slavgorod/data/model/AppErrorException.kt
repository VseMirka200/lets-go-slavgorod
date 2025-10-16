package com.example.lets_go_slavgorod.data.model

/**
 * Exception wrapper для AppError
 * 
 * Оборачивает AppError в Exception для совместимости с Result<T>.
 * Позволяет использовать type-safe AppError с существующим Result классом.
 * 
 * @param appError ошибка приложения
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 2.0
 */
class AppErrorException(
    val appError: AppError
) : Exception(appError.getUserMessage())

/**
 * Extension функция для создания Result.Error из AppError
 * Generic функция для совместимости с любым типом Result<T>
 */
fun <T> AppError.toResultError(): Result<T> {
    return Result.Error(AppErrorException(this))
}

/**
 * Extension функция для извлечения AppError из Result.Error
 */
fun Result.Error.getAppError(): AppError? {
    return (exception as? AppErrorException)?.appError
}

