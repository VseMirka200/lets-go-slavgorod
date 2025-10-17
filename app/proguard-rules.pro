# =====================================================================================
# ProGuard правила для оптимизации приложения "Поехали! Славгород"
# =====================================================================================
# 
# Высокооптимизированные правила для максимальной производительности:
# - Агрессивное сжатие кода для минимального размера APK
# - Улучшение производительности через оптимизацию байт-кода
# - Защита критически важных классов от обфускации
# - Удаление неиспользуемого кода и ресурсов
# - Оптимизация для быстрого запуска приложения
#
# Основные цели:
# - Уменьшение размера APK на 30-50%
# - Улучшение производительности на 15-25%
# - Защита критически важных классов
# - Удаление неиспользуемого кода и ресурсов
# - Оптимизация для быстрого запуска
#
# Для подробной информации см.:
# http://developer.android.com/guide/developing/tools/proguard.html
# =====================================================================================

# =====================================================================================
#                              ОБЩИЕ НАСТРОЙКИ
# =====================================================================================

# Сохраняем номера строк для отладки stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Сохраняем все аннотации для рефлексии
-keepattributes *Annotation*

# Оптимизация: удаляем неиспользуемые атрибуты
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Дополнительные оптимизации для производительности
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers class ** {
    @kotlin.Metadata *;
}

# =====================================================================================
#                              ANDROIDX И ANDROID КОМПОНЕНТЫ
# =====================================================================================

# Room database - критически важные классы для работы с БД
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keep class androidx.room.** { *; }
-dontwarn androidx.room.paging.**

# DataStore - настройки приложения
-keep class androidx.datastore.** { *; }

# Compose - UI компоненты
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Navigation - навигация между экранами
-keep class androidx.navigation.** { *; }

# Lifecycle - управление жизненным циклом
-keep class androidx.lifecycle.** { *; }

# ViewModel - управление состоянием
-keep class androidx.lifecycle.ViewModel { *; }
-keep class androidx.lifecycle.AndroidViewModel { *; }

# =============================================================================
# КРИТИЧЕСКИ ВАЖНЫЕ КЛАССЫ ПРИЛОЖЕНИЯ
# =============================================================================

# Модели данных - должны быть доступны для сериализации
-keep class com.example.lets_go_slavgorod.data.model.** { *; }
-keep class com.example.lets_go_slavgorod.data.local.entity.** { *; }

# ViewModels - управление состоянием UI
-keep class com.example.lets_go_slavgorod.ui.viewmodel.** { *; }

# Уведомления - система уведомлений
-keep class com.example.lets_go_slavgorod.notifications.** { *; }

# BroadcastReceiver - обработка системных событий
-keep class * extends android.content.BroadcastReceiver { *; }

# AlarmManager - планирование уведомлений
-keep class android.app.AlarmManager { *; }
-keep class android.app.PendingIntent { *; }

# Главные классы приложения
-keep class com.example.lets_go_slavgorod.BusApplication { *; }
-keep class com.example.lets_go_slavgorod.MainActivity { *; }

# =============================================================================
# ОПТИМИЗАЦИИ ПРОИЗВОДИТЕЛЬНОСТИ
# =============================================================================

# Удаляем логирование в релизных сборках
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# Удаляем отладочный код Kotlin
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(...);
    static void checkNotNullParameter(...);
    static void checkReturnedValueIsNotNull(...);
    static void checkNotNullReturnValue(...);
    static void checkFieldIsNotNull(...);
    static void checkNotNullField(...);
    static void checkExpressionValueIsNotNull(...);
    static void checkNotNullExpressionValue(...);
}

# Удаляем Timber логирование в релизе
-assumenosideeffects class timber.log.Timber {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
}

# Удаляем ConditionalLogging в релизе
-assumenosideeffects class com.example.lets_go_slavgorod.utils.ConditionalLogging {
    public static *** debug(...);
    public static *** info(...);
}

# =============================================================================
# KOTLIN И COROUTINES
# =============================================================================

# Сохраняем метаданные Kotlin для рефлексии
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers class ** {
    @kotlin.Metadata *;
}

# Coroutines - асинхронное программирование
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# =============================================================================
# ДОПОЛНИТЕЛЬНЫЕ ОПТИМИЗАЦИИ
# =============================================================================

# Включаем агрессивные оптимизации для уменьшения размера APK
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

# Удаляем неиспользуемые атрибуты для экономии места
-keepattributes !LocalVariableTable,!LocalVariableTypeTable

# =============================================================================
# НОВЫЕ КОМПОНЕНТЫ (v2.0)
# =============================================================================

# Error Handling система - type-safe обработка ошибок
-keep class com.example.lets_go_slavgorod.data.model.AppError { *; }
-keep class com.example.lets_go_slavgorod.data.model.AppError$* { *; }
-keep class com.example.lets_go_slavgorod.utils.ErrorHandler { *; }

# Update Manager - система обновлений
-keep class com.example.lets_go_slavgorod.updates.UpdateManager { *; }

# ViewModelFactory - создание ViewModels
-keep class com.example.lets_go_slavgorod.ui.viewmodel.AndroidViewModelFactory { *; }
-keep class com.example.lets_go_slavgorod.ui.viewmodel.ContextViewModelFactory { *; }

# Use Cases - бизнес-логика приложения
-keep class com.example.lets_go_slavgorod.domain.usecase.** { *; }

# Remote Data Source - загрузка данных из GitHub
-keep class com.example.lets_go_slavgorod.data.remote.RemoteDataSource { *; }