package com.example.lets_go_slavgorod.ui.screens.settings

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lets_go_slavgorod.ui.components.SettingsTopBar
import com.example.lets_go_slavgorod.ui.viewmodel.ContextViewModelFactory
import com.example.lets_go_slavgorod.ui.viewmodel.UpdateMode
import com.example.lets_go_slavgorod.ui.viewmodel.UpdateSettingsViewModel
import com.example.lets_go_slavgorod.core.Constants
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

/**
 * Экран настроек обновлений
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 2.1
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateSettingsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val updateSettingsViewModel: UpdateSettingsViewModel = viewModel(
        factory = ContextViewModelFactory.create(context) { UpdateSettingsViewModel(it) }
    )
    
    val currentUpdateMode by updateSettingsViewModel.currentUpdateMode.collectAsState(initial = UpdateMode.AUTOMATIC)
    val isCheckingUpdates by updateSettingsViewModel.isCheckingUpdates.collectAsState(initial = false)
    val lastUpdateCheckTime by updateSettingsViewModel.lastUpdateCheckTime.collectAsState(initial = null)
    val availableUpdateVersion by updateSettingsViewModel.availableUpdateVersion.collectAsState(initial = null)
    val availableUpdateUrl by updateSettingsViewModel.availableUpdateUrl.collectAsState(initial = null)
    
    Scaffold(
        topBar = {
            SettingsTopBar(
                title = "Обновления",
                onBackClick = onBackClick
            )
        },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0)
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(Constants.SETTINGS_HORIZONTAL_PADDING.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Constants.SETTINGS_ITEM_SPACING.dp)
        ) {
            // Режим обновлений
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(Constants.SETTINGS_HORIZONTAL_PADDING.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Автоматические обновления",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                    )
                    
                    UpdateMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RadioButton(
                                selected = currentUpdateMode == mode,
                                onClick = { updateSettingsViewModel.setUpdateMode(mode) }
                            )
                            Text(
                                text = when (mode) {
                                    UpdateMode.AUTOMATIC -> "Автоматически"
                                    UpdateMode.MANUAL -> "Вручную"
                                    UpdateMode.DISABLED -> "Отключено"
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
            
            // Проверка обновлений
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(Constants.SETTINGS_HORIZONTAL_PADDING.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Проверка обновлений",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                    )
                    
                    if (lastUpdateCheckTime != null) {
                        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                        Text(
                            text = "Последняя проверка: ${dateFormat.format(Date(lastUpdateCheckTime!!))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Button(
                        onClick = { updateSettingsViewModel.checkForUpdates() },
                        enabled = !isCheckingUpdates,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isCheckingUpdates) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Default.Download, null, Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(if (isCheckingUpdates) "Проверка..." else "Проверить обновления")
                    }
                    
                    if (availableUpdateVersion != null) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "🎉 Доступна версия $availableUpdateVersion",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                
                                if (availableUpdateUrl != null) {
                                    TextButton(
                                        onClick = {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, availableUpdateUrl!!.toUri())
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Timber.e(e, "Failed to open update URL")
                                            }
                                        }
                                    ) {
                                        Text("Скачать обновление")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

