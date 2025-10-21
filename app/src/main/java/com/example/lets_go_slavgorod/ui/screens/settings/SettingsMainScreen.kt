package com.example.lets_go_slavgorod.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.lets_go_slavgorod.ui.navigation.Screen
import com.example.lets_go_slavgorod.ui.components.SettingsTopBar
import com.example.lets_go_slavgorod.core.Constants

/**
 * Главный экран настроек со списком всех разделов
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 2.1
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsMainScreen(
    navController: NavController,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            SettingsTopBar(
                title = "Настройки",
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
            // Внешний вид
            SettingsItem(
                icon = Icons.Default.Palette,
                title = "Внешний вид",
                subtitle = "Тема и отображение",
                onClick = { navController.navigate(Screen.DisplaySettings.route) }
            )
            
            // Обновления
            SettingsItem(
                icon = Icons.Default.Update,
                title = "Обновления",
                subtitle = "Автообновления приложения и расписания",
                onClick = { navController.navigate(Screen.UpdateSettings.route) }
            )
            
            // Уведомления
            SettingsItem(
                icon = Icons.Default.Notifications,
                title = "Уведомления",
                subtitle = "Время, тихий режим, вибрация",
                onClick = { navController.navigate(Screen.NotificationSettings.route) }
            )
            
            // Управление данными
            SettingsItem(
                icon = Icons.Default.Storage,
                title = "Управление данными",
                subtitle = "Обновление, кэш, сброс",
                onClick = { navController.navigate(Screen.DataManagement.route) }
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // О приложении
            SettingsItem(
                icon = Icons.Default.Info,
                title = "О приложении",
                subtitle = "Версия, разработчики, обратная связь",
                onClick = { navController.navigate(Screen.About.route) }
            )
        }
    }
}

/**
 * Элемент списка настроек
 */
@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Constants.SETTINGS_HORIZONTAL_PADDING.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Перейти",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

