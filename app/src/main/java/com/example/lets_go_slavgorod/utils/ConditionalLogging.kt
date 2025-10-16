package com.example.lets_go_slavgorod.utils

import com.example.lets_go_slavgorod.BuildConfig
import timber.log.Timber

/**
 * Утилиты для условного логирования с оптимизацией производительности
 * 
 * Предоставляет методы для логирования, которые автоматически отключаются
 * в релизной версии приложения для улучшения производительности и безопасности.
 * 
 * Преимущества над стандартным Timber:
 * - Автоматическое отключение debug/info логов в релизе
 * - Inline функции с lambda для ленивого вычисления сообщений
 * - Нулевая производительность в релизе (код полностью удаляется ProGuard)
 * - Поддержка тегов для категоризации логов
 * 
 * Использование:
 * ```kotlin
 * ConditionalLogging.debug("MyTag") { "Expensive computation: ${heavyOperation()}" }
 * // heavyOperation() НЕ вызовется в release версии
 * ```
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 2.0
 * 
 * @see Timber
 * @see BuildConfig.DEBUG
 */
object ConditionalLogging {
    
    /**
     * Логирует отладочную информацию только в DEBUG версии
     * 
     * В release версии этот код полностью удаляется компилятором
     * и ProGuard, обеспечивая нулевые накладные расходы.
     * 
     * @param tag тег для категоризации логов (опционально)
     * @param message lambda функция возвращающая сообщение для логирования
     */
    inline fun debug(tag: String = "", message: () -> String) {
        if (BuildConfig.DEBUG) {
            Timber.tag(tag).d(message())
        }
    }
    
    /**
     * Логирует информационные сообщения только в DEBUG версии
     * 
     * Используйте для логирования важной информации о состоянии приложения
     * которая полезна при разработке но не нужна в production.
     * 
     * @param tag тег для категоризации логов (опционально)
     * @param message lambda функция возвращающая сообщение для логирования
     */
    inline fun info(tag: String = "", message: () -> String) {
        if (BuildConfig.DEBUG) {
            Timber.tag(tag).i(message())
        }
    }
    
    /**
     * Логирует предупреждения (работает во всех версиях)
     * 
     * Используйте для логирования потенциальных проблем которые
     * не являются критичными но требуют внимания.
     * 
     * @param tag тег для категоризации логов (опционально)
     * @param message lambda функция возвращающая сообщение для логирования
     */
    inline fun warn(tag: String = "", message: () -> String) {
        Timber.tag(tag).w(message())
    }
    
    /**
     * Логирует ошибки (работает во всех версиях)
     * 
     * Используйте для логирования ошибок которые были обработаны
     * но важно знать что они произошли.
     * 
     * @param tag тег для категоризации логов (опционально)
     * @param throwable исключение для логирования (опционально)
     * @param message lambda функция возвращающая сообщение для логирования
     */
    inline fun error(tag: String = "", throwable: Throwable? = null, message: () -> String) {
        if (throwable != null) {
            Timber.tag(tag).e(throwable, message())
        } else {
            Timber.tag(tag).e(message())
        }
    }
    
    /**
     * Логирует критические ошибки которые не должны происходить (работает во всех версиях)
     * 
     * "What a Terrible Failure" - используйте для ошибок которые указывают
     * на серьезные проблемы в коде и не должны происходить в production.
     * 
     * @param tag тег для категоризации логов (опционально)
     * @param message lambda функция возвращающая сообщение для логирования
     */
    inline fun wtf(tag: String = "", message: () -> String) {
        Timber.tag(tag).wtf(message())
    }
}
