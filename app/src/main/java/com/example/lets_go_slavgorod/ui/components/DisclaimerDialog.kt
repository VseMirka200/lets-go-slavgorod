package com.example.lets_go_slavgorod.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lets_go_slavgorod.R
import com.example.lets_go_slavgorod.ui.theme.lets_go_slavgorodTheme

/**
 * Диалог с предупреждением о неофициальном статусе приложения
 * 
 * Версия: 1.0
 * Последнее обновление: Октябрь 2025
 * 
 * Отображается при первом запуске приложения для информирования пользователя
 * о том, что приложение не является официальным продуктом транспортной компании.
 * 
 * Основные функции:
 * - Информирует пользователя о неофициальном статусе приложения
 * - Предупреждает о том, что приложение не связано с ООО "Транспорт"
 * - Объясняет назначение приложения (справочный сервис)
 * - Напоминает проверять актуальность расписания
 * - Позволяет пользователю подтвердить понимание
 * - Запоминает выбор пользователя через DisclaimerManager
 * 
 * Особенности:
 * - Прокручиваемое содержимое
 * - Чекбокс "Не показывать снова"
 * - Иконка предупреждения (Warning)
 * - Иконка информации (Info)
 * - Material Design 3 стиль
 * 
 * @param onDismiss вызывается при закрытии диалога без принятия
 * @param onAccept вызывается при принятии условий (без "Не показывать снова")
 * @param onDontShowAgain вызывается при выборе "Не показывать снова"
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 1.0
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisclaimerDialog(
    onDismiss: () -> Unit,
    onAccept: () -> Unit,
    onDontShowAgain: () -> Unit
) {
    var dontShowAgain by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = stringResource(R.string.disclaimer_warning_icon),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = stringResource(R.string.disclaimer_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Основное предупреждение
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = stringResource(R.string.disclaimer_info_icon),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.disclaimer_main_text),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Дополнительная информация
                Text(
                    text = stringResource(R.string.disclaimer_description_text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = stringResource(R.string.disclaimer_purpose_text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = stringResource(R.string.disclaimer_accuracy_text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Чекбокс для "Не показывать снова"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = dontShowAgain,
                        onCheckedChange = { dontShowAgain = it }
                    )
                    Text(
                        text = stringResource(R.string.disclaimer_dont_show_again),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (dontShowAgain) {
                        onDontShowAgain()
                    } else {
                        onAccept()
                    }
                }
            ) {
                Text(stringResource(R.string.disclaimer_button_understand))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(stringResource(R.string.disclaimer_button_close))
            }
        }
    )
}

// =============================================================================
//                              PREVIEWS
// =============================================================================

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewDisclaimerDialog() {
    lets_go_slavgorodTheme {
        DisclaimerDialog(
            onDismiss = {},
            onAccept = {},
            onDontShowAgain = {}
        )
    }
}
