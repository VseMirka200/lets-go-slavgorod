package com.example.lets_go_slavgorod.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.lets_go_slavgorod.data.model.BusRoute
import com.example.lets_go_slavgorod.data.model.BusSchedule
import com.example.lets_go_slavgorod.data.local.JsonDataSource
import com.example.lets_go_slavgorod.core.Constants
import com.example.lets_go_slavgorod.core.RetryUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar

/**
 * Источник удалённых данных для загрузки расписания из GitHub
 * 
 * Загружает JSON файл с расписанием из GitHub репозитория и сохраняет
 * его локально для работы в оффлайн режиме.
 * 
 * Основные функции:
 * - Загрузка актуального расписания из GitHub
 * - Кэширование данных локально
 * - Проверка версии файла
 * - Fallback на локальный assets при отсутствии соединения
 * 
 * URL файла настраивается через константу REMOTE_JSON_URL.
 * По умолчанию указывает на raw.githubusercontent.com.
 * 
 * Формат URL:
 * https://raw.githubusercontent.com/USERNAME/REPO/BRANCH/path/to/routes_data.json
 * 
 * @param context контекст приложения для доступа к файловой системе
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 3.0
 */
class RemoteDataSource(private val context: Context) {
    
    companion object {
        /** Имя кэш-файла для сохранения загруженных данных */
        private const val CACHE_FILE_NAME = "remote_routes_data.json"
        /** Имя файла для хранения версии кэша */
        private const val CACHE_VERSION_FILE = "cache_version.txt"
        /** Имя файла для хранения метаданных кэша */
        private const val CACHE_METADATA_FILE = "cache_metadata.json"
        /** Имя файла для хранения ETag */
        private const val CACHE_ETAG_FILE = "cache_etag.txt"
        /** TTL кэша в часах */
        private const val CACHE_TTL_HOURS = 24L
        /** Таймаут загрузки */
        private const val DOWNLOAD_TIMEOUT_MS = 30000L
    }
    
    private var cachedRoutes: List<BusRoute>? = null
    private val cachedSchedules = mutableMapOf<String, List<BusSchedule>>()
    private val metrics = DownloadMetrics(context)
    
    /**
     * Получает файл кэша в internal storage
     */
    private fun getCacheFile(): File {
        return File(context.filesDir, CACHE_FILE_NAME)
    }
    
    /**
     * Получает файл версии кэша
     */
    private fun getCacheVersionFile(): File {
        return File(context.filesDir, CACHE_VERSION_FILE)
    }
    
    /**
     * Получает файл ETag кэша
     */
    private fun getCacheETagFile(): File {
        return File(context.filesDir, CACHE_ETAG_FILE)
    }
    
    /**
     * Сохраняет ETag
     */
    private fun saveETag(etag: String) {
        try {
            getCacheETagFile().writeText(etag)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка сохранения ETag")
        }
    }
    
