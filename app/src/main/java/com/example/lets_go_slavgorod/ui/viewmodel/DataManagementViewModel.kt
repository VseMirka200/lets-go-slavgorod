package com.example.lets_go_slavgorod.ui.viewmodel

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lets_go_slavgorod.data.local.dataStore
import com.example.lets_go_slavgorod.ui.viewmodel.themeDataStore
import com.example.lets_go_slavgorod.ui.viewmodel.displayDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

/**
 * ViewModel для управления данными приложения
 * 
 * Функции:
 * - Сброс настроек к значениям по умолчанию
 */
class DataManagementViewModel(private val context: Context) : ViewModel() {

    /**
     * Сброс всех настроек к значениям по умолчанию
     */
    fun resetAllSettings() {
        viewModelScope.launch {
            try {
                Timber.d("=== Starting reset of all settings ===")
                
                withContext(Dispatchers.IO) {
                    // Очищаем все DataStore
                    try {
                        context.dataStore.edit { it.clear() }
                        Timber.d("Main DataStore cleared")
                    } catch (e: Exception) {
                        Timber.e(e, "Error clearing main DataStore")
                    }
                    
                    try {
                        context.themeDataStore.edit { it.clear() }
                        Timber.d("Theme DataStore cleared")
                    } catch (e: Exception) {
                        Timber.e(e, "Error clearing theme DataStore")
                    }
                    
                    try {
                        context.displayDataStore.edit { it.clear() }
                        Timber.d("Display DataStore cleared")
                    } catch (e: Exception) {
                        Timber.e(e, "Error clearing display DataStore")
                    }
                    
                    // Удаляем файлы DataStore напрямую для полной очистки
                    try {
                        val dataStoreDir = File(context.filesDir, "datastore")
                        if (dataStoreDir.exists() && dataStoreDir.isDirectory) {
                            val files = dataStoreDir.listFiles()
                            Timber.d("Found ${files?.size ?: 0} DataStore files to delete")
                            files?.forEach { file ->
                                if (file.delete()) {
                                    Timber.d("Deleted: ${file.name}")
                                } else {
                                    Timber.w("Failed to delete: ${file.name}")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Timber.w(e, "Error deleting DataStore files")
                    }
                    
                    // Очищаем SharedPreferences (если есть)
                    try {
                        val prefsDir = File(context.filesDir.parent, "shared_prefs")
                        if (prefsDir.exists() && prefsDir.isDirectory) {
                            val files = prefsDir.listFiles()
                            Timber.d("Found ${files?.size ?: 0} SharedPreferences files to delete")
                            files?.forEach { file ->
                                if (file.delete()) {
                                    Timber.d("Deleted: ${file.name}")
                                } else {
                                    Timber.w("Failed to delete: ${file.name}")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Timber.w(e, "Error deleting SharedPreferences files")
                    }
                }
                
                Timber.d("=== All settings cleared, restarting app ===")
                
                // Даем время на завершение всех операций
                kotlinx.coroutines.delay(500)
                
                // Перезапускаем приложение
                withContext(Dispatchers.Main) {
                    restartApp()
                }
                
            } catch (e: Exception) {
                Timber.e(e, "Critical error resetting settings")
            }
        }
    }
    
    /**
     * Перезапускает приложение
     */
    private fun restartApp() {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            if (intent != null) {
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                context.startActivity(intent)
                
                // Завершаем текущий процесс
                android.os.Process.killProcess(android.os.Process.myPid())
                kotlin.system.exitProcess(0)
            } else {
                Timber.e("Failed to get launch intent for restart")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error restarting app")
            // Принудительный выход
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }

}


