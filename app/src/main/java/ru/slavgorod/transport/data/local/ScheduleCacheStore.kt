package ru.slavgorod.transport.data.local

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.charset.StandardCharsets

data class CachedScheduleSnapshot(
    val json: String,
    val savedAtMillis: Long
)

class ScheduleCacheStore(context: Context) {

    private val cacheDirectory = File(context.filesDir, CACHE_DIRECTORY_NAME)
    private val cacheFile = File(cacheDirectory, CACHE_FILE_NAME)

    suspend fun readSnapshot(): CachedScheduleSnapshot? = withContext(Dispatchers.IO) {
        runCatching {
            if (!cacheFile.isFile || cacheFile.length() <= 0L) return@runCatching null

            val json = cacheFile.readText(StandardCharsets.UTF_8).trim()
            if (json.isBlank()) return@runCatching null

            CachedScheduleSnapshot(
                json = json,
                savedAtMillis = cacheFile.lastModified().takeIf { it > 0L }
                    ?: System.currentTimeMillis()
            )
        }.getOrNull()
    }

    suspend fun writeSnapshot(json: String) = withContext(Dispatchers.IO) {
        val normalizedJson = json.trim()
        require(normalizedJson.isNotBlank()) { "Snapshot JSON cannot be blank" }

        cacheDirectory.mkdirs()
        val tempFile = tempCacheFile()
        tempFile.writeText(normalizedJson, StandardCharsets.UTF_8)

        if (cacheFile.exists() && !cacheFile.delete()) {
            throw IllegalStateException("Unable to replace existing schedule cache")
        }

        if (!tempFile.renameTo(cacheFile)) {
            tempFile.copyTo(cacheFile, overwrite = true)
            tempFile.delete()
        }

        cacheFile.setLastModified(System.currentTimeMillis())
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        runCatching { cacheFile.delete() }
        runCatching { tempCacheFile().delete() }
        if (cacheDirectory.isDirectory && cacheDirectory.listFiles().isNullOrEmpty()) {
            runCatching { cacheDirectory.delete() }
        }
    }

    suspend fun hasSnapshot(): Boolean = withContext(Dispatchers.IO) {
        cacheFile.isFile && cacheFile.length() > 0L
    }

    private companion object {
        private const val CACHE_DIRECTORY_NAME = "schedule_cache"
        private const val CACHE_FILE_NAME = "latest_routes.json"
    }

    private fun tempCacheFile(): File {
        return File(cacheDirectory, "$CACHE_FILE_NAME.tmp")
    }
}
