package com.example.lets_go_slavgorod.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import timber.log.Timber

/**
 * Шифрование данных для безопасного хранения
 * 
 * Предоставляет методы для шифрования чувствительных данных
 * с использованием EncryptedSharedPreferences.
 * 
 * Особенности:
 * - AES-256 шифрование
 * - Автоматическое управление ключами
 * - Безопасное хранение настроек
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 2.0
 */
object DataEncryption {
    
    private const val ENCRYPTED_PREFS_NAME = "encrypted_settings"
    
    /**
     * Создает зашифрованные SharedPreferences
     * 
     * @param context контекст приложения
     * @return EncryptedSharedPreferences
     */
    fun createEncryptedPreferences(context: Context): EncryptedSharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        
        return EncryptedSharedPreferences.create(
            context,
            ENCRYPTED_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ) as EncryptedSharedPreferences
    }
    
    /**
     * Безопасно сохраняет строковое значение
     * 
     * @param context контекст приложения
     * @param key ключ для сохранения
     * @param value значение для сохранения
     */
    fun saveEncryptedString(context: Context, key: String, value: String) {
        try {
            val prefs = createEncryptedPreferences(context)
            prefs.edit()
                .putString(key, value)
                .apply()
            Timber.d("🔐 Encrypted string saved for key: $key")
        } catch (e: Exception) {
            Timber.e(e, "Error saving encrypted string for key: $key")
        }
    }
    
    /**
     * Безопасно получает строковое значение
     * 
     * @param context контекст приложения
     * @param key ключ для получения
     * @param defaultValue значение по умолчанию
     * @return зашифрованное значение или значение по умолчанию
     */
    fun getEncryptedString(context: Context, key: String, defaultValue: String = ""): String {
        return try {
            val prefs = createEncryptedPreferences(context)
            prefs.getString(key, defaultValue) ?: defaultValue
        } catch (e: Exception) {
            Timber.e(e, "Error getting encrypted string for key: $key")
            defaultValue
        }
    }
    
    /**
     * Безопасно сохраняет булево значение
     * 
     * @param context контекст приложения
     * @param key ключ для сохранения
     * @param value значение для сохранения
     */
    fun saveEncryptedBoolean(context: Context, key: String, value: Boolean) {
        try {
            val prefs = createEncryptedPreferences(context)
            prefs.edit()
                .putBoolean(key, value)
                .apply()
            Timber.d("🔐 Encrypted boolean saved for key: $key")
        } catch (e: Exception) {
            Timber.e(e, "Error saving encrypted boolean for key: $key")
        }
    }
    
    /**
     * Безопасно получает булево значение
     * 
     * @param context контекст приложения
     * @param key ключ для получения
     * @param defaultValue значение по умолчанию
     * @return зашифрованное значение или значение по умолчанию
     */
    fun getEncryptedBoolean(context: Context, key: String, defaultValue: Boolean = false): Boolean {
        return try {
            val prefs = createEncryptedPreferences(context)
            prefs.getBoolean(key, defaultValue)
        } catch (e: Exception) {
            Timber.e(e, "Error getting encrypted boolean for key: $key")
            defaultValue
        }
    }
    
    /**
     * Очищает все зашифрованные данные
     * 
     * @param context контекст приложения
     */
    fun clearEncryptedData(context: Context) {
        try {
            val prefs = createEncryptedPreferences(context)
            prefs.edit().clear().apply()
            Timber.d("🧹 Encrypted data cleared")
        } catch (e: Exception) {
            Timber.e(e, "Error clearing encrypted data")
        }
    }
}