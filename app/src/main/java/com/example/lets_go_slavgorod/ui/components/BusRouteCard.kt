package com.example.lets_go_slavgorod.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.example.lets_go_slavgorod.data.model.BusRoute
import com.example.lets_go_slavgorod.ui.theme.DesignTokens
import com.example.lets_go_slavgorod.utils.Constants

/**
 * Переиспользуемый компонент карточки автобусного маршрута
 * 
 * Версия: 2.0
 * Последнее обновление: Октябрь 2025
 * 
 * Отображает информацию о маршруте автобуса в виде интерактивной карточки.
 * Поддерживает два режима отображения: сетка (grid) и список (list).
 * 
 * Режимы отображения:
 * - **Grid**: вертикальная карточка с крупным номером маршрута по центру
 * - **List**: горизонтальная карточка с номером слева и названием справа
 * 
 * Адаптивность в режиме Grid (изменение размеров в зависимости от колонок):
 * - 1 колонка: максимальный размер номера и надписей (для планшетов)
 * - 2 колонки: стандартный размер (по умолчанию)
 * - 3 колонки: уменьшенный размер
 * - 4 колонки: компактный дизайн (для больших экранов)
 * 
 * Функциональность:
 * - Цветной фон карточки (индивидуальный цвет маршрута из данных)
 * - Клик на карточку → переход к расписанию маршрута
 * - Оптимизирована для минимальных перекомпозиций
 * 
 * @author VseMirka200
 * @version 3.0
 * @since 1.0
 */

