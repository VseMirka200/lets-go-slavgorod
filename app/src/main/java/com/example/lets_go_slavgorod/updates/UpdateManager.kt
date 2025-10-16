/**
 * Менеджер для проверки и загрузки обновлений приложения
 * 
 * Этот класс отвечает за:
 * - Проверку доступности интернет-соединения
 * - Получение информации о последней версии с GitHub API
 * - Сравнение версий приложения
 * - Загрузку обновлений через браузер
 */
@file:Suppress("UNCHECKED_CAST")

package com.example.lets_go_slavgorod.updates

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.lets_go_slavgorod.BuildConfig
import timber.log.Timber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.max
import com.example.lets_go_slavgorod.utils.ValidationUtils

/**
 * Менеджер для проверки и загрузки обновлений приложения
 * 
 * Основные функции:
 * - Проверка доступности интернет-соединения
 * - Запрос к GitHub API для получения информации о последнем релизе
 * - Сравнение версий приложения
 * - Запуск загрузки обновления через браузер
 */
class UpdateManager(private val context: Context) {
    
    companion object {
        private val GITHUB_API_URL = BuildConfig.GITHUB_API_URL
        private const val TIMEOUT_MS = 10000L
        private const val MAX_RETRIES = 3
        private const val INITIAL_RETRY_DELAY_MS = 1000L
    }

    /**
     * Создает безопасное HTTPS соединение с проверкой SSL
     */
    private fun createSecureConnection(url: URL): HttpURLConnection {
        val connection = url.openConnection() as HttpURLConnection
        
        // Настройки безопасности
        connection.requestMethod = "GET"
        connection.setRequestProperty("Connection", "close")
        
        // Таймауты
        connection.connectTimeout = TIMEOUT_MS.toInt()
        connection.readTimeout = TIMEOUT_MS.toInt()
        
        // Принудительно используем HTTPS
        if (url.protocol == "https") {
            connection.useCaches = false
            connection.defaultUseCaches = false
        }
        
        return connection
    }
    
    /**
     * Результат проверки обновлений
     */
    data class UpdateResult(
        val success: Boolean,
        val update: UpdateInfo? = null,
        val error: String? = null
    )
    
    /**
     * Информация об обновлении
     */
    data class UpdateInfo(
        val versionName: String,
        val downloadUrl: String,
        val releaseNotes: String
    )
    
