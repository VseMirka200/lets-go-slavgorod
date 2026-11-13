package ru.slavgorod.transport.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import ru.slavgorod.transport.core.Constants

/** Хранит адрес источника расписания в настройках приложения. */
class ScheduleSourceSettings(private val dataStore: DataStore<Preferences>) {
    /** Поток сохранённого адреса; при отсутствии настройки используется адрес по умолчанию. */
    val url = dataStore.data.map { it[PreferencesKeys.scheduleSourceUrl] ?: Constants.REMOTE_JSON_URL }

    /** Возвращает первое доступное значение адреса из DataStore. */
    suspend fun currentUrl(): String = url.first()

    /**
     * Удаляет пробелы по краям и сохраняет HTTPS-адрес без учётных данных.
     *
     * @throws IllegalArgumentException если адрес не соответствует этим требованиям.
     */
    suspend fun saveUrl(value: String) {
        val normalized = value.trim()
        require(isValidScheduleSourceUrl(normalized))
        dataStore.edit { it[PreferencesKeys.scheduleSourceUrl] = normalized }
    }
}

/** Проверяет, что адрес использует HTTPS и не содержит имени пользователя или пароля. */
internal fun isValidScheduleSourceUrl(value: String): Boolean {
    val url = value.toHttpUrlOrNull() ?: return false
    return url.isHttps && url.username.isEmpty() && url.password.isEmpty()
}
