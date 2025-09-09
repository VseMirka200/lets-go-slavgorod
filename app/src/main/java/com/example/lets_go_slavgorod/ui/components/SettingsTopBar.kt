package com.example.lets_go_slavgorod.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight

/**
 * Унифицированная верхняя панель для всех экранов настроек
 * 
 * @param title основной заголовок
 * @param subtitle подзаголовок (опционально)
 * @param onBackClick действие при нажатии кнопки назад
 * 
 * v3.0 Changes (Октябрь 2025):
 * - Оптимизированы импорты и зависимости
 * - Улучшена производительность рендеринга
 * - Обновлены комментарии и документация
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTopBar(
    title: String,
    onBackClick: () -> Unit,
    subtitle: String? = null
) {
    TopAppBar(
        title = {
            if (subtitle != null) {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            } else {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        windowInsets = WindowInsets(0)
    )
}