# Используемые библиотеки

Здесь перечислены основные библиотеки и инструменты, которые используются в проекте «Поехали! Славгород».

## Платформа и интерфейс

- AndroidX Core KTX;
- Lifecycle Runtime, ViewModel Compose и Lifecycle Process;
- Activity Compose;
- Compose BOM, Compose UI, Material, Material 3 и Navigation Compose.

## Данные и сетевые запросы

- Kotlin Coroutines;
- AndroidX DataStore Preferences;
- Gson для обработки JSON;
- OkHttp для сетевых запросов.

Периодическая проверка расписания связана с жизненным циклом открытого приложения; WorkManager для неё не используется.

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

## Публикация расписания и сайт

Редакторы используют Google Таблицы и опубликованный Google Apps Script. GitHub Actions и Node.js выполняют проверку и синхронизацию резервной копии. Сайт в `docs/` использует HTML, CSS и JavaScript. Подробности — в [описании передачи расписания](SCHEDULE_DATA.md).
