package com.example.lets_go_slavgorod.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lets_go_slavgorod.ui.components.SettingsTopBar
import com.example.lets_go_slavgorod.ui.viewmodel.ContextViewModelFactory
import com.example.lets_go_slavgorod.ui.viewmodel.DataManagementViewModel
import com.example.lets_go_slavgorod.utils.Constants
import timber.log.Timber

/**
 * Экран управления данными
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 2.1
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataManagementScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dataManagementViewModel: DataManagementViewModel = viewModel(
        factory = ContextViewModelFactory.create(context) { DataManagementViewModel(it) }
    )
    
    val isRefreshing by dataManagementViewModel.isRefreshingSchedule.collectAsState()
    val refreshError by dataManagementViewModel.scheduleRefreshError.collectAsState()
    val refreshSuccess by dataManagementViewModel.scheduleRefreshSuccess.collectAsState()
    val dataVersion by dataManagementViewModel.dataVersion.collectAsState()
    val dataLastUpdated by dataManagementViewModel.dataLastUpdated.collectAsState()
    
    var showResetDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var cacheCleared by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            SettingsTopBar(
                title = "Управление данными",
                onBackClick = onBackClick
            )
        },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0)
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(Constants.SETTINGS_HORIZONTAL_PADDING.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Constants.SETTINGS_ITEM_SPACING.dp)
        ) {
            // Обновление расписания
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(Constants.SETTINGS_HORIZONTAL_PADDING.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Обновление расписания",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                    )
                    
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
                    
                    Button(
                        onClick = { dataManagementViewModel.refreshScheduleFromGitHub() },
                        enabled = !isRefreshing,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Default.Download, null, Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(if (isRefreshing) "Обновление..." else "Обновить расписание")
                    }
                    
                    if (refreshSuccess) {
                        Text(
                            text = "✅ Расписание успешно обновлено",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    if (refreshError != null) {
                        Text(
                            text = "❌ $refreshError",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            // Очистка кэша
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(Constants.SETTINGS_HORIZONTAL_PADDING.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Очистка кэша",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                    )
                    
                    Text(
                        text = "Удаление временных файлов для освобождения памяти",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    OutlinedButton(
                        onClick = { showClearCacheDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Очистить кэш")
                    }
                    
                    if (cacheCleared) {
                        Text(
                            text = "✅ Кэш успешно очищен",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            // Сброс настроек
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(Constants.SETTINGS_HORIZONTAL_PADDING.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Сброс настроек",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.error
                    )
                    
                    Text(
                        text = "Вернуть все настройки к значениям по умолчанию",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    OutlinedButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Close, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Сбросить настройки")
                    }
                }
            }
        }
    }
    
    // Диалог подтверждения сброса
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Сбросить настройки?") },
            text = { Text("Все настройки приложения будут сброшены к значениям по умолчанию. Избранные маршруты сохранятся.") },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Отмена")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
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
            icon = { Icon(Icons.Default.Delete, null) },
            title = { Text("Очистить кэш?") },
            text = { Text("Временные файлы будут удалены. Приложение повторно загрузит данные при следующем запуске.") },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text("Отмена")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearCacheDialog = false
                        // Очищаем кэш вручную
                        try {
                            val cacheDir = context.cacheDir
                            cacheDir.deleteRecursively()
                            cacheCleared = true
                            Timber.d("Cache cleared successfully")
                        } catch (e: Exception) {
                            Timber.e(e, "Error clearing cache")
                        }
                    }
                ) {
                    Text("Да, очистить")
                }
            }
        )
    }
}

