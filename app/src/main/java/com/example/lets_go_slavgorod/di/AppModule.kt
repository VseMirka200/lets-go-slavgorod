package com.example.lets_go_slavgorod.di

import com.example.lets_go_slavgorod.data.local.AppDatabase
import com.example.lets_go_slavgorod.data.local.dao.FavoriteTimeDao
import com.example.lets_go_slavgorod.data.repository.BusRouteRepository
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
import com.example.lets_go_slavgorod.updates.UpdateManager
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Модуль Koin для Dependency Injection
 * 
 * Определяет все зависимости приложения в одном месте.
 * Koin автоматически создает и инжектит эти зависимости.
 * 
 * Преимущества:
 * - Централизованное управление зависимостями
 * - Легко мокировать для тестов
 * - Нет boilerplate кода
 * - Runtime DI (без кодогенерации)
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 2.1
 */

/**
 * Модуль данных (Database, Repository, DAO)
 */
val dataModule = module {
    // Database - singleton
    single {
        AppDatabase.getDatabase(androidContext())
    }
    
    // DAO
    single<FavoriteTimeDao> {
        get<AppDatabase>().favoriteTimeDao()
    }
    
    // Repository
    single {
        BusRouteRepository(androidContext())
    }
    
    // Update Manager
    single {
        UpdateManager(androidContext())
    }
}

/**
 * Модуль ViewModels
 */
val viewModelModule = module {
    // Новые модульные ViewModels
    viewModel { RoutesViewModel(androidApplication()) }
    viewModel { FavoritesViewModel(androidApplication()) }
    viewModel { ScheduleViewModel(androidApplication()) }
    
    // ViewModels с DataStore параметрами
    viewModel { (context: android.content.Context) -> 
        ThemeViewModel(context.themeDataStore)
    }
    
    // ViewModels с Context параметром
    viewModel { (context: android.content.Context) ->
        DisplaySettingsViewModel(context)
    }
    viewModel { (context: android.content.Context) ->
        UpdateSettingsViewModel(context)
    }
    viewModel { (context: android.content.Context) ->
        QuietModeViewModel(context)
    }
    viewModel { (context: android.content.Context) ->
        VibrationSettingsViewModel(context)
    }
    viewModel { (context: android.content.Context) ->
        DataManagementViewModel(context)
    }
    
    // ViewModels с Application параметром
    viewModel {
        NotificationSettingsViewModel(androidApplication())
    }
}

/**
 * Список всех модулей приложения
 */
val appModules = listOf(
    dataModule,
    viewModelModule
)