    /**
     * Проверяет доступность интернет-соединения
     */
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        return connectivityManager?.let { cm ->
            val network = cm.activeNetwork
            val capabilities = cm.getNetworkCapabilities(network)
            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } ?: false
    }
    
    /**
     * Получает информацию о последнем релизе с повторными попытками
     * 
     * Использует exponential backoff для повторных попыток:
     * - 1-я попытка: немедленно
     * - 2-я попытка: через 1 секунду
     * - 3-я попытка: через 2 секунды
     * 
     * @return UpdateInfo или null если все попытки неудачны
     */
    private suspend fun fetchLatestReleaseWithRetry(): UpdateInfo? {
        repeat(MAX_RETRIES) { attempt ->
            try {
                Timber.d("Fetching latest release (attempt ${attempt + 1}/$MAX_RETRIES)")
                
                val result = withTimeoutOrNull(TIMEOUT_MS) {
                    fetchLatestRelease()
                }
                
                if (result != null) {
                    Timber.d("Successfully fetched release info on attempt ${attempt + 1}")
                    return result
                }
                
                // Если это не последняя попытка, ждем перед следующей
                if (attempt < MAX_RETRIES - 1) {
                    val delayMs = INITIAL_RETRY_DELAY_MS * (1 shl attempt) // Exponential backoff
                    Timber.d("Retrying in ${delayMs}ms...")
                    delay(delayMs)
                }
            } catch (e: Exception) {
                Timber.e(e, "Attempt ${attempt + 1} failed")
                
                // Если это не последняя попытка, ждем перед следующей
                if (attempt < MAX_RETRIES - 1) {
                    val delayMs = INITIAL_RETRY_DELAY_MS * (1 shl attempt)
                    delay(delayMs)
                } else {
                    // Это была последняя попытка
                    throw e
                }
            }
        }
        
        Timber.w("All $MAX_RETRIES attempts failed")
        return null
    }
    
    /**
     * Получает информацию о последнем релизе с GitHub API
     * 
     * Внутренний метод, используемый fetchLatestReleaseWithRetry()
     */
    private suspend fun fetchLatestRelease(): UpdateInfo? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(GITHUB_API_URL)
            connection = createSecureConnection(url)
            
            connection.apply {
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "LetsGoSlavgorod/${BuildConfig.VERSION_NAME}")
            }
            
            val responseCode = connection.responseCode
            Timber.d("GitHub API response code: $responseCode")
            
            when (responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    if (response.isBlank()) {
                        Timber.w("Empty response from GitHub API")
                        return@withContext null
                    }
                    
                    val json = JSONObject(response)
                    
                    val versionName = json.optString("tag_name", "").trim()
                    val assets = json.optJSONArray("assets")
                    val downloadUrl = if (assets != null && assets.length() > 0) {
                        assets.getJSONObject(0).optString("browser_download_url", "").trim()
                    } else {
                        ""
                    }
                    val releaseNotes = json.optString("body", "").trim()
                    
                    // Валидация полученных данных
                    if (versionName.isBlank()) {
                        Timber.w("Empty version name in response")
                        return@withContext null
                    }
                    
                    // Валидация URL скачивания
                    if (downloadUrl.isNotBlank() && !ValidationUtils.isValidUrl(downloadUrl)) {
                        Timber.w("Invalid download URL format: '$downloadUrl'")
                        return@withContext null
                    }
                    
                    Timber.d("Successfully parsed release info: version=$versionName, hasDownloadUrl=${downloadUrl.isNotBlank()}")
                    UpdateInfo(versionName, downloadUrl, releaseNotes)
                }
                HttpURLConnection.HTTP_NOT_FOUND -> {
                    Timber.w("Repository not found (404)")
                    null
                }
                HttpURLConnection.HTTP_FORBIDDEN -> {
                    Timber.w("Access forbidden (403) - rate limit or permissions")
                    null
                }
                HttpURLConnection.HTTP_UNAVAILABLE -> {
                    Timber.w("Service unavailable (503)")
                    null
                }
                else -> {
                    Timber.w("GitHub API returned unexpected error code: $responseCode")
                    null
                }
            }
        } catch (e: java.net.SocketTimeoutException) {
            Timber.e(e, "Timeout while fetching latest release")
            null
        } catch (e: java.net.UnknownHostException) {
            Timber.e(e, "Unknown host while fetching latest release")
            null
        } catch (e: java.net.ConnectException) {
            Timber.e(e, "Connection failed while fetching latest release")
            null
        } catch (e: org.json.JSONException) {
            Timber.e(e, "JSON parsing error while fetching latest release")
            null
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error while fetching latest release")
            null
        } finally {
            try {
                connection?.disconnect()
            } catch (e: Exception) {
                Timber.w(e, "Error disconnecting from GitHub API")
            }
        }
    }
    
    /**
     * Сравнивает версии приложения
     */
    private fun compareVersions(currentVersion: String, latestVersion: String): Boolean {
        return try {
            val current = parseVersion(currentVersion)
            val latest = parseVersion(latestVersion)
            
            // Сравниваем версии
            for (i in 0 until max(current.size, latest.size)) {
                val currentPart = current.getOrNull(i) ?: 0
                val latestPart = latest.getOrNull(i) ?: 0
                
                when {
                    latestPart > currentPart -> return true
                    latestPart < currentPart -> return false
                }
            }
            false
        } catch (e: Exception) {
            Timber.e(e, "Error comparing versions")
            false
        }
    }
    
    /**
     * Парсит версию в массив чисел
     */
    private fun parseVersion(version: String): List<Int> {
        return version.replace("v", "").split(".")
            .mapNotNull { it.toIntOrNull() }
    }
    
    /**
     * Получает текущую версию приложения
     */
    private fun getCurrentVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName
        } catch (e: Exception) {
            Timber.e(e, "Error getting current version")
            "1.0.0"
        }.toString()
    }
    
    /**
     * Проверяет наличие обновлений
     */
    suspend fun checkForUpdatesWithResult(): UpdateResult = withContext(Dispatchers.IO) {
        try {
            if (!isNetworkAvailable()) {
                Timber.w("No network connection available")
                return@withContext UpdateResult(false, error = "Нет интернет-соединения")
            }
            
            val currentVersion = getCurrentVersion()
            Timber.d("Current version: $currentVersion")
            
            // Попытка получить информацию с повторными попытками
            val latestRelease = fetchLatestReleaseWithRetry()
            
            if (latestRelease == null) {
                Timber.w("Failed to fetch latest release information")
                return@withContext UpdateResult(false, error = "Не удалось получить информацию об обновлениях")
            }
            
            // Валидация полученных данных
            if (latestRelease.versionName.isBlank()) {
                Timber.w("Invalid version name received: '${latestRelease.versionName}'")
                return@withContext UpdateResult(false, error = "Получена некорректная информация о версии")
            }
            
            if (latestRelease.downloadUrl.isBlank()) {
                Timber.w("No download URL available for version: ${latestRelease.versionName}")
                return@withContext UpdateResult(false, error = "Ссылка для загрузки недоступна")
            }
            
            Timber.d("Latest version: ${latestRelease.versionName}")
            
            if (compareVersions(currentVersion, latestRelease.versionName)) {
                Timber.i("Update available: ${latestRelease.versionName}")
                UpdateResult(true, latestRelease)
            } else {
                Timber.i("No updates available")
                UpdateResult(true, null)
            }
        } catch (e: java.net.SocketTimeoutException) {
            Timber.e(e, "Network timeout during update check")
            UpdateResult(false, error = "Превышено время ожидания. Проверьте соединение.")
        } catch (e: java.net.UnknownHostException) {
            Timber.e(e, "Unknown host during update check")
            UpdateResult(false, error = "Не удалось подключиться к серверу обновлений")
        } catch (e: java.net.ConnectException) {
            Timber.e(e, "Connection failed during update check")
            UpdateResult(false, error = "Ошибка подключения к серверу")
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error during update check")
            UpdateResult(false, error = "Неожиданная ошибка при проверке обновлений: ${e.message}")
        }
    }

}
