package com.example.lets_go_slavgorod.data.model

/**
 * Sealed class для безопасной обработки результатов операций
 * 
 * Обеспечивает type-safe обработку различных состояний асинхронных операций:
 * - Success: успешное выполнение с данными
 * - Error: ошибка с информацией об исключении
 * - Loading: процесс выполнения операции
 * 
 * Использование:
 * ```kotlin
 * when (result) {
 *     is Result.Success -> handleData(result.data)
 *     is Result.Error -> handleError(result.exception)
 *     is Result.Loading -> showLoader()
 * }
 * ```
 * 
 * @author VseMirka200
 * @version 1.0
 */
sealed class Result<out T> {
    /**
     * Успешный результат операции
     * 
     * @param data данные результата
     */
    data class Success<T>(val data: T) : Result<T>()
    
    /**
     * Ошибка при выполнении операции
     * 
     * @param exception исключение, которое привело к ошибке
     * @param message пользовательское сообщение об ошибке (опционально)
     */
    data class Error(
        val exception: Throwable,
        val message: String? = exception.message
    ) : Result<Nothing>()
    
    /**
     * Процесс выполнения операции
     * 
     * @param progress прогресс выполнения от 0 до 1 (опционально)
     */
    data class Loading(val progress: Float? = null) : Result<Nothing>()
    
    /**
     * Проверка на успешность
     */
    val isSuccess: Boolean
        get() = this is Success
    
    /**
     * Проверка на ошибку
     */
    val isError: Boolean
        get() = this is Error
    
    /**
     * Проверка на загрузку
     */
    val isLoading: Boolean
        get() = this is Loading
    
    /**
     * Получение данных или null
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }
    
    /**
     * Получение данных или значение по умолчанию
     */
    fun getOrDefault(defaultValue: @UnsafeVariance T): @UnsafeVariance T = when (this) {
        is Success -> data
        else -> defaultValue
    }
    
    /**
     * Получение данных или выброс исключения
     */
    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw exception
        is Loading -> throw IllegalStateException("Cannot get data while loading")
    }
    
    /**
     * Выполнение действия при успехе
     */
    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }
    
    /**
     * Выполнение действия при ошибке
     */
    inline fun onError(action: (Throwable) -> Unit): Result<T> {
        if (this is Error) action(exception)
        return this
    }
    
    /**
     * Выполнение действия при загрузке
     */
    inline fun onLoading(action: (Float?) -> Unit): Result<T> {
        if (this is Loading) action(progress)
        return this
    }
    
    /**
     * Трансформация данных
     */
    inline fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> Error(exception, message)
        is Loading -> Loading(progress)
    }
    
    /**
     * Трансформация с возможной ошибкой
     */
    inline fun <R> flatMap(transform: (T) -> Result<R>): Result<R> = when (this) {
        is Success -> try {
            transform(data)
        } catch (e: Exception) {
            Error(e)
        }
        is Error -> Error(exception, message)
        is Loading -> Loading(progress)
    }
}

/**
 * Вспомогательные функции для создания Result
 */

/**
 * Создает успешный Result
 */
fun <T> successOf(data: T): Result<T> = Result.Success(data)

/**
 * Создает Result с ошибкой
 */
fun errorOf(exception: Throwable, message: String? = null): Result<Nothing> =
    Result.Error(exception, message)

/**
 * Создает Result загрузки
 */
fun loadingOf(progress: Float? = null): Result<Nothing> = Result.Loading(progress)

/**
 * Выполняет блок кода и оборачивает результат в Result
 */
inline fun <T> resultOf(block: () -> T): Result<T> = try {
    Result.Success(block())
} catch (e: Exception) {
    Result.Error(e)
}

/**
 * Выполняет suspend блок кода и оборачивает результат в Result
 */
suspend inline fun <T> suspendResultOf(crossinline block: suspend () -> T): Result<T> = try {
    Result.Success(block())
} catch (e: Exception) {
    Result.Error(e)
}

