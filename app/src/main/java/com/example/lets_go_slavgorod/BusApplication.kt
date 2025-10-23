package com.example.lets_go_slavgorod

import androidx.lifecycle.ProcessLifecycleOwner
import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication
import com.example.lets_go_slavgorod.core.Constants
import com.example.lets_go_slavgorod.core.createBusRoute
import com.example.lets_go_slavgorod.core.logd
import com.example.lets_go_slavgorod.core.loge
import com.example.lets_go_slavgorod.data.local.AppDatabase
import com.example.lets_go_slavgorod.data.local.NotificationPreferencesCache
import com.example.lets_go_slavgorod.data.local.UpdatePreferences
import com.example.lets_go_slavgorod.data.local.entity.FavoriteTimeEntity
import com.example.lets_go_slavgorod.data.model.BusRoute
import com.example.lets_go_slavgorod.data.model.FavoriteTime
import com.example.lets_go_slavgorod.data.notification.NotificationHelper
import com.example.lets_go_slavgorod.data.repository.BusRouteRepository
import com.example.lets_go_slavgorod.data.workers.DataSyncManager
import com.example.lets_go_slavgorod.di.appModule
import com.example.lets_go_slavgorod.domain.notification.AlarmScheduler
import com.example.lets_go_slavgorod.domain.update.UpdateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import timber.log.Timber

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
 * - Детальное логирование на русском языке
 * - Фоновая синхронизация данных через DataSyncManager
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
    
    // Единый ApplicationScope для предотвращения утечек памяти
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
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
     * 
     * Отменяет все корутины и освобождает ресурсы при завершении приложения.
     * Вызывается при onTerminate() и при остановке процесса.
     */
    private fun cleanupResources() {
        applicationScope.cancel()
    }
    
    
    // Инициализация приложения
    override fun onCreate() {
        super.onCreate()
        
        // Критически важные компоненты
        MultiDex.install(this)
        initializeLogging()
        
        // Инициализация Koin DI
        startKoin {
            androidContext(this@BusApplication)
            modules(appModule)
        }
        
        NotificationHelper.createNotificationChannel(this)
        
        // Запускаем фоновую синхронизацию
        DataSyncManager.schedulePeriodic(this)
        
        
        // Фоновые задачи - используем applicationScope для всех операций
        applicationScope.launch(Dispatchers.IO) {
            try {
                // Обновляем кэш настроек уведомлений (чтобы избежать runBlocking)
                try {
                    NotificationPreferencesCache.updateCache(this@BusApplication)
                } catch (e: Exception) {
                    loge("Failed to update notification preferences cache: ${e.message}")
                }
                
                // Восстанавливаем запланированные уведомления
                try {
                    rescheduleAlarmsOnStartup()
                } catch (e: Exception) {
                    loge("Failed to reschedule alarms: ${e.message}")
                }
                
                // Маршруты загружаются автоматически при инициализации репозитория
                
                // Запускаем автоматическую проверку обновлений через Koin
                try {
                    val updateManager = org.koin.core.context.GlobalContext.get().get<UpdateManager>()
                    startAutomaticUpdateCheck()
                } catch (e: Exception) {
                    loge("Failed to get UpdateManager from Koin: ${e.message}")
                }
            } catch (e: Exception) {
                loge("Error during app initialization: ${e.message}")
            }
        }
        
        // Добавляем наблюдатель за жизненным циклом процесса
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            androidx.lifecycle.LifecycleEventObserver { _, event ->
                if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                    // Приложение ушло в фон
                    // Здесь можно добавить логику очистки при уходе в фон
                }
            }
        )
    }
    
    // Инициализация логирования
    private fun initializeLogging() {
        try {
            if (BuildConfig.DEBUG) {
                Timber.plant(Timber.DebugTree())
            } else {
                // В релизной сборке логируем только критические ошибки
                Timber.plant(object : Timber.Tree() {
                    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                        if (priority >= android.util.Log.ERROR) {
                            try {
                                loge("Release", message, t)
                            } catch (e: Exception) {
                                // Fallback на системное логирование если Timber не работает
                                android.util.Log.e("Release", message, t)
                            }
                        }
                    }
                })
            }
            
            logd("Application onCreate() called")
        } catch (e: Exception) {
            // Fallback на системное логирование если Timber не инициализируется
            android.util.Log.e("BusApplication", "Failed to initialize logging: ${e.message}")
        }
    }
    
    // Восстановление уведомлений после перезагрузки
    private suspend fun rescheduleAlarmsOnStartup() {
        try {
            logd("Starting alarm rescheduling on app startup")
            
            val database = try {
                AppDatabase.getDatabase(this@BusApplication)
            } catch (e: Exception) {
                loge("Failed to get database: ${e.message}")
                return
            }
            
            val favoriteTimeDao = try {
                database.favoriteTimeDao()
            } catch (e: Exception) {
                loge("Failed to get favoriteTimeDao: ${e.message}")
                return
            }
            
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
                    } catch (e: Exception) {
                        Timber.e(e, "Ошибка перепланирования будильника для избранного времени: ${entity.id}")
                    }
                }
            
            Timber.i("Перепланировано $rescheduledCount из ${favoriteTimeEntities.size} избранных времен")
            
        } catch (e: Exception) {
            Timber.e(e, "Ошибка перепланирования будильников")
        }
    }
    
    // Получение маршрута по ID
    private fun getRouteById(routeId: String): BusRoute? {
        return try {
            // Используем Koin для получения репозитория
            val busRouteRepository = org.koin.core.context.GlobalContext.get().get<BusRouteRepository>()
            busRouteRepository.getRouteById(routeId) ?: run {
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
                    logd("Automatic update check failed: ${result.error}")
                    // Не очищаем кэш при ошибке, чтобы не потерять данные
                    // Это нормальное поведение - репозиторий может быть недоступен или приватным
                    // Приложение продолжает работать в обычном режиме
                    
                    // Если это ошибка 404 (репозиторий не найден), отключаем автоматическую проверку
                    if (result.error?.contains("404") == true || result.error?.contains("репозиторий не найден") == true) {
                        try {
                            updatePreferences.disableAutoUpdateCheck()
                            logd("Автоматическая проверка обновлений отключена из-за недоступности репозитория")
                        } catch (e: Exception) {
                            loge("Ошибка отключения автоматической проверки обновлений", e)
                        }
                    }
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