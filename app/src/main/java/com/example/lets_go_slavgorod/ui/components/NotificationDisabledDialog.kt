package com.example.lets_go_slavgorod.ui.components

import android.content.Intent
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Диалог для информирования пользователя об отключенных уведомлениях
 * 
 * v3.0 Changes (Октябрь 2025):
 * - Оптимизированы импорты и зависимости
 * - Улучшена производительность рендеринга
 * - Обновлены комментарии и документация
 */
@Composable
fun NotificationDisabledDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Уведомления отключены") },
        text = {
            Text(
                "Уведомления о времени отправления автобусов отключены в настройках системы. " +
                "Для получения уведомлений включите их в настройках приложения."
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // Открываем настройки приложения
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    context.startActivity(intent)
                    onDismiss()
                }
            ) {
                Text("Открыть настройки")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}