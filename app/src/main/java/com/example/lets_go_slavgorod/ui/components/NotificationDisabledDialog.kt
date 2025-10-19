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
 * Показывается когда приложение пытается отправить уведомление,
 * но уведомления отключены в системных настройках.
 * 
 * Предлагает пользователю перейти в настройки приложения для включения уведомлений.
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

