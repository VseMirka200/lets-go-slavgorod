<table>
  <tr>
    <td width="180" align="center" valign="middle">
      <img src="docs/assets/media/branding/app-logo.png" alt="Логотип приложения «Поехали! Славгород»" width="150">
    </td>
    <td valign="middle">
      <h1>Поехали! Славгород</h1>
      <p>Неофициальное Android-приложение с расписанием городского и пригородного транспорта Славгорода.</p>
      <p>Находите маршруты и следите за ближайшими отправлениями.</p>
      <p>
        <a href="https://vsemirka200.github.io/lets-go-slavgorod/"><img alt="Открыть сайт" src="https://img.shields.io/badge/ОТКРЫТЬ_САЙТ-2468dc?style=for-the-badge"></a>
        <a href="https://github.com/VseMirka200/lets-go-slavgorod/releases"><img alt="Скачать APK" src="https://img.shields.io/badge/СКАЧАТЬ_APK-555555?style=for-the-badge&logo=android"></a>
        <a href="https://vsemirka200.github.io/lets-go-slavgorod/schedule.html"><img alt="Расписание" src="https://img.shields.io/badge/РАСПИСАНИЕ-16803c?style=for-the-badge"></a>
      </p>
    </td>
  </tr>
</table>

Версия сборки в проекте — **3.1.2**, минимальная версия Android — **7.0 (API 24)**. Опубликованные версии доступны в GitHub Releases.

> [!IMPORTANT]
> Проект независимый и не является официальным источником транспортной информации. Перед поездкой сверяйте важное время отправления с официальными объявлениями перевозчика.

## Возможности

- городские и пригородные маршруты в одном приложении;
- поиск маршрутов и просмотр времени отправления;
- отдельный экран расписания с ближайшими отправлениями и выбором дня;
- информация об изменениях расписания;
- светлая, тёмная и системная темы;
- локальное сохранение настроек и кэша расписания;
- ручное обновление расписания и обратная связь через ВКонтакте или почту.

Автоматическая проверка работает, пока приложение открыто. Настройка уведомлений присутствует, но фоновая доставка уведомлений об изменениях не гарантируется.

По умолчанию расписание открывается на текущем дне. В настройках отображения можно выбрать режим, в котором сначала выбирается день недели; ранее сохранённый выбор сохраняется.

В разделе **Настройки → Расписание → Обновление расписания** можно проверить состояние и обновить данные. Подробнее — в [руководстве по расписанию](docs/guides/SCHEDULE_DATA.md).

## Сборка из исходного кода

Для сборки используется Gradle Wrapper и Android SDK с платформой API 36. Для Gradle в проекте настроен JDK 21.

```bash
git clone https://github.com/VseMirka200/lets-go-slavgorod.git
cd lets-go-slavgorod
./gradlew assembleDebug
```

В Windows запускайте `./gradlew assembleDebug` из Git Bash или WSL.

## Источник расписания

Основной источник — Google Таблица через Apps Script. Подробности — в [руководстве по расписанию](docs/guides/SCHEDULE_DATA.md).

Веб-страница получает расписание из Google и использует сохранённую в браузере копию при ошибке.

Приложение загружает расписание из основного источника. Если он недоступен, приложение использует последнюю локальную копию при её наличии.

## Полезные ссылки

- [Сайт проекта](https://vsemirka200.github.io/lets-go-slavgorod/)
- [Актуальное расписание](https://vsemirka200.github.io/lets-go-slavgorod/schedule.html)
- [Релизы и загрузка APK](https://github.com/VseMirka200/lets-go-slavgorod/releases)
- [Сообщить об ошибке или предложить улучшение](https://github.com/VseMirka200/lets-go-slavgorod/issues/new)
- [Политика конфиденциальности](docs/policies/PRIVACY.md)
- [Политика безопасности](docs/policies/SECURITY.md)
- [Правила участия в разработке](docs/guides/CONTRIBUTING.md)
- [Как редактировать и передавать расписание](docs/guides/SCHEDULE_DATA.md)
- [Проверки проекта](docs/guides/SECURITY_CHECKS.md)
- [История изменений](docs/release/CHANGELOG.md)
