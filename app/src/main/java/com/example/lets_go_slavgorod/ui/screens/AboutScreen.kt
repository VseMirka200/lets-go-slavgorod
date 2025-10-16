package com.example.lets_go_slavgorod.ui.screens

// import com.example.lets_go_slavgorod.BuildConfig
import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavController
import com.example.lets_go_slavgorod.R
import com.example.lets_go_slavgorod.utils.Constants
import timber.log.Timber

/**
 * Экран "О программе" - отображает информацию о приложении и разработчике
 * 
 * Версия: 2.0
 * Последнее обновление: Октябрь 2025
 * 
 * Содержит следующие разделы:
 * 1. Информация о приложении:
 *    - Название приложения "Поехали! Славгород"
 *    - Текущая версия (v1.8)
 *    - Описание функциональности
 * 
 * 2. Информация о разработчике:
 *    - Имя разработчика (из strings.xml)
 *    - Ссылки на социальные сети (VK)
 *    - Контактная информация
 * 
 * 3. Ссылки и ресурсы:
 *    - GitHub репозиторий проекта
 *    - Telegram для обратной связи
 *    - Email разработчика
 * 
 * 4. Поддержка разработчика:
 *    - Кнопки для благодарности
 *    - Возможность оставить отзыв
 *    - Ссылка на донаты/поддержку
 * 
 * Шапка:
 * - Заголовок "О приложении"
 * - Стрелка назад (унифицированный стиль)
 * 
 * Изменения v2.0:
 * - Унифицирован стиль стрелки назад
 * - Улучшена типографика (Roboto)
 * - Оптимизирована верстка секций
 * 
 * @param navController контроллер навигации (может быть null)
 * @param onBackClick callback для обработки нажатия кнопки "Назад"
 * @param modifier модификатор для настройки внешнего вида
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    navController: NavController? = null,
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {

    val developerName = stringResource(id = R.string.developer_name)
    val developerVkUrl = stringResource(id = R.string.developer_vk_url)
    val context = LocalContext.current

    // Строки для обратной связи через Telegram

    val appVersion = "v1.8"

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.about_screen_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
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
        },
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(16.dp))
            
            // Раздел Информация о приложении
            Text(
                text = "Информация о приложении",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            AppInfoCard(
                appName = stringResource(id = R.string.app_name),
                developer = developerName,
                developerVkUrl = developerVkUrl,
                version = appVersion,
                onDeveloperClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, developerVkUrl.toUri())
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Timber.e(e, "Could not open VK profile")
                    }
                }
            )

            Spacer(Modifier.height(24.dp))

            // Раздел Обратная связь
            Text(
                text = "Обратная связь",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            FeedbackCard()

            Spacer(Modifier.height(24.dp))

            // Раздел Поддержка разработчика
            Text(
                text = "Поддержка разработчика",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            SupportCard(navController = navController)
        }
    }
}

/**
 * Карточка с информацией о приложении
 */
@Composable
private fun AppInfoCard(
    appName: String,
    developer: String,
    developerVkUrl: String,
    version: String,
    onDeveloperClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = appName,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Разработал:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = developer,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        textDecoration = TextDecoration.Underline
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onDeveloperClick() }
                )
            }
            
            Text(
                text = "Версия: $version",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Карточка поддержки разработчика
 */
@Composable
private fun SupportCard(
    navController: NavController?
) {
    val context = LocalContext.current
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = "Если приложение вам нравится, вы можете поддержать его разработку:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(Modifier.height(12.dp))
            
            // Кнопки поддержки
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Кнопка "Донат"
                OutlinedButton(
                    onClick = {
                        // Открываем ссылку в браузере
                        val intent = Intent(Intent.ACTION_VIEW, "https://donatty.com/vv-olyushin".toUri())
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Timber.e(e, "Could not open donation link")
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Поддержать")
                }
                
                // Кнопка "Оценить"
                OutlinedButton(
                    onClick = {
                        // Открываем ссылку в браузере
                        val intent = Intent(Intent.ACTION_VIEW, "https://github.com/VseMirka200/lets_go_slavgorod".toUri())
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Timber.e(e, "Could not open GitHub")
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Оценить")
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "💡 Способы поддержки:",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "• Оставить отзыв в Telegram\n• Поставить звезду на GitHub\n• Поделиться с друзьями\n• Сообщить об ошибках",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

/**
 * Карточка обратной связи
 */
@Composable
private fun FeedbackCard() {
    val context = LocalContext.current
    val telegramUrl = stringResource(id = R.string.feedback_telegram_url)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = "Есть вопросы или предложения? Заполните форму обратной связи!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            // Кнопка обратной связи
            Button(
                onClick = {
                    try {
                        // Открываем форму Yandex в браузере
                        val intent = Intent(Intent.ACTION_VIEW, telegramUrl.toUri())
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Timber.e(e, "Could not open feedback form")
                        // Показываем сообщение пользователю
                        android.widget.Toast.makeText(
                            context,
                            context.getString(R.string.error_open_feedback_form),
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Feedback,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Обратная связь")
            }

            Spacer(Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "💬 Что можно сообщить:",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "• Ошибки в приложении\n" +
                               "• Предложения по улучшению\n" +
                               "• Вопросы по расписанию",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

