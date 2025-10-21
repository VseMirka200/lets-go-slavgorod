package com.example.lets_go_slavgorod.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lets_go_slavgorod.core.Constants

/**
 * Селектор времени уведомления с модальным окном
 * 
 * Позволяет выбрать за сколько минут до отправления показывать уведомление.
 * 
 * @author VseMirka200
 * @version 2.0
 * @since 2.1
 */
@Composable
fun NotificationTimeSelector(
    selectedMinutes: Int,
    onMinutesSelected: (Int) -> Unit,
    useGlobal: Boolean = false,
    onUseGlobalChange: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Constants.SETTINGS_ITEM_SPACING.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = !useGlobal
                ) { showDialog = true },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Время уведомления",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (useGlobal) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = "За сколько минут до отправления",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Text(
                text = formatMinutes(selectedMinutes),
                style = MaterialTheme.typography.bodyLarge,
                color = if (useGlobal) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
        }
        
        // Опция "Использовать глобальную настройку" (только для настроек маршрута)
        if (onUseGlobalChange != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Использовать глобальную настройку",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = useGlobal,
                    onCheckedChange = onUseGlobalChange
                )
            }
        }
    }
    
    // Модальное окно для выбора времени
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(
                    text = "Выберите время уведомления",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "За сколько минут до отправления показывать уведомление",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Constants.NOTIFICATION_TIME_OPTIONS.forEach { minutes ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = minutes == selectedMinutes,
                                    onClick = {
                                        onMinutesSelected(minutes)
                                        showDialog = false
                                    }
                                )
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RadioButton(
                                selected = minutes == selectedMinutes,
                                onClick = null
                            )
                            Text(
                                text = formatMinutes(minutes),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Закрыть")
                }
            }
        )
    }
}

/**
 * Форматирует минуты в читаемый вид
 */
private fun formatMinutes(minutes: Int): String {
    return when {
        minutes < 60 -> "$minutes мин"
        minutes == 60 -> "1 час"
        else -> "${minutes / 60} ч ${minutes % 60} мин"
    }
}

