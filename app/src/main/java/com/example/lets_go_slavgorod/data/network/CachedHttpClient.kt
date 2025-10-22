package com.example.lets_go_slavgorod.data.network

import android.content.Context
import com.example.lets_go_slavgorod.BuildConfig
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * HTTP клиент с кэшированием для оптимизации сетевых запросов
 * 
 * Предоставляет настроенный OkHttpClient с кэшированием, логированием
 * и оптимизациями для мобильных устройств.
 * 
 * Особенности:
 * - HTTP кэш для уменьшения сетевых запросов
 * - Логирование запросов в debug режиме
 * - Оптимизированные таймауты
 * - Автоматическое управление кэшем
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 2.0
 */
object CachedHttpClient {
    
    private var client: OkHttpClient? = null
    
    /**
     * Получает настроенный HTTP клиент
     * 
     * @param context контекст приложения
     * @return настроенный OkHttpClient
     */
    fun getClient(context: Context): OkHttpClient {
        return client ?: createClient(context).also { client = it }
    }
    
    /**
     * Создает настроенный HTTP клиент
     */
    private fun createClient(context: Context): OkHttpClient {
        val cacheSize = 10 * 1024 * 1024L // 10 MB
        val cacheDirectory = File(context.cacheDir, "http_cache")
        val cache = Cache(cacheDirectory, cacheSize)
        
        val builder = OkHttpClient.Builder()
            .cache(cache)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
        
        // Добавляем логирование в debug режиме
        if (BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor { message ->
                Timber.d("🌐 HTTP: $message")
            }.apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(loggingInterceptor)
        }
        
        // Добавляем certificate pinning в production
        if (!BuildConfig.DEBUG) {
            try {
                val certificatePinner = com.example.lets_go_slavgorod.security.CertificatePinning.createGitHubPinner()
                builder.certificatePinner(certificatePinner)
                Timber.d("🔒 Certificate pinning enabled for production")
            } catch (e: Exception) {
                Timber.w(e, "Failed to enable certificate pinning")
            }
        }
        
        // Добавляем User-Agent для идентификации
        builder.addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("User-Agent", "LetsGoSlavgorod/${BuildConfig.VERSION_NAME}")
                .build()
            chain.proceed(request)
        }
        
        return builder.build()
    }
    
    /**
     * Очищает HTTP кэш
     * 
     * @param context контекст приложения
     */
    fun clearCache(context: Context) {
        try {
            val cacheDirectory = File(context.cacheDir, "http_cache")
            if (cacheDirectory.exists()) {
                cacheDirectory.deleteRecursively()
                Timber.d("🧹 HTTP cache cleared")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error clearing HTTP cache")
        }
    }
    
    /**
     * Получает размер HTTP кэша
     * 
     * @param context контекст приложения
     * @return размер кэша в байтах
     */
    fun getCacheSize(context: Context): Long {
        return try {
            val cacheDirectory = File(context.cacheDir, "http_cache")
            if (cacheDirectory.exists()) {
                cacheDirectory.walkTopDown().sumOf { it.length() }
            } else {
                0L
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting cache size")
            0L
        }
    }
}