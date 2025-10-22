package com.example.lets_go_slavgorod.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import androidx.work.*
import com.example.lets_go_slavgorod.MainActivity
import com.example.lets_go_slavgorod.R
import com.example.lets_go_slavgorod.core.Constants
import com.example.lets_go_slavgorod.data.local.JsonDataSource
import com.example.lets_go_slavgorod.data.model.BusRoute
import com.example.lets_go_slavgorod.data.model.BusSchedule
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Базовый класс для всех виджетов маршрутов
 * 
 * Предоставляет общую функциональность для виджетов автобусных маршрутов:
 * - Обновление содержимого виджета с расписанием
 * - Обработка кликов для навигации к маршруту
 * - Периодическое обновление через WorkManager
 * - Определение следующего времени отправления
 * 
 * Архитектура:
 * - Использует JsonDataSource для загрузки данных
 * - WorkManager для периодических обновлений
 * - Coroutines для асинхронных операций
 * - RemoteViews для обновления UI
 * 
 * v2.0 Changes:
 * - Добавлена поддержка навигации к конкретным маршрутам
 * - Улучшена обработка Intent с флагами
 * - Добавлено логирование для отладки
 * - Оптимизирована навигация с задержками
 * 
 * @author VseMirka200
 * @version 2.0
 * @since 1.0
 */
abstract class BaseRouteWidgetProvider : AppWidgetProvider() {

    // Оптимизированный CoroutineScope с SupervisorJob для изоляции ошибок
    protected val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    abstract val routeId: String
    abstract val layoutId: Int
    abstract val workName: String
    abstract val leftDirection: String
    abstract val rightDirection: String

    /**
     * Вызывается при обновлении виджета
     * 
     * Обновляет все экземпляры виджета данного типа на главном экране.
     * Запускает периодическое обновление через WorkManager.
     * 
     * @param context Контекст приложения
     * @param appWidgetManager Менеджер виджетов
     * @param appWidgetIds Массив ID виджетов для обновления
     */
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Timber.d("${javaClass.simpleName}: onUpdate called for ${appWidgetIds.size} widgets")
        
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    /**
     * Вызывается при создании первого экземпляра виджета
     * 
     * Запускает периодическое обновление виджета через WorkManager.
     * 
     * @param context Контекст приложения
     */
    override fun onEnabled(context: Context) {
        Timber.d("${javaClass.simpleName}: onEnabled")
        super.onEnabled(context)
        startPeriodicUpdate(context)
    }

    /**
     * Вызывается при удалении последнего экземпляра виджета
     * 
     * Останавливает периодическое обновление виджета и отменяет корутины.
     * 
     * @param context Контекст приложения
     */
    override fun onDisabled(context: Context) {
        Timber.d("${javaClass.simpleName}: onDisabled")
        super.onDisabled(context)
        stopPeriodicUpdate(context)
        
        // Явная отмена корутин для предотвращения утечек памяти
        widgetScope.cancel()
        Timber.d("${javaClass.simpleName}: Widget scope cancelled")
    }

