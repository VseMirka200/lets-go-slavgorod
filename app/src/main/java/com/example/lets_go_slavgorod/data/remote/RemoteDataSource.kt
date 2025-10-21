package com.example.lets_go_slavgorod.data.remote

import android.content.Context
import com.example.lets_go_slavgorod.data.model.BusRoute
import com.example.lets_go_slavgorod.data.model.BusSchedule
import com.example.lets_go_slavgorod.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    }
    
    private var cachedRoutes: List<BusRoute>? = null
    private val cachedSchedules = mutableMapOf<String, List<BusSchedule>>()
    
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
     * Сохраняет версию кэша
     */
    private fun saveCacheVersion(version: String) {
        try {
            getCacheVersionFile().writeText(version)
            Timber.d("Saved cache version: $version")
        } catch (e: Exception) {
            Timber.e(e, "Error saving cache version")
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
            Timber.e(e, "Error loading cache version")
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
            Timber.e(e, "Error extracting version from JSON")
            null
        }
    }
    
    /**
     * Загружает JSON с удалённого сервера (GitHub)
     * 
     * @return содержимое JSON файла или null при ошибке
     */
    private suspend fun downloadRemoteJson(): String? = withContext(Dispatchers.IO) {
        try {
            Timber.i("🌐 Downloading routes data from GitHub: ${Constants.REMOTE_JSON_URL}")
            
            val url = URL(Constants.REMOTE_JSON_URL)
            val connection = url.openConnection() as HttpURLConnection
            
            connection.apply {
                requestMethod = "GET"
                connectTimeout = Constants.REMOTE_CONNECTION_TIMEOUT
                readTimeout = Constants.REMOTE_READ_TIMEOUT
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "LetsGoSlavgorod-Android")
            }
            
            val responseCode = connection.responseCode
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()
                
                // Проверяем, что загруженный JSON валиден
                try {
                    val testJson = JSONObject(jsonString)
                    val testRoutes = testJson.getJSONArray("routes")
                    val version = testJson.optString("version", "unknown")
                    Timber.i("✅ Successfully downloaded routes data from GitHub (${jsonString.length} bytes, ${testRoutes.length()} routes, version: $version)")
                } catch (e: Exception) {
                    Timber.e(e, "❌ Downloaded JSON is invalid, discarding")
                    return@withContext null
                }
                
                jsonString
            } else {
                Timber.w("⚠️ Failed to download from GitHub: HTTP $responseCode")
                connection.disconnect()
                null
            }
        } catch (e: java.net.UnknownHostException) {
            Timber.w("⚠️ No internet connection or GitHub is unreachable: ${e.message}")
            null
        } catch (e: java.net.SocketTimeoutException) {
            Timber.w("⚠️ Connection timeout while downloading from GitHub: ${e.message}")
            null
        } catch (e: Exception) {
            Timber.e(e, "❌ Error downloading routes data from GitHub: ${e.javaClass.simpleName} - ${e.message}")
            null
        }
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
            
            Timber.d("Routes data cached successfully to ${cacheFile.absolutePath}")
        } catch (e: Exception) {
            Timber.e(e, "Error saving routes data to cache")
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
                Timber.d("💾 Cache file does not exist")
                return@withContext null
            }
            
            val jsonString = cacheFile.readText()
            
            // Проверяем, что JSON валиден и парсится без ошибок
            try {
                val testJson = JSONObject(jsonString)
                val testRoutes = testJson.getJSONArray("routes")
                val cachedVersion = testJson.optString("version", "unknown")
                Timber.d("💾 Cache JSON is valid with ${testRoutes.length()} routes, version: $cachedVersion")
            } catch (e: Exception) {
                Timber.e(e, "❌ Cache JSON is corrupted, clearing cache")
                clearCache()
                return@withContext null
            }
            
            jsonString
        } catch (e: Exception) {
            Timber.e(e, "❌ Error loading routes data from cache")
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
                Timber.i("✅ Loaded routes data from assets (${jsonString.length} bytes, ${testRoutes.length()} routes, version: $version)")
            } catch (e: Exception) {
                Timber.e(e, "❌ Assets JSON is invalid")
                return@withContext null
            }
            
            jsonString
        } catch (e: Exception) {
            Timber.e(e, "❌ Error loading routes data from assets - FILE NOT FOUND OR INACCESSIBLE")
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
            Timber.d("🔍 Checking if update is needed...")
            
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
                Timber.d("⚠️ Cannot check remote version (no internet?): ${e.message}")
                null
            }
            
            if (remoteJson == null) {
                Timber.d("⚠️ Remote version unavailable, will use cached data")
                return@withContext Pair(false, null)
            }
            
            val remoteVersion = extractVersionFromJson(remoteJson)
            
            // Если кэша нет - нужно обновление
            if (cachedVersion == null) {
                Timber.i("📦 No cached version, update needed")
                return@withContext Pair(true, remoteJson)
            }
            
            // Сравниваем версии
            val needsUpdate = remoteVersion != null && remoteVersion != cachedVersion
            if (needsUpdate) {
                Timber.i("🆕 Update available: $cachedVersion → $remoteVersion")
                return@withContext Pair(true, remoteJson)
            } else {
                Timber.d("✅ Already on latest version: $cachedVersion")
                return@withContext Pair(false, null)
            }
        } catch (e: Exception) {
            Timber.w(e, "⚠️ Error checking for updates: ${e.message}")
            Pair(false, null)
        }
    }
    
    /**
     * Получает актуальный JSON (из GitHub, кэша или assets)
     * 
     * НОВАЯ ЛОГИКА:
     * 1. При каждом запуске проверяет версию на GitHub (если есть интернет)
     * 2. Если версия новее - автоматически скачивает и сохраняет
     * 3. Если интернета нет - использует кэш
     * 4. Если кэша нет - fallback на assets
     * 
     * @param forceRefresh принудительная загрузка с GitHub (игнорирует проверку версии)
     * @return JSON строка или null при ошибке
     */
    private suspend fun getJsonString(forceRefresh: Boolean = false): String? {
        Timber.d("📥 getJsonString called with forceRefresh=$forceRefresh")
        
        // Если принудительное обновление - скачиваем без проверки версии
        if (forceRefresh) {
            Timber.d("🔄 Force refresh requested, attempting GitHub download...")
            val remoteJson = downloadRemoteJson()
            if (remoteJson != null) {
                Timber.i("✅ Force refresh successful, saving to cache")
                saveToCache(remoteJson)
                return remoteJson
            }
            Timber.w("⚠️ Force refresh failed, falling through to cache/assets")
        } else {
            // Обычная загрузка - проверяем нужно ли обновление
            Timber.d("🔍 Checking for updates...")
            
            val (needsUpdate, remoteJson) = shouldUpdate()
            if (needsUpdate && remoteJson != null) {
                Timber.i("✅ Update downloaded, saving to cache")
                saveToCache(remoteJson)
                return remoteJson
            } else if (needsUpdate) {
                Timber.w("⚠️ Update needed but download failed, will use cached data")
            }
        }
        
        // Пробуем загрузить из кэша
        Timber.d("💾 Loading from cache...")
        val cachedJson = loadFromCache()
        if (cachedJson != null) {
            Timber.i("✅ Using cached data")
            return cachedJson
        }
        Timber.d("⚠️ Cache not available, attempting GitHub download...")
        
        // Если в кэше нет, пробуем загрузить с GitHub
        val remoteJson = downloadRemoteJson()
        if (remoteJson != null) {
            Timber.i("✅ Downloaded from GitHub, saving to cache")
            saveToCache(remoteJson)
            return remoteJson
        }
        Timber.w("⚠️ GitHub download failed, falling back to assets...")
        
        // Если всё не удалось, используем assets
        val assetsJson = loadFromAssets()
        if (assetsJson != null) {
            Timber.i("✅ Using assets as fallback")
            return assetsJson
        }
        
        Timber.e("❌ ALL DATA SOURCES FAILED - NO DATA AVAILABLE!")
        return null
    }
    
    /**
     * Загружает маршруты
     * 
     * @param forceRefresh принудительная загрузка с GitHub
     * @return список маршрутов
     */
    suspend fun loadRoutes(forceRefresh: Boolean = false): List<BusRoute> = withContext(Dispatchers.IO) {
        Timber.d("🚌 loadRoutes called with forceRefresh=$forceRefresh")
        
        // Возвращаем кэшированные данные если не требуется обновление
        if (!forceRefresh && cachedRoutes != null) {
            Timber.d("✅ Returning cached routes in memory: ${cachedRoutes!!.size} routes")
            return@withContext cachedRoutes!!
        }
        
        try {
            Timber.d("📥 Getting JSON string...")
            val jsonString = getJsonString(forceRefresh)
            
            if (jsonString == null) {
                Timber.e("❌ getJsonString returned null - NO DATA AVAILABLE")
                return@withContext emptyList()
            }
            
            Timber.d("📝 Parsing JSON (${jsonString.length} bytes)...")
            val jsonObject = JSONObject(jsonString)
            val routesArray = jsonObject.getJSONArray("routes")
            
            Timber.d("🔍 Found ${routesArray.length()} routes in JSON, parsing...")
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
            Timber.i("✅ Successfully loaded ${routes.size} routes (forceRefresh=$forceRefresh)")
            
            routes
        } catch (e: Exception) {
            Timber.e(e, "❌ Error parsing routes JSON: ${e.javaClass.simpleName} - ${e.message}")
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
                        Timber.d("Loaded ${schedules.size} schedules for route $routeId")
                        return@withContext schedules
                    } else {
                        Timber.d("No schedules for route $routeId")
                        return@withContext null
                    }
                }
            }
            
            Timber.d("Route $routeId not found")
            null
        } catch (e: Exception) {
            Timber.e(e, "Error loading schedules for route $routeId")
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
            Timber.e(e, "Error getting data version")
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
            Timber.e(e, "Error getting remote data version")
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
            Timber.e(e, "Error getting last updated date")
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
            Timber.d("🔍 Checking for updates from GitHub...")
            val remoteJson = downloadRemoteJson()
            
            if (remoteJson == null) {
                Timber.w("❌ Failed to download remote JSON - no internet or server error")
                return@withContext false
            }
            
            Timber.d("✓ Successfully downloaded remote JSON")
            val cachedJson = loadFromCache()
            
            if (cachedJson == null) {
                // Если кэша нет, есть обновление
                Timber.i("📦 No cache found - update available")
                return@withContext true
            }
            
            val remoteVersion = JSONObject(remoteJson).optString("version", "unknown")
            val cachedVersion = JSONObject(cachedJson).optString("version", "unknown")
            
            val hasUpdate = remoteVersion.isNotEmpty() && 
                           cachedVersion.isNotEmpty() && 
                           remoteVersion != cachedVersion
            
            Timber.i("📊 Update check: remote=$remoteVersion, cached=$cachedVersion, hasUpdate=$hasUpdate")
            
            if (hasUpdate) {
                Timber.i("🎉 New version available: $remoteVersion (current: $cachedVersion)")
            } else {
                Timber.d("✓ Already on latest version: $cachedVersion")
            }
            
            hasUpdate
        } catch (e: Exception) {
            Timber.e(e, "❌ Error checking for updates")
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
                Timber.d("Cache file deleted")
            }
            
            val versionFile = getCacheVersionFile()
            if (versionFile.exists()) {
                versionFile.delete()
                Timber.d("Cache version file deleted")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error deleting cache file")
        }
    }
    
    /**
     * Очищает кэш маршрутов только в памяти (не удаляет файл)
     * 
     * Используется при обновлении данных, чтобы принудить перечитать файл
     */
    fun clearRoutesMemoryCache() {
        cachedRoutes = null
        Timber.d("Routes memory cache cleared (file preserved)")
    }
    
    /**
     * Очищает только кэш расписаний (чтобы они перезагрузились)
     */
    fun clearSchedulesCache() {
        cachedSchedules.clear()
        Timber.d("Schedules cache cleared")
    }
    
    /**
     * Очищает кэш расписания для конкретного маршрута
     */
    fun clearScheduleCache(routeId: String) {
        cachedSchedules.remove(routeId)
        Timber.d("Cleared schedule cache for route $routeId")
    }
}

