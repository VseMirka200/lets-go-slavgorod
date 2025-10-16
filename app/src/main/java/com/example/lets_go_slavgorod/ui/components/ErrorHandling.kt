package com.example.lets_go_slavgorod.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.lets_go_slavgorod.R
import com.example.lets_go_slavgorod.data.model.AppError
import com.example.lets_go_slavgorod.data.model.getUserMessage

/**
 * Централизованная система обработки ошибок в UI
 * 
 * Предоставляет единообразные компоненты для отображения ошибок
 * во всем приложении с поддержкой различных типов ошибок.
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 2.0
 */

/**
 * Snackbar для отображения ошибок
 * 
 * @param error ошибка для отображения
 * @param onDismiss callback при закрытии
 * @param onAction callback для кнопки действия (опционально)
 */
@Composable
fun ErrorSnackbar(
    error: AppError?,
    onDismiss: () -> Unit,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    error?.let { appError ->
        Snackbar(
            modifier = modifier.padding(16.dp),
            action = {
                val actionLabelRes = when (appError) {
                    is AppError.Network.NoConnection,
                    is AppError.Network.Timeout,
                    is AppError.Network.HttpError,
                    is AppError.Network.Generic -> R.string.action_retry
                    
                    is AppError.Database.ReadError,
                    is AppError.Database.WriteError,
                    is AppError.Database.NotFound,
                    is AppError.Database.Generic -> R.string.action_ok
                    
                    is AppError.Validation.InvalidFormat,
                    is AppError.Validation.MissingField,
                    is AppError.Validation.OutOfRange -> R.string.action_understood
                    
                    is AppError.Permission.Denied,
                    is AppError.Permission.NotGranted -> R.string.action_settings
                    
                    is AppError.System.OutOfMemory,
                    is AppError.System.OutOfStorage,
                    is AppError.System.Generic -> R.string.action_ok
                    
                    is AppError.Unknown -> R.string.action_ok
                }
                
                TextButton(
                    onClick = {
                        onAction?.invoke() ?: onDismiss()
                    }
                ) {
                    Text(stringResource(actionLabelRes))
                }
            },
            dismissAction = {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_close))
                }
            }
        ) {
            Text(appError.getUserMessage())
        }
    }
}

/**
 * Полноэкранное отображение ошибки
 * 
 * @param error ошибка для отображения
 * @param onRetry callback для повторной попытки
 * @param modifier модификатор
 */
@Composable
fun ErrorScreen(
    error: AppError,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val (icon, title) = when (error) {
        is AppError.Network.NoConnection,
        is AppError.Network.Timeout,
        is AppError.Network.HttpError,
        is AppError.Network.Generic -> Icons.Default.WifiOff to "Ошибка сети"
        
        is AppError.Database.ReadError,
        is AppError.Database.WriteError,
        is AppError.Database.NotFound,
        is AppError.Database.Generic -> Icons.Default.Storage to "Ошибка базы данных"
        
        is AppError.Validation.InvalidFormat,
        is AppError.Validation.MissingField,
        is AppError.Validation.OutOfRange -> Icons.Default.Error to "Неверные данные"
        
        is AppError.Permission.Denied,
        is AppError.Permission.NotGranted -> Icons.Default.Lock to "Нет разрешений"
        
        is AppError.System.OutOfMemory,
        is AppError.System.OutOfStorage,
        is AppError.System.Generic -> Icons.Default.ErrorOutline to "Системная ошибка"
        
        is AppError.Unknown -> Icons.AutoMirrored.Filled.Help to "Неизвестная ошибка"
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = error.getUserMessage(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        onRetry?.let {
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = it,
                modifier = Modifier.fillMaxWidth(0.6f)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Повторить")
            }
        }
    }
}

/**
 * Универсальный sealed class для состояний UI
 * 
 * @param T тип данных
 */
sealed class UiState<out T> {
    /** Начальное состояние */
    object Initial : UiState<Nothing>()
    
    /** Загрузка данных */
    object Loading : UiState<Nothing>()
    
    /** Успешная загрузка с данными */
    data class Success<T>(val data: T) : UiState<T>()
    
    /** Ошибка с информацией */
    data class Error(val error: AppError) : UiState<Nothing>()
    
    /** Пустое состояние (нет данных) */
    object Empty : UiState<Nothing>()
}

/**
 * Extension функция для безопасного получения данных
 */
fun <T> UiState<T>.dataOrNull(): T? = when (this) {
    is UiState.Success -> data
    else -> null
}

/**
 * Extension функция для проверки загрузки
 */
fun <T> UiState<T>.isLoading(): Boolean = this is UiState.Loading

/**
 * Extension функция для проверки ошибки
 */
fun <T> UiState<T>.isError(): Boolean = this is UiState.Error

/**
 * Extension функция для получения ошибки
 */
fun <T> UiState<T>.errorOrNull(): AppError? = when (this) {
    is UiState.Error -> error
    else -> null
}

/**
 * Компонент для обработки различных состояний UI
 * 
 * @param state текущее состояние
 * @param onRetry callback для повтора при ошибке
 * @param loadingContent контент при загрузке
 * @param emptyContent контент при пустом состоянии
 * @param errorContent контент при ошибке
 * @param successContent контент при успехе
 */
@Composable
fun <T> HandleUiState(
    state: UiState<T>,
    onRetry: (() -> Unit)? = null,
    loadingContent: @Composable () -> Unit = { 
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    },
    emptyContent: @Composable () -> Unit = {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Inbox,
                    contentDescription = null,
                    modifier = Modifier.size(120.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Нет данных",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Здесь пока ничего нет",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    },
    errorContent: @Composable (AppError) -> Unit = { error ->
        ErrorScreen(error = error, onRetry = onRetry)
    },
    successContent: @Composable (T) -> Unit
) {
    when (state) {
        is UiState.Initial -> { /* Ничего не показываем */ }
        is UiState.Loading -> loadingContent()
        is UiState.Success -> successContent(state.data)
        is UiState.Error -> errorContent(state.error)
        is UiState.Empty -> emptyContent()
    }
}