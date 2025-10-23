package com.example.lets_go_slavgorod.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.lets_go_slavgorod.data.local.AppDatabase
import com.example.lets_go_slavgorod.data.local.entity.FavoriteTimeEntity
import com.example.lets_go_slavgorod.data.model.FavoriteTime
import com.example.lets_go_slavgorod.data.repository.BusRouteRepository
import com.example.lets_go_slavgorod.domain.notification.AlarmScheduler
import com.example.lets_go_slavgorod.core.toFavoriteTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * BroadcastReceiver для обработки системных событий загрузки и обновления
 * 
 * Автоматически восстанавливает все запланированные уведомления о времени
 * отправления автобусов после перезагрузки устройства или обновления приложения.
 * 
 * Обрабатываемые события:
 * - ACTION_BOOT_COMPLETED: завершение загрузки системы
 * - ACTION_MY_PACKAGE_REPLACED: обновление приложения
 * - ACTION_PACKAGE_REPLACED: замена пакета приложения
 * 
 * Процесс восстановления:
 * 1. Получение всех активных избранных времен из базы данных
 * 2. Преобразование Entity в модель FavoriteTime
 * 3. Планирование уведомления через AlarmScheduler
 * 4. Логирование результатов для отладки
 * 
 * Все операции выполняются асинхронно в фоновом потоке (Dispatchers.IO)
 * для избежания блокировки главного потока.
 * 
 * @author VseMirka200
 * @version 2.0
 * @since 1.0
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // goAsync() предотвращает завершение BroadcastReceiver пока не завершится асинхронная работа
        val pendingResult = goAsync()
        
        
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                
                // Восстанавливаем уведомления в фоновом потоке
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        restoreAllNotifications(context)
                    } catch (e: Exception) {
                        Timber.e(e, "Ошибка восстановления уведомлений")
                    } finally {
                        // ВАЖНО: Сигнализируем что BroadcastReceiver завершил работу
                        pendingResult.finish()
                    }
                }
            }
            else -> {
                pendingResult.finish()
            }
        }
    }
    
    /**
     * Восстанавливает все активные уведомления из базы данных
     * 
     * @param context контекст приложения
     */
    private suspend fun restoreAllNotifications(context: Context) {
        try {
            
            // Получаем базу данных и репозиторий
            val database = AppDatabase.getDatabase(context.applicationContext)
            val favoriteTimeDao = database.favoriteTimeDao()
            val repository = BusRouteRepository(context.applicationContext)
            
            // Удаляем избранные времена для удалённых маршрутов
            val removedRouteIds = listOf("2", "4", "5")
            removedRouteIds.forEach { routeId: String ->
                val deletedCount = favoriteTimeDao.deleteByRouteId(routeId)
                if (deletedCount > 0) {
                }
            }
            
            // Получаем все активные избранные времена
            val favoriteTimeEntities: List<FavoriteTimeEntity> = favoriteTimeDao.getAllFavoriteTimes().firstOrNull() ?: emptyList()
            
            val activeFavoriteTimes: List<FavoriteTime> = favoriteTimeEntities
                .filter { entity: FavoriteTimeEntity -> entity.isActive }
                .map { entity: FavoriteTimeEntity -> entity.toFavoriteTime(repository) }
            
            
            // Восстанавливаем уведомления для каждого активного избранного времени
            activeFavoriteTimes.forEach { favoriteTime: FavoriteTime ->
                try {
                    AlarmScheduler.checkAndUpdateNotifications(context.applicationContext, favoriteTime)
                } catch (e: Exception) {
                    Timber.e(e, "Ошибка восстановления уведомления для ${favoriteTime.id}")
                }
            }
            
            
        } catch (e: Exception) {
            Timber.e(e, "Критическая ошибка при восстановлении уведомлений")
        }
    }
}