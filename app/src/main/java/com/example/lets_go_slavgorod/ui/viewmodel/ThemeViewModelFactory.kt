package com.example.lets_go_slavgorod.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Фабрика для создания ThemeViewModel с правильными зависимостями
 */
class ThemeViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ThemeViewModel::class.java)) {
            return try {
                ThemeViewModel(context.applicationContext.themeDataStore) as T
            } catch (e: Exception) {
                // Fallback на создание с базовым DataStore
                ThemeViewModel(context.applicationContext.themeDataStore) as T
            }
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}