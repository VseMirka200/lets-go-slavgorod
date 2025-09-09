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
import kotlin.math.pow

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
     * Загружает начальные маршруты с оптимизацией и улучшенной обработкой ошибок
     * 
     * Логика загрузки:
     * 1. Если есть интернет - попытка загрузки из RemoteDataSource (GitHub) с forceRefresh = true
     * 2. Если нет интернета или загрузка не удалась - попытка из RemoteDataSource с кэшем
     * 3. Если не удалось - попытка загрузки из JsonDataSource (assets)
     * 4. Fallback на hardcoded данные
     * 5. Валидация данных
     * 6. Кэширование валидных маршрутов
     * 
     * ИСПРАВЛЕНО: 
     * - При первой загрузке всегда пытается загрузить из GitHub, если есть интернет
     * - Проверяет наличие маршрутов перед возвратом (если пусто - продолжает загрузку)
     * - При повторном запуске после очистки памяти перезагружает маршруты
     */
    private suspend fun loadInitialRoutes(forceReload: Boolean = false) {
        loadMutex.withLock {
            // Если уже инициализирован и есть маршруты - не перезагружаем
            if (isInitialized && !forceReload && _routes.value.isNotEmpty()) {
                Timber.d("Маршруты уже загружены (${_routes.value.size} шт.), пропускаем загрузку")
                return
            }
            
            // Если маршруты пусты даже после инициализации - пробуем загрузить снова
            if (isInitialized && _routes.value.isEmpty() && !forceReload) {
                Timber.w("Маршруты пусты, но флаг инициализации установлен. Пробуем загрузить снова...")
                // Сбрасываем флаг для повторной попытки
                isInitialized = false
            }
            
            try {
                // Проверяем наличие интернета
                val hasInternet = isOnline()
                
                // Приоритет 1: Если есть интернет - принудительно загружаем из GitHub
                if (hasInternet) {
                    Timber.d("Интернет доступен, загружаем маршруты из GitHub...")
                    val remoteRoutes = retryWithBackoff(
                        maxRetries = 3,
                        initialDelay = 1000L
                    ) {
                        remoteDataSource.loadRoutes(forceRefresh = forceReload || !isInitialized)
                    }
                    
                    // Если удалённая загрузка успешна, используем её
                    if (remoteRoutes != null && remoteRoutes.isNotEmpty()) {
                        Timber.d("Успешно загружено ${remoteRoutes.size} маршрутов из GitHub")
                        remoteRoutes.forEach { route ->
                            routesCache[route.id] = route
                        }
                        _routes.value = remoteRoutes
                        isInitialized = true
                        return
                    } else {
                        Timber.w("Не удалось загрузить маршруты из GitHub, пробуем кэш...")
                    }
                }
                
                // Приоритет 2: Пытаемся загрузить из RemoteDataSource (может использовать кэш)
                val remoteRoutes = retryWithBackoff(
                    maxRetries = 2,
                    initialDelay = 500L
                ) {
                    remoteDataSource.loadRoutes(forceRefresh = false)
                }
                
                // Если удалённая загрузка успешна, используем её
                if (remoteRoutes != null && remoteRoutes.isNotEmpty()) {
                    Timber.d("Загружено ${remoteRoutes.size} маршрутов из кэша/RemoteDataSource")
                    remoteRoutes.forEach { route ->
                        routesCache[route.id] = route
                    }
                    _routes.value = remoteRoutes
                    isInitialized = true
                    return
                }
                
                // Приоритет 3: Пытаемся загрузить из JsonDataSource (assets)
                Timber.d("Пробуем загрузить маршруты из assets...")
                val jsonRoutes = try {
                    jsonDataSource.loadRoutes()
                } catch (e: Exception) {
                    Timber.w(e, "Не удалось загрузить маршруты из assets")
                    null
                }
                
                // Если JSON загрузился успешно, используем его
                if (jsonRoutes != null && jsonRoutes.isNotEmpty()) {
                    Timber.d("Загружено ${jsonRoutes.size} маршрутов из assets")
                    jsonRoutes.forEach { route ->
                        routesCache[route.id] = route
                    }
                    _routes.value = jsonRoutes
                    isInitialized = true
                    return
                }
                
                // Приоритет 4: Если всё не удалось - возвращаем пустой список
                Timber.e("ВСЕ ИСТОЧНИКИ ДАННЫХ НЕ УДАЛИСЬ - МАРШРУТЫ НЕ ЗАГРУЖЕНЫ")
                // НЕ устанавливаем isInitialized = true если маршруты пусты!
                // Это позволит попытаться загрузить снова при следующем запуске
                if (_routes.value.isEmpty()) {
                    Timber.w("Маршруты пусты, НЕ устанавливаем флаг инициализации для повторной попытки")
                    // Не устанавливаем isInitialized, чтобы можно было попробовать снова
                } else {
                    isInitialized = true
                }
            
            } catch (e: Exception) {
                Timber.e(e, "КРИТИЧЕСКАЯ ОШИБКА загрузки начальных маршрутов: ${e.javaClass.simpleName} - ${e.message}")
                // При ошибке тоже не устанавливаем флаг, если маршруты пусты
                if (_routes.value.isEmpty()) {
                    Timber.w("Ошибка загрузки, маршруты пусты - не устанавливаем флаг инициализации")
                } else {
                    isInitialized = true
                }
            }
        }
    }
    
    /**
     * Принудительно перезагружает маршруты (для случаев после очистки памяти)
     */
    suspend fun forceReloadRoutes() {
        Timber.d("Принудительная перезагрузка маршрутов...")
        isInitialized = false
        routesCache.clear()
        _routes.value = emptyList()
        loadInitialRoutes(forceReload = true)
    }
    
    /**
     * Получает маршрут по идентификатору из кэша
     * 
     * Оптимизация: использует локальный кэш в памяти для быстрого доступа O(1).
     * Кэш автоматически обновляется при загрузке маршрутов из любого источника.
     * 
     * Валидация:
     * - Проверяет, что routeId не null и не пустой
     * - Возвращает null для невалидных идентификаторов
     * 
     * Производительность:
     * - O(1) доступ к кэшу через HashMap
     * - Не выполняет запросы к базе данных или сети
     * 
     * @param routeId идентификатор маршрута (например, "102", "1")
     * @return объект BusRoute или null если маршрут не найден или невалидный ID
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
     * Выполняет поиск маршрутов по текстовому запросу
     * 
     * Реализует нечеткий поиск (fuzzy search) по следующим полям:
     * - Номер маршрута (routeNumber)
     * - Название маршрута (name)
     * - Описание маршрута (description)
     * - Детали направления (directionDetails)
     * 
     * Особенности:
     * - Поиск без учета регистра (case-insensitive)
     * - Поддержка частичного совпадения (contains)
     * - Если запрос пустой, возвращает все маршруты
     * - Результаты сортируются по релевантности
     * 
     * Валидация:
     * - Проверяет, что query не null
     * - Пустой запрос возвращает все маршруты
     * 
     * @param query поисковый запрос (например, "102", "Яровое", "Вокзал")
     * @return отсортированный список найденных маршрутов (пустой список если ничего не найдено)
     * @throws IllegalArgumentException если query равен null
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
     * Получает все доступные маршруты из текущего состояния
     * 
     * Возвращает актуальный список маршрутов из StateFlow.
     * Данные автоматически обновляются при загрузке из любого источника
     * (GitHub, локальный кэш, assets).
     * 
     * Производительность:
     * - O(1) доступ к StateFlow.value
     * - Не выполняет запросы к сети или базе данных
     * - Данные всегда актуальны благодаря реактивному StateFlow
     * 
     * @return неизменяемый список всех загруженных маршрутов
     *         (может быть пустым при первой загрузке)
     */
    fun getAllRoutes(): List<BusRoute> = _routes.value
    
    /**
     * Получает расписание отправлений для конкретного маршрута
     * 
     * Реализует многоуровневую стратегию загрузки с приоритетами:
     * 1. **Приоритет 1**: RemoteDataSource (GitHub) - актуальные данные
     * 2. **Приоритет 2**: JsonDataSource (assets) - локальные данные
     * 3. **Приоритет 3**: ScheduleUtils.generateSchedules() - fallback данные
     * 
     * Кэширование:
     * - При forceRefresh=true очищает все кэши для маршрута
     * - Использует кэш для быстрого доступа при повторных запросах
     * - Кэш автоматически обновляется при успешной загрузке
     * 
     * Валидация:
     * - Проверяет, что routeId не пустой
     * - Обрабатывает ошибки загрузки с graceful degradation
     * 
     * @param routeId ID маршрута (например, "102", "1")
     * @param forceRefresh если true, принудительно обновляет данные, игнорируя кэш
     * @return список расписаний для маршрута (не пустой, минимум fallback данные)
     * @throws IllegalArgumentException если routeId пустой
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
     * Проверяет доступность обновлений данных на GitHub
     * 
     * Выполняет проверку наличия новой версии данных расписания
     * на удаленном сервере. Сравнивает версию локальных данных
     * с версией на GitHub.
     * 
     * Использование:
     * - Автоматическая проверка обновлений при запуске
     * - Ручная проверка из настроек приложения
     * - Уведомление пользователя о доступности обновлений
     * 
     * Обработка ошибок:
     * - Возвращает false при ошибках сети или парсинга
     * - Логирует ошибки для отладки
     * - Не прерывает работу приложения при неудаче
     * 
     * @return true если доступна новая версия данных, false в противном случае
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
    
    /**
     * Retry механизм с экспоненциальной задержкой для надежной загрузки данных
     * 
     * Реализует стратегию повторных попыток (exponential backoff) для обработки
     * временных сетевых ошибок и повышения надежности загрузки данных.
     * 
     * Алгоритм:
     * - Попытка 1: немедленное выполнение
     * - Попытка 2: задержка = initialDelay (например, 1000ms)
     * - Попытка 3: задержка = initialDelay * 2^1 (2000ms)
     * - Попытка N: задержка = initialDelay * 2^(N-2)
     * 
     * Преимущества:
     * - Устойчивость к временным сетевым сбоям
     * - Оптимизация нагрузки на сервер (увеличение задержки)
     * - Детальное логирование всех попыток
     * 
     * Использование:
     * - Загрузка маршрутов из GitHub
     * - Загрузка расписаний из удаленных источников
     * - Любые сетевые операции, требующие надежности
     * 
     * @param maxRetries максимальное количество попыток (по умолчанию 3)
     * @param initialDelay начальная задержка в миллисекундах (по умолчанию 1000ms)
     * @param operation suspend функция для выполнения (может вернуть null при ошибке)
     * @return результат операции или null если все попытки исчерпаны
     */
    private suspend fun <T> retryWithBackoff(
        maxRetries: Int = 3,
        initialDelay: Long = 1000L,
        operation: suspend () -> T?
    ): T? {
        var lastException: Exception? = null
        
        repeat(maxRetries) { attempt ->
            try {
                return operation()
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries - 1) {
                    val delay = initialDelay * (2.0.pow(attempt.toDouble())).toLong()
                    kotlinx.coroutines.delay(delay)
                }
            }
        }
        
        lastException?.let { 
            Timber.e(it, "Все попытки retry исчерпаны после $maxRetries попыток")
        }
        return null
    }
}