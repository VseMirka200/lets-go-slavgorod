package com.example.lets_go_slavgorod.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.example.lets_go_slavgorod.ui.viewmodel.AppTheme
import com.example.lets_go_slavgorod.ui.viewmodel.ThemeViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Экран настроек внешнего вида
 * 
 * Модульный экран для управления темой приложения.
 * Отдельный экран обеспечивает:
 * - Изолированную перекомпозицию (только этот экран)
 * - Простое тестирование
 * - Быструю загрузку
 * - Низкую память usage
 * 
 * @param onNavigateBack callback для возврата
 * @param viewModel ViewModel для управления темой
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 2.1
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: ThemeViewModel = koinViewModel(parameters = { parametersOf(context) })
    val currentTheme by viewModel.currentTheme.collectAsState()
    
    // Используем derivedStateOf для оптимизации - перекомпозиция только при изменении лейбла
    val themeLabel by remember {
        derivedStateOf {
            when (currentTheme) {
                AppTheme.LIGHT -> "Светлая"
                AppTheme.DARK -> "Тёмная"
                AppTheme.SYSTEM -> "Системная"
            }
        }
    }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Внешний вид",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                windowInsets = WindowInsets(0)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            // Информация о текущей теме
            SettingsInfoCard(
                text = "Текущая тема: $themeLabel",
                icon = Icons.Default.Palette
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Заголовок секции
            SettingsSectionHeader(title = "Выберите тему")
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Варианты тем
            SettingsOptionCard(
                title = "Системная",
                subtitle = "Автоматически следовать системной теме устройства",
                icon = Icons.Default.Palette,
                isSelected = currentTheme == AppTheme.SYSTEM,
                onClick = { viewModel.setTheme(AppTheme.SYSTEM) }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            SettingsOptionCard(
                title = "Светлая",
                subtitle = "Всегда использовать светлую тему",
                icon = Icons.Default.LightMode,
                isSelected = currentTheme == AppTheme.LIGHT,
                onClick = { viewModel.setTheme(AppTheme.LIGHT) }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            SettingsOptionCard(
                title = "Тёмная",
                subtitle = "Всегда использовать тёмную тему",
                icon = Icons.Default.DarkMode,
                isSelected = currentTheme == AppTheme.DARK,
                onClick = { viewModel.setTheme(AppTheme.DARK) }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}


