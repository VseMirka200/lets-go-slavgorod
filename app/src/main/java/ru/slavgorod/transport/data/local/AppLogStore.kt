package ru.slavgorod.transport.data.local

import android.content.Context
import android.util.Log
import ru.slavgorod.transport.R
import ru.slavgorod.transport.domain.util.DateTimeFormatterUtils
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.Date

class AppLogStore(context: Context) {

    private val appContext = context.applicationContext
    private val logDirectory = File(appContext.filesDir, LOG_DIRECTORY_NAME)
    private val logFile = File(logDirectory, LOG_FILE_NAME)
    private val lock = Any()
    private val entryStartPattern =
        Regex("""^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3}( \| | [VDIWEA]/)""")

    init {
        ensureLogFile()
    }

    fun getLogFile(): File {
        ensureLogFile()
        return logFile
    }

    fun ensureLogFile() {
        synchronized(lock) {
            if (logFile.isFile && logFile.length() > 0L) return

            logDirectory.mkdirs()
            logFile.createNewFile()
            logFile.appendText(buildHeaderLine(), StandardCharsets.UTF_8)
        }
    }

    fun append(priority: Int, tag: String?, message: String, throwable: Throwable?) {
        synchronized(lock) {
            ensureLogFile()
            trimToLastEntriesBeforeAppend()
            logFile.appendText(
                buildEntry(priority, tag, message, throwable),
                StandardCharsets.UTF_8
            )
        }
    }

    private fun trimToLastEntriesBeforeAppend() {
        if (!logFile.isFile || logFile.length() <= 0L) return

        val lines = runCatching { logFile.readLines(StandardCharsets.UTF_8) }.getOrNull() ?: return
        val entryStartIndices = lines.withIndex()
            .filter { (_, line) -> entryStartPattern.matches(line) }
            .map { it.index }

        if (entryStartIndices.size <= MAX_LOG_ENTRIES_BEFORE_APPEND) return

        val headerEndIndex = entryStartIndices.first()
        val headerLines = lines.take(headerEndIndex)
        val retainedStartIndices = entryStartIndices.takeLast(MAX_LOG_ENTRIES_BEFORE_APPEND)
        val retainedLines = buildList {
            retainedStartIndices.forEachIndexed { retainedIndex, startIndex ->
                val endIndex = retainedStartIndices.getOrNull(retainedIndex + 1) ?: lines.size
                addAll(lines.subList(startIndex, endIndex))
            }
        }

        logFile.writeText(
            buildString {
                headerLines.forEach { appendLine(it) }
                retainedLines.forEach { appendLine(it) }
            },
            StandardCharsets.UTF_8
        )
    }

    private fun buildHeaderLine(): String {
        return buildString {
            appendLine(appContext.getString(R.string.app_log_header_title))
            appendLine(
                appContext.getString(
                    R.string.app_log_session_started,
                    formatTimestamp(Date())
                )
            )
            appendLine(appContext.getString(R.string.app_log_package, appContext.packageName))
            appendLine(appContext.getString(R.string.app_log_format))
            appendLine()
        }
    }

    private fun buildEntry(
        priority: Int,
        tag: String?,
        message: String,
        throwable: Throwable?
    ): String {
        return buildString {
            append(formatTimestamp(Date()))
            append(" | ")
            append(priorityLabel(priority))
            append(" | ")
            append(sectionLabel(tag))
            append(" | ")
            appendLine(message)
            throwable?.let {
                appendLine(appContext.getString(R.string.app_log_stack_trace))
                appendLine(Log.getStackTraceString(it).trimEnd())
            }
        }
    }

    private fun formatTimestamp(date: Date): String {
        return DateTimeFormatterUtils.formatLogTimestamp(date.time)
    }

    private fun priorityLabel(priority: Int): String {
        return when (priority) {
            Log.VERBOSE -> appContext.getString(R.string.app_log_priority_verbose)
            Log.DEBUG -> appContext.getString(R.string.app_log_priority_debug)
            Log.INFO -> appContext.getString(R.string.app_log_priority_info)
            Log.WARN -> appContext.getString(R.string.app_log_priority_warn)
            Log.ERROR -> appContext.getString(R.string.app_log_priority_error)
            Log.ASSERT -> appContext.getString(R.string.app_log_priority_assert)
            else -> appContext.getString(R.string.app_log_priority_unknown_format, priority)
        }
    }

    private fun sectionLabel(tag: String?): String {
        return when (tag) {
            "UserAction" -> appContext.getString(R.string.app_log_section_user_action)
            "BusApplication" -> appContext.getString(R.string.app_log_section_app_launch)
            "Schedule" -> appContext.getString(R.string.app_log_section_schedule)
            "Navigation" -> appContext.getString(R.string.app_log_section_navigation)
            null, "" -> appContext.getString(R.string.app_log_section_default)
            else -> tag
        }
    }

    private companion object {
        private const val LOG_DIRECTORY_NAME = "logs"
        private const val LOG_FILE_NAME = "app.txt"
        private const val MAX_LOG_ENTRIES = 5000
        private const val MAX_LOG_ENTRIES_BEFORE_APPEND = MAX_LOG_ENTRIES - 1
    }
}
