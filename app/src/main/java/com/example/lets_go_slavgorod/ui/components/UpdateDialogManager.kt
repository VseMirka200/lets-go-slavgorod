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
 * Отвечает за:
 * - Управление состоянием показа диалога обновления
 * - Отображение диалога с информацией о новой версии
 * - Обработку действий пользователя (скачать/отложить)
 * 
 * @author VseMirka200
 * @version 1.0
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
