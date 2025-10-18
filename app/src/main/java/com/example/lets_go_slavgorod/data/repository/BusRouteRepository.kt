package com.example.lets_go_slavgorod.data.repository

import android.content.Context
import timber.log.Timber
import com.example.lets_go_slavgorod.data.local.JsonDataSource
import com.example.lets_go_slavgorod.data.remote.RemoteDataSource
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
    
    // JSON источник данных (локальный assets)
    private val jsonDataSource = context?.let { JsonDataSource(it) }
    
    // Удалённый источник данных (GitHub)
    private val remoteDataSource = context?.let { RemoteDataSource(it) }
    
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
     * 1. Попытка загрузки из RemoteDataSource (GitHub с fallback на кэш/assets)
     * 2. Если не удалось - попытка загрузки из JsonDataSource (assets)
     * 3. Fallback на hardcoded данные
     * 4. Валидация данных
     * 5. Кэширование валидных маршрутов
     */
    private suspend fun loadInitialRoutes() {
        loadMutex.withLock {
            if (isInitialized) {
                return
            }
            
            try {
                // Приоритет 1: Пытаемся загрузить из RemoteDataSource (умная загрузка)
                val remoteRoutes = if (remoteDataSource != null) {
                    try {
                        Timber.d("Attempting to load routes from RemoteDataSource")
                        remoteDataSource.loadRoutes(forceRefresh = false)
                    } catch (e: Exception) {
                        Timber.w(e, "Failed to load routes from RemoteDataSource")
                        null
                    }
                } else {
                    null
                }
                
                // Если удалённая загрузка успешна, используем её
                if (remoteRoutes != null && remoteRoutes.isNotEmpty()) {
                    Timber.i("Using routes from RemoteDataSource: ${remoteRoutes.size} routes")
                    remoteRoutes.forEach { route ->
                        routesCache[route.id] = route
                    }
                    _routes.value = remoteRoutes
                    isInitialized = true
                    return
                }
                
                // Приоритет 2: Пытаемся загрузить из JsonDataSource (assets)
                val jsonRoutes = if (jsonDataSource != null) {
                    try {
                        Timber.d("Attempting to load routes from JsonDataSource (assets)")
                        jsonDataSource.loadRoutes()
                    } catch (e: Exception) {
                        Timber.w(e, "Failed to load routes from JsonDataSource")
                        null
                    }
                } else {
                    null
                }
                
                // Если JSON загрузился успешно, используем его
                if (jsonRoutes != null && jsonRoutes.isNotEmpty()) {
                    Timber.i("Using routes from JsonDataSource: ${jsonRoutes.size} routes")
                    jsonRoutes.forEach { route ->
                        routesCache[route.id] = route
                    }
                    _routes.value = jsonRoutes
                    isInitialized = true
                    return
                }
                
                // Приоритет 3: Fallback на hardcoded данные
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
                ),
                createBusRoute(
                    id = "3",
                    routeNumber = "3",
                    name = "Автобус №3 Кольцевой",
                    description = "Автовокзал Славгород — Радиозавод / Мясокомбинат",
                    travelTime = "~25 минут",
                    pricePrimary = "38₽ город",
                    paymentMethods = "Наличный / Картой",
                    color = "#FF2E7D32"
                ),
                createBusRoute(
                    id = "4",
                    routeNumber = "4",
                    name = "Автобус №4 Пригородный",
                    description = "Славгород — Пригородные маршруты",
                    travelTime = "~20 минут",
                    pricePrimary = "38₽ город",
                    paymentMethods = "Наличный / Картой",
                    color = "#FFF57C00"
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
    suspend fun getSchedulesForRoute(routeId: String, forceRefresh: Boolean = false): List<BusSchedule> {
        // Валидация входных данных
        require(routeId.isNotBlank()) { "Route ID cannot be blank" }
        
        // Если требуется принудительное обновление, очищаем кэш для этого маршрута
        if (forceRefresh) {
            remoteDataSource?.clearScheduleCache(routeId)
            jsonDataSource?.clearScheduleCache(routeId)
            Timber.d("Force refresh: cleared schedule cache for route $routeId")
        }
        
        // Приоритет 1: Пытаемся загрузить из RemoteDataSource (GitHub)
        // ВАЖНО: передаём forceRefresh чтобы принудительно загрузить с GitHub
        val remoteSchedules = if (remoteDataSource != null) {
            try {
                remoteDataSource.loadSchedules(routeId, forceRefresh = forceRefresh)
            } catch (e: Exception) {
                Timber.w(e, "Failed to load schedules from RemoteDataSource for route $routeId")
                null
            }
        } else {
            null
        }
        
        // Если удалённая загрузка успешна, используем её
        if (remoteSchedules != null && remoteSchedules.isNotEmpty()) {
            Timber.d("Using schedules from RemoteDataSource for route $routeId: ${remoteSchedules.size} schedules")
            return remoteSchedules
        }
        
        // Приоритет 2: Пытаемся загрузить из JsonDataSource (assets)
        val jsonSchedules = if (jsonDataSource != null) {
            try {
                jsonDataSource.loadSchedules(routeId)
            } catch (e: Exception) {
                Timber.w(e, "Failed to load schedules from JsonDataSource for route $routeId")
                null
            }
        } else {
            null
        }
        
        // Если JSON загрузился успешно, используем его
        if (jsonSchedules != null && jsonSchedules.isNotEmpty()) {
            Timber.d("Using schedules from JsonDataSource for route $routeId: ${jsonSchedules.size} schedules")
            return jsonSchedules
        }
        
        // Приоритет 3: Fallback на hardcoded данные
        Timber.d("Using hardcoded schedules for route $routeId")
        return ScheduleUtils.generateSchedules(routeId)
    }
    
    /**
     * Принудительно обновляет данные расписания из GitHub (v2.1)
     * 
     * Улучшенная версия с реактивным обновлением UI:
     * - Загружает актуальные данные с GitHub
     * - Очищает все кэши для чистой загрузки
     * - Обновляет StateFlow для автоматического обновления UI
     * - Больше НЕ требует перезапуска приложения ✅
     * 
     * @return true если обновление прошло успешно
     */
    suspend fun refreshRoutesFromRemote(): Boolean {
        return try {
            if (remoteDataSource == null) {
                Timber.w("RemoteDataSource is null, cannot refresh")
                return false
            }
            
            Timber.i("Starting manual refresh from GitHub...")
            
            // ВАЖНО: Очищаем ВСЕ кэши В ПАМЯТИ перед загрузкой
            // Это гарантирует, что мы загрузим свежие данные с GitHub
            // НО НЕ удаляем файл кэша, т.к. туда будут сохранены новые данные
            remoteDataSource.clearRoutesMemoryCache()  // Очищаем кэш маршрутов в памяти
            remoteDataSource.clearSchedulesCache()  // Очищаем кэш расписаний в памяти
            jsonDataSource?.clearCache()  // Очищаем кэш маршрутов в JsonDataSource
            jsonDataSource?.clearAllScheduleCache()  // Очищаем кэш расписаний в JsonDataSource
            
            // Загружаем свежие данные с GitHub (forceRefresh = true принудительно скачивает)
            val routes = remoteDataSource.loadRoutes(forceRefresh = true)
            
            if (routes.isNotEmpty()) {
                // Обновляем кэш маршрутов в Repository
                routesCache.clear()
                routes.forEach { route ->
                    routesCache[route.id] = route
                }
                
                // Обновляем StateFlow - UI автоматически обновится! ✅
                _routes.value = routes
                
                Timber.i("Successfully refreshed ${routes.size} routes from GitHub")
                Timber.i("All caches cleared and refreshed with new data")
                Timber.i("StateFlow updated - all subscribed UI will refresh automatically")
                true
            } else {
                Timber.w("No routes received from GitHub")
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "Error refreshing routes from GitHub")
            false
        }
    }
    
    /**
     * Проверяет доступность обновлений на GitHub
     * 
     * @return true если доступна новая версия данных
     */
    suspend fun checkForDataUpdates(): Boolean {
        return try {
            if (remoteDataSource == null) {
                Timber.d("RemoteDataSource is null, no updates available")
                return false
            }
            
            remoteDataSource.checkForUpdates()
        } catch (e: Exception) {
            Timber.e(e, "Error checking for data updates")
            false
        }
    }
    
    /**
     * Получает версию данных из кэша
     * 
     * @return строка с версией или null
     */
    suspend fun getDataVersion(): String? {
        return try {
            remoteDataSource?.getDataVersion()
        } catch (e: Exception) {
            Timber.e(e, "Error getting data version")
            null
        }
    }
    
    /**
     * Получает версию данных напрямую с GitHub
     * 
     * Используется для показа версии в уведомлении о доступности обновления
     * 
     * @return строка с версией или null
     */
    suspend fun getRemoteDataVersion(): String? {
        return try {
            remoteDataSource?.getRemoteDataVersion()
        } catch (e: Exception) {
            Timber.e(e, "Error getting remote data version")
            null
        }
    }
    
    /**
     * Получает дату последнего обновления данных
     * 
     * @return строка с датой или null
     */
    suspend fun getDataLastUpdated(): String? {
        return try {
            remoteDataSource?.getLastUpdated()
        } catch (e: Exception) {
            Timber.e(e, "Error getting last updated date")
            null
        }
    }
}
