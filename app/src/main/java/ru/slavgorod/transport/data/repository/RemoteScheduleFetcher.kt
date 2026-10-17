package ru.slavgorod.transport.data.repository

import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.slavgorod.transport.core.Constants
import timber.log.Timber
import java.util.concurrent.TimeUnit

interface ScheduleFetcher {
    fun isOnline(): Boolean

    suspend fun fetchWithRetry(): String?
}

class RemoteScheduleFetcher(
    private val remoteJsonUrlProvider: suspend () -> String = { Constants.REMOTE_JSON_URL },
    private val onlineChecker: () -> Boolean,
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(Constants.REMOTE_CONNECTION_TIMEOUT.toLong(), TimeUnit.MILLISECONDS)
        .readTimeout(Constants.REMOTE_READ_TIMEOUT.toLong(), TimeUnit.MILLISECONDS)
        .build(),
    private val retryDelaysMillis: List<Long> = DEFAULT_RETRY_DELAYS_MILLIS,
    private val delayProvider: suspend (Long) -> Unit = { delay(it) }
) : ScheduleFetcher {

    override fun isOnline(): Boolean = onlineChecker()

    override suspend fun fetchWithRetry(): String? {
        if (!isOnline()) return null

        repeat(retryDelaysMillis.size + 1) { attempt ->
            val result = fetchOnce()
            if (!result.isNullOrBlank()) {
                return result
            }

            retryDelaysMillis.getOrNull(attempt)?.let { delayMillis ->
                delayProvider(delayMillis)
            }
        }

        return null
    }

    private suspend fun fetchOnce(): String? {
        return try {
            val request = Request.Builder()
                .url(normalizeRemoteJsonUrl(remoteJsonUrlProvider()))
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Timber.w("Remote table returned code %d", response.code)
                    null
                } else {
                    response.body?.string()
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }
                }
            }
        } catch (exception: Exception) {
            Timber.w(exception, "Failed to fetch remote table JSON")
            null
        }
    }

    private fun normalizeRemoteJsonUrl(rawUrl: String): String {
        return rawUrl.trim().ifBlank { Constants.REMOTE_JSON_URL }
    }

    private companion object {
        private val DEFAULT_RETRY_DELAYS_MILLIS = listOf(400L, 1_000L)
    }
}
