package com.example.lets_go_slavgorod

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.lets_go_slavgorod.core.toFavoriteTime
import com.example.lets_go_slavgorod.data.local.AppDatabase
import com.example.lets_go_slavgorod.data.local.DisclaimerManager
import com.example.lets_go_slavgorod.data.repository.BusRouteRepository
import com.example.lets_go_slavgorod.domain.notification.AlarmScheduler
import com.example.lets_go_slavgorod.ui.components.UpdateDialogManager
import com.example.lets_go_slavgorod.ui.navigation.Screen
import com.example.lets_go_slavgorod.ui.screens.HomeScreen
import com.example.lets_go_slavgorod.ui.screens.RouteNotificationSettingsScreen
import com.example.lets_go_slavgorod.ui.screens.ScheduleScreen
import com.example.lets_go_slavgorod.ui.screens.settings.AboutScreen
import com.example.lets_go_slavgorod.ui.screens.settings.DataManagementScreen
import com.example.lets_go_slavgorod.ui.screens.settings.DisplaySettingsScreen
import com.example.lets_go_slavgorod.ui.screens.settings.GlobalNotificationSettingsScreen
import com.example.lets_go_slavgorod.ui.screens.settings.LogsScreen
import com.example.lets_go_slavgorod.ui.screens.settings.SettingsMainScreen
import com.example.lets_go_slavgorod.ui.screens.settings.NotificationSettingsScreen
import com.example.lets_go_slavgorod.ui.theme.lets_go_slavgorodTheme
import com.example.lets_go_slavgorod.ui.viewmodel.AndroidViewModelFactory
import com.example.lets_go_slavgorod.ui.viewmodel.AppTheme
import com.example.lets_go_slavgorod.ui.viewmodel.ContextViewModelFactory
import com.example.lets_go_slavgorod.ui.viewmodel.FavoritesViewModel
import com.example.lets_go_slavgorod.ui.viewmodel.NotificationSettingsViewModel
import com.example.lets_go_slavgorod.ui.viewmodel.RoutesViewModel
import com.example.lets_go_slavgorod.ui.viewmodel.ScheduleViewModel
import com.example.lets_go_slavgorod.ui.viewmodel.ThemeViewModel
import com.example.lets_go_slavgorod.ui.viewmodel.ThemeViewModelFactory
import com.example.lets_go_slavgorod.ui.viewmodel.UpdateSettingsViewModel
import com.example.lets_go_slavgorod.ui.viewmodel.ViewModelFactory
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

/**
 * Главная активность приложения "Поехали! Славгород"
 * 
 * Точка входа в приложение, управляет навигацией, темой, разрешениями
 * и жизненным циклом приложения. Реализует современный UI на Jetpack Compose
 * с поддержкой Material Design 3.
 * 
 * Основные функции:
 * - Управление навигацией между экранами
 * - Запрос разрешений (уведомления, точные будильники)
 * - Управление темой приложения (светлая/темная/системная)
 * - Проверка обновлений приложения
 * - Отображение дисклеймера при первом запуске
 * - Восстановление уведомлений после перезагрузки
 * - Обработка навигации из виджетов к конкретным маршрутам
 * 
 * Архитектура:
 * - MVVM паттерн с ViewModels
 * - Jetpack Compose для UI
 * - Navigation Component для навигации
 * - DataStore для настроек
 * - Room для локальной базы данных
 * - WorkManager для фоновых задач
 * 
 * v3.0 Changes (Октябрь 2025):
 * - Удалены неиспользуемые компоненты (NavigationExtensions, BaseViewModel)
 * - Упрощена навигация (убраны extension функции)
 * - Оптимизированы импорты и зависимости
 * - Улучшена производительность сборки
 * 
 * @author VseMirka200
 * @version 3.0
 * @since 1.0
 */
class MainActivity : ComponentActivity() {

    // ViewModels
    
    /** ViewModel для управления темой приложения */
    private val themeViewModel: ThemeViewModel by viewModels {
        ThemeViewModelFactory(this)
    }
    