    /**
     * Загружает сохранённый ETag
     */
    private fun loadETag(): String? {
        return try {
            val etagFile = getCacheETagFile()
            if (etagFile.exists()) {
                etagFile.readText().trim()
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Ошибка загрузки ETag")
            null
        }
    }
    
    /**
     * Сохраняет версию кэша
     */
    private fun saveCacheVersion(version: String) {
        try {
            getCacheVersionFile().writeText(version)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка сохранения версии кэша")
        }
    }
    
    /**
     * Загружает сохранённую версию кэша
     */
    private fun loadCacheVersion(): String? {
        return try {
            val versionFile = getCacheVersionFile()
            if (versionFile.exists()) {
                versionFile.readText().trim()
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Ошибка загрузки версии кэша")
            null
        }
    }
    
    /**
     * Извлекает версию из JSON строки
     */
    private fun extractVersionFromJson(jsonString: String): String? {
        return try {
            val jsonObject = JSONObject(jsonString)
            jsonObject.optString("version").takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            Timber.e(e, "Ошибка извлечения версии из JSON")
            null
        }
    }
    
    /**
     * Загружает JSON с удалённого сервера (GitHub) с retry механизмом
     * 
     * @return содержимое JSON файла или null при ошибке
     */
    private suspend fun downloadRemoteJson(): String? = withContext(Dispatchers.IO) {
        // Проверяем качество соединения
        if (!isNetworkAvailable()) {
            return@withContext null
        }
        
        // Загружаем с retry механизмом
        RetryPolicy.executeWithRetry(
            operation = { attempt -> downloadWithTimeout() }
        )
    }
    
    /**
     * Загружает данные с таймаутом
     */
    private suspend fun downloadWithTimeout(): String? = withTimeoutOrNull(DOWNLOAD_TIMEOUT_MS) {
        try {
            
            val url = URL(Constants.REMOTE_JSON_URL)
            val connection = url.openConnection() as HttpURLConnection
            
            connection.apply {
                requestMethod = "GET"
                connectTimeout = Constants.REMOTE_CONNECTION_TIMEOUT
                readTimeout = Constants.REMOTE_READ_TIMEOUT
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "LetsGoSlavgorod-Android/2.0")
                setRequestProperty("Cache-Control", "no-cache")
                setRequestProperty("Connection", "close")
                
                // Добавляем ETag для условной загрузки
                val savedETag = loadETag()
                if (savedETag != null) {
                    setRequestProperty("If-None-Match", savedETag)
                }
                
                useCaches = false
                defaultUseCaches = false
            }
            
            val responseCode = connection.responseCode
            
            when (responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
                    
                    // Сохраняем ETag для будущих запросов
                    val etag = connection.getHeaderField("ETag")
                    if (etag != null) {
                        saveETag(etag)
                    }
                    
                    connection.disconnect()
                    
                    // Расширенная валидация
                    if (validateJsonData(jsonString)) {
                        return@withTimeoutOrNull jsonString
                    } else {
                        return@withTimeoutOrNull null
                    }
                }
                HttpURLConnection.HTTP_NOT_MODIFIED -> {
                    // 304 - данные не изменились, используем кэш
                    connection.disconnect()
                    
                    // Загружаем из кэша
                    val cacheFile = getCacheFile()
                    if (cacheFile.exists()) {
                        cacheFile.readText()
                    } else {
                        null
                    }
                }
                HttpURLConnection.HTTP_NOT_FOUND -> {
                    return@withTimeoutOrNull null
                }
                HttpURLConnection.HTTP_UNAVAILABLE -> {
                    return@withTimeoutOrNull null
                }
                else -> {
                    return@withTimeoutOrNull null
                }
            }
            
        } catch (e: java.net.UnknownHostException) {
            null
        } catch (e: java.net.SocketTimeoutException) {
            null
        } catch (e: Exception) {
            try {
                Timber.e(e, "Ошибка загрузки с GitHub: ${e.javaClass.simpleName}")
            } catch (logError: Exception) {
                // Fallback на системное логирование если Timber не работает
                android.util.Log.e("RemoteDataSource", "Ошибка загрузки с GitHub: ${e.javaClass.simpleName}", e)
            }
            null
        }
    }
    
