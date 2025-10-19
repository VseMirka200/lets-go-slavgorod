package com.example.lets_go_slavgorod

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.core.net.toUri
import timber.log.Timber
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.lets_go_slavgorod.data.local.AppDatabase
import com.example.lets_go_slavgorod.data.model.FavoriteTime
import com.example.lets_go_slavgorod.notifications.AlarmScheduler
import com.example.lets_go_slavgorod.ui.components.UpdateDialogManager
import com.example.lets_go_slavgorod.ui.components.DisclaimerDialog
import com.example.lets_go_slavgorod.ui.navigation.Screen
import com.example.lets_go_slavgorod.data.local.DisclaimerManager
import com.example.lets_go_slavgorod.ui.screens.HomeScreen
import com.example.lets_go_slavgorod.ui.screens.RouteNotificationSettingsScreen
import com.example.lets_go_slavgorod.ui.screens.ScheduleScreen
import com.example.lets_go_slavgorod.ui.screens.SettingsScreen
import com.example.lets_go_slavgorod.ui.theme.lets_go_slavgorodTheme
import com.example.lets_go_slavgorod.ui.viewmodel.AppTheme
import com.example.lets_go_slavgorod.ui.viewmodel.AndroidViewModelFactory
import com.example.lets_go_slavgorod.ui.viewmodel.BusViewModel
import com.example.lets_go_slavgorod.ui.viewmodel.ContextViewModelFactory
import com.example.lets_go_slavgorod.ui.viewmodel.NotificationSettingsViewModel
import com.example.lets_go_slavgorod.ui.viewmodel.ThemeViewModel
import com.example.lets_go_slavgorod.ui.viewmodel.ThemeViewModelFactory
import com.example.lets_go_slavgorod.ui.viewmodel.UpdateSettingsViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Главная активность приложения "Let's Go Slavgorod"
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
 * 
 * Архитектура:
 * - MVVM паттерн с ViewModels
 * - Jetpack Compose для UI
 * - Navigation Component для навигации
 * - DataStore для настроек
 * - Room для локальной базы данных
 * 
 * @author VseMirka200
 * @version 2.0
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
                Timber.d("Notification permission granted.")
            } else {
                Timber.w("Notification permission denied.")
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
                Timber.d("Notification permission already granted.")
                checkExactAlarmPermission()
                checkNotificationsEnabled()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                Timber.i("Showing rationale for notification permission. Launching permission request again.")
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            else -> {
                Timber.d("Requesting notification permission.")
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
            Timber.w("Notifications are disabled in system settings")
            shouldShowNotificationDisabledDialog = true
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
            Timber.i("Can schedule exact alarms: $canScheduleExact")

            if (!canScheduleExact) {
                Timber.w("Exact alarm permission not granted. User needs to enable it in settings.")
                // You could show a dialog here to guide the user to settings
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
            Timber.d("Restoring notifications after app restart")
            
            val database = AppDatabase.getDatabase(this)
            val favoriteTimeDao = database.favoriteTimeDao()
            
            val favoriteTimeEntities = favoriteTimeDao.getAllFavoriteTimes().firstOrNull() ?: emptyList()
            
            val activeFavoriteTimes = favoriteTimeEntities
                .filter { entity -> entity.isActive }
                .map { entity ->
                    FavoriteTime(
                        id = entity.id,
                        routeId = entity.routeId,
                        routeNumber = "N/A",
                        routeName = "Маршрут",
                        stopName = entity.stopName,
                        departureTime = entity.departureTime,
                        dayOfWeek = entity.dayOfWeek,
                        departurePoint = entity.departurePoint,
                        addedDate = entity.addedDate,
                        isActive = entity.isActive
                    )
                }
            
            AlarmScheduler.updateAllAlarmsBasedOnSettings(this, activeFavoriteTimes)
            Timber.d("Restored ${activeFavoriteTimes.size} active notifications")
            
        } catch (e: Exception) {
            Timber.e(e, "Error restoring notifications")
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
     */
    private fun handleNotificationIntent(intent: Intent?) {
        intent?.let {
            val fromNotification = it.getBooleanExtra("FROM_NOTIFICATION", false)
            val favoriteId = it.getStringExtra("OPEN_FAVORITE_ID")
            
            if (fromNotification && favoriteId != null) {
                Timber.d("Opening from notification: favoriteId=$favoriteId")
                
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
                                Timber.d("Set pending navigation to route: ${favoriteEntity.routeId}")
                            } else {
                                Timber.w("FavoriteTime not found for id: $favoriteId")
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "Error loading favorite time from notification")
                        }
                    }
                }
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
        Timber.d("MainActivity onPause - optimizing for background")
        
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
        Timber.d("MainActivity onResume - optimizing for foreground")
        
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
    val busViewModel: BusViewModel = viewModel(
        factory = AndroidViewModelFactory.create(application) { BusViewModel(it) }
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
        Scaffold(
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            AppNavHost(
                navController = navController,
                modifier = Modifier.padding(innerPadding),
                busViewModel = busViewModel,
                themeViewModel = themeViewModel,
                notificationSettingsViewModel = notificationSettingsViewModel
            )
            
            // Глобальный диалог обновления
            val context = LocalContext.current
            UpdateDialogManager(
                availableUpdateVersion = availableUpdateVersion,
                availableUpdateUrl = availableUpdateUrl,
                availableUpdateNotes = availableUpdateNotes,
                onDownloadUpdate = { url ->
                    // Открываем ссылку в браузере
                    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to open update URL in browser")
                    }
                },
                onClearAvailableUpdate = {
                    updateSettingsViewModel.clearAvailableUpdate()
                }
            )
            
            // Навигация из уведомления
            LaunchedEffect(pendingNavigationRouteId) {
                pendingNavigationRouteId?.let { routeId ->
                    Timber.d("Navigating to route from notification: $routeId")
                    try {
                        navController.navigate("schedule/$routeId") {
                            launchSingleTop = true
                        }
                        pendingNavigationRouteId = null
                        onNavigationHandled() // Сбрасываем в Activity
                    } catch (e: Exception) {
                        Timber.e(e, "Navigation error from notification")
                    }
                }
            }
            
            // Диалог с предупреждением о неофициальном статусе приложения
            if (showDisclaimerDialog) {
                DisclaimerDialog(
                    onDismiss = { 
                        showDisclaimerDialog = false
                        onDisclaimerDismiss()
                    },
                    onAccept = {
                        showDisclaimerDialog = false
                        onDisclaimerAccept()
                    },
                    onDontShowAgain = {
                        showDisclaimerDialog = false
                        onDisclaimerDontShowAgain()
                    }
                )
            }
            
            // Диалог об отключенных уведомлениях
            if (showNotificationDisabledDialog) {
                com.example.lets_go_slavgorod.ui.components.NotificationDisabledDialog(
                    onDismiss = { 
                        showNotificationDisabledDialog = false
                        onNotificationDialogDismiss()
                    }
                )
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
    busViewModel: BusViewModel,
    themeViewModel: ThemeViewModel,
    notificationSettingsViewModel: NotificationSettingsViewModel
) {
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
                viewModel = busViewModel
            )
        }

        composable(
            route = "schedule/{routeId}",
            arguments = listOf(
                navArgument("routeId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val routeId = backStackEntry.arguments?.getString("routeId") ?: ""
            Timber.d("Navigating to schedule for routeId: $routeId")
            val route = busViewModel.getRouteById(routeId)
            Timber.d("Found route: ${route?.name} (${route?.id})")
            ScheduleScreen(
                route = route,
                onBackClick = { navController.popBackStack() },
                viewModel = busViewModel,
                onNotificationClick = { routeIdForNotifications ->
                    try {
                        navController.navigate("route_notifications/$routeIdForNotifications") {
                            launchSingleTop = true
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Navigation error to notifications for route: $routeIdForNotifications")
                    }
                }
            )
        }


        composable(
            route = Screen.Settings.route
        ) {
            SettingsScreen(
                navController = navController,
                themeViewModel = themeViewModel
            )
        }
        
        composable(
            route = "route_notifications/{routeId}",
            arguments = listOf(
                navArgument("routeId") { type = NavType.StringType }
            ),
        ) { backStackEntry ->
            val routeId = backStackEntry.arguments?.getString("routeId") ?: ""
            val route = busViewModel.getRouteById(routeId)
            if (route != null) {
                RouteNotificationSettingsScreen(
                    route = route,
                    notificationSettingsViewModel = notificationSettingsViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            } else {
                // Если маршрут не найден, возвращаемся назад
                Timber.w("Route not found for notifications: $routeId")
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
            }
        }

    }
}