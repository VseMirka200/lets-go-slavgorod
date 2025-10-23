package com.example.lets_go_slavgorod.ui.screens.settings

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.example.lets_go_slavgorod.R
import com.example.lets_go_slavgorod.core.Constants
import com.example.lets_go_slavgorod.ui.components.FeedbackTypeDialog
import com.example.lets_go_slavgorod.ui.components.SettingsTopBar
import timber.log.Timber

/**
 * Экран "О приложении"
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 2.1
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val developerName = stringResource(id = R.string.developer_name)
    val developerVkUrl = stringResource(id = R.string.developer_vk_url)
    val appVersion = Constants.APP_VERSION
    var showFeedbackDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            SettingsTopBar(
                title = "О приложении",
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
            // Информация о приложении
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(Constants.SETTINGS_HORIZONTAL_PADDING.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    
                    Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Разработали:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Urban SOLUTION",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                textDecoration = TextDecoration.Underline
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, "https://vk.com/urban_solution".toUri())
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Timber.e(e, "Не удалось открыть группу VK")
                                }
                            }
                        )
                    }
                    
                    Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Автор идеи:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = developerName,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                textDecoration = TextDecoration.Underline
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, developerVkUrl.toUri())
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Timber.e(e, "Не удалось открыть профиль VK")
                                }
                            }
                        )
                    }
                    
                    Text(
                        text = "Версия: $appVersion",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Обратная связь
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(Constants.SETTINGS_HORIZONTAL_PADDING.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Есть вопросы или предложения? Напишите нам на почту!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = { showFeedbackDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Feedback,
                            contentDescription = "Обратная связь",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Написать на почту")
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "💬 Что можно сообщить:",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(Modifier.height(6.dp))
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
            
            // Поддержка разработчика
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(Constants.SETTINGS_HORIZONTAL_PADDING.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Если приложение вам нравится, вы можете поддержать его разработку:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Кнопки поддержки
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Кнопка "Поддержать"
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, "https://pay.cloudtips.ru/p/1fa22ea5".toUri())
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Timber.e(e, "Не удалось открыть ссылку на донат")
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Поддержать",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Поддержать")
                        }
                        
                        // Кнопка "Оценить"
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, "https://github.com/VseMirka200/lets_go_slavgorod".toUri())
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Timber.e(e, "Не удалось открыть GitHub")
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Оценить",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Оценить")
                        }
                    }
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "💡 Способы поддержки:",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "• Поставить звезду на GitHub\n" +
                                       "• Поделиться приложением с друзьями\n" +
                                       "• Оставить отзыв и предложения",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Диалог выбора типа обратной связи
    if (showFeedbackDialog) {
        FeedbackTypeDialog(
            onDismiss = { showFeedbackDialog = false },
            onTypeSelected = { feedbackType ->
                showFeedbackDialog = false
                
                try {
                    val feedbackEmail = context.getString(R.string.feedback_email)
                    val deviceInfo = "Android ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})"
                    val deviceLabel = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}".take(20)
                    
                    val subject = "${feedbackType.emailPrefix} [Поехали! Славгород] $deviceLabel"
                    
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = "mailto:".toUri()
                        putExtra(Intent.EXTRA_EMAIL, arrayOf(feedbackEmail))
                        putExtra(Intent.EXTRA_SUBJECT, subject)
                        putExtra(Intent.EXTRA_TEXT, "\n\n---\n$deviceInfo\nВерсия: ${Constants.APP_VERSION}")
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Timber.e(e, "Не удалось открыть приложение электронной почты")
                    android.widget.Toast.makeText(
                        context,
                        context.getString(R.string.error_open_email),
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
    }
}