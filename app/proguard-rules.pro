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
# Архитектура правил:
# - Общие настройки для всех классов
# - Защита AndroidX компонентов
# - Защита критически важных классов приложения
# - Оптимизации производительности
# - Удаление отладочного кода
#
# Для подробной информации см.:
# http://developer.android.com/guide/developing/tools/proguard.html
# =====================================================================================

# =====================================================================================
#                              ОБЩИЕ НАСТРОЙКИ
# =====================================================================================

# =============================================================================
# КРИТИЧЕСКИ ВАЖНО ДЛЯ КОНВЕРТАЦИИ AAB -> APK!
# =============================================================================
# При конвертации AAB в APK R8 может удалить классы, которые используются
# через рефлексию или динамически. Эти правила предотвращают это.

# Сохраняем номера строк для отладки stack traces (ВАЖНО для отладки!)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Сохраняем все аннотации для рефлексии (КРИТИЧЕСКИ ВАЖНО!)
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes RuntimeInvisibleParameterAnnotations

# Оптимизация: удаляем неиспользуемые атрибуты
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Дополнительные оптимизации для производительности
-keepattributes Exceptions
-keepattributes StackTrace

# ВАЖНО: Сохраняем все классы для рефлексии (нужно для AAB -> APK)
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod

# Сохраняем метаданные Kotlin
-keepclassmembers class ** {
    @kotlin.Metadata *;
}

# ВАЖНО: Не удаляем классы исключений для отладки
-keep class * extends java.lang.Exception
-keep class * extends java.lang.Error
-keepclassmembers class * extends java.lang.Exception {
    <init>(...);
}

# КРИТИЧЕСКИ ВАЖНО: Не обфусцируем имена классов приложения
# Это нужно для работы рефлексии в Koin, Compose, Navigation
-keepnames class ru.slavgorod.transport.** { *; }
-keepclassmembernames class ru.slavgorod.transport.** {
    <methods>;
    <fields>;
}

# =====================================================================================
#                              ANDROIDX И ANDROID КОМПОНЕНТЫ
# =====================================================================================

# Room database - критически важные классы для работы с БД
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keep class androidx.room.** { *; }
-keep class androidx.room.migration.** { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public <init>();
}
-keepclassmembers class * {
    @androidx.room.Entity <fields>;
    @androidx.room.PrimaryKey <fields>;
    @androidx.room.ColumnInfo <fields>;
}
-dontwarn androidx.room.paging.**

# Room Database приложения
-keep class ru.slavgorod.transport.data.local.dao.** { *; }
-keep class ru.slavgorod.transport.data.local.entity.** { *; }

# DataStore - настройки приложения (КРИТИЧЕСКИ ВАЖНО!)
-keep class androidx.datastore.** { *; }
-keep interface androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# DataStore Preferences - защищаем все классы для хранения настроек
-keep class androidx.datastore.preferences.core.** { *; }
-keep class androidx.datastore.preferences.core.Preferences { *; }
-keep class androidx.datastore.preferences.core.PreferenceDataStoreFactory { *; }
-keep class androidx.datastore.preferences.** { *; }

# DataStore Preferences Keys
-keep class androidx.datastore.preferences.core.Preferences$Key { *; }
-keep class androidx.datastore.preferences.core.booleanPreferencesKey { *; }
-keep class androidx.datastore.preferences.core.stringPreferencesKey { *; }
-keep class androidx.datastore.preferences.core.intPreferencesKey { *; }
-keep class androidx.datastore.preferences.core.longPreferencesKey { *; }
-keep class androidx.datastore.preferences.core.floatPreferencesKey { *; }

# Наши классы для DataStore
-keep class ru.slavgorod.transport.data.local.UpdatePreferences { *; }
-keep class ru.slavgorod.transport.data.local.UpdatePreferences$* { *; }
-keep class ru.slavgorod.transport.data.local.UpdatePreferencesKeys { *; }
-keep class ru.slavgorod.transport.data.local.UpdatePreferencesKeys$* { *; }
-keepclassmembers class ru.slavgorod.transport.data.local.UpdatePreferences {
    <init>(...);
    <methods>;
    <fields>;
}
-keepclassmembers class ru.slavgorod.transport.data.local.UpdatePreferencesKeys {
    <fields>;
    <methods>;
}

