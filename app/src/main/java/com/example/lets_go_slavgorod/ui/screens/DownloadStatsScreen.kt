package com.example.lets_go_slavgorod.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lets_go_slavgorod.data.remote.DownloadMetrics
import com.example.lets_go_slavgorod.data.repository.BusRouteRepository
import java.text.SimpleDateFormat
import java.util.*

/**
 * Экран статистики загрузок
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 2.1
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadStatsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { BusRouteRepository(context) }
    val stats = remember { 
        repository.getRemoteDataSource()?.getDownloadStats() ?: DownloadMetrics.DownloadStats(
            totalSuccess = 0, totalFailures = 0, totalBytes = 0, averageTimeMs = 0,
            lastSuccessTime = 0, lastFailureTime = 0, githubSuccess = 0, cacheSuccess = 0,
            assetsSuccess = 0, etagHits = 0
        )
    }
    
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Статистика загрузок") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Общая статистика
            StatsCard(
                title = "📈 Общая статистика",
                items = listOf(
                    "Успешных загрузок" to "${stats.totalSuccess}",
                    "Неудачных загрузок" to "${stats.totalFailures}",
                    "Процент успеха" to "${String.format("%.1f", stats.successRate * 100)}%",
                    "Общий размер" to formatBytes(stats.totalBytes),
                    "Средний размер" to formatBytes(stats.averageBytesPerDownload)
                )
            )
            
            // Источники данных
            StatsCard(
                title = "🌐 Источники данных",
                items = listOf(
                    "GitHub" to "${stats.githubSuccess}",
                    "Кэш" to "${stats.cacheSuccess}",
                    "Assets" to "${stats.assetsSuccess}",
                    "ETag hits" to "${stats.etagHits}",
                    "Трафик сэкономлен" to formatBytes(stats.trafficSavedByETag)
                )
            )
            
            // Производительность
            StatsCard(
                title = "⚡ Производительность",
                items = listOf(
                    "Среднее время" to "${stats.averageTimeMs}мс",
                    "Последний успех" to if (stats.lastSuccessTime > 0) {
                        dateFormat.format(Date(stats.lastSuccessTime))
                    } else "Никогда",
                    "Последняя ошибка" to if (stats.lastFailureTime > 0) {
                        dateFormat.format(Date(stats.lastFailureTime))
                    } else "Никогда"
                )
            )
        }
    }
}

@Composable
private fun StatsCard(
    title: String,
    items: List<Pair<String, String>>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            items.forEach { (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 -> "${bytes / (1024 * 1024)} МБ"
        bytes >= 1024 -> "${bytes / 1024} КБ"
        else -> "$bytes Б"
    }
}