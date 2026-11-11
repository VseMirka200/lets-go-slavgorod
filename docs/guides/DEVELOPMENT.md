# Разработка и проверки

## Окружение

Используйте Android Studio с Android SDK для `compileSdk 37` и Gradle Wrapper
из репозитория. Версии плагинов и библиотек заданы в
[`libs.versions.toml`](../../gradle/libs.versions.toml).

[`gradle-daemon-jvm.properties`](../../gradle/gradle-daemon-jvm.properties)
задаёт JDK 21 для Gradle daemon; Wrapper может загрузить её автоматически.
Workflow GitHub Actions также настраивает JDK 21.
Для проверки расписания используется Node.js 22.

Путь к Android SDK задавайте через локальное окружение или `local.properties`.
Не добавляйте локальные пути и секреты в Git.

## Команды

Из корня репозитория:

```bash
./gradlew --no-daemon testDebugUnitTest lintDebug detekt assembleDebug
node --test scripts/*.test.mjs
python scripts/check-links.py
```

В Windows используйте `./gradlew` через Git Bash или WSL.
Debug APK создаётся в `app/build/outputs/apk/debug/`; его applicationId
имеет суффикс `.debug`. Это отдельная сборка для разработки.

## Структура

| Каталог | Назначение |
| --- | --- |
| `app/src/main/java/ru/slavgorod/transport/` | Kotlin-код приложения |
| `app/src/test/` | Unit-тесты |
| `app/src/androidTest/` | Инструментальные и UI-тесты |
| `docs/guides/SCHEDULE_DATA.md` | Источник и процесс публикации расписания |
| `scripts/` | Тесты сайта и проверка опубликованного расписания |
| `.github/workflows/` | CI и анализ безопасности |
| `docs/` | Сайт и документация |

## Сайт

Страницы сайта находятся в `docs/`: `index.html`, `schedule.html`,
`feedback.html` и `support.html`. Общая навигация присутствует на каждой
странице; при её изменении обновляйте все четыре страницы и поиск в
`assets/scripts/features/site-search.js`. Стили основы и адаптации находятся в
`assets/styles/core/`, стили отдельных страниц — в `assets/styles/pages/`.
Скрипт `contact.js` формирует ссылки на почтовые приложения, не отправляя письма.

`sw.js` кэширует оболочку для работы без сети. При изменении опубликованных
файлов увеличивайте версию кэша и добавляйте новые необходимые ресурсы в
`APP_SHELL`. Проверяйте сайт локально через HTTP-сервер, например
`python -m http.server 8000 --directory docs`, затем открывайте
`http://localhost:8000/`. Обновление service worker может требовать перезагрузки.

Инструментальные тесты требуют устройства или эмулятора и отдельно запускаются
через `./gradlew connectedDebugAndroidTest`. Отдельный workflow Android UI tests
запускает их на эмуляторе еженедельно и вручную через GitHub Actions.

## Изменения и выпуск

Изменения отправляйте через PR в `main`. Проверяйте результаты CI перед слиянием.
Обновления Dependabot сливаются вручную.

Для release необходим существующий ключ подписи и переменные, перечисленные в
[CONTRIBUTING](CONTRIBUTING.md#release-сборки).
Не меняйте applicationId, ключ подписи или опубликованные APK при обычном
исправлении документации. Workflow автоматической публикации релизов отсутствует.
