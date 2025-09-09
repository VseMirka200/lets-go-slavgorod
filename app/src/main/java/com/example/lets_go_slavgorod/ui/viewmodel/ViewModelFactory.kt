package com.example.lets_go_slavgorod.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Фабрика для создания ViewModels с Application параметром
 * 
 * v3.0 Changes (Октябрь 2025):
 * - Оптимизированы импорты и зависимости
 * - Улучшена производительность создания ViewModels
 * - Обновлены комментарии и документация
 */
class ViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(RoutesViewModel::class.java) -> {
                RoutesViewModel(application) as T
            }
            modelClass.isAssignableFrom(ScheduleViewModel::class.java) -> {
                ScheduleViewModel(application) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}