package com.example.lets_go_slavgorod.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Базовый ViewModel с общими состояниями и управлением ресурсами
 * 
 * Предоставляет:
 * - Общие состояния (loading, error)
 * - Управление корутинами
 * - Автоматическая очистка ресурсов
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 2.0
 */
abstract class BaseViewModel : ViewModel() {
    
    // SupervisorJob для безопасной отмены всех корутин
    protected val supervisorJob = SupervisorJob()
    protected val viewModelScope = CoroutineScope(supervisorJob + Dispatchers.Main)
    
    // Общие состояния
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    /**
     * Устанавливает состояние загрузки
     */
    protected fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }
    
    /**
     * Устанавливает ошибку
     */
    protected fun setError(error: String?) {
        _error.value = error
    }
    
    /**
     * Очищает ошибку
     */
    protected fun clearError() {
        _error.value = null
    }
    
    /**
     * Очистка ресурсов при уничтожении ViewModel
     */
    override fun onCleared() {
        super.onCleared()
        
        // Отменяем все корутины
        supervisorJob.cancel()
        viewModelScope.cancel()
        
        // Очищаем состояния
        _isLoading.value = false
        _error.value = null
    }
}
