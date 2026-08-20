# Используемые библиотеки

**Русский** | [English](LIBRARIES_EN.md)

Здесь перечислены основные библиотеки и инструменты, которые используются в проекте «Поехали! Славгород».

## Платформа и интерфейс

- AndroidX Core KTX;
- Lifecycle Runtime, ViewModel Compose и Lifecycle Process;
- Activity Compose;
- Compose BOM, Compose UI, Material, Material 3 и Navigation Compose.

## Данные и фоновые задачи

- Kotlin Coroutines;
- AndroidX DataStore Preferences;
- Gson для обработки JSON;
- OkHttp для сетевых запросов.

## Логирование и внедрение зависимостей

- Timber для логирования;
- Koin Android и Koin Compose для внедрения зависимостей.

## Тестирование

- JUnit;
- Kotlin Test;
- Mockito и Mockito Kotlin;
- Robolectric;
- MockWebServer;
- AndroidX Test Core, JUnit Ext, Espresso и Compose UI Test.

## Сборка и статический анализ

- Android Gradle Plugin;
- Kotlin Android и Kotlin Compose Compiler;
- Detekt;
- Desugar JDK Libraries.

Актуальные версии зависимостей определяются файлами Gradle проекта, прежде всего `gradle/libs.versions.toml`.