# DataStore extension properties для Context (КРИТИЧЕСКИ ВАЖНО!)
-keepclassmembers class android.content.Context {
    *** updatePreferencesDataStore;
    *** themeDataStore;
    *** dataStore;
    *** displayDataStore;
    *** disclaimerDataStore;
}

# Все DataStore extension файлы
-keep class ru.slavgorod.transport.data.local.DataStore { *; }
-keep class ru.slavgorod.transport.data.local.NotificationTimePreferences { *; }
-keep class ru.slavgorod.transport.data.local.DisclaimerPreferences { *; }
-keep class ru.slavgorod.transport.data.local.DisclaimerPreferences$* { *; }

# DataStore extension для Context
-keep class ru.slavgorod.transport.data.local.** { *; }
-keepclassmembers class ru.slavgorod.transport.data.local.** {
    <methods>;
}

# NotificationPreferencesCache
-keep class ru.slavgorod.transport.data.local.NotificationPreferencesCache { *; }
-keep class ru.slavgorod.transport.data.local.NotificationPreferencesCache$* { *; }

# Flow для DataStore - защищаем все Flow операции
-keep class kotlinx.coroutines.flow.Flow { *; }
-keep class kotlinx.coroutines.flow.StateFlow { *; }
-keepclassmembers class kotlinx.coroutines.flow.** {
    <methods>;
}

# Kotlin делегаты для DataStore (by preferencesDataStore)
-keep class kotlin.properties.ReadOnlyProperty { *; }
-keep class kotlin.properties.ReadWriteProperty { *; }