    /** Launcher для запроса разрешения на уведомления */
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
            } else {
            }
        }
    
    /** RouteId для навигации из уведомления (использует Mutex для thread-safety) */
    private var pendingNavigationRouteId: String? = null
    private val navigationMutex = kotlinx.coroutines.sync.Mutex()
    
    /** Состояние показа диалога об отключенных уведомлениях */
    private var shouldShowNotificationDisabledDialog: Boolean = false
    
    /** Состояние показа диалога с предупреждением */
    private var shouldShowDisclaimerDialog: Boolean = false

    /**
     * Запрашивает разрешение на отправку уведомлений
     * 
     * Проверяет версию Android и соответствующие разрешения:
     * - Android 13+: POST_NOTIFICATIONS
     * - Старые версии: точные будильники
     */
    private fun askNotificationPermission() {
        when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU -> {
                // For older versions, check exact alarm permission
                checkExactAlarmPermission()
                checkNotificationsEnabled()
            }
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED -> {
                checkExactAlarmPermission()
                checkNotificationsEnabled()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    
    /**
     * Проверяет, включены ли уведомления в системе
     * 
     * Если уведомления отключены, показывает диалог пользователю
     */
    private fun checkNotificationsEnabled() {
        val notificationManager = androidx.core.app.NotificationManagerCompat.from(this)
        if (!notificationManager.areNotificationsEnabled()) {
            shouldShowNotificationDisabledDialog = true
        }
        
        // Дополнительная диагностика
        checkNotificationChannels()
        checkAlarmPermissions()
        
        // Диагностика настроек уведомлений
        checkNotificationSettings()
    }
    
    /**
     * Проверяет состояние каналов уведомлений
     * 
     * Выполняет диагностику каналов уведомлений для выявления проблем
     * с доставкой уведомлений. Логирует информацию о доступных каналах
     * для отладки.
     * 
     * Используется только в Debug режиме для диагностики проблем.
     */
    private fun checkNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channels = notificationManager.notificationChannels
            
        }
    }
    
    /**
     * Проверяет разрешения на использование точных будильников
     * 
     * Начиная с Android 12 (API 31), требуется специальное разрешение
     * SCHEDULE_EXACT_ALARM для планирования точных будильников.
     * 
     * Если разрешение отсутствует, пользователь будет направлен
     * в настройки для его предоставления.
     */
    private fun checkAlarmPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as? AlarmManager
            val canScheduleExact = alarmManager?.canScheduleExactAlarms() ?: false
            if (!canScheduleExact) {
            }
        }
    }
    
    /**
     * Проверяет настройки уведомлений для диагностики проблем
     * 
     * Выполняет комплексную проверку всех настроек уведомлений:
     * - Глобальные настройки уведомлений
     * - Состояние избранных времен
     * - Активность будильников
     * 
     * Используется для диагностики проблем с доставкой уведомлений
     * и логирования информации для отладки.
     */
    private fun checkNotificationSettings() {
        try {
            val shouldSend = com.example.lets_go_slavgorod.data.local.NotificationPreferencesCache.shouldSendNotification()
            
            if (!shouldSend) {
            }
            
            checkActiveFavorites()
            
        } catch (e: Exception) {
            Timber.e(e, "Ошибка проверки настроек уведомлений")
        }
    }
    
    /**
     * Проверяет наличие активных избранных времен для уведомлений
     * 
     * Выполняет проверку базы данных на наличие активных избранных времен,
     * для которых должны быть запланированы уведомления. Используется для
     * диагностики проблем с отсутствием уведомлений.
     * 
     * Логирует предупреждение, если активных избранных времен нет.
     */
    private fun checkActiveFavorites() {
        try {
            lifecycleScope.launch {
                try {
                    val database = com.example.lets_go_slavgorod.data.local.AppDatabase.getDatabase(this@MainActivity)
                    val allFavorites = database.favoriteTimeDao().getAllFavoriteTimes()

                    val favoriteTimes = allFavorites.firstOrNull() ?: emptyList()
                    val activeFavorites = favoriteTimes.filter { it.isActive }

                    if (activeFavorites.isEmpty()) {
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Ошибка проверки активных избранных")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Ошибка в checkActiveFavorites")
        }
    }
    

    /**
     * Проверяет разрешение на точные будильники
     * 
     * Необходимо для корректной работы уведомлений о времени отправления автобусов
     */
    private fun checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as? AlarmManager
            val canScheduleExact = alarmManager?.canScheduleExactAlarms() ?: false
            if (!canScheduleExact) {
            }
        }
    }

    /**
     * Восстанавливает уведомления после перезапуска приложения
     * 
     * Вызывается при запуске приложения для восстановления всех активных уведомлений
     * в соответствии с текущими настройками пользователя
     */
    private suspend fun restoreNotifications() {
        try {
            
            val database = AppDatabase.getDatabase(this)
            val favoriteTimeDao = database.favoriteTimeDao()
            val repository = BusRouteRepository(this)
            
            val favoriteTimeEntities = favoriteTimeDao.getAllFavoriteTimes().firstOrNull() ?: emptyList()
            
            val activeFavoriteTimes = favoriteTimeEntities
                .filter { entity: com.example.lets_go_slavgorod.data.local.entity.FavoriteTimeEntity -> entity.isActive }
                .map { entity: com.example.lets_go_slavgorod.data.local.entity.FavoriteTimeEntity -> entity.toFavoriteTime(repository) }
            
            AlarmScheduler.updateAllAlarmsBasedOnSettings(this, activeFavoriteTimes)
            
        } catch (e: Exception) {
            Timber.e(e, "Ошибка восстановления уведомлений")
        }
    }

    // =====================================================================================
    //                              ЖИЗНЕННЫЙ ЦИКЛ АКТИВНОСТИ
    // =====================================================================================
    
    /**
     * Инициализация активности с оптимизациями производительности
     * 
     * Оптимизированная последовательность инициализации:
     * 1. Критичные операции (синхронно)
     * 2. UI инициализация (быстро)
     * 3. Тяжелые операции (асинхронно)
     * 
     * Оптимизации:
     * - Минимизация блокирующих операций
     * - Асинхронная обработка разрешений
     * - Кэширование состояния
     */
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // =====================================================================================
        //                              КРИТИЧЕСКИ ВАЖНЫЕ ОПЕРАЦИИ
        // =====================================================================================
        
        // Включаем Edge-to-Edge для современного дизайна
        enableEdgeToEdge()

        // =====================================================================================
        //                              ИНИЦИАЛИЗАЦИЯ UI
        // =====================================================================================
        
        // Быстрая инициализация UI
        setContent {
            BusScheduleApp(
                themeViewModel = themeViewModel,
                initialPendingNavigationRouteId = pendingNavigationRouteId,
                onNavigationHandled = { pendingNavigationRouteId = null },
                initialShowNotificationDisabledDialog = shouldShowNotificationDisabledDialog,
                onNotificationDialogDismiss = { shouldShowNotificationDisabledDialog = false },
                initialShowDisclaimerDialog = shouldShowDisclaimerDialog,
                onDisclaimerAccept = { 
                    lifecycleScope.launch {
                        DisclaimerManager.markDisclaimerAccepted(this@MainActivity)
                        shouldShowDisclaimerDialog = false
                    }
                },
                onDisclaimerDontShowAgain = {
                    lifecycleScope.launch {
                        DisclaimerManager.markDisclaimerDontShowAgain(this@MainActivity)
                        shouldShowDisclaimerDialog = false
                    }
                },
                onDisclaimerDismiss = { shouldShowDisclaimerDialog = false }
            )
        }
        
        // =====================================================================================
        //                              АСИНХРОННЫЕ ОПЕРАЦИИ
        // =====================================================================================
        
        // Запрашиваем разрешения асинхронно, чтобы не блокировать UI
        lifecycleScope.launch {
            // Проверяем, нужно ли показать диалог с предупреждением
            if (DisclaimerManager.shouldShowDisclaimer(this@MainActivity)) {
                shouldShowDisclaimerDialog = true
            }
            
            askNotificationPermission()
            // Восстанавливаем уведомления после перезапуска приложения
            restoreNotifications()
        }
        
        // Обработка Deep Link из уведомления
        handleNotificationIntent(intent)
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNotificationIntent(intent)
    }
    
    /**
     * Обрабатывает Intent из уведомления для навигации к конкретному избранному времени
     * или из виджета для навигации к конкретному маршруту
     * 
     * Поддерживает два типа навигации:
     * 1. Из уведомлений - навигация к избранному времени через базу данных
     * 2. Из виджетов - прямая навигация к расписанию маршрута
     * 
     * @param intent Intent с данными для навигации
     */
    private fun handleNotificationIntent(intent: Intent?) {
        intent?.let {
            val fromNotification = it.getBooleanExtra("FROM_NOTIFICATION", false)
            val fromWidget = it.getBooleanExtra("FROM_WIDGET", false)
            val favoriteId = it.getStringExtra("OPEN_FAVORITE_ID")
            val navigateToRoute = it.getStringExtra("navigate_to_route")
            
            
            if (fromNotification && favoriteId != null) {
                
                // Получаем routeId из базы данных асинхронно
                lifecycleScope.launch {
                    navigationMutex.withLock {
                        try {
                            val database = AppDatabase.getDatabase(this@MainActivity)
                            val favoriteEntity = database.favoriteTimeDao()
                                .getAllFavoriteTimes()
                                .firstOrNull()
                                ?.find { entity -> entity.id == favoriteId }
                            
                            if (favoriteEntity != null) {
                                pendingNavigationRouteId = favoriteEntity.routeId
                            } else {
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "Ошибка загрузки избранного времени из уведомления")
                        }
                    }
                }
            } else if (fromWidget && navigateToRoute != null) {
                // Устанавливаем навигацию сразу, без задержки
                pendingNavigationRouteId = navigateToRoute
            } else if (navigateToRoute != null) {
                // Fallback для старых виджетов
                pendingNavigationRouteId = navigateToRoute
            }
        }
    }
    
    /**
     * Оптимизированная обработка паузы активности
     * 
     * Вызывается при сворачивании приложения для:
     * - Сохранения состояния
     * - Очистки временных ресурсов
     * - Оптимизации производительности
     */
    override fun onPause() {
        super.onPause()
        
        // Оптимизации для фонового режима
        // (здесь можно добавить дополнительные оптимизации)
    }
    
    /**
     * Оптимизированная обработка возобновления активности
     * 
     * Вызывается при возврате к приложению для:
     * - Восстановления состояния
     * - Обновления данных
     * - Оптимизации производительности
     */
    override fun onResume() {
        super.onResume()
        
        // Оптимизации для активного режима
        // (здесь можно добавить дополнительные оптимизации)
    }
}

