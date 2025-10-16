package com.example.lets_go_slavgorod.data.repository

import android.content.Context
import timber.log.Timber
import com.example.lets_go_slavgorod.data.local.JsonDataSource
import com.example.lets_go_slavgorod.data.model.BusRoute
import com.example.lets_go_slavgorod.data.model.BusSchedule
import com.example.lets_go_slavgorod.utils.Constants
import com.example.lets_go_slavgorod.utils.ScheduleUtils
import com.example.lets_go_slavgorod.utils.createBusRoute
import com.example.lets_go_slavgorod.utils.logd
import com.example.lets_go_slavgorod.utils.loge
import com.example.lets_go_slavgorod.utils.search
import com.example.lets_go_slavgorod.utils.NetworkMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Репозиторий для управления маршрутами автобусов
 * 
 * Центральный источник данных о маршрутах автобусов в приложении.
 * Реализует паттерн Repository для абстракции источников данных.
 * 
 * Основные функции:
 * - Загрузка и кэширование маршрутов
 * - Поиск маршрутов по различным критериям
 * - Валидация данных маршрутов
 * - Управление состоянием через StateFlow
 * - Интеграция с локальным кэшем
 * 
 * Архитектура:
 * - Single Source of Truth для данных маршрутов
 * - Двухуровневое кэширование (память + диск)
 * - Реактивное обновление через Flow
 * - Валидация всех входящих данных
 * 
 * @param context контекст приложения для доступа к кэшу (опционально)
 * 
 * @author VseMirka200
 * @version 2.0
 * @since 1.0
 */
class BusRouteRepository(private val context: Context? = null) {
    
    // Потоки данных и кэширование
    private val _routes = MutableStateFlow<List<BusRoute>>(emptyList())
    private val routesCache = mutableMapOf<String, BusRoute>()
    
    // JSON источник данных
    private val jsonDataSource = context?.let { JsonDataSource(it) }
    
    // Scope для асинхронной загрузки
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // Mutex для синхронизации первичной загрузки
    private val loadMutex = Mutex()
    private var isInitialized = false
    
    /**
     * Наблюдает за состоянием сетевого подключения
     * 
     * Предоставляет реактивный Flow для отслеживания доступности интернета.
     * Используется для показа индикаторов offline режима в UI.
     * 
     * @return Flow<Boolean> true если есть соединение, false иначе
     */
    fun observeConnectivity(): Flow<Boolean> {
        return if (context != null) {
            NetworkMonitor.observeConnectivity(context)
        } else {
            // Если контекст не доступен, считаем что всегда online
            kotlinx.coroutines.flow.flowOf(true)
        }
    }
    
    /**
     * Проверяет наличие интернет-соединения
     * 
     * Синхронная проверка текущего состояния сети.
     * 
     * @return true если есть соединение, false иначе
     */
    fun isOnline(): Boolean {
        return if (context != null) {
            NetworkMonitor.isConnected(context)
        } else {
            true // По умолчанию считаем что online
        }
    }
    
    init {
        Timber.d("Repository initializing...")
        // Очищаем JSON кэш при инициализации
        jsonDataSource?.clearCache()
        Timber.d("Cleared JSON cache on initialization")
        
        // Запускаем асинхронную загрузку
        repositoryScope.launch {
            loadInitialRoutes()
        }
        Timber.d("Repository initialization started (async)")
    }
    
