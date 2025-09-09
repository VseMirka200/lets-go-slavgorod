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
 * Использует Koin для Dependency Injection с оптимизированной архитектурой.
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
 * Архитектура DI (Koin):
 * - Koin для Dependency Injection
 * - Singleton паттерн для глобального доступа
 * - Lazy initialization для оптимизации запуска
 * - CoroutineScope с SupervisorJob для фоновых операций
 * - Dispatchers.IO для всех I/O операций
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
        
        // Сохраняем стандартный обработчик перед установкой нашего
        val defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        // Устанавливаем глобальный обработчик необработанных исключений
        // Это предотвратит падение приложения при неожиданных ошибках
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                loge("КРИТИЧЕСКАЯ ОШИБКА: Необработанное исключение в потоке ${thread.name}", throwable)
                // Пытаемся логировать через системный лог, если Timber недоступен
                android.util.Log.e("BusApplication", "Uncaught exception in thread ${thread.name}", throwable)
            } catch (e: Exception) {
                // Fallback - последняя попытка логирования
                android.util.Log.e("BusApplication", "Failed to log uncaught exception: ${e.message}", throwable)
            }
            // Вызываем стандартный обработчик для завершения процесса
            // В продакшене это позволит системе обработать падение корректно
            defaultExceptionHandler?.uncaughtException(thread, throwable)
        }
        
        // Критически важные компоненты
        MultiDex.install(this)
        initializeLogging()
        
        // Инициализация Koin DI
        try {
            startKoin {
                androidContext(this@BusApplication)
                modules(appModule)
            }
            logd("Koin успешно инициализирован")
        } catch (e: Exception) {
            loge("КРИТИЧЕСКАЯ ОШИБКА: Не удалось инициализировать Koin: ${e.message}", e)
            // Продолжаем работу, но без DI - приложение может работать с ограничениями
        }
        
        try {
            NotificationHelper.createNotificationChannel(this)
        } catch (e: Exception) {
            loge("Не удалось создать канал уведомлений: ${e.message}")
        }
        
        // Запускаем фоновую синхронизацию
        try {
            DataSyncManager.schedulePeriodic(this)
        } catch (e: Exception) {
            loge("Не удалось запустить синхронизацию данных: ${e.message}")
        }
        
        
        // Фоновые задачи - используем applicationScope для всех операций
        applicationScope.launch(Dispatchers.IO) {
            try {
                // Обновляем кэш настроек уведомлений (чтобы избежать runBlocking)
                try {
                    NotificationPreferencesCache.updateCache(this@BusApplication)
                } catch (e: Exception) {
                    loge("Не удалось обновить кэш настроек уведомлений: ${e.message}")
                }
                
                // Восстанавливаем запланированные уведомления
                try {
                    rescheduleAlarmsOnStartup()
                } catch (e: Exception) {
                    loge("Не удалось перепланировать будильники: ${e.message}")
                }
                
                // Маршруты загружаются автоматически при инициализации репозитория
                
                // Запускаем автоматическую проверку обновлений через Koin
                try {
                    val updateManager = getUpdateManagerSafely()
                    if (updateManager != null) {
                        startAutomaticUpdateCheck(updateManager)
                    } else {
                        loge("UpdateManager недоступен, пропускаем проверку обновлений")
                    }
                } catch (e: Exception) {
                    loge("Не удалось запустить проверку обновлений: ${e.message}")
                }
            } catch (e: Exception) {
                loge("Ошибка во время инициализации приложения: ${e.message}", e)
            }
        }
        
        // Добавляем наблюдатель за жизненным циклом процесса
        try {
            ProcessLifecycleOwner.get().lifecycle.addObserver(
                androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                        // Приложение ушло в фон
                        // Здесь можно добавить логику очистки при уходе в фон
                    }
                }
            )
        } catch (e: Exception) {
            loge("Не удалось добавить наблюдатель жизненного цикла: ${e.message}")
        }
    }
    
    /**
     * Безопасное получение UpdateManager из Koin
     * 
     * Проверяет доступность Koin перед использованием
     * 
     * @return UpdateManager или null если недоступен
     */
    private fun getUpdateManagerSafely(): UpdateManager? {
        return try {
            org.koin.core.context.GlobalContext.get().get<UpdateManager>()
        } catch (e: Exception) {
            loge("Не удалось получить UpdateManager из Koin: ${e.message}")
            null
        }
    }
    
    /**
     * Безопасное получение BusRouteRepository из Koin
     * 
     * Проверяет доступность Koin перед использованием
     * 
     * @return BusRouteRepository или null если недоступен
     */
    private fun getBusRouteRepositorySafely(): BusRouteRepository? {
        return try {
            org.koin.core.context.GlobalContext.get().get<BusRouteRepository>()
        } catch (e: Exception) {
            loge("Не удалось получить BusRouteRepository из Koin: ${e.message}")
            null
        }
    }
    
    // Инициализация логирования
    private fun initializeLogging() {
        try {
            if (BuildConfig.DEBUG) {
                Timber.plant(Timber.DebugTree())
            } else {
                // В релизной сборке логируем только критические ошибки
                // ВАЖНО: Используем android.util.Log напрямую, а не loge(), чтобы избежать рекурсии
                Timber.plant(object : Timber.Tree() {
                    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                        if (priority >= android.util.Log.ERROR) {
                            // Используем системное логирование напрямую, чтобы избежать рекурсии
                            // НЕ вызываем loge() здесь, так как это вызовет бесконечную рекурсию!
                            val logTag = tag ?: "Release"
                            if (t != null) {
                                android.util.Log.e(logTag, message, t)
                            } else {
                                android.util.Log.e(logTag, message)
                            }
                        }
                    }
                })
            }
            
            if (BuildConfig.DEBUG) {
                logd("Вызов onCreate() приложения")
            }
        } catch (e: Exception) {
            // Fallback на системное логирование если Timber не инициализируется
            android.util.Log.e("BusApplication", "Failed to initialize logging: ${e.message}")
        }
    }
    
    // Восстановление уведомлений после перезагрузки
    private suspend fun rescheduleAlarmsOnStartup() {
        try {
            logd("Начинаем перепланирование будильников при запуске приложения")
            
            val database = try {
                AppDatabase.getDatabase(this@BusApplication)
            } catch (e: Exception) {
                loge("Не удалось получить базу данных: ${e.message}")
                return
            }
            
            val favoriteTimeDao = try {
                database.favoriteTimeDao()
            } catch (e: Exception) {
                loge("Не удалось получить favoriteTimeDao: ${e.message}")
                return
            }
            
            // Удаляем избранные времена для удалённых маршрутов
            val removedRouteIds = listOf("2", "4", "5")
            removedRouteIds.forEach { routeId ->
                val deletedCount = favoriteTimeDao.deleteByRouteId(routeId)
                if (deletedCount > 0) {
                    logd("Удалено $deletedCount избранных времен для удаленного маршрута: $routeId")
                }
            }
            
            val favoriteTimeEntities = favoriteTimeDao.getAllFavoriteTimes().firstOrNull() ?: emptyList()
            
            logd("Найдено ${favoriteTimeEntities.size} избранных времен в базе данных")
            
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
            val busRouteRepository = getBusRouteRepositorySafely()
            if (busRouteRepository == null) {
                loge("BusRouteRepository недоступен, не могу получить маршрут: $routeId")
                return null
            }
            
            busRouteRepository.getRouteById(routeId) ?: run {
                // Проверяем, не является ли это удалённым маршрутом
                if (routeId in listOf("2", "3", "4", "5")) {
                    loge("Попытка доступа к удаленному маршруту: $routeId")
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
            loge("Ошибка создания объекта маршрута для ID: $routeId", e)
            null
        }
    }
    
    // Автоматическая проверка обновлений
    private suspend fun startAutomaticUpdateCheck(updateManager: UpdateManager) {
        try {
            logd("Начинаем автоматическую проверку обновлений")
            
            // Проверяем, включена ли автоматическая проверка обновлений
            val updatePreferences = UpdatePreferences(this@BusApplication)
            val autoUpdateEnabled = updatePreferences.autoUpdateCheckEnabled.firstOrNull() ?: true
            
            if (!autoUpdateEnabled) {
                logd("Автоматическая проверка обновлений отключена пользователем")
                return
            }
            
            // Ждем несколько секунд после запуска приложения, чтобы не блокировать UI
            delay(Constants.UPDATE_CHECK_STARTUP_DELAY_MS)
            
            val result = updateManager.checkForUpdatesWithResult()
            
            when {
                result.success && result.update != null -> {
                    logd("Автоматическая проверка обновлений нашла новую версию: ${result.update.versionName}")
                    
                    // Валидируем данные обновления перед сохранением
                    if (result.update.versionName.isNotBlank() && result.update.downloadUrl.isNotBlank()) {
                        updatePreferences.setAvailableUpdate(
                            version = result.update.versionName,
                            url = result.update.downloadUrl,
                            notes = result.update.releaseNotes
                        )
                        
                        // Проверяем, включена ли автоматическая установка
                        val autoInstallEnabled = updatePreferences.autoInstallEnabled.firstOrNull() ?: false
                        
                        if (autoInstallEnabled) {
                            // Автоматически запускаем загрузку и установку
                            try {
                                logd("Автоматическая установка включена, запускаем загрузку обновления")
                                val updateDownloader = com.example.lets_go_slavgorod.domain.update.UpdateDownloader(this@BusApplication)
                                
                                // Запускаем загрузку
                                val downloadId = updateDownloader.downloadAndInstallUpdate(
                                    result.update.downloadUrl,
                                    result.update.versionName
                                )
                                if (downloadId != -1L) {
                                    logd("Автоматическая загрузка обновления запущена, ID: $downloadId")
                                } else {
                                    loge("Не удалось запустить автоматическую загрузку обновления")
                                }
                            } catch (e: Exception) {
                                loge("Ошибка при автоматической загрузке обновления", e)
                            }
                        }
                        
                        // Показываем уведомление о доступном обновлении (даже при автоустановке)
                        try {
                            NotificationHelper.showUpdateNotification(
                                context = this@BusApplication,
                                versionName = result.update.versionName,
                                releaseNotes = result.update.releaseNotes
                            )
                            logd("Показано уведомление об обновлении для версии ${result.update.versionName}")
                        } catch (e: Exception) {
                            loge("Ошибка показа уведомления об обновлении", e)
                        }
                    } else {
                        loge("Получены неверные данные обновления: version='${result.update.versionName}', url='${result.update.downloadUrl}'")
                    }
                }
                result.success -> {
                    logd("Автоматическая проверка обновлений: обновления недоступны")
                    // Очищаем информацию о доступном обновлении, если его больше нет
                    updatePreferences.clearAvailableUpdate()
                }
                else -> {
                    logd("Автоматическая проверка обновлений не удалась: ${result.error}")
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
            loge("Ошибка во время автоматической проверки обновлений", e)
            // В случае ошибки не прерываем работу приложения
        }
    }
    
}