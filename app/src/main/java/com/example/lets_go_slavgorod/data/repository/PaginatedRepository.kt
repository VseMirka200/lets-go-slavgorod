package com.example.lets_go_slavgorod.data.repository

import android.content.Context
import com.example.lets_go_slavgorod.data.model.BusRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Репозиторий с поддержкой пагинации для lazy loading
 * 
 * Реализует lazy loading маршрутов с пагинацией для оптимизации
 * производительности и использования памяти.
 * 
 * Особенности:
 * - Пагинация с настраиваемым размером страницы
 * - Кэширование загруженных страниц
 * - Автоматическая загрузка следующей страницы
 * - Оптимизация памяти через очистку старых страниц
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 2.0
 */
class PaginatedRepository(private val context: Context) {
    
    // Кэш загруженных страниц
    private val pageCache = mutableMapOf<Int, List<BusRoute>>()
    
    // Текущее состояние пагинации
    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()
    
    // Все загруженные маршруты
    private val _allRoutes = MutableStateFlow<List<BusRoute>>(emptyList())
    val allRoutes: StateFlow<List<BusRoute>> = _allRoutes.asStateFlow()
    
    // Состояние загрузки
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Есть ли еще страницы для загрузки
    private val _hasMorePages = MutableStateFlow(true)
    val hasMorePages: StateFlow<Boolean> = _hasMorePages.asStateFlow()
    
    companion object {
        private const val PAGE_SIZE = 20
        private const val MAX_CACHED_PAGES = 5
    }
    
    /**
     * Загружает следующую страницу маршрутов
     * 
     * @return список маршрутов следующей страницы
     */
    suspend fun loadNextPage(): List<BusRoute> {
        if (_isLoading.value) {
            Timber.d("📄 Already loading, skipping")
            return emptyList()
        }
        
        if (!_hasMorePages.value) {
            Timber.d("📄 No more pages to load")
            return emptyList()
        }
        
        _isLoading.value = true
        
        try {
            val nextPage = _currentPage.value + 1
            Timber.d("📄 Loading page $nextPage")
            
            // Проверяем кэш
            val cachedPage = pageCache[nextPage]
            if (cachedPage != null) {
                Timber.d("📄 Using cached page $nextPage")
                _currentPage.value = nextPage
                _isLoading.value = false
                return cachedPage
            }
            
            // Загружаем новую страницу
            val routes = loadPageFromSource(nextPage)
            
            if (routes.isEmpty()) {
                _hasMorePages.value = false
                Timber.d("📄 No more routes available")
            } else {
                // Кэшируем страницу
                pageCache[nextPage] = routes
                
                // Обновляем общий список
                _allRoutes.value = _allRoutes.value + routes
                _currentPage.value = nextPage
                
                // Очищаем старые страницы если кэш переполнен
                cleanupOldPages()
                
                Timber.d("📄 Loaded ${routes.size} routes for page $nextPage")
            }
            
            return routes
            
        } catch (e: Exception) {
            Timber.e(e, "Error loading page ${_currentPage.value + 1}")
            return emptyList()
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * Загружает конкретную страницу из источника данных
     */
    private suspend fun loadPageFromSource(page: Int): List<BusRoute> {
        // Здесь должна быть логика загрузки из реального источника
        // Для демонстрации возвращаем пустой список
        return emptyList()
    }
    
    /**
     * Очищает старые страницы из кэша
     */
    private fun cleanupOldPages() {
        if (pageCache.size > MAX_CACHED_PAGES) {
            val pagesToRemove = pageCache.keys.sorted().take(pageCache.size - MAX_CACHED_PAGES)
            pagesToRemove.forEach { page ->
                pageCache.remove(page)
                Timber.d("🧹 Removed page $page from cache")
            }
        }
    }
    
    /**
     * Сбрасывает пагинацию и очищает кэш
     */
    fun resetPagination() {
        pageCache.clear()
        _currentPage.value = 0
        _allRoutes.value = emptyList()
        _hasMorePages.value = true
        _isLoading.value = false
        Timber.d("🔄 Pagination reset")
    }
    
    /**
     * Получает общее количество загруженных маршрутов
     */
    fun getLoadedRoutesCount(): Int {
        return _allRoutes.value.size
    }
    
    /**
     * Получает количество загруженных страниц
     */
    fun getLoadedPagesCount(): Int {
        return _currentPage.value
    }
}