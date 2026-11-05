package ru.slavgorod.transport.data.local

import android.content.Context
import android.util.AtomicFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.charset.StandardCharsets

data class CachedScheduleSnapshot(
    val json: String,
    val savedAtMillis: Long
)

/**
 * Хранит копию JSON расписания во внутреннем хранилище приложения.
 *
 * При записи AtomicFile заменяет прежнюю копию после успешного завершения записи.
 * При сбое предыдущая копия остаётся доступной для восстановления.
 */
class ScheduleCacheStore(context: Context) {

    private val cacheDirectory = File(context.filesDir, CACHE_DIRECTORY_NAME)
    private val cacheFile = File(cacheDirectory, CACHE_FILE_NAME)
    private val atomicCacheFile = AtomicFile(cacheFile)
    private val mutex = Mutex()

    suspend fun readSnapshot(): CachedScheduleSnapshot? = withContext(Dispatchers.IO) {
        mutex.withLock {
            runCatching {
                val json = atomicCacheFile.openRead()
                    .bufferedReader(StandardCharsets.UTF_8)
                    .use { it.readText() }
                    .trim()
                if (json.isBlank()) return@runCatching null

                CachedScheduleSnapshot(
                    json = json,
                    savedAtMillis = cacheFile.lastModified().takeIf { it > 0L }
                        ?: System.currentTimeMillis()
                )
            }.getOrNull()
        }
    }

    suspend fun writeSnapshot(json: String) = withContext(Dispatchers.IO) {
        val normalizedJson = json.trim()
        require(normalizedJson.isNotBlank()) { "JSON снимка не может быть пустым" }

        mutex.withLock {
            if (!cacheDirectory.isDirectory && !cacheDirectory.mkdirs() && !cacheDirectory.isDirectory) {
                throw IllegalStateException("Не удалось создать каталог кэша расписания")
            }
            val output = atomicCacheFile.startWrite()
            try {
                output.write(normalizedJson.toByteArray(StandardCharsets.UTF_8))
                atomicCacheFile.finishWrite(output)
            } catch (error: Throwable) {
                atomicCacheFile.failWrite(output)
                throw error
            }
            tempCacheFile().delete()
        }
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        mutex.withLock {
            atomicCacheFile.delete()
            tempCacheFile().delete()
            if (cacheDirectory.isDirectory && cacheDirectory.listFiles().isNullOrEmpty()) {
                cacheDirectory.delete()
            }
        }
    }

    suspend fun hasSnapshot(): Boolean = readSnapshot() != null

    private companion object {
        private const val CACHE_DIRECTORY_NAME = "schedule_cache"
        private const val CACHE_FILE_NAME = "latest_routes.json"
    }

    private fun tempCacheFile(): File {
        return File(cacheDirectory, "$CACHE_FILE_NAME.tmp")
    }
}
