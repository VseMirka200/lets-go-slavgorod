package com.example.lets_go_slavgorod.data.network

import android.content.Context
import com.example.lets_go_slavgorod.data.model.NetworkResult
import timber.log.Timber
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Утилиты для безопасной работы с сетевыми операциями
 * 
 * Предоставляет функции для выполнения API вызовов с автоматической
 * обработкой ошибок и проверкой сетевого подключения.
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 2.1
 */
object NetworkUtils {
    
    /**
     * Выполняет API вызов с автоматической обработкой ошибок
     * 
     * Оборачивает suspend функцию в try-catch и возвращает NetworkResult.
     * Автоматически проверяет наличие интернета перед вызовом.
     * 
     * Обрабатываемые типы ошибок:
     * - UnknownHostException → NetworkUnavailable (DNS не может разрешить хост)
     * - SocketTimeoutException → Error ("Timeout")
     * - IOException → Error ("Network I/O error")
     * - Exception → Error (общая ошибка)
     * 
     * Использование:
     * ```kotlin
     * val result = safeApiCall(context) {
     *     api.fetchData()
     * }
     * 
     * when (result) {
     *     is NetworkResult.Success -> println(result.data)
     *     is NetworkResult.Error -> println(result.message)
     *     is NetworkResult.NetworkUnavailable -> println("Offline")
     *     is NetworkResult.Loading -> { }
     * }
     * ```
     * 
     * @param context контекст для проверки сетевого подключения
     * @param apiCall suspend функция которая выполняет API вызов
     * @return NetworkResult<T> с результатом операции
     */
    suspend fun <T> safeApiCall(
        context: Context,
        apiCall: suspend () -> T
    ): NetworkResult<T> {
        return try {
            // Проверяем подключение к интернету перед вызовом
            if (!NetworkMonitor.isConnected(context)) {
                Timber.w("No internet connection available")
                return NetworkResult.NetworkUnavailable
            }
            
            // Выполняем API вызов
            val result = apiCall()
            NetworkResult.Success(result)
            
        } catch (e: UnknownHostException) {
            // DNS не может разрешить имя хоста - обычно означает отсутствие интернета
            Timber.e(e, "Unknown host - network unavailable")
            NetworkResult.NetworkUnavailable
            
        } catch (e: SocketTimeoutException) {
            // Таймаут соединения
            Timber.e(e, "Socket timeout")
            NetworkResult.Error(
                exception = e,
                message = "Превышено время ожидания ответа сервера"
            )
            
        } catch (e: IOException) {
            // Общие I/O ошибки сети
            Timber.e(e, "Network I/O error")
            NetworkResult.Error(
                exception = e,
                message = "Ошибка сети: ${e.localizedMessage ?: "Unknown I/O error"}"
            )
            
        } catch (e: Exception) {
            // Все остальные ошибки
            Timber.e(e, "API call failed")
            NetworkResult.Error(
                exception = e,
                message = e.localizedMessage ?: "Неизвестная ошибка"
            )
        }
    }
    
    /**
     * Выполняет API вызов без проверки подключения
     * 
     * Используйте этот метод когда вы хотите попытаться выполнить запрос
     * даже если NetworkMonitor показывает отсутствие подключения.
     * Полезно для ситуаций когда NetworkMonitor может ошибаться.
     * 
     * @param apiCall suspend функция которая выполняет API вызов
     * @return NetworkResult<T> с результатом операции
     */
    suspend fun <T> safeApiCallWithoutCheck(
        apiCall: suspend () -> T
    ): NetworkResult<T> {
        return try {
            val result = apiCall()
            NetworkResult.Success(result)
            
        } catch (e: UnknownHostException) {
            Timber.e(e, "Unknown host")
            NetworkResult.NetworkUnavailable
            
        } catch (e: SocketTimeoutException) {
            Timber.e(e, "Socket timeout")
            NetworkResult.Error(
                exception = e,
                message = "Превышено время ожидания ответа сервера"
            )
            
        } catch (e: IOException) {
            Timber.e(e, "Network I/O error")
            NetworkResult.Error(
                exception = e,
                message = "Ошибка сети: ${e.localizedMessage ?: "Unknown I/O error"}"
            )
            
        } catch (e: Exception) {
            Timber.e(e, "API call failed")
            NetworkResult.Error(
                exception = e,
                message = e.localizedMessage ?: "Неизвестная ошибка"
            )
        }
    }
    
    /**
     * Получает человекочитаемое сообщение для NetworkResult
     * 
     * Преобразует NetworkResult в сообщение которое можно показать пользователю.
     * 
     * @param result NetworkResult для получения сообщения
     * @param successMessage сообщение для Success (по умолчанию "Успешно")
     * @return человекочитаемое сообщение
     */
    fun <T> getDisplayMessage(
        result: NetworkResult<T>,
        successMessage: String = "Успешно"
    ): String {
        return when (result) {
            is NetworkResult.Success -> successMessage
            is NetworkResult.Error -> result.message ?: "Произошла ошибка"
            is NetworkResult.Loading -> "Загрузка..."
            is NetworkResult.NetworkUnavailable -> "Нет подключения к интернету"
        }
    }
}