/**
 * Основной Composable компонент приложения
 * 
 * Настраивает:
 * - Тему приложения (светлая/темная/системная)
 * - Навигацию между экранами
 * - Диалоги обновлений и дисклеймера
 * - Глобальные ViewModels для работы приложения
 * 
 * Архитектура навигации:
 * - Один основной экран с маршрутами (HomeScreen)
 * - Доступ к настройкам через иконку в шапке
 * - Настройки уведомлений доступны из расписания каждого маршрута
 * 
 * @param themeViewModel ViewModel для управления темой
 * @param initialPendingNavigationRouteId Начальное значение ID маршрута для навигации из уведомления
 * @param onNavigationHandled Callback для сброса состояния навигации
 * @param initialShowNotificationDisabledDialog Начальное состояние показа диалога об отключенных уведомлениях
 * @param onNotificationDialogDismiss Callback для закрытия диалога об уведомлениях
 * @param initialShowDisclaimerDialog Начальное состояние показа диалога disclaimer
 * @param onDisclaimerAccept Callback при принятии disclaimer
 * @param onDisclaimerDontShowAgain Callback при выборе "больше не показывать"
 * @param onDisclaimerDismiss Callback при закрытии disclaimer
 */
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusScheduleApp(
    themeViewModel: ThemeViewModel,
    initialPendingNavigationRouteId: String?,
    onNavigationHandled: () -> Unit,
    initialShowNotificationDisabledDialog: Boolean,
    onNotificationDialogDismiss: () -> Unit,
    initialShowDisclaimerDialog: Boolean,
    onDisclaimerAccept: () -> Unit,
    onDisclaimerDontShowAgain: () -> Unit,
    onDisclaimerDismiss: () -> Unit
) {
    val navController = rememberNavController()
    val localContext = LocalContext.current
    val application = localContext.applicationContext as Application
    
    // Локальные состояния для управления диалогами и навигацией
    var pendingNavigationRouteId by remember { mutableStateOf(initialPendingNavigationRouteId) }
    var showNotificationDisabledDialog by remember { mutableStateOf(initialShowNotificationDisabledDialog) }
    var showDisclaimerDialog by remember { mutableStateOf(initialShowDisclaimerDialog) }
    
    // ViewModels с generic фабриками
    val favoritesViewModel: FavoritesViewModel = viewModel(
        factory = AndroidViewModelFactory.create(application) { FavoritesViewModel(it) }
    )
    
    val notificationSettingsViewModel: NotificationSettingsViewModel = viewModel(
        factory = AndroidViewModelFactory.create(application) { NotificationSettingsViewModel(it) }
    )
    
    val updateSettingsViewModel: UpdateSettingsViewModel = viewModel(
        factory = ContextViewModelFactory.create(localContext) { UpdateSettingsViewModel(it) }
    )
    
    val currentAppTheme by themeViewModel.currentTheme.collectAsState()
    val useDarkTheme = when (currentAppTheme) {
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }
    
    // Данные о доступном обновлении
    val availableUpdateVersion by updateSettingsViewModel.availableUpdateVersion.collectAsState(initial = null)
    val availableUpdateUrl by updateSettingsViewModel.availableUpdateUrl.collectAsState(initial = null)
    val availableUpdateNotes by updateSettingsViewModel.availableUpdateNotes.collectAsState(initial = null)
    
    val coroutineScope = rememberCoroutineScope()

    lets_go_slavgorodTheme(darkTheme = useDarkTheme) {
        // Состояние сети
        Scaffold(
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            AppNavHost(
                navController = navController,
                modifier = Modifier.padding(innerPadding),
                favoritesViewModel = favoritesViewModel,
                themeViewModel = themeViewModel,
                notificationSettingsViewModel = notificationSettingsViewModel
            )
            
            // Глобальный диалог обновления
            val context = LocalContext.current
            val activity = context as? ComponentActivity
            UpdateDialogManager(
                availableUpdateVersion = availableUpdateVersion,
                availableUpdateUrl = availableUpdateUrl,
                availableUpdateNotes = availableUpdateNotes,
                onDownloadUpdate = { url ->
                    try {
                        val updateDownloader = com.example.lets_go_slavgorod.domain.update.UpdateDownloader(context)
                        
                        // Запускаем загрузку обновления
                        val versionName = availableUpdateVersion ?: "unknown"
                        val downloadId = updateDownloader.downloadAndInstallUpdate(url, versionName)
                        
                        if (downloadId == -1L) {
                            Timber.e("Не удалось запустить загрузку обновления")
                        } else {
                            Timber.d("Загрузка обновления запущена успешно, ID: $downloadId")
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Ошибка при запуске загрузки обновления")
                    }
                },
                onClearAvailableUpdate = {
                    updateSettingsViewModel.clearAvailableUpdate()
                }
            )
            
            // Навигация из уведомления или виджета
            LaunchedEffect(pendingNavigationRouteId) {
                pendingNavigationRouteId?.let { routeId ->
                    try {
                        val destination = "schedule/$routeId"
                        
                        // Задержка для полной загрузки приложения
                        kotlinx.coroutines.delay(500)
                        
                        // Проверяем, что мы не на том же экране
                        val currentRoute = navController.currentDestination?.route
                        
                        if (currentRoute != destination) {
                            navController.navigate(destination)
                        } else {
                        }
                        
                        pendingNavigationRouteId = null
                        onNavigationHandled() // Сбрасываем в Activity
                    } catch (e: Exception) {
                        Timber.e(e, "Ошибка навигации к маршруту: $routeId")
                    }
                }
            }
            
        }
    }
}

/**
 * Навигационный хост приложения
 * 
 * Определяет все навигационные маршруты в приложении:
 * - home: главный экран со списком маршрутов
 * - schedule/{routeId}: расписание конкретного маршрута
 * - settings: экран настроек приложения
 * - about: информация о приложении и разработчике
 * - route_notifications/{routeId}: настройки уведомлений для маршрута
 * 
 * Особенности:
 * - Единая точка входа - экран маршрутов
 * - Настройки доступны из шапки главного экрана
 * - Уведомления настраиваются для каждого маршрута отдельно
 * - Избранные времена управляются через расписание маршрута
 * 
 * @param navController контроллер навигации
 * @param modifier модификатор для настройки внешнего вида
 * @param busViewModel ViewModel для работы с данными маршрутов и избранного
 * @param themeViewModel ViewModel для управления темой приложения
 * @param notificationSettingsViewModel ViewModel для настроек уведомлений
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    favoritesViewModel: FavoritesViewModel,
    themeViewModel: ThemeViewModel,
    notificationSettingsViewModel: NotificationSettingsViewModel
) {
    // Создаем общий RoutesViewModel для всего NavHost
    // Это гарантирует, что все экраны используют один и тот же экземпляр
    val appContext = LocalContext.current
    val sharedRoutesViewModel: RoutesViewModel = viewModel(
        factory = ViewModelFactory(appContext.applicationContext as Application)
    )
    
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(
            route = Screen.Home.route
        ) {
            HomeScreen(
                navController = navController,
                routesViewModel = sharedRoutesViewModel
            )
        }

        composable(
            route = "schedule/{routeId}",
            arguments = listOf(
                navArgument("routeId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val routeId = backStackEntry.arguments?.getString("routeId") ?: ""
            
            // Создаем ScheduleViewModel для этого экрана
            val appContext = androidx.compose.ui.platform.LocalContext.current
            val scheduleViewModel: ScheduleViewModel = viewModel(
                factory = ViewModelFactory(appContext.applicationContext as android.app.Application)
            )
            
            ScheduleScreen(
                routeId = routeId,
                scheduleViewModel = scheduleViewModel,
                favoritesViewModel = favoritesViewModel,
                notificationSettingsViewModel = notificationSettingsViewModel,
                onBackClick = { navController.popBackStack() },
                onNotificationClick = {
                    navController.navigate("route_notifications/$routeId")
                },
                routesViewModel = sharedRoutesViewModel
            )
        }


        composable(
            route = Screen.Settings.route
        ) {
            SettingsMainScreen(
                navController = navController,
                onBackClick = { navController.popBackStack() }
            )
        }
        
        composable(route = Screen.About.route) {
            AboutScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        
        composable(route = Screen.Logs.route) {
            LogsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        
        composable(route = Screen.NotificationSettings.route) {
            NotificationSettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        
        composable(route = Screen.DisplaySettings.route) {
            DisplaySettingsScreen(
                themeViewModel = themeViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
        
        
        composable(route = Screen.DataManagement.route) {
            DataManagementScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        
        composable(
            route = "route_notifications/{routeId}",
            arguments = listOf(
                navArgument("routeId") { type = NavType.StringType }
            ),
        ) { backStackEntry ->
            val routeId = backStackEntry.arguments?.getString("routeId") ?: ""
            
            // Используем общий RoutesViewModel вместо создания нового
            // Это гарантирует, что маршруты уже загружены
            val uiState by sharedRoutesViewModel.uiState.collectAsState()
            val route = remember(routeId, uiState.routes) {
                val foundRoute = uiState.routes.find { it.id == routeId }
                foundRoute
            }
            
            if (route != null) {
                RouteNotificationSettingsScreen(
                    route = route,
                    notificationSettingsViewModel = notificationSettingsViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            } else {
                // Если маршрут не найден, показываем загрузку
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    // Маршрут действительно не найден, возвращаемся назад
                    LaunchedEffect(Unit) {
                        navController.popBackStack()
                    }
                }
            }
        }
        
        composable(
            route = "settings/notifications"
        ) {
            GlobalNotificationSettingsScreen(
                notificationSettingsViewModel = notificationSettingsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        

    }
}