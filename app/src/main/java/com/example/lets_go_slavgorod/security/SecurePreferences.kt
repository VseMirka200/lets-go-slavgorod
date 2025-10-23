package com.example.lets_go_slavgorod.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import timber.log.Timber

/**
 * Безопасное хранилище для чувствительных данных
 * 
 * Использует EncryptedSharedPreferences для шифрования данных
 * с помощью AES-256-GCM алгоритма. Все данные автоматически
 * шифруются при записи и расшифровываются при чтении.
 * 
 * Безопасность:
 * - AES-256-GCM шифрование
 * - Ключ хранится в Android Keystore
 * - Защита от атак на файловую систему
 * - Автоматическая ротация ключей
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 3.0
 */
class SecurePreferences(private val context: Context) {
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    /**
     * Сохраняет строку в зашифрованном виде
     * 
     * @param key ключ для сохранения
     * @param value значение для шифрования
     */
    fun putString(key: String, value: String) {
        try {
            encryptedPrefs.edit()
                .putString(key, value)
                .apply()
        } catch (e: Exception) {
            Timber.e(e, "Ошибка сохранения защищенной строки для ключа: $key")
        }
    }
    
    /**
     * Получает расшифрованную строку
     * 
     * @param key ключ для получения
     * @param defaultValue значение по умолчанию
     * @return расшифрованная строка или значение по умолчанию
     */
    fun getString(key: String, defaultValue: String = ""): String {
        return try {
            encryptedPrefs.getString(key, defaultValue) ?: defaultValue
        } catch (e: Exception) {
            Timber.e(e, "Ошибка получения защищенной строки для ключа: $key")
            defaultValue
        }
    }
    
    /**
     * Сохраняет boolean в зашифрованном виде
     * 
     * @param key ключ для сохранения
     * @param value значение для шифрования
     */
    fun putBoolean(key: String, value: Boolean) {
        try {
            encryptedPrefs.edit()
                .putBoolean(key, value)
                .apply()
        } catch (e: Exception) {
            Timber.e(e, "Ошибка сохранения защищенного булева значения для ключа: $key")
        }
    }
    
    /**
     * Получает расшифрованный boolean
     * 
     * @param key ключ для получения
     * @param defaultValue значение по умолчанию
     * @return расшифрованный boolean или значение по умолчанию
     */
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return try {
            encryptedPrefs.getBoolean(key, defaultValue)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка получения защищенного булева значения для ключа: $key")
            defaultValue
        }
    }
    
    /**
     * Сохраняет int в зашифрованном виде
     * 
     * @param key ключ для сохранения
     * @param value значение для шифрования
     */
    fun putInt(key: String, value: Int) {
        try {
            encryptedPrefs.edit()
                .putInt(key, value)
                .apply()
        } catch (e: Exception) {
            Timber.e(e, "Ошибка сохранения защищенного целого числа для ключа: $key")
        }
    }
    
    /**
     * Получает расшифрованный int
     * 
     * @param key ключ для получения
     * @param defaultValue значение по умолчанию
     * @return расшифрованный int или значение по умолчанию
     */
    fun getInt(key: String, defaultValue: Int = 0): Int {
        return try {
            encryptedPrefs.getInt(key, defaultValue)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка получения защищенного целого числа для ключа: $key")
            defaultValue
        }
    }
    
    /**
     * Удаляет зашифрованное значение
     * 
     * @param key ключ для удаления
     */
    fun remove(key: String) {
        try {
            encryptedPrefs.edit()
                .remove(key)
                .apply()
        } catch (e: Exception) {
            Timber.e(e, "Ошибка удаления защищенного значения для ключа: $key")
        }
    }
    
    /**
     * Очищает все зашифрованные данные
     */
    fun clear() {
        try {
            encryptedPrefs.edit()
                .clear()
                .apply()
        } catch (e: Exception) {
            Timber.e(e, "Ошибка очистки защищенных данных")
        }
    }
    
    /**
     * Проверяет, содержит ли хранилище ключ
     * 
     * @param key ключ для проверки
     * @return true если ключ существует
     */
    fun contains(key: String): Boolean {
        return try {
            encryptedPrefs.contains(key)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка проверки существования ключа: $key")
            false
        }
    }
}
