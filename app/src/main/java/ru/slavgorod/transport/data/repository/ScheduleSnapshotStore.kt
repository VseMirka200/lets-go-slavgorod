package ru.slavgorod.transport.data.repository

import ru.slavgorod.transport.data.local.CachedScheduleSnapshot

interface ScheduleSnapshotStore {
    suspend fun readSnapshot(): CachedScheduleSnapshot?
    suspend fun writeSnapshot(json: String)
    suspend fun hasSnapshot(): Boolean
    fun formatSavedAt(timestampMillis: Long): String
}
