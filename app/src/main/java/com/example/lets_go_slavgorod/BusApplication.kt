package com.example.lets_go_slavgorod

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication
import timber.log.Timber
import com.example.lets_go_slavgorod.data.local.AppDatabase
import com.example.lets_go_slavgorod.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import com.example.lets_go_slavgorod.data.local.entity.FavoriteTimeEntity
import com.example.lets_go_slavgorod.data.local.UpdatePreferences
import com.example.lets_go_slavgorod.data.local.NotificationPreferencesCache
import com.example.lets_go_slavgorod.data.model.BusRoute
import com.example.lets_go_slavgorod.data.model.FavoriteTime
import com.example.lets_go_slavgorod.data.repository.BusRouteRepository
import com.example.lets_go_slavgorod.notifications.AlarmScheduler
import com.example.lets_go_slavgorod.notifications.NotificationHelper
import com.example.lets_go_slavgorod.updates.UpdateManager
import com.example.lets_go_slavgorod.utils.Constants
import com.example.lets_go_slavgorod.utils.createBusRoute
import com.example.lets_go_slavgorod.utils.logd
import com.example.lets_go_slavgorod.utils.loge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * Главный класс приложения "Поехали! Славгород"
 * 
 * Управляет глобальным состоянием приложения и инициализацией компонентов.
 * Наследуется от MultiDexApplication для поддержки большого количества методов.
 * Использует ручную Dependency Injection через lazy properties и фабрики.
 * 
 * Основные функции:
 * - Инициализация Timber для логирования (только в Debug режиме)
 * - Создание каналов уведомлений через NotificationHelper
 * - Восстановление запланированных уведомлений после перезагрузки
 * - Управление жизненным циклом фоновых задач через CoroutineScope
 * - Ленивая инициализация компонентов (database, repository, updateManager)
 * - Автоматическая проверка обновлений при запуске
 * - Мониторинг lifecycle приложения
 * 
 * Выполняется при:
 * - Первом запуске приложения
 * - Перезапуске после завершения процесса
 * - Обновлении приложения
 * - Восстановлении после перезагрузки устройства
 * 
 * Архитектура DI (без фреймворков):
 * - Singleton паттерн для глобального доступа
 * - Lazy initialization для оптимизации запуска
 * - CoroutineScope с SupervisorJob для фоновых операций
 * - Dispatchers.IO для всех I/O операций
 * - ViewModelFactory для передачи зависимостей в ViewModels
 * 
 * Оптимизации:
 * - Lazy database/repository - создаются только при необходимости
 * - Асинхронная проверка обновлений после задержки 5 секунд
 * - Корректная очистка ресурсов через ProcessLifecycleOwner
 * 
 * @author VseMirka200
 * @version 3.0
 * @since 1.0
 */
class BusApplication : MultiDexApplication() {
    
    // Область видимости корутин для фоновых задач
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    /**
     * Ленивая инициализация базы данных
     * Создается только при первом обращении для ускорения запуска
     */
    val database by lazy {
        logd("Initializing database...")
        AppDatabase.getDatabase(this)
    }
    
    /**
     * Ленивая инициализация репозитория
     * Создается только при первом обращении для ускорения запуска
     */
    val busRouteRepository by lazy {
        logd("Initializing repository...")
        BusRouteRepository(this)
    }
    
    /**
     * Ленивая инициализация менеджера обновлений
     * Создается только при первом обращении
     */
    val updateManager by lazy {
        logd("Initializing update manager...")
        UpdateManager(this)
    }
    
    /**
     * Очистка ресурсов
     * 
     * Примечание: onTerminate() вызывается только в эмуляторе, не на реальных устройствах.
     * Для правильной очистки ресурсов используем ProcessLifecycleOwner.
     */
    override fun onTerminate() {
        super.onTerminate()
        cleanupResources()
    }
    
    /**
     * Централизованная очистка ресурсов
     */
    private fun cleanupResources() {
        applicationScope.cancel()
        Timber.d("Application resources cleaned up")
    }
    
