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
import com.example.lets_go_slavgorod.ui.components.StyledDropdownMenu
import com.example.lets_go_slavgorod.ui.components.StyledDropdownMenuItem
import com.example.lets_go_slavgorod.ui.viewmodel.QuietMode
import com.example.lets_go_slavgorod.ui.viewmodel.QuietModeViewModel
import com.example.lets_go_slavgorod.ui.viewmodel.RouteDisplayMode
import com.example.lets_go_slavgorod.ui.viewmodel.ThemeViewModel
import com.example.lets_go_slavgorod.ui.viewmodel.UpdateMode
import com.example.lets_go_slavgorod.ui.viewmodel.UpdateSettingsViewModel
import timber.log.Timber

/**
 * Экран настроек приложения
 * 
 * Содержит настройки для:
 * - Темы приложения (светлая/темная/системная)
 * - Обновлений (автоматические/ручные/отключено)
 * - О программе (информация о приложении, ссылки, обратная связь, поддержка)
 * 
 * @param modifier модификатор для настройки внешнего вида
 * @param themeViewModel ViewModel для управления темой приложения
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
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
                    fontWeight = FontWeight.Bold,
                    fontSize = MaterialTheme.typography.titleMedium.fontSize * 1.15f // +2dp эффект
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
                    fontWeight = FontWeight.Bold,
                    fontSize = MaterialTheme.typography.titleMedium.fontSize * 1.15f
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
                    fontWeight = FontWeight.Bold,
                    fontSize = MaterialTheme.typography.titleMedium.fontSize * 1.15f
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
                    fontWeight = FontWeight.Bold,
                    fontSize = MaterialTheme.typography.titleMedium.fontSize * 1.15f
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
            
            Spacer(Modifier.height(24.dp))

            // Заголовок секции "Сброс настроек"
            Text(
                text = "Сброс настроек",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = MaterialTheme.typography.titleMedium.fontSize * 1.15f
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
                    fontWeight = FontWeight.Bold,
                    fontSize = MaterialTheme.typography.titleMedium.fontSize * 1.15f
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
 * Объединенная карточка настроек отображения
 * 
 * Содержит настройки режима отображения (клетка/список) и количества колонок
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

                Box {
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
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = "Выбрать режим отображения",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    StyledDropdownMenu(
                        expanded = showDisplayModeDropdown,
                        onDismissRequest = { onShowDisplayModeDropdownChange(false) }
                    ) {
                        displayModeOptions.forEach { (mode, label) ->
                            StyledDropdownMenuItem(
                                text = label,
                                selected = mode == currentDisplayMode,
                                onClick = {
                                    onDisplayModeSelected(mode)
                                    onShowDisplayModeDropdownChange(false)
                                }
                            )
                        }
                    }
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
                    
                    Box {
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
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Выбрать количество колонок",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        StyledDropdownMenu(
                            expanded = showColumnsDropdown,
                            onDismissRequest = { onShowColumnsDropdownChange(false) }
                        ) {
                            columnsOptions.forEach { (columns, label) ->
                                StyledDropdownMenuItem(
                                    text = label,
                                    selected = columns == currentGridColumns,
                                    onClick = {
                                        onColumnsSelected(columns)
                                        onShowColumnsDropdownChange(false)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Карточка настроек темы приложения
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
            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .clickable { onShowThemeDropdownChange(true) }
                ) {
                    Text(
                        text = themeOptions.find { it.first == currentAppTheme }?.second ?: "Как в системе",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = stringResource(R.string.settings_select_notification_mode_desc),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
            }

            StyledDropdownMenu(
                expanded = showThemeDropdown,
                onDismissRequest = { onShowThemeDropdownChange(false) }
            ) {
                themeOptions.forEach { (theme, label) ->
                    StyledDropdownMenuItem(
                        text = label,
                        selected = theme == currentAppTheme,
                        onClick = {
                            onThemeSelected(theme)
                            onShowThemeDropdownChange(false)
                        }
                    )
                }
            }
            }
        }
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
                    Box {
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
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = "Выбрать режим обновлений",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                }

                    StyledDropdownMenu(
                        expanded = showUpdateModeDropdown,
                        onDismissRequest = { onShowUpdateModeDropdownChange(false) }
                    ) {
                        updateModeOptions.forEach { mode ->
                            StyledDropdownMenuItem(
                                text = when (mode) {
                                    UpdateMode.AUTOMATIC -> "Автоматическая проверка"
                                    UpdateMode.MANUAL -> "Только ручная проверка"
                                    UpdateMode.DISABLED -> "Отключено"
                                },
                                selected = mode == currentUpdateMode,
                                onClick = {
                                    onUpdateModeSelected(mode)
                                    onShowUpdateModeDropdownChange(false)
                                }
                            )
                        }
                    }
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
 * Карточка настроек уведомлений
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
                Box {
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
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = "Выбрать тему",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    StyledDropdownMenu(
                        expanded = showQuietModeDropdown,
                        onDismissRequest = { onShowQuietModeDropdownChange(false) }
                    ) {
                        quietModeOptions.forEach { mode ->
                            StyledDropdownMenuItem(
                                text = mode.displayName,
                                selected = mode == currentQuietMode,
                                onClick = {
                                    if (mode == QuietMode.CUSTOM_DAYS) {
                                        showDaysDialog = true
                                        onShowQuietModeDropdownChange(false)
                                    } else {
                                        onQuietModeSelected(mode, 0)
                                        onShowQuietModeDropdownChange(false)
                                    }
                                }
                            )
                        }
                    }
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
 * Диалог для ввода количества дней
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
 * Карточка очистки кэша
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
 * Карточка навигации к экрану "О программе"
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