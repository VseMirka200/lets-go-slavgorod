/**
 * BroadcastReceiver для отслеживания завершения загрузки обновлений
 * 
 * Получает уведомление когда DownloadManager завершает загрузку APK файла
 * и автоматически запускает установку обновления.
 */
package com.example.lets_go_slavgorod.data.notification

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import timber.log.Timber
import com.example.lets_go_slavgorod.domain.update.UpdateDownloader
import java.io.File

class UpdateDownloadReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.DOWNLOAD_COMPLETE") {
            val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            
            if (downloadId == -1L) {
                return
            }
            
            try {
                val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)
                
                if (cursor.moveToFirst()) {
                    val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    val status = cursor.getInt(statusIndex)
                    
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        // Получаем путь к файлу
                        val localUriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                        val localUri = cursor.getString(localUriIndex)
                        
                        if (localUri != null) {
                            val apkFile = File(android.net.Uri.parse(localUri).path ?: "")
                            
                            if (apkFile.exists()) {
                                Timber.d("Загрузка завершена, запускаем установку: ${apkFile.absolutePath}")
                                val downloader = UpdateDownloader(context)
                                downloader.installApk(apkFile)
                            } else {
                                Timber.e("APK файл не найден: $localUri")
                            }
                        } else {
                            // Если URI не получен, пытаемся найти файл по стандартному пути
                            val downloadDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "updates")
                            val files = downloadDir.listFiles()
                            if (files != null && files.isNotEmpty()) {
                                // Берем самый новый файл
                                val latestFile = files.maxByOrNull { it.lastModified() }
                                if (latestFile != null && latestFile.exists()) {
                                    Timber.d("Найден APK файл, запускаем установку: ${latestFile.absolutePath}")
                                    val downloader = UpdateDownloader(context)
                                    downloader.installApk(latestFile)
                                }
                            }
                        }
                    } else if (status == DownloadManager.STATUS_FAILED) {
                        val reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                        val reason = cursor.getInt(reasonIndex)
                        Timber.e("Ошибка загрузки обновления: $reason")
                    }
                }
                cursor.close()
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при обработке завершения загрузки")
            }
        }
    }
}