    /**
     * Расширенная валидация JSON данных
     */
    private fun validateJsonData(jsonString: String): Boolean {
        return try {
            val json = JSONObject(jsonString)
            
            // Проверяем обязательные поля
            if (!json.has("routes") || !json.has("version")) {
                return false
            }
            
            val routes = json.getJSONArray("routes")
            if (routes.length() == 0) {
                return false
            }
            
            // Проверяем структуру первого маршрута
            val firstRoute = routes.getJSONObject(0)
            val requiredFields = listOf("id", "routeNumber", "name", "schedules")
            for (field in requiredFields) {
                if (!firstRoute.has(field)) {
                    return false
                }
            }
            
            // Проверяем размер файла (не слишком большой)
            if (jsonString.length > 10 * 1024 * 1024) { // 10MB
                return false
            }
            
            true
            
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Проверяет доступность сети
     */
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    
    /**
     * Сохраняет JSON в кэш-файл и версию
     */
    private suspend fun saveToCache(jsonString: String) = withContext(Dispatchers.IO) {
        try {
            val cacheFile = getCacheFile()
            cacheFile.writeText(jsonString)
            
            // Сохраняем версию
            val version = extractVersionFromJson(jsonString)
            if (version != null) {
                saveCacheVersion(version)
            }
            
        } catch (e: Exception) {
            Timber.e(e, "Ошибка сохранения данных маршрутов в кэш")
        }
    }
    
    /**
     * Читает JSON из кэш-файла с проверкой валидности
     * 
     * Проверка версий теперь происходит в shouldUpdate()
     */
    private suspend fun loadFromCache(): String? = withContext(Dispatchers.IO) {
        try {
            val cacheFile = getCacheFile()
            if (!cacheFile.exists()) {
                return@withContext null
            }
            
            val jsonString = cacheFile.readText()
            
            // Проверяем, что JSON валиден и парсится без ошибок
            try {
                val testJson = JSONObject(jsonString)
                val testRoutes = testJson.getJSONArray("routes")
                val cachedVersion = testJson.optString("version", "unknown")
            } catch (e: Exception) {
                clearCache()
                return@withContext null
            }
            
            jsonString
        } catch (e: Exception) {
            clearCache() // Очищаем кэш при любой ошибке
            null
        }
    }
    
    /**
     * Загружает JSON из assets (fallback)
     */
    private suspend fun loadFromAssets(): String? = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.assets.open("routes_data.json")
                .bufferedReader()
                .use { it.readText() }
            
            // Проверяем валидность JSON из assets
            try {
                val testJson = JSONObject(jsonString)
                val testRoutes = testJson.getJSONArray("routes")
                val version = testJson.optString("version", "unknown")
            } catch (e: Exception) {
                return@withContext null
            }
            
            jsonString
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Проверяет нужно ли обновление, сравнивая версии
     * 
     * @return Pair<Boolean, String?> - (нужно ли обновление, скачанный JSON если есть)
     */
    private suspend fun shouldUpdate(): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        try {
            
            // Получаем локальную версию из кэша
            val cachedVersion = try {
                val cachedJson = loadFromCache()
                cachedJson?.let { extractVersionFromJson(it) }
            } catch (e: Exception) {
                null
            }
            
            // Получаем версию с GitHub и сам JSON
            val remoteJson = try {
                downloadRemoteJson()
            } catch (e: Exception) {
                null
            }
            
            if (remoteJson == null) {
                return@withContext Pair(false, null)
            }
            
            val remoteVersion = extractVersionFromJson(remoteJson)
            
            // Если кэша нет - нужно обновление
            if (cachedVersion == null) {
                return@withContext Pair(true, remoteJson)
            }
            
            // Сравниваем версии
            val needsUpdate = remoteVersion != null && remoteVersion != cachedVersion
            if (needsUpdate) {
                return@withContext Pair(true, remoteJson)
            } else {
                return@withContext Pair(false, null)
            }
        } catch (e: Exception) {
            Pair(false, null)
        }
    }
    
    /**
     * Получает актуальный JSON (из GitHub, кэша или assets)
     * 
     * НОВАЯ ЛОГИКА:
     * 1. Проверяет наличие интернета в начале
     * 2. Если интернета НЕТ - сразу использует кэш или assets (быстро!)
     * 3. Если интернет ЕСТЬ - проверяет обновления на GitHub
     * 4. Если версия новее - автоматически скачивает и сохраняет
     * 5. Если кэша нет - fallback на assets
     * 
     * @param forceRefresh принудительная загрузка с GitHub (игнорирует проверку версии)
     * @return JSON строка или null при ошибке
     */
    suspend fun getJsonString(forceRefresh: Boolean = false): String? {
        val hasInternet = isNetworkAvailable()
        
        // Если интернета нет и не force refresh - сразу используем кэш/assets
        if (!hasInternet && !forceRefresh) {
            
            // Пробуем загрузить из кэша
            val cachedJson = loadFromCache()
            if (cachedJson != null) {
                return cachedJson
            }
            
            // Если кэша нет - загружаем из assets
            val assetsJson = loadFromAssets()
            if (assetsJson != null) {
                return assetsJson
            }
            
            return null
        }
        
        // Если есть интернет или force refresh - пытаемся обновить
        if (forceRefresh) {
            val remoteJson = downloadRemoteJson()
            if (remoteJson != null) {
                saveToCache(remoteJson)
                return remoteJson
            }
        } else {
            // Обычная загрузка - проверяем нужно ли обновление
            
            val (needsUpdate, remoteJson) = shouldUpdate()
            if (needsUpdate && remoteJson != null) {
                saveToCache(remoteJson)
                return remoteJson
            } else if (needsUpdate) {
            }
        }
        
        // Пробуем загрузить из кэша
        val cachedJson = loadFromCache()
        if (cachedJson != null) {
            return cachedJson
        }
        
        // Если в кэше нет и есть интернет, пробуем загрузить с GitHub
        if (hasInternet) {
            val remoteJson = downloadRemoteJson()
            if (remoteJson != null) {
                saveToCache(remoteJson)
                return remoteJson
            }
        }
        
        // Если всё не удалось, используем assets
        val assetsJson = loadFromAssets()
        if (assetsJson != null) {
            return assetsJson
        }
        
        try {
            Timber.e("Все источники данных недоступны")
        } catch (e: Exception) {
            android.util.Log.e("RemoteDataSource", "Все источники данных недоступны")
        }
        return null
    }
    
    /**
     * Загружает маршруты с метриками и retry механизмом
     * 
     * @param forceRefresh принудительная загрузка с GitHub
     * @return список маршрутов
     */
    suspend fun loadRoutes(forceRefresh: Boolean = false): List<BusRoute> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        
        // Возвращаем кэшированные данные если не требуется обновление
        if (!forceRefresh && cachedRoutes != null) {
            return@withContext cachedRoutes!!
        }
        
        try {
            val jsonString = RetryUtils.retryNetwork {
                getJsonString(forceRefresh)
            }
            
            if (jsonString == null) {
                Timber.e("getJsonString вернул null - данные недоступны")
                return@withContext emptyList()
            }
            
            val jsonObject = JSONObject(jsonString)
            val routesArray = jsonObject.getJSONArray("routes")
            
            val routes = mutableListOf<BusRoute>()
            
            for (i in 0 until routesArray.length()) {
                val routeJson = routesArray.getJSONObject(i)
                
                val route = BusRoute(
                    id = routeJson.getString("id"),
                    routeNumber = routeJson.getString("routeNumber"),
                    name = routeJson.getString("name"),
                    description = routeJson.getString("description"),
                    color = routeJson.optString("color").takeIf { it.isNotEmpty() } ?: "#1976D2",
                    travelTime = routeJson.optString("travelTime").takeIf { it.isNotEmpty() },
                    pricePrimary = routeJson.optString("pricePrimary").takeIf { it.isNotEmpty() },
                    priceSecondary = routeJson.optString("priceSecondary").takeIf { it.isNotEmpty() },
                    paymentMethods = routeJson.optString("paymentMethods").takeIf { it.isNotEmpty() }
                )
                
                routes.add(route)
            }
            
            cachedRoutes = routes
            
            val loadTime = System.currentTimeMillis() - startTime
            
            // Записываем метрики
            metrics.recordSuccess(
                DownloadMetrics.DataSource.GITHUB,
                jsonString.length.toLong(),
                loadTime
            )
            
            
            routes
        } catch (e: Exception) {
            metrics.recordFailure(
                DownloadMetrics.DataSource.GITHUB,
                e.message ?: "Unknown error"
            )
            
            try {
                Timber.e(e, "Ошибка парсинга JSON маршрутов: ${e.javaClass.simpleName} - ${e.message}")
            } catch (logError: Exception) {
                android.util.Log.e("RemoteDataSource", "Ошибка парсинга JSON маршрутов: ${e.javaClass.simpleName} - ${e.message}", e)
            }
            emptyList()
        }
    }
    
