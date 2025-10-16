package com.example.lets_go_slavgorod.ui.screens

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.lets_go_slavgorod.R
import com.example.lets_go_slavgorod.ui.navigation.Screen
import com.example.lets_go_slavgorod.ui.viewmodel.AppTheme
import com.example.lets_go_slavgorod.ui.viewmodel.ContextViewModelFactory
import com.example.lets_go_slavgorod.ui.viewmodel.DataManagementViewModel
import com.example.lets_go_slavgorod.ui.viewmodel.DisplaySettingsViewModel
import com.example.lets_go_slavgorod.ui.viewmodel.QuietMode
import com.example.lets_go_slavgorod.ui.viewmodel.QuietModeViewModel
import com.example.lets_go_slavgorod.ui.viewmodel.RouteDisplayMode
import com.example.lets_go_slavgorod.ui.viewmodel.ThemeViewModel
import com.example.lets_go_slavgorod.ui.viewmodel.UpdateMode
import com.example.lets_go_slavgorod.ui.viewmodel.UpdateSettingsViewModel
import com.example.lets_go_slavgorod.ui.viewmodel.VibrationSettingsViewModel
import androidx.compose.material3.Switch
import androidx.compose.material.icons.filled.PhoneAndroid
import timber.log.Timber

/**
 * Экран настроек приложения
 * 
 * Централизованный экран для управления всеми настройками приложения.
 * Доступен из главного экрана через иконку настроек в шапке.
 * 
 * Разделы настроек:
 * 1. Настройка темы
 *    - Выбор темы: Системная / Светлая / Темная
 *    - Модальный диалог выбора
 * 
 * 2. Настройка отображения
 *    - Режим отображения маршрутов: Клетка / Список
 *    - Количество колонок в сетке: 1-4 (только для режима "Клетка")
 *    - Модальные диалоги выбора
 * 
 * 3. Настройка обновлений
 *    - Режим проверки: Автоматически / Вручную / Отключено
 *    - Ручная проверка обновлений
 *    - Информация о последней проверке
 *    - Модальный диалог выбора режима
 * 
 * 4. Настройка уведомлений (глобальные)
 *    - Режим уведомлений: Включены / Отключены / Временно
 *    - Временное отключение на N дней
 *    - Модальный диалог выбора
 * 
 * 5. Сброс настроек
 *    - Сброс всех настроек к значениям по умолчанию
 *    - Очистка кэша приложения
 * 
 * 6. О приложении
 *    - Переход к экрану "О программе"
 * 
 * Все выпадающие меню реализованы как модальные диалоги с радио-кнопками.
 * 
 * @param navController контроллер навигации
 * @param modifier модификатор для настройки внешнего вида
 * @param themeViewModel ViewModel для управления темой
 * @param updateSettingsViewModel ViewModel для настроек обновлений (опционально)
 */
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController? = null,
    modifier: Modifier = Modifier,
    themeViewModel: ThemeViewModel = viewModel(),
    updateSettingsViewModel: UpdateSettingsViewModel? = null,
) {
    val context = LocalContext.current
    
    val displaySettingsViewModel: DisplaySettingsViewModel = viewModel(
        factory = ContextViewModelFactory.create(context) { DisplaySettingsViewModel(it) }
    )
    val updateSettingsVM = updateSettingsViewModel ?: viewModel(
        factory = ContextViewModelFactory.create(context) { UpdateSettingsViewModel(it) }
    )
    
    val quietModeViewModel: QuietModeViewModel = viewModel(
        factory = ContextViewModelFactory.create(context) { QuietModeViewModel(it) }
    )
    
    val dataManagementViewModel: DataManagementViewModel = viewModel(
        factory = ContextViewModelFactory.create(context) { DataManagementViewModel(it) }
    )
    
    val vibrationSettingsViewModel: VibrationSettingsViewModel = viewModel(
        factory = ContextViewModelFactory.create(context) { VibrationSettingsViewModel(it) }
    )
    
    val currentAppTheme by themeViewModel.currentTheme.collectAsState()
    var showThemeDropdown by remember { mutableStateOf(false) }
    val themeOptions = listOf(
        AppTheme.SYSTEM to stringResource(R.string.theme_system),
        AppTheme.LIGHT to stringResource(R.string.theme_light),
        AppTheme.DARK to stringResource(R.string.theme_dark)
    )

    // Quiet mode state
    val currentQuietMode by quietModeViewModel.quietMode.collectAsState()
    var showQuietModeDropdown by remember { mutableStateOf(false) }
    val quietModeOptions = remember { QuietMode.entries.toTypedArray() }
    
    // Update settings state
    val currentUpdateMode by updateSettingsVM.currentUpdateMode.collectAsState(initial = UpdateMode.AUTOMATIC)
    var showUpdateModeDropdown by remember { mutableStateOf(false) }
    val updateModeOptions = remember { UpdateMode.entries.toTypedArray() }
    val isCheckingUpdates by updateSettingsVM.isCheckingUpdates.collectAsState(initial = false)
    val updateCheckError by updateSettingsVM.updateCheckError.collectAsState(initial = null)
    val updateCheckStatus by updateSettingsVM.updateCheckStatus.collectAsState(initial = null)
    val lastUpdateCheckTime by updateSettingsVM.lastUpdateCheckTime.collectAsState(initial = 0L)
    val availableUpdateVersion by updateSettingsVM.availableUpdateVersion.collectAsState(initial = null)
    val availableUpdateUrl by updateSettingsVM.availableUpdateUrl.collectAsState(initial = null)
    val availableUpdateNotes by updateSettingsVM.availableUpdateNotes.collectAsState(initial = null)
    

    // Настройки отображения
    val currentDisplayMode by displaySettingsViewModel.displayMode.collectAsState(initial = RouteDisplayMode.GRID)
    val currentGridColumns by displaySettingsViewModel.gridColumns.collectAsState(initial = 2)
    var showDisplayModeDropdown by remember { mutableStateOf(false) }
    var showColumnsDropdown by remember { mutableStateOf(false) }
    val displayModeOptions = listOf(
        RouteDisplayMode.GRID to "Клетка",
        RouteDisplayMode.LIST to "Список"
    )
    val columnsOptions = listOf(
        1 to "1 колонка",
        2 to "2 колонки", 
        3 to "3 колонки",
        4 to "4 колонки"
    )

    var showResetSettingsDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var cacheCleared by remember { mutableStateOf(false) }

        Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_screen_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController?.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                windowInsets = WindowInsets(0)
            )
        },
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(16.dp))
            // Заголовок секции "Настройка темы"
            Text(
                text = "Настройка темы",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            ThemeSettingsCard(
                currentAppTheme = currentAppTheme,
                showThemeDropdown = showThemeDropdown,
                onShowThemeDropdownChange = { showThemeDropdown = it },
                themeOptions = themeOptions,
                onThemeSelected = { theme ->
                    themeViewModel.setTheme(theme)
                }
            )

            Spacer(Modifier.height(24.dp))

            // Заголовок секции "Отображение"
            Text(
                text = "Настройка отображения",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            DisplaySettingsCard(
                currentDisplayMode = currentDisplayMode,
                currentGridColumns = currentGridColumns,
                showDisplayModeDropdown = showDisplayModeDropdown,
                onShowDisplayModeDropdownChange = { showDisplayModeDropdown = it },
                displayModeOptions = displayModeOptions,
                onDisplayModeSelected = { mode ->
                    displaySettingsViewModel.setDisplayMode(mode)
                },
                onColumnsSelected = { columns ->
                    displaySettingsViewModel.setGridColumns(columns)
                },
                showColumnsDropdown = showColumnsDropdown,
                onShowColumnsDropdownChange = { showColumnsDropdown = it },
                columnsOptions = columnsOptions
            )

            Spacer(Modifier.height(16.dp))
            
            // Заголовок секции "Обновления"
            Text(
                text = "Настройка обновлений",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            UpdateSettingsCard(
                currentUpdateMode = currentUpdateMode,
                showUpdateModeDropdown = showUpdateModeDropdown,
                onShowUpdateModeDropdownChange = { showUpdateModeDropdown = it },
                updateModeOptions = updateModeOptions,
                onUpdateModeSelected = { mode ->
                    updateSettingsVM.setUpdateMode(mode)
                },
                isCheckingUpdates = isCheckingUpdates,
                updateCheckError = updateCheckError,
                updateCheckStatus = updateCheckStatus,
                lastUpdateCheckTime = lastUpdateCheckTime,
                availableUpdateVersion = availableUpdateVersion,
                availableUpdateUrl = availableUpdateUrl,
                availableUpdateNotes = availableUpdateNotes,
                onCheckForUpdates = {
                    updateSettingsVM.checkForUpdates()
                },
                onClearAvailableUpdate = {
                    updateSettingsVM.clearAvailableUpdate()
                },
                onClearUpdateStatus = {
                    updateSettingsVM.clearUpdateCheckStatus()
                },
                onDownloadUpdate = { url ->
                    // Открываем ссылку в браузере
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, url.toUri())
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to open update URL")
                    }
                }
            )

            Spacer(Modifier.height(24.dp))

            // Заголовок секции "Уведомления"
            Text(
                text = "Настройка уведомлений",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            QuietModeSettingsCard(
                currentQuietMode = currentQuietMode,
                showQuietModeDropdown = showQuietModeDropdown,
                onShowQuietModeDropdownChange = { showQuietModeDropdown = it },
                quietModeOptions = quietModeOptions,
                customDays = quietModeViewModel.customDays.collectAsState().value,
                onQuietModeSelected = { mode, days ->
                    quietModeViewModel.setQuietMode(mode, days)
                }
            )
            
            Spacer(Modifier.height(16.dp))
            
            // Карточка настройки вибрации
            VibrationSettingsCard(
                vibrationEnabled = vibrationSettingsViewModel.vibrationEnabled.collectAsState().value,
                onVibrationToggle = { enabled ->
                    vibrationSettingsViewModel.setVibrationEnabled(enabled)
                }
            )
            
            Spacer(Modifier.height(24.dp))

            // Заголовок секции "Управление данными"
            Text(
                text = "Управление данными",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            ScheduleUpdateCard(
                isRefreshing = dataManagementViewModel.isRefreshingSchedule.collectAsState().value,
                refreshError = dataManagementViewModel.scheduleRefreshError.collectAsState().value,
                refreshSuccess = dataManagementViewModel.scheduleRefreshSuccess.collectAsState().value,
                dataVersion = dataManagementViewModel.dataVersion.collectAsState().value,
                dataLastUpdated = dataManagementViewModel.dataLastUpdated.collectAsState().value,
                onRefresh = {
                    dataManagementViewModel.refreshScheduleFromGitHub()
                },
                onClearStatus = {
                    dataManagementViewModel.clearScheduleRefreshStatus()
                }
            )
            
            Spacer(Modifier.height(24.dp))

            // Заголовок секции "Сброс настроек"
            Text(
                text = "Сброс настроек",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            ResetSettingsCard(
                onResetSettings = { showResetSettingsDialog = true }
            )
            
            Spacer(Modifier.height(12.dp))
            
            ClearCacheCard(
                onClearCache = { showClearCacheDialog = true },
                cacheCleared = cacheCleared
            )

            Spacer(Modifier.height(24.dp))
            
            // Заголовок секции "О приложении"
            Text(
                text = "О приложении",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            AboutNavigationCard(
                navController = navController
            )
            
            Spacer(Modifier.height(16.dp))
        }
    }
    
    // Диалоги подтверждения
    if (showResetSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showResetSettingsDialog = false },
            title = { 
                Text(
                    text = stringResource(R.string.warning_title),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                ) 
            },
            text = { 
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_reset_warning),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.error
                    )
                    
                    Text(
                        text = "Это действие:",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    
                    Column(
                        modifier = Modifier.padding(start = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "• Удалит все ваши настройки",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "• Сбросит тему на системную",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "• Вернёт режим отображения",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "• Перезапустит приложение",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    
                    Text(
                        text = "Избранные маршруты НЕ будут удалены.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                    
                    Spacer(Modifier.height(4.dp))
                    
                    Text(
                        text = "Вы точно хотите продолжить?",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetSettingsDialog = false }) {
                    Text("Отмена")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetSettingsDialog = false
                        dataManagementViewModel.resetAllSettings()
                    }
                ) {
                    Text("Да, сбросить")
                }
            }
        )
    }
    
    // Диалог очистки кэша
    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { 
                Text(
                    text = "Очистка кэша",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                ) 
            },
            text = { 
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Это действие очистит весь кэш приложения:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Column(
                        modifier = Modifier.padding(start = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "• Кэш маршрутов",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "• Временные данные",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    Spacer(Modifier.height(4.dp))
                    
                    Text(
                        text = "Настройки и избранное НЕ будут удалены.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text("Отмена")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        Timber.d("Cache clearing is no longer needed (managed automatically by JsonDataSource)")
                        cacheCleared = true
                        showClearCacheDialog = false
                    }
                ) {
                    Text("Очистить")
                }
            }
        )
    }
}

