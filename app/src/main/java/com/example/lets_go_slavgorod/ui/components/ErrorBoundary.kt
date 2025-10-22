package com.example.lets_go_slavgorod.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.lets_go_slavgorod.R
import timber.log.Timber

/**
 * Error Boundary для обработки ошибок в Compose
 * 
 * Предоставляет централизованную обработку ошибок в UI компонентах.
 * Показывает пользователю понятные сообщения об ошибках.
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 2.0
 */
@Composable
fun ErrorBoundary(
    error: Throwable?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (error != null) {
        Timber.e(error, "Error boundary caught exception")
        ErrorState(
            errorMessage = error.message ?: "Произошла неизвестная ошибка",
            onRetry = onRetry,
            modifier = modifier
        )
    } else {
        content()
    }
}

/**
 * Состояние ошибки с возможностью повтора
 */
@Composable
fun ErrorState(
    errorMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Ошибка",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            
            Text(
                text = "Ошибка",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.error
            )
            
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Повторить")
            }
        }
    }
}
