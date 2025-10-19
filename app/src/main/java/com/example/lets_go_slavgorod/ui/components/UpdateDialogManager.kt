package com.example.lets_go_slavgorod.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.lets_go_slavgorod.ui.screens.UpdateAvailableDialog

/**
 * Менеджер для показа диалога обновления приложения
 * 
 * Версия: 1.0
 * Последнее обновление: Октябрь 2025
 * 
 * Управляет жизненным циклом диалога уведомления об обновлениях.
 * Автоматически показывает диалог когда доступно новое обновление
 * и скрывает его после действий пользователя.
 * 
 * Отвечает за:
 * - Управление состоянием показа диалога обновления
 * - Автоматическое отображение при наличии обновления
 * - Отображение диалога с информацией о новой версии
 * - Обработку действий пользователя (скачать/отложить/закрыть)
 * - Очистку состояния после действий
 * 
 * Использование:
 * ```kotlin
 * UpdateDialogManager(
 *     availableUpdateVersion = "v1.9",
 *     availableUpdateUrl = "https://...",
 *     availableUpdateNotes = "Что нового...",
 *     onDownloadUpdate = { url -> openBrowser(url) },
 *     onClearAvailableUpdate = { clearCache() }
 * )
 * ```
 * 
 * @param availableUpdateVersion версия доступного обновления или null
 * @param availableUpdateUrl URL для скачивания обновления или null
 * @param availableUpdateNotes описание изменений или null
 * @param onDownloadUpdate callback для скачивания обновления
 * @param onClearAvailableUpdate callback для очистки информации об обновлении
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 2.0
 */
@Composable
fun UpdateDialogManager(
    availableUpdateVersion: String?,
    availableUpdateUrl: String?,
    availableUpdateNotes: String?,
    onDownloadUpdate: (String) -> Unit,
    onClearAvailableUpdate: () -> Unit
) {
    var showUpdateDialog by remember { mutableStateOf(false) }
    
    // Показываем диалог, если есть доступное обновление
    LaunchedEffect(availableUpdateVersion, availableUpdateUrl) {
        if (!availableUpdateVersion.isNullOrBlank() && !availableUpdateUrl.isNullOrBlank()) {
            showUpdateDialog = true
        }
    }
    
    // Диалог обновления
    if (showUpdateDialog && !availableUpdateVersion.isNullOrBlank() && !availableUpdateUrl.isNullOrBlank()) {
        UpdateAvailableDialog(
            versionName = availableUpdateVersion,
            releaseNotes = availableUpdateNotes,
            downloadUrl = availableUpdateUrl,
            onDismissRequest = {
                showUpdateDialog = false
                onClearAvailableUpdate()
            },
            onDownload = { url ->
                showUpdateDialog = false
                onDownloadUpdate(url)
                onClearAvailableUpdate()
            },
            onLater = {
                showUpdateDialog = false
                onClearAvailableUpdate()
            }
        )
    }
}
