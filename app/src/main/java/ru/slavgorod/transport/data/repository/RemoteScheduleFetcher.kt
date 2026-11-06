package ru.slavgorod.transport.data.repository

import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import ru.slavgorod.transport.core.Constants
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/** Сетевая граница загрузки расписания, заменяемая тестовой реализацией. */
interface ScheduleFetcher {
    /** Возвращает последнюю известную оценку доступности сети. */
    fun isOnline(): Boolean

    /** Загружает непустой ответ с ограниченным числом повторов или возвращает `null`. */
    suspend fun fetchWithRetry(): String?

}

/**
 * Загружает JSON расписания с ограниченным числом повторных попыток.
 *
 * Подсказка разработчику: задержки повторов намеренно внедряются зависимостью, чтобы
 * сетевой сценарий можно было проверять без реального ожидания в тестах.
 */
class RemoteScheduleFetcher(
    private val remoteJsonUrlProvider: suspend () -> String = { Constants.REMOTE_JSON_URL },
    private val onlineChecker: () -> Boolean,
    private val httpClient: OkHttpClient = defaultScheduleHttpClient(),
    private val retryDelaysMillis: List<Long> = DEFAULT_RETRY_DELAYS_MILLIS,
    private val delayProvider: suspend (Long) -> Unit = { delay(it) }
) : ScheduleFetcher {

    override fun isOnline(): Boolean = onlineChecker()

    override suspend fun fetchWithRetry(): String? {
        if (!isOnline()) return null

        return fetchWithRetryFrom { normalizeRemoteJsonUrl(remoteJsonUrlProvider()) }
    }

    private suspend fun fetchWithRetryFrom(urlProvider: suspend () -> String): String? {
        repeat(retryDelaysMillis.size + 1) { attempt ->
            val result = fetchOnce(urlProvider)
            if (result.value != null) return result.value
            if (!result.retryable) return null

            retryDelaysMillis.getOrNull(attempt)?.let { delayMillis ->
                delayProvider(delayMillis)
            }
        }

        return null
    }

    private suspend fun fetchOnce(urlProvider: suspend () -> String): FetchResult {
        return try {
            val request = Request.Builder()
                .url(urlProvider())
                .get()
                .build()

            httpClient.newCall(request).awaitSchedule()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Timber.w(exception, "Не удалось загрузить JSON удалённого расписания")
            FetchResult(null, retryable = false)
        }
    }

    private fun normalizeRemoteJsonUrl(rawUrl: String): String {
        return rawUrl.trim().ifBlank { Constants.REMOTE_JSON_URL }
    }

    private companion object {
        private val DEFAULT_RETRY_DELAYS_MILLIS = listOf(400L, 1_000L)
    }
}

// Ограничение применяется и к ответам без Content-Length, включая распакованные тела.
private const val MAX_SCHEDULE_BYTES = 5L * 1024 * 1024

private data class FetchResult(val value: String?, val retryable: Boolean)

/**
 * Ожидает ответ без блокировки вызывающей корутины и отменяет HTTP-вызов вместе с ней.
 * Размер проверяется по фактически прочитанным байтам, поскольку сервер может не
 * прислать длину тела или передать сжатый ответ. Постоянные ошибки не повторяются.
 */
private suspend fun Call.awaitSchedule(): FetchResult = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            if (continuation.isActive) {
                Timber.w(e, "Не удалось загрузить JSON удалённого расписания")
                continuation.resume(FetchResult(null, retryable = true))
            }
        }

        override fun onResponse(call: Call, response: Response) {
            val result = try {
                response.use {
                    if (!response.isSuccessful) {
                        Timber.w("Удалённое расписание вернуло код %d", response.code)
                        FetchResult(null, retryable = response.code == 408 || response.code == 429 || response.code >= 500)
                    } else {
                        val body = response.body ?: return@use FetchResult(null, retryable = false)
                        if (body.contentLength() > MAX_SCHEDULE_BYTES) return@use FetchResult(null, retryable = false)
                        val source = body.source()
                        val buffer = okio.Buffer()
                        while (buffer.size <= MAX_SCHEDULE_BYTES) {
                            val read = source.read(buffer, minOf(8192L, MAX_SCHEDULE_BYTES + 1 - buffer.size))
                            if (read == -1L) break
                        }
                        if (buffer.size > MAX_SCHEDULE_BYTES) FetchResult(null, retryable = false)
                        else FetchResult(buffer.readString(body.contentType()?.charset(Charsets.UTF_8) ?: Charsets.UTF_8)
                            .trim().takeIf(String::isNotBlank), retryable = false)
                    }
                }
            } catch (e: IOException) {
                Timber.w(e, "Не удалось прочитать расписание")
                FetchResult(null, retryable = true)
            }
            if (continuation.isActive) continuation.resume(result)
        }
    })
}

internal fun defaultScheduleHttpClient(): OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(Constants.REMOTE_CONNECTION_TIMEOUT.toLong(), TimeUnit.MILLISECONDS)
    .readTimeout(Constants.REMOTE_READ_TIMEOUT.toLong(), TimeUnit.MILLISECONDS)
    .build()
