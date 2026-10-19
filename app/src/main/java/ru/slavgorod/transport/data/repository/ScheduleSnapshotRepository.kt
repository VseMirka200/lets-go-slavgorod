package ru.slavgorod.transport.data.repository

import ru.slavgorod.transport.data.local.CachedScheduleSnapshot
import ru.slavgorod.transport.data.local.ScheduleCacheStore
import ru.slavgorod.transport.domain.util.DateTimeFormatterUtils

/**
 * Адаптер снимка расписания для репозитория маршрутов.
 *
 * Подсказка разработчику: формат времени сохранения принадлежит этому слою, чтобы
 * экран не зависел от устройства и способа хранения кэша.
 */
class ScheduleSnapshotRepository(
    private val cacheStore: ScheduleCacheStore
) : ScheduleSnapshotStore {

    override suspend fun readSnapshot(): CachedScheduleSnapshot? {
        return cacheStore.readSnapshot()
    }

    override suspend fun writeSnapshot(json: String) {
        cacheStore.writeSnapshot(json)
    }

    override suspend fun hasSnapshot(): Boolean {
        return cacheStore.hasSnapshot()
    }

    override fun formatSavedAt(timestampMillis: Long): String {
        return DateTimeFormatterUtils.formatFullDateTime(timestampMillis)
    }
}
