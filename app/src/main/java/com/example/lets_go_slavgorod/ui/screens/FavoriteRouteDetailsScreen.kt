package com.example.lets_go_slavgorod.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.lets_go_slavgorod.data.model.BusRoute
import com.example.lets_go_slavgorod.data.model.BusSchedule
import com.example.lets_go_slavgorod.data.model.FavoriteTime
import com.example.lets_go_slavgorod.ui.components.ScheduleCard
import com.example.lets_go_slavgorod.ui.components.StickyDepartureHeader
import com.example.lets_go_slavgorod.ui.components.schedule.UnifiedScheduleHeader
import com.example.lets_go_slavgorod.ui.viewmodel.BusViewModel
import timber.log.Timber

/**
 * Экран деталей избранных времен для конкретного маршрута
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FavoriteRouteDetailsScreen(
    routeId: String,
    viewModel: BusViewModel,
    navController: NavController? = null,
    modifier: Modifier = Modifier
) {
    val favoriteTimesList by viewModel.favoriteTimes.collectAsState()
    
    // Фильтруем избранные времена для этого маршрута
    val routeFavorites = remember(favoriteTimesList, routeId) {
        favoriteTimesList.filter { it.routeId == routeId }
    }
    
    // Создаем объект маршрута из данных избранного
    val route = remember(routeFavorites, routeId) {
        val firstFavorite = routeFavorites.firstOrNull()
        if (firstFavorite != null) {
            BusRoute(
                id = routeId,
                routeNumber = firstFavorite.routeNumber,
                name = firstFavorite.routeName,
                description = "Избранные времена: ${routeFavorites.size}",
                travelTime = when (routeId) {
                    "102" -> "45 мин"
                    "102B" -> "50 мин"
                    "1" -> "20 мин"
                    else -> null
                },
                pricePrimary = when (routeId) {
                    "102", "102B" -> "25₽"
                    "1" -> "15₽"
                    else -> null
                },
                paymentMethods = "Наличные, Карта"
            )
        } else {
            null
        }
    }
    
    val routeNumber = route?.routeNumber ?: ""
    val routeName = route?.name ?: "Маршрут"
    
    // Группируем по пунктам отправления с улучшенными названиями
    val groupedByDeparturePoint = remember(routeFavorites) {
        routeFavorites.groupBy { favoriteTime ->
            // Улучшаем название пункта отправления
            val departurePoint = favoriteTime.departurePoint.trim()
            when {
                departurePoint.equals("вокзал", ignoreCase = true) -> "Вокзал (отправление)"
                departurePoint.equals("совхоз", ignoreCase = true) -> "Совхоз (отправление)"
                departurePoint.startsWith("Рынок", ignoreCase = true) -> departurePoint
                departurePoint.startsWith("МСЧ", ignoreCase = true) -> departurePoint
                else -> "$departurePoint (отправление)"
            }
        }
    }
    
    val expandedStates = remember { mutableStateMapOf<String, Boolean>() }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Единый заголовок в стиле маршрутов
        UnifiedScheduleHeader(
            route = route,
            onBackClick = { navController?.navigateUp() }
        )
        
        // Основной контент
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            groupedByDeparturePoint.forEach { (departurePoint, timesInDepartureGroup) ->
                val departureKey = "$routeId-$departurePoint"
                val isExpanded = expandedStates[departureKey] ?: true
                
                // Sticky header для пункта отправления
                stickyHeader(key = "header_$departureKey") {
                    StickyDepartureHeader(
                        title = departurePoint,
                        isExpanded = isExpanded,
                        onToggleExpand = {
                            expandedStates[departureKey] = !isExpanded
                        }
                    )
                }
                
                // Содержимое секции (только если развернуто)
                if (isExpanded) {
                    timesInDepartureGroup.forEach { favoriteTime ->
                        item(key = "time_${favoriteTime.id}") {
                            val scheduleDisplay = BusSchedule(
                                id = favoriteTime.id,
                                routeId = favoriteTime.routeId ?: "",
                                departureTime = favoriteTime.departureTime,
                                dayOfWeek = favoriteTime.dayOfWeek,
                                stopName = favoriteTime.stopName,
                                departurePoint = favoriteTime.departurePoint
                            )
                            
                            ScheduleCard(
                                schedule = scheduleDisplay,
                                isFavorite = favoriteTime.isActive,
                                onFavoriteClick = {
                                    viewModel.updateFavoriteActiveState(favoriteTime, !favoriteTime.isActive)
                                },
                                routeNumber = null,
                                routeName = null,
                                hideRouteInfo = true,
                                isNextUpcoming = false,
                                allSchedules = emptyList(),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