/**
 * Карточка настроек отображения маршрутов
 * 
 * Содержит две настройки:
 * 1. Режим отображения (клетка/список)
 * 2. Количество колонок (1-4, только для режима "клетка")
 * 
 * При клике на настройку открывается модальный диалог выбора.
 * 
 * @param currentDisplayMode текущий режим отображения
 * @param currentGridColumns текущее количество колонок
 * @param showDisplayModeDropdown флаг отображения диалога режима
 * @param onShowDisplayModeDropdownChange callback изменения флага диалога режима
 * @param displayModeOptions доступные режимы отображения
 * @param onDisplayModeSelected callback выбора режима отображения
 * @param onColumnsSelected callback выбора количества колонок
 * @param showColumnsDropdown флаг отображения диалога колонок
 * @param onShowColumnsDropdownChange callback изменения флага диалога колонок
 * @param columnsOptions доступные варианты количества колонок
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DisplaySettingsCard(
    currentDisplayMode: RouteDisplayMode,
    currentGridColumns: Int,
    showDisplayModeDropdown: Boolean,
    onShowDisplayModeDropdownChange: (Boolean) -> Unit,
    displayModeOptions: List<Pair<RouteDisplayMode, String>>,
    onDisplayModeSelected: (RouteDisplayMode) -> Unit,
    onColumnsSelected: (Int) -> Unit,
    showColumnsDropdown: Boolean,
    onShowColumnsDropdownChange: (Boolean) -> Unit,
    columnsOptions: List<Pair<Int, String>>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Режим отображения
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Palette,
                        contentDescription = "Иконка настроек отображения",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Spacer(Modifier.width(16.dp))
                    
                    Text(
                        text = "Режим отображения",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .clickable { onShowDisplayModeDropdownChange(true) }
                ) {
                    Text(
                        text = displayModeOptions.find { it.first == currentDisplayMode }?.second ?: "Клетка",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // Настройка колонок (только для режима клетка)
            if (currentDisplayMode == RouteDisplayMode.GRID) {
                // Разделитель
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
                
                // Выпадающее меню для выбора количества колонок
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Колонок в строке",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clickable { onShowColumnsDropdownChange(true) }
                    ) {
                        Text(
                            text = columnsOptions.find { it.first == currentGridColumns }?.second ?: "2 колонки",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
    
    // Модальные диалоги
    if (showDisplayModeDropdown) {
        ModalSelectionDialog(
            title = "Режим отображения",
            options = displayModeOptions.map { it.second },
            selectedIndex = displayModeOptions.indexOfFirst { it.first == currentDisplayMode },
            onOptionSelected = { index ->
                onDisplayModeSelected(displayModeOptions[index].first)
                onShowDisplayModeDropdownChange(false)
            },
            onDismiss = { onShowDisplayModeDropdownChange(false) }
        )
    }
    
    if (showColumnsDropdown && currentDisplayMode == RouteDisplayMode.GRID) {
        ModalSelectionDialog(
            title = "Количество колонок",
            options = columnsOptions.map { it.second },
            selectedIndex = columnsOptions.indexOfFirst { it.first == currentGridColumns },
            onOptionSelected = { index ->
                onColumnsSelected(columnsOptions[index].first)
                onShowColumnsDropdownChange(false)
            },
            onDismiss = { onShowColumnsDropdownChange(false) }
        )
    }
}

/**
 * Карточка настроек темы приложения
 * 
 * Позволяет выбрать тему оформления приложения.
 * При клике открывается модальный диалог с вариантами.
 * 
 * Доступные темы:
 * - Как в системе (следует системной теме Android)
 * - Светлая
 * - Темная
 * 
 * @param currentAppTheme текущая выбранная тема
 * @param showThemeDropdown флаг отображения модального диалога
 * @param onShowThemeDropdownChange callback изменения флага диалога
 * @param themeOptions список доступных тем
 * @param onThemeSelected callback выбора темы
 */
