/**
 * Класс для загрузки и установки обновлений приложения
 * 
 * Этот класс отвечает за:
 * - Загрузку APK файла через DownloadManager
 * - Установку загруженного APK файла
 * 
 * Особенности:
 * - Использует DownloadManager для надежной загрузки
 * - Использует FileProvider для безопасной передачи файла
 * - Обрабатывает ошибки и показывает уведомления
 */
package com.example.lets_go_slavgorod.domain.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import timber.log.Timber
import java.io.File

class UpdateDownloader(private val context: Context) {
    
    companion object {
        private const val DOWNLOAD_SUBDIRECTORY = "updates"
        private const val FILE_PROVIDER_AUTHORITY = "com.example.lets_go_slavgorod.fileprovider"
    }
    
    /**
     * Загружает и устанавливает обновление приложения
     * 
     * Процесс:
     * 1. Загружает APK через DownloadManager
     * 2. После завершения загрузки автоматически устанавливает APK
     * 
     * @param downloadUrl URL для скачивания APK файла
     * @param versionName название версии для имени файла
     * @return ID загрузки DownloadManager или -1 при ошибке
     */
    fun downloadAndInstallUpdate(downloadUrl: String, versionName: String): Long {
        try {
            // Создаем директорию для загрузок, если её нет
            val downloadDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), DOWNLOAD_SUBDIRECTORY)
            if (!downloadDir.exists()) {
                downloadDir.mkdirs()
            }
            
            // Создаем имя файла
            val fileName = "app-update-$versionName.apk"
            val file = File(downloadDir, fileName)
            
            // Удаляем старый файл, если существует
            if (file.exists()) {
                file.delete()
            }
            
            // Настраиваем DownloadManager
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
                setTitle("Обновление приложения")
                setDescription("Загрузка версии $versionName")
                setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "$DOWNLOAD_SUBDIRECTORY/$fileName")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(false)
            }
            
            // Запускаем загрузку
            val downloadId = downloadManager.enqueue(request)
            Timber.d("Загрузка обновления начата, ID: $downloadId")
            
            // Регистрируем BroadcastReceiver для отслеживания завершения загрузки
            // Это делается автоматически через манифест
            
            return downloadId
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при запуске загрузки обновления")
            return -1
        }
    }
    
    /**
     * Устанавливает APK файл
     * 
     * @param apkFile файл APK для установки
     */
    fun installApk(apkFile: File) {
        try {
            if (!apkFile.exists()) {
                Timber.e("APK файл не найден: ${apkFile.absolutePath}")
                return
            }
            
            val apkUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // Используем FileProvider для Android 7.0+
                FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, apkFile)
            } else {
                // Для старых версий используем прямой URI
                Uri.fromFile(apkFile)
            }
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            context.startActivity(intent)
            Timber.d("Запущена установка APK: ${apkFile.absolutePath}")
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при установке APK")
        }
    }
}

