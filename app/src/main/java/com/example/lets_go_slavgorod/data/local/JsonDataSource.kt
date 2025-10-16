package com.example.lets_go_slavgorod.data.local

import android.content.Context
import com.example.lets_go_slavgorod.data.model.BusRoute
import com.example.lets_go_slavgorod.data.model.BusSchedule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.util.Calendar

/**
 * Источник данных для загрузки маршрутов из JSON файла
 * 
 * Загружает данные о маршрутах и расписаниях из assets/routes_data.json.
 * Предоставляет кэшированный доступ к данным.
 * 
 * @param context контекст приложения
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 2.0
 */
class JsonDataSource(private val context: Context) {
    
    private var cachedRoutes: List<BusRoute>? = null
    private val cachedSchedules = mutableMapOf<String, List<BusSchedule>>()
    
    /**
     * Загружает маршруты из JSON файла
     * 
     * @return список маршрутов с полными данными
     */
    suspend fun loadRoutes(): List<BusRoute> = withContext(Dispatchers.IO) {
        // Возвращаем кэшированные данные если есть
        cachedRoutes?.let { return@withContext it }
        
        try {
            val jsonString = context.assets.open("routes_data.json")
                .bufferedReader()
                .use { it.readText() }
            
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
            Timber.d("Loaded ${routes.size} routes from JSON")
            
            routes
        } catch (e: Exception) {
            Timber.e(e, "Error loading routes from JSON, falling back to hardcoded data")
            // Fallback на старый метод если JSON не загрузился
            loadFallbackRoutes()
        }
    }
    
    /**
     * Загружает расписание для конкретного маршрута из JSON
     * 
     * @param routeId ID маршрута
     * @return список расписаний или null если данных нет в JSON
     */
    suspend fun loadSchedules(routeId: String): List<BusSchedule>? = withContext(Dispatchers.IO) {
        // Проверяем кэш
        cachedSchedules[routeId]?.let { return@withContext it }
        
        try {
            val jsonString = context.assets.open("routes_data.json")
                .bufferedReader()
                .use { it.readText() }
            
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
                        Timber.d("Loaded ${schedules.size} schedules from JSON for route $routeId")
                        return@withContext schedules
                    } else {
                        Timber.d("No schedules in JSON for route $routeId")
                        return@withContext null
                    }
                }
            }
            
            Timber.d("Route $routeId not found in JSON")
            null
        } catch (e: Exception) {
            Timber.e(e, "Error loading schedules from JSON for route $routeId")
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
     * Fallback метод загрузки из hardcoded данных
     */
    private fun loadFallbackRoutes(): List<BusRoute> {
        Timber.w("Using fallback hardcoded routes")
        return emptyList() // Будет использован существующий ScheduleUtils
    }
    
    /**
     * Очищает кэш маршрутов (для обновлений данных)
     */
    fun clearCache() {
        cachedRoutes = null
        Timber.d("Routes cache cleared")
    }
    
    /**
     * Очищает кэш расписания для конкретного маршрута
     */
    fun clearScheduleCache(routeId: String) {
        cachedSchedules.remove(routeId)
        Timber.d("Schedule cache cleared for route $routeId")
    }
    
    /**
     * Очищает весь кэш расписания
     */
    fun clearAllScheduleCache() {
        cachedSchedules.clear()
        Timber.d("All schedule cache cleared")
    }
}

