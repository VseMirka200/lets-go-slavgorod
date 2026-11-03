# Разработка и проверки

## Окружение

Используйте Android Studio с Android SDK для `compileSdk 36` и Gradle Wrapper
из репозитория. Версии плагинов и библиотек заданы в
[`libs.versions.toml`](../gradle/libs.versions.toml).

[`gradle-daemon-jvm.properties`](../gradle/gradle-daemon-jvm.properties)
задаёт JDK 21 для Gradle daemon; Wrapper может загрузить её автоматически.
JDK 17, настраиваемая workflow, не отменяет это требование daemon.
Для проверки расписания используется Node.js 22.

Путь к Android SDK задавайте через локальное окружение или `local.properties`.
Не добавляйте локальные пути и секреты в Git.

## Команды

Из корня репозитория:

```bash
./gradlew --no-daemon testDebugUnitTest lintDebug detekt assembleDebug
node tools/validate-schedule-data.mjs schedule-data/schedule.json
```

В Windows используйте `gradlew.bat`.
Debug APK создаётся в `app/build/outputs/apk/debug/`; его applicationId
имеет суффикс `.debug`. Это отдельная сборка для разработки.

## Структура

| Каталог | Назначение |
| --- | --- |
| `app/src/main/java/ru/slavgorod/transport/` | Kotlin-код приложения |
| `app/src/test/` | Unit-тесты |
| `app/src/androidTest/` | Инструментальные и UI-тесты |
| `schedule-data/` | JSON расписания |
| `tools/` | Проверка расписания |
| `.github/workflows/` | CI и анализ безопасности |
| `docs/` | Сайт и документация |

Инструментальные тесты требуют устройства или эмулятора и отдельно запускаются
через `./gradlew connectedDebugAndroidTest`. Основной workflow CI их не запускает.

## Изменения и выпуск

Изменения отправляйте через PR в `main`. Проверяйте все четыре статуса,
указанные в [настройке защиты](GITHUB_SECURITY_SETUP.md).
Обновления Dependabot сливаются вручную.

Для release необходим существующий ключ подписи и переменные, перечисленные в
[CONTRIBUTING](CONTRIBUTING.md#release-сборки).
Не меняйте applicationId, ключ подписи или опубликованные APK при обычном
исправлении документации. Workflow автоматической публикации релизов отсутствует.
