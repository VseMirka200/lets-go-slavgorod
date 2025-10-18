package com.example.lets_go_slavgorod.ui.components.schedule

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lets_go_slavgorod.data.model.BusRoute

/**
 * Объединенный заголовок экрана расписания маршрута
 * 
 * Версия: 2.0
 * Последнее обновление: Октябрь 2025
 * 
 * Расширенный заголовок с навигацией и детальной информацией о маршруте.
 * Отображается как первый элемент в LazyColumn экрана расписания.
 * Унифицирован со стилем остальных экранов приложения.
 * 
 * Структура:
 * 1. Шапка (высота 64dp, унифицированный стиль TopAppBar):
 *    - Стрелка назад слева (onPrimaryContainer tint)
 *    - Название маршрута (titleLarge, fontWeight Bold, maxLines 1)
 *    - Кнопка уведомлений справа (если onNotificationClick не null)
 * 
 * 2. Детальная информация о маршруте (под шапкой):
 *    - Время в пути (если указано)
 *    - Стоимость проезда (если указана)
 *    - Способы оплаты (наличные/карта, если указаны)
 *    - Примечание о времени отправления
 * 
 * Визуальный дизайн:
 * - Фон: primaryContainer (единый цвет для всего заголовка)
 * - Закругленные углы снизу (bottomStart/End 16dp)
 * - Разделители между секциями информации
 * 
 * Изменения v2.0:
 * - Унифицированы все стрелки назад (tint, стиль)
 * - Добавлена кнопка уведомлений в заголовок
 * - Улучшена типографика (Roboto, единый размер)
 * 
 * @param route маршрут для отображения информации (может быть null при загрузке)
 * @param onBackClick callback для возврата на главный экран
 * @param isVisible не используется (deprecated, оставлен для совместимости API)
 * @param onNotificationClick callback для открытия настроек уведомлений (если null - кнопка не отображается)
 * @param modifier модификатор для настройки внешнего вида
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedScheduleHeader(
    route: BusRoute?,
    onBackClick: () -> Unit,
    isVisible: Boolean = true,
    onNotificationClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Единая карточка для всего заголовка
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(
            topStart = 0.dp,
            topEnd = 0.dp,
            bottomStart = 16.dp,
            bottomEnd = 16.dp
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Заголовок в стиле TopAppBar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    
                    Text(
                        text = route?.name ?: "Расписание",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                
                // Кнопка уведомлений
                if (onNotificationClick != null) {
                    IconButton(onClick = onNotificationClick) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Настройки уведомлений",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            
            // Информация о маршруте (если есть)
            if (route != null) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                )
                
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Основная информация о маршруте
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Время в пути
                        route.travelTime?.let { travelTime ->
                            InfoItem("Время в пути", travelTime)
                        }
                        
                        // Стоимость
                        route.pricePrimary?.let { price ->
                            InfoItem("Стоимость", price)
                        }
                    }
                    
                    // Способы оплаты
                    route.paymentMethods?.let { paymentMethods ->
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                        )
                        InfoItem("Способы оплаты", paymentMethods)
                    }
                    
                    // Примечание
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                    )
                    Text(
                        text = "Примечание: Указано время отправления от начальных/конечных остановок маршрута.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        textAlign = TextAlign.Start
                    )
                }
            }
        }
    }
}

/**
 * Элемент информации о маршруте
 * 
 * Отображает пару "метка: значение" в заголовке расписания.
 * Используется для времени в пути и стоимости.
 * 
 * @param label название параметра (например, "Время в пути")
 * @param value значение параметра (например, "~40 минут")
 * @param modifier модификатор для настройки внешнего вида
 */
@Composable
private fun InfoItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
