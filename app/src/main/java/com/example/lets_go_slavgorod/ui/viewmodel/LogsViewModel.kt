package com.example.lets_go_slavgorod.ui.viewmodel

import android.content.Context
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * ViewModel для управления логами приложения
 * 
 * Предоставляет функциональность для:
 * - Получения логов из Timber через кастомный Tree
 * - Экспорта логов в файл с поддержкой кириллицы (UTF-8)
 * - Очистки логов и управления историей
 * - Отображения истории логов в реальном времени
 * 
 * Архитектура:
 * - Использует ConcurrentLinkedQueue для thread-safe хранения логов
 * - Автоматически ограничивает размер истории (максимум 1000 записей)
 * - Предоставляет два метода экспорта для максимальной совместимости
 * - Интегрируется с Timber для автоматического сбора логов
 * 
 * Особенности:
 * - Thread-safe операции с логами
 * - Автоматическое управление памятью (FIFO при превышении лимита)
 * - Поддержка кириллицы в экспорте (UTF-8 с BOM и PrintWriter)
 * - Реактивное обновление UI через StateFlow
 * 
 * v3.0 Changes (Октябрь 2025):
 * - Оптимизированы импорты и зависимости
 * - Улучшена производительность работы с логами
 * - Обновлены комментарии и документация
 * - Добавлена система сбора реальных логов
 * - Добавлена история логов с экспортом
 */
class LogsViewModel : ViewModel() {
    
    // =====================================================================================
    //                              ПРИВАТНЫЕ ПОЛЯ
    // =====================================================================================
    
    /**
     * Хранилище логов в памяти (thread-safe)
     * 
     * Использует ConcurrentLinkedQueue для обеспечения thread-safety
     * при одновременном доступе из разных потоков (Timber и UI).
     * Автоматически ограничивает размер до 1000 записей.
     */
    private val logHistory = ConcurrentLinkedQueue<LogEntry>()
    
    // =====================================================================================
    //                              ПУБЛИЧНЫЕ ПОТОКИ ДАННЫХ
    // =====================================================================================
    
    /**
     * Поток данных с текущим списком логов
     * 
     * Содержит отсортированный по времени список всех логов
     * (новые записи сверху). Обновляется автоматически при
     * добавлении новых логов через Timber.
     */
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()
    
    /**
     * Поток данных о состоянии экспорта
     * 
     * Показывает, выполняется ли в данный момент экспорт логов.
     * Используется для блокировки UI во время экспорта.
     */
    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()
    
    /**
     * Поток данных с сообщениями об экспорте
     * 
     * Содержит сообщения об успешном экспорте или ошибках.
     * Автоматически очищается после показа пользователю.
     */
    private val _exportMessage = MutableStateFlow<String?>(null)
    val exportMessage: StateFlow<String?> = _exportMessage.asStateFlow()
    
    // =====================================================================================
    //                              ИНИЦИАЛИЗАЦИЯ
    // =====================================================================================
    
    init {
        // Инициализируем систему логирования при создании ViewModel
        setupLogging()
    }
    
    // =====================================================================================
    //                              НАСТРОЙКА ЛОГИРОВАНИЯ
    // =====================================================================================
    
