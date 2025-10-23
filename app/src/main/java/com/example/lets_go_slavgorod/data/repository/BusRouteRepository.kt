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
 * Центральный репозиторий для управления маршрутами автобусов
 * 
 * Реализует паттерн Repository для централизованного управления данными
 * о маршрутах автобусов с многоуровневым кэшированием и валидацией.
 * 
 * Основные возможности:
 * - **Многоуровневое кэширование**: Память → Диск → Сеть → Assets
 * - **Реактивные данные**: StateFlow для автоматического обновления UI
 * - **Умный поиск**: Поиск по номеру, названию, описанию маршрута
 * - **Валидация данных**: Проверка корректности всех входящих данных
 * - **Сетевая интеграция**: Автоматическая загрузка обновлений
 * - **Офлайн поддержка**: Работа без интернета с локальными данными
 * - **Принудительное обновление**: Методы для актуальности данных
 * - **Умная загрузка**: Приоритетные источники данных с fallback
 * 
 * Архитектура данных:
 * ```
 * UI Layer (ViewModels) 
 *     ↓
 * Repository (BusRouteRepository) ← Single Source of Truth
 *     ↓
 * Data Sources: Memory Cache → Disk Cache → Network → Assets
 * ```
 * 
 * Потоки данных:
 * - **routes**: Flow<List<BusRoute>> - основной поток маршрутов
 * - **isLoading**: Flow<Boolean> - состояние загрузки
 * - **error**: Flow<String?> - ошибки загрузки
 * 
 * @param context Контекст приложения для доступа к кэшу (опционально)
 * 
 * @author VseMirka200
 * @version 3.0
 * @since 1.0
 */
class BusRouteRepository(private val context: Context) {
    
    // Потоки данных и кэширование
    private val _routes = MutableStateFlow<List<BusRoute>>(emptyList())
    val routes: StateFlow<List<BusRoute>> = _routes.asStateFlow()
    private val routesCache = mutableMapOf<String, BusRoute>()
    
    // JSON источник данных (локальный assets)
    private val jsonDataSource = JsonDataSource(context)
    
