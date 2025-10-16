package com.example.lets_go_slavgorod.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Эффект shimmer для skeleton loading
 * 
 * Создает анимированный эффект мерцания для плейсхолдеров загрузки,
 * улучшая восприятие времени ожидания пользователем.
 * 
 * @author VseMirka200
 * @version 1.0
 */

/**
 * Модификатор для добавления shimmer эффекта
 */
fun Modifier.shimmerEffect(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val shimmerColors = listOf(
        Color.LightGray.copy(alpha = 0.6f),
        Color.LightGray.copy(alpha = 0.2f),
        Color.LightGray.copy(alpha = 0.6f),
    )

    background(
        brush = Brush.linearGradient(
            colors = shimmerColors,
            start = Offset.Zero,
            end = Offset(x = translateAnim.value, y = translateAnim.value)
        )
    )
}

/**
 * Skeleton карточка маршрута для Grid режима
 */
@Composable
fun SkeletonRouteCardGrid(
    modifier: Modifier = Modifier,
    gridColumns: Int = 2
) {
    val cardHeight = when (gridColumns) {
        1 -> 220.dp
        2 -> 180.dp
        3 -> 160.dp
        else -> 140.dp
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(cardHeight),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shimmerEffect(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                // Placeholder для "АВТОБУС"
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(16.dp)
                        .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Placeholder для номера маршрута
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                )
            }
        }
    }
}

/**
 * Skeleton карточка маршрута для List режима
 */
@Composable
fun SkeletonRouteCardList(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shimmerEffect()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Placeholder для номера
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                // Placeholder для названия
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(20.dp)
                        .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Placeholder для описания
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(16.dp)
                        .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                )
            }
        }
    }
}

/**
 * Экран загрузки со skeleton карточками
 */
@Composable
fun SkeletonLoadingScreen(
    displayMode: com.example.lets_go_slavgorod.ui.viewmodel.RouteDisplayMode,
    gridColumns: Int = 2,
    itemCount: Int = 6
) {
    when (displayMode) {
        com.example.lets_go_slavgorod.ui.viewmodel.RouteDisplayMode.GRID -> {
            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(gridColumns),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(itemCount) {
                    SkeletonRouteCardGrid(gridColumns = gridColumns)
                }
            }
        }
        com.example.lets_go_slavgorod.ui.viewmodel.RouteDisplayMode.LIST -> {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(itemCount) {
                    SkeletonRouteCardList()
                }
            }
        }
    }
}