    /**
     * Загружает начальные маршруты с оптимизацией
     * 
     * Логика загрузки:
     * 1. Попытка загрузки из JSON (если доступен)
     * 2. Fallback на hardcoded данные
     * 3. Валидация данных
     * 4. Кэширование валидных маршрутов
     */
    private suspend fun loadInitialRoutes() {
        loadMutex.withLock {
            if (isInitialized) {
                return
            }
            
            try {
                // Пытаемся загрузить из JSON
                val jsonRoutes = if (jsonDataSource != null) {
                    try {
                        jsonDataSource.loadRoutes()
                    } catch (e: Exception) {
                        Timber.w(e, "Failed to load routes from JSON, using hardcoded data")
                        null
                    }
                } else {
                    null
                }
                
                // Если JSON загрузился успешно, используем его
                if (jsonRoutes != null && jsonRoutes.isNotEmpty()) {
                    Timber.d("Using routes from JSON: ${jsonRoutes.size} routes")
                    jsonRoutes.forEach { route ->
                        routesCache[route.id] = route
                    }
                    _routes.value = jsonRoutes
                    isInitialized = true
                    return
                }
                
                // Fallback: используем hardcoded данные
                logd("Using hardcoded routes data")
            
            // Загружаем базовые маршруты
            val sampleRoutes = listOfNotNull(
                createBusRoute(
                    id = "102",
                    routeNumber = "102",
                    name = "Автобус №102",
                    description = "Рынок (Славгород) — Ст. Зори (Яровое)",
                    travelTime = "~40 минут",
                    pricePrimary = "38₽ город / 55₽ межгород",
                    paymentMethods = "Нал. / Безнал.",
                    color = Constants.DEFAULT_ROUTE_COLOR
                ),
                createBusRoute(
                    id = "102B",
                    routeNumber = "102Б",
                    name = "Автобус 102Б",
                    description = "Рынок (Славгород) — Ст. Зори (Яровое)",
                    travelTime = "~40 минут",
                    pricePrimary = "38₽ город / 55₽ межгород",
                    paymentMethods = "Нал. / Безнал.",
                    color = Constants.DEFAULT_ROUTE_COLOR_GREEN
                ),
                createBusRoute(
                    id = "1",
                    routeNumber = "1",
                    name = "Автобус №1",
                    description = "Маршрут вокзал — совхоз",
                    travelTime = "~24 минуты",
                    pricePrimary = "38₽ город",
                    paymentMethods = "Нал. / Безнал.",
                    color = Constants.DEFAULT_ROUTE_COLOR_ALT
                )
            )
            
            Timber.d("Created sample routes: ${sampleRoutes.size} routes")
            sampleRoutes.forEach { route ->
                Timber.d("Sample route: ${route.id} - ${route.name}")
                if (route.id == "102B") {
                    Timber.d("102B route details: id=${route.id}, routeNumber=${route.routeNumber}, name=${route.name}, isValid=${route.isValid()}")
                }
            }
            
            // Валидируем и кэшируем маршруты
            val validRoutes = sampleRoutes.filter { route ->
                val isValid = route.isValid()
                if (!isValid) {
                    loge("Invalid route found: ${route.id}")
                }
                isValid
            }
            
            // Кэшируем валидные маршруты для быстрого доступа
            validRoutes.forEach { route ->
                routesCache[route.id] = route
                if (route.id == "102B") {
                    Timber.d("102B route cached successfully")
                }
            }
            
            _routes.value = validRoutes
            Timber.d("Routes set to _routes: ${validRoutes.size} routes")
            validRoutes.forEach { route ->
                Timber.d("Route in _routes: ${route.id} - ${route.name}")
                if (route.id == "102B") {
                    Timber.d("102B route in final _routes list!")
                }
            }
            
            logd("Loaded ${validRoutes.size} valid routes")
            isInitialized = true
            
            } catch (e: Exception) {
                loge("Error loading initial routes", e)
                _routes.value = emptyList()
                isInitialized = true // Даже при ошибке считаем инициализированным
            }
        }
    }
    
    /**
     * Получает маршрут по идентификатору
     * 
     * Оптимизация: использует локальный кэш для быстрого доступа
     * 
     * @param routeId идентификатор маршрута
     * @return объект BusRoute или null если не найден
     */
    fun getRouteById(routeId: String?): BusRoute? {
        // Валидация входных данных
        if (routeId == null) {
            Timber.w("getRouteById called with null routeId")
            return null
        }
        if (routeId.isBlank()) {
            Timber.w("getRouteById called with blank routeId")
            return null
        }
        
        val route = routesCache[routeId]
        if (route == null) {
            Timber.w("Route not found in cache for routeId: $routeId")
        } else {
            Timber.d("Route found in cache: ${route.id} - ${route.name}")
        }
        return route
    }
    
    /**
     * Выполняет поиск маршрутов по запросу
     * 
     * @param query поисковый запрос
     * @return список найденных маршрутов
     */
    fun searchRoutes(query: String): List<BusRoute> {
        // Валидация входных данных
        requireNotNull(query) { "Search query cannot be null" }
        
        // Если запрос пустой, возвращаем все маршруты
        if (query.isBlank()) {
            return getAllRoutes()
        }
        
        return _routes.value.search(query)
    }
    
    /**
     * Получает все доступные маршруты
     * 
     * @return список всех маршрутов
     */
    fun getAllRoutes(): List<BusRoute> = _routes.value
    
    /**
     * Получает расписание для маршрута
     * 
     * Логика загрузки:
     * 1. Попытка загрузки из JSON (если доступен)
     * 2. Fallback на hardcoded данные из ScheduleUtils
     * 
     * @param routeId ID маршрута
     * @return список расписаний для маршрута
     */
    suspend fun getSchedulesForRoute(routeId: String): List<BusSchedule> {
        // Валидация входных данных
        require(routeId.isNotBlank()) { "Route ID cannot be blank" }
        
        // Пытаемся загрузить из JSON
        val jsonSchedules = if (jsonDataSource != null) {
            try {
                jsonDataSource.loadSchedules(routeId)
            } catch (e: Exception) {
                Timber.w(e, "Failed to load schedules from JSON for route $routeId, using hardcoded data")
                null
            }
        } else {
            null
        }
        
        // Если JSON загрузился успешно, используем его
        if (jsonSchedules != null && jsonSchedules.isNotEmpty()) {
            Timber.d("Using schedules from JSON for route $routeId: ${jsonSchedules.size} schedules")
            return jsonSchedules
        }
        
        // Fallback: используем hardcoded данные
        Timber.d("Using hardcoded schedules for route $routeId")
        return ScheduleUtils.generateSchedules(routeId)
    }
    
    // Функция forceRefreshRoutes удалена - обновление маршрутов больше не требуется

}
