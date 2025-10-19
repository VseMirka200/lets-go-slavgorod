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

