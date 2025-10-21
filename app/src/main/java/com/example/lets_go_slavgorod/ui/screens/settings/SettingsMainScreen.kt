package com.example.lets_go_slavgorod.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

/**
 * Главный экран настроек с навигационным списком
 * 
 * Модульный подход - каждая секция настроек это отдельный экран.
 * Преимущества:
 * - Быстрая загрузка (только список секций)
 * - Изолированные перекомпозиции
 * - Легко тестировать
 * - Параллельная разработка
 * 
 * @param navController контроллер навигации
 * @param onNavigateBack callback для возврата назад
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 2.1
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsMainScreen(
    navController: NavController,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Настройки",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                windowInsets = WindowInsets(0)
            )
        },
        contentWindowInsets = WindowInsets(0)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Верхний отступ
            item { Spacer(modifier = Modifier.height(16.dp)) }
            
            // ========== ИНТЕРФЕЙС ==========
            item(key = "header_interface") {
                SettingsSectionHeader(title = "Интерфейс")
            }
            
            item { Spacer(modifier = Modifier.height(12.dp)) }
            
            item(key = "appearance") {
                SettingsSectionCard(
                    title = "Внешний вид",
                    subtitle = "Тема приложения",
                    icon = Icons.Default.Palette,
                    onClick = { navController.navigate(SettingsDestination.Appearance.route) }
                )
            }
            
            item(key = "display") {
                SettingsSectionCard(
                    title = "Отображение",
                    subtitle = "Режим и количество колонок",
                    icon = Icons.Default.PhoneAndroid,
                    onClick = { navController.navigate(SettingsDestination.Display.route) }
                )
            }
            
            // ========== ДАННЫЕ ==========
            item(key = "header_data") {
                SettingsSectionHeader(title = "Данные")
            }
            
            item { Spacer(modifier = Modifier.height(12.dp)) }
            
            item(key = "updates") {
                SettingsSectionCard(
                    title = "Обновления",
                    subtitle = "Обновление приложения и данных",
                    icon = Icons.Default.Update,
                    onClick = { navController.navigate(SettingsDestination.Updates.route) }
                )
            }
            
            item(key = "data") {
                SettingsSectionCard(
                    title = "Управление данными",
                    subtitle = "Очистка кэша и сброс настроек",
                    icon = Icons.Default.Delete,
                    onClick = { navController.navigate(SettingsDestination.Data.route) }
                )
            }
            
            // ========== УВЕДОМЛЕНИЯ ==========
            item(key = "header_notifications") {
                SettingsSectionHeader(title = "Уведомления")
            }
            
            item { Spacer(modifier = Modifier.height(12.dp)) }
            
            item(key = "notifications") {
                SettingsSectionCard(
                    title = "Уведомления",
                    subtitle = "Режим и расписание уведомлений",
                    icon = Icons.Default.Notifications,
                    onClick = { navController.navigate(SettingsDestination.Notifications.route) }
                )
            }
            
            item { Spacer(modifier = Modifier.height(8.dp)) }
            
            item(key = "quiet") {
                SettingsSectionCard(
                    title = "Тихий режим",
                    subtitle = "Временное отключение уведомлений",
                    icon = Icons.Default.NotificationsOff,
                    onClick = { navController.navigate(SettingsDestination.QuietMode.route) }
                )
            }
            
            item { Spacer(modifier = Modifier.height(8.dp)) }
            
            item(key = "vibration") {
                SettingsSectionCard(
                    title = "Вибрация",
                    subtitle = "Вибрация при уведомлениях",
                    icon = Icons.Default.Vibration,
                    onClick = { navController.navigate(SettingsDestination.Vibration.route) }
                )
            }
            
            // ========== ПРОЧЕЕ ==========
            item(key = "header_other") {
                SettingsSectionHeader(title = "Прочее")
            }
            
            item { Spacer(modifier = Modifier.height(12.dp)) }
            
            item(key = "about") {
                SettingsSectionCard(
                    title = "О программе",
                    subtitle = "Информация и обратная связь",
                    icon = Icons.Default.Info,
                    onClick = { navController.navigate(SettingsDestination.About.route) }
                )
            }
            
            // Нижний отступ
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

/**
 * Карточка секции настроек для навигации
 */
@Composable
private fun SettingsSectionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    iconBackgroundGradient: List<Color> = listOf(
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.secondaryContainer
    )
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp,
            pressedElevation = 6.dp,
            hoveredElevation = 4.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Иконка с градиентным фоном
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = iconBackgroundGradient
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Стрелка в кружке
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Открыть",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

