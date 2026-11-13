package ru.slavgorod.transport.data.repository

import ru.slavgorod.transport.data.local.CachedScheduleSnapshot

/**
 * Граница постоянного хранения последнего принятого снимка расписания.
 *
 * Репозиторий проверяет содержимое до записи. Реализация хранилища отвечает за
 * сохранность прежней копии при ошибке записи и может выполнять файловый ввод-вывод.
 */
interface ScheduleSnapshotStore {
    /** Возвращает сохранённый JSON и время его записи либо `null`, если копии нет. */
    suspend fun readSnapshot(): CachedScheduleSnapshot?

    /** Сохраняет непустой JSON; ошибка записи передаётся вызывающему коду. */
    suspend fun writeSnapshot(json: String)

    suspend fun clear()

    /** Сообщает, удалось ли прочитать сохранённую копию. */
    suspend fun hasSnapshot(): Boolean

    /** Форматирует момент сохранения в миллисекундах для пользовательского журнала. */
    fun formatSavedAt(timestampMillis: Long): String
}