    /**
     * Инициализация приложения при запуске
     * 
     * Выполняется синхронно в главном потоке, поэтому критична оптимизация.
     * Тяжёлые операции (загрузка данных, проверка обновлений) запускаются асинхронно.
     * 
     * Порядок инициализации:
     * 1. MultiDex (для поддержки >65k методов)
     * 2. Logging (Timber для debug/release)
     * 3. Notification channels (Android требует создать заранее)
     * 4. Асинхронные операции (уведомления, обновления)
     * 5. Lifecycle observer (мониторинг состояния приложения)
     */
    override fun onCreate() {
        super.onCreate()
        
        // 1. MultiDex - необходим для большого количества методов в проекте
        MultiDex.install(this)
        
        // 2. Logging - инициализируем Timber для всего приложения
        initializeLogging()
        
        // 3. Koin - инициализируем Dependency Injection
        initializeKoin()
        
        // 4. Notification channels - создаём каналы для уведомлений (требование Android 8+)
        NotificationHelper.createNotificationChannel(this)
        
        // 4. Фоновые задачи - запускаются асинхронно чтобы не блокировать UI
        applicationScope.launch {
            // 4.1. Обновляем кэш настроек уведомлений (чтобы избежать runBlocking в AlarmScheduler)
            NotificationPreferencesCache.updateCache(this@BusApplication)
            
            // 4.2. Восстанавливаем запланированные уведомления после перезагрузки устройства
            rescheduleAlarmsOnStartup()
            
            // 4.3. Маршруты загружаются автоматически при инициализации BusRouteRepository
            //      (ленивая инициализация через lazy - создаётся при первом обращении)
            
            // 4.4. Запускаем автоматическую проверку обновлений APK приложения (с GitHub)
            startAutomaticUpdateCheck()
            
            // 4.5. Проверяем обновления данных расписания в фоне (routes_data.json на GitHub)
            checkScheduleUpdatesInBackground()
        }
        
        // 5. Lifecycle observer - мониторинг состояния приложения (foreground/background)
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            androidx.lifecycle.LifecycleEventObserver { _, event ->
                when (event) {
                    androidx.lifecycle.Lifecycle.Event.ON_STOP -> {
                        // Приложение ушло в фон - можно очистить временные данные
                        Timber.d("Application stopped (moved to background)")
                        // Потенциальные оптимизации: очистка кэша изображений, паузы анимаций и т.д.
                    }
                    androidx.lifecycle.Lifecycle.Event.ON_START -> {
                        // Приложение вернулось на передний план
                        Timber.d("Application started (moved to foreground)")
                    }
                    else -> { /* Остальные события игнорируем */ }
                }
            }
        )
    }
    
    /**
     * Инициализация системы логирования Timber
     * 
     * Debug build: Полное логирование всех уровней (verbose, debug, info, warn, error)
     * Release build: Только критические ошибки (error level)
     * 
     * Timber используется вместо стандартного Log для:
     * - Автоматического определения тега
     * - Условного логирования в зависимости от build type
     * - Централизованного управления логами
     */
    private fun initializeLogging() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            // В релизной сборке логируем только критические ошибки
            Timber.plant(object : Timber.Tree() {
                override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                    if (priority >= android.util.Log.ERROR) {
                        loge("Release", message, t)
                    }
                }
            })
        }
        
        logd("Application onCreate() called")
    }
    
    /**
     * Инициализация Koin Dependency Injection
     * 
     * Настраивает Koin для автоматического инжекта зависимостей.
     * Используется легковесный Koin вместо Hilt для:
     * - Простоты настройки
     * - Отсутствия проблем совместимости с KSP
     * - Runtime DI без кодогенерации
     */
    private fun initializeKoin() {
        startKoin {
            // Логирование Koin (только в debug)
            if (BuildConfig.DEBUG) {
                androidLogger(Level.ERROR) // Только ошибки в логе
            }
            
            // Контекст Android
            androidContext(this@BusApplication)
            
            // Модули зависимостей
            modules(appModules)
        }
        
        logd("Koin DI initialized")
    }
    
    /**
     * Восстановление запланированных уведомлений после перезагрузки устройства
     * 
     * Android очищает все AlarmManager задачи при перезагрузке, поэтому необходимо
     * восстановить все активные уведомления из базы данных.
     * 
     * Процесс:
     * 1. Загружает все избранные времена из БД
     * 2. Фильтрует только активные (isActive = true)
     * 3. Для каждого получает актуальную информацию о маршруте
     * 4. Создаёт объект FavoriteTime с полными данными
     * 5. Перепланирует уведомление через AlarmScheduler
     * 
     * Обработка ошибок:
     * - Если маршрут не найден - использует "N/A" и "Неизвестный маршрут"
     * - Если ошибка при планировании конкретного уведомления - пропускает его и продолжает
     * - Если критическая ошибка - логирует и завершает без краша
     * 
     * Вызывается из: onCreate() в фоновом потоке (Dispatchers.IO)
     */
    private suspend fun rescheduleAlarmsOnStartup() {
        try {
            logd("Starting alarm rescheduling on app startup")
            
            val database = AppDatabase.getDatabase(this@BusApplication)
            val favoriteTimeDao = database.favoriteTimeDao()
            
            // Очистка избранных времён для несуществующих маршрутов выполняется автоматически при валидации
            
            val favoriteTimeEntities = favoriteTimeDao.getAllFavoriteTimes().firstOrNull() ?: emptyList()
            
            logd("Found ${favoriteTimeEntities.size} favorite times in database")
            
            var rescheduledCount = 0
            favoriteTimeEntities
                .filter { it.isActive }
                .forEach { entity: FavoriteTimeEntity ->
                    try {
                        val route = getRouteByIdSuspend(entity.routeId)
                        val favoriteTime = FavoriteTime(
                            id = entity.id,
                            routeId = entity.routeId,
                            routeNumber = route?.routeNumber ?: "N/A",
                            routeName = route?.name ?: "Неизвестный маршрут",
                            stopName = entity.stopName,
                            departureTime = entity.departureTime,
                            dayOfWeek = entity.dayOfWeek,
                            departurePoint = entity.departurePoint,
                            addedDate = entity.addedDate,
                            isActive = entity.isActive
                        )
                        
                        AlarmScheduler.scheduleAlarm(this@BusApplication, favoriteTime)
                        rescheduledCount++
                        Timber.d("Rescheduled alarm for favorite time: ${entity.id}")
                    } catch (e: Exception) {
                        Timber.e(e, "Error rescheduling alarm for favorite time: ${entity.id}")
                    }
                }
            
            Timber.i("Successfully rescheduled $rescheduledCount out of ${favoriteTimeEntities.size} favorite times on startup")
            
        } catch (e: Exception) {
            Timber.e(e, "Error during alarm rescheduling on startup")
        }
    }
    
    /**
     * Получение маршрута по ID в suspend контексте
     * 
     * Оптимизация: использует кэш из BusRouteRepository вместо прямого обращения к БД.
     * Безопасный вызов с переключением на IO диспетчер.
     * 
     * @param routeId уникальный идентификатор маршрута
     * @return объект BusRoute или null если маршрут не найден
     */
    private suspend fun getRouteByIdSuspend(routeId: String): BusRoute? {
        return try {
            // Используем withContext для безопасного вызова
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                busRouteRepository.getRouteById(routeId)
            }
        } catch (e: Exception) {
            loge("Error getting route for ID: $routeId", e)
            null
        }
    }
    
    /**
     * Автоматическая проверка обновлений APK приложения
     * 
     * Проверяет наличие новых версий приложения на GitHub Releases.
     * Работает в фоне и не блокирует UI.
     * 
     * Логика работы:
     * 1. Проверяет настройку автоматических обновлений (может быть отключена пользователем)
     * 2. Ждёт 5 секунд после запуска (чтобы не мешать инициализации UI)
     * 3. Запрашивает информацию о последнем релизе через GitHub API
     * 4. Сравнивает с текущей версией приложения
     * 5. Если доступна новая версия:
     *    - Валидирует данные обновления
     *    - Сохраняет в UpdatePreferences
     *    - Показывает уведомление пользователю
     * 6. Обновляет время последней проверки
     * 
     * Безопасность:
     * - Все ошибки обрабатываются без краша приложения
     * - Валидация данных перед сохранением
     * - Очистка устаревших данных если обновлений нет
     * 
     * @see UpdateManager.checkForUpdatesWithResult для логики проверки
     * @see NotificationHelper.showUpdateNotification для отображения уведомления
     */
    private suspend fun startAutomaticUpdateCheck() {
        try {
            logd("Starting automatic update check")
            
            // Проверяем, включена ли автоматическая проверка обновлений
            val updatePreferences = UpdatePreferences(this@BusApplication)
            val autoUpdateEnabled = updatePreferences.autoUpdateCheckEnabled.firstOrNull() ?: true
            
            if (!autoUpdateEnabled) {
                logd("Automatic update check is disabled by user")
                return
            }
            
            // Ждем несколько секунд после запуска приложения, чтобы не блокировать UI
            delay(Constants.UPDATE_CHECK_STARTUP_DELAY_MS)
            
            val updateManager = UpdateManager(this@BusApplication)
            val result = updateManager.checkForUpdatesWithResult()
            
            when {
                result.success && result.update != null -> {
                    logd("Automatic update check found new version: ${result.update.versionName}")
                    
                    // Валидируем данные обновления перед сохранением
                    if (result.update.versionName.isNotBlank() && result.update.downloadUrl.isNotBlank()) {
                        updatePreferences.setAvailableUpdate(
                            version = result.update.versionName,
                            url = result.update.downloadUrl,
                            notes = result.update.releaseNotes
                        )
                        
                        // Показываем уведомление о доступном обновлении
                        try {
                            NotificationHelper.showUpdateNotification(
                                context = this@BusApplication,
                                versionName = result.update.versionName,
                                releaseNotes = result.update.releaseNotes
                            )
                            logd("Update notification shown for version ${result.update.versionName}")
                        } catch (e: Exception) {
                            loge("Error showing update notification", e)
                        }
                    } else {
                        loge("Invalid update data received: version='${result.update.versionName}', url='${result.update.downloadUrl}'")
                    }
                }
                result.success -> {
                    logd("Automatic update check: no updates available")
                    // Очищаем информацию о доступном обновлении, если его больше нет
                    updatePreferences.clearAvailableUpdate()
                }
                else -> {
                    loge("Automatic update check failed: ${result.error}")
                    // Не очищаем кэш при ошибке, чтобы не потерять данные
                }
            }
            
            // Обновляем время последней проверки
            updatePreferences.setLastUpdateCheckTime(System.currentTimeMillis())
            
        } catch (e: Exception) {
            loge("Error during automatic update check", e)
            // В случае ошибки не прерываем работу приложения
        }
    }
    
    /**
     * Фоновая проверка обновлений данных расписания
     * 
     * Проверяет наличие новой версии файла routes_data.json на GitHub.
     * Позволяет обновлять расписания автобусов без выпуска нового APK.
     * 
     * Процесс:
     * 1. Ждёт 5 секунд после запуска приложения
     * 2. Вызывает BusRouteRepository.checkForDataUpdates()
     * 3. Сравнивает версию в кэше с версией на GitHub
     * 4. Если есть обновление:
     *    - Получает новую версию
     *    - Показывает уведомление пользователю
     *    - Пользователь может обновить через Settings
     * 
     * Особенности:
     * - Не блокирует UI
     * - Не скачивает данные автоматически (только уведомление)
     * - Пользователь сам решает когда обновить
     * - Работает даже если приложение в фоне
     * 
     * @see BusRouteRepository.checkForDataUpdates для логики сравнения версий
     * @see NotificationHelper.showScheduleUpdateNotification для уведомления
     */
    private suspend fun checkScheduleUpdatesInBackground() {
        try {
            logd("Checking for schedule data updates in background")
            
            // Ждем немного после запуска приложения
            delay(Constants.UPDATE_CHECK_STARTUP_DELAY_MS)
            
            logd("Starting background check for schedule updates...")
            val hasUpdates = busRouteRepository.checkForDataUpdates()
            logd("Update check result: hasUpdates = $hasUpdates")
            
            if (hasUpdates) {
                logd("Schedule data update available on GitHub")
                
                // Получаем версию обновления с GitHub
                val newVersion = busRouteRepository.getRemoteDataVersion()
                logd("New version available: $newVersion")
                
                // Показываем уведомление пользователю
                try {
                    NotificationHelper.showScheduleUpdateNotification(
                        context = this@BusApplication,
                        dataVersion = newVersion
                    )
                    logd("✅ Schedule update notification shown for version: $newVersion")
                } catch (e: Exception) {
                    loge("❌ Error showing schedule update notification", e)
                }
            } else {
                logd("✓ Schedule data is up to date - no notification needed")
            }
        } catch (e: Exception) {
            loge("Error checking for schedule updates", e)
        }
    }
}