@Composable
fun ThemeSettingsCard(
    currentAppTheme: AppTheme,
    showThemeDropdown: Boolean,
    onShowThemeDropdownChange: (Boolean) -> Unit,
    themeOptions: List<Pair<AppTheme, String>>,
    onThemeSelected: (AppTheme) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onShowThemeDropdownChange(true) }
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Filled.Palette,
                    contentDescription = stringResource(R.string.settings_appearance_icon_desc),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    text = "Темы",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            
            Text(
                text = themeOptions.find { it.first == currentAppTheme }?.second ?: "Как в системе",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
    
    // Модальный диалог выбора темы
    if (showThemeDropdown) {
        ModalSelectionDialog(
            title = "Выберите тему",
            options = themeOptions.map { it.second },
            selectedIndex = themeOptions.indexOfFirst { it.first == currentAppTheme },
            onOptionSelected = { index ->
                onThemeSelected(themeOptions[index].first)
                onShowThemeDropdownChange(false)
            },
            onDismiss = { onShowThemeDropdownChange(false) }
        )
    }
}

/**
 * Единая карточка настроек обновлений
 * 
 * Объединяет все функции управления обновлениями в одном месте:
 * - Выбор режима обновлений
 * - Ручная проверка обновлений
 * - Отображение статуса и результатов
 * - Управление доступными обновлениями
 * 
 * @param currentUpdateMode текущий режим обновлений
 * @param showUpdateModeDropdown флаг отображения выпадающего меню
 * @param onShowUpdateModeDropdownChange callback для изменения состояния меню
 * @param updateModeOptions доступные режимы обновлений
 * @param onUpdateModeSelected callback для выбора режима
 * @param isCheckingUpdates флаг процесса проверки обновлений
 * @param updateCheckError ошибка проверки обновлений
 * @param updateCheckStatus статус проверки обновлений
 * @param lastUpdateCheckTime время последней проверки
 * @param availableUpdateVersion версия доступного обновления
 * @param availableUpdateUrl ссылка для скачивания обновления
 * @param availableUpdateNotes описание изменений в обновлении
 * @param onCheckForUpdates callback для запуска проверки обновлений
 * @param onClearAvailableUpdate callback для очистки информации об обновлении
 * @param onClearUpdateStatus callback для очистки статуса
 * @param onDownloadUpdate callback для скачивания обновления
 */