    /**
     * Запускает периодическое обновление виджета через WorkManager
     * 
     * Создает уникальную задачу для каждого типа виджета с интервалом 15 минут.
     * Использует ограничения для оптимизации батареи.
     * 
     * @param context Контекст приложения
     */
    private fun startPeriodicUpdate(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(true)
            .build()

        val workRequest = PeriodicWorkRequest.Builder(
            BaseWidgetUpdateWorker::class.java,
            Constants.WIDGET_UPDATE_INTERVAL_MINUTES, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setInputData(workDataOf("route_id" to routeId))
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                workName,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
    }

    /**
     * Останавливает периодическое обновление виджета
     * 
     * Отменяет уникальную задачу WorkManager для данного типа виджета.
     * 
     * @param context Контекст приложения
     */
    private fun stopPeriodicUpdate(context: Context) {
        WorkManager.getInstance(context)
            .cancelUniqueWork(workName)
    }

    /**
     * Обновляет содержимое виджета
     * 
     * Загружает данные маршрута, определяет следующее время отправления
     * и обновляет UI виджета. Настраивает обработчик кликов для навигации.
     * 
     * @param context Контекст приложения
     * @param appWidgetManager Менеджер виджетов
     * @param appWidgetId ID виджета для обновления
     */
    protected fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        Timber.d("${javaClass.simpleName}: updateAppWidget for widget $appWidgetId, routeId: $routeId")
        
        val views = RemoteViews(context.packageName, layoutId)
        
        // Настраиваем клик для открытия конкретного маршрута
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("navigate_to_route", routeId)
            putExtra("FROM_WIDGET", true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, routeId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
        
        Timber.d("${javaClass.simpleName}: Set click intent for routeId: $routeId")
        
        // Загружаем данные в фоновом режиме
        widgetScope.launch {
            try {
                // Используем JsonDataSource для загрузки данных
                val jsonDataSource = JsonDataSource(context)
                val routeNextTimes = getNextDepartureTimes(listOf(routeId), jsonDataSource)
                
                // Обновляем UI в главном потоке
                ContextCompat.getMainExecutor(context).execute {
                    updateWidgetContent(context, views, routeNextTimes)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error updating ${javaClass.simpleName} widget")
                ContextCompat.getMainExecutor(context).execute {
                    updateWidgetContent(context, views, emptyList())
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }
    }

    /**
     * Получает следующее время отправления для маршрутов
     * 
     * Загружает расписание маршрутов и определяет следующее время отправления
     * для каждого направления (левое и правое).
     * 
     * @param routeIds Список ID маршрутов для обработки
     * @param jsonDataSource Источник данных JSON
     * @return Список пар (маршрут, время отправления с направлением)
     */
    private suspend fun getNextDepartureTimes(
        routeIds: List<String>,
        jsonDataSource: JsonDataSource
    ): List<Pair<BusRoute, String>> {
        val currentTime = System.currentTimeMillis()
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val currentDayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        
        val routeNextTimes = mutableListOf<Pair<BusRoute, String>>()
        
        for (routeId in routeIds) {
            try {
                val routes = jsonDataSource.loadRoutes()
                val route = routes.find { it.id == routeId }
                if (route == null) {
                    Timber.w("Route $routeId not found")
                    continue
                }
                
                val schedules = jsonDataSource.loadSchedules(route.id)
                if (schedules != null && schedules.isNotEmpty()) {
                    val todaySchedules = schedules.filter { it.dayOfWeek == currentDayOfWeek }
                    
                    if (todaySchedules.isNotEmpty()) {
                        val schedulesByDirection = todaySchedules.groupBy { it.departurePoint }
                        
                        Timber.d("${javaClass.simpleName}: Found ${schedulesByDirection.size} directions: ${schedulesByDirection.keys}")
                        
                        val leftTimes = mutableListOf<Long>()
                        val rightTimes = mutableListOf<Long>()
                        
                        for ((direction, directionSchedules) in schedulesByDirection) {
                            val nextTime = findNextDepartureTime(directionSchedules, currentTime)
                            if (nextTime != null) {
                                val timeStr = timeFormat.format(Date(nextTime))
                                
                                when {
                                    isLeftDirection(direction) -> {
                                        leftTimes.add(nextTime)
                                        routeNextTimes.add(Pair(route, "1: $timeStr"))
                                        Timber.d("${javaClass.simpleName}: Direction: $direction -> $leftDirection, Time: $timeStr")
                                    }
                                    isRightDirection(direction) -> {
                                        rightTimes.add(nextTime)
                                        routeNextTimes.add(Pair(route, "2: $timeStr"))
                                        Timber.d("${javaClass.simpleName}: Direction: $direction -> $rightDirection, Time: $timeStr")
                                    }
                                    else -> {
                                        if (leftTimes.isEmpty()) {
                                            leftTimes.add(nextTime)
                                            routeNextTimes.add(Pair(route, "1: $timeStr"))
                                            Timber.d("${javaClass.simpleName}: Direction: $direction -> $leftDirection (fallback), Time: $timeStr")
                                        } else if (rightTimes.isEmpty()) {
                                            rightTimes.add(nextTime)
                                            routeNextTimes.add(Pair(route, "2: $timeStr"))
                                            Timber.d("${javaClass.simpleName}: Direction: $direction -> $rightDirection (fallback), Time: $timeStr")
                                        }
                                    }
                                }
                            }
                        }
                        
                        if (schedulesByDirection.size == 1 && routeNextTimes.isEmpty()) {
                            val singleDirection = schedulesByDirection.values.first()
                            val nextTime = findNextDepartureTime(singleDirection, currentTime)
                            if (nextTime != null) {
                                val timeStr = timeFormat.format(Date(nextTime))
                                routeNextTimes.add(Pair(route, "1: $timeStr"))
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading schedules for route $routeId")
            }
        }
        
        return routeNextTimes.sortedBy { (_, timeStr) -> 
            try {
                val time = timeStr.substringAfter(": ").trim()
                timeFormat.parse(time)?.time ?: 0L
            } catch (e: Exception) {
                0L
            }
        }
    }

    /**
     * Определяет, является ли направление левым
     * 
     * @param direction Строка с описанием направления
     * @return true, если направление левое
     */
    protected abstract fun isLeftDirection(direction: String): Boolean
    
    /**
     * Определяет, является ли направление правым
     * 
     * @param direction Строка с описанием направления
     * @return true, если направление правое
     */
    protected abstract fun isRightDirection(direction: String): Boolean

    /**
     * Находит следующее время отправления из расписания
     * 
     * Анализирует расписание и находит ближайшее время отправления
     * после текущего времени. Обрабатывает переход через полночь.
     * 
     * @param schedules Список расписаний для анализа
     * @param currentTime Текущее время в миллисекундах
     * @return Время следующего отправления или null, если не найдено
     */
    private fun findNextDepartureTime(schedules: List<BusSchedule>, currentTime: Long): Long? {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentTime
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        val currentTimeInMinutes = currentHour * 60 + currentMinute
        
        Timber.d("${javaClass.simpleName}: Current time: $currentHour:$currentMinute (${currentTimeInMinutes} minutes)")
        
        val validTimes = schedules.mapNotNull { schedule ->
            try {
                val timeParts = schedule.departureTime.split(":")
                val hour = timeParts[0].toInt()
                val minute = timeParts[1].toInt()
                val scheduleTimeInMinutes = hour * 60 + minute
                Timber.d("${javaClass.simpleName}: Schedule time: $hour:$minute (${scheduleTimeInMinutes} minutes)")
                scheduleTimeInMinutes
            } catch (e: Exception) {
                Timber.e(e, "${javaClass.simpleName}: Error parsing time: ${schedule.departureTime}")
                null
            }
        }
        
        Timber.d("${javaClass.simpleName}: All schedule times: $validTimes")
        
        // Ищем время сегодня
        val todayTimes = validTimes.filter { it >= currentTimeInMinutes }
        Timber.d("${javaClass.simpleName}: Today times after current: $todayTimes")
        
        return if (todayTimes.isNotEmpty()) {
            val nextTimeInMinutes = todayTimes.minOrNull() ?: return null
            val result = calendar.apply {
                set(Calendar.HOUR_OF_DAY, nextTimeInMinutes / 60)
                set(Calendar.MINUTE, nextTimeInMinutes % 60)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            Timber.d("${javaClass.simpleName}: Next departure time: ${nextTimeInMinutes / 60}:${nextTimeInMinutes % 60} -> $result")
            result
        } else {
            // Если нет времени сегодня, берем первое время завтра
            val firstTimeTomorrow = validTimes.minOrNull()
            if (firstTimeTomorrow != null) {
                val result = calendar.apply {
                    add(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, firstTimeTomorrow / 60)
                    set(Calendar.MINUTE, firstTimeTomorrow % 60)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                
                Timber.d("${javaClass.simpleName}: First time tomorrow: ${firstTimeTomorrow / 60}:${firstTimeTomorrow % 60} -> $result")
                result
            } else {
                Timber.d("${javaClass.simpleName}: No valid departure times found")
                null
            }
        }
    }

    private fun updateWidgetContent(
        context: Context,
        views: RemoteViews,
        routeNextTimes: List<Pair<BusRoute, String>>
    ) {
        if (routeNextTimes.isEmpty()) {
            views.setTextViewText(R.id.widget_route_title, "Автобус --")
            views.setTextViewText(R.id.widget_direction_left, leftDirection)
            views.setTextViewText(R.id.widget_direction_right, rightDirection)
            views.setTextViewText(R.id.widget_next_time_left, "--:--")
            views.setTextViewText(R.id.widget_next_time_right, "--:--")
            return
        }

        val route = routeNextTimes.first().first
        views.setTextViewText(R.id.widget_route_title, "Автобус ${route.routeNumber}")
        views.setTextViewText(R.id.widget_direction_left, leftDirection)
        views.setTextViewText(R.id.widget_direction_right, rightDirection)

        // Показываем следующий рейс для каждого направления
        val leftRoute = routeNextTimes.firstOrNull { it.second.startsWith("1:") }
        val leftTime = leftRoute?.second?.substringAfter(": ")?.trim() ?: "--:--"
        views.setTextViewText(R.id.widget_next_time_left, leftTime)

        val rightRoute = routeNextTimes.firstOrNull { it.second.startsWith("2:") }
        val rightTime = rightRoute?.second?.substringAfter(": ")?.trim() ?: "--:--"
        views.setTextViewText(R.id.widget_next_time_right, rightTime)
    }

    /**
     * Worker для обновления виджетов
     */
    class BaseWidgetUpdateWorker(
        context: Context,
        workerParams: WorkerParameters
    ) : Worker(context, workerParams) {

        override fun doWork(): Result {
            return try {
                val routeId = inputData.getString("route_id") ?: return Result.failure()
                
                // Находим соответствующий провайдер по routeId
                val providerClass = when (routeId) {
                    "102" -> Route102WidgetProvider::class.java
                    "1" -> Route1WidgetProvider::class.java
                    "102B" -> Route102BWidgetProvider::class.java
                    "3" -> Route3WidgetProvider::class.java
                    "4" -> Route4WidgetProvider::class.java
                    else -> return Result.failure()
                }
                
                val intent = Intent(applicationContext, providerClass).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                }
                applicationContext.sendBroadcast(intent)
                
                Result.success()
            } catch (e: Exception) {
                Timber.e(e, "Error updating widget")
                Result.failure()
            }
        }
    }
}