    /**
     * Fallback загрузка данных при ошибке основного источника
     * 
     * Пытается загрузить данные из альтернативных источников:
     * 1. Локальный кэш файл
     * 2. Assets файл
     * 3. Пустой список (последний fallback)
     */
    private suspend fun tryFallbackLoad(): List<BusRoute> {
        return try {
            
            // Пытаемся загрузить из кэша
            val cacheFile = File(context.cacheDir, "routes_data.json")
            if (cacheFile.exists()) {
                val cachedJson = cacheFile.readText()
                val jsonDataSource = JsonDataSource(context)
                val cachedRoutes = jsonDataSource.parseRoutesFromJson(cachedJson)
                if (cachedRoutes.isNotEmpty()) {
                    return cachedRoutes
                }
            }
            
            // Пытаемся загрузить из assets
            val jsonDataSource = JsonDataSource(context)
            val assetsRoutes = jsonDataSource.loadRoutes()
            if (assetsRoutes.isNotEmpty()) {
                return assetsRoutes
            }
            
            Timber.e("Все резервные источники недоступны, возвращаем пустой список")
            emptyList()
        } catch (e: Exception) {
            Timber.e(e, "Ошибка резервной загрузки: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Загружает расписание для конкретного маршрута
     * 
     * @param routeId ID маршрута
     * @param forceRefresh принудительная загрузка с GitHub
     * @return список расписаний или null если данных нет
     */
    suspend fun loadSchedules(routeId: String, forceRefresh: Boolean = false): List<BusSchedule>? = withContext(Dispatchers.IO) {
        // Проверяем кэш если не требуется обновление
        if (!forceRefresh && cachedSchedules.containsKey(routeId)) {
            return@withContext cachedSchedules[routeId]
        }
        
        try {
            val jsonString = getJsonString(forceRefresh) ?: return@withContext null
            val jsonObject = JSONObject(jsonString)
            val routesArray = jsonObject.getJSONArray("routes")
            val currentDayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
            
            for (i in 0 until routesArray.length()) {
                val routeJson = routesArray.getJSONObject(i)
                val id = routeJson.getString("id")
                
                if (id == routeId) {
                    val schedulesArray = routeJson.optJSONArray("schedules")
                    if (schedulesArray != null && schedulesArray.length() > 0) {
                        val schedules = parseSchedules(schedulesArray, routeId, currentDayOfWeek)
                        cachedSchedules[routeId] = schedules
                        return@withContext schedules
                    } else {
                        return@withContext null
                    }
                }
            }
            
            null
        } catch (e: Exception) {
            Timber.e(e, "Ошибка загрузки расписания для маршрута $routeId")
            null
        }
    }
    
    /**
     * Парсит расписания из JSON
     */
    private fun parseSchedules(schedulesArray: JSONArray, routeId: String, dayOfWeek: Int): List<BusSchedule> {
        val schedules = mutableListOf<BusSchedule>()
        
        for (i in 0 until schedulesArray.length()) {
            val scheduleJson = schedulesArray.getJSONObject(i)
            
            val schedule = BusSchedule(
                id = scheduleJson.getString("id"),
                routeId = routeId,
                stopName = scheduleJson.getString("stopName"),
                departureTime = scheduleJson.getString("departureTime"),
                dayOfWeek = dayOfWeek,
                notes = scheduleJson.optString("notes").takeIf { it.isNotEmpty() },
                departurePoint = scheduleJson.getString("departurePoint")
            )
            
            schedules.add(schedule)
        }
        
        return schedules
    }
    
    /**
     * Получает версию данных из JSON
     */
    suspend fun getDataVersion(): String? = withContext(Dispatchers.IO) {
        try {
            val jsonString = getJsonString() ?: return@withContext null
            val jsonObject = JSONObject(jsonString)
            jsonObject.optString("version").takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            Timber.e(e, "Ошибка получения версии данных")
            null
        }
    }
    
    /**
     * Получает версию данных напрямую с GitHub (не из кэша)
     * 
     * Используется для проверки доступности обновлений
     */
    suspend fun getRemoteDataVersion(): String? = withContext(Dispatchers.IO) {
        try {
            val remoteJson = downloadRemoteJson() ?: return@withContext null
            val jsonObject = JSONObject(remoteJson)
            jsonObject.optString("version").takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            Timber.e(e, "Ошибка получения удаленной версии данных")
            null
        }
    }
    
    /**
     * Получает дату последнего обновления из JSON
     */
    suspend fun getLastUpdated(): String? = withContext(Dispatchers.IO) {
        try {
            val jsonString = getJsonString() ?: return@withContext null
            val jsonObject = JSONObject(jsonString)
            jsonObject.optString("last_updated").takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            Timber.e(e, "Ошибка получения даты последнего обновления")
            null
        }
    }
    
    /**
     * Проверяет доступность обновлений
     * 
     * @return true если доступна новая версия данных
     */
    suspend fun checkForUpdates(): Boolean = withContext(Dispatchers.IO) {
        try {
            val remoteJson = downloadRemoteJson()
            
            if (remoteJson == null) {
                return@withContext false
            }
            
            val cachedJson = loadFromCache()
            
            if (cachedJson == null) {
                // Если кэша нет, есть обновление
                return@withContext true
            }
            
            val remoteVersion = JSONObject(remoteJson).optString("version", "unknown")
            val cachedVersion = JSONObject(cachedJson).optString("version", "unknown")
            
            val hasUpdate = remoteVersion.isNotEmpty() && 
                           cachedVersion.isNotEmpty() && 
                           remoteVersion != cachedVersion
            
            
            if (hasUpdate) {
            } else {
            }
            
            hasUpdate
        } catch (e: Exception) {
            Timber.e(e, "Ошибка проверки обновлений")
            false
        }
    }
    
    /**
     * Очищает кэш маршрутов и расписаний
     */
    fun clearCache() {
        cachedRoutes = null
        cachedSchedules.clear()
        
        try {
            val cacheFile = getCacheFile()
            if (cacheFile.exists()) {
                cacheFile.delete()
            }
            
            val versionFile = getCacheVersionFile()
            if (versionFile.exists()) {
                versionFile.delete()
            }
        } catch (e: Exception) {
            Timber.e(e, "Ошибка удаления файла кэша")
        }
    }
    
    /**
     * Очищает кэш маршрутов только в памяти (не удаляет файл)
     * 
     * Используется при обновлении данных, чтобы принудить перечитать файл
     */
    fun clearRoutesMemoryCache() {
        cachedRoutes = null
    }
    
    /**
     * Очищает только кэш расписаний (чтобы они перезагрузились)
     */
    fun clearSchedulesCache() {
        cachedSchedules.clear()
    }
    
    /**
     * Очищает кэш расписания для конкретного маршрута
     */
    fun clearScheduleCache(routeId: String) {
        cachedSchedules.remove(routeId)
    }
    
    /**
     * Получает статистику загрузок
     */
    fun getDownloadStats(): DownloadMetrics.DownloadStats {
        return metrics.getStats()
    }
}