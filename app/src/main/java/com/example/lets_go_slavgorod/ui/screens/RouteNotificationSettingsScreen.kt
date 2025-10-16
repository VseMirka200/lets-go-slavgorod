package com.example.lets_go_slavgorod.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lets_go_slavgorod.data.model.BusRoute
import com.example.lets_go_slavgorod.ui.viewmodel.NotificationMode
import com.example.lets_go_slavgorod.ui.viewmodel.NotificationSettingsViewModel
import com.example.lets_go_slavgorod.ui.components.StyledDropdownMenu
import com.example.lets_go_slavgorod.ui.components.StyledDropdownMenuItem
import java.time.DayOfWeek

/**
 * Экран настроек уведомлений для конкретного маршрута
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteNotificationSettingsScreen(
    route: BusRoute,
    notificationSettingsViewModel: NotificationSettingsViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentNotificationMode by notificationSettingsViewModel.getRouteNotificationMode(route.id).collectAsState()
    val selectedDays by notificationSettingsViewModel.getRouteSelectedDays(route.id).collectAsState()
    
    var showModeDropdown by remember { mutableStateOf(false) }

    val notificationModeOptions = arrayOf(
        NotificationMode.ALL_DAYS,
        NotificationMode.WEEKDAYS,
        NotificationMode.SELECTED_DAYS,
        NotificationMode.DISABLED
    )
    
    val dayOptions = DayOfWeek.entries
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Уведомления",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
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
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Режим уведомлений
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Выбрать режим отображения",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    ExposedDropdownMenuBox(
                        expanded = showModeDropdown,
                        onExpandedChange = { showModeDropdown = !showModeDropdown }
                    ) {
                        OutlinedTextField(
                            value = when (currentNotificationMode) {
                                NotificationMode.ALL_DAYS -> "Все дни"
                                NotificationMode.WEEKDAYS -> "Только будни"
                                NotificationMode.SELECTED_DAYS -> "Выбранные дни"
                                NotificationMode.DISABLED -> "Отключено"
                            },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Выберите режим") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showModeDropdown) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(type = androidx.compose.material3.MenuAnchorType.PrimaryNotEditable)
                        )
                        
                        ExposedDropdownMenu(
                            expanded = showModeDropdown,
                            onDismissRequest = { showModeDropdown = false }
                        ) {
                            notificationModeOptions.forEach { mode ->
                                StyledDropdownMenuItem(
                                    text = when (mode) {
                                        NotificationMode.ALL_DAYS -> "Все дни"
                                        NotificationMode.WEEKDAYS -> "Только будни"
                                        NotificationMode.SELECTED_DAYS -> "Выбранные дни"
                                        NotificationMode.DISABLED -> "Отключено"
                                    },
                                    selected = mode == currentNotificationMode,
                                    onClick = {
                                        notificationSettingsViewModel.setRouteNotificationMode(route.id, mode)
                                        showModeDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
            
            // Выбор дней (только если выбран режим "Выбранные дни")
            if (currentNotificationMode == NotificationMode.SELECTED_DAYS) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Дни недели",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        dayOptions.forEach { day ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when (day) {
                                        DayOfWeek.MONDAY -> "Понедельник"
                                        DayOfWeek.TUESDAY -> "Вторник"
                                        DayOfWeek.WEDNESDAY -> "Среда"
                                        DayOfWeek.THURSDAY -> "Четверг"
                                        DayOfWeek.FRIDAY -> "Пятница"
                                        DayOfWeek.SATURDAY -> "Суббота"
                                        DayOfWeek.SUNDAY -> "Воскресенье"
                                    },
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                
                                Checkbox(
                                    checked = selectedDays.contains(day),
                                    onCheckedChange = { isChecked ->
                                        val newDays = if (isChecked) {
                                            selectedDays + day
                                        } else {
                                            selectedDays - day
                                        }
                                        notificationSettingsViewModel.setRouteSelectedDays(route.id, newDays)
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
