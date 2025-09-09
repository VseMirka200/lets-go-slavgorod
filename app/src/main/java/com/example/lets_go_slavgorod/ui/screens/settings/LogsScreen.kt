package com.example.lets_go_slavgorod.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lets_go_slavgorod.core.Constants
import com.example.lets_go_slavgorod.ui.components.SettingsTopBar
import com.example.lets_go_slavgorod.ui.viewmodel.LogsViewModel
import com.example.lets_go_slavgorod.ui.viewmodel.LogEntry

/**
 * Экран просмотра и экспорта логов приложения
 * 
 * Предоставляет полнофункциональный интерфейс для работы с логами:
 * - Просмотр логов в реальном времени с цветовой индикацией уровней
 * - Экспорт логов в файл с поддержкой кириллицы (два метода)
 * - Управление историей логов (обновление, очистка, тестирование)
 * - Информационная панель с статистикой логов
 * 
 * Архитектура UI:
 * - Использует MVVM паттерн с LogsViewModel
 * - Реактивное обновление через StateFlow
 * - Material Design 3 компоненты
 * - Адаптивная компоновка с LazyColumn для производительности
 * 
 * Особенности:
 * - Автоматическая загрузка логов при открытии экрана
 * - Snackbar уведомления о результатах операций
 * - Блокировка UI во время экспорта
 * - Цветовая индикация уровней важности логов
 * - Поддержка кириллицы в отображении и экспорте
 * 
 * v3.0 Changes (Октябрь 2025):
 * - Добавлена поддержка кириллицы в экспорте
 * - Добавлен альтернативный метод экспорта
 * - Улучшена производительность отображения
 * - Обновлены комментарии и документация
 * - Добавлены кнопки управления историей логов
 * 
 * @param logsViewModel ViewModel для управления логами
 * @param onBackClick callback для возврата на предыдущий экран
 * @param modifier модификатор для настройки компоновки
 * 
 * @author VseMirka200
 * @version 3.0
 * @since 2.1
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(
    logsViewModel: LogsViewModel = viewModel(),
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // =====================================================================================
    //                              СОСТОЯНИЕ И ДАННЫЕ
    // =====================================================================================
    
    val context = LocalContext.current
    val logs by logsViewModel.logs.collectAsState()
    val isExporting by logsViewModel.isExporting.collectAsState()
    val exportMessage by logsViewModel.exportMessage.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    // =====================================================================================
    //                              ЭФФЕКТЫ И ИНИЦИАЛИЗАЦИЯ
    // =====================================================================================
    
    // Загружаем логи при первом запуске экрана
    LaunchedEffect(Unit) {
        logsViewModel.loadLogs(context)
    }
    
    // Показываем сообщение об экспорте через Snackbar
    LaunchedEffect(exportMessage) {
        exportMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            logsViewModel.clearExportMessage()
        }
    }
    
    Scaffold(
        topBar = {
            SettingsTopBar(
                title = "Логи",
                onBackClick = onBackClick
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0)
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // =====================================================================================
            //                              ПАНЕЛЬ УПРАВЛЕНИЯ
            // =====================================================================================
            
            /**
             * Панель управления логами с кнопками для основных операций:
             * - Обновление списка логов
             * - Экспорт логов (основной метод с BOM)
             * - Добавление тестового лога
             * - Очистка истории логов
             * - Альтернативный экспорт (PrintWriter)
             */
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Constants.SETTINGS_HORIZONTAL_PADDING.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Первая строка кнопок
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { logsViewModel.loadLogs(context) },
                            enabled = !isExporting,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Обновить",
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Обновить",
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        
                        Button(
                            onClick = { logsViewModel.exportLogs(context) },
                            enabled = !isExporting && logs.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isExporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Экспорт",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = if (isExporting) "Экспорт..." else "Экспорт",
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                    
                    // Вторая строка кнопок
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { logsViewModel.addTestLog() },
                            enabled = !isExporting,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Добавить тест",
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Тест лог",
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        
                        Button(
                            onClick = { logsViewModel.clearLogs() },
                            enabled = !isExporting && logs.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Очистить",
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Очистить",
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                    
                    // Третья строка кнопок
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { logsViewModel.exportLogsAlternative(context) },
                            enabled = !isExporting && logs.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Альтернативный экспорт",
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Экспорт Alt",
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        
                        // Пустая кнопка для симметрии
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            
            // =====================================================================================
            //                              ИНФОРМАЦИОННАЯ ПАНЕЛЬ
            // =====================================================================================
            
            /**
             * Информационная панель с статистикой логов:
             * - Общее количество записей
             * - Время последнего обновления
             * - Описание методов экспорта
             * - Информация о хранении логов
             */
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Constants.SETTINGS_HORIZONTAL_PADDING.dp, vertical = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Всего записей: ${logs.size}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Text(
                            text = "Последнее обновление: ${if (logs.isNotEmpty()) logs.first().formattedTimestamp else "—"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Text(
                        text = "История логов сохраняется в памяти приложения и экспортируется в файл",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = "Два метода экспорта: основной (с BOM) и альтернативный (PrintWriter)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(horizontal = Constants.SETTINGS_HORIZONTAL_PADDING.dp))
            
            // =====================================================================================
            //                              СПИСОК ЛОГОВ
            // =====================================================================================
            
            /**
             * Основной список логов с адаптивным отображением:
             * - LazyColumn для производительности с большим количеством логов
             * - Пустое состояние если логи отсутствуют
             * - Цветовая индикация уровней важности
             * - Ограничение количества строк для компактности
             */
            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Логи не найдены",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = Constants.SETTINGS_HORIZONTAL_PADDING.dp,
                        vertical = 8.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(logs) { log ->
                        LogEntryItem(log = log)
                    }
                }
            }
        }
    }
}

/**
 * Компонент для отображения одной записи лога в списке
 * 
 * Представляет отдельную запись лога с полной информацией:
 * - Цветовой индикатор уровня важности
 * - Форматированное время создания
 * - Тег компонента-источника
 * - Текст сообщения с ограничением строк
 * 
 * Дизайн:
 * - Компактная карточка с минимальными отступами
 * - Цветовая схема соответствует Material Design 3
 * - Адаптивная компоновка для разных размеров экрана
 * - Поддержка кириллицы в тексте сообщений
 * 
 * @param log объект LogEntry для отображения
 * @param modifier модификатор для настройки компоновки
 * 
 * @author VseMirka200
 * @version 3.0
 * @since 2.1
 */
@Composable
private fun LogEntryItem(
    log: LogEntry,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // =====================================================================================
            //                              ИНДИКАТОР УРОВНЯ
            // =====================================================================================
            
            /**
             * Цветовой индикатор уровня важности лога
             * 
             * Маленький круглый индикатор слева от содержимого,
             * цвет которого соответствует уровню важности:
             * - ERROR: красный
             * - WARN: оранжевый  
             * - INFO: синий
             * - DEBUG: зеленый
             * - Остальные: серый
             */
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(log.levelColor))
            )
            
            // =====================================================================================
            //                              СОДЕРЖИМОЕ ЛОГА
            // =====================================================================================
            
            /**
             * Основное содержимое записи лога:
             * - Заголовок с временем и уровнем
             * - Тег компонента-источника
             * - Текст сообщения с ограничением строк
             */
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Заголовок с временем и уровнем важности
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = log.formattedTimestamp,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = log.level,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(log.levelColor)
                    )
                }
                
                // Тег компонента-источника лога
                Text(
                    text = log.tag,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                // Текст сообщения с ограничением строк для компактности
                Text(
                    text = log.message,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
