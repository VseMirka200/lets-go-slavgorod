package com.example.lets_go_slavgorod.di

import com.example.lets_go_slavgorod.domain.notification.AlarmScheduler
import com.example.lets_go_slavgorod.data.notification.NotificationHelper
import com.example.lets_go_slavgorod.data.local.AppDatabase
import com.example.lets_go_slavgorod.data.local.JsonDataSource
import com.example.lets_go_slavgorod.data.remote.RemoteDataSource
import com.example.lets_go_slavgorod.data.repository.BusRouteRepository
import com.example.lets_go_slavgorod.data.source.AssetsDataSourceStrategy
import com.example.lets_go_slavgorod.data.source.CacheDataSourceStrategy
import com.example.lets_go_slavgorod.data.source.DataSourceChain
import com.example.lets_go_slavgorod.data.source.RemoteDataSourceStrategy
import com.example.lets_go_slavgorod.domain.update.UpdateManager
import com.example.lets_go_slavgorod.domain.usecase.SearchRoutesUseCase
import com.example.lets_go_slavgorod.domain.usecase.GetRouteByIdUseCase
import com.example.lets_go_slavgorod.ui.viewmodel.DataManagementViewModel
import com.example.lets_go_slavgorod.ui.viewmodel.DisplaySettingsViewModel
import com.example.lets_go_slavgorod.ui.viewmodel.FavoritesViewModel
import com.example.lets_go_slavgorod.ui.viewmodel.NotificationSettingsViewModel
import com.example.lets_go_slavgorod.ui.viewmodel.QuietModeViewModel
import com.example.lets_go_slavgorod.ui.viewmodel.RoutesViewModel
import com.example.lets_go_slavgorod.ui.viewmodel.ScheduleViewModel
import com.example.lets_go_slavgorod.ui.viewmodel.ThemeViewModel
import com.example.lets_go_slavgorod.ui.viewmodel.UpdateSettingsViewModel
import com.example.lets_go_slavgorod.ui.viewmodel.VibrationSettingsViewModel
import com.example.lets_go_slavgorod.ui.viewmodel.themeDataStore
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import java.io.File

/**
 * Модуль Dependency Injection для приложения
 * 
 * Определяет все зависимости приложения с использованием Koin.
 * Заменяет ручную DI на автоматическую с улучшенной тестируемостью.
 * 
 * Модули:
 * - Database: Room база данных и DAO
 * - Repository: Репозитории для работы с данными
 * - ViewModel: ViewModels для UI
 * - Service: Сервисы и утилиты
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 2.0
 */
val appModule = module {
    
    // =====================================================================================
    //                              DATABASE LAYER
    // =====================================================================================
    
    // База данных
    single<AppDatabase> {
        AppDatabase.getDatabase(androidContext())
    }
    
    // DAO
    single { get<AppDatabase>().favoriteTimeDao() }
    
    // =====================================================================================
    //                              DATA SOURCES
    // =====================================================================================
    
    // Remote Data Source
    single<RemoteDataSource> {
        RemoteDataSource(androidContext())
    }
    
    // JSON Data Source
    single<JsonDataSource> {
        JsonDataSource(androidContext())
    }
    
    // Data Source Chain
    single<DataSourceChain> {
        DataSourceChain(
            strategies = listOf(
                RemoteDataSourceStrategy { get<RemoteDataSource>().getJsonString() },
                CacheDataSourceStrategy(File(androidContext().cacheDir, "routes_data.json")),
                AssetsDataSourceStrategy(androidContext(), "routes_data.json")
            )
        )
    }
    
    // =====================================================================================
    //                              REPOSITORY LAYER
    // =====================================================================================
    
    // Bus Route Repository
    single<BusRouteRepository> {
        BusRouteRepository(androidContext())
    }
    
    // =====================================================================================
    //                              DOMAIN LAYER
    // =====================================================================================
    
    // Use Cases
    single<SearchRoutesUseCase> {
        SearchRoutesUseCase(get<BusRouteRepository>())
    }
    
    single<GetRouteByIdUseCase> {
        GetRouteByIdUseCase(get<BusRouteRepository>())
    }
    
    // Update Manager
    single<UpdateManager> {
        UpdateManager(androidContext())
    }
    
    // Alarm Scheduler - включен для работы уведомлений
    single<AlarmScheduler> {
        AlarmScheduler
    }
    
    // Notification Helper - включен для работы уведомлений
    single<NotificationHelper> {
        NotificationHelper
    }
    
    // =====================================================================================
    //                              VIEWMODEL LAYER
    // =====================================================================================
    
    // Routes ViewModel
    viewModel<RoutesViewModel> {
        RoutesViewModel(androidContext() as android.app.Application)
    }
    
    // Schedule ViewModel
    viewModel<ScheduleViewModel> {
        ScheduleViewModel(androidContext() as android.app.Application)
    }
    
    // Favorites ViewModel
    viewModel<FavoritesViewModel> {
        FavoritesViewModel(androidContext() as android.app.Application)
    }
    
    // Theme ViewModel
    viewModel<ThemeViewModel> {
        ThemeViewModel(androidContext().themeDataStore)
    }
    
    // Display Settings ViewModel
    viewModel<DisplaySettingsViewModel> {
        DisplaySettingsViewModel(androidContext() as android.app.Application)
    }
    
    // Notification Settings ViewModel
    viewModel<NotificationSettingsViewModel> {
        NotificationSettingsViewModel(androidContext() as android.app.Application)
    }
    
    // Quiet Mode ViewModel
    viewModel<QuietModeViewModel> {
        QuietModeViewModel(androidContext() as android.app.Application)
    }
    
    // Update Settings ViewModel
    viewModel<UpdateSettingsViewModel> {
        UpdateSettingsViewModel(androidContext() as android.app.Application)
    }
    
    // Vibration Settings ViewModel
    viewModel<VibrationSettingsViewModel> {
        VibrationSettingsViewModel(androidContext() as android.app.Application)
    }
    
    // Data Management ViewModel
    viewModel<DataManagementViewModel> {
        DataManagementViewModel(androidContext() as android.app.Application)
    }
}