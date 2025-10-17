package com.example.lets_go_slavgorod.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.lets_go_slavgorod.R

/**
 * Диалог уведомления о доступном обновлении
 * 
 * Версия: 1.0
 * Последнее обновление: Октябрь 2025
 * 
 * Информирует пользователя о новой версии приложения с подробным
 * описанием изменений и возможностью скачать обновление.
 * 
 * Основные функции:
 * - Отображение информации о новой версии
 * - Показ описания изменений (release notes) в прокручиваемой области
 * - Кнопка "Скачать" для загрузки обновления
 * - Кнопка "Позже" для отложения обновления
 * - Красивый дизайн с иконками и Material Design 3
 * 
 * Особенности:
 * - Прокручиваемое содержимое для длинных release notes
 * - Цветовая схема с акцентом на primary цвет
 * - Подробная информация в карточке
 * - Иконки для визуальной идентификации
 * 
 * @param versionName название новой версии (например, "v1.9")
 * @param releaseNotes описание изменений в обновлении (опционально)
 * @param downloadUrl ссылка для скачивания обновления
 * @param onDismissRequest вызывается при закрытии диалога
 * @param onDownload вызывается при нажатии "Скачать" (передается downloadUrl)
 * @param onLater вызывается при нажатии "Позже"
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 2.0
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateAvailableDialog(
    versionName: String,
    releaseNotes: String?,
    downloadUrl: String,
    onDismissRequest: () -> Unit,
    onDownload: (String) -> Unit,
    onLater: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Update,
                    contentDescription = "Обновление",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Доступно обновление",
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
                // Информация о версии
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Информация",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Новая версия: $versionName",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Описание изменений
                if (!releaseNotes.isNullOrBlank()) {
                    Text(
                        text = "Что нового:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Text(
                            text = releaseNotes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else {
                    Text(
                        text = "Обновление содержит улучшения и исправления ошибок.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Рекомендация
                Text(
                    text = "Рекомендуется обновить приложение для получения новых функций и исправлений.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onDownload(downloadUrl) }
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Скачать",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("Скачать")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onLater
            ) {
                Text("Позже")
            }
        }
    )
}
