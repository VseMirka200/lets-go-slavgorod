package com.example.lets_go_slavgorod.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lets_go_slavgorod.data.model.BusSchedule

/**
 * Компактная карточка расписания для отображения в двухколоночной сетке
 * 
 * Отображает только основную информацию: время отправления и кнопку избранного.
 * Оптимизирована для компактного отображения в сетке.
 * 
 * @param schedule расписание для отображения
 * @param isFavorite флаг, добавлено ли время в избранное
 * @param onFavoriteClick callback-функция при клике на кнопку избранного
 * @param isNextUpcoming флаг, является ли это расписание ближайшим рейсом
 * @param allSchedules все расписания для расчета времени до отправления
 * @param modifier модификатор для настройки внешнего вида
 */
@Composable
fun CompactScheduleCard(
    schedule: BusSchedule,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    isNextUpcoming: Boolean = false,
    allSchedules: List<BusSchedule> = emptyList(),
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp)),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isNextUpcoming) 3.dp else 1.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isNextUpcoming) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Время отправления
            Text(
                text = schedule.departureTime,
                style = androidx.compose.ui.text.TextStyle(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Default, // Roboto
                    fontSize = 16.sp,
                    fontWeight = if (isNextUpcoming) FontWeight.Bold else FontWeight.Medium
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
                    style = androidx.compose.ui.text.TextStyle(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Default, // Roboto
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                )
            }
            
            // Кнопка избранного
            IconButton(
                onClick = onFavoriteClick,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (isFavorite) "Убрать из избранного" else "Добавить в избранное",
                    tint = if (isFavorite) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

