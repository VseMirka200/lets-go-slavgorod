# Поехали! Славгород

![Баннер проекта](assets/images/banner_1.png)

<p align="center">
  <a href="https://github.com/VseMirka200/lets-go-slavgorod/releases/download/3.0.2/lets-go-slavgorod-3-0-2.apk"><img alt="Скачать" src="https://img.shields.io/badge/-СКАЧАТЬ-555555?style=for-the-badge&logo=github"></a>&nbsp;
  <a href="https://vsemirka200.github.io/lets-go-slavgorod/"><img alt="Сайт проекта" src="https://img.shields.io/badge/-САЙТ-2468dc?style=for-the-badge"></a>&nbsp;
  <a href="https://github.com/VseMirka200/lets-go-slavgorod/issues/new"><img alt="Сообщить об ошибке" src="https://img.shields.io/badge/-ОШИБКА-dc3545?style=for-the-badge&logo=github"></a>
</p>

**«Поехали! Славгород»** — неофициальное Android-приложение с расписанием городского и пригородного транспорта Славгорода. Оно помогает быстро находить маршруты и остановки, смотреть ближайшие отправления и сохранять нужные маршруты в избранное.

Текущая версия — **3.0.2**. Минимальная версия Android — **7.0 (API 24)**.

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
  <img width="200" src="assets/screenshots/screenshot_1.png" alt="Главный экран приложения">
  <img width="200" src="assets/screenshots/screenshot_2.png" alt="Список маршрутов">
  <img width="200" src="assets/screenshots/screenshot_3.png" alt="Экран расписания">
  <img width="200" src="assets/screenshots/screenshot_4.png" alt="Настройки">
</p>

## Установка

Готовая версия **3.0.2** доступна по кнопке **«СКАЧАТЬ»** выше. Также релизы публикуются на:

- [странице релизов проекта](https://vsemirka200.github.io/lets-go-slavgorod/releases.html);
- [GitHub Releases](https://github.com/VseMirka200/lets-go-slavgorod/releases).

Для загрузки актуального расписания требуется доступ к интернету. Разрешение на уведомления используется только для функции уведомлений и может быть отключено пользователем.

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

Полезные проверки:

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

Проект собирается с `compileSdk 36`, `targetSdk 36` и `minSdk 24`.

## Структура репозитория

```text
app/                   Android-приложение, ресурсы и тесты
config/detekt/         конфигурация статического анализа
docs/                  сайт проекта и Markdown-документация
  README.md            обзор проекта
  CONTRIBUTING.md      правила участия в разработке
  CODE_OF_CONDUCT.md   правила общения и поведения
  SECURITY.md          порядок сообщения об уязвимостях
  PRIVACY.md           политика конфиденциальности
  LIBRARIES.md         используемые библиотеки
  CHANGELOG.md         история заметных изменений
gradle/                Gradle Wrapper и каталог версий
LICENSE                лицензия проекта
```

## Данные и конфиденциальность

Приложение работает без аккаунта. Пользовательские настройки, избранные маршруты, кэш расписания и локальные журналы хранятся на устройстве. Для получения расписания приложение обращается по сети к выбранному источнику данных. Подробности приведены в [PRIVACY.md](PRIVACY.md).

## Документация

- [Сайт проекта](https://vsemirka200.github.io/lets-go-slavgorod/)
- [Актуальное расписание](https://vsemirka200.github.io/lets-go-slavgorod/schedule.html)
- [Релизы](https://vsemirka200.github.io/lets-go-slavgorod/releases.html)
- [Политика конфиденциальности](PRIVACY.md)
- [Политика безопасности](SECURITY.md)
- [Кодекс поведения](CODE_OF_CONDUCT.md)
- [Используемые библиотеки](LIBRARIES.md)
- [Как внести вклад](CONTRIBUTING.md)
- [История изменений](CHANGELOG.md)

## Обратная связь

Сообщить об ошибке или предложить улучшение можно через [GitHub Issues](https://github.com/VseMirka200/lets-go-slavgorod/issues/new) или через раздел контактов на [сайте проекта](https://vsemirka200.github.io/lets-go-slavgorod/#feedback).

## Лицензия

Условия использования исходного кода приведены в [LICENSE](../LICENSE).
