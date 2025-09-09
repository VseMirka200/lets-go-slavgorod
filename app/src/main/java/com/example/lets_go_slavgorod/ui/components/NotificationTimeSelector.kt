package com.example.lets_go_slavgorod.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.lets_go_slavgorod.core.Constants

/**
 * Селектор времени уведомления с модальным окном и полем ввода
 * 
 * Позволяет выбрать за сколько минут до отправления показывать уведомление.
 * Использует поле ввода с валидацией (не больше 30 минут).
 * 
 * v3.0 Changes (Октябрь 2025):
 * - Оптимизированы импорты и зависимости
 * - Улучшена производительность рендеринга
 * - Обновлены комментарии и документация
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
                NotificationTimeInputField(
                    selectedMinutes = selectedMinutes,
                    onMinutesSelected = onMinutesSelected,
                    onDialogClose = { showDialog = false }
                )
            },
            confirmButton = {
                TextButton(onClick = { 
                    // Валидация будет происходить в NotificationTimeInputField
                    showDialog = false 
                }) {
                    Text("Применить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

/**
 * Поле ввода времени уведомления в модальном окне
 * 
 * Позволяет ввести количество минут с валидацией (1-30 минут).
 * Работает с минутами напрямую для удобства пользователя.
 * 
 * @param selectedMinutes текущее выбранное количество минут
 * @param onMinutesSelected callback при выборе нового времени (в минутах)
 * @param onDialogClose callback для закрытия диалога
 */
@Composable
private fun NotificationTimeInputField(
    selectedMinutes: Int,
    onMinutesSelected: (Int) -> Unit,
    onDialogClose: () -> Unit
) {
    var inputText by remember { mutableStateOf(selectedMinutes.toString()) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "За сколько минут до отправления показывать уведомление",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        OutlinedTextField(
            value = inputText,
            onValueChange = { newValue ->
                inputText = newValue
                // Валидация при вводе
                val minutes = newValue.toIntOrNull()
                when {
                    newValue.isEmpty() -> {
                        showError = false
                    }
                    minutes == null -> {
                        showError = true
                        errorMessage = "Введите корректное число"
                    }
                    minutes < 1 -> {
                        showError = true
                        errorMessage = "Минимум 1 минута"
                    }
                    minutes > 30 -> {
                        showError = true
                        errorMessage = "Максимум 30 минут"
                    }
                    else -> {
                        showError = false
                    }
                }
            },
            label = { Text("Минуты") },
            suffix = { Text("мин") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            isError = showError,
            supportingText = if (showError) {
                { Text(errorMessage, color = MaterialTheme.colorScheme.error) }
            } else {
                { Text("От 1 до 30 минут") }
            }
        )
        
        if (showError) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
    
    // Применяем изменения при валидном вводе
    LaunchedEffect(inputText) {
        if (!showError && inputText.isNotEmpty()) {
            val minutes = inputText.toIntOrNull()
            if (minutes != null && minutes >= 1 && minutes <= 30) {
                // Передаем минуты напрямую, без конвертации в секунды
                onMinutesSelected(minutes)
            }
        }
    }
}

/**
 * Форматирует время в читаемый вид
 */
private fun formatMinutes(minutes: Int): String {
    return when {
        minutes < 60 -> "$minutes мин"
        minutes == 60 -> "1 час"
        else -> "${minutes / 60} ч ${minutes % 60} мин"
    }
}