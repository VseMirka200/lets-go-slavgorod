package com.example.lets_go_slavgorod.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lets_go_slavgorod.core.Constants
import com.example.lets_go_slavgorod.data.local.NotificationTimePreferences
import com.example.lets_go_slavgorod.ui.components.NotificationTimeSelector
import com.example.lets_go_slavgorod.ui.components.SettingsTopBar
import com.example.lets_go_slavgorod.ui.viewmodel.ContextViewModelFactory
import com.example.lets_go_slavgorod.ui.viewmodel.QuietModeViewModel
import com.example.lets_go_slavgorod.ui.viewmodel.VibrationSettingsViewModel
import kotlinx.coroutines.launch

/**
 * Экран настроек уведомлений
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 2.1
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Восстанавливаем ViewModels
    val timePreferences = remember { NotificationTimePreferences(context) }
    val globalLeadTime by timePreferences.globalLeadTime.collectAsState(initial = Constants.DEFAULT_NOTIFICATION_LEAD_TIME)
    val coroutineScope = rememberCoroutineScope()
    
    // ViewModels для настроек
    val quietModeViewModel: QuietModeViewModel = viewModel(
        factory = ContextViewModelFactory.create(context) { QuietModeViewModel(it) }
    )
    val vibrationViewModel: VibrationSettingsViewModel = viewModel(
        factory = ContextViewModelFactory.create(context) { VibrationSettingsViewModel(it) }
    )
    
    // Состояния ViewModels
    val quietMode by quietModeViewModel.quietMode.collectAsState()
    val vibrationEnabled by vibrationViewModel.vibrationEnabled.collectAsState()
    
    Scaffold(
        topBar = {
            SettingsTopBar(
                title = "Настройки уведомлений",
                onBackClick = onBackClick
            )
        },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(Constants.SETTINGS_HORIZONTAL_PADDING.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Constants.SETTINGS_ITEM_SPACING.dp)
        ) {
            // Глобальное время уведомления
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(Constants.SETTINGS_HORIZONTAL_PADDING.dp)) {
                    NotificationTimeSelector(
                        selectedMinutes = globalLeadTime,
                        onMinutesSelected = { minutes ->
                            coroutineScope.launch {
                                timePreferences.setGlobalLeadTime(minutes)
                            }
                        }
                    )
                }
            }
            
            // Настройки вибрации
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Constants.SETTINGS_HORIZONTAL_PADDING.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Вибрация",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "При получении уведомления",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = vibrationEnabled,
                        onCheckedChange = { enabled ->
                            vibrationViewModel.setVibrationEnabled(enabled)
                        }
                    )
                }
            }
            
            // Информация
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(Constants.SETTINGS_HORIZONTAL_PADDING.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "💡 Индивидуальные настройки",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Вы можете настроить время уведомления отдельно для каждого маршрута в настройках уведомлений на экране расписания.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}