# Compose - UI компоненты (КРИТИЧЕСКИ ВАЖНО!)
-keep class androidx.compose.** { *; }
-keep interface androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Compose Runtime - защищаем все Composable функции
-keep @androidx.compose.runtime.Composable class * { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# Compose Navigation
-keep class androidx.compose.runtime.Composable { *; }
-keep @androidx.compose.runtime.Composable class * { *; }

# Compose Material3 и другие UI библиотеки
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.material.** { *; }
-keep class androidx.compose.foundation.** { *; }
-keep class androidx.compose.ui.** { *; }

# Navigation - навигация между экранами (КРИТИЧЕСКИ ВАЖНО!)
-keep class androidx.navigation.** { *; }
-keep class androidx.navigation.compose.** { *; }
-keep interface androidx.navigation.compose.** { *; }
-dontwarn androidx.navigation.**

# Compose Navigation - защищаем все навигационные классы
-keep class * implements androidx.navigation.NavGraph { *; }
-keep class * implements androidx.navigation.NavDestination { *; }

# Screen sealed class для навигации (КРИТИЧЕСКИ ВАЖНО!)
-keep class ru.slavgorod.transport.ui.navigation.Screen { *; }
-keep class ru.slavgorod.transport.ui.navigation.Screen$* { *; }
-keepclassmembers class ru.slavgorod.transport.ui.navigation.Screen {
    *;
}

# Все экраны настроек
-keep class ru.slavgorod.transport.ui.screens.settings.** { *; }
-keepclassmembers class ru.slavgorod.transport.ui.screens.settings.** {
    <methods>;
}

# Все UI компоненты
-keep class ru.slavgorod.transport.ui.components.** { *; }
-keepclassmembers class ru.slavgorod.transport.ui.components.** {
    @androidx.compose.runtime.Composable <methods>;
    <methods>;
}

# Все экраны приложения
-keep class ru.slavgorod.transport.ui.screens.** { *; }
-keepclassmembers class ru.slavgorod.transport.ui.screens.** {
    @androidx.compose.runtime.Composable <methods>;
    <methods>;
}

# Composable функции для навигации
-keep @androidx.compose.runtime.Composable class ru.slavgorod.transport.ui.screens.** { *; }
-keepclassmembers class ru.slavgorod.transport.ui.screens.** {
    @androidx.compose.runtime.Composable <methods>;
}

# Lifecycle - управление жизненным циклом
-keep class androidx.lifecycle.** { *; }

# ViewModel - управление состоянием
-keep class androidx.lifecycle.ViewModel { *; }
-keep class androidx.lifecycle.AndroidViewModel { *; }

# =============================================================================
# КРИТИЧЕСКИ ВАЖНЫЕ КЛАССЫ ПРИЛОЖЕНИЯ
# =============================================================================

# Модели данных - должны быть доступны для сериализации
-keep class ru.slavgorod.transport.data.model.** { *; }
-keep class ru.slavgorod.transport.data.local.entity.** { *; }

# ViewModels - управление состоянием UI (КРИТИЧЕСКИ ВАЖНО!)
-keep class ru.slavgorod.transport.ui.viewmodel.** { *; }
-keepclassmembers class ru.slavgorod.transport.ui.viewmodel.** {
    <init>(...);
    <methods>;
    <fields>;
}

# Защищаем все ViewModels из AppModule
-keep class ru.slavgorod.transport.ui.viewmodel.DataManagementViewModel { *; }
-keep class ru.slavgorod.transport.ui.viewmodel.DisplaySettingsViewModel { *; }
-keep class ru.slavgorod.transport.ui.viewmodel.NotificationSettingsViewModel { *; }
-keep class ru.slavgorod.transport.ui.viewmodel.QuietModeViewModel { *; }
-keep class ru.slavgorod.transport.ui.viewmodel.RoutesViewModel { *; }
-keep class ru.slavgorod.transport.ui.viewmodel.ScheduleViewModel { *; }
-keep class ru.slavgorod.transport.ui.viewmodel.ThemeViewModel { *; }
-keep class ru.slavgorod.transport.ui.viewmodel.UpdateSettingsViewModel { *; }
-keep class ru.slavgorod.transport.ui.viewmodel.VibrationSettingsViewModel { *; }

# Уведомления - система уведомлений
-keep class ru.slavgorod.transport.notifications.** { *; }

# BroadcastReceiver - обработка системных событий (КРИТИЧЕСКИ ВАЖНО!)
-keep class * extends android.content.BroadcastReceiver { *; }
-keepclassmembers class * extends android.content.BroadcastReceiver {
    public <init>();
    public void onReceive(android.content.Context, android.content.Intent);
}

# Конкретные BroadcastReceiver приложения
-keep class ru.slavgorod.transport.data.notification.BootReceiver { *; }
-keep class ru.slavgorod.transport.data.notification.AlarmReceiver { *; }
-keep class ru.slavgorod.transport.data.notification.UpdateDownloadReceiver { *; }

# AlarmManager - планирование уведомлений
-keep class android.app.AlarmManager { *; }
-keep class android.app.PendingIntent { *; }

# Главные классы приложения (КРИТИЧЕСКИ ВАЖНО!)
-keep class ru.slavgorod.transport.app.bootstrap.BusApplication { *; }
-keepclassmembers class ru.slavgorod.transport.app.bootstrap.BusApplication {
    <init>();
    void onCreate();
    void onTerminate();
    <methods>;
    <fields>;
}
-keep class ru.slavgorod.transport.app.bootstrap.MainActivity { *; }
-keepclassmembers class ru.slavgorod.transport.app.bootstrap.MainActivity {
    <init>();
    void onCreate(android.os.Bundle);
    <methods>;
}

# =============================================================================
# ОПТИМИЗАЦИИ ПРОИЗВОДИТЕЛЬНОСТИ
# =============================================================================

# Удаляем логирование в релизных сборках (только отладочные уровни)
# ВАЖНО: Оставляем ERROR логи для отладки проблем в релизе
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    # НЕ удаляем e(...) - ошибки нужны для отладки!
}

# Удаляем отладочный код Kotlin (ослабляем для стабильности)
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(...);
    static void checkNotNullParameter(...);
    # Оставляем проверки возвращаемых значений для безопасности
}

# Удаляем Timber логирование в релизе (только отладочные уровни)
-assumenosideeffects class timber.log.Timber {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    # НЕ удаляем e(...) - ошибки нужны для отладки!
}

# Удаляем ConditionalLogging в релизе (только отладочные уровни)
-assumenosideeffects class ru.slavgorod.transport.utils.ConditionalLogging {
    public static *** debug(...);
    public static *** info(...);
    # НЕ удаляем error/warn - нужны для отладки!
}

# =============================================================================
# ОБРАБОТКА ОТСУТСТВУЮЩИХ КЛАССОВ (Missing classes)
# =============================================================================

