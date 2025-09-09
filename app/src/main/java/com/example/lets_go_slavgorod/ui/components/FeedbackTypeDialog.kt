package com.example.lets_go_slavgorod.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * v3.0 Changes (Октябрь 2025):
 * - Оптимизированы импорты и зависимости
 * - Улучшена производительность рендеринга
 * - Обновлены комментарии и документация
 */
enum class FeedbackType(
    val label: String,
    val icon: ImageVector,
    val emailPrefix: String,
    val description: String
) {
    BUG(
        label = "🐛 Сообщить о баге",
        icon = Icons.Default.BugReport,
        emailPrefix = "[БАГ]",
        description = "Что-то работает не так?"
    ),
    QUESTION(
        label = "❓ Задать вопрос",
        icon = Icons.AutoMirrored.Filled.HelpOutline,
        emailPrefix = "[ВОПРОС]",
        description = "Нужна помощь или информация?"
    ),
    SUGGESTION(
        label = "💡 Предложить идею",
        icon = Icons.Default.Lightbulb,
        emailPrefix = "[ПРЕДЛОЖЕНИЕ]",
        description = "Есть идея для улучшения?"
    )
}

/**
 * Результат выбора типа обратной связи с опцией отправки логов
 */
data class FeedbackSelection(
    val type: FeedbackType,
    val includeLogs: Boolean
)

/**
 * Диалог выбора типа обратной связи с опцией отправки логов
 * 
 * Позволяет пользователю выбрать тип обращения (баг, вопрос, предложение)
 * и решить, отправлять ли логи вместе с обращением.
 * 
 * @param onDismiss callback при закрытии диалога
 * @param onTypeSelected callback при выборе типа обратной связи с опцией логов
 */
@Composable
fun FeedbackTypeDialog(
    onDismiss: () -> Unit,
    onTypeSelected: (FeedbackSelection) -> Unit
) {
    var includeLogs by remember { mutableStateOf(true) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Обратная связь",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Выберите тип обращения:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                FeedbackType.entries.forEach { type ->
                    FeedbackTypeCard(
                        type = type,
                        onClick = { 
                            onTypeSelected(FeedbackSelection(type, includeLogs))
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Опция отправки логов
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { includeLogs = !includeLogs }
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = includeLogs,
                        onCheckedChange = { includeLogs = it }
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "📋 Отправить логи приложения",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Text(
                            text = "Поможет быстрее решить проблему",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = { },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

/**
 * Карточка выбора типа обратной связи
 */
@Composable
private fun FeedbackTypeCard(
    type: FeedbackType,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = type.icon,
                contentDescription = type.label,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = type.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = type.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
