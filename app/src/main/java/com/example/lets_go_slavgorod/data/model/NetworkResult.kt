package com.example.lets_go_slavgorod.data.model

/**
 * Обертка для результатов сетевых операций
 * 
 * Централизованный способ обработки результатов API вызовов и сетевых операций.
 * Предоставляет типобезопасное представление различных состояний сети.
 * 
 * Типы результатов:
 * - Success<T>: Успешный результат с данными типа T
 * - Error: Ошибка с exception и опциональным сообщением
 * - Loading: Состояние загрузки (для отображения прогресса)
 * - NetworkUnavailable: Нет соединения с интернетом
 * 
 * Использование:
 * ```kotlin
 * suspend fun loadData(): NetworkResult<List<Item>> {
 *     return safeApiCall {
 *         api.fetchItems()
 *     }
 * }
 * 
 * // В UI:
 * when (result) {
 *     is NetworkResult.Success -> showData(result.data)
 *     is NetworkResult.Error -> showError(result.message)
 *     is NetworkResult.Loading -> showProgress()
 *     is NetworkResult.NetworkUnavailable -> showOfflineMessage()
 * }
 * ```
 * 
 * @param T тип данных при успешном результате
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 2.1
 */
sealed class NetworkResult<out T> {
    /**
     * Успешный результат с данными
     * 
     * @param data полученные данные типа T
     */
    data class Success<T>(val data: T) : NetworkResult<T>()
    
    /**
     * Ошибка при выполнении операции
     * 
     * @param exception исключение которое вызвало ошибку
     * @param message опциональное человекочитаемое сообщение об ошибке
     */
    data class Error(
        val exception: Exception, 
        val message: String? = null
    ) : NetworkResult<Nothing>()
    
    /**
     * Состояние загрузки
     * 
     * Используется для отображения прогресса загрузки в UI
     */
    data object Loading : NetworkResult<Nothing>()
    
    /**
     * Отсутствие сетевого подключения
     * 
     * Специальное состояние для offline режима, позволяет отличить
     * отсутствие сети от других ошибок
     */
    data object NetworkUnavailable : NetworkResult<Nothing>()
}

/**
 * Extension функция для маппинга успешного результата
 * 
 * Преобразует данные внутри Success в другой тип.
 * Другие состояния (Error, Loading, NetworkUnavailable) проходят без изменений.
 * 
 * @param transform функция преобразования данных
 * @return NetworkResult с преобразованными данными
 */
inline fun <T, R> NetworkResult<T>.map(transform: (T) -> R): NetworkResult<R> {
    return when (this) {
        is NetworkResult.Success -> NetworkResult.Success(transform(data))
        is NetworkResult.Error -> this
        is NetworkResult.Loading -> this
        is NetworkResult.NetworkUnavailable -> this
    }
}

/**
 * Extension функция для получения данных или null
 * 
 * @return данные если Success, null в противном случае
 */
fun <T> NetworkResult<T>.dataOrNull(): T? {
    return when (this) {
        is NetworkResult.Success -> data
        else -> null
    }
}

/**
 * Extension функция для проверки успешности
 * 
 * @return true если результат Success
 */
fun <T> NetworkResult<T>.isSuccess(): Boolean {
    return this is NetworkResult.Success
}

/**
 * Extension функция для проверки ошибки
 * 
 * @return true если результат Error
 */
fun <T> NetworkResult<T>.isError(): Boolean {
    return this is NetworkResult.Error
}