    /**
     * Настраивает систему логирования для автоматического сбора логов
     * 
     * Создает кастомный Timber.Tree, который перехватывает все логи
     * приложения и сохраняет их в историю. Обеспечивает:
     * - Автоматическое преобразование уровней логирования
     * - Добавление stack trace для ошибок
     * - Ограничение размера истории (FIFO при превышении лимита)
     * - Обновление UI в реальном времени
     * 
     * Thread-safety: Timber.Tree вызывается из разных потоков,
     * поэтому используется ConcurrentLinkedQueue для безопасного доступа.
     */
    private fun setupLogging() {
        // Добавляем кастомный Tree для сбора логов
        Timber.plant(object : Timber.Tree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                val level = when (priority) {
                    android.util.Log.VERBOSE -> "VERBOSE"
                    android.util.Log.DEBUG -> "DEBUG"
                    android.util.Log.INFO -> "INFO"
                    android.util.Log.WARN -> "WARN"
                    android.util.Log.ERROR -> "ERROR"
                    else -> "UNKNOWN"
                }
                
                val logEntry = LogEntry(
                    timestamp = System.currentTimeMillis(),
                    level = level,
                    tag = tag ?: "Unknown",
                    message = message + if (t != null) "\n${t.stackTraceToString()}" else ""
                )
                
                // Добавляем в историю (ограничиваем размер)
                logHistory.offer(logEntry)
                while (logHistory.size > 1000) { // Максимум 1000 записей
                    logHistory.poll()
                }
                
                // Обновляем UI
                _logs.value = logHistory.toList().sortedByDescending { it.timestamp }
            }
        })
    }
    
    // =====================================================================================
    //                              УПРАВЛЕНИЕ ЛОГАМИ
    // =====================================================================================
    
    /**
     * Загружает логи из истории и обновляет UI
     * 
     * Синхронизирует отображаемый список логов с внутренним хранилищем.
     * Если история пуста, добавляет тестовые логи для демонстрации.
     * 
     * @param context контекст приложения (не используется, но требуется для совместимости)
     */
    fun loadLogs(context: Context) {
        viewModelScope.launch {
            try {
                // Обновляем список логов из истории
                _logs.value = logHistory.toList().sortedByDescending { it.timestamp }
                
                // Добавляем тестовые логи, если история пуста
                if (logHistory.isEmpty()) {
                    addTestLogs()
                }
                
                Timber.d("Загружено ${logHistory.size} логов из истории")
            } catch (e: Exception) {
                Timber.e(e, "Ошибка загрузки логов")
                _logs.value = listOf(
                    LogEntry(
                        timestamp = System.currentTimeMillis(),
                        level = "ERROR",
                        tag = "LogsViewModel",
                        message = "Не удалось загрузить логи: ${e.message}"
                    )
                )
            }
        }
    }
    
    /**
     * Добавляет тестовые логи для демонстрации функциональности
     * 
     * Создает набор реалистичных логов с различными уровнями важности
     * и временными метками для демонстрации возможностей системы.
     * Используется только если история логов пуста.
     * 
     * Логи включают:
     * - Запуск приложения
     * - Загрузку расписания
     * - Планирование уведомлений
     * - Сетевые предупреждения
     * - Ошибки загрузки данных
     * - Действия пользователя
     * - Изменения темы
     * - Операции экспорта
     */
    private fun addTestLogs() {
        val testLogs = listOf(
            LogEntry(
                timestamp = System.currentTimeMillis() - 300000, // 5 минут назад
                level = "INFO",
                tag = "MainActivity",
                message = "Приложение 'Поехали! Славгород' успешно запущено"
            ),
            LogEntry(
                timestamp = System.currentTimeMillis() - 240000, // 4 минуты назад
                level = "DEBUG",
                tag = "ScheduleViewModel",
                message = "Загружено расписание для маршрута №102 (Славгород — Яровое)"
            ),
            LogEntry(
                timestamp = System.currentTimeMillis() - 180000, // 3 минуты назад
                level = "INFO",
                tag = "NotificationManager",
                message = "Уведомление запланировано на 08:00 для маршрута 'Вокзал — Совхоз'"
            ),
            LogEntry(
                timestamp = System.currentTimeMillis() - 120000, // 2 минуты назад
                level = "WARN",
                tag = "NetworkManager",
                message = "Обнаружено медленное соединение с сервером обновлений"
            ),
            LogEntry(
                timestamp = System.currentTimeMillis() - 60000, // 1 минута назад
                level = "ERROR",
                tag = "DataRepository",
                message = "Ошибка загрузки данных расписания: Connection timeout"
            ),
            LogEntry(
                timestamp = System.currentTimeMillis() - 30000, // 30 секунд назад
                level = "INFO",
                tag = "UserAction",
                message = "Пользователь добавил в избранное время отправления 14:30"
            ),
            LogEntry(
                timestamp = System.currentTimeMillis() - 15000, // 15 секунд назад
                level = "DEBUG",
                tag = "ThemeManager",
                message = "Применена тёмная тема приложения"
            ),
            LogEntry(
                timestamp = System.currentTimeMillis() - 5000, // 5 секунд назад
                level = "INFO",
                tag = "ExportManager",
                message = "Начало экспорта логов в файл с поддержкой кириллицы"
            )
        )
        
        testLogs.forEach { logHistory.offer(it) }
        _logs.value = logHistory.toList().sortedByDescending { it.timestamp }
    }
    
    // =====================================================================================
    //                              ЭКСПОРТ ЛОГОВ
    // =====================================================================================
    
    /**
     * Основной метод экспорта логов в файл с поддержкой кириллицы
     * 
     * Экспортирует все логи в текстовый файл с использованием UTF-8 кодировки
     * и BOM (Byte Order Mark) для максимальной совместимости с различными
     * текстовыми редакторами.
     * 
     * Особенности:
     * - Использует UTF-8 BOM для корректного отображения кириллицы
     * - Сохраняет файл в папку Downloads с временной меткой
     * - Включает заголовок с информацией о файле
     * - Сортирует логи по времени (новые сверху)
     * - Обрабатывает ошибки и показывает статус пользователю
     * 
     * @param context контекст приложения для доступа к файловой системе
     */
    fun exportLogs(context: Context) {
        viewModelScope.launch {
            _isExporting.value = true
            _exportMessage.value = null
            
            try {
                val logs = _logs.value
                if (logs.isEmpty()) {
                    _exportMessage.value = "Нет логов для экспорта"
                    return@launch
                }
                
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "slavgorod_logs_$timestamp.txt"
                
                // Создаем файл в общей папке Downloads
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                
                val logFile = File(downloadsDir, fileName)
                
                // Используем FileOutputStream с явной записью UTF-8 BOM для лучшей совместимости
                FileOutputStream(logFile).use { outputStream ->
                    // Записываем UTF-8 BOM (Byte Order Mark) для корректного отображения кириллицы
                    outputStream.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                    
                    // Используем OutputStreamWriter с UTF-8 кодировкой
                    OutputStreamWriter(outputStream, StandardCharsets.UTF_8).use { writer ->
                        writer.write("Логи приложения Славгород\n")
                        writer.write("Экспортировано: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
                        writer.write("Всего записей: ${logs.size}\n")
                        writer.write("Кодировка: UTF-8 с BOM\n")
                        writer.write("=".repeat(50) + "\n\n")
                        
                        logs.forEach { log ->
                            writer.write("${log.formattedTimestamp} [${log.level}] ${log.tag}: ${log.message}\n")
                        }
                        
                        writer.write("\n" + "=".repeat(50) + "\n")
                        writer.write("Конец файла логов\n")
                    }
                }
                
                _exportMessage.value = "Логи экспортированы в папку Downloads: $fileName"
                Timber.d("Логи экспортированы в файл: ${logFile.absolutePath}")
                
            } catch (e: Exception) {
                Timber.e(e, "Ошибка экспорта логов")
                _exportMessage.value = "Ошибка экспорта: ${e.message}"
            } finally {
                _isExporting.value = false
            }
        }
    }
    
    // =====================================================================================
    //                              ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // =====================================================================================
    
    /**
     * Очищает сообщение об экспорте
     * 
     * Удаляет текущее сообщение об экспорте из потока данных.
     * Вызывается после показа сообщения пользователю через Snackbar.
     */
    fun clearExportMessage() {
        _exportMessage.value = null
    }
    
    /**
     * Очищает всю историю логов
     * 
     * Удаляет все логи из внутреннего хранилища и обновляет UI.
     * Используется для освобождения памяти и сброса истории.
     * 
     * Внимание: Операция необратима. Все логи будут потеряны.
     */
    fun clearLogs() {
        viewModelScope.launch {
            logHistory.clear()
            _logs.value = emptyList()
            Timber.d("История логов очищена")
        }
    }
    
    /**
     * Добавляет один тестовый лог для демонстрации
     * 
     * Создает и добавляет в историю один тестовый лог с текущим временем.
     * Используется для тестирования функциональности добавления логов
     * и демонстрации работы системы в реальном времени.
     */
    fun addTestLog() {
        val testLog = LogEntry(
            timestamp = System.currentTimeMillis(),
            level = "INFO",
            tag = "TestLog",
            message = "Тестовый лог добавлен в ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}"
        )
        
        logHistory.offer(testLog)
        _logs.value = logHistory.toList().sortedByDescending { it.timestamp }
        Timber.d("Добавлен тестовый лог")
    }
    
    /**
     * Альтернативный метод экспорта логов с использованием PrintWriter
     * 
     * Предоставляет альтернативный способ экспорта логов для случаев,
     * когда основной метод не обеспечивает корректное отображение
     * кириллицы в некоторых редакторах.
     * 
     * Особенности:
     * - Использует PrintWriter с явным указанием UTF-8 кодировки
     * - Автоматически управляет кодировкой и переносами строк
     * - Создает файл с суффиксом "_alt" для различения
     * - Более простой и надежный подход для некоторых редакторов
     * 
     * Рекомендации:
     * - Используйте основной метод exportLogs() в первую очередь
     * - Применяйте этот метод если кириллица отображается некорректно
     * 
     * @param context контекст приложения для доступа к файловой системе
     */
    fun exportLogsAlternative(context: Context) {
        viewModelScope.launch {
            _isExporting.value = true
            _exportMessage.value = null
            
            try {
                val logs = _logs.value
                if (logs.isEmpty()) {
                    _exportMessage.value = "Нет логов для экспорта"
                    return@launch
                }
                
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "slavgorod_logs_alt_$timestamp.txt"
                
                // Создаем файл в общей папке Downloads
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                
                val logFile = File(downloadsDir, fileName)
                
                // Используем PrintWriter с UTF-8 кодировкой
                PrintWriter(logFile, "UTF-8").use { writer ->
                    writer.println("Логи приложения Славгород")
                    writer.println("Экспортировано: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
                    writer.println("Всего записей: ${logs.size}")
                    writer.println("Кодировка: UTF-8 (альтернативный метод)")
                    writer.println("=".repeat(50))
                    writer.println()
                    
                    logs.forEach { log ->
                        writer.println("${log.formattedTimestamp} [${log.level}] ${log.tag}: ${log.message}")
                    }
                    
                    writer.println()
                    writer.println("=".repeat(50))
                    writer.println("Конец файла логов")
                }
                
                _exportMessage.value = "Логи экспортированы (альтернативный метод): $fileName"
                Timber.d("Логи экспортированы альтернативным методом: ${logFile.absolutePath}")
                
            } catch (e: Exception) {
                Timber.e(e, "Ошибка альтернативного экспорта логов")
                _exportMessage.value = "Ошибка экспорта: ${e.message}"
            } finally {
                _isExporting.value = false
            }
        }
    }
    
}

/**
 * Представляет одну запись лога в системе логирования
 * 
 * Содержит всю необходимую информацию для отображения, хранения и экспорта логов.
 * Используется как в UI для отображения, так и в файловой системе для экспорта.
 * 
 * Особенности:
 * - Автоматическое форматирование времени для отображения
 * - Цветовая индикация уровня важности для UI
 * - Поддержка кириллицы в сообщениях
 * - Immutable data class для thread-safety
 * 
 * @param timestamp время создания записи в миллисекундах (System.currentTimeMillis())
 * @param level уровень важности лога (ERROR, WARN, INFO, DEBUG, VERBOSE)
 * @param tag тег компонента, который создал запись (например, "MainActivity", "ScheduleViewModel")
 * @param message текст сообщения (может содержать кириллицу и специальные символы)
 * 
 * @author VseMirka200
 * @version 3.0
 * @since 2.1
 */
data class LogEntry(
    val timestamp: Long,
    val level: String,
    val tag: String,
    val message: String
) {
    /**
     * Форматированное время для отображения в UI
     * 
     * Преобразует timestamp в читаемый формат времени (HH:mm:ss).
     * Используется в списке логов для показа времени создания записи.
     * 
     * @return строка времени в формате "14:30:25"
     */
    val formattedTimestamp: String
        get() = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
    
    /**
     * Цвет для отображения уровня важности лога в UI
     * 
     * Возвращает цвет в формате Long для использования в Compose.
     * Каждый уровень важности имеет свой уникальный цвет:
     * - ERROR: красный (0xFFD32F2F)
     * - WARN: оранжевый (0xFFFF9800)
     * - INFO: синий (0xFF2196F3)
     * - DEBUG: зеленый (0xFF4CAF50)
     * - Остальные: серый (0xFF757575)
     * 
     * @return цвет в формате Long для MaterialTheme
     */
    val levelColor: Long
        get() = when (level) {
            "ERROR" -> 0xFFD32F2F.toLong() // Красный
            "WARN" -> 0xFFFF9800.toLong()  // Оранжевый
            "INFO" -> 0xFF2196F3.toLong()  // Синий
            "DEBUG" -> 0xFF4CAF50.toLong() // Зеленый
            else -> 0xFF757575.toLong()    // Серый
        }
}