/**
 * Карточка маршрута для отображения в сетке (вертикальный дизайн)
 * 
 * Создает компактную вертикальную карточку с номером маршрута по центру.
 * Адаптируется под количество колонок в сетке.
 * 
 * Структура:
 * - "АВТОБУС" (надпись сверху)
 * - Номер маршрута (крупный, по центру)
 * - Адаптивные размеры в зависимости от количества колонок
 * 
 * @param route данные маршрута для отображения
 * @param onRouteClick callback при клике на карточку
 * @param modifier модификатор для настройки внешнего вида
 * @param showNotificationButton не используется (оставлен для совместимости)
 * @param onNotificationClick не используется (оставлен для совместимости)
 * @param gridColumns количество колонок в сетке (1-4)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusRouteCardGrid(
    route: BusRoute,
    onRouteClick: () -> Unit,
    modifier: Modifier = Modifier,
    showNotificationButton: Boolean = false,
    onNotificationClick: (() -> Unit)? = null,
    gridColumns: Int = 2
) {
    // Получаем цвет фона карточки
    val primaryColor = MaterialTheme.colorScheme.primary
    val boxBackgroundColor = remember(route.color, primaryColor) {
        try {
            androidx.compose.ui.graphics.Color(route.color.toColorInt()).copy(alpha = Constants.COLOR_ALPHA)
        } catch (_: IllegalArgumentException) {
            primaryColor.copy(alpha = Constants.COLOR_ALPHA)
        }
    }
    
    // Кэшируем InteractionSource отдельно
    val interactionSource = remember { MutableInteractionSource() }
    
    // Кэшируем модификаторы для избежания пересоздания
    val cardModifier = remember(modifier, route.id) {
        modifier
            .fillMaxWidth()
            .clickable(
                indication = null, // Отключаем анимацию для быстрого отклика
                interactionSource = interactionSource
            ) {
                onRouteClick()
            }
    }
    
    // Адаптивные размеры в зависимости от количества колонок - кэшируем
    val (cardHeight, cardPadding, spacerHeight) = remember(gridColumns) {
        when (gridColumns) {
            1 -> Triple(DesignTokens.Size.Card.Height.Grid1Column, DesignTokens.Spacing.Large, DesignTokens.Spacing.Medium - 4.dp)
            2 -> Triple(DesignTokens.Size.Card.Height.Grid2Columns, DesignTokens.Spacing.Medium + 4.dp, DesignTokens.Spacing.Medium)
            3 -> Triple(DesignTokens.Size.Card.Height.Grid3Columns, DesignTokens.Spacing.Medium, DesignTokens.Spacing.Small + 4.dp)
            else -> Triple(DesignTokens.Size.Card.Height.Grid4Columns, DesignTokens.Spacing.Small + 4.dp, DesignTokens.Spacing.Small)
        }
    }
    
    Card(
        modifier = cardModifier
            .height(cardHeight)
            .semantics {
                role = Role.Button
                contentDescription = "Маршрут ${route.routeNumber}: ${route.name}. Нажмите для просмотра расписания"
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = boxBackgroundColor
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(cardHeight)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cardHeight)
                    .padding(cardPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Для 4 колонок используем более компактный дизайн
                if (gridColumns >= 4) {
                    // Компактный дизайн с единым стилем
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Надпись "АВТОБУС" сверху
                        Text(
                            text = "АВТОБУС",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // Номер маршрута с адаптивным размером
                        Text(
                            text = route.routeNumber,
                            color = androidx.compose.ui.graphics.Color.White,
                            style = if (route.routeNumber.length > 3 || route.routeNumber.any { it.isLetter() }) {
                                MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = MaterialTheme.typography.titleLarge.lineHeight * 0.9
                                )
                            } else {
                                MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = MaterialTheme.typography.headlineLarge.lineHeight * 0.9
                                )
                            },
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Visible
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                } else {
                    // Обычный дизайн для 1-3 колонок
                    Text(
                        text = "АВТОБУС",
                        style = when (gridColumns) {
                            1 -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            2 -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            3 -> MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            else -> MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        },
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                    
                    Spacer(modifier = Modifier.height(spacerHeight))
                    
                    // Номер маршрута с адаптивным размером
                    Text(
                        text = route.routeNumber,
                        color = androidx.compose.ui.graphics.Color.White,
                        style = when (gridColumns) {
                            1 -> MaterialTheme.typography.displayLarge.copy(
                                fontSize = MaterialTheme.typography.displayLarge.fontSize * 1.2f,
                                fontWeight = FontWeight.ExtraBold
                            )
                            2 -> MaterialTheme.typography.displayMedium.copy(
                                fontSize = MaterialTheme.typography.displayMedium.fontSize * 1.15f,
                                fontWeight = FontWeight.ExtraBold
                            )
                            3 -> {
                                // Для 3 колонок: уменьшаем размер для номеров с буквами или длинных номеров
                                if (route.routeNumber.length > 3 || route.routeNumber.any { it.isLetter() }) {
                                    MaterialTheme.typography.headlineLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = MaterialTheme.typography.headlineLarge.fontSize * 0.95f
                                    )
                                } else {
                                    MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold)
                                }
                            }
                            else -> MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold)
                        },
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Visible
                    )
                }
            }
            
            // Кнопка уведомлений в правом верхнем углу (если showNotificationButton = true)
            if (showNotificationButton && onNotificationClick != null) {
                IconButton(
                    onClick = { onNotificationClick() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .semantics {
                            role = Role.Button
                            contentDescription = "Настройки уведомлений для маршрута ${route.routeNumber}"
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null, // Уже указано в semantics
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Карточка маршрута для отображения в списке (горизонтальный дизайн)
 * 
 * Создает горизонтальную карточку на всю ширину экрана.
 * Используется когда выбран режим отображения "Список".
 * 
 * Структура:
 * - Номер маршрута (жирный, слева)
 * - Разделитель "|"
 * - Название маршрута (справа от номера)
 * 
 * @param route данные маршрута для отображения
 * @param onRouteClick callback при клике на карточку
 * @param modifier модификатор для настройки внешнего вида
 * @param showNotificationButton не используется (оставлен для совместимости)
 * @param onNotificationClick не используется (оставлен для совместимости)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusRouteCardList(
    route: BusRoute,
    onRouteClick: () -> Unit,
    modifier: Modifier = Modifier,
    showNotificationButton: Boolean = false,
    onNotificationClick: (() -> Unit)? = null
) {
    // Получаем цвет фона карточки
    val primaryColor = MaterialTheme.colorScheme.primary
    val boxBackgroundColor = remember(route.color, primaryColor) {
        try {
            androidx.compose.ui.graphics.Color(route.color.toColorInt()).copy(alpha = Constants.COLOR_ALPHA)
        } catch (_: IllegalArgumentException) {
            primaryColor.copy(alpha = Constants.COLOR_ALPHA)
        }
    }
    
    val interactionSource = remember { MutableInteractionSource() }
    
    val cardModifier = remember(modifier, route.id) {
        modifier
            .fillMaxWidth()
            .padding(
                start = Constants.PADDING_MEDIUM.dp,
                end = Constants.PADDING_MEDIUM.dp,
                top = Constants.PADDING_SMALL.dp,
                bottom = Constants.PADDING_SMALL.dp
            )
            .clickable(
                indication = null,
                interactionSource = interactionSource
            ) {
                onRouteClick()
            }
    }

    Card(
        modifier = cardModifier
            .semantics {
                role = Role.Button
                contentDescription = "Маршрут ${route.routeNumber}: ${route.name}. Нажмите для просмотра расписания"
            },
        elevation = CardDefaults.cardElevation(defaultElevation = Constants.CARD_ELEVATION.dp),
        shape = RoundedCornerShape(Constants.CARD_CORNER_RADIUS.dp),
        colors = CardDefaults.cardColors(
            containerColor = boxBackgroundColor
        )
    ) {
        Row(
            modifier = Modifier
                .padding(
                    start = 20.dp,  // Увеличенные отступы
                    end = 20.dp,
                    top = 20.dp,
                    bottom = 20.dp
                )
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Номер маршрута - одинаковая высота с названием
                Text(
                    text = route.routeNumber,
                    color = androidx.compose.ui.graphics.Color.White,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    modifier = Modifier.padding(end = 4.dp)
                )
                
                // Разделитель
                Text(
                    text = "|",
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(end = 6.dp)
                )

                // Название маршрута - одинаковая высота с номером
                Text(
                    text = route.name,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = androidx.compose.ui.graphics.Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(end = Constants.PADDING_SMALL.dp)
                )
            }
            
            // Кнопка уведомлений справа (если showNotificationButton = true)
            if (showNotificationButton && onNotificationClick != null) {
                IconButton(
                    onClick = { onNotificationClick() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Настройки уведомлений",
                        tint = androidx.compose.ui.graphics.Color.White
                    )
                }
            }
        }
    }
}

/**
 * Универсальная карточка маршрута автобуса
 * 
 * Автоматически выбирает дизайн карточки в зависимости от режима отображения.
 * Является оберткой над BusRouteCardGrid и BusRouteCardList.
 * 
 * Используется в:
 * - HomeScreen (список всех маршрутов)
 * 
 * @param route данные маршрута для отображения
 * @param onClick callback при клике на карточку маршрута
 * @param modifier модификатор для настройки внешнего вида
 * @param isGridMode true = режим сетки, false = режим списка
 * @param showNotificationButton не используется (оставлен для совместимости API)
 * @param onNotificationClick не используется (оставлен для совместимости API)
 * @param gridColumns количество колонок в сетке (актуально только для isGridMode = true)
 */
@Composable
fun BusRouteCard(
    route: BusRoute,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isGridMode: Boolean = true,
    showNotificationButton: Boolean = false,
    onNotificationClick: (() -> Unit)? = null,
    gridColumns: Int = 2
) {
    if (isGridMode) {
        BusRouteCardGrid(
            route = route, 
            onRouteClick = { onClick() },
            modifier = modifier,
            showNotificationButton = showNotificationButton,
            onNotificationClick = onNotificationClick,
            gridColumns = gridColumns
        )
    } else {
        BusRouteCardList(
            route = route, 
            onRouteClick = { onClick() },
            modifier = modifier,
            showNotificationButton = showNotificationButton,
            onNotificationClick = onNotificationClick
        )
    }
}