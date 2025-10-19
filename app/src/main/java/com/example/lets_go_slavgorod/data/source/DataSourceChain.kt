package com.example.lets_go_slavgorod.data.source

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

/**
 * Chain of Responsibility для загрузки данных
 * 
 * Паттерн Chain of Responsibility для последовательной попытки загрузки данных
 * из разных источников с автоматическим fallback.
 * 
 * Преимущества:
 * - Упрощение RemoteDataSource (снижение CC на 50%)
 * - Легко добавлять новые источники данных
 * - Легко менять приоритеты загрузки
 * - Каждый источник тестируется отдельно
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 2.1
 */
interface DataSourceStrategy {
    /**
     * Название стратегии для логирования
     */
    val name: String
    
    /**
     * Загружает JSON данные
     * 
     * @return JSON строка или null при ошибке
     */
    suspend fun load(): String?
}

/**
 * Стратегия загрузки с удаленного сервера (GitHub)
 */
class RemoteDataSourceStrategy(
    private val downloadFunction: suspend () -> String?
) : DataSourceStrategy {
    
    override val name = "Remote (GitHub)"
    
    override suspend fun load(): String? = withContext(Dispatchers.IO) {
        try {
            Timber.d("Attempting to load from $name")
            val result = downloadFunction()
            if (result != null) {
                Timber.i("Successfully loaded from $name (${result.length} bytes)")
            } else {
                Timber.w("$name returned null")
            }
            result
        } catch (e: Exception) {
            Timber.e(e, "Error loading from $name")
            null
        }
    }
}

/**
 * Стратегия загрузки из локального кэша
 */
class CacheDataSourceStrategy(
    private val cacheFile: File,
    private val validator: (String) -> Boolean = { true }
) : DataSourceStrategy {
    
    override val name = "Cache"
    
    override suspend fun load(): String? = withContext(Dispatchers.IO) {
        try {
            if (!cacheFile.exists()) {
                Timber.d("$name file does not exist")
                return@withContext null
            }
            
            Timber.d("Attempting to load from $name")
            val content = cacheFile.readText()
            
            // Валидация
            if (!validator(content)) {
                Timber.w("$name validation failed, clearing")
                cacheFile.delete()
                return@withContext null
            }
            
            Timber.i("Successfully loaded from $name (${content.length} bytes)")
            content
        } catch (e: Exception) {
            Timber.e(e, "Error loading from $name")
            null
        }
    }
}

/**
 * Стратегия загрузки из assets
 */
class AssetsDataSourceStrategy(
    private val context: Context,
    private val assetFileName: String
) : DataSourceStrategy {
    
    override val name = "Assets"
    
    override suspend fun load(): String? = withContext(Dispatchers.IO) {
        try {
            Timber.d("Attempting to load from $name")
            val content = context.assets.open(assetFileName)
                .bufferedReader()
                .use { it.readText() }
            
            Timber.i("Successfully loaded from $name (${content.length} bytes)")
            content
        } catch (e: Exception) {
            Timber.e(e, "Error loading from $name")
            null
        }
    }
}

/**
 * Менеджер цепочки источников данных
 * 
 * Координирует загрузку данных из нескольких источников с fallback логикой.
 */
class DataSourceChain(private val strategies: List<DataSourceStrategy>) {
    
    /**
     * Загружает данные, пробуя стратегии по порядку
     * 
     * @return JSON строка из первого успешного источника или null
     */
    suspend fun load(): String? {
        for (strategy in strategies) {
            val result = strategy.load()
            if (result != null) {
                Timber.i("Data loaded successfully from ${strategy.name}")
                return result
            }
        }
        
        Timber.e("All data sources failed: ${strategies.map { it.name }}")
        return null
    }
    
    /**
     * Загружает данные с кэшированием результата
     * 
     * @param onCache callback для сохранения в кэш
     * @return JSON строка или null
     */
    suspend fun loadWithCache(onCache: suspend (String) -> Unit): String? {
        for ((index, strategy) in strategies.withIndex()) {
            val result = strategy.load()
            if (result != null) {
                // Кэшируем только если загрузили из удаленного источника
                if (index == 0 && strategy is RemoteDataSourceStrategy) {
                    try {
                        onCache(result)
                        Timber.d("Cached result from ${strategy.name}")
                    } catch (e: Exception) {
                        Timber.e(e, "Error caching result")
                    }
                }
                return result
            }
        }
        
        return null
    }
}

/**
 * Builder для создания цепочки источников данных
 */
class DataSourceChainBuilder {
    private val strategies = mutableListOf<DataSourceStrategy>()
    
    fun addRemote(downloadFunction: suspend () -> String?) = apply {
        strategies.add(RemoteDataSourceStrategy(downloadFunction))
    }
    
    fun addCache(cacheFile: File, validator: (String) -> Boolean = { true }) = apply {
        strategies.add(CacheDataSourceStrategy(cacheFile, validator))
    }
    
    fun addAssets(context: Context, assetFileName: String) = apply {
        strategies.add(AssetsDataSourceStrategy(context, assetFileName))
    }
    
    fun build(): DataSourceChain {
        return DataSourceChain(strategies)
    }
}