@Composable
fun UpdateSettingsCard(
    currentUpdateMode: UpdateMode,
    showUpdateModeDropdown: Boolean,
    onShowUpdateModeDropdownChange: (Boolean) -> Unit,
    updateModeOptions: Array<UpdateMode>,
    onUpdateModeSelected: (UpdateMode) -> Unit,
    isCheckingUpdates: Boolean,
    updateCheckError: String?,
    updateCheckStatus: String?,
    lastUpdateCheckTime: Long,
    availableUpdateVersion: String?,
    availableUpdateUrl: String?,
    availableUpdateNotes: String?,
    onCheckForUpdates: () -> Unit,
    onClearAvailableUpdate: () -> Unit,
    onClearUpdateStatus: () -> Unit,
    onDownloadUpdate: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(0.dp)
        ) {
            // Выбор режима обновлений в виде единой строки, как в других карточках
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Update,
                            contentDescription = "Режим обновления",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = "Режим обновления",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clickable { onShowUpdateModeDropdownChange(true) }
                    ) {
                        Text(
                            text = when (currentUpdateMode) {
                                UpdateMode.AUTOMATIC -> "Авто"
                                UpdateMode.MANUAL -> "Ручной"
                                UpdateMode.DISABLED -> "Выкл"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
            }
            
            // Разделитель
            if (currentUpdateMode != UpdateMode.DISABLED) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
                
                // Ручная проверка обновлений
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Проверка обновлений",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (lastUpdateCheckTime > 0L) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Последняя проверка: ${formatLastCheckTime(lastUpdateCheckTime)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onCheckForUpdates,
                        enabled = !isCheckingUpdates,
                        modifier = Modifier.height(36.dp)
                    ) {
                        if (isCheckingUpdates) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Проверяем...")
                        } else {
                            Text("Проверить")
                        }
                    }
                }
                
                // Показываем статус проверки обновлений
                updateCheckStatus?.let { status ->
                    Spacer(Modifier.height(6.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (status.contains("последняя версия")) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (status.contains("последняя версия")) {
                                        Icons.Filled.CheckCircle
                                    } else {
                                        Icons.Filled.Update
                                    },
                                    contentDescription = "Статус обновления",
                                    tint = if (status.contains("последняя версия")) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.secondary
                                    },
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = status,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (status.contains("последняя версия")) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    }
                                )
                            }
                            IconButton(
                                onClick = onClearUpdateStatus,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Закрыть",
                                    tint = if (status.contains("последняя версия")) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    },
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
                
                // Показываем ошибку, если есть
                updateCheckError?.let { error ->
                    Spacer(Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Error,
                                contentDescription = "Ошибка",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
                
                // Показываем доступное обновление, если есть
                availableUpdateVersion?.let { version ->
                    Spacer(Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Download,
                                    contentDescription = "Обновление",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Доступно обновление $version",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            
                            availableUpdateNotes?.let { notes ->
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = notes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            
                            Spacer(Modifier.height(12.dp))
                            
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        availableUpdateUrl?.let { url ->
                                            onDownloadUpdate(url)
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Download,
                                        contentDescription = "Скачать",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("Скачать")
                                }
                                
                                OutlinedButton(
                                    onClick = onClearAvailableUpdate,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Позже")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Модальный диалог выбора режима обновлений
    if (showUpdateModeDropdown) {
        ModalSelectionDialog(
            title = "Режим обновления",
            options = updateModeOptions.map { mode ->
                when (mode) {
                    UpdateMode.AUTOMATIC -> "Автоматическая проверка"
                    UpdateMode.MANUAL -> "Только ручная проверка"
                    UpdateMode.DISABLED -> "Отключено"
                }
            },
            selectedIndex = updateModeOptions.indexOf(currentUpdateMode),
            onOptionSelected = { index ->
                onUpdateModeSelected(updateModeOptions[index])
                onShowUpdateModeDropdownChange(false)
            },
            onDismiss = { onShowUpdateModeDropdownChange(false) }
        )
    }
}


/**
 * Форматирует время последней проверки обновлений в читаемый вид
 */
private fun formatLastCheckTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "только что" // менее минуты
        diff < 3600_000 -> "${diff / 60_000} мин. назад" // менее часа
        diff < 86400_000 -> "${diff / 3600_000} ч. назад" // менее суток
        else -> {
            val dateFormat = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
            dateFormat.format(java.util.Date(timestamp))
        }
    }
}

/**
 * Карточка глобальных настроек уведомлений
 * 
 * Управляет глобальным режимом уведомлений для всех маршрутов.
 * Режим можно переопределить для каждого маршрута индивидуально.
 * 
 * Режимы:
 * - Включены: уведомления работают для всех маршрутов
 * - Отключены: все уведомления отключены
 * - Временно: отключены на заданное количество дней
 * 
 * Примечание: Индивидуальные настройки маршрутов настраиваются
 * через экран расписания (кнопка уведомлений в шапке).
 * 
 * @param currentQuietMode текущий режим тихого режима
 * @param showQuietModeDropdown флаг отображения модального диалога
 * @param onShowQuietModeDropdownChange callback изменения флага диалога
 * @param quietModeOptions доступные режимы тихого режима
 * @param customDays количество дней для временного отключения
 * @param onQuietModeSelected callback выбора режима (режим, дни)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuietModeSettingsCard(
    currentQuietMode: QuietMode,
    showQuietModeDropdown: Boolean,
    onShowQuietModeDropdownChange: (Boolean) -> Unit,
    quietModeOptions: Array<QuietMode>,
    customDays: Int,
    onQuietModeSelected: (QuietMode, Int) -> Unit
) {
    var showDaysDialog by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.NotificationsOff,
                        contentDescription = stringResource(R.string.settings_quiet_mode_icon_desc),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = "Уведомления",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.clickable { onShowQuietModeDropdownChange(true) }
                ) {
                    Text(
                        text = when (currentQuietMode) {
                            QuietMode.DISABLED -> "Отключены"
                            QuietMode.ENABLED -> "Включены"
                            QuietMode.CUSTOM_DAYS -> "Временно: $customDays ${getDaysWord(customDays)}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // Показываем количество дней если выбран режим CUSTOM_DAYS
            if (currentQuietMode == QuietMode.CUSTOM_DAYS && customDays > 0) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.quiet_mode_info, customDays, getDaysWord(customDays)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Информация о статусе
            if (currentQuietMode != QuietMode.ENABLED && currentQuietMode != QuietMode.CUSTOM_DAYS) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.quiet_mode_all_disabled),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
    
    // Модальный диалог выбора режима уведомлений
    if (showQuietModeDropdown) {
        ModalSelectionDialog(
            title = "Режим уведомлений",
            options = quietModeOptions.map { it.displayName },
            selectedIndex = quietModeOptions.indexOf(currentQuietMode),
            onOptionSelected = { index ->
                val mode = quietModeOptions[index]
                if (mode == QuietMode.CUSTOM_DAYS) {
                    showDaysDialog = true
                    onShowQuietModeDropdownChange(false)
                } else {
                    onQuietModeSelected(mode, 0)
                    onShowQuietModeDropdownChange(false)
                }
            },
            onDismiss = { onShowQuietModeDropdownChange(false) }
        )
    }
    
    // Диалог для ввода количества дней
    if (showDaysDialog) {
        CustomDaysDialog(
            onDismiss = { showDaysDialog = false },
            onConfirm = { days ->
                onQuietModeSelected(QuietMode.CUSTOM_DAYS, days)
                showDaysDialog = false
            }
        )
    }
}

// Склонение слова "день"
private fun getDaysWord(count: Int): String {
    return when {
        count % 10 == 1 && count % 100 != 11 -> "день"
        count % 10 in 2..4 && (count % 100 < 10 || count % 100 >= 20) -> "дня"
        else -> "дней"
    }
}

/**
 * Универсальный модальный диалог выбора опций
 * 
 * Переиспользуемый компонент для отображения списка вариантов выбора.
 * Используется для всех настроек с выбором из списка.
 * 
 * Особенности:
 * - Радио-кнопки для визуального отображения выбранного элемента
 * - Клик на всю строку для выбора
 * - Автоматическое закрытие после выбора (управляется извне)
 * - Кнопка "Закрыть" для отмены
 * 
 * @param title заголовок диалога
 * @param options список текстовых вариантов для выбора
 * @param selectedIndex индекс выбранного элемента
 * @param onOptionSelected callback при выборе элемента (передается индекс)
 * @param onDismiss callback при закрытии диалога
 */
@Composable
private fun ModalSelectionDialog(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                options.forEachIndexed { index, option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOptionSelected(index) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = index == selectedIndex,
                            onClick = { onOptionSelected(index) }
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
}

/**
 * Диалог для ввода количества дней временного отключения уведомлений
 * 
 * Позволяет пользователю указать на сколько дней отключить уведомления.
 * Используется при выборе режима "Временно отключить".
 * 
 * Ограничения:
 * - Только цифры
 * - Максимум 3 цифры (до 999 дней)
 * - Минимум 1 день
 * 
 * @param onDismiss callback при закрытии без сохранения
 * @param onConfirm callback при подтверждении (передается количество дней)
 */
@Composable
private fun CustomDaysDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var daysInput by remember { mutableStateOf("1") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Отключить уведомления") },
        text = {
            Column {
                Text(
                    text = "На сколько дней отключить уведомления?",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = daysInput,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() } && newValue.length <= 3) {
                            daysInput = newValue
                        }
                    },
                    label = { Text("Количество дней") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val days = daysInput.toIntOrNull() ?: 1
                    if (days > 0) {
                        onConfirm(days)
                    }
                }
            ) {
                Text("ОК")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

/**
 * Карточка сброса настроек
 * 
 * Опасная операция - сбрасывает все настройки приложения к значениям по умолчанию.
 * При клике показывается диалог подтверждения с предупреждением.
 * 
 * Что сбрасывается:
 * - Тема приложения
 * - Режим отображения
 * - Настройки обновлений
 * - Глобальные настройки уведомлений
 * 
 * Что НЕ сбрасывается:
 * - Избранные времена отправления
 * - Индивидуальные настройки уведомлений маршрутов
 * 
 * @param onResetSettings callback для запуска сброса настроек
 */
@Composable
private fun ResetSettingsCard(
    onResetSettings: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onResetSettings() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Сброс настроек",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Сброс настроек",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Вернуть к значениям по умолчанию",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Сбросить",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}


/**
 * Карточка очистки кэша приложения
 * 
 * Удаляет временные данные и кэш маршрутов.
 * Требует перезапуска приложения для применения изменений.
 * 
 * Что очищается:
 * - Кэш маршрутов
 * - Временные файлы
 * 
 * Что НЕ очищается:
 * - Настройки приложения
 * - Избранные времена
 * 
 * @param onClearCache callback для очистки кэша
 * @param cacheCleared флаг успешной очистки кэша
 */
@Composable
private fun ClearCacheCard(
    onClearCache: () -> Unit,
    cacheCleared: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (cacheCleared) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !cacheCleared) { onClearCache() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (cacheCleared) Icons.Filled.CheckCircle else Icons.Default.Delete,
                    contentDescription = "Очистка кэша",
                    tint = if (cacheCleared) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = if (cacheCleared) "Кэш очищен" else "Очистка кэша",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (cacheCleared) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = if (cacheCleared) {
                            "Перезапустите приложение для применения"
                        } else {
                            "Удалить временные данные и кэш маршрутов"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (!cacheCleared) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Очистить",
                    tint = MaterialTheme.colorScheme.error
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Очищено",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Карточка обновления расписания из GitHub
 * 
 * Позволяет пользователю вручную обновить расписание автобусов
 * из удалённого JSON файла на GitHub.
 * 
 * @param isRefreshing флаг процесса обновления
 * @param refreshError сообщение об ошибке обновления
 * @param refreshSuccess флаг успешного обновления
 * @param dataVersion текущая версия данных
 * @param dataLastUpdated дата последнего обновления
 * @param onRefresh callback для запуска обновления
 * @param onClearStatus callback для очистки статуса
 */
@Composable
private fun ScheduleUpdateCard(
    isRefreshing: Boolean,
    refreshError: String?,
    refreshSuccess: Boolean,
    dataVersion: String?,
    dataLastUpdated: String?,
    onRefresh: () -> Unit,
    onClearStatus: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Заголовок и описание
            Text(
                text = "Обновление расписания",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = "Загрузить актуальное расписание из GitHub",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Информация о версии
            if (dataVersion != null || dataLastUpdated != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (dataVersion != null) {
                            Text(
                                text = "Версия данных: $dataVersion",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (dataLastUpdated != null) {
                            Text(
                                text = "Обновлено: $dataLastUpdated",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // Кнопка обновления
            Button(
                onClick = onRefresh,
                enabled = !isRefreshing,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Обновление...")
                } else {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Обновить расписание")
                }
            }
            
            // Сообщение об успехе
            if (refreshSuccess) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Расписание успешно обновлено",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Приложение перезапустится автоматически...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        IconButton(
                            onClick = onClearStatus,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Закрыть",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
            
            // Сообщение об ошибке
            if (refreshError != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = refreshError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.weight(1f))
                        IconButton(
                            onClick = onClearStatus,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Закрыть",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Карточка настройки вибрации при уведомлениях
 * 
 * Позволяет пользователю включать/выключать вибрацию устройства
 * при получении уведомлений о предстоящем отправлении автобуса.
 * 
 * @param vibrationEnabled текущее состояние настройки вибрации
 * @param onVibrationToggle callback для изменения настройки
 */
@Composable
private fun VibrationSettingsCard(
    vibrationEnabled: Boolean,
    onVibrationToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.PhoneAndroid,
                    contentDescription = "Вибрация",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Вибрация при уведомлениях",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (vibrationEnabled) "Включена" else "Выключена",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(
                checked = vibrationEnabled,
                onCheckedChange = onVibrationToggle
            )
        }
    }
}

/**
 * Карточка навигации к экрану "О программе"
 * 
 * При клике переходит к экрану AboutScreen с информацией о приложении,
 * разработчике, обратной связью и поддержкой.
 * 
 * @param navController контроллер навигации
 */
@Composable
private fun AboutNavigationCard(
    navController: NavController?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    try {
                        navController?.navigate(Screen.About.route) {
                            launchSingleTop = true
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Navigation error to About screen")
                    }
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = stringResource(R.string.settings_about_icon_desc),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    text = stringResource(R.string.about_screen_title),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Перейти к разделу О программе",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}