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
     * Загружает JSON с удалённого сервера (GitHub)
     * 
     * @return содержимое JSON файла или null при ошибке
     */
    private suspend fun downloadRemoteJson(): String? = withContext(Dispatchers.IO) {
        try {
            Timber.d("Downloading routes data from GitHub: ${Constants.REMOTE_JSON_URL}")
            
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
                
                Timber.i("Successfully downloaded routes data from GitHub (${jsonString.length} bytes)")
                jsonString
            } else {
                Timber.w("Failed to download from GitHub: HTTP $responseCode")
                connection.disconnect()
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Error downloading routes data from GitHub")
            null
        }
    }
    
    /**
     * Сохраняет JSON в кэш-файл
     */
    private suspend fun saveToCache(jsonString: String) = withContext(Dispatchers.IO) {
        try {
            val cacheFile = getCacheFile()
            cacheFile.writeText(jsonString)
            Timber.d("Routes data cached successfully to ${cacheFile.absolutePath}")
        } catch (e: Exception) {
            Timber.e(e, "Error saving routes data to cache")
        }
    }
    
    /**
     * Читает JSON из кэш-файла
     */
    private suspend fun loadFromCache(): String? = withContext(Dispatchers.IO) {
        try {
            val cacheFile = getCacheFile()
            if (cacheFile.exists()) {
                val jsonString = cacheFile.readText()
                Timber.d("Loaded routes data from cache (${jsonString.length} bytes)")
                jsonString
            } else {
                Timber.d("Cache file does not exist")
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Error loading routes data from cache")
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
            Timber.d("Loaded routes data from assets (fallback)")
            jsonString
        } catch (e: Exception) {
            Timber.e(e, "Error loading routes data from assets")
            null
        }
    }
    
    /**
     * Получает актуальный JSON (из GitHub, кэша или assets)
     * 
     * Приоритет загрузки:
     * 1. Попытка загрузить с GitHub
     * 2. Если не удалось - загрузка из кэша
     * 3. Если кэша нет - загрузка из assets
     * 
     * @param forceRefresh принудительная загрузка с GitHub
     * @return JSON строка или null при ошибке
     */
    private suspend fun getJsonString(forceRefresh: Boolean = false): String? {
        // Если принудительное обновление, пробуем загрузить с GitHub
        if (forceRefresh) {
            val remoteJson = downloadRemoteJson()
            if (remoteJson != null) {
                saveToCache(remoteJson)
                return remoteJson
            }
        }
        
        // Пробуем загрузить из кэша
        val cachedJson = loadFromCache()
        if (cachedJson != null) {
            return cachedJson
        }
        
        // Если в кэше нет, пробуем загрузить с GitHub
        val remoteJson = downloadRemoteJson()
        if (remoteJson != null) {
            saveToCache(remoteJson)
            return remoteJson
        }
        
        // Если всё не удалось, используем assets
        return loadFromAssets()
    }
    
    /**
     * Загружает маршруты
     * 
     * @param forceRefresh принудительная загрузка с GitHub
     * @return список маршрутов
     */
    suspend fun loadRoutes(forceRefresh: Boolean = false): List<BusRoute> = withContext(Dispatchers.IO) {
        // Возвращаем кэшированные данные если не требуется обновление
        if (!forceRefresh && cachedRoutes != null) {
            return@withContext cachedRoutes!!
        }
        
        try {
            val jsonString = getJsonString(forceRefresh) ?: return@withContext emptyList()
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
            Timber.i("Loaded ${routes.size} routes (forceRefresh=$forceRefresh)")
            
            routes
        } catch (e: Exception) {
            Timber.e(e, "Error parsing routes JSON")
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