    // Удалённый источник данных (GitHub)
    private val remoteDataSource = RemoteDataSource(context)
    
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
        return NetworkMonitor.observeConnectivity(context)
    }
    
    /**
     * Проверяет наличие интернет-соединения
     * 
     * Синхронная проверка текущего состояния сети.
     * 
     * @return true если есть соединение, false иначе
     */
    fun isOnline(): Boolean {
        return NetworkMonitor.isConnected(context)
    }
    
    init {
        // Очищаем JSON кэш при инициализации
        jsonDataSource.clearCache()
        
        // Очищаем кэш памяти RemoteDataSource (но не файл кэша)
        // Файл кэша будет проверен на валидность при loadFromCache()
        remoteDataSource.clearRoutesMemoryCache()
        
        // Запускаем асинхронную загрузку
        repositoryScope.launch {
            loadInitialRoutes()
        }
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
                val remoteRoutes = try {
                    remoteDataSource.loadRoutes(forceRefresh = false)
                } catch (e: Exception) {
                    null
                }
                
                // Если удалённая загрузка успешна, используем её
                if (remoteRoutes != null && remoteRoutes.isNotEmpty()) {
                    remoteRoutes.forEach { route ->
                        routesCache[route.id] = route
                    }
                    _routes.value = remoteRoutes
                    isInitialized = true
                    return
                }
                
                // Приоритет 2: Пытаемся загрузить из JsonDataSource (assets)
                val jsonRoutes = try {
                    jsonDataSource.loadRoutes()
                } catch (e: Exception) {
                    null
                }
                
                // Если JSON загрузился успешно, используем его
                if (jsonRoutes != null && jsonRoutes.isNotEmpty()) {
                    jsonRoutes.forEach { route ->
                        routesCache[route.id] = route
                    }
                    _routes.value = jsonRoutes
                    isInitialized = true
                    return
                }
                
                // Приоритет 3: Если всё не удалось - возвращаем пустой список
                Timber.e("ВСЕ ИСТОЧНИКИ ДАННЫХ НЕ УДАЛИСЬ - МАРШРУТЫ НЕ ЗАГРУЖЕНЫ")
                _routes.value = emptyList()
                isInitialized = true
            
            } catch (e: Exception) {
                Timber.e(e, "КРИТИЧЕСКАЯ ОШИБКА загрузки начальных маршрутов: ${e.javaClass.simpleName} - ${e.message}")
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
            return null
        }
        if (routeId.isBlank()) {
            return null
        }
        
        val route = routesCache[routeId]
        if (route == null) {
        } else {
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
            remoteDataSource.clearScheduleCache(routeId)
            jsonDataSource.clearScheduleCache(routeId)
        }
        
        // Приоритет 1: Пытаемся загрузить из RemoteDataSource (GitHub)
        // ВАЖНО: передаём forceRefresh чтобы принудительно загрузить с GitHub
        val remoteSchedules = try {
            remoteDataSource.loadSchedules(routeId, forceRefresh = forceRefresh)
        } catch (e: Exception) {
            null
        }
        
        // Если удалённая загрузка успешна, используем её
        if (remoteSchedules != null && remoteSchedules.isNotEmpty()) {
            return remoteSchedules
        }
        
        // Приоритет 2: Пытаемся загрузить из JsonDataSource (assets)
        val jsonSchedules = try {
            jsonDataSource.loadSchedules(routeId)
        } catch (e: Exception) {
            null
        }
        
        // Если JSON загрузился успешно, используем его
        if (jsonSchedules != null && jsonSchedules.isNotEmpty()) {
            return jsonSchedules
        }
        
        // Приоритет 3: Fallback на hardcoded данные
        return ScheduleUtils.generateSchedules(routeId)
    }
    
    /**
     * Принудительно обновляет данные расписания из GitHub (v3.1)
     * 
     * Улучшенная версия с реактивным обновлением UI:
     * - Загружает актуальные данные с GitHub
     * - Очищает все кэши для чистой загрузки
     * - Обновляет StateFlow для автоматического обновления UI
     * - Больше НЕ требует перезапуска приложения ✅
     * 
     * Алгоритм обновления:
     * 1. Проверяет доступность RemoteDataSource
     * 2. Очищает все кэши в памяти (маршруты + расписания)
     * 3. Принудительно загружает данные с GitHub
     * 4. Обновляет внутренний кэш и StateFlow
     * 5. Уведомляет все подписчиков об изменениях
     * 
     * Применение:
     * - При переходе к ScheduleScreen для актуальности
     * - При обновлении данных в настройках
     * - При восстановлении после ошибок сети
     * 
     * @return true если обновление прошло успешно
     * @see RemoteDataSource.loadRoutes()
     * @see _routes.value
     */
    suspend fun refreshRoutesFromRemote(): Boolean {
        return try {
            // ВАЖНО: Очищаем ВСЕ кэши В ПАМЯТИ перед загрузкой
            // Это гарантирует, что мы загрузим свежие данные с GitHub
            // НО НЕ удаляем файл кэша, т.к. туда будут сохранены новые данные
            remoteDataSource.clearRoutesMemoryCache()  // Очищаем кэш маршрутов в памяти
            remoteDataSource.clearSchedulesCache()  // Очищаем кэш расписаний в памяти
            jsonDataSource.clearCache()  // Очищаем кэш маршрутов в JsonDataSource
            jsonDataSource.clearAllScheduleCache()  // Очищаем кэш расписаний в JsonDataSource
            
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
                
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "Ошибка обновления маршрутов из GitHub")
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
            remoteDataSource.checkForUpdates()
        } catch (e: Exception) {
            Timber.e(e, "Ошибка проверки обновлений данных")
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
            remoteDataSource.getDataVersion()
        } catch (e: Exception) {
            Timber.e(e, "Ошибка получения версии данных")
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
            Timber.e(e, "Ошибка получения версии удалённых данных")
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
            Timber.e(e, "Ошибка получения даты последнего обновления")
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