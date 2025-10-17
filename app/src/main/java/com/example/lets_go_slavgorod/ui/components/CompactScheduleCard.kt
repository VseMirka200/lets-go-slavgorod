package com.example.lets_go_slavgorod.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lets_go_slavgorod.data.model.BusSchedule
import com.example.lets_go_slavgorod.ui.theme.lets_go_slavgorodTheme
import com.example.lets_go_slavgorod.utils.Constants

/**
 * Компактная карточка рейса для двухколоночной сетки расписания
 * 
 * Версия: 3.1
 * Последнее обновление: Октябрь 2025
 * 
 * Минималистичная карточка для отображения времени отправления в сетке.
 * Используется в FilterableScheduleGrid и TwoColumnScheduleGrid для компактного
 * представления расписаний в двухколоночном формате.
 * 
 * Структура (слева направо):
 * - Порядковый номер рейса слева (1., 2., 3... - опционально, отступ 8dp)
 * - Время отправления крупным шрифтом (headlineMedium - 28sp) по центру
 * - Метка "Следующий" под временем (минимальный отступ для компактности)
 * - Звёздочка избранного справа по центру вертикали
 * 
 * Визуальные состояния:
 * - Обычный рейс: серый фон (surfaceVariant), средний размер, тень 1dp
 * - Ближайший рейс: цветной фон (primaryContainer), жирный шрифт, метка "Следующий", тень 3dp
 * 
 * Избранное:
 * - Пустая звёздочка (☆): не в избранном (Icons.Outlined.StarBorder)
 * - Заполненная звёздочка (★): в избранном (Icons.Filled.Star)
 * 
 * Изменения v3.1:
 * - Убраны лишние отступы между временем и меткой "Следующий" (0dp)
 * - Порядковый номер имеет отступ 8dp от левого края
 * - Все размеры используют константы из Constants.kt
 * 
 * Изменения v3.0:
 * - Добавлены порядковые номера рейсов для удобной навигации
 * - Иконка избранного изменена с сердца (Heart) на звезду (Star)
 * - Увеличен размер времени отправления (bodyLarge -> headlineMedium)
 * - Звездочка перемещена в центр по вертикали справа
 * - Используются константы из Constants для всех размеров
 * - Оптимизирован код с использованием remember для избежания перекомпозиций
 * 
 * @param schedule расписание для отображения
 * @param isFavorite добавлено ли время в избранное
 * @param onFavoriteClick callback при клике на звёздочку
 * @param isNextUpcoming является ли это ближайшим рейсом
 * @param orderNumber порядковый номер рейса в списке (для удобной навигации)
 * @param modifier модификатор для настройки внешнего вида
 * 
 * @author VseMirka200
 * @version 3.1
 * @since 1.0
 */
@Composable
fun CompactScheduleCard(
    schedule: BusSchedule,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    isNextUpcoming: Boolean = false,
    orderNumber: Int? = null,
    modifier: Modifier = Modifier
) {
    // Оптимизация: кэшируем значения для избежания перекомпозиций
    val elevation = remember(isNextUpcoming) {
        if (isNextUpcoming) Constants.SCHEDULE_CARD_ELEVATION_UPCOMING.dp 
        else Constants.SCHEDULE_CARD_ELEVATION_DEFAULT.dp
    }
    
    val containerColor = remember(isNextUpcoming) { isNextUpcoming }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Constants.SCHEDULE_CARD_CORNER_RADIUS.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        colors = CardDefaults.cardColors(
            containerColor = if (containerColor)
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Constants.SCHEDULE_CARD_PADDING.dp)
        ) {
            // Порядковый номер слева по центру вертикали
            if (orderNumber != null) {
                Text(
                    text = "$orderNumber.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 8.dp)
                )
            }
            
            // Основное содержимое по центру
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Время отправления
                Text(
                    text = schedule.departureTime,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = if (isNextUpcoming) FontWeight.Bold else FontWeight.SemiBold
                    ),
                    color = if (isNextUpcoming) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurface
                )
                
                // Простое отображение для ближайшего рейса
                if (isNextUpcoming) {
                    Text(
                        text = "Следующий",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                }
            }
            
            // Кнопка избранного справа по центру
            IconButton(
                onClick = onFavoriteClick,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(Constants.FAVORITE_BUTTON_SIZE.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (isFavorite) "Убрать из избранного" else "Добавить в избранное",
                    tint = if (isFavorite) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(Constants.FAVORITE_ICON_SIZE.dp)
                )
            }
        }
    }
}

// =============================================================================
//                              PREVIEWS
// =============================================================================

@Preview(name = "Light Mode - Regular", showBackground = true)
@Preview(name = "Dark Mode - Regular", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewCompactScheduleCard() {
    lets_go_slavgorodTheme {
        CompactScheduleCard(
            schedule = BusSchedule(
                id = "102_1",
                routeId = "102",
                stopName = "Рынок (Славгород)",
                departureTime = "08:30",
                dayOfWeek = 1,
                isWeekend = false,
                departurePoint = "Рынок (Славгород)"
            ),
            isFavorite = false,
            onFavoriteClick = {},
            isNextUpcoming = false,
            orderNumber = 1
        )
    }
}

@Preview(name = "Light Mode - Next & Favorite", showBackground = true)
@Preview(name = "Dark Mode - Next & Favorite", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewCompactScheduleCardNextFavorite() {
    lets_go_slavgorodTheme {
        CompactScheduleCard(
            schedule = BusSchedule(
                id = "102_5",
                routeId = "102",
                stopName = "Рынок (Славгород)",
                departureTime = "14:45",
                dayOfWeek = 1,
                isWeekend = false,
                departurePoint = "Рынок (Славгород)"
            ),
            isFavorite = true,
            onFavoriteClick = {},
            isNextUpcoming = true,
            orderNumber = 5
        )
    }
}
