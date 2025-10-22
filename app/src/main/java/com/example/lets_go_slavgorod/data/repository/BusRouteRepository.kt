package com.example.lets_go_slavgorod.data.repository

import android.content.Context
import com.example.lets_go_slavgorod.core.search
import com.example.lets_go_slavgorod.data.local.JsonDataSource
import com.example.lets_go_slavgorod.data.model.BusRoute
import com.example.lets_go_slavgorod.data.model.BusSchedule
import com.example.lets_go_slavgorod.data.network.NetworkMonitor
import com.example.lets_go_slavgorod.data.remote.RemoteDataSource
import com.example.lets_go_slavgorod.domain.util.ScheduleUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

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
    val routes: StateFlow<List<BusRoute>> = _routes.asStateFlow()
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
        Timber.i("🚀 Repository initializing...")
        // Очищаем JSON кэш при инициализации
        jsonDataSource?.clearCache()
        Timber.d("🗑️ Cleared JSON cache on initialization")
        
        // Очищаем кэш памяти RemoteDataSource (но не файл кэша)
        // Файл кэша будет проверен на валидность при loadFromCache()
        remoteDataSource?.clearRoutesMemoryCache()
        Timber.d("🗑️ Cleared RemoteDataSource memory cache")
        
        // Запускаем асинхронную загрузку
        repositoryScope.launch {
            loadInitialRoutes()
        }
        Timber.i("⏳ Repository initialization started (async)")
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
                Timber.d("⚠️ Already initialized, skipping")
                return
            }
            
            Timber.i("🔄 Starting initial routes loading...")
            
            try {
                // Приоритет 1: Пытаемся загрузить из RemoteDataSource (умная загрузка)
                Timber.d("📡 Priority 1: Attempting RemoteDataSource (GitHub/Cache/Assets)...")
                val remoteRoutes = if (remoteDataSource != null) {
                    try {
                        remoteDataSource.loadRoutes(forceRefresh = false)
                    } catch (e: Exception) {
                        Timber.w(e, "❌ RemoteDataSource failed: ${e.message}")
                        null
                    }
                } else {
                    Timber.w("⚠️ RemoteDataSource is null (no context?)")
                    null
                }
                
                // Если удалённая загрузка успешна, используем её
                if (remoteRoutes != null && remoteRoutes.isNotEmpty()) {
                    Timber.i("✅ SUCCESS! Using routes from RemoteDataSource: ${remoteRoutes.size} routes")
                    remoteRoutes.forEach { route ->
                        routesCache[route.id] = route
                    }
                    _routes.value = remoteRoutes
                    isInitialized = true
                    Timber.i("🎉 Repository initialized successfully with ${remoteRoutes.size} routes")
                    return
                }
                
                // Приоритет 2: Пытаемся загрузить из JsonDataSource (assets)
                Timber.d("📄 Priority 2: RemoteDataSource failed, trying JsonDataSource (assets)...")
                val jsonRoutes = if (jsonDataSource != null) {
                    try {
                        jsonDataSource.loadRoutes()
                    } catch (e: Exception) {
                        Timber.w(e, "❌ JsonDataSource failed: ${e.message}")
                        null
                    }
                } else {
                    Timber.w("⚠️ JsonDataSource is null (no context?)")
                    null
                }
                
                // Если JSON загрузился успешно, используем его
                if (jsonRoutes != null && jsonRoutes.isNotEmpty()) {
                    Timber.i("✅ SUCCESS! Using routes from JsonDataSource: ${jsonRoutes.size} routes")
                    jsonRoutes.forEach { route ->
                        routesCache[route.id] = route
                    }
                    _routes.value = jsonRoutes
                    isInitialized = true
                    Timber.i("🎉 Repository initialized successfully with ${jsonRoutes.size} routes")
                    return
                }
                
                // Приоритет 3: Если всё не удалось - возвращаем пустой список
                Timber.e("❌❌❌ ALL DATA SOURCES FAILED - NO ROUTES LOADED ❌❌❌")
                Timber.e("Please check:")
                Timber.e("  1) Internet connection")
                Timber.e("  2) app/src/main/assets/routes_data.json exists")
                Timber.e("  3) GitHub repository accessible: https://github.com/VseMirka200/lets-go-slavgorod")
                _routes.value = emptyList()
                isInitialized = true
            
            } catch (e: Exception) {
                Timber.e(e, "❌ FATAL ERROR loading initial routes: ${e.javaClass.simpleName} - ${e.message}")
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
                
                // Обновляем виджет с новыми данными
                
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
    
    /**
     * Получает RemoteDataSource для доступа к метрикам
     */
    fun getRemoteDataSource(): RemoteDataSource? {
        return remoteDataSource
    }
}
