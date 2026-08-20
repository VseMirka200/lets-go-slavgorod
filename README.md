# Поехали! Славгород

**Русский** | [English](README_EN.md)

![Баннер проекта](docs/assets/images/banner_1.png)

<p align="center">
  <a href="https://vsemirka200.github.io/lets-go-slavgorod/"><img alt="Сайт проекта" src="https://img.shields.io/badge/Сайт-проекта-2ea44f?style=for-the-badge"></a>
  <a href="https://vsemirka200.github.io/lets-go-slavgorod/schedule.html"><img alt="Расписание" src="https://img.shields.io/badge/Расписание-открыть-0969da?style=for-the-badge"></a>
  <a href="https://github.com/VseMirka200/lets-go-slavgorod/releases"><img alt="Релизы" src="https://img.shields.io/badge/Релизы-GitHub-8250df?style=for-the-badge"></a>
  <a href="https://github.com/VseMirka200/lets-go-slavgorod/issues"><img alt="Issues" src="https://img.shields.io/badge/Issues-сообщить_о_проблеме-d1242f?style=for-the-badge"></a>
</p>

**«Поехали! Славгород»** — неофициальное Android-приложение с расписанием городского и пригородного транспорта Славгорода. Оно помогает быстро находить маршруты и остановки, смотреть ближайшие отправления и сохранять нужные маршруты в избранное.

> [!IMPORTANT]
> Проект является независимым и не связан с администрацией города, государственными учреждениями, ООО «Славгородское АТП», МУП «Торговый ряд г. Славгород» или другими муниципальными организациями. Приложение не является официальным источником транспортной информации.

## Возможности

- городские и пригородные маршруты в одном приложении;
- поиск по номеру, названию, описанию, остановкам и времени отправления;
- отдельный экран расписания с фильтрами и ближайшими рейсами;
- избранные маршруты для быстрого доступа;
- локальное сохранение настроек и кэша расписания;
- выбор источника расписания;
- уведомления об изменениях расписания;
- светлая, тёмная и системная темы;
- настройки отображения и поведения интерфейса;
- экспорт журналов приложения по инициативе пользователя.

## Скриншоты

<p>
  <img width="200" src="docs/assets/screenshots/screenshot_1.png" alt="Главный экран приложения">
  <img width="200" src="docs/assets/screenshots/screenshot_2.png" alt="Список маршрутов">
  <img width="200" src="docs/assets/screenshots/screenshot_3.png" alt="Экран расписания">
  <img width="200" src="docs/assets/screenshots/screenshot_4.png" alt="Настройки">
</p>

## Установка

Готовые версии публикуются на странице релизов проекта:

- [Релизы на сайте проекта](https://vsemirka200.github.io/lets-go-slavgorod/releases.html)
- [GitHub Releases](https://github.com/VseMirka200/lets-go-slavgorod/releases)

Минимальная версия Android — **7.0 (API 24)**. Для загрузки актуального расписания требуется доступ к интернету. Разрешение на уведомления используется только для функции уведомлений и может быть отключено пользователем.

## Сборка из исходного кода

Проект использует Gradle Wrapper, поэтому устанавливать Gradle отдельно не требуется.

```bash
git clone https://github.com/VseMirka200/lets-go-slavgorod.git
cd lets-go-slavgorod
./gradlew assembleDebug
```

В Windows:

```powershell
gradlew.bat assembleDebug
```

Debug APK после успешной сборки находится в `app/build/outputs/apk/debug/`.

Полезные проверки перед отправкой изменений:

```bash
./gradlew test
./gradlew lint
./gradlew detekt
```

Для release-сборки требуется настроить подпись через `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` и `KEY_PASSWORD` либо через поддерживаемые Android Gradle Plugin параметры `android.injected.signing.*`.

## Технологии

- Kotlin и Jetpack Compose;
- Material 3 и Navigation Compose;
- Koin для внедрения зависимостей;
- OkHttp и Gson для сетевого слоя и обработки данных;
- DataStore Preferences для локальных настроек;
- Kotlin Coroutines;
- JUnit, Robolectric, Mockito, MockWebServer и AndroidX Test;
- Detekt и Android Lint для статических проверок.

Текущая версия приложения в исходном коде — **3.0.2**. Проект собирается с `compileSdk 36`, `targetSdk 36` и `minSdk 24`.

## Структура репозитория

```text
app/                   Android-приложение, ресурсы и тесты
config/detekt/         конфигурация статического анализа
docs/                  сайт проекта и публичная документация
gradle/                Gradle Wrapper и каталог версий
README.md              обзор проекта
CONTRIBUTING.md        правила участия в разработке
CODE_OF_CONDUCT.md     правила общения и поведения
SECURITY.md            порядок сообщения об уязвимостях
PRIVACY.md             краткая политика конфиденциальности
CHANGELOG.md           история заметных изменений
```

Английские версии корневой документации имеют суффикс `_EN.md`.

## Данные и конфиденциальность

Приложение работает без аккаунта. Пользовательские настройки, избранные маршруты, кэш расписания и локальные журналы хранятся на устройстве. Для получения расписания приложение обращается по сети к выбранному источнику данных. Подробности приведены в [PRIVACY.md](PRIVACY.md) и на [публичной странице политики конфиденциальности](https://vsemirka200.github.io/lets-go-slavgorod/privacy.html).

## Документация

- [Сайт проекта](https://vsemirka200.github.io/lets-go-slavgorod/)
- [Актуальное расписание](https://vsemirka200.github.io/lets-go-slavgorod/schedule.html)
- [Релизы](https://vsemirka200.github.io/lets-go-slavgorod/releases.html)
- [Политика конфиденциальности](PRIVACY.md)
- [Политика безопасности](SECURITY.md)
- [Кодекс поведения](CODE_OF_CONDUCT.md)
- [Используемые библиотеки](https://vsemirka200.github.io/lets-go-slavgorod/libraries.html)
- [Как внести вклад](CONTRIBUTING.md)
- [История изменений](CHANGELOG.md)

## Участие в проекте

Сообщения об ошибках, предложения и pull request приветствуются. Перед изменениями ознакомьтесь с [CONTRIBUTING.md](CONTRIBUTING.md) и [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md). Для потенциальных уязвимостей используйте порядок из [SECURITY.md](SECURITY.md), а не публичный issue.

## Обратная связь

Сообщить об ошибке или предложить улучшение можно через [GitHub Issues](https://github.com/VseMirka200/lets-go-slavgorod/issues) или через раздел контактов на [сайте проекта](https://vsemirka200.github.io/lets-go-slavgorod/#feedback).

## Лицензия

Условия использования исходного кода приведены в существующем файле [LICENSE](LICENSE).