# Google Errorprone annotations - используются в security-crypto
# Эти аннотации нужны только на этапе компиляции, не нужны в runtime
-dontwarn com.google.errorprone.annotations.**
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.CheckReturnValue
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn com.google.errorprone.annotations.RestrictedApi

# Java annotations (могут использоваться в зависимостях)
-dontwarn javax.annotation.**
-dontwarn javax.annotation.Nullable
-dontwarn javax.annotation.Nonnull

# Google Crypto Tink - используется в androidx.security:security-crypto
-dontwarn com.google.crypto.tink.**
-keep class com.google.crypto.tink.** { *; }

# AndroidX Security Crypto
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**

# =============================================================================
# KOTLIN И COROUTINES
# =============================================================================

# Сохраняем метаданные Kotlin для рефлексии
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers class ** {
    @kotlin.Metadata *;
}

# Coroutines - асинхронное программирование (КРИТИЧЕСКИ ВАЖНО!)
-keep class kotlinx.coroutines.** { *; }
-keep interface kotlinx.coroutines.** { *; }
-keepclassmembers class kotlinx.coroutines.** {
    <methods>;
    <fields>;
}
-dontwarn kotlinx.coroutines.**

# CoroutineScope, SupervisorJob, Dispatchers - используются в BusApplication
-keep class kotlinx.coroutines.CoroutineScope { *; }
-keep class kotlinx.coroutines.SupervisorJob { *; }
-keep class kotlinx.coroutines.Dispatchers { *; }
-keepclassmembers class kotlinx.coroutines.Dispatchers {
    public static final *;
}

# =============================================================================
# ДОПОЛНИТЕЛЬНЫЕ ОПТИМИЗАЦИИ
# =============================================================================

# ВАЖНО: Ослабляем оптимизации для стабильности при конвертации AAB -> APK
# Отключаем агрессивные оптимизации, которые могут сломать приложение
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 1
-allowaccessmodification
-dontpreverify

# НЕ используем -dontshrink, так как это может вызвать проблемы
# Вместо этого защищаем все нужные классы явно

# =============================================================================
# КРИТИЧЕСКИ ВАЖНО ДЛЯ AAB -> APK: Защита от удаления классов
# =============================================================================

# ВАЖНО: Не удаляем классы, которые могут использоваться через рефлексию
-keep,allowobfuscation,allowshrinking class * {
    @org.koin.core.annotation.Single <init>(...);
    @org.koin.core.annotation.Factory <init>(...);
}

# НЕ удаляем классы, которые используются через рефлексию в Koin
-keepclassmembers class * {
    @org.koin.core.annotation.Single <init>(...);
    @org.koin.core.annotation.Factory <init>(...);
    @org.koin.core.annotation.Inject <methods>;
}

# Защита от удаления классов при конвертации AAB -> APK
# Сохраняем все классы, которые могут быть найдены через Class.forName()
-keepnames class * extends java.lang.Exception
-keepnames class * extends java.lang.Error

# НЕ удаляем классы, которые используются в манифесте
-keep class * extends android.app.Activity
-keep class * extends android.app.Service
-keep class * extends android.content.BroadcastReceiver
-keep class * extends android.content.ContentProvider
-keep class * extends androidx.multidex.MultiDexApplication

# =============================================================================
# ДОПОЛНИТЕЛЬНАЯ ЗАЩИТА ДЛЯ AAB -> APK КОНВЕРТАЦИИ
# =============================================================================

# ВАЖНО: При конвертации AAB в APK R8 может удалить классы, которые
# используются через рефлексию. Эти правила предотвращают это.

# КРИТИЧЕСКИ ВАЖНО: Сохраняем ВСЕ классы приложения (не только имена!)
# Это предотвращает удаление классов при конвертации AAB -> APK
# ВАЖНО: Используем -keep, а не только -keepnames, чтобы классы не удалялись
-keep class ru.slavgorod.transport.** { *; }
-keepnames class ru.slavgorod.transport.** { *; }

# Дополнительно: защищаем все методы и поля
-keepclassmembers class ru.slavgorod.transport.** {
    <methods>;
    <fields>;
    <init>(...);
}

# Сохраняем все enum классы (могут использоваться через рефлексию)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Сохраняем все sealed class и их подклассы
-keep class ru.slavgorod.transport.ui.navigation.Screen { *; }
-keep class ru.slavgorod.transport.ui.navigation.Screen$* { *; }

