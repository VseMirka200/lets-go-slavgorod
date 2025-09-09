package com.example.lets_go_slavgorod.data.remote

import android.content.Context
import android.content.SharedPreferences
import timber.log.Timber

/**
 * Метрики загрузки данных для мониторинга производительности
 * 
 * v3.0 Changes (Октябрь 2025):
 * - Оптимизированы импорты и зависимости
 * - Улучшена производительность метрик
 * - Обновлены комментарии и документация
 */
class DownloadMetrics(private val context: Context) {
    
    companion object {
        private const val PREFS_NAME = "download_metrics"
        private const val KEY_SUCCESS_COUNT = "success_count"
        private const val KEY_FAILURE_COUNT = "failure_count"
        private const val KEY_TOTAL_BYTES = "total_bytes"
        private const val KEY_AVERAGE_TIME = "average_time"
        private const val KEY_LAST_SUCCESS = "last_success"
        private const val KEY_LAST_FAILURE = "last_failure"
        private const val KEY_GITHUB_SUCCESS = "github_success"
        private const val KEY_CACHE_SUCCESS = "cache_success"
        private const val KEY_ASSETS_SUCCESS = "assets_success"
        private const val KEY_ETAG_HITS = "etag_hits"
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * Записывает успешную загрузку
     */
    fun recordSuccess(source: DataSource, bytes: Long, timeMs: Long, etagHit: Boolean = false) {
        val totalSuccess = prefs.getLong(KEY_SUCCESS_COUNT, 0) + 1
        val totalBytes = prefs.getLong(KEY_TOTAL_BYTES, 0) + bytes
        val currentAvgTime = prefs.getLong(KEY_AVERAGE_TIME, 0)
        val newAvgTime = ((currentAvgTime * (totalSuccess - 1)) + timeMs) / totalSuccess
        
        prefs.edit()
            .putLong(KEY_SUCCESS_COUNT, totalSuccess)
            .putLong(KEY_TOTAL_BYTES, totalBytes)
            .putLong(KEY_AVERAGE_TIME, newAvgTime)
            .putLong(KEY_LAST_SUCCESS, System.currentTimeMillis())
            .putLong(getSourceKey(source), prefs.getLong(getSourceKey(source), 0) + 1)
            .apply()
        
        if (etagHit) {
            prefs.edit()
                .putLong(KEY_ETAG_HITS, prefs.getLong(KEY_ETAG_HITS, 0) + 1)
                .apply()
        }
        
    }
    
    /**
     * Записывает неудачную загрузку
     */
    fun recordFailure(source: DataSource, error: String) {
        val totalFailures = prefs.getLong(KEY_FAILURE_COUNT, 0) + 1
        prefs.edit()
            .putLong(KEY_FAILURE_COUNT, totalFailures)
            .putLong(KEY_LAST_FAILURE, System.currentTimeMillis())
            .apply()
        
    }
    
    /**
     * Получает статистику загрузок
     */
    fun getStats(): DownloadStats {
        val totalSuccess = prefs.getLong(KEY_SUCCESS_COUNT, 0)
        val totalFailures = prefs.getLong(KEY_FAILURE_COUNT, 0)
        val totalBytes = prefs.getLong(KEY_TOTAL_BYTES, 0)
        val avgTime = prefs.getLong(KEY_AVERAGE_TIME, 0)
        val lastSuccess = prefs.getLong(KEY_LAST_SUCCESS, 0)
        val lastFailure = prefs.getLong(KEY_LAST_FAILURE, 0)
        val etagHits = prefs.getLong(KEY_ETAG_HITS, 0)
        
        return DownloadStats(
            totalSuccess = totalSuccess,
            totalFailures = totalFailures,
            totalBytes = totalBytes,
            averageTimeMs = avgTime,
            lastSuccessTime = lastSuccess,
            lastFailureTime = lastFailure,
            githubSuccess = prefs.getLong(KEY_GITHUB_SUCCESS, 0),
            cacheSuccess = prefs.getLong(KEY_CACHE_SUCCESS, 0),
            assetsSuccess = prefs.getLong(KEY_ASSETS_SUCCESS, 0),
            etagHits = etagHits
        )
    }
    
    private fun getSourceKey(source: DataSource): String {
        return when (source) {
            DataSource.GITHUB -> KEY_GITHUB_SUCCESS
            DataSource.CACHE -> KEY_CACHE_SUCCESS
            DataSource.ASSETS -> KEY_ASSETS_SUCCESS
        }
    }
    
    enum class DataSource {
        GITHUB, CACHE, ASSETS
    }
    
    data class DownloadStats(
        val totalSuccess: Long,
        val totalFailures: Long,
        val totalBytes: Long,
        val averageTimeMs: Long,
        val lastSuccessTime: Long,
        val lastFailureTime: Long,
        val githubSuccess: Long,
        val cacheSuccess: Long,
        val assetsSuccess: Long,
        val etagHits: Long
    ) {
        val successRate: Double
            get() = if (totalSuccess + totalFailures > 0) {
                totalSuccess.toDouble() / (totalSuccess + totalFailures)
            } else 0.0
        
        val averageBytesPerDownload: Long
            get() = if (totalSuccess > 0) totalBytes / totalSuccess else 0L
            
        val trafficSavedByETag: Long
            get() = etagHits * averageBytesPerDownload
    }
}