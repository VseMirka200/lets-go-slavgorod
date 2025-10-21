package com.example.lets_go_slavgorod.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lets_go_slavgorod.ui.components.SettingsTopBar
import com.example.lets_go_slavgorod.ui.viewmodel.ContextViewModelFactory
import com.example.lets_go_slavgorod.ui.viewmodel.DisplaySettingsViewModel
import com.example.lets_go_slavgorod.ui.viewmodel.RouteDisplayMode
import com.example.lets_go_slavgorod.ui.viewmodel.ThemeViewModel
import com.example.lets_go_slavgorod.ui.viewmodel.AppTheme
import com.example.lets_go_slavgorod.core.Constants

/**
 * Экран настроек отображения
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 2.1
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplaySettingsScreen(
    themeViewModel: ThemeViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val displaySettingsViewModel: DisplaySettingsViewModel = viewModel(
        factory = ContextViewModelFactory.create(context) { DisplaySettingsViewModel(it) }
    )
    
    val currentDisplayMode = displaySettingsViewModel.displayMode.collectAsStateWithLifecycle(initialValue = RouteDisplayMode.GRID).value
    val currentGridColumns = displaySettingsViewModel.gridColumns.collectAsStateWithLifecycle(initialValue = 2).value
    val currentTheme = themeViewModel.currentTheme.collectAsState(initial = AppTheme.SYSTEM).value
    
    Scaffold(
        topBar = {
            SettingsTopBar(
                title = "Внешний вид",
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
            // Тема
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(Constants.SETTINGS_HORIZONTAL_PADDING.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Тема",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    AppTheme.entries.forEach { theme ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RadioButton(
                                selected = currentTheme == theme,
                                onClick = { themeViewModel.setTheme(theme) }
                            )
                            Text(
                                text = when (theme) {
                                    AppTheme.SYSTEM -> "Системная"
                                    AppTheme.LIGHT -> "Светлая"
                                    AppTheme.DARK -> "Тёмная"
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
            
            // Режим отображения
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(Constants.SETTINGS_HORIZONTAL_PADDING.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Режим отображения маршрутов",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    RouteDisplayMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RadioButton(
                                selected = currentDisplayMode == mode,
                                onClick = { displaySettingsViewModel.setDisplayMode(mode) }
                            )
                            Text(
                                text = when (mode) {
                                    RouteDisplayMode.GRID -> "Сетка"
                                    RouteDisplayMode.LIST -> "Список"
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
            
            // Количество колонок (если режим сетки)
            if (currentDisplayMode == RouteDisplayMode.GRID) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(Constants.SETTINGS_HORIZONTAL_PADDING.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Количество колонок",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        
                        listOf(2, 3, 4).forEach { columns ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                RadioButton(
                                    selected = currentGridColumns == columns,
                                    onClick = { displaySettingsViewModel.setGridColumns(columns) }
                                )
                                Text(
                                    text = "$columns колонки",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