# Защита от удаления классов, используемых в Compose через рефлексию
-keep @androidx.compose.runtime.Composable class * { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# Защита от удаления классов, используемых в Navigation через рефлексию
-keep class * implements androidx.navigation.NavDestination { *; }
-keep class * implements androidx.navigation.NavGraph { *; }

# НЕ удаляем классы, которые используются через Koin рефлексию
# ВАЖНО: При конвертации AAB -> APK эти классы могут быть удалены
# если не защищены явно
-keep class ru.slavgorod.transport.di.** { *; }
-keep class ru.slavgorod.transport.data.** { *; }
-keep class ru.slavgorod.transport.domain.** { *; }
-keep class ru.slavgorod.transport.ui.** { *; }
-keep class ru.slavgorod.transport.core.** { *; }

# Защищаем все классы из манифеста (критически важно для AAB -> APK!)
-keep class ru.slavgorod.transport.app.bootstrap.BusApplication { *; }
-keep class ru.slavgorod.transport.app.bootstrap.MainActivity { *; }
-keep class ru.slavgorod.transport.data.notification.AlarmReceiver { *; }
-keep class ru.slavgorod.transport.data.notification.BootReceiver { *; }
-keep class ru.slavgorod.transport.data.notification.UpdateDownloadReceiver { *; }

# =============================================================================
# КРИТИЧЕСКИ ВАЖНО: Классы, используемые при инициализации BusApplication
# =============================================================================
# Эти классы вызываются в onCreate() и могут быть удалены при AAB -> APK

# Koin модуль
-keep class ru.slavgorod.transport.di.appModule { *; }
-keep class ru.slavgorod.transport.di.appModule$* { *; }

# NotificationHelper - создание канала уведомлений
-keep class ru.slavgorod.transport.data.notification.NotificationHelper { *; }
-keepclassmembers class ru.slavgorod.transport.data.notification.NotificationHelper {
    <methods>;
    <fields>;
}

# DataSyncManager - фоновая синхронизация
-keep class ru.slavgorod.transport.data.workers.DataSyncManager { *; }
-keep class ru.slavgorod.transport.data.workers.DataSyncManager$* { *; }
-keepclassmembers class ru.slavgorod.transport.data.workers.DataSyncManager {
    <methods>;
    <fields>;
}

# NotificationPreferencesCache - кэш настроек
-keep class ru.slavgorod.transport.data.local.NotificationPreferencesCache { *; }
-keep class ru.slavgorod.transport.data.local.NotificationPreferencesCache$* { *; }
-keepclassmembers class ru.slavgorod.transport.data.local.NotificationPreferencesCache {
    <methods>;
    <fields>;
}

# ProcessLifecycleOwner - lifecycle observer
-keep class androidx.lifecycle.ProcessLifecycleOwner { *; }
-keep class androidx.lifecycle.ProcessLifecycleOwner$* { *; }
-keepclassmembers class androidx.lifecycle.ProcessLifecycleOwner {
    <methods>;
}

# LifecycleEventObserver
-keep interface androidx.lifecycle.LifecycleEventObserver { *; }
-keep class * implements androidx.lifecycle.LifecycleEventObserver { *; }

# Core утилиты, используемые в BusApplication
-keep class ru.slavgorod.transport.core.loge { *; }
-keep class ru.slavgorod.transport.core.logd { *; }
-keepclassmembers class ru.slavgorod.transport.core.** {
    <methods>;
}

# UpdateManager - используется через Koin
-keep class ru.slavgorod.transport.domain.update.UpdateManager { *; }
-keepclassmembers class ru.slavgorod.transport.domain.update.UpdateManager {
    <init>(...);
    <methods>;
}

# AlarmScheduler - используется при rescheduleAlarmsOnStartup
-keep class ru.slavgorod.transport.domain.notification.AlarmScheduler { *; }
-keepclassmembers class ru.slavgorod.transport.domain.notification.AlarmScheduler {
    <methods>;
    <fields>;
}

# Удаляем неиспользуемые атрибуты для экономии места
-keepattributes !LocalVariableTable,!LocalVariableTypeTable

# =============================================================================
# НОВЫЕ КОМПОНЕНТЫ (v2.0)
# =============================================================================

# Error Handling система - type-safe обработка ошибок
-keep class ru.slavgorod.transport.data.model.AppError { *; }
-keep class ru.slavgorod.transport.data.model.AppError$* { *; }
-keep class ru.slavgorod.transport.utils.ErrorHandler { *; }

# Update Manager - система обновлений
-keep class ru.slavgorod.transport.updates.UpdateManager { *; }

# ViewModelFactory - создание ViewModels
-keep class ru.slavgorod.transport.ui.viewmodel.AndroidViewModelFactory { *; }
-keep class ru.slavgorod.transport.ui.viewmodel.ContextViewModelFactory { *; }

# Use Cases - бизнес-логика приложения
-keep class ru.slavgorod.transport.domain.usecase.** { *; }

# Remote Data Source - загрузка данных из GitHub
-keep class ru.slavgorod.transport.data.remote.RemoteDataSource { *; }

# =============================================================================
# KOIN DEPENDENCY INJECTION (КРИТИЧЕСКИ ВАЖНО ДЛЯ AAB -> APK)
# =============================================================================

# Koin - система dependency injection
-keep class org.koin.** { *; }
-keep interface org.koin.** { *; }
-dontwarn org.koin.**

# Koin Android
-keep class org.koin.android.** { *; }
-keep class org.koin.androidx.** { *; }
-keep class org.koin.androidx.compose.** { *; }

# Koin модули и конфигурация
-keep class ru.slavgorod.transport.di.** { *; }
-keep class ru.slavgorod.transport.di.AppModule { *; }
-keep class ru.slavgorod.transport.di.AppModule$* { *; }

# ВАЖНО: Защищаем все классы, используемые в Koin модулях через рефлексию

# Koin Context и Scope
-keep class org.koin.core.context.** { *; }
-keep class org.koin.core.scope.** { *; }

# =============================================================================
# ДОПОЛНИТЕЛЬНЫЕ ЗАЩИТЫ ДЛЯ КОНВЕРТАЦИИ AAB -> APK
# =============================================================================

# Сохраняем все классы с @Inject или использующиеся через рефлексию
-keepclassmembers class * {
    @org.koin.core.annotation.Inject <methods>;
}

# Сохраняем конструкторы для классов используемых в Koin модулях
-keep class ru.slavgorod.transport.data.repository.** {
    <init>(...);
    <methods>;
}
-keep class ru.slavgorod.transport.data.local.** {
    <init>(...);
    <methods>;
}
-keep class ru.slavgorod.transport.domain.** {
    <init>(...);
    <methods>;
}

# Критически важно: защищаем все классы из AppModule
-keep class ru.slavgorod.transport.domain.notification.AlarmScheduler { *; }
-keep class ru.slavgorod.transport.data.notification.NotificationHelper { *; }
-keep class ru.slavgorod.transport.domain.update.UpdateManager { *; }
-keep class ru.slavgorod.transport.domain.usecase.SearchRoutesUseCase { *; }
-keep class ru.slavgorod.transport.domain.usecase.GetRouteByIdUseCase { *; }

# Сохраняем классы используемые в Koin через рефлексию
-keepclassmembers class * {
    @org.koin.core.annotation.Single <init>();
    @org.koin.core.annotation.Factory <init>();
}

# Сохраняем все классы используемые в Koin модулях
-keep class ru.slavgorod.transport.data.repository.** { *; }
-keep class ru.slavgorod.transport.data.local.** { *; }
-keep class ru.slavgorod.transport.domain.** { *; }

# Data Sources
-keep class ru.slavgorod.transport.data.local.JsonDataSource { *; }
-keep class ru.slavgorod.transport.data.remote.RemoteDataSource { *; }

# Repository
-keep class ru.slavgorod.transport.data.repository.RoutesTableDataSource { *; }

# Use Cases
-keep class ru.slavgorod.transport.domain.usecase.** { *; }

# Managers
-keep class ru.slavgorod.transport.domain.update.UpdateManager { *; }
-keep class ru.slavgorod.transport.domain.notification.AlarmScheduler { *; }
-keep class ru.slavgorod.transport.data.notification.NotificationHelper { *; }

# =============================================================================
# MULTIDEX ЗАЩИТА
# =============================================================================

# MultiDex Application - критически важно для работы приложения
-keep class androidx.multidex.** { *; }
-keep class ru.slavgorod.transport.app.bootstrap.BusApplication { *; }

# Сохраняем методы для MultiDex
-keepclassmembers class * extends androidx.multidex.MultiDexApplication {
    <init>();
    void attachBaseContext(android.content.Context);
}

# =============================================================================
# WORKMANAGER ЗАЩИТА
# =============================================================================

# WorkManager - фоновые задачи
-keep class androidx.work.** { *; }
-keep class androidx.work.Worker { *; }
-keep class androidx.work.WorkRequest { *; }
-keepclassmembers class * extends androidx.work.Worker {
    <init>(android.content.Context, androidx.work.WorkerParameters);
    public androidx.work.ListenableWorker.Result doWork();
}
-dontwarn androidx.work.**

# WorkManager приложения
-keep class ru.slavgorod.transport.data.workers.** { *; }

# DataSyncManager для фоновой синхронизации
-keep class ru.slavgorod.transport.data.workers.DataSyncManager { *; }
-keep class ru.slavgorod.transport.data.workers.DataSyncManager$* { *; }

# =============================================================================
# FILEPROVIDER ЗАЩИТА
# =============================================================================

# FileProvider для обновлений APK
-keep class androidx.core.content.FileProvider { *; }
-keep class android.support.v4.content.FileProvider { *; }

# =============================================================================
# ОБЩИЕ ЗАЩИТЫ ДЛЯ РЕФЛЕКСИИ
# =============================================================================

# Сохраняем все классы используемые через рефлексию в Compose
-keep class androidx.compose.runtime.Composable { *; }
-keep @androidx.compose.runtime.Composable class * { *; }

# Сохраняем Parcelable классы
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# =============================================================================
# ЗАЩИТА ДЛЯ ПАРСИНГА JSON ИЗ GITHUB (КРИТИЧЕСКИ ВАЖНО!)
# =============================================================================

# JSON библиотека Android (org.json)
-keep class org.json.** { *; }
-dontwarn org.json.**

# Модели данных для парсинга JSON - ВСЕ поля и конструкторы
-keep class ru.slavgorod.transport.data.model.BusRoute {
    <fields>;
    <init>(...);
    <methods>;
}
-keep class ru.slavgorod.transport.data.model.BusSchedule {
    <fields>;
    <init>(...);
    <methods>;
}

# Сохраняем все data классы с их конструкторами и полями
-keepclassmembers class ru.slavgorod.transport.data.model.** {
    <fields>;
    <init>(...);
}

# Сохраняем все методы в моделях данных (для валидации и других методов)
-keep class ru.slavgorod.transport.data.model.** {
    *;
}

# ВАЖНО: Защищаем все методы валидации и утилиты
-keep class ru.slavgorod.transport.domain.util.ValidationUtils { *; }
-keepclassmembers class ru.slavgorod.transport.domain.util.ValidationUtils {
    <methods>;
}

# Защищаем методы создания объектов (createBusRoute и т.д.)
-keep class ru.slavgorod.transport.core.* { *; }
-keepclassmembers class ru.slavgorod.transport.core.* {
    <methods>;
}

# =============================================================================
# ЗАЩИТА NETWORK КЛАССОВ ДЛЯ GITHUB ЗАГРУЗКИ
# =============================================================================

# Java сетевые классы
-keep class java.net.** { *; }
-keep class java.io.** { *; }
-dontwarn java.net.**
-dontwarn java.io.**

# HttpURLConnection и связанные классы
-keep class java.net.HttpURLConnection { *; }
-keep class java.net.URL { *; }
-keep class java.net.URLConnection { *; }

# Android сетевые классы
-keep class android.net.** { *; }
-dontwarn android.net.**

# ConnectivityManager и NetworkCapabilities
-keep class android.net.ConnectivityManager { *; }
-keep class android.net.NetworkCapabilities { *; }
-keep class android.net.Network { *; }

# =============================================================================
# ЗАЩИТА REMOTEDATASOURCE И СВЯЗАННЫХ КЛАССОВ
# =============================================================================

# RemoteDataSource - критически важно для парсинга GitHub
-keep class ru.slavgorod.transport.data.remote.RemoteDataSource { *; }
-keep class ru.slavgorod.transport.data.remote.RemoteDataSource$* { *; }
-keepclassmembers class ru.slavgorod.transport.data.remote.RemoteDataSource {
    <init>(...);
    <methods>;
    <fields>;
}

# DownloadMetrics и связанные классы
-keep class ru.slavgorod.transport.data.remote.DownloadMetrics { *; }
-keep class ru.slavgorod.transport.data.remote.DownloadMetrics$* { *; }
-keep class ru.slavgorod.transport.data.remote.DownloadMetrics$DataSource { *; }
-keep class ru.slavgorod.transport.data.remote.DownloadMetrics$DownloadStats { *; }

# NetworkMonitor для проверки сетевого подключения
-keep class ru.slavgorod.transport.data.network.NetworkMonitor { *; }
-keep class ru.slavgorod.transport.data.network.NetworkMonitor$* { *; }
-keep class ru.slavgorod.transport.data.network.NetworkMonitor$ConnectionType { *; }
-keepclassmembers class ru.slavgorod.transport.data.network.NetworkMonitor {
    <methods>;
    <fields>;
}

# NetworkRequest и NetworkCallback для мониторинга сети
-keep class android.net.NetworkRequest { *; }
-keep class android.net.NetworkRequest$Builder { *; }
-keep class android.net.ConnectivityManager$NetworkCallback { *; }

# JsonDataSource для парсинга
-keep class ru.slavgorod.transport.data.local.JsonDataSource { *; }
-keepclassmembers class ru.slavgorod.transport.data.local.JsonDataSource {
    <init>(...);
    <methods>;
}

# =============================================================================
# ЗАЩИТА KOTLIN DATA CLASSES ДЛЯ JSON ПАРСИНГА
# =============================================================================

# Сохраняем все конструкторы Kotlin data классов с default параметрами
-keepclassmembers class ru.slavgorod.transport.data.model.** {
    <init>(...);
}

# Сохраняем методы copy() для data классов
-keepclassmembers class ru.slavgorod.transport.data.model.** {
    *** copy(...);
}

# Сохраняем методы componentN() для data классов
-keepclassmembers class ru.slavgorod.transport.data.model.** {
    *** component*();
}

# Сохраняем equals, hashCode, toString для data классов
-keepclassmembers class ru.slavgorod.transport.data.model.** {
    boolean equals(java.lang.Object);
    int hashCode();
    java.lang.String toString();
}

# =============================================================================
# ЗАЩИТА КОНСТАНТ И UTILS ДЛЯ ПАРСИНГА
# =============================================================================

# Constants для URL GitHub
-keep class ru.slavgorod.transport.core.Constants { *; }
-keepclassmembers class ru.slavgorod.transport.core.Constants {
    public static final *;
}

# RetryUtils для сетевых запросов
-keep class ru.slavgorod.transport.core.RetryUtils { *; }
-keepclassmembers class ru.slavgorod.transport.core.RetryUtils {
    <methods>;
}

# ValidationUtils для валидации данных
-keep class ru.slavgorod.transport.domain.util.ValidationUtils { *; }
-keepclassmembers class ru.slavgorod.transport.domain.util.ValidationUtils {
    <methods>;
}

# ScheduleUtils для fallback расписаний
-keep class ru.slavgorod.transport.domain.util.ScheduleUtils { *; }
-keepclassmembers class ru.slavgorod.transport.domain.util.ScheduleUtils {
    <methods>;
}

# =============================================================================
# ЗАЩИТА КОРУТИН ДЛЯ АСИНХРОННОЙ ЗАГРУЗКИ
# =============================================================================

# Coroutines для асинхронной загрузки
-keep class kotlinx.coroutines.** { *; }
-keepclassmembers class kotlinx.coroutines.** {
    <methods>;
}
-dontwarn kotlinx.coroutines.**

# =============================================================================
# ЗАЩИТА КЛАССОВ ДЛЯ КЭШИРОВАНИЯ
# =============================================================================

# File операции для кэширования
-keep class java.io.File { *; }
-keepclassmembers class java.io.File {
    <methods>;
}

# Calendar для работы с днями недели
-keep class java.util.Calendar { *; }
-keepclassmembers class java.util.Calendar {
    <methods>;
}
