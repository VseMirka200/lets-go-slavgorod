package com.example.lets_go_slavgorod.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Generic фабрика для создания AndroidViewModel
 * 
 * Упрощает создание ViewModels, устраняя дублирование кода.
 * Работает с любыми AndroidViewModel, принимающими Application в конструкторе.
 * 
 * Использование:
 * ```kotlin
 * val viewModel: BusViewModel = viewModel(
 *     factory = AndroidViewModelFactory.create { BusViewModel(it) }
 * )
 * ```
 * 
 * @param VM тип ViewModel
 * @author VseMirka200
 * @version 1.0
 * @since 2.0
 */
class AndroidViewModelFactory<VM : AndroidViewModel> private constructor(
    private val application: Application,
    private val creator: (Application) -> VM
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return creator(application) as T
    }
    
    companion object {
        /**
         * Создает фабрику для AndroidViewModel
         * 
         * @param application контекст приложения
         * @param creator функция создания ViewModel
         * @return фабрика для ViewModelProvider
         */
        fun <VM : AndroidViewModel> create(
            application: Application,
            creator: (Application) -> VM
        ): AndroidViewModelFactory<VM> {
            return AndroidViewModelFactory(application, creator)
        }
    }
}

/**
 * Generic фабрика для создания обычного ViewModel с контекстом
 * 
 * Для ViewModels, которые не наследуют AndroidViewModel,
 * но требуют Context в конструкторе.
 * 
 * @param VM тип ViewModel
 */
class ContextViewModelFactory<VM : ViewModel> private constructor(
    private val context: android.content.Context,
    private val creator: (android.content.Context) -> VM
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return creator(context) as T
    }
    
    companion object {
        /**
         * Создает фабрику для ViewModel с контекстом
         * 
         * @param context контекст
         * @param creator функция создания ViewModel
         * @return фабрика для ViewModelProvider
         */
        fun <VM : ViewModel> create(
            context: android.content.Context,
            creator: (android.content.Context) -> VM
        ): ContextViewModelFactory<VM> {
            return ContextViewModelFactory(context, creator)
        }
    }
}

