package com.example.lets_go_slavgorod

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication
import timber.log.Timber
import com.example.lets_go_slavgorod.data.local.AppDatabase
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
 * Главный класс приложения "Let's Go Slavgorod" с Hilt DI
 * 
 * Управляет глобальным состоянием приложения и инициализацией компонентов.
 * Наследуется от MultiDexApplication для поддержки большого количества методов.
 * Использует Hilt для Dependency Injection.
 * 
 * Основные функции:
 * - Инициализация Timber для логирования
 * - Создание каналов уведомлений
 * - Восстановление запланированных уведомлений при запуске
 * - Управление жизненным циклом фоновых задач
 * - Инициализация репозиториев и менеджеров
 * 
 * Выполняется при:
 * - Первом запуске приложения
 * - Перезапуске после завершения процесса
 * - Обновлении приложения
 * 
 * Архитектура:
 * - Singleton паттерн для глобального доступа
 * - CoroutineScope для фоновых операций
 * - SupervisorJob для изоляции ошибок
 * - Dispatchers.IO для I/O операций
 * 
 * @author VseMirka200
 * @version 2.0
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
    
    // Инициализация приложения
    override fun onCreate() {
        super.onCreate()
        
        // Критически важные компоненты
        MultiDex.install(this)
        initializeLogging()
        NotificationHelper.createNotificationChannel(this)
        
        // Фоновые задачи
        applicationScope.launch {
            // Обновляем кэш настроек уведомлений (чтобы избежать runBlocking)
            NotificationPreferencesCache.updateCache(this@BusApplication)
            
            // Восстанавливаем запланированные уведомления
            rescheduleAlarmsOnStartup()
            
            // Маршруты загружаются автоматически при инициализации репозитория
            
            // Запускаем автоматическую проверку обновлений
            startAutomaticUpdateCheck()
        }
        
        // Добавляем наблюдатель за жизненным циклом процесса
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            androidx.lifecycle.LifecycleEventObserver { _, event ->
                if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                    // Приложение ушло в фон
                    Timber.d("Application stopped (moved to background)")
                    // Здесь можно добавить логику очистки при уходе в фон
                }
            }
        )
    }
    
    // Инициализация логирования
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
    
    // Восстановление уведомлений после перезагрузки
    private suspend fun rescheduleAlarmsOnStartup() {
        try {
            logd("Starting alarm rescheduling on app startup")
            
            val database = AppDatabase.getDatabase(this@BusApplication)
            val favoriteTimeDao = database.favoriteTimeDao()
            
            // Удаляем избранные времена для удалённых маршрутов
            val removedRouteIds = listOf("2", "4", "5")
            removedRouteIds.forEach { routeId ->
                val deletedCount = favoriteTimeDao.deleteByRouteId(routeId)
                if (deletedCount > 0) {
                    logd("Removed $deletedCount favorite times for deleted route: $routeId")
                }
            }
            
            val favoriteTimeEntities = favoriteTimeDao.getAllFavoriteTimes().firstOrNull() ?: emptyList()
            
            logd("Found ${favoriteTimeEntities.size} favorite times in database")
            
            var rescheduledCount = 0
            favoriteTimeEntities
                .filter { it.isActive }
                .forEach { entity: FavoriteTimeEntity ->
                    try {
                        val route = getRouteById(entity.routeId)
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
    
    // Получение маршрута по ID
    private fun getRouteById(routeId: String): BusRoute? {
        return try {
            val repository = BusRouteRepository()
            repository.getRouteById(routeId) ?: run {
                // Проверяем, не является ли это удалённым маршрутом
                if (routeId in listOf("2", "3", "4", "5")) {
                    loge("Attempted to access removed route: $routeId")
                    return null
                }
                
                // Создаем fallback объект только для допустимых маршрутов
                createBusRoute(
                    id = routeId,
                    routeNumber = routeId,
                    name = "Автобус №$routeId",
                    description = "Маршрут",
                    travelTime = "~40 минут",
                    pricePrimary = "38₽ город / 55₽ межгород",
                    paymentMethods = "Наличный / Картой",
                    color = Constants.DEFAULT_ROUTE_COLOR
                )
            }
        } catch (e: Exception) {
            loge("Error creating route object for ID: $routeId", e)
            null
        }
    }
    
    // Автоматическая проверка обновлений
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
            
            // Ждем 5 секунд после запуска приложения, чтобы не блокировать UI
            delay(5000)
            